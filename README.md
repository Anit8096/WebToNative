# WebToNative 🌐→📱

A production-ready Android application that wraps a web experience inside a native shell with Google Sign-In (Firebase Auth), browsing history (Room DB), and smart local push notifications — all built with Jetpack Compose and a clean MVVM architecture.

---

## Table of Contents

- [Project Setup](#project-setup)
- [Firebase Setup](#firebase-setup)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [Notification Flow](#notification-flow)
- [WebView Lifecycle Handling](#webview-lifecycle-handling)
- [Challenges Faced](#challenges-faced)
- [Future Improvements](#future-improvements)

---

## Project Setup

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 36
- A Firebase project (see [Firebase Setup](#firebase-setup) below)

### Steps

1. **Clone the repository**

   ```bash
   git clone https://github.com/Anit8096/WebToNative.git
   cd WebToNative
   ```

2. **Add your `google-services.json`**

   Download it from your Firebase project console and place it at:
   ```
   app/google-services.json
   ```

3. **Add secret keys to `local.properties`**

   Open (or create) `local.properties` in the project root and add the following two entries:

   ```properties
   WEB_CLIENT_ID=your_web_client_id_here
   ```

   > ⚠️ **Important:** The `WEB_CLIENT_ID` is the **Web Client ID** (not the Android Client ID) from your Firebase project's Google Sign-In configuration. You can find it in the Firebase console under **Authentication → Sign-in method → Google → Web SDK configuration**.
   >
   > These values are injected into `BuildConfig` at compile time via `buildConfigField` in `build.gradle.kts` and are **never committed to version control**.

4. **Sync Gradle and run**

   Open the project in Android Studio, let Gradle sync complete, then run on a device or emulator (API 26+).

---

## Firebase Setup

This project uses **Firebase Authentication** with the Google Sign-In provider.

### Console Configuration

1. Go to [Firebase Console](https://console.firebase.google.com/) and create a new project (or use an existing one).
2. Register your Android app with the package name `com.kmp.webtonative`.
3. Download `google-services.json` and place it in the `app/` directory.
4. In the Firebase Console, navigate to **Authentication → Sign-in method** and enable the **Google** provider.
5. Under the Google provider settings, copy the **Web Client ID** — this is what goes into `local.properties` as `WEB_CLIENT_ID`.

### How Sign-In Works

Authentication is handled by `AuthRepository` using the modern **Credential Manager API** (`androidx.credentials`), replacing the deprecated `GoogleSignInClient`:

1. A `GetGoogleIdOption` is built with `setFilterByAuthorizedAccounts(false)` so all Google accounts on the device are shown.
2. A `GetCredentialRequest` is constructed and launched — this suspends until the user picks an account.
3. The returned `CustomCredential` is cast to a `GoogleIdTokenCredential` and its ID token is extracted.
4. The ID token is exchanged with Firebase via `GoogleAuthProvider.getCredential(idToken, null)` and `auth.signInWithCredential(...)`.
5. On success, a `FirebaseUser` is returned wrapped in `Result.success(user)`.

`AuthViewModel` drives the UI state via a `StateFlow<AuthState>` (`Idle`, `Loading`, `Success`, `Error`) and exposes `isLoggedIn` so the nav graph can skip the sign-in screen on re-launch.

---

## Architecture

The app follows **MVVM (Model-View-ViewModel)** with a **Repository pattern** and **Koin** for dependency injection.

```
UI Layer (Compose Screens)
        │
        ▼
ViewModel (StateFlow / coroutines)
        │
        ▼
Repository (abstraction over data sources)
        │
   ┌────┴────┐
   ▼         ▼
Room DB   Firebase Auth
(local)   (remote)
```

### Layer Responsibilities

| Layer | Components | Responsibility |
|---|---|---|
| **UI** | `AuthScreen`, `HomeScreen`, `WebViewScreen`, `HistoryScreen` | Render state, forward user events |
| **ViewModel** | `AuthViewModel`, `WebViewViewModel`, `HistoryViewModel` | Hold & transform UI state, call repositories |
| **Repository** | `AuthRepository`, `HistoryRepository` | Single source of truth; hides data-source details |
| **Model** | `History` entity, `HistoryDao`, `AppDatabase` | Room DB entities and DAOs |
| **DI** | `appModule` in `di/` | Koin module wiring |
| **Navigation** | `NavGraph`, `Route` | Type-safe Compose Navigation routes |

---

## Project Structure

```
com.kmp.webtonative/
├── di/
│   └── appModule.kt              # Koin DI bindings
├── model/
│   ├── repository/
│   │   ├── auth/
│   │   │   └── AuthRepository.kt # Firebase Google Sign-In logic
│   │   └── database/
│   │       └── HistoryRepository.kt
│   └── room/
│       ├── AppDatabase.kt        # Room database definition
│       ├── History.kt            # Entity
│       └── HistoryDAO.kt         # Data access object
├── navigation/
│   ├── NavGraph.kt               # Compose NavHost
│   └── Route.kt                  # Sealed route definitions
├── notification/
│   └── NotificationHelper.kt     # Channel creation, scheduling, show logic
└── ui/
    └── screens/
        ├── auth/
        │   ├── AuthScreen.kt
        │   ├── AuthState.kt
        │   └── AuthViewModel.kt
        ├── history/
        │   ├── components/
        │   │   └── HistoryItem.kt
        │   ├── HistoryScreen.kt
        │   ├── HistoryState.kt
        │   └── HistoryViewModel.kt
        ├── home/
        │   ├── carousel/
        │   │   ├── CarouselSectionUi.kt
        │   │   └── CarouselSlide.kt
        │   ├── HomeScreen.kt
        │   └── HomeState.kt
        └── webView/
            ├── components/
            │   └── ErrorView.kt
            ├── WebViewScreen.kt
            ├── WebViewState.kt
            └── WebViewViewModel.kt
```

---

## Database Schema

Room is used for local persistence of browsing history. There is a single table:

### `History` Table

| Column | Type | Description |
|---|---|---|
| `id` | `INTEGER` (PK, auto-generated) | Unique row identifier |
| `url` | `TEXT` | Full URL of the visited page |
| `title` | `TEXT` | Page title at the time of visit |
| `visitCount` | `INTEGER` (default `1`) | How many times this URL has been visited |
| `lastVisitedTime` | `INTEGER` | Unix timestamp (ms) of the most recent visit |

### How History is Recorded

`HistoryDao` exposes a `@Transaction`-annotated `recordVisit(url, title)` function that implements an **upsert** pattern:

1. Query for an existing row with the same URL (`getByUrl`).
2. If **no row exists** → `INSERT` a new `History` with `visitCount = 1`.
3. If a **row already exists** → `UPDATE` it, incrementing `visitCount` by 1 and updating `lastVisitedTime`.

`WebViewViewModel` calls `historyRepository.recordVisit(url, title)` inside `onPageFinished`, with a **3-second deduplication guard** — if the same URL finishes loading again within 3 seconds (e.g., due to a redirect loop), the second call is silently dropped to avoid double-counting.

History is observed as a `Flow<List<History>>` (ordered by `lastVisitedTime DESC`) and exposed to `HistoryScreen` via a `StateFlow` in `HistoryViewModel`.

---

## Notification Flow

The app shows a single **"Welcome Back"** local push notification once per day when the user leaves the app.

### Step-by-Step Flow

```
App launched
     │
     ▼
onCreate  →  NotificationHelper.createChannel()   (idempotent, safe to call repeatedly)
     │
     ▼
onResume  →  Check POST_NOTIFICATIONS permission (Android 13+)
             │
             └─ Not granted → request via permissionLauncher
                              └─ Granted → scheduleNotification()
     │
     ▼
onPause   →  shouldShowToday() == true?
             └─ Yes → scheduleNotification()
                       handler.postDelayed(showNotificationRunnable, 8_000 ms)
     │
     ▼
After 8 seconds (app still in background)
     │
     ▼
NotificationHelper.show()
     ├── shouldShowToday()? No → return (already shown today)
     ├── isForeground?     Yes → return (user came back)
     └── No to both → build & post notification via NotificationManagerCompat
                       markShownToday() → saves LocalDate.now() to SharedPreferences
```

### Key Design Decisions

- **Once-per-day guard** — `shouldShowToday()` compares today's `LocalDate` string against the value stored in `SharedPreferences`. The notification is suppressed if they match.
- **Foreground guard** — `ActivityManager.runningAppProcesses` is checked inside `show()` to prevent showing the notification if the user has already returned to the app before the 8-second delay fires.
- **Handler cleanup** — `handler.removeCallbacks(showNotificationRunnable)` is called in `onResume` and `onDestroy` to cancel any pending delivery when the user returns to the app or the process is killed.
- **Permission handling** — On Android 13+ (API 33, `TIRAMISU`), `POST_NOTIFICATIONS` is requested at runtime via `ActivityResultContracts.RequestPermission`. The request is skipped if `shouldShowRequestPermissionRationale` returns `true` (meaning the user has permanently denied it).

---

## WebView Lifecycle Handling

`WebViewScreen` is a Composable that hosts an Android `WebView` via `AndroidView`. Lifecycle integration is managed across two layers:

### WebViewClient — Navigation & Error Events

| Callback | Action |
|---|---|
| `onPageStarted(url)` | Sets state to `Loading`, updates `currentUrl` in ViewModel |
| `onPageFinished(url, title)` | Sets state to `Success`, records visit in Room (with dedup guard) |
| `onReceivedError(request, error)` | If main-frame error → translates error code to user-friendly message, sets state to `Error` |

Only **main-frame** errors (checked via `request?.isForMainFrame`) trigger the error UI. Sub-resource failures (images, scripts) are silently ignored.

### WebChromeClient — Progress & URL Display

| Callback | Action |
|---|---|
| `onProgressChanged(newProgress)` | Updates `progress: StateFlow<Int>` (0–100) for the `LinearProgressIndicator` |
| `onReceivedTitle(view, title)` | Reads `view.url` and pushes it to `currentUrl` so the top-bar pill reflects SPA navigation |

### State Model (`WebViewState`)

```kotlin
sealed class WebViewState {
    object Loading : WebViewState()
    object Success : WebViewState()
    data class Error(val message: String) : WebViewState()
}
```

The `LinearProgressIndicator` is wrapped in `AnimatedVisibility` and shown only when state is `Loading`. The `ErrorView` composable appears in place of the WebView when state is `Error`, offering a **Retry** button that calls `webViewRef?.reload()`.

### WebView Instance Retention

```kotlin
var webViewRef by remember { mutableStateOf<WebView?>(null) }
```

The `WebView` instance is held in a `remember`ed state variable so that back-navigation (`webView.canGoBack() / goBack()`) and retry (`webView.reload()`) work across Compose recompositions without recreating the view.

### Back Navigation

`BackHandler` intercepts the system back gesture. If `webView.canGoBack()` is true, it navigates back within the WebView's history stack; otherwise it calls `onBack()` to pop the Compose back stack.

---

## Challenges Faced

- **Credential Manager migration** — The legacy `GoogleSignInClient` API is deprecated. Migrating to `androidx.credentials.CredentialManager` required understanding the new builder pattern for `GetGoogleIdOption` and correctly distinguishing between the Android Client ID (used in `google-services.json`) and the **Web Client ID** (required by `CredentialManager` for ID token issuance). Keeping the Web Client ID out of source control (via `local.properties` → `BuildConfig`) added an extra configuration step.

- **WebView + Compose lifecycle mismatch** — Compose recomposes freely, but `WebView` is a stateful Android View that must not be recreated on every recomposition. Using `remember { mutableStateOf<WebView?>(null) }` as a stable reference solved this but required careful null-checking at every usage site.

- **History deduplication** — `onPageFinished` fires multiple times for the same URL during redirects and SPA route changes. Without the 3-second timestamp guard in `WebViewViewModel`, a single navigation could insert duplicate or inflated visit counts into Room.

- **Notification timing edge case** — The notification must fire only when the app is genuinely in the background. A plain `postDelayed` on `onPause` could fire while the user is still in the app (e.g., switching activities). The foreground process check inside `NotificationHelper.show()` was added as a secondary defence.

- **SPA URL tracking** — For single-page applications the `onPageStarted` / `onPageFinished` pair does not fire on in-app navigation. `WebChromeClient.onReceivedTitle` was used as a complementary signal to keep the URL pill in the top bar accurate.

---

## Future Improvements

- **Search bar in WebView top bar** — Allow users to type a URL or search query directly instead of being locked to the initial URL.
- **Multiple tabs** — Maintain a list of active WebView sessions with tab switching UI.
- **History backup** — Allowing user to sync there history with firebase
- **Offline caching** — Use `WebView.setNetworkAvailable(false)` in combination with a `ServiceWorker`-enabled site to serve cached content when offline.
- **History search & filtering** — Add a search field to `HistoryScreen` filtering against URL and title using a `LIKE` query in Room, Also allowing for single instances of history to be removed.
- **Biometric lock** — Gate the app (or history screen) behind `BiometricPrompt` for privacy.
- **FCM push notifications** — Replace local notifications with Firebase Cloud Messaging to enable server-triggered alerts (e.g., new content available).
- **Dark/light theme persistence** — Persist the user's theme preference using `DataStore<Preferences>` and apply it to the WebView via `forceDark` settings.
- **Export history** — Allow users to export their browsing history as a CSV or share it via the Android share sheet.
