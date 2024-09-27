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
import org.bouncycastle.jce.provider.BouncyCastleProvider
import work.socialhub.kbsky.internal.share._InternalUtility
import work.socialhub.kbsky.util.MediaType
import work.socialhub.khttpclient.HttpRequest
import java.security.MessageDigest
import java.security.Security
import java.security.Signature
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


class MainActivity : ComponentActivity() {

    private val myPref by lazy { MyPreferences(this) }

    // テスト用
    // TODO 認証のたびに変更すること
    private val codeVerifier = "a10b58dd6ea4d9c4debf94d035c5975c5c915ea04ftest"
    private val codeChallenge = generateSha256Hash(codeVerifier)
    private val state = "8760520e052dcca8a0b82845a5a6d945b180f8b570e92acfbc9a5a1446a50007"

    private val codeTextFlow: MutableStateFlow<String> by lazy {
        MutableStateFlow(
            // 下記をブラウザで認証・認可後に書き換えること
            "cod-ca24011dd07de91b8aed72dc5159447f5679e9d07eb7b5e2fd475becd3ba29a9"
        )
    }

    // Key Pair
    private val publicKeyTextFlow: MutableStateFlow<String> by lazy { MutableStateFlow(myPref.publicKeyBase64) }
    private val privateKeyTextFlow: MutableStateFlow<String> by lazy { MutableStateFlow(myPref.privateKeyBase64) }

    // 最新のdpop-nonceを保持する
    private val dpopNonceTextFlow: MutableStateFlow<String> by lazy { MutableStateFlow(myPref.dpopNonce) }

    private val screenNameText: MutableStateFlow<String> by lazy { MutableStateFlow("takke.jp") }

    private val resultTextFlow: MutableStateFlow<String> by lazy { MutableStateFlow("") }

    // https://github.com/bluesky-social/atproto/issues/2814 の認証サーバのバグによりまだ動かない
//    private val clientId = "https://zonepane.com/oauth/bluesky/zonepane/client-metadata.json"
//    private val redirectUri = "http://zonepane.com/oauth/bluesky/zonepane/callback"
    private val clientId = "http://localhost/zonepane"
    private val redirectUri = "http://127.0.0.1/zonepane"

    private val scopes = listOf("atproto", "transition:generic", "transition:chat.bsky")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BlueskyOAuthSampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TheSampleScreen(
                        publicKeyTextFlow = publicKeyTextFlow,
                        privateKeyTextFlow = privateKeyTextFlow,
                        screenNameTextFlow = screenNameText,
                        codeTextFlow = codeTextFlow,
                        dpopNonceTextFlow = dpopNonceTextFlow,
                        resultTextFlow = resultTextFlow,
                        onGenerateKeyPair = {
                            //--------------------------------------------------
                            // ES256 の鍵ペア生成
                            //--------------------------------------------------
                            val keyPair = Es256KeyHelper.generateKeyPair()
                            publicKeyTextFlow.value = Es256KeyHelper.serializeKeyToString(keyPair.first)
                            privateKeyTextFlow.value = Es256KeyHelper.serializeKeyToString(keyPair.second)

                            // save
                            myPref.publicKeyBase64 = publicKeyTextFlow.value
                            myPref.privateKeyBase64 = privateKeyTextFlow.value
                        },
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

    private suspend fun makeDpopHeader(endpoint: String, method: String): String {

        val dpopNonce = dpopNonceTextFlow.value
        // dpop-nonce なしの場合はリクエストに失敗し、新たな dpop-nonce を収集する
//        if (dpopNonce.isEmpty()) {
//            appendResult("dpop-nonceが見つかりません")
//            return ""
//        }

        appendResult("dpop-none=[$dpopNonce]")

        // "SHA256withPLAIN-ECDSA" を使えるようにする
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

        val publicKey = Es256KeyHelper.deserializePublicKeyFromString(publicKeyTextFlow.value)
        val privateKey = Es256KeyHelper.deserializePrivateKeyFromString(privateKeyTextFlow.value)

        val jwt = OAuthHelper.makeDpopHeader(
            clientId = clientId,
            endpoint = endpoint,
            method = method,
            dpopNonce = dpopNonce,
            publicKeyWAffineX = publicKey.w.affineX.toByteArray(),
            publicKeyWAffineY = publicKey.w.affineY.toByteArray(),
            sign = { jwtMessage ->
                // jwtMessage を署名し、バイト列を返す

                // "SHA256withECDSAinP1363Format" は Android で使用不可
//              val jwtSignature = Signature.getInstance("SHA256withECDSAinP1363Format")

                val jwtSignature = Signature.getInstance("SHA256withPLAIN-ECDSA", "BC") // IEEE P1363

                // jwtMessage の署名
                jwtSignature.initSign(privateKey)
                jwtSignature.update(jwtMessage.toByteArray())
                jwtSignature.sign()
            }
        )

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

    @OptIn(ExperimentalEncodingApi::class)
    private fun generateSha256Hash(base64Value: String): String {
        val value = base64Value.toByteArray()

        // SHA-256でハッシュ化
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(value)

        // BASE64エンコード
        return Base64.encode(hashBytes)
    }

}
