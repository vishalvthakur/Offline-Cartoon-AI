package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.ProcessingScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.VideoViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    private val viewModel: VideoViewModel by viewModels()

    // Request permissions launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val notifGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        if (notifGranted) {
            Log.d("MainActivity", "Notification permission granted.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        checkAndRequestPermissions()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "dashboard"
                    ) {
                        // 1. Dashboard (Home) Screen
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = {
                                    navController.navigate("editor")
                                },
                                onViewResult = { path ->
                                    val encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                                    navController.navigate("result/$encodedPath")
                                }
                            )
                        }

                        // 2. Video Editor Screen
                        composable("editor") {
                            EditorScreen(
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToProcessing = {
                                    navController.navigate("processing") {
                                        popUpTo("dashboard") { saveState = true }
                                    }
                                }
                            )
                        }

                        // 3. Background Processing Screen
                        composable("processing") {
                            ProcessingScreen(
                                viewModel = viewModel,
                                onCompleted = { path ->
                                    val encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                                    navController.navigate("result/$encodedPath") {
                                        popUpTo("dashboard")
                                    }
                                },
                                onCancelled = {
                                    navController.navigate("dashboard") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 4. Conversion Result Screen (ExoPlayer playback)
                        composable(
                            route = "result/{processedPath}",
                            arguments = listOf(
                                navArgument("processedPath") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val encodedPath = backStackEntry.arguments?.getString("processedPath") ?: ""
                            val decodedPath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())

                            ResultScreen(
                                viewModel = viewModel,
                                processedPath = decodedPath,
                                onNavigateHome = {
                                    navController.navigate("dashboard") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}

// Global logger helper
object Log {
    fun d(tag: String, msg: String) { android.util.Log.d(tag, msg) }
    fun i(tag: String, msg: String) { android.util.Log.i(tag, msg) }
    fun w(tag: String, msg: String) { android.util.Log.w(tag, msg) }
    fun e(tag: String, msg: String) { android.util.Log.e(tag, msg) }
    fun e(tag: String, msg: String, tr: Throwable) { android.util.Log.e(tag, msg, tr) }
}
