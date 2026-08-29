package com.petgroom.autosms

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OverdueActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clients)
        findViewById<Button>(R.id.btnAdd).visibility = View.GONE
        val db = DbHelper(this)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val rows = db.overdue(today)
        findViewById<TextView>(R.id.hint).text = "${rows.size} موعد گذشته"
        findViewById<RecyclerView>(R.id.list).apply {
            layoutManager = LinearLayoutManager(this@OverdueActivity)
            adapter = object : RecyclerView.Adapter<VH>() {
                override fun onCreateViewHolder(p: ViewGroup, v: Int): VH {
                    val view = LayoutInflater.from(p.context).inflate(R.layout.item_contact, p, false)
                    view.findViewById<View>(R.id.box).visibility = View.GONE
                    return VH(view)
                }
                override fun getItemCount() = rows.size
                override fun onBindViewHolder(h: VH, i: Int) {
                    val c = rows[i]
                    h.t.text = "${c.ownerName} — ${c.petName}"
                    h.s.text = "${c.nextVisit} · ${c.phone}"
                    h.itemView.setOnClickListener {
                        startActivity(Intent(this@OverdueActivity, ClientEditActivity::class.java).putExtra("id", c.id))
                    }
                }
            }
        }
    }
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val t: TextView = v.findViewById(R.id.title)
        val s: TextView = v.findViewById(R.id.sub)
    }
}
