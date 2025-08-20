package com.appgurjant.stickynotes.ui.screens.settings

import SetPinBottomSheet
import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.app.data.PLAY_STORE_APP_URL
import com.appgurjant.stickynotes.BuildConfig
import com.appgurjant.stickynotes.MainActivity
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.firebase.FirebaseEvent
import com.appgurjant.stickynotes.navigation.Screen
import com.appgurjant.stickynotes.ui.screens.NoteViewModel
import com.google.android.datatransport.runtime.ExecutionModule_ExecutorFactory.executor


@SuppressLint("ContextCastToActivity")
@Composable
fun SettingScreen(navController: NavController) {
    val context = LocalContext.current
    val noteViewModel : NoteViewModel = hiltViewModel<NoteViewModel>()
//    var isOpenBottomSheet by remember { mutableStateOf(false) }
//
//    if (isOpenBottomSheet) {
//        SetPinBottomSheet(navController) {
//            if(it.isNotEmpty()){
//                isOpenBottomSheet=false
//                noteViewModel.setAppPin(it)
//                Log.e("UserPin", it)
//            }
//
//        }
//    }


    val settingsCategories = listOf(
//        CategoryItem(
//            title = stringResource(R.string.security),
//            features = listOf(
//                FeatureItem("app_lock", "App PIN", R.drawable.ic_lock) {
//                    /* open App Lock */
//                    isOpenBottomSheet = true
//                }
//            )
//        ),
        CategoryItem(
            title = stringResource(R.string.preferences),
            features = listOf(
                FeatureItem(
                    "language",
                    stringResource(R.string.change_language),
                    R.drawable.ic_language
                ) {
                    /* change language */
                    FirebaseEvent.logEvent(context, FirebaseEvent.changeLanguageEvent)
                    val packageName = context.packageName
                    val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    context.startActivity(intent)
                }
            )
        ),
        CategoryItem(
            title = stringResource(R.string.support),
            features = listOf(
                FeatureItem("share", stringResource(R.string.tell_a_friend), R.drawable.ic_share) {
                    FirebaseEvent.logEvent(context, FirebaseEvent.appShareEvent)
                    shareApp(context)
                },
                FeatureItem("rate", stringResource(R.string.love_the_app), R.drawable.ic_love) {
                    FirebaseEvent.logEvent(context, FirebaseEvent.appRatingEvent)
                    openAppInPlayStore(context)

                }
            )
        )
    )
    SettingScreenUi(navController = navController, settingsCategories)


}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreenUi(navController: NavController, categories: List<CategoryItem>) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        modifier = Modifier.wrapContentWidth()
                    )
                },
                navigationIcon = {
                    Image(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_back),
                        "back",
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .size(30.dp)
                            .clickable {
                                navController.popBackStack()
                            },
                        alignment = Alignment.TopStart
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues),
            ) {
Column(modifier = Modifier.fillMaxWidth()) {
    LazyColumn (modifier = Modifier.weight(1f)){
        categories.forEach { category ->
            item {
                Text(
                    text = category.title,
                    fontFamily = FontFamily(Font(R.font.inter_semibold)),
                    modifier = Modifier.padding(16.dp)
                )
            }

            items(category.features) { feature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            feature.onClick()
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = feature.icon),
                        contentDescription = feature.title
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = feature.title,
                        fontFamily = FontFamily(Font(R.font.inter_regular)),
                    )
                }
            }
        }
    }
    Text(
        text = "App Version :  ${BuildConfig.VERSION_NAME}",
        fontFamily = FontFamily(Font(R.font.inter_regular)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        textAlign = TextAlign.Center,
        fontSize = 12.sp,
        color = Color.LightGray,
    )
}



        }

    }

}

fun openAppInPlayStore(context: Context) {

    try {
        // Open in Play Store app
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_APP_URL))
        )
    } catch (e: Exception) {
        // Fallback → Open in browser
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_APP_URL))
        )
    }
}

fun shareApp(context: Context) {
    val packageName = context.packageName
    val appLink = PLAY_STORE_APP_URL

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
        putExtra(
            Intent.EXTRA_TEXT,
            "Every idea deserves a secure place. Capture yours with NoteZia.\n\n$appLink"
        )
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share via"))
}

data class FeatureItem(
    val id: String,
    val title: String,
    val icon: Int,           // Drawable resource id
    val onClick: () -> Unit  // Action when clicked
)

data class CategoryItem(
    val title: String,
    val features: List<FeatureItem>
)


@Composable
@Preview
fun SettingScreenPreview() {
    SettingScreenUi(navController = rememberNavController(), listOf())
}

