package com.jobchef.feature.discovery.presentation

import com.jobchef.core.domain.model.Job

/**
 * UI state for the Swipe Deck screen.
 *
 * [jobs] now holds the domain [Job] list sourced directly from the local Room cache
 * (via [SwipeDeckViewModel] → [com.jobchef.core.domain.repository.JobRepository]).
 * The UI maps [Job] fields to card display elements inside [DeckCardItem].
 */
data class SwipeDeckState(
    val jobs: List<Job> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)
