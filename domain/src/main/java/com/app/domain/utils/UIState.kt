package com.app.domain.utils

sealed class UIState {
    object Loading : UIState()
    data class Success(val data: Any) : UIState()
    data class Error(val message: String) : UIState()
}