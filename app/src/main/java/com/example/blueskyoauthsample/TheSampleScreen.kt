package com.example.blueskyoauthsample

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.blueskyoauthsample.ui.theme.BlueskyOAuthSampleTheme
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun TheSampleScreen(
    screenNameTextFlow: MutableStateFlow<String>,
    resultTextFlow: MutableStateFlow<String>,
    modifier: Modifier = Modifier,
    onStartAuth: (String) -> Unit = {},
) {

    Column {

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


@Preview(showBackground = true, widthDp = 400, heightDp = 400)
@Composable
fun GreetingPreview() {
    BlueskyOAuthSampleTheme {
        TheSampleScreen(
            screenNameTextFlow = MutableStateFlow("takke.jp"),
            resultTextFlow = MutableStateFlow("ここに実行結果が表示されます"),
        )
    }
}