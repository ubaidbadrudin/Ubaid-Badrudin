package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.InventoryItem
import com.example.data.PlayerProfile
import com.example.game.AdventureWorld
import com.example.game.InventoryCategory
import com.example.game.InventoryState
import com.example.game.ShopItem
import com.example.ui.theme.GoldCoin

@Composable
fun BagTabScreen(
  profile: PlayerProfile,
  inventoryState: InventoryState,
  isMutating: Boolean,
  banner: FeedbackBanner?,
  onDismissBanner: () -> Unit,
  onSelectCategory: (InventoryCategory) -> Unit,
  onDismissCollectedNotice: () -> Unit,
  onUseItem: (InventoryItem) -> Unit,
  onBuyShopItem: (ShopItem) -> Unit,
  onBackToAdventure: () -> Unit,
) {
  BackHandler { onBackToAdventure() }

  val displayedItems = inventoryState.filteredItems

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier.fillMaxSize(),
  ) {
    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Tas Petualang",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "Gunakan bekal kapan saja atau belanja di Toko Perkemahan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Surface(
          color = GoldCoin.copy(alpha = 0.16f),
          shape = RoundedCornerShape(50),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.MonetizationOn,
              contentDescription = "Jumlah Koin",
              tint = GoldCoin,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = "${profile.coins} Koin",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }

    item {
      BannerFeedbackCard(banner = banner, onDismiss = onDismissBanner)
    }

    inventoryState.lastCollectedItem?.let { collected ->
      item {
        Surface(
          color = MaterialTheme.colorScheme.tertiaryContainer,
          contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier.fillMaxWidth().testTag("last_collected_item_card"),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
          ) {
            Icon(
              imageVector = itemIconForKey(collected.iconKey),
              contentDescription = collected.itemName,
              modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Baru Dikumpulkan: +${collected.quantityAdded} ${collected.itemName}",
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = "Ditemukan di ${collected.sourceLabel}",
                style = MaterialTheme.typography.bodyMedium,
              )
            }
            IconButton(
              onClick = onDismissCollectedNotice,
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Tutup info item baru",
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }
    }

    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag("inventory_summary_card"),
      ) {
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
          InventoryStatColumn(label = "Total Item", value = "${inventoryState.totalItemQuantity}")
          InventoryStatColumn(label = "Bekal", value = "${inventoryState.consumableQuantity}")
          InventoryStatColumn(label = "Kunci & Relik", value = "${inventoryState.keyAndRelicQuantity}")
        }
      }
    }

    item {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      ) {
        InventoryCategory.entries.forEach { category ->
          FilterChip(
            selected = inventoryState.selectedCategory == category,
            onClick = { onSelectCategory(category) },
            label = { Text(category.displayLabel) },
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("bag_filter_${category.key}"),
          )
        }
      }
    }

    if (displayedItems.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(24.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Backpack,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(44.dp),
            )
            Text(
              text =
                if (inventoryState.items.isEmpty()) "Tas Masih Kosong"
                else "Belum Ada Item ${inventoryState.selectedCategory.displayLabel}",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text =
                if (inventoryState.items.isEmpty())
                  "Jelajahi kisah atau beli camilan segar di Toko Perkemahan di bawah!"
                else "Teruskan petualanganmu untuk mengumpulkan item kategori ini.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      items(displayedItems, key = { it.id.ifBlank { it.itemKey } }) { item ->
        InventoryItemCard(
          item = item,
          isMutating = isMutating,
          onUseClick = { onUseItem(item) },
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.ShoppingBag,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.secondary,
        )
        Text(
          text = "Toko Perkemahan",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }
    }

    items(AdventureWorld.shopItems, key = { it.itemKey }) { shopItem ->
      ShopItemCard(
        shopItem = shopItem,
        canAfford = profile.coins >= shopItem.priceCoins,
        isMutating = isMutating,
        onBuyClick = { onBuyShopItem(shopItem) },
      )
    }

    item { Spacer(modifier = Modifier.height(12.dp)) }
  }
}

@Composable
private fun InventoryItemCard(
  item: InventoryItem,
  isMutating: Boolean,
  onUseClick: () -> Unit,
) {
  val isConsumable = item.healAmount > 0L || item.energyAmount > 0L

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth().testTag("inventory_item_${item.itemKey}"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.padding(14.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color =
          if (isConsumable) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.secondaryContainer,
        contentColor =
          if (isConsumable) MaterialTheme.colorScheme.onPrimaryContainer
          else MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.size(48.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = itemIconForKey(item.iconKey),
            contentDescription = item.name,
            modifier = Modifier.size(24.dp),
          )
        }
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(50),
          ) {
            Text(
              text = "x${item.quantity}",
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
        }
        Text(
          text = item.description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (isConsumable) {
        Button(
          onClick = onUseClick,
          enabled = !isMutating,
          shape = RoundedCornerShape(14.dp),
          modifier =
            Modifier.minimumInteractiveComponentSize().testTag("use_item_${item.itemKey}"),
        ) {
          Text("Pakai", style = MaterialTheme.typography.labelLarge)
        }
      } else {
        Surface(
          color = MaterialTheme.colorScheme.tertiaryContainer,
          contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
          shape = RoundedCornerShape(50),
        ) {
          Text(
            text = "Koleksi",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun ShopItemCard(
  shopItem: ShopItem,
  canAfford: Boolean,
  isMutating: Boolean,
  onBuyClick: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier.fillMaxWidth().testTag("shop_item_${shopItem.itemKey}"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.padding(14.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.size(46.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = itemIconForKey(shopItem.iconKey),
            contentDescription = shopItem.name,
            modifier = Modifier.size(22.dp),
          )
        }
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = shopItem.name,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = shopItem.description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.width(4.dp))

      FilledTonalButton(
        onClick = onBuyClick,
        enabled = canAfford && !isMutating,
        shape = RoundedCornerShape(14.dp),
        modifier =
          Modifier.minimumInteractiveComponentSize().testTag("buy_item_${shopItem.itemKey}"),
      ) {
        Text("${shopItem.priceCoins} Koin", style = MaterialTheme.typography.labelLarge)
      }
    }
  }
}

@Composable
private fun InventoryStatColumn(
  label: String,
  value: String,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.primary,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
