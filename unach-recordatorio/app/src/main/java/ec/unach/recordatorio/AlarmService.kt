package ec.unach.recordatorio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator

class AlarmService : Service() {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val idx = intent?.getIntExtra("idx", -1) ?: -1
        val notification = buildNotification(idx)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, notification)
        }
        startRinging()
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ stopSelf() }, RING_MILLIS)
        return START_NOT_STICKY
    }

    private fun startRinging() {
        stopRinging()
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        ringtone = RingtoneManager.getRingtone(
            this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        )?.apply {
            audioAttributes = attrs
            isLooping = true
            play()
        }
        vibrator = getSystemService(Vibrator::class.java)?.also {
            it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0), attrs)
        }
    }

    private fun stopRinging() {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun buildNotification(idx: Int): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Alarma de registro", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null) // el sonido lo reproduce el servicio
                enableVibration(false)
            }
        )
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val ring = PendingIntent.getActivity(this, 30, Intent(this, RingActivity::class.java), flags)
        val snooze = Alarms.receiverIntent(this, Alarms.ACTION_SNOOZE, idx, 11)
        val stop = Alarms.receiverIntent(this, Alarms.ACTION_STOP, idx, 12)
        val open = PendingIntent.getActivity(
            this, 31, Intent(this, RingActivity::class.java).putExtra("open", true), flags
        )
        fun action(title: String, pi: PendingIntent) = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_menu_send), title, pi
        ).build()

        val text = if (idx in Alarms.NAMES.indices) Alarms.NAMES[idx] else "Recordatorio"
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Registra tu asistencia en Mi UNACH")
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(ring)
            .setFullScreenIntent(ring, true)
            .addAction(action("Abrir Mi UNACH", open))
            .addAction(action("Posponer 5 min", snooze))
            .addAction(action("Ya registré", stop))
            .build()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopRinging()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL = "alarma"
        const val NOTIF_ID = 1
        const val RING_MILLIS = 3 * 60_000L
    }
}
