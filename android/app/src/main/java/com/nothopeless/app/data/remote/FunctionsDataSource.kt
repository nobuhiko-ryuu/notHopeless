package com.nothopeless.app.data.remote

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FunctionsDataSource @Inject constructor(
    private val functions: FirebaseFunctions,
) {
    suspend fun createPost(
        scene: String,
        kindnessType: String,
        userState: String?,
        effect: String,
        body: String,
    ): String {
        val data = hashMapOf(
            "scene" to scene,
            "kindnessType" to kindnessType,
            "userState" to userState,
            "effect" to effect,
            "body" to body,
        )
        val result = functions.getHttpsCallable("createPost").call(data).await()
        @Suppress("UNCHECKED_CAST")
        return (result.data as Map<String, Any>)["postId"] as String
    }

    suspend fun reactToPost(postId: String, reactionType: String) {
        val data = hashMapOf("postId" to postId, "reactionType" to reactionType)
        functions.getHttpsCallable("reactToPost").call(data).await()
    }

    suspend fun reportPost(postId: String, reason: String, comment: String? = null) {
        val data = hashMapOf<String, Any?>("postId" to postId, "reason" to reason, "comment" to comment)
        functions.getHttpsCallable("reportPost").call(data).await()
    }

    /** Cloud Functions のエラーコードを取り出す */
    fun extractErrorCode(e: Exception): String {
        return if (e is FirebaseFunctionsException) {
            e.details?.toString() ?: e.code.name
        } else {
            "NETWORK"
        }
    }
}
