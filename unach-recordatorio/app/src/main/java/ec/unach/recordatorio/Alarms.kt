package ec.unach.recordatorio

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object Alarms {
    const val MI_UNACH = "com.t_rex.mi_unach"

    const val ACTION_RING = "ec.unach.recordatorio.RING"
    const val ACTION_SHOW = "ec.unach.recordatorio.SHOW"
    const val ACTION_SNOOZE = "ec.unach.recordatorio.SNOOZE"
    const val ACTION_STOP = "ec.unach.recordatorio.STOP"

    val NAMES = listOf(
        "Mañana (antes de las 7)",
        "Mediodía (a la 1)",
        "Tarde (antes de las 3)",
        "Noche (después de las 5)",
    )
    private val DEFAULT_MINUTES = listOf(6 * 60 + 55, 13 * 60, 14 * 60 + 55, 17 * 60 + 5)

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences("alarmas", Context.MODE_PRIVATE)

    fun minutes(ctx: Context, i: Int) = prefs(ctx).getInt("m$i", DEFAULT_MINUTES[i])
    fun setMinutes(ctx: Context, i: Int, v: Int) = prefs(ctx).edit().putInt("m$i", v).apply()
    fun enabled(ctx: Context, i: Int) = prefs(ctx).getBoolean("e$i", true)
    fun setEnabled(ctx: Context, i: Int, v: Boolean) = prefs(ctx).edit().putBoolean("e$i", v).apply()
    fun weekdaysOnly(ctx: Context) = prefs(ctx).getBoolean("wd", true)
    fun setWeekdaysOnly(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean("wd", v).apply()

    fun format(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

    fun nextTrigger(ctx: Context, i: Int, now: Long = System.currentTimeMillis()): Long {
        val m = minutes(ctx, i)
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, m / 60)
            set(Calendar.MINUTE, m % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
        if (weekdaysOnly(ctx)) {
            while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            ) cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun receiverIntent(ctx: Context, action: String, idx: Int, code: Int): PendingIntent {
        val intent = Intent(ctx, AlarmReceiver::class.java)
            .setAction(action)
            .putExtra("idx", idx)
        return PendingIntent.getBroadcast(
            ctx, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun alarmManager(ctx: Context) = ctx.getSystemService(AlarmManager::class.java)

    private fun showIntent(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 20, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
    )

    // setAlarmClock despierta el teléfono incluso en modo ahorro y no pide permiso de alarma exacta.
    private fun setClock(ctx: Context, at: Long, pi: PendingIntent) {
        alarmManager(ctx).setAlarmClock(AlarmManager.AlarmClockInfo(at, showIntent(ctx)), pi)
    }

    fun schedule(ctx: Context, i: Int) {
        val pi = receiverIntent(ctx, ACTION_RING, i, i)
        if (enabled(ctx, i)) setClock(ctx, nextTrigger(ctx, i), pi) else alarmManager(ctx).cancel(pi)
    }

    fun scheduleAll(ctx: Context) = NAMES.indices.forEach { schedule(ctx, it) }

    fun snooze(ctx: Context, minutes: Int = 5) =
        setClock(ctx, System.currentTimeMillis() + minutes * 60_000L, receiverIntent(ctx, ACTION_SHOW, -1, 10))

    fun test(ctx: Context) =
        setClock(ctx, System.currentTimeMillis() + 10_000L, receiverIntent(ctx, ACTION_SHOW, -1, 13))

    fun openMiUnach(ctx: Context): Boolean {
        val intent = ctx.packageManager.getLaunchIntentForPackage(MI_UNACH) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
        return true
    }
}
