package com.bruhtek.opdsexplorer.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.bruhtek.opdsexplorer.R
import com.bruhtek.opdsexplorer.opdsclient.OpdsEntry
import com.bruhtek.opdsexplorer.proto.AuthTypeProto
import com.bruhtek.opdsexplorer.proto.FeedProto
import com.bruhtek.opdsexplorer.ui.theme.OPDSExplorerTheme
import com.bruhtek.opdsexplorer.utils.getHostUrl
import kotlin.io.encoding.Base64

@Composable
fun Item(
    title: String,
    subtitle: String,
    url: Any? = null,
    placeholder: Int = R.drawable.books
) {
    Row(
    ) {
        Box(
            modifier = Modifier.Companion.border(
                2.dp,
                Color.Companion.Black,
                RoundedCornerShape(8.dp)
            )
        ) {
            AsyncImage(
                model = url,
                contentDescription = "Item image",
                placeholder = painterResource(id = placeholder),
                error = painterResource(id = placeholder),
                contentScale = ContentScale.Companion.Fit,
                modifier = Modifier.Companion.size(100.dp)
            )
        }
        Column(
            modifier = Modifier.Companion.padding(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = subtitle,
            )
        }
    }
}

@Composable
fun Item(
    feed: FeedProto,
    placeholder: Int = R.drawable.books
) {
    fun iconUrl(): String {
        if (feed.imageUrl.isNullOrEmpty()) {
            return "";
        }

        if (feed.imageUrl.startsWith("/")) {
            return getHostUrl(feed.url) + feed.imageUrl
        }

        return feed.imageUrl;
    }

    Item(
        title = feed.title,
        subtitle = feed.subtitle,
        url = iconUrl(),
        placeholder = placeholder,
    )
}

@Composable
fun Item(
    entry: OpdsEntry,
    feed: FeedProto,
    placeholder: Int = R.drawable.books
) {
    val builder = ImageRequest.Builder(LocalContext.current)
        .data(entry.thumbnailUrl(feed.url))
        .diskCachePolicy(CachePolicy.ENABLED)


    if (feed.authType == AuthTypeProto.BASIC) {
        builder.httpHeaders(
            NetworkHeaders.Builder()
                .add(
                    "Authorization",
                    "Basic ${
                        Base64.encode(
                            "${feed.username}:${feed.password}".toByteArray()
                        )
                    }"
                )
                .build()
        );
    }

    val request = builder.build()

    Item(
        title = entry.title,
        subtitle = entry.subtitle(),
        url = request,
    )
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    OPDSExplorerTheme {
        Item(
            title = "Android", subtitle = "Compose is awesome!",
        )
    }
}