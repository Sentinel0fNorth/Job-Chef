package com.jobchef.feature.tailor.di

import com.jobchef.feature.tailor.presentation.export.DocumentExportService
import com.jobchef.feature.tailor.presentation.export.ExportViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val tailorModule = module {
    single { DocumentExportService(androidContext(), get()) }
    viewModel { parameters ->
        ExportViewModel(
            jobId = parameters.get(),
            context = androidContext(),
            applications = get(),
            jobs = get(),
            resumes = get(),
            api = get(),
            documents = get()
        )
    }
}
