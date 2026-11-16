package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.BucketListItem
import com.example.data.model.LoveLetter
import com.example.data.model.Relationship
import com.example.data.model.SurprisePlan
import com.example.domain.DateCalculations
import com.example.export.ShareAndExportHelper
import com.example.ui.components.DateSelectorField
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LiveDurationGrid
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.SageEmerald
import com.example.ui.viewmodel.CelebrationData
import com.example.ui.viewmodel.LifeTimeViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun LoveAndRelationshipScreen(viewModel: LifeTimeViewModel) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val nowMillis by viewModel.nowMillis.collectAsStateWithLifecycle()
    val relState by viewModel.relationships.collectAsStateWithLifecycle()
    val lettersState by viewModel.loveLetters.collectAsStateWithLifecycle()
    val surprisesState by viewModel.surprises.collectAsStateWithLifecycle()
    val bucketState by viewModel.bucketList.collectAsStateWithLifecycle()

    val relationships = (relState as? UiState.Success<List<Relationship>>)?.data.orEmpty()
    val letters = (lettersState as? UiState.Success<List<LoveLetter>>)?.data.orEmpty()
    val surprises = (surprisesState as? UiState.Success<List<SurprisePlan>>)?.data.orEmpty()
    val dreams = (bucketState as? UiState.Success<List<BucketListItem>>)?.data.orEmpty()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showRelDialog by remember { mutableStateOf(false) }
    var editingRel by remember { mutableStateOf<Relationship?>(null) }
    var showLetterDialog by remember { mutableStateOf(false) }
    var showSurpriseDialog by remember { mutableStateOf(false) }
    var showBucketDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.recordViewedSection("Love") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0, 1 -> { editingRel = null; showRelDialog = true }
                        2 -> showLetterDialog = true
                        3 -> showSurpriseDialog = true
                        4 -> showBucketDialog = true
                    }
                },
                modifier = Modifier.testTag("love_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.add)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(strings.liveLoveCounter) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(strings.achievementBadges) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(strings.loveLetters) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text(strings.surprisePlanner) })
                Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text(strings.bucketList) })
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (relationships.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "Add Your Relationship",
                                    subtitle = "Track your live love counter, partner's birthday, first meeting, first kiss, engagement, wedding, and yearly anniversaries."
                                )
                            }
                        } else {
                            items(relationships, key = { it.id }) { rel ->
                                val dur = DateCalculations.calculateDuration(rel.startDateMillis, nowMillis)
                                val anniv = DateCalculations.calculateNextAnnualOccurrence(rel.startDateMillis, nowMillis)

                                Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                if (rel.partnerPhotoUri.isNotBlank()) {
                                                    AsyncImage(
                                                        model = rel.partnerPhotoUri,
                                                        contentDescription = rel.partnerName,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.size(54.dp).clip(CircleShape)
                                                    )
                                                } else {
                                                    Surface(shape = CircleShape, color = RoseCrimson.copy(alpha = 0.14f), modifier = Modifier.size(54.dp)) {
                                                        Icon(Icons.Default.Favorite, contentDescription = null, tint = RoseCrimson, modifier = Modifier.padding(14.dp))
                                                    }
                                                }
                                                Column {
                                                    Text(rel.partnerName, style = MaterialTheme.typography.titleLarge)
                                                    Text(
                                                        text = "Started ${DateCalculations.formatDate(rel.startDateMillis)} • ${dur.totalDays} days",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Row {
                                                IconButton(onClick = {
                                                    ShareAndExportHelper.shareCounterCardImage(
                                                        context = context,
                                                        title = "Together with ${rel.partnerName}",
                                                        headlineValue = "${dur.years}y ${dur.months}m ${dur.remainingDaysInMonth}d",
                                                        subtitle = "${dur.totalDays} days of love & memories"
                                                    )
                                                }) {
                                                    Icon(Icons.Default.Share, contentDescription = strings.shareCard)
                                                }
                                                IconButton(onClick = { viewModel.deleteRelationship(rel.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = strings.delete)
                                                }
                                            }
                                        }

                                        LiveDurationGrid(breakdown = dur, accentColor = RoseCrimson, showWeeks = true)

                                        Text(
                                            text = "Next Anniversary (${anniv.upcomingNumber} Year): ${if (anniv.isToday) strings.happyAnniversary else "${anniv.daysRemaining} ${strings.daysLeft}"}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = ChampagneGold
                                        )

                                        if (rel.partnerBirthdayMillis > 0L) {
                                            val partnerBday = DateCalculations.calculateNextAnnualOccurrence(rel.partnerBirthdayMillis, nowMillis)
                                            Text(
                                                text = "Partner Birthday: ${DateCalculations.formatDate(rel.partnerBirthdayMillis)} (${partnerBday.daysRemaining} ${strings.daysLeft})",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }

                                        // Relationship Milestones Timeline
                                        val milestoneDates = listOf(
                                            "First Meeting" to rel.firstMeetingMillis,
                                            "First Conversation" to rel.firstConversationMillis,
                                            "First Message" to rel.firstMessageMillis,
                                            "First Phone Call" to rel.firstPhoneCallMillis,
                                            "First Video Call" to rel.firstVideoCallMillis,
                                            "First Date" to rel.firstDateMillis,
                                            "First Proposal" to rel.firstProposalMillis,
                                            "First Kiss" to rel.firstKissMillis,
                                            "First Gift" to rel.firstGiftMillis,
                                            "Engagement" to rel.engagementDateMillis,
                                            "Wedding" to rel.weddingDateMillis
                                        ).filter { it.second > 0L }

                                        if (milestoneDates.isNotEmpty()) {
                                            Text(strings.relationshipMilestones, style = MaterialTheme.typography.labelLarge)
                                            milestoneDates.forEach { (label, ms) ->
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("• $label", style = MaterialTheme.typography.bodyMedium)
                                                    Text(DateCalculations.formatDate(ms), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }

                                        // Yearly Anniversary Milestones Celebration Picker
                                        Text("Yearly Anniversaries", style = MaterialTheme.typography.labelLarge)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { editingRel = rel; showRelDialog = true },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(strings.edit)
                                            }
                                            Button(
                                                onClick = {
                                                    viewModel.openCelebration(
                                                        CelebrationData(
                                                            title = "With ${rel.partnerName}",
                                                            yearsCount = anniv.upcomingNumber.coerceAtLeast(1),
                                                            subtitle = "Celebrating ${dur.totalDays} unforgettable days since ${DateCalculations.formatDate(rel.startDateMillis)}"
                                                        )
                                                    )
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Celebrate")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    val primaryRel = relationships.firstOrNull { it.isPrimary } ?: relationships.firstOrNull()
                    val badges = DateCalculations.relationshipMilestones(primaryRel?.startDateMillis ?: 0L, nowMillis)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(badges, key = { it.id }) { badge ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (badge.unlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = if (badge.unlocked) ChampagneGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(badge.titleEn, style = MaterialTheme.typography.titleMedium)
                                        Text(badge.descriptionEn, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (badge.unlocked) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Unlocked", tint = SageEmerald)
                                    } else {
                                        Text("${badge.requiredDays}d", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (letters.isEmpty()) {
                            item { EmptyStateCard("No Love Letters Yet", "Write romantic letters, anniversary notes, or future scheduled letters locked until a special date.") }
                        } else {
                            items(letters, key = { it.id }) { letter ->
                                val isTimeLocked = letter.isLockedUntilDate && letter.scheduledDateMillis > nowMillis
                                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(if (isTimeLocked) Icons.Default.Lock else Icons.Default.Mail, contentDescription = null, tint = RoseCrimson)
                                                Text(letter.title, style = MaterialTheme.typography.titleMedium)
                                            }
                                            IconButton(onClick = { viewModel.deleteLoveLetter(letter.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = strings.delete)
                                            }
                                        }
                                        Text(
                                            text = "${letter.category.replace("_", " ").uppercase()} • For ${letter.recipient.ifBlank { "My Love" }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ChampagneGold
                                        )
                                        if (isTimeLocked) {
                                            Text(
                                                text = "Sealed until ${DateCalculations.formatDate(letter.scheduledDateMillis)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            Text(letter.body, style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (surprises.isEmpty()) {
                            item { EmptyStateCard("Plan a Surprise", "Organize birthday or anniversary surprises with location, budget, cake, guests, and decoration checklists.") }
                        } else {
                            items(surprises, key = { it.id }) { sur ->
                                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text(sur.title, style = MaterialTheme.typography.titleLarge)
                                                Text("${DateCalculations.formatDate(sur.celebrationDateMillis)} • ${sur.location.ifBlank { "Location TBD" }} • Budget $${sur.budget}", style = MaterialTheme.typography.bodyMedium)
                                            }
                                            IconButton(onClick = { viewModel.deleteSurprise(sur.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = strings.delete)
                                            }
                                        }
                                        if (sur.cakeAndFood.isNotBlank()) Text("Cake & Food: ${sur.cakeAndFood}", style = MaterialTheme.typography.bodyMedium)
                                        if (sur.giftIdea.isNotBlank()) Text("Gift Idea: ${sur.giftIdea}", style = MaterialTheme.typography.bodyMedium)
                                        if (sur.guestList.isNotEmpty()) Text("Guests: ${sur.guestList.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
                                        if (sur.decorations.isNotEmpty()) {
                                            Text("Decorations Checklist", style = MaterialTheme.typography.labelLarge)
                                            sur.decorations.forEach { deco ->
                                                val done = deco in sur.completedDecorations
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Checkbox(
                                                        checked = done,
                                                        onCheckedChange = { checked ->
                                                            val updatedDone = if (checked) (sur.completedDecorations + deco).distinct()
                                                            else sur.completedDecorations.filterNot { it == deco }
                                                            viewModel.saveSurprise(sur.copy(completedDecorations = updatedDone))
                                                        }
                                                    )
                                                    Text(deco, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (dreams.isEmpty()) {
                            item { EmptyStateCard("Couple Bucket List & Dreams", "Add places to visit, dream house goals, marriage plans, and shared wishes.") }
                        } else {
                            items(dreams, key = { it.id }) { dream ->
                                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = dream.isCompleted,
                                            onCheckedChange = { checked ->
                                                viewModel.saveBucketListItem(
                                                    dream.copy(
                                                        isCompleted = checked,
                                                        completedDateMillis = if (checked) System.currentTimeMillis() else 0L
                                                    )
                                                )
                                            }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(dream.title, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                text = "${dream.category.replace("_", " ").uppercase()}" +
                                                    if (dream.isCompleted) " • Completed" else " • Dream",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (dream.isCompleted) SageEmerald else ChampagneGold
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteBucketListItem(dream.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = strings.delete)
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

    if (showRelDialog) {
        RelationshipDialog(
            initial = editingRel,
            onDismiss = { showRelDialog = false },
            onSave = {
                viewModel.saveRelationship(it)
                showRelDialog = false
            }
        )
    }

    if (showLetterDialog) {
        LoveLetterDialog(
            onDismiss = { showLetterDialog = false },
            onSave = {
                viewModel.saveLoveLetter(it)
                showLetterDialog = false
            }
        )
    }

    if (showSurpriseDialog) {
        SurpriseDialog(
            onDismiss = { showSurpriseDialog = false },
            onSave = {
                viewModel.saveSurprise(it)
                showSurpriseDialog = false
            }
        )
    }

    if (showBucketDialog) {
        BucketListDialog(
            onDismiss = { showBucketDialog = false },
            onSave = {
                viewModel.saveBucketListItem(it)
                showBucketDialog = false
            }
        )
    }
}

@Composable
private fun RelationshipDialog(
    initial: Relationship?,
    onDismiss: () -> Unit,
    onSave: (Relationship) -> Unit
) {
    var partnerName by remember { mutableStateOf(initial?.partnerName ?: "") }
    var partnerPhotoUri by remember { mutableStateOf(initial?.partnerPhotoUri ?: "") }
    var startDate by remember { mutableLongStateOf(initial?.startDateMillis ?: System.currentTimeMillis()) }
    var partnerBirthday by remember { mutableLongStateOf(initial?.partnerBirthdayMillis ?: 0L) }
    var firstMeeting by remember { mutableLongStateOf(initial?.firstMeetingMillis ?: 0L) }
    var firstConversation by remember { mutableLongStateOf(initial?.firstConversationMillis ?: 0L) }
    var firstMessage by remember { mutableLongStateOf(initial?.firstMessageMillis ?: 0L) }
    var firstCall by remember { mutableLongStateOf(initial?.firstPhoneCallMillis ?: 0L) }
    var firstVideo by remember { mutableLongStateOf(initial?.firstVideoCallMillis ?: 0L) }
    var firstDate by remember { mutableLongStateOf(initial?.firstDateMillis ?: 0L) }
    var firstProposal by remember { mutableLongStateOf(initial?.firstProposalMillis ?: 0L) }
    var firstKiss by remember { mutableLongStateOf(initial?.firstKissMillis ?: 0L) }
    var firstGift by remember { mutableLongStateOf(initial?.firstGiftMillis ?: 0L) }
    var engagement by remember { mutableLongStateOf(initial?.engagementDateMillis ?: 0L) }
    var wedding by remember { mutableLongStateOf(initial?.weddingDateMillis ?: 0L) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { partnerPhotoUri = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Relationship" else "Edit Relationship") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = partnerName, onValueChange = { partnerName = it }, label = { Text("Partner Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedButton(
                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (partnerPhotoUri.isBlank()) "Choose Partner Photo" else "Partner Photo Selected")
                }
                DateSelectorField("Relationship Start Date", startDate, { startDate = it })
                DateSelectorField("Partner Birthday (Optional)", partnerBirthday, { partnerBirthday = it })
                DateSelectorField("First Meeting", firstMeeting, { firstMeeting = it })
                DateSelectorField("First Conversation", firstConversation, { firstConversation = it })
                DateSelectorField("First Message", firstMessage, { firstMessage = it })
                DateSelectorField("First Phone Call", firstCall, { firstCall = it })
                DateSelectorField("First Video Call", firstVideo, { firstVideo = it })
                DateSelectorField("First Date", firstDate, { firstDate = it })
                DateSelectorField("First Proposal", firstProposal, { firstProposal = it })
                DateSelectorField("First Kiss", firstKiss, { firstKiss = it })
                DateSelectorField("First Gift", firstGift, { firstGift = it })
                DateSelectorField("Engagement Date", engagement, { engagement = it })
                DateSelectorField("Wedding Date", wedding, { wedding = it })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (partnerName.isNotBlank()) {
                    onSave(
                        (initial ?: Relationship()).copy(
                            partnerName = partnerName.trim(),
                            partnerPhotoUri = partnerPhotoUri,
                            startDateMillis = startDate,
                            partnerBirthdayMillis = partnerBirthday,
                            firstMeetingMillis = firstMeeting,
                            firstConversationMillis = firstConversation,
                            firstMessageMillis = firstMessage,
                            firstPhoneCallMillis = firstCall,
                            firstVideoCallMillis = firstVideo,
                            firstDateMillis = firstDate,
                            firstProposalMillis = firstProposal,
                            firstKissMillis = firstKiss,
                            firstGiftMillis = firstGift,
                            engagementDateMillis = engagement,
                            weddingDateMillis = wedding
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun LoveLetterDialog(
    onDismiss: () -> Unit,
    onSave: (LoveLetter) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var recipient by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("love_letter") }
    var scheduledDate by remember { mutableLongStateOf(System.currentTimeMillis() + 30L * 86_400_000L) }
    var lockUntilDate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Write Love Letter") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = {
                        title = "Happy Anniversary, My Love"
                        body = "Every day beside you feels like my favorite chapter. Thank you for filling my life with warmth, laughter, and endless patience."
                    }, modifier = Modifier.weight(1f)) { Text("Template 1") }
                    OutlinedButton(onClick = {
                        title = "A Letter for Our Future"
                        category = "future_letter"
                        lockUntilDate = true
                        body = "When we open this together in the future, I hope we still laugh at our smallest jokes and celebrate every dream we built."
                    }, modifier = Modifier.weight(1f)) { Text("Future") }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Letter Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = recipient, onValueChange = { recipient = it }, label = { Text("Recipient") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Your Message") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = lockUntilDate, onCheckedChange = { lockUntilDate = it })
                    Text("Lock as Future Letter until date")
                }
                if (lockUntilDate) {
                    DateSelectorField("Unlock Date", scheduledDate, { scheduledDate = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank() && body.isNotBlank()) {
                    onSave(
                        LoveLetter(
                            title = title.trim(),
                            category = category,
                            recipient = recipient.trim(),
                            body = body.trim(),
                            scheduledDateMillis = scheduledDate,
                            isLockedUntilDate = lockUntilDate
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SurpriseDialog(
    onDismiss: () -> Unit,
    onSave: (SurprisePlan) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var budgetText by remember { mutableStateOf("100") }
    var giftIdea by remember { mutableStateOf("") }
    var cakeAndFood by remember { mutableStateOf("") }
    var guestsText by remember { mutableStateOf("") }
    var decosText by remember { mutableStateOf("Balloons, Fairy Lights, Fresh Roses") }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 7L * 86_400_000L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plan Surprise") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Surprise Title") }, modifier = Modifier.fillMaxWidth())
                DateSelectorField("Celebration Date", dateMillis, { dateMillis = it })
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Secret Location") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = budgetText, onValueChange = { budgetText = it }, label = { Text("Budget ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = giftIdea, onValueChange = { giftIdea = it }, label = { Text("Gift Plan") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cakeAndFood, onValueChange = { cakeAndFood = it }, label = { Text("Cake & Food") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = guestsText, onValueChange = { guestsText = it }, label = { Text("Guests (comma separated)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = decosText, onValueChange = { decosText = it }, label = { Text("Decorations (comma separated)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) {
                    onSave(
                        SurprisePlan(
                            title = title.trim(),
                            celebrationDateMillis = dateMillis,
                            location = location.trim(),
                            budget = budgetText.toDoubleOrNull() ?: 0.0,
                            giftIdea = giftIdea.trim(),
                            cakeAndFood = cakeAndFood.trim(),
                            guestList = guestsText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                            decorations = decosText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun BucketListDialog(
    onDismiss: () -> Unit,
    onSave: (BucketListItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("travel") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Future Dream") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Dream or Goal") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("travel", "dream_house", "marriage", "activity").forEach { cat ->
                        OutlinedButton(onClick = { category = cat }, modifier = Modifier.weight(1f)) {
                            Text(cat.substringBefore("_"), maxLines = 1)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) {
                    onSave(BucketListItem(title = title.trim(), category = category))
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
