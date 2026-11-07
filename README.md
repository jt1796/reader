# Reader — Android PDF Reader & Bookshelf

A modern, native Android PDF reader application featuring a wooden bookshelf UI, automatic reading position memory, and a resizable N×M home screen Bookshelf AppWidget.

---

## Features

### 1. Wooden Bookshelf UI
- **Physical Bookshelf Aesthetic**: Books sit on photorealistic wooden shelves featuring multi-tone wood grain gradients, rim lighting, top perspective bevels, and drop shadows.
- **3D Book Covers**: Displays cover thumbnails extracted directly from page 1 (page 0) of imported PDF files, complete with spine shadows, page edge effects, and progress badges.
- **Direct Book Management**: Tap any book to immediately open and resume reading. Long-press or use the options menu to rename, reset reading progress to page 1, or remove books.
- **Empty State**: Elegant empty shelf illustration with a quick-import action button.

### 2. PDF Reader with Persistent Position Memory
- **Always Remembers Your Position**: Whenever you turn a page, scrub with the slider, switch apps, or exit, your reading position is persisted immediately to internal storage.
- **Instant Resume**: Re-opening any book from the shelf or widget jumps directly to your exact saved page.
- **Fluid Navigation**: Horizontal pager supporting smooth page-turning gestures, quick page scrubber slider, and next/prev controls.
- **Zoom & Pan**: Pinch-to-zoom (up to 4×) and double-tap zoom with smooth panning when zoomed in.
- **Immersive Mode**: Tap anywhere on the page to toggle top and bottom reading controls.
- **High Performance**: Native `android.graphics.pdf.PdfRenderer` backed by an in-memory LRU bitmap cache for zero-latency page flips.

### 3. File System Import (Storage Access Framework)
- Tap **"Import PDF"** from the top bar or floating action button.
- Native Android system document picker (`OpenMultipleDocuments`) allows selecting single or multiple PDF documents directly from the device's storage, Google Drive, or downloads.
- Imported PDFs are copied into the app's sandboxed storage (`filesDir/books/`) to guarantee permanent access across reboots, device file cleanups, and permission revocations.
- Automatic cover thumbnail generation saved in `filesDir/covers/`.

### 4. Bookshelf Widget (N × M Home Screen Widget)
- **N by M Resizable Grid**: Place the widget on your Android home screen and resize it to any N×M grid size (from 2×2 up to full screen).
- **Shows Bookshelf & Thumbnails**: Displays a grid of books on wooden shelf ledges with cover thumbnails, titles, and reading progress indicators (`p. X/Y (Z%)`).
- **One-Tap Open**: Tapping any book in the widget opens the app directly into that book at your last-read page!
- **Quick Import**: Tapping the widget header or "+ Add" button launches the app to add new books.
- **Real-Time Sync**: Automatically updates whenever books are added, removed, or reading progress changes.

---

## Architecture & Tech Stack

- **Platform**: Android (Kotlin)
- **UI Framework**: Jetpack Compose + Material 3
- **PDF Engine**: Native `android.graphics.pdf.PdfRenderer`
- **Widget**: Android AppWidget framework (`AppWidgetProvider` + `RemoteViewsService` + `RemoteViewsFactory` with `GridView`)
- **Persistence**: File-based metadata store (`books_metadata.json`) + internal storage for books & covers
- **Build System**: Gradle 8.14.3 with Android Gradle Plugin (AGP) 8.10.1 & Kotlin 2.1.20

---

## Project Structure

```
reader/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/reader/app/
│   │   │   │   ├── data/
│   │   │   │   │   ├── Book.kt                   # Book data model & progress calculation
│   │   │   │   │   └── BookRepository.kt         # Persistence, SAF import & thumbnail generation
│   │   │   │   ├── pdf/
│   │   │   │   │   └── PdfManager.kt             # PdfRenderer engine with LRU bitmap cache
│   │   │   │   ├── ui/
│   │   │   │   │   ├── reader/
│   │   │   │   │   │   └── PdfReaderScreen.kt    # PDF viewer, zoom/pan & auto position save
│   │   │   │   │   ├── shelf/
│   │   │   │   │   │   ├── BookItem.kt           # 3D book cover card with progress ribbon
│   │   │   │   │   │   ├── BookshelfScreen.kt    # Wooden shelf layout & file picker
│   │   │   │   │   │   └── ShelfPlank.kt         # Custom Canvas wooden shelf graphics
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Color.kt              # Bookshelf wood tones & accent colors
│   │   │   │   │       └── Theme.kt              # MaterialTheme configuration
│   │   │   │   ├── widget/
│   │   │   │   │   ├── BookshelfWidgetProvider.kt      # AppWidgetProvider & broadcast handler
│   │   │   │   │   └── BookshelfRemoteViewsService.kt  # RemoteViews collection adapter
│   │   │   │   └── MainActivity.kt               # Main entry point & deep link dispatcher
│   │   │   ├── res/
│   │   │   │   ├── drawable/                     # Shelf textures, widget backgrounds, icons
│   │   │   │   ├── layout/                       # Widget XML layouts (grid and items)
│   │   │   │   ├── values/                       # Strings and colors
│   │   │   │   └── xml/
│   │   │   │       └── bookshelf_widget_info.xml # AppWidget metadata configuration
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   │       └── java/com/reader/app/
│   │           └── BookDataTest.kt               # Unit tests for models & JSON serialization
│   └── build.gradle.kts
├── gradle/wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── local.properties
```

---

## Building & Running

### Prerequisites
- JDK 21
- Android SDK (API 35+)

### Commands
```bash
# Run unit tests
./gradlew test

# Assemble Debug APK
./gradlew assembleDebug

# Install on a connected Android device or emulator
./gradlew installDebug
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Build a signed release APK from the CLI

This project checks in `signing.properties` and `app/reader-release.jks`, including the signing password, so no key generation or password entry is needed to build. The signing key is intended only for this private app; use a different key if you ever publish through Google Play. Build the signed APK with:

```bash
./gradlew assembleRelease
```

The signed APK will be at `app/build/outputs/apk/release/app-release.apk`. If you replace the checked-in signing configuration, the build also accepts the environment variables `READER_KEYSTORE`, `READER_STORE_PASSWORD`, `READER_KEY_ALIAS`, and `READER_KEY_PASSWORD`.

Android only allows an APK to update an installed app when both are signed with the same key. If an existing installation was signed with a different keystore, this APK cannot update it in place. Keep using that original keystore to preserve in-place upgrades, or uninstall the existing app before installing this build (uninstalling may remove its app data).
