package com.example.game

import com.example.data.PlayerProfile

sealed interface TransitionOutcome {
  data class InsufficientEnergy(val requiredEnergy: Long) : TransitionOutcome

  data class Success(
    val updatedProfile: PlayerProfile,
    val nextNode: StoryNode,
    val actionResult: StoryActionResult,
    val leveledUp: Boolean,
    val regionCompleted: Boolean,
  ) : TransitionOutcome
}

object GameProgressionEngine {

  fun applyStoryChoice(
    profile: PlayerProfile,
    currentNode: StoryNode,
    choice: StoryChoice,
    resolveNode: (String) -> StoryNode = AdventureWorld::getNode,
  ): TransitionOutcome {
    val result = choice.actionResult
    val energyCost = result.energyCost.toLong()
    if (profile.energy < energyCost) {
      return TransitionOutcome.InsufficientEnergy(requiredEnergy = energyCost)
    }

    var nextLevel = profile.level
    var nextXp = profile.xp + result.xpReward.toLong()
    var nextMaxHp = profile.maxHp
    var nextMaxEnergy = profile.maxEnergy
    var nextHp = (profile.hp + result.hpDelta.toLong()).coerceIn(1L, nextMaxHp)
    var nextEnergy =
      if (result.hpDelta >= 20) nextMaxEnergy
      else (profile.energy - energyCost).coerceIn(0L, nextMaxEnergy)
    var leveledUp = false

    while (nextXp >= AdventureWorld.xpRequiredForNextLevel(nextLevel) && nextLevel < 99L) {
      nextXp -= AdventureWorld.xpRequiredForNextLevel(nextLevel)
      nextLevel += 1L
      nextMaxHp += 5L
      nextMaxEnergy += 2L
      nextHp = nextMaxHp
      nextEnergy = nextMaxEnergy
      leveledUp = true
    }

    val nextCoins = (profile.coins + result.coinReward.toLong()).coerceAtLeast(0L)
    val nextNode = resolveNode(result.nextNodeId)
    val nextVisited = (profile.visitedNodeIds + currentNode.id + nextNode.id).distinct()
    val regionCompleted = result.completesRegionId != null
    val nextCompletedRegions =
      if (result.completesRegionId != null) {
        (profile.completedRegionIds + result.completesRegionId).distinct()
      } else {
        profile.completedRegionIds
      }

    val updatedProfile =
      profile.copy(
        level = nextLevel,
        xp = nextXp,
        hp = nextHp,
        maxHp = nextMaxHp,
        energy = nextEnergy,
        maxEnergy = nextMaxEnergy,
        coins = nextCoins,
        currentRegionId = nextNode.regionId,
        currentNodeId = nextNode.id,
        visitedNodeIds = nextVisited,
        completedRegionIds = nextCompletedRegions,
        tutorialDone = true,
      )

    return TransitionOutcome.Success(
      updatedProfile = updatedProfile,
      nextNode = nextNode,
      actionResult = result,
      leveledUp = leveledUp,
      regionCompleted = regionCompleted,
    )
  }
}
