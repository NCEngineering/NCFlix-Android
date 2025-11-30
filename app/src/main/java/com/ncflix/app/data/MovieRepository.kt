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

    suspend fun searchMovies(query: String): Resource<List<Movie>> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "${Constants.BASE_URL}/?s=$encodedQuery"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", Constants.USER_AGENT)
                .header("Cookie", getCookieHeader())
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Resource.Error("Server Error: ${response.code}")

            val html = response.body?.string() ?: return@withContext Resource.Error("Empty response")
            val doc = Jsoup.parse(html, url)

            val movieList = parseMovies(doc)
            if (movieList.isEmpty()) {
                return@withContext Resource.Error("No results found")
            }
            return@withContext Resource.Success(movieList)

        } catch (e: Exception) {
            return@withContext Resource.Error(e.message ?: "Network Error", e)
        }
    }

    suspend fun fetchLatestSeries(): Resource<List<Movie>> = fetchList(Constants.BASE_URL + "/series/")
    suspend fun fetchLatestMovies(): Resource<List<Movie>> = fetchList(Constants.BASE_URL + "/movies/")
    suspend fun fetchMostViewed(): Resource<List<Movie>> = fetchList(Constants.BASE_URL + "/most-viewed/")

    suspend fun fetchTop10Malaysia(): Resource<List<Movie>> = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.imdb.com/search/title/?country_of_origin=MY"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", Constants.USER_AGENT)
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: throw Exception("Empty response")
            val doc = Jsoup.parse(html, url)

            val items = doc.select("li.ipc-metadata-list-summary-item")
            val movies = mutableListOf<Movie>()

            for (item in items) {
                val titleElement = item.selectFirst("h3.ipc-title__text")
                val imgElement = item.selectFirst("img.ipc-image")
                
                // Helper to find year: Find all spans, look for 4 digits
                val spans = item.select("span")
                var year = ""
                for (span in spans) {
                    if (span.text().matches(Regex("^\\d{4}$"))) {
                        year = span.text()
                        break
                    }
                }

                if (titleElement != null) {
                    val title = titleElement.text().replace(Regex("^\\d+\\.\\s+"), "") // Remove "1. " prefix
                    // Use a higher res image if available in srcset, otherwise src
                    val poster = imgElement?.attr("src") ?: ""
                    // Improve poster resolution hack (IMDb thumbnails are small)
                    val highResPoster = poster.replace(Regex("UX\\d+"), "UX600").replace(Regex("CR\\d+,\\d+,\\d+,\\d+"), "")

                    movies.add(
                        Movie(
                            title = title,
                            posterUrl = highResPoster,
                            pageLink = "search:$title",
                            description = if (year.isNotEmpty()) "Released: $year" else "Top Movie"
                        )
                    )
                }
            }

            if (movies.isNotEmpty()) {
                return@withContext Resource.Success(movies)
            }
            throw Exception("Scraping failed or no items")

        } catch (e: Exception) {
            // Fallback list
            val localTop10 = listOf(
                "Mat Kilau", "Polis Evo 3", "Munafik 2", "Mechamato Movie",
                "Hantu Kak Limah", "Ejen Ali: The Movie", "Abang Long Fadil 3",
                "Paskal", "BoBoiBoy Movie 2", "Sheriff: Narko Integriti"
            )
            
            val movies = localTop10.map { title ->
                Movie(
                    title = title, 
                    posterUrl = "", 
                    pageLink = "search:$title",
                    description = "Trending in Malaysia"
                )
            }
            return@withContext Resource.Success(movies)
        }
    }

    private suspend fun fetchList(url: String): Resource<List<Movie>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", Constants.USER_AGENT)
                .header("Cookie", getCookieHeader())
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Resource.Error("Server Error: ${response.code}")

            val html = response.body?.string() ?: return@withContext Resource.Error("Empty response")
            val doc = Jsoup.parse(html, url)

            val list = parseMovies(doc)
            return@withContext Resource.Success(list)
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
                // It might be a movie (no seasons found). Return as a single item.
                val title = doc.selectFirst("h1.entry-title")?.text() ?: "Movie"
                val singleMovie = Movie(title, "", seriesUrl, seasonTitle = "Movie")
                seasonsMap["Movie"] = mutableListOf(singleMovie)
                return@withContext Resource.Success(seasonsMap)
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
}