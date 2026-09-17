# Paste Camera

Paste Camera is for photos you need for a moment, then want to paste into another app. Take a receipt, note, label, document, whiteboard, or quick reference photo, switch apps, and paste it.

Normal captures stay in private app cache and are copied to the Android clipboard. They do not appear in Gallery, Google Photos, Camera, or cloud backup. Use Save only when you actually want to keep the photo.

## Open and run

Open this folder in Android Studio, allow Gradle to sync, then run the `app` configuration on an Android 10 or later device. The app needs camera permission.

## Intended usage

- Capture takes a JPEG, stores it in temporary cache, and copies it to the clipboard.
- Paste the photo into an app that accepts image clipboard data.
- The app never writes a normal capture to your gallery.
- Save writes the current photo to `Pictures/Paste Camera/`.
- Share is available when paste is not supported by the destination app.
- The app keeps the current capture, removes captures older than 24 hours, and limits older files to 20.

Clipboard image support varies by receiving app. Test paste behavior on the specific Android apps you use.
