package com.example.game

import com.example.data.InventoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryStateTest {

  private val berry =
    InventoryItem(
      id = "berry_sweet",
      itemKey = "berry_sweet",
      name = "Beri Manis Rimba",
      description = "Memulihkan 10 HP dan 4 Energi.",
      category = "snack",
      quantity = 2L,
      healAmount = 10L,
      energyAmount = 4L,
      iconKey = "snack",
    )

  private val mossKey =
    InventoryItem(
      id = "key_moss",
      itemKey = "key_moss",
      name = "Kunci Lumut",
      description = "Kunci kuno berukir daun.",
      category = "key",
      quantity = 1L,
      healAmount = 0L,
      energyAmount = 0L,
      iconKey = "key",
    )

  private val starRelic =
    InventoryItem(
      id = "relic_star",
      itemKey = "relic_star",
      name = "Kompas Bintang",
      description = "Relik penunjuk arah puncak kristal.",
      category = "relic",
      quantity = 1L,
      healAmount = 0L,
      energyAmount = 0L,
      iconKey = "amulet",
    )

  @Test
  fun inventoryState_calculatesTotalsAndFiltersByCategory() {
    val state =
      InventoryState(
        items = listOf(berry, mossKey, starRelic),
        selectedCategory = InventoryCategory.ALL,
        lastCollectedItem =
          CollectedItemEvent(
            itemKey = mossKey.itemKey,
            itemName = mossKey.name,
            quantityAdded = 1L,
            iconKey = mossKey.iconKey,
            sourceLabel = "Tepi Sungai Berkilau",
          ),
      )

    assertEquals(4L, state.totalItemQuantity)
    assertEquals(3, state.uniqueItemCount)
    assertEquals(2L, state.consumableQuantity)
    assertEquals(2L, state.keyAndRelicQuantity)
    assertTrue(state.hasItem("key_moss"))
    assertFalse(state.hasItem("golden_lantern"))
    assertEquals(2L, state.quantityOf("berry_sweet"))

    assertEquals(1, state.copy(selectedCategory = InventoryCategory.CONSUMABLE).filteredItems.size)
    assertEquals(1, state.copy(selectedCategory = InventoryCategory.KEY).filteredItems.size)
    assertEquals(1, state.copy(selectedCategory = InventoryCategory.RELIC).filteredItems.size)
  }

  @Test
  fun itemReward_convertsToInventoryItemWithExpectedFields() {
    val reward =
      ItemReward(
        itemKey = "potion_forest",
        name = "Ramuan Daun Embun",
        description = "Memulihkan 20 HP.",
        category = "potion",
        quantity = 1L,
        healAmount = 20L,
        energyAmount = 0L,
        iconKey = "potion",
      )

    val item = reward.toInventoryItem()
    assertEquals("potion_forest", item.itemKey)
    assertEquals("Ramuan Daun Embun", item.name)
    assertTrue(item.isConsumable())
    assertEquals(20L, item.healAmount)
  }
}
