package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.HydrationSettings
import com.llsit.sipduddy.domain.model.SaveHydrationSettingsResult
import com.llsit.sipduddy.domain.reminder.ReminderScheduler
import com.llsit.sipduddy.domain.repository.HydrationSettingsRepository
import kotlinx.coroutines.CancellationException

class SaveHydrationSettingsUseCase(
    private val repository: HydrationSettingsRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke(settings: HydrationSettings): SaveHydrationSettingsResult {
        repository.save(settings)
        // DataStore remains the source of truth; a failed alarm is reported as partial success.
        return try {
            scheduler.schedule(settings.reminder)
            SaveHydrationSettingsResult.SAVED
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            SaveHydrationSettingsResult.SAVED_WITH_REMINDER_ERROR
        }
    }
}
