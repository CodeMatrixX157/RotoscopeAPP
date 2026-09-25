package com.example.rotoscope.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rotoscope.ui.screens.ConverterScreen
import com.example.rotoscope.ui.screens.HomeScreen
import com.example.rotoscope.ui.screens.ImportScreen

object Routes {
    const val HOME = "home"
    const val IMPORT = "import"
    const val CONVERT = "convert"
    // Added as later modules land:
    // const val SELECT_OBJECT = "select_object"
    // const val PROCESSING = "processing"
    // const val EXPORT = "export"
}

@Composable
fun RotoscopeNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNewProject = { navController.navigate(Routes.IMPORT) },
                onConvertVideo = { navController.navigate(Routes.CONVERT) }
            )
        }
        composable(Routes.IMPORT) {
            ImportScreen(
                onSequenceReady = {
                    // TODO: navigate to Routes.SELECT_OBJECT once module 3 lands
                },
                onNeedConversion = { navController.navigate(Routes.CONVERT) }
            )
        }
        composable(Routes.CONVERT) {
            ConverterScreen()
        }
    }
}
