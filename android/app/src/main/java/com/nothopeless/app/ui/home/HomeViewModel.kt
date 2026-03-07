package com.nothopeless.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.nothopeless.app.data.model.Post
import com.nothopeless.app.data.model.ReactionType
import com.nothopeless.app.data.repository.PostRepository
import com.nothopeless.app.data.repository.ReactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val dailyPicks: List<Post> = emptyList(),
    val feed: List<FeedItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val nextCursor: Timestamp? = null,
    val isLoadingMore: Boolean = false,
    val reactionState: Map<String, ReactionType?> = emptyMap(),
)

sealed class FeedItem {
    data class PostCard(val post: Post) : FeedItem()
    object AdCard : FeedItem()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val reactionRepository: ReactionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _snackbar = MutableSharedFlow<String>()
    val snackbar = _snackbar.asSharedFlow()

    init { loadAll() }

    fun refresh() {
        _uiState.update { HomeUiState() }
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            runCatching {
                val picks = postRepository.getDailyPicks()
                val posts = postRepository.getFeed()
                val cursor = posts.lastOrNull()?.createdAt
                _uiState.update {
                    it.copy(
                        dailyPicks = picks,
                        feed = buildFeedItems(posts),
                        isLoading = false,
                        nextCursor = cursor,
                    )
                }
            }.onFailure {
                _uiState.update { s -> s.copy(isLoading = false, hasError = true) }
            }
        }
    }

    fun loadMore() {
        val cursor = _uiState.value.nextCursor ?: return
        if (_uiState.value.isLoadingMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            runCatching {
                val posts = postRepository.getFeed(cursor)
                val newCursor = posts.lastOrNull()?.createdAt
                val currentPosts = _uiState.value.feed
                    .filterIsInstance<FeedItem.PostCard>()
                    .map { it.post }
                _uiState.update {
                    it.copy(
                        feed = buildFeedItems(currentPosts + posts),
                        isLoadingMore = false,
                        nextCursor = newCursor,
                    )
                }
            }.onFailure {
                _uiState.update { s -> s.copy(isLoadingMore = false) }
            }
        }
    }

    fun react(postId: String, type: ReactionType) {
        val prev = _uiState.value.reactionState[postId]
        // 楽観更新
        _uiState.update { state ->
            val newFeed = state.feed.map { item ->
                if (item is FeedItem.PostCard && item.post.postId == postId) {
                    val updated = item.post.copy(
                        reactionCounts = item.post.reactionCounts.toMutableMap().apply {
                            if (prev != null && prev != type) {
                                this[prev.key] = (this[prev.key] ?: 1L) - 1L
                            }
                            if (prev != type) this[type.key] = (this[type.key] ?: 0L) + 1L
                        }
                    )
                    FeedItem.PostCard(updated)
                } else item
            }
            state.copy(
                feed = newFeed,
                reactionState = state.reactionState + (postId to type),
            )
        }
        viewModelScope.launch {
            runCatching {
                reactionRepository.react(postId, type.key)
            }.onFailure {
                // ロールバック
                _uiState.update { state ->
                    val newFeed = state.feed.map { item ->
                        if (item is FeedItem.PostCard && item.post.postId == postId) {
                            val updated = item.post.copy(
                                reactionCounts = item.post.reactionCounts.toMutableMap().apply {
                                    if (prev != type) this[type.key] = (this[type.key] ?: 1L) - 1L
                                    if (prev != null && prev != type) {
                                        this[prev.key] = (this[prev.key] ?: 0L) + 1L
                                    }
                                }
                            )
                            FeedItem.PostCard(updated)
                        } else item
                    }
                    state.copy(
                        feed = newFeed,
                        reactionState = state.reactionState + (postId to prev),
                    )
                }
                _snackbar.emit("反応の送信に失敗しました")
            }
        }
    }

    private fun buildFeedItems(posts: List<Post>): List<FeedItem> {
        // 11件ごと（0-based index 10, 21, 32... の後ろ）に AdCard を挿入
        val result = mutableListOf<FeedItem>()
        posts.forEachIndexed { index, post ->
            result.add(FeedItem.PostCard(post))
            if ((index + 1) % 11 == 0) result.add(FeedItem.AdCard)
        }
        return result
    }
}
