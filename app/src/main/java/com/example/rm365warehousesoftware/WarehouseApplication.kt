package com.example.rm365warehousesoftware

import android.app.Application
import com.example.rm365warehousesoftware.data.local.AppDatabase
import com.example.rm365warehousesoftware.data.remote.RetrofitClient
import com.example.rm365warehousesoftware.data.repository.ScanRepository

/**
 * Application entry point. Initialises the network client and database, and
 * exposes a single [ScanRepository] instance for the rest of the app.
 *
 * For larger apps prefer a DI framework (Hilt/Koin); this manual wiring keeps
 * the boilerplate dependency-free.
 */
class WarehouseApplication : Application() {

    val repository: ScanRepository by lazy {
        ScanRepository(
            appContext = this,
            apiService = RetrofitClient.apiService,
            scanDao = AppDatabase.getInstance(this).scanDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
    }
}
