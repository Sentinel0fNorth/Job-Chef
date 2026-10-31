package com.jobchef

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.jobchef.core.designsystem.theme.JobMatcherTheme
import com.jobchef.core.designsystem.util.UiNotificationManager
import com.jobchef.navigation.BottomNavBar
import com.jobchef.navigation.JobMatcherNavHost
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val uiNotificationManager: UiNotificationManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JobMatcherTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(uiNotificationManager) {
                    uiNotificationManager.errors.collect { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = MaterialTheme.shapes.medium
                            )
                        }
                    },
                    bottomBar = {
                        BottomNavBar(navController = navController)
                    }
                ) { innerPadding ->
                    JobMatcherNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

