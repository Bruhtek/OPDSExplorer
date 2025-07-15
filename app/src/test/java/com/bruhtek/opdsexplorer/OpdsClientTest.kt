package com.bruhtek.opdsexplorer

import com.bruhtek.opdsexplorer.opdsclient.OpdsFeed
import com.bruhtek.opdsexplorer.opdsclient.fetchNextPage
import com.bruhtek.opdsexplorer.opdsclient.fetchPath
import com.bruhtek.opdsexplorer.opdsclient.fetchRoot
import com.bruhtek.opdsexplorer.proto.FeedProto
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(RobolectricTestRunner::class)
class OpdsClientTest {
    val feed: FeedProto = FeedProto.newBuilder()
        .setUrl("https://m.gutenberg.org/ebooks.opds/")
        .setTitle("Project Gutenberg")
        .build()


    @Test
    fun properly_parse_opds_feed() {
        var feedData: OpdsFeed?
        runBlocking {
            feedData = fetchRoot(feed)
        }

        assert(feedData != null)
    }


    @Test
    fun properly_parse_content_text() {
        var feedData: OpdsFeed?
        runBlocking {
            feedData = fetchPath(feed, "/ebooks/2641.opds")
        }
        assert(feedData != null)
    }

    @Test
    fun properly_parse_next_page() {
        var updatedFeed: OpdsFeed?;
        var feedData: OpdsFeed?
        runBlocking {
            feedData = fetchPath(feed, "/ebooks/search.opds/?sort_order=downloads")

            updatedFeed = fetchNextPage(feed, feedData)
        }
        assert(feedData != null)
        assert(updatedFeed != null)
        println("Starting size: ${feedData!!.entry.size}, New size: ${updatedFeed!!.entry.size}")
        assert(updatedFeed.entry.containsAll(feedData.entry))
    }
}