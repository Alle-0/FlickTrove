package com.cinetrack

import android.os.Bundle
import android.content.Intent

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.view.animation.AnticipateInterpolator
import androidx.core.animation.doOnEnd
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.*
import com.cinetrack.ui.navigation.SettingsRoute
import com.cinetrack.ui.navigation.SplashRoute
import com.cinetrack.ui.navigation.SurpriseMeRoute
import com.cinetrack.ui.navigation.UpdatesRoute
import com.cinetrack.ui.navigation.VistiRoute
import com.cinetrack.ui.navigation.SearchRoute
import com.cinetrack.ui.navigation.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.toRoute
import androidx.navigation.NavDestination.Companion.hasRoute
import com.cinetrack.ui.screens.*
import com.cinetrack.ui.viewmodel.*
import com.cinetrack.ui.theme.*
import com.cinetrack.ui.components.*
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.Alignment
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.zIndex
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import com.cinetrack.ui.assets.CustomIcons
import dev.chrisbanes.haze.hazeChild
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import com.cinetrack.ui.components.glass.hazeGlass
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.graphics.graphicsLayer
import com.cinetrack.ui.utils.bounceClick
import androidx.compose.ui.graphics.TransformOrigin
import com.cinetrack.ui.components.shared.MovieActionsManager
import com.cinetrack.ui.components.shared.LocalMovieActions
import com.cinetrack.ui.components.shared.GlobalMovieActions
import com.cinetrack.ui.components.shared.nextGridColumns
import com.cinetrack.ui.components.glass.hazeGlass
import androidx.compose.runtime.CompositionLocalProvider
import com.cinetrack.util.toComposeColor
import com.cinetrack.util.toComposeColorOrNull
import androidx.compose.ui.platform.LocalView
import android.graphics.Bitmap

import kotlinx.serialization.Serializable
import dagger.hilt.android.AndroidEntryPoint

@Serializable
data object DetailOverlayPlaceholder

val LocalDisabledBadges = androidx.compose.runtime.compositionLocalOf<Set<String>> { emptySet() }
val LocalTitleTextSizeMultiplier = androidx.compose.runtime.compositionLocalOf<Float> { 1.0f }
val LocalAdvancedVisualEffects = androidx.compose.runtime.compositionLocalOf<Boolean> { true }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val deepLinkIntent = mutableStateOf<Intent?>(null)

    @OptIn(ExperimentalSharedTransitionApi::class)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        deepLinkIntent.value = intent

        
        // Smooth exit animation for the native splash screen
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val fadeOut = android.animation.ObjectAnimator.ofFloat(
                splashScreenView.view,
                android.view.View.ALPHA,
                1f,
                0f
            )
            fadeOut.interpolator = android.view.animation.AccelerateInterpolator()
            fadeOut.duration = 400L
            fadeOut.doOnEnd { splashScreenView.remove() }
            fadeOut.start()
        }
        
        // Immersive Mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        setContent {
            FlickTroveApp(deepLinkIntent)
        } // setContent
} // onCreate

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkIntent.value = intent
    }
}
