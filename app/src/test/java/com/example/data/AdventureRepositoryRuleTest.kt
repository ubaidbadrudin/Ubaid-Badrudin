package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.game.InventoryState
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdventureRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureStarterProfile_authenticatedOwner_createsProfileAndStarterData() = runBlocking {
    val uid = signInTestUser("alice_hero_${UUID.randomUUID()}@test.com")
    val repo = AdventureRepository(firestore)

    val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureStarterProfile("Bima Rimba").getOrThrow()
    }
    assertEquals(uid, profile.userId)
    assertEquals("Bima Rimba", profile.heroName)

    val items = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeInventory(uid).first { it.isNotEmpty() }
    }
    assertTrue(items.any { it.itemKey == "berry_sweet" })

    val journal = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeJournal(uid).first { it.isNotEmpty() }
    }
    assertTrue(journal.any { it.regionId == "forest" })
  }

  @Test
  fun saveProgressAndItems_authenticatedOwner_updatesProfileAndInventory() = runBlocking {
    val uid = signInTestUser("clara_hero_${UUID.randomUUID()}@test.com")
    val repo = AdventureRepository(firestore)

    withTimeout(DEFAULT_TIMEOUT_MS) { repo.ensureStarterProfile("Clara").getOrThrow() }

    val progressResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.saveProgressUpdate(
        level = 2L,
        xp = 15L,
        hp = 28L,
        maxHp = 35L,
        energy = 14L,
        maxEnergy = 18L,
        coins = 60L,
        currentRegionId = "ruins",
        currentNodeId = "ruins_gate",
        visitedNodeIds = listOf("forest_start", "ruins_gate"),
        completedRegionIds = listOf("forest"),
        tutorialDone = true,
      )
    }
    assertTrue(progressResult.isSuccess)

    val itemResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addOrIncrementItem(
        itemKey = "potion_herb",
        name = "Ramuan Herbal",
        description = "Memulihkan 12 HP.",
        category = "potion",
        quantityDelta = 1L,
        healAmount = 12L,
        energyAmount = 0L,
        iconKey = "potion",
      )
    }
    assertTrue(itemResult.isSuccess)

    val entryId = "entry_${UUID.randomUUID().toString().replace("-", "")}"
    val journalResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addJournalEntry(
        entryId = entryId,
        title = "Menyelesaikan Hutan Bisikan",
        summary = "Menemukan kunci lumut di tepi sungai.",
        regionId = "forest",
        rewardXp = 30L,
      )
    }
    assertTrue(journalResult.isSuccess)
  }

  @Test
  fun observePlayerProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser("alice_iso_${UUID.randomUUID()}@test.com")
    val aliceRepo = AdventureRepository(firestore)
    withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.ensureStarterProfile("Alice").getOrThrow() }

    signInTestUser("bob_iso_${UUID.randomUUID()}@test.com")
    val bobRepo = AdventureRepository(firestore)

    var thrown: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observePlayerProfile(aliceUid).first() }
    } catch (e: Exception) {
      thrown = e
    }
    assertNotNull(thrown)
    val firestoreException = checkNotNull(thrown!!.unwrapFirestoreException()) {
      "Expected nested FirebaseFirestoreException, got $thrown"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
  }

  @Test
  fun observeInventory_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = AdventureRepository(firestore)

    var thrown: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeInventory("some_user_123").first() }
    } catch (e: Exception) {
      thrown = e
    }
    assertNotNull(thrown)
    val firestoreException = checkNotNull(thrown!!.unwrapFirestoreException()) {
      "Expected nested FirebaseFirestoreException, got $thrown"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
  }

  @Test
  fun savePlayerProgress_authenticatedOwner_persistsLocationInventoryAndStatsAtomically() =
    runBlocking {
      val uid = signInTestUser("dina_save_${UUID.randomUUID()}@test.com")
      val repo = AdventureRepository(firestore)

      val starter = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.ensureStarterProfile("Dina Penjelajah").getOrThrow()
      }

      val updatedProfile =
        starter.copy(
          level = 3L,
          xp = 42L,
          hp = 36L,
          maxHp = 40L,
          energy = 12L,
          maxEnergy = 18L,
          coins = 115L,
          currentRegionId = "ruins",
          currentNodeId = "ruins_puzzle",
          visitedNodeIds = listOf("forest_start", "forest_stream", "ruins_gate", "ruins_puzzle"),
          completedRegionIds = listOf("forest"),
          tutorialDone = true,
        )

      val updatedInventory =
        InventoryState(
          items =
            listOf(
              InventoryItem(
                id = "berry_sweet",
                itemKey = "berry_sweet",
                name = "Beri Manis Rimba",
                description = "Camilan segar pemula yang memulihkan 10 HP dan 4 Energi.",
                category = "snack",
                quantity = 5L,
                healAmount = 10L,
                energyAmount = 4L,
                iconKey = "snack",
              ),
              InventoryItem(
                id = "key_moss",
                itemKey = "key_moss",
                name = "Kunci Lumut",
                description = "Kunci kuno berukir daun dari jembatan Sungai Berkilau.",
                category = "key",
                quantity = 1L,
                iconKey = "key",
              ),
            ),
          isLoading = false,
        )

      val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.savePlayerProgress(updatedProfile, updatedInventory)
      }
      assertTrue(saveResult.isSuccess)

      val persistedProfile = withTimeout(FLOW_TIMEOUT_MS) {
        repo.observePlayerProfile(uid).first { it?.currentNodeId == "ruins_puzzle" }
      }
      assertNotNull(persistedProfile)
      assertEquals("ruins", persistedProfile!!.currentRegionId)
      assertEquals("ruins_puzzle", persistedProfile.currentNodeId)
      assertEquals(listOf("forest"), persistedProfile.completedRegionIds)
      assertEquals(3L, persistedProfile.level)
      assertEquals(42L, persistedProfile.xp)
      assertEquals(36L, persistedProfile.hp)
      assertEquals(40L, persistedProfile.maxHp)
      assertEquals(12L, persistedProfile.energy)
      assertEquals(18L, persistedProfile.maxEnergy)
      assertEquals(115L, persistedProfile.coins)

      val persistedInventory = withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeInventory(uid).first { list ->
          list.any { it.itemKey == "key_moss" } &&
            list.firstOrNull { it.itemKey == "berry_sweet" }?.quantity == 5L
        }
      }
      assertEquals(2, persistedInventory.size)
      assertEquals(5L, persistedInventory.first { it.itemKey == "berry_sweet" }.quantity)
      assertEquals(1L, persistedInventory.first { it.itemKey == "key_moss" }.quantity)
    }

  @Test
  fun savePlayerProgress_unauthenticatedUser_returnsFailure() = runBlocking {
    auth.signOut()
    val repo = AdventureRepository(firestore)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.savePlayerProgress(
        profile = PlayerProfile(currentRegionId = "forest", currentNodeId = "forest_start"),
        inventoryState = InventoryState(),
      )
    }
    assertTrue(result.isFailure)
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
