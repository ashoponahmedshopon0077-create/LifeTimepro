package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val birthDateMillis: Long = 0L,
    val birthTimeMinutes: Int = 0,
    val timezoneId: String = "",
    val languageCode: String = "en",
    val themeMode: String = "romantic",
    val privacyPinHash: String = "",
    val biometricEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val dailyMemoryHour: Int = 9,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AgeProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val relation: String = "Self",
    val birthDateMillis: Long = 0L,
    val birthTimeMinutes: Int = 0,
    @get:PropertyName("isSelf")
    val isSelf: Boolean = false,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Relationship(
    @DocumentId val id: String = "",
    val userId: String = "",
    val partnerName: String = "",
    val partnerBirthdayMillis: Long = 0L,
    val partnerPhotoUri: String = "",
    val startDateMillis: Long = 0L,
    val firstMeetingMillis: Long = 0L,
    val firstConversationMillis: Long = 0L,
    val firstMessageMillis: Long = 0L,
    val firstPhoneCallMillis: Long = 0L,
    val firstVideoCallMillis: Long = 0L,
    val firstDateMillis: Long = 0L,
    val firstProposalMillis: Long = 0L,
    val firstKissMillis: Long = 0L,
    val firstGiftMillis: Long = 0L,
    val engagementDateMillis: Long = 0L,
    val weddingDateMillis: Long = 0L,
    val storyNote: String = "",
    @get:PropertyName("isPrimary")
    val isPrimary: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Friendship(
    @DocumentId val id: String = "",
    val userId: String = "",
    val friendName: String = "",
    @get:PropertyName("isBestFriend")
    val isBestFriend: Boolean = false,
    val startDateMillis: Long = 0L,
    val firstMeetingMillis: Long = 0L,
    val birthdayMillis: Long = 0L,
    val photoUri: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class GroupMember(
    val name: String = "",
    val joinedAtMillis: Long = 0L
)

@IgnoreExtraProperties
data class FriendGroup(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val pictureUri: String = "",
    val creationDateMillis: Long = 0L,
    val firstMeetingMillis: Long = 0L,
    val members: List<GroupMember> = emptyList(),
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LifeMoment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "custom",
    val eventDateMillis: Long = 0L,
    val location: String = "",
    val description: String = "",
    val notes: String = "",
    val mediaUris: List<String> = emptyList(),
    val mediaType: String = "none",
    val albumName: String = "General",
    @get:PropertyName("isFavorite")
    val isFavorite: Boolean = false,
    val reminderEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class GiftItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val occasion: String = "birthday",
    val recipientName: String = "",
    val giverName: String = "",
    val direction: String = "giving",
    val status: String = "planned",
    val budget: Double = 0.0,
    val purchaseDateMillis: Long = 0L,
    val givingDateMillis: Long = 0L,
    val photoUri: String = "",
    val notes: String = "",
    val reminderEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SpecialEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "custom",
    val eventDateMillis: Long = 0L,
    @get:PropertyName("isRecurringYearly")
    val isRecurringYearly: Boolean = true,
    @get:PropertyName("isFavorite")
    val isFavorite: Boolean = false,
    val notify30Days: Boolean = true,
    val notify7Days: Boolean = true,
    val notify3Days: Boolean = true,
    val notify1Day: Boolean = true,
    val notifyOnDay: Boolean = true,
    val notifyHour: Int = 9,
    val notifyMinute: Int = 0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SurprisePlan(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val occasion: String = "romantic",
    val celebrationDateMillis: Long = 0L,
    val location: String = "",
    val budget: Double = 0.0,
    val giftIdea: String = "",
    val cakeAndFood: String = "",
    val guestList: List<String> = emptyList(),
    val decorations: List<String> = emptyList(),
    val completedDecorations: List<String> = emptyList(),
    val privateNotes: String = "",
    val reminderEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LoveLetter(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "love_letter",
    val recipient: String = "",
    val body: String = "",
    val scheduledDateMillis: Long = 0L,
    @get:PropertyName("isLockedUntilDate")
    val isLockedUntilDate: Boolean = false,
    @get:PropertyName("isFavorite")
    val isFavorite: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BucketListItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "activity",
    val targetDateMillis: Long = 0L,
    @get:PropertyName("isCompleted")
    val isCompleted: Boolean = false,
    val completedDateMillis: Long = 0L,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NotificationSetting(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "custom",
    val targetDateMillis: Long = 0L,
    val hour: Int = 9,
    val minute: Int = 0,
    val repeatYearly: Boolean = true,
    val enabled: Boolean = true,
    val soundName: String = "default",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
