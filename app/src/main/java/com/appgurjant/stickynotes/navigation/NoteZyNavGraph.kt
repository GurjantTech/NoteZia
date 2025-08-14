package com.appgurjant.stickynotes.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.appgurjant.stickynotes.AppUtil.AppEnum
import com.appgurjant.stickynotes.ui.screens.CreateNewNote
import com.appgurjant.stickynotes.ui.screens.DashboardScreen

import com.appgurjant.stickynotes.ui.screens.NoteDetailScreen
import com.appgurjant.stickynotes.ui.screens.OnboardingScreen
import com.appgurjant.stickynotes.ui.screens.qrScanner.QrScanScreen
import com.appgurjant.stickynotes.ui.screens.SplashScreen


@Composable
fun NoteZyNavGraph(navController:NavHostController){
 NavHost(navController = navController, startDestination = "splash"){
     composable(Screen.Splash.route){
         SplashScreen(navController)
     }
     composable(Screen.OnboardingScreen.route){
         OnboardingScreen(navController)
     }
     composable(Screen.DashboardScreen.route){
         DashboardScreen(navController)
     }
     composable(Screen.CreateNewNoteScreen.route,arguments = listOf(
         navArgument("noteType") { type = NavType.StringType },
         navArgument("voiceNote") { type = NavType.StringType }
     )){backStackEntry->
         val noteType = backStackEntry.arguments?.getString("noteType").toString()
         val voiceNote = backStackEntry.arguments?.getString("voiceNote").toString()
         Log.e("NoteZyNavGraph", "noteType: $noteType voiceNote: $voiceNote")
         CreateNewNote(navController,noteType,voiceNote)

     }
     composable(Screen.NoteDetailScreen.route) { backStackEntry->
         val noteId = backStackEntry.arguments?.getString("noteId").toString()
         NoteDetailScreen(navController,noteId)
     }
     composable(Screen.QrScanScreen.route) {
         QrScanScreen(navController)
     }


 }
}