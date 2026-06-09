package com.example.goodmail.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.goodmail.ui.screens.auth.AuthScreen
import com.example.goodmail.ui.screens.detail.EmailDetailScreen
import com.example.goodmail.ui.screens.inbox.InboxScreen
import com.example.goodmail.ui.screens.rules.RulesScreen
import com.example.goodmail.ui.screens.settings.SettingsScreen

object Routes {
    const val AUTH = "auth"
    const val INBOX = "inbox"
    const val DETAIL = "detail"
    const val SETTINGS = "settings"
    const val RULES = "rules"
    const val ARG_EMAIL_ID = "emailId"
}

@Composable
fun GoodmailNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.AUTH) {
        composable(Routes.AUTH) {
            AuthScreen(
                onSignedIn = {
                    navController.navigate(Routes.INBOX) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.INBOX) {
            InboxScreen(
                onEmailClick = { emailId ->
                    navController.navigate("${Routes.DETAIL}/$emailId")
                },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onSignedOut = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.INBOX) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = "${Routes.DETAIL}/{${Routes.ARG_EMAIL_ID}}",
            arguments = listOf(navArgument(Routes.ARG_EMAIL_ID) { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "goodmail://email/{${Routes.ARG_EMAIL_ID}}" }),
        ) {
            EmailDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onRules = { navController.navigate(Routes.RULES) },
                onSignedOut = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.INBOX) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.RULES) {
            RulesScreen(onBack = { navController.popBackStack() })
        }
    }
}
