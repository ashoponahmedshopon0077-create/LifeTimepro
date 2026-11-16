package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AgeProfile
import com.example.domain.DateCalculations
import com.example.export.ShareAndExportHelper
import com.example.ui.components.DateSelectorField
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LiveDurationGrid
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.CelebrationData
import com.example.ui.viewmodel.LifeTimeViewModel
import com.example.ui.viewmodel.UiState
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun AgeAndCalculatorScreen(viewModel: LifeTimeViewModel) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val nowMillis by viewModel.nowMillis.collectAsStateWithLifecycle()
    val agesState by viewModel.ageProfiles.collectAsStateWithLifecycle()
    val profiles = (agesState as? UiState.Success<List<AgeProfile>>)?.data.orEmpty()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<AgeProfile?>(null) }

    LaunchedEffect(Unit) { viewModel.recordViewedSection("Age") }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        editingProfile = null
                        showAddDialog = true
                    },
                    modifier = Modifier.testTag("add_age_profile_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = strings.addProfile)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(strings.liveAgeCounter) },
                    icon = { Icon(Icons.Default.Cake, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(strings.dateCalculator) },
                    icon = { Icon(Icons.Default.Calculate, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (profiles.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "Add Your Birth Date",
                                subtitle = "Create profiles for yourself, partner, family, or friends to track live age, total seconds lived, leap years, and milestones."
                            )
                        }
                    } else {
                        items(profiles, key = { it.id }) { item ->
                            val combinedBirth = DateCalculations.combineDateAndTime(item.birthDateMillis, item.birthTimeMinutes)
                            val dur = DateCalculations.calculateDuration(combinedBirth, nowMillis)
                            val nextBday = DateCalculations.calculateNextAnnualOccurrence(item.birthDateMillis, nowMillis)
                            val birthYear = GregorianCalendar().apply { timeInMillis = item.birthDateMillis }.get(Calendar.YEAR)
                            val weekday = DateCalculations.getWeekdayName(item.birthDateMillis, Locale.getDefault())
                            val milestones = DateCalculations.upcomingAgeMilestones(item.birthDateMillis, nowMillis)

                            Card(
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${item.name} (${item.relation})",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                            Text(
                                                text = "${DateCalculations.formatDate(item.birthDateMillis)} • ${strings.birthWeekday} $weekday" +
                                                    if (DateCalculations.isLeapYear(birthYear)) " • Leap Year" else "",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row {
                                            IconButton(onClick = {
                                                ShareAndExportHelper.shareCounterCardImage(
                                                    context = context,
                                                    title = "${item.name}'s Age",
                                                    headlineValue = "${dur.years}y ${dur.months}m ${dur.remainingDaysInMonth}d",
                                                    subtitle = "${dur.totalDays} days • ${dur.totalHours} hours lived"
                                                )
                                            }) {
                                                Icon(Icons.Default.Share, contentDescription = strings.shareCard)
                                            }
                                            IconButton(onClick = { viewModel.deleteAgeProfile(item.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = strings.delete)
                                            }
                                        }
                                    }

                                    LiveDurationGrid(breakdown = dur, accentColor = MaterialTheme.colorScheme.primary)

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(strings.totalLived, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                            Text(
                                                text = "${dur.totalDays} ${strings.days} • ${dur.totalHours} ${strings.hours} • ${dur.totalMinutes} ${strings.minutes} • ${dur.totalSeconds} ${strings.seconds}",
                                                fontFamily = MonoFontFamily,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "${strings.nextBirthday} (${nextBday.upcomingNumber}th): ${if (nextBday.isToday) strings.happyBirthday else "${nextBday.daysRemaining}d ${nextBday.hoursRemaining}h ${nextBday.minutesRemaining}m ${nextBday.secondsRemaining}s"}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = ChampagneGold
                                            )
                                        }
                                    }

                                    Text(strings.ageMilestones, style = MaterialTheme.typography.labelLarge)
                                    milestones.forEach { (title, dateMs) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(title, style = MaterialTheme.typography.bodyMedium)
                                            Text(DateCalculations.formatDate(dateMs), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                editingProfile = item
                                                showAddDialog = true
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(strings.edit)
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.openCelebration(
                                                    CelebrationData(
                                                        title = "${item.name} • ${nextBday.upcomingNumber} Years",
                                                        yearsCount = nextBday.upcomingNumber,
                                                        subtitle = "Born on ${DateCalculations.formatDate(item.birthDateMillis)} ($weekday)",
                                                        isBirthday = true
                                                    )
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Birthday Card")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                TimeAndDateCalculatorTab(nowMillis = nowMillis)
            }
        }
    }

    if (showAddDialog) {
        AgeProfileDialog(
            initial = editingProfile,
            onDismiss = { showAddDialog = false },
            onSave = {
                viewModel.saveAgeProfile(it)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun TimeAndDateCalculatorTab(nowMillis: Long) {
    val strings = LocalAppStrings.current
    var startDate by remember { mutableLongStateOf(nowMillis - 365L * 86_400_000L) }
    var endDate by remember { mutableLongStateOf(nowMillis) }

    var baseDate by remember { mutableLongStateOf(nowMillis) }
    var deltaYears by remember { mutableStateOf("0") }
    var deltaMonths by remember { mutableStateOf("0") }
    var deltaDays by remember { mutableStateOf("100") }
    var isSubtract by remember { mutableStateOf(false) }

    var tzSelected by remember { mutableStateOf("Asia/Dhaka") }

    val diff = DateCalculations.calculateDuration(startDate, endDate)
    val sign = if (isSubtract) -1 else 1
    val shiftedMillis = DateCalculations.addOrSubtract(
        baseMillis = baseDate,
        yearsDelta = sign * (deltaYears.toIntOrNull() ?: 0),
        monthsDelta = sign * (deltaMonths.toIntOrNull() ?: 0),
        daysDelta = sign * (deltaDays.toIntOrNull() ?: 0)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings.calculateDifference, style = MaterialTheme.typography.titleLarge)
                    DateSelectorField("Start Date", startDate, { startDate = it })
                    DateSelectorField("End Date", endDate, { endDate = it })
                    LiveDurationGrid(breakdown = diff, showWeeks = true)
                    Text(
                        text = "${diff.totalDays} ${strings.days} • ${diff.totalHours} ${strings.hours} • ${diff.totalMinutes} ${strings.minutes} • ${diff.totalSeconds} ${strings.seconds} • ${diff.totalMilliseconds} ${strings.milliseconds}",
                        fontFamily = MonoFontFamily,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings.addSubtractDate, style = MaterialTheme.typography.titleLarge)
                    DateSelectorField("Base Date", baseDate, { baseDate = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = deltaYears,
                            onValueChange = { deltaYears = it.filter(Char::isDigit) },
                            label = { Text(strings.years) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = deltaMonths,
                            onValueChange = { deltaMonths = it.filter(Char::isDigit) },
                            label = { Text(strings.months) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = deltaDays,
                            onValueChange = { deltaDays = it.filter(Char::isDigit) },
                            label = { Text(strings.days) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isSubtract, onCheckedChange = { isSubtract = it })
                        Text("Subtract instead of Add")
                    }
                    Text(
                        text = "Result: ${DateCalculations.formatDate(shiftedMillis)} (${DateCalculations.getWeekdayName(shiftedMillis)})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("World Timezone Converter", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Asia/Dhaka", "UTC", "America/New_York", "Europe/London").forEach { tz ->
                            OutlinedButton(
                                onClick = { tzSelected = tz },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(tz.substringAfter("/"), maxLines = 1)
                            }
                        }
                    }
                    val cal = GregorianCalendar(TimeZone.getTimeZone(tzSelected)).apply { timeInMillis = nowMillis }
                    Text(
                        text = "$tzSelected: %02d:%02d:%02d".format(
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            cal.get(Calendar.SECOND)
                        ),
                        fontFamily = MonoFontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun AgeProfileDialog(
    initial: AgeProfile?,
    onDismiss: () -> Unit,
    onSave: (AgeProfile) -> Unit
) {
    val strings = LocalAppStrings.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var relation by remember { mutableStateOf(initial?.relation ?: "Self") }
    var birthDate by remember { mutableLongStateOf(initial?.birthDateMillis ?: (System.currentTimeMillis() - 25L * 365 * 86_400_000L)) }
    var isSelf by remember { mutableStateOf(initial?.isSelf ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) strings.addProfile else strings.edit) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = relation,
                    onValueChange = { relation = it },
                    label = { Text("Relation (Self, Partner, Mom, Friend)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                DateSelectorField("Birth Date", birthDate, { birthDate = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isSelf, onCheckedChange = { isSelf = it })
                    Text("Primary Personal Profile")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            (initial ?: AgeProfile()).copy(
                                name = name.trim(),
                                relation = relation.ifBlank { "Self" },
                                birthDateMillis = birthDate,
                                isSelf = isSelf
                            )
                        )
                    }
                }
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
