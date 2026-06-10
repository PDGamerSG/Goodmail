package com.example.goodmail.ui.screens.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodmail.data.repository.AuthRepository
import com.example.goodmail.data.repository.EmailRepository
import com.example.goodmail.domain.model.Email
import com.example.goodmail.domain.model.EmailImportance
import com.example.goodmail.domain.usecase.ClassifyEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class InboxFilter { ALL, IMPORTANT, OTHERS }

data class InboxUiState(
    val emails: List<Email> = emptyList(),
    val isRefreshing: Boolean = false,
    val isInitialLoading: Boolean = true,
    val isClassifying: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val emailRepository: EmailRepository,
    private val authRepository: AuthRepository,
    private val classifyEmailUseCase: ClassifyEmailUseCase,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val initialLoading = MutableStateFlow(true)
    private val classifying = MutableStateFlow(false)
    private val loadingMore = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    /** Whether Gmail still has older pages; reset on every refresh. */
    private var hasMore = true

    private val _filter = MutableStateFlow(InboxFilter.ALL)
    val filter: StateFlow<InboxFilter> = _filter.asStateFlow()

    private val filteredEmails = combine(emailRepository.emails, _filter) { emails, filter ->
        when (filter) {
            InboxFilter.ALL -> emails
            InboxFilter.IMPORTANT -> emails.filter { it.importance == EmailImportance.IMPORTANT }
            InboxFilter.OTHERS -> emails.filter { it.importance != EmailImportance.IMPORTANT }
        }
    }

    val uiState: StateFlow<InboxUiState> = combine(
        filteredEmails,
        refreshing,
        initialLoading,
        classifying,
        loadingMore,
        error,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val emails = values[0] as List<Email>
        InboxUiState(
            emails = emails,
            isRefreshing = values[1] as Boolean,
            isInitialLoading = values[2] as Boolean && emails.isEmpty(),
            isClassifying = values[3] as Boolean,
            isLoadingMore = values[4] as Boolean,
            error = values[5] as String?,
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
            val refreshed = runCatching { emailRepository.refreshInbox() }
                .onFailure { error.value = "Couldn't reach Gmail. Showing cached mail." }
                .isSuccess
            refreshing.value = false
            initialLoading.value = false
            if (refreshed) {
                hasMore = true
                classify()
            }
        }
    }

    /** Fetch the next (older) page when the user scrolls near the bottom. */
    fun loadMore() {
        if (loadingMore.value || refreshing.value || !hasMore) return
        viewModelScope.launch {
            loadingMore.value = true
            runCatching { emailRepository.loadMore() }
                .onSuccess { more ->
                    hasMore = more
                    classify()
                }
                .onFailure { error.value = "Couldn't load more emails" }
            loadingMore.value = false
        }
    }

    fun setFilter(filter: InboxFilter) {
        _filter.value = filter
    }

    private fun classify() {
        viewModelScope.launch {
            classifying.value = true
            runCatching { classifyEmailUseCase.classifyUnclassified() }
            classifying.value = false
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

    fun dismissError() {
        error.value = null
    }
}
