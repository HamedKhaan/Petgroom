package com.petgroom.autosms

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ContactsActivity : AppCompatActivity() {

    private val rows = mutableListOf<Row>()
    private lateinit var adapter: Adapter
    private lateinit var info: TextView
    private var saveToDb = false

    private val askContacts = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        if (ok) loadContacts()
        else {
            Toast.makeText(
                this,
                "اجازه مخاطبین لازم است. در سیستم «فقط این بار» را انتخاب کنید.",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)
        saveToDb = intent.getBooleanExtra("saveToDb", false)
        info = findViewById(R.id.info)
        adapter = Adapter(rows) { refreshCount() }
        findViewById<RecyclerView>(R.id.list).apply {
            layoutManager = LinearLayoutManager(this@ContactsActivity)
            adapter = laura.c@example.net
        }
        findViewById<Button>(R.id.btnAll).setOnClickListener {
            rows.forEach { it.checked = true }; adapter.notifyDataSetChanged(); refreshCount()
        }
        findViewById<Button>(R.id.btnNone).setOnClickListener {
            rows.forEach { it.checked = false }; adapter.notifyDataSetChanged(); refreshCount()
        }
        findViewById<Button>(R.id.btnImport).setOnClickListener { finishWithSelection() }
        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            setResult(Activity.RESULT_CANCELED); finish()
        }
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED -> loadContacts()
            else -> askContacts.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun loadContacts() {
        rows.clear()
        val seen = HashSet<String>()
        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
        )?.use {
            val iName = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val iNum = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val name = if (iName >= 0) it.getString(iName) ?: "" else ""
                val phone = PhoneUtil.normalize(if (iNum >= 0) it.getString(iNum) ?: "" else "")
                if (phone.isBlank() || !seen.add(phone)) continue
                rows += Row(name.ifBlank { "بدون نام" }, phone, false)
            }
        }
        adapter.notifyDataSetChanged()
        info.text = "آفلاین. «فقط این بار» برای مجوز امن‌تر است. هر تعداد را تیک بزنید."
        refreshCount()
    }

    private fun refreshCount() {
        val n = rows.count { it.checked }
        findViewById<Button>(R.id.btnImport).text = "ورود $n مخاطب انتخاب‌شده"
    }

    private fun finishWithSelection() {
        val picked = rows.filter { it.checked }
        if (picked.isEmpty()) {
            Toast.makeText(this, "حداقل یک نفر را تیک بزنید", Toast.LENGTH_SHORT).show()
            return
        }
        if (saveToDb) {
            val db = DbHelper(this)
            var n = 0
            for (r in picked) {
                db.upsert(
                    DbHelper.Client(
                        ownerName = r.name,
                        phone = r.phone,
                        petName = "",
                        petType = "سگ"
                    )
                )
                n++
            }
            Toast.makeText(this, "$n نفر در پایگاه آفلاین گوشی ذخیره شد", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val data = Intent().apply {
            putStringArrayListExtra("names", ArrayList(picked.map { it.name }))
            putStringArrayListExtra("phones", ArrayList(picked.map { it.phone }))
        }
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    data class Row(val name: String, val phone: String, var checked: Boolean)

    class Adapter(
        private val data: List<Row>,
        private val onChange: () -> Unit
    ) : RecyclerView.Adapter<Adapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val box: CheckBox = v.findViewById(R.id.box)
            val title: TextView = v.findViewById(R.id.title)
            val sub: TextView = v.findViewById(R.id.sub)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
            return VH(v)
        }
        override fun getItemCount() = data.size
        override fun onBindViewHolder(h: VH, position: Int) {
            val r = data[position]
            h.title.text = r.name
            h.sub.text = r.phone
            h.box.setOnCheckedChangeListener(null)
            h.box.isChecked = r.checked
            h.box.setOnCheckedChangeListener { _, on -> r.checked = on; onChange() }
            h.itemView.setOnClickListener {
                r.checked = !r.checked; h.box.isChecked = r.checked; onChange()
            }
        }
    }
}
