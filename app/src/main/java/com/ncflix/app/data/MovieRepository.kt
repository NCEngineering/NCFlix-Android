package com.ncflix.app.data

import com.ncflix.app.model.Movie
import com.ncflix.app.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

/**
 * Repository responsible for fetching and parsing movie data from an external source.
 *
 * This class handles network operations including searching for movies, fetching home page data,
 * retrieving episode lists for series, and extracting video server URLs. It uses Jsoup for HTML
 * parsing and OkHttp for network requests.
 *
 * The repository acts as a data abstraction layer for the application.
 */
class MovieRepository {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Searches for movies matching the given query string.
     *
     * @param query The search keyword or phrase.
     * @return A list of [Movie] objects matching the query. Returns an empty list if an error occurs.
     */
    suspend fun searchMovies(query: String): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val searchUrl = "${Constants.BASE_URL}/?s=$query"
            val doc = Jsoup.connect(searchUrl)
                .userAgent(Constants.USER_AGENT)
                .cookies(Constants.COOKIES)
                .timeout(10000)
                .get()
            return@withContext parseMovies(doc)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    /**
     * Fetches the data for the home screen.
     *
     * This typically includes a featured movie and a list of other movies.
     *
     * @return A [Pair] where the first element is the featured [Movie] (nullable) and the second
     * element is a [List] of [Movie] objects. Returns a Pair with (null, emptyList) if an error occurs.
     */
    suspend fun fetchHomeData(): Pair<Movie?, List<Movie>> = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(Constants.BASE_URL)
                .userAgent(Constants.USER_AGENT)
                .cookies(Constants.COOKIES)
                .timeout(10000)
                .get()
            val allMovies = parseMovies(doc)
            if (allMovies.isNotEmpty()) {
                return@withContext Pair(allMovies.first(), allMovies.drop(1))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext Pair(null, emptyList())
    }

    /**
     * Fetches the episodes of a series from its URL.
     *
     * @param seriesUrl The URL of the series page.
     * @return A [Map] where the key is the season title (e.g., "Season 1") and the value is a
     * [List] of [Movie] objects representing the episodes. Returns an empty map on error.
     */
    suspend fun fetchEpisodes(seriesUrl: String): Map<String, List<Movie>> = withContext(Dispatchers.IO) {
        val seasonsMap = mutableMapOf<String, MutableList<Movie>>()

        try {
            println("NC-FLIX: Fetching Series -> $seriesUrl")
            val doc = Jsoup.connect(seriesUrl)
                .userAgent(Constants.USER_AGENT)
                .cookies(Constants.COOKIES)
                .timeout(10000)
                .get()

            val seasonDivs = doc.select("div.tvseason")

            for (seasonDiv in seasonDivs) {
                val seasonTitle = seasonDiv.selectFirst(".les-title strong")?.text() ?: "Unknown Season"

                val links = seasonDiv.select("div.les-content a")
                val episodes = mutableListOf<Movie>()

                for (link in links) {
                    val title = link.text().trim()
                    val url = link.attr("href")
                    if (url.isNotEmpty()) {
                        episodes.add(Movie(title, "", url, seasonTitle = seasonTitle))
                    }
                }

                if (episodes.isNotEmpty()) {
                    seasonsMap[seasonTitle] = episodes
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext seasonsMap
    }

    /**
     * Extracts available video server URLs for a specific episode.
     *
     * This method parses the episode page to find embedded video players and validates them against
     * a list of known video hosts. It also handles redirects for certain hosts.
     *
     * @param episodeUrl The URL of the episode page.
     * @return An [ArrayList] of strings containing the validated video server URLs.
     */
    suspend fun extractAllServers(episodeUrl: String): ArrayList<String> = withContext(Dispatchers.IO) {
        val serverList = ArrayList<String>()
        // List of hosts that potentially contain the video stream (updated list)
        val videoHosts = listOf("myvidplay.com", "voe.sx", "dsvplay.com", "bigwarp.pro", "listeamed.net", "walterprettytheir.com")

        try {
            println("NC-FLIX: Visiting Episode -> $episodeUrl")
            val doc = Jsoup.connect(episodeUrl)
                .userAgent(Constants.USER_AGENT)
                .cookies(Constants.COOKIES)
                .header("Referer", Constants.BASE_URL)
                .get()

            val iframes = doc.select("div#player2 iframe[data-src]")

            for (iframe in iframes) {
                var embedUrl = iframe.attr("data-src")
                if (embedUrl.isEmpty()) continue

                // 1. Resolve redirectors (like dsvplay)
                if (embedUrl.contains("dsvplay")) {
                    println("NC-FLIX: Resolving redirect for -> $embedUrl")
                    embedUrl = resolveRedirect(embedUrl)
                }

                // 2. Check if the URL belongs to a known working video host
                val isKnownHost = videoHosts.any { embedUrl.contains(it, ignoreCase = true) }

                if (isKnownHost) {
                    if (!serverList.contains(embedUrl)) {
                        serverList.add(embedUrl)
                        println("NC-FLIX: Server Added -> $embedUrl")
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        println("NC-FLIX: Found ${serverList.size} working servers.")
        return@withContext serverList
    }

    /**
     * Parses a Jsoup Document to extract a list of movies.
     *
     * @param doc The Jsoup [Document] representing the HTML page.
     * @return A [List] of [Movie] objects extracted from the document.
     */
    private fun parseMovies(doc: Document): List<Movie> {
        val movieList = mutableListOf<Movie>()
        val items = doc.select("div.ml-item")
        for (item in items) {
            val linkTag = item.selectFirst("a.ml-mask")
            val imgTag = item.selectFirst("img")
            if (linkTag != null && imgTag != null) {
                movieList.add(Movie(linkTag.attr("oldtitle"), imgTag.attr("data-original"), linkTag.attr("href")))
            }
        }
        return movieList
    }

    /**
     * Resolves the final URL after following redirects.
     *
     * @param url The initial URL.
     * @return The resolved URL as a String, or the original URL if resolution fails.
     */
    private fun resolveRedirect(url: String): String {
        return try {
            val request = Request.Builder().url(url)
                .header("User-Agent", Constants.USER_AGENT)
                .header("Referer", Constants.BASE_URL)
                .build()
            client.newCall(request).execute().use { it.request.url.toString() }
        } catch (e: Exception) { url }
    }
}
