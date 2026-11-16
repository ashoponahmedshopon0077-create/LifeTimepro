package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AgeProfile
import com.example.data.model.LifeMoment
import com.example.data.model.Relationship
import com.example.data.model.SpecialEvent
import com.example.data.model.UserProfile
import com.example.domain.DateCalculations
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LiveDurationGrid
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.RoseCrimsonDark
import com.example.ui.viewmodel.CelebrationData
import com.example.ui.viewmodel.LifeTimeViewModel
import com.example.ui.viewmodel.UiState
import com.example.widget.WidgetSyncPrefs

@Composable
fun HomeScreen(
    viewModel: LifeTimeViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val nowMillis by viewModel.nowMillis.collectAsStateWithLifecycle()
    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val agesState by viewModel.ageProfiles.collectAsStateWithLifecycle()
    val relsState by viewModel.relationships.collectAsStateWithLifecycle()
    val momentsState by viewModel.moments.collectAsStateWithLifecycle()
    val eventsState by viewModel.specialEvents.collectAsStateWithLifecycle()
    val recentlyViewed by viewModel.recentlyViewed.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success<UserProfile?>)?.data
    val ages = (agesState as? UiState.Success<List<AgeProfile>>)?.data.orEmpty()
    val relationships = (relsState as? UiState.Success<List<Relationship>>)?.data.orEmpty()
    val moments = (momentsState as? UiState.Success<List<LifeMoment>>)?.data.orEmpty()
    val events = (eventsState as? UiState.Success<List<SpecialEvent>>)?.data.orEmpty()

    val primaryAge = ages.firstOrNull { it.isSelf } ?: ages.firstOrNull()
    val primaryRel = relationships.firstOrNull { it.isPrimary } ?: relationships.firstOrNull()

    val onThisDayMoments = moments.filter {
        it.eventDateMillis > 0L && DateCalculations.isSameMonthAndDay(it.eventDateMillis, nowMillis)
    }

    // Keep all 6 Android Home Screen Widgets synchronized with latest live counters
    LaunchedEffect(primaryAge, primaryRel, events, onThisDayMoments) {
        val ageDur = primaryAge?.let { DateCalculations.calculateDuration(it.birthDateMillis, nowMillis) }
        val bdayNext = primaryAge?.let { DateCalculations.calculateNextAnnualOccurrence(it.birthDateMillis, nowMillis) }
        val relDur = primaryRel?.let { DateCalculations.calculateDuration(it.startDateMillis, nowMillis) }
        val annivNext = primaryRel?.let { DateCalculations.calculateNextAnnualOccurrence(it.startDateMillis, nowMillis) }
        val nextEvent = events.minByOrNull { DateCalculations.calculateNextAnnualOccurrence(it.eventDateMillis, nowMillis).daysRemaining }
        val nextEventDays = nextEvent?.let { DateCalculations.calculateNextAnnualOccurrence(it.eventDateMillis, nowMillis).daysRemaining }
        val todayMem = onThisDayMoments.firstOrNull()

        WidgetSyncPrefs.saveWidgetData(
            context = context,
            ageTitle = ageDur?.let { "${it.years}y ${it.months}m ${it.remainingDaysInMonth}d" } ?: "Set Birthday",
            ageSubtitle = primaryAge?.name ?: "Open LifeTime",
            loveTitle = relDur?.let { "${it.totalDays} Days Together" } ?: "Add Love Counter",
            loveSubtitle = primaryRel?.let { "With ${it.partnerName}" } ?: "Track milestones",
            birthdayTitle = bdayNext?.let { if (it.isToday) "Happy Birthday Today!" else "${it.daysRemaining} Days Left" } ?: "Set Birthday",
            birthdaySubtitle = primaryAge?.let { "${it.name}'s ${bdayNext?.upcomingNumber}th" } ?: "Birthday reminder",
            anniversaryTitle = annivNext?.let { if (it.isToday) "Happy Anniversary!" else "${it.daysRemaining} Days Left" } ?: "Anniversary",
            anniversarySubtitle = primaryRel?.let { "${annivNext?.upcomingNumber} Year Milestone" } ?: "Add relationship",
            eventTitle = nextEvent?.title ?: "Special Calendar",
            eventSubtitle = nextEventDays?.let { if (it == 0L) "Happening Today" else "$it days away" } ?: "Add special dates",
            memoryTitle = todayMem?.title ?: "On This Day",
            memorySubtitle = todayMem?.let { "${DateCalculations.yearsAgo(it.eventDateMillis)} yr ago" } ?: "${moments.size} saved memories"
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Greeting Banner
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.height(170.dp).fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_romance_1794843156930),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(RoseCrimsonDark.copy(alpha = 0.88f), RoseCrimson.copy(alpha = 0.45f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.22f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = DateCalculations.formatDate(nowMillis),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                            Surface(
                                color = ChampagneGold,
                                shape = CircleShape,
                                modifier = Modifier.clickable { onNavigate("notifications") }
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = strings.navAlerts,
                                    tint = Color.White,
                                    modifier = Modifier.padding(8.dp).size(18.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Hello, ${profile?.displayName?.ifBlank { "Friend" } ?: "Friend"}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.appTagline,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        // Recently Viewed Chips
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                recentlyViewed.forEach { tag ->
                    AssistChip(
                        onClick = {
                            val route = when (tag) {
                                "Age" -> "age"
                                "Love" -> "love"
                                "Friends" -> "friendship"
                                "Groups" -> "groups"
                                "Memories" -> "memories"
                                "Gifts" -> "gifts"
                                "Calendar" -> "calendar"
                                else -> "age"
                            }
                            onNavigate(route)
                        },
                        label = { Text(tag) }
                    )
                }
            }
        }

        // Live Age Snapshot Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("age") }
                    .testTag("home_age_card")
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Cake, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(strings.liveAgeCounter, style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = { onNavigate("age") }) {
                            Text(if (primaryAge == null) strings.addProfile else strings.edit)
                        }
                    }
                    if (primaryAge != null) {
                        val breakdown = DateCalculations.calculateDuration(
                            DateCalculations.combineDateAndTime(primaryAge.birthDateMillis, primaryAge.birthTimeMinutes),
                            nowMillis
                        )
                        val nextBday = DateCalculations.calculateNextAnnualOccurrence(primaryAge.birthDateMillis, nowMillis)
                        Text(
                            text = "${primaryAge.name} • ${strings.nextBirthday}: ${if (nextBday.isToday) strings.today else "${nextBday.daysRemaining} ${strings.daysLeft}"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LiveDurationGrid(breakdown = breakdown, accentColor = MaterialTheme.colorScheme.primary)
                    } else {
                        EmptyStateCard(
                            title = "Set Your Birth Date",
                            subtitle = "Tap to calculate your live age in years, months, days, and seconds."
                        )
                    }
                }
            }
        }

        // Live Love Counter Snapshot Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("love") }
                    .testTag("home_love_card")
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = RoseCrimson)
                            Text(strings.liveLoveCounter, style = MaterialTheme.typography.titleMedium)
                        }
                        if (primaryRel != null) {
                            val anniv = DateCalculations.calculateNextAnnualOccurrence(primaryRel.startDateMillis, nowMillis)
                            TextButton(onClick = {
                                viewModel.openCelebration(
                                    CelebrationData(
                                        title = "Together with ${primaryRel.partnerName}",
                                        yearsCount = anniv.upcomingNumber.coerceAtLeast(1),
                                        subtitle = "Since ${DateCalculations.formatDate(primaryRel.startDateMillis)}"
                                    )
                                )
                            }) {
                                Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Celebrate")
                            }
                        } else {
                            TextButton(onClick = { onNavigate("love") }) {
                                Text(strings.add)
                            }
                        }
                    }
                    if (primaryRel != null) {
                        val relDur = DateCalculations.calculateDuration(primaryRel.startDateMillis, nowMillis)
                        val anniv = DateCalculations.calculateNextAnnualOccurrence(primaryRel.startDateMillis, nowMillis)
                        Text(
                            text = "${primaryRel.partnerName} • ${relDur.totalDays} ${strings.days} • Anniversary in ${anniv.daysRemaining}d",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LiveDurationGrid(breakdown = relDur, accentColor = RoseCrimson, showWeeks = true)
                    } else {
                        EmptyStateCard(
                            title = "Start Your Love Counter",
                            subtitle = "Track your relationship milestones, anniversaries, love letters, and surprises."
                        )
                    }
                }
            }
        }

        // On This Day – Memory Reminder
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ChampagneGold)
                        Text(strings.onThisDay, style = MaterialTheme.typography.titleLarge)
                    }
                    TextButton(onClick = { onNavigate("memories") }) {
                        Text(strings.navMemories)
                    }
                }

                if (onThisDayMoments.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onNavigate("memories") }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_memories_keepsake_1794843157876),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))
                            )
                            Text(
                                text = strings.onThisDayEmpty,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    onThisDayMoments.forEach { mom ->
                        val yrsAgo = DateCalculations.yearsAgo(mom.eventDateMillis, nowMillis)
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth().clickable { onNavigate("memories") }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (yrsAgo > 0) String.format(strings.yearsAgoFormat, yrsAgo) else strings.today,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ChampagneGold
                                )
                                Text(mom.title, style = MaterialTheme.typography.titleMedium)
                                if (mom.description.isNotBlank()) {
                                    Text(
                                        text = mom.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Navigation Hub to ALL 10 Sections
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.moreFeatures, style = MaterialTheme.typography.titleLarge)
                val shortcuts = listOf(
                    Triple(strings.navAge, Icons.Default.Cake, "age"),
                    Triple(strings.navLove, Icons.Default.Favorite, "love"),
                    Triple(strings.navFriends, Icons.Default.People, "friendship"),
                    Triple(strings.navGroups, Icons.Default.Groups, "groups"),
                    Triple(strings.navMemories, Icons.Default.PhotoLibrary, "memories"),
                    Triple(strings.navGifts, Icons.Default.CardGiftcard, "gifts"),
                    Triple(strings.navCalendar, Icons.Default.CalendarMonth, "calendar"),
                    Triple(strings.navAlerts, Icons.Default.Notifications, "notifications"),
                    Triple(strings.navSettings, Icons.Default.Settings, "settings")
                )
                shortcuts.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { (label, icon, route) ->
                            QuickTile(
                                label = label,
                                icon = icon,
                                onClick = { onNavigate(route) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(86.dp)
            .clickable(onClick = onClick)
            .testTag("quick_tile_${label.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
