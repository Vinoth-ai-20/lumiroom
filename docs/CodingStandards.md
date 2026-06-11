# Coding Standards and Conventions

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

[⬅ Back to README](../README.md) | [Next: Risk Assessment](RiskAssessment.md)

---

## 1. Kotlin Style Guide

We strictly follow the [official Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html), enforced via Android Lint.

- 4-space indentation.
- CamelCase for variables/functionsS, PascalCase for Classes/Interfaces.

## 2. Jetpack Compose Guidelines

1. **State Hoisting**: Composables should be stateless whenever possible. Pass state down as parameters, and events up as lambdas.
2. **Modifier Chaining**: Always provide a default `Modifier` parameter to public composables.

```kotlin
// GOOD
@Composable
fun FurnitureCard(
    item: CatalogItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) { ... }
```

## 3. Coroutines and Flows

- Never use `GlobalScope`.
- Use `viewModelScope` in ViewModels and `lifecycleScope` in UI.
- Use `StateFlow` for UI state representation. Use `SharedFlow` for one-time UI events (like Toast messages).

## 4. Architecture Enforcement

- **Domain Cannot Know About UI or Data**: Use Case modules cannot import `androidx.compose` or `com.google.firebase`.
- Repositories must return Domain models, not DTOs or Entity models. Map DTOs to Models at the repository boundary.
