package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.data.PlayerProfile
import com.example.game.AdventureWorld
import com.example.game.RegionInfo

@Composable
fun MapTabScreen(
  profile: PlayerProfile,
  isMutating: Boolean,
  banner: FeedbackBanner?,
  onDismissBanner: () -> Unit,
  onTravelToRegion: (String) -> Unit,
  onBackToAdventure: () -> Unit,
) {
  BackHandler { onBackToAdventure() }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier.fillMaxSize(),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "Peta Wilayah Loka",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = "Tuntaskan setiap wilayah untuk membuka tantangan baru yang seru.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item {
      BannerFeedbackCard(banner = banner, onDismiss = onDismissBanner)
    }

    items(AdventureWorld.regions, key = { it.id }) { region ->
      val unlocked = AdventureWorld.isRegionUnlocked(region.id, profile.completedRegionIds)
      val completed = profile.completedRegionIds.contains(region.id)
      val isCurrent = profile.currentRegionId == region.id
      val visitedCount = region.nodeIds.count { profile.visitedNodeIds.contains(it) }

      RegionMapCard(
        region = region,
        unlocked = unlocked,
        completed = completed,
        isCurrent = isCurrent,
        visitedCount = visitedCount,
        isMutating = isMutating,
        onTravelClick = { onTravelToRegion(region.id) },
      )
    }

    item { Spacer(modifier = Modifier.height(12.dp)) }
  }
}

@Composable
private fun RegionMapCard(
  region: RegionInfo,
  unlocked: Boolean,
  completed: Boolean,
  isCurrent: Boolean,
  visitedCount: Int,
  isMutating: Boolean,
  onTravelClick: () -> Unit,
) {
  val totalNodes = region.nodeIds.size.coerceAtLeast(1)
  val progress = (visitedCount.toFloat() / totalNodes.toFloat()).coerceIn(0f, 1f)

  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = if (unlocked) 3.dp else 0.dp),
    modifier = Modifier.fillMaxWidth().alpha(if (unlocked) 1f else 0.72f),
  ) {
    Column {
      Box(modifier = Modifier.fillMaxWidth().height(142.dp)) {
        Image(
          painter = painterResource(region.imageRes),
          contentDescription = region.name,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.verticalGradient(
                  listOf(Color.Black.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.72f))
                )
              )
        )
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(50),
          ) {
            Text(
              text = "${region.difficultyLabel} • Lv.${region.recommendedLevel}+",
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }

          Surface(
            color =
              when {
                completed -> MaterialTheme.colorScheme.primary
                isCurrent -> MaterialTheme.colorScheme.secondary
                unlocked -> Color.Black.copy(alpha = 0.55f)
                else -> Color.Black.copy(alpha = 0.65f)
              },
            contentColor = Color.White,
            shape = RoundedCornerShape(50),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
              Icon(
                imageVector =
                  when {
                    completed -> Icons.Outlined.CheckCircle
                    unlocked -> Icons.Outlined.Explore
                    else -> Icons.Outlined.Lock
                  },
                contentDescription = null,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text =
                  when {
                    completed -> "Tuntas"
                    isCurrent -> "Lokasi Saat Ini"
                    unlocked -> "Terbuka"
                    else -> "Terkunci"
                  },
                style = MaterialTheme.typography.labelSmall,
              )
            }
          }
        }

        Text(
          text = region.name,
          style = MaterialTheme.typography.headlineMedium,
          color = Color.White,
          modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
        )
      }

      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = region.subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          LinearProgressIndicator(
            progress = { progress },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(50)),
          )
          Text(
            text = "$visitedCount/$totalNodes tahap",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Text(
          text = "Hadiah Wilayah: ${region.rewardSummary}",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )

        Button(
          onClick = onTravelClick,
          enabled = unlocked && !isMutating,
          shape = RoundedCornerShape(14.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor =
                if (isCurrent) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.primary
            ),
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("travel_region_${region.id}"),
        ) {
          Icon(
            imageVector = if (unlocked) Icons.Outlined.Explore else Icons.Outlined.Lock,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text =
              when {
                !unlocked -> "Selesaikan Wilayah Sebelumnya"
                isCurrent -> "Mulai Ulang dari Gerbang Wilayah"
                else -> "Jelajahi ${region.name}"
              },
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}
