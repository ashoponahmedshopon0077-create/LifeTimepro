const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const serverTimestamp = () => new Date();

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any LifeTime collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("relationships").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("moments").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("gifts").get());
});

test("Authenticated user can create, read, and update their own UserProfile, while cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      languageCode: "en",
      themeMode: "romantic",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).set({
    userId: BOB_UID,
    displayName: "Intruder",
    languageCode: "en",
    themeMode: "light",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));

  // Shadow update with ghost field must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Updated",
      isAdmin: true,
      updatedAt: serverTimestamp(),
    })
  );

  // Valid partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      languageCode: "bn",
      updatedAt: serverTimestamp(),
    })
  );
});

test("User-scoped subcollections enforce ownership, schema validation, and reject cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const aliceUser = aliceDb.collection("users").doc(ALICE_UID);

  // AgeProfile
  await assertSucceeds(
    aliceUser.collection("ageProfiles").doc("age_1").set({
      userId: ALICE_UID,
      name: "Me",
      birthDateMillis: 946684800000,
      isSelf: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Relationship
  await assertSucceeds(
    aliceUser.collection("relationships").doc("rel_1").set({
      userId: ALICE_UID,
      partnerName: "Aarav",
      startDateMillis: 1644883200000,
      isPrimary: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Friendship
  await assertSucceeds(
    aliceUser.collection("friendships").doc("fr_1").set({
      userId: ALICE_UID,
      friendName: "Maya",
      startDateMillis: 1577836800000,
      isBestFriend: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // FriendGroup
  await assertSucceeds(
    aliceUser.collection("groups").doc("grp_1").set({
      userId: ALICE_UID,
      name: "College Squad",
      creationDateMillis: 1567296000000,
      members: [{ name: "Alice", joinedAtMillis: 1567296000000 }],
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // LifeMoment
  await assertSucceeds(
    aliceUser.collection("moments").doc("mom_1").set({
      userId: ALICE_UID,
      title: "First Rainy Walk",
      category: "first_date",
      eventDateMillis: 1650000000000,
      mediaUris: ["content://media/1"],
      isFavorite: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // GiftItem
  await assertSucceeds(
    aliceUser.collection("gifts").doc("gift_1").set({
      userId: ALICE_UID,
      name: "Handwritten Watch",
      occasion: "anniversary",
      direction: "giving",
      status: "planned",
      budget: 150.0,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // SpecialEvent
  await assertSucceeds(
    aliceUser.collection("specialEvents").doc("evt_1").set({
      userId: ALICE_UID,
      title: "Our Anniversary",
      category: "anniversary",
      eventDateMillis: 1644883200000,
      isRecurringYearly: true,
      notify30Days: true,
      notifyOnDay: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // SurprisePlan
  await assertSucceeds(
    aliceUser.collection("surprises").doc("sur_1").set({
      userId: ALICE_UID,
      title: "Rooftop Dinner",
      occasion: "anniversary",
      celebrationDateMillis: 1771113600000,
      decorations: ["Fairy Lights", "Roses"],
      completedDecorations: [],
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // LoveLetter
  await assertSucceeds(
    aliceUser.collection("loveLetters").doc("let_1").set({
      userId: ALICE_UID,
      title: "For Year Five",
      category: "future_letter",
      body: "Always grateful for every second together.",
      isFavorite: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // BucketListItem
  await assertSucceeds(
    aliceUser.collection("bucketList").doc("bkt_1").set({
      userId: ALICE_UID,
      title: "Watch Northern Lights",
      category: "travel",
      isCompleted: false,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // NotificationSetting
  await assertSucceeds(
    aliceUser.collection("notificationSettings").doc("not_1").set({
      userId: ALICE_UID,
      title: "Daily Memory Reminder",
      category: "daily_memory",
      enabled: true,
      hour: 9,
      minute: 0,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Verify Bob cannot list or read Alice's subcollections
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("relationships").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("loveLetters").doc("let_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("gifts").doc("gift_1").delete());

  // Verify Alice can list her own subcollections
  await assertSucceeds(aliceUser.collection("relationships").get());
  await assertSucceeds(aliceUser.collection("loveLetters").get());
  await assertSucceeds(aliceUser.collection("gifts").get());

  // Verify invalid enum or spoofed userId is rejected
  await assertFails(
    aliceUser.collection("gifts").doc("gift_bad").set({
      userId: BOB_UID,
      name: "Spoof",
      occasion: "birthday",
      direction: "giving",
      status: "planned",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    aliceUser.collection("gifts").doc("gift_bad_status").set({
      userId: ALICE_UID,
      name: "Bad Status",
      occasion: "birthday",
      direction: "giving",
      status: "invalid_status",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});
