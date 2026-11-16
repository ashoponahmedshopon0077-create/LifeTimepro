package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.domain.DateCalculations
import com.example.domain.DurationBreakdown
import com.example.export.ShareAndExportHelper
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.RoseBlush
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.RoseCrimsonDark
import com.example.ui.viewmodel.CelebrationData

@Composable
fun LiveDurationGrid(
    breakdown: DurationBreakdown,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    showWeeks: Boolean = false
) {
    val strings = LocalAppStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimePill(breakdown.years.toString(), strings.years, accentColor, Modifier.weight(1f))
            TimePill(breakdown.months.toString(), strings.months, accentColor, Modifier.weight(1f))
            if (showWeeks) {
                TimePill(breakdown.weeks.toString(), strings.weeks, accentColor, Modifier.weight(1f))
                TimePill(breakdown.days.toString(), strings.days, accentColor, Modifier.weight(1f))
            } else {
                TimePill(breakdown.remainingDaysInMonth.toString(), strings.days, accentColor, Modifier.weight(1f))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimePill(breakdown.hours.toString().padStart(2, '0'), strings.hours, accentColor, Modifier.weight(1f))
            TimePill(breakdown.minutes.toString().padStart(2, '0'), strings.minutes, accentColor, Modifier.weight(1f))
            TimePill(breakdown.seconds.toString().padStart(2, '0'), strings.seconds, accentColor, Modifier.weight(1f))
        }
    }
}

@Composable
fun TimePill(
    value: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = accentColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = accentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectorField(
    label: String,
    dateMillis: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showPicker = true },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (dateMillis > 0L) DateCalculations.formatDate(dateMillis) else "Tap to choose date",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = if (dateMillis > 0L) dateMillis else System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let(onDateSelected)
                    showPicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AnniversaryCelebrationDialog(
    data: CelebrationData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val transition = rememberInfiniteTransition(label = "confetti")
    val animProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_progress"
    )

    val milestoneBadgeTitle = when (data.yearsCount) {
        25 -> strings.silverAnniversary
        50 -> strings.goldenAnniversary
        else -> if (data.isBirthday) strings.happyBirthday else "${data.yearsCount} ${strings.years} • ${strings.happyAnniversary}"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(RoseCrimsonDark, RoseCrimson, Color(0xFF3E001D))
                    )
                )
                .systemBarsPadding()
        ) {
            // Animated floating golden sparkles
            Canvas(modifier = Modifier.fillMaxSize()) {
                val colors = listOf(Color(0xFFFFD08A), RoseBlush, Color.White, ChampagneGold)
                for (i in 0..24) {
                    val x = ((i * 137) % 100) / 100f * size.width
                    val y = (((i * 73) / 100f + animProgress) % 1f) * size.height
                    drawCircle(
                        color = colors[i % colors.size].copy(alpha = 0.55f),
                        radius = (4 + (i % 5) * 2).dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = strings.cancel, tint = Color.White)
                    }
                }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.height(210.dp).fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_anniversary_celebration_1794843157811),
                            contentDescription = milestoneBadgeTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f))
                        )
                        Surface(
                            color = ChampagneGold,
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Celebration, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(milestoneBadgeTitle, color = Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = data.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = data.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = RoseBlush,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.14f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Celebration Ideas & Gift Suggestions",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFFE4BE)
                        )
                        Text(
                            text = "• Handwritten love letter paired with framed photos from your first year\n• Candlelit dinner or sunset walk at your first meeting spot\n• Custom engraved keepsake watch or memory scrapbook album",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        ShareAndExportHelper.shareCounterCardImage(
                            context = context,
                            title = milestoneBadgeTitle,
                            headlineValue = data.title,
                            subtitle = data.subtitle
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Share Celebration Card", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Back to LifeTime", color = Color.White)
                }
            }
        }
    }
}
