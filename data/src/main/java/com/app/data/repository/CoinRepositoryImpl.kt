package com.app.data.repository

import com.app.data.local.CoinStorage
import com.app.domain.model.CoinEconomy
import com.app.domain.model.CoinState
import com.app.domain.repository.CoinOperationResult
import com.app.domain.repository.CoinRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CoinRepositoryImpl(
    private val coinStorage: CoinStorage
) : CoinRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    /** Session-only — resets when the process is killed. */
    private val sessionSecurityUnlocked = MutableStateFlow(false)
    private val cachedState = MutableStateFlow(CoinState())

    init {
        runBlocking(Dispatchers.IO) {
            cachedState.value = CoinState(
                balance = coinStorage.snapshotBalance(),
                premiumExpiresAtMillis = coinStorage.snapshotPremiumExpiresAtMillis()
            )
        }
        scope.launch {
            combine(
                coinStorage.coinBalance,
                sessionSecurityUnlocked,
                coinStorage.premiumExpiresAtMillis
            ) { balance, unlocked, premiumExpiresAt ->
                CoinState(
                    balance = balance,
                    isSecurityUnlocked = unlocked,
                    premiumExpiresAtMillis = premiumExpiresAt
                )
            }.collect { cachedState.value = it }
        }
    }

    override val coinState: Flow<CoinState> = combine(
        coinStorage.coinBalance,
        sessionSecurityUnlocked,
        coinStorage.premiumExpiresAtMillis
    ) { balance, unlocked, premiumExpiresAt ->
        CoinState(
            balance = balance,
            isSecurityUnlocked = unlocked,
            premiumExpiresAtMillis = premiumExpiresAt
        )
    }

    override fun areAdsEnabled(): Boolean {
        val premiumActive =
            cachedState.value.premiumExpiresAtMillis > System.currentTimeMillis()
        return !premiumActive
    }

    override suspend fun addCoins(amount: Int): CoinOperationResult {
        if (amount <= 0) return CoinOperationResult.InvalidAmount
        val newBalance = coinStorage.addCoins(amount)
        val state = CoinState(
            balance = newBalance,
            isSecurityUnlocked = sessionSecurityUnlocked.value,
            premiumExpiresAtMillis = coinStorage.snapshotPremiumExpiresAtMillis()
        )
        cachedState.value = state
        return CoinOperationResult.Success(state)
    }

    override suspend fun unlockSecurity(): CoinOperationResult = mutex.withLock {
        if (sessionSecurityUnlocked.value) {
            return CoinOperationResult.AlreadyUnlocked
        }
        sessionSecurityUnlocked.value = true
        val state = CoinState(
            balance = coinStorage.snapshotBalance(),
            isSecurityUnlocked = true,
            premiumExpiresAtMillis = coinStorage.snapshotPremiumExpiresAtMillis()
        )
        cachedState.value = state
        return CoinOperationResult.Success(state)
    }

    override suspend fun activatePremiumTrial(): CoinOperationResult = mutex.withLock {
        val now = System.currentTimeMillis()
        val currentExpiry = coinStorage.snapshotPremiumExpiresAtMillis()
        if (currentExpiry > now) {
            return CoinOperationResult.AlreadyUnlocked
        }
        val expiresAt = now + CoinEconomy.PREMIUM_TRIAL_DURATION_MS
        val savedExpiry = coinStorage.setPremiumExpiresAtMillis(expiresAt)
        val state = CoinState(
            balance = coinStorage.snapshotBalance(),
            isSecurityUnlocked = sessionSecurityUnlocked.value,
            premiumExpiresAtMillis = savedExpiry
        )
        cachedState.value = state
        return CoinOperationResult.Success(state)
    }
}
