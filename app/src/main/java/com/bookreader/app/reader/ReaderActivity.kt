package com.bookreader.app.reader

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bookreader.app.BookFormat
import com.bookreader.app.app
import com.bookreader.app.data.ReaderSettings
import com.bookreader.app.data.DarkPictures
import com.bookreader.app.data.ReadingTheme
import com.bookreader.app.data.ScreenOrientation
import com.bookreader.app.data.bookFormat
import com.bookreader.app.narration.EpubSource
import com.bookreader.app.narration.NarrationSource
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl

/** Shows one book. EPUB uses Readium's navigator; PDF and TXT use Compose screens. */
class ReaderActivity : AppCompatActivity() {

    private var session: ReaderSession? = null
    private var askedForNotifications = false

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* narration works either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        val bookId = intent.getLongExtra(EXTRA_BOOK_ID, -1L)
        val session = app.openBooks.get(bookId)
        this.session = session

        // Readium's fragment factory must be set before super.onCreate so the
        // reading view can be restored after rotation.
        val publication = session?.publication
        if (session != null && publication != null) {
            val initialLocator = session.lastLocation?.let {
                runCatching { Locator.fromJSON(JSONObject(it)) }.getOrNull()
            }
            supportFragmentManager.fragmentFactory = EpubNavigatorFactory(publication).createFragmentFactory(
                initialLocator = initialLocator,
                initialPreferences = session.initialSettings.toEpubPreferences(),
                listener = object : EpubNavigatorFragment.Listener {
                    override fun onExternalLinkActivated(url: AbsoluteUrl) {
                        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url.toString()))) }
                    }
                },
                // The reader screen places the page between the status bar and the status line itself.
                configuration = EpubNavigatorFragment.Configuration(
                    selectionActionModeCallback = selectionCallback,
                    shouldApplyInsetsPadding = false,
                ),
            )
        }
        // Without a session the saved reading view can't be rebuilt, so don't restore it.
        super.onCreate(if (session == null) null else savedInstanceState)

        if (session == null) {
            // The app was closed in the background; the book needs to be opened again from the library.
            Toast.makeText(this, "Please open the book again from your library.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        enableEdgeToEdge()
        lifecycleScope.launch { app.database.books().markOpened(session.book.id, System.currentTimeMillis()) }
        lifecycleScope.launch {
            app.settings.settings.map { it.orientation to it.keepScreenOn }.distinctUntilChanged().collect { (orientation, keepOn) ->
                applyScreenSettings(orientation, keepOn)
            }
        }

        setContent {
            ReaderScreen(
                session = session,
                settingsFlow = app.settings.settings,
                onSettingsChange = { lifecycleScope.launch { app.settings.update(it) } },
                onSavePosition = { location, progress ->
                    session.lastLocation = location
                    lifecycleScope.launch { app.database.books().savePosition(session.book.id, location, progress) }
                },
                onStartNarration = { source -> startNarration(session, source) },
                onBack = { finish() },
            )
        }
    }

    private fun applyScreenSettings(orientation: ScreenOrientation, keepScreenOn: Boolean) {
        val wanted = when (orientation) {
            ScreenOrientation.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        if (requestedOrientation != wanted) requestedOrientation = wanted
        if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /** Starts reading [source] aloud with the current voice settings. */
    private fun startNarration(session: ReaderSession, source: NarrationSource) {
        maybeAskForNotifications()
        val narrator = app.narrator
        narrator.applySettings(session.currentSettings.voice)
        narrator.start(session.book.id, session.book.title, session.book.author, session.language, source)
    }

    /** Android 13+ needs permission to show the playback notification. Asked once, at first use. */
    private fun maybeAskForNotifications() {
        if (askedForNotifications || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        askedForNotifications = true
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ---------- EPUB selection menu ----------

    private fun navigator(): EpubNavigatorFragment? =
        supportFragmentManager.fragments.filterIsInstance<EpubNavigatorFragment>().firstOrNull()

    /** Reads aloud from the selected word in the EPUB. */
    private suspend fun readFromSelection() {
        val session = session ?: return
        val publication = session.publication ?: return
        val nav = navigator() ?: return
        val selection = nav.currentSelection() ?: return
        val text = selection.locator.text
        // Start the search at the first element on screen, which carries a precise position.
        val start = nav.firstVisibleElementLocator()?.takeIf { it.href.toString() == selection.locator.href.toString() }
            ?: selection.locator
        nav.clearSelection()
        startNarration(session, EpubSource(publication, start, text.highlight, text.before))
    }

    private suspend fun copySelection() {
        val nav = navigator() ?: return
        val words = nav.currentSelection()?.locator?.text?.highlight ?: return
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Book text", words))
        nav.clearSelection()
    }

    /**
     * Replaces the text-selection menu in EPUBs. With "long-press reads" on, reading
     * starts as soon as a word is selected; otherwise the menu offers "Read from here" and "Copy".
     */
    private val selectionCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.add(Menu.NONE, MENU_READ, 0, "Read from here")
            menu.add(Menu.NONE, MENU_COPY, 1, "Copy")
            if (session?.currentSettings?.longPressReads == true) {
                lifecycleScope.launch {
                    readFromSelection()
                    mode.finish()
                }
            }
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = when (item.itemId) {
            MENU_READ -> { lifecycleScope.launch { readFromSelection(); mode.finish() }; true }
            MENU_COPY -> { lifecycleScope.launch { copySelection(); mode.finish() }; true }
            else -> false
        }

        override fun onDestroyActionMode(mode: ActionMode) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        val s = session ?: return
        // Keep the book open while it's still being read aloud in the background.
        if (isFinishing && app.narrator.state.value.bookId != s.book.id) app.openBooks.close(s.book.id)
    }

    companion object {
        private const val EXTRA_BOOK_ID = "book_id"
        private const val MENU_READ = 1001
        private const val MENU_COPY = 1002

        fun intent(context: Context, bookId: Long): Intent =
            Intent(context, ReaderActivity::class.java).putExtra(EXTRA_BOOK_ID, bookId)
    }
}

internal fun ReaderSettings.toEpubPreferences() =
    org.readium.r2.navigator.epub.EpubPreferences(
        theme = when (theme) {
            ReadingTheme.LIGHT -> org.readium.r2.navigator.preferences.Theme.LIGHT
            ReadingTheme.DARK -> org.readium.r2.navigator.preferences.Theme.DARK
            ReadingTheme.SEPIA -> org.readium.r2.navigator.preferences.Theme.SEPIA
        },
        fontSize = fontScale,
        lineHeight = lineHeight,
        pageMargins = pageMargins,
        scroll = scroll,
        // Readium applies this only with its dark theme.
        imageFilter = when (darkImages) {
            DarkPictures.DIM -> org.readium.r2.navigator.preferences.ImageFilter.DARKEN
            DarkPictures.INVERT -> org.readium.r2.navigator.preferences.ImageFilter.INVERT
            DarkPictures.ORIGINAL -> null
        },
        // Publisher styles must be off for line height to apply.
        publisherStyles = false,
    )

internal val ReaderSession.format: BookFormat get() = book.bookFormat
