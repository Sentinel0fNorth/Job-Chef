package com.jobchef.feature.saved.di

import com.jobchef.feature.saved.presentation.SavedOpportunitiesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val savedModule = module {
    viewModel { SavedOpportunitiesViewModel(repository = get(), applications = get()) }
}
