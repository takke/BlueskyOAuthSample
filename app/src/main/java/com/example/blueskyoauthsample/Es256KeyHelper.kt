package com.example.blueskyoauthsample

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object Es256KeyHelper {

    /**
     * ES256の鍵ペアを生成
     */
    fun generateKeyPair(): Pair<ECPublicKey, ECPrivateKey> {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"))
        val keyPair = keyPairGenerator.generateKeyPair()
        val publicKey = keyPair.public as ECPublicKey
        val privateKey = keyPair.private as ECPrivateKey

        return Pair(publicKey, privateKey)
    }

    /**
     * KeyをBase64文字列に変換
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun serializeKeyToString(key: java.security.Key): String {
        return Base64.encode(key.encoded)
    }

    /**
     * Base64文字列から公開鍵を復元
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun deserializePublicKeyFromString(publicKeyString: String): ECPublicKey {
        val keyBytes = Base64.decode(publicKeyString)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("EC")
        return keyFactory.generatePublic(keySpec) as ECPublicKey
    }

    /**
     * Base64文字列から秘密鍵を復元
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun deserializePrivateKeyFromString(privateKeyString: String): ECPrivateKey {
        val keyBytes = Base64.decode(privateKeyString)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("EC")
        return keyFactory.generatePrivate(keySpec) as ECPrivateKey
    }
}