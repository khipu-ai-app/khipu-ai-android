package pe.khipuai.app.ui.screens.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.khipuai.app.ui.components.BottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToTab: (Int) -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToTutorHistory: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToFaq: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showPersonalInfoDialog by remember { mutableStateOf(false) }
    var showUniversityDialog by remember { mutableStateOf(false) }
    var showStudyGoalsDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Khipu AI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* Open drawer */ }) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = "Perfil", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Notifications */ }) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notificaciones", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            BottomNavigationBar(selectedTab = 4, onTabSelected = onNavigateToTab)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                ProfileHeader(
                    name = uiState.userName,
                    career = uiState.career,
                    university = uiState.university,
                    isPro = uiState.isPro,
                    onProClick = onNavigateToSubscription
                )
            }

            item { SectionTitle("Cuenta") }
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), shape = RoundedCornerShape(12.dp)) {
                    Column {
                        SettingsItem(icon = Icons.Default.TrendingUp, title = "Mi Progreso", onClick = onNavigateToStatistics)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsItem(icon = Icons.Default.EmojiEvents, title = "Mis Logros", onClick = onNavigateToAchievements)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsItem(icon = Icons.Default.Person, title = "Información Personal", onClick = { showPersonalInfoDialog = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsItem(icon = Icons.Default.Chat, title = "Historial de Khipu", onClick = onNavigateToTutorHistory)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsItem(icon = Icons.Default.School, title = "Universidad y Carrera", onClick = { showUniversityDialog = true })
                    }
                }
            }

            item { SectionTitle("Preferencias") }
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), shape = RoundedCornerShape(12.dp)) {
                    Column {
                        SettingsItem(icon = Icons.Default.TrackChanges, title = "Metas de Estudio", onClick = { showStudyGoalsDialog = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsItem(icon = Icons.Default.Notifications, title = "Notificaciones", onClick = onNavigateToNotificationSettings)
                    }
                }
            }

            item { SectionTitle("Aplicación") }
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), shape = RoundedCornerShape(12.dp)) {
                    Column {
                        SettingsItemWithSwitch(icon = Icons.Default.DarkMode, title = "Modo Oscuro", checked = uiState.isDarkMode, onCheckedChange = viewModel::toggleDarkMode)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsItemWithValue(icon = Icons.Default.Language, title = "Idioma", value = uiState.language, onClick = { showLanguageDialog = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsItem(icon = Icons.AutoMirrored.Filled.Help, title = "Ayuda y Soporte", onClick = onNavigateToFaq)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
            item {
                Text(
                    text = "Zona de peligro",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        viewModel.logout {
                            onLogout()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Cerrar Sesión",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Cerrar Sesión Seguro",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { showDeleteAccountDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar cuenta",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Eliminar cuenta",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // Dialogs
    if (showPersonalInfoDialog) {
        var fullName by remember { mutableStateOf(uiState.userName) }
        var nameError by remember { mutableStateOf<String?>(null) }
        
        AlertDialog(
            onDismissRequest = { showPersonalInfoDialog = false },
            title = { Text("Información Personal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { 
                            fullName = it 
                            if (it.isBlank()) nameError = "El nombre no puede estar vacío"
                            else if (it.length < 2) nameError = "Mínimo 2 caracteres"
                            else if (it.length > 100) nameError = "Máximo 100 caracteres"
                            else nameError = null
                        },
                        label = { Text("Nombre completo") },
                        isError = nameError != null,
                        supportingText = nameError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = { },
                        label = { Text("Email") },
                        enabled = false,
                        trailingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameError == null && fullName.isNotBlank()) {
                            viewModel.updateProfile(fullName = fullName)
                            showPersonalInfoDialog = false
                        }
                    },
                    enabled = nameError == null && fullName.isNotBlank()
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPersonalInfoDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showUniversityDialog) {
        var university by remember { mutableStateOf(uiState.university) }
        var career by remember { mutableStateOf(uiState.career) }
        var semesterText by remember { mutableStateOf(uiState.semester?.toString() ?: "") }

        AlertDialog(
            onDismissRequest = { showUniversityDialog = false },
            title = { Text("Universidad y Carrera") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = university,
                        onValueChange = { university = it },
                        label = { Text("Universidad") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = career,
                        onValueChange = { career = it },
                        label = { Text("Carrera / Programa") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = semesterText,
                        onValueChange = { semesterText = it },
                        label = { Text("Semestre actual") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val sem = semesterText.toIntOrNull()
                    viewModel.updateProfile(university = university, career = career, semester = sem)
                    showUniversityDialog = false
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUniversityDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showStudyGoalsDialog) {
        var goalMinutes by remember { mutableFloatStateOf(uiState.studyGoalMinutes.toFloat()) }
        val daysOfWeek = listOf("L", "M", "Mi", "J", "V", "S", "D")
        var selectedDays by remember { mutableStateOf(uiState.studyDays.toSet()) }

        AlertDialog(
            onDismissRequest = { showStudyGoalsDialog = false },
            title = { Text("Metas de Estudio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Meta diaria de estudio: ${goalMinutes.toInt()} min", fontWeight = FontWeight.Bold)
                    Slider(
                        value = goalMinutes,
                        onValueChange = { goalMinutes = it },
                        valueRange = 15f..120f,
                        steps = 6
                    )
                    Text("Días de estudio:", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        daysOfWeek.forEachIndexed { index, day ->
                            FilterChip(
                                selected = selectedDays.contains(index),
                                onClick = {
                                    if (selectedDays.contains(index)) {
                                        selectedDays = selectedDays - index
                                    } else {
                                        selectedDays = selectedDays + index
                                    }
                                },
                                label = { Text(day) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateProfile(studyGoalMinutes = goalMinutes.toInt(), studyDays = selectedDays.toList().sorted())
                    showStudyGoalsDialog = false
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStudyGoalsDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showLanguageDialog) {
        val languages = listOf("Español", "English")
        var selectedLang by remember { mutableStateOf(uiState.language) }

        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Idioma") },
            text = {
                Column {
                    languages.forEach { lang ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            RadioButton(
                                selected = lang == selectedLang,
                                onClick = { selectedLang = lang }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(lang)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateProfile(language = selectedLang)
                    showLanguageDialog = false
                }) {
                    Text("Aplicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showDeleteAccountDialog) {
        var step by remember { mutableIntStateOf(1) }
        var password by remember { mutableStateOf("") }
        var passwordError by remember { mutableStateOf<String?>(null) }
        
        LaunchedEffect(uiState.errorMessage) {
            if (uiState.errorMessage?.contains("Contraseña incorrecta", ignoreCase = true) == true) {
                passwordError = "Contraseña incorrecta"
            }
        }

        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Eliminar cuenta", color = MaterialTheme.colorScheme.error) },
            text = {
                if (step == 1) {
                    Text("Esta acción eliminará permanentemente tu cuenta, todos tus apuntes, conceptos y datos de estudio. Esta acción NO se puede deshacer.")
                } else {
                    Column {
                        Text("Para confirmar, escribe tu contraseña:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { 
                                password = it 
                                passwordError = null
                            },
                            label = { Text("Contraseña") },
                            visualTransformation = PasswordVisualTransformation(),
                            isError = passwordError != null,
                            supportingText = passwordError?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (step == 1) {
                            step = 2
                        } else {
                            if (password.isNotBlank()) {
                                viewModel.deleteAccount(password) {
                                    onLogout()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text(if (step == 1) "Entiendo, continuar" else "Eliminar cuenta")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ProfileHeader(
    name: String,
    career: String,
    university: String,
    isPro: Boolean,
    onProClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onProClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile picture
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.firstOrNull()?.toString() ?: "U",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // User info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isPro) {
                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "PRO",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = career,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = university,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsItemWithSwitch(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SettingsItemWithValue(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}