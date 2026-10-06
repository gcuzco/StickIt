package ec.unach.recordatorio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val service = Intent(ctx, AlarmService::class.java)
        when (intent.action) {
            Alarms.ACTION_RING -> {
                val idx = intent.getIntExtra("idx", -1)
                if (idx in Alarms.NAMES.indices) Alarms.schedule(ctx, idx) // programa la siguiente
                ctx.startForegroundService(service.putExtra("idx", idx))
            }
            Alarms.ACTION_SHOW -> ctx.startForegroundService(service.putExtra("idx", -1))
            Alarms.ACTION_SNOOZE -> {
                ctx.stopService(service)
                Alarms.snooze(ctx)
            }
            Alarms.ACTION_STOP -> ctx.stopService(service)
        }
    }
}
