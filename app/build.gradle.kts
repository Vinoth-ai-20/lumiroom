import java.io.File
import java.util.UUID

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.lumiroom.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.lumiroom.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "com.lumiroom.app.HiltTestRunner"

        // ARCore feature declaration
        manifestPlaceholders["arCoreRequired"] = "required"
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ── Core Modules ──────────────────────────────────────────────────────────
    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))

    // ── Feature Modules ───────────────────────────────────────────────────────
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:catalog"))
    implementation(project(":feature:ar"))
    implementation(project(":feature:room-planner"))
    implementation(project(":feature:ai-assistant"))
    implementation(project(":feature:voice"))
    implementation(project(":feature:saved-rooms"))
    implementation(project(":feature:settings"))

    // ── Jetpack ───────────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose.core)

    // ── Hilt ──────────────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.android.compiler)
    ksp(libs.hilt.work.compiler)

    // ── Firebase ──────────────────────────────────────────────────────────────
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    // ── WorkManager + Hilt ────────────────────────────────────────────────────
    implementation(libs.androidx.work.runtime)
    implementation(libs.hilt.work)

    // ── Testing ───────────────────────────────────────────────────────────────
    testImplementation(libs.bundles.testing.unit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.testing.android)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.leakcanary.android)
}

// ── Automatic Furniture Catalog Generation ─────────────────────────────────
tasks.register("generateFurnitureCatalog") {
    val modelsDir = rootProject.file("models")
    val thumbnailsDir = rootProject.file("thumbnails")
    val outputAssetsDir = project.file("src/main/assets")
    val outputFile = File(outputAssetsDir, "furniture_seed.json")

    // We only declare inputs if they exist to avoid task configuration errors
    if (modelsDir.exists()) inputs.dir(modelsDir)
    if (thumbnailsDir.exists()) inputs.dir(thumbnailsDir)
    outputs.file(outputFile)

    doLast {
        if (!outputAssetsDir.exists()) outputAssetsDir.mkdirs()
        
        val modelsAssetsDir = File(outputAssetsDir, "models")
        if (!modelsAssetsDir.exists()) modelsAssetsDir.mkdirs()
        
        val thumbnailsAssetsDir = File(outputAssetsDir, "thumbnails")
        if (!thumbnailsAssetsDir.exists()) thumbnailsAssetsDir.mkdirs()

        val jsonItems = mutableListOf<String>()

        if (modelsDir.exists()) {
            val glbFiles = modelsDir.walkTopDown().filter { it.isFile && it.extension == "glb" }.toList()
            
            glbFiles.forEach { glbFile ->
                val fileName = glbFile.nameWithoutExtension
                val categoryRaw = fileName.split("_").firstOrNull() ?: "other"
                val categoryMap = mapOf(
                    "sofa" to "Sofas",
                    "chair" to "Chairs",
                    "table" to "Tables",
                    "bed" to "Beds",
                    "vanity" to "Cabinets",
                    "bathtub" to "Decor",
                    "shower" to "Decor",
                    "toilet" to "Decor",
                    "washbasin" to "Decor"
                )
                val category = categoryMap[categoryRaw] ?: categoryRaw.replaceFirstChar { it.uppercase() }

                val style = "Modern"
                val title = fileName.split("_").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
                
                // Copy to assets
                val destModel = File(modelsAssetsDir, glbFile.name)
                glbFile.copyTo(destModel, overwrite = true)
                
                // Find matching thumbnail
                var thumbPath: String? = null
                if (thumbnailsDir.exists()) {
                    val thumbFile = thumbnailsDir.walkTopDown().find { it.nameWithoutExtension == fileName && it.extension == "webp" }
                    if (thumbFile != null && thumbFile.exists()) {
                        val destThumb = File(thumbnailsAssetsDir, thumbFile.name)
                        thumbFile.copyTo(destThumb, overwrite = true)
                        thumbPath = "thumbnails/${thumbFile.name}"
                    }
                }

                val id = UUID.nameUUIDFromBytes(fileName.toByteArray()).toString()
                
                val jsonItem = """
                {
                    "id": "$id",
                    "name": "$title",
                    "category": "$category",
                    "description": "High-quality $title.",
                    "width": 1.0,
                    "depth": 1.0,
                    "height": 1.0,
                    "priceEstimate": 299.99,
                    "modelPath": "models/${glbFile.name}",
                    "thumbnailPath": ${thumbPath?.let { "\"$it\"" } ?: "null"},
                    "style": "$style"
                }
                """.trimIndent()
                
                jsonItems.add(jsonItem)
            }
        }
        
        outputFile.writeText("[\n${jsonItems.joinToString(",\n")}\n]")
    }
}

tasks.whenTaskAdded {
    if (name == "preBuild") {
        dependsOn("generateFurnitureCatalog")
    }
}
