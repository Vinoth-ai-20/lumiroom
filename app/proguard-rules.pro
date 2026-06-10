# ── AndroidX & Compose ──────────────────────────────────────────────────────────
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# ── Coroutines ────────────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Firebase & Play Services ──────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ── ARCore & SceneView ────────────────────────────────────────────────────────
-keep class com.google.ar.** { *; }
-dontwarn com.google.ar.**
-keep class io.github.sceneview.** { *; }
-dontwarn io.github.sceneview.**

# ── Hilt / Dagger ─────────────────────────────────────────────────────────────
-keep,allowobfuscation,allowshrinking class dagger.**
-keep class dagger.hilt.** { *; }
-dontwarn dagger.**

# ── Room Database ─────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase$Builder

# ── Models (Entities & Data Classes) ──────────────────────────────────────────
-keep class com.lumiroom.core.database.entity.** { *; }
-keep class com.lumiroom.core.network.dto.** { *; }

# ── General Rules ─────────────────────────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
