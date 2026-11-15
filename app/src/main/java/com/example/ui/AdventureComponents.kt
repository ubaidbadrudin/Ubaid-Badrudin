package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Flare
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.MusicOff
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.AudioSettingsState
import com.example.data.PlayerProfile
import com.example.game.AchievementBadge
import com.example.game.AchievementSummary
import com.example.game.AdventureWorld
import com.example.ui.theme.EnergyTeal
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.HeartRed
import com.example.ui.theme.XpViolet

@Composable
fun AdventureTopHud(
  profile: PlayerProfile,
  isMutating: Boolean,
  onRestClick: () -> Unit,
  modifier: Modifier = Modifier,
  achievementSummary: AchievementSummary = AchievementSummary(),
  audioSettings: AudioSettingsState = AudioSettingsState(),
  onToggleBgm: () -> Unit = {},
  onToggleSfx: () -> Unit = {},
) {
  var showAchievementsDialog by remember { mutableStateOf(false) }

  val xpGoal = AdventureWorld.xpRequiredForNextLevel(profile.level).coerceAtLeast(1L)
  val rawProgress = (profile.xp.toFloat() / xpGoal.toFloat()).coerceIn(0f, 1f)
  val animatedXp by
    animateFloatAsState(
      targetValue = rawProgress,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "xpProgress",
    )

  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(44.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(text = profile.avatarEmoji, style = MaterialTheme.typography.titleLarge)
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = profile.heroName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Surface(
                color = XpViolet.copy(alpha = 0.14f),
                contentColor = XpViolet,
                shape = RoundedCornerShape(50),
              ) {
                Text(
                  text = "Lv.${profile.level}",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              LinearProgressIndicator(
                progress = { animatedXp },
                color = XpViolet,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                modifier = Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(50)),
              )
              Text(
                text = "${profile.xp}/$xpGoal XP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        FilledTonalButton(
          onClick = onRestClick,
          enabled = !isMutating,
          shape = RoundedCornerShape(14.dp),
          colors =
            ButtonDefaults.filledTonalButtonColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("camp_rest_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.NightsStay,
            contentDescription = "Istirahat di Kemah",
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.camp_rest_button),
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }

      // Stat pills row: HP, Energy, Coins
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        HudStatPill(
          icon = Icons.Outlined.Favorite,
          tint = HeartRed,
          label = "HP",
          value = "${profile.hp}/${profile.maxHp}",
          modifier = Modifier.weight(1f),
        )
        HudStatPill(
          icon = Icons.Rounded.Bolt,
          tint = EnergyTeal,
          label = "Energi",
          value = "${profile.energy}/${profile.maxEnergy}",
          modifier = Modifier.weight(1f),
        )
        HudStatPill(
          icon = Icons.Outlined.MonetizationOn,
          tint = GoldCoin,
          label = "Koin",
          value = "${profile.coins}",
          modifier = Modifier.weight(1f),
        )
      }

      // Achievement Badge & Points Strip
      HudAchievementStrip(
        achievementSummary = achievementSummary,
        onClick = { showAchievementsDialog = true },
      )

      // Quick Audio Control Strip: Musik Latar (BGM) & Efek Suara (SFX)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        AudioQuickToggleChip(
          enabled = audioSettings.bgmEnabled,
          activeIcon = Icons.Outlined.MusicNote,
          mutedIcon = Icons.Outlined.MusicOff,
          label = if (audioSettings.bgmEnabled) "Musik Latar: Aktif" else "Musik Latar: Mati",
          contentDescription =
            if (audioSettings.bgmEnabled) "Matikan musik latar" else "Nyalakan musik latar",
          onClick = onToggleBgm,
          modifier = Modifier.weight(1f).testTag("toggle_bgm_button"),
        )
        AudioQuickToggleChip(
          enabled = audioSettings.sfxEnabled,
          activeIcon = Icons.AutoMirrored.Outlined.VolumeUp,
          mutedIcon = Icons.AutoMirrored.Outlined.VolumeOff,
          label = if (audioSettings.sfxEnabled) "Efek Suara: Aktif" else "Efek Suara: Mati",
          contentDescription =
            if (audioSettings.sfxEnabled) "Matikan efek suara" else "Nyalakan efek suara",
          onClick = onToggleSfx,
          modifier = Modifier.weight(1f).testTag("toggle_sfx_button"),
        )
      }
    }
  }

  if (showAchievementsDialog) {
    AchievementsCatalogDialog(
      achievementSummary = achievementSummary,
      onDismiss = { showAchievementsDialog = false },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HudAchievementStrip(
  achievementSummary: AchievementSummary,
  onClick: () -> Unit,
) {
  val unlocked = achievementSummary.unlockedAchievements
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    color = GoldCoin.copy(alpha = 0.12f),
    border = BorderStroke(1.dp, GoldCoin.copy(alpha = 0.35f)),
    modifier =
      Modifier.fillMaxWidth()
        .minimumInteractiveComponentSize()
        .testTag("player_achievements_panel"),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.EmojiEvents,
            contentDescription = "Panel Achievement",
            tint = GoldCoin,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "Achievement & Badge",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            color = GoldCoin.copy(alpha = 0.22f),
            shape = RoundedCornerShape(50),
          ) {
            Text(
              text = "${achievementSummary.totalPoints} Poin",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
          Text(
            text = "${achievementSummary.unlockedCount}/${achievementSummary.totalCount}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = "Lihat semua badge achievement",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
          )
        }
      }

      if (unlocked.isEmpty()) {
        Text(
          text = "Belum ada badge — selesaikan tugas atau temukan item langka! (Ketuk untuk daftar)",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          unlocked.forEach { badge ->
            Surface(
              shape = RoundedCornerShape(50),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, GoldCoin.copy(alpha = 0.4f)),
              modifier = Modifier.testTag("unlocked_badge_${badge.id}"),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              ) {
                Text(text = badge.badgeEmoji, style = MaterialTheme.typography.labelMedium)
                Text(
                  text = badge.title,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AchievementsCatalogDialog(
  achievementSummary: AchievementSummary,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.EmojiEvents,
            contentDescription = null,
            tint = GoldCoin,
          )
          Text("Daftar Achievement", style = MaterialTheme.typography.titleLarge)
        }
        Text(
          text =
            "${achievementSummary.unlockedCount}/${achievementSummary.totalCount} Badge Terbuka • Total ${achievementSummary.totalPoints}/${achievementSummary.maxPoints} Poin",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
          Modifier.fillMaxWidth()
            .heightIn(max = 360.dp)
            .verticalScroll(rememberScrollState())
            .testTag("achievements_dialog"),
      ) {
        achievementSummary.achievements.forEach { badge ->
          Surface(
            shape = RoundedCornerShape(16.dp),
            color =
              if (badge.isUnlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border =
              BorderStroke(
                width = 1.dp,
                color =
                  if (badge.isUnlocked) GoldCoin.copy(alpha = 0.5f)
                  else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
              ),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier =
                Modifier.padding(12.dp).alpha(if (badge.isUnlocked) 1f else 0.72f),
            ) {
              Surface(
                shape = CircleShape,
                color =
                  if (badge.isUnlocked) GoldCoin.copy(alpha = 0.22f)
                  else MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(42.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(text = badge.badgeEmoji, style = MaterialTheme.typography.titleMedium)
                }
              }
              Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = badge.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                  )
                  Surface(
                    shape = RoundedCornerShape(50),
                    color =
                      if (badge.isUnlocked) GoldCoin.copy(alpha = 0.24f)
                      else MaterialTheme.colorScheme.surfaceVariant,
                  ) {
                    Text(
                      text = "+${badge.points} Poin",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                  }
                }
                Text(
                  text = badge.description,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = "${badge.category.label} • ${if (badge.isUnlocked) "Terbuka" else "Terkunci"}",
                  style = MaterialTheme.typography.labelSmall,
                  color =
                    if (badge.isUnlocked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Icon(
                imageVector =
                  if (badge.isUnlocked) Icons.Rounded.CheckCircle else Icons.Outlined.Lock,
                contentDescription = if (badge.isUnlocked) "Badge terbuka" else "Badge terkunci",
                tint =
                  if (badge.isUnlocked) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Tutup")
      }
    },
  )
}

@Composable
fun AchievementUnlockNoticeCard(
  badge: AchievementBadge?,
  onDismiss: () -> Unit,
) {
  AnimatedVisibility(visible = badge != null) {
    if (badge != null) {
      Surface(
        color = GoldCoin.copy(alpha = 0.16f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, GoldCoin.copy(alpha = 0.48f)),
        modifier = Modifier.fillMaxWidth().testTag("achievement_unlock_banner"),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = GoldCoin.copy(alpha = 0.25f),
            modifier = Modifier.size(36.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(text = badge.badgeEmoji, style = MaterialTheme.typography.titleMedium)
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Achievement Terbuka! (+${badge.points} Poin)",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "${badge.title} — ${badge.description}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("dismiss_achievement_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = "Tutup notifikasi achievement",
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AudioQuickToggleChip(
  enabled: Boolean,
  activeIcon: ImageVector,
  mutedIcon: ImageVector,
  label: String,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    color =
      if (enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    contentColor =
      if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
      else MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier.minimumInteractiveComponentSize(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
      Icon(
        imageVector = if (enabled) activeIcon else mutedIcon,
        contentDescription = contentDescription,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun HudStatPill(
  icon: ImageVector,
  tint: Color,
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = tint.copy(alpha = 0.11f),
    modifier = modifier,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = tint,
        modifier = Modifier.size(17.dp),
      )
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
      )
    }
  }
}

@Composable
fun BannerFeedbackCard(
  banner: FeedbackBanner?,
  onDismiss: () -> Unit,
) {
  AnimatedVisibility(visible = banner != null) {
    if (banner != null) {
      val containerColor =
        when {
          banner.isWarning -> MaterialTheme.colorScheme.errorContainer
          banner.isCelebration -> MaterialTheme.colorScheme.primaryContainer
          else -> MaterialTheme.colorScheme.secondaryContainer
        }
      val contentColor =
        when {
          banner.isWarning -> MaterialTheme.colorScheme.onErrorContainer
          banner.isCelebration -> MaterialTheme.colorScheme.onPrimaryContainer
          else -> MaterialTheme.colorScheme.onSecondaryContainer
        }
      val icon =
        when {
          banner.isWarning -> Icons.Rounded.WarningAmber
          banner.isCelebration -> Icons.Rounded.CheckCircle
          else -> Icons.Outlined.AutoAwesome
        }

      Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("feedback_banner"),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
        ) {
          Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(text = banner.title, style = MaterialTheme.typography.titleMedium)
            Text(text = banner.message, style = MaterialTheme.typography.bodyMedium)
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("dismiss_banner_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = "Tutup pesan",
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
fun PipHelperCard(
  tipText: String,
  expanded: Boolean,
  onToggle: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
      ),
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .clickable(onClick = onToggle)
        .testTag("pip_tip_card"),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary),
          ) {
            Icon(
              imageVector = Icons.Outlined.Lightbulb,
              contentDescription = "Panduan Pip",
              tint = MaterialTheme.colorScheme.onTertiary,
              modifier = Modifier.size(18.dp),
            )
          }
          Text(
            text = "Panduan Pemula dari Pip 🦉",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
          )
        }
        Icon(
          imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
          contentDescription = if (expanded) "Sembunyikan tips" else "Tampilkan tips",
          tint = MaterialTheme.colorScheme.onTertiaryContainer,
        )
      }
      AnimatedVisibility(visible = expanded) {
        Text(
          text = tipText,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onTertiaryContainer,
          modifier = Modifier.padding(top = 8.dp),
        )
      }
    }
  }
}

fun itemIconForKey(iconKey: String): ImageVector =
  when (iconKey) {
    "snack" -> Icons.Outlined.Spa
    "bread" -> Icons.Outlined.BakeryDining
    "potion" -> Icons.Outlined.LocalDrink
    "key" -> Icons.Outlined.Key
    "lantern" -> Icons.Outlined.WbSunny
    "scroll" -> Icons.Outlined.MenuBook
    "amulet" -> Icons.Outlined.Flare
    "crystal" -> Icons.Outlined.Diamond
    "crown" -> Icons.Outlined.EmojiEvents
    else -> Icons.Outlined.AutoAwesome
  }

@Composable
fun DeveloperCreditCard(
  modifier: Modifier = Modifier,
) {
  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier.fillMaxWidth().testTag("developer_credit_card"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
      Surface(
        shape = CircleShape,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
        modifier = Modifier.size(60.dp),
      ) {
        Image(
          painter = painterResource(R.drawable.img_developer_ubaid),
          contentDescription = "Foto Developer @ubaidbadrudin",
          contentScale = ContentScale.Crop,
          modifier = Modifier.clip(CircleShape),
        )
      }
      Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.weight(1f),
      ) {
        Text(
          text = "Tentang Developer",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = stringResource(R.string.developer_handle),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = stringResource(R.string.developer_copyright),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun DeveloperFooterRow(
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
      Surface(
        shape = CircleShape,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
        modifier = Modifier.size(30.dp),
      ) {
        Image(
          painter = painterResource(R.drawable.img_developer_ubaid),
          contentDescription = "Foto Developer @ubaidbadrudin",
          contentScale = ContentScale.Crop,
          modifier = Modifier.clip(CircleShape),
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = stringResource(R.string.developer_copyright),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
