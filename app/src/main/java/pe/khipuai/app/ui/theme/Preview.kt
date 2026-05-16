package pe.khipuai.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun DesignSystemPreview() {
    KhipuAITheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Colors Section
                Text(
                    text = "Colores",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                ColorPalette()
                
                HorizontalDivider()
                
                // Typography Section
                Text(
                    text = "Tipografía",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                TypographyShowcase()
                
                HorizontalDivider()
                
                // Buttons Section
                Text(
                    text = "Botones",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                ButtonShowcase()
            }
        }
    }
}

@Composable
fun ColorPalette() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorRow("Primary", MaterialTheme.colorScheme.primary)
        ColorRow("Secondary", MaterialTheme.colorScheme.secondary)
        ColorRow("Tertiary", MaterialTheme.colorScheme.tertiary)
        ColorRow("Error", MaterialTheme.colorScheme.error)
        ColorRow("Surface", MaterialTheme.colorScheme.surface)
        ColorRow("Background", MaterialTheme.colorScheme.background)
    }
}

@Composable
fun ColorRow(name: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color, MaterialTheme.shapes.small)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically)
        )
    }
}

@Composable
fun TypographyShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Display Large", style = MaterialTheme.typography.displayLarge)
        Text("Display Medium", style = MaterialTheme.typography.displayMedium)
        Text("Display Small", style = MaterialTheme.typography.displaySmall)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("Headline Large", style = MaterialTheme.typography.headlineLarge)
        Text("Headline Medium", style = MaterialTheme.typography.headlineMedium)
        Text("Headline Small", style = MaterialTheme.typography.headlineSmall)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("Title Large", style = MaterialTheme.typography.titleLarge)
        Text("Title Medium", style = MaterialTheme.typography.titleMedium)
        Text("Title Small", style = MaterialTheme.typography.titleSmall)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("Body Large - Inter font family", style = MaterialTheme.typography.bodyLarge)
        Text("Body Medium - Inter font family", style = MaterialTheme.typography.bodyMedium)
        Text("Body Small - Inter font family", style = MaterialTheme.typography.bodySmall)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("Label Large", style = MaterialTheme.typography.labelLarge)
        Text("Label Medium", style = MaterialTheme.typography.labelMedium)
        Text("Label Small", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun ButtonShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = {}) {
            Text("Primary Button")
        }
        
        OutlinedButton(onClick = {}) {
            Text("Outlined Button")
        }
        
        FilledTonalButton(onClick = {}) {
            Text("Tonal Button")
        }
        
        TextButton(onClick = {}) {
            Text("Text Button")
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Secondary")
            }
            
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("Tertiary")
            }
            
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Error")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DesignSystemPreviewLight() {
    DesignSystemPreview()
}

@Preview(showBackground = true, showSystemUi = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DesignSystemPreviewDark() {
    DesignSystemPreview()
}
