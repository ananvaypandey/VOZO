package com.voikes.technologies.vozo.mesh

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedInputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

/**
 * Dials out to a phone running [TcpHostTransport].
 *
 * Tries mDNS discovery first, and always accepts a manually typed
 * `host` or `host:port` so the app still works when mDNS is blocked, which it
 * commonly is on a phone hotspot.
 */
class TcpClientTransport(
    private val localPeerId: String,
    private val localDisplayName: String,
    private val port: Int = MeshProtocol.PORT,
) : Transport {

    private val _state = MutableStateFlow<TransportState>(TransportState.Idle)
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    private val _peers = MutableStateFlow<List<Peer>>(emptyList())
    override val peers: StateFlow<List<Peer>> = _peers.asStateFlow()

    override val label: String = "LAN"
    override val needsManualAddress: Boolean = true
    override var onFrame: (String) -> Unit = {}

    private val sessions = CopyOnWriteArrayList<Session>()
    private var jmdns: JmDNS? = null
    private var listener: ServiceListener? = null

    @Volatile
    var remoteAddress: String = ""
        private set

    /** Last connection error, shown in the Mesh screen and used by tests. */
    @Volatile
    var lastError: String? = null
        private set

    private class Session(val socket: Socket, val peerId: String, val name: String, val address: String)

    override fun start() {
        if (jmdns != null) return
        _state.value = TransportState.Starting
        try {
            val mdns = JmDNS.create()
            jmdns = mdns
            val l = object : ServiceListener {
                override fun serviceAdded(event: ServiceEvent) = Unit
                override fun serviceRemoved(event: ServiceEvent) = Unit
                override fun serviceResolved(event: ServiceEvent) {
                    val info = event.info ?: return
                    val host = info.inetAddresses.firstOrNull()?.hostAddress ?: return
                    dial(host, info.port)
                }
            }
            listener = l
            mdns.addServiceListener(MeshProtocol.SERVICE_TYPE, l)
            _state.value = TransportState.Running(0)
        } catch (e: Exception) {
            _state.value = TransportState.Failed(e.message ?: "mDNS unavailable, type the phone address")
        }
    }

    override fun connectTo(address: String) {
        val trimmed = address.trim()
        if (trimmed.isEmpty()) return
        val host = trimmed.substringBefore(':')
        val targetPort = trimmed.substringAfter(':', "").toIntOrNull() ?: port
        dial(host, targetPort)
    }

    private fun dial(host: String, targetPort: Int) {
        Thread({
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(host, targetPort), 6000)
                socket.tcpNoDelay = true
                remoteAddress = "$host:$targetPort"
                attach(socket)
            } catch (e: Exception) {
                _state.value = TransportState.Failed(e.message ?: "cannot reach $host:$targetPort")
            }
        }, "vozo-tcp-dial").apply { isDaemon = true }.start()
    }

    private fun attach(socket: Socket) {
        Thread({
            try {
                val input = BufferedInputStream(socket.getInputStream())
                StreamFraming.writeFrame(socket.getOutputStream(), Wire.hello(localPeerId, localDisplayName))

                while (true) {
                    val frame = StreamFraming.readFrame(input) ?: break
                    val parsed = Wire.parse(frame)
                    if (parsed is Wire.Frame.Hello) {
                        val session = Session(socket, parsed.peerId, parsed.displayName, remoteAddress)
                        sessions.add(session)
                        _peers.value = _peers.value.filterNot { it.id == parsed.peerId } + Peer(
                            id = parsed.peerId,
                            displayName = parsed.displayName,
                            connected = true,
                        )
                        _state.value = TransportState.Running(_peers.value.count { it.connected })
                    }
                    onFrame(frame)
                }
            } catch (e: Exception) {
                lastError = "${e::class.simpleName}: ${e.message}"
                _state.value = TransportState.Failed(lastError ?: "connection ended")
            } finally {
                sessions.firstOrNull { it.socket === socket }?.let { session ->
                    sessions.remove(session)
                    _peers.value = _peers.value.filterNot { it.id == session.peerId }
                    _state.value = TransportState.Running(_peers.value.count { it.connected })
                }
                try { socket.close() } catch (e: Exception) { }
            }
        }, "vozo-tcp-reader").apply { isDaemon = true }.start()
    }

    override fun send(peerId: String, payload: String): Boolean {
        val target = sessions.firstOrNull { it.peerId == peerId } ?: return false
        return try {
            StreamFraming.writeFrame(target.socket.getOutputStream(), payload)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun broadcast(raw: String) {
        sessions.forEach { session ->
            try { StreamFraming.writeFrame(session.socket.getOutputStream(), raw) } catch (e: Exception) { }
        }
    }

    override fun stop() {
        listener?.let { l -> try { jmdns?.removeServiceListener(MeshProtocol.SERVICE_TYPE, l) } catch (e: Exception) { } }
        listener = null
        try { jmdns?.close() } catch (e: Exception) { }
        jmdns = null
        sessions.forEach { try { it.socket.close() } catch (e: Exception) { } }
        sessions.clear()
        _peers.value = emptyList()
        _state.value = TransportState.Idle
    }
}
