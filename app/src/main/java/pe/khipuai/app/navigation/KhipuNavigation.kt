package pe.khipuai.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pe.khipuai.app.ui.screens.auth.LoginScreen
import pe.khipuai.app.ui.screens.auth.RegisterScreen
import pe.khipuai.app.ui.screens.auth.OnboardingScreen
import pe.khipuai.app.ui.screens.home.HomeScreen
import pe.khipuai.app.ui.screens.capture.CaptureScreen
import pe.khipuai.app.ui.screens.planner.PlannerScreen
import pe.khipuai.app.ui.screens.planner.CalendarScreen
import pe.khipuai.app.ui.screens.maps.MapsScreen
import pe.khipuai.app.ui.screens.profile.ProfileScreen
import pe.khipuai.app.ui.screens.processing.ProcessingScreen
import pe.khipuai.app.ui.screens.analysis.AnalysisScreen
import pe.khipuai.app.ui.screens.studyguide.StudyGuideScreen
import pe.khipuai.app.ui.screens.tutor.TutorChatScreen
import pe.khipuai.app.ui.screens.courses.CoursesScreen
import pe.khipuai.app.ui.screens.courses.CreateCourseScreen
import pe.khipuai.app.ui.screens.coursedetail.CourseDetailScreen
import pe.khipuai.app.ui.screens.notedetail.NoteDetailScreen
import pe.khipuai.app.ui.screens.quiz.QuizCreationScreen
import pe.khipuai.app.ui.screens.subscription.SubscriptionScreen
import pe.khipuai.app.ui.screens.fileviewer.FileViewerScreen

@Composable
fun KhipuNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
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
                }
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
                onNavigateToCourseDetail = { courseId ->
                    navController.navigate("${Screen.CourseDetail.route}/$courseId")
                },
                onNavigateToFileViewer = { encodedPath ->
                    navController.navigate("${Screen.FileViewer.route}/$encodedPath")
                },
                onNavigateToNoteDetail = { noteId ->
                    navController.navigate("${Screen.NoteDetail.route}/$noteId")
                },
                onNavigateToSubscription = {
                    navController.navigate(Screen.Subscription.route)
                }
            )
        }

        composable(Screen.Capture.route) {
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
                onNavigateToSubscription = {
                    navController.navigate(Screen.Subscription.route)
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
                }
            )
        }

        composable(Screen.Maps.route) {
            MapsScreen(
                onNavigateToTab = { tabIndex ->
                    when (tabIndex) {
                        0 -> navController.navigate(Screen.Home.route)
                        1 -> navController.navigate(Screen.Capture.route)
                        2 -> navController.navigate(Screen.Planner.route)
                        3 -> { /* Already on Maps */ }
                        4 -> navController.navigate(Screen.Profile.route)
                    }
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
                onNavigateToSubscription = {
                    navController.navigate(Screen.Subscription.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true } // Limpieza total del historial
                    }
                }
            )
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
                }
            )
        }

        composable(
            route = "${Screen.StudyGuide.route}/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) {
            StudyGuideScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Screen.Tutor.route}/{sessionId}?courseId={courseId}",
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("courseId") {
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
        ) {
            CourseDetailScreen(
                onBackClick = { navController.popBackStack() },
                onNoteClick = { noteId ->
                    navController.navigate("${Screen.NoteDetail.route}/$noteId")
                },
                onExpandMapClick = {
                    navController.navigate(Screen.Maps.route)
                }
            )
        }

        composable(
            route = "${Screen.NoteDetail.route}/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) {
            NoteDetailScreen(
                onBackClick = { navController.popBackStack() },
                onReviewClick = { /* Acción para repaso */ },
                onAskTutorClick = {
                    // Navigate to tutor with a generated session id or hardcoded one since we just need it to open
                    navController.navigate("${Screen.Tutor.route}/new_session")
                },
                onViewOriginalClick = { encodedPath ->
                    navController.navigate("${Screen.FileViewer.route}/$encodedPath")
                }
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

        composable(Screen.Subscription.route) {
            SubscriptionScreen(
                onCloseClick = { navController.popBackStack() }
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
                onNavigateToTab = { tabIndex ->
                    when (tabIndex) {
                        0 -> navController.navigate(Screen.Home.route)
                        1 -> navController.navigate(Screen.Capture.route)
                        2 -> navController.navigate(Screen.Planner.route)
                        3 -> navController.navigate(Screen.Maps.route)
                        4 -> navController.navigate(Screen.Profile.route)
                    }
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
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
    object Subscription : Screen("subscription")
    object FileViewer : Screen("file_viewer")
    object Calendar : Screen("calendar")
}