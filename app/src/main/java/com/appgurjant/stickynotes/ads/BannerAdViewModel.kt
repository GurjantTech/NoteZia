package com.appgurjant.stickynotes.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.domain.usecase.ObserveCoinStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BannerAdViewModel @Inject constructor(
    observeCoinStateUseCase: ObserveCoinStateUseCase,
    private val adsManager: AdsManager
) : ViewModel() {

    /** True when banners should be requested (Security not unlocked). */
    val showBanner: StateFlow<Boolean> = observeCoinStateUseCase()
        .map { !it.isAdFree && adsManager.canShowAds() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, adsManager.canShowAds())
}
