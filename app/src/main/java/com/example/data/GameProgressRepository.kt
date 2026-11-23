package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GameProgressRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observePlayerProfile(userId: String): Flow<PlayerProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch for profile observation." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            PlayerProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeAllLevelProgress(userId: String): Flow<List<LevelProgress>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch for level progress observation." }
        val path = "users/$uid/level_progress"
        emitAll(
            db.collection("users").document(uid).collection("level_progress")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LevelProgress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureProfileInitialized(displayName: String): Result<Unit> {
        val safeName = displayName.trim().take(60).ifEmpty { "Bubble Player" }
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "highScore" to 0L,
                    "totalStars" to 0L,
                    "highestUnlockedLevel" to 1L,
                    "bonusCoins" to 100L,
                    "lastClaimedDailyRewardDay" to "",
                    "soundEnabled" to true,
                    "hapticsEnabled" to true,
                    "aimGuideExtended" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordLevelResult(
        levelNumber: Int,
        score: Long,
        starsEarned: Int,
        won: Boolean,
        currentProfile: PlayerProfile,
        existingLevel: LevelProgress?,
        allExistingProgress: Map<Int, LevelProgress>
    ): Result<Unit> {
        val clampedLevel = levelNumber.coerceIn(1, 100)
        val clampedScore = score.coerceIn(0L, 10_000_000L)
        val clampedStars = if (won) starsEarned.coerceIn(1, 3).toLong() else 0L
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/level_progress/level_$clampedLevel"

        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            val levelDocId = "level_$clampedLevel"
            val levelRef = userRef.collection("level_progress").document(levelDocId)
            docPath = levelRef.path

            val newBestLevelScore = maxOf(existingLevel?.bestScore ?: 0L, clampedScore)
            val newBestStars = maxOf(existingLevel?.stars ?: 0L, clampedStars)
            val newCompleted = (existingLevel?.completed == true) || won
            val newAttempts = ((existingLevel?.attempts ?: 0L) + 1L).coerceAtMost(1_000_000L)

            if (existingLevel == null) {
                val createPayload = mapOf(
                    "userId" to uid,
                    "levelNumber" to clampedLevel.toLong(),
                    "bestScore" to newBestLevelScore,
                    "stars" to newBestStars,
                    "completed" to newCompleted,
                    "attempts" to newAttempts,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                levelRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf(
                    "bestScore" to newBestLevelScore,
                    "stars" to newBestStars,
                    "completed" to newCompleted,
                    "attempts" to newAttempts,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                levelRef.update(updatePayload).await()
            }

            // Recompute global profile aggregates cleanly
            val updatedStarsTotal = (allExistingProgress.values
                .filter { it.levelNumber.toInt() != clampedLevel }
                .sumOf { it.stars } + newBestStars)
                .coerceIn(0L, 300L)

            val newHighScore = maxOf(currentProfile.highScore, clampedScore).coerceIn(0L, 100_000_000L)
            val unlockedTarget = if (won && clampedLevel < 100) (clampedLevel + 1).toLong() else clampedLevel.toLong()
            val newUnlockedLevel = maxOf(currentProfile.highestUnlockedLevel, unlockedTarget).coerceIn(1L, 100L)
            val coinReward = if (won) (10L + clampedStars * 5L) else 0L
            val newBonusCoins = (currentProfile.bonusCoins + coinReward).coerceIn(0L, 10_000_000L)

            docPath = userRef.path
            userRef.update(
                mapOf(
                    "highScore" to newHighScore,
                    "totalStars" to updatedStarsTotal,
                    "highestUnlockedLevel" to newUnlockedLevel,
                    "bonusCoins" to newBonusCoins,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun claimDailyReward(todayIso: String, currentCoins: Long, rewardAmount: Long = 50L): Result<Unit> {
        val safeDay = todayIso.trim().take(20)
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val updatedCoins = (currentCoins + rewardAmount).coerceIn(0L, 10_000_000L)
            userRef.update(
                mapOf(
                    "bonusCoins" to updatedCoins,
                    "lastClaimedDailyRewardDay" to safeDay,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun spendBonusCoins(currentCoins: Long, cost: Long): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            require(currentCoins >= cost && cost >= 0L) { "Insufficient bonus coins." }
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            userRef.update(
                mapOf(
                    "bonusCoins" to (currentCoins - cost).coerceIn(0L, 10_000_000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSettings(
        soundEnabled: Boolean,
        hapticsEnabled: Boolean,
        aimGuideExtended: Boolean
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            userRef.update(
                mapOf(
                    "soundEnabled" to soundEnabled,
                    "hapticsEnabled" to hapticsEnabled,
                    "aimGuideExtended" to aimGuideExtended,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }
}
