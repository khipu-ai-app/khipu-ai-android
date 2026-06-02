package pe.khipuai.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Light Color Scheme ────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary             = Purple30,          // #4A0E8F – brand purple
    onPrimary           = OnPrimary,         // white
    primaryContainer    = Purple90,          // #E9DDFF – light lavender
    onPrimaryContainer  = Purple10,          // #21005E – deepest purple

    secondary           = PurpleGrey30,      // #625B71
    onSecondary         = OnSecondary,
    secondaryContainer  = PurpleGrey90,      // #E8DEF8
    onSecondaryContainer = Color(0xFF1D192B),

    tertiary            = Pink40,            // #7E5260
    onTertiary          = OnTertiary,
    tertiaryContainer   = Pink90,            // #FFD8E4
    onTertiaryContainer = Color(0xFF31111D),

    error               = Error,
    onError             = OnError,
    errorContainer      = ErrorContainer,
    onErrorContainer    = OnErrorContainer,

    background          = Background,        // #F8F7FF – brand light bg
    onBackground        = OnBackground,

    surface             = Surface,           // white
    onSurface           = OnSurface,
    surfaceVariant      = SurfaceVariant,    // #E7E0EC
    onSurfaceVariant    = OnSurfaceVariant,  // #49454F

    outline             = Outline,
    outlineVariant      = OutlineVariant,

    inverseSurface      = Inverted,          // #313033
    inverseOnSurface    = OnInverted,        // #F4EFF4
    inversePrimary      = Purple80,          // #CFBCFF
)

// ── Dark Color Scheme ─────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary             = Purple80,          // #CFBCFF
    onPrimary           = Purple20,          // #380094
    primaryContainer    = Purple30,          // #4A0E8F
    onPrimaryContainer  = Purple90,          // #E9DDFF

    secondary           = PurpleGrey80,      // #CCC2DC
    onSecondary         = Color(0xFF332D41),
    secondaryContainer  = Color(0xFF4A4458),
    onSecondaryContainer = PurpleGrey90,

    tertiary            = Pink80,            // #EFB8C8
    onTertiary          = Color(0xFF492532),
    tertiaryContainer   = Color(0xFF633B48),
    onTertiaryContainer = Pink90,

    error               = Color(0xFFF2B8B5),
    onError             = Color(0xFF601410),
    errorContainer      = Color(0xFF8C1D18),
    onErrorContainer    = ErrorContainer,

    background          = Gray900,
    onBackground        = Color(0xFFE6E1E5),

    surface             = Gray800,
    onSurface           = Color(0xFFE6E1E5),
    surfaceVariant      = Color(0xFF49454F),
    onSurfaceVariant    = OutlineVariant,

    outline             = Color(0xFF938F99),
    outlineVariant      = Color(0xFF49454F),

    inverseSurface      = Color(0xFFE6E1E5),
    inverseOnSurface    = Color(0xFF313033),
    inversePrimary      = Purple30,
)

// ── Theme ─────────────────────────────────────────────────────────────────────
@Composable
fun KhipuAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color (Material You) enabled on Android 12+; falls back to brand palette below.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = Shapes,
        content     = content,
    )
}
