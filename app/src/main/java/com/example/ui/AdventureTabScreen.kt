package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.audio.AudioSettingsState
import com.example.data.InventoryItem
import com.example.data.PlayerProfile
import com.example.game.AchievementSummary
import com.example.game.AdventureWorld
import com.example.game.FriendlyEncounter
import com.example.game.RiddleChallenge
import com.example.game.StoryChoice
import com.example.game.StoryNode
import com.example.ui.theme.HeartRed

@Composable
fun AdventureTabScreen(
  profile: PlayerProfile,
  currentNode: StoryNode,
  inventory: List<InventoryItem>,
  uiState: AdventureTransientState,
  onSyncNode: (StoryNode) -> Unit,
  onTogglePipTip: () -> Unit,
  onDismissBanner: () -> Unit,
  onRestAtCamp: () -> Unit,
  onSubmitRiddleOption: (StoryNode, Int) -> Unit,
  onToggleRiddleHint: () -> Unit,
  onAttackEncounter: (StoryNode) -> Unit,
  onNegotiateEncounter: (StoryNode) -> Unit,
  onUseHealingItem: (InventoryItem) -> Unit,
  onChooseOption: (StoryNode, StoryChoice) -> Unit,
  onOpenWorldMap: () -> Unit = {},
  onSaveProgress: () -> Unit = {},
  achievementSummary: AchievementSummary = AchievementSummary(),
  onDismissAchievementNotice: () -> Unit = {},
  audioSettings: AudioSettingsState = AudioSettingsState(),
  onToggleBgm: () -> Unit = {},
  onToggleSfx: () -> Unit = {},
) {
  val region = AdventureWorld.getRegion(currentNode.regionId)

  LaunchedEffect(currentNode.id) {
    onSyncNode(currentNode)
  }

  val stepNumber =
    (region.nodeIds.indexOf(currentNode.id).takeIf { it >= 0 } ?: 0) + 1
  val totalSteps = region.nodeIds.size

  val riddleSolved =
    currentNode.riddle == null || (uiState.riddle.nodeId == currentNode.id && uiState.riddle.solved)
  val encounterCleared =
    currentNode.encounter == null ||
      (uiState.encounter.nodeId == currentNode.id && uiState.encounter.won)

  val healingItem = inventory.firstOrNull { it.healAmount > 0L || it.energyAmount > 0L }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier.fillMaxSize(),
  ) {
    // 1. Panel Status Pemain
    item {
      PlayerStatusPanel(
        profile = profile,
        isMutating = uiState.isMutating,
        onRestClick = onRestAtCamp,
        achievementSummary = achievementSummary,
        audioSettings = audioSettings,
        onToggleBgm = onToggleBgm,
        onToggleSfx = onToggleSfx,
      )
    }

    item {
      AchievementUnlockNoticeCard(
        badge = uiState.newlyUnlockedBadge,
        onDismiss = onDismissAchievementNotice,
      )
    }

    item {
      BannerFeedbackCard(
        banner = uiState.banner,
        onDismiss = onDismissBanner,
      )
    }

    // 1b. Peta Area yang Sedang Dijelajahi
    item {
      AreaExplorationMapCard(
        region = region,
        currentNodeId = currentNode.id,
        visitedNodeIds = profile.visitedNodeIds,
        playerAvatarEmoji = profile.avatarEmoji,
        onOpenWorldMap = onOpenWorldMap,
      )
    }

    // 2. Area Narasi (Banner Lokasi + Kartu Cerita)
    item {
      NarrativeArea(
        regionName = region.name,
        regionImageRes = region.imageRes,
        nodeTitle = currentNode.title,
        speaker = currentNode.speaker,
        narrative = currentNode.narrative,
        stepNumber = stepNumber,
        totalSteps = totalSteps,
      )
    }

    // Panduan pemula oleh Pip
    item {
      PipHelperCard(
        tipText = currentNode.pipTip,
        expanded = uiState.pipTipExpanded,
        onToggle = onTogglePipTip,
      )
    }

    // Tantangan Teka-Teki (jika ada pada tahap ini)
    currentNode.riddle?.let { riddle ->
      item {
        RiddleChallengeCard(
          riddle = riddle,
          riddleState = uiState.riddle,
          onSelectOption = { idx -> onSubmitRiddleOption(currentNode, idx) },
          onToggleHint = onToggleRiddleHint,
        )
      }
    }

    // Pertemuan Ramah Pemula (jika ada pada tahap ini)
    currentNode.encounter?.let { encounter ->
      item {
        FriendlyEncounterCard(
          encounter = encounter,
          encounterState = uiState.encounter,
          isMutating = uiState.isMutating,
          healingItem = healingItem,
          onAttack = { onAttackEncounter(currentNode) },
          onNegotiate = { onNegotiateEncounter(currentNode) },
          onUseHeal = { item -> onUseHealingItem(item) },
        )
      }
    }

    // 3. Tombol Pilihan Aksi
    item {
      ActionChoicesSection(
        choices = currentNode.choices,
        isMutating = uiState.isMutating,
        isChoiceLocked = { index ->
          val requiresRiddle = currentNode.riddle != null && index == 0 && !riddleSolved
          val requiresEncounter = currentNode.encounter != null && !encounterCleared
          when {
            requiresEncounter -> "Selesaikan latihan pertemuan di atas terlebih dahulu"
            requiresRiddle -> "Jawab teka-teki di atas untuk membuka jalur ini"
            else -> null
          }
        },
        onChooseOption = { choice -> onChooseOption(currentNode, choice) },
      )
    }

    // 4. Tombol Simpan Progres Terpadu ke Firestore
    item {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth().testTag("adventure_cloud_save_card"),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = Icons.Outlined.CloudDone,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
            Column {
              Text(
                text = "Sinkronisasi Cloud Firestore",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "Simpan lokasi, isi tas (${inventory.sumOf { it.quantity }}), & statistik Lv.${profile.level}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          FilledTonalButton(
            onClick = onSaveProgress,
            enabled = !uiState.isMutating,
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("adventure_save_progress_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.CloudUpload,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Simpan", style = MaterialTheme.typography.labelLarge)
          }
        }
      }
    }

    item {
      DeveloperFooterRow(modifier = Modifier.testTag("adventure_copyright_footer"))
    }

    item { Spacer(modifier = Modifier.height(12.dp)) }
  }
}

@Composable
private fun RiddleChallengeCard(
  riddle: RiddleChallenge,
  riddleState: RiddleUiState,
  onSelectOption: (Int) -> Unit,
  onToggleHint: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(22.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
    modifier = Modifier.fillMaxWidth().testTag("riddle_card"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = "Teka-Teki Ramah Pemula",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        TextButton(
          onClick = onToggleHint,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("riddle_hint_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.HelpOutline,
            contentDescription = "Petunjuk teka-teki",
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (riddleState.hintVisible) "Tutup Petunjuk" else "Petunjuk")
        }
      }

      Text(
        text = riddle.question,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )

      AnimatedVisibility(visible = riddleState.hintVisible) {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Petunjuk: ${riddle.hint}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        riddle.options.forEachIndexed { idx, label ->
          val selected = riddleState.selectedOptionIndex == idx
          val isCorrectChoice = selected && idx == riddle.correctIndex
          FilledTonalButton(
            onClick = { onSelectOption(idx) },
            colors =
              ButtonDefaults.filledTonalButtonColors(
                containerColor =
                  when {
                    isCorrectChoice -> MaterialTheme.colorScheme.primary
                    selected -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surface
                  },
                contentColor =
                  when {
                    isCorrectChoice -> MaterialTheme.colorScheme.onPrimary
                    selected -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurface
                  },
              ),
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("riddle_option_$idx"),
          ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
          }
        }
      }

      riddleState.statusMessage?.let { msg ->
        Text(
          text = msg,
          style = MaterialTheme.typography.bodyMedium,
          color =
            if (riddleState.solved) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}

@Composable
private fun FriendlyEncounterCard(
  encounter: FriendlyEncounter,
  encounterState: EncounterUiState,
  isMutating: Boolean,
  healingItem: InventoryItem?,
  onAttack: () -> Unit,
  onNegotiate: () -> Unit,
  onUseHeal: (InventoryItem) -> Unit,
) {
  val enemyHpRatio =
    if (encounterState.enemyMaxHp > 0) {
      (encounterState.enemyHp.toFloat() / encounterState.enemyMaxHp.toFloat()).coerceIn(0f, 1f)
    } else 0f

  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    modifier = Modifier.fillMaxWidth().testTag("encounter_card"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.size(48.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(text = encounter.enemyEmoji, style = MaterialTheme.typography.headlineMedium)
            }
          }
          Column {
            Text(
              text = encounter.enemyName,
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text =
                if (encounterState.won) "Latihan Selesai!"
                else "HP Teman Uji: ${encounterState.enemyHp}/${encounterState.enemyMaxHp}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Icon(
          imageVector = Icons.Outlined.Shield,
          contentDescription = "Mode Aman Pemula",
          tint = MaterialTheme.colorScheme.primary,
        )
      }

      LinearProgressIndicator(
        progress = { enemyHpRatio },
        color = if (encounterState.won) MaterialTheme.colorScheme.primary else HeartRed,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
        modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(50)),
      )

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = encounterState.battleLog.ifBlank { encounter.introLine },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(12.dp),
        )
      }

      if (!encounterState.won) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Button(
            onClick = onAttack,
            enabled = !isMutating,
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("encounter_attack_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.AutoFixHigh,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Serang Lembut", style = MaterialTheme.typography.labelLarge)
          }

          OutlinedButton(
            onClick = onNegotiate,
            enabled = !isMutating,
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("encounter_peace_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Handshake,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Sapa (-${encounter.peaceCostEnergy} E)",
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }

        if (healingItem != null) {
          FilledTonalButton(
            onClick = { onUseHeal(healingItem) },
            enabled = !isMutating,
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("encounter_heal_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.LocalDrink,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pakai ${healingItem.name} (Sisa ${healingItem.quantity})")
          }
        }
      }
    }
  }
}
