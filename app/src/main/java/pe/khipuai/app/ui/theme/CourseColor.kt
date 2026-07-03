package pe.khipuai.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Helper compartido para parsear el color hex de un curso (formato `#RRGGBB`
 * o `#AARRGGBB` que es lo que devuelve el backend) a un `Color` de Compose.
 *
 * Centralizamos el fallback en un morado neutro (`#6750A4`, el primary
 * por defecto de Material 3) para mantener coherencia visual si el backend
 * no envía color o el hex es inválido.
 *
 * T-16: este helper lo usan CoursesScreen, NoteDetailScreen, PlannerScreen
 * y StatisticsScreen para propagar el color elegido por el usuario al
 * resto de la UI.
 */
fun parseCourseColor(hex: String?): Color {
    if (hex.isNullOrBlank()) return Color(0xFF6750A4)
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrElse { Color(0xFF6750A4) }
}
