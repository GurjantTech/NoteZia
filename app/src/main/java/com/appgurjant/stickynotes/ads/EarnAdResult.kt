package com.appgurjant.stickynotes.ads

/**
 * Result of a user-initiated Earn Coins rewarded ad request.
 *
 * Coins must be granted only on [Earned] (user completed the rewarded video).
 */
sealed class EarnAdResult {
    /** User earned the reward — grant coins. */
    data object Earned : EarnAdResult()

    /** No ad available (load/show failure, ads disabled, etc.). */
    data object Unavailable : EarnAdResult()

    /** User closed the ad before earning a reward. */
    data object Dismissed : EarnAdResult()
}
