package com.jobchef.feature.resume.di

import com.jobchef.feature.resume.presentation.vault.ResumeVaultViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val resumeModule = module {
    viewModel { ResumeVaultViewModel() }
}
