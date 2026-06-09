package com.example.goodmail.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.goodmail.ui.screens.auth.AuthScreen
import com.example.goodmail.ui.screens.inbox.InboxScreen

object Routes {
    const val AUTH = "auth"
    const val INBOX = "inbox"
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
                onSignedOut = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.INBOX) { inclusive = true }
                    }
                },
            )
        }
    }
}
