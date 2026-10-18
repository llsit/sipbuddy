package com.llsit.sipduddy.data.auth
import android.content.Context
import com.llsit.sipduddy.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.ktor.client.engine.okhttp.OkHttp
import io.github.jan.supabase.logging.LogLevel

class SupabaseClientProvider(context: Context) {
    val client = if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_PUBLISHABLE_KEY.isBlank()) null
    else createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) {
        httpEngine = OkHttp.create()
        defaultLogLevel = LogLevel.NONE
        install(Auth) {
            flowType = FlowType.PKCE
            scheme = BuildConfig.AUTH_SCHEME
            host = "auth-callback"
            sessionManager = EncryptedSessionManager(context)
        }
    }
    val recoveryRequired = kotlinx.coroutines.flow.MutableStateFlow(false)
    val callbackError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val redirectUrl get() = "${BuildConfig.AUTH_SCHEME}://auth-callback"
}
