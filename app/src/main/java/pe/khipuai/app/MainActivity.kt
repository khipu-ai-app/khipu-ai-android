package pe.khipuai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import pe.khipuai.app.navigation.KhipuNavigation
import pe.khipuai.app.ui.theme.KhipuAITheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge: cada Scaffold interno maneja su propio padding de insets.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            KhipuAITheme {
                KhipuNavigation()
            }
        }
    }
}
