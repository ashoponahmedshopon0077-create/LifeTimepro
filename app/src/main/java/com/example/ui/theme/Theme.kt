package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val RomanticLightColorScheme = lightColorScheme(
    primary = RoseCrimson,
    onPrimary = Color.White,
    primaryContainer = RoseBlush,
    onPrimaryContainer = RoseDeepPlum,
    secondary = ChampagneGold,
    onSecondary = Color.White,
    secondaryContainer = ChampagneSoft,
    onSecondaryContainer = Color(0xFF331D00),
    tertiary = WarmAmber,
    onTertiary = Color.White,
    background = IvoryBackground,
    onBackground = DarkInk,
    surface = IvorySurface,
    onSurface = DarkInk,
    surfaceVariant = IvorySurfaceVariant,
    onSurfaceVariant = MutedInk,
    outline = Color(0xFFD9B8C0)
)

private val VelvetDarkColorScheme = darkColorScheme(
    primary = RoseGlow,
    onPrimary = Color(0xFF5D0024),
    primaryContainer = RoseCrimsonDark,
    onPrimaryContainer = RoseBlush,
    secondary = GoldGlow,
    onSecondary = Color(0xFF3E2600),
    secondaryContainer = Color(0xFF5A3B08),
    onSecondaryContainer = ChampagneSoft,
    tertiary = Color(0xFFFFB59D),
    onTertiary = Color(0xFF5A1C07),
    background = VelvetBackground,
    onBackground = SoftBlushText,
    surface = VelvetSurface,
    onSurface = SoftBlushText,
    surfaceVariant = VelvetSurfaceVariant,
    onSurfaceVariant = MutedBlushText,
    outline = Color(0xFF6B4854)
)

private val ClassicLightColorScheme = lightColorScheme(
    primary = Color(0xFF35639E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = RoseCrimson,
    onSecondary = Color.White,
    secondaryContainer = RoseBlush,
    onSecondaryContainer = RoseDeepPlum,
    background = Color(0xFFFAFAFC),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFEAEFF7),
    onSurfaceVariant = Color(0xFF44474E)
)

val LifeTimeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun LifeTimeTheme(
    themeMode: String = "romantic",
    languageCode: String = "en",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val context = LocalContext.current

    val colorScheme = when (themeMode) {
        "dark" -> VelvetDarkColorScheme
        "light" -> ClassicLightColorScheme
        "system" -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (systemDark) VelvetDarkColorScheme else RomanticLightColorScheme
            }
        }
        else -> {
            // "romantic" follows system dark/light subtly or keeps romantic ivory/velvet
            if (systemDark) VelvetDarkColorScheme else RomanticLightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = buildAppTypography(isBengali = languageCode == "bn"),
        shapes = LifeTimeShapes,
        content = content
    )
}
