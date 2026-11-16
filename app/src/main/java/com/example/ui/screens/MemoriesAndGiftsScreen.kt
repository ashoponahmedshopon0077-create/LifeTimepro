package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.GiftItem
import com.example.data.model.LifeMoment
import com.example.domain.DateCalculations
import com.example.export.ShareAndExportHelper
import com.example.ui.components.DateSelectorField
import com.example.ui.components.EmptyStateCard
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.SageEmerald
import com.example.ui.viewmodel.LifeTimeViewModel
import com.example.ui.viewmodel.UiState

private val momentCategories = listOf(
    "first_meeting" to "First Meeting",
    "first_conversation" to "First Conversation",
    "first_message" to "First Message",
    "first_phone_call" to "First Phone Call",
    "first_video_call" to "First Video Call",
    "first_date" to "First Date",
    "first_proposal" to "First Proposal",
    "first_love_confession" to "First Love Confession",
    "first_gift" to "First Gift",
    "first_trip" to "First Trip Together",
    "first_birthday" to "First Birthday Together",
    "first_anniversary" to "First Anniversary",
    "wedding" to "Wedding Day",
    "graduation" to "Graduation Day",
    "first_job" to "First Job",
    "family" to "Family Event",
    "travel" to "Travel Memory",
    "diary" to "Personal Diary",
    "custom" to "Custom Moment"
)

@Composable
fun MemoriesScreen(viewModel: LifeTimeViewModel) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val nowMillis by viewModel.nowMillis.collectAsStateWithLifecycle()
    val state by viewModel.moments.collectAsStateWithLifecycle()
    val allMoments = (state as? UiState.Success<List<LifeMoment>>)?.data.orEmpty()

    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("all") }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.recordViewedSection("Memories") }

    val filtered = remember(allMoments, searchQuery, filterMode, nowMillis) {
        allMoments.filter { m ->
            val matchesSearch = searchQuery.isBlank() ||
                m.title.contains(searchQuery, ignoreCase = true) ||
                m.description.contains(searchQuery, ignoreCase = true) ||
                m.location.contains(searchQuery, ignoreCase = true) ||
                m.albumName.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filterMode) {
                "favorites" -> m.isFavorite
                "on_this_day" -> DateCalculations.isSameMonthAndDay(m.eventDateMillis, nowMillis)
                "diary" -> m.category == "diary"
                "travel" -> m.category == "travel" || m.category == "first_trip"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_moment_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.add)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
            )

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = filterMode == "all", onClick = { filterMode = "all" }, label = { Text("All (${allMoments.size})") })
                FilterChip(selected = filterMode == "favorites", onClick = { filterMode = "favorites" }, label = { Text(strings.favoritesOnly) })
                FilterChip(selected = filterMode == "on_this_day", onClick = { filterMode = "on_this_day" }, label = { Text(strings.onThisDay) })
                FilterChip(selected = filterMode == "travel", onClick = { filterMode = "travel" }, label = { Text("Travel") })
                FilterChip(selected = filterMode == "diary", onClick = { filterMode = "diary" }, label = { Text("Diary") })
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (filtered.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = strings.importantMoments,
                            subtitle = "Save First Meeting, First Date, Travel Photos/Videos, Personal Diary entries, and Family Milestones."
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { mom ->
                        Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = (momentCategories.find { it.first == mom.category }?.second ?: mom.category).uppercase() +
                                                if (mom.albumName.isNotBlank()) " • ${mom.albumName}" else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ChampagneGold
                                        )
                                        Text(mom.title, style = MaterialTheme.typography.titleLarge)
                                        Text(
                                            text = DateCalculations.formatDate(mom.eventDateMillis),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = { viewModel.saveMoment(mom.copy(isFavorite = !mom.isFavorite)) }) {
                                            Icon(
                                                if (mom.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Favorite",
                                                tint = RoseCrimson
                                            )
                                        }
                                        IconButton(onClick = {
                                            ShareAndExportHelper.shareCounterCardImage(
                                                context = context,
                                                title = mom.title,
                                                headlineValue = DateCalculations.formatDate(mom.eventDateMillis),
                                                subtitle = mom.description.ifBlank { mom.location }
                                            )
                                        }) {
                                            Icon(Icons.Default.Share, contentDescription = strings.shareCard)
                                        }
                                        IconButton(onClick = { viewModel.deleteMoment(mom.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = strings.delete)
                                        }
                                    }
                                }

                                if (mom.location.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Text(mom.location, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }

                                if (mom.description.isNotBlank()) {
                                    Text(mom.description, style = MaterialTheme.typography.bodyLarge)
                                }

                                if (mom.mediaUris.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        mom.mediaUris.forEach { uri ->
                                            AsyncImage(
                                                model = uri,
                                                contentDescription = mom.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(110.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        MomentDialog(
            onDismiss = { showAddDialog = false },
            onSave = {
                viewModel.saveMoment(it)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun MomentDialog(
    onDismiss: () -> Unit,
    onSave: (LifeMoment) -> Unit
) {
    val strings = LocalAppStrings.current
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("first_date") }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var location by remember { mutableStateOf("") }
    var albumName by remember { mutableStateOf("Memories") }
    var description by remember { mutableStateOf("") }
    val pickedUris = remember { mutableStateListOf<String>() }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        pickedUris.addAll(uris.map { it.toString() })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Important Moment") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Moment Title") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    momentCategories.forEach { (key, label) ->
                        FilterChip(
                            selected = category == key,
                            onClick = { category = key },
                            label = { Text(label) }
                        )
                    }
                }
                DateSelectorField("Date & Time", dateMillis, { dateMillis = it })
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = albumName, onValueChange = { albumName = it }, label = { Text("Album Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Story & Notes") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                OutlinedButton(
                    onClick = { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("${strings.pickPhotoVideo} (${pickedUris.size})")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) {
                    onSave(
                        LifeMoment(
                            title = title.trim(),
                            category = category,
                            eventDateMillis = dateMillis,
                            location = location.trim(),
                            description = description.trim(),
                            albumName = albumName.trim(),
                            mediaUris = pickedUris.toList(),
                            mediaType = if (pickedUris.isEmpty()) "none" else "mixed"
                        )
                    )
                }
            }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
fun GiftsScreen(viewModel: LifeTimeViewModel) {
    val strings = LocalAppStrings.current
    val state by viewModel.gifts.collectAsStateWithLifecycle()
    val allGifts = (state as? UiState.Success<List<GiftItem>>)?.data.orEmpty()

    var filterDirection by remember { mutableStateOf("all") }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.recordViewedSection("Gifts") }

    val filtered = remember(allGifts, filterDirection) {
        if (filterDirection == "all") allGifts else allGifts.filter { it.direction == filterDirection }
    }
    val totalBudget = filtered.sumOf { it.budget }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier.testTag("add_gift_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.add)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(strings.giftManager, style = MaterialTheme.typography.titleMedium)
                        Text("${filtered.size} gifts • Total Budget: $${"%.2f".format(totalBudget)}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(30.dp))
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = filterDirection == "all", onClick = { filterDirection = "all" }, label = { Text("All") })
                FilterChip(selected = filterDirection == "giving", onClick = { filterDirection = "giving" }, label = { Text(strings.giving) })
                FilterChip(selected = filterDirection == "received", onClick = { filterDirection = "received" }, label = { Text(strings.received) })
                FilterChip(selected = filterDirection == "wishlist", onClick = { filterDirection = "wishlist" }, label = { Text(strings.wishlist) })
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filtered.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "Plan & Remember Every Gift",
                            subtitle = "Track Birthday, Anniversary, Valentine's Day, and Surprise gifts with budgets, statuses, and who gave whom."
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { gift ->
                        Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                                        if (gift.photoUri.isNotBlank()) {
                                            AsyncImage(
                                                model = gift.photoUri,
                                                contentDescription = gift.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                                            )
                                        }
                                        Column {
                                            Text(gift.name, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                text = "${gift.occasion.uppercase()} • $${gift.budget} • ${gift.direction.uppercase()}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ChampagneGold
                                            )
                                            if (gift.recipientName.isNotBlank() || gift.giverName.isNotBlank()) {
                                                Text(
                                                    text = "From: ${gift.giverName.ifBlank { "Me" }} → To: ${gift.recipientName.ifBlank { "Partner" }}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteGift(gift.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = strings.delete)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("planned" to strings.planned, "purchased" to strings.purchased, "given" to strings.given).forEach { (statusKey, statusLabel) ->
                                        AssistChip(
                                            onClick = { viewModel.saveGift(gift.copy(status = statusKey)) },
                                            label = {
                                                Text(
                                                    statusLabel,
                                                    color = if (gift.status == statusKey) SageEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        GiftDialog(
            onDismiss = { showDialog = false },
            onSave = {
                viewModel.saveGift(it)
                showDialog = false
            }
        )
    }
}

@Composable
private fun GiftDialog(
    onDismiss: () -> Unit,
    onSave: (GiftItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var occasion by remember { mutableStateOf("birthday") }
    var direction by remember { mutableStateOf("giving") }
    var status by remember { mutableStateOf("planned") }
    var giver by remember { mutableStateOf("Me") }
    var recipient by remember { mutableStateOf("") }
    var budgetText by remember { mutableStateOf("50") }
    var purchaseDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var givingDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var photoUri by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { photoUri = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Gift") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Gift Name") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("birthday", "anniversary", "valentine", "surprise", "custom").forEach { occ ->
                        FilterChip(selected = occasion == occ, onClick = { occasion = occ }, label = { Text(occ) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("giving", "received", "wishlist").forEach { dir ->
                        FilterChip(selected = direction == dir, onClick = { direction = dir }, label = { Text(dir) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("planned", "purchased", "given").forEach { st ->
                        FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st) })
                    }
                }
                OutlinedTextField(value = giver, onValueChange = { giver = it }, label = { Text("Giver Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = recipient, onValueChange = { recipient = it }, label = { Text("Recipient Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = budgetText, onValueChange = { budgetText = it }, label = { Text("Budget ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                DateSelectorField("Purchase Date", purchaseDate, { purchaseDate = it })
                DateSelectorField("Giving Date", givingDate, { givingDate = it })
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (photoUri.isBlank()) "Pick Gift Photo" else "Photo Selected")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onSave(
                        GiftItem(
                            name = name.trim(),
                            occasion = occasion,
                            direction = direction,
                            status = status,
                            giverName = giver.trim(),
                            recipientName = recipient.trim(),
                            budget = budgetText.toDoubleOrNull() ?: 0.0,
                            purchaseDateMillis = purchaseDate,
                            givingDateMillis = givingDate,
                            photoUri = photoUri
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
