package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.HydrationOverview
import com.llsit.sipduddy.domain.repository.DrinkRepository
import com.llsit.sipduddy.domain.repository.HydrationSettingsRepository
import com.llsit.sipduddy.domain.time.DateProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class ObserveHydrationOverviewUseCase(
    private val drinkRepository: DrinkRepository,
    private val settingsRepository: HydrationSettingsRepository,
    private val dateProvider: DateProvider,
    private val calculateWeeklyHydrationUseCase: CalculateWeeklyHydrationUseCase,
    private val dispatcher: CoroutineDispatcher,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<HydrationOverview> = dateProvider.dates.distinctUntilChanged().flatMapLatest { context ->
        val start =
            context.date.minusDays(6).atStartOfDay(context.zone).toInstant().toEpochMilli()
        val end = context.date.plusDays(1).atStartOfDay(context.zone).toInstant().toEpochMilli()
        val days = drinkRepository.observeEntries(start, end)
            .distinctUntilChanged()
            .map { calculateWeeklyHydrationUseCase(it, context) }
        combine(days, settingsRepository.settings.distinctUntilChanged()) { summaries, settings ->
            HydrationOverview(context, settings, summaries)
        }
    }.flowOn(dispatcher)
}
