package com.petgroom.autosms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
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

class SendActivity : AppCompatActivity() {
    private lateinit var db: DbHelper
    private var clients = listOf<DbHelper.Client>()
    private var templates = emptyList<Pair<Long, String>>()

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) doSend() else Toast.makeText(this, "برای ارسال پیامک اجازه SMS لازم است", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)
        db = DbHelper(this)
        clients = db.allClients().filter { !it.muted }
        val prefs = getSharedPreferences("petgroom_settings", MODE_PRIVATE)
        findViewById<EditText>(R.id.delay).setText(prefs.getLong("delay_seconds", 4L).toString())
        findViewById<TextView>(R.id.info).text = "${clients.size} مشتری آماده ارسال هستند  •  مشتری‌های خاموش حذف شده‌اند"

        templates = db.templates()
        val spin = findViewById<Spinner>(R.id.template)
        spin.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, templates.map { it.second })
        spin.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in templates.indices) findViewById<EditText>(R.id.body).setText(db.templateBody(templates[position].first))
            }
        }
        findViewById<Button>(R.id.btnSend).setOnClickListener { ask() }
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            startService(Intent(this, SendService::class.java).setAction(SendService.STOP))
        }
    }

    private fun ask() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) doSend()
        else permission.launch(Manifest.permission.SEND_SMS)
    }

    private fun doSend() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val list = if (findViewById<CheckBox>(R.id.onlyOverdue).isChecked) db.overdue(today).filter { !it.muted } else clients
        if (list.isEmpty()) { Toast.makeText(this, "مشتری مناسبی برای ارسال وجود ندارد", Toast.LENGTH_LONG).show(); return }
        var body = findViewById<EditText>(R.id.body).text.toString().trim()
        if (body.isEmpty()) { Toast.makeText(this, "متن پیام خالی است", Toast.LENGTH_SHORT).show(); return }
        val signature = getSharedPreferences("petgroom_settings", MODE_PRIVATE).getString("signature", "")?.trim().orEmpty()
        if (signature.isNotBlank() && !body.contains(signature)) body += "\n$signature"
        val wait = (findViewById<EditText>(R.id.delay).text.toString().toLongOrNull() ?: 4L).coerceIn(2L, 30L)
        val intent = Intent(this, SendService::class.java).apply {
            action = SendService.START
            putStringArrayListExtra("phones", ArrayList(list.map { it.phone }))
            putStringArrayListExtra("names", ArrayList(list.map { it.ownerName }))
            putStringArrayListExtra("pets", ArrayList(list.map { it.petName }))
            putStringArrayListExtra("types", ArrayList(list.map { it.petType }))
            putExtra("body", body)
            putExtra("delay", wait * 1000L)
        }
        ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this, "ارسال برای ${list.size} نفر شروع شد", Toast.LENGTH_LONG).show()
    }
}
