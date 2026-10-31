package com.notewise.clone.core.sync

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.notewise.clone.core.database.AppDatabase
import com.notewise.clone.core.database.NotebookEntity
import com.notewise.clone.core.database.PageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class SyncManager(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    // Configurable endpoint
    var serverBaseUrl: String = "http://10.0.2.2:3000" // 10.0.2.2 maps to localhost on Android Emulator
    var authToken: String? = null

    private val prefs = context.getSharedPreferences("notewise_sync_prefs", Context.MODE_PRIVATE)

    suspend fun sync(): Boolean = withContext(Dispatchers.IO) {
        val token = authToken ?: return@withContext false
        val lastSyncTime = prefs.getLong("last_sync_timestamp", 0L)

        try {
            // 1. PUSH local changes to Prisma Cloud Backend
            val modifiedNotebooks = db.notebookDao().getModifiedSince(lastSyncTime)
            val modifiedPages = db.pageDao().getModifiedSince(lastSyncTime)

            if (modifiedNotebooks.isNotEmpty() || modifiedPages.isNotEmpty()) {
                val pushPayload = mapOf(
                    "notebooks" to modifiedNotebooks,
                    "pages" to modifiedPages
                )
                val pushRequest = Request.Builder()
                    .url("$serverBaseUrl/api/sync/push")
                    .header("Authorization", "Bearer $token")
                    .post(gson.toJson(pushPayload).toRequestBody(JSON))
                    .build()

                client.newCall(pushRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("SyncManager", "Push failed: ${response.code}")
                        return@withContext false
                    }
                }
            }

            // 2. PULL remote changes from Prisma Cloud Backend
            val sinceParam = if (lastSyncTime > 0) "?since=${java.time.Instant.ofEpochMilli(lastSyncTime)}" else ""
            val pullRequest = Request.Builder()
                .url("$serverBaseUrl/api/sync/pull$sinceParam")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(pullRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("SyncManager", "Pull failed: ${response.code}")
                    return@withContext false
                }
                val body = response.body?.string() ?: return@withContext false
                val pullData = gson.fromJson(body, PullResponse::class.java)

                // Update local Room database with remote records
                pullData.notebooks?.forEach { nb ->
                    db.notebookDao().insertNotebook(nb)
                }
                pullData.pages?.forEach { page ->
                    db.pageDao().upsertPage(page)
                }

                prefs.edit().putLong("last_sync_timestamp", System.currentTimeMillis()).apply()
            }

            return@withContext true
        } catch (e: Exception) {
            Log.e("SyncManager", "Sync error", e)
            return@withContext false
        }
    }

    private data class PullResponse(
        val serverTime: String?,
        val notebooks: List<NotebookEntity>?,
        val pages: List<PageEntity>?
    )
}

