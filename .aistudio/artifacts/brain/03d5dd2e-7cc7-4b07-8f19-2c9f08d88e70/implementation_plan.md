# Firebase Cloud Messaging (FCM) Push Notifications Plan (Revised)

Implement end-to-end Firebase Cloud Messaging (FCM) push notifications in Mulberry, supporting rich image attachments (`BigPictureStyle`), automatic topic subscriptions (`"announcements"`, `"all_users"`), Android 13+ runtime permission requests, and deep-link routing for URLs, book details, and tab destinations.

## User Requirements & Architecture

> [!IMPORTANT]
> The implementation strictly adheres to the user-specified requirements:
> 1. **Version Catalog & Gradle**: Manage `firebase-bom` and `firebase-messaging` via `gradle/libs.versions.toml` and implement in `app/build.gradle.kts`.
> 2. **Manifest**: Register `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` and declare `MulberryFirebaseMessagingService` with `com.google.firebase.MESSAGING_EVENT`.
> 3. **Messaging Service (`MulberryFirebaseMessagingService.kt`)**:
>    - `onNewToken`: Auto-subscribe to `"announcements"` and `"all_users"`.
>    - `onMessageReceived`: Handle both notification and data payloads.
>    - Channel: Create high-importance channel `"announcements_channel"` named `"Mulberry Announcements"`.
>    - Image Attachments: Resilient helper downloading bitmap with a 4-second network timeout, rendering with `NotificationCompat.BigPictureStyle` (falling back cleanly to text if absent/failed).
>    - PendingIntent: Targets `MainActivity` with `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`, forwarding all key-value entries from `remoteMessage.data`.
> 4. **Android 13+ Runtime Permissions**:
>    - Jetpack Compose permission launcher using `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` on API 33+ (TIRAMISU).
>    - Verify topic subscription on initial app launch.
> 5. **Deep Linking & Intent Routing (`MainActivity.kt`)**:
>    - Support cold start (`onCreate`) and warm start (`onNewIntent` + `setIntent(intent)`).
>    - Priority routing order:
>      1. If `url` is present: launch external browser intent (`ACTION_VIEW`).
>      2. If `book_slug` (or `book_id`) is present: navigate to book details / reader (`book_details/{slug}`).
>      3. If `destination` is present: navigate to target tab/route (e.g. `"desk"`, `"snips"`).

---

## 1. Technical Architecture & Routing Flow

```
┌────────────────────────────────────────────────────────┐
│                   Firebase Console                     │
│  Broadcast campaign sent to topic "announcements":     │
│  • Title & Body                                        │
│  • Image URL (Optional)                                │
│  • Custom data: {"book_slug": "..."} OR {"url": "..."} │
└──────────────────────────┬─────────────────────────────┘
                           │ FCM Transmission
                           ▼
┌────────────────────────────────────────────────────────┐
│           MulberryFirebaseMessagingService             │
├────────────────────────────────────────────────────────┤
│  1. Check Channel: "announcements_channel" (High)      │
│  2. Download Image Bitmap (4s timeout)                │
│  3. Build NotificationCompat (BigPictureStyle)         │
│  4. Attach PendingIntent with data extras              │
└──────────────────────────┬─────────────────────────────┘
                           │ User taps notification
                           ▼
┌────────────────────────────────────────────────────────┐
│               MainActivity (Deep Link Handler)         │
├────────────────────────────────────────────────────────┤
│  Priority 1: url ───────────────► External Browser     │
│  Priority 2: book_slug / book_id ► Open Book Sheet/Read│
│  Priority 3: destination ────────► Switch Tab (Desk...)│
└────────────────────────────────────────────────────────┘
```

---

## 2. Implementation Steps

1. **Version Catalog & Gradle Dependencies**:
   - In `gradle/libs.versions.toml`:
     - Add `firebase-messaging = { group = "com.google.firebase", name = "firebase-messaging" }`.
   - In `app/build.gradle.kts`:
     - Add `implementation(platform(libs.firebase.bom))`.
     - Add `implementation(libs.firebase.messaging)`.

2. **Android Manifest Configuration (`AndroidManifest.xml`)**:
   - Add `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`.
   - Register service:
     ```xml
     <service
         android:name="com.example.service.MulberryFirebaseMessagingService"
         android:exported="false">
         <intent-filter>
             <action android:name="com.google.firebase.MESSAGING_EVENT" />
         </intent-filter>
     </service>
     ```

3. **FCM Service (`com.example.service.MulberryFirebaseMessagingService.kt`)**:
   - Implement `onNewToken(token: String)`:
     - Subscribe to `"announcements"` and `"all_users"`.
   - Implement `onMessageReceived(remoteMessage: RemoteMessage)`:
     - Create `announcements_channel` with `NotificationManager.IMPORTANCE_HIGH`.
     - Download image bitmap with OkHttpClient (4-second connect/read timeout) from `remoteMessage.notification?.imageUrl` or `remoteMessage.data["image_url"]` or `remoteMessage.data["image"]`.
     - Construct `NotificationCompat.Builder` with `BigPictureStyle` if bitmap is retrieved, fallback to `BigTextStyle`.
     - Build `PendingIntent` for `MainActivity` containing all `remoteMessage.data` bundle extras with `PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE`.

4. **Runtime Permission Flow (`NotificationPermissionHelper.kt` / `MainActivity.kt`)**:
   - Create Compose helper `RequestNotificationPermissionEffect()`:
     - On API 33+, check `ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)`.
     - If not granted, trigger `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`.
   - On start, call `FirebaseMessaging.getInstance().subscribeToTopic("announcements")` and `"all_users"` to guarantee subscription even if token was refreshed earlier.

5. **Deep Linking in `MainActivity.kt`**:
   - Add handler function `handlePushNotificationIntent(intent: Intent)`:
     - If `extras.getString("url")` exists: launch browser intent `Intent(Intent.ACTION_VIEW, Uri.parse(url))`.
     - If `extras.getString("book_slug")` or `extras.getString("book_id")` exists: trigger navigation or select book in `MulberryViewModel`.
     - If `extras.getString("destination")` exists: switch bottom navigation tab (e.g. `"desk"`, `"snips"`, `"library"`).
   - Invoke `handlePushNotificationIntent(intent)` in `onCreate()`.
   - Override `onNewIntent(intent: Intent)`: call `super.onNewIntent(intent)`, `setIntent(intent)`, and `handlePushNotificationIntent(intent)`.

---

## 3. Verification Plan

1. **Compilation**:
   - Run `compile_applet` to verify Gradle dependency resolution and Kotlin compilation.
2. **Unit Tests**:
   - Run `gradle :app:testDebugUnitTest` to verify app functionality and test suite stability.
