package com.bookreader.app.narration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class VoiceChoiceTest {
    private fun v(name: String, locale: Locale, net: Boolean = false, missing: Boolean = false, q: Int = 300) =
        VoiceInfo(name, locale, needsInternet = net, notInstalled = missing, quality = q)

    @Test fun prefersOfflineVoiceInTheBooksLanguage() {
        val voices = listOf(v("en-net", Locale.US, net = true, q = 500), v("en-local", Locale.US), v("fr-local", Locale.FRANCE))
        assertEquals("fr-local", VoiceChoice.pickDefault(voices, "fr", Locale.US)?.name)
        assertEquals("en-local", VoiceChoice.pickDefault(voices, null, Locale.US)?.name)
    }

    @Test fun neverPicksAVoiceThatIsNotInstalled() {
        val voices = listOf(v("missing", Locale.US, missing = true))
        assertNull(VoiceChoice.pickDefault(voices, "en", Locale.US))
    }

    @Test fun fallsBackToOnlineVoiceWhenNoOfflineVoiceExists() {
        assertEquals("net", VoiceChoice.pickDefault(listOf(v("net", Locale.US, net = true)), "en", Locale.US)?.name)
    }
}
