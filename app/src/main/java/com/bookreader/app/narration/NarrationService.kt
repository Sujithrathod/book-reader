package com.bookreader.app.narration

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.bookreader.app.MainActivity
import com.bookreader.app.R
import com.bookreader.app.app
import com.bookreader.app.reader.ReaderActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps narration running when the screen is off or the app is in the background,
 * shows the playback notification, and connects lock-screen, headset and
 * Bluetooth buttons through a media session. Pauses when headphones are unplugged.
 * Stops itself when narration stops.
 */
class NarrationService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var session: MediaSession
    private var wakeLock: PowerManager.WakeLock? = null
    private var noisyRegistered = false
    private val narrator get() = app.narrator

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) narrator.pause()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Reading aloud", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Controls for the book being read aloud"
                setShowBadge(false)
            }
        )
        session = MediaSession(this, "BookReader").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = narrator.resume()
                override fun onPause() = narrator.pause()
                override fun onStop() = narrator.stop()
                override fun onSkipToNext() = narrator.nextSentence()
                override fun onSkipToPrevious() = narrator.previousSentence()
            })
            isActive = true
        }
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BookReader:narration")
            .apply { setReferenceCounted(false) }

        // Show the notification right away; Android requires it within a few seconds.
        startInForeground(buildNotification(narrator.state.value))

        scope.launch {
            narrator.state
                .map { Triple(it.playState, it.bookTitle, it.sleepRemainingMs?.let { ms -> ms / 60_000 }) }
                .distinctUntilChanged()
                .collect { update(narrator.state.value) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground(buildNotification(narrator.state.value))
        when (intent?.action) {
            ACTION_TOGGLE -> narrator.togglePlayPause()
            ACTION_NEXT -> narrator.nextSentence()
            ACTION_PREVIOUS -> narrator.previousSentence()
            ACTION_STOP -> narrator.stop()
        }
        return START_NOT_STICKY
    }

    private fun startInForeground(n: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, n)
        }
    }

    private fun update(s: NarrationState) {
        if (s.playState == PlayState.IDLE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        val playing = s.playState == PlayState.PLAYING || s.playState == PlayState.LOADING
        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, s.bookTitle)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, s.bookAuthor ?: "")
                .build()
        )
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_STOP or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS
                )
                .setState(if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build()
        )
        if (playing) {
            wakeLock?.acquire(60 * 60 * 1000L)
            if (!noisyRegistered) {
                val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(noisyReceiver, filter, RECEIVER_NOT_EXPORTED)
                } else {
                    registerReceiver(noisyReceiver, filter)
                }
                noisyRegistered = true
            }
        } else {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        }
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(s))
    }

    private fun action(icon: Int, title: String, act: String): Notification.Action {
        val pi = PendingIntent.getService(
            this, act.hashCode(), Intent(this, NarrationService::class.java).setAction(act),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Action.Builder(Icon.createWithResource(this, icon), title, pi).build()
    }

    private fun buildNotification(s: NarrationState): Notification {
        val playing = s.playState == PlayState.PLAYING || s.playState == PlayState.LOADING
        // Tapping the notification opens the book being read (with the library behind it).
        val library = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val bookId = s.bookId
        val open = if (bookId != null && app.openBooks.get(bookId) != null) {
            PendingIntent.getActivities(
                this, 1, arrayOf(library, ReaderActivity.intent(this, bookId)),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        } else {
            PendingIntent.getActivity(this, 0, library, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val timer = s.sleepRemainingMs?.let { " · sleep in ${(it / 60_000) + 1} min" } ?: ""
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_reading)
            .setContentTitle(s.bookTitle.ifBlank { "Reading aloud" })
            .setContentText((if (playing) "Reading aloud" else "Paused") + timer)
            .setContentIntent(open)
            .setOngoing(playing)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(action(android.R.drawable.ic_media_previous, "Previous sentence", ACTION_PREVIOUS))
            .addAction(
                if (playing) action(android.R.drawable.ic_media_pause, "Pause", ACTION_TOGGLE)
                else action(android.R.drawable.ic_media_play, "Play", ACTION_TOGGLE)
            )
            .addAction(action(android.R.drawable.ic_media_next, "Next sentence", ACTION_NEXT))
            .addAction(action(android.R.drawable.ic_menu_close_clear_cancel, "Stop", ACTION_STOP))
            .setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0, 1, 2))
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        if (noisyRegistered) unregisterReceiver(noisyReceiver)
        if (wakeLock?.isHeld == true) wakeLock?.release()
        session.release()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "narration"
        private const val NOTIFICATION_ID = 42
        private const val ACTION_TOGGLE = "com.bookreader.app.TOGGLE"
        private const val ACTION_NEXT = "com.bookreader.app.NEXT"
        private const val ACTION_PREVIOUS = "com.bookreader.app.PREVIOUS"
        private const val ACTION_STOP = "com.bookreader.app.STOP"
    }
}
