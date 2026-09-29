package com.voikes.technologies.vozo.mesh

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedInputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Hosts a TCP endpoint that desktops can dial into.
 *
 * Discovery uses Android NSD, but mDNS does not always traverse a phone
 * hotspot, so the phone also publishes its reachable IPv4 addresses and the
 * desktop can always fall back to typing one in.
 */
class TcpHostTransport(
    private val context: Context,
    private val localPeerId: String,
    private val localDisplayName: String,
    private val port: Int = MeshProtocol.PORT,
) : Transport {

    private val _state = MutableStateFlow<TransportState>(TransportState.Idle)
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    private val _peers = MutableStateFlow<List<Peer>>(emptyList())
    override val peers: StateFlow<List<Peer>> = _peers.asStateFlow()

    override val label: String = "LAN"
    override val linkHint: String = "tcp/$port"
    override var onFrame: (String) -> Unit = {}

    private val _localAddresses = MutableStateFlow<List<String>>(emptyList())
    override val localAddresses: StateFlow<List<String>> = _localAddresses.asStateFlow()

    private val sessions = CopyOnWriteArrayList<Session>()
    private var serverSocket: ServerSocket? = null
    private var nsdManager: NsdManager? = null
    private var registration: NsdManager.RegistrationListener? = null

    private class Session(val socket: Socket, val peerId: String, val name: String)

    override fun start() {
        if (serverSocket != null) return
        _state.value = TransportState.Starting

        _localAddresses.value = localIpv4Addresses()

        try {
            val server = ServerSocket(port)
            serverSocket = server
            Thread({ acceptLoop(server) }, "vozo-tcp-accept").apply { isDaemon = true }.start()
            registerNsd(server.localPort)
            _state.value = TransportState.Running(0)
        } catch (e: Exception) {
            _state.value = TransportState.Failed(e.message ?: "cannot open tcp port $port")
        }
    }

    private fun acceptLoop(server: ServerSocket) {
        while (!server.isClosed) {
            val socket = try {
                server.accept()
            } catch (e: Exception) {
                return
            }
            Thread({ handleSocket(socket) }, "vozo-tcp-session").apply { isDaemon = true }.start()
        }
    }

    private fun handleSocket(socket: Socket) {
        var session: Session? = null
        try {
            val input = BufferedInputStream(socket.getInputStream())
            val first = StreamFraming.readFrame(input) ?: return
            val hello = Wire.parse(first)
            if (hello !is Wire.Frame.Hello) return

            session = Session(socket, hello.peerId, hello.displayName)
            sessions.add(session)
            _peers.value = _peers.value.filterNot { it.id == hello.peerId } + Peer(
                id = hello.peerId,
                displayName = hello.displayName,
                connected = true,
            )
            _state.value = TransportState.Running(_peers.value.count { it.connected })

            writeFrame(socket, Wire.hello(localPeerId, localDisplayName))

            while (true) {
                val frame = StreamFraming.readFrame(input) ?: break
                onFrame(frame)
            }
        } catch (e: Exception) {
            // Socket closed or malformed frame; fall through to cleanup.
        } finally {
            session?.let {
                sessions.remove(it)
                _peers.value = _peers.value.filterNot { p -> p.id == it.peerId }
                _state.value = TransportState.Running(_peers.value.count { p -> p.connected })
            }
            try { socket.close() } catch (e: Exception) { }
        }
    }

    private fun writeFrame(socket: Socket, frame: String) {
        StreamFraming.writeFrame(socket.getOutputStream(), frame)
    }

    override fun send(peerId: String, payload: String): Boolean {
        val target = sessions.firstOrNull { it.peerId == peerId } ?: return false
        return try {
            writeFrame(target.socket, payload)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun broadcast(raw: String) {
        sessions.forEach { session ->
            try { writeFrame(session.socket, raw) } catch (e: Exception) { }
        }
    }

    override fun stop() {
        try { registration?.let { nsdManager?.unregisterService(it) } } catch (e: Exception) { }
        registration = null
        nsdManager = null
        try { serverSocket?.close() } catch (e: Exception) { }
        serverSocket = null
        sessions.forEach { try { it.socket.close() } catch (e: Exception) { } }
        sessions.clear()
        _peers.value = emptyList()
        _state.value = TransportState.Idle
    }

    private fun registerNsd(boundPort: Int) {
        try {
            val manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
            nsdManager = manager
            val info = NsdServiceInfo().apply {
                serviceName = MeshProtocol.serviceName(localPeerId)
                serviceType = MeshProtocol.SERVICE_TYPE
                port = boundPort
            }
            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(info: NsdServiceInfo) = Unit
                override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) = Unit
                override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
                override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) = Unit
            }
            registration = listener
            manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            // NSD is a convenience only; manual address entry still works.
        }
    }

    companion object {
        fun localIpv4Addresses(): List<String> = try {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { nic ->
                    nic.interfaceAddresses
                        .mapNotNull { addr ->
                            (addr.address as? Inet4Address)?.takeIf { !it.isLoopbackAddress }?.hostAddress
                        }
                }
                .distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
