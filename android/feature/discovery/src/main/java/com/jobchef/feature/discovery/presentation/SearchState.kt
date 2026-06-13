package com.jobchef.feature.discovery.presentation

data class SearchState(
    val targetRole: String = "Software Development Engineer",
    val location: String = "Jaipur · Bangalore · Remote",
    val keywords: String = "Java, Spring Boot, microservices",
    val activeResumeName: String = "SDE Backend",
    val suggestedFilters: List<String> = listOf("Hybrid", "₹40k+/mo", "0-3 yrs", "Product co.", "Series B+"),
    val selectedFilters: Set<String> = emptySet(),
    val matchedRolesCount: Int = 218,
    val isLoading: Boolean = false
)
