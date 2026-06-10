package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyberTeal,
    secondary = ElectricCyan,
    tertiary = MatrixGreen,
    background = ObsidianBg,
    surface = SurfaceTech,
    surfaceVariant = SurfaceTechElevated,
    onPrimary = Color(0xFF040D14),
    onSecondary = Color(0xFF040D14),
    onTertiary = Color(0xFF040D14),
    onBackground = LightText,
    onSurface = LightText,
    onSurfaceVariant = LightText,
    outline = BorderSlate
)

private val LightColorScheme = darkColorScheme(
    primary = CyberTeal,
    secondary = ElectricCyan,
    tertiary = MatrixGreen,
    background = Color(0xFF0C0F14), // Force dark mode aesthetic for premium LLM Studio feel
    surface = Color(0xFF161B22),
    surfaceVariant = Color(0xFF222B35),
    onPrimary = Color(0xFF040D14),
    onSecondary = Color(0xFF040D14),
    onTertiary = Color(0xFF040D14),
    onBackground = Color(0xFFF0F6FC),
    onSurface = Color(0xFFF0F6FC),
    onSurfaceVariant = Color(0xFFF0F6FC),
    outline = Color(0xFF30363D)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark theme for high-end LLM terminal experience
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve our cohesive cyberpunk palette
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
