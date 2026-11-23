package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class PlayerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Player",
    val highScore: Long = 0L,
    val totalStars: Long = 0L,
    val highestUnlockedLevel: Long = 1L,
    val bonusCoins: Long = 100L,
    val lastClaimedDailyRewardDay: String = "",
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val aimGuideExtended: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LevelProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val levelNumber: Long = 1L,
    val bestScore: Long = 0L,
    val stars: Long = 0L,
    val completed: Boolean = false,
    val attempts: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
