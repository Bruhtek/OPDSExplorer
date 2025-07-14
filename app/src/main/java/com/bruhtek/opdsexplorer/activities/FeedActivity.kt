package com.bruhtek.opdsexplorer.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.bruhtek.opdsexplorer.components.Item
import com.bruhtek.opdsexplorer.opdsclient.OpdsEntry
import com.bruhtek.opdsexplorer.opdsclient.OpdsFeed
import com.bruhtek.opdsexplorer.opdsclient.fetchPath
import com.bruhtek.opdsexplorer.proto.FeedProto
import com.bruhtek.opdsexplorer.ui.theme.OPDSExplorerTheme

const val EXTRA_FEED_PROTO_BYTES = "feed_proto_bytes"
const val EXTRA_FEED_PATH = "feed_path"
const val EXTRA_FEED_PREVIOUS = "feed_previous"

class FeedActivity : ComponentActivity() {
    private lateinit var feedProto: FeedProto
    private lateinit var feedPath: String
    private lateinit var feedPrevious: Array<String>

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val feedBytes = intent.getByteArrayExtra(EXTRA_FEED_PROTO_BYTES) ?: return;
        feedProto = FeedProto.parseFrom(feedBytes)

        feedPath = intent.getStringExtra(EXTRA_FEED_PATH) ?: return;
        feedPrevious = intent.getStringArrayExtra(EXTRA_FEED_PREVIOUS) ?: emptyArray();

        setContent {
            OPDSExplorerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    feedPrevious.lastOrNull() ?: feedProto.title
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    finish()
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        )
                    }

                ) {
                    Surface(
                        modifier = Modifier
                            .padding(it)
                            .padding(16.dp)
                    ) {
                        FeedContent(feedPath, feedPrevious)
                    }
                }
            }
        }
    }

    @Composable
    fun FeedContent(path: String, feedPrevious: Array<String>) {
        var opdsFeed by remember { mutableStateOf<OpdsFeed?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        val scope = rememberCoroutineScope()

        val context = LocalContext.current

        LaunchedEffect(Unit) {
            isLoading = true
            try {
                opdsFeed = fetchPath(feedProto, path)
            } catch (e: Exception) {
                Log.e("FeedActivity", "Error fetching feed: ${e.message}", e)
                opdsFeed = null
            } finally {
                isLoading = false
            }
        }

        if (isLoading) {
            val loadingTitle = feedPrevious.lastOrNull() ?: feedProto.title

            Column() {
                Text(
                    text = feedProto.title
                )
                Text(
                    text = "Loading data: $loadingTitle...",
                )
            }
        } else {
            opdsFeed?.let { data ->
                if (data.entry.isEmpty()) {
                    Text(text = "No entries found in this feed.")
                } else {
                    EntriesDisplay(data)
                }
            } ?: run {
                Text(text = "Failed to load feed.")
            }
        }
    }

    @SuppressLint("UnusedBoxWithConstraintsScope")
    @Composable
    fun EntriesDisplay(feed: OpdsFeed) {
        val context = LocalContext.current
        var currentPage by remember { mutableIntStateOf(0) }
        var itemHeight by remember { mutableIntStateOf(-1) }
        var offsetX by remember { mutableStateOf(0f) }

        if (itemHeight == -1) {
            MeasureItem { placeable ->
                itemHeight = placeable.height
            }
        } else {

            Column() {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val itemPadding = 8.dp;
                    val itemPaddingPx = with(LocalDensity.current) { itemPadding.toPx() }
                    val maxHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

                    var itemsPerPage = 0;
                    if (itemHeight > 0) {
                        itemsPerPage =
                            ((maxHeightPx - itemPaddingPx) / (itemHeight + itemPaddingPx)).toInt()
                    }
                    val pageCount = (feed.entry.size + itemsPerPage - 1) / itemsPerPage

                    val startIndex = currentPage * itemsPerPage
                    var endIndex = startIndex + itemsPerPage
                    if (endIndex > feed.entry.size) {
                        endIndex = feed.entry.size
                    }


                    Log.d(
                        "FeedActivity",
                        "Current page: $currentPage, Items per page: $itemsPerPage, Start index: $startIndex, End index: $endIndex"
                    )

                    fun onSwipeRight() {
                        if (currentPage > 0) {
                            Log.d("FeedActivity", "Swiping left, current page: $currentPage")
                            currentPage--
                        }
                    }

                    fun onSwipeLeft() {
                        if (currentPage < pageCount - 1) {
                            Log.d("FeedActivity", "Swiping right, current page: $currentPage")
                            currentPage++
                        }
                    }

                    Column(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = {
                                        when {
                                            offsetX > 100 -> {
                                                onSwipeRight()
                                                offsetX = 0f // Reset position
                                            }

                                            offsetX < -100 -> {
                                                onSwipeLeft()
                                                offsetX = 0f // Reset position
                                            }

                                            else -> {
                                                offsetX = 0f // Reset if threshold not met
                                            }
                                        }
                                    }
                                ) { change, dragAmount ->
                                    offsetX += dragAmount.x
                                }
                            }, verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(itemPadding)
                        ) {
                            for (i in startIndex until endIndex) {
                                val entry = feed.entry[i]
                                ItemWithHrefs(
                                    entry = entry,
                                )
                            }
                        }
                        Text(
                            text = "Total entries: ${feed.entry.size} | Page ${currentPage + 1} of $pageCount",
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ItemWithHrefs(entry: OpdsEntry) {
        val href = entry.linkUrl();
        if (href == null) {
            Item(
                entry,
                feedProto
            )
        } else {
            val context = LocalContext.current

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = {
                        Log.d("FeedItem", "Clicked on feed: ${feedProto.title}")
                        val intent = Intent(context, FeedActivity::class.java)
                        intent.putExtra(EXTRA_FEED_PROTO_BYTES, feedProto.toByteArray())
                        intent.putExtra(EXTRA_FEED_PATH, href)
                        intent.putExtra(
                            EXTRA_FEED_PREVIOUS,
                            feedPrevious.plus(entry.title)
                        )
                        context.startActivity(intent)
                    })
            ) {
                Item(
                    entry,
                    feedProto
                )
            }
        }
    }

    @Composable
    fun MeasureItem(
        onMeasured: (Placeable) -> Unit
    ) {
        Layout(
            modifier = Modifier.size(DpSize.Zero),
            content = {
                Item(
                    title = "Placeholder",
                    subtitle = "This is a placeholder item for measuring layout",
                )
            }
        ) { measurable, _ ->
            val placeable = measurable.first().measure(Constraints())
            onMeasured(placeable)

            layout(0, 0) {
                // draw nothing
            }
        }
    }

}


