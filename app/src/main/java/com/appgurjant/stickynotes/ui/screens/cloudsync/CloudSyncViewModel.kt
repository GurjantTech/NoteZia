package com.appgurjant.stickynotes.ui.screens.cloudsync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.domain.model.PhoneVerificationResult
import com.app.domain.model.UserProfile
import com.app.domain.usecase.CompletePhoneAutoSignInUseCase
import com.app.domain.usecase.DismissSyncBannerUseCase
import com.app.domain.usecase.ObserveCurrentUserUseCase
import com.app.domain.usecase.ObserveSyncBannerDismissedUseCase
import com.app.domain.usecase.SendPhoneVerificationCodeUseCase
import com.app.domain.usecase.SignInWithGoogleUseCase
import com.app.domain.usecase.SignOutUseCase
import com.app.domain.usecase.VerifyPhoneCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shared ViewModel for Firebase Authentication (Google + Phone) and account UI.
 *
 * Lives at the activity scope so Settings, the sign-in screen, and onboarding
 * all observe the same [currentUser] without re-querying Firebase.
 *
 * Note synchronization is automatic (Room save → WorkManager) and is not
 * triggered manually from this ViewModel.
 */
@HiltViewModel
class CloudSyncViewModel @Inject constructor(
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val sendPhoneVerificationCodeUseCase: SendPhoneVerificationCodeUseCase,
    private val verifyPhoneCodeUseCase: VerifyPhoneCodeUseCase,
    private val completePhoneAutoSignInUseCase: CompletePhoneAutoSignInUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val dismissSyncBannerUseCase: DismissSyncBannerUseCase,
    observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    observeSyncBannerDismissedUseCase: ObserveSyncBannerDismissedUseCase
) : ViewModel() {

    val currentUser: StateFlow<UserProfile?> = observeCurrentUserUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isSyncBannerDismissed: StateFlow<Boolean> = observeSyncBannerDismissedUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _authInProgress = MutableStateFlow<AuthInProgress?>(null)
    val authInProgress: StateFlow<AuthInProgress?> = _authInProgress.asStateFlow()

    private val _events = MutableSharedFlow<CloudSyncEvent>()
    val events = _events.asSharedFlow()

    private val _signInMessage = MutableStateFlow<SignInMessage?>(null)
    val signInMessage: StateFlow<SignInMessage?> = _signInMessage.asStateFlow()

    private val _phoneVerificationId = MutableStateFlow<String?>(null)
    val phoneVerificationId: StateFlow<String?> = _phoneVerificationId.asStateFlow()

    private val _awaitingPhoneCode = MutableStateFlow(false)
    val awaitingPhoneCode: StateFlow<Boolean> = _awaitingPhoneCode.asStateFlow()

    fun consumeSignInMessage() {
        _signInMessage.value = null
    }

    fun resetPhoneAuthState() {
        _phoneVerificationId.value = null
        _awaitingPhoneCode.value = false
    }

    fun signInWithGoogle() {
        if (_authInProgress.value != null) return
        viewModelScope.launch {
            _authInProgress.value = AuthInProgress.Google
            runCatching { signInWithGoogleUseCase() }
                .onSuccess { profile ->
                    resetPhoneAuthState()
                    _events.emit(CloudSyncEvent.SignInSuccess(profile))
                    _signInMessage.value = SignInMessage.Success
                }
                .onFailure { throwable ->
                    val message = throwable.message ?: "Sign-in failed"
                    _events.emit(CloudSyncEvent.SignInFailed(message))
                    if (!throwable.isUserCancellation()) {
                        _signInMessage.value = SignInMessage.Failed(message)
                    }
                }
            _authInProgress.value = null
        }
    }

    fun sendPhoneVerificationCode(phoneNumber: String, activityHost: Any) {
        if (_authInProgress.value != null) return
        viewModelScope.launch {
            _authInProgress.value = AuthInProgress.Phone
            runCatching { sendPhoneVerificationCodeUseCase(phoneNumber, activityHost) }
                .onSuccess { result ->
                    when (result) {
                        is PhoneVerificationResult.CodeSent -> {
                            _phoneVerificationId.value = result.verificationId
                            _awaitingPhoneCode.value = true
                            _events.emit(CloudSyncEvent.PhoneCodeSent)
                        }
                        is PhoneVerificationResult.AutoVerified -> {
                            runCatching { completePhoneAutoSignInUseCase(result.profile) }
                                .onSuccess { profile ->
                                    resetPhoneAuthState()
                                    _events.emit(CloudSyncEvent.SignInSuccess(profile))
                                    _signInMessage.value = SignInMessage.Success
                                }
                                .onFailure { error ->
                                    val message = error.message ?: "Phone sign-in failed"
                                    _events.emit(CloudSyncEvent.SignInFailed(message))
                                    _signInMessage.value = SignInMessage.Failed(message)
                                }
                        }
                    }
                }
                .onFailure { throwable ->
                    val message = throwable.message ?: "Failed to send verification code"
                    _events.emit(CloudSyncEvent.SignInFailed(message))
                    _signInMessage.value = SignInMessage.Failed(message)
                }
            _authInProgress.value = null
        }
    }

    fun verifyPhoneCode(code: String) {
        val verificationId = _phoneVerificationId.value ?: return
        if (_authInProgress.value != null) return
        viewModelScope.launch {
            _authInProgress.value = AuthInProgress.Phone
            runCatching { verifyPhoneCodeUseCase(verificationId, code) }
                .onSuccess { profile ->
                    resetPhoneAuthState()
                    _events.emit(CloudSyncEvent.SignInSuccess(profile))
                    _signInMessage.value = SignInMessage.Success
                }
                .onFailure { throwable ->
                    val message = throwable.message ?: "Invalid verification code"
                    _events.emit(CloudSyncEvent.SignInFailed(message))
                    _signInMessage.value = SignInMessage.Failed(message)
                }
            _authInProgress.value = null
        }
    }

    private fun Throwable.isUserCancellation(): Boolean {
        val typeName = javaClass.simpleName.lowercase()
        val messageText = message.orEmpty().lowercase()
        return "cancel" in typeName ||
            "cancel" in messageText ||
            "user canceled" in messageText
    }

    fun dismissSyncBanner() {
        viewModelScope.launch {
            dismissSyncBannerUseCase()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { signOutUseCase() }
                .onSuccess {
                    resetPhoneAuthState()
                    _events.emit(CloudSyncEvent.SignedOut)
                }
                .onFailure {
                    _events.emit(CloudSyncEvent.SignOutFailed(it.message ?: "Sign-out failed"))
                }
        }
    }
}

enum class AuthInProgress {
    Google,
    Phone
}

sealed interface SignInMessage {
    data object Success : SignInMessage
    data class Failed(val message: String) : SignInMessage
}

sealed interface CloudSyncEvent {
    data class SignInSuccess(val profile: UserProfile) : CloudSyncEvent
    data class SignInFailed(val message: String) : CloudSyncEvent
    data object PhoneCodeSent : CloudSyncEvent
    data object SignedOut : CloudSyncEvent
    data class SignOutFailed(val message: String) : CloudSyncEvent
}
