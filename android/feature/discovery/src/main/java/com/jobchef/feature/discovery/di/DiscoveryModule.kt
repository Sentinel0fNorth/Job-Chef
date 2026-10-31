package com.jobchef.feature.discovery.di

import com.jobchef.feature.discovery.presentation.SearchViewModel
import com.jobchef.feature.discovery.presentation.SwipeDeckViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val discoveryModule = module {
    viewModel { SearchViewModel(repository = get()) }
    viewModel { SwipeDeckViewModel(repository = get()) }
}
