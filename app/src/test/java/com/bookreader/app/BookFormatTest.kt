package com.bookreader.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookFormatTest {
    @Test fun detectsByMimeType() {
        assertEquals(BookFormat.EPUB, BookFormat.detect("x.bin", "application/epub+zip"))
        assertEquals(BookFormat.PDF, BookFormat.detect(null, "application/pdf"))
        assertEquals(BookFormat.TXT, BookFormat.detect(null, "text/plain; charset=utf-8"))
    }

    @Test fun fallsBackToExtensionWhenMimeIsGeneric() {
        assertEquals(BookFormat.EPUB, BookFormat.detect("Moby Dick.EPUB", "application/octet-stream"))
        assertEquals(BookFormat.PDF, BookFormat.detect("paper.pdf", null))
        assertEquals(BookFormat.TXT, BookFormat.detect("notes.txt", ""))
    }

    @Test fun rejectsUnknownFiles() {
        assertNull(BookFormat.detect("song.mp3", "audio/mpeg"))
        assertNull(BookFormat.detect("README", null))
        assertNull(BookFormat.detect(null, null))
    }
}
