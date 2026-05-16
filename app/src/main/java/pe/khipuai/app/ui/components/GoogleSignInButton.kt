package pe.khipuai.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.khipuai.app.R

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        ),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Google Icon (usando un Canvas simple por ahora)
            GoogleIcon()
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = "Continuar con Google",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun GoogleIcon() {
    // Ícono simple de Google usando Canvas
    androidx.compose.foundation.Canvas(
        modifier = Modifier.size(20.dp)
    ) {
        val size = this.size.minDimension
        val center = this.center
        
        // Colores de Google
        val blue = androidx.compose.ui.graphics.Color(0xFF4285F4)
        val red = androidx.compose.ui.graphics.Color(0xFFEA4335)
        val yellow = androidx.compose.ui.graphics.Color(0xFFFBBC05)
        val green = androidx.compose.ui.graphics.Color(0xFF34A853)
        
        // Dibujar la "G" de Google de forma simplificada
        val radius = size / 3
        
        // Círculo azul (parte superior derecha)
        drawArc(
            color = blue,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(
                center.x - radius,
                center.y - radius
            ),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )
        
        // Círculo rojo (parte superior izquierda)
        drawArc(
            color = red,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(
                center.x - radius,
                center.y - radius
            ),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )
        
        // Círculo amarillo (parte inferior izquierda)
        drawArc(
            color = yellow,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(
                center.x - radius,
                center.y - radius
            ),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )
        
        // Círculo verde (parte inferior derecha)
        drawArc(
            color = green,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(
                center.x - radius,
                center.y - radius
            ),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )
    }
}