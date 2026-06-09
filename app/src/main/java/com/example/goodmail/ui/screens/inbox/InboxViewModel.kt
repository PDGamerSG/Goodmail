package com.example.goodmail.ui.screens.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodmail.data.repository.AuthRepository
import com.example.goodmail.data.repository.EmailRepository
import com.example.goodmail.domain.model.Email
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboxUiState(
    val emails: List<Email> = emptyList(),
    val isRefreshing: Boolean = false,
    val isInitialLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val emailRepository: EmailRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val initialLoading = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<InboxUiState> = combine(
        emailRepository.emails,
        refreshing,
        initialLoading,
        error,
    ) { emails, isRefreshing, isInitial, err ->
        InboxUiState(
            emails = emails,
            isRefreshing = isRefreshing,
            isInitialLoading = isInitial && emails.isEmpty(),
            error = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxUiState())

    val isSignedOut: StateFlow<Boolean> = authRepository.accountEmail
        .map { it == null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            error.value = null
            runCatching { emailRepository.refreshInbox() }
                .onFailure { error.value = "Couldn't reach Gmail. Showing cached mail." }
            refreshing.value = false
            initialLoading.value = false
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            runCatching { emailRepository.deleteEmail(id) }
                .onFailure { error.value = "Couldn't delete email" }
        }
    }

    fun undoDelete() {
        viewModelScope.launch { emailRepository.undoLastDelete() }
    }

    fun markRead(id: String) {
        viewModelScope.launch { emailRepository.markAsRead(id) }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun dismissError() {
        error.value = null
    }
}
