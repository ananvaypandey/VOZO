package com.voikes.technologies.vozo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voikes.technologies.vozo.mesh.Peer
import com.voikes.technologies.vozo.mesh.Transport
import com.voikes.technologies.vozo.mesh.TransportRegistry
import com.voikes.technologies.vozo.mesh.TransportState
import com.voikes.technologies.vozo.ui.theme.VozoCyan
import com.voikes.technologies.vozo.ui.theme.VozoOrange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val emptyPeers: StateFlow<List<Peer>> = MutableStateFlow(emptyList())
private val idleState: StateFlow<TransportState> = MutableStateFlow(TransportState.Idle).asStateFlow()

@Composable
fun MeshScreen(
    modifier: Modifier = Modifier,
    transport: Transport? = null,
    registry: TransportRegistry? = null,
    onOpenPeerChat: (peerId: String, peerName: String) -> Unit = { _, _ -> },
) {
    val peerSource = transport?.peers ?: registry?.peers ?: emptyPeers
    val stateSource = transport?.state ?: idleState
    val peers by peerSource.collectAsState()
    val state by stateSource.collectAsState()

    val connected = peers.count { it.connected }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Mesh",
            style = MaterialTheme.typography.headlineMedium,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    MeshStat(label = "Peers", value = peers.size.toString())
                    MeshStat(label = "Connected", value = connected.toString())
                    MeshStat(label = "Status", value = stateLabel(state))
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stateDetail(state),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
            }
        }

        Text(
            text = "Nearby devices",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 4.dp),
        )

        if (peers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(VozoCyan.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.headlineMedium,
                            color = VozoCyan,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No peers found yet",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Open VOZO on a second phone nearby. It will appear here automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(peers, key = { it.id }) { peer ->
                    PeerRow(peer = peer, onClick = { onOpenPeerChat(peer.id, peer.displayName) })
                }
            }
        }
    }
}

private fun stateLabel(state: TransportState): String = when (state) {
    TransportState.Idle -> "Off"
    TransportState.Starting -> "..."
    is TransportState.Running -> "On"
    is TransportState.Failed -> "Error"
}

private fun stateDetail(state: TransportState): String = when (state) {
    TransportState.Idle -> "Mesh is not running"
    TransportState.Starting -> "Starting discovery..."
    is TransportState.Running -> "Scanning over Bluetooth and Wi-Fi Direct"
    is TransportState.Failed -> state.reason
}

@Composable
private fun MeshStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun PeerRow(
    peer: Peer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(if (peer.connected) VozoCyan else VozoOrange, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (peer.connected) "1" else "0",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(peer.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (peer.connected) "connected · direct" else "discovered · connecting",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}
