package com.voikes.technologies.vozo.mesh

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UnavailableTransport(
    private val reason: String = "Mesh transport is Android-only in P03",
) : Transport {

    private val _state = MutableStateFlow<TransportState>(TransportState.Failed(reason))
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    private val _peers = MutableStateFlow<List<Peer>>(emptyList())
    override val peers: StateFlow<List<Peer>> = _peers.asStateFlow()

    @Volatile
    var onFrame: (String) -> Unit = {}

    override fun start() = Unit
    override fun stop() = Unit
    override fun send(peerId: String, payload: String): Boolean = false
}
