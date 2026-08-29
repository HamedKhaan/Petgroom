package com.petgroom.autosms

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper(ctx: Context) : SQLiteOpenHelper(ctx, "petgroom_offline.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE clients (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              owner_name TEXT NOT NULL,
              phone TEXT NOT NULL,
              pet_name TEXT,
              pet_type TEXT DEFAULT 'سگ',
              breed TEXT,
              category TEXT DEFAULT 'عادی',
              last_visit TEXT,
              next_visit TEXT,
              cycle_days INTEGER DEFAULT 30,
              notes TEXT,
              muted INTEGER DEFAULT 0,
              temperament TEXT,
              debt INTEGER DEFAULT 0,
              service TEXT,
              created_at TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE templates (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              name TEXT NOT NULL,
              body TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO templates(name,body) VALUES
            ('یادآوری نوبت','سلام {name} عزیز\nنوبت اصلاح {pet} نزدیک است. برای هماهنگی همین پیام را جواب بدهید.\nسالن گرومینگ'),
            ('کار تمام','سلام {name}\nکار {pet} تمام شد و آماده تحویل است.'),
            ('موعد گذشته','سلام {name}\nاز آخرین اصلاح {pet} مدتی گذشته. اگر نوبت می‌خواهید پیام بدهید.')
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {}

    data class Client(
        val id: Long = 0,
        val ownerName: String,
        val phone: String,
        val petName: String = "",
        val petType: String = "سگ",
        val breed: String = "",
        val category: String = "عادی",
        val lastVisit: String = "",
        val nextVisit: String = "",
        val cycleDays: Int = 30,
        val notes: String = "",
        val muted: Boolean = false,
        val temperament: String = "",
        val debt: Int = 0,
        val service: String = ""
    )

    fun allClients(): List<Client> {
        val out = mutableListOf<Client>()
        readableDatabase.rawQuery("SELECT * FROM clients ORDER BY owner_name", null).use { c ->
            while (c.moveToNext()) out += readClient(c)
        }
        return out
    }

    fun overdue(today: String): List<Client> {
        val out = mutableListOf<Client>()
        readableDatabase.rawQuery(
            "SELECT * FROM clients WHERE next_visit IS NOT NULL AND next_visit != '' AND next_visit < ? ORDER BY next_visit",
            arrayOf(today)
        ).use { c ->
            while (c.moveToNext()) out += readClient(c)
        }
        return out
    }

    fun getClient(id: Long): Client? {
        readableDatabase.rawQuery("SELECT * FROM clients WHERE id=?", arrayOf(id.toString())).use { c ->
            if (c.moveToFirst()) return readClient(c)
        }
        return null
    }

    fun upsert(client: Client): Long {
        val v = ContentValues().apply {
            put("owner_name", client.ownerName)
            put("phone", client.phone)
            put("pet_name", client.petName)
            put("pet_type", client.petType)
            put("breed", client.breed)
            put("category", client.category)
            put("last_visit", client.lastVisit)
            put("next_visit", client.nextVisit)
            put("cycle_days", client.cycleDays)
            put("notes", client.notes)
            put("muted", if (client.muted) 1 else 0)
            put("temperament", client.temperament)
            put("debt", client.debt)
            put("service", client.service)
            if (client.id == 0L) put("created_at", System.currentTimeMillis().toString())
        }
        return if (client.id == 0L) {
            val existing = findByPhone(client.phone)
            if (existing != null) {
                writableDatabase.update("clients", v, "id=?", arrayOf(existing.id.toString()))
                existing.id
            } else writableDatabase.insert("clients", null, v)
        } else {
            writableDatabase.update("clients", v, "id=?", arrayOf(client.id.toString()))
            client.id
        }
    }

    fun findByPhone(phone: String): Client? {
        readableDatabase.rawQuery("SELECT * FROM clients WHERE phone=?", arrayOf(phone)).use { c ->
            if (c.moveToFirst()) return readClient(c)
        }
        return null
    }

    fun deleteClient(id: Long) {
        writableDatabase.delete("clients", "id=?", arrayOf(id.toString()))
    }

    fun markDone(id: Long, today: String, next: String) {
        val v = ContentValues().apply {
            put("last_visit", today)
            put("next_visit", next)
        }
        writableDatabase.update("clients", v, "id=?", arrayOf(id.toString()))
    }

    fun templates(): List<Pair<Long, String>> {
        val out = mutableListOf<Pair<Long, String>>()
        readableDatabase.rawQuery("SELECT id, name FROM templates ORDER BY id", null).use { c ->
            while (c.moveToNext()) out += c.getLong(0) to c.getString(1)
        }
        return out
    }

    fun templateBody(id: Long): String {
        readableDatabase.rawQuery("SELECT body FROM templates WHERE id=?", arrayOf(id.toString())).use { c ->
            if (c.moveToFirst()) return c.getString(0) ?: ""
        }
        return ""
    }

    private fun readClient(c: android.database.Cursor) = Client(
        id = c.getLong(c.getColumnIndexOrThrow("id")),
        ownerName = c.getString(c.getColumnIndexOrThrow("owner_name")) ?: "",
        phone = c.getString(c.getColumnIndexOrThrow("phone")) ?: "",
        petName = c.getString(c.getColumnIndexOrThrow("pet_name")) ?: "",
        petType = c.getString(c.getColumnIndexOrThrow("pet_type")) ?: "سگ",
        breed = c.getString(c.getColumnIndexOrThrow("breed")) ?: "",
        category = c.getString(c.getColumnIndexOrThrow("category")) ?: "عادی",
        lastVisit = c.getString(c.getColumnIndexOrThrow("last_visit")) ?: "",
        nextVisit = c.getString(c.getColumnIndexOrThrow("next_visit")) ?: "",
        cycleDays = c.getInt(c.getColumnIndexOrThrow("cycle_days")),
        notes = c.getString(c.getColumnIndexOrThrow("notes")) ?: "",
        muted = c.getInt(c.getColumnIndexOrThrow("muted")) == 1,
        temperament = c.getString(c.getColumnIndexOrThrow("temperament")) ?: "",
        debt = c.getInt(c.getColumnIndexOrThrow("debt")),
        service = c.getString(c.getColumnIndexOrThrow("service")) ?: ""
    )

    companion object {
        fun fill(body: String, c: Client): String =
            body
                .replace("{name}", c.ownerName)
                .replace("{owner_name}", c.ownerName)
                .replace("{pet}", c.petName.ifBlank { "پت" })
                .replace("{pet_name}", c.petName.ifBlank { "پت" })
                .replace("{pet_type}", c.petType)
                .replace("{type}", c.petType)
                .replace("{next_visit}", c.nextVisit)
                .replace("{last_visit}", c.lastVisit)
                .replace("{cycle_days}", c.cycleDays.toString())
    }
}
