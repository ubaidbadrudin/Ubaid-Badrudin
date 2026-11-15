package com.example.game

import androidx.annotation.DrawableRes

data class RegionInfo(
  val id: String,
  val name: String,
  val subtitle: String,
  val difficultyLabel: String,
  val recommendedLevel: Int,
  val startNodeId: String,
  val nodeIds: List<String>,
  @param:DrawableRes val imageRes: Int,
  val rewardSummary: String,
)

data class ItemReward(
  val itemKey: String,
  val name: String,
  val description: String,
  val category: String,
  val quantity: Long = 1L,
  val healAmount: Long = 0L,
  val energyAmount: Long = 0L,
  val iconKey: String = "relic",
)

data class RiddleChallenge(
  val question: String,
  val hint: String,
  val options: List<String>,
  val correctIndex: Int,
  val successText: String,
  val wrongHintText: String,
)

data class FriendlyEncounter(
  val enemyName: String,
  val enemyEmoji: String,
  val maxHp: Int,
  val enemyDamage: Int,
  val introLine: String,
  val peaceCostEnergy: Int = 2,
  val victoryText: String,
)

/**
 * Hasil aksi (outcome) setelah pemain memilih salah satu opsi cerita:
 * menentukan perpindahan tahap berikutnya, teks hasil, perubahan HP/Energi, hadiah XP/Koin/Item,
 * dan catatan jurnal opsional.
 */
data class StoryActionResult(
  val nextNodeId: String,
  val message: String,
  val energyCost: Int = 0,
  val hpDelta: Int = 0,
  val xpReward: Int = 10,
  val coinReward: Int = 5,
  val itemReward: ItemReward? = null,
  val completesRegionId: String? = null,
  val journalTitle: String? = null,
  val journalSummary: String? = null,
)

/**
 * Pilihan aksi yang ditampilkan sebagai tombol interaktif kepada pemain,
 * lengkap dengan hasil aksi ([actionResult]) saat dipilih.
 */
data class StoryChoice(
  val id: String,
  val label: String,
  val badge: String,
  val actionResult: StoryActionResult,
) {
  constructor(
    id: String,
    label: String,
    badge: String,
    energyCost: Int = 0,
    hpDelta: Int = 0,
    xpReward: Int = 10,
    coinReward: Int = 5,
    nextNodeId: String,
    resultMessage: String,
    itemReward: ItemReward? = null,
    completesRegionId: String? = null,
    journalTitle: String? = null,
    journalSummary: String? = null,
  ) : this(
    id = id,
    label = label,
    badge = badge,
    actionResult =
      StoryActionResult(
        nextNodeId = nextNodeId,
        message = resultMessage,
        energyCost = energyCost,
        hpDelta = hpDelta,
        xpReward = xpReward,
        coinReward = coinReward,
        itemReward = itemReward,
        completesRegionId = completesRegionId,
        journalTitle = journalTitle,
        journalSummary = journalSummary,
      ),
  )

  val energyCost: Int
    get() = actionResult.energyCost

  val hpDelta: Int
    get() = actionResult.hpDelta

  val xpReward: Int
    get() = actionResult.xpReward

  val coinReward: Int
    get() = actionResult.coinReward

  val nextNodeId: String
    get() = actionResult.nextNodeId

  val resultMessage: String
    get() = actionResult.message

  val itemReward: ItemReward?
    get() = actionResult.itemReward

  val completesRegionId: String?
    get() = actionResult.completesRegionId

  val journalTitle: String?
    get() = actionResult.journalTitle

  val journalSummary: String?
    get() = actionResult.journalSummary
}

/**
 * Struktur satu adegan/naskah cerita: memuat naskah utama, daftar pilihan aksi,
 * panduan pemula Pip, serta tantangan teka-teki atau pertemuan ramah opsional.
 */
data class StoryNode(
  val id: String,
  val regionId: String,
  val title: String,
  val speaker: String,
  val pipTip: String,
  val narrative: String,
  val choices: List<StoryChoice>,
  val riddle: RiddleChallenge? = null,
  val encounter: FriendlyEncounter? = null,
)

data class ShopItem(
  val itemKey: String,
  val name: String,
  val description: String,
  val category: String,
  val priceCoins: Long,
  val healAmount: Long,
  val energyAmount: Long,
  val iconKey: String,
)
