package com.madassignment.myaccomodationapp.data.util

import android.util.Base64

object FirestoreCursors {
    fun encodePath(path: String): String =
        Base64.encodeToString(path.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    fun decodePath(encoded: String): String =
        String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)
}
