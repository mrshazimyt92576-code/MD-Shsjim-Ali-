const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_hdr_123";
const BOB_UID = "bob_hdr_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProjectPayload(uid, now) {
  return {
    userId: uid,
    title: "Sunset Dolby Master",
    sourceUri: "content://media/external/video/media/42",
    width: 3840,
    height: 2160,
    fps: 60,
    durationMs: 18500,
    status: "EDITING",
    presetName: "Dolby Cinema",
    highlightRecovery: 0.65,
    shadowLift: 0.45,
    dynamicContrast: 0.55,
    saturationVibrance: 0.52,
    brightnessExposure: 0.1,
    overallIntensity: 0.85,
    highlightEnabled: true,
    shadowEnabled: true,
    contrastEnabled: true,
    saturationEnabled: true,
    brightnessEnabled: true,
    intensityEnabled: true,
    lastExportResolution: "UHD_4K",
    lastExportFps: 60,
    createdAt: now,
    updatedAt: now,
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

test("Unauthenticated user cannot read or write profiles or projects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .set(validProjectPayload(ALICE_UID, now))
  );
});

test("Owner can create, update, list, and delete their profile and projects", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Shajim HDR",
      email: "mrshazimff@gmail.com",
      preferredLanguage: "en",
      themeMode: "obsidian",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "bn",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .set(validProjectPayload(ALICE_UID, now))
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .update({
        status: "EXPORTED",
        highlightRecovery: 0.8,
        lastExportUri: "content://media/external/video/media/99",
        updatedAt: new Date(Date.now() - 500),
      })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .delete()
  );
});

test("Rejects ghost fields, @DocumentId id field, tampered timestamps, and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_alice")
      .set(validProjectPayload(ALICE_UID, now))
  );

  // Reject ghost field
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_ghost")
      .set({ ...validProjectPayload(ALICE_UID, now), isAdmin: true })
  );

  // Reject @DocumentId `id` field
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_id")
      .set({ ...validProjectPayload(ALICE_UID, now), id: "proj_id" })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_future")
      .set(validProjectPayload(ALICE_UID, new Date(Date.now() + 60000)))
  );

  // Reject immutable createdAt update
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_alice")
      .update({
        createdAt: new Date(Date.now() - 100),
        updatedAt: new Date(Date.now() - 100),
      })
  );

  // Reject cross-user read, list, update, delete
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_alice")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_alice")
      .update({ title: "Hijacked" })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_alice")
      .delete()
  );
});
