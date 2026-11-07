# Known limitations (unreleased, after 0.4.0)

- **Tested on an emulator only, not yet on a phone.** The app builds and 35 unit tests pass. Import,
  reading and reading aloud were checked on an Android 15 emulator. Lock-screen playback, headset buttons,
  calls and real voices still need a phone; see `docs/test-plan.md`.
- **PDF long-press:** PDF pages are shown as pictures, so you can't long-press a single word on them.
  Switch to text reflow (the "A" button) to long-press a word, or press Play to read from the top of the page.
- **Text reflow** shows the PDF's text only (no pictures or tables), split into paragraphs the way the PDF
  stores its lines. The first page can take a few seconds while the PDF text engine starts.
- **No pinch zoom** on PDF pages yet; use "Trim white page margins" or text reflow for larger text.
- **Moon+ Reader style:** the reader follows Moon+ Reader's layout and menus, but the app keeps its own
  name, icon and colours, and only options that work in this app are shown (no annotations or OpenGL engine).
- **Scanned PDFs** have no text and can't be read aloud yet (needs on-device OCR, planned for later).
- **Password-protected PDFs and DRM-protected EPUBs** can't be opened; the app says so.
- **EPUB highlighting** is per sentence; TXT highlights the word when the voice reports word timing.
- **Word highlighting** depends on the voice engine; some Samsung and network voices don't report it.
- **TXT** uses scroll mode only; EPUB and PDF have pages and scroll.
- **Tables, footnotes and image captions** in EPUBs are read in book order; image text is skipped.
- **Two-column PDFs** are read in the order the PDF stores its text, which is usually right but not always.
- **Very first Play** may take a second or two while the voice engine starts.
- **Not included yet:** bookmarks, highlights and notes, pronunciation fixes, font family choice,
  collections, reading stats, cloud sync (all planned for later phases).
- **Night mode pictures** (PDF pages and EPUB pictures): dark pictures are found by their size and darkness.
  A mostly dark photo smaller than about a tenth of the page width is inverted like text; very large bold
  headings stay readable either way.
- **EPUB night pictures:** SVG pictures can't be processed, so they get a plain colour inversion
  (a dark SVG turns light). Very large pictures are scaled to 1600 pixels on their longest side at night.
