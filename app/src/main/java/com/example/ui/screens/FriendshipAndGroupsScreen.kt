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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.FriendGroup
import com.example.data.model.Friendship
import com.example.data.model.GroupMember
import com.example.domain.DateCalculations
import com.example.export.ShareAndExportHelper
import com.example.ui.components.DateSelectorField
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LiveDurationGrid
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.ChampagneGold
import com.example.ui.viewmodel.LifeTimeViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun FriendshipScreen(viewModel: LifeTimeViewModel) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val nowMillis by viewModel.nowMillis.collectAsStateWithLifecycle()
    val state by viewModel.friendships.collectAsStateWithLifecycle()
    val items = (state as? UiState.Success<List<Friendship>>)?.data.orEmpty()

    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Friendship?>(null) }

    LaunchedEffect(Unit) { viewModel.recordViewedSection("Friends") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editing = null; showDialog = true },
                modifier = Modifier.testTag("add_friendship_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.add)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (items.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = strings.friendshipCounter,
                        subtitle = "Track friendships, best friends, first meetings, birthday reminders, and friendship anniversaries."
                    )
                }
            } else {
                items(items, key = { it.id }) { fr ->
                    val dur = DateCalculations.calculateDuration(fr.startDateMillis, nowMillis)
                    val anniv = DateCalculations.calculateNextAnnualOccurrence(fr.startDateMillis, nowMillis)

                    Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (fr.photoUri.isNotBlank()) {
                                        AsyncImage(
                                            model = fr.photoUri,
                                            contentDescription = fr.friendName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(48.dp).clip(CircleShape)
                                        )
                                    } else {
                                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                                            Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.padding(12.dp))
                                        }
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(fr.friendName, style = MaterialTheme.typography.titleLarge)
                                            if (fr.isBestFriend) {
                                                Icon(Icons.Default.Star, contentDescription = strings.bestFriend, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Text(
                                            text = "Friends since ${DateCalculations.formatDate(fr.startDateMillis)} • ${dur.totalDays} days",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row {
                                    IconButton(onClick = {
                                        ShareAndExportHelper.shareCounterCardImage(
                                            context = context,
                                            title = "Friendship with ${fr.friendName}",
                                            headlineValue = "${dur.years}y ${dur.months}m ${dur.remainingDaysInMonth}d",
                                            subtitle = "${dur.totalDays} days of friendship!"
                                        )
                                    }) {
                                        Icon(Icons.Default.Share, contentDescription = strings.shareCard)
                                    }
                                    IconButton(onClick = { viewModel.deleteFriendship(fr.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = strings.delete)
                                    }
                                }
                            }

                            LiveDurationGrid(breakdown = dur, accentColor = MaterialTheme.colorScheme.secondary)

                            Text(
                                text = "Friendship Anniversary in ${anniv.daysRemaining} days" +
                                    if (fr.birthdayMillis > 0L) {
                                        val bday = DateCalculations.calculateNextAnnualOccurrence(fr.birthdayMillis, nowMillis)
                                        " • Birthday in ${bday.daysRemaining} days"
                                    } else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ChampagneGold
                            )
                            if (fr.notes.isNotBlank()) {
                                Text(fr.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                            OutlinedButton(onClick = { editing = fr; showDialog = true }) {
                                Text(strings.edit)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        FriendshipDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = {
                viewModel.saveFriendship(it)
                showDialog = false
            }
        )
    }
}

@Composable
private fun FriendshipDialog(
    initial: Friendship?,
    onDismiss: () -> Unit,
    onSave: (Friendship) -> Unit
) {
    var name by remember { mutableStateOf(initial?.friendName ?: "") }
    var isBest by remember { mutableStateOf(initial?.isBestFriend ?: false) }
    var startDate by remember { mutableLongStateOf(initial?.startDateMillis ?: System.currentTimeMillis()) }
    var firstMeet by remember { mutableLongStateOf(initial?.firstMeetingMillis ?: 0L) }
    var birthday by remember { mutableLongStateOf(initial?.birthdayMillis ?: 0L) }
    var photoUri by remember { mutableStateOf(initial?.photoUri ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { photoUri = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Friend" else "Edit Friend") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Friend Name") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isBest, onCheckedChange = { isBest = it })
                    Text("Best Friend")
                }
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (photoUri.isBlank()) "Pick Friend Photo" else "Photo Selected")
                }
                DateSelectorField("Friendship Start Date", startDate, { startDate = it })
                DateSelectorField("First Meeting (Optional)", firstMeet, { firstMeet = it })
                DateSelectorField("Friend Birthday (Optional)", birthday, { birthday = it })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Memories & Notes") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onSave(
                        (initial ?: Friendship()).copy(
                            friendName = name.trim(),
                            isBestFriend = isBest,
                            startDateMillis = startDate,
                            firstMeetingMillis = firstMeet,
                            birthdayMillis = birthday,
                            photoUri = photoUri,
                            notes = notes.trim()
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun GroupsScreen(viewModel: LifeTimeViewModel) {
    val strings = LocalAppStrings.current
    val nowMillis by viewModel.nowMillis.collectAsStateWithLifecycle()
    val state by viewModel.groups.collectAsStateWithLifecycle()
    val groups = (state as? UiState.Success<List<FriendGroup>>)?.data.orEmpty()

    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FriendGroup?>(null) }

    LaunchedEffect(Unit) { viewModel.recordViewedSection("Groups") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editing = null; showDialog = true },
                modifier = Modifier.testTag("add_group_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.add)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (groups.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = strings.groupCounter,
                        subtitle = "Create friend squads or family groups with creation dates, member joining dates, and shared memories."
                    )
                }
            } else {
                items(groups, key = { it.id }) { grp ->
                    val dur = DateCalculations.calculateDuration(grp.creationDateMillis, nowMillis)
                    val anniv = DateCalculations.calculateNextAnnualOccurrence(grp.creationDateMillis, nowMillis)

                    Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (grp.pictureUri.isNotBlank()) {
                                        AsyncImage(
                                            model = grp.pictureUri,
                                            contentDescription = grp.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(48.dp).clip(CircleShape)
                                        )
                                    } else {
                                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(48.dp)) {
                                            Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.padding(12.dp))
                                        }
                                    }
                                    Column {
                                        Text(grp.name, style = MaterialTheme.typography.titleLarge)
                                        Text(
                                            text = "Created ${DateCalculations.formatDate(grp.creationDateMillis)} • ${grp.members.size} ${strings.members}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteGroup(grp.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = strings.delete)
                                }
                            }

                            LiveDurationGrid(breakdown = dur, accentColor = MaterialTheme.colorScheme.primary)
                            Text("Group Anniversary in ${anniv.daysRemaining} days", style = MaterialTheme.typography.bodyMedium, color = ChampagneGold)

                            if (grp.members.isNotEmpty()) {
                                Text(strings.members, style = MaterialTheme.typography.labelLarge)
                                grp.members.forEach { member ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("• ${member.name}", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = "Joined ${DateCalculations.formatDate(member.joinedAtMillis)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (grp.notes.isNotBlank()) Text(grp.notes, style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = { editing = grp; showDialog = true }) { Text(strings.edit) }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        GroupDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = {
                viewModel.saveGroup(it)
                showDialog = false
            }
        )
    }
}

@Composable
private fun GroupDialog(
    initial: FriendGroup?,
    onDismiss: () -> Unit,
    onSave: (FriendGroup) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var picUri by remember { mutableStateOf(initial?.pictureUri ?: "") }
    var createdDate by remember { mutableLongStateOf(initial?.creationDateMillis ?: System.currentTimeMillis()) }
    var firstMeet by remember { mutableLongStateOf(initial?.firstMeetingMillis ?: 0L) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    val members = remember { mutableStateListOf<GroupMember>().apply { initial?.members?.let { addAll(it) } } }
    var newMemberName by remember { mutableStateOf("") }
    var newMemberDate by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { picUri = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Create Group" else "Edit Group") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Group Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (picUri.isBlank()) "Choose Group Photo" else "Photo Selected")
                }
                DateSelectorField("Group Creation Date", createdDate, { createdDate = it })
                DateSelectorField("First Group Meeting", firstMeet, { firstMeet = it })

                Text("Add Member & Joining Date", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(value = newMemberName, onValueChange = { newMemberName = it }, label = { Text("Member Name") }, modifier = Modifier.fillMaxWidth())
                DateSelectorField("Member Joined Date", newMemberDate, { newMemberDate = it })
                OutlinedButton(onClick = {
                    if (newMemberName.isNotBlank()) {
                        members.add(GroupMember(newMemberName.trim(), newMemberDate))
                        newMemberName = ""
                    }
                }) {
                    Text("Add Member to Group (${members.size})")
                }
                members.forEachIndexed { index, m ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${m.name} (${DateCalculations.formatDate(m.joinedAtMillis)})", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { members.removeAt(index) }) { Text("Remove") }
                    }
                }

                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Group Memories") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onSave(
                        (initial ?: FriendGroup()).copy(
                            name = name.trim(),
                            pictureUri = picUri,
                            creationDateMillis = createdDate,
                            firstMeetingMillis = firstMeet,
                            members = members.toList(),
                            notes = notes.trim()
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
