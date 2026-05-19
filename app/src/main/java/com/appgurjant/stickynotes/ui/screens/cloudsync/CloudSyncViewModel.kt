package com.appgurjant.stickynotes.ui.screens.cloudsync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.domain.model.SyncResult
import com.app.domain.model.UserProfile
import com.app.domain.usecase.ObserveCurrentUserUseCase
import com.app.domain.usecase.SignInWithGoogleUseCase
import com.app.domain.usecase.SignOutUseCase
import com.app.domain.usecase.SyncPendingNotesUseCase
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
 * Shared ViewModel for Google Sign-In and cloud-sync UI flows.
 *
 * Lives at the activity scope so the Settings screen, the dedicated
 * `GoogleSignInScreen`, and any future entry points all observe the same
 * `currentUser` state without re-querying DataStore.
 */
@HiltViewModel
class CloudSyncViewModel @Inject constructor(
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val syncPendingNotesUseCase: SyncPendingNotesUseCase,
    observeCurrentUserUseCase: ObserveCurrentUserUseCase
) : ViewModel() {

    val currentUser: StateFlow<UserProfile?> = observeCurrentUserUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _events = MutableSharedFlow<CloudSyncEvent>()
    val events = _events.asSharedFlow()

    /**
     * Durable, one-shot sign-in result message that survives navigation and
     * configuration changes. Settings observes this and shows a toast on
     * non-null values, then calls [consumeSignInMessage] to clear it so the
     * same message isn't re-shown on rotation.
     *
     * We can't reuse [_events] for the post-sign-in toast because the
     * Settings composition is destroyed by Compose Navigation while the
     * Google Sign-In screen is on top — by the time Settings re-enters
     * composition after `popBackStack()`, a SharedFlow with replay = 0 has
     * already dispatched the event to whatever collectors were active and
     * there's nothing left for the new collector to receive.
     */
    private val _signInMessage = MutableStateFlow<SignInMessage?>(null)
    val signInMessage: StateFlow<SignInMessage?> = _signInMessage.asStateFlow()

    /** Clear the pending sign-in message after the UI has displayed it. */
    fun consumeSignInMessage() {
        _signInMessage.value = null
    }

    /**
     * Sign-in is intentionally minimal: persist the session (handled inside
     * [SignInWithGoogleUseCase] via the auth repository) and save the user
     * profile to Firestore. Notes are NEVER auto-synced here — the user
     * must explicitly tap **Sync My Notes** to upload local data, keeping
     * the manual-sync contract documented in [requestSync] / [syncNow].
     */
    fun signIn() {
        viewModelScope.launch {
            runCatching { signInWithGoogleUseCase() }
                .onSuccess { profile ->
                    _events.emit(CloudSyncEvent.SignInSuccess(profile))
                    _signInMessage.value = SignInMessage.Success
                }
                .onFailure { throwable ->
                    val message = throwable.message ?: "Sign-in failed"
                    _events.emit(CloudSyncEvent.SignInFailed(message))
                    // User-cancellations should be silent — tapping "back" or
                    // dismissing the Google account picker isn't a failure
                    // worth toasting about (matches spec: "If login is
                    // cancelled, do not show any success message" / no error).
                    if (!throwable.isUserCancellation()) {
                        _signInMessage.value = SignInMessage.Failed(message)
                    }
                }
        }
    }

    private fun Throwable.isUserCancellation(): Boolean {
        // We deliberately match by simple class name + message keywords to
        // avoid pulling the Credentials / GMS types into the app-module
        // ViewModel — keeps the dependency direction clean.
        val typeName = javaClass.simpleName.lowercase()
        val messageText = message.orEmpty().lowercase()
        return "cancel" in typeName ||
            "cancel" in messageText ||
            "user canceled" in messageText
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { signOutUseCase() }
                .onSuccess { _events.emit(CloudSyncEvent.SignedOut) }
                .onFailure {
                    _events.emit(CloudSyncEvent.SignOutFailed(it.message ?: "Sign-out failed"))
                }
        }
    }


    fun requestSync() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            if (currentUser.value == null) {
                _events.emit(CloudSyncEvent.SyncRequiresSignIn)
                return@launch
            }
            _events.emit(CloudSyncEvent.SyncReady)
        }
    }


    fun syncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            val result = runCatching { syncPendingNotesUseCase() }
                .getOrDefault(SyncResult(0, 0, 0, errorMessage = "Sync failed"))
            _isSyncing.value = false
            _events.emit(CloudSyncEvent.SyncFinished(result))
        }
    }
}

/**
 * Durable sign-in outcome surfaced via [CloudSyncViewModel.signInMessage].
 * Mapped to a localized toast/snackbar in the UI layer.
 */
sealed interface SignInMessage {
    data object Success : SignInMessage
    data class Failed(val message: String) : SignInMessage
}

/** One-shot UI events emitted by [CloudSyncViewModel]. */
sealed interface CloudSyncEvent {
    data class SignInSuccess(val profile: UserProfile) : CloudSyncEvent
    data class SignInFailed(val message: String) : CloudSyncEvent
    data object SignedOut : CloudSyncEvent
    data class SignOutFailed(val message: String) : CloudSyncEvent
    data class SyncFinished(val result: SyncResult) : CloudSyncEvent

    /** User tapped sync but isn't signed in — UI should route to Google Sign-In. */
    data object SyncRequiresSignIn : CloudSyncEvent

    /** Signed in — UI shows the rewarded ad, then calls [CloudSyncViewModel.syncNow]. */
    data object SyncReady : CloudSyncEvent
}
