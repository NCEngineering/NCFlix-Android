package com.ncflix.app.utils

import com.ncflix.app.di.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

object UpdateChecker {
    private const val REPO_OWNER = "NCEngineering"
    private const val REPO_NAME = "NCFlix-Android"
    private const val GITHUB_API_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"

    suspend fun checkForUpdate(currentVersion: String): UpdateResult = withContext(Dispatchers.IO) {
        // Reuse shared client to save resources (connection pool, thread pool, etc)
        val client = NetworkClient.client
        val request = Request.Builder()
            .url(GITHUB_API_URL)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        try {
            val response = client.newCall(request).execute()
            
            if (response.code == 404) {
                // Repo likely private or no releases yet
                return@withContext UpdateResult.NoUpdate
            }
            
            if (!response.isSuccessful) {
                return@withContext UpdateResult.Error("Failed to check for updates: ${response.code}")
            }

            val jsonStr = response.body?.string() ?: return@withContext UpdateResult.Error("Empty response")
            val json = JSONObject(jsonStr)
            
            val latestVersionTag = json.optString("tag_name", "").removePrefix("v")
            val downloadUrl = json.optString("html_url", "")

            if (isNewerVersion(currentVersion, latestVersionTag)) {
                return@withContext UpdateResult.Available(latestVersionTag, downloadUrl)
            } else {
                return@withContext UpdateResult.NoUpdate
            }

        } catch (e: Exception) {
            return@withContext UpdateResult.Error(e.message ?: "Unknown error")
        }
    }
    
    private fun isNewerVersion(current: String, latest: String): Boolean {
        try {
            val currentParts = current.split(".").map { it.toInt() }
            val latestParts = latest.split(".").map { it.toInt() }
            val length = maxOf(currentParts.size, latestParts.size)

            for (i in 0 until length) {
                val c = if (i < currentParts.size) currentParts[i] else 0
                val l = if (i < latestParts.size) latestParts[i] else 0
                if (l > c) return true
                if (l < c) return false
            }
        } catch (e: Exception) {
            // Fallback to simple string check if parsing fails
             return latest != current
        }
        return false
    }

    sealed class UpdateResult {
        data class Available(val version: String, val url: String) : UpdateResult()
        object NoUpdate : UpdateResult()
        data class Error(val message: String) : UpdateResult()
    }
}