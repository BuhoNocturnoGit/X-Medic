package com.example.xmedic_v100.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.xmedic_v100.data.repository.AuthRepository
import com.example.xmedic_v100.data.repository.UserRepository
import com.example.xmedic_v100.ui.screens.LoginScreen
import com.example.xmedic_v100.ui.screens.RegisterScreen
import com.example.xmedic_v100.ui.screens.RoleSelectionScreen
import com.example.xmedic_v100.ui.screens.SplashScreen
import com.example.xmedic_v100.ui.screens.camera.CameraScannerScreen
import com.example.xmedic_v100.ui.screens.main.MainContainerScreen
import com.example.xmedic_v100.ui.screens.processing.ProcessingScreen
import com.example.xmedic_v100.ui.screens.store.TokenStoreScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object RoleSelection : Screen("role_selection")
    object Main : Screen("main")
    object TokenStore : Screen("token_store")
    object CameraScanner : Screen("camera_scanner")
    object Processing : Screen("processing/{imageUri}") {
        fun createRoute(imageUri: Uri): String = "processing/${Uri.encode(imageUri.toString())}"
    }
}

@Composable
fun XmedicNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateBack = {
                    // Volver o salir
                },
                onLoginSuccess = {
                    navController.navigate(Screen.RoleSelection.route)
                },
                onForgotPassword = {
                    // Restablecimiento procesado por ViewModel
                },
                onCreateAccount = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                onNavigateBack = {
                    AuthRepository().signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.RoleSelection.route) { inclusive = true }
                    }
                },
                onContinue = { selectedRole ->
                    UserRepository(context).saveUserRole(selectedRole)
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.RoleSelection.route) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.RoleSelection.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainContainerScreen(
                onOpenStore = {
                    navController.navigate(Screen.TokenStore.route)
                },
                onSignOut = {
                    AuthRepository().signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onOpenScanner = {
                    navController.navigate(Screen.CameraScanner.route)
                }
            )
        }

        composable(Screen.CameraScanner.route) {
            CameraScannerScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onImageConfirmed = { uri ->
                    navController.navigate(Screen.Processing.createRoute(uri))
                },
                onTokensClick = {
                    navController.navigate(Screen.TokenStore.route)
                }
            )
        }

        composable(
            route = "processing/{imageUri}",
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: ""
            val imageUri = Uri.parse(Uri.decode(encodedUri))

            ProcessingScreen(
                imageUri = imageUri,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProcessingComplete = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.TokenStore.route) {
            val userRepository = UserRepository(context)
            TokenStoreScreen(
                tokenCount = userRepository.getTokenCount(),
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
