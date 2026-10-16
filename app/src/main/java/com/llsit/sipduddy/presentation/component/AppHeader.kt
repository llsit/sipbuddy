package com.llsit.sipduddy.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.llsit.sipduddy.ui.theme.Ink
import com.llsit.sipduddy.ui.theme.PaleBlue

@Composable
internal fun AppHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("SipBuddy", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text("เพื่อนตัวจิ๋ว ชวนจิบน้ำ", color = Ink.copy(alpha = .65f), fontSize = 13.sp)
        }
        Surface(shape = RoundedCornerShape(50), color = PaleBlue) {
            Text("♡ ทุกหยดมีค่า", Modifier.padding(10.dp), fontSize = 12.sp, color = Ink)
        }
    }
}
