package pe.khipuai.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import pe.khipuai.app.R

@Composable
fun KhipuLogo(
    modifier: Modifier = Modifier,
    size: Int = 64
) {
    // Por ahora usamos un ícono simple, después se puede reemplazar con el logo real
    androidx.compose.foundation.Canvas(
        modifier = modifier.size(size.dp)
    ) {
        // Dibujo simple del logo de Khipu (nodos conectados)
        val center = this.center
        val radius = size.dp.toPx() / 8
        
        // Color del logo
        val color = androidx.compose.ui.graphics.Color(0xFF4B00B2)
        
        // Dibujar nodos
        drawCircle(
            color = color,
            radius = radius,
            center = androidx.compose.ui.geometry.Offset(
                center.x - radius * 2,
                center.y - radius * 2
            )
        )
        
        drawCircle(
            color = color,
            radius = radius,
            center = androidx.compose.ui.geometry.Offset(
                center.x + radius * 2,
                center.y - radius * 2
            )
        )
        
        drawCircle(
            color = color,
            radius = radius,
            center = androidx.compose.ui.geometry.Offset(
                center.x,
                center.y + radius * 2
            )
        )
        
        drawCircle(
            color = color,
            radius = radius,
            center = center
        )
        
        // Dibujar líneas conectoras
        val strokeWidth = 4.dp.toPx()
        
        // Línea izquierda-centro
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(
                center.x - radius * 2 + radius,
                center.y - radius * 2 + radius
            ),
            end = androidx.compose.ui.geometry.Offset(
                center.x - radius,
                center.y - radius
            ),
            strokeWidth = strokeWidth
        )
        
        // Línea derecha-centro
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(
                center.x + radius * 2 - radius,
                center.y - radius * 2 + radius
            ),
            end = androidx.compose.ui.geometry.Offset(
                center.x + radius,
                center.y - radius
            ),
            strokeWidth = strokeWidth
        )
        
        // Línea centro-abajo
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(
                center.x,
                center.y + radius
            ),
            end = androidx.compose.ui.geometry.Offset(
                center.x,
                center.y + radius * 2 - radius
            ),
            strokeWidth = strokeWidth
        )
    }
}