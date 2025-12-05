package com.ncflix.app.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object UpdateChecker {
    private const val REPO_OWNER = "BOMBTIMECS-Co"
    private const val REPO_NAME = "NCFlix-Android"
    private const val GITHUB_API_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"

    suspend fun checkForUpdate(currentVersion: String): UpdateResult = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
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

            if (latestVersionTag.isNotEmpty() && latestVersionTag != currentVersion) {
                // Simple string comparison might not be enough for semantic versioning (1.10 vs 1.2), 
                // but for now assume standard string inequality implies a difference worth checking.
                // Ideally use a SemVer parser.
                return@withContext UpdateResult.Available(latestVersionTag, downloadUrl)
            } else {
                return@withContext UpdateResult.NoUpdate
            }

        } catch (e: Exception) {
            return@withContext UpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    sealed class UpdateResult {
        data class Available(val version: String, val url: String) : UpdateResult()
        object NoUpdate : UpdateResult()
        data class Error(val message: String) : UpdateResult()
    }
}