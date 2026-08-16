package com.appgurjant.stickynotes.ui.screens.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.domain.model.CoinEconomy
import com.app.domain.model.CoinState
import com.app.domain.repository.CoinOperationResult
import com.app.domain.usecase.ActivatePremiumTrialUseCase
import com.app.domain.usecase.ObserveCoinStateUseCase
import com.app.domain.usecase.UnlockSecurityUseCase
import com.appgurjant.stickynotes.ads.AdsManager
import com.appgurjant.stickynotes.ads.EarnAdResult
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

@HiltViewModel
class SettingsEconomyViewModel @Inject constructor(
    observeCoinStateUseCase: ObserveCoinStateUseCase,
    private val unlockSecurityUseCase: UnlockSecurityUseCase,
    private val activatePremiumTrialUseCase: ActivatePremiumTrialUseCase,
    private val adsManager: AdsManager
) : ViewModel() {

    val coinState: StateFlow<CoinState> = observeCoinStateUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, CoinState())

    private val _isWatchingAd = MutableStateFlow(false)
    val isWatchingAd: StateFlow<Boolean> = _isWatchingAd.asStateFlow()

    private val _securityAdsWatched = MutableStateFlow(0)
    val securityAdsWatched: StateFlow<Int> = _securityAdsWatched.asStateFlow()

    private val _premiumTrialAdsWatched = MutableStateFlow(0)
    val premiumTrialAdsWatched: StateFlow<Int> = _premiumTrialAdsWatched.asStateFlow()

    private val _events = MutableSharedFlow<EconomyEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    /**
     * Watch rewarded ads toward session Security unlock.
     * After [CoinEconomy.SECURITY_UNLOCK_ADS_REQUIRED] completed ads,
     * Password & Biometric become available for this session.
     */
    fun watchSecurityUnlockAd(activity: Activity) {
        if (_isWatchingAd.value) return
        if (coinState.value.hasSecurityAccess || coinState.value.isPremiumActive) {
            viewModelScope.launch { _events.emit(EconomyEvent.SecurityAlreadyUnlocked) }
            return
        }
        if (!adsManager.canShowAds()) {
            viewModelScope.launch {
                if (adsManager.shouldOfferFallback()) {
                    _events.emit(EconomyEvent.AdSystemFailedUseFallback)
                } else {
                    _events.emit(EconomyEvent.AdUnavailable)
                }
            }
            return
        }

        _isWatchingAd.value = true
        adsManager.showRewardedAd(activity) { result ->
            viewModelScope.launch {
                when (result) {
                    EarnAdResult.Earned -> {
                        val next = (_securityAdsWatched.value + 1)
                            .coerceAtMost(CoinEconomy.SECURITY_UNLOCK_ADS_REQUIRED)
                        _securityAdsWatched.value = next
                        adsManager.preloadRewardedAd()

                        if (next >= CoinEconomy.SECURITY_UNLOCK_ADS_REQUIRED) {
                            when (unlockSecurityUseCase()) {
                                is CoinOperationResult.Success,
                                CoinOperationResult.AlreadyUnlocked -> {
                                    _securityAdsWatched.value = 0
                                    _events.emit(EconomyEvent.SecurityUnlocked)
                                }
                                else -> Unit
                            }
                        }
                    }
                    EarnAdResult.Unavailable -> {
                        if (adsManager.shouldOfferFallback()) {
                            _events.emit(EconomyEvent.AdSystemFailedUseFallback)
                        } else {
                            _events.emit(EconomyEvent.AdUnavailable)
                        }
                    }
                    EarnAdResult.Dismissed -> Unit
                }
                _isWatchingAd.value = false
            }
        }
    }

    /**
     * Watch rewarded ads toward the 24-hour Premium trial.
     * After 4 ads, Premium (ad-free + Security) activates for 24 hours.
     */
    fun watchPremiumTrialAd(activity: Activity) {
        if (_isWatchingAd.value) return
        if (coinState.value.isPremiumActive || !adsManager.canShowAds()) {
            viewModelScope.launch {
                if (coinState.value.isPremiumActive) {
                    _events.emit(EconomyEvent.PremiumAlreadyActive)
                } else if (adsManager.shouldOfferFallback()) {
                    _events.emit(EconomyEvent.AdSystemFailedUseFallback)
                } else {
                    _events.emit(EconomyEvent.AdUnavailable)
                }
            }
            return
        }

        _isWatchingAd.value = true
        adsManager.showRewardedAd(activity) { result ->
            viewModelScope.launch {
                when (result) {
                    EarnAdResult.Earned -> {
                        val next = (_premiumTrialAdsWatched.value + 1)
                            .coerceAtMost(CoinEconomy.PREMIUM_TRIAL_ADS_REQUIRED)
                        _premiumTrialAdsWatched.value = next
                        adsManager.preloadRewardedAd()

                        if (next >= CoinEconomy.PREMIUM_TRIAL_ADS_REQUIRED) {
                            when (activatePremiumTrialUseCase()) {
                                is CoinOperationResult.Success,
                                CoinOperationResult.AlreadyUnlocked -> {
                                    _premiumTrialAdsWatched.value = 0
                                    _events.emit(EconomyEvent.PremiumTrialActivated)
                                }
                                else -> Unit
                            }
                        }
                    }
                    EarnAdResult.Unavailable -> {
                        if (adsManager.shouldOfferFallback()) {
                            _events.emit(EconomyEvent.AdSystemFailedUseFallback)
                        } else {
                            _events.emit(EconomyEvent.AdUnavailable)
                        }
                    }
                    EarnAdResult.Dismissed -> Unit
                }
                _isWatchingAd.value = false
            }
        }
    }

    /** Temporary workaround when AdMob cannot load on the device. */
    fun grantFallbackSecurityUnlock() {
        viewModelScope.launch {
            when (unlockSecurityUseCase()) {
                is CoinOperationResult.Success,
                CoinOperationResult.AlreadyUnlocked -> {
                    _securityAdsWatched.value = 0
                    _events.emit(EconomyEvent.SecurityUnlocked)
                }
                else -> _events.emit(EconomyEvent.AdUnavailable)
            }
        }
    }

    sealed class EconomyEvent {
        data object AdUnavailable : EconomyEvent()
        data object AdSystemFailedUseFallback : EconomyEvent()
        data object SecurityUnlocked : EconomyEvent()
        data object SecurityAlreadyUnlocked : EconomyEvent()
        data object PremiumTrialActivated : EconomyEvent()
        data object PremiumAlreadyActive : EconomyEvent()
    }
}
