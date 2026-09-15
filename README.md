# Paste Camera

An Android camera app for disposable photos. It captures a JPEG into private app cache, copies the image to the clipboard, and does not add it to the gallery unless the user presses Save.

## Open and run

Open this folder in Android Studio, allow Gradle to sync, then run the `app` configuration on an Android 10 or later device. The app needs camera permission.

## Main behavior

- Capture copies the latest image to the Android clipboard.
- The capture is exposed through a `content://` URI backed by cache storage.
- Cache cleanup keeps the active capture, removes captures older than 24 hours, and limits old files to 20.
- Save writes the image to `Pictures/Paste Camera/` through MediaStore.
- Share opens Android's standard share sheet.

Clipboard image support varies by receiving app. Test paste behavior on the specific Android apps you use.

The original product plan is [clipboard-camera-plan.md](clipboard-camera-plan.md).
