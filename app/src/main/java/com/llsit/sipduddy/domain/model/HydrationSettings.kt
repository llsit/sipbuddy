package com.llsit.sipduddy.domain.model

data class HydrationSettings(
    val goalMl: Int = DEFAULT_GOAL_ML,
    val reminder: ReminderSettings = ReminderSettings(),
    val ownerId: String? = null,
) {
    init {
        require(goalMl in MIN_GOAL_ML..MAX_GOAL_ML) { "ตั้งเป้าหมาย 250–10,000 มล." }
    }

    companion object {
        const val DEFAULT_GOAL_ML = 2000
        const val MIN_GOAL_ML = 250
        const val MAX_GOAL_ML = 10000
    }
}
