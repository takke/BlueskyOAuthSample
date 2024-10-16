package com.example.blueskyoauthsample

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

object MyOAuthHelper {

    @OptIn(ExperimentalEncodingApi::class)
    fun makeDpopHeader(
        clientId: String,
        endpoint: String,
        method: String,
        dpopNonce: String,
        // ES256 公開鍵の x 値、y 値
        publicKeyWAffineX: ByteArray,
        publicKeyWAffineY: ByteArray,
        // 秘密鍵でメッセージを署名する lambda
        sign: (String) -> ByteArray
    ): String {

        // kotlinx-serialization で JSON を生成

        // ヘッダー部生成
        val headerJson = buildJsonObject {
            put("alg", "ES256")
            put("typ", "dpop+jwt")
            put("jwk", buildJsonObject {
                put("kty", "EC")
                put("crv", "P-256")
                put("x", Base64.encode(publicKeyWAffineX))
                put("y", Base64.encode(publicKeyWAffineY))
            })

        }
//        appendResult("----- JWT Header -----")
//        val formatter = Json { prettyPrint = true }
////        appendResult(headerJson.toString())
//        appendResult(formatter.encodeToString(headerJson))

        // ペイロード部生成
        val payloadJson = buildJsonObject {
            // iss, ath は PDS へのリクエスト時に必須
            put("iss", clientId)
            put("sub", clientId)
            put("htu", endpoint)
            put("htm", method)
            val epoch = System.currentTimeMillis() / 1000
            put("exp", epoch + 60)
            // random token string (unique per request)
            put("jti", generateRandomValue())
            put("iat", epoch)
            put("nonce", dpopNonce)
        }
//        appendResult("----- JWT Payload -----")
////        appendResult(payloadJson.toString())
//        appendResult(formatter.encodeToString(payloadJson))

        // 署名
        val headerBase64 = Base64.encode(headerJson.toString().toByteArray())
        val payloadBase64 = Base64.encode(payloadJson.toString().toByteArray())
        val jwtMessage = "$headerBase64.$payloadBase64"

        // 署名生成
        val jwtSignatureBytes: ByteArray = sign(jwtMessage)
        val jwtSignatureStr: String = Base64.encode(jwtSignatureBytes)
        val jwt = "$headerBase64.$payloadBase64.$jwtSignatureStr"

        return jwt
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun generateRandomValue(): String {
        // ランダム値を生成
        val randomValueBytes = ByteArray(32)
        Random.nextBytes(randomValueBytes)

        // BASE64エンコード
        return Base64.encode(randomValueBytes)
    }
}
