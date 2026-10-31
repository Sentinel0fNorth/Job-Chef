package com.jobchef.feature.resume.di

import com.jobchef.core.designsystem.util.UiNotificationManager
import com.jobchef.feature.resume.presentation.vault.ResumeVaultViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val resumeModule = module {
    single { UiNotificationManager() }
    viewModel { ResumeVaultViewModel(uiNotificationManager = get()) }
}
