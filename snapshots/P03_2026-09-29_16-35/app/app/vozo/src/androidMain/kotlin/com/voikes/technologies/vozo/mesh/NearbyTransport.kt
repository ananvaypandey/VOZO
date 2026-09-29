package com.voikes.technologies.vozo.mesh

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets

class NearbyTransport(
    private val context: Context,
    localPeerId: String,
    localDisplayName: String,
    private val registry: TransportRegistry,
) : Transport {

    private val _state = MutableStateFlow<TransportState>(TransportState.Idle)
    override val state: StateFlow<TransportState> = _state.asStateFlow()
    override val peers: StateFlow<List<Peer>> = registry.peers

    private var client: ConnectionsClient? = null

    private val localToken: ByteArray =
        Wire.hello(localPeerId, localDisplayName).toByteArray(StandardCharsets.UTF_8)
    private val endpointName: String = localDisplayName

    /** logical peerId -> nearby endpointId */
    private val endpointByPeer = mutableMapOf<String, String>()
    private val peerByEndpoint = mutableMapOf<String, String>()
    private val pendingEndpoints = mutableSetOf<String>()

    @Volatile
    var onFrame: (String) -> Unit = {}

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            val raw = String(bytes, StandardCharsets.UTF_8)
            handleFrame(endpointId, raw)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val token = info.endpointInfo
            if (token != null) {
                val frame = Wire.parse(String(token, StandardCharsets.UTF_8))
                if (frame is Wire.Frame.Hello) {
                    registry.upsert(Peer(frame.peerId, frame.displayName, connected = false))
                    peerByEndpoint[endpointId] = frame.peerId
                    endpointByPeer[frame.peerId] = endpointId
                }
            }
            if (pendingEndpoints.add(endpointId)) {
                client?.requestConnection(localToken, endpointId, connectionCallback)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            pendingEndpoints.remove(endpointId)
            val peerId = peerByEndpoint.remove(endpointId) ?: return
            endpointByPeer.remove(peerId)
            registry.peers.value.firstOrNull { it.id == peerId }?.let {
                registry.upsert(it.copy(connected = false))
            }
        }
    }

    private val connectionCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            val token = info.endpointInfo
            if (token != null) {
                val frame = Wire.parse(String(token, StandardCharsets.UTF_8))
                if (frame is Wire.Frame.Hello) {
                    peerByEndpoint[endpointId] = frame.peerId
                    endpointByPeer[frame.peerId] = endpointId
                    registry.upsert(Peer(frame.peerId, frame.displayName, connected = false))
                }
            }
            client?.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            val peerId = peerByEndpoint[endpointId]
            val ok = resolution.status.isSuccess
            if (peerId != null) {
                registry.peers.value.firstOrNull { it.id == peerId }?.let {
                    registry.upsert(it.copy(connected = ok))
                }
            }
            _state.value = TransportState.Running(registry.peers.value.count { it.connected })
        }

        override fun onDisconnected(endpointId: String) {
            peerByEndpoint.remove(endpointId)?.let { peerId ->
                registry.peers.value.firstOrNull { it.id == peerId }?.let {
                    registry.upsert(it.copy(connected = false))
                }
            }
        }
    }

    private fun handleFrame(endpointId: String, raw: String) {
        when (Wire.parse(raw)) {
            is Wire.Frame.Hello -> Unit
            else -> onFrame(raw)
        }
    }

    override fun start() {
        if (!hasPermissions()) {
            _state.value = TransportState.Failed("Nearby permission not granted")
            return
        }
        if (client != null) return

        _state.value = TransportState.Starting
        val c = Nearby.getConnectionsClient(context)
        client = c

        c.startAdvertising(
            localToken,
            endpointName,
            connectionCallback,
            AdvertisingOptions(Strategy.P2P_CLUSTER),
        ).addOnFailureListener { _state.value = TransportState.Failed(it.message ?: "advertise failed") }

        c.startDiscovery(
            DISCOVERY_FILTER,
            discoveryCallback,
            DiscoveryOptions(Strategy.P2P_CLUSTER),
        ).addOnFailureListener { _state.value = TransportState.Failed(it.message ?: "discovery failed") }

        _state.value = TransportState.Running(0)
    }

    override fun stop() {
        client?.stopAdvertising()
        client?.stopDiscovery()
        client?.stopAllEndpoints()
        client = null
        endpointByPeer.clear()
        peerByEndpoint.clear()
        pendingEndpoints.clear()
        registry.clear()
        _state.value = TransportState.Idle
    }

    override fun send(peerId: String, payload: String): Boolean {
        val c = client ?: return false
        val endpoint = endpointByPeer[peerId] ?: return false
        c.sendPayload(endpoint, Payload.fromBytes(payload.toByteArray(StandardCharsets.UTF_8)))
        return true
    }

    fun hasPermissions(): Boolean = requiredPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        /** Nearby's "no filter" sentinel: discover every advertising endpoint. */
        private const val DISCOVERY_FILTER = ""

        fun requiredPermissions(): List<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                )
            } else {
                listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
    }
}
