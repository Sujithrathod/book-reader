# Known limitations (unreleased, after 0.4.0)

- **Tested on an emulator only, not yet on a phone.** The app builds and 53 unit tests pass. Import,
  reading and reading aloud were checked on an Android 15 emulator. Lock-screen playback, headset buttons,
  calls and real voices still need a phone; see `docs/test-plan.md`.
- **PDF long-press:** works on pages that have real text. Words in the page header, footer or page number
  aren't read aloud, so a long-press there shows a message. Scanned pages have no words to press.
- **Voice downloads** are done by the phone's voice engine when the app asks for a voice. This was checked
  with Google's engine; other engines (for example Samsung's) may not download this way, so the voice list
  keeps a link to the phone's own voice settings. A download can take a few minutes. It shows the time
  taken, not a percentage: Android's voice engine doesn't report how far a download is. A real percentage
  would need voices the app downloads itself. That was started with sherpa-onnx but isn't finished or included.
- **Reading themes on PDF pages:** PDF pages keep their own paper colour. Dark themes show the night
  version of the page; the other themes change only the menus and the text reflow view.
- **Read-aloud marks in EPUB** are per sentence and use Readium's own highlight, so they aren't rounded
  like the marks in TXT books and on PDF pages.
- **Bookmarks** mark a page (EPUB position, PDF page, TXT paragraph), not a word, and have no notes.
- **Text reflow** shows the PDF's text only (no pictures or tables), split into paragraphs the way the PDF
  stores its lines. The first page can take a few seconds while the PDF text engine starts.
- **No pinch zoom** on PDF pages yet; use "Trim white page margins" or text reflow for larger text.
- **Moon+ Reader style:** night mode, tap areas and the status line follow Moon+ Reader. The menus
  (top bar, bottom card, Settings page) are the app's own design.
- **Font choice** is Default, Serif, Sans or Mono, using the phone's fonts. PDF pages keep their own fonts;
  the choice applies to EPUB, TXT and PDF text reflow.
- **Scanned PDFs** have no text and can't be read aloud yet (needs on-device OCR, planned for later).
- **Password-protected PDFs and DRM-protected EPUBs** can't be opened; the app says so.
- **EPUB highlighting** is per sentence; TXT highlights the word when the voice reports word timing.
- **Word highlighting** depends on the voice engine; some Samsung and network voices don't report it.
- **TXT** uses scroll mode only; EPUB and PDF have pages and scroll.
- **Tables, footnotes and image captions** in EPUBs are read in book order; image text is skipped.
- **Two-column PDFs** are read in the order the PDF stores its text, which is usually right but not always.
- **Very first Play** may take a second or two while the voice engine starts.
- **Not included yet:** highlights and notes, pronunciation fixes, custom font files,
  collections, reading stats, cloud sync (all planned for later phases).
- **Night mode pictures** (PDF pages and EPUB pictures): dark pictures are found by their size and darkness.
  A mostly dark photo smaller than about a tenth of the page width is inverted like text; very large bold
  headings stay readable either way.
- **EPUB night pictures:** SVG pictures can't be processed, so they get a plain colour inversion
  (a dark SVG turns light). Very large pictures are scaled to 1600 pixels on their longest side at night.
