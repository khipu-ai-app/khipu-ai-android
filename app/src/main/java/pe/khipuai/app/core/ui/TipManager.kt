package pe.khipuai.app.core.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * C-06: gestiona qué tips contextuales se han mostrado.
 * Almacena en SharedPreferences una bandera por [TipId].
 */
object TipManager {
    private const val PREFS_NAME = "khipu_tips"
    private const val KEY_PREFIX = "tip_shown_"

    fun shouldShow(context: Context, tipId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_PREFIX + tipId, false)
    }

    fun markShown(context: Context, tipId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PREFIX + tipId, true)
            .apply()
    }
}

/**
 * C-06: overlay semi-transparente con un tip contextual.
 * Se muestra la primera vez que el usuario visita la pantalla.
 * Auto-dismiss al tocar cualquier parte o después de 5 segundos.
 */
@Composable
fun ContextualTip(
    tipId: String,
    message: String,
    emoji: String = "💡",
    onDismiss: () -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val shouldShow = remember { TipManager.shouldShow(context, tipId) }
    var visible by remember { mutableStateOf(shouldShow) }

    if (visible) {
        LaunchedEffect(tipId) {
            TipManager.markShown(context, tipId)
            delay(5000)
            visible = false
            onDismiss()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable { visible = false; onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .padding(bottom = 48.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(emoji, style = MaterialTheme.typography.headlineSmall)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "¿Sabías que…?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.inverseOnSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(
                        onClick = { visible = false; onDismiss() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
