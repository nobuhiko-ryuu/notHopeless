package com.nothopeless.app.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

private data class OnboardingPage(
    val icon: String,
    val title: String,
    val body: String,
)

private val pages = listOf(
    OnboardingPage(
        icon = "✦",
        title = "世の中って、\n捨てたもんじゃない",
        body = "ふと気づいた誰かの優しさ。\n小さな出来事を、ここで共有しましょう。",
    ),
    OnboardingPage(
        icon = "◎",
        title = "匿名で、安心して",
        body = "名前も顔も不要です。\n日常の中の小さな「ほっこり」を\n気軽に投稿できます。",
    ),
    OnboardingPage(
        icon = "♡",
        title = "あなたも、きっと\n感じるはず",
        body = "誰かの投稿に「捨てたもんじゃない」と\nリアクションして、気持ちをシェアしよう。",
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onCompleted: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) onCompleted()
    }
    val pagerState = rememberPagerState(pageCount = { 3 })
    LaunchedEffect(uiState.currentPage) {
        pagerState.animateScrollToPage(uiState.currentPage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                val pageData = pages[page]
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Text(
                            text = pageData.icon,
                            fontSize = 48.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = pageData.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            lineHeight = 36.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = pageData.body,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            lineHeight = 26.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (i == uiState.currentPage) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(8.dp),
                    ) {}
                }
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { viewModel.nextPage() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.currentPage < 2) "次へ" else "はじめる")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
