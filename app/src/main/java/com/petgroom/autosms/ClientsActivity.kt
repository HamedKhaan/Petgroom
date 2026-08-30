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

class ClientsActivity : AppCompatActivity() {
    private lateinit var db: DbHelper
    private val rows = mutableListOf<DbHelper.Client>()
    private lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clients)
        db = DbHelper(this)
        adapter = Adapter(rows) { c ->
            startActivity(Intent(this, ClientEditActivity::class.java).putExtra("id", c.id))
        }
        findViewById<RecyclerView>(R.id.list).apply {
            layoutManager = LinearLayoutManager(this@ClientsActivity)
            adapter = this@ClientsActivity.adapter
        }
        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            startActivity(Intent(this, ClientEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        rows.clear()
        rows.addAll(db.allClients())
        adapter.notifyDataSetChanged()
        findViewById<TextView>(R.id.hint).text = "${rows.size} مشتری روی گوشی"
    }

    class Adapter(
        private val data: List<DbHelper.Client>,
        private val onClick: (DbHelper.Client) -> Unit
    ) : RecyclerView.Adapter<Adapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val t: TextView = v.findViewById(R.id.title)
            val s: TextView = v.findViewById(R.id.sub)
        }

        override fun onCreateViewHolder(p: ViewGroup, v: Int): VH {
            val view = LayoutInflater.from(p.context).inflate(R.layout.item_contact, p, false)
            view.findViewById<View>(R.id.box).visibility = View.GONE
            return VH(view)
        }

        override fun getItemCount() = data.size
        override fun onBindViewHolder(h: VH, i: Int) {
            val c = data[i]
            h.t.text = "${c.ownerName} — ${c.petName}"
            h.s.text = "${c.phone} · ${c.petType}" +
                (if (c.nextVisit.isNotBlank()) " · نوبت ${c.nextVisit}" else "") +
                (if (c.muted) " · خاموش" else "")
            h.itemView.setOnClickListener { onClick(c) }
        }
    }
}
