package pl.croolyy.timer

import org.junit.Assert.*
import org.junit.Test
import pl.croolyy.timer.data.OpenLibraryClient
import pl.croolyy.timer.data.CoverCandidate
import kotlinx.coroutines.runBlocking
import java.nio.file.Files

class OpenLibraryClientTest {
    @Test fun userSearchAndDownloadedCoverAreCachedWithoutAnyAccount() = runBlocking {
        val directory = Files.createTempDirectory("covers-test").toFile()
        try {
            val requests = mutableListOf<Pair<String, Int>>()
            val image = byteArrayOf(1, 2, 3, 4)
            val client = OpenLibraryClient(directory) { address, limit ->
                requests += address to limit
                if (address.contains("search.json")) """{"docs":[{"title":"Book","cover_i":123}]}""".toByteArray() else image
            }
            val result = client.search("Book").single()
            assertEquals(result, client.search("  Book  ").single())
            assertEquals(1, requests.size)
            val saved = client.image(result, large = true)
            assertArrayEquals(image, saved.readBytes())
            assertEquals(saved, client.image(result, large = true))
            assertEquals(2, requests.size)
            assertEquals(2 * 1024 * 1024, requests[0].second)
            assertEquals(10 * 1024 * 1024, requests[1].second)
        } finally { directory.deleteRecursively() }
        Unit
    }
    @Test fun onlyUsableUniqueCoversAreOfferedAndOptionalMetadataCanBeMissing() {
        val results = OpenLibraryClient.parseSearch("""{"docs":[
            {"title":"Mały Książę","author_name":["Antoine de Saint-Exupéry"],"cover_i":123,"first_publish_year":1943},
            {"title":"To samo wydanie","cover_i":123},
            {"title":"Bez okładki"},{"title":"Zły identyfikator","cover_i":-9},
            {"title":"Inne wydanie","cover_i":456},{"cover_i":789}
        ]}""")
        assertEquals(2, results.size)
        assertEquals(123L, results[0].coverId)
        assertEquals("Antoine de Saint-Exupéry", results[0].author)
        assertEquals(1943, results[0].year)
        assertEquals("", results[1].author); assertNull(results[1].year)
    }
    @Test fun queriesAreEncodedAndCannotChangeTheRequestHostOrParameters() {
        val url = OpenLibraryClient.searchUrl("  Mały Książę &limit=999  ")
        assertTrue(url.startsWith("https://openlibrary.org/search.json?title=Ma%C5%82y+Ksi%C4%85%C5%BC%C4%99+%26limit%3D999&"))
        assertTrue(url.endsWith("&limit=24&lang=pl"))
        assertEquals("https://covers.openlibrary.org/b/id/123-L.jpg?default=false", OpenLibraryClient.coverUrl(123, true))
        assertThrows(IllegalArgumentException::class.java) { OpenLibraryClient.coverUrl(-1, false) }
    }
    @Test fun suggestionsAreLimitedAndAnEmptySearchIsValid() {
        assertTrue(OpenLibraryClient.parseSearch("""{"docs":[]}""").isEmpty())
        val docs = (1..30).joinToString(",") { """{"title":"Book $it","cover_i":$it}""" }
        assertEquals(12, OpenLibraryClient.parseSearch("""{"docs":[$docs]}""").size)
    }
}
