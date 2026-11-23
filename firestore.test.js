const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_player_123";
const BOB_UID = "bob_player_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, timestamp = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    displayName: "Bubble Hero",
    highScore: 0,
    totalStars: 0,
    highestUnlockedLevel: 1,
    bonusCoins: 100,
    lastClaimedDailyRewardDay: "",
    soundEnabled: true,
    hapticsEnabled: true,
    aimGuideExtended: true,
    createdAt: timestamp,
    updatedAt: timestamp,
  };
}

function validLevelPayload(uid, levelNumber, timestamp = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    levelNumber,
    bestScore: 1450,
    stars: 3,
    completed: true,
    attempts: 1,
    createdAt: timestamp,
    updatedAt: timestamp,
  };
}

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

test("Unauthenticated user: cannot read or write profiles or level progress", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("level_progress").get()
  );
});

test("Owner: can create and update own PlayerProfile with Kotlin-mirrored payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Create profile (matches ensureProfileInitialized)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );

  // Read own profile (matches observePlayerProfile)
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Update gameplay aggregates (matches recordLevelResult)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      highScore: 25876,
      totalStars: 36,
      highestUnlockedLevel: 13,
      bonusCoins: 245,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Update daily reward (matches claimDailyReward)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bonusCoins: 295,
      lastClaimedDailyRewardDay: "2026-11-22",
      updatedAt: new Date(Date.now() - 900),
    })
  );

  // Update settings (matches updateSettings)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      soundEnabled: false,
      hapticsEnabled: true,
      aimGuideExtended: false,
      updatedAt: new Date(Date.now() - 800),
    })
  );

  // Spend bonus coins (matches spendBonusCoins)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bonusCoins: 270,
      updatedAt: new Date(Date.now() - 700),
    })
  );
});

test("Owner: PlayerProfile rejects ghost fields, future timestamps, immutable createdAt mutation, and invalid bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Shadow/ghost field rejected on create (including forbidden @DocumentId `id`)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfilePayload(ALICE_UID, created),
      id: ALICE_UID,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfilePayload(ALICE_UID, created),
      isAdmin: true,
    })
  );

  // Tampered future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(
      validProfilePayload(ALICE_UID, new Date(Date.now() + 60000))
    )
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );

  // Immutable createdAt mutation rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Out-of-bounds level (> 100) rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      highScore: 100,
      totalStars: 3,
      highestUnlockedLevel: 101,
      bonusCoins: 100,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Listing top-level users collection is strictly forbidden
  await assertFails(aliceDb.collection("users").get());
});

test("Owner: can create, partial-update, and list own level_progress documents", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const levelRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("level_progress")
    .doc("level_12");

  await assertSucceeds(levelRef.set(validLevelPayload(ALICE_UID, 12, created)));

  // List subcollection (matches observeAllLevelProgress)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("level_progress").get()
  );

  // Partial update preserving levelNumber & createdAt
  await assertSucceeds(
    levelRef.update({
      bestScore: 2200,
      stars: 3,
      completed: true,
      attempts: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject mismatched levelDocId vs levelNumber
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("level_progress")
      .doc("level_5")
      .set(validLevelPayload(ALICE_UID, 6, created))
  );

  // Reject invalid stars > 3
  await assertFails(
    levelRef.update({
      bestScore: 3000,
      stars: 4,
      completed: true,
      attempts: 3,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation: Alice cannot read or modify Bob's profile or level progress", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("users").doc(BOB_UID).set(validProfilePayload(BOB_UID, created));
    await adminDb
      .collection("users")
      .doc(BOB_UID)
      .collection("level_progress")
      .doc("level_1")
      .set(validLevelPayload(BOB_UID, 1, created));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({
      bonusCoins: 999999,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("level_progress").get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("level_progress")
      .doc("level_1")
      .get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("level_progress")
      .doc("level_1")
      .update({
        bestScore: 9999,
        stars: 3,
        completed: true,
        attempts: 9,
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});
