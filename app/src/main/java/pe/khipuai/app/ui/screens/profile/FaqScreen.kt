package pe.khipuai.app.ui.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class FaqItem(val question: String, val answer: String)

val allFaqs = listOf(
    FaqItem("¿Cómo subo mis apuntes?", "Para subir apuntes, ve a la pestaña 'Capturar' en el menú inferior. Puedes tomar fotos, elegir imágenes de tu galería o subir archivos PDF y de texto."),
    FaqItem("¿Qué es el repaso espaciado?", "El repaso espaciado (basado en el algoritmo SM-2) es un método de estudio que te hace repasar la información justo antes de que estés a punto de olvidarla, optimizando tu memoria a largo plazo."),
    FaqItem("¿Por qué mi nota tardó en procesarse?", "El procesamiento incluye la lectura del documento (OCR), el análisis mediante Inteligencia Artificial para extraer conceptos clave y la generación de grafos de conocimiento. Esto puede tomar unos segundos dependiendo del tamaño de la nota."),
    FaqItem("¿Puedo eliminar una nota?", "Sí. Ve al detalle de la nota y en el menú de opciones (los tres puntos en la esquina superior derecha) selecciona 'Eliminar Nota'."),
    FaqItem("¿Khipu AI funciona sin internet?", "Para la mayoría de las funciones de Inteligencia Artificial y sincronización se requiere una conexión a internet activa."),
    FaqItem("¿Cómo edito mis datos personales?", "Desde esta misma sección de Perfil, selecciona 'Información Personal' para cambiar tu nombre, o 'Universidad y Carrera' para actualizar tus datos académicos.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(onNavigateBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    val filteredFaqs = remember(searchQuery) {
        if (searchQuery.isBlank()) allFaqs
        else allFaqs.filter { 
            it.question.contains(searchQuery, ignoreCase = true) || 
            it.answer.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayuda y Soporte") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Buscar en preguntas frecuentes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                shape = RoundedCornerShape(12.dp)
            )

            Text(
                text = "PREGUNTAS FRECUENTES",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredFaqs) { faq ->
                    ExpandableFaqItem(faq)
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "¿No encontraste lo que buscabas?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:soporte@khipuai.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Soporte Khipu AI - Android App")
                            }
                            context.startActivity(Intent.createChooser(intent, "Enviar correo a soporte"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Contactar Soporte")
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun ExpandableFaqItem(faq: FaqItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Contraer" else "Expandir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                Text(
                    text = faq.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
