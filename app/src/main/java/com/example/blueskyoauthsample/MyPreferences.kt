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

    // public key
    var publicKeyBase64: String
        get() = pref.getString("publicKey", "") ?: ""
        set(value) {
            pref.edit {
                putString("publicKey", value)
            }
        }

    // private key
    var privateKeyBase64: String
        get() = pref.getString("privateKey", "") ?: ""
        set(value) {
            pref.edit {
                putString("privateKey", value)
            }
        }
}
