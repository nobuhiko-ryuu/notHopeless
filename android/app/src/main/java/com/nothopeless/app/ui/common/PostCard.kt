package com.nothopeless.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nothopeless.app.data.model.EffectType
import com.nothopeless.app.data.model.KindnessType
import com.nothopeless.app.data.model.Post
import com.nothopeless.app.data.model.PostStatus
import com.nothopeless.app.data.model.ReactionType
import com.nothopeless.app.data.model.SceneType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PostCard(
    post: Post,
    reactionState: ReactionType? = null,
    onReact: (ReactionType) -> Unit = {},
    onReport: () -> Unit = {},
    showReactionButtons: Boolean = post.status == PostStatus.VISIBLE,
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (post.status == PostStatus.HIDDEN) {
                Text(
                    text = "この投稿は非表示になりました",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(8.dp))
            }
            // タグ行
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SceneType.values().find { it.key == post.scene }?.let {
                    SuggestionChip(onClick = {}, label = { Text(it.label, style = MaterialTheme.typography.labelSmall) })
                }
                KindnessType.values().find { it.key == post.kindnessType }?.let {
                    SuggestionChip(onClick = {}, label = { Text(it.label, style = MaterialTheme.typography.labelSmall) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(text = post.body, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            EffectType.values().find { it.key == post.effect }?.let {
                Text(text = "→ ${it.label}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
            // 相対時刻
            post.createdAt?.let { ts ->
                val formatted = Instant.ofEpochSecond(ts.seconds)
                    .atZone(ZoneId.of("Asia/Tokyo"))
                    .format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            if (showReactionButtons) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ReactionType.values().forEach { type ->
                        val count = post.reactionCounts[type.key] ?: 0L
                        val selected = reactionState == type
                        FilterChip(
                            selected = selected,
                            onClick = { onReact(type) },
                            label = { Text("${type.label} $count", style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "メニュー")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("通報") }, onClick = { menuExpanded = false; onReport() })
                    }
                }
            }
        }
    }
}
