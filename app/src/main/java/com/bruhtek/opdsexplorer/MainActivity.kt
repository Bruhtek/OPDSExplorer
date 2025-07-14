package com.bruhtek.opdsexplorer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bruhtek.opdsexplorer.activities.AddNewFeedActivity
import com.bruhtek.opdsexplorer.activities.EXTRA_FEED_PATH
import com.bruhtek.opdsexplorer.activities.EXTRA_FEED_PROTO_BYTES
import com.bruhtek.opdsexplorer.activities.EXTRA_FEET_TO_EDIT_BYTES
import com.bruhtek.opdsexplorer.activities.FeedActivity
import com.bruhtek.opdsexplorer.components.Item
import com.bruhtek.opdsexplorer.datastore.feedListStore
import com.bruhtek.opdsexplorer.datastore.initializeFeedListStore
import com.bruhtek.opdsexplorer.datastore.removeFeed
import com.bruhtek.opdsexplorer.proto.FeedListProto
import com.bruhtek.opdsexplorer.proto.FeedProto
import com.bruhtek.opdsexplorer.ui.theme.OPDSExplorerTheme
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        runBlocking { this@MainActivity.initializeFeedListStore() }


        setContent {
            var editMode by remember { mutableStateOf(false) }

            OPDSExplorerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        MainTopAppBar(
                            editModeToggle = {
                                editMode = !editMode
                            },
                            editMode = editMode
                        )
                    }
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(it)
                            .padding(16.dp)
                    ) {
                        OpdsFeedListScreen(this, editMode)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    editModeToggle: () -> Unit,
    editMode: Boolean,
) {
    val context = LocalContext.current

    TopAppBar(
        title = {
            Text(
                "OPDS Explorer" + if (editMode) " (Edit Mode)" else "",
            )
        },
        actions = {
            IconButton(
                onClick = {
                    val intent = Intent(context, AddNewFeedActivity::class.java)
                    context.startActivity(intent)
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = "Add Feed",
                )
            }
            IconButton(
                onClick = {
                    editModeToggle()
                }
            ) {
                if (editMode) {
                    Icon(
                        imageVector = Icons.Outlined.Done,
                        contentDescription = "View Feeds",
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Feeds",
                    )
                }
            }
        }
    )
}

@Composable
fun OpdsFeedListScreen(context: Context, editMode: Boolean) {
    val data =
        context.feedListStore.data.collectAsStateWithLifecycle(initialValue = FeedListProto.getDefaultInstance())

    Log.d("Feeds", "Feeds: ${data.value.feedsList.size}")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)

    ) {
        data.value.feedsList.forEach { feed ->
            FeedItem(feed, editMode)
        }
    }
}

@Composable
fun FeedItem(feed: FeedProto, editMode: Boolean) {
    val context = LocalContext.current

    if (!editMode) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = {
                    Log.d("FeedItem", "Clicked on feed: ${feed.title}")
                    val intent = Intent(context, FeedActivity::class.java)
                    intent.putExtra(EXTRA_FEED_PROTO_BYTES, feed.toByteArray())
                    intent.putExtra(EXTRA_FEED_PATH, "/")
                    context.startActivity(intent)
                })
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Item(
                    feed
                )
            }
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Item(
                feed
            )
            Column() {
                IconButton(
                    onClick = {
                        val intent = Intent(context, AddNewFeedActivity::class.java)
                        intent.putExtra(EXTRA_FEET_TO_EDIT_BYTES, feed.toByteArray())
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Feed",
                    )
                }
                IconButton(
                    onClick = {
                        Log.d("FeedItem", "Deleting feed: ${feed.title}")
                        runBlocking {
                            removeFeed(context, feed)
                        }
                        Log.d("FeedItem", "Feed deleted: ${feed.title}")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Feed",
                    )
                }
            }
        }

    }
}