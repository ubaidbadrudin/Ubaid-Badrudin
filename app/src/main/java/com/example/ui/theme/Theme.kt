package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkEmeraldOnPrimary,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = DarkEmeraldOnContainer,
    secondary = DarkAmberSecondary,
    onSecondary = DarkAmberOnSecondary,
    secondaryContainer = DarkAmberContainer,
    onSecondaryContainer = DarkAmberOnContainer,
    tertiary = DarkMysticTertiary,
    onTertiary = DarkMysticOnTertiary,
    tertiaryContainer = DarkMysticContainer,
    onTertiaryContainer = DarkMysticOnContainer,
    background = DarkForestBackground,
    onBackground = DarkForestOnBackground,
    surface = DarkForestSurface,
    onSurface = DarkForestOnBackground,
    surfaceVariant = DarkForestSurfaceVariant,
    onSurfaceVariant = DarkForestOnSurfaceVariant,
    outline = DarkForestOutline,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = AmberSecondary,
    onSecondary = AmberOnSecondary,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = AmberOnContainer,
    tertiary = MysticTertiary,
    onTertiary = MysticOnTertiary,
    tertiaryContainer = MysticContainer,
    onTertiaryContainer = MysticOnContainer,
    background = ParchmentBackground,
    onBackground = ParchmentOnBackground,
    surface = ParchmentSurface,
    onSurface = ParchmentOnBackground,
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = ParchmentOnSurfaceVariant,
    outline = ParchmentOutline,
  )

val LokaShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun LokaPetualangTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = LokaTypography,
    shapes = LokaShapes,
    content = content,
  )
}
