package com.petgroom.autosms

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ClientEditActivity : AppCompatActivity() {
    private lateinit var db: DbHelper
    private var id: Long = 0L
    private var existing: DbHelper.Client? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_edit)
        db = DbHelper(this)
        id = intent.getLongExtra("id", 0L)

        val name = findViewById<EditText>(R.id.name)
        val phone = findViewById<EditText>(R.id.phone)
        val pet = findViewById<EditText>(R.id.pet)
        val type = findViewById<EditText>(R.id.type)
        val next = findViewById<EditText>(R.id.next)
        val cycle = findViewById<EditText>(R.id.cycle)
        val notes = findViewById<EditText>(R.id.notes)
        val muted = findViewById<CheckBox>(R.id.muted)
        val temp = findViewById<EditText>(R.id.temp)
        val debt = findViewById<EditText>(R.id.debt)

        if (id != 0L) {
            existing = db.getClient(id)
            existing?.let { c ->
                findViewById<android.widget.TextView>(R.id.screenTitle).text = "ویرایش پرونده"
                name.setText(c.ownerName); phone.setText(c.phone); pet.setText(c.petName)
                type.setText(c.petType); next.setText(c.nextVisit); cycle.setText(c.cycleDays.toString())
                notes.setText(c.notes); muted.isChecked = c.muted; temp.setText(c.temperament); debt.setText(c.debt.toString())
            }
        }

        findViewById<Button>(R.id.save).setOnClickListener {
            val owner = name.text.toString().trim()
            val p = PhoneUtil.normalize(phone.text.toString())
            if (owner.isBlank()) { Toast.makeText(this, "نام صاحب را وارد کنید", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            if (p.isBlank()) { Toast.makeText(this, "شماره موبایل معتبر لازم است", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val old = existing
            db.upsert(DbHelper.Client(
                id = id, ownerName = owner, phone = p, petName = pet.text.toString().trim(),
                petType = type.text.toString().trim().ifBlank { "سگ" }, breed = old?.breed ?: "",
                category = old?.category ?: "عادی", lastVisit = old?.lastVisit ?: "",
                nextVisit = next.text.toString().trim(), cycleDays = cycle.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 30,
                notes = notes.text.toString().trim(), muted = muted.isChecked, temperament = temp.text.toString().trim(),
                debt = debt.text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0, service = old?.service ?: ""
            ))
            Toast.makeText(this, "پرونده ذخیره شد", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<Button>(R.id.done).setOnClickListener {
            if (id == 0L) { Toast.makeText(this, "اول پرونده را ذخیره کنید", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val cyc = cycle.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 30
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, cyc) }
            val nxt = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
            db.markDone(id, today, nxt)
            Toast.makeText(this, "کار ثبت شد؛ نوبت بعد: $nxt", Toast.LENGTH_LONG).show()
            finish()
        }

        findViewById<Button>(R.id.delete).setOnClickListener {
            if (id == 0L) { finish(); return@setOnClickListener }
            AlertDialog.Builder(this).setTitle("حذف پرونده؟").setMessage("این مشتری از حافظه گوشی حذف می‌شود.")
                .setNegativeButton("انصراف", null).setPositiveButton("حذف") { _, _ -> db.deleteClient(id); finish() }.show()
        }
    }
}
