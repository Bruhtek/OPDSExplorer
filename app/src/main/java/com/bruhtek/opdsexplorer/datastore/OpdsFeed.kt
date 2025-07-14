package com.bruhtek.opdsexplorer.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.bruhtek.opdsexplorer.proto.AuthTypeProto
import com.bruhtek.opdsexplorer.proto.FeedListProto
import com.bruhtek.opdsexplorer.proto.FeedProto
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream

object FeedListProtoSerializer : Serializer<FeedListProto> {
    override val defaultValue: FeedListProto
        get() = FeedListProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): FeedListProto {
        try {
            return FeedListProto.parseFrom(input)
        } catch (exception: Exception) {
            throw Exception("Error reading FeedListProto from input stream", exception)
        }
    }

    override suspend fun writeTo(t: FeedListProto, output: OutputStream) {
        try {
            t.writeTo(output)
        } catch (exception: Exception) {
            throw Exception("Error writing FeedListProto to output stream", exception)
        }
    }
}

private const val FEED_LIST_STORE_NAME = "feed_list.pb"
val Context.feedListStore: DataStore<FeedListProto> by dataStore(
    fileName = FEED_LIST_STORE_NAME,
    serializer = FeedListProtoSerializer,
)

suspend fun Context.initializeFeedListStore() {
    val current = feedListStore.data.first()
    if (current.initialized) {
        return;
    }

    val initialFeedList = FeedListProto.newBuilder()
        .setInitialized(true)
        .clearFeeds()
        .addFeeds(
            FeedProto.newBuilder()
                .setUrl("https://m.gutenberg.org/ebooks.opds/")
                .setAuthType(AuthTypeProto.NONE)
                .setTitle("Project Gutenberg")
                .setSubtitle("Free eBooks since 1971.")
                .setImageUrl("https://www.gutenberg.org/gutenberg/favicon.ico")
        )
        .build()

    Log.d("Feeds", "Initializing feed list with: ${initialFeedList.feedsList.size} feeds")
    feedListStore.updateData { initialFeedList }
}

suspend fun removeFeed(context: Context, feed: FeedProto) {
    val current = context.feedListStore.data.first()
    val updatedFeeds = current.feedsList.filter { it != feed }

    val updatedFeedList = current.toBuilder()
        .clearFeeds()
        .addAllFeeds(updatedFeeds)
        .build()

    context.feedListStore.updateData { updatedFeedList }
    Log.d("Feeds", "Removed feed: ${feed.title}. Remaining feeds: ${updatedFeeds.size}")
}

suspend fun updateFeed(context: Context, old: FeedProto, new: FeedProto) {
    val current = context.feedListStore.data.first()
    val updatedFeeds = current.feedsList.map { if (it == old) new else it }

    val updatedFeedList = current.toBuilder()
        .clearFeeds()
        .addAllFeeds(updatedFeeds)
        .build()

    context.feedListStore.updateData { updatedFeedList }
    Log.d("Feeds", "Updated feed: ${old.title} to ${new.title}. Total feeds: ${updatedFeeds.size}")
}

suspend fun addFeed(context: Context, feed: FeedProto) {
    val current = context.feedListStore.data.first()
    val updatedFeedList = current.toBuilder()
        .addFeeds(feed)
        .build()

    context.feedListStore.updateData { updatedFeedList }
    Log.d("Feeds", "Added feed: ${feed.title}. Total feeds: ${updatedFeedList.feedsList.size}")
}