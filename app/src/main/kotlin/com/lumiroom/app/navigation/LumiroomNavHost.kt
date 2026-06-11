package com.lumiroom.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lumiroom.feature.ai_assistant.presentation.chat.ChatScreen
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
    val safeNavigateBack: () -> Unit = {
        if (!navController.popBackStack()) {
            navController.navigate(LumiroomRoutes.CATALOG) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

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
                onNavigateBack = safeNavigateBack,
            )
        }

        // ── Main / Catalog ─────────────────────────────────────────────────
        composable(LumiroomRoutes.CATALOG) {
            CatalogScreen(
                onNavigateToDetail = { id ->
                    navController.navigate(LumiroomRoutes.furnitureDetail(id))
                },
                onNavigateToAr = { roomId ->
                    if (roomId != null) {
                        navController.navigate(LumiroomRoutes.arWithRoom(roomId)) {
                            popUpTo(LumiroomRoutes.CATALOG) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        navController.navigate(LumiroomRoutes.AR) {
                            popUpTo(LumiroomRoutes.CATALOG) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onNavigateToSaved = { 
                    navController.navigate(LumiroomRoutes.SAVED_ROOMS) {
                        popUpTo(LumiroomRoutes.CATALOG) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToAi = { 
                    navController.navigate(LumiroomRoutes.AI_ASSISTANT) {
                        popUpTo(LumiroomRoutes.CATALOG) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToSettings = { navController.navigate(LumiroomRoutes.SETTINGS) },
                onNavigateToFavorites = { navController.navigate(LumiroomRoutes.FAVORITES) },
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
                onNavigateBack = safeNavigateBack,
            )
        }

        // ── AR ─────────────────────────────────────────────────────────────
        // Old AR route removed as ArViewModel now requires roomId
        
        composable(
            route = LumiroomRoutes.AR_WITH_ROOM,
            arguments = listOf(androidx.navigation.navArgument("roomId") {
                type = androidx.navigation.NavType.StringType
            })
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId")
            ArScreen(
                furnitureId = null,
                onNavigateToCatalog = { 
                    navController.navigate(LumiroomRoutes.CATALOG) {
                        popUpTo(LumiroomRoutes.CATALOG) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToPlanner = { navController.navigate(LumiroomRoutes.roomPlanner(roomId ?: "")) },
                onNavigateToAi = { 
                    navController.navigate(LumiroomRoutes.AI_ASSISTANT) {
                        popUpTo(LumiroomRoutes.CATALOG) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateBack = safeNavigateBack,
            )
        }

        // 🟢 Room Planner 🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢
        composable(
            route = LumiroomRoutes.ROOM_PLANNER,
            arguments = listOf(androidx.navigation.navArgument("planId") {
                type = androidx.navigation.NavType.StringType
            })
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getString("planId")
            RoomPlannerScreen(
                onNavigateBack = safeNavigateBack,
                onNavigateToAr = { navController.navigate(LumiroomRoutes.arWithRoom(planId ?: "")) }
            )
        }

        // ── Saved Rooms ────────────────────────────────────────────────────
        composable(LumiroomRoutes.SAVED_ROOMS) {
            SavedRoomsScreen(
                onNavigateToAr = { roomId ->
                    navController.navigate(LumiroomRoutes.arWithRoom(roomId))
                },
                onNavigateBack = safeNavigateBack,
            )
        }

        // ── Favorites ──────────────────────────────────────────────────────
        composable(LumiroomRoutes.FAVORITES) {
            com.lumiroom.feature.catalog.presentation.FavoritesScreen(
                onNavigateBack = safeNavigateBack,
                onNavigateToDetail = { id -> navController.navigate(LumiroomRoutes.furnitureDetail(id)) }
            )
        }

        // ── AI Assistant ───────────────────────────────────────────────────
        composable(LumiroomRoutes.AI_ASSISTANT) {
            ChatScreen(
                onNavigateBack = safeNavigateBack
            )
        }

        // ── Settings ───────────────────────────────────────────────────────
        composable(LumiroomRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = safeNavigateBack,
                onNavigateToAbout = { navController.navigate(LumiroomRoutes.ABOUT) },
                onSignOut = {
                    navController.navigate(LumiroomRoutes.SIGN_IN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        // ── About ──────────────────────────────────────────────────────────
        composable(LumiroomRoutes.ABOUT) {
            com.lumiroom.feature.settings.presentation.AboutScreen(
                onNavigateBack = safeNavigateBack
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
    const val ROOM_PLANNER      = "room_planner/{planId}"
    const val SAVED_ROOMS       = "saved_rooms"
    const val FAVORITES         = "favorites"
    const val AI_ASSISTANT      = "ai_assistant"
    const val SETTINGS          = "settings"
    const val ABOUT             = "about"

    fun furnitureDetail(id: String)  = "catalog/detail/$id"
    fun arWithFurniture(id: String)  = "ar?furnitureId=$id"
    fun arWithRoom(roomId: String)   = "ar/room/$roomId"
    fun roomPlanner(planId: String)  = "room_planner/$planId"
}
