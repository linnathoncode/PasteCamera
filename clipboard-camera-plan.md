# Clipboard Camera — App Plan

## Overview

Clipboard Camera is an Android camera app that lets users take a photo and copy it directly to the system clipboard instead of saving it to the Gallery.

The core experience is:

> Take a photo. Paste it. Nothing saved.

The photo is temporary by default. It should not appear in the Gallery, Camera folder, or cloud backups unless the user explicitly chooses to save it.

The app is intended for disposable photos such as:

- Receipts
- Documents
- Notes
- Whiteboards
- Labels
- Objects
- Screens
- Anything the user only needs to quickly copy and paste

---

## Platform Strategy

### Initial Platform

Android only.

The first version should be built as a native Android app because the core functionality depends heavily on Android-specific APIs:

- Camera capture
- Clipboard access
- Content URIs
- FileProvider
- Cache storage
- MediaStore
- URI permissions

Using Flutter, React Native, or another cross-platform framework would add complexity without providing much benefit for V1.

### Future iOS Version

If the Android version proves useful, build a native iOS version using Swift and SwiftUI.

The app is small enough that maintaining two native implementations should remain manageable.

---

# Recommended Stack

| Area | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Camera | CameraX |
| Architecture | Simple MVVM |
| Async work | Kotlin Coroutines |
| Temporary storage | `context.cacheDir` |
| Clipboard | Android `ClipboardManager` + `ClipData` |
| Temporary image access | `FileProvider` or custom content provider |
| Permanent image saving | MediaStore |
| Build system | Gradle Kotlin DSL |
| Database | None |
| Backend | None |
| Networking | None |
| Dependency injection | None initially |

A reasonable minimum Android version is Android 10 / API 29.

---

# Architecture Principles

The most important architectural rule is:

> Taking a picture must never imply saving a picture.

The normal Android camera flow often looks like:

```text
Camera
  ↓
DCIM/Camera/photo.jpg
  ↓
Gallery
```

Clipboard Camera should instead work like this:

```text
CameraX
   │
   ▼
App private cache
/cache/clipboard_camera/
    capture_123.jpg
   │
   ▼
Content URI
content://com.clipboardcamera.fileprovider/...
   │
   ▼
Android Clipboard
   │
   ▼
Paste into another app
```

The capture flow should not touch:

```text
MediaStore
DCIM/
Pictures/
Gallery
Google Photos backup
```

unless the user explicitly presses **Save**.

---

# Main User Flow

The opening screen should primarily be the camera.

```text
┌─────────────────────────────┐
│                             │
│                             │
│        CAMERA PREVIEW       │
│                             │
│                             │
│                             │
│                             │
│                             │
│             ⚡              │
│                             │
├─────────────────────────────┤
│                             │
│            ●                │
│                             │
└─────────────────────────────┘
```

The ideal interaction is:

```text
Open app
   ↓
Take photo
   ↓
Encode JPEG
   ↓
Store in app cache
   ↓
Copy image URI to clipboard
   ↓
Show confirmation
   ↓
User switches apps and pastes
```

Feedback can be as simple as:

```text
✓ Copied to clipboard
```

The camera should remain ready for another capture.

The intended experience is:

> Open → Tap → Paste

and not:

> Open → Tap → Preview → Confirm → Share → Choose app → Send

---

# V1 Feature Set

## Camera Features

Include:

- Rear camera
- Tap to focus
- Pinch to zoom
- Flash modes
  - Auto
  - On
  - Off
- Orientation handling
- Shutter button

Do not initially add:

- Filters
- Video
- Manual ISO
- Manual exposure controls
- Complex multi-lens controls
- QR scanning
- OCR
- Image editing

These can be considered later if they improve the core disposable-photo workflow.

---

# Clipboard Implementation

After taking a photo, the app should place the image on the Android clipboard.

Conceptually:

```kotlin
val clip = ClipData.newUri(
    contentResolver,
    "Clipboard Camera",
    imageUri
)

clipboardManager.setPrimaryClip(clip)
```

The URI should be a `content://` URI.

Do not expose:

```text
file:///data/user/...
```

because other apps cannot directly read private app files.

The temporary image should instead be exposed through:

- `FileProvider`, or
- a small custom content provider

with appropriate read permissions.

---

# Compatibility Testing

Image clipboard handling is less universal than text clipboard handling.

The first technical prototype should test pasting into common apps such as:

- WhatsApp
- Telegram
- Discord
- Google Keep
- Gmail
- Google Docs
- ChatGPT
- Gemini
- Samsung Notes

Different apps may handle image clipboard data differently.

Some may:

- Show Paste
- Accept the image directly
- Ignore clipboard images
- Expect specific MIME types
- Require URI permissions

This compatibility testing should happen before spending much time on UI polish.

---

# Temporary File Lifecycle

Temporary images should live in private app cache storage.

Example:

```text
cacheDir/
└── captures/
    ├── 1726423172.jpg
    ├── 1726423188.jpg
    └── ...
```

These files are not visible to the Gallery or other media apps.

The app should not delete the image immediately after placing it on the clipboard.

The clipboard may only contain a URI pointing to the image. If the underlying file disappears too early, the destination app may fail to read it.

Possible failure:

```text
FileNotFoundException
```

A sensible cleanup policy:

- Keep the current clipboard image
- Delete old captures automatically
- Delete stale images during app startup
- Optionally expire images after a configured time
- Limit cache count if necessary

Example policies:

```text
Maximum 20 temporary captures
```

or:

```text
Maximum age: 24 hours
```

A database is unnecessary for V1.

The filesystem can be treated as the temporary image store.

---

# Explicit Save Behavior

Saving should be an explicit user action.

After a capture, secondary actions could include:

```text
✓ Copied

[ Save ]   [ Share ]
```

These actions should not interrupt the main workflow.

When the user presses **Save**, the app copies or inserts the image into Android MediaStore.

Example destination:

```text
Pictures/Clipboard Camera/
```

Only then should the image become visible to:

- Gallery apps
- Google Photos
- Cloud backup systems
- Other media scanners

The product rule remains:

```text
Capture = temporary

Save = permanent
```

---

# UI Structure

The app should remain extremely small.

## Screen 1 — Camera

The default screen.

```text
┌───────────────────────────┐
│ ⚙                     ⚡  │
│                           │
│                           │
│       Camera Preview      │
│                           │
│                           │
│                           │
│                           │
│            ●              │
└───────────────────────────┘
```

Primary elements:

- Camera preview
- Shutter button
- Flash control
- Optional settings button
- Small thumbnail for the last capture

---

## Screen 2 — Last Capture Preview

This does not necessarily need to be a full screen.

A bottom sheet is probably enough.

```text
┌───────────────────────────┐
│                           │
│          PHOTO            │
│                           │
│                           │
├───────────────────────────┤
│ ✓ Copied                  │
│                           │
│ Save       Share    Delete│
└───────────────────────────┘
```

Possible actions:

- Save
- Share
- Delete
- Copy again

---

## Screen 3 — Settings

Keep settings minimal.

Example:

```text
Clipboard Camera

Image quality
○ High
● Balanced
○ Small

Auto-copy                 ON

Keep temporary images
24 hours

Haptic feedback           ON

About
```

Many of these settings can be postponed until after V1.

---

# Package Structure

Avoid over-engineering.

Suggested structure:

```text
com.clipboardcamera
│
├── MainActivity.kt
│
├── camera/
│   ├── CameraScreen.kt
│   ├── CameraViewModel.kt
│   └── CameraController.kt
│
├── clipboard/
│   └── ImageClipboard.kt
│
├── storage/
│   ├── TemporaryImageStore.kt
│   └── ImageSaver.kt
│
├── ui/
│   ├── components/
│   └── theme/
│
└── settings/
    └── SettingsScreen.kt
```

There is no need for a large enterprise-style structure such as:

```text
domain/
repository/
datasource/
usecases/
entities/
```

unless the project grows significantly.

---

# Core Components

## CameraController

Responsibilities:

```text
capturePhoto()
```

Handles interaction with CameraX.

---

## TemporaryImageStore

Responsibilities:

```text
createCaptureFile()
getContentUri()
deleteCapture()
cleanupOldCaptures()
```

Manages private cache files.

---

## ImageClipboard

Responsibilities:

```text
copyImage(uri)
```

Handles clipboard interaction.

---

## ImageSaver

Responsibilities:

```text
saveToGallery(uri)
```

Handles explicit permanent saving through MediaStore.

---

# ViewModel Responsibilities

`CameraViewModel` should coordinate the main flow.

Conceptually:

```text
CameraViewModel
       │
       ├── CameraController
       ├── TemporaryImageStore
       └── ImageClipboard
```

Example capture flow:

```kotlin
fun capture() {
    val file = temporaryStore.createCaptureFile()

    camera.capture(file) { result ->

        val uri = temporaryStore.getContentUri(file)

        clipboard.copyImage(uri)

        state.update {
            it.copy(
                lastCapture = uri,
                copied = true
            )
        }
    }
}
```

The central business logic should remain roughly this simple.

---

# Image Format

Use JPEG initially.

Recommended default:

```text
JPEG
~85–90% quality
```

Reasons:

- Excellent compatibility
- Good compression
- Suitable for photos
- Easy to paste into other apps

Avoid PNG for regular camera photos because file sizes are unnecessarily large.

Avoid HEIC initially because clipboard and receiving-app compatibility matters more than storage efficiency.

A later setting could expose:

```text
Small       70
Balanced    85
High        95
```

but this is not necessary for the first release.

---

# Permissions

The normal capture flow should require very few permissions.

Primary permission:

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

Broad storage permissions should not be needed.

Temporary photos stay in private cache storage.

Explicit gallery saving can be implemented using modern Android MediaStore APIs and scoped storage.

---

# Fallback Sharing

Some apps may not support pasting image clipboard content.

The preview should therefore offer a secondary:

```text
Share
```

action.

This can launch the Android share sheet.

However, Share should remain secondary.

The product should not become just another camera + share-sheet app.

The core value is still:

```text
Take → Paste
```

---

# Future iOS Architecture

The eventual platform equivalents could look like this:

| Android | iOS |
|---|---|
| Kotlin | Swift |
| Jetpack Compose | SwiftUI |
| CameraX | AVFoundation |
| ClipboardManager | UIPasteboard |
| `cacheDir` | temporary directory |
| MediaStore | Photos framework |

The core architecture can remain conceptually identical.

---

# Kotlin Multiplatform

Kotlin Multiplatform is not recommended for V1.

Potentially shareable code later:

- Settings models
- Cleanup policies
- Image metadata
- General business rules

Platform-specific functionality would still include:

- Camera
- Clipboard
- Temporary URI access
- Gallery / Photos saving

Because those platform-specific features represent most of the app, KMP offers little benefit initially.

Revisit it only after the Android version is stable.

---

# Development Phases

## Phase 1 — Technical Prototype

Build the smallest possible prototype.

Goal:

```text
CameraX
   ↓
Private cache
   ↓
Content URI
   ↓
Android clipboard
   ↓
Paste into another app
```

Test against major target apps.

Do not spend significant time on UI yet.

---

## Phase 2 — Core App

Implement:

- Jetpack Compose UI
- Camera preview
- Shutter button
- Focus
- Zoom
- Flash
- Copy confirmation
- Last capture thumbnail
- Temporary cache cleanup

---

## Phase 3 — Persistence Actions

Add:

- Save to Gallery
- Share
- Delete
- Copy again

Ensure normal captures still never enter MediaStore automatically.

---

## Phase 4 — Polish

Add:

- Haptic feedback
- Small animations
- Better orientation handling
- Permission explanations
- Error states
- App icon
- Launch screen
- Accessibility improvements

---

## Phase 5 — Release Hardening

Test on:

- Android 10+
- Samsung devices
- Pixel devices
- Xiaomi / HyperOS devices
- Different clipboard targets
- Different screen orientations
- Process death
- App restart
- Low storage
- Permission denial
- Cache cleanup edge cases

---

# MVP Success Criteria

The MVP is successful if the following flow works reliably:

```text
1. Open Clipboard Camera
2. Point camera
3. Tap shutter
4. Image is copied automatically
5. Switch to another app
6. Paste image
7. Image never appears in Gallery
```

The image should remain temporary unless the user explicitly selects **Save**.

---

# Core Product Invariant

The application should always preserve this distinction:

```text
Capture = temporary

Save = permanent
```

Everything else should remain secondary to the core concept:

> Take a photo. Paste it. Nothing saved.
