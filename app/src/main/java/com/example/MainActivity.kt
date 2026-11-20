package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.model.HdrProject
import com.example.data.model.ThemeMode
import com.example.data.preferences.PreferencesRepository
import com.example.localization.AppLanguage
import com.example.localization.LocalStrings
import com.example.localization.stringsFor
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.HdrStudiosTheme
import com.example.ui.viewmodel.EditorViewModel
import com.example.ui.viewmodel.StudioViewModel
import kotlinx.coroutines.launch

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_EDITOR = "editor"
const val ROUTE_SETTINGS = "settings"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HdrStudiosRoot()
        }
    }
}

@Composable
fun HdrStudiosRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember(context) { PreferencesRepository(context.applicationContext) }

    val selectedLanguage by preferences.languageFlow
        .collectAsStateWithLifecycle(initialValue = AppLanguage.ENGLISH)
    val selectedTheme by preferences.themeModeFlow
        .collectAsStateWithLifecycle(initialValue = ThemeMode.OBSIDIAN)
    val onboardingCompleted by preferences.onboardingCompletedFlow
        .collectAsStateWithLifecycle(initialValue = false)

    var showSplash by remember { mutableStateOf(true) }
    var forceOnboarding by remember { mutableStateOf(false) }

    val strings = remember(selectedLanguage) { stringsFor(selectedLanguage) }

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalLayoutDirection provides selectedLanguage.layoutDirection
    ) {
        HdrStudiosTheme(themeMode = selectedTheme) {
            val stage = when {
                showSplash -> "splash"
                !onboardingCompleted || forceOnboarding -> "onboarding"
                else -> "studio"
            }

            AnimatedContent(
                targetState = stage,
                transitionSpec = {
                    (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                        scaleIn(
                            initialScale = 0.96f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                        ))
                        .togetherWith(fadeOut())
                },
                label = "root_stage"
            ) { activeStage ->
                when (activeStage) {
                    "splash" -> SplashScreen(onFinished = { showSplash = false })

                    "onboarding" -> OnboardingScreen(
                        onComplete = {
                            forceOnboarding = false
                            scope.launch { preferences.setOnboardingCompleted(true) }
                        }
                    )

                    else -> StudioNav(
                        preferences = preferences,
                        selectedLanguage = selectedLanguage,
                        selectedThemeMode = selectedTheme,
                        onReplayOnboarding = { forceOnboarding = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioNav(
    preferences: PreferencesRepository,
    selectedLanguage: AppLanguage,
    selectedThemeMode: ThemeMode,
    onReplayOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val studioViewModel: StudioViewModel = viewModel(
        factory = StudioViewModel.factory(context, preferences)
    )
    var activeProject by remember { mutableStateOf<HdrProject?>(null) }

    NavHost(
        navController = navController,
        startDestination = ROUTE_DASHBOARD
    ) {
        composable(ROUTE_DASHBOARD) {
            DashboardScreen(
                viewModel = studioViewModel,
                onOpenProject = { project ->
                    activeProject = project
                    navController.navigate(ROUTE_EDITOR)
                },
                onOpenSettings = {
                    navController.navigate(ROUTE_SETTINGS)
                }
            )
        }

        composable(ROUTE_EDITOR) {
            val project = activeProject
            if (project != null) {
                val editorViewModel: EditorViewModel = viewModel(
                    key = project.id,
                    factory = EditorViewModel.factory(context, project)
                )
                EditorScreen(
                    viewModel = editorViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                selectedLanguage = selectedLanguage,
                selectedThemeMode = selectedThemeMode,
                onSelectLanguage = { lang -> studioViewModel.updateLanguage(lang) },
                onSelectThemeMode = { mode -> studioViewModel.updateThemeMode(mode) },
                onReplayOnboarding = {
                    navController.popBackStack()
                    onReplayOnboarding()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
