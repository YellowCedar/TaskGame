/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.cedaryellow.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.cedaryellow.ui.store.StoreScreen
import com.github.cedaryellow.ui.tag.TagDetailsScreen
import com.github.cedaryellow.ui.tag.TagLibraryScreen
import com.github.cedaryellow.ui.task.AddEditTaskScreen
import com.github.cedaryellow.ui.task.TaskDetailsScreen
import com.github.cedaryellow.ui.task.TaskScreen
import com.github.cedaryellow.ui.task.TagContentScreen

sealed class Screen(val route: String, val icon: @Composable () -> Unit, val label: @Composable () -> Unit) {
    object Tasks : Screen(
        route = "tasks",
        icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
        label = { Text("Tasks") }
    )
    object Store : Screen(
        route = "store",
        icon = { Icon(Icons.Filled.Store, contentDescription = null) },
        label = { Text("Store") }
    )
    object Tags : Screen(
        route = "tags",
        icon = { Icon(Icons.Filled.Tag, contentDescription = null) },
        label = { Text("Tags") }
    )
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = Screen.Tasks.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Tasks.route) { 
                TaskScreen(
                    onAddTask = { navController.navigate("add_task") },
                    onTaskClick = { taskId -> navController.navigate("task_details/$taskId") }
                ) 
            }
            composable(Screen.Store.route) { 
                StoreScreen() 
            }
            composable(Screen.Tags.route) {
                TagLibraryScreen(
                    onTagClick = { tagId -> navController.navigate("tag_details/$tagId") },
                    navController = navController
                )
            }
            composable("add_task") {
                AddEditTaskScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "task_details/{taskId}",
                arguments = listOf(navArgument("taskId") { type = NavType.IntType })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getInt("taskId") ?: return@composable
                TaskDetailsScreen(
                    taskId = taskId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTagContent = { tagId -> navController.navigate("tag_content/$tagId") }
                )
            }
            composable(
                route = "tag_content/{tagId}",
                arguments = listOf(navArgument("tagId") { type = NavType.IntType })
            ) { backStackEntry ->
                val tagId = backStackEntry.arguments?.getInt("tagId") ?: return@composable
                TagContentScreen(
                    tagId = tagId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "tag_details/{tagId}",
                arguments = listOf(navArgument("tagId") { type = NavType.IntType })
            ) { backStackEntry ->
                val tagId = backStackEntry.arguments?.getInt("tagId") ?: return@composable
                TagDetailsScreen(
                    tagId = tagId,
                    onNavigateBack = { navController.popBackStack() },
                    onTaskClick = { taskId -> navController.navigate("task_details/$taskId") },
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavHostController) {
    val items = listOf(Screen.Tasks, Screen.Tags, Screen.Store)
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        
        items.forEach { screen ->
            NavigationBarItem(
                icon = { screen.icon() },
                label = { screen.label() },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
