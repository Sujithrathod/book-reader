# On-phone test plan

Run on at least one real phone (Android 8 or newer). Ideally one Samsung and one Pixel,
since their voice engines differ. Tick each line; note the phone and Android version.

Test books: one EPUB with several chapters, one text-based PDF with headers/page numbers,
one scanned PDF, one TXT (a Project Gutenberg book is ideal), one DRM EPUB if available.

## Install and library
- [ ] APK installs; app opens with an empty library and an Import button.
- [ ] Import several books at once (EPUB, PDF, TXT): covers, titles, authors appear.
- [ ] Search by title and author; each sort option works.
- [ ] Long-press a book > Remove: it disappears and stays gone after restart.
- [ ] DRM EPUB / password PDF / random file: clear message, no crash.

## Reading
- [ ] EPUB: pages and scroll mode, contents list jumps to chapters.
- [ ] Text size, line spacing, margins, light/dark/sepia apply immediately (EPUB, TXT).
- [ ] PDF pages render sharply; contents list jumps to pages.
- [ ] Close and reopen each book: it reopens at the same place.
- [ ] Rotate the phone while reading: position kept, no crash.

## Reader layout (Moon+ Reader style)
- [ ] Book opens full screen; status line shows battery, time, chapter and page; navigation bar hidden.
- [ ] Tap the middle: grey top bar and bottom panel appear. Tap again or press Back: they hide.
- [ ] Tap the right side: next page (PDF: the next page starts at the top). Left side: previous page.
- [ ] Tap the top or bottom of the middle: the menu opens (no scrolling).
- [ ] Page slider and its arrows jump to the right page; the number next to it updates while dragging.
- [ ] Day/night pop-up: Day Theme, Night Theme, Load from theme each apply at once.
- [ ] Day: blue top bar, light grey bottom panel, grey margins. Night: dark grey bars, black margins.
- [ ] Night PDF with colour charts: colours are inverted (blue lines show yellow, white paper black).
- [ ] Night PDF with a figure on a dark background: the figure stays dark with its own colours, no white edge around it.
- [ ] Rotation button cycles Auto-rotate / Portrait / Landscape.
- [ ] Auto scroll: starts, speed −/+ works, tapping the page or ✕ stops it.
- [ ] PDF with bookmarks: contents list shows chapters; status line shows the chapter.
- [ ] PDF round buttons: page by page / continuous, and trim margins.
- [ ] PDF "A" button: text reflow; long-press a word reads from it; "A" again shows pages at the same page.
- [ ] PDF Options: margin colour, render quality, page gap, thumbnails, page flip animation, Control Options, Miscellaneous.

## Reading aloud: starting point
- [ ] TXT: long-press a word mid-sentence. Reading starts at **that word**, not the sentence start.
- [ ] EPUB: same, in the middle of a chapter.
- [ ] Repeated words: long-press the second "the" in a sentence with two. It starts at the second.
- [ ] Turn off "Long-press reads at once": long-press shows Read from here / Copy; both work.
- [ ] Long-press another word while reading: old speech stops, new one starts there.
- [ ] PDF: Play reads from the top of the page on screen; headers and page numbers are skipped.
- [ ] Scanned PDF: Play shows the "no readable text" message.

## Reading aloud: while playing
- [ ] Sentence (and, with Google voices, word) highlight follows the voice; the page follows along.
- [ ] Reading continues into the next paragraph, the next chapter (EPUB) and next page (PDF).
- [ ] Pause, wait, resume: continues from the last spoken word.
- [ ] Previous / next sentence buttons.
- [ ] Change speed and pitch while playing: applies from the current word.
- [ ] Pick another voice; preview a voice; reading continues in the chosen voice after preview.
- [ ] "Needs internet" voice in airplane mode: clear message; offline voice works in airplane mode.
- [ ] Sleep timer 10 min (or patch to 1 min for testing): reading pauses when it ends.

## Background and interruptions
- [ ] Lock the screen: reading continues for 10+ minutes.
- [ ] Notification shows the book; play/pause/next/previous/stop work; tapping it opens the book.
- [ ] Lock-screen and Bluetooth/headset buttons play and pause.
- [ ] Unplug wired headphones or disconnect Bluetooth: reading pauses.
- [ ] Incoming call or another app playing music: reading pauses; resumes after a short interruption.
- [ ] Stop: notification disappears.
- [ ] Android 13+: notification permission asked once at first Play; reading works even if denied.

## Restart
- [ ] Force-stop the app, reopen the book, press Play: continues from the saved listening position.
- [ ] Reading position and listening position are separate (scroll away, Play still resumes listening).

Report anything odd with: phone model, Android version, book type, what you did, what happened.
