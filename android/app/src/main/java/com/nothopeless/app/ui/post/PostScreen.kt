package com.nothopeless.app.ui.post

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nothopeless.app.data.model.EffectType
import com.nothopeless.app.data.model.KindnessType
import com.nothopeless.app.data.model.SceneType
import com.nothopeless.app.data.model.UserStateType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    onPostSuccess: () -> Unit,
    viewModel: PostViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) { viewModel.clearSuccess(); onPostSuccess() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("投稿する") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // シーン選択
            Text("どこで？", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SceneType.values().forEach { scene ->
                    FilterChip(
                        selected = uiState.scene == scene,
                        onClick = { viewModel.onSceneSelected(scene) },
                        label = { Text(scene.label) },
                    )
                }
            }
            // 優しさタイプ
            Text("どんな優しさ？", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KindnessType.values().forEach { kt ->
                    FilterChip(
                        selected = uiState.kindnessType == kt,
                        onClick = { viewModel.onKindnessTypeSelected(kt) },
                        label = { Text(kt.label) },
                    )
                }
            }
            // 自分の状態（任意）
            Text("その時の自分（任意）", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UserStateType.values().forEach { us ->
                    FilterChip(
                        selected = uiState.userState == us,
                        onClick = { viewModel.onUserStateSelected(if (uiState.userState == us) null else us) },
                        label = { Text(us.label) },
                    )
                }
            }
            // 本文
            Text("なにがあった？", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = uiState.body,
                onValueChange = { viewModel.onBodyChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                placeholder = { Text("受け取った優しさを書いてね（140字以内）") },
                supportingText = {
                    Text(
                        "${uiState.body.length}/140",
                        color = if (uiState.body.length > 140) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface,
                    )
                },
                isError = uiState.body.length > 140,
            )
            if (uiState.hasProperNounWarning) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        "安心のため、店名・駅名・人が特定できる情報は控えてね。ぼかして書き直してみよう。",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            // 変化
            Text("それで、どう変わった？", style = MaterialTheme.typography.labelLarge)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EffectType.values().forEach { ef ->
                    FilterChip(
                        selected = uiState.effect == ef,
                        onClick = { viewModel.onEffectSelected(ef) },
                        label = { Text(ef.label) },
                    )
                }
            }
            // エラー表示
            uiState.submitError?.let { err ->
                val msg = when (err) {
                    SubmitError.PERSONAL_INFO -> "個人情報が含まれているため送信できませんでした"
                    SubmitError.SPECIFIC_NOUN -> "特定できる情報が含まれているため送信できませんでした"
                    SubmitError.COOLDOWN -> "少し時間をおいてから投稿してください（5分間隔）"
                    SubmitError.NETWORK -> "通信エラーが発生しました。もう一度試してください"
                    SubmitError.UNKNOWN -> "送信できませんでした。もう一度試してください"
                }
                Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            // 送信ボタン
            Button(
                onClick = { viewModel.submit() },
                enabled = viewModel.isSubmitEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isSubmitting) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("投稿する")
            }
        }
    }
}
