package ec.unach.recordatorio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class RingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        if (intent.getBooleanExtra("open", false)) {
            openMiUnach()
            return
        }

        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
        }
        root.addView(TextView(this).apply {
            text = "Registra tu asistencia\nen Mi UNACH"
            textSize = 28f
            gravity = Gravity.CENTER
        })
        root.addView(Button(this).apply {
            text = "Abrir Mi UNACH"
            setOnClickListener { openMiUnach() }
        })
        root.addView(Button(this).apply {
            text = "Posponer 5 min"
            setOnClickListener { send(Alarms.ACTION_SNOOZE); finish() }
        })
        root.addView(Button(this).apply {
            text = "Ya registré"
            setOnClickListener { send(Alarms.ACTION_STOP); finish() }
        })
        setContentView(root)
    }

    private fun send(action: String) {
        sendBroadcast(Intent(this, AlarmReceiver::class.java).setAction(action))
    }

    private fun openMiUnach() {
        send(Alarms.ACTION_STOP)
        if (!Alarms.openMiUnach(this)) {
            Toast.makeText(this, "Mi UNACH no está instalada", Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
