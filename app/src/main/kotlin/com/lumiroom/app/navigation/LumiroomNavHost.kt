package com.lumiroom.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lumiroom.feature.ai_assistant.presentation.AiAssistantScreen
import com.lumiroom.feature.ar.presentation.ArScreen
import com.lumiroom.feature.auth.presentation.SignInScreen
import com.lumiroom.feature.auth.presentation.SignUpScreen
import com.lumiroom.feature.catalog.presentation.CatalogScreen
import com.lumiroom.feature.catalog.presentation.FurnitureDetailScreen
import com.lumiroom.feature.onboarding.OnboardingScreen
import com.lumiroom.feature.onboarding.SplashScreen
import com.lumiroom.feature.roomplanner.presentation.RoomPlannerScreen
import com.lumiroom.feature.savedrooms.presentation.SavedRoomsScreen
import com.lumiroom.feature.settings.presentation.SettingsScreen

/**
 * Root navigation host for Lumiroom.
 *
 * Defines all top-level routes. Nested navigation graphs for bottom-nav
 * destinations (Catalog, AR, Saved, AI) are defined within each feature's
 * own NavGraph extension.
 *
 * Route naming convention: snake_case, arguments use {curly_braces}.
 */
@Composable
fun LumiroomNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = LumiroomRoutes.SPLASH,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        // ── Splash / Onboarding / Auth ─────────────────────────────────────
        composable(LumiroomRoutes.SPLASH) {
            SplashScreen(
                onNavigateToOnboarding = { 
                    navController.navigate(LumiroomRoutes.ONBOARDING) {
                        popUpTo(LumiroomRoutes.SPLASH) { inclusive = true }
                    } 
                },
                onNavigateToAuth = { 
                    navController.navigate(LumiroomRoutes.SIGN_IN) {
                        popUpTo(LumiroomRoutes.SPLASH) { inclusive = true }
                    } 
                },
                onNavigateToMain = { 
                    navController.navigate(LumiroomRoutes.CATALOG) {
                        popUpTo(LumiroomRoutes.SPLASH) { inclusive = true }
                    } 
                },
            )
        }

        composable(LumiroomRoutes.ONBOARDING) {
            OnboardingScreen(
                onNavigateToSignIn = { 
                    navController.navigate(LumiroomRoutes.SIGN_IN) {
                        popUpTo(LumiroomRoutes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(LumiroomRoutes.SIGN_IN) {
            SignInScreen(
                onNavigateToMain = {
                    navController.navigate(LumiroomRoutes.CATALOG) {
                        popUpTo(LumiroomRoutes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate(LumiroomRoutes.SIGN_UP) },
            )
        }

        composable(LumiroomRoutes.SIGN_UP) {
            SignUpScreen(
                onNavigateToMain = {
                    navController.navigate(LumiroomRoutes.CATALOG) {
                        popUpTo(LumiroomRoutes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── Main / Catalog ─────────────────────────────────────────────────
        composable(LumiroomRoutes.CATALOG) {
            CatalogScreen(
                onNavigateToDetail = { id ->
                    navController.navigate(LumiroomRoutes.furnitureDetail(id))
                },
                onNavigateToAr = { navController.navigate(LumiroomRoutes.AR) },
                onNavigateToSaved = { navController.navigate(LumiroomRoutes.SAVED_ROOMS) },
                onNavigateToAi = { navController.navigate(LumiroomRoutes.AI_ASSISTANT) },
                onNavigateToSettings = { navController.navigate(LumiroomRoutes.SETTINGS) },
            )
        }

        composable(LumiroomRoutes.FURNITURE_DETAIL) { backStackEntry ->
            val furnitureId = backStackEntry.arguments?.getString("furnitureId") ?: return@composable
            FurnitureDetailScreen(
                furnitureId = furnitureId,
                onNavigateToAr = { 
                    navController.navigate(LumiroomRoutes.arWithFurniture(furnitureId)) {
                        popUpTo(LumiroomRoutes.CATALOG) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── AR ─────────────────────────────────────────────────────────────
        composable(
            route = LumiroomRoutes.AR,
            arguments = listOf(androidx.navigation.navArgument("furnitureId") {
                type = androidx.navigation.NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val furnitureId = backStackEntry.arguments?.getString("furnitureId")
            ArScreen(
                furnitureId = furnitureId,
                onNavigateToCatalog = { 
                    navController.navigate(LumiroomRoutes.CATALOG) {
                        popUpTo(LumiroomRoutes.CATALOG) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToPlanner = { navController.navigate(LumiroomRoutes.ROOM_PLANNER) },
                onNavigateToAi = { navController.navigate(LumiroomRoutes.AI_ASSISTANT) },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── Room Planner ───────────────────────────────────────────────────
        composable(LumiroomRoutes.ROOM_PLANNER) {
            RoomPlannerScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAi = { navController.navigate(LumiroomRoutes.AI_ASSISTANT) }
            )
        }

        // ── Saved Rooms ────────────────────────────────────────────────────
        composable(LumiroomRoutes.SAVED_ROOMS) {
            SavedRoomsScreen(
                onNavigateToAr = { roomId ->
                    navController.navigate(LumiroomRoutes.arWithRoom(roomId))
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── AI Assistant ───────────────────────────────────────────────────
        composable(LumiroomRoutes.AI_ASSISTANT) {
            AiAssistantScreen(
                roomDesignId = null,
                onNavigateToCatalog = { navController.navigate(LumiroomRoutes.CATALOG) },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── Settings ───────────────────────────────────────────────────────
        composable(LumiroomRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAbout = { navController.navigate(LumiroomRoutes.ABOUT) },
                onSignOut = {
                    navController.navigate(LumiroomRoutes.SIGN_IN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        // ── About ──────────────────────────────────────────────────────────
        composable(LumiroomRoutes.ABOUT) {
            com.lumiroom.feature.settings.presentation.AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Centralized route string constants and route builder functions.
 */
object LumiroomRoutes {
    const val SPLASH            = "splash"
    const val ONBOARDING        = "onboarding"
    const val SIGN_IN           = "sign_in"
    const val SIGN_UP           = "sign_up"
    const val CATALOG           = "catalog"
    const val FURNITURE_DETAIL  = "catalog/detail/{furnitureId}"
    const val AR                = "ar?furnitureId={furnitureId}"
    const val AR_WITH_ROOM      = "ar/room/{roomId}"
    const val ROOM_PLANNER      = "room_planner"
    const val SAVED_ROOMS       = "saved_rooms"
    const val AI_ASSISTANT      = "ai_assistant"
    const val SETTINGS          = "settings"
    const val ABOUT             = "about"

    fun furnitureDetail(id: String)  = "catalog/detail/$id"
    fun arWithFurniture(id: String)  = "ar?furnitureId=$id"
    fun arWithRoom(roomId: String)   = "ar/room/$roomId"
}
