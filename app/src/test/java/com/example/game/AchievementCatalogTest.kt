package com.example.game

import com.example.data.InventoryItem
import com.example.data.PlayerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementCatalogTest {

  @Test
  fun starterProfileHasZeroUnlockedAchievementsAndZeroPoints() {
    val starterProfile = PlayerProfile()
    val summary = AchievementCatalog.evaluate(starterProfile, emptyList())

    assertEquals(6, summary.totalCount)
    assertEquals(0, summary.unlockedCount)
    assertEquals(0, summary.totalPoints)
    assertEquals(180, summary.maxPoints)
  }

  @Test
  fun completingExplorationTasksUnlocksTaskBadgesAndAccumulatesPoints() {
    val profile =
      PlayerProfile(
        visitedNodeIds = listOf("forest_start", "forest_stream", "forest_guardian", "forest_shrine"),
        completedRegionIds = listOf("forest", "ruins"),
      )

    val summary = AchievementCatalog.evaluate(profile, emptyList())
    val unlockedIds = summary.unlockedAchievements.map { it.id }.toSet()

    assertTrue(unlockedIds.contains("first_step"))
    assertTrue(unlockedIds.contains("forest_hero"))
    assertTrue(unlockedIds.contains("ruins_solver"))
    assertFalse(unlockedIds.contains("summit_champion"))
    assertFalse(unlockedIds.contains("rare_finder"))
    assertEquals(10 + 25 + 35, summary.totalPoints)
  }

  @Test
  fun collectingRareItemsUnlocksRareFinderAndRelicCollector() {
    val profile = PlayerProfile()
    val consumablesOnly =
      listOf(
        InventoryItem(itemKey = "berry_sweet", name = "Beri Manis Rimba", category = "snack", quantity = 3L),
        InventoryItem(itemKey = "potion_herb", name = "Ramuan Herbal", category = "potion", quantity = 2L),
      )

    val consumableSummary = AchievementCatalog.evaluate(profile, consumablesOnly)
    assertEquals(0, consumableSummary.unlockedCount)
    assertEquals(0, consumableSummary.totalPoints)

    val oneRareItem =
      consumablesOnly +
        InventoryItem(itemKey = "key_moss", name = "Kunci Lumut", category = "key", quantity = 1L)
    val oneRareSummary = AchievementCatalog.evaluate(profile, oneRareItem)
    assertTrue(oneRareSummary.unlockedAchievements.any { it.id == "rare_finder" })
    assertFalse(oneRareSummary.unlockedAchievements.any { it.id == "relic_collector" })
    assertEquals(20, oneRareSummary.totalPoints)

    val threeRareItems =
      oneRareItem +
        listOf(
          InventoryItem(itemKey = "lantern_gold", name = "Lentera Emas", category = "relic", quantity = 1L),
          InventoryItem(itemKey = "crown_explorer", name = "Lencana Petualang Utama", category = "relic", quantity = 1L),
        )
    val collectorSummary = AchievementCatalog.evaluate(profile, threeRareItems)
    val unlockedIds = collectorSummary.unlockedAchievements.map { it.id }.toSet()
    assertTrue(unlockedIds.contains("rare_finder"))
    assertTrue(unlockedIds.contains("relic_collector"))
    assertTrue(unlockedIds.contains("summit_champion"))
    assertEquals(20 + 40 + 50, collectorSummary.totalPoints)
  }

  @Test
  fun findNewlyUnlockedReturnsOnlyFreshlyEarnedBadges() {
    val previousSummary = AchievementCatalog.evaluate(PlayerProfile(visitedNodeIds = listOf("forest_start", "forest_stream")), emptyList())
    val previousIds = previousSummary.unlockedAchievements.map { it.id }.toSet()

    val nextSummary =
      AchievementCatalog.evaluate(
        PlayerProfile(
          visitedNodeIds = listOf("forest_start", "forest_stream"),
          completedRegionIds = listOf("forest"),
        ),
        listOf(InventoryItem(itemKey = "lantern_gold", name = "Lentera Emas", category = "relic", quantity = 1L)),
      )

    val newlyUnlocked = AchievementCatalog.findNewlyUnlocked(previousIds, nextSummary)
    assertEquals(setOf("rare_finder", "forest_hero"), newlyUnlocked.map { it.id }.toSet())
  }
}
