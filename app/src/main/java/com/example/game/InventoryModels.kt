package com.example.game

import com.example.data.InventoryItem

/**
 * Kategori penyaringan isi Tas Petualang pada sistem inventaris.
 */
enum class InventoryCategory(val key: String, val displayLabel: String) {
  ALL("all", "Semua"),
  CONSUMABLE("consumable", "Bekal"),
  KEY("key", "Kunci"),
  RELIC("relic", "Relik"),
}

/**
 * Mencatat item yang baru saja dikumpulkan pemain selama perjalanan.
 */
data class CollectedItemEvent(
  val itemKey: String,
  val itemName: String,
  val quantityAdded: Long,
  val iconKey: String,
  val sourceLabel: String = "Perjalanan",
)

/**
 * Status sistem inventaris pemain yang dikelola oleh [com.example.ui.GameViewModel].
 * Menyimpan daftar item yang terkumpul, kategori filter aktif, ringkasan jumlah,
 * serta notifikasi item terbaru yang didapat selama petualangan.
 */
data class InventoryState(
  val items: List<InventoryItem> = emptyList(),
  val selectedCategory: InventoryCategory = InventoryCategory.ALL,
  val lastCollectedItem: CollectedItemEvent? = null,
  val isLoading: Boolean = false,
) {
  val totalItemQuantity: Long
    get() = items.sumOf { it.quantity }

  val uniqueItemCount: Int
    get() = items.size

  val consumableQuantity: Long
    get() = items.filter { it.isConsumable() }.sumOf { it.quantity }

  val keyAndRelicQuantity: Long
    get() = items.filter { it.category == "key" || it.category == "relic" }.sumOf { it.quantity }

  val filteredItems: List<InventoryItem>
    get() =
      when (selectedCategory) {
        InventoryCategory.ALL -> items
        InventoryCategory.CONSUMABLE -> items.filter { it.isConsumable() }
        InventoryCategory.KEY -> items.filter { it.category == "key" }
        InventoryCategory.RELIC -> items.filter { it.category == "relic" }
      }

  fun hasItem(itemKey: String): Boolean = items.any { it.itemKey == itemKey && it.quantity > 0L }

  fun quantityOf(itemKey: String): Long = items.firstOrNull { it.itemKey == itemKey }?.quantity ?: 0L
}

fun InventoryItem.isConsumable(): Boolean =
  healAmount > 0L || energyAmount > 0L || category == "snack" || category == "potion"

fun ItemReward.toInventoryItem(): InventoryItem =
  InventoryItem(
    id = itemKey,
    itemKey = itemKey,
    name = name,
    description = description,
    category = category,
    quantity = quantity,
    healAmount = healAmount,
    energyAmount = energyAmount,
    iconKey = iconKey,
  )
