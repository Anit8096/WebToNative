package com.kmp.webtonative.navigation

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.kmp.webtonative.ui.screens.auth.AuthViewModel
import com.kmp.webtonative.ui.screens.auth.SignInScreen
import com.kmp.webtonative.ui.screens.history.HistoryScreen
import com.kmp.webtonative.ui.screens.home.HomeScreen
import com.kmp.webtonative.ui.screens.home.HomeViewModel
import com.kmp.webtonative.ui.screens.webView.WebViewScreen
import org.koin.androidx.compose.koinViewModel

private const val DURATION = 650
private const val FADE_DURATION = 550

@Composable
fun NavGraph() {
    val authViewModel: AuthViewModel = koinViewModel()
    val homeViewModel: HomeViewModel = koinViewModel()
    val startDestination = if (authViewModel.isLoggedIn) Route.Home else Route.Auth
    val backStack = rememberNavBackStack(startDestination)

    Surface(Modifier.fillMaxSize()) {
        NavDisplay(
            modifier = Modifier.fillMaxSize(),
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            transitionSpec = {
                slideInHorizontally(
                    animationSpec = tween(DURATION, easing = LinearOutSlowInEasing),
                    initialOffsetX = { it }
                ) + fadeIn(
                    animationSpec = tween(FADE_DURATION, easing = LinearOutSlowInEasing)
                ) togetherWith slideOutHorizontally(
                    animationSpec = tween(DURATION, easing = LinearOutSlowInEasing),
                    targetOffsetX = { -it }
                ) + fadeOut(
                    animationSpec = tween(FADE_DURATION, easing = LinearOutSlowInEasing)
                )
            },
            popTransitionSpec = {
                slideInHorizontally(
                    animationSpec = tween(DURATION, easing = LinearOutSlowInEasing),
                    initialOffsetX = { -it }
                ) + fadeIn(
                    animationSpec = tween(FADE_DURATION, easing = LinearOutSlowInEasing)
                ) togetherWith slideOutHorizontally(
                    animationSpec = tween(DURATION, easing = LinearOutSlowInEasing),
                    targetOffsetX = { it }
                ) + fadeOut(
                    animationSpec = tween(FADE_DURATION, easing = LinearOutSlowInEasing)
                )
            },
            predictivePopTransitionSpec = {
                slideInHorizontally(
                    animationSpec = tween(DURATION, easing = LinearOutSlowInEasing),
                    initialOffsetX = { -it }
                ) + fadeIn(
                    animationSpec = tween(FADE_DURATION, easing = LinearOutSlowInEasing)
                ) togetherWith slideOutHorizontally(
                    animationSpec = tween(DURATION, easing = LinearOutSlowInEasing),
                    targetOffsetX = { it }
                ) + fadeOut(
                    animationSpec = tween(FADE_DURATION, easing = LinearOutSlowInEasing)
                )
            },
            entryProvider = entryProvider {
                entry<Route.Auth> {
                    SignInScreen(
                        onSignInSuccess = {
                            backStack.removeLastOrNull()
                            backStack.add(Route.Home)
                        }
                    )
                }

                entry<Route.Home> {
                    HomeScreen(
                        onOpenUrl = { url -> backStack.add(Route.WebView(url)) },
                        onHistoryClick = { backStack.add(Route.History) },
                        onSignOut = {
                            backStack.removeLastOrNull()
                            backStack.add(Route.Auth)
                        }
                    )
                }

                entry<Route.WebView> { entry ->
                    WebViewScreen(
                        initialUrl = entry.url,
                        onBack = {
                            homeViewModel.setUrl(entry.url)
                            backStack.removeLastOrNull()
                        },
                        onClose = {
                            homeViewModel.clearUrl()
                            backStack.removeLastOrNull()
                        }
                    )
                }

                entry<Route.History> {
                    HistoryScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
            }
        )
    }
}