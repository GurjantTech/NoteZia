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

    fun signIn() {
        viewModelScope.launch {
            val signInOutcome = runCatching { signInWithGoogleUseCase() }
            signInOutcome
                .onSuccess { profile ->
                    _events.emit(CloudSyncEvent.SignInSuccess(profile))
                    // Auto-sync once on login: upload any pre-existing local
                    // notes (isSync = 0) so the freshly signed-in user sees
                    // their notes mirrored to Firestore without an extra tap.
                    // All subsequent note mutations stay manual-sync only.
                    runPostSignInSync()
                }
                .onFailure {
                    _events.emit(CloudSyncEvent.SignInFailed(it.message ?: "Sign-in failed"))
                }
        }
    }

    /**
     * Runs the same pipeline as [syncNow] but without any pre-flight UX
     * gating (rewarded ad, sign-in prompt) — those are owned by
     * [requestSync] for manual user-initiated syncs.
     *
     * Toggles [isSyncing] so any observing UI shows a loading state and
     * emits [CloudSyncEvent.SyncFinished] so existing subscribers
     * (Settings + Dashboard) reuse their toast messaging.
     */
    private suspend fun runPostSignInSync() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        val result = runCatching { syncPendingNotesUseCase() }
            .getOrDefault(SyncResult(0, 0, 0, errorMessage = "Sync failed"))
        _isSyncing.value = false
        _events.emit(CloudSyncEvent.SyncFinished(result))
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
