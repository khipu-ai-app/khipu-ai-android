package pe.khipuai.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * F5: layout que se adapta al ancho disponible.
 *
 * En compact (<600dp): una sola columna (stack vertical).
 * En medium/expanded (>=600dp): dos columnas lado a lado.
 *
 * No requiere dependencias externas (usa [BoxWithConstraints] de foundation).
 */
@Composable
fun AdaptiveColumn(
    compactContent: @Composable ColumnScope.() -> Unit,
    expandedContent: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        if (maxWidth < 600.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = compactContent,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                content = expandedContent,
            )
        }
    }
}

/**
 * F5: layout de grid adaptativo para listas de cards.
 * En compact: 1 columna. En expanded: 2 columnas.
 */
@Composable
fun AdaptiveGrid(
    items: List<*>,
    modifier: Modifier = Modifier,
    itemContent: @Composable (Any?) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val columns = if (maxWidth >= 600.dp) 2 else 1

        if (columns == 1) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items.forEach { item -> itemContent(item) }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                    items.filterIndexed { index, _ -> index % 2 == 0 }.forEach { itemContent(it) }
                }
                Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                    items.filterIndexed { index, _ -> index % 2 == 1 }.forEach { itemContent(it) }
                }
            }
        }
    }
}
