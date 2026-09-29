package com.voikes.technologies.vozo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import com.voikes.technologies.vozo.data.ChatStore
import com.voikes.technologies.vozo.data.DatabaseDriverFactory
import com.voikes.technologies.vozo.db.VozoDatabase
import com.voikes.technologies.vozo.mesh.NearbyTransport
import com.voikes.technologies.vozo.mesh.TransportRegistry

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            nearby?.takeIf { it.hasPermissions() }?.start()
        }

    private var nearby: NearbyTransport? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val database = VozoDatabase(DatabaseDriverFactory(applicationContext).createDriver())
        val store = ChatStore(database)
        val registry = TransportRegistry()
        val transport = NearbyTransport(
            context = applicationContext,
            localPeerId = store.selfId,
            localDisplayName = store.selfName,
            registry = registry,
        )
        transport.onFrame = { raw -> store.onFrame(raw) }
        store.attachTransport(transport)

        nearby = transport

        setContent {
            App(store = store, transport = transport, registry = registry)
        }

        if (transport.hasPermissions()) {
            transport.start()
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
