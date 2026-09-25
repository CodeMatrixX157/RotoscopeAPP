# Rotoscope — Module 1: Project Scaffold

On-device, frame-by-frame object cutout from an image sequence.

## Fixes made in a pre-test review pass

- **Frame-order bug**: sequence sorting used `Uri.lastPathSegment`, which for content:// URIs
  from the system picker is often just an internal numeric row ID, not the filename — this could
  silently scramble frame order. Now queries the real `DISPLAY_NAME` via `ContentResolver`.
- **Format-mismatch bug**: image-sequence import now decodes and re-encodes every frame to a real
  PNG instead of byte-copying, so a JPG renamed to `.png` can't later break native code that
  trusts the extension.
- **Per-file import progress**: image-sequence ingest now emits progress per file (was previously
  silent until the whole batch finished).
- **Partial-failure tolerance**: a single unreadable file no longer aborts the whole import —
  it's skipped and counted, surfaced to the user instead of silently lost or a hard failure.
- **Gallery-visibility bug (API 24-28)**: converted frames written directly to external storage
  weren't showing up in Gallery/Photos until a reboot — added a `MediaScannerConnection` scan.
- **Removed the heavy-processing warning from plain image-sequence import** — copying files isn't
  the sustained CPU/GPU work that warning is about; it now only appears on Converter (real video
  decode) and will appear again once segmentation/propagation land.
- **Cancel support** added to both Import and Convert — previously no way to stop a long-running
  job once started.
- **Permission-denial feedback**: denying the legacy storage permission (API 24-28) now shows a
  message instead of silently doing nothing.

## What's in this module

- Gradle project skeleton (Kotlin + Jetpack Compose + NDK-ready), matching the conventions from
  CAMERA-TRACKER and PDF Tools: `arm64-v8a`/`armeabi-v7a` ABI filtering, Compose BOM, minSdk 24.
- **Import (rotoscoping)**: image sequence only — no video path here. Rotoscoping always starts
  from an already-validated numbered folder, which keeps the pipeline's assumptions simple.
- **Convert (standalone tool)**: a separate screen, reachable from Home, that turns a video into
  a numbered PNG sequence via `MediaMetadataRetriever` (see the perf note in `FrameExtractor.kt` —
  first thing to swap for a native decoder later) and saves it to `Pictures/RotoscopeFrames/<name>/`
  via MediaStore, so it shows up in the gallery/any file app afterward — useful on its own, not
  just a rotoscoping prerequisite. Decode logic in `FrameExtractor` is decoupled from where frames
  get written (`writeFrame` callback), so it isn't tied to MediaStore specifically.
- **Thermal-aware processing**: `ThermalMonitor.kt` wraps Android's `PowerManager` thermal-status
  API (API 29+). Any heavy loop collects its `Flow<ThermalState>` and pauses on HOT/CRITICAL,
  resuming automatically on NORMAL — no polling, no manual intervention. Devices below API 29 get
  `ThermalState.UNSUPPORTED` and a plain-language heads-up instead of a silent no-op. Both the
  converter and (eventually) mask propagation reuse this same monitor.
- **Warning dialog** (`WarningDialog.kt`): shown once before the first heavy job (import or
  convert), explains phone heating and auto-pause/resume in plain language, with a persisted
  "don't show again" (DataStore, same pattern as PDF Tools' `ThemePreference.kt`).
- **Thermal status banner**: visible only when something's actually happening (paused/cooling) —
  doesn't nag during normal operation.

## Before you build

1. **Rename the package.** `com.example.rotoscope` is a placeholder — change `applicationId` in
   `app/build.gradle.kts` (and the package structure) to your own reverse-domain id before this
   ever touches Play Store. Google will reject `com.example.*`.
2. **Gradle wrapper**: not committed — the wrapper jar is a binary this sandbox couldn't download.
   Run `gradle wrapper --gradle-version 8.5` once on a machine with internet and commit the
   result; until then, CI builds via `gradle/actions/setup-gradle` instead of `./gradlew` (see
   `.github/workflows/android-build.yml`, in a hidden `.github` folder — enable "show hidden
   files" if your extractor doesn't show it).
3. Push to GitHub, let CI build the debug APK the way your other projects do.

## Open decision before Module 4

**On-device model vs. cloud API for segmentation** — this changes the architecture, so flagging
it before building further:
- **On-device (ONNX Runtime / TFLite)**: works offline, no per-use cost, fast — but bigger app
  size and you own the model-optimization work.
- **Cloud API**: simpler to integrate, but every frame gets uploaded (cost + latency + requires
  internet), which cuts against "fast because it's local."
- **Hybrid**: on-device by default, cloud fallback for phones that can't run the model well.

## Roadmap

1. ~~Project scaffold~~ ✅ (this module)
2. ~~Import (image sequence) + standalone video-to-sequence converter~~ ✅ (this module)
3. Object selection UI — tap/scribble on frame 1
4. Segmentation module — **blocked on the decision above**
5. Mask propagation across the sequence (thermal-aware batching, reusing `ThermalMonitor`)
6. Edge matting / refinement pass
7. Manual correction + re-propagation UI
8. Export (PNG sequence w/ alpha, or ProRes4444 / WebM-alpha video)
9. Settings, onboarding, Play Store polish
