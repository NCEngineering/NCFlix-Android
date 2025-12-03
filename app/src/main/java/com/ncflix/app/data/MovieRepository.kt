package com.ncflix.app.data

import com.ncflix.app.di.NetworkClient
import com.ncflix.app.model.Movie
import com.ncflix.app.utils.Constants
import com.ncflix.app.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.regex.Pattern
import android.net.Uri
import java.util.ArrayList

class MovieRepository {

    private val client = NetworkClient.client

    private fun getCookieHeader(): String {
        return Constants.COOKIES.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    suspend fun fetchHomeData(): Resource<Pair<Movie?, List<Movie>>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(Constants.BASE_URL)
                .header("User-Agent", Constants.USER_AGENT)
                .header("Cookie", getCookieHeader())
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Resource.Error("Server Error: ${response.code}")

            val html = response.body?.string() ?: return@withContext Resource.Error("Empty response")

            val doc = Jsoup.parse(html, Constants.BASE_URL)
            // Logic aligned with pencuri_cli.py: Support 'article' fallback
            var items = doc.select("div.ml-item")
            if (items.isEmpty()) {
                items = doc.select("article")
            }

            val trendingList = mutableListOf<Movie>()
            var heroMovie: Movie? = null

            for ((index, item) in items.withIndex()) {
                val linkTag = item.selectFirst("a.ml-mask") ?: item.selectFirst("a")
                val imgTag = item.selectFirst("img")

                if (linkTag != null && imgTag != null) {
                    // Fetch title: Try 'oldtitle' attr first, then 'h2' text
                    var title = linkTag.attr("oldtitle")
                    if (title.isEmpty()) {
                        title = item.selectFirst("h2")?.text() ?: ""
                    }

                    // Filter invalid titles (from Python script logic)
                    if (title.isEmpty() || title.uppercase() in listOf("WEB-DL", "HD", "CAM")) continue

                    val link = linkTag.attr("abs:href")
                    // Fetch poster: Try 'data-original' (lazy load) then standard 'src'
                    val poster = imgTag.attr("data-original").ifEmpty { imgTag.attr("src") }

                    val movie = Movie(title, poster, link)

                    if (index == 0) heroMovie = movie
                    else trendingList.add(movie)
                }
            }
            return@withContext Resource.Success(Pair(heroMovie, trendingList))

        } catch (e: Exception) {
            return@withContext Resource.Error(e.message ?: "Network Error", e)
        }
    }

    // FIX: Corrected "withWithContext" to "withContext"
    suspend fun fetchEpisodes(seriesUrl: String): Resource<Map<String, List<Movie>>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(seriesUrl)
                .header("User-Agent", Constants.USER_AGENT)
                .header("Cookie", getCookieHeader())
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Resource.Error("Failed to load episodes: ${response.code}")

            val html = response.body?.string() ?: return@withContext Resource.Error("Empty response")
            val doc = Jsoup.parse(html, seriesUrl)

            val seasonsMap = mutableMapOf<String, MutableList<Movie>>()

            val seasonDivs = doc.select("div.tvseason")

            for (seasonDiv in seasonDivs) {
                val seasonTitle = seasonDiv.selectFirst(".les-title strong")?.text() ?: "Episodes"
                val links = seasonDiv.select("div.les-content a")
                val episodes = mutableListOf<Movie>()

                for (link in links) {
                    val title = link.text().trim()
                    val url = link.attr("abs:href")
                    if (url.isNotEmpty()) {
                        episodes.add(Movie(title, "", url, seasonTitle = seasonTitle))
                    }
                }

                if (episodes.isNotEmpty()) {
                    seasonsMap[seasonTitle] = episodes
                }
            }

            if (seasonsMap.isEmpty()) {
                return@withContext Resource.Error("No episodes found on this page")
            }
            return@withContext Resource.Success(seasonsMap)

        } catch (e: Exception) {
            return@withContext Resource.Error(e.message ?: "Network Error", e)
        }
    }

    // --- CRITICAL FIX: The WebView Player needs a LIST of embed URLs ---
    suspend fun extractStreamUrl(episodeUrl: String): Resource<ArrayList<String>> = withContext(Dispatchers.IO) {
        val serverList = ArrayList<String>()

        try {
            val requestPage = Request.Builder()
                .url(episodeUrl)
                .header("User-Agent", Constants.USER_AGENT)
                .header("Cookie", getCookieHeader())
                .header("Referer", Constants.BASE_URL)
                .build()

            val responsePage = client.newCall(requestPage).execute()
            val pageHtml = responsePage.body?.string() ?: return@withContext Resource.Error("Failed to load page")
            val doc = Jsoup.parse(pageHtml, episodeUrl)

            // Logic adapted from pencuri_cli.py
            // 1. Search for standard tabs (id starting with "tab")
            val tabDivs = doc.select("div[id^=tab]")

            fun addUrlIfValid(url: String) {
                if (url.isNotEmpty() && !url.contains("facebook.com") && !serverList.contains(url)) {
                    // Keep dsvplay resolution as it is useful
                    if (url.contains("dsvplay")) {
                        val resolved = resolveRedirect(url)
                        if (!serverList.contains(resolved)) serverList.add(resolved)
                    } else {
                        serverList.add(url)
                    }
                }
            }

            if (tabDivs.isNotEmpty()) {
                for (div in tabDivs) {
                    val iframe = div.selectFirst("iframe") ?: continue
                    val src = iframe.attr("abs:src").ifEmpty { iframe.attr("abs:data-src") }
                    addUrlIfValid(src)
                }
            }

            // 2. Fallback: If no links found in tabs, look at ALL iframes
            if (serverList.isEmpty()) {
                val allIframes = doc.select("iframe")
                for (iframe in allIframes) {
                    val src = iframe.attr("abs:src").ifEmpty { iframe.attr("abs:data-src") }
                    addUrlIfValid(src)
                }
            }

            return@withContext if (serverList.isNotEmpty()) {
                Resource.Success(serverList)
            } else {
                Resource.Error("No embed links found on episode page.")
            }

        } catch (e: Exception) {
            return@withContext Resource.Error(e.message ?: "Extraction failed", e)
        }
    }

    private fun resolveRedirect(url: String): String {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", Constants.USER_AGENT)
                .build()
            client.newCall(request).execute().use { it.request.url.toString() }
        } catch (e: Exception) { url }
    }

    private fun parseMovies(doc: Document): List<Movie> {
        val movieList = mutableListOf<Movie>()
        val items = doc.select("div.ml-item")
        for (item in items) {
            val linkTag = item.selectFirst("a.ml-mask")
            val imgTag = item.selectFirst("img")
            if (linkTag != null && imgTag != null) {
                movieList.add(Movie(linkTag.attr("oldtitle"), imgTag.attr("data-original"), linkTag.attr("abs:href")))
            }
        }
        return movieList
    }

    suspend fun searchMovies(query: String): Resource<List<Movie>> = withContext(Dispatchers.IO) {
        try {
            val searchUrl = "${Constants.BASE_URL}/?s=${Uri.encode(query)}"
            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", Constants.USER_AGENT)
                .header("Cookie", getCookieHeader())
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Resource.Error("Search failed: ${response.code}")

            val html = response.body?.string() ?: return@withContext Resource.Error("Empty response")
            val doc = Jsoup.parse(html, searchUrl)

            // Search results also use "div.ml-item"
            val movieList = mutableListOf<Movie>()
            var items = doc.select("div.ml-item")
            // Fallback if structure is different
            if (items.isEmpty()) {
                items = doc.select("div.result-item")
            }

            for (item in items) {
                val linkTag = item.selectFirst("a.ml-mask") ?: item.selectFirst("a")
                val imgTag = item.selectFirst("img")

                if (linkTag != null && imgTag != null) {
                    var title = linkTag.attr("oldtitle")
                    if (title.isEmpty()) {
                        title = item.selectFirst("h2")?.text() ?: ""
                    }

                    if (title.isNotEmpty()) {
                         val link = linkTag.attr("abs:href")
                         val poster = imgTag.attr("data-original").ifEmpty { imgTag.attr("src") }
                         movieList.add(Movie(title, poster, link))
                    }
                }
            }

            return@withContext Resource.Success(movieList)

        } catch (e: Exception) {
            return@withContext Resource.Error(e.message ?: "Search Error", e)
        }
    }
}