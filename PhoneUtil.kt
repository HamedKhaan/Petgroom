package com.petgroom.autosms

object PhoneUtil {
    fun normalize(raw: String): String {
        var s = raw.filter { it.isDigit() || it == '+' }
        if (s.startsWith("0098")) s = "+98" + s.drop(4)
        else if (s.startsWith("98") && !s.startsWith("+")) s = "+$s"
        else if (s.startsWith("09")) s = "+98" + s.drop(1)
        else if (s.startsWith("9") && s.length == 10) s = "+98$s"
        return s
    }
}
