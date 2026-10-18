package com.llsit.sipduddy.presentation.model

import com.llsit.sipduddy.R

internal enum class AppDestination(val title: String, val iconRes: Int) {
    TODAY("วันนี้", R.drawable.ic_water),
    HISTORY("ประวัติ", R.drawable.ic_history),
    SETTINGS("ตั้งค่า", R.drawable.ic_settings),
}
