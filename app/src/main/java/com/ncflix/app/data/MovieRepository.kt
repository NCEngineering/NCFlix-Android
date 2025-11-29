package com.ncflix.app.data

import com.ncflix.app.di.NetworkClient
import com.ncflix.app.model.Movie
import com.ncflix.app.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.regex.Pattern

class MovieRepository {

    private val baseUrl = "https://ww93.pencurimovie.bond"
    private val client = NetworkClient.client

    // Function 1: Get the Homepage Data (Hero + Trending)
    suspend fun fetchHomeData(): Resource<Pair<Movie?, List<Movie>>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(baseUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Resource.Error("Server Error: ${response.code}")

            val html = response.body?.string() ?: return@withContext Resource.Error("Empty response")
            
            val doc = Jsoup.parse(html, baseUrl)
            val items = doc.select("div.ml-item")

            val trendingList = mutableListOf<Movie>()
            var heroMovie: Movie? = null

            for ((index, item) in items.withIndex()) {
                val linkTag = item.selectFirst("a.ml-mask")
                val imgTag = item.selectFirst("img")

                if (linkTag != null && imgTag != null) {
                    val title = linkTag.attr("oldtitle")
                    val link = linkTag.attr("abs:href")
                    val poster = imgTag.attr("data-original")

                    val movie = Movie(title, poster, link)

                    if (index == 0) {
                        heroMovie = movie
                    } else {
                        trendingList.add(movie)
                    }
                }
            }
            return@withContext Resource.Success(Pair(heroMovie, trendingList))

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Resource.Error(e.message ?: "Unknown Error", e)
        }
    }

    // Function 2: Fetch Episodes for TV Series
    suspend fun fetchEpisodes(seriesUrl: String): Resource<Map<String, List<Movie>>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(seriesUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Resource.Error("Failed to load episodes: ${response.code}")
            
            val html = response.body?.string() ?: return@withContext Resource.Error("Empty response")

            val doc = Jsoup.parse(html, seriesUrl)
            val seasonsMap = mutableMapOf<String, MutableList<Movie>>()

            val episodeLinks = doc.select("div.les-content a, ul.episodes a")
            val episodeList = mutableListOf<Movie>()
            
            for (link in episodeLinks) {
                val title = link.text()
                val href = link.attr("abs:href")
                if (href.isNotEmpty()) {
                    episodeList.add(Movie(title, "", href))
                }
            }

            if (episodeList.isNotEmpty()) {
                seasonsMap["Season 1"] = episodeList
            }

            if (seasonsMap.isEmpty()) {
                return@withContext Resource.Error("No episodes found")
            }

            return@withContext Resource.Success(seasonsMap)

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Resource.Error(e.message ?: "Unknown Error", e)
        }
    }

    // Function 3: Extract Stream URL
    suspend fun extractStreamUrl(episodeUrl: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val requestPage = Request.Builder()
                .url(episodeUrl)
                .header("Referer", baseUrl)
                .build()
            
            val responsePage = client.newCall(requestPage).execute()
            val pageHtml = responsePage.body?.string() ?: return@withContext Resource.Error("Failed to load page")
            val doc = Jsoup.parse(pageHtml, episodeUrl)

            val iframe = doc.selectFirst("div#tab1 iframe")
            val embedUrl = iframe?.attr("abs:src") ?: return@withContext Resource.Error("No video source found")

            println("NC-FLIX: Found Embed -> $embedUrl")

            val requestEmbed = Request.Builder()
                .url(embedUrl)
                .header("Referer", baseUrl)
                .build()

            val responseEmbed = client.newCall(requestEmbed).execute()
            val html = responseEmbed.body?.string() ?: ""

            val regex = Pattern.compile("""file\s*:\s*["']([^"']+\.m3u8)["']""")
            val matcher = regex.matcher(html)

            if (matcher.find()) {
                return@withContext Resource.Success(matcher.group(1)!!)
            } else {
                return@withContext Resource.Error("Video file not found in source")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Resource.Error(e.message ?: "Unknown Error", e)
        }
    }
}