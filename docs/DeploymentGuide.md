# Lumiroom Deployment & Installation Guide

This document outlines the complete workflow for generating, signing, and deploying the Lumiroom Android application to physical devices for ARCore testing.

---

## 1. Build Generation Commands

Lumiroom uses the Gradle build system wrapper. Run these commands from the root directory of the project (`e:\projects\Lumiroom`).

### Debug APK (For immediate testing)
Generates an APK with debugging enabled. No manual signing config is needed (uses the default debug keystore).
```bash
# Windows
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```
**Output Location:** `app/build/outputs/apk/debug/app-debug.apk`

### Release APK (For manual distribution)
Generates an optimized, minified APK. ProGuard/R8 rules will be applied.
```bash
.\gradlew.bat assembleRelease
```
**Output Location:** `app/build/outputs/apk/release/app-release-unsigned.apk`

### Android App Bundle / AAB (For Google Play Store)
Generates an optimized `.aab` file required by the Google Play Console.
```bash
.\gradlew.bat bundleRelease
```
**Output Location:** `app/build/outputs/bundle/release/app-release.aab`

---

## 2. Signing Configuration

To install a Release build, it must be signed. For production, do not use the debug keystore.

1. **Generate a Keystore (If you don't have one):**
   ```bash
   keytool -genkey -v -keystore lumiroom-release.keystore -alias lumiroom_alias -keyalg RSA -keysize 2048 -validity 10000
   ```
2. **Configure `build.gradle.kts` (Optional for CI/CD):**
   Add the signing config inside `android { ... }` in `app/build.gradle.kts`:
   ```kotlin
   signingConfigs {
       create("release") {
           storeFile = file("lumiroom-release.keystore")
           storePassword = System.getenv("KEYSTORE_PASSWORD")
           keyAlias = System.getenv("KEY_ALIAS")
           keyPassword = System.getenv("KEY_PASSWORD")
       }
   }
   buildTypes {
       release {
           signingConfig = signingConfigs.getByName("release")
           // ...
       }
   }
   ```
3. **Manual Signing (apksigner):**
   If building `assembleRelease` without configuring gradle signing:
   ```bash
   apksigner sign --ks lumiroom-release.keystore --out app-release-signed.apk app/build/outputs/apk/release/app-release-unsigned.apk
   ```

---

## 3. Device Installation Workflow

Because Lumiroom heavily relies on **ARCore**, it *must* be tested on a physical device. Android Studio Emulators lack the necessary IMU sensors and camera calibration.

### Option A: Direct APK Installation
1. Build the Debug or Signed-Release APK.
2. Transfer the `.apk` file to your device (via Google Drive, Email, or USB mass storage).
3. On your phone, go to **Settings > Security > Install Unknown Apps** and allow your file manager.
4. Tap the APK to install.

### Option B: USB Debugging & ADB Setup
1. **Enable Developer Options:** On your phone, go to **Settings > About Phone** and tap **Build Number** 7 times.
2. **Enable USB Debugging:** Go to **Settings > System > Developer Options** and toggle **USB Debugging** ON.
3. Plug your phone into your computer via a data-capable USB-C cable.
4. Allow the prompt *"Allow USB Debugging?"* on your phone screen.
5. **Install via ADB:**
   ```bash
   adb devices # Verify your device is listed
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Option C: Wireless Debugging (Android 11+)
1. Ensure your phone and PC are on the **same Wi-Fi network**.
2. Go to **Settings > System > Developer Options > Wireless Debugging**.
3. Toggle it ON and tap **"Pair device with pairing code"**.
4. On your PC, run:
   ```bash
   adb pair <IP_ADDRESS>:<PORT>
   # Enter the pairing code shown on your phone
   adb connect <IP_ADDRESS>:<PORT>
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 4. Device Compatibility Checklist

Before deployment, ensure the target physical device meets the following criteria:

- [ ] **OS Version:** Android 8.0 (Oreo / API 26) or higher.
- [ ] **Hardware:** Supports OpenGL ES 3.0 or higher.
- [ ] **Camera:** Working rear camera with standard calibration.
- [ ] **Sensors:** Working Gyroscope and Accelerometer.
- [ ] **ARCore Support:** The device must be on the [Google Play Services for AR Supported Devices List](https://developers.google.com/ar/devices).
- [ ] **Google Play Services:** Must be installed and up-to-date (required for Firebase Auth and ARCore).

---

## 5. Troubleshooting Guide

| Issue | Cause / Solution |
|-------|------------------|
| **"App not installed" Error** | You are trying to install a Release APK over a Debug APK (or vice versa). **Solution:** Uninstall the existing app first: `adb uninstall com.lumiroom.app`. |
| **AR Camera is Black / Fails to Start** | ARCore requires Camera permissions. **Solution:** Go to App Info > Permissions and grant Camera access. Ensure "Google Play Services for AR" is updated in the Play Store. |
| **ADB Command Not Found** | The Android SDK Platform-Tools are not in your system PATH. **Solution:** Add `C:\Users\YourUser\AppData\Local\Android\Sdk\platform-tools` to your Windows Environment Variables. |
| **adb devices shows "unauthorized"** | The PC's RSA key was not accepted by the phone. **Solution:** Unplug, run `adb kill-server`, plug back in, and watch your phone screen to accept the prompt. |
| **Crash on Launch (Release Build Only)** | ProGuard/R8 obfuscated a class required by reflection (e.g., Firebase or SceneView). **Solution:** Check `logcat`. Ensure `proguard-rules.pro` contains the `-keep` directives for the crashing library. |
| **Voice Commands Not Recognizing** | Android `SpeechRecognizer` requires a network connection or offline language packs, plus the Microphone permission. **Solution:** Grant Mic permission and ensure you are connected to the internet. |
