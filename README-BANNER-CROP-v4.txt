BlueKite LMS Android v4 — Banner Crop Test

This version does NOT change the LMS backend or existing LMS features.
It only changes the Android WebView wrapper:
1. Crops the top 72px of the Apps Script frame to hide the Google Apps Script warning/banner.
2. Handles Android 15 / target SDK 35 window insets so content does not overlap the status/navigation bars.
3. Keeps the existing JavaScript, cookies, file chooser, fullscreen video, and back-button behavior.

Build: GitHub Actions -> assembleDebug
