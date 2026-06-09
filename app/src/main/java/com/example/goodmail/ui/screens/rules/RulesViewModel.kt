package com.example.goodmail.ui.screens.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodmail.data.repository.RuleRepository
import com.example.goodmail.domain.model.EmailImportance
import com.example.goodmail.domain.model.ImportanceRule
import com.example.goodmail.domain.model.RuleType
import com.example.goodmail.domain.usecase.ClassifyEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val classifyEmailUseCase: ClassifyEmailUseCase,
) : ViewModel() {

    val rules: StateFlow<List<ImportanceRule>> = ruleRepository.rules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addRule(type: RuleType, value: String, action: EmailImportance) {
        if (value.isBlank()) return
        viewModelScope.launch {
            ruleRepository.addRule(
                ImportanceRule(
                    type = type,
                    value = value.trim(),
                    action = action,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            classifyEmailUseCase.reapplyRules()
        }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch { ruleRepository.deleteRule(id) }
    }
}
