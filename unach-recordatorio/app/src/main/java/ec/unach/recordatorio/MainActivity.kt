package ec.unach.recordatorio

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val d = resources.displayMetrics.density
        val pad = (16 * d).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        root.addView(TextView(this).apply {
            text = "Alarmas para registrar en Mi UNACH"
            textSize = 20f
        })

        Alarms.NAMES.forEachIndexed { i, name ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, pad, 0, 0)
            }
            val sw = Switch(this).apply {
                text = name
                isChecked = Alarms.enabled(this@MainActivity, i)
                setOnCheckedChangeListener { _, on ->
                    Alarms.setEnabled(this@MainActivity, i, on)
                    Alarms.schedule(this@MainActivity, i)
                }
            }
            val time = Button(this)
            fun refresh() { time.text = Alarms.format(Alarms.minutes(this, i)) }
            refresh()
            time.setOnClickListener {
                val m = Alarms.minutes(this, i)
                TimePickerDialog(this, { _, h, min ->
                    Alarms.setMinutes(this, i, h * 60 + min)
                    Alarms.schedule(this, i)
                    refresh()
                }, m / 60, m % 60, true).show()
            }
            row.addView(sw, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(time)
            root.addView(row)
        }

        root.addView(CheckBox(this).apply {
            text = "Solo de lunes a viernes"
            isChecked = Alarms.weekdaysOnly(this@MainActivity)
            setOnCheckedChangeListener { _, on ->
                Alarms.setWeekdaysOnly(this@MainActivity, on)
                Alarms.scheduleAll(this@MainActivity)
            }
        })
        root.addView(Button(this).apply {
            text = "Probar alarma (suena en 10 s)"
            setOnClickListener {
                Alarms.test(this@MainActivity)
                Toast.makeText(this@MainActivity, "Sonará en 10 segundos", Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(Button(this).apply {
            text = "Abrir Mi UNACH"
            setOnClickListener {
                if (!Alarms.openMiUnach(this@MainActivity)) {
                    Toast.makeText(this@MainActivity, "Mi UNACH no está instalada", Toast.LENGTH_LONG).show()
                }
            }
        })

        setContentView(ScrollView(this).apply { addView(root) })
        Alarms.scheduleAll(this)
    }
}
