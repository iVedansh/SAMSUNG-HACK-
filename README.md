# Morrow Android

Android implementation of the Teachable Voice Assistant for the Samsung Hack project.

## What is implemented

- Native Kotlin Android application
- Android Accessibility Service declaration and event capture
- Teach-a-routine flow
- Local routine/action data model
- Local routine persistence
- Sensitive-field protection for password, OTP, PIN, CVV and card-related fields
- Android SpeechRecognizer voice-input foundation
- Accessibility status UI

## Architecture

The Windows prototype used Electron + PowerShell + Win32 hooks. This repository intentionally does not port those Windows APIs. Android automation is built around Accessibility Service so the resulting artifact can be packaged as an APK.

## Run locally

1. Install Android Studio.
2. Open this repository as an existing Gradle project.
3. Allow Android Studio to install/sync the Android Gradle Plugin and required SDK components.
4. Select an Android emulator or physical Android phone.
5. Run the app configuration.
6. On first launch, tap **Enable Accessibility Service** and enable **Morrow** in Android Accessibility settings.
7. Grant microphone permission when using **Speak a Command**.

## First end-to-end prototype

1. Enable the Accessibility Service.
2. Tap **Teach a Routine**.
3. Enter a routine name and voice phrase.
4. Switch to the target Android app and perform the routine.
5. Return to Morrow and tap **Stop Teaching**.
6. The captured accessibility actions are stored locally.

The current voice button only performs speech recognition and displays the recognized command. Voice-to-routine matching and playback are the next implementation milestone.

## Safety boundary

Morrow does not intentionally record values from password fields and skips fields whose accessibility metadata strongly indicates password, OTP, PIN, CVV, card or verification-code usage. Payment/login hand-off logic will be added before real-world automation is enabled.

## Project status

This is the Android rebuild of the earlier Windows prototype. The Windows implementation remains useful as a reference for the original recording/replay concept, but Windows hooks, Windows UI Automation and PowerShell SendInput are not part of this Android implementation.