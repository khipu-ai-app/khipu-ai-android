package pe.khipuai.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import pe.khipuai.app.core.auth.AuthEventBus
import pe.khipuai.app.core.auth.AuthStartupChecker
import pe.khipuai.app.core.deeplink.DeepLinkBus
import pe.khipuai.app.core.preferences.ThemePreferences
import pe.khipuai.app.data.notification.NotificationDeepLinks
import pe.khipuai.app.navigation.KhipuNavigation
import pe.khipuai.app.ui.theme.KhipuAITheme
import pe.khipuai.app.ui.theme.ThemeMode
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var achievementManager: pe.khipuai.app.ui.screens.achievements.AchievementManager

    @Inject
    lateinit var authEventBus: AuthEventBus

    @Inject
    lateinit var themePreferences: ThemePreferences

    // T-04: bus para deep links desde notificaciones (locales o FCM).
    @Inject
    lateinit var deepLinkBus: DeepLinkBus

    // T-07: decide el start destination (Login vs Home) al arrancar.
    @Inject
    lateinit var authStartupChecker: AuthStartupChecker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge: cada Scaffold interno maneja su propio padding de insets.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // T-04: si la app se abrió desde una notificación, emitimos el
        // deep link al bus para que KhipuNavigation navegue al destino.
        handleDeepLink(intent)
        setContent {
            // T-03: colectamos el ThemeMode persistido. Cuando el usuario
            // cambia la opción en ProfileScreen, el DataStore emite y la
            // UI recompone con el nuevo esquema de colores. No necesitamos
            // recreate() ni AppCompatDelegate.
            val themeMode by themePreferences.themeModeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            KhipuAITheme(themeMode = themeMode) {
                // T-03: Surface raíz con el color de fondo del tema. Sin esto,
                // el background del window de Android (blanco/negro) no cambia
                // con el tema y pantallas como Login (que no usan Scaffold)
                // muestran el color del sistema en vez del colorScheme.background.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        KhipuNavigation(
                            authEventBus = authEventBus,
                            deepLinkBus = deepLinkBus,
                            authStartupChecker = authStartupChecker
                        )
                        pe.khipuai.app.ui.screens.achievements.AchievementPopupOverlay(manager = achievementManager)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // T-04: cuando la app está en background y se toca una notificación,
        // Android no recrea la Activity; llama onNewIntent. Procesamos el
        // deep link aquí.
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val deepLink = intent?.getStringExtra(NotificationDeepLinks.EXTRA_DEEP_LINK)
        if (!deepLink.isNullOrBlank()) {
            deepLinkBus.emit(deepLink)
            // Limpiamos el extra para que un process restoration no re-navegue
            intent.removeExtra(NotificationDeepLinks.EXTRA_DEEP_LINK)
        }
    }
}
