package com.voikes.technologies.vozo.mesh

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Peer(
    val id: String,
    val displayName: String,
    val connected: Boolean,
)

sealed interface TransportState {
    data object Idle : TransportState
    data object Starting : TransportState
    data class Running(val discovered: Int) : TransportState
    data class Failed(val reason: String) : TransportState
}

interface Transport {
    val peers: StateFlow<List<Peer>>
    val state: StateFlow<TransportState>

    /** Human readable name shown in the Mesh screen. */
    val label: String

    /** True when this transport needs a host address typed in by the user. */
    val needsManualAddress: Boolean
        get() = false

    /** How a peer on this transport is reached, shown under the peer name. */
    val linkHint: String
        get() = "peer-to-peer"

    /** Addresses this device can be reached on, shown so peers can type them in. */
    val localAddresses: StateFlow<List<String>>
        get() = MutableStateFlow(emptyList())

    /** Inbound frames from any connected peer. */
    var onFrame: (String) -> Unit

    fun start()
    fun stop()

    /** Send to one specific peer. Returns false if it is not reachable. */
    fun send(peerId: String, payload: String): Boolean

    /** Send to every connection this transport holds. Used for relaying. */
    fun broadcast(raw: String) = Unit

    /** Connect to a manually specified address. */
    fun connectTo(address: String) = Unit
}

/** Constants shared by every transport so phone and desktop agree on them. */
object MeshProtocol {
    /** TCP port the phone listens on. */
    const val PORT = 47653

    /** mDNS/NSD service type used for auto discovery. */
    const val SERVICE_TYPE = "_vozo._tcp."

    fun serviceName(peerId: String): String = "vozo-$peerId"
}

class TransportRegistry {
    private val _peers = MutableStateFlow<List<Peer>>(emptyList())
    val peers: StateFlow<List<Peer>> = _peers.asStateFlow()

    fun upsert(peer: Peer) {
        val existing = _peers.value
        val index = existing.indexOfFirst { it.id == peer.id }
        val next = if (index >= 0) {
            existing.toMutableList().also { it[index] = peer }
        } else {
            existing + peer
        }
        _peers.value = next.sortedBy { it.displayName.lowercase() }
    }

    fun remove(peerId: String) {
        _peers.value = _peers.value.filterNot { it.id == peerId }
    }

    fun clear() {
        _peers.value = emptyList()
    }
}

/**
 * Aggregates several transports into a single view of the mesh.
 *
 * A phone runs Nearby (for other phones) plus a TCP host (for laptops). A
 * future wall-mounted extender runs a TCP host plus a TCP client and relays
 * frames between them, so the same aggregation covers every case.
 */
class MeshController(
    val transports: List<Transport>,
    private val registry: TransportRegistry = TransportRegistry(),
) : Transport {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<TransportState>(TransportState.Idle)
    override val state: StateFlow<TransportState> = _state.asStateFlow()
    override val peers: StateFlow<List<Peer>> = registry.peers
    override val label: String = transports.joinToString(" + ") { it.label }
    override val needsManualAddress: Boolean = transports.any { it.needsManualAddress }

    override var onFrame: (String) -> Unit = {}

    init {
        transports.forEach { transport ->
            // A frame arriving on one transport is re-published to the others,
            // which is exactly what a relay node needs.
            transport.onFrame = { raw ->
                onFrame(raw)
                transports.filter { it !== transport }.forEach { it.broadcast(raw) }
            }

            scope.launch {
                transport.peers.collect { found ->
                    found.forEach { registry.upsert(it) }
                }
            }

            scope.launch {
                transport.state.collect { refreshState() }
            }
        }
    }

    private fun refreshState() {
        val states = transports.map { it.state.value }
        _state.value = when {
            states.any { it is TransportState.Failed } ->
                states.filterIsInstance<TransportState.Failed>().first()
            states.any { it is TransportState.Starting } -> TransportState.Starting
            states.any { it is TransportState.Running } ->
                TransportState.Running(registry.peers.value.count { it.connected })
            else -> TransportState.Idle
        }
    }

    override fun start() = transports.forEach { it.start() }

    override fun stop() {
        transports.forEach { it.stop() }
        registry.clear()
    }

    override fun send(peerId: String, payload: String): Boolean =
        transports.any { it.send(peerId, payload) }

    override fun broadcast(raw: String) = transports.forEach { it.broadcast(raw) }

    override fun connectTo(address: String) = transports.forEach { it.connectTo(address) }
}
