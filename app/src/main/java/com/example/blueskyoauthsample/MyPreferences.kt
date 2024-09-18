package com.example.blueskyoauthsample

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class MyPreferences(context: Context) {

    private val pref = PreferenceManager.getDefaultSharedPreferences(context)

    // current dpop-nonce
    var dpopNonce: String
        get() = pref.getString("dpopNonce", "") ?: ""
        set(value) {
            pref.edit {
                putString("dpopNonce", value)
            }
        }
}
