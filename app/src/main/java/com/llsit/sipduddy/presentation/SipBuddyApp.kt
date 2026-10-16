package com.llsit.sipduddy.presentation

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.llsit.sipduddy.domain.*
import com.llsit.sipduddy.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SipBuddyApp(viewModel: HydrationViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<DrinkEntry?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() } }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = Cream, tonalElevation = 0.dp) {
                listOf("วันนี้" to "◉", "ประวัติ" to "▥", "ตั้งค่า" to "⚙").forEachIndexed { index, item ->
                    NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Text(item.second, fontSize = 22.sp) }, label = { Text(item.first) })
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("SipBuddy", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold); Text("เพื่อนตัวจิ๋ว ชวนจิบน้ำ", color = Ink.copy(alpha = .65f), fontSize = 13.sp) }
                Surface(shape = RoundedCornerShape(50), color = PaleBlue) { Text("♡  ทุกหยดมีค่า", Modifier.padding(12.dp), fontSize = 12.sp, color = Ink) }
            }
            if (state.loading) { CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally)) }
            else when (tab) {
                0 -> TodayScreen(state, onAdd = { adding = true }, onDelete = { deleting = it })
                1 -> HistoryScreen(state, onDelete = { deleting = it })
                else -> SettingsScreen(state, viewModel::save)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    if (adding) ModalBottomSheet(onDismissRequest = { if (!state.busy) adding = false }, containerColor = Cream) {
        AddDrinkSheet(state.busy) { drink, amount -> viewModel.add(drink, amount) { adding = false } }
    }
    deleting?.let { entry ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("ลบรายการนี้?") },
            text = { Text("${entry.drink.title} ${entry.amountMl} มล. จะถูกลบออกจากประวัติ") },
            confirmButton = { TextButton(onClick = { viewModel.delete(entry.id); deleting = null }) { Text("ลบรายการ") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("เก็บไว้") } })
    }
}

@Composable
private fun TodayScreen(state: HydrationState, onAdd: () -> Unit, onDelete: (DrinkEntry) -> Unit) {
    val fraction = progress(state.totalMl, state.preferences.goalMl)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ค่อย ๆ จิบ ก็ถึงเป้าได้", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        Text(state.today.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.forLanguageTag("th"))), fontSize = 13.sp)
        WaterMascot(fraction, Modifier.size(230.dp))
        Text(if (fraction >= 1f) "เย้! วันนี้ถึงเป้าแล้ว ♡" else "เติมความสดชื่นให้บัดดี้กัน", fontWeight = FontWeight.Medium)
    }
    SoftCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column { Text("ดื่มไปแล้ววันนี้", fontSize = 13.sp); Text("${state.totalMl}", fontSize = 38.sp, fontWeight = FontWeight.Bold) }
            Text("/ ${state.preferences.goalMl} มล.", Modifier.padding(bottom = 8.dp), color = Ink.copy(alpha = .65f))
        }
        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(10.dp), color = Blue, trackColor = Color.White)
        Text(if (fraction >= 1f) "สำเร็จแล้ว ดูแลตัวเองดีมากเลย" else "อีก ${(state.preferences.goalMl - state.totalMl).coerceAtLeast(0)} มล. ถึงเป้าหมาย", fontSize = 13.sp)
    }
    Button(onClick = onAdd, enabled = !state.busy, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(20.dp)) {
        Text("+  บันทึกเครื่องดื่ม", fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
    val todayEntries = state.entries.filter { Instant.ofEpochMilli(it.timestamp).atZone(state.zone).toLocalDate() == state.today }
    Text("จิบเล็ก ๆ ของวันนี้", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    if (todayEntries.isEmpty()) EmptyState("เริ่มต้นด้วยน้ำสักแก้ว", "แตะบันทึกเครื่องดื่ม เพื่อเติมหยดแรกของวัน")
    todayEntries.forEach { EntryRow(it, state, onDelete) }
}

@Composable
fun WaterMascot(fraction: Float, modifier: Modifier = Modifier) {
    val fill by animateFloatAsState(fraction, tween(1000), label = "water level")
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(0f, 6.283f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "phase")
    Canvas(modifier.semantics { contentDescription = "บัดดี้หยดน้ำ เติมแล้ว ${(fraction * 100).toInt()} เปอร์เซ็นต์" }) {
        val w = size.width; val h = size.height
        drawOval(Color(0xFFE9E3D7), Offset(w * .22f, h * .9f), Size(w * .56f, h * .06f))
        val drop = Path().apply {
            moveTo(w * .5f, h * .06f)
            cubicTo(w * .39f, h * .27f, w * .16f, h * .4f, w * .16f, h * .62f)
            cubicTo(w * .16f, h * 1.02f, w * .84f, h * 1.02f, w * .84f, h * .62f)
            cubicTo(w * .84f, h * .4f, w * .61f, h * .27f, w * .5f, h * .06f)
            close()
        }
        drawPath(drop, Color(0xFFDFF3FA))
        clipPath(drop) {
            val water = Path().apply {
                val level = h * (.92f - .86f * fill)
                moveTo(0f, h)
                lineTo(0f, level)
                for (x in 0..100) { val px = w * x / 100; lineTo(px, level + sin(x / 100f * 6.283f + phase) * h * .016f) }
                lineTo(w, h); close()
            }
            drawPath(water, Color(0xFF78C9E8))
            drawCircle(Color.White.copy(alpha = .5f), w * .035f, Offset(w * .3f, h * .75f))
            drawCircle(Color.White.copy(alpha = .4f), w * .02f, Offset(w * .7f, h * .83f))
        }
        drawPath(drop, Color(0xFF4E9EBB), style = Stroke(2.dp.toPx()))
        drawOval(Color.White.copy(alpha = .85f), Offset(w * .28f, h * .38f), Size(w * .065f, h * .14f))
        drawCircle(Ink, w * .022f, Offset(w * .41f, h * .62f))
        drawCircle(Ink, w * .022f, Offset(w * .59f, h * .62f))
        drawOval(Color(0xFFF3B7AE), Offset(w * .31f, h * .65f), Size(w * .09f, h * .04f))
        drawOval(Color(0xFFF3B7AE), Offset(w * .60f, h * .65f), Size(w * .09f, h * .04f))
        drawArc(Ink, 0f, 180f, false, Offset(w * .455f, h * .635f), Size(w * .09f, h * .065f), style = Stroke(2.5.dp.toPx()))
    }
}

@Composable
private fun HistoryScreen(state: HydrationState, onDelete: (DrinkEntry) -> Unit) {
    Text("ทีละจิบ ทีละวัน", fontSize = 25.sp, fontWeight = FontWeight.Bold)
    Text("ย้อนหลัง 7 วัน รวมวันนี้ • ปริมาณเครื่องดื่มจริง", fontSize = 13.sp)
    SoftCard {
        Text("สัปดาห์นี้ ${state.days.sumOf { it.totalMl }} มล.", fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            state.days.forEach { day ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${day.totalMl}", fontSize = 10.sp, maxLines = 1)
                    Box(Modifier.width(18.dp).height((progress(day.totalMl, maxOf(state.preferences.goalMl, state.days.maxOfOrNull { it.totalMl } ?: 0)) * 90).coerceAtLeast(4f).dp).background(if (day.date == state.today) Blue else Color(0xFF9CD5E8), RoundedCornerShape(8.dp)))
                    Text(day.date.format(DateTimeFormatter.ofPattern("EE", Locale.forLanguageTag("th"))), fontSize = 11.sp)
                }
            }
        }
    }
    state.days.asReversed().forEach { day ->
        Text("${day.date.format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("th")))}  ·  ${day.totalMl} มล.", fontWeight = FontWeight.Bold)
        val entries = state.entries.filter { Instant.ofEpochMilli(it.timestamp).atZone(state.zone).toLocalDate() == day.date }
        if (entries.isEmpty()) Text("ยังไม่มีรายการ", fontSize = 13.sp, color = Ink.copy(alpha = .5f))
        entries.forEach { EntryRow(it, state, onDelete) }
    }
}

@Composable
private fun EntryRow(entry: DrinkEntry, state: HydrationState, onDelete: (DrinkEntry) -> Unit) {
    Surface(color = Color.White.copy(alpha = .8f), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = PaleBlue, shape = RoundedCornerShape(14.dp)) { Text(entry.drink.symbol, Modifier.padding(12.dp), fontSize = 22.sp) }
            Column(Modifier.weight(1f)) { Text(entry.drink.title, fontWeight = FontWeight.SemiBold); Text(Instant.ofEpochMilli(entry.timestamp).atZone(state.zone).format(DateTimeFormatter.ofPattern("HH:mm")), fontSize = 12.sp) }
            Text("${entry.amountMl} มล.", fontWeight = FontWeight.Bold)
            TextButton(onClick = { onDelete(entry) }, enabled = !state.busy, contentPadding = PaddingValues(4.dp)) { Text("ลบ") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddDrinkSheet(busy: Boolean, onSave: (Drink, Int) -> Unit) {
    var drinkName by rememberSaveable { mutableStateOf(Drink.WATER.name) }
    var amount by rememberSaveable { mutableStateOf("250") }
    val ml = amount.toIntOrNull()
    Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("วันนี้จิบอะไรดี?", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("ทุกเครื่องดื่มนับตามปริมาณที่บันทึก", fontSize = 13.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Drink.entries.forEach { drink ->
                FilterChip(selected = drinkName == drink.name, onClick = { drinkName = drink.name }, label = { Text("${drink.symbol} ${drink.title}") })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(150, 250, 500).forEach { preset ->
            FilterChip(selected = amount == preset.toString(), onClick = { amount = preset.toString() }, label = { Text("$preset มล.") })
        } }
        OutlinedTextField(value = amount, onValueChange = { amount = it.filter(Char::isDigit).take(5) }, modifier = Modifier.fillMaxWidth(), label = { Text("ปริมาณ (มล.)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = ml == null || ml !in 1..5000, supportingText = { Text("ระบุ 1–5,000 มล.") })
        Button(onClick = { ml?.let { onSave(Drink.valueOf(drinkName), it) } }, enabled = !busy && ml != null && ml in 1..5000, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(if (busy) "กำลังบันทึก…" else "เติมให้บัดดี้ ♡") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsScreen(state: HydrationState, onSave: (Preferences) -> Unit) {
    val prefs = state.preferences
    var goal by rememberSaveable(prefs.goalMl) { mutableStateOf(prefs.goalMl.toString()) }
    var hour by rememberSaveable(prefs.hour) { mutableIntStateOf(prefs.hour) }
    var minute by rememberSaveable(prefs.minute) { mutableIntStateOf(prefs.minute) }
    var enabled by rememberSaveable(prefs.reminderEnabled) { mutableStateOf(prefs.reminderEnabled) }
    val context = LocalContext.current
    fun notificationsAllowed() = (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) && NotificationManagerCompat.from(context).areNotificationsEnabled()
    var permissionGranted by remember { mutableStateOf(notificationsAllowed()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permissionGranted = notificationsAllowed() }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> permissionGranted = granted && notificationsAllowed() }
    Text("ดูแลตัวเองในแบบคุณ", fontSize = 25.sp, fontWeight = FontWeight.Bold)
    SoftCard {
        Text("เป้าหมายรายวัน", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        OutlinedTextField(value = goal, onValueChange = { goal = it.filter(Char::isDigit).take(5) }, modifier = Modifier.fillMaxWidth(), label = { Text("มิลลิลิตรต่อวัน") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
            isError = goal.toIntOrNull() !in 250..10000, supportingText = { Text("ปรับได้ตั้งแต่ 250–10,000 มล.") })
        Text("เลือกเป้าที่เหมาะกับตัวคุณ", fontSize = 12.sp)
    }
    SoftCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("เตือนให้จิบน้ำ", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Switch(checked = enabled, onCheckedChange = { enabled = it })
        }
        OutlinedButton(onClick = { TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, true).show() }, modifier = Modifier.fillMaxWidth()) {
            Text("ทุกวัน เวลา %02d:%02d".format(hour, minute))
        }
        Text("แจ้งเตือนวันละครั้ง เวลาจริงอาจคลาดเคลื่อนตามการประหยัดแบตเตอรี่", fontSize = 12.sp)
        if (!permissionGranted) {
            Text("ยังไม่ได้เปิดสิทธิ์แจ้งเตือน", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            TextButton(onClick = {
                if (Build.VERSION.SDK_INT >= 33) request.launch(Manifest.permission.POST_NOTIFICATIONS)
                else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }) { Text("ขอสิทธิ์แจ้งเตือน") }
        }
        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("เปิดการตั้งค่าแจ้งเตือนของระบบ") }
    }
    Button(onClick = { goal.toIntOrNull()?.let { onSave(Preferences(it, enabled, hour, minute)) } }, enabled = !state.busy && goal.toIntOrNull() in 250..10000,
        modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(if (state.busy) "กำลังบันทึก…" else "บันทึกการตั้งค่า") }
    EmptyState("พื้นที่เล็ก ๆ ของคุณ", "ข้อมูลอยู่ในเครื่อง ใช้ได้ออฟไลน์ ไม่ต้องเข้าสู่ระบบ")
}

@Composable
private fun SoftCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = PaleBlue.copy(alpha = .65f), shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.Medium)
        Text(subtitle, fontSize = 12.sp, color = Ink.copy(alpha = .65f))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF9EF, widthDp = 393, heightDp = 1000)
@Composable
private fun TodayPreview() {
    SipDuddyTheme {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("SipBuddy", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            TodayScreen(HydrationState(loading = false, days = listOf(DailyTotal(java.time.LocalDate.now(), 750))), {}, {})
        }
    }
}
