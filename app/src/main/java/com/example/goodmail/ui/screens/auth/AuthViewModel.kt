package com.example.goodmail.ui.screens.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodmail.data.repository.AuthRepository
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data class SignedOut(val error: String? = null) : AuthUiState
    data class SignedIn(val email: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    val signInIntent: Intent get() = authRepository.signInIntent

    init {
        viewModelScope.launch {
            val email = authRepository.restoreSession()
            _state.value = if (email != null) AuthUiState.SignedIn(email) else AuthUiState.SignedOut()
        }
    }

    fun onSignInResult(data: Intent?) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            authRepository.handleSignInResult(data)
                .onSuccess { email -> _state.value = AuthUiState.SignedIn(email) }
                .onFailure { e ->
                    val message = when (e) {
                        is ApiException -> "Sign-in failed (code ${e.statusCode})"
                        else -> e.message ?: "Sign-in failed"
                    }
                    _state.value = AuthUiState.SignedOut(error = message)
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _state.value = AuthUiState.SignedOut()
        }
    }
}
