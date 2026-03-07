package com.nothopeless.app.data.repository

import com.google.firebase.Timestamp
import com.nothopeless.app.data.model.Post
import com.nothopeless.app.data.remote.FirestoreDataSource
import com.nothopeless.app.data.remote.FunctionsDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    private val firestore: FirestoreDataSource,
    private val functions: FunctionsDataSource,
) {
    suspend fun getFeed(cursor: Timestamp? = null): List<Post> = firestore.getFeed(cursor)
    suspend fun getDailyPicks(): List<Post> = firestore.getDailyPickPosts()
    suspend fun getMyPosts(authorId: String): List<Post> = firestore.getMyPosts(authorId)
    suspend fun createPost(
        scene: String, kindnessType: String, userState: String?,
        effect: String, body: String,
    ): String = functions.createPost(scene, kindnessType, userState, effect, body)
}
