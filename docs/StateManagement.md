# State Management Guidelines

> [!NOTE]
> **Asset Integration & Pricing Update (v10):**
> Lumiroom has been updated to use a dynamic Model Discovery Engine. Hardcoded `furniture_seed.json` lists have been eliminated. Assets are automatically indexed from the `/assets/models` directory. All prices have been dynamically recalculated to reflect the realistic Indian Market pricing (₹).


**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  

[⬅ Back to Architecture](Architecture.md)

Lumiroom relies heavily on Kotlin Coroutines and `StateFlow` for state management. This document outlines the standard conventions used across the repository.

## 1. UI State Modeling

Each Screen should have a single, immutable `UiState` data class.

```kotlin
data class CatalogUiState(
    val isLoading: Boolean = true,
    val items: List<Furniture> = emptyList(),
    val errorMessage: String? = null
)
```

## 2. ViewModel Exposing State

State should be mutated privately within the ViewModel using a `MutableStateFlow` and exposed publicly as an immutable `StateFlow`.

```kotlin
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val repository: FurnitureRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState = _uiState.asStateFlow()
    
    // ...
}
```

## 3. Handling State Updates

To avoid race conditions, use the `.update {}` function provided by Kotlin Coroutines rather than directly assigning `.value =`.

```kotlin
// Correct
_uiState.update { it.copy(isLoading = false, items = newItems) }

// Incorrect (Race condition prone)
_uiState.value = _uiState.value.copy(isLoading = false, items = newItems)
```

## 4. Collecting State in Compose

Always use `collectAsStateWithLifecycle()` to ensure that the flow collection pauses when the app goes into the background.

```kotlin
@Composable
fun CatalogScreen(viewModel: CatalogViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // ...
}
```
