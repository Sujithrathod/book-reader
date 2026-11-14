# Changelog

## Unreleased (2026-11-03) - First build and emulator test
- Builds for Android: enabled core library desugaring, which Readium requires.
- Fix: PDFs crashed on opening ("Document already closed"); the reader closed the document it had just opened.
- Dark theme: EPUB pictures are dimmed (like Moon+ Reader's night mode) and PDF pages turn dark, with light text and colours keeping their hue.
- New "Pictures in dark mode" / "Pages in dark mode" setting (Dim, Invert, Original), shown with the dark theme.
- Tested on an Android 15 emulator: import, reading and reading aloud for TXT, EPUB and PDF. 26 unit tests pass.

### Reader redesigned in the style of Moon+ Reader
- Full-screen reading: the page fills the screen with a status line under it (battery, time, chapter, page "29/535").
  The navigation bar hides while reading.
- Tap the middle of the page to show the menu; tap the right side for the next page and the left side for the previous one.
  Back closes the menu first.
- Grey top bar: back, book title, text-reflow "A" button (PDF), more menu.
- Bottom panel: page slider with previous/next arrows and page number, and six tools:
  screen rotation, day/night, read aloud, auto scroll, contents, reading settings.
- "Day/Night Mode" pop-up: Day Theme, Night Theme, Load from theme (light, sepia, dark).
- Floating player bar while reading aloud (previous, pause, next, stop, sleep timer, voice).
- Auto scroll with adjustable speed (continuous scroll glides; pages turn on a timer).
- PDF: chapters from the PDF's own bookmarks in the contents list and status line.
- PDF: page-by-page mode (pages turned sideways) as well as continuous scroll, and trimming of white margins
  (round buttons above the panel).
- PDF: text reflow shows the PDF's text in the reading font and size; long-press a word to read aloud from it.
- PDF: optional page thumbnails above the slider.
- New "PDF Options" page: margin colour, render quality, page gap, reflow button, thumbnails, trim margins,
  page flip animation, page layout; "Control Options" (tap edges to turn pages) and "Miscellaneous"
  (keep screen on, status line).
- 30 unit tests pass (adds tap areas, chapter lookup, title shortening).

### Day and night look matched to Moon+ Reader
- Night mode for PDF pages is now a plain colour inversion, measured from Moon+ Reader screenshots
  (white paper turns black, black text white, blue charts yellow, purple link highlights green).
  It used to rotate colours back to their original hue, so blue stayed blue.
- The menus follow the theme: by day a blue top bar with white text, light grey bottom panel and
  grey page margins; by night the dark grey bars. Menus, sheets and the options page follow too.
- 31 unit tests pass (adds the night colour check against Moon+ Reader colours).

### Night mode keeps dark pictures dark
- Figures that are already dark (diagrams on a dark background, dark photos, dark slides) are found
  and kept as they are, instead of being inverted to light. White charts and white paper are still
  inverted, so the whole night page stays dark.
- 35 unit tests pass (adds night page tests: Moon+ colours, dark figure kept, bold text and table
  lines still inverted, whole dark slide kept).

### Page switching by tapping
- Tapping the right side of a PDF page switches to the next page (its top lands at the top of the
  screen) and the left side to the previous page, instead of scrolling down by a screen.
  Text reflow does the same. TXT books still move one screen per tap.
- Tapping the top or bottom of the middle now opens the menu instead of scrolling.
- 32 unit tests pass.

### EPUB pictures at night match PDF pages
- In night mode, EPUB pictures now get the same treatment as PDF pages: white charts and drawings
  turn dark (colours inverted as in Moon+ Reader), pictures that are already dark stay as they are.
  They used to be only dimmed, so white charts showed as a bright grey box.
- Charts with a see-through background turn dark too, with their lines still visible.
- Back to day mode puts the original pictures back. "Dim" and "Original" are still in the settings;
  "Invert" is the new default, so an earlier saved "Dim" goes back to the default once.
- 38 unit tests pass (adds see-through pictures and matching pictures to the book's files).

### New menus, in-app voice download, long-press on PDF pages (2026-11-12)
- New look for the reading menu: a floating rounded card at the bottom with the chapter and page,
  a slim slider, and five labelled tools (Contents, Theme, Listen, Auto scroll, Settings). The top bar
  matches it. This replaces the flat grey bar with six unlabelled icons.
- Theme button shows Light, Dark and Sepia as circles inside the card.
- Settings button opens one Settings page with every option, in four groups: Display, Page turning,
  Read aloud and PDF pages. It replaces the old settings sheet and the "PDF Options" page.
- New settings: reading brightness, font (Default, Serif, Sans, Mono), auto scroll speed, and screen
  rotation as a choice (it used to be a button that cycled).
- Voices can be downloaded inside the app: a voice that isn't on the phone has a Download button, and it
  is selected once the download finishes. Checked with Google's voice engine.
- Fix: tapping a voice didn't move the selection mark until the list was reopened.
- Voices downloaded outside the app now show up when the list opens or the app comes back to the front.
- The voice list shows the phone's and the book's languages first, with short names ("Voice 1", "Offline").
- PDF pages: long-press a word on the page picture to read aloud from it (or get "Read from here" /
  "Copy word" when "Long-press a word to read at once" is off). The pressed word is highlighted.
- 49 unit tests pass (adds finding the pressed word, matching it to the read text, voice choice and order).

### Page colours, modern highlight, bookmarks, animated library (2026-11-12)
- Eight reading themes, each a background and text colour chosen to read well together: White, Sepia,
  Cream, Green, Grey, Dim, Night blue and Black. They replace Light, Dark and Sepia and apply to EPUB,
  TXT and PDF text reflow. Every pair is tested for strong contrast.
- Reading aloud is marked in a softer, rounded style: the sentence gets a light bar and the spoken word
  a solid pill. PDF pages now show the same marks on the page itself, instead of a frame around the page.
- Auto scroll: new control with pause/resume, slower/faster, and (in paged views) a line that fills up
  before the next page turn. Scrolling is now paced by the clock: it was far too fast, and twice as fast
  on 120 Hz screens. Pages now stay up 6 to 60 seconds depending on speed (it was 1 to 10).
- Voice download shows how long it has been running and a moving bar. Android's voice engine gives no
  percentage, so none is shown.
- Bookmarks: a bookmark button on the top bar, and a Bookmarks tab in the Contents sheet to jump back
  or remove them (database version 3, with migration).
- New settings: warm light (orange tint that cuts blue light) and volume buttons to turn pages.
- Library: "Continue reading" card for the book in progress, EPUB / PDF / TXT filters, covers that
  press in and slide into place, shimmering placeholders while books import, animated progress bars,
  and a details sheet on long-press (it used to go straight to "Remove").
- 53 unit tests pass (adds highlight placement on PDF pages, auto scroll speeds, theme contrast).

## 0.4.0 (2026-11-02) - Step 4: review and handover
- Review fixes: reading continues in the chosen voice after previewing another voice.
- Review fixes: no crash when Android restores the reader after the app was closed in the background.
- Review fixes: headphone-unplug receiver registered the Android 13+ way; PDF close can't crash mid-read.
- Tapping the playback notification opens the book being read.
- Release signing from a git-ignored `keystore.properties` (`docs/release-signing.md`).
- Added `docs/test-plan.md` (on-phone checklist) and `docs/known-limitations.md`.
- 26 unit tests pass. Still not compiled for Android or run on a phone (SDK not reachable from the cloud workspace).

## 0.3.0 (2026-11-02) - Step 3: reading aloud
- Long-press a word in TXT and EPUB books to start reading aloud from that exact word (not the sentence start).
- Setting to show a "Read from here" / "Copy" menu on long-press instead; copying stays available.
- PDF: reads text-based PDFs from the page on screen with PdfBox-Android; drops repeated headers, footers and page numbers; joins words hyphenated across lines; clear message for scanned PDFs.
- Narration continues across paragraphs, chapters and pages, loading text one block at a time (whole books are never loaded into speech memory).
- Player bar: play/pause, previous/next sentence, stop, sleep timer (10 to 60 minutes), voice and speed.
- Voice picker: phone voices grouped by language, "Works offline" / "Needs internet" / "Not downloaded" labels, preview, speed 0.5x to 2.5x, pitch, links to download more voices.
- Default voice: an installed offline voice in the book's language, else the phone's language.
- Pause/resume restarts from the last spoken word; speed or voice changes apply from the current word.
- Word highlighting when the engine reports timing, otherwise sentence highlighting; the reader follows along.
- Listening position saved separately from the reading position (database version 2, with migration).
- Background playback: foreground service, media notification, lock-screen, headset and Bluetooth buttons, pauses on calls, other audio and unplugged headphones.
- 26 unit tests pass (adds sentence splitting, exact-word start, repeated words, selection matching, PDF cleanup, voice choice).
- Not yet verified: Android compile and on-device behaviour.

## 0.2.0 (2026-11-02) - Step 2: library and reader
- Import one or more books through the system file picker; files are copied into private app storage.
- EPUB: title, author and cover read with Readium; DRM-protected or damaged files show a clear message.
- PDF: first page used as the cover; password-protected PDFs show a clear message.
- TXT: UTF-8 with Windows-1252 fallback, hard-wrapped lines joined into paragraphs, chapter headings detected.
- Library grid with covers, format badge, progress bar, search by title or author, sort (recent, title, author, date added), remove with confirmation.
- Reader: EPUB with Readium (pages or scroll), PDF with Android's PdfRenderer, TXT with a Compose text view.
- Contents list: EPUB table of contents, TXT detected chapters, PDF page list.
- Reading settings shared by all books: light, dark, sepia; text size; line spacing; margins; scroll mode (EPUB).
- Reading position and progress saved automatically and restored when a book is reopened.
- 11 unit tests (format detection, text parsing, library search and sort) pass.
- Not yet verified: Android compile and on-device behaviour (Android SDK unreachable from the cloud workspace).

## 0.1.0 (2026-11-02) - Step 1: project setup
- Kotlin + Jetpack Compose Android project (Gradle wrapper 8.14.3, AGP 8.9.1).
- App icon (open book with sound wave), light and dark app theme.
- Library screen with empty state.
- `BookFormat` detection for EPUB, PDF and TXT by MIME type or extension, with unit tests (3 passing).
- Not yet verified: full Android build. The cloud workspace cannot reach dl.google.com, so the APK must be built in Android Studio.
