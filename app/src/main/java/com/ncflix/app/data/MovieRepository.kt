package com.ncflix.app.data

import com.ncflix.app.model.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

class MovieRepository {

    private val baseUrl = "https://ww93.pencurimovie.bond"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"

    private val cookies = mapOf(
        "_ga" to "GA1.1.1793413717.1764381264",
        "wpdiscuz_hide_bubble_hint" to "1",
        "_ga_BS36YHXFDN" to "GS2.1.s1764381264\$o1\$g1\$t1764381686\$j60\$l0\$h0"
    )

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun searchMovies(query: String): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val searchUrl = "$baseUrl/?s=$query"
            val doc = Jsoup.connect(searchUrl).userAgent(userAgent).cookies(cookies).timeout(10000).get()
            return@withContext parseMovies(doc)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun fetchHomeData(): Pair<Movie?, List<Movie>> = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(baseUrl).userAgent(userAgent).cookies(cookies).timeout(10000).get()
            val allMovies = parseMovies(doc)
            if (allMovies.isNotEmpty()) {
                return@withContext Pair(allMovies.first(), allMovies.drop(1))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext Pair(null, emptyList())
    }

    suspend fun fetchEpisodes(seriesUrl: String): Map<String, List<Movie>> = withContext(Dispatchers.IO) {
        val seasonsMap = mutableMapOf<String, MutableList<Movie>>()

        try {
            println("NC-FLIX: Fetching Series -> $seriesUrl")
            val doc = Jsoup.connect(seriesUrl).userAgent(userAgent).cookies(cookies).timeout(10000).get()

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

    suspend fun extractAllServers(episodeUrl: String): ArrayList<String> = withContext(Dispatchers.IO) {
        val serverList = ArrayList<String>()
        // List of hosts that potentially contain the video stream (updated list)
        val videoHosts = listOf("myvidplay.com", "voe.sx", "dsvplay.com", "bigwarp.pro", "listeamed.net", "walterprettytheir.com")

        try {
            println("NC-FLIX: Visiting Episode -> $episodeUrl")
            val doc = Jsoup.connect(episodeUrl).userAgent(userAgent).cookies(cookies).header("Referer", baseUrl).get()

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

    private fun resolveRedirect(url: String): String {
        return try {
            val request = Request.Builder().url(url).header("User-Agent", userAgent).header("Referer", baseUrl).build()
            client.newCall(request).execute().use { it.request.url.toString() }
        } catch (e: Exception) { url }
    }
}