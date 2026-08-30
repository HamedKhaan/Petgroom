package com.petgroom.autosms

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btnClients).setOnClickListener { startActivity(Intent(this, ClientsActivity::class.java)) }
        findViewById<Button>(R.id.btnOverdue).setOnClickListener { startActivity(Intent(this, OverdueActivity::class.java)) }
        findViewById<Button>(R.id.btnSend).setOnClickListener { startActivity(Intent(this, SendActivity::class.java)) }
        findViewById<Button>(R.id.btnContacts).setOnClickListener { startActivity(Intent(this, ContactsActivity::class.java).putExtra("saveToDb", true)) }
        findViewById<Button>(R.id.btnSettings).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        val db = DbHelper(this)
        val clients = db.allClients()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val overdue = db.overdue(today)
        findViewById<TextView>(R.id.salonTitle).text = getSharedPreferences("petgroom_settings", MODE_PRIVATE).getString("salon_name", "پت‌گروم")
        findViewById<TextView>(R.id.countClients).text = clients.size.toString()
        findViewById<TextView>(R.id.countOverdue).text = overdue.size.toString()
        findViewById<TextView>(R.id.status).text = "●  آفلاین و آماده  •  ${clients.size} مشتری ثبت شده  •  ${overdue.size} مورد نیازمند پیگیری"
    }
}
