package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AgeProfile
import com.example.data.model.BucketListItem
import com.example.data.model.FriendGroup
import com.example.data.model.Friendship
import com.example.data.model.GiftItem
import com.example.data.model.GroupMember
import com.example.data.model.LifeMoment
import com.example.data.model.LoveLetter
import com.example.data.model.NotificationSetting
import com.example.data.model.Relationship
import com.example.data.model.SpecialEvent
import com.example.data.model.SurprisePlan
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
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
import org.json.JSONArray
import org.json.JSONObject

class LifeTimeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private inline fun <reified T> observeSubcollection(
        subcollection: String,
        crossinline sortSelector: (T) -> Long
    ): Flow<List<T>> = flow {
        val uid = requireUserId()
        val colRef = userDoc(uid).collection(subcollection)
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(T::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { sortSelector(it) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, colRef.path, uid)
                    throw error
                }
        )
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    // --- User Profile ---
    fun observeUserProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val docRef = userDoc(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, docRef.path, uid)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        displayName: String,
        email: String,
        photoUrl: String,
        timezoneId: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid)
        val snap = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
        val safeName = displayName.ifBlank { "LifeTime User" }.take(100)
        if (!snap.exists()) {
            val payload = mapOf(
                "userId" to uid,
                "displayName" to safeName,
                "email" to email.take(200),
                "photoUrl" to photoUrl.take(500),
                "birthDateMillis" to 0L,
                "birthTimeMinutes" to 0,
                "timezoneId" to timezoneId.take(80),
                "languageCode" to "en",
                "themeMode" to "romantic",
                "privacyPinHash" to "",
                "biometricEnabled" to false,
                "notificationsEnabled" to true,
                "dailyMemoryHour" to 9,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.set(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                throw e
            }
        }
    }

    suspend fun updateUserProfile(profile: UserProfile): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid)
        val updates = mapOf(
            "displayName" to profile.displayName.ifBlank { "LifeTime User" }.take(100),
            "email" to profile.email.take(200),
            "photoUrl" to profile.photoUrl.take(500),
            "birthDateMillis" to profile.birthDateMillis,
            "birthTimeMinutes" to profile.birthTimeMinutes.coerceIn(0, 1439),
            "timezoneId" to profile.timezoneId.take(80),
            "languageCode" to (if (profile.languageCode in listOf("en", "bn")) profile.languageCode else "en"),
            "themeMode" to (if (profile.themeMode in listOf("system", "light", "dark", "romantic")) profile.themeMode else "romantic"),
            "privacyPinHash" to profile.privacyPinHash.take(128),
            "biometricEnabled" to profile.biometricEnabled,
            "notificationsEnabled" to profile.notificationsEnabled,
            "dailyMemoryHour" to profile.dailyMemoryHour.coerceIn(0, 23),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Age Profiles ---
    fun observeAgeProfiles(): Flow<List<AgeProfile>> =
        observeSubcollection("ageProfiles") { if (it.isSelf) Long.MAX_VALUE else it.birthDateMillis }

    suspend fun saveAgeProfile(item: AgeProfile): Result<String> = saveSubdocument(
        subcollection = "ageProfiles",
        docId = item.id,
        createFields = { uid ->
            mapOf(
                "userId" to uid,
                "name" to item.name.trim().take(100),
                "relation" to item.relation.trim().take(60),
                "birthDateMillis" to item.birthDateMillis,
                "birthTimeMinutes" to item.birthTimeMinutes.coerceIn(0, 1439),
                "isSelf" to item.isSelf,
                "notes" to item.notes.take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        },
        updateFields = mapOf(
            "name" to item.name.trim().take(100),
            "relation" to item.relation.trim().take(60),
            "birthDateMillis" to item.birthDateMillis,
            "birthTimeMinutes" to item.birthTimeMinutes.coerceIn(0, 1439),
            "isSelf" to item.isSelf,
            "notes" to item.notes.take(500),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    )

    suspend fun deleteAgeProfile(id: String): Result<Unit> = deleteSubdocument("ageProfiles", id)

    // --- Relationships ---
    fun observeRelationships(): Flow<List<Relationship>> =
        observeSubcollection("relationships") { if (it.isPrimary) Long.MAX_VALUE else it.startDateMillis }

    suspend fun saveRelationship(item: Relationship): Result<String> = saveSubdocument(
        subcollection = "relationships",
        docId = item.id,
        createFields = { uid ->
            mapOf(
                "userId" to uid,
                "partnerName" to item.partnerName.trim().take(100),
                "partnerBirthdayMillis" to item.partnerBirthdayMillis,
                "partnerPhotoUri" to item.partnerPhotoUri.take(500),
                "startDateMillis" to item.startDateMillis,
                "firstMeetingMillis" to item.firstMeetingMillis,
                "firstConversationMillis" to item.firstConversationMillis,
                "firstMessageMillis" to item.firstMessageMillis,
                "firstPhoneCallMillis" to item.firstPhoneCallMillis,
                "firstVideoCallMillis" to item.firstVideoCallMillis,
                "firstDateMillis" to item.firstDateMillis,
                "firstProposalMillis" to item.firstProposalMillis,
                "firstKissMillis" to item.firstKissMillis,
                "firstGiftMillis" to item.firstGiftMillis,
                "engagementDateMillis" to item.engagementDateMillis,
                "weddingDateMillis" to item.weddingDateMillis,
                "storyNote" to item.storyNote.take(1000),
                "isPrimary" to item.isPrimary,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        },
        updateFields = mapOf(
            "partnerName" to item.partnerName.trim().take(100),
            "partnerBirthdayMillis" to item.partnerBirthdayMillis,
            "partnerPhotoUri" to item.partnerPhotoUri.take(500),
            "startDateMillis" to item.startDateMillis,
            "firstMeetingMillis" to item.firstMeetingMillis,
            "firstConversationMillis" to item.firstConversationMillis,
            "firstMessageMillis" to item.firstMessageMillis,
            "firstPhoneCallMillis" to item.firstPhoneCallMillis,
            "firstVideoCallMillis" to item.firstVideoCallMillis,
            "firstDateMillis" to item.firstDateMillis,
            "firstProposalMillis" to item.firstProposalMillis,
            "firstKissMillis" to item.firstKissMillis,
            "firstGiftMillis" to item.firstGiftMillis,
            "engagementDateMillis" to item.engagementDateMillis,
            "weddingDateMillis" to item.weddingDateMillis,
            "storyNote" to item.storyNote.take(1000),
            "isPrimary" to item.isPrimary,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    )

    suspend fun deleteRelationship(id: String): Result<Unit> = deleteSubdocument("relationships", id)

    // --- Friendships ---
    fun observeFriendships(): Flow<List<Friendship>> =
        observeSubcollection("friendships") { if (it.isBestFriend) Long.MAX_VALUE else it.startDateMillis }

    suspend fun saveFriendship(item: Friendship): Result<String> = saveSubdocument(
        subcollection = "friendships",
        docId = item.id,
        createFields = { uid ->
            mapOf(
                "userId" to uid,
                "friendName" to item.friendName.trim().take(100),
                "isBestFriend" to item.isBestFriend,
                "startDateMillis" to item.startDateMillis,
                "firstMeetingMillis" to item.firstMeetingMillis,
                "birthdayMillis" to item.birthdayMillis,
                "photoUri" to item.photoUri.take(500),
                "notes" to item.notes.take(1000),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        },
        updateFields = mapOf(
            "friendName" to item.friendName.trim().take(100),
            "isBestFriend" to item.isBestFriend,
            "startDateMillis" to item.startDateMillis,
            "firstMeetingMillis" to item.firstMeetingMillis,
            "birthdayMillis" to item.birthdayMillis,
            "photoUri" to item.photoUri.take(500),
            "notes" to item.notes.take(1000),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    )

    suspend fun deleteFriendship(id: String): Result<Unit> = deleteSubdocument("friendships", id)

    // --- Groups ---
    fun observeGroups(): Flow<List<FriendGroup>> =
        observeSubcollection("groups") { it.creationDateMillis }

    suspend fun saveGroup(item: FriendGroup): Result<String> {
        val memberMaps = item.members.take(30).map {
            mapOf("name" to it.name.take(80), "joinedAtMillis" to it.joinedAtMillis)
        }
        return saveSubdocument(
            subcollection = "groups",
            docId = item.id,
            createFields = { uid ->
                mapOf(
                    "userId" to uid,
                    "name" to item.name.trim().take(100),
                    "pictureUri" to item.pictureUri.take(500),
                    "creationDateMillis" to item.creationDateMillis,
                    "firstMeetingMillis" to item.firstMeetingMillis,
                    "members" to memberMaps,
                    "notes" to item.notes.take(1000),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            },
            updateFields = mapOf(
                "name" to item.name.trim().take(100),
                "pictureUri" to item.pictureUri.take(500),
                "creationDateMillis" to item.creationDateMillis,
                "firstMeetingMillis" to item.firstMeetingMillis,
                "members" to memberMaps,
                "notes" to item.notes.take(1000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
    }

    suspend fun deleteGroup(id: String): Result<Unit> = deleteSubdocument("groups", id)

    // --- Life Moments & Memories ---
    fun observeMoments(): Flow<List<LifeMoment>> =
        observeSubcollection("moments") { it.eventDateMillis }

    suspend fun saveMoment(item: LifeMoment): Result<String> = saveSubdocument(
        subcollection = "moments",
        docId = item.id,
        createFields = { uid ->
            mapOf(
                "userId" to uid,
                "title" to item.title.trim().take(150),
                "category" to item.category.ifBlank { "custom" }.take(50),
                "eventDateMillis" to item.eventDateMillis,
                "location" to item.location.take(150),
                "description" to item.description.take(2000),
                "notes" to item.notes.take(1000),
                "mediaUris" to item.mediaUris.take(10).map { it.take(500) },
                "mediaType" to item.mediaType.take(30),
                "albumName" to item.albumName.take(80),
                "isFavorite" to item.isFavorite,
                "reminderEnabled" to item.reminderEnabled,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        },
        updateFields = mapOf(
            "title" to item.title.trim().take(150),
            "category" to item.category.ifBlank { "custom" }.take(50),
            "eventDateMillis" to item.eventDateMillis,
            "location" to item.location.take(150),
            "description" to item.description.take(2000),
            "notes" to item.notes.take(1000),
            "mediaUris" to item.mediaUris.take(10).map { it.take(500) },
            "mediaType" to item.mediaType.take(30),
            "albumName" to item.albumName.take(80),
            "isFavorite" to item.isFavorite,
            "reminderEnabled" to item.reminderEnabled,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    )

    suspend fun deleteMoment(id: String): Result<Unit> = deleteSubdocument("moments", id)

    // --- Gifts ---
    fun observeGifts(): Flow<List<GiftItem>> =
        observeSubcollection("gifts") { it.givingDateMillis }

    suspend fun saveGift(item: GiftItem): Result<String> {
        val safeDir = if (item.direction in listOf("giving", "received", "wishlist")) item.direction else "giving"
        val safeStatus = if (item.status in listOf("planned", "purchased", "given")) item.status else "planned"
        return saveSubdocument(
            subcollection = "gifts",
            docId = item.id,
            createFields = { uid ->
                mapOf(
                    "userId" to uid,
                    "name" to item.name.trim().take(150),
                    "occasion" to item.occasion.ifBlank { "birthday" }.take(50),
                    "recipientName" to item.recipientName.take(100),
                    "giverName" to item.giverName.take(100),
                    "direction" to safeDir,
                    "status" to safeStatus,
                    "budget" to item.budget.coerceAtLeast(0.0),
                    "purchaseDateMillis" to item.purchaseDateMillis,
                    "givingDateMillis" to item.givingDateMillis,
                    "photoUri" to item.photoUri.take(500),
                    "notes" to item.notes.take(1000),
                    "reminderEnabled" to item.reminderEnabled,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            },
            updateFields = mapOf(
                "name" to item.name.trim().take(150),
                "occasion" to item.occasion.ifBlank { "birthday" }.take(50),
                "recipientName" to item.recipientName.take(100),
                "giverName" to item.giverName.take(100),
                "direction" to safeDir,
                "status" to safeStatus,
                "budget" to item.budget.coerceAtLeast(0.0),
                "purchaseDateMillis" to item.purchaseDateMillis,
                "givingDateMillis" to item.givingDateMillis,
                "photoUri" to item.photoUri.take(500),
                "notes" to item.notes.take(1000),
                "reminderEnabled" to item.reminderEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
    }

    suspend fun deleteGift(id: String): Result<Unit> = deleteSubdocument("gifts", id)

    // --- Special Events ---
    fun observeSpecialEvents(): Flow<List<SpecialEvent>> =
        observeSubcollection("specialEvents") { it.eventDateMillis }

    suspend fun saveSpecialEvent(item: SpecialEvent): Result<String> = saveSubdocument(
        subcollection = "specialEvents",
        docId = item.id,
        createFields = { uid ->
            mapOf(
                "userId" to uid,
                "title" to item.title.trim().take(120),
                "category" to item.category.ifBlank { "custom" }.take(50),
                "eventDateMillis" to item.eventDateMillis,
                "isRecurringYearly" to item.isRecurringYearly,
                "isFavorite" to item.isFavorite,
                "notify30Days" to item.notify30Days,
                "notify7Days" to item.notify7Days,
                "notify3Days" to item.notify3Days,
                "notify1Day" to item.notify1Day,
                "notifyOnDay" to item.notifyOnDay,
                "notifyHour" to item.notifyHour.coerceIn(0, 23),
                "notifyMinute" to item.notifyMinute.coerceIn(0, 59),
                "notes" to item.notes.take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        },
        updateFields = mapOf(
            "title" to item.title.trim().take(120),
            "category" to item.category.ifBlank { "custom" }.take(50),
            "eventDateMillis" to item.eventDateMillis,
            "isRecurringYearly" to item.isRecurringYearly,
            "isFavorite" to item.isFavorite,
            "notify30Days" to item.notify30Days,
            "notify7Days" to item.notify7Days,
            "notify3Days" to item.notify3Days,
            "notify1Day" to item.notify1Day,
            "notifyOnDay" to item.notifyOnDay,
            "notifyHour" to item.notifyHour.coerceIn(0, 23),
            "notifyMinute" to item.notifyMinute.coerceIn(0, 59),
            "notes" to item.notes.take(500),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    )

    suspend fun deleteSpecialEvent(id: String): Result<Unit> = deleteSubdocument("specialEvents", id)

    // --- Surprises ---
    fun observeSurprises(): Flow<List<SurprisePlan>> =
        observeSubcollection("surprises") { it.celebrationDateMillis }

    suspend fun saveSurprise(item: SurprisePlan): Result<String> = saveSubdocument(
        subcollection = "surprises",
        docId = item.id,
        createFields = { uid ->
            mapOf(
                "userId" to uid,
                "title" to item.title.trim().take(150),
                "occasion" to item.occasion.ifBlank { "romantic" }.take(50),
                "celebrationDateMillis" to item.celebrationDateMillis,
                "location" to item.location.take(150),
                "budget" to item.budget.coerceAtLeast(0.0),
                "giftIdea" to item.giftIdea.take(200),
                "cakeAndFood" to item.cakeAndFood.take(300),
                "guestList" to item.guestList.take(30),
                "decorations" to item.decorations.take(30),
                "completedDecorations" to item.completedDecorations.take(30),
                "privateNotes" to item.privateNotes.take(1000),
                "reminderEnabled" to item.reminderEnabled,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        },
        updateFields = mapOf(
            "title" to item.title.trim().take(150),
            "occasion" to item.occasion.ifBlank { "romantic" }.take(50),
            "celebrationDateMillis" to item.celebrationDateMillis,
            "location" to item.location.take(150),
            "budget" to item.budget.coerceAtLeast(0.0),
            "giftIdea" to item.giftIdea.take(200),
            "cakeAndFood" to item.cakeAndFood.take(300),
            "guestList" to item.guestList.take(30),
            "decorations" to item.decorations.take(30),
            "completedDecorations" to item.completedDecorations.take(30),
            "privateNotes" to item.privateNotes.take(1000),
            "reminderEnabled" to item.reminderEnabled,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    )

    suspend fun deleteSurprise(id: String): Result<Unit> = deleteSubdocument("surprises", id)

    // --- Love Letters ---
    fun observeLoveLetters(): Flow<List<LoveLetter>> =
        observeSubcollection("loveLetters") { it.scheduledDateMillis }

    suspend fun saveLoveLetter(item: LoveLetter): Result<String> = saveSubdocument(
        subcollection = "loveLetters",
        docId = item.id,
        createFields = { uid ->
            mapOf(
                "userId" to uid,
                "title" to item.title.trim().take(150),
                "category" to item.category.ifBlank { "love_letter" }.take(50),
                "recipient" to item.recipient.take(100),
                "body" to item.body.trim().take(5000),
                "scheduledDateMillis" to item.scheduledDateMillis,
                "isLockedUntilDate" to item.isLockedUntilDate,
                "isFavorite" to item.isFavorite,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        },
        updateFields = mapOf(
            "title" to item.title.trim().take(150),
            "category" to item.category.ifBlank { "love_letter" }.take(50),
            "recipient" to item.recipient.take(100),
            "body" to item.body.trim().take(5000),
            "scheduledDateMillis" to item.scheduledDateMillis,
            "isLockedUntilDate" to item.isLockedUntilDate,
            "isFavorite" to item.isFavorite,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    )

    suspend fun deleteLoveLetter(id: String): Result<Unit> = deleteSubdocument("loveLetters", id)

    // --- Bucket List ---
    fun observeBucketList(): Flow<List<BucketListItem>> =
        observeSubcollection("bucketList") { if (it.isCompleted) 0L else Long.MAX_VALUE }

    suspend fun saveBucketListItem(item: BucketListItem): Result<String> = saveSubdocument(
        subcollection = "bucketList",
        docId = item.id,
        createFields = { uid ->
            mapOf(
                "userId" to uid,
                "title" to item.title.trim().take(150),
                "category" to item.category.ifBlank { "activity" }.take(50),
                "targetDateMillis" to item.targetDateMillis,
                "isCompleted" to item.isCompleted,
                "completedDateMillis" to item.completedDateMillis,
                "notes" to item.notes.take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        },
        updateFields = mapOf(
            "title" to item.title.trim().take(150),
            "category" to item.category.ifBlank { "activity" }.take(50),
            "targetDateMillis" to item.targetDateMillis,
            "isCompleted" to item.isCompleted,
            "completedDateMillis" to item.completedDateMillis,
            "notes" to item.notes.take(500),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    )

    suspend fun deleteBucketListItem(id: String): Result<Unit> = deleteSubdocument("bucketList", id)

    // --- Notification Settings ---
    fun observeNotificationSettings(): Flow<List<NotificationSetting>> =
        observeSubcollection("notificationSettings") { it.targetDateMillis }

    suspend fun saveNotificationSetting(item: NotificationSetting): Result<String> = saveSubdocument(
        subcollection = "notificationSettings",
        docId = item.id,
        createFields = { uid ->
            mapOf(
                "userId" to uid,
                "title" to item.title.trim().take(120),
                "category" to item.category.ifBlank { "custom" }.take(50),
                "targetDateMillis" to item.targetDateMillis,
                "hour" to item.hour.coerceIn(0, 23),
                "minute" to item.minute.coerceIn(0, 59),
                "repeatYearly" to item.repeatYearly,
                "enabled" to item.enabled,
                "soundName" to item.soundName.take(60),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        },
        updateFields = mapOf(
            "title" to item.title.trim().take(120),
            "category" to item.category.ifBlank { "custom" }.take(50),
            "targetDateMillis" to item.targetDateMillis,
            "hour" to item.hour.coerceIn(0, 23),
            "minute" to item.minute.coerceIn(0, 59),
            "repeatYearly" to item.repeatYearly,
            "enabled" to item.enabled,
            "soundName" to item.soundName.take(60),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    )

    suspend fun deleteNotificationSetting(id: String): Result<Unit> = deleteSubdocument("notificationSettings", id)

    // --- Generic Subcollection Helpers ---
    private suspend fun saveSubdocument(
        subcollection: String,
        docId: String,
        createFields: (String) -> Map<String, Any>,
        updateFields: Map<String, Any>
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = userDoc(uid).collection(subcollection)
        if (docId.isBlank()) {
            val docRef = colRef.document()
            try {
                docRef.set(createFields(uid)).await()
                docRef.id
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                throw e
            }
        } else {
            val docRef = colRef.document(docId)
            try {
                docRef.update(updateFields).await()
                docRef.id
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        }
    }

    private suspend fun deleteSubdocument(subcollection: String, docId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(docId.isNotBlank()) { "Document ID cannot be blank for delete" }
        val docRef = userDoc(uid).collection(subcollection).document(docId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Backup Export, Restore & Account Data Deletion ---
    suspend fun exportAllDataToJson(): Result<String> = runCatching {
        val uid = requireUserId()
        val root = JSONObject()
        val subcollections = listOf(
            "ageProfiles", "relationships", "friendships", "groups",
            "moments", "gifts", "specialEvents", "surprises",
            "loveLetters", "bucketList", "notificationSettings"
        )
        for (sub in subcollections) {
            val colRef = userDoc(uid).collection(sub)
            val snap = try {
                colRef.get().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.LIST, colRef.path)
                throw e
            }
            val arr = JSONArray()
            for (doc in snap.documents) {
                val obj = JSONObject()
                for ((k, v) in doc.data.orEmpty()) {
                    if (k != "createdAt" && k != "updatedAt" && k != "userId") {
                        when (v) {
                            is List<*> -> {
                                val listArr = JSONArray()
                                v.forEach { elem ->
                                    if (elem is Map<*, *>) {
                                        val mObj = JSONObject()
                                        elem.forEach { (mk, mv) -> mObj.put(mk.toString(), mv) }
                                        listArr.put(mObj)
                                    } else {
                                        listArr.put(elem)
                                    }
                                }
                                obj.put(k, listArr)
                            }
                            else -> obj.put(k, v)
                        }
                    }
                }
                arr.put(obj)
            }
            root.put(sub, arr)
        }
        root.toString(2)
    }

    suspend fun restoreFromBackupJson(jsonString: String): Result<Int> = runCatching {
        val uid = requireUserId()
        val root = JSONObject(jsonString)
        var restoredCount = 0
        val subcollections = listOf(
            "ageProfiles", "relationships", "friendships", "groups",
            "moments", "gifts", "specialEvents", "surprises",
            "loveLetters", "bucketList", "notificationSettings"
        )
        for (sub in subcollections) {
            if (!root.has(sub)) continue
            val arr = root.getJSONArray(sub)
            val colRef = userDoc(uid).collection(sub)
            for (i in 0 until arr.length()) {
                val itemObj = arr.getJSONObject(i)
                val map = mutableMapOf<String, Any>()
                val keys = itemObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val valObj = itemObj.get(key)
                    if (valObj is JSONArray) {
                        val list = mutableListOf<Any>()
                        for (j in 0 until valObj.length()) {
                            val elem = valObj.get(j)
                            if (elem is JSONObject) {
                                val m = mutableMapOf<String, Any>()
                                val mKeys = elem.keys()
                                while (mKeys.hasNext()) {
                                    val mk = mKeys.next()
                                    m[mk] = elem.get(mk)
                                }
                                list.add(m)
                            } else {
                                list.add(elem)
                            }
                        }
                        map[key] = list
                    } else if (valObj != JSONObject.NULL) {
                        map[key] = valObj
                    }
                }
                map["userId"] = uid
                map["createdAt"] = FieldValue.serverTimestamp()
                map["updatedAt"] = FieldValue.serverTimestamp()
                val docRef = colRef.document()
                try {
                    docRef.set(map).await()
                    restoredCount++
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.CREATE, docRef.path)
                    throw e
                }
            }
        }
        restoredCount
    }

    suspend fun deleteAllUserData(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val subcollections = listOf(
            "ageProfiles", "relationships", "friendships", "groups",
            "moments", "gifts", "specialEvents", "surprises",
            "loveLetters", "bucketList", "notificationSettings"
        )
        for (sub in subcollections) {
            val colRef = userDoc(uid).collection(sub)
            val snap = try {
                colRef.get().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.LIST, colRef.path)
                throw e
            }
            for (doc in snap.documents) {
                try {
                    doc.reference.delete().await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.DELETE, doc.reference.path)
                    throw e
                }
            }
        }
        try {
            userDoc(uid).delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, userDoc(uid).path)
            throw e
        }
    }
}
