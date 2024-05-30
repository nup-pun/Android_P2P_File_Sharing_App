package com.xianfeng.wjcscx.screen

import com.xianfeng.wjcscx.FileViewModel
import com.xianfeng.wjcscx.NetworkService

import android.net.nsd.NsdServiceInfo
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xianfeng.wjcscx.screen.FilesScreen
import com.xianfeng.wjcscx.screen.MainScreen
import com.xianfeng.wjcscx.screen.ReceiveScreen
import com.xianfeng.wjcscx.screen.SendScreen

@Composable
fun AppNavigationScreen(
    fileViewModel: FileViewModel,
    networkService: NetworkService,
    onDeviceSelected: (NsdServiceInfo) -> Unit,
    selectFilesLauncher: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    NavHost(navController = navController, startDestination = "mainScreen") {

        composable(
            route = "mainScreen",
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    tween(1000)
                )
            },
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    tween(1000)
                )
            }
        ) {
            MainScreen(
                onSendClick = { navController.navigate("sendScreen") },
                onReceiveClick = { navController.navigate("receiveScreen") },
                onReceivedFilesClick = { navController.navigate("fileScreen") }
            )
        }

        composable(
            route = "sendScreen",
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    tween(1000)
                )
            },
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    tween(1000)
                )
            }
        ) {
            SendScreen(
                onFileSelectClick = { selectFilesLauncher.invoke() },
                networkService = networkService,
                onDeviceSelected = onDeviceSelected,
                fileViewModel = fileViewModel
            )
        }

        composable(
            route = "receiveScreen",
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    tween(500)
                )
            },
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    tween(500)
                )
            }
        ) {
            ReceiveScreen(networkService = networkService, fileViewModel = fileViewModel)
        }

        composable(
            route = "fileScreen",
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    tween(500)
                )
            },
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    tween(500)
                )
            }
        ) {
            FilesScreen(
                fileViewModel = fileViewModel,
                onFileClick = { file -> fileViewModel.openFile(file, context) },
                onBackClick = { navController.navigateUp() }
            )
        }
    }
}