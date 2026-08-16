package com.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.coinDataStore by preferencesDataStore(name = "coin_preferences")

class CoinStorage(private val context: Context) {

    private val mutex = Mutex()
    private val coinBalanceKey = intPreferencesKey(KEY_COIN_BALANCE)
    private val premiumExpiresAtKey = longPreferencesKey(KEY_PREMIUM_EXPIRES_AT)

    val coinBalance: Flow<Int> = context.coinDataStore.data.map { prefs ->
        prefs[coinBalanceKey] ?: 0
    }

    val premiumExpiresAtMillis: Flow<Long> = context.coinDataStore.data.map { prefs ->
        prefs[premiumExpiresAtKey] ?: 0L
    }

    suspend fun snapshotBalance(): Int = coinBalance.first()

    suspend fun snapshotPremiumExpiresAtMillis(): Long = premiumExpiresAtMillis.first()

    suspend fun addCoins(amount: Int): Int = mutex.withLock {
        var updated = 0
        context.coinDataStore.edit { prefs ->
            val current = prefs[coinBalanceKey] ?: 0
            updated = (current + amount).coerceAtLeast(0)
            prefs[coinBalanceKey] = updated
        }
        updated
    }

    /**
     * Deducts [cost] if the balance is sufficient. Returns the new balance,
     * or null if there were not enough coins (balance unchanged).
     */
    suspend fun trySpend(cost: Int): Int? = mutex.withLock {
        var result: Int? = null
        context.coinDataStore.edit { prefs ->
            val current = prefs[coinBalanceKey] ?: 0
            if (current >= cost) {
                val next = (current - cost).coerceAtLeast(0)
                prefs[coinBalanceKey] = next
                result = next
            }
        }
        result
    }

    /**
     * Sets Premium expiry to at least [expiresAtMillis]. If an existing expiry
     * is later, it is kept (never shortens an active trial).
     */
    suspend fun setPremiumExpiresAtMillis(expiresAtMillis: Long): Long = mutex.withLock {
        var updated = expiresAtMillis
        context.coinDataStore.edit { prefs ->
            val current = prefs[premiumExpiresAtKey] ?: 0L
            updated = maxOf(current, expiresAtMillis)
            prefs[premiumExpiresAtKey] = updated
        }
        updated
    }

    companion object {
        const val KEY_COIN_BALANCE = "coin_balance"
        const val KEY_PREMIUM_EXPIRES_AT = "premium_expires_at_millis"
    }
}
