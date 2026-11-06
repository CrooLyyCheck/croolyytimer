package pl.croolyy.timer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Pine = Color(0xFF244D3E)
val Cream = Color(0xFFF7F4EB)
val Gold = Color(0xFFB28C48)
private val LightColors = lightColorScheme(
    primary = Pine, onPrimary = Color.White, primaryContainer = Color(0xFFDFE9D9), onPrimaryContainer = Color(0xFF16392C),
    secondary = Color(0xFF75603B), secondaryContainer = Color(0xFFF0E4C8), onSecondaryContainer = Color(0xFF44371F),
    background = Cream, onBackground = Color(0xFF232D27),
    surface = Color(0xFFFFFCF5), onSurface = Color(0xFF232D27),
    surfaceVariant = Color(0xFFECEDE2), onSurfaceVariant = Color(0xFF5E695E),
    outline = Color(0xFF9AA293), outlineVariant = Color(0xFFDADDD0),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFB7D4AD), onPrimary = Color(0xFF19382B),
    primaryContainer = Color(0xFF2D4939), onPrimaryContainer = Color(0xFFDAECD5),
    secondary = Color(0xFFD9BD85), secondaryContainer = Color(0xFF50432C), onSecondaryContainer = Color(0xFFF0E2C4),
    background = Color(0xFF141D18), onBackground = Color(0xFFE8EBDD),
    surface = Color(0xFF1D2821), onSurface = Color(0xFFE8EBDD),
    surfaceVariant = Color(0xFF2C382E), onSurfaceVariant = Color(0xFFBBC6B5),
    outline = Color(0xFF87917E), outlineVariant = Color(0xFF3F4D40),
)

@Composable fun CrooTheme(theme: String, content: @Composable () -> Unit) {
    val dark = when (theme) { "light" -> false; "dark" -> true; else -> isSystemInDarkTheme() }
    val view = LocalView.current
    val activity = LocalActivity.current
    DisposableEffect(activity, view, dark) {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
        onDispose { }
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography(
            displayLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 56.sp, fontWeight = FontWeight.Normal),
            headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 34.sp, lineHeight = 39.sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 28.sp, lineHeight = 34.sp),
            titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 22.sp, lineHeight = 28.sp),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
            labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
        ), content = content)
}
