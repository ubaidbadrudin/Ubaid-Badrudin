package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class PlayerProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val heroName: String = "Petualang Baru",
  val avatarEmoji: String = "🧭",
  val level: Long = 1L,
  val xp: Long = 0L,
  val hp: Long = 30L,
  val maxHp: Long = 30L,
  val energy: Long = 15L,
  val maxEnergy: Long = 15L,
  val coins: Long = 20L,
  val currentRegionId: String = "forest",
  val currentNodeId: String = "forest_start",
  val visitedNodeIds: List<String> = listOf("forest_start"),
  val completedRegionIds: List<String> = emptyList(),
  val tutorialDone: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class InventoryItem(
  @DocumentId val id: String = "",
  val itemKey: String = "",
  val name: String = "",
  val description: String = "",
  val category: String = "potion",
  val quantity: Long = 1L,
  val healAmount: Long = 0L,
  val energyAmount: Long = 0L,
  val iconKey: String = "potion",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class JournalEntry(
  @DocumentId val id: String = "",
  val title: String = "",
  val summary: String = "",
  val regionId: String = "forest",
  val rewardXp: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

/**
 * Snapshot progres lengkap pemain untuk disimpan ke Firestore dalam satu transaksi batch:
 * mencakup data lokasi saat ini, status inventaris, dan poin statistik pemain.
 */
data class PlayerProgressSnapshot(
  val userId: String = "",
  val heroName: String = "Petualang Baru",
  val avatarEmoji: String = "🧭",
  val currentRegionId: String = "forest",
  val currentNodeId: String = "forest_start",
  val visitedNodeIds: List<String> = listOf("forest_start"),
  val completedRegionIds: List<String> = emptyList(),
  val inventoryItems: List<InventoryItem> = emptyList(),
  val level: Long = 1L,
  val xp: Long = 0L,
  val hp: Long = 30L,
  val maxHp: Long = 30L,
  val energy: Long = 15L,
  val maxEnergy: Long = 15L,
  val coins: Long = 20L,
  val tutorialDone: Boolean = false,
)

fun PlayerProfile.toProgressSnapshot(inventoryItems: List<InventoryItem>): PlayerProgressSnapshot =
  PlayerProgressSnapshot(
    userId = userId,
    heroName = heroName,
    avatarEmoji = avatarEmoji,
    currentRegionId = currentRegionId,
    currentNodeId = currentNodeId,
    visitedNodeIds = visitedNodeIds,
    completedRegionIds = completedRegionIds,
    inventoryItems = inventoryItems,
    level = level,
    xp = xp,
    hp = hp,
    maxHp = maxHp,
    energy = energy,
    maxEnergy = maxEnergy,
    coins = coins,
    tutorialDone = tutorialDone,
  )
