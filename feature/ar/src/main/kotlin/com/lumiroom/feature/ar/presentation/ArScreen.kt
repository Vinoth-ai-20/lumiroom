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
    val transformNodesMap = remember { mutableMapOf<String, Node>() }

    // Used for dragging delta calculations in ROTATE mode
    var lastTouchX by remember { mutableStateOf(0f) }
    var lastTouchY by remember { mutableStateOf(0f) }
    
    // Floating Overlays for Scale and Rotation
    var activeScaleText by remember { mutableStateOf<String?>(null) }
    var activeRotationText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            android.widget.Toast.makeText(context, "Room Saved Successfully", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.onSaveSuccessHandled()
        }
    }

    // Initialize AR Engine and Loaders to ModelNodes
    LaunchedEffect(uiState.placedItems, uiState.selectedItemIds, uiState.lockedItemIds, uiState.hiddenItemIds) {
        // Cleanup removed items
        val currentIds = uiState.placedItems.map { it.placedItem.id }.toSet()
        val nodeIds = childNodes.mapNotNull { it.name }.toSet()
        val idsToRemove = nodeIds - currentIds
        idsToRemove.forEach { idToRemove ->
            val nodeToRemove = childNodes.find { it.name == idToRemove }
            if (nodeToRemove != null) {
                childNodes.remove(nodeToRemove)
                transformNodesMap.remove(idToRemove)
            }
        }
        
        val toAdd = currentIds - nodeIds
        toAdd.forEach { id ->
            val placedItem = uiState.placedItems.find { it.placedItem.id == id } ?: return@forEach
            coroutineScope.launch {

                val modelInstance = try {
                    modelLoader.loadModelInstance("models/${placedItem.furniture.modelPath.substringAfterLast("/")}")
                } catch (e: Exception) {
                    null
                }
                if (modelInstance != null) {

                    
                    val anchor = anchorCache[id]
                    val baseNode: Node = if (anchor != null) {

                        AnchorNode(engine, anchor).apply {
                            name = id
                        }
                    } else {

                        Node(engine).apply {
                            name = id
                            position = Position(placedItem.placedItem.posX, placedItem.placedItem.posY, placedItem.placedItem.posZ)
                        }
                    }
                    
                    baseNode.isPositionEditable = false
                    baseNode.isRotationEditable = false
                    baseNode.isScaleEditable = false

                    val transformNode = io.github.sceneview.node.Node(engine = engine).apply {
                        name = "transform_$id"
                        this.scale = Scale(placedItem.placedItem.scaleX, placedItem.placedItem.scaleY, placedItem.placedItem.scaleZ)
                        this.quaternion = dev.romainguy.kotlin.math.Quaternion(
                            placedItem.placedItem.rotX, placedItem.placedItem.rotY,
                            placedItem.placedItem.rotZ, placedItem.placedItem.rotW
                        )
                        transformNodesMap[id] = this
                        
                        isPositionEditable = false
                        isRotationEditable = false
                        isScaleEditable = false
                    }

                    val modelNode = object : ModelNode(modelInstance = modelInstance) {
                    }.apply {
                        name = id
                        val boundingBox = modelInstance.asset?.boundingBox
                        if (boundingBox != null) {
                            val center = boundingBox.center
                            val halfExtent = boundingBox.halfExtent
                            // Center in X and Z, align bottom to Y=0
                            position = Position(-center[0], halfExtent[1] - center[1], -center[2])
                        } else {
                            centerOrigin(Position(0f, -1f, 0f)) // -1f = bottom alignment
                        }
                        
                        isPositionEditable = false
                        isRotationEditable = false
                        isScaleEditable = false
                    }
                    
                    transformNode.addChildNode(modelNode)
                    baseNode.addChildNode(transformNode)
                    baseNode.name = id // So it can be found for removal
                    childNodes.add(baseNode)

                } else {
                    Log.e("ArPlacement", "Failed to load model instance!")
                }
            }
        }

        // Update properties
        childNodes.forEach { baseNode ->
            val id = baseNode.name ?: return@forEach
            val transformNode = baseNode.childNodes.firstOrNull { it.name == "transform_$id" }
            val modelNode = transformNode?.childNodes?.filterIsInstance<ModelNode>()?.firstOrNull() ?: baseNode.childNodes.filterIsInstance<ModelNode>().firstOrNull()
            val isSelected = uiState.selectedItemIds.contains(id)
            
            if (transformNode != null) {
                // Add or remove selection box
                val selectionBoxName = "selection_box_$id"
                val existingBox = transformNode.childNodes.find { it.name == selectionBoxName }
                
                if (isSelected && existingBox == null) {
                    val bb = modelNode?.modelInstance?.asset?.boundingBox
                    val boxSize = if (bb != null) {
                        io.github.sceneview.math.Position(bb.halfExtent[0] * 2.2f, 0.02f, bb.halfExtent[2] * 2.2f)
                    } else {
                        io.github.sceneview.math.Position(1.2f, 0.02f, 1.2f)
                    }
                    val boxNode = io.github.sceneview.node.CubeNode(
                        engine = engine,
                        size = boxSize,
                        center = io.github.sceneview.math.Position(0f, 0.01f, 0f)
                    ).apply {
                        name = selectionBoxName
                    }
                    transformNode.addChildNode(boxNode)
                } else if (!isSelected && existingBox != null) {
                    transformNode.removeChildNode(existingBox)
                    existingBox.destroy()
                }

                // Sync and animate external transform changes (e.g., Undo, Redo, Reset)
                val uiItem = uiState.placedItems.find { it.placedItem.id == id }?.placedItem

                
                if (uiItem != null && uiState.interactionMode != InteractionMode.MOVE && uiState.interactionMode != InteractionMode.ROTATE && uiState.interactionMode != InteractionMode.SCALE) {
                    val targetPos = io.github.sceneview.math.Position(uiItem.posX, uiItem.posY, uiItem.posZ)
                    val targetRot = dev.romainguy.kotlin.math.Quaternion(uiItem.rotX, uiItem.rotY, uiItem.rotZ, uiItem.rotW)
                    val targetScale = io.github.sceneview.math.Scale(uiItem.scaleX, uiItem.scaleY, uiItem.scaleZ)

                    val dist = dev.romainguy.kotlin.math.length(baseNode.position - targetPos)
                    val scaleDist = dev.romainguy.kotlin.math.length(transformNode.scale - targetScale)
                    val rotDist = kotlin.math.abs(dev.romainguy.kotlin.math.dot(transformNode.quaternion, targetRot))
                    
                    if (dist > 0.005f || scaleDist > 0.005f || rotDist < 0.999f) {
                        coroutineScope.launch {
                            val startPos = baseNode.position
                            val startRot = transformNode.quaternion
                            val startScale = transformNode.scale
                            val steps = 25 // 400ms at ~60fps
                            for (i in 0..steps) {
                                val t = i.toFloat() / steps
                                val smoothT = t * t * (3f - 2f * t) // ease in out
                                baseNode.position = dev.romainguy.kotlin.math.mix(startPos, targetPos, smoothT)
                                transformNode.quaternion = dev.romainguy.kotlin.math.slerp(startRot, targetRot, smoothT)
                                transformNode.scale = dev.romainguy.kotlin.math.mix(startScale, targetScale, smoothT)
                                kotlinx.coroutines.delay(16)
                            }
                            baseNode.position = targetPos
                            transformNode.quaternion = targetRot
                            transformNode.scale = targetScale
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

    // Global state for continuous gesture tracking
    val touchState = remember { object { var x = 0f; var y = 0f } }

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

            },
            onSessionFailed = { exception ->
                viewModel.onSessionError(exception.message ?: "Unknown AR Session Error")
            },
            onGestureListener = object : io.github.sceneview.gesture.GestureDetector.SimpleOnGestureListener() {
                
                override fun onMoveBegin(detector: io.github.sceneview.gesture.MoveGestureDetector, e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {
                    if (uiState.selectedItemIds.isEmpty()) return
                    
                    val selectedId = uiState.selectedItemIds.first()
                    if (uiState.lockedItemIds.contains(selectedId)) return
                    
                    touchState.x = e.x
                    touchState.y = e.y
                    
                    if (uiState.interactionMode == InteractionMode.MOVE) {
                        return
                    }
                    if (uiState.interactionMode == InteractionMode.ROTATE) {
                        return
                    }
                    
                    return
                }

                override fun onMove(detector: io.github.sceneview.gesture.MoveGestureDetector, e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {
                    if (uiState.selectedItemIds.isEmpty()) return
                    val selectedId = uiState.selectedItemIds.first()
                    if (uiState.lockedItemIds.contains(selectedId)) return
                    
                    if (uiState.interactionMode == InteractionMode.ROTATE) {
                        val dx = e.x - touchState.x
                        touchState.x = e.x
                        touchState.y = e.y

                        val transformNode = transformNodesMap[selectedId] ?: return
                        val rotY = dev.romainguy.kotlin.math.Quaternion.fromAxisAngle(dev.romainguy.kotlin.math.Float3(0f, 1f, 0f), dx * 0.5f)
                        transformNode.quaternion = transformNode.quaternion * rotY
                        
                        val euler = dev.romainguy.kotlin.math.eulerAngles(transformNode.quaternion)
                        val degrees = Math.toDegrees(euler.y.toDouble()).toInt()
                        val normalizedDegrees = ((degrees % 360) + 360) % 360
                        activeRotationText = "$normalizedDegrees°"
                        return
                    }
                    
                    if (uiState.interactionMode == InteractionMode.MOVE) {
                        val baseNode = childNodes.find { it.name == selectedId } ?: return
                        val hitResult = currentFrame?.hitTest(e.x, e.y)?.firstOrNull { hit ->
                            val trackable = hit.trackable
                            trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)
                        }
                        val anchorNode = baseNode as? AnchorNode
                        if (hitResult != null) {
                            val identityPose = com.google.ar.core.Pose(
                                hitResult.hitPose.translation,
                                floatArrayOf(0f, 0f, 0f, 1f)
                            )
                            val newAnchor = hitResult.trackable.createAnchor(identityPose)
                            if (anchorNode != null) {
                                anchorNode.anchor?.detach()
                                anchorNode.anchor = newAnchor
                            } else {
                                val pose = hitResult.hitPose
                                baseNode.position = Position(pose.tx(), pose.ty(), pose.tz())
                            }
                        }
                        return
                    }
                    
                    return
                }

                override fun onMoveEnd(detector: io.github.sceneview.gesture.MoveGestureDetector, e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {
                    super.onMoveEnd(detector, e, node)
                    if (uiState.selectedItemIds.isEmpty()) return
                    val selectedId = uiState.selectedItemIds.first()
                    
                    if (uiState.interactionMode == InteractionMode.MOVE) {
                        val baseNode = childNodes.find { it.name == selectedId } ?: return
                        val transformNode = transformNodesMap[selectedId] ?: return
                        
                        val hitResult = currentFrame?.hitTest(e.x, e.y)?.firstOrNull { hit ->
                            val trackable = hit.trackable
                            trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)
                        }
                        val anchorNode = baseNode as? AnchorNode
                        if (hitResult != null) {
                            val identityPose = com.google.ar.core.Pose(
                                hitResult.hitPose.translation,
                                floatArrayOf(0f, 0f, 0f, 1f)
                            )
                            val newAnchor = hitResult.trackable.createAnchor(identityPose)
                            if (anchorNode != null) {
                                anchorNode.anchor?.detach()
                                anchorNode.anchor = newAnchor
                            }
                            
                            viewModel.onItemTransformed(
                                itemId = selectedId,
                                posX = baseNode.worldPosition.x,
                                posY = baseNode.worldPosition.y,
                                posZ = baseNode.worldPosition.z,
                                rotX = transformNode.quaternion.x, rotY = transformNode.quaternion.y, rotZ = transformNode.quaternion.z, rotW = transformNode.quaternion.w,
                                scaleX = transformNode.scale.x, scaleY = transformNode.scale.y, scaleZ = transformNode.scale.z,
                                matrixJson = ""
                            )
                        }
                    }
                    
                    if (uiState.interactionMode == InteractionMode.ROTATE) {
                        activeRotationText = null
                        val baseNode = childNodes.find { it.name == selectedId } ?: return
                        val transformNode = transformNodesMap[selectedId] ?: return
                        viewModel.onItemTransformed(
                            itemId = selectedId,
                            posX = baseNode.worldPosition.x,
                            posY = baseNode.worldPosition.y,
                            posZ = baseNode.worldPosition.z,
                            rotX = transformNode.quaternion.x, rotY = transformNode.quaternion.y, rotZ = transformNode.quaternion.z, rotW = transformNode.quaternion.w,
                            scaleX = transformNode.scale.x, scaleY = transformNode.scale.y, scaleZ = transformNode.scale.z,
                            matrixJson = ""
                        )
                    }
                }

                override fun onScale(detector: io.github.sceneview.gesture.ScaleGestureDetector, e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {
                    if (uiState.interactionMode != InteractionMode.SCALE || uiState.selectedItemIds.isEmpty()) return
                    val selectedId = uiState.selectedItemIds.first()
                    if (uiState.lockedItemIds.contains(selectedId)) return
                    
                    val transformNode = transformNodesMap[selectedId] ?: return
                    val placedItem = uiState.placedItems.find { it.placedItem.id == selectedId } ?: return
                    
                    super.onScale(detector, e, node)
                    val factor = detector.scaleFactor
                    val dampenedFactor = 1.0f + (factor - 1.0f) * 0.15f
                    val newScaleX = (transformNode.scale.x * dampenedFactor).coerceIn(0.1f, 5.0f)
                    val newScaleY = (transformNode.scale.y * dampenedFactor).coerceIn(0.1f, 5.0f)
                    val newScaleZ = (transformNode.scale.z * dampenedFactor).coerceIn(0.1f, 5.0f)
                    transformNode.scale = Scale(newScaleX, newScaleY, newScaleZ)
                    
                    val percentage = (newScaleX / placedItem.placedItem.initScaleX * 100).toInt()
                    activeScaleText = "$percentage%"
                }

                override fun onScaleEnd(detector: io.github.sceneview.gesture.ScaleGestureDetector, e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {
                    if (uiState.interactionMode != InteractionMode.SCALE || uiState.selectedItemIds.isEmpty()) return
                    val selectedId = uiState.selectedItemIds.first()
                    
                    val transformNode = transformNodesMap[selectedId] ?: return
                    val baseNode = childNodes.find { it.name == selectedId } ?: return
                    
                    super.onScaleEnd(detector, e, node)
                    activeScaleText = null
                    viewModel.onItemTransformed(
                        itemId = selectedId,
                        posX = baseNode.worldPosition.x,
                        posY = baseNode.worldPosition.y,
                        posZ = baseNode.worldPosition.z,
                        rotX = transformNode.quaternion.x, rotY = transformNode.quaternion.y, rotZ = transformNode.quaternion.z, rotW = transformNode.quaternion.w,
                        scaleX = transformNode.scale.x, scaleY = transformNode.scale.y, scaleZ = transformNode.scale.z,
                        matrixJson = ""
                    )
                }

                override fun onSingleTapConfirmed(e: android.view.MotionEvent, node: io.github.sceneview.node.Node?) {
                    if (node != null && node.name != null) {
                        viewModel.onItemSelected(node.name!!, multiSelect = false) // Add long press later for multi-select
                    } else {
                        viewModel.onItemSelected(null)
                        if (pendingFurnitureId != null) {
                            val hitResult = currentFrame?.hitTest(e.x, e.y)?.firstOrNull { hit ->
                                val trackable = hit.trackable
                                trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)
                            }
                            if (hitResult != null) {
                                val pose = hitResult.hitPose

                                
                                val instanceId = java.util.UUID.randomUUID().toString()
                                val identityPose = com.google.ar.core.Pose(
                                    pose.translation,
                                    floatArrayOf(0f, 0f, 0f, 1f)
                                )
                                val anchor = hitResult.trackable.createAnchor(identityPose)
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
                onSetInteractionMode = { viewModel.setInteractionMode(it) },
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
            Box(modifier = Modifier.align(Alignment.Center).padding(bottom = 60.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(24.dp)).padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text(text = activeScaleText!!, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            }
        }
        if (activeRotationText != null) {
            Box(modifier = Modifier.align(Alignment.Center).padding(bottom = 60.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(24.dp)).padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text(text = activeRotationText!!, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
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
                    onNavigateToSettings = { showCatalogOverlay = false },
                    onNavigateToFavorites = { showCatalogOverlay = false; onNavigateToCatalog() }
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
    onSetInteractionMode: (InteractionMode) -> Unit,
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
