package com.app.domain.usecase

import com.app.domain.model.CoinEconomy
import com.app.domain.model.CoinState
import com.app.domain.repository.CoinOperationResult
import com.app.domain.repository.CoinRepository
import kotlinx.coroutines.flow.Flow

class ObserveCoinStateUseCase(
    private val coinRepository: CoinRepository
) {
    operator fun invoke(): Flow<CoinState> = coinRepository.coinState
}

class AddRewardedAdCoinsUseCase(
    private val coinRepository: CoinRepository
) {
    suspend operator fun invoke(): CoinOperationResult =
        coinRepository.addCoins(CoinEconomy.REWARDED_AD_COIN_REWARD)
}

class UnlockSecurityUseCase(
    private val coinRepository: CoinRepository
) {
    suspend operator fun invoke(): CoinOperationResult = coinRepository.unlockSecurity()
}

class ActivatePremiumTrialUseCase(
    private val coinRepository: CoinRepository
) {
    suspend operator fun invoke(): CoinOperationResult = coinRepository.activatePremiumTrial()
}
