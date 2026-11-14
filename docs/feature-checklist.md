# Feature checklist (vs. Moon+ Reader)

Status: implemented / partial / planned / deferred / unsupported.
"Implemented" means written and unit-tested where possible. On-phone checks are listed in `test-plan.md`;
limitations are in `known-limitations.md`.

| Feature | Status | Step |
| --- | --- | --- |
| Library with covers, title, author | implemented | 2 |
| Search, sort, recent books | implemented | 2 |
| Remove a book | implemented | 2 |
| Import EPUB | implemented | 2 |
| Import PDF (text-based and scanned, for viewing) | implemented | 2 |
| Import TXT | implemented | 2 |
| Table of contents | implemented (EPUB TOC, TXT chapters, PDF pages) | 2 |
| Font size, line spacing, margins | implemented (EPUB, TXT) | 2 |
| Light / dark / sepia themes | implemented (PDF night mode inverts colours like Moon+ Reader; menus follow the theme) | 2, 5 |
| Paged and scroll modes | partial (EPUB and PDF both; TXT scroll only) | 2, 5 |
| Full-screen reading, tap middle for menu, tap edges to turn pages | implemented | 5 |
| Status line (battery, time, chapter, page) | implemented | 5 |
| Page slider, day/night pop-up, rotation lock, auto scroll | implemented | 5 |
| PDF outline (bookmarks) as contents and chapter names | implemented | 5 |
| PDF text reflow (with long-press to read aloud) | implemented | 5 |
| PDF options: margin colour, render quality, page gap, trim margins, thumbnails | implemented | 5 |
| Pinch zoom on PDF pages | planned | - |
| PDF annotations | deferred (phase 2) | - |
| Save reading position | implemented | 2 |
| Long-press word to read aloud | implemented (TXT, EPUB; PDF reads from the page) | 3 |
| Voice list, preview, multiple voices | implemented | 3 |
| "Read from here" menu option, copy kept | implemented | 3 |
| Word / sentence highlighting while reading | implemented (word in TXT when the engine reports timing; sentence in EPUB) | 3 |
| Separate listening position | implemented | 3 |
| Speed and pitch | implemented | 3 |
| Background listening, lock-screen controls | implemented | 3 |
| Notification opens the book being read | implemented | 4 |
| Signed release build for sharing | implemented (keystore.properties, see release-signing.md) | 4 |
| Sleep timer | implemented | 3 |
| Read PDFs aloud (text-based) | implemented (play reads from the page; long-press a word on the page or in text reflow) | 3, 5 |
| Pronunciation replacements | deferred (phase 2) | - |
| Bookmarks, highlights, notes | deferred (phase 2) | - |
| Collections, favourites, stats | deferred (phase 2) | - |
| Font family choice | deferred (phase 2) | - |
| OCR for scanned PDFs | deferred (phase 3) | - |
| Password-protected PDFs | unsupported (clear message) | - |
| DRM-protected books | unsupported (clear message) | - |
