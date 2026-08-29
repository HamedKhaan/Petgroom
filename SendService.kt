package com.petgroom.autosms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import kotlin.concurrent.thread

class SendService : Service() {

    @Volatile
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            STOP -> {
                running = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            START -> {
                val phones = intent.getStringArrayListExtra("phones") ?: arrayListOf()
                val names = intent.getStringArrayListExtra("names") ?: arrayListOf()
                val pets = intent.getStringArrayListExtra("pets") ?: arrayListOf()
                val types = intent.getStringArrayListExtra("types") ?: arrayListOf()
                val body = intent.getStringExtra("body") ?: ""
                val delay = intent.getLongExtra("delay", 4000L)
                startForeground(1, buildNotif("در حال ارسال ۰ از ${phones.size}"))
                running = true
                thread(start = true, name = "sms-send") {
                    sendAll(phones, names, pets, types, body, delay)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun smsManager(): SmsManager {
        return if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(SmsManager::class.java) ?: @Suppress("DEPRECATION") SmsManager.getDefault()
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }

    private fun sendAll(
        phones: List<String>,
        names: List<String>,
        pets: List<String>,
        types: List<String>,
        body: String,
        delay: Long
    ) {
        val sms = smsManager()
        var ok = 0
        var fail = 0
        for (i in phones.indices) {
            if (!running) break
            val text = body
                .replace("{name}", names.getOrElse(i) { "" })
                .replace("{owner_name}", names.getOrElse(i) { "" })
                .replace("{pet}", pets.getOrElse(i) { "پت" })
                .replace("{pet_name}", pets.getOrElse(i) { "پت" })
                .replace("{pet_type}", types.getOrElse(i) { "" })
                .replace("{type}", types.getOrElse(i) { "" })
            try {
                val parts = sms.divideMessage(text)
                if (parts.size <= 1) {
                    sms.sendTextMessage(phones[i], null, text, null, null)
                } else {
                    sms.sendMultipartTextMessage(phones[i], null, parts, null, null)
                }
                ok++
            } catch (_: Exception) {
                fail++
            }
            push("ارسال شد $ok از ${phones.size}" + if (fail > 0) " — ناموفق $fail" else "")
            if (i < phones.lastIndex && running) {
                try {
                    Thread.sleep(delay)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }
        push("تمام. موفق $ok  ناموفق $fail")
        running = false
        stopSelf()
    }

    private fun buildNotif(text: String): android.app.Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CH, "ارسال پیامک", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val stop = PendingIntent.getService(
            this,
            2,
            Intent(this, SendService::class.java).setAction(STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CH)
            .setContentTitle("پت‌گروم")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setOngoing(true)
            .addAction(0, "توقف", stop)
            .build()
    }

    private fun push(text: String) {
        getSystemService(NotificationManager::class.java).notify(1, buildNotif(text))
    }

    companion object {
        const val START = "start"
        const val STOP = "stop"
        const val CH = "send"
    }
}
