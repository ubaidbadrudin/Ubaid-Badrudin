package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.AudioSettingsState
import com.example.data.JournalEntry
import com.example.data.PlayerProfile
import com.example.game.AchievementSummary
import com.example.game.AdventureWorld
import com.example.game.InventoryState
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun JournalProfileTabScreen(
  profile: PlayerProfile,
  inventoryState: InventoryState,
  journal: List<JournalEntry>,
  isMutating: Boolean,
  lastSavedAtMillis: Long?,
  banner: FeedbackBanner?,
  onDismissBanner: () -> Unit,
  onSaveProgress: () -> Unit,
  onUpdateHeroIdentity: (String, String, Boolean) -> Unit,
  onResetAdventure: () -> Unit,
  onSignOut: () -> Unit,
  onBackToAdventure: () -> Unit,
  achievementSummary: AchievementSummary = AchievementSummary(),
  audioSettings: AudioSettingsState = AudioSettingsState(),
  onToggleBgm: () -> Unit = {},
  onToggleSfx: () -> Unit = {},
  onBgmVolumeChange: (Float) -> Unit = {},
  onSfxVolumeChange: (Float) -> Unit = {},
  onTestChoiceSfx: () -> Unit = {},
  onTestItemSfx: () -> Unit = {},
) {
  BackHandler { onBackToAdventure() }

  var showEditDialog by remember { mutableStateOf(false) }
  var showResetDialog by remember { mutableStateOf(false) }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier.fillMaxSize(),
  ) {
    item {
      Text(
        text = "Jurnal & Profil Petualang",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
    }

    item {
      BannerFeedbackCard(banner = banner, onDismiss = onDismissBanner)
    }

    // Hero Identity Card
    item {
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.weight(1f),
            ) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(text = profile.avatarEmoji, style = MaterialTheme.typography.headlineMedium)
                }
              }
              Column {
                Text(
                  text = profile.heroName,
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text =
                    "Level ${profile.level} • ${profile.completedRegionIds.size}/3 Wilayah • ${achievementSummary.unlockedCount} Badge (${achievementSummary.totalPoints} Poin)",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            IconButton(
              onClick = { showEditDialog = true },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("edit_hero_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Ubah Nama & Avatar",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }

          val activeRegion = AdventureWorld.getRegion(profile.currentRegionId)
          val activeNode = AdventureWorld.getNode(profile.currentNodeId)
          val lastSavedText =
            remember(lastSavedAtMillis) {
              lastSavedAtMillis?.let {
                val timeStr =
                  SimpleDateFormat("HH:mm:ss", Locale.forLanguageTag("id-ID")).format(Date(it))
                "Tersinkron pukul $timeStr"
              } ?: "Progres siap disimpan ke Firebase Cloud"
            }

          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.padding(12.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Outlined.CloudDone,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Text(text = lastSavedText, style = MaterialTheme.typography.labelLarge)
              }
              Text(
                text =
                  "Lokasi: ${activeRegion.name} • ${activeNode.title}\n" +
                    "Inventaris: ${inventoryState.totalItemQuantity} item (${inventoryState.uniqueItemCount} jenis)\n" +
                    "Statistik: Lv.${profile.level} • ${profile.xp} XP • ${profile.hp}/${profile.maxHp} HP • ${profile.energy}/${profile.maxEnergy} Energi • ${profile.coins} Koin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
              )
              FilledTonalButton(
                onClick = onSaveProgress,
                enabled = !isMutating,
                shape = RoundedCornerShape(12.dp),
                modifier =
                  Modifier.fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("save_progress_button"),
              ) {
                Icon(
                  imageVector = Icons.Outlined.CloudUpload,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan Progres ke Firestore", style = MaterialTheme.typography.labelLarge)
              }
            }
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            OutlinedButton(
              onClick = { showResetDialog = true },
              enabled = !isMutating,
              shape = RoundedCornerShape(14.dp),
              modifier =
                Modifier.weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("reset_adventure_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Mulai Ulang", style = MaterialTheme.typography.labelLarge)
            }

            FilledTonalButton(
              onClick = onSignOut,
              shape = RoundedCornerShape(14.dp),
              colors =
                ButtonDefaults.filledTonalButtonColors(
                  containerColor = MaterialTheme.colorScheme.errorContainer,
                  contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
              modifier =
                Modifier.weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("sign_out_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = stringResource(R.string.sign_out_button),
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }
        }
      }
    }

    // Pengaturan Suara & Musik Game
    item {
      AudioSettingsCard(
        audioSettings = audioSettings,
        onToggleBgm = onToggleBgm,
        onToggleSfx = onToggleSfx,
        onBgmVolumeChange = onBgmVolumeChange,
        onSfxVolumeChange = onSfxVolumeChange,
        onTestChoiceSfx = onTestChoiceSfx,
        onTestItemSfx = onTestItemSfx,
      )
    }

    // Beginner Guide Card
    item {
      Card(
        shape = RoundedCornerShape(22.dp),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
          ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.HelpOutline,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
              text = "Panduan Cepat untuk Pemula",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
          }
          Text(
            text =
              "1. Baca cerita di tab Petualangan dan pilih langkah terbaikmu.\n" +
                "2. Energi berkurang saat aksi khusus — ketuk tombol Kemah di atas kapan saja untuk pulih gratis!\n" +
                "3. Gunakan Beri atau Ramuan dari tab Tas untuk menambah HP & Energi.\n" +
                "4. Selesaikan semua wilayah di tab Peta untuk menjadi Penjelajah Sejati.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
          )
        }
      }
    }

    // Journal Timeline Header
    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.AutoStories,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "Catatan Jurnal (${journal.size})",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }
    }

    if (journal.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Belum ada catatan baru. Lanjutkan kisahmu untuk mencatat momen bersejarah!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(18.dp),
          )
        }
      }
    } else {
      items(journal, key = { it.id.ifBlank { "${it.title}_${it.createdAt?.seconds ?: 0L}" } }) {
        entry ->
        JournalEntryCard(entry = entry)
      }
    }

    item {
      DeveloperCreditCard()
    }

    item { Spacer(modifier = Modifier.height(12.dp)) }
  }

  if (showEditDialog) {
    EditHeroDialog(
      currentName = profile.heroName,
      currentAvatar = profile.avatarEmoji,
      onDismiss = { showEditDialog = false },
      onSave = { newName, newAvatar ->
        showEditDialog = false
        onUpdateHeroIdentity(newName, newAvatar, profile.tutorialDone)
      },
    )
  }

  if (showResetDialog) {
    AlertDialog(
      onDismissRequest = { showResetDialog = false },
      title = { Text("Mulai Ulang Petualangan?", style = MaterialTheme.typography.titleLarge) },
      text = {
        Text(
          "Level, koin, isi tas, dan peta akan dikembalikan ke awal (Gerbang Hutan Bisikan). Nama petualangmu tetap disimpan.",
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showResetDialog = false
            onResetAdventure()
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
          modifier = Modifier.testTag("confirm_reset_button"),
        ) {
          Text("Ya, Mulai Ulang")
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetDialog = false }) { Text("Batal") }
      },
    )
  }
}

@Composable
private fun JournalEntryCard(entry: JournalEntry) {
  val region = AdventureWorld.getRegion(entry.regionId)
  val formattedDate = remember(entry.createdAt) { formatTimestamp(entry.createdAt) }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          shape = RoundedCornerShape(50),
        ) {
          Text(
            text = region.name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
          )
        }
        Text(
          text = "+${entry.rewardXp} XP • $formattedDate",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = entry.title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = entry.summary,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun EditHeroDialog(
  currentName: String,
  currentAvatar: String,
  onDismiss: () -> Unit,
  onSave: (String, String) -> Unit,
) {
  var heroName by remember { mutableStateOf(currentName) }
  var selectedAvatar by remember { mutableStateOf(currentAvatar) }
  val isValid = heroName.trim().length in 2..24

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Ubah Profil Petualang", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = heroName,
          onValueChange = { if (it.length <= 24) heroName = it },
          label = { Text("Nama Petualang (2–24 huruf)") },
          singleLine = true,
          isError = !isValid,
          modifier = Modifier.fillMaxWidth().testTag("hero_name_input"),
        )
        Text("Pilih Ikon Petualang:", style = MaterialTheme.typography.labelLarge)
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          AdventureWorld.avatarOptions.forEach { emoji ->
            val selected = emoji == selectedAvatar
            Surface(
              shape = CircleShape,
              color =
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
              border =
                if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
              modifier =
                Modifier.size(44.dp)
                  .clip(CircleShape)
                  .clickable { selectedAvatar = emoji },
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(text = emoji, style = MaterialTheme.typography.titleLarge)
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(heroName.trim(), selectedAvatar) },
        enabled = isValid,
        modifier = Modifier.testTag("save_hero_button"),
      ) {
        Text("Simpan")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Batal") }
    },
  )
}

private fun formatTimestamp(timestamp: Timestamp?): String {
  val date = (timestamp ?: Timestamp.now()).toDate()
  return SimpleDateFormat("dd MMM, HH:mm", Locale.forLanguageTag("id-ID")).format(date)
}

@Composable
private fun AudioSettingsCard(
  audioSettings: AudioSettingsState,
  onToggleBgm: () -> Unit,
  onToggleSfx: () -> Unit,
  onBgmVolumeChange: (Float) -> Unit,
  onSfxVolumeChange: (Float) -> Unit,
  onTestChoiceSfx: () -> Unit,
  onTestItemSfx: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth().testTag("audio_settings_card"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.MusicNote,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "Pengaturan Suara & Musik",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      // Musik Latar (BGM)
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Musik Latar Petualangan",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Volume: ${(audioSettings.bgmVolume * 100).roundToInt()}%",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = audioSettings.bgmEnabled,
            onCheckedChange = { onToggleBgm() },
            modifier = Modifier.testTag("settings_bgm_switch"),
          )
        }
        Slider(
          value = audioSettings.bgmVolume,
          onValueChange = onBgmVolumeChange,
          enabled = audioSettings.bgmEnabled,
          valueRange = 0f..1f,
          modifier = Modifier.fillMaxWidth().testTag("settings_bgm_slider"),
        )
      }

      // Efek Suara (SFX)
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Efek Suara Pilihan & Item",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Volume: ${(audioSettings.sfxVolume * 100).roundToInt()}%",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = audioSettings.sfxEnabled,
            onCheckedChange = { onToggleSfx() },
            modifier = Modifier.testTag("settings_sfx_switch"),
          )
        }
        Slider(
          value = audioSettings.sfxVolume,
          onValueChange = onSfxVolumeChange,
          enabled = audioSettings.sfxEnabled,
          valueRange = 0f..1f,
          modifier = Modifier.fillMaxWidth().testTag("settings_sfx_slider"),
        )
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        OutlinedButton(
          onClick = onTestChoiceSfx,
          enabled = audioSettings.sfxEnabled,
          shape = RoundedCornerShape(14.dp),
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("test_choice_sfx_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Tes Pilihan", style = MaterialTheme.typography.labelLarge)
        }
        FilledTonalButton(
          onClick = onTestItemSfx,
          enabled = audioSettings.sfxEnabled,
          shape = RoundedCornerShape(14.dp),
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("test_item_sfx_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Tes Item Baru", style = MaterialTheme.typography.labelLarge)
        }
      }
    }
  }
}
