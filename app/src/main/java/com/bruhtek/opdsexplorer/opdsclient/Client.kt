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
        .path(path)
    val url = builder.build().toString()
    Log.d("OpdsClient", "Fetching path: $url, made from $feedUrl and $path")
    val res = getClient(feed).get(url)
    return res.body()
}