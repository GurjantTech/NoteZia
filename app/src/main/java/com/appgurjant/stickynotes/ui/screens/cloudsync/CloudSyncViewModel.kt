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
            runCatching { signInWithGoogleUseCase() }
                .onSuccess { _events.emit(CloudSyncEvent.SignInSuccess(it)) }
                .onFailure {
                    _events.emit(CloudSyncEvent.SignInFailed(it.message ?: "Sign-in failed"))
                }
        }
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

    /**
     * Pre-flight check for the manual "Sync with Drive" tap.
     *
     * With full two-way synchronisation, "having work to do" is no longer a
     * pure local-state question — a freshly signed-in device may have zero
     * local notes but a non-empty remote backlog to download. Determining
     * that requires the same Firestore round-trip as the sync itself, so
     * the precheck is intentionally minimal:
     *
     * - Not signed in → emit [CloudSyncEvent.SyncRequiresSignIn] (UI redirects
     *   to Google Sign-In; no ad).
     * - Signed in → emit [CloudSyncEvent.SyncReady] (UI shows the rewarded
     *   ad and only then invokes [syncNow]).
     *
     * The "nothing to sync" UX still surfaces — once [syncNow] completes with
     * `attempted == 0`, [syncMessage] resolves to the same friendly toast.
     */
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

    /**
     * Performs the actual upload. Should only be called by the UI **after**
     * [requestSync] emitted [CloudSyncEvent.SyncReady] and (optionally) the
     * rewarded ad finished. Safe to call directly in tests.
     */
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
