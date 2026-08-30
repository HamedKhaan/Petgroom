package com.petgroom.autosms

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        setContentView(R.layout.activity_overdue)
        val db = DbHelper(this)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val rows = db.overdue(today)
        findViewById<TextView>(R.id.hint).text = "${rows.size} مشتری نیازمند پیگیری"
        findViewById<TextView>(R.id.empty).visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        findViewById<RecyclerView>(R.id.list).apply {
            layoutManager = LinearLayoutManager(this@OverdueActivity)
            adapter = Adapter(rows) { id -> startActivity(Intent(this@OverdueActivity, ClientEditActivity::class.java).putExtra("id", id)) }
        }
    }

    class Adapter(private val data: List<DbHelper.Client>, private val onClick: (Long) -> Unit) : RecyclerView.Adapter<Adapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.title)
            val sub: TextView = v.findViewById(R.id.sub)
        }
        override fun onCreateViewHolder(p: ViewGroup, type: Int): VH =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_overdue, p, false))
        override fun getItemCount() = data.size
        override fun onBindViewHolder(h: VH, i: Int) {
            val c = data[i]
            h.title.text = if (c.petName.isBlank()) c.ownerName else "${c.ownerName}  •  ${c.petName}"
            h.sub.text = "نوبت ${c.nextVisit}  •  ${c.phone}"
            h.itemView.setOnClickListener { onClick(c.id) }
        }
    }
}
