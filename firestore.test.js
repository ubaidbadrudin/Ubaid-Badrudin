const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 2000);
}

function makeValidProfile(uid, ts = pastTimestamp()) {
  return {
    userId: uid,
    heroName: "Ksatria Rimba",
    avatarEmoji: "🧭",
    level: 1,
    xp: 0,
    hp: 30,
    maxHp: 30,
    energy: 15,
    maxEnergy: 15,
    coins: 20,
    currentRegionId: "forest",
    currentNodeId: "forest_start",
    visitedNodeIds: ["forest_start"],
    completedRegionIds: [],
    tutorialDone: false,
    createdAt: ts,
    updatedAt: ts,
  };
}

function makeValidItem(ts = pastTimestamp()) {
  return {
    itemKey: "potion_herb",
    name: "Ramuan Herbal",
    description: "Memulihkan 12 HP saat bertualang.",
    category: "potion",
    quantity: 2,
    healAmount: 12,
    energyAmount: 0,
    iconKey: "potion",
    createdAt: ts,
    updatedAt: ts,
  };
}

function makeValidJournal(ts = pastTimestamp()) {
  return {
    title: "Langkah Pertama di Hutan Bisikan",
    summary: "Memulai petualangan bersama Pip si Burung Hantu.",
    regionId: "forest",
    rewardXp: 15,
    createdAt: ts,
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

test("Unauthenticated user cannot read or write profile, inventory, or journal", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(makeValidProfile(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("inventory").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("journal").get());
});

test("Owner can create, read, and update their PlayerProfile with valid action keys", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(profileRef.set(makeValidProfile(ALICE_UID)));
  await assertSucceeds(profileRef.get());

  await assertSucceeds(
    profileRef.update({
      heroName: "Sari Petualang",
      avatarEmoji: "🦊",
      tutorialDone: true,
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    profileRef.update({
      xp: 25,
      coins: 35,
      currentNodeId: "forest_stream",
      visitedNodeIds: ["forest_start", "forest_stream"],
      updatedAt: pastTimestamp(),
    })
  );
});

test("Profile rejects shadow update ghost fields, invalid HP, and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(makeValidProfile(ALICE_UID)));

  // Cross-user read & write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ coins: 999, updatedAt: pastTimestamp() }));

  // Ghost field denied
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 50,
      isAdmin: true,
      updatedAt: pastTimestamp(),
    })
  );

  // HP > maxHp denied
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      hp: 999,
      updatedAt: pastTimestamp(),
    })
  );

  // Spoofed userId on create denied
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set(makeValidProfile(ALICE_UID))
  );
});

test("Owner can CRUD inventory items and cross-user access is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const invCol = aliceDb.collection("users").doc(ALICE_UID).collection("inventory");
  const itemRef = invCol.doc("potion_herb");

  await assertSucceeds(itemRef.set(makeValidItem()));
  await assertSucceeds(invCol.get());
  await assertSucceeds(itemRef.update({ quantity: 3, updatedAt: pastTimestamp() }));

  // Bob cannot list or read Alice's inventory
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("inventory").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("inventory").doc("potion_herb").get());

  // Invalid category denied
  await assertFails(
    invCol.doc("bad_item").set({
      ...makeValidItem(),
      category: "weapon",
    })
  );

  await assertSucceeds(itemRef.delete());
});

test("Owner can create and list append-only journal entries; update is forbidden", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const journalCol = aliceDb.collection("users").doc(ALICE_UID).collection("journal");
  const entryRef = journalCol.doc("entry_1");

  await assertSucceeds(entryRef.set(makeValidJournal()));
  await assertSucceeds(journalCol.get());

  // Update is forbidden (append-only)
  await assertFails(entryRef.update({ title: "Modified" }));

  // Cross-user list/create denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("journal").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("journal").doc("entry_2").set(makeValidJournal())
  );
});
