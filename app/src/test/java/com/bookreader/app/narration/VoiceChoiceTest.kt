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

    @Test fun chosenVoiceIsUsedAsSoonAsItIsPicked() {
        val voices = listOf(v("a", Locale.US, q = 500), v("b", Locale.US), v("c", Locale.UK))
        assertEquals("b", VoiceChoice.effective(voices, "b", null, Locale.US)?.name)
        assertEquals("c", VoiceChoice.effective(voices, "c", null, Locale.US)?.name)
        // Nothing chosen, or the choice is gone: the default pick.
        assertEquals("a", VoiceChoice.effective(voices, null, null, Locale.US)?.name)
        assertEquals("a", VoiceChoice.effective(voices, "removed", null, Locale.US)?.name)
    }

    @Test fun chosenVoiceCountsOnlyOnceItIsDownloaded() {
        val before = listOf(v("a", Locale.US), v("new", Locale.US, missing = true))
        assertEquals("a", VoiceChoice.effective(before, "new", null, Locale.US)?.name)
        val after = listOf(v("a", Locale.US), v("new", Locale.US))
        assertEquals("new", VoiceChoice.effective(after, "new", null, Locale.US)?.name)
    }

    @Test fun likelyLanguagesComeFirstInThePicker() {
        val voices = listOf(v("de", Locale.GERMANY), v("en", Locale.US), v("fr", Locale.FRANCE), v("hi", Locale("hi", "IN")))
        val (groups, likely) = VoiceChoice.ordered(voices, "hi", "fr", Locale.US)
        assertEquals(3, likely)
        assertEquals(setOf("en", "fr", "hi"), groups.take(likely).flatMap { it.second }.map { it.name }.toSet())
        assertEquals("de", groups.last().second.single().name)
        assertEquals("hi", groups.first().second.single().name) // the chosen voice's language leads
    }

    @Test fun voiceTagsSayWhatAVoiceNeeds() {
        assertEquals(listOf("Offline", "High quality"), VoiceChoice.tags(v("a", Locale.US, q = 400)))
        assertEquals(listOf("Needs internet"), VoiceChoice.tags(v("a", Locale.US, net = true)))
        assertEquals(listOf("Not downloaded"), VoiceChoice.tags(v("a", Locale.US, missing = true)))
    }
}
