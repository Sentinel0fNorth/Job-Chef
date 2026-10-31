package com.jobchef.feature.onboarding.di

import com.jobchef.feature.onboarding.presentation.DeviceEnrollmentViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val onboardingModule = module {
    viewModel {
        DeviceEnrollmentViewModel(api = get(), credentials = get(), syncScheduler = get())
    }
}
