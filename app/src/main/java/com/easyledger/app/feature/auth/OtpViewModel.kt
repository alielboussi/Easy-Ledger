package com.easyledger.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easyledger.app.core.data.OtpRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class OtpState {
    data object Idle: OtpState()
    data object Sending: OtpState()
    data object Verifying: OtpState()
    data class Error(val message: String): OtpState()
    data class Sent(val message: String? = null): OtpState()
    data object Success: OtpState()
}

class OtpViewModel: ViewModel() {
    private val repo = OtpRepository()

    private val _state = MutableStateFlow<OtpState>(OtpState.Idle)
    val state: StateFlow<OtpState> = _state

    fun send(email: String) {
        viewModelScope.launch {
            _state.value = OtpState.Sending
            runCatching { repo.send(email) }.onSuccess {
                if (!it.ok) {
                    _state.value = OtpState.Error(it.message ?: "Failed to send code")
                } else {
                    _state.value = OtpState.Sent(it.message)
                }
            }.onFailure { _state.value = OtpState.Error(it.message ?: "Failed to send code") }
        }
    }

    fun verify(email: String, code: String) {
        viewModelScope.launch {
            _state.value = OtpState.Verifying
            runCatching { repo.verify(email, code) }.onSuccess {
                if (it.ok) _state.value = OtpState.Success else _state.value = OtpState.Error(it.message ?: "Invalid code")
            }.onFailure { _state.value = OtpState.Error(it.message ?: "Verification failed") }
        }
    }
}
