pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Lumiroom"

// ── App ──────────────────────────────────────────────────────────────────────
include(":app")

// ── Core Modules ─────────────────────────────────────────────────────────────
include(":core:ui")
include(":core:common")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:domain")
include(":core:room-analysis")
include(":core:recommendation")
include(":core:testing")

// ── Feature Modules ───────────────────────────────────────────────────────────
include(":feature:onboarding")
include(":feature:auth")
include(":feature:catalog")
include(":feature:ar")
include(":feature:room-planner")
include(":feature:ai-assistant")
include(":feature:voice")
include(":feature:saved-rooms")
include(":feature:settings")
