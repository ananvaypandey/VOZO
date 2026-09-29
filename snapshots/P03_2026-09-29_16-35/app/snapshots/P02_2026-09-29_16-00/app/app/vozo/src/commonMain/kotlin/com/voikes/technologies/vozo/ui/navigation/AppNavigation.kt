package com.voikes.technologies.vozo.ui.navigation

enum class AppTab(
    val label: String,
    val icon: String,
) {
    Home(label = "Home", icon = "home"),
    Chats(label = "Chats", icon = "chats"),
    Mesh(label = "Mesh", icon = "mesh"),
    Account(label = "Account", icon = "person"),
}

data class AppRoute(
    val tab: AppTab,
    val screen: Screen,
)

enum class Screen {
    Home,
    ChatThread,
    Mesh,
    Account,
}

sealed interface NavEvent {
    data class SelectTab(val tab: AppTab) : NavEvent
    data class OpenChat(val peerId: String) : NavEvent
    object Back : NavEvent
}

enum class ThemeChoice(
    val label: String,
) {
    SYSTEM(label = "System"),
    LIGHT(label = "Light"),
    DARK(label = "Dark"),
}