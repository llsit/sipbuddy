package com.llsit.sipduddy.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llsit.sipduddy.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.ZoneId

data class HydrationState(
    val loading: Boolean = true,
    val entries: List<DrinkEntry> = emptyList(),
    val preferences: Preferences = Preferences(),
    val days: List<DailyTotal> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val zone: ZoneId = ZoneId.systemDefault(),
    val busy: Boolean = false,
    val message: String? = null
) { val totalMl get() = days.lastOrNull()?.totalMl ?: 0 }

class HydrationViewModel(
    repository: HydrationRepository,
    private val addDrink: AddDrink,
    private val deleteDrink: DeleteDrink,
    private val savePreferences: SavePreferences
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val clock = flow { while (true) { emit(LocalDate.now() to ZoneId.systemDefault()); delay(15_000) } }
    val state = combine(repository.entries, repository.preferences, clock, busy, message) { entries, prefs, date, saving, error ->
        HydrationState(false, entries, prefs, weeklyTotals(entries, date.first, date.second), date.first, date.second, saving, error)
    }.catch { emit(HydrationState(loading = false, message = "อ่านข้อมูลไม่สำเร็จ กรุณาปิดแล้วเปิดแอปอีกครั้ง")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HydrationState())

    fun add(drink: Drink, amountMl: Int, onSuccess: () -> Unit) = launchAction { addDrink(drink, amountMl); onSuccess() }
    fun delete(id: Long) = launchAction { deleteDrink(id) }
    fun save(preferences: Preferences) = launchAction { savePreferences(preferences); message.value = "บันทึกการตั้งค่าแล้ว" }
    fun clearMessage() { message.value = null }
    private fun launchAction(action: suspend () -> Unit) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            try { action() } catch (e: CancellationException) { throw e }
            catch (e: Exception) { message.value = e.message ?: "บันทึกไม่สำเร็จ ลองอีกครั้ง" }
            finally { busy.value = false }
        }
    }
}
