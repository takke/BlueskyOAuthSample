package com.example.blueskyoauthsample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.example.blueskyoauthsample.ui.theme.BlueskyOAuthSampleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import work.socialhub.kbsky.ATProtocolException
import work.socialhub.kbsky.auth.AuthFactory
import work.socialhub.kbsky.auth.OAuthContext
import work.socialhub.kbsky.auth.api.entity.oauth.BuildAuthorizationUrlRequest
import work.socialhub.kbsky.auth.api.entity.oauth.OAuthPushedAuthorizationRequest
import work.socialhub.kbsky.auth.api.entity.oauth.OAuthTokenRequest
import work.socialhub.kbsky.domain.Service


class MainActivity : ComponentActivity() {

    private val myPref by lazy { MyPreferences(this) }

    private val screenNameText: MutableStateFlow<String> by lazy { MutableStateFlow("takke.jp") }

    private val resultTextFlow: MutableStateFlow<String> by lazy { MutableStateFlow("") }

    private val clientId = "https://zonepane.com/oauth/bluesky/zonepane/client-metadata.json"

    private val redirectUri = "com.zonepane:/callback"
//    private val redirectUri = "https://zonepane.com/oauth/bluesky/zonepane/callback"
//    private val clientId = "http://localhost/zonepane"
//    private val redirectUri = "http://127.0.0.1/zonepane"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BlueskyOAuthSampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TheSampleScreen(
                        screenNameTextFlow = screenNameText,
                        resultTextFlow = resultTextFlow,
                        onStartAuth = { screenName ->
                            startAuth(screenName)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        lifecycleScope.launch {
            // intent 取得
            val intent = intent
            val uri = intent.data

            // urlが非nullの場合はブラウザからのカスタムスキーマ起動とみなし、codeを取得する
            if (uri != null) {
                appendResult("intent.data: $uri")

//                val state = uri.getQueryParameter("state")
                // TODO state をチェックすべき

                val code = uri.getQueryParameter("code")
                if (code != null) {
                    appendResult("code: $code")
                    startGetToken(code)
                }
            }
        }
    }

    private fun startAuth(screenName: String) {
        lifecycleScope.launch {
            try {
                startAuthIn(screenName)
            } catch (e: Exception) {
                if (e is ATProtocolException) {
                    appendResult("ATProtocolException: ${e.message} (body:${e.body})")
                } else {
                    appendResult("Error: " + e.message)
                }
                appendResult(e.stackTraceToString())
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

        appendResult("PARを発行します")

        val oauthContext = OAuthContext().also {
            it.clientId = clientId
            it.redirectUri = redirectUri
        }

        val authorizeUrl = withContext(Dispatchers.Default) {
            val auth = AuthFactory.instance(Service.BSKY_SOCIAL.uri)
//            auth.wellKnown()
//                .oAuthProtectedResource()

            val response = auth.oauth()
                .pushedAuthorizationRequest(oauthContext,
                    OAuthPushedAuthorizationRequest().also {
                        it.loginHint = screenName
                    }
                )

            auth.oauth()
                .buildAuthorizationUrl(oauthContext,
                    BuildAuthorizationUrlRequest().also {
                        it.requestUri = response.data.requestUri
                    })
        }

        appendResult("authorizeUrl: $authorizeUrl")

        appendResult("publicKey: ${oauthContext.publicKey}")
        appendResult("privateKey: ${oauthContext.privateKey}")
        appendResult("dPoPNonce: ${oauthContext.dPoPNonce}")
        appendResult("codeVerifier: ${oauthContext.codeVerifier}")
        appendResult("state: ${oauthContext.state}")

        // トークン取得時に必要なパラメータを保存する
        myPref.dpopNonce = oauthContext.dPoPNonce ?: ""
        myPref.codeVerifier = oauthContext.codeVerifier ?: ""

        // codeChallenge と state はブラウザの認可後のリダイレクトパラメータに含まれるので検証する
        appendResult("ブラウザで開きます: $authorizeUrl")

        //--------------------------------------------------
        // ブラウザで開く
        //--------------------------------------------------
        val intent = Intent(Intent.ACTION_VIEW, authorizeUrl.toUri())
        startActivity(intent)

        // ブラウザのリダイレクト先:
        // http://127.0.0.1/zonepane?
        // iss=https%3A%2F%2Fbsky.social&
        // state=xxx
        // code=cod-xxxxxxxx
    }

    private fun startGetToken(code: String) {
        lifecycleScope.launch {
            try {
                startGetTokenIn(code)
            } catch (e: Exception) {
                appendResult("Error: " + e.message)
                appendResult(e.stackTraceToString())
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

            val oAuthContext = OAuthContext().also {
                it.clientId = clientId
                it.redirectUri = redirectUri
                it.codeVerifier = myPref.codeVerifier
                it.dPoPNonce = myPref.dpopNonce
//                it.publicKey = myPref.publicKeyBase64
//                it.privateKey = myPref.privateKeyBase64
            }

            appendResult("clientId: ${oAuthContext.clientId}")
            appendResult("redirectUri: ${oAuthContext.redirectUri}")
            appendResult("codeVerifier: ${oAuthContext.codeVerifier}")
            appendResult("dPoPNonce: ${oAuthContext.dPoPNonce}")

            // kbsky 版
            val auth = AuthFactory.instance(Service.BSKY_SOCIAL.uri)
            val response = auth.oauth()
                .tokenRequest(oAuthContext,
                    OAuthTokenRequest().also {
                        it.code = code
                    }
                )

            appendResult("accessToken: ${response.data.accessToken}")
            appendResult("tokenType: ${response.data.tokenType}")
            appendResult("refreshToken: ${response.data.refreshToken}")
            appendResult("scope: ${response.data.scope}")
            appendResult("expiresIn: ${response.data.expiresIn}")
            appendResult("sub: ${response.data.sub}")

            appendResult("publicKey: ${oAuthContext.publicKey}")
            appendResult("privateKey: ${oAuthContext.privateKey}")

        }
    }

    private suspend fun appendResult(text: String) {
        withContext(Dispatchers.Main) {
            resultTextFlow.value += text + "\n"

            Log.d("BlueskyOAuthSample", text)
        }
    }

}
