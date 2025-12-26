package com.perez.dutchlearner.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val _notificationTime = MutableStateFlow(Pair(9, 0)) // 9:00 AM por defecto
    val notificationTime: StateFlow<Pair<Int, Int>> = _notificationTime

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled

    fun setNotificationTime(hour: Int, minute: Int) {
        _notificationTime.value = Pair(hour, minute)
        // Aquí guardarías en SharedPreferences
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        // Aquí guardarías en SharedPreferences
    }
}