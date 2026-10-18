package com.llsit.sipduddy.data.sync
import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import org.koin.core.context.GlobalContext
class HydrationSyncWorkerFactory : WorkerFactory() {
    override fun createWorker(appContext: Context,workerClassName: String,workerParameters: WorkerParameters): ListenableWorker? {
        if (workerClassName != HydrationSyncWorker::class.java.name) return null
        val koin=GlobalContext.get()
        return HydrationSyncWorker(appContext,workerParameters,koin.get(),koin.get(),koin.get(),koin.get(),koin.get())
    }
}
