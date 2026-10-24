package com.llsit.sipduddy.di

import com.llsit.sipduddy.presentation.AccountScopeViewModel
import com.llsit.sipduddy.presentation.HydrationViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { AccountScopeViewModel() }
    viewModel { parameters ->
        HydrationViewModel(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            parameters.getOrNull<String>() ?: get<com.llsit.sipduddy.domain.repository.AccountRepository>().owner.value,
        )
    }
}
