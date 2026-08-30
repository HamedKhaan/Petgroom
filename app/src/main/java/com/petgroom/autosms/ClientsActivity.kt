package com.petgroom.autosms

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ClientsActivity : AppCompatActivity() {
    private lateinit var db: DbHelper
    private val allRows = mutableListOf<DbHelper.Client>()
    private val visibleRows = mutableListOf<DbHelper.Client>()
    private lateinit var adapter: Adapter
    private lateinit var hint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clients)
        db = DbHelper(this)
        hint = findViewById(R.id.hint)
        adapter = Adapter(visibleRows) { c ->
            startActivity(Intent(this, ClientEditActivity::class.java).putExtra("id", c.id))
        }
        findViewById<RecyclerView>(R.id.list).apply {
            layoutManager = LinearLayoutManager(this@ClientsActivity)
            adapter = this@ClientsActivity.adapter
        }
        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            startActivity(Intent(this, ClientEditActivity::class.java))
        }
        findViewById<EditText>(R.id.search).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = filter(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    override fun onResume() {
        super.onResume()
        allRows.clear()
        allRows.addAll(db.allClients())
        filter(findViewById<EditText>(R.id.search).text.toString())
    }

    private fun filter(q: String) {
        val query = q.trim()
        visibleRows.clear()
        visibleRows.addAll(if (query.isBlank()) allRows else allRows.filter {
            it.ownerName.contains(query, true) || it.phone.contains(query, true) || it.petName.contains(query, true)
        })
        adapter.notifyDataSetChanged()
        hint.text = if (query.isBlank()) "${allRows.size} مشتری ثبت شده" else "${visibleRows.size} نتیجه از ${allRows.size} مشتری"
    }

    class Adapter(private val data: List<DbHelper.Client>, private val onClick: (DbHelper.Client) -> Unit) :
        RecyclerView.Adapter<Adapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.title)
            val sub: TextView = v.findViewById(R.id.sub)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_client, parent, false))
        override fun getItemCount() = data.size
        override fun onBindViewHolder(holder: VH, position: Int) {
            val c = data[position]
            holder.title.text = if (c.petName.isBlank()) c.ownerName else "${c.ownerName}  •  ${c.petName}"
            holder.sub.text = buildString {
                append(c.phone)
                if (c.petType.isNotBlank()) append("  •  ${c.petType}")
                if (c.nextVisit.isNotBlank()) append("  •  نوبت ${c.nextVisit}")
                if (c.muted) append("  •  پیامک خاموش")
            }
            holder.itemView.setOnClickListener { onClick(c) }
        }
    }
}
