package com.nothopeless.app.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nothopeless.app.data.model.Post
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreDataSource @Inject constructor(
    private val db: FirebaseFirestore,
) {
    companion object {
        private const val PAGE_SIZE = 20L
        private val JST = ZoneId.of("Asia/Tokyo")
    }

    suspend fun getFeed(cursor: Timestamp? = null): List<Post> {
        var query = db.collection("posts")
            .whereEqualTo("status", "visible")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)
        if (cursor != null) query = query.startAfter(cursor)
        return query.get().await().documents.mapNotNull { doc ->
            doc.toObject(Post::class.java)?.copy(postId = doc.id)
        }
    }

    suspend fun getMyPosts(authorId: String): List<Post> {
        return db.collection("posts")
            .whereEqualTo("authorId", authorId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get().await()
            .documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(postId = doc.id)
            }
    }

    suspend fun getDailyPickPosts(): List<Post> {
        val today = LocalDate.now(JST).toString()
        val picksSnap = db.collection("dailyPicks").document(today).get().await()
        val pickIds = (picksSnap.get("pickIds") as? List<*>)
            ?.filterIsInstance<String>()
            ?.take(3)
            ?.filter { it.isNotBlank() }
            ?: return emptyList()

        if (pickIds.isEmpty()) return emptyList()

        // in クエリで一括取得（N+1 禁止）
        return db.collection("posts")
            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), pickIds)
            .get().await()
            .documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(postId = doc.id)
            }
    }
}
