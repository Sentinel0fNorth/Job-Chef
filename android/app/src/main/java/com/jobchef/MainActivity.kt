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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.jobchef.core.designsystem.theme.JobMatcherTheme
import com.jobchef.core.designsystem.util.UiNotificationManager
import com.jobchef.navigation.BottomNavBar
import com.jobchef.navigation.JobMatcherNavHost
import com.jobchef.feature.onboarding.presentation.DeviceEnrollmentScreen
import com.jobchef.feature.onboarding.presentation.DeviceEnrollmentViewModel
import org.koin.compose.viewmodel.koinViewModel
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
                val enrollmentViewModel: DeviceEnrollmentViewModel = koinViewModel()
                val enrollmentState by enrollmentViewModel.state.collectAsState()

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
                    bottomBar = if (enrollmentState.isRegistered) {
                        { BottomNavBar(navController = navController) }
                    } else {
                        {}
                    }
                ) { innerPadding ->
                    if (enrollmentState.isRegistered) {
                        JobMatcherNavHost(
                            navController = navController,
                            onRevokeDevice = enrollmentViewModel::revoke,
                            isRevokingDevice = enrollmentState.isRevoking,
                            deviceAccessError = enrollmentState.errorMessage,
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        DeviceEnrollmentScreen(
                            state = enrollmentState,
                            onRegister = enrollmentViewModel::register,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
