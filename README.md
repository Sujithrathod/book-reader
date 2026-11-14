# Book Reader (Android)

A free, offline ebook reader for Android that reads books aloud with the
phone's built-in voices. Long-press any word to start listening from there.
Inspired by Moon+ Reader's feature set, with original branding.

Status: **all 4 steps done, version 0.4.0**, ready for its first build and on-phone test.
See `CHANGELOG.md`, `docs/test-plan.md` and `docs/known-limitations.md`.

## Reading screen

The page fills the screen with a status line (battery, time, chapter, page).
Tap the middle for the menu: a top bar with the title, and a floating card at
the bottom with the page slider and five tools (Contents, Theme, Listen, Auto
scroll, Settings). Tap the edges to turn pages. The Settings button opens one
page with every option: display, page turning, reading aloud and PDF pages.
Night mode and page behaviour follow Moon+ Reader; the menus are the app's own.

## Build the APK

This cloud workspace cannot download the Android SDK, so build on a computer:

1. Install [Android Studio](https://developer.android.com/studio) (free).
2. Open this `book-reader` folder with **File > Open**. Let Gradle sync finish
   (first time downloads about 1 GB).
3. Plug in an Android phone with USB debugging on, and press **Run**,
   or use **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
   The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

From a terminal with the SDK installed:

```
./gradlew assembleDebug        # build the debug APK
./gradlew testDebugUnitTest    # run unit tests
```

## Install on a phone

Copy `app-debug.apk` to the phone (USB, email, Drive), tap it, and allow
"Install unknown apps" for the app you opened it from. Needs Android 8.0+.

## Release signing

To share with colleagues, build a signed release APK: create a key once, put its
location and passwords in a git-ignored `keystore.properties`, then run
`./gradlew assembleRelease`. Step by step: `docs/release-signing.md`.
Never commit or share the keystore.

## Tech stack

| Library | Version | License | Used for |
| --- | --- | --- | --- |
| Kotlin | 2.1.20 | Apache 2.0 | Language |
| Jetpack Compose (BOM) | 2025.04.00 | Apache 2.0 | Screens |
| Android Gradle Plugin / Gradle | 8.9.1 / 8.14.3 | Apache 2.0 | Build |
| Readium Kotlin Toolkit | 3.1.1 | BSD 3-Clause | Opening and showing EPUB |
| Android PdfRenderer | built in | - | Showing PDF pages |
| Room (+ KSP 2.1.20-2.0.0) | 2.7.1 | Apache 2.0 | Library database |
| DataStore Preferences | 1.1.4 | Apache 2.0 | Reading settings |
| AppCompat / Fragment Compose | 1.7.0 / 1.8.6 | Apache 2.0 | Hosting Readium's reader view |
| PdfBox-Android | 2.0.27.0 | Apache 2.0 | Pulling text out of PDFs to read aloud |
| Android TextToSpeech, MediaSession | built in | - | Voices, lock-screen and headset controls |

minSdk 26 (Android 8.0), targetSdk 35.

## Reading aloud: how it works

- Long-press a word (TXT, EPUB) and reading starts at that exact word, then
  continues sentence by sentence through paragraphs and chapters. With the
  "Long-press a word to read at once" setting off, long-press shows a menu
  with "Read from here" and "Copy" instead.
- PDF pages are shown as images, so the play button reads from the top of
  the page on screen. Words can be long-pressed on the page itself or in text
  reflow (the "A" button), like in TXT books. Scanned PDFs have no text and show a clear message.
- The play button continues from where listening last stopped (saved
  separately from the reading position), or from the screen if there is none.
- Android's TextToSpeech has no pause/resume, so the app speaks one sentence
  at a time, remembers the last word the voice reported, and resumes there.
- Words are highlighted when the voice engine reports word timing (most
  Google voices on Android 8+); otherwise the sentence is highlighted.
  EPUB highlights the sentence.
- Reading continues with the screen off through a foreground service with a
  playback notification. Phone calls and other audio pause it; unplugging
  headphones pauses it. Plain-language guide:
"Book Reader: how it works and why" in the project docs.

## License

MIT, see [LICENSE](LICENSE). Uses Readium (BSD-3-Clause) and PdfBox-Android (Apache 2.0).

## Open-source projects this app uses or could use

- In use: [Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit) (EPUB) and PdfBox-Android (PDF text).
- Candidates looked at on 2026-11-12, not added yet:
  [sherpa-onnx](https://k2-fsa.github.io/sherpa/onnx/) (offline voices the app downloads itself, which
  would allow a real download percentage; tried on 2026-11-13: version 2.0 can no longer speak Piper
  voices, so its Supertonic model is the one to use; unfinished work is on the `built-in-voices-wip` branch), Google ML Kit text recognition (reading scanned
  PDFs aloud), and a pinch-zoom library such as Telephoto (zooming PDF pages).
