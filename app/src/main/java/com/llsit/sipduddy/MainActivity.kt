package com.llsit.sipduddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.llsit.sipduddy.presentation.SipBuddyApp
import com.llsit.sipduddy.ui.theme.SipDuddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val systemBars = SystemBarStyle.light(
            android.graphics.Color.TRANSPARENT,
            android.graphics.Color.TRANSPARENT
        )
        enableEdgeToEdge(statusBarStyle = systemBars, navigationBarStyle = systemBars)
        setContent { SipDuddyTheme { SipBuddyApp() } }
    }
}
