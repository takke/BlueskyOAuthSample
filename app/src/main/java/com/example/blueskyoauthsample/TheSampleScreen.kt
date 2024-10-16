package com.example.blueskyoauthsample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blueskyoauthsample.ui.theme.BlueskyOAuthSampleTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
fun TheSampleScreen(
    publicKeyTextFlow: MutableStateFlow<String>,
    privateKeyTextFlow: MutableStateFlow<String>,
    screenNameTextFlow: MutableStateFlow<String>,
    resultTextFlow: MutableStateFlow<String>,
    modifier: Modifier = Modifier,
    onGenerateKeyPair: () -> Unit = {},
    onStartAuth: (String) -> Unit = {},
) {

    Column {

        // Key Pair
        val publicKeyText by publicKeyTextFlow.collectAsState()
        val privateKeyText by privateKeyTextFlow.collectAsState()
        Text(
            text = "Key Pair",
            modifier = modifier
                .padding(start = 4.dp, top = 4.dp)
                .fillMaxWidth()
        )
        Row(
            modifier = modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = modifier
                    .weight(1f)
            ) {
                Text(
                    text = publicKeyText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    modifier = modifier
                        .padding(start = 4.dp, bottom = 4.dp)
                        .fillMaxWidth()
                        .border(0.5.dp, Color.Gray)
                        .background(Color.LightGray)
                        .padding(4.dp)
                )
                Text(
                    text = privateKeyText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    modifier = modifier
                        .padding(start = 4.dp)
                        .fillMaxWidth()
                        .border(0.5.dp, Color.Gray)
                        .background(Color.LightGray)
                        .padding(4.dp)
                )
            }

            Button(
                onClick = { onGenerateKeyPair() },
                modifier = modifier
                    .width(120.dp)
                    .padding(4.dp)
            ) {
                Text("Generate")
            }
        }

        // ScreenName
        Row(
            modifier = modifier
                .fillMaxWidth()
        ) {
            // 入力エリア
            val text by screenNameTextFlow.collectAsState()
            TextField(
                value = text,
                onValueChange = {
                    screenNameTextFlow.value = it
                },
                label = { Text("ScreenName") },
                modifier = modifier
                    .weight(1f)
                    .padding(4.dp)
            )

            // ボタン
            Button(
                onClick = { onStartAuth(text) },
                modifier = modifier
                    .width(120.dp)
                    .padding(4.dp)
            ) {
                Text("Start")
            }
        }

        // 実行結果
        val resultText by resultTextFlow.collectAsState()
        Box(
            modifier = modifier
                .padding(4.dp)
                .background(Color(0xFFE0E0E0))
                .weight(1f)
                .fillMaxWidth()
        ) {
            Text(
                text = resultText,
                modifier = modifier
                    .padding(4.dp)
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}


@OptIn(ExperimentalEncodingApi::class)
@Preview(showBackground = true, widthDp = 400, heightDp = 400)
@Composable
fun GreetingPreview() {
    BlueskyOAuthSampleTheme {
        TheSampleScreen(
            publicKeyTextFlow = MutableStateFlow(
                Base64.encode("public key".repeat(10).toByteArray())
            ),
            privateKeyTextFlow = MutableStateFlow(
                Base64.encode("private key".repeat(10).toByteArray())
            ),
            screenNameTextFlow = MutableStateFlow("takke.jp"),
            resultTextFlow = MutableStateFlow("ここに実行結果が表示されます"),
        )
    }
}