package com.nothopeless.app.ui.guidelines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidelinesScreen(onBack: () -> Unit) {
    val rules = listOf(
        "受け取った優しさだけを投稿できます",
        "画像・コメントはありません",
        "店名・駅名・会社名・人名など特定できる情報は書かないでください",
        "個人情報（電話番号・メールアドレス等）は禁止です",
        "ルール違反の投稿は非表示・投稿不可になる場合があります",
        "気になる投稿は通報ボタンからご連絡ください",
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("投稿ルール") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            rules.forEachIndexed { i, rule ->
                Text("${i + 1}. $rule", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
