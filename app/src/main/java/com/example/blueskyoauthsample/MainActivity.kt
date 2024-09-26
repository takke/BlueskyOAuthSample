package com.example.blueskyoauthsample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.blueskyoauthsample.ui.theme.BlueskyOAuthSampleTheme
import io.ktor.http.URLBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.bouncycastle.jce.provider.BouncyCastleProvider
import work.socialhub.kbsky.internal.share._InternalUtility
import work.socialhub.kbsky.util.MediaType
import work.socialhub.khttpclient.HttpRequest
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Security
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random


class MainActivity : ComponentActivity() {

    private val myPref by lazy { MyPreferences(this) }

    // テスト用
    // TODO 認証のたびに変更すること
    private val codeVerifier = "110b58dd6ea4d9c4debf94d035c5975c5c915ea04f7a9d04c665e78b2f7c0007"
    private val codeChallenge = generateSha256Hash(codeVerifier)
    private val state = "8760520e052dcca8a0b82845a5a6d945b180f8b570e92acfbc9a5a1446a50007"

    private val codeTextFlow: MutableStateFlow<String> by lazy {
        MutableStateFlow(
            // 下記をブラウザで認証・認可後に書き換えること
            "cod-fc099f815d64093f103cb36cf37ee2ab45504e9db992fa15e6fcbfbc71716a93"
        )
    }

    private val screenNameText: MutableStateFlow<String> by lazy { MutableStateFlow("takke.jp") }

    // 最新のdpop-nonceを保持する
    private val dpopNonceTextFlow: MutableStateFlow<String> by lazy { MutableStateFlow(myPref.dpopNonce) }

    private val resultTextFlow: MutableStateFlow<String> by lazy { MutableStateFlow("") }

    // https://github.com/bluesky-social/atproto/issues/2814 の認証サーバのバグによりまだ動かない
    private val clientId = "https://zonepane.com/oauth/bluesky/zonepane/client-metadata.json"
    private val redirectUri = "http://zonepane.com/oauth/bluesky/zonepane/callback"
//    private val clientId = "http://localhost/zonepane"
//    private val redirectUri = "http://127.0.0.1/zonepane"

    private val scopes = listOf("atproto", "transition:generic", "transition:chat.bsky")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BlueskyOAuthSampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TheSampleScreen(
                        screenNameTextFlow = screenNameText,
                        codeTextFlow = codeTextFlow,
                        dpopNonceTextFlow = dpopNonceTextFlow,
                        resultTextFlow = resultTextFlow,
                        onStartAuth = { screenName -> startAuth(screenName) },
                        onStartGetToken = { code -> startGetToken(code) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        // ここで自動実行する
//        lifecycleScope.launch {
//            delay(1000)
//            startAuth("takke.jp")
//        }
    }

    private fun startAuth(screenName: String) {
        lifecycleScope.launch {
            try {
                startAuthIn(screenName)
            } catch (e: Exception) {
                appendResult("Error: " + e.message)
                appendResult(e.stackTraceToString())

                throw e
            }
        }
    }

    private suspend fun startAuthIn(screenName: String) {

        resultTextFlow.value = ""

        appendResult("@" + screenName + "で認証します")

        //--------------------------------------------------
        // DID,PDS解決
        //--------------------------------------------------
//        val atp = ATProtocolFactory
//            .instance(BSKY_SOCIAL.uri)
//        val pds = withContext(Dispatchers.Default) {
//            val response = atp
//                .repo()
//                .describeRepo(
//                    RepoDescribeRepoRequest(
//                        repo = screenName
//                    )
//                )
//            val pds = response.data.didDoc?.asDIDDetails?.pdsEndpoint()
//            pds
//        }
////        appendResult("response => [" + response.data + "]")
//        // "https://shimeji.us-east.host.bsky.network" などを取得できる
//        appendResult("pds = $pds")

        appendResult("--------------------")

        //--------------------------------------------------
        // PAR発行
        //--------------------------------------------------
        val parUrl = "https://bsky.social/oauth/par"

        // codeChallenge と state はブラウザの認可後のリダイレクトパラメータに含まれるので検証する

        val authorizeUrl = withContext(Dispatchers.Default) {
            postPar(clientId, redirectUri, codeChallenge, state, screenName, scopes, parUrl)
        }
        if (authorizeUrl == null) {
            appendResult("PAR発行に失敗しました")
            return
        }

        appendResult("ブラウザで開きます: $authorizeUrl")

        //--------------------------------------------------
        // ブラウザで開く
        //--------------------------------------------------
//        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authorizeUrl))
//        startActivity(intent)

        // ブラウザのリダイレクト先:
        // http://127.0.0.1/zonepane?
        // iss=https%3A%2F%2Fbsky.social&
        // state=xxx
        // code=cod-xxxxxxxx
    }

    private suspend fun postPar(
        clientId: String,
        redirectUri: String,
        codeChallenge: String,
        state: String,
        screenName: String,
        scopes: List<String>,
        parUrl: String
    ): String? {

        appendResult("PARを発行します")

        val parameters = mapOf(
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "code_challenge" to codeChallenge,
            "code_challenge_method" to "S256",
            "state" to state,
            "login_hint" to screenName,
            "response_type" to "code",
            "scope" to scopes.joinToString(" "),
        )

        appendResult("parameters = $parameters")

        val r = HttpRequest()
            .url(parUrl)
            .accept(MediaType.JSON)
            .params(parameters)
            .forceApplicationFormUrlEncoded(true)
            .post()

        appendResult("----- status[${r.status}] + body -----")
        appendResult("" + r.stringBody + "")
        appendResult("----- headers -----")
        appendResult("" + r.headers + "")

        // 以下のようなエラーが返ってくる
        // {"error":"invalid_request","error_description":"Invalid redirect_uri https://127.0.0.1/zonepane (allowed: http://127.0.0.1/zonepane http://[::1]/zonepane)"}

        // 成功時は下記のレスポンスが返ってくる
        // {"request_uri":"urn:ietf:params:oauth:request_uri:req-76eb55d33c22d08d2a8a2c0479037868","expires_in":299}
        return if (r.status in 200..299) {
            val parResponse = _InternalUtility.fromJson<ParResponse>(r.stringBody)
            appendResult("$parResponse")

            // ブラウザで開く
            val authorizeEndpoint = URLBuilder("https://bsky.social/oauth/authorize").apply {
                this.parameters.append("request_uri", parResponse.requestUri)
                this.parameters.append("client_id", clientId)
            }.buildString()

            // dpop-nonce の収集と永続化
            collectAndSaveDpopNonce(r.headers)

            authorizeEndpoint
        } else {
            null
        }
    }

    private suspend fun collectAndSaveDpopNonce(headers: Map<String, List<String>>) {

        val dpopNonce = headers["dpop-nonce"]?.firstOrNull()
        if (dpopNonce == null) {
            appendResult("dpop-nonceが見つかりません")
            return
        }
        appendResult("dpop-nonce=[$dpopNonce]")

        this.dpopNonceTextFlow.value = dpopNonce
        myPref.dpopNonce = dpopNonce
    }

    private fun startGetToken(code: String) {
        lifecycleScope.launch {
            try {
                startGetTokenIn(code)
            } catch (e: Exception) {
                appendResult("Error: " + e.message)
                appendResult(e.stackTraceToString())

                throw e
            }
        }
    }

    private suspend fun startGetTokenIn(code: String) {

        resultTextFlow.value = ""

        if (code.isEmpty()) {
            appendResult("codeが空です")
            return
        }

        withContext(Dispatchers.Default) {
            appendResult("tokenを取得します")

            val parameters = mapOf(
                "grant_type" to "authorization_code",
                "code" to code,
                "code_verifier" to codeVerifier,
                "client_id" to clientId,
                "redirect_uri" to redirectUri,
                "response_type" to "code",
            )

            appendResult("parameters = $parameters")

            val tokenUrl = "https://bsky.social/oauth/token"

            val dpopHeader = makeDpopHeader(tokenUrl, "POST")

            if (dpopHeader.isEmpty()) {
                appendResult("dpopHeaderが空です")
                return@withContext
            }

            val r = HttpRequest()
                .url(tokenUrl)
                .accept(MediaType.JSON)
                .params(parameters)
                .forceApplicationFormUrlEncoded(true)
                .header("DPoP", dpopHeader)
                .post()

            appendResult("----- status[${r.status}] + body -----")
            appendResult("" + r.stringBody + "")
            appendResult("----- headers -----")
            appendResult("" + r.headers + "")

            // dpop-nonce の収集と永続化
            collectAndSaveDpopNonce(r.headers)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun makeDpopHeader(endpoint: String, method: String): String {

        val dpopNonce = dpopNonceTextFlow.value
        // dpop-nonce なしの場合はリクエストに失敗し、新たな dpop-nonce を収集する
//        if (dpopNonce.isEmpty()) {
//            appendResult("dpop-nonceが見つかりません")
//            return ""
//        }

        appendResult("dpop-none=[$dpopNonce]")

        //--------------------------------------------------
        // ES256 の鍵ペア生成
        //--------------------------------------------------
        appendResult("ES256の鍵ペアを生成します")
        // TODO この鍵ペアは永続化して保存すること
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"))
        val keyPair = keyPairGenerator.generateKeyPair()
        val publicKey = keyPair.public as ECPublicKey
        val privateKey = keyPair.private as ECPrivateKey

        val publicKeyEncodedBytes: ByteArray = Base64.encodeToByteArray(publicKey.encoded)
        val privateKeyEncodedBytes: ByteArray = Base64.encodeToByteArray(privateKey.encoded)
        appendResult("----- ES256 Public Key -----")
        appendResult(String(publicKeyEncodedBytes))
        appendResult("----- ES256 Private Key -----")
        appendResult(String(privateKeyEncodedBytes))

        // kotlinx-serialization で JSON を生成

        // ヘッダー部生成
        val headerJson = buildJsonObject {
            put("alg", "ES256")
            put("typ", "dpop+jwt")
            put("jwk", buildJsonObject {
                put("kty", "EC")
                put("crv", "P-256")
                put("x", Base64.encode(publicKey.w.affineX.toByteArray()))
                put("y", Base64.encode(publicKey.w.affineY.toByteArray()))
            })
        }
        appendResult("----- JWT Header -----")
        val formatter = Json { prettyPrint = true }
//        appendResult(headerJson.toString())
        appendResult(formatter.encodeToString(headerJson))

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
        appendResult("----- JWT Payload -----")
//        appendResult(payloadJson.toString())
        appendResult(formatter.encodeToString(payloadJson))

        // 署名
        val headerBase64 = Base64.encode(headerJson.toString().toByteArray())
        val payloadBase64 = Base64.encode(payloadJson.toString().toByteArray())
        val jwtMessage = "$headerBase64.$payloadBase64"

        // 署名生成
        // "SHA256withECDSAinP1363Format" は Android で使用不可
//        val jwtSignature = Signature.getInstance("SHA256withECDSAinP1363Format")

        val provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
        if (provider.javaClass != BouncyCastleProvider::class.java) {
            // Android には BouncyCastle の短縮版が既に含まれていてそれには "SHA256withPLAIN-ECDSA" が含まれていないので
            // BouncyCastleProvider を置き換える
            // https://stackoverflow.com/questions/55123228/java-security-nosuchalgorithmexception-no-such-algorithm-ecdsa-for-provider-bc
            appendResult("BouncyCastleProvider を置き換えます")
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        } else {
            appendResult("BouncyCastleProvider はすでに登録されています")
        }
        val jwtSignature = Signature.getInstance("SHA256withPLAIN-ECDSA", "BC") //IEEE P1363

        jwtSignature.initSign(privateKey)
        jwtSignature.update(jwtMessage.toByteArray())
        val jwtSignatureBytes: ByteArray = jwtSignature.sign()
        val jwtSignatureStr: String = Base64.encode(jwtSignatureBytes)
        val jwt = "$headerBase64.$payloadBase64.$jwtSignatureStr"
        appendResult("----- JWT -----")
        appendResult(jwt)

        return jwt
    }

    private suspend fun appendResult(text: String) {
        withContext(Dispatchers.Main) {
            resultTextFlow.value += text + "\n"

            Log.d("BlueskyOAuthSample", text)
        }
    }

    private fun generateSha256Hash(base64Value: String): String {
        val value = base64Value.toByteArray()

        // SHA-256でハッシュ化
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(value)

        // BASE64エンコード
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun generateRandomValue(): String {
        // ランダム値を生成
        val randomValueBytes = ByteArray(32)
        Random.nextBytes(randomValueBytes)

        // BASE64エンコード
        return randomValueBytes.joinToString("") { "%02x".format(it) }
    }
}
