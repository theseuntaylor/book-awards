package com.theseuntaylor.bookawards.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OpenLibraryTest {

    @Test
    fun parsesTheFirstSearchResult() {
        val json = """{"numFound": 2, "docs": [
            {"key": "/works/OL123W", "cover_i": 987, "first_publish_year": 2023},
            {"key": "/works/OL456W"}
        ]}"""

        val info = parseSearchResult(json)

        assertEquals(BookInfo("/works/OL123W", 987, 2023), info)
        assertEquals("https://covers.openlibrary.org/b/id/987-L.jpg", info?.coverUrl)
        assertEquals("https://openlibrary.org/works/OL123W", info?.openLibraryUrl)
    }

    @Test
    fun searchResultWithoutCoverHasNoCoverUrl() {
        assertNull(parseSearchResult("""{"docs": [{"key": "/works/OL1W"}]}""")?.coverUrl)
    }

    @Test
    fun emptySearchReturnsNull() {
        assertNull(parseSearchResult("""{"numFound": 0, "docs": []}"""))
    }

    @Test
    fun readsPlainStringDescription() {
        assertEquals("A novel.", parseDescription("""{"description": "A novel.\n"}"""))
    }

    @Test
    fun readsTypedTextDescription() {
        assertEquals("A novel.", parseDescription("""{"description": {"type": "/type/text", "value": "A novel."}}"""))
    }

    @Test
    fun missingOrBlankDescriptionIsNull() {
        assertNull(parseDescription("""{"title": "Untitled"}"""))
        assertNull(parseDescription("""{"description": "  "}"""))
    }
}
