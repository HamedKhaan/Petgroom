package com.petgroom.autosms

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
    private var id: Long = 0

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
            db.getClient(id)?.let { c ->
                name.setText(c.ownerName)
                phone.setText(c.phone)
                pet.setText(c.petName)
                type.setText(c.petType)
                next.setText(c.nextVisit)
                cycle.setText(c.cycleDays.toString())
                notes.setText(c.notes)
                muted.isChecked = c.muted
                temp.setText(c.temperament)
                debt.setText(c.debt.toString())
            }
        }

        findViewById<Button>(R.id.save).setOnClickListener {
            val p = PhoneUtil.normalize(phone.text.toString())
            if (p.isBlank()) {
                Toast.makeText(this, "شماره لازم است", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            db.upsert(
                DbHelper.Client(
                    id = id,
                    ownerName = name.text.toString().ifBlank { "مشتری" },
                    phone = p,
                    petName = pet.text.toString(),
                    petType = type.text.toString().ifBlank { "سگ" },
                    nextVisit = next.text.toString().trim(),
                    cycleDays = cycle.text.toString().toIntOrNull() ?: 30,
                    notes = notes.text.toString(),
                    muted = muted.isChecked,
                    temperament = temp.text.toString(),
                    debt = debt.text.toString().toIntOrNull() ?: 0
                )
            )
            Toast.makeText(this, "ذخیره شد", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<Button>(R.id.done).setOnClickListener {
            if (id == 0L) {
                Toast.makeText(this, "اول ذخیره کنید", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val cyc = cycle.text.toString().toIntOrNull() ?: 30
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, cyc)
            val nxt = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
            db.markDone(id, today, nxt)
            Toast.makeText(this, "کار ثبت شد. نوبت بعد: $nxt", Toast.LENGTH_LONG).show()
            finish()
        }

        findViewById<Button>(R.id.delete).setOnClickListener {
            if (id != 0L) db.deleteClient(id)
            finish()
        }
    }
}
