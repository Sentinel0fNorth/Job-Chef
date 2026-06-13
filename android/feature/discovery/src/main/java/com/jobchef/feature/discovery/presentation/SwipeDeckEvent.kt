package com.jobchef.feature.discovery.presentation

sealed interface SwipeDeckEvent {
    data object OnLike : SwipeDeckEvent
    data object OnReject : SwipeDeckEvent
    data object OnRewind : SwipeDeckEvent
}
