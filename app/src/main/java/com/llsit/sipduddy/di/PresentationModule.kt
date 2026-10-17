package com.llsit.sipduddy.di

import com.llsit.sipduddy.presentation.HydrationViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module { viewModel { HydrationViewModel(get(), get(), get(), get(), get(), get()) } }
