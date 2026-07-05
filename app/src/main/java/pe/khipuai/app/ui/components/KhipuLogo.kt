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
    size: Int = 100
) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = R.mipmap.ic_launcher_foreground),
        contentDescription = "Khipu AI Logo",
        modifier = modifier.size(size.dp),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit
    )
}