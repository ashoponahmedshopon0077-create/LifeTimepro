package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NotificationSetting
import com.example.data.model.SpecialEvent
import com.example.domain.DateCalculations
import com.example.notifications.LifeTimeNotificationHelper
import com.example.ui.components.DateSelectorField
import com.example.ui.components.EmptyStateCard
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.RoseCrimson
import com.example.ui.viewmodel.LifeTimeViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

private val specialEventCategories = listOf(
    "birthday" to "Birthday",
    "anniversary" to "Relationship Anniversary",
    "wedding" to "Wedding Anniversary",
    "valentine" to "Valentine's Day",
    "friendship" to "Friendship Day",
    "first_meeting" to "First Meeting Day",
    "first_love" to "First Love Day",
    "eid" to "Eid",
    "new_year" to "New Year",
    "graduation" to "Graduation Day",
    "custom_counter" to "Custom Counter",
    "custom" to "Custom Event"
)

@Composable
fun CalendarScreen(viewModel: LifeTimeViewModel) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val nowMillis by viewModel.nowMillis.collectAsStateWithLifecycle()
    val state by viewModel.specialEvents.collectAsStateWithLifecycle()
    val events = (state as? UiState.Success<List<SpecialEvent>>)?.data.orEmpty()

    var displayMonthOffset by remember { mutableIntStateOf(0) }
    var selectedDayOfMonth by remember { mutableIntStateOf(GregorianCalendar().get(Calendar.DAY_OF_MONTH)) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.recordViewedSection("Calendar") }

    val monthCal = remember(displayMonthOffset) {
        GregorianCalendar().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, displayMonthOffset)
        }
    }
    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) - 1
    val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthCal.time)

    val monthEvents = remember(events, monthCal) {
        val m = monthCal.get(Calendar.MONTH)
        events.filter { evt ->
            val c = GregorianCalendar().apply { timeInMillis = evt.eventDateMillis }
            if (evt.isRecurringYearly) c.get(Calendar.MONTH) == m
            else c.get(Calendar.MONTH) == m && c.get(Calendar.YEAR) == monthCal.get(Calendar.YEAR)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_calendar_event_fab")
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
            item {
                Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { displayMonthOffset-- }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
                            }
                            Text(monthTitle, style = MaterialTheme.typography.titleLarge)
                            IconButton(onClick = { displayMonthOffset++ }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("S", "M", "T", "W", "T", "F", "S").forEach { wd ->
                                Text(
                                    text = wd,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val totalCells = firstDayOfWeek + daysInMonth
                        val rows = (totalCells + 6) / 7
                        for (r in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (c in 0 until 7) {
                                    val cellIndex = r * 7 + c
                                    val dayNum = cellIndex - firstDayOfWeek + 1
                                    if (dayNum in 1..daysInMonth) {
                                        val hasEvent = monthEvents.any { evt ->
                                            GregorianCalendar().apply { timeInMillis = evt.eventDateMillis }
                                                .get(Calendar.DAY_OF_MONTH) == dayNum
                                        }
                                        val isSelected = dayNum == selectedDayOfMonth
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isSelected -> RoseCrimson
                                                        hasEvent -> MaterialTheme.colorScheme.primaryContainer
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .clickable { selectedDayOfMonth = dayNum },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = if (hasEvent || isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                if (hasEvent && !isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(RoseCrimson)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Special Days & Custom Counters (${events.size})", style = MaterialTheme.typography.titleLarge)
            }

            if (events.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = strings.specialCalendar,
                        subtitle = "Add Birthdays, Anniversaries, Valentine's Day, Eid, New Year, Friendship Day, and custom yearly counters."
                    )
                }
            } else {
                items(events, key = { it.id }) { evt ->
                    val next = DateCalculations.calculateNextAnnualOccurrence(evt.eventDateMillis, nowMillis)
                    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = evt.category.replace("_", " ").uppercase() +
                                        if (evt.isRecurringYearly) " • YEARLY" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ChampagneGold
                                )
                                Text(evt.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${DateCalculations.formatDate(evt.eventDateMillis)} • ${if (next.isToday) strings.today else "${next.daysRemaining} ${strings.daysLeft}"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(onClick = { viewModel.saveSpecialEvent(evt.copy(isFavorite = !evt.isFavorite)) }) {
                                    Icon(
                                        if (evt.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Favorite",
                                        tint = ChampagneGold
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteSpecialEvent(evt.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = strings.delete)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val defaultCal = (monthCal.clone() as GregorianCalendar).apply {
            set(Calendar.DAY_OF_MONTH, selectedDayOfMonth.coerceIn(1, daysInMonth))
        }
        SpecialEventDialog(
            initialDateMillis = defaultCal.timeInMillis,
            onDismiss = { showAddDialog = false },
            onSave = { evt ->
                viewModel.saveSpecialEvent(evt) { saved ->
                    LifeTimeNotificationHelper.scheduleSpecialEventAlerts(context, saved)
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SpecialEventDialog(
    initialDateMillis: Long,
    onDismiss: () -> Unit,
    onSave: (SpecialEvent) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("anniversary") }
    var dateMillis by remember { mutableLongStateOf(initialDateMillis) }
    var yearly by remember { mutableStateOf(true) }
    var n30 by remember { mutableStateOf(true) }
    var n7 by remember { mutableStateOf(true) }
    var n3 by remember { mutableStateOf(true) }
    var n1 by remember { mutableStateOf(true) }
    var n0 by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Special Day") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Title") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    specialEventCategories.forEach { (key, label) ->
                        FilterChip(selected = category == key, onClick = { category = key }, label = { Text(label) })
                    }
                }
                DateSelectorField("Event Date", dateMillis, { dateMillis = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = yearly, onCheckedChange = { yearly = it })
                    Text("Recurring Yearly Notification")
                }
                Text("Notification Schedule", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(n30, { n30 = it }); Text("30 days before") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(n7, { n7 = it }); Text("7 days before") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(n3, { n3 = it }); Text("3 days before") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(n1, { n1 = it }); Text("1 day before") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(n0, { n0 = it }); Text("On Event Day") }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) {
                    onSave(
                        SpecialEvent(
                            title = title.trim(),
                            category = category,
                            eventDateMillis = dateMillis,
                            isRecurringYearly = yearly,
                            notify30Days = n30,
                            notify7Days = n7,
                            notify3Days = n3,
                            notify1Day = n1,
                            notifyOnDay = n0
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun NotificationsScreen(viewModel: LifeTimeViewModel) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val notifState by viewModel.notificationSettings.collectAsStateWithLifecycle()
    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()

    val settings = (notifState as? UiState.Success<List<NotificationSetting>>)?.data.orEmpty()
    val profile = (profileState as? UiState.Success)?.data

    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasNotifPermission = granted
    }

    var showAddReminderDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddReminderDialog = true },
                modifier = Modifier.testTag("add_notification_fab")
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
            if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Enable Notification Permission", style = MaterialTheme.typography.titleMedium)
                            Text("Allow LifeTime to send yearly anniversary, birthday, gift, and daily memory reminders.")
                            Button(onClick = { permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                Text("Grant Notification Permission")
                            }
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.notificationCenter, style = MaterialTheme.typography.titleLarge)
                                Text("Daily On This Day reminders & Yearly Anniversary alerts", style = MaterialTheme.typography.bodyMedium)
                            }
                            Switch(
                                checked = profile?.notificationsEnabled ?: true,
                                onCheckedChange = { enabled ->
                                    profile?.let {
                                        viewModel.updateProfile(it.copy(notificationsEnabled = enabled))
                                        if (enabled) LifeTimeNotificationHelper.scheduleDailyMemoryReminder(context, it.dailyMemoryHour)
                                    }
                                }
                            )
                        }

                        Button(
                            onClick = {
                                LifeTimeNotificationHelper.showNotificationNow(
                                    context = context,
                                    title = "LifeTime Celebration Alert",
                                    body = "Yearly anniversaries, birthdays, and On This Day reminders are active!",
                                    isCelebration = true
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("test_notification_button")
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.sendTestNotification)
                        }
                    }
                }
            }

            if (settings.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "Custom Reminder Schedules",
                        subtitle = "Add custom date reminders, gift purchase alerts, or surprise countdown notifications with custom times and sounds."
                    )
                }
            } else {
                items(settings, key = { it.id }) { item ->
                    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${item.category.uppercase()} • %02d:%02d • Sound: ${item.soundName}".format(item.hour, item.minute),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = item.enabled,
                                onCheckedChange = { viewModel.saveNotificationSetting(item.copy(enabled = it)) }
                            )
                            IconButton(onClick = { viewModel.deleteNotificationSetting(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = strings.delete)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddReminderDialog) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("birthday") }
        var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 86_400_000L) }
        var hour by remember { mutableIntStateOf(9) }
        var sound by remember { mutableStateOf("Romantic Chime") }

        AlertDialog(
            onDismissRequest = { showAddReminderDialog = false },
            title = { Text("Add Custom Notification") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Reminder Title") }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("birthday", "anniversary", "gift", "surprise", "daily_memory", "custom").forEach { c ->
                            FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                        }
                    }
                    DateSelectorField("Target Date", dateMillis, { dateMillis = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Default", "Romantic Chime", "Soft Harp").forEach { s ->
                            FilterChip(selected = sound == s, onClick = { sound = s }, label = { Text(s) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) {
                        viewModel.saveNotificationSetting(
                            NotificationSetting(
                                title = title.trim(),
                                category = category,
                                targetDateMillis = dateMillis,
                                hour = hour,
                                minute = 0,
                                repeatYearly = true,
                                enabled = true,
                                soundName = sound
                            )
                        )
                        LifeTimeNotificationHelper.scheduleExactOrInexactAlarm(
                            context = context,
                            triggerAtMillis = dateMillis,
                            requestCode = title.hashCode(),
                            title = title.trim(),
                            body = "LifeTime Reminder ($category)",
                            isCelebration = category == "anniversary"
                        )
                        showAddReminderDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddReminderDialog = false }) { Text("Cancel") } }
        )
    }
}
