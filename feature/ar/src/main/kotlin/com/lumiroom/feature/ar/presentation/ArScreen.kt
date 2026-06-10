package com.lumiroom.feature.ar.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumiroom.feature.ar.engine.LumiroomArSessionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import com.google.ar.core.Anchor
import com.lumiroom.feature.voice.presentation.VoiceHelpDialog
import com.lumiroom.feature.ar.utils.ArCaptureUtils
import com.lumiroom.feature.catalog.presentation.CatalogScreen
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchor
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.Node
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import kotlinx.coroutines.launch
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.Float3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArScreen(
    furnitureId: String?,
    onNavigateToCatalog: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ArViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val childNodes = rememberNodes()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var pendingFurnitureId by remember(furnitureId) { mutableStateOf(furnitureId) }
    var currentFrame by remember { mutableStateOf<Frame?>(null) }
    var showCatalogOverlay by remember { mutableStateOf(false) }
    val anchorCache = remember { mutableMapOf<String, Anchor>() }

    // Used for dragging delta calculations in ROTATE mode
    var lastTouchX by remember { mutableStateOf(0f) }
    var lastTouchY by remember { mutableStateOf(0f) }
    
    // Floating Overlays for Scale and Rotation
    var activeScaleText by remember { mutableStateOf<String?>(null) }
    var activeRotationText by remember { mutableStateOf<String?>(null) }

    // Ghost node for copy mode
    var ghostModelInstance by remember { mutableStateOf<com.google.android.filament.gltfio.FilamentInstance?>(null) }
    var ghostNode by remember { mutableStateOf<io.github.sceneview.node.ModelNode?>(null) }
    var ghostActive by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isPasteMode) {
        if (uiState.isPasteMode) {
            val copiedId = uiState.copiedFurnitureId
            val itemToClone = uiState.placedItems.find { it.placedItem.id == copiedId }
            if (itemToClone != null) {
                val modelPath = "models/${itemToClone.furniture.modelPath.substringAfterLast("/")}"
                try {
                    val instance = modelLoader.loadModelInstance(modelPath)
                    ghostModelInstance = instance
                    if (instance != null) {
                        ghostNode = io.github.sceneview.node.ModelNode(modelInstance = instance).apply {
                            centerOrigin(Position(0f, 0f, 0f))
                            scale = Scale(itemToClone.placedItem.scaleX, itemToClone.placedItem.scaleY, itemToClone.placedItem.scaleZ)
                            quaternion = Quaternion(itemToClone.placedItem.rotX, itemToClone.placedItem.rotY, itemToClone.placedItem.rotZ, itemToClone.placedItem.rotW)
                        }
                        ghostActive = true
                        ghostNode?.let { node ->
                            if (!childNodes.contains(node)) {
                                childNodes.add(node)
                            }
                        }
                    }
                } catch (e: Exception) {
                    ghostActive = false
                }
            }
        } else {
            ghostActive = false
            ghostNode?.let { childNodes.remove(it) }
            ghostNode?.destroy()
            ghostNode = null
            ghostModelInstance = null
        }
    }

    // Map UI State items to ModelNodes
    LaunchedEffect(uiState.placedItems, uiState.selectedItemIds, uiState.lockedItemIds, uiState.hiddenItemIds) {
        val currentIds = uiState.placedItems.map { it.placedItem.id }.toSet()
        val existingNodes = childNodes.mapNotNull { it.name }.toSet()
        
        val toRemove = existingNodes - currentIds
        childNodes.removeAll { it.name in toRemove }
        
        val toAdd = currentIds - existingNodes
        toAdd.forEach { id ->
            val placedItem = uiState.placedItems.find { it.placedItem.id == id } ?: return@forEach
            coroutineScope.launch {
                Log.d("ArPlacement", "Attempting to load model instance for item: $id")
                val modelInstance = try {
                    modelLoader.loadModelInstance("models/${placedItem.furniture.modelPath.substringAfterLast("/")}")
                } catch (e: Exception) {
                    null
                }
                if (modelInstance != null) {
                    Log.d("ArPlacement", "Model instance loaded successfully.")
                    
                    val anchor = anchorCache[id]
                    val baseNode: Node = if (anchor != null) {
                        Log.d("ArPlacement", "Attaching to cached ARCore AnchorNode.")
                        AnchorNode(engine, anchor).apply {
                            name = id
                        }
                    } else {
                        Log.d("ArPlacement", "No cached anchor found. Creating unanchored Node with DB pose.")
                        Node(engine).apply {
                            name = id
                            position = Position(placedItem.placedItem.posX, placedItem.placedItem.posY, placedItem.placedItem.posZ)
                            quaternion = dev.romainguy.kotlin.math.Quaternion(
                                placedItem.placedItem.rotX, placedItem.placedItem.rotY,
                                placedItem.placedItem.rotZ, placedItem.placedItem.rotW
                            )
                        }
                    }

                    val pivotNode = io.github.sceneview.node.Node(engine = engine).apply {
                        name = "pivot_$id"
                        this.scale = Scale(placedItem.placedItem.scaleX, placedItem.placedItem.scaleY, placedItem.placedItem.scaleZ)
                        this.quaternion = dev.romainguy.kotlin.math.Quaternion(
                            placedItem.placedItem.rotX, placedItem.placedItem.rotY,
                            placedItem.placedItem.rotZ, placedItem.placedItem.rotW
                        )
                    }

                    val floorAlignmentNode = io.github.sceneview.node.Node(engine = engine).apply {
                        name = "floor_$id"
                    }

                    val modelNode = object : ModelNode(modelInstance = modelInstance) {
                        override fun onMoveBegin(detector: io.github.sceneview.gesture.MoveGestureDetector, e: android.view.MotionEvent): Boolean {
                            if (uiState.lockedItemIds.contains(id)) return false
                            super.onMoveBegin(detector, e)
                            lastTouchX = e.x
                            lastTouchY = e.y
                            // Detach anchor to allow free movement during drag
                            if (uiState.interactionMode == InteractionMode.MOVE) {
                                (baseNode as? AnchorNode)?.anchor?.detach()
                            }
                            return true
                        }

                        override fun onMove(detector: io.github.sceneview.gesture.MoveGestureDetector, e: android.view.MotionEvent): Boolean {
                            if (uiState.lockedItemIds.contains(id)) return false
                            if (!uiState.selectedItemIds.contains(id)) return false
                            
                            if (uiState.interactionMode == InteractionMode.ROTATE) {
                                val dx = e.x - lastTouchX
                                lastTouchX = e.x
                                lastTouchY = e.y

                                // Rotate only around Y-axis for AR objects
                                val rotY = dev.romainguy.kotlin.math.Quaternion.fromAxisAngle(dev.romainguy.kotlin.math.Float3(0f, 1f, 0f), dx * 0.5f)
                                pivotNode.quaternion = pivotNode.quaternion * rotY
                                
                                val euler = dev.romainguy.kotlin.math.eulerAngles(pivotNode.quaternion)
                                val degrees = Math.toDegrees(euler.y.toDouble()).toInt()
                                activeRotationText = "$degrees°"
                                
                                return true
                            }
                            
                            if (uiState.interactionMode != InteractionMode.MOVE) return false

                            super.onMove(detector, e)
                            val hitResult = currentFrame?.hitTest(e.x, e.y)?.firstOrNull { hit ->
                                val trackable = hit.trackable
                                trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)
                            }
                            if (hitResult != null) {
                                val pose = hitResult.hitPose
                                baseNode.position = Position(pose.tx(), pose.ty(), pose.tz())
                            }
                            return true
                        }

                        override fun onMoveEnd(detector: io.github.sceneview.gesture.MoveGestureDetector, e: android.view.MotionEvent) {
                            super.onMoveEnd(detector, e)
                            
                            if (uiState.interactionMode == InteractionMode.MOVE) {
                                val hitResult = currentFrame?.hitTest(e.x, e.y)?.firstOrNull { hit ->
                                    val trackable = hit.trackable
                                    trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)
                                }
                                val anchorNode = baseNode as? AnchorNode
                                if (hitResult != null && anchorNode != null) {
                                    anchorNode.anchor?.detach()
                                    val newAnchor = hitResult.createAnchor()
                                    anchorNode.anchor = newAnchor
                                    
                                    val pose = newAnchor.pose
                                    viewModel.onPlaneTapped(
                                        instanceId = id,
                                        furnitureId = placedItem.furniture.id,
                                        hitPosX = pose.tx(), hitPosY = pose.ty(), hitPosZ = pose.tz(),
                                        rotX = pivotNode.quaternion.x, rotY = pivotNode.quaternion.y, rotZ = pivotNode.quaternion.z, rotW = pivotNode.quaternion.w
                                    )
                                }
                            }
                            
                            if (uiState.interactionMode == InteractionMode.ROTATE) {
                                activeRotationText = null
                                viewModel.onItemTransformed(
                                    itemId = id,
                                    posX = baseNode.worldPosition.x,
                                    posY = baseNode.worldPosition.y,
                                    posZ = baseNode.worldPosition.z,
                                    rotX = pivotNode.quaternion.x, rotY = pivotNode.quaternion.y, rotZ = pivotNode.quaternion.z, rotW = pivotNode.quaternion.w,
                                    scaleX = pivotNode.scale.x, scaleY = pivotNode.scale.y, scaleZ = pivotNode.scale.z,
                                    matrixJson = ""
                                )
                            }
                        }

                        override fun onRotate(detector: io.github.sceneview.gesture.RotateGestureDetector, e: android.view.MotionEvent): Boolean {
                            return false // Rotation handled by onMove 1-finger swipe
                        }

                        override fun onScale(detector: io.github.sceneview.gesture.ScaleGestureDetector, e: android.view.MotionEvent): Boolean {
                            if (uiState.lockedItemIds.contains(id)) return false
                            if (uiState.interactionMode != InteractionMode.SCALE || !uiState.selectedItemIds.contains(id)) return false
                            super.onScale(detector, e)
                            val factor = detector.scaleFactor
                            val newScaleX = (pivotNode.scale.x * factor).coerceIn(0.1f, 5.0f)
                            val newScaleY = (pivotNode.scale.y * factor).coerceIn(0.1f, 5.0f)
                            val newScaleZ = (pivotNode.scale.z * factor).coerceIn(0.1f, 5.0f)
                            pivotNode.scale = Scale(newScaleX, newScaleY, newScaleZ)
                            
                            val percentage = (newScaleX / placedItem.placedItem.initScaleX * 100).toInt()
                            activeScaleText = "$percentage%"
                            return true
                        }

                        override fun onScaleEnd(detector: io.github.sceneview.gesture.ScaleGestureDetector, e: android.view.MotionEvent) {
                            if (uiState.interactionMode != InteractionMode.SCALE || !uiState.selectedItemIds.contains(id)) return
                            super.onScaleEnd(detector, e)
                            activeScaleText = null
                            viewModel.onItemTransformed(
                                itemId = id,
                                posX = baseNode.worldPosition.x,
                                posY = baseNode.worldPosition.y,
                                posZ = baseNode.worldPosition.z,
                                rotX = pivotNode.quaternion.x, rotY = pivotNode.quaternion.y, rotZ = pivotNode.quaternion.z, rotW = pivotNode.quaternion.w,
                                scaleX = pivotNode.scale.x, scaleY = pivotNode.scale.y, scaleZ = pivotNode.scale.z,
                                matrixJson = ""
                            )
                        }
                    }.apply {
                        name = id
                        val boundingBox = modelInstance.asset?.boundingBox
                        if (boundingBox != null) {
                            val center = boundingBox.center
                            val halfExtent = boundingBox.halfExtent
                            // Position the floorAlignmentNode so the bottom of the object hits y=0
                            floorAlignmentNode.position = Position(0f, halfExtent[1] - center[1], 0f)
                            // Position the modelNode so the center is zeroed out in X and Z
                            position = Position(-center[0], 0f, -center[2])
                        } else {
                            centerOrigin(Position(0f, -1f, 0f)) // -1f = bottom alignment
                        }
                    }
                    
                    floorAlignmentNode.addChildNode(modelNode)
                    pivotNode.addChildNode(floorAlignmentNode)
                    baseNode.addChildNode(pivotNode)
                    baseNode.name = id // So it can be found for removal
                    childNodes.add(baseNode)
                    Log.d("ArPlacement", "Model attached to AR scene graph via ARPivotNode!")
                } else {
                    Log.e("ArPlacement", "Failed to load model instance!")
                }
            }
        }

        // Update properties
        childNodes.forEach { baseNode ->
            val id = baseNode.name ?: return@forEach
            val pivotNode = baseNode.childNodes.firstOrNull { it.name == "pivot_$id" }
            val modelNode = pivotNode?.childNodes?.filterIsInstance<ModelNode>()?.firstOrNull() ?: baseNode.childNodes.filterIsInstance<ModelNode>().firstOrNull()
            val isSelected = uiState.selectedItemIds.contains(id)
            
            if (pivotNode != null) {
                // Add or remove selection box
                val selectionBoxName = "selection_box_$id"
                val existingBox = pivotNode.childNodes.find { it.name == selectionBoxName }
                
                if (isSelected && existingBox == null) {
                    val boxSize = io.github.sceneview.math.Position(1.2f, 0.02f, 1.2f)
                    val boxNode = io.github.sceneview.node.CubeNode(
                        engine = engine,
                        size = boxSize,
                        center = io.github.sceneview.math.Position(0f, 0.01f, 0f)
                    ).apply {
                        name = selectionBoxName
                    }
                    pivotNode.addChildNode(boxNode)
                } else if (!isSelected && existingBox != null) {
                    pivotNode.removeChildNode(existingBox)
                    existingBox.destroy()
                }

                // Sync and animate external transform changes (e.g., Undo, Redo, Reset)
                val uiItem = uiState.placedItems.find { it.placedItem.id == id }?.placedItem

                
                if (uiItem != null && uiState.interactionMode != InteractionMode.MOVE && uiState.interactionMode != InteractionMode.ROTATE && uiState.interactionMode != InteractionMode.SCALE) {
                    val targetPos = io.github.sceneview.math.Position(uiItem.posX, uiItem.posY, uiItem.posZ)
                    val targetRot = dev.romainguy.kotlin.math.Quaternion(uiItem.rotX, uiItem.rotY, uiItem.rotZ, uiItem.rotW)
                    val targetScale = io.github.sceneview.math.Scale(uiItem.scaleX, uiItem.scaleY, uiItem.scaleZ)

                    val dist = dev.romainguy.kotlin.math.length(baseNode.position - targetPos)
                    val scaleDist = dev.romainguy.kotlin.math.length(pivotNode.scale - targetScale)
                    val rotDist = kotlin.math.abs(dev.romainguy.kotlin.math.dot(pivotNode.quaternion, targetRot))
                    
                    if (dist > 0.005f || scaleDist > 0.005f || rotDist < 0.999f) {
                        coroutineScope.launch {
                            val startPos = baseNode.position
                            val startRot = pivotNode.quaternion
                            val startScale = pivotNode.scale
                            val steps = 25 // 400ms at ~60fps
                            for (i in 0..steps) {
                                val t = i.toFloat() / steps
                                val smoothT = t * t * (3f - 2f * t) // ease in out
                                baseNode.position = dev.romainguy.kotlin.math.mix(startPos, targetPos, smoothT)
                                pivotNode.quaternion = dev.romainguy.kotlin.math.slerp(startRot, targetRot, smoothT)
                                pivotNode.scale = dev.romainguy.kotlin.math.mix(startScale, targetScale, smoothT)
                                kotlinx.coroutines.delay(16)
                            }
                            baseNode.position = targetPos
                            pivotNode.quaternion = targetRot
                            pivotNode.scale = targetScale
                        }
                    }
                }
            }
            baseNode.isVisible = !uiState.hiddenItemIds.contains(id)
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.toggleVoiceListening()
            } else {
                android.widget.Toast.makeText(context, "Microphone permission is required for voice commands.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    )

    fun onMicClicked() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.toggleVoiceListening()
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (uiState.showVoiceHelpDialog) {
        VoiceHelpDialog(onDismiss = { viewModel.onVoiceHelpDismissed() })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            environmentLoader = environmentLoader,
            childNodes = childNodes,
            planeRenderer = uiState.showPlaneVisualization,
            sessionConfiguration = { session, config ->
                LumiroomArSessionManager.configureSession(session, config)
                viewModel.onSessionInitializing()
            },
            onSessionUpdated = { session, frame ->
                currentFrame = frame
                if (uiState.sessionState == ArSessionState.Initializing) {
                    viewModel.onSessionScanning()
                }
                
                if (uiState.isPasteMode && ghostNode != null) {
                    val hitResultCenter = currentFrame?.hitTest(
                        context.resources.displayMetrics.widthPixels / 2f,
                        context.resources.displayMetrics.heightPixels / 2f
                    )?.firstOrNull { hit ->
                        hit.trackable is Plane
                    }
                    if (hitResultCenter != null) {
                        val pose = hitResultCenter.hitPose
                        ghostNode?.position = Position(pose.tx(), pose.ty(), pose.tz())
                    }
                }
            },
            onSessionFailed = { exception ->
                viewModel.onSessionError(exception.message ?: "Unknown AR Session Error")
            },
            onGestureListener = object : io.github.sceneview.gesture.GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {
                    if (node != null && node.name != null) {
                        viewModel.onItemSelected(node.name!!, multiSelect = false) // Add long press later for multi-select
                    } else {
                        viewModel.onItemSelected(null)
                        if (uiState.isPasteMode) {
                            val hitResult = currentFrame?.hitTest(e.x, e.y)?.firstOrNull { hit ->
                                val trackable = hit.trackable
                                trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)
                            } ?: currentFrame?.hitTest(
                                context.resources.displayMetrics.widthPixels / 2f,
                                context.resources.displayMetrics.heightPixels / 2f
                            )?.firstOrNull { hit ->
                                val trackable = hit.trackable
                                trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)
                            }
                            
                            if (hitResult != null) {
                                val pose = hitResult.hitPose
                                val instanceId = java.util.UUID.randomUUID().toString()
                                val anchor = hitResult.createAnchor()
                                anchorCache[instanceId] = anchor
                                
                                viewModel.onPastePlaneTapped(
                                    hitPosX = pose.tx(), hitPosY = pose.ty(), hitPosZ = pose.tz(),
                                    rotX = ghostNode?.quaternion?.x ?: 0f,
                                    rotY = ghostNode?.quaternion?.y ?: 0f,
                                    rotZ = ghostNode?.quaternion?.z ?: 0f,
                                    rotW = ghostNode?.quaternion?.w ?: 1f,
                                    newInstanceId = instanceId
                                )
                            }
                        } else if (pendingFurnitureId != null) {
                            val hitResult = currentFrame?.hitTest(e.x, e.y)?.firstOrNull { hit ->
                                val trackable = hit.trackable
                                trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)
                            }
                            if (hitResult != null) {
                                val pose = hitResult.hitPose
                                Log.d("ArPlacement", "Plane tapped! Creating ARCore Anchor at tx:${pose.tx()}, ty:${pose.ty()}, tz:${pose.tz()}")
                                
                                val instanceId = java.util.UUID.randomUUID().toString()
                                val anchor = hitResult.createAnchor()
                                anchorCache[instanceId] = anchor
                                
                                viewModel.onPlaneTapped(
                                    instanceId = instanceId,
                                    furnitureId = pendingFurnitureId!!,
                                    hitPosX = pose.tx(), hitPosY = pose.ty(), hitPosZ = pose.tz(),
                                    rotX = pose.qx(), rotY = pose.qy(), rotZ = pose.qz(), rotW = pose.qw()
                                )
                                pendingFurnitureId = null
                            } else {
                                Log.w("ArPlacement", "Hit result did not match any planes.")
                            }
                        }
                    }
                }
            }
        )

        // ── Top Bar overlay ───────────────────────────────────────────────────
        val window = (context as? android.app.Activity)?.window
        var showExportMenu by remember { mutableStateOf(false) }
        
        TopAppBar(
            title = { Text(uiState.currentRoomName) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.togglePlaneVisualization() }) {
                    Icon(if (uiState.showPlaneVisualization) Icons.Default.Visibility else Icons.Default.VisibilityOff, "Toggle Planes")
                }
                Box {
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Default.Share, "Export")
                    }
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export Summary") },
                            onClick = {
                                showExportMenu = false
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, viewModel.exportRoomSummary())
                                    type = "text/plain"
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Summary"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export CSV Inventory") },
                            onClick = {
                                showExportMenu = false
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, viewModel.exportInventoryCsv())
                                    type = "text/csv"
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Inventory"))
                            }
                        )
                    }
                }
                IconButton(onClick = {
                    window?.let {
                        coroutineScope.launch {
                            val result = ArCaptureUtils.captureAndSaveScreenshot(context, it)
                            if (result.isSuccess) {
                                android.widget.Toast.makeText(context, "Saved to Gallery", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "Capture Failed", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }) {
                    Icon(Icons.Default.CameraAlt, "Capture Screenshot")
                }
                IconButton(onClick = { viewModel.onSaveRoom(uiState.currentRoomName) }) {
                    Icon(Icons.Default.Save, "Save Room")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            ),
        )

        // ── Selection Toolbar (Top) ───────────────────────────────────────────
        if (uiState.selectedItemIds.isNotEmpty()) {
            SelectionToolbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 180.dp), // Google AR style bottom panel
                interactionMode = uiState.interactionMode,
                isLocked = uiState.selectedItemIds.all { uiState.lockedItemIds.contains(it) },
                canPaste = uiState.copiedFurnitureId != null,
                onSetInteractionMode = { viewModel.setInteractionMode(it) },
                onCopy = { viewModel.onCopySelected() },
                onPaste = { viewModel.onPaste() },
                onDelete = { viewModel.onRemoveSelectedItems() },
                onLockToggle = { 
                    uiState.selectedItemIds.forEach { viewModel.toggleLock(it) } 
                },
                onHideToggle = {
                    uiState.selectedItemIds.forEach { viewModel.toggleVisibility(it) }
                },
                onResetTransform = { viewModel.onResetTransform() },
                isMeasuring = uiState.isMeasuring,
                onMeasureToggle = { viewModel.toggleMeasuring() }
            )
        }
        
        // ── Paste Mode Overlay ────────────────────────────────────────────
        if (uiState.isPasteMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tap a surface to place duplicate", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(16.dp))
                    IconButton(
                        onClick = { viewModel.cancelPasteMode() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, "Cancel Paste")
                    }
                }
            }
        }

        // ── Analytics Panel (Top Left) ───────────────────────────────────────
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 120.dp, start = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Items: ${uiState.totalItemCount}", style = MaterialTheme.typography.bodySmall)
                Text("Cost: ₹${uiState.totalCostEstimate}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }

        // ── Furniture Details Panel (Left) ────────────────────────────────────
        if (uiState.selectedItemIds.size == 1) {
            val selectedId = uiState.selectedItemIds.first()
            val item = uiState.placedItems.find { it.placedItem.id == selectedId }?.furniture
            if (item != null) {
                FurnitureDetailsPanel(
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp),
                    furniture = item
                )
            }
        }
        
        // ── Measurement Panel (Right) ─────────────────────────────────────────
        if (uiState.isMeasuring && uiState.selectedItemIds.size == 1) {
            val selectedId = uiState.selectedItemIds.first()
            val item = uiState.placedItems.find { it.placedItem.id == selectedId }
            if (item != null) {
                Card(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Measurements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("W: ${String.format("%.2f", item.furniture.width * item.placedItem.scaleX)} cm")
                        Text("H: ${String.format("%.2f", item.furniture.height * item.placedItem.scaleY)} cm")
                        Text("D: ${String.format("%.2f", item.furniture.depth * item.placedItem.scaleZ)} cm")
                    }
                }
            }
        }
        
        // ── Active Gesture Overlays (Center) ───────────────────────────────────
        if (activeScaleText != null) {
            Box(modifier = Modifier.align(Alignment.Center).padding(bottom = 60.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(24.dp)).padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text(text = activeScaleText!!, color = Color.White, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            }
        }
        if (activeRotationText != null) {
            Box(modifier = Modifier.align(Alignment.Center).padding(bottom = 60.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(24.dp)).padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text(text = activeRotationText!!, color = Color.White, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            }
        }

        // ── Voice UI (Bottom Center, above control panel) ────────────────────
        if (uiState.isVoiceListening || uiState.voiceTranscript != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.isVoiceListening) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = uiState.voiceTranscript ?: "...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { onMicClicked() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 16.dp),
            containerColor = if (uiState.isVoiceListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            contentColor = if (uiState.isVoiceListening) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(if (uiState.isVoiceListening) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Voice Command")
        }

        // ── AR Control Panel (bottom) ─────────────────────────────────────────
        var showPlannerWarning by remember { mutableStateOf(false) }

        ArControlPanel(
            modifier = Modifier.align(Alignment.BottomCenter),
            canUndo = uiState.canUndo,
            canRedo = uiState.canRedo,
            onUndo = viewModel::onUndo,
            onRedo = viewModel::onRedo,
            onOpenCatalog = { showCatalogOverlay = true },
            onOpenPlanner = { showPlannerWarning = true },
        )

        if (showPlannerWarning) {
            AlertDialog(
                onDismissRequest = { showPlannerWarning = false },
                title = { Text("Leaving AR Mode") },
                text = { Text("Navigating to the 2D Planner will destroy the current AR tracking session. When you return, placed items will lose their floor alignment and float around the camera. Are you sure you want to continue?") },
                confirmButton = {
                    TextButton(onClick = { 
                        showPlannerWarning = false
                        onNavigateToPlanner()
                    }) {
                        Text("Continue")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPlannerWarning = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // ── Catalog Overlay ───────────────────────────────────────────────────
        if (showCatalogOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                CatalogScreen(
                    onNavigateToDetail = { id ->
                        pendingFurnitureId = id
                        showCatalogOverlay = false
                    },
                    onNavigateToAr = { showCatalogOverlay = false },
                    onNavigateToSaved = { showCatalogOverlay = false; onNavigateToCatalog() },
                    onNavigateToAi = { showCatalogOverlay = false; onNavigateToAi() },
                    onNavigateToSettings = { showCatalogOverlay = false }
                )
            }
        }
    }
}

@Composable
fun SelectionToolbar(
    modifier: Modifier = Modifier,
    interactionMode: InteractionMode,
    isLocked: Boolean = false,
    canPaste: Boolean = false,
    onSetInteractionMode: (InteractionMode) -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onDelete: () -> Unit,
    onLockToggle: () -> Unit,
    onHideToggle: () -> Unit,
    onResetTransform: () -> Unit,
    isMeasuring: Boolean = false,
    onMeasureToggle: () -> Unit,
) {
    Surface(
        modifier = modifier.wrapContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isLocked) {
                IconButton(onClick = onLockToggle) {
                    Icon(Icons.Default.LockOpen, "Unlock")
                }
                Text("Item Locked", modifier = Modifier.padding(end = 12.dp), style = MaterialTheme.typography.labelMedium)
            } else {
                IconButton(
                    onClick = { onSetInteractionMode(if(interactionMode == InteractionMode.MOVE) InteractionMode.IDLE else InteractionMode.MOVE) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = if(interactionMode == InteractionMode.MOVE) MaterialTheme.colorScheme.primary else Color.Transparent, contentColor = if(interactionMode == InteractionMode.MOVE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Default.OpenWith, "Move")
                }
                IconButton(
                    onClick = { onSetInteractionMode(if(interactionMode == InteractionMode.ROTATE) InteractionMode.IDLE else InteractionMode.ROTATE) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = if(interactionMode == InteractionMode.ROTATE) MaterialTheme.colorScheme.primary else Color.Transparent, contentColor = if(interactionMode == InteractionMode.ROTATE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Default.Refresh, "Rotate")
                }
                IconButton(
                    onClick = { onSetInteractionMode(if(interactionMode == InteractionMode.SCALE) InteractionMode.IDLE else InteractionMode.SCALE) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = if(interactionMode == InteractionMode.SCALE) MaterialTheme.colorScheme.primary else Color.Transparent, contentColor = if(interactionMode == InteractionMode.SCALE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Default.ZoomOutMap, "Scale")
                }
                VerticalDivider(modifier = Modifier.height(24.dp))
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.FileCopy, "Copy")
                }
                IconButton(onClick = onPaste, enabled = canPaste) {
                    Icon(Icons.Default.ContentPaste, "Paste", tint = if (canPaste) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f))
                }
                IconButton(onClick = onLockToggle) {
                    Icon(Icons.Default.Lock, "Lock")
                }
                IconButton(onClick = onHideToggle) {
                    Icon(Icons.Default.VisibilityOff, "Hide")
                }
                IconButton(onClick = onResetTransform) {
                    Icon(Icons.Default.Restore, "Reset Transform")
                }
                IconButton(
                    onClick = onMeasureToggle,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = if(isMeasuring) MaterialTheme.colorScheme.primary else Color.Transparent, contentColor = if(isMeasuring) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Default.Straighten, "Measure")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun FurnitureDetailsPanel(
    modifier: Modifier = Modifier,
    furniture: com.lumiroom.core.database.entity.FurnitureEntity
) {
    Card(
        modifier = modifier.width(200.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(furniture.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(furniture.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Dimensions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${furniture.width}W × ${furniture.height}H × ${furniture.depth}D", style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Est. Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("₹${furniture.priceEstimate ?: "N/A"}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            
            if (furniture.style != null) {
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(onClick = {}, label = { Text(furniture.style!!) })
            }
        }
    }
}
