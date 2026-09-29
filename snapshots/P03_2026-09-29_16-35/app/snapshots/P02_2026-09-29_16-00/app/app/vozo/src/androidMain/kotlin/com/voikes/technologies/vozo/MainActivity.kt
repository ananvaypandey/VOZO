package com.voikes.technologies.vozo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.voikes.technologies.vozo.data.ChatStore
import com.voikes.technologies.vozo.data.DatabaseDriverFactory
import com.voikes.technologies.vozo.db.VozoDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val store = remember {
                ChatStore(VozoDatabase(DatabaseDriverFactory(applicationContext).createDriver()))
            }
            App(store = store)
        }
    }
}