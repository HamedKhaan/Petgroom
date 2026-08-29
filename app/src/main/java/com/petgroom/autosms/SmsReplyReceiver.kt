package com.petgroom.autosms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast

class SmsReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val body = msgs.joinToString("") { it.displayMessageBody ?: "" }.trim()
        val from = msgs.firstOrNull()?.displayOriginatingAddress ?: return
        val yes = body.lowercase() in setOf("بله", "ب", "yes", "ok", "1", "تایید", "تأیید")
        if (!yes) return
        context.getSharedPreferences("replies", Context.MODE_PRIVATE)
            .edit()
            .putString(System.currentTimeMillis().toString(), "$from|$body")
            .apply()
        Toast.makeText(context, "جواب بله از $from ثبت شد", Toast.LENGTH_LONG).show()
    }
}
