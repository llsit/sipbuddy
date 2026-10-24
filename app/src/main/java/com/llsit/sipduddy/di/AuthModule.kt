package com.llsit.sipduddy.di
import com.llsit.sipduddy.data.auth.AndroidKeystoreSessionCipher
import com.llsit.sipduddy.data.auth.AuthDeepLinkHandler
import com.llsit.sipduddy.data.auth.EncryptedDataStoreSessionManager
import com.llsit.sipduddy.data.auth.SessionCipher
import com.llsit.sipduddy.data.auth.SupabaseClientProvider
import com.llsit.sipduddy.data.local.preferences.authSessionDataStore
import com.llsit.sipduddy.data.repository.SupabaseAuthRepository
import com.llsit.sipduddy.domain.repository.AuthRepository
import com.llsit.sipduddy.domain.usecase.*
import com.llsit.sipduddy.presentation.AuthViewModel
import com.llsit.sipduddy.presentation.formatter.AuthErrorFormatter
import io.github.jan.supabase.auth.SessionManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    single<SessionCipher> { AndroidKeystoreSessionCipher(androidContext()) }
    single<SessionManager> { EncryptedDataStoreSessionManager(androidContext().authSessionDataStore, get()) }
    single { SupabaseClientProvider(get()) }
    single { AuthDeepLinkHandler(get()) }
    single<AuthRepository> { SupabaseAuthRepository(get()) }
    factory { ValidateAuthInputUseCase() }
    factory { ObserveAuthSessionUseCase(get()) }
    factory { SignInUseCase(get(), get()) }
    factory { SignUpUseCase(get(), get()) }
    factory { SendPasswordResetUseCase(get(), get()) }
    factory { UpdatePasswordUseCase(get(), get()) }
    factory { SignInWithGoogleUseCase(get()) }
    factory { SignOutUseCase(get()) }
    factory { AuthErrorFormatter() }
    viewModel { AuthViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
}
