package com.example.game

import com.example.data.InventoryItem
import com.example.data.PlayerProfile

enum class AchievementCategory(val label: String) {
  TASK("Tugas Petualangan"),
  RARE_ITEM("Item Langka"),
}

data class AchievementBadge(
  val id: String,
  val title: String,
  val description: String,
  val badgeEmoji: String,
  val points: Int,
  val category: AchievementCategory,
  val isUnlocked: Boolean = false,
)

data class AchievementSummary(
  val achievements: List<AchievementBadge> = emptyList(),
) {
  val unlockedAchievements: List<AchievementBadge>
    get() = achievements.filter { it.isUnlocked }

  val unlockedCount: Int
    get() = unlockedAchievements.size

  val totalCount: Int
    get() = achievements.size

  val totalPoints: Int
    get() = unlockedAchievements.sumOf { it.points }

  val maxPoints: Int
    get() = achievements.sumOf { it.points }
}

/**
 * Katalog pencapaian sederhana yang menilai tugas petualangan yang diselesaikan pemain
 * serta koleksi item kunci/relik langka di dalam inventaris.
 */
object AchievementCatalog {

  fun isRareItem(item: InventoryItem): Boolean =
    (item.category == "key" || item.category == "relic") && item.quantity > 0L

  fun evaluate(
    profile: PlayerProfile,
    inventory: List<InventoryItem>,
  ): AchievementSummary {
    val visitedCount = profile.visitedNodeIds.distinct().size
    val rareKeys = inventory.filter(::isRareItem).map { it.itemKey }.distinct()
    val completedRegions = profile.completedRegionIds.toSet()

    val badges =
      listOf(
        AchievementBadge(
          id = "first_step",
          title = "Langkah Pertama",
          description = "Menjelajahi minimal 2 titik lokasi petualangan.",
          badgeEmoji = "👣",
          points = 10,
          category = AchievementCategory.TASK,
          isUnlocked = visitedCount >= 2,
        ),
        AchievementBadge(
          id = "rare_finder",
          title = "Pemburu Item Langka",
          description = "Mengumpulkan minimal 1 kunci atau relik langka.",
          badgeEmoji = "💎",
          points = 20,
          category = AchievementCategory.RARE_ITEM,
          isUnlocked = rareKeys.isNotEmpty(),
        ),
        AchievementBadge(
          id = "forest_hero",
          title = "Penjaga Hutan Bisikan",
          description = "Menyelesaikan seluruh perjalanan di Hutan Bisikan.",
          badgeEmoji = "🌿",
          points = 25,
          category = AchievementCategory.TASK,
          isUnlocked = completedRegions.contains("forest"),
        ),
        AchievementBadge(
          id = "ruins_solver",
          title = "Penakluk Reruntuhan",
          description = "Menuntaskan misteri di wilayah Reruntuhan Kuno.",
          badgeEmoji = "🏛️",
          points = 35,
          category = AchievementCategory.TASK,
          isUnlocked = completedRegions.contains("ruins"),
        ),
        AchievementBadge(
          id = "relic_collector",
          title = "Kolektor Relik Sejati",
          description = "Mengumpulkan 3 jenis kunci atau relik langka yang berbeda.",
          badgeEmoji = "🏺",
          points = 40,
          category = AchievementCategory.RARE_ITEM,
          isUnlocked = rareKeys.size >= 3,
        ),
        AchievementBadge(
          id = "summit_champion",
          title = "Legenda Puncak Kristal",
          description = "Menyalakan suar di Puncak Kristal atau meraih Lencana Petualang Utama.",
          badgeEmoji = "👑",
          points = 50,
          category = AchievementCategory.TASK,
          isUnlocked =
            completedRegions.contains("summit") || rareKeys.contains("crown_explorer"),
        ),
      )

    return AchievementSummary(achievements = badges)
  }

  fun findNewlyUnlocked(
    previouslyUnlockedIds: Set<String>,
    currentSummary: AchievementSummary,
  ): List<AchievementBadge> =
    currentSummary.unlockedAchievements.filter { it.id !in previouslyUnlockedIds }
}
