package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
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
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class LifeTimeRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ownerCrud_allCollections_succeedAndDeserializeWithIds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = LifeTimeRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile("Alice", ALICE_EMAIL, "", "UTC").getOrThrow()
        }
        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile().first { it != null }
        }
        assertEquals(uid, profile?.userId)

        val ageId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveAgeProfile(
                AgeProfile(name = "Alice", relation = "Self", birthDateMillis = 946684800000L, isSelf = true)
            ).getOrThrow()
        }
        val ages = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeAgeProfiles().first { list -> list.any { it.id == ageId } }
        }
        assertTrue(ages.first { it.id == ageId }.isSelf)

        val relId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveRelationship(
                Relationship(partnerName = "Partner ${UUID.randomUUID().toString().take(4)}", startDateMillis = 1644883200000L, isPrimary = true)
            ).getOrThrow()
        }
        val rels = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeRelationships().first { list -> list.any { it.id == relId } }
        }
        assertTrue(rels.first { it.id == relId }.isPrimary)

        val frId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveFriendship(
                Friendship(friendName = "Maya", isBestFriend = true, startDateMillis = 1577836800000L)
            ).getOrThrow()
        }
        assertTrue(frId.isNotBlank())

        val grpId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveGroup(
                FriendGroup(
                    name = "Squad",
                    creationDateMillis = 1567296000000L,
                    members = listOf(GroupMember("Alice", 1567296000000L))
                )
            ).getOrThrow()
        }
        assertTrue(grpId.isNotBlank())

        val momId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveMoment(
                LifeMoment(title = "First Walk", category = "first_date", eventDateMillis = 1650000000000L, isFavorite = true)
            ).getOrThrow()
        }
        assertTrue(momId.isNotBlank())

        val giftId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveGift(
                GiftItem(name = "Watch", occasion = "anniversary", direction = "giving", status = "planned", budget = 120.0)
            ).getOrThrow()
        }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveGift(
                GiftItem(id = giftId, name = "Watch", occasion = "anniversary", direction = "giving", status = "purchased", budget = 120.0)
            ).getOrThrow()
        }

        val evtId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveSpecialEvent(
                SpecialEvent(title = "Anniversary", category = "anniversary", eventDateMillis = 1644883200000L, isRecurringYearly = true)
            ).getOrThrow()
        }
        assertTrue(evtId.isNotBlank())

        val surId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveSurprise(
                SurprisePlan(title = "Dinner", occasion = "anniversary", celebrationDateMillis = 1771113600000L, decorations = listOf("Candles"))
            ).getOrThrow()
        }
        assertTrue(surId.isNotBlank())

        val letId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveLoveLetter(
                LoveLetter(title = "Future Us", category = "future_letter", body = "Always grateful.", isFavorite = true)
            ).getOrThrow()
        }
        assertTrue(letId.isNotBlank())

        val bktId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveBucketListItem(
                BucketListItem(title = "Aurora", category = "travel", isCompleted = false)
            ).getOrThrow()
        }
        assertTrue(bktId.isNotBlank())

        val notId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveNotificationSetting(
                NotificationSetting(title = "Daily Memory", category = "daily_memory", enabled = true)
            ).getOrThrow()
        }
        assertTrue(notId.isNotBlank())
    }

    @Test
    fun observeRelationships_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        try {
            withTimeout<Unit>(FLOW_TIMEOUT_MS) {
                firestore.collection("users").document("some_uid").collection("relationships")
                    .get()
                    .await()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated read")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
                ?: throw AssertionError("Expected FirebaseFirestoreException, got $e")
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }
    }

    @Test
    fun crossUserRead_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = LifeTimeRepository(firestore, auth)
        val giftId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveGift(
                GiftItem(name = "Secret Gift", occasion = "birthday", direction = "giving", status = "planned")
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        try {
            withTimeout<Unit>(FLOW_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("gifts").document(giftId)
                    .get()
                    .await()
            }
            fail("Expected PERMISSION_DENIED for cross-user access")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
                ?: throw AssertionError("Expected FirebaseFirestoreException, got $e")
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@lifetime.test"
        const val BOB_EMAIL = "bob@lifetime.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
