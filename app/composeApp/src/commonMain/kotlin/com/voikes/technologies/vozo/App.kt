package com.voikes.technologies.vozo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.voikes.technologies.vozo.ui.navigation.AppTab
import com.voikes.technologies.vozo.ui.navigation.ThemeChoice
import com.voikes.technologies.vozo.ui.screens.AccountScreen
import com.voikes.technologies.vozo.ui.screens.ChatThreadScreen
import com.voikes.technologies.vozo.ui.screens.HomeScreen
import com.voikes.technologies.vozo.ui.screens.MeshScreen
import com.voikes.technologies.vozo.ui.theme.VozoTheme

@Composable
fun App() {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Home) }
    var activePeerId by rememberSaveable { mutableStateOf<String?>(null) }
    var themeChoice by rememberSaveable { mutableStateOf(ThemeChoice.SYSTEM) }

    val darkTheme = when (themeChoice) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }

    VozoTheme(darkTheme = darkTheme) {
        val currentScreen = if (activePeerId != null) "chat" else when (selectedTab) {
            AppTab.Home -> "home"
            AppTab.Mesh -> "mesh"
            AppTab.Account -> "account"
        }

        Scaffold(
            bottomBar = {
                if (activePeerId == null) {
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
            when (currentScreen) {
                "chat" -> ChatThreadScreen(
                    modifier = contentModifier,
                    peerId = activePeerId ?: "",
                    onBack = { activePeerId = null },
                )

                "home" -> HomeScreen(
                    modifier = contentModifier,
                    onOpenChat = { peerId -> activePeerId = peerId },
                )

                "mesh" -> MeshScreen(modifier = contentModifier)

                "account" -> AccountScreen(
                    modifier = contentModifier,
                    currentTheme = themeChoice,
                    onThemeChange = { themeChoice = it },
                )
            }
        }
    }
}

private fun AppTab.icon(): ImageVector = when (this) {
    AppTab.Home -> Icons.Filled.Home
    AppTab.Mesh -> Icons.Filled.Share
    AppTab.Account -> Icons.Filled.Person
}