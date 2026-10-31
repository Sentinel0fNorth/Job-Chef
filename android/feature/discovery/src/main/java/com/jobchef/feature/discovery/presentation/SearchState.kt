package com.jobchef.feature.discovery.presentation

data class SearchState(
    val targetRole: String = "Software Development Engineer",
    val location: String = "Jaipur",
    val keywords: String = "",
    val activeResumeName: String? = null,
    val suggestedFilters: List<String> = listOf("Remote", "Hybrid", "On-site"),
    val selectedFilters: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
