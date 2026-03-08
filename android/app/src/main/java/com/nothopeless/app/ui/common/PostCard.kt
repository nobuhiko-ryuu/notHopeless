package com.nothopeless.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nothopeless.app.data.model.EffectType
import com.nothopeless.app.data.model.KindnessType
import com.nothopeless.app.data.model.Post
import com.nothopeless.app.data.model.PostStatus
import com.nothopeless.app.data.model.ReactionType
import com.nothopeless.app.data.model.SceneType

@Composable
fun PostCard(
    post: Post,
    reactionState: ReactionType? = null,
    onReact: (ReactionType) -> Unit = {},
    onReport: () -> Unit = {},
    showReactionButtons: Boolean = post.status == PostStatus.VISIBLE,
) {
    val accentColor = when (post.kindnessType) {
        KindnessType.CARE.key      -> Color(0xFFE8885A)
        KindnessType.HELP.key      -> Color(0xFF6B9E7A)
        KindnessType.INTEGRITY.key -> Color(0xFF5B7E9E)
        KindnessType.COURAGE.key   -> Color(0xFF9E6B8F)
        KindnessType.PRO.key       -> Color(0xFF8F7E5A)
        else                       -> Color(0xFFE8885A)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )
            Column(modifier = Modifier.padding(start = 14.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SceneBadge(post.scene)
                    KindnessBadge(post.kindnessType, accentColor)
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = onReport,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "通報",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = post.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.height(10.dp))

                EffectLabel(post.effect)

                if (post.status == PostStatus.HIDDEN) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Text(
                            text = "この投稿は非表示になりました",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }

                if (showReactionButtons) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ReactionType.values().forEach { type ->
                            val count = post.reactionCounts[type.key] ?: 0L
                            val isSelected = reactionState == type
                            ReactionChip(
                                type = type,
                                count = count,
                                isSelected = isSelected,
                                onClick = { onReact(type) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SceneBadge(scene: String) {
    val sceneType = SceneType.values().find { it.key == scene }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = sceneType?.label ?: scene,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun KindnessBadge(kindnessType: String, accentColor: Color) {
    val type = KindnessType.values().find { it.key == kindnessType }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = accentColor.copy(alpha = 0.15f),
    ) {
        Text(
            text = type?.label ?: kindnessType,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EffectLabel(effect: String) {
    val effectType = EffectType.values().find { it.key == effect }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "→ ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = effectType?.label ?: effect,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ReactionChip(
    type: ReactionType,
    count: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val label = when (type) {
        ReactionType.NOT_HOPELESS -> "捨てたもんじゃない"
        ReactionType.MOVED -> "じーん"
        ReactionType.DO_TOO -> "私も"
    }
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = if (count > 0) "$label $count" else label,
                style = MaterialTheme.typography.labelSmall,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}
