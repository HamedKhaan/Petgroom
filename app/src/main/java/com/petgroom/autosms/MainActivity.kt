package com.petgroom.autosms

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** خانه اپ — کاملاً آفلاین */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btnClients).setOnClickListener {
            startActivity(Intent(this, ClientsActivity::class.java))
        }
        findViewById<Button>(R.id.btnOverdue).setOnClickListener {
            startActivity(Intent(this, OverdueActivity::class.java))
        }
        findViewById<Button>(R.id.btnSend).setOnClickListener {
            startActivity(Intent(this, SendActivity::class.java))
        }
        findViewById<Button>(R.id.btnContacts).setOnClickListener {
            startActivity(Intent(this, ContactsActivity::class.java).putExtra("saveToDb", true))
        }
    }

    override fun onResume() {
        super.onResume()
        val db = DbHelper(this)
        val n = db.allClients().size
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val over = db.overdue(today).size
        findViewById<TextView>(R.id.status).text =
            "آفلاین روی همین گوشی\nمشتریان: $n  |  موعد گذشته: $over"
    }
}
