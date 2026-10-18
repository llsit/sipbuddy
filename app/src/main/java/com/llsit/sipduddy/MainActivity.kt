package com.llsit.sipduddy

import android.os.Bundle
import android.content.Intent
import com.llsit.sipduddy.data.auth.AuthDeepLinkHandler
import org.koin.android.ext.android.inject
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.llsit.sipduddy.presentation.SipBuddyRoot
import com.llsit.sipduddy.ui.theme.SipDuddyTheme

class MainActivity : ComponentActivity() {
    private val authDeepLinkHandler: AuthDeepLinkHandler by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authDeepLinkHandler.handle(intent)
        val systemBars = SystemBarStyle.light(
            android.graphics.Color.TRANSPARENT,
            android.graphics.Color.TRANSPARENT
        )
        enableEdgeToEdge(statusBarStyle = systemBars, navigationBarStyle = systemBars)
        setContent { SipDuddyTheme { SipBuddyRoot() } }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        authDeepLinkHandler.handle(intent)
    }
}
