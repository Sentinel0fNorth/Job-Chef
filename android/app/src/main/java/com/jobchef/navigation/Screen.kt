package com.jobchef.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Vault : Screen

    @Serializable
    data object Discovery : Screen

    @Serializable
    data object SwipeDeck : Screen

    @Serializable
    data object Saved : Screen

    @Serializable
    data object Export : Screen
}
