package com.llsit.sipduddy.domain.time

import com.llsit.sipduddy.domain.model.DateContext
import kotlinx.coroutines.flow.Flow

interface DateProvider { val dates: Flow<DateContext> }
