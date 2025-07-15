package com.bruhtek.opdsexplorer.opdsclient

import android.net.Uri
import android.util.Log
import com.bruhtek.opdsexplorer.proto.FeedProto
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.Url
import io.ktor.http.authority

suspend fun fetchRoot(feed: FeedProto): OpdsFeed {
    val res = getClient(feed).get(feed.url)
    return res.body()
}

suspend fun fetchPath(feed: FeedProto, path: String): OpdsFeed {
    val feedUrl = feed.url
    if (path.isEmpty() || path == "/") {
        Log.d("OpdsClient", "Fetching root feed: $feedUrl")
        return fetchRoot(feed)
    }

    val builder = Uri.Builder();
    builder.scheme(Url(feedUrl).protocol.name)
        .authority(Url(feedUrl).authority)
        .encodedPath(path)
    val url = builder.build().toString()
    Log.d("OpdsClient", "Fetching path: $url, made from $feedUrl and $path")
    val res = getClient(feed).get(url)
    return res.body()
}

suspend fun fetchNextPage(feed: FeedProto, opdsFeed: OpdsFeed): OpdsFeed {
    val next = opdsFeed.nextPageUrl()
    if (next.isNullOrEmpty()) {
        Log.d("OpdsClient", "No next page to fetch for path: ${opdsFeed.selfPage()}")
        return opdsFeed
    }
    val decoded = Uri.decode(next)

    val nextPage = fetchPath(feed, decoded)

    val newFeed = OpdsFeed(
        id = opdsFeed.id,
        title = opdsFeed.title,
        subtitle = opdsFeed.subtitle,
        updated = opdsFeed.updated,
        author = opdsFeed.author,
        icon = opdsFeed.icon,
        link = nextPage.link,
        entry = opdsFeed.entry + nextPage.entry,
    )

    return newFeed
}