package com.bruhtek.opdsexplorer.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bruhtek.opdsexplorer.datastore.addFeed
import com.bruhtek.opdsexplorer.datastore.updateFeed
import com.bruhtek.opdsexplorer.opdsclient.OpdsFeed
import com.bruhtek.opdsexplorer.opdsclient.fetchRoot
import com.bruhtek.opdsexplorer.proto.AuthTypeProto
import com.bruhtek.opdsexplorer.proto.FeedProto
import com.bruhtek.opdsexplorer.ui.theme.OPDSExplorerTheme
import io.ktor.client.call.NoTransformationFoundException
import kotlinx.coroutines.launch

const val EXTRA_FEET_TO_EDIT_BYTES = "feed_proto_to_edit_bytes"

class AddNewFeedActivity : ComponentActivity() {
    private var feed: FeedProto? = null;

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val feedBytes = intent.getByteArrayExtra(EXTRA_FEET_TO_EDIT_BYTES);
        if (feedBytes != null) {
            feed = FeedProto.parseFrom(feedBytes)
        }


        setContent {
            OPDSExplorerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Add New Feed") },
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
                        AddFeedScreen(
                            feed,
                            onFinish = {
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

}

@Composable
fun AddFeedScreen(
    defaultFeed: FeedProto? = null,
    onFinish: () -> Unit = {},
) {
    val context = LocalContext.current;

    var feedUrl by rememberSaveable { mutableStateOf(defaultFeed?.url ?: "") }

    var selectedAuthIndex by remember { mutableIntStateOf(defaultFeed?.authType?.number ?: 0) }
    fun onAuthSelected(index: Int) {
        selectedAuthIndex = index
    }

    var authUsername by rememberSaveable { mutableStateOf(defaultFeed?.username ?: "") }
    var authPassword by rememberSaveable { mutableStateOf(defaultFeed?.password ?: "") }

    val coroutineScope = rememberCoroutineScope()

    fun gatherFeedData(): FeedProto {
        return FeedProto.newBuilder()
            .setUrl(feedUrl)
            .setAuthType(AuthTypeProto.forNumber(selectedAuthIndex))
            .setUsername(authUsername)
            .setPassword(authPassword)
            .build()
    }

    var isFeedTested by remember { mutableStateOf<OpdsFeed?>(null) }
    var feedTestError by remember { mutableStateOf<String?>(null) }

    fun testFeed() {
        val feed = gatherFeedData()
        isFeedTested = null;
        feedTestError = null;
        coroutineScope.launch {
            try {
                val data = fetchRoot(feed)
                // Handle the fetched data, e.g., show it in a list or log it
                isFeedTested = data;
            } catch (e: NoTransformationFoundException) {
                feedTestError = "Error fetching feed"
            } catch (e: Exception) {
                feedTestError = "Error fetching feed"
            }
        }
    }

    fun saveFeed() {
        if (isFeedTested == null) {
            return;
        }
        val feed = FeedProto.newBuilder()
            .setUrl(feedUrl)
            .setAuthType(AuthTypeProto.forNumber(selectedAuthIndex))
            .setUsername(authUsername)
            .setPassword(authPassword)
            .setTitle(isFeedTested?.title ?: "Unknown Feed")
            .setSubtitle(isFeedTested?.subtitle ?: "")
            .setImageUrl(isFeedTested?.icon ?: "")
            .build()

        coroutineScope.launch {
            if (defaultFeed != null) {
                updateFeed(context, defaultFeed, feed)
            } else {
                addFeed(context, feed)
            }

            // finish activity
            onFinish()
        }
    }


    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Text("Feed URL", modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = feedUrl,
            onValueChange = { feedUrl = it; isFeedTested = null },
            placeholder = { Text("https://example.com/opds") },
            label = { Text("URL") },
            singleLine = true,
        )
        AuthTypeSelector(
            onSelected = { index ->
                onAuthSelected(index)
                isFeedTested = null;
            },
            selected = selectedAuthIndex
        )
        if (selectedAuthIndex == AuthTypeProto.BASIC.number) {
            OutlinedTextField(
                value = authUsername,
                onValueChange = { authUsername = it; isFeedTested = null },
                label = { Text("Username") },
                singleLine = true,
            )
            OutlinedTextField(
                value = authPassword,
                onValueChange = { authPassword = it; isFeedTested = null },
                label = { Text("Password") },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                singleLine = true,
            )
        }

        Row(
            modifier = Modifier.padding(top = 16.dp),
        )
        {
            Button(
                modifier = Modifier.padding(end = 8.dp),
                onClick = { testFeed() },
                colors = ButtonColors(
                    containerColor = Color(0xFF000000),
                    contentColor = Color(0xFFFFFFFF),
                    disabledContainerColor = Color(0xFFD0D0D0),
                    disabledContentColor = Color(0xFFA0A0A0)
                ),
            ) {
                Text("Test Feed")
            }

            Button(
                colors = ButtonColors(
                    containerColor = Color(0xFF000000),
                    contentColor = Color(0xFFFFFFFF),
                    disabledContainerColor = Color(0xFFD0D0D0),
                    disabledContentColor = Color(0xFF808080)
                ),
                enabled = isFeedTested != null,
                onClick = { saveFeed() },
            ) {
                Text("Save Feed")
            }
        }

        if (isFeedTested != null) {
            Text("Feed is valid. You can save it now.")
            Text("Name: ${isFeedTested?.title ?: "Error"}")
            Text("Subtitle: ${isFeedTested?.subtitle ?: "No subtitle"}")
            Text("Icon URL: ${isFeedTested?.icon ?: "No icon"}")
        }
        if (feedTestError != null) {
            Text("$feedTestError")
        }
    }
}


@Composable
fun AuthTypeSelector(onSelected: (Int) -> Unit, selected: Int) {
    val options = listOf("None", "Basic")

    Column() {
        Text("Authentication Type", modifier = Modifier.padding(top = 16.dp))
        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { index, string ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size
                    ),
                    colors = SegmentedButtonColors(
                        activeContainerColor = Color(0xFF000000),
                        activeContentColor = Color(0xFFFFFFFF),
                        activeBorderColor = Color(0xFF000000),
                        inactiveContainerColor = Color(0xFFFFFFFF),
                        inactiveContentColor = Color(0xFF000000),
                        inactiveBorderColor = Color(0xFF000000),
                        disabledActiveContainerColor = Color(0xFFD0D0D0),
                        disabledActiveContentColor = Color(0xFFA0A0A0),
                        disabledActiveBorderColor = Color(0xFFA0A0A0),
                        disabledInactiveContainerColor = Color(0xFFE0E0E0),
                        disabledInactiveContentColor = Color(0xFF808080),
                        disabledInactiveBorderColor = Color(0xFF808080),
                    ),
                    onClick = { onSelected(index) },
                    selected = index == selected,
                    label = { Text(string) },
                )
            }
        }
    }
}
