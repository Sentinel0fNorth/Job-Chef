package com.jobchef.feature.resume.di

import com.jobchef.feature.resume.data.ResumeVaultRepository
import com.jobchef.feature.resume.presentation.vault.ResumeImportProcessor
import com.jobchef.feature.resume.presentation.vault.ResumeVaultViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val resumeModule = module {
    single { ResumeVaultRepository(get(), get(), get(), get(), get()) }
    single { ResumeImportProcessor(androidContext()) }
    viewModel { ResumeVaultViewModel(repository = get(), importProcessor = get()) }
}
