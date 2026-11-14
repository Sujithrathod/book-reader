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

    /** The voice that will be used: [chosen] if it is installed, else the default pick. */
    fun effective(voices: List<VoiceInfo>, chosen: String?, bookLanguage: String?, deviceLocale: Locale): VoiceInfo? =
        voices.firstOrNull { it.name == chosen && it.usable } ?: pickDefault(voices, bookLanguage, deviceLocale)

    /**
     * Language groups for the picker. Languages the reader is likely to want come first
     * (the chosen voice's, the book's, the phone's); the rest follow in A to Z order.
     * Returns the groups and how many of them are the "likely" ones.
     */
    fun ordered(voices: List<VoiceInfo>, chosen: String?, bookLanguage: String?, deviceLocale: Locale): Pair<List<Pair<String, List<VoiceInfo>>>, Int> {
        fun lang(l: String) = l.substringBefore('-').substringBefore('_').lowercase()
        val wanted = setOfNotNull(
            voices.firstOrNull { it.name == chosen }?.locale?.language,
            bookLanguage?.takeIf { it.isNotBlank() }?.let(::lang),
            deviceLocale.language,
        )
        val (likely, rest) = grouped(voices).partition { (_, list) -> list.any { it.locale.language in wanted } }
        // The chosen voice's own group first, then the phone's country, then other countries.
        val sorted = likely.sortedBy { (_, list) ->
            when {
                list.any { it.name == chosen } -> 0
                list.any { it.locale == deviceLocale } -> 1
                else -> 2
            }
        }
        return (sorted + rest) to sorted.size
    }

    /** Short facts about a voice for the picker, e.g. "Offline · High quality". */
    fun tags(voice: VoiceInfo): List<String> = listOfNotNull(
        when {
            voice.notInstalled -> "Not downloaded"
            voice.needsInternet -> "Needs internet"
            else -> "Offline"
        },
        if (voice.quality >= 400 && !voice.notInstalled) "High quality" else null,
    )

    /** How the picker names a voice, e.g. "English (United States) · Voice 2". */
    fun title(voice: VoiceInfo, voices: List<VoiceInfo>): String {
        val group = grouped(voices).firstOrNull { it.first == voice.languageLabel }?.second.orEmpty()
        val n = group.indexOfFirst { it.name == voice.name } + 1
        return if (n > 0) "${voice.languageLabel} · Voice $n" else voice.languageLabel
    }

    /** Voices grouped by language for the picker, offline voices first. */
    fun grouped(voices: List<VoiceInfo>): List<Pair<String, List<VoiceInfo>>> =
        voices.groupBy { it.languageLabel }
            .toSortedMap()
            .map { (label, list) -> label to list.sortedWith(compareBy<VoiceInfo> { it.notInstalled }.thenBy { it.needsInternet }.thenByDescending { it.quality }.thenBy { it.name }) }
}
