package com.example.goodmail.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodmail.data.local.datastore.SettingsStore
import com.example.goodmail.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _model = MutableStateFlow(SettingsStore.DEFAULT_MODEL)
    val model: StateFlow<String> = _model.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    val isSignedOut: StateFlow<Boolean> = authRepository.accountEmail
        .map { it == null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch {
            _apiKey.value = settingsStore.apiKey.first().orEmpty()
            _model.value = settingsStore.model.first()
        }
    }

    fun onApiKeyChange(value: String) {
        _apiKey.value = value
        _saved.value = false
    }

    fun onModelChange(value: String) {
        _model.value = value
        _saved.value = false
    }

    fun save() {
        viewModelScope.launch {
            settingsStore.setApiKey(_apiKey.value)
            settingsStore.setModel(_model.value.ifBlank { SettingsStore.DEFAULT_MODEL })
            _saved.value = true
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
