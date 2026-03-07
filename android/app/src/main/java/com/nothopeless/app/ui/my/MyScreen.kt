package com.nothopeless.app.ui.my

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nothopeless.app.ui.common.PostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(
    onReport: (String) -> Unit,
    viewModel: MyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("マイ投稿") }) }) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.hasError -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("読み込みに失敗しました")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.load() }) { Text("再試行") }
                }
            }
            uiState.posts.isEmpty() -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("まだ投稿がありません。最初の投稿をしてみよう")
            }
            else -> LazyColumn(contentPadding = padding) {
                items(uiState.posts) { post ->
                    PostCard(
                        post = post,
                        showReactionButtons = post.status == "visible",
                        onReport = { onReport(post.postId) },
                    )
                }
            }
        }
    }
}
