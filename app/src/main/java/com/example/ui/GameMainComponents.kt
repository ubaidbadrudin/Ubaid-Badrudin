package com.example.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.AudioSettingsState
import com.example.data.PlayerProfile
import com.example.game.AchievementCatalog
import com.example.game.AchievementSummary
import com.example.game.AdventureWorld
import com.example.game.RegionInfo
import com.example.game.StoryChoice
import com.example.ui.theme.LokaPetualangTheme

/**
 * 1. Panel Status Pemain:
 * Menampilkan identitas petualang, Level & XP, HP, Energi, Koin, Badge Achievement, dan tombol istirahat Kemah.
 */
@Composable
fun PlayerStatusPanel(
  profile: PlayerProfile,
  isMutating: Boolean,
  onRestClick: () -> Unit,
  modifier: Modifier = Modifier,
  achievementSummary: AchievementSummary = AchievementSummary(),
  audioSettings: AudioSettingsState = AudioSettingsState(),
  onToggleBgm: () -> Unit = {},
  onToggleSfx: () -> Unit = {},
) {
  AdventureTopHud(
    profile = profile,
    isMutating = isMutating,
    onRestClick = onRestClick,
    modifier = modifier.testTag("player_status_panel"),
    achievementSummary = achievementSummary,
    audioSettings = audioSettings,
    onToggleBgm = onToggleBgm,
    onToggleSfx = onToggleSfx,
  )
}

/**
 * 1b. Peta Area Eksplorasi:
 * Menampilkan peta rute titik-titik lokasi pada area yang sedang dijelajahi pemain,
 * lengkap dengan penanda lokasi saat ini, titik yang sudah dikunjungi, dan jalur ke depan.
 */
@Composable
fun AreaExplorationMapCard(
  region: RegionInfo,
  currentNodeId: String,
  visitedNodeIds: List<String>,
  playerAvatarEmoji: String,
  onOpenWorldMap: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val nodeIds = region.nodeIds
  val currentIndex = nodeIds.indexOf(currentNodeId).takeIf { it >= 0 } ?: 0
  val visitedInRegion = nodeIds.count { visitedNodeIds.contains(it) }
  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.secondary
  val outlineColor = MaterialTheme.colorScheme.outline

  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = modifier.fillMaxWidth().testTag("area_exploration_map_card"),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.padding(14.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(34.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Outlined.Map,
                contentDescription = "Peta Area ${region.name}",
                modifier = Modifier.size(18.dp),
              )
            }
          }
          Column {
            Text(
              text = "Peta Area: ${region.name}",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "$visitedInRegion/${nodeIds.size} titik dijelajahi • ${region.difficultyLabel}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        TextButton(
          onClick = onOpenWorldMap,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("open_world_map_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.Explore,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = "Peta Dunia", style = MaterialTheme.typography.labelLarge)
        }
      }

      // Kanvas jalur peta area + penanda lokasi
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
              Brush.linearGradient(
                listOf(
                  MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                  MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.24f),
                )
              )
            ),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().height(54.dp),
          ) {
            // Garis rute berkelok antar titik
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
              if (nodeIds.size > 1) {
                val stepWidth = size.width / (nodeIds.size - 1)
                val centerY = size.height / 2f
                val waveOffset = 10.dp.toPx()
                val points =
                  nodeIds.indices.map { idx ->
                    val yShift = if (idx % 2 == 0) -waveOffset else waveOffset
                    Offset(x = idx * stepWidth, y = centerY + yShift)
                  }

                for (i in 0 until points.lastIndex) {
                  val start = points[i]
                  val end = points[i + 1]
                  val midX = (start.x + end.x) / 2f
                  val segmentPath =
                    Path().apply {
                      moveTo(start.x, start.y)
                      cubicTo(midX, start.y, midX, end.y, end.x, end.y)
                    }
                  val isCompletedPath = i < currentIndex
                  val isNextPath = i == currentIndex
                  drawPath(
                    path = segmentPath,
                    color =
                      when {
                        isCompletedPath -> primaryColor
                        isNextPath -> secondaryColor.copy(alpha = 0.75f)
                        else -> outlineColor.copy(alpha = 0.45f)
                      },
                    style =
                      Stroke(
                        width = if (isCompletedPath) 4.dp.toPx() else 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect =
                          if (isCompletedPath) null
                          else PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                      ),
                  )
                }
              }
            }

            // Penanda titik lokasi pada jalur
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            ) {
              nodeIds.forEachIndexed { idx, nodeId ->
                val isCurrent = nodeId == currentNodeId
                val isVisited = visitedNodeIds.contains(nodeId)
                val isGoal = idx == nodeIds.lastIndex
                val yOffset = if (idx % 2 == 0) (-10).dp else 10.dp

                Box(
                  contentAlignment = Alignment.Center,
                  modifier =
                    Modifier.padding(
                        top = if (yOffset > 0.dp) 12.dp else 0.dp,
                        bottom = if (yOffset < 0.dp) 12.dp else 0.dp,
                      )
                      .size(if (isCurrent) 38.dp else 30.dp)
                      .clip(CircleShape)
                      .background(
                        when {
                          isCurrent -> MaterialTheme.colorScheme.secondary
                          isVisited -> MaterialTheme.colorScheme.primary
                          else -> MaterialTheme.colorScheme.surface
                        }
                      )
                      .testTag("area_map_node_$nodeId"),
                ) {
                  when {
                    isCurrent ->
                      Text(
                        text = playerAvatarEmoji,
                        style = MaterialTheme.typography.titleMedium,
                      )
                    isVisited ->
                      Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Titik sudah dikunjungi",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                      )
                    isGoal ->
                      Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = "Titik tujuan akhir area",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                      )
                    else ->
                      Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = "Titik belum dijelajahi",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.size(16.dp),
                      )
                  }
                }
              }
            }
          }

          // Label singkat masing-masing titik lokasi
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            nodeIds.forEach { nodeId ->
              val node = AdventureWorld.getNode(nodeId)
              val isCurrent = nodeId == currentNodeId
              val isVisited = visitedNodeIds.contains(nodeId)
              Text(
                text = node.title,
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                  ),
                color =
                  when {
                    isCurrent -> MaterialTheme.colorScheme.secondary
                    isVisited -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                  },
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
              )
            }
          }
        }
      }

      val activeNodeTitle = AdventureWorld.getNode(currentNodeId).title
      val statusCaption =
        if (currentIndex >= nodeIds.lastIndex) {
          "Posisi saat ini: $activeNodeTitle (Titik puncak area ini!)"
        } else {
          "Posisi saat ini: $activeNodeTitle • Tahap ${currentIndex + 1} dari ${nodeIds.size}"
        }
      Text(
        text = statusCaption,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/**
 * 2. Area Narasi:
 * Menampilkan visual wilayah, nama lokasi, pembicara, progres tahap, dan kotak narasi cerita.
 */
@Composable
fun NarrativeArea(
  regionName: String,
  @DrawableRes regionImageRes: Int,
  nodeTitle: String,
  speaker: String,
  narrative: String,
  stepNumber: Int,
  totalSteps: Int,
  modifier: Modifier = Modifier,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = modifier.fillMaxWidth().testTag("narrative_area"),
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
      modifier = Modifier.fillMaxWidth().height(176.dp),
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        Image(
          painter = painterResource(regionImageRes),
          contentDescription = "Pemandangan $regionName",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.verticalGradient(
                  listOf(Color.Black.copy(alpha = 0.12f), Color.Black.copy(alpha = 0.72f))
                )
              )
        )
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().padding(14.dp),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(50),
          ) {
            Text(
              text = regionName,
              style = MaterialTheme.typography.labelLarge,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
          }
          Surface(
            color = Color.Black.copy(alpha = 0.55f),
            contentColor = Color.White,
            shape = RoundedCornerShape(50),
          ) {
            Text(
              text = "Tahap $stepNumber / $totalSteps",
              style = MaterialTheme.typography.labelLarge,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
          }
        }
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
          Text(
            text = nodeTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
          )
          Text(
            text = speaker,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
          )
        }
      }
    }

    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = narrative,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(18.dp),
      )
    }
  }
}

/**
 * 3. Tombol Pilihan Aksi:
 * Menampilkan daftar tombol aksi yang dapat dipilih pemain beserta badge hadiah/energi.
 */
@Composable
fun ActionChoicesSection(
  choices: List<StoryChoice>,
  isMutating: Boolean,
  isChoiceLocked: (index: Int) -> String?,
  onChooseOption: (StoryChoice) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = modifier.fillMaxWidth().testTag("action_choices_section"),
  ) {
    Text(
      text = "Pilih Langkahmu:",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onBackground,
    )

    choices.forEachIndexed { index, choice ->
      val lockedReason = isChoiceLocked(index)
      val enabled = lockedReason == null && !isMutating

      ActionChoiceButtonCard(
        choice = choice,
        enabled = enabled,
        lockedReason = lockedReason,
        testTagName = "story_choice_${choice.id}",
        onClick = { onChooseOption(choice) },
      )
    }
  }
}

@Composable
private fun ActionChoiceButtonCard(
  choice: StoryChoice,
  enabled: Boolean,
  lockedReason: String?,
  testTagName: String,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    enabled = enabled,
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
      ),
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
          else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
      ),
    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag(testTagName),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = choice.label,
          style = MaterialTheme.typography.titleMedium,
          color =
            if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
          color =
            if (enabled) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
          contentColor =
            if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
          shape = RoundedCornerShape(50),
        ) {
          Text(
            text = lockedReason ?: choice.badge,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Surface(
        shape = RoundedCornerShape(12.dp),
        color =
          if (enabled) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.surfaceVariant,
        contentColor =
          if (enabled) MaterialTheme.colorScheme.onPrimary
          else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(38.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = "Pilih langkah",
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun GameMainBasicStructurePreview() {
  LokaPetualangTheme {
    val sampleProfile =
      PlayerProfile(
        heroName = "Arka",
        level = 2L,
        xp = 35L,
        hp = 28L,
        maxHp = 35L,
        energy = 14L,
        maxEnergy = 17L,
        coins = 45L,
      )
    val sampleChoices =
      listOf(
        StoryChoice(
          id = "preview_safe",
          label = "Ikuti Jalan Setapak Berbatu",
          badge = "Aman • +10 XP",
          nextNodeId = "forest_stream",
          resultMessage = "",
        ),
        StoryChoice(
          id = "preview_berry",
          label = "Cari Beri di Semak Berkilau",
          badge = "Dapat Item • -1 Energi",
          energyCost = 1,
          nextNodeId = "forest_stream",
          resultMessage = "",
        ),
      )

    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
      item {
        PlayerStatusPanel(
          profile = sampleProfile,
          isMutating = false,
          onRestClick = {},
          achievementSummary =
            AchievementCatalog.evaluate(
              profile =
                sampleProfile.copy(
                  visitedNodeIds = listOf("forest_start", "forest_stream"),
                ),
              inventory = emptyList(),
            ),
        )
      }
      item {
        AreaExplorationMapCard(
          region = AdventureWorld.getRegion("forest"),
          currentNodeId = "forest_stream",
          visitedNodeIds = listOf("forest_start", "forest_stream"),
          playerAvatarEmoji = sampleProfile.avatarEmoji,
          onOpenWorldMap = {},
        )
      }
      item {
        NarrativeArea(
          regionName = "Hutan Bisikan",
          regionImageRes = R.drawable.img_region_forest,
          nodeTitle = "Gerbang Hutan Bisikan",
          speaker = "Pip si Burung Hantu",
          narrative =
            "Cahaya pagi menembus dedaunan hijau. Pip mengepakkan sayap kecilnya dan menunjukkan dua jalur petualangan.",
          stepNumber = 1,
          totalSteps = 4,
        )
      }
      item {
        ActionChoicesSection(
          choices = sampleChoices,
          isMutating = false,
          isChoiceLocked = { null },
          onChooseOption = {},
        )
      }
    }
  }
}
