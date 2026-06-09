package com.example.goodmail.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodmail.data.repository.EmailRepository
import com.example.goodmail.domain.model.Email
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val email: Email? = null,
    val body: String? = null,
    val isLoadingBody: Boolean = true,
    val error: String? = null,
    val deleted: Boolean = false,
)

@HiltViewModel
class EmailDetailViewModel @Inject constructor(
    private val emailRepository: EmailRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val emailId: String = checkNotNull(savedStateHandle["emailId"])

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val email = emailRepository.emails.first().firstOrNull { it.id == emailId }
            _state.update { it.copy(email = email) }
            runCatching { emailRepository.loadBody(emailId) }
                .onSuccess { body -> _state.update { it.copy(body = body, isLoadingBody = false) } }
                .onFailure {
                    _state.update {
                        it.copy(isLoadingBody = false, error = "Couldn't load this message")
                    }
                }
        }
    }

    fun delete() {
        viewModelScope.launch {
            runCatching { emailRepository.deleteEmail(emailId) }
                .onSuccess { _state.update { it.copy(deleted = true) } }
                .onFailure { _state.update { it.copy(error = "Couldn't delete email") } }
        }
    }
}
