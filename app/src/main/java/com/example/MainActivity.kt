package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBlack
import com.example.viewmodel.EditorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: EditorViewModel = viewModel()
                val projects by viewModel.projects.collectAsState()
                val currentProject by viewModel.currentProject.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBlack)
                ) {
                    AnimatedContent(
                        targetState = currentProject != null,
                        transitionSpec = {
                            if (targetState) {
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width / 3 } + fadeOut()
                                )
                            } else {
                                (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut()
                                )
                            }
                        },
                        label = "ScreenTransition"
                    ) { isEditing ->
                        if (isEditing) {
                            EditorScreen(
                                viewModel = viewModel,
                                onBack = {
                                    viewModel.closeProject()
                                }
                            )
                        } else {
                            HomeScreen(
                                projects = projects,
                                stockClips = viewModel.getStockClips(),
                                onOpenProject = { projectId ->
                                    viewModel.selectProject(projectId)
                                },
                                onCreateNewProject = { name, ratio, clips ->
                                    viewModel.createNewProject(name, ratio, clips)
                                },
                                onDuplicateProject = { projectId ->
                                    viewModel.duplicateProject(projectId)
                                },
                                onDeleteProject = { projectId ->
                                    viewModel.deleteProject(projectId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
