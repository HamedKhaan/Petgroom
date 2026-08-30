package com.petgroom.autosms

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var db: DbHelper
    private val prefs by lazy { getSharedPreferences("petgroom_settings", MODE_PRIVATE) }
    private lateinit var salonName: EditText
    private lateinit var phone: EditText
    private lateinit var signature: EditText
    private lateinit var delay: EditText
    private lateinit var templateSpinner: Spinner
    private lateinit var templateBody: EditText
    private lateinit var templateName: EditText
    private var templates = emptyList<Pair<Long, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        db = DbHelper(this)

        salonName = findViewById(R.id.salonName)
        phone = findViewById(R.id.salonPhone)
        signature = findViewById(R.id.signature)
        delay = findViewById(R.id.defaultDelay)
        templateSpinner = findViewById(R.id.templateSpinner)
        templateBody = findViewById(R.id.templateBody)
        templateName = findViewById(R.id.templateName)

        salonName.setText(prefs.getString("salon_name", "پت‌گروم"))
        phone.setText(prefs.getString("salon_phone", ""))
        signature.setText(prefs.getString("signature", "پت‌گروم 🐾"))
        delay.setText(prefs.getLong("delay_seconds", 4L).toString())

        findViewById<Button>(R.id.saveSettings).setOnClickListener { saveSettings() }
        findViewById<Button>(R.id.openGuide).setOnClickListener {
            startActivity(Intent(this, GuideActivity::class.java))
        }
        findViewById<Button>(R.id.resetSettings).setOnClickListener {
            salonName.setText("پت‌گروم")
            phone.setText("")
            signature.setText("پت‌گروم 🐾")
            delay.setText("4")
            Toast.makeText(this, "مقادیر پیش‌فرض برگشت؛ برای ثبت روی ذخیره بزنید.", Toast.LENGTH_SHORT).show()
        }

        loadTemplates()
        findViewById<Button>(R.id.saveTemplate).setOnClickListener { saveTemplate() }
        findViewById<Button>(R.id.newTemplate).setOnClickListener {
            templateSpinner.setSelection(0)
            templateName.setText("")
            templateBody.setText("")
            templateName.requestFocus()
        }
        findViewById<Button>(R.id.deleteTemplate).setOnClickListener { deleteTemplate() }
    }

    private fun saveSettings() {
        val seconds = delay.text.toString().toLongOrNull()?.coerceIn(2L, 30L) ?: 4L
        prefs.edit()
            .putString("salon_name", salonName.text.toString().trim().ifBlank { "پت‌گروم" })
            .putString("salon_phone", phone.text.toString().trim())
            .putString("signature", signature.text.toString().trim())
            .putLong("delay_seconds", seconds)
            .apply()
        Toast.makeText(this, "تنظیمات ذخیره شد ✓", Toast.LENGTH_SHORT).show()
    }

    private fun loadTemplates() {
        templates = db.templates()
        val names = mutableListOf("قالب جدید")
        names.addAll(templates.map { it.second })
        templateSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        templateSpinner.setSelection(if (templates.isEmpty()) 0 else 1)
        templateSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position == 0) {
                    templateName.setText("")
                    templateBody.setText("")
                } else {
                    val item = templates[position - 1]
                    templateName.setText(item.second)
                    templateBody.setText(db.templateBody(item.first))
                }
            }
        })
    }

    private fun selectedTemplateId(): Long =
        if (templateSpinner.selectedItemPosition in 1..templates.size) templates[templateSpinner.selectedItemPosition - 1].first else 0L

    private fun saveTemplate() {
        val name = templateName.text.toString().trim()
        val body = templateBody.text.toString().trim()
        if (name.isBlank() || body.isBlank()) {
            Toast.makeText(this, "نام و متن قالب را کامل کنید.", Toast.LENGTH_SHORT).show()
            return
        }
        db.saveTemplate(selectedTemplateId(), name, body)
        Toast.makeText(this, "قالب ذخیره شد ✓", Toast.LENGTH_SHORT).show()
        loadTemplates()
    }

    private fun deleteTemplate() {
        val id = selectedTemplateId()
        if (id == 0L) {
            Toast.makeText(this, "اول یک قالب موجود را انتخاب کنید.", Toast.LENGTH_SHORT).show()
            return
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("حذف قالب؟")
            .setMessage("این قالب از لیست پیامک‌ها حذف می‌شود.")
            .setNegativeButton("انصراف", null)
            .setPositiveButton("حذف") { _, _ ->
                db.deleteTemplate(id)
                loadTemplates()
                Toast.makeText(this, "قالب حذف شد.", Toast.LENGTH_SHORT).show()
            }.show()
    }
}
