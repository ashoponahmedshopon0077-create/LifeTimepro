package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.SpecialEvent
import com.example.domain.DateCalculations
import java.util.Calendar
import java.util.GregorianCalendar

object LifeTimeNotificationHelper {
    const val CHANNEL_ID_REMINDERS = "lifetime_reminders"
    const val CHANNEL_ID_CELEBRATIONS = "lifetime_celebrations"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_BODY = "extra_body"
    const val EXTRA_NOTIF_ID = "extra_notif_id"
    const val EXTRA_IS_CELEBRATION = "extra_is_celebration"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "LifeTime Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Birthdays, gift reminders, daily memories, and countdown alerts"
            }
            val celebrationChannel = NotificationChannel(
                CHANNEL_ID_CELEBRATIONS,
                "Anniversaries & Celebrations",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Yearly anniversary, 25th Silver, and 50th Golden Jubilee celebrations"
            }
            nm.createNotificationChannel(reminderChannel)
            nm.createNotificationChannel(celebrationChannel)
        }
    }

    fun showNotificationNow(
        context: Context,
        title: String,
        body: String,
        notifId: Int = (System.currentTimeMillis() % 100000).toInt(),
        isCelebration: Boolean = false
    ) {
        ensureChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val channelId = if (isCelebration) CHANNEL_ID_CELEBRATIONS else CHANNEL_ID_REMINDERS
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            nm.notify(notifId, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not yet granted
        }
    }

    fun scheduleSpecialEventAlerts(context: Context, event: SpecialEvent) {
        if (event.eventDateMillis <= 0L) return
        val next = DateCalculations.calculateNextAnnualOccurrence(event.eventDateMillis)
        val baseId = (event.id.hashCode() and 0x0FFFFFFF)
        val offsets = mutableListOf<Pair<Int, String>>()
        if (event.notify30Days) offsets.add(30 to "30 days until ${event.title}!")
        if (event.notify7Days) offsets.add(7 to "7 days until ${event.title}!")
        if (event.notify3Days) offsets.add(3 to "3 days until ${event.title}!")
        if (event.notify1Day) offsets.add(1 to "Tomorrow is ${event.title}!")
        if (event.notifyOnDay) offsets.add(0 to "Today is ${event.title}! Celebrate every moment.")

        val now = System.currentTimeMillis()
        for ((daysBefore, message) in offsets) {
            val triggerCal = GregorianCalendar().apply {
                timeInMillis = next.nextOccurrenceMillis
                add(Calendar.DAY_OF_YEAR, -daysBefore)
                set(Calendar.HOUR_OF_DAY, event.notifyHour.coerceIn(0, 23))
                set(Calendar.MINUTE, event.notifyMinute.coerceIn(0, 59))
                set(Calendar.SECOND, 0)
            }
            if (triggerCal.timeInMillis > now) {
                scheduleExactOrInexactAlarm(
                    context = context,
                    triggerAtMillis = triggerCal.timeInMillis,
                    requestCode = baseId + daysBefore,
                    title = event.title,
                    body = message,
                    isCelebration = (daysBefore == 0)
                )
            }
        }
    }

    fun scheduleDailyMemoryReminder(context: Context, hour: Int) {
        val cal = GregorianCalendar().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, "On This Day in LifeTime")
            putExtra(EXTRA_BODY, "Tap to revisit cherished memories from this day in past years.")
            putExtra(EXTRA_NOTIF_ID, 9001)
            putExtra(EXTRA_IS_CELEBRATION, false)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            9001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pi
        )
    }

    fun scheduleExactOrInexactAlarm(
        context: Context,
        triggerAtMillis: Long,
        requestCode: Int,
        title: String,
        body: String,
        isCelebration: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_NOTIF_ID, requestCode)
            putExtra(EXTRA_IS_CELEBRATION, isCelebration)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
    }
}

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            LifeTimeNotificationHelper.scheduleDailyMemoryReminder(context, 9)
            return
        }
        val title = intent.getStringExtra(LifeTimeNotificationHelper.EXTRA_TITLE) ?: "LifeTime Reminder"
        val body = intent.getStringExtra(LifeTimeNotificationHelper.EXTRA_BODY) ?: "You have a special moment today!"
        val notifId = intent.getIntExtra(LifeTimeNotificationHelper.EXTRA_NOTIF_ID, 1001)
        val isCelebration = intent.getBooleanExtra(LifeTimeNotificationHelper.EXTRA_IS_CELEBRATION, false)
        LifeTimeNotificationHelper.showNotificationNow(context, title, body, notifId, isCelebration)
    }
}
