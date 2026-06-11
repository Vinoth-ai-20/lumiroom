package com.lumiroom.feature.catalog.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumiroom.core.ui.components.FurnitureCard
import com.lumiroom.core.ui.components.LoadingOverlay
import com.lumiroom.core.ui.components.CreateRoomDialog
import kotlinx.coroutines.launch

private val CATEGORIES = listOf("All", "Favorites", "Sofas", "Chairs", "Tables", "Beds", "Cabinets", "Shelves", "Decor")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAr: (String?) -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val furnitureList by viewModel.furniture.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.lumiroom.core.ui.R.drawable.lumiroom_logo),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp).padding(end = 8.dp)
                        )
                        Text("Lumiroom", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToFavorites) {
                        Icon(Icons.Default.Favorite, contentDescription = "Favorites")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Home, contentDescription = "Catalog") },
                    label = { Text("Catalog") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showCreateDialog = true },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "AR") },
                    label = { Text("AR View") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToSaved,
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = "Saved") },
                    label = { Text("Saved") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToAi,
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI") },
                    label = { Text("AI Design") },
                )
            }
        },
    ) { paddingValues ->
        if (showCreateDialog) {
            CreateRoomDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, type ->
                    coroutineScope.launch {
                        val roomId = viewModel.createRoom(name, type)
                        showCreateDialog = false
                        onNavigateToAr(roomId)
                    }
                }
            )
        }
        
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading) {
                LoadingOverlay()
            } else {
                Column {
                    // Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search furniture...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )

                    // Category Filters
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(CATEGORIES) { category ->
                            FilterChip(
                                selected = (uiState.selectedCategory == category) || (category == "All" && uiState.selectedCategory == null),
                                onClick = { viewModel.onCategorySelected(category) },
                                label = { Text(category) }
                            )
                        }
                    }

                    // Catalog Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items = furnitureList, key = { it.id }) { item ->
                            FurnitureCard(
                                name = item.name,
                                brand = item.category, // Using category as brand equivalent for now
                                priceUsd = item.priceEstimate,
                                thumbnailUrl = item.thumbnailPath ?: "",
                                isFavorite = item.isFavorite,
                                isDownloaded = item.isDownloaded,
                                onClick = { onNavigateToDetail(item.id) },
                                onFavoriteToggle = { viewModel.toggleFavorite(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
