package pe.khipuai.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * F4: niveles de elevación semánticos para cards Khipu.
 */
object KhipuElevation {
    val flat = 0.dp
    val low = 1.dp
    val medium = 2.dp
    val high = 4.dp
}

/**
 * F4: Card unificada de Khipu AI.
 *
 * Usar este componente en lugar de `Card` directamente para mantener
 * consistencia visual en toda la app.
 *
 * Elevaciones recomendadas según uso:
 *  - `flat` (0dp): cards en fondos ya elevados (dialogos, sheets).
 *  - `low` (1dp): cards estándar en surfaces (lista de cursos, archivos).
 *  - `medium` (2dp): cards destacadas (progreso, métricas).
 *  - `high` (4dp): cards flotantes (mazo de estudio, featured).
 *
 * Shapes recomendados:
 *  - `MaterialTheme.shapes.medium` (12dp) → cards pequeñas y botones.
 *  - `MaterialTheme.shapes.large` (16dp) → cards estándar.
 *  - `MaterialTheme.shapes.extraLarge` (24dp) → cards destacadas.
 */
@Composable
fun KhipuCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    elevation: Dp = KhipuElevation.low,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    innerPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val card = @Composable {
        Card(
            onClick = onClick ?: {},
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            enabled = onClick != null,
        ) {
            Column(modifier = Modifier.padding(innerPadding), content = content)
        }
    }
    if (onClick != null) {
        card()
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        ) {
            Column(modifier = Modifier.padding(innerPadding), content = content)
        }
    }
}

/**
 * F4: Surface unificada de Khipu AI.
 * Similar a KhipuCard pero sin elevación ni click, ideal para fondos
 * de secciones dentro de un scroll.
 */
@Composable
fun KhipuSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    innerPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = color,
    ) {
        Column(modifier = Modifier.padding(innerPadding), content = content)
    }
}
