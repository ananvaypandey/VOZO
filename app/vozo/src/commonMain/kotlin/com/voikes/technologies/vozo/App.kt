package com.voikes.technologies.vozo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.voikes.technologies.vozo.data.ChatStore
import com.voikes.technologies.vozo.mesh.MeshController
import com.voikes.technologies.vozo.ui.navigation.AppTab
import com.voikes.technologies.vozo.ui.navigation.ThemeChoice
import com.voikes.technologies.vozo.ui.screens.AccountScreen
import com.voikes.technologies.vozo.ui.screens.ChatListScreen
import com.voikes.technologies.vozo.ui.screens.ChatThreadScreen
import com.voikes.technologies.vozo.ui.screens.HomeScreen
import com.voikes.technologies.vozo.ui.screens.MeshScreen
import com.voikes.technologies.vozo.ui.theme.VozoTheme

@Composable
fun App(
    store: ChatStore,
    mesh: MeshController? = null,
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Home) }
    var activeConversationId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeConversationTitle by rememberSaveable { mutableStateOf("") }
    var themeChoice by rememberSaveable { mutableStateOf(ThemeChoice.SYSTEM) }
    var showNewChat by rememberSaveable { mutableStateOf(false) }

    val darkTheme = when (themeChoice) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }

    VozoTheme(darkTheme = darkTheme) {
        Scaffold(
            bottomBar = {
                if (activeConversationId == null) {
                    NavigationBar {
                        AppTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(imageVector = tab.icon(), contentDescription = null) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            val contentModifier = Modifier.padding(innerPadding)
            val conversationId = activeConversationId
            when {
                conversationId != null -> ChatThreadScreen(
                    modifier = contentModifier,
                    store = store,
                    conversationId = conversationId,
                    conversationTitle = activeConversationTitle,
                    onBack = {
                        activeConversationId = null
                        activeConversationTitle = ""
                    },
                )

                selectedTab == AppTab.Home -> HomeScreen(
                    modifier = contentModifier,
                    onOpenChat = { showNewChat = true },
                )

                selectedTab == AppTab.Chats -> ChatListScreen(
                    modifier = contentModifier,
                    store = store,
                    onOpenConversation = { id, title ->
                        activeConversationId = id
                        activeConversationTitle = title
                    },
                    onNewChat = { showNewChat = true },
                )

                selectedTab == AppTab.Mesh -> MeshScreen(
                    modifier = contentModifier,
                    transport = mesh,
                    onOpenPeerChat = { peerId, peerName ->
                        val id = store.startPeerChat(peerId, peerName)
                        activeConversationId = id
                        activeConversationTitle = peerName
                    },
                )

                selectedTab == AppTab.Account -> AccountScreen(
                    modifier = contentModifier,
                    currentTheme = themeChoice,
                    onThemeChange = { themeChoice = it },
                )
            }
        }

        if (showNewChat) {
            NewChatDialog(
                onDismiss = { showNewChat = false },
                onCreate = { name ->
                    val id = store.createConversation(name)
                    showNewChat = false
                    activeConversationId = id
                    activeConversationTitle = name
                },
            )
        }
    }
}

@Composable
private fun NewChatDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New chat") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("Name this chat") },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun AppTab.icon(): ImageVector = when (this) {
    AppTab.Home -> Icons.Filled.Home
    AppTab.Chats -> Icons.AutoMirrored.Filled.Send
    AppTab.Mesh -> Icons.Filled.Share
    AppTab.Account -> Icons.Filled.Person
}