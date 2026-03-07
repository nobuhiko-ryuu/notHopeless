package com.nothopeless.app.data.repository

import com.nothopeless.app.data.remote.FunctionsDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(private val functions: FunctionsDataSource) {
    suspend fun report(postId: String, reason: String) =
        functions.reportPost(postId, reason)
}
