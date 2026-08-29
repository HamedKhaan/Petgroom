package com.petgroom.autosms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/** ارسال پیامک از سیم‌کارت — بدون اینترنت */
class SendActivity : AppCompatActivity() {
    private lateinit var db: DbHelper
    private var clients = listOf<DbHelper.Client>()

    private val permission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) doSend()
        else Toast.makeText(this, "اجازه پیامک لازم است", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)
        db = DbHelper(this)
        clients = db.allClients().filter { !it.muted }
        findViewById<TextView>(R.id.info).text =
            "${clients.size} مشتری قابل ارسال (خاموش‌ها حذف شدند)"
        val templates = db.templates()
        val spin = findViewById<Spinner>(R.id.template)
        spin.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            templates.map { it.second }
        )
        findViewById<CheckBox>(R.id.onlyOverdue).setOnCheckedChangeListener { _, _ -> }
        findViewById<Button>(R.id.btnSend).setOnClickListener { ask() }
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            startService(Intent(this, SendService::class.java).setAction(SendService.STOP))
        }
        if (templates.isNotEmpty()) {
            findViewById<EditText>(R.id.body).setText(db.templateBody(templates[0].first))
        }
        spin.setSelection(0)
    }

    private fun ask() {
        val need = mutableListOf(Manifest.permission.SEND_SMS)
        if (Build.VERSION.SDK_INT >= 33) need += Manifest.permission.POST_NOTIFICATIONS
        val missing = need.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) doSend() else permission.launch(missing.toTypedArray())
    }

    private fun doSend() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        var list = clients
        if (findViewById<CheckBox>(R.id.onlyOverdue).isChecked) {
            list = db.overdue(today).filter { !it.muted }
        }
        if (list.isEmpty()) {
            Toast.makeText(this, "کسی برای ارسال نیست", Toast.LENGTH_LONG).show()
            return
        }
        var body = findViewById<EditText>(R.id.body).text.toString().trim()
        if (body.isEmpty()) {
            Toast.makeText(this, "متن خالی است", Toast.LENGTH_SHORT).show()
            return
        }
        val wait = (findViewById<EditText>(R.id.delay).text.toString().toLongOrNull() ?: 4L)
            .coerceIn(2L, 30L)
        // پر کردن تکی برای هر نفر در سرویس با name/pet از extras
        val phones = ArrayList(list.map { it.phone })
        val names = ArrayList(list.map { it.ownerName })
        val pets = ArrayList(list.map { it.petName })
        val types = ArrayList(list.map { it.petType })
        val i = Intent(this, SendService::class.java).apply {
            action = SendService.START
            putStringArrayListExtra("phones", phones)
            putStringArrayListExtra("names", names)
            putStringArrayListExtra("pets", pets)
            putStringArrayListExtra("types", types)
            putExtra("body", body)
            putExtra("delay", wait * 1000L)
        }
        ContextCompat.startForegroundService(this, i)
        Toast.makeText(this, "ارسال آفلاین شروع شد (${list.size} نفر)", Toast.LENGTH_LONG).show()
    }
}
