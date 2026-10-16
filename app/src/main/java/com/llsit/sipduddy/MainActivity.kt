package com.llsit.sipduddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.llsit.sipduddy.presentation.SipBuddyApp
import com.llsit.sipduddy.ui.theme.SipDuddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SipDuddyTheme { SipBuddyApp() } }
    }
}
