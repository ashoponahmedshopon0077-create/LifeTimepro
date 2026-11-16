package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

object WidgetSyncPrefs {
    private const val PREFS_NAME = "lifetime_widget_prefs"

    fun saveWidgetData(
        context: Context,
        ageTitle: String,
        ageSubtitle: String,
        loveTitle: String,
        loveSubtitle: String,
        birthdayTitle: String,
        birthdaySubtitle: String,
        anniversaryTitle: String,
        anniversarySubtitle: String,
        eventTitle: String,
        eventSubtitle: String,
        memoryTitle: String,
        memorySubtitle: String
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("age_title", ageTitle)
            .putString("age_sub", ageSubtitle)
            .putString("love_title", loveTitle)
            .putString("love_sub", loveSubtitle)
            .putString("bday_title", birthdayTitle)
            .putString("bday_sub", birthdaySubtitle)
            .putString("anniv_title", anniversaryTitle)
            .putString("anniv_sub", anniversarySubtitle)
            .putString("event_title", eventTitle)
            .putString("event_sub", eventSubtitle)
            .putString("mem_title", memoryTitle)
            .putString("mem_sub", memorySubtitle)
            .apply()
        refreshAllWidgets(context)
    }

    fun getPair(context: Context, keyPrefix: String, defaultTitle: String, defaultSub: String): Pair<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val t = prefs.getString("${keyPrefix}_title", null) ?: defaultTitle
        val s = prefs.getString("${keyPrefix}_sub", null) ?: defaultSub
        return t to s
    }

    fun refreshAllWidgets(context: Context) {
        val providers = listOf(
            AgeWidgetProvider::class.java,
            LoveWidgetProvider::class.java,
            BirthdayWidgetProvider::class.java,
            AnniversaryWidgetProvider::class.java,
            ImportantEventWidgetProvider::class.java,
            DailyMemoryWidgetProvider::class.java
        )
        val manager = AppWidgetManager.getInstance(context)
        for (cls in providers) {
            val ids = manager.getAppWidgetIds(ComponentName(context, cls))
            if (ids.isNotEmpty()) {
                val intent = Intent(context, cls).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}

abstract class BaseLifeTimeWidgetProvider(
    private val badgeText: String,
    private val keyPrefix: String,
    private val defaultTitle: String,
    private val defaultSubtitle: String
) : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val (title, subtitle) = WidgetSyncPrefs.getPair(context, keyPrefix, defaultTitle, defaultSubtitle)
        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            keyPrefix.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_lifetime_card).apply {
                setTextViewText(R.id.widget_badge, badgeText)
                setTextViewText(R.id.widget_headline, title)
                setTextViewText(R.id.widget_subtitle, subtitle)
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

class AgeWidgetProvider : BaseLifeTimeWidgetProvider(
    badgeText = "LIVE AGE",
    keyPrefix = "age",
    defaultTitle = "Set your Birthday",
    defaultSubtitle = "Open LifeTime to track live age"
)

class LoveWidgetProvider : BaseLifeTimeWidgetProvider(
    badgeText = "LOVE COUNTER",
    keyPrefix = "love",
    defaultTitle = "Add Relationship",
    defaultSubtitle = "Track every moment together"
)

class BirthdayWidgetProvider : BaseLifeTimeWidgetProvider(
    badgeText = "BIRTHDAY COUNTDOWN",
    keyPrefix = "bday",
    defaultTitle = "Next Birthday",
    defaultSubtitle = "Tap to view birthday countdown"
)

class AnniversaryWidgetProvider : BaseLifeTimeWidgetProvider(
    badgeText = "ANNIVERSARY",
    keyPrefix = "anniv",
    defaultTitle = "Next Anniversary",
    defaultSubtitle = "Plan your celebration"
)

class ImportantEventWidgetProvider : BaseLifeTimeWidgetProvider(
    badgeText = "SPECIAL EVENT",
    keyPrefix = "event",
    defaultTitle = "Upcoming Milestone",
    defaultSubtitle = "Tap to view calendar"
)

class DailyMemoryWidgetProvider : BaseLifeTimeWidgetProvider(
    badgeText = "ON THIS DAY",
    keyPrefix = "mem",
    defaultTitle = "Daily Memory",
    defaultSubtitle = "Revisit precious memories"
)
