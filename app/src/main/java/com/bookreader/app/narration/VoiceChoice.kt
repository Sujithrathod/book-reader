package com.bookreader.app.narration

import java.util.Locale

/** What the app knows about one installed voice. */
data class VoiceInfo(
    val name: String,
    val locale: Locale,
    val needsInternet: Boolean,
    val notInstalled: Boolean,
    val quality: Int,
) {
    val usable get() = !notInstalled
    val languageLabel: String get() = locale.getDisplayName(Locale.getDefault()).ifBlank { locale.toLanguageTag() }
}

object VoiceChoice {
    /**
     * Picks a sensible default: an installed, offline voice in the book's language,
     * then the phone's language, then any installed offline voice, then any installed voice.
     */
    fun pickDefault(voices: List<VoiceInfo>, bookLanguage: String?, deviceLocale: Locale): VoiceInfo? {
        val usable = voices.filter { it.usable }
        val offline = usable.filter { !it.needsInternet }
        fun best(list: List<VoiceInfo>) = list.maxByOrNull { it.quality }
        fun lang(l: String) = l.substringBefore('-').substringBefore('_').lowercase()
        bookLanguage?.takeIf { it.isNotBlank() }?.let { bl ->
            best(offline.filter { it.locale.language == lang(bl) })?.let { return it }
        }
        best(offline.filter { it.locale == deviceLocale })?.let { return it }
        best(offline.filter { it.locale.language == deviceLocale.language })?.let { return it }
        return best(offline) ?: best(usable)
    }

    /** Voices grouped by language for the picker, offline voices first. */
    fun grouped(voices: List<VoiceInfo>): List<Pair<String, List<VoiceInfo>>> =
        voices.groupBy { it.languageLabel }
            .toSortedMap()
            .map { (label, list) -> label to list.sortedWith(compareBy<VoiceInfo> { it.notInstalled }.thenBy { it.needsInternet }.thenByDescending { it.quality }.thenBy { it.name }) }
}
