package com.appgurjant.stickynotes.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.appgurjant.stickynotes.ui.screens.CreateNewNote
import com.appgurjant.stickynotes.ui.screens.GoogleSignInScreen
import com.appgurjant.stickynotes.ui.screens.allnotes.AllNotesScreen
import com.appgurjant.stickynotes.ui.screens.dashboard.DashboardScreen
import com.appgurjant.stickynotes.ui.screens.NoteDetailScreen
import com.appgurjant.stickynotes.ui.screens.OnboardingScreen
import com.appgurjant.stickynotes.ui.screens.qrScanner.QrScanScreen
import com.appgurjant.stickynotes.ui.screens.security.AppLockScreen
import com.appgurjant.stickynotes.ui.screens.SplashScreen

import com.appgurjant.stickynotes.ui.screens.settings.SettingScreen


@Composable
fun NoteZyNavGraph(navController:NavHostController){
 NavHost(navController = navController, startDestination = Screen.Splash.route){
     composable(Screen.Splash.route){
         SplashScreen(navController)
     }
     composable(Screen.OnboardingScreen.route){
         OnboardingScreen(navController)
     }
     composable(Screen.DashboardScreen.route){
         DashboardScreen(navController)
     }
     composable(Screen.AppLockScreen.route) {
         AppLockScreen(navController)
     }
     composable(Screen.AllNotesScreen.route) {
         AllNotesScreen(navController)
     }
     composable(Screen.CreateNewNoteScreen.route,arguments = listOf(
         navArgument("noteType") { type = NavType.StringType },
         navArgument("noteDescription") { type = NavType.StringType }
     )){backStackEntry->
         val noteType = backStackEntry.arguments?.getString("noteType").toString()
         val noteDescription = backStackEntry.arguments?.getString("noteDescription").toString()
         Log.e("NoteZyNavGraph", "noteType: $noteType noteDescription: $noteDescription")
         CreateNewNote(navController,noteType,noteDescription)
     }
     composable(Screen.NoteDetailScreen.route) { backStackEntry->
         val noteId = backStackEntry.arguments?.getString("noteId").toString()
         NoteDetailScreen(navController,noteId)
     }
     composable(Screen.QrScanScreen.route) {
         QrScanScreen(navController)
     }
     composable(Screen.SettingScreen.route) {
         SettingScreen(navController)
     }
     composable(Screen.GoogleSignInScreen.route) {
         GoogleSignInScreen(navController)
     }

 }

}
