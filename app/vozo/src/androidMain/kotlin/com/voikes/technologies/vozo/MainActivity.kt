package com.voikes.technologies.vozo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.voikes.technologies.vozo.data.ChatStore
import com.voikes.technologies.vozo.data.DatabaseDriverFactory
import com.voikes.technologies.vozo.db.VozoDatabase
import com.voikes.technologies.vozo.mesh.MeshController
import com.voikes.technologies.vozo.mesh.NearbyTransport
import com.voikes.technologies.vozo.mesh.TcpHostTransport
import com.voikes.technologies.vozo.mesh.TransportRegistry

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            nearby?.takeIf { it.hasPermissions() }?.start()
        }

    private var mesh: MeshController? = null
    private var nearby: NearbyTransport? = null
    private var host: TcpHostTransport? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val store = ChatStore(VozoDatabase(DatabaseDriverFactory(applicationContext).createDriver()))

        val nearbyTransport = NearbyTransport(
            context = applicationContext,
            localPeerId = store.selfId,
            localDisplayName = store.selfName,
            registry = TransportRegistry(),
        )
        val hostTransport = TcpHostTransport(
            context = applicationContext,
            localPeerId = store.selfId,
            localDisplayName = store.selfName,
        )
        val controller = MeshController(listOf(nearbyTransport, hostTransport))
        controller.onFrame = { raw -> store.onFrame(raw) }
        store.attachTransport(controller)

        nearby = nearbyTransport
        host = hostTransport
        mesh = controller

        setContent {
            App(store = store, mesh = controller)
        }

        hostTransport.start()

        if (nearbyTransport.hasPermissions()) {
            nearbyTransport.start()
        } else {
            permissionLauncher.launch(NearbyTransport.requiredPermissions().toTypedArray())
        }
    }

    override fun onStart() {
        super.onStart()
        nearby?.takeIf { it.hasPermissions() }?.start()
    }

    override fun onStop() {
        nearby?.stop()
        super.onStop()
    }
}
