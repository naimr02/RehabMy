package com.naimrlet.rehabmy.patient.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class DashboardViewModel : ViewModel() {
    var isLoggedOut by mutableStateOf(false)

    fun logout() {
        isLoggedOut = true
    }
}
