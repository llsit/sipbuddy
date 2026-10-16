package com.llsit.sipduddy.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.llsit.sipduddy.domain.model.DrinkType
import com.llsit.sipduddy.domain.usecase.AddDrinkUseCase.Companion.MAX_AMOUNT_ML
import com.llsit.sipduddy.domain.usecase.AddDrinkUseCase.Companion.MIN_AMOUNT_ML
import com.llsit.sipduddy.presentation.mapper.symbol
import com.llsit.sipduddy.presentation.mapper.title

@Composable
internal fun AddDrinkSheet(busy: Boolean, onSave: (DrinkType, Int) -> Unit) {
    var drinkName by rememberSaveable { mutableStateOf(DrinkType.WATER.name) }
    var amount by rememberSaveable { mutableStateOf("250") }
    val ml = amount.toIntOrNull()
    Column(
        Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("วันนี้จิบอะไรดี?", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("ทุกเครื่องดื่มนับตามปริมาณที่บันทึก", fontSize = 13.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DrinkType.entries.forEach { drink ->
                FilterChip(
                    enabled = !busy,
                    selected = drinkName == drink.name,
                    onClick = { drinkName = drink.name },
                    label = { Text("${drink.symbol} ${drink.title}") })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(150, 250, 500).forEach { preset ->
                FilterChip(
                    enabled = !busy,
                    selected = amount == preset.toString(),
                    onClick = { amount = preset.toString() },
                    label = { Text("$preset มล.") })
            }
        }
        OutlinedTextField(
            enabled = !busy,
            value = amount,
            onValueChange = { amount = it.filter(Char::isDigit).take(5) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("ปริมาณ (มล.)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = ml == null || ml !in MIN_AMOUNT_ML..MAX_AMOUNT_ML,
            supportingText = { Text("ระบุ 1–5,000 มล.") })
        Button(
            onClick = { ml?.let { onSave(DrinkType.valueOf(drinkName), it) } },
            enabled = !busy && ml != null && ml in MIN_AMOUNT_ML..MAX_AMOUNT_ML,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) { Text(if (busy) "กำลังบันทึก…" else "เติมให้บัดดี้ ♡") }
        Spacer(Modifier.height(16.dp))
    }
}
