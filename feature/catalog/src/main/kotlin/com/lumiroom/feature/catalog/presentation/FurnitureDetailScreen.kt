package com.lumiroom.feature.catalog.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumiroom.core.ui.components.LoadingOverlay
import com.lumiroom.core.ui.theme.LumiroomPrimary
// Note: io.github.sceneview Compose API can vary, using basic AR or 3D placeholder if not compiling
import io.github.sceneview.Scene
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.node.ModelNode
import io.github.sceneview.math.Position
import androidx.compose.ui.Alignment
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.Float3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FurnitureDetailScreen(
    furnitureId: String, // Kept for interface compatibility
    onNavigateToAr: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: FurnitureDetailViewModel = hiltViewModel(),
) {
    val furniture by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(furniture?.name ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Button(
                    onClick = onNavigateToAr,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = LumiroomPrimary),
                ) {
                    Text("Place in AR", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            if (furniture == null) {
                LoadingOverlay()
            } else {
                val item = furniture!!
                Column {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val engine = rememberEngine()
                            val modelLoader = rememberModelLoader(engine)
                            val childNodes = rememberNodes()

                            var rotX by remember { mutableFloatStateOf(0f) }
                            var rotY by remember { mutableFloatStateOf(0f) }

                            LaunchedEffect(item.modelPath) {
                                val modelInstance = modelLoader.loadModelInstance("models/${item.modelPath.substringAfterLast("/")}")
                                if (modelInstance != null) {
                                    val pivotNode = io.github.sceneview.node.Node(engine = engine).apply {
                                        position = Position(0f, -0.2f, -2.0f) // Keep basic camera depth offset
                                    }
                                    
                                    val modelNode = ModelNode(modelInstance)
                                    // Use mathematical bounding box to offset model so origin is exactly at the geometric center
                                    val boundingBox = modelInstance.asset?.boundingBox
                                    if (boundingBox != null) {
                                        val center = boundingBox.center
                                        modelNode.position = Position(-center[0], -center[1], -center[2])
                                    } else {
                                        modelNode.centerOrigin(Position(0f, 0f, 0f))
                                    }
                                    
                                    pivotNode.addChildNode(modelNode)
                                    childNodes.clear()
                                    childNodes.add(pivotNode)
                                }
                            }

                            LaunchedEffect(rotX, rotY) {
                                val pivot = childNodes.firstOrNull()
                                if (pivot != null) {
                                    pivot.rotation = io.github.sceneview.math.Rotation(rotX, rotY, 0f)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            // Horizontal swipe -> rotY, Vertical swipe -> rotX
                                            rotY += dragAmount.x * 0.5f
                                            rotX = (rotX + dragAmount.y * 0.5f).coerceIn(-90f, 90f) // Prevent flipping upside down
                                        }
                                    }
                            ) {
                                Scene(
                                    modifier = Modifier.fillMaxSize(),
                                    engine = engine,
                                    modelLoader = modelLoader,
                                    childNodes = childNodes
                                )

                                Text(
                                    "Swipe to rotate model",
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )

                                IconButton(
                                    onClick = {
                                        rotX = 0f
                                        rotY = 0f
                                    },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset Rotation")
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(text = "Category: ${item.category}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(text = "Description: ${item.description}", style = MaterialTheme.typography.bodyMedium)
                    
                    Spacer(Modifier.height(16.dp))
                    Text(text = "Dimensions", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Width: ${item.width} cm")
                        Text("Height: ${item.height} cm")
                        Text("Depth: ${item.depth} cm")
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    if (item.priceEstimate != null) {
                        Text(text = "Estimated Price: ₹${String.format("%.2f", item.priceEstimate)}", style = MaterialTheme.typography.titleMedium, color = LumiroomPrimary)
                    }
                }
            }
        }
    }
}
