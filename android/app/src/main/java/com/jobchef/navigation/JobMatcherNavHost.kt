package com.jobchef.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jobchef.feature.discovery.presentation.SearchScreen
import com.jobchef.feature.discovery.presentation.SearchViewModel
import com.jobchef.feature.discovery.presentation.SwipeDeckScreen
import com.jobchef.feature.discovery.presentation.SwipeDeckViewModel
import com.jobchef.feature.resume.presentation.vault.ResumeVaultScreen
import com.jobchef.feature.resume.presentation.vault.ResumeVaultViewModel
import androidx.navigation.toRoute
import com.jobchef.feature.saved.presentation.SavedOpportunitiesScreen
import com.jobchef.feature.saved.presentation.SavedOpportunitiesViewModel
import com.jobchef.feature.tailor.presentation.export.ExportScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun JobMatcherNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Discovery,
        modifier = modifier
    ) {
        composable<Screen.Vault> {
            val viewModel: ResumeVaultViewModel = koinViewModel()
            val state by viewModel.state.collectAsState()
            ResumeVaultScreen(
                state = state,
                onEvent = viewModel::onEvent
            )
        }

        composable<Screen.Discovery> {
            val viewModel: SearchViewModel = koinViewModel()
            val state by viewModel.state.collectAsState()
            SearchScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onSearchClicked = {
                    navController.navigate(Screen.SwipeDeck)
                }
            )
        }

        composable<Screen.SwipeDeck> {
            val viewModel: SwipeDeckViewModel = koinViewModel()
            val state by viewModel.state.collectAsState()
            SwipeDeckScreen(
                state = state,
                onEvent = viewModel::onEvent
            )
        }

        composable<Screen.Saved> {
            val viewModel: SavedOpportunitiesViewModel = koinViewModel()
            val state by viewModel.state.collectAsState()
            SavedOpportunitiesScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onJobDetailsClick = { job ->
                    navController.navigate(Screen.Export(job.id))
                }
            )
        }

        composable<Screen.Export> { navBackStackEntry ->
            val export = navBackStackEntry.toRoute<Screen.Export>()
            ExportScreen(jobId = export.jobId)
        }
    }
}
