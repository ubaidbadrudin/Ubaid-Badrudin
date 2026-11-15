package com.example.game

import com.example.data.PlayerProfile
import com.example.ui.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameProgressionEngineTest {

  @Test
  fun initialStorySample_containsValidNarrativeChoicesAndActionResults() {
    val sampleNodes = GameViewModel.createInitialStorySample()
    assertTrue(sampleNodes.size >= 2)

    val openingNode = sampleNodes.first()
    assertEquals("forest_start", openingNode.id)
    assertTrue(openingNode.narrative.isNotBlank())
    assertTrue(openingNode.choices.size >= 2)

    openingNode.choices.forEach { choice ->
      val result: StoryActionResult = choice.actionResult
      assertTrue(choice.label.isNotBlank())
      assertTrue(result.nextNodeId.isNotBlank())
      assertTrue(result.message.isNotBlank())
    }

    val berryChoice = openingNode.choices[1]
    assertNotNull(berryChoice.actionResult.itemReward)
    assertEquals("Beri Manis Rimba", berryChoice.actionResult.itemReward?.name)
  }

  @Test
  fun applyStoryChoice_updatesNodeCoinsXpAndVisitedNodes() {
    val profile = PlayerProfile(userId = "u1", level = 1L, xp = 0L, energy = 15L, coins = 25L)
    val startNode = GameViewModel.createInitialStorySample().first()
    val safeChoice = startNode.choices.first()

    val outcome = GameProgressionEngine.applyStoryChoice(profile, startNode, safeChoice)
    assertTrue(outcome is TransitionOutcome.Success)
    val success = outcome as TransitionOutcome.Success

    assertEquals("forest_stream", success.updatedProfile.currentNodeId)
    assertEquals(10L, success.updatedProfile.xp)
    assertEquals(33L, success.updatedProfile.coins)
    assertEquals(safeChoice.actionResult, success.actionResult)
    assertTrue(success.updatedProfile.visitedNodeIds.contains("forest_stream"))
  }

  @Test
  fun applyStoryChoice_levelsUpAndBoostsStatsWhenXpThresholdReached() {
    val profile =
      PlayerProfile(
        userId = "u1",
        level = 1L,
        xp = 45L,
        hp = 12L,
        maxHp = 30L,
        energy = 5L,
        maxEnergy = 15L,
      )
    val startNode = AdventureWorld.getNode("forest_start")
    val safeChoice = startNode.choices.first() // +10 XP -> 55 >= 50

    val outcome =
      GameProgressionEngine.applyStoryChoice(profile, startNode, safeChoice) as TransitionOutcome.Success

    assertTrue(outcome.leveledUp)
    assertEquals(2L, outcome.updatedProfile.level)
    assertEquals(5L, outcome.updatedProfile.xp)
    assertEquals(35L, outcome.updatedProfile.maxHp)
    assertEquals(35L, outcome.updatedProfile.hp)
    assertEquals(17L, outcome.updatedProfile.maxEnergy)
    assertEquals(17L, outcome.updatedProfile.energy)
  }

  @Test
  fun applyStoryChoice_returnsInsufficientEnergyWhenPlayerEnergyTooLow() {
    val profile = PlayerProfile(userId = "u1", energy = 0L)
    val startNode = AdventureWorld.getNode("forest_start")
    val berryChoice = startNode.choices[1] // energyCost = 1

    val outcome = GameProgressionEngine.applyStoryChoice(profile, startNode, berryChoice)
    assertTrue(outcome is TransitionOutcome.InsufficientEnergy)
    assertEquals(1L, (outcome as TransitionOutcome.InsufficientEnergy).requiredEnergy)
  }
}
