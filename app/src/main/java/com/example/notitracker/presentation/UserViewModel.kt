package com.example.notitracker.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notitracker.data.remote.network.NetworkResult
import com.example.notitracker.data.repository.UserRepository
import com.example.notitracker.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class UserViewModel(
    private val userId: String,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getUser(userId)
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "Error")
                    }
                }
                .collect { result ->
                    _uiState.update {
                        when (result) {
                            is NetworkResult.Success -> UserUiState(
                                user = result.data,
                                isLoading = false,
                                errorMessage = null,
                            )
                            is NetworkResult.Failure -> UserUiState(
                                user = null,
                                isLoading = false,
                                errorMessage = result.error.toUserMessage(),
                            )
                        }
                    }
                }
        }
    }
}
