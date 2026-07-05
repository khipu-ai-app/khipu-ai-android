package pe.khipuai.app.ui.screens.auth

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    fun OnboardingScreen(
        onNavigateToHome: () -> Unit,
        viewModel: OnboardingViewModel = hiltViewModel()
    ) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var fullName by remember { mutableStateOf("") }
    var selectedProfile by remember { mutableStateOf("ingenieria") }

    LaunchedEffect(uiState.fullName) {
        if (fullName.isBlank() && uiState.fullName.isNotBlank()) {
            fullName = uiState.fullName
        }
    }

    LaunchedEffect(selectedProfile) {
        viewModel.loadCatalog(selectedProfile)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración de Perfil", fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = "¡Hola! Personalicemos tu entorno",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Selecciona tu área para que la IA organice tus asignaturas y optimice tu plan de repaso espaciado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Input Nombre Completo Premium
            item {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nombre y Apellido") },
                    leadingIcon = { Icon(Icons.Default.Badge, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            // Selector de Perfiles Académicos de Alto Nivel
            item {
                Text(
                    text = "Área Académica",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileCard("Preuniversitario", Icons.Default.School, selectedProfile == "preuniversitario", Modifier.weight(1f)) { selectedProfile = "preuniversitario" }
                        ProfileCard("Ingeniería", Icons.Default.Terminal, selectedProfile == "ingenieria", Modifier.weight(1f)) { selectedProfile = "ingenieria" }
                        ProfileCard("Ciencias", Icons.Default.Science, selectedProfile == "ciencias", Modifier.weight(1f)) { selectedProfile = "ciencias" }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileCard("Medicina", Icons.Default.MedicalServices, selectedProfile == "medicina", Modifier.weight(1f)) { selectedProfile = "medicina" }
                        ProfileCard("Administración", Icons.Default.Business, selectedProfile == "administracion", Modifier.weight(1f)) { selectedProfile = "administracion" }
                        ProfileCard("Derecho", Icons.Default.Gavel, selectedProfile == "derecho", Modifier.weight(1f)) { selectedProfile = "derecho" }
                    }
                }
            }

            // Listado de Asignaturas
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Selecciona tus cursos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${uiState.selectedCourses.size}/3",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.selectedCourses.size >= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                    }
                }
            } else if (uiState.catalogCourses.isEmpty()) {
                item {
                    Text(
                        text = "No se encontraron cursos para este perfil.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        uiState.catalogCourses.forEach { course ->
                            val isSelected = uiState.selectedCourses.contains(course)
                            val atLimit = !isSelected && uiState.selectedCourses.size >= 3
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (!atLimit || isSelected) viewModel.toggleCourseSelection(course)
                                },
                                label = { Text(course, style = MaterialTheme.typography.bodySmall) },
                                enabled = !atLimit || isSelected,
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                } else null,
                            )
                        }
                    }
                }
            }

            // Botón guardar
            item {
                Button(
                    onClick = { viewModel.saveOnboarding(fullName, selectedProfile, onNavigateToHome) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !uiState.isLoading && uiState.selectedCourses.isNotEmpty() && fullName.isNotBlank()
                ) {
                    Text("Configurar mi espacio Khipu AI", fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun ProfileCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        label = "bgColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        label = "contentColor"
    )

    Card(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = contentColor)
        }
    }
}