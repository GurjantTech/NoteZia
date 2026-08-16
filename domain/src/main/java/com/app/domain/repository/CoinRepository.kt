package com.app.domain.repository

import com.app.domain.model.CoinState
import kotlinx.coroutines.flow.Flow

sealed class CoinOperationResult {
    data class Success(val state: CoinState) : CoinOperationResult()
    data object InsufficientFunds : CoinOperationResult()
    data object AlreadyUnlocked : CoinOperationResult()
    data object InvalidAmount : CoinOperationResult()
}

interface CoinRepository {
    val coinState: Flow<CoinState>

    /** Cached, main-thread-safe snapshot for AdsManager.canShowAds(). */
    fun areAdsEnabled(): Boolean

    suspend fun addCoins(amount: Int): CoinOperationResult

    suspend fun unlockSecurity(): CoinOperationResult

    /** Starts / extends the 24-hour Premium trial (ad-free + Security). */
    suspend fun activatePremiumTrial(): CoinOperationResult
}
