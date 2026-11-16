package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AgeProfile
import com.example.data.model.BucketListItem
import com.example.data.model.FriendGroup
import com.example.data.model.Friendship
import com.example.data.model.GiftItem
import com.example.data.model.LifeMoment
import com.example.data.model.LoveLetter
import com.example.data.model.NotificationSetting
import com.example.data.model.Relationship
import com.example.data.model.SpecialEvent
import com.example.data.model.SurprisePlan
import com.example.data.model.UserProfile
import com.example.data.repository.LifeTimeRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.TimeZone

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class LifeTimeViewModel(
    private val repository: LifeTimeRepository,
    val currentUserId: String,
    initialDisplayName: String,
    initialEmail: String,
    initialPhotoUrl: String
) : ViewModel() {

    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    val nowMillis: StateFlow<Long> = _nowMillis.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _recentlyViewed = MutableStateFlow<List<String>>(listOf("Age", "Love", "Memories"))
    val recentlyViewed: StateFlow<List<String>> = _recentlyViewed.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _activeCelebration = MutableStateFlow<CelebrationData?>(null)
    val activeCelebration: StateFlow<CelebrationData?> = _activeCelebration.asStateFlow()

    private val _exportedJson = MutableStateFlow<String?>(null)
    val exportedJson: StateFlow<String?> = _exportedJson.asStateFlow()

    val userProfile: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile()
        .toUiState("profile")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val ageProfiles: StateFlow<UiState<List<AgeProfile>>> = repository.observeAgeProfiles()
        .toUiState("age profiles")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val relationships: StateFlow<UiState<List<Relationship>>> = repository.observeRelationships()
        .toUiState("relationships")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val friendships: StateFlow<UiState<List<Friendship>>> = repository.observeFriendships()
        .toUiState("friendships")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val groups: StateFlow<UiState<List<FriendGroup>>> = repository.observeGroups()
        .toUiState("groups")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val moments: StateFlow<UiState<List<LifeMoment>>> = repository.observeMoments()
        .toUiState("moments")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val gifts: StateFlow<UiState<List<GiftItem>>> = repository.observeGifts()
        .toUiState("gifts")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val specialEvents: StateFlow<UiState<List<SpecialEvent>>> = repository.observeSpecialEvents()
        .toUiState("special events")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val surprises: StateFlow<UiState<List<SurprisePlan>>> = repository.observeSurprises()
        .toUiState("surprises")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val loveLetters: StateFlow<UiState<List<LoveLetter>>> = repository.observeLoveLetters()
        .toUiState("love letters")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val bucketList: StateFlow<UiState<List<BucketListItem>>> = repository.observeBucketList()
        .toUiState("bucket list")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val notificationSettings: StateFlow<UiState<List<NotificationSetting>>> = repository.observeNotificationSettings()
        .toUiState("notification settings")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                displayName = initialDisplayName,
                email = initialEmail,
                photoUrl = initialPhotoUrl,
                timezoneId = TimeZone.getDefault().id
            ).onFailure {
                Log.w(TAG, "Failed to ensure profile", it)
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                _nowMillis.value = System.currentTimeMillis()
            }
        }
    }

    private fun <T> Flow<T>.toUiState(label: String): Flow<UiState<T>> {
        return map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $label"))
            }
    }

    fun markUnlocked() {
        _isUnlocked.value = true
    }

    fun recordViewedSection(section: String) {
        val updated = (listOf(section) + _recentlyViewed.value.filterNot { it == section }).take(5)
        _recentlyViewed.value = updated
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun openCelebration(data: CelebrationData) {
        _activeCelebration.value = data
    }

    fun closeCelebration() {
        _activeCelebration.value = null
    }

    fun clearExportedJson() {
        _exportedJson.value = null
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.updateUserProfile(profile)
                .onSuccess { _statusMessage.value = "Settings synced to cloud" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update settings" }
        }
    }

    fun saveAgeProfile(item: AgeProfile) {
        viewModelScope.launch {
            repository.saveAgeProfile(item)
                .onSuccess { _statusMessage.value = "Age profile saved" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save age profile" }
        }
    }

    fun deleteAgeProfile(id: String) {
        viewModelScope.launch {
            repository.deleteAgeProfile(id)
                .onSuccess { _statusMessage.value = "Age profile deleted" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete" }
        }
    }

    fun saveRelationship(item: Relationship) {
        viewModelScope.launch {
            repository.saveRelationship(item)
                .onSuccess { _statusMessage.value = "Relationship counter saved" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save relationship" }
        }
    }

    fun deleteRelationship(id: String) {
        viewModelScope.launch {
            repository.deleteRelationship(id)
                .onSuccess { _statusMessage.value = "Relationship deleted" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete" }
        }
    }

    fun saveFriendship(item: Friendship) {
        viewModelScope.launch {
            repository.saveFriendship(item)
                .onSuccess { _statusMessage.value = "Friendship saved" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save friendship" }
        }
    }

    fun deleteFriendship(id: String) {
        viewModelScope.launch {
            repository.deleteFriendship(id)
                .onSuccess { _statusMessage.value = "Friendship deleted" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete" }
        }
    }

    fun saveGroup(item: FriendGroup) {
        viewModelScope.launch {
            repository.saveGroup(item)
                .onSuccess { _statusMessage.value = "Group counter saved" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save group" }
        }
    }

    fun deleteGroup(id: String) {
        viewModelScope.launch {
            repository.deleteGroup(id)
                .onSuccess { _statusMessage.value = "Group deleted" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete" }
        }
    }

    fun saveMoment(item: LifeMoment) {
        viewModelScope.launch {
            repository.saveMoment(item)
                .onSuccess { _statusMessage.value = "Memory preserved in cloud" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save memory" }
        }
    }

    fun deleteMoment(id: String) {
        viewModelScope.launch {
            repository.deleteMoment(id)
                .onSuccess { _statusMessage.value = "Memory deleted" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete" }
        }
    }

    fun saveGift(item: GiftItem) {
        viewModelScope.launch {
            repository.saveGift(item)
                .onSuccess { _statusMessage.value = "Gift saved" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save gift" }
        }
    }

    fun deleteGift(id: String) {
        viewModelScope.launch {
            repository.deleteGift(id)
                .onSuccess { _statusMessage.value = "Gift removed" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete" }
        }
    }

    fun saveSpecialEvent(item: SpecialEvent, onSaved: ((SpecialEvent) -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveSpecialEvent(item)
                .onSuccess { id ->
                    _statusMessage.value = "Special day saved"
                    onSaved?.invoke(item.copy(id = item.id.ifBlank { id }))
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save event" }
        }
    }

    fun deleteSpecialEvent(id: String) {
        viewModelScope.launch {
            repository.deleteSpecialEvent(id)
                .onSuccess { _statusMessage.value = "Event deleted" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete" }
        }
    }

    fun saveSurprise(item: SurprisePlan) {
        viewModelScope.launch {
            repository.saveSurprise(item)
                .onSuccess { _statusMessage.value = "Surprise plan saved" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save surprise" }
        }
    }

    fun deleteSurprise(id: String) {
        viewModelScope.launch {
            repository.deleteSurprise(id)
                .onSuccess { _statusMessage.value = "Surprise deleted" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete" }
        }
    }

    fun saveLoveLetter(item: LoveLetter) {
        viewModelScope.launch {
            repository.saveLoveLetter(item)
                .onSuccess { _statusMessage.value = "Love letter saved" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save letter" }
        }
    }

    fun deleteLoveLetter(id: String) {
        viewModelScope.launch {
            repository.deleteLoveLetter(id)
                .onSuccess { _statusMessage.value = "Letter deleted" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete" }
        }
    }

    fun saveBucketListItem(item: BucketListItem) {
        viewModelScope.launch {
            repository.saveBucketListItem(item)
                .onSuccess { _statusMessage.value = "Dream updated" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save dream" }
        }
    }

    fun deleteBucketListItem(id: String) {
        viewModelScope.launch {
            repository.deleteBucketListItem(id)
                .onSuccess { _statusMessage.value = "Dream removed" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete" }
        }
    }

    fun saveNotificationSetting(item: NotificationSetting) {
        viewModelScope.launch {
            repository.saveNotificationSetting(item)
                .onSuccess { _statusMessage.value = "Reminder schedule updated" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update reminder" }
        }
    }

    fun deleteNotificationSetting(id: String) {
        viewModelScope.launch {
            repository.deleteNotificationSetting(id)
                .onSuccess { _statusMessage.value = "Reminder removed" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete" }
        }
    }

    fun exportCloudBackup() {
        viewModelScope.launch {
            repository.exportAllDataToJson()
                .onSuccess { json -> _exportedJson.value = json }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Export failed" }
        }
    }

    fun restoreCloudBackup(json: String) {
        viewModelScope.launch {
            repository.restoreFromBackupJson(json)
                .onSuccess { count -> _statusMessage.value = "Restored $count items to cloud" }
                .onFailure { _statusMessage.value = "Invalid backup JSON: ${it.localizedMessage}" }
        }
    }

    fun deleteAllCloudData(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllUserData()
                .onSuccess { onDeleted() }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete account data" }
        }
    }

    private companion object {
        const val TAG = "LifeTimeVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}

data class CelebrationData(
    val title: String,
    val yearsCount: Int,
    val subtitle: String,
    val isBirthday: Boolean = false
)
