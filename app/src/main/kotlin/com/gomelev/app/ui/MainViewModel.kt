package com.gomelev.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.gomelev.app.GomelEvApp
import com.gomelev.app.model.ForecastScenario
import com.gomelev.app.model.UserFinanceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppDestination(val labelRu: String) {
    CARS("Авто"),
    DEALERS("Дилеры"),
    CREDITS("Кредиты"),
    FORECAST_SYNC("2027 · Сеть")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GomelEvApp
    private val repo = app.repository

    val cars = repo.cars
    val dealers = repo.dealers
    val programs = repo.programs
    val profile = repo.profile
    val snapshot = repo.snapshot
    val scenario = repo.scenario
    val autoSyncEnabled = repo.autoSyncEnabled

    private val _destination = MutableStateFlow(AppDestination.CARS)
    val destination: StateFlow<AppDestination> = _destination.asStateFlow()

    private val _selectedCarId = MutableStateFlow<String?>(null)
    val selectedCarId: StateFlow<String?> = _selectedCarId.asStateFlow()

    private val _selectedDealerId = MutableStateFlow<String?>(null)
    val selectedDealerId: StateFlow<String?> = _selectedDealerId.asStateFlow()

    private val _selectedProgramId = MutableStateFlow<String?>(null)
    val selectedProgramId: StateFlow<String?> = _selectedProgramId.asStateFlow()

    private var initialSyncDone = false

    fun onAppLaunchSyncIfNeeded() {
        if (initialSyncDone) return
        initialSyncDone = true
        triggerLiveSync()
    }

    fun selectDestination(dest: AppDestination) {
        _destination.value = dest
    }

    fun selectCar(id: String?) {
        _selectedCarId.value = id
    }

    fun openCarFromDealer(carId: String) {
        _selectedCarId.value = carId
        _destination.value = AppDestination.CARS
    }

    fun selectDealer(id: String?) {
        _selectedDealerId.value = id
    }

    fun selectProgram(id: String?) {
        _selectedProgramId.value = id
    }

    fun updateProfile(transform: (UserFinanceProfile) -> UserFinanceProfile) {
        repo.updateProfile(transform)
    }

    fun selectScenario(newScenario: ForecastScenario) {
        repo.selectScenario(newScenario)
    }

    fun triggerLiveSync() {
        viewModelScope.launch {
            repo.syncLiveWebData()
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        repo.setAutoSyncEnabled(enabled)
        if (enabled) {
            app.scheduleBackgroundSync()
        } else {
            WorkManager.getInstance(app).cancelUniqueWork("gomel_ev_periodic_sync")
        }
    }

    fun updateManifestUrl(url: String) {
        repo.updateManifestUrl(url)
    }

    fun checkRemoteUpdate() {
        viewModelScope.launch {
            repo.checkAppRemoteUpdate()
        }
    }

    fun installRemoteApk(apkUrl: String) {
        viewModelScope.launch {
            repo.installRemoteApk(apkUrl)
        }
    }
}
