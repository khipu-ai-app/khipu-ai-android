package pe.khipuai.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pe.khipuai.app.core.auth.AuthEventBus
import pe.khipuai.app.core.auth.AuthStartupChecker
import pe.khipuai.app.core.auth.AuthStartupState
import pe.khipuai.app.core.deeplink.DeepLinkBus
import pe.khipuai.app.ui.screens.auth.LoginScreen
import pe.khipuai.app.ui.screens.auth.RegisterScreen
import pe.khipuai.app.ui.screens.auth.OnboardingScreen
import pe.khipuai.app.ui.screens.auth.ForgotPasswordScreen
import pe.khipuai.app.ui.screens.home.HomeScreen
import pe.khipuai.app.ui.screens.capture.CaptureScreen
import pe.khipuai.app.ui.screens.planner.PlannerScreen
import pe.khipuai.app.ui.screens.planner.CalendarScreen
import pe.khipuai.app.ui.screens.maps.MapsScreen
import pe.khipuai.app.ui.screens.profile.ProfileScreen
import pe.khipuai.app.ui.screens.processing.ProcessingScreen
import pe.khipuai.app.ui.screens.analysis.AnalysisScreen
import pe.khipuai.app.ui.screens.search.SearchScreen
import pe.khipuai.app.ui.screens.statistics.StatisticsScreen
import pe.khipuai.app.ui.screens.achievements.AchievementsScreen
import pe.khipuai.app.ui.screens.studyguide.StudyGuideScreen
import pe.khipuai.app.ui.screens.tutor.TutorChatScreen
import pe.khipuai.app.ui.screens.tutor.TutorHistoryScreen
import pe.khipuai.app.ui.screens.courses.CoursesScreen
import pe.khipuai.app.ui.screens.courses.CreateCourseScreen
import pe.khipuai.app.ui.screens.coursedetail.CourseDetailScreen
import pe.khipuai.app.ui.screens.notedetail.NoteDetailScreen
import pe.khipuai.app.ui.screens.quiz.QuizCreationScreen
import pe.khipuai.app.ui.screens.subscription.SubscriptionScreen
import pe.khipuai.app.ui.screens.fileviewer.FileViewerScreen
import pe.khipuai.app.ui.screens.review.ReviewSessionScreen
import pe.khipuai.app.ui.screens.exam.ExamConfigScreen
import pe.khipuai.app.ui.screens.exam.ExamScreen
import pe.khipuai.app.ui.screens.exam.ExamResultScreen
import pe.khipuai.app.ui.screens.exam.ExamStep
import pe.khipuai.app.ui.screens.exam.ExamViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun KhipuNavigation(
    navController: NavHostController = rememberNavController(),
    authEventBus: AuthEventBus? = null,
    deepLinkBus: DeepLinkBus? = null,
    authStartupChecker: AuthStartupChecker
) {
    // T-07: dispara la decisión de start destination una vez al inicio.
    LaunchedEffect(authStartupChecker) {
        authStartupChecker.runOnce()
    }

    val startupState by authStartupChecker.state.collectAsState()

    // T-08: si el refresh token falla, el Authenticator emite SessionExpired
    // y navegamos a Login limpiando todo el backstack.
    LaunchedEffect(authEventBus) {
        val bus = authEventBus ?: return@LaunchedEffect
        bus.events.collect { event ->
            when (event) {
                AuthEventBus.AuthEvent.SessionExpired -> {
                    // Guard: si ya estamos en Login, NO navegar. Sin este
                    // check, un SessionExpired disparado por cualquier
                    // llamada en background (ej. una request autenticada
                    // paralela al login) ejecutaría popUpTo(0) sobre el
                    // LoginScreen actual, destruyéndolo y haciendo perder
                    // lo que el usuario estaba tipeando.
                    val currentRoute = navController.currentDestination?.route
                    if (currentRoute == Screen.Login.route) return@collect
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                    // Resetear el checker para que el próximo arranque
                    // vuelva a decidir el start destination.
                    authStartupChecker.reset()
                }
            }
        }
    }

    // T-04: deep links desde notificaciones (locales o FCM). Mapeamos
    // el string del deep link a la ruta del NavHost correspondiente.
    LaunchedEffect(deepLinkBus) {
        val bus = deepLinkBus ?: return@LaunchedEffect
        bus.events.collect { deepLink ->
            handleDeepLink(navController, deepLink)
        }
    }

    // T-07: mientras el startupState sea Loading, no mostramos nada.
    // El sistema operativo muestra el splash screen mientras tanto.
    if (startupState == AuthStartupState.Loading) {
        return
    }

    val startDestination = when (startupState) {
        AuthStartupState.LoggedIn -> Screen.Home.route
        else -> Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTab = { tabIndex ->
                    when (tabIndex) {
                        0 -> { /* Already on Home */ }
                        1 -> navController.navigate(Screen.Capture.route)
                        2 -> navController.navigate(Screen.Planner.route)
                        3 -> navController.navigate(Screen.Maps.route)
                        4 -> navController.navigate(Screen.Profile.route)
                    }
                },
                onNavigateToCourses = {
                    navController.navigate(Screen.Courses.route)
                },
                onNavigateToCreateCourse = {
                    navController.navigate(Screen.CreateCourse.route)
                },
                onNavigateToCourseDetail = { courseId ->
                    navController.navigate("${Screen.CourseDetail.route}/$courseId")
                },
                onNavigateToFileViewer = { encodedPath ->
                    navController.navigate("${Screen.FileViewer.route}/$encodedPath")
                },
                onNavigateToNoteDetail = { noteId ->
                    navController.navigate("${Screen.NoteDetail.route}/$noteId")
                },
                onNavigateToSubscription = { reason ->
                    navController.navigate(Screen.Subscription.create(reason))
                },
                onNavigateToTutorHistory = {
                    navController.navigate("tutor_history?contextType=general")
                },
                onNavigateToSearch = {
                    navController.navigate("search")
                }
            )
        }

        composable(
            route = "${Screen.Capture.route}?preselectedCourseId={preselectedCourseId}",
            arguments = listOf(
                navArgument("preselectedCourseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            CaptureScreen(
                onNavigateToTab = { tabIndex ->
                    when (tabIndex) {
                        0 -> navController.navigate(Screen.Home.route)
                        1 -> { /* Already on Capture */ }
                        2 -> navController.navigate(Screen.Planner.route)
                        3 -> navController.navigate(Screen.Maps.route)
                        4 -> navController.navigate(Screen.Profile.route)
                    }
                },
                onNavigateToProcessing = { uploadId ->
                    navController.navigate("${Screen.Processing.route}/$uploadId")
                },
                onNavigateToSubscription = { reason ->
                    navController.navigate(Screen.Subscription.create(reason))
                },
                // T-17: navegar a la nota existente desde el dialog
                // de "Documento duplicado". Hacemos popUpTo del Capture
                // para que al volver desde NoteDetail no se reactive el
                // dialog de upload.
                onNavigateToNoteDetail = { noteId ->
                    navController.navigate("${Screen.NoteDetail.route}/$noteId") {
                        popUpTo(Screen.Capture.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.Planner.route) {
            PlannerScreen(
                onNavigateToTab = { tabIndex ->
                    when (tabIndex) {
                        0 -> navController.navigate(Screen.Home.route)
                        1 -> navController.navigate(Screen.Capture.route)
                        2 -> { /* Already on Planner */ }
                        3 -> navController.navigate(Screen.Maps.route)
                        4 -> navController.navigate(Screen.Profile.route)
                    }
                },
                onNavigateToCalendar = {
                    navController.navigate(Screen.Calendar.route)
                },
                onNavigateToNote = { noteId ->
                    navController.navigate("${Screen.NoteDetail.route}/$noteId")
                },
                // T-10: tap en un concepto SIN nota asociada navega al
                // grafo del concepto en MapsScreen. El usuario puede ver
                // si el concepto está conectado a algo o no.
                onNavigateToConcept = { conceptName ->
                    val encoded = java.net.URLEncoder.encode(
                        conceptName,
                        Charsets.UTF_8.name()
                    )
                    navController.navigate("${Screen.Maps.route}?highlightConcept=$encoded")
                },
                onNavigateToDailyDeck = { navController.navigate("daily_deck_session") },
                // T-10: tap en "Iniciar repaso" de un bloque de curso navega
                // a la ReviewSession de esa nota específica.
                onStartCourseReview = { noteId ->
                    navController.navigate("${Screen.ReviewSession.route}/$noteId")
                }
            )
        }

        composable(
            route = "${Screen.Maps.route}?preselectedCourseId={preselectedCourseId}&highlightConcept={highlightConcept}",
            arguments = listOf(
                navArgument("preselectedCourseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("highlightConcept") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val preselectedCourseId = backStackEntry.arguments?.getString("preselectedCourseId")
            val highlightConcept = backStackEntry.arguments?.getString("highlightConcept")
            MapsScreen(
                onNavigateToTab = { tabIndex ->
                    when (tabIndex) {
                        0 -> navController.navigate(Screen.Home.route)
                        1 -> navController.navigate(Screen.Capture.route)
                        2 -> navController.navigate(Screen.Planner.route)
                        3 -> { /* Already on Maps */ }
                        4 -> navController.navigate(Screen.Profile.route)
                    }
                },
                preselectedCourseId = preselectedCourseId,
                highlightConcept = highlightConcept,
                onNoteClick = { noteId ->
                    navController.navigate("${Screen.NoteDetail.route}/$noteId")
                },
                onStartReview = { conceptName ->
                    val encoded = java.net.URLEncoder.encode(conceptName, "UTF-8")
                    navController.navigate("review_session/by-concept?conceptName=$encoded")
                },
                onAskTutor = { conceptName ->
                    val encoded = java.net.URLEncoder.encode(conceptName, "UTF-8")
                    // Abrimos el chat global con el concepto como prefill
                    navController.navigate(
                        "tutor_history?contextType=general&initialConcepts=$encoded"
                    )
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToTab = { tabIndex ->
                    val targetRoute = when (tabIndex) {
                        0 -> Screen.Home.route
                        1 -> Screen.Capture.route
                        2 -> Screen.Planner.route
                        3 -> Screen.Maps.route
                        4 -> null // Ya estamos en Profile
                        else -> null
                    }

                    targetRoute?.let { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onNavigateToSubscription = { reason ->
                    navController.navigate(Screen.Subscription.create(reason))
                },
                onNavigateToTutorHistory = {
                    navController.navigate("tutor_history")
                },
                onNavigateToNotificationSettings = {
                    navController.navigate(Screen.NotificationSettings.route)
                },
                onNavigateToFaq = {
                    navController.navigate(Screen.Faq.route)
                },
                onNavigateToStatistics = {
                    navController.navigate("statistics")
                },
                onNavigateToAchievements = {
                    navController.navigate("achievements")
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true } // Limpieza total del historial
                    }
                }
            )
        }

        composable(Screen.NotificationSettings.route) {
            pe.khipuai.app.ui.screens.profile.NotificationSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = androidx.hilt.navigation.compose.hiltViewModel<pe.khipuai.app.ui.screens.profile.ProfileViewModel>()
            )
        }

        composable(Screen.Faq.route) {
            pe.khipuai.app.ui.screens.profile.FaqScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // C-03: Modo Examen
        composable(
            route = "${Screen.Exam.route}/{courseId}?courseName={courseName}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.StringType },
                navArgument("courseName") { type = NavType.StringType; nullable = true; defaultValue = "" }
            )
        ) {
            ExamHostScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = "${Screen.Processing.route}/{uploadId}",
            arguments = listOf(navArgument("uploadId") { type = NavType.StringType })
        ) {
            ProcessingScreen(
                onProcessingComplete = { noteId ->
                    navController.navigate("${Screen.Analysis.route}/$noteId") {
                        popUpTo(Screen.Capture.route) { inclusive = true }
                    }
                },
                onErrorEscape = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "${Screen.Analysis.route}/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            AnalysisScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStudyGuide = {
                    navController.navigate("${Screen.StudyGuide.route}/$noteId")
                },
                onNavigateToQuizCreation = {
                    navController.navigate("${Screen.QuizCreation.route}/$noteId")
                },
                onConceptClick = { conceptName ->
                    val encoded = java.net.URLEncoder.encode(conceptName, "UTF-8")
                    navController.navigate("${Screen.Maps.route}?highlightConcept=$encoded")
                }
            )
        }

        composable(
            route = "${Screen.StudyGuide.route}/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) {
            StudyGuideScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReview = { reviewNoteId ->
                    navController.navigate("${Screen.ReviewSession.route}/$reviewNoteId")
                }
            )
        }

        composable(
            route = "${Screen.Tutor.route}/{sessionId}?courseId={courseId}&contextType={contextType}&contextId={contextId}&initialConcepts={initialConcepts}&noteContext={noteContext}&noteTitle={noteTitle}",
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("courseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("contextType") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("contextId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("initialConcepts") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("noteContext") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("noteTitle") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            TutorChatScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "tutor_history?contextType={contextType}&contextId={contextId}&noteContext={noteContext}&noteTitle={noteTitle}&initialConcepts={initialConcepts}",
            arguments = listOf(
                navArgument("contextType") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("contextId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("noteContext") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("noteTitle") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("initialConcepts") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val ctxType = backStackEntry.arguments?.getString("contextType") ?: "general"
            val ctxId = backStackEntry.arguments?.getString("contextId")
            val noteCtx = backStackEntry.arguments?.getString("noteContext")
            val noteTit = backStackEntry.arguments?.getString("noteTitle")
            val initConcepts = backStackEntry.arguments?.getString("initialConcepts")

            fun buildQueryParams(): String {
                val parts = mutableListOf<String>()
                parts += "contextType=$ctxType"
                if (ctxId != null) parts += "contextId=$ctxId"
                if (noteCtx != null) parts += "noteContext=$noteCtx"
                if (noteTit != null) parts += "noteTitle=${java.net.URLEncoder.encode(noteTit, "UTF-8")}"
                if (initConcepts != null) parts += "initialConcepts=$initConcepts"
                return "?${parts.joinToString("&")}"
            }

            TutorHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSession = { sessionId ->
                    navController.navigate("${Screen.Tutor.route}/$sessionId${buildQueryParams()}")
                },
                onNewSession = {
                    navController.navigate("${Screen.Tutor.route}/new_session${buildQueryParams()}")
                }
            )
        }

        // --- Nuevas Rutas SPRINT 5 ---

        composable(Screen.Courses.route) {
            CoursesScreen(
                onCourseClick = { courseId ->
                    navController.navigate("${Screen.CourseDetail.route}/$courseId")
                },
                onCreateCourseClick = {
                    navController.navigate(Screen.CreateCourse.route)
                }
            )
        }

        composable(Screen.CreateCourse.route) {
            CreateCourseScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Screen.CourseDetail.route}/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            CourseDetailScreen(
                onBackClick = { navController.popBackStack() },
                onNoteClick = { noteId ->
                    navController.navigate("${Screen.NoteDetail.route}/$noteId")
                },
                onExpandMapClick = {
                    navController.navigate("${Screen.Maps.route}?preselectedCourseId=$courseId")
                },
                onNavigateToCapture = { cid ->
                    navController.navigate("${Screen.Capture.route}?preselectedCourseId=$cid")
                },
                onNavigateToTutor = { cid ->
                    navController.navigate("tutor_history?contextType=course&contextId=$cid")
                },
                onNavigateToStudy = { route ->
                    navController.navigate(route)
                },
                // C-03: Modo Examen
                onNavigateToExam = { courseId, courseName ->
                    val encoded = java.net.URLEncoder.encode(courseName, "UTF-8")
                    navController.navigate("${Screen.Exam.route}/$courseId?courseName=$encoded")
                },
                // CE-03: Repasar curso (navega con courseId a la ruta dedicada)
                onNavigateToReview = { courseId ->
                    navController.navigate("review_session/by-course/$courseId")
                }
            )
        }

        composable(
            route = "${Screen.NoteDetail.route}/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            NoteDetailScreen(
                onBackClick = { navController.popBackStack() },
                onReviewClick = {
                    navController.navigate("${Screen.ReviewSession.route}/$noteId")
                },
                onAskTutorClick = { concept, courseId, noteIdFromCb, noteTitle ->
                    // Nuevo modelo: SIEMPRE abrimos el chat del curso.
                    // - Si la nota no tiene curso → caemos al chat global.
                    // - Si viene un concept: prefill con "Explícame el concepto «X»"
                    // - Si no viene concept: prefill con "Tengo una pregunta sobre la nota «X»"
                    val encodedNoteId = java.net.URLEncoder.encode(noteIdFromCb, "UTF-8")
                    val encodedNoteTitle = java.net.URLEncoder.encode(noteTitle, "UTF-8")

                    if (courseId.isNullOrBlank()) {
                        // Nota sin curso → chat global con hint de la nota
                        navController.navigate(
                            "tutor_history?contextType=general" +
                            "&noteContext=$encodedNoteId&noteTitle=$encodedNoteTitle"
                        )
                    } else if (!concept.isNullOrBlank()) {
                        // Tapped un chip de concepto → prefill con el concepto
                        val encodedConcept = java.net.URLEncoder.encode(concept, "UTF-8")
                        navController.navigate(
                            "tutor_history?contextType=course&contextId=$courseId" +
                            "&initialConcepts=$encodedConcept"
                        )
                    } else {
                        // Botón "Chat Tutor" general → chat del curso con prefill de la nota
                        navController.navigate(
                            "tutor_history?contextType=course&contextId=$courseId" +
                            "&noteContext=$encodedNoteId&noteTitle=$encodedNoteTitle"
                        )
                    }
                },
                onStudyGuideClick = {
                    navController.navigate("${Screen.StudyGuide.route}/$noteId")
                },
                onNavigateToQuizCreation = {
                    navController.navigate("${Screen.QuizCreation.route}/$noteId")
                },
                onViewOriginalClick = { encodedPath ->
                    navController.navigate("${Screen.FileViewer.route}/$encodedPath")
                },
                // T-13 evolution: tap en un archivo adjunto → FileViewer
                // con el `uploadId` del archivo (NO del legacy).
                onFileClick = { fileId ->
                    val encoded = java.net.URLEncoder.encode(fileId, "UTF-8")
                    navController.navigate("${Screen.FileViewer.route}/$encoded")
                },
                onScheduleClick = { noteId, noteTitle ->
                    val encodedTitle = java.net.URLEncoder.encode(noteTitle, "UTF-8")
                    navController.navigate("${Screen.ScheduleNote.route}/$noteId?noteTitle=$encodedTitle")
                }
            )
        }

        composable(
            route = "${Screen.ReviewSession.route}/{noteId}?conceptName={conceptName}",
            arguments = listOf(
                navArgument("noteId") { type = NavType.StringType },
                navArgument("conceptName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            ReviewSessionScreen(
                onBackClick = { navController.popBackStack() },
                onComplete = { navController.popBackStack() },
            )
        }

        // CE-03: ruta para repaso por curso
        composable(
            route = "review_session/by-course/{courseId}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.StringType }
            )
        ) {
            ReviewSessionScreen(
                onBackClick = { navController.popBackStack() },
                onComplete = { navController.popBackStack() },
            )
        }

        // Ruta dedicada para repasos por concepto (F-09, disparado desde el
        // ConceptBottomSheet del grafo de Maps).
        composable(
            route = "review_session/by-concept?conceptName={conceptName}",
            arguments = listOf(
                navArgument("conceptName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            ReviewSessionScreen(
                onBackClick = { navController.popBackStack() },
                onComplete = { navController.popBackStack() },
            )
        }

        // Ruta dedicada para el Mazo Diario Global (F-10 Opción 1)
        composable("daily_deck_session") {
            ReviewSessionScreen(
                onBackClick = { navController.popBackStack() },
                onComplete = { navController.popBackStack() },
            )
        }

        composable(
            route = "${Screen.QuizCreation.route}/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) {
            QuizCreationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Subscription.route,
            arguments = listOf(
                navArgument("reason") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val reason = backStackEntry.arguments?.getString("reason")
            SubscriptionScreen(
                onCloseClick = { navController.popBackStack() },
                reason = reason
            )
        }

        composable(
            route = "${Screen.ScheduleNote.route}/{noteId}?noteTitle={noteTitle}",
            arguments = listOf(
                navArgument("noteId") { type = NavType.StringType },
                navArgument("noteTitle") { type = NavType.StringType }
            )
        ) {
            pe.khipuai.app.ui.screens.planner.ScheduleNoteScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Screen.FileViewer.route}/{uploadId}",
            arguments = listOf(navArgument("uploadId") { type = NavType.StringType })
        ) { backStackEntry ->
            val uploadIdEncoded = backStackEntry.arguments?.getString("uploadId") ?: ""
            val decodedPath = java.net.URLDecoder.decode(uploadIdEncoded, "UTF-8")
            FileViewerScreen(
                uploadId = decodedPath,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTab = { tabIndex ->
                    when (tabIndex) {
                        0 -> navController.navigate(Screen.Home.route)
                        1 -> navController.navigate(Screen.Capture.route)
                        2 -> navController.navigate(Screen.Planner.route)
                        3 -> navController.navigate(Screen.Maps.route)
                        4 -> navController.navigate(Screen.Profile.route)
                    }
                },
                // T-11: tap en un concepto del día navega a su NoteDetail
                onConceptClick = { noteId ->
                    navController.navigate("${Screen.NoteDetail.route}/$noteId")
                }
            )
        }
        composable("search") {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNoteDetail = { noteId ->
                    navController.navigate("${Screen.NoteDetail.route}/$noteId")
                },
                onNavigateToMaps = { conceptName ->
                    val encodedConcept = java.net.URLEncoder.encode(conceptName, "UTF-8")
                    navController.navigate("${Screen.Maps.route}?highlightConcept=$encodedConcept")
                }
            )
        }
        composable("statistics") {
            StatisticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("achievements") {
            AchievementsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * T-04: resuelve un deep link emitido por una notificación y navega al destino
 * correspondiente. Es un top-level function (no @Composable) para no inflar
 * el recompose del NavHost.
 *
 * Convenciones (definidas en `NotificationDeepLinks`):
 *  - "planner"               → PlannerScreen
 *  - "analysis/{noteId}"     → AnalysisScreen con noteId
 *  - "achievements"          → AchievementsScreen
 */
private fun handleDeepLink(navController: NavHostController, deepLink: String) {
    val target = when {
        deepLink == "planner" -> Screen.Planner.route
        deepLink.startsWith("analysis/") -> {
            val noteId = deepLink.removePrefix("analysis/")
            "${Screen.Analysis.route}/$noteId"
        }
        deepLink.startsWith("scheduled/") -> {
            val noteId = deepLink.removePrefix("scheduled/")
            "${Screen.NoteDetail.route}/$noteId"
        }
        deepLink.startsWith("note/") -> {
            val noteId = deepLink.removePrefix("note/")
            "${Screen.NoteDetail.route}/$noteId"
        }
        deepLink == "achievements" -> "achievements"
        deepLink.startsWith("achievements/") -> "achievements"
        else -> {
            android.util.Log.w("DeepLink", "Deep link desconocido: $deepLink")
            return
        }
    }
    try {
        navController.navigate(target) {
            popUpTo(navController.graph.startDestinationId) {
                inclusive = false
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    } catch (e: Exception) {
        android.util.Log.e("DeepLink", "Fallo al navegar deep link '$deepLink': ${e.message}")
    }
}

// ── C-03: Exam routes ────────────────────────────────────────────────────

@Composable
fun ExamHostScreen(
    onBack: () -> Unit,
    viewModel: ExamViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    when (state.step) {
        ExamStep.CONFIG -> ExamConfigScreen(onBack = onBack, onStartExam = { })
        ExamStep.EXAM -> ExamScreen(onBack = onBack)
        ExamStep.RESULTS -> ExamResultScreen(onBackToCourse = onBack)
    }
}

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Home : Screen("home")
    object Capture : Screen("capture")
    object Planner : Screen("planner")
    object Maps : Screen("maps")
    object Profile : Screen("profile")
    object Processing : Screen("processing")
    object Analysis : Screen("analysis")
    object StudyGuide : Screen("study_guide")
    object Onboarding : Screen("onboarding")
    object Tutor : Screen("tutor")
    
    // Sprint 5 Nuevas Rutas
    object Courses : Screen("courses")
    object CreateCourse : Screen("create_course")
    object CourseDetail : Screen("course_detail")
    object NoteDetail : Screen("note_detail")
    object QuizCreation : Screen("quiz_creation")
    object Subscription : Screen("subscription?reason={reason}") {
        // Helper para construir la ruta con el reason opcional.
        fun create(reason: String? = null): String =
            if (reason.isNullOrBlank()) "subscription"
            else "subscription?reason=$reason"
    }
    object FileViewer : Screen("file_viewer")
    object Calendar : Screen("calendar")
    object ScheduleNote : Screen("schedule_note")
    object ReviewSession : Screen("review_session")
    object NotificationSettings : Screen("notification_settings")
    object Faq : Screen("faq")
    object Exam : Screen("exam")
}