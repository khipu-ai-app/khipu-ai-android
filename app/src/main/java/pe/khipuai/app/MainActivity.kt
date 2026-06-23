package pe.khipuai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import pe.khipuai.app.navigation.KhipuNavigation
import pe.khipuai.app.ui.theme.KhipuAITheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var achievementManager: pe.khipuai.app.ui.screens.achievements.AchievementManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge: cada Scaffold interno maneja su propio padding de insets.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            KhipuAITheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    KhipuNavigation()
                    pe.khipuai.app.ui.screens.achievements.AchievementPopupOverlay(manager = achievementManager)
                }
            }
        }
    }
}
