# On-phone test plan

Run on at least one real phone (Android 8 or newer). Ideally one Samsung and one Pixel,
since their voice engines differ. Tick each line; note the phone and Android version.

Test books: one EPUB with several chapters, one text-based PDF with headers/page numbers,
one scanned PDF, one TXT (a Project Gutenberg book is ideal), one DRM EPUB if available.

## Install and library
- [ ] APK installs; app opens with an empty library and an Import button.
- [ ] While books import, shimmering placeholder covers show; the books then slide into the grid.
- [ ] After opening a book, the library shows a "Continue reading" card for it; tapping it reopens the book.
- [ ] EPUB / PDF / TXT filter chips narrow the grid; "All" shows everything again.
- [ ] Press a cover: it shrinks slightly. Long-press: a details sheet with Continue/Read and Remove.
- [ ] Scroll the grid: the Import button shrinks to a round button; back at the top it grows again.
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

## Reader layout
- [ ] Book opens full screen; status line shows battery, time, chapter and page; navigation bar hidden.
- [ ] Tap the middle: top bar and the floating bottom card appear. Tap the page or press Back: they hide.
- [ ] Tap the right side: next page (PDF: the next page starts at the top). Left side: previous page.
- [ ] Tap the top or bottom of the middle: the menu opens (no scrolling).
- [ ] Page slider and its arrows jump to the right page; the number next to it updates while dragging.
- [ ] Theme button: eight colour circles appear in the card (swipe sideways); each applies at once.
- [ ] Bookmark button on the top bar: adds a bookmark; Contents > Bookmarks lists it, opens it and removes it.
- [ ] Settings > Display > Warm light: the page turns warmer as the slider moves; Off removes the tint.
- [ ] Settings > Page turning > Volume buttons turn pages: volume down = next, up = previous; volume unchanged.
- [ ] Reading aloud: the sentence has a soft rounded bar and the spoken word a solid pill (TXT, PDF reflow).
- [ ] Reading aloud on a PDF page: the sentence and spoken word are marked on the page and move along.
- [ ] Day: white bars with dark icons. Night: near-black bars with light icons. Listen button is blue in both.
- [ ] Night PDF with colour charts: colours are inverted (blue lines show yellow, white paper black).
- [ ] Night PDF with a figure on a dark background: the figure stays dark with its own colours, no white edge around it.
- [ ] Settings button opens the Settings page; Back returns to the book.
- [ ] Settings > Display: brightness (turn off "Use phone brightness"), text size, font, line spacing, margins.
- [ ] Settings > Page turning: screen rotation Auto / Portrait / Landscape, auto scroll speed, keep screen on.
- [ ] Auto scroll: starts at a readable pace; pause and resume work; − / + change the speed; ✕ or a tap stops it.
- [ ] Auto scroll in a paged view: the line in the control fills up, then the page turns.
- [ ] PDF with bookmarks: contents list shows chapters; status line shows the chapter.
- [ ] PDF round buttons: page by page / continuous, and trim margins.
- [ ] PDF "A" button: text reflow; long-press a word reads from it; "A" again shows pages at the same page.
- [ ] PDF page (not reflow): long-press a word. It is highlighted and reading starts at that word.
- [ ] PDF page with trimmed margins and in page-by-page layout: long-press still picks the right word.
- [ ] Settings > PDF pages: trim margins, sharpness, page gap, colour around pages, reflow button, thumbnails.
- [ ] Voice list: tap Download on a voice. It shows "Downloading" with a timer, then becomes selected without leaving the app.
- [ ] Voice list: tap another downloaded voice. The mark moves at once and the next sentence uses it.

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
