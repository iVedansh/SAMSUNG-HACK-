# Morrow Android

Android implementation of the Teachable Voice Assistant for the Samsung Hack project.

## Current milestone

The repository now contains a native Android automation foundation:

- Kotlin Android application
- Android Accessibility Service
- Teach mode for clicks, text changes and scrolling
- Sensitive-field filtering for password, OTP, PIN and payment-related fields
- Local JSON routine persistence
- Android SpeechRecognizer voice input
- Voice-to-routine matching
- Accessibility-based routine replay using UI selectors
- Basic step-level failure reporting

## Architecture

The Windows prototype is a behavioral reference only. The Android app does not depend on Electron, PowerShell, Win32 hooks, Windows UI Automation, or screen-coordinate replay.

~~~
Voice
  |
  v
SpeechRecognizer
  |
  v
RoutineMatcher <----> RoutineStore
  |
  v
AccessibilityService
  |
  v
Android UI elements
~~~

## Run locally

1. Install Android Studio.
2. Open this repository as an existing Gradle project.
3. Let Gradle sync and install the required SDK components.
4. Select an Android emulator or physical Android phone.
5. Run the app configuration.
6. Open Morrow and enable its Accessibility Service in Android Settings.
7. Grant microphone permission when requested.
8. Tap Teach a Routine, switch to a test app, and perform a simple flow.
9. Return to Morrow and tap Stop Teaching.
10. Tap Speak a Command and say the saved phrase.

## Important limitation

This is the first automation milestone, not the final competition-grade generalized agent. The next stages are parameter/slot extraction, better selector fallback, screen-state recovery, clarification dialogue, safety hand-off for payment/login/OTP screens, and broader cross-app testing.
