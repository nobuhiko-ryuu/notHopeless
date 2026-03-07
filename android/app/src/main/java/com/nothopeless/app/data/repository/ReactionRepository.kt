package com.nothopeless.app.data.repository

import com.nothopeless.app.data.remote.FunctionsDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReactionRepository @Inject constructor(private val functions: FunctionsDataSource) {
    suspend fun react(postId: String, reactionType: String) =
        functions.reactToPost(postId, reactionType)
}
