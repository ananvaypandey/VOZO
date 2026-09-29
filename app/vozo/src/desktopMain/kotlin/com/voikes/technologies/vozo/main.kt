package com.voikes.technologies.vozo

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.voikes.technologies.vozo.data.ChatStore
import com.voikes.technologies.vozo.data.DatabaseDriverFactory
import com.voikes.technologies.vozo.db.VozoDatabase

fun main() = application {
    val windowState = rememberWindowState(
        width = 1050.dp,
        height = 720.dp,
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "VOZO",
        state = windowState,
    ) {
        val store = remember {
            ChatStore(VozoDatabase(DatabaseDriverFactory().createDriver()))
        }
        App(store = store)
    }
}