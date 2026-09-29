package com.voikes.technologies.vozo.mesh

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    fun start()
    fun stop()
    fun send(peerId: String, payload: String): Boolean
}

class TransportRegistry {
    private val _peers = MutableStateFlow<List<Peer>>(emptyList())
    val peers: StateFlow<List<Peer>> = _peers.asStateFlow()

    fun upsert(peer: Peer) {
        _peers.value = (_peers.value.filterNot { it.id == peer.id } + peer)
            .sortedBy { it.displayName.lowercase() }
    }

    fun remove(peerId: String) {
        _peers.value = _peers.value.filterNot { it.id == peerId }
    }

    fun clear() {
        _peers.value = emptyList()
    }
}
