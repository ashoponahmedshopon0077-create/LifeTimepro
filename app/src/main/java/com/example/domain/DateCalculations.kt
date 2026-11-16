package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

data class DurationBreakdown(
    val years: Int,
    val months: Int,
    val weeks: Int,
    val days: Int,
    val remainingDaysInMonth: Int,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val totalDays: Long,
    val totalHours: Long,
    val totalMinutes: Long,
    val totalSeconds: Long,
    val totalMilliseconds: Long,
    val isFuture: Boolean
)

data class NextAnnualOccurrence(
    val nextOccurrenceMillis: Long,
    val daysRemaining: Long,
    val hoursRemaining: Int,
    val minutesRemaining: Int,
    val secondsRemaining: Int,
    val upcomingNumber: Int,
    val isToday: Boolean
)

data class MilestoneBadge(
    val id: String,
    val titleEn: String,
    val titleBn: String,
    val descriptionEn: String,
    val descriptionBn: String,
    val requiredDays: Long,
    val unlocked: Boolean,
    val unlockedDateMillis: Long?
)

object DateCalculations {

    fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    fun combineDateAndTime(
        dateMillis: Long,
        timeMinutes: Int = 0,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val cal = GregorianCalendar(timeZone).apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, (timeMinutes / 60).coerceIn(0, 23))
            set(Calendar.MINUTE, (timeMinutes % 60).coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun calculateDuration(
        startMillis: Long,
        endMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): DurationBreakdown {
        val isFuture = startMillis > endMillis
        val fromMillis = minOf(startMillis, endMillis)
        val toMillis = maxOf(startMillis, endMillis)

        val diffMs = toMillis - fromMillis
        val totalSeconds = diffMs / 1000L
        val totalMinutes = totalSeconds / 60L
        val totalHours = totalMinutes / 60L
        val totalDays = totalHours / 24L

        val startCal = GregorianCalendar(timeZone).apply { timeInMillis = fromMillis }
        val endCal = GregorianCalendar(timeZone).apply { timeInMillis = toMillis }

        var years = endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)
        var months = endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH)
        var days = endCal.get(Calendar.DAY_OF_MONTH) - startCal.get(Calendar.DAY_OF_MONTH)
        var hours = endCal.get(Calendar.HOUR_OF_DAY) - startCal.get(Calendar.HOUR_OF_DAY)
        var minutes = endCal.get(Calendar.MINUTE) - startCal.get(Calendar.MINUTE)
        var seconds = endCal.get(Calendar.SECOND) - startCal.get(Calendar.SECOND)

        if (seconds < 0) {
            seconds += 60
            minutes -= 1
        }
        if (minutes < 0) {
            minutes += 60
            hours -= 1
        }
        if (hours < 0) {
            hours += 24
            days -= 1
        }
        if (days < 0) {
            months -= 1
            val prevMonthCal = (endCal.clone() as GregorianCalendar).apply {
                add(Calendar.MONTH, -1)
            }
            days += prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
        if (months < 0) {
            months += 12
            years -= 1
        }
        if (years < 0) {
            years = 0
        }

        val weeks = days / 7
        val remainingDaysAfterWeeks = days % 7

        return DurationBreakdown(
            years = years,
            months = months,
            weeks = weeks,
            days = remainingDaysAfterWeeks,
            remainingDaysInMonth = days,
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            totalDays = totalDays,
            totalHours = totalHours,
            totalMinutes = totalMinutes,
            totalSeconds = totalSeconds,
            totalMilliseconds = diffMs,
            isFuture = isFuture
        )
    }

    fun calculateNextAnnualOccurrence(
        originMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): NextAnnualOccurrence {
        val originCal = GregorianCalendar(timeZone).apply { timeInMillis = originMillis }
        val nowCal = GregorianCalendar(timeZone).apply { timeInMillis = nowMillis }

        val originYear = originCal.get(Calendar.YEAR)
        val originMonth = originCal.get(Calendar.MONTH)
        val originDay = originCal.get(Calendar.DAY_OF_MONTH)

        val nowYear = nowCal.get(Calendar.YEAR)
        val nowMonth = nowCal.get(Calendar.MONTH)
        val nowDay = nowCal.get(Calendar.DAY_OF_MONTH)

        fun resolvedDayForYear(year: Int): Int {
            return if (originMonth == Calendar.FEBRUARY && originDay == 29 && !isLeapYear(year)) {
                28
            } else {
                val tmp = GregorianCalendar(timeZone).apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, originMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                originDay.coerceAtMost(tmp.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
        }

        val thisYearDay = resolvedDayForYear(nowYear)
        val isToday = (nowMonth == originMonth && nowDay == thisYearDay)

        var targetYear = nowYear
        if (!isToday) {
            val candidateThisYear = GregorianCalendar(timeZone).apply {
                set(Calendar.YEAR, nowYear)
                set(Calendar.MONTH, originMonth)
                set(Calendar.DAY_OF_MONTH, thisYearDay)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (candidateThisYear.timeInMillis <= nowMillis) {
                targetYear = nowYear + 1
            }
        }

        val targetDay = resolvedDayForYear(targetYear)
        val targetCal = GregorianCalendar(timeZone).apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, originMonth)
            set(Calendar.DAY_OF_MONTH, targetDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val remainingMs = (targetCal.timeInMillis - nowMillis).coerceAtLeast(0L)
        val totalSec = remainingMs / 1000L
        val daysRem = if (isToday) 0L else (totalSec / 86400L) + if (totalSec % 86400L > 0) 0L else 0L
        val hoursRem = if (isToday) 0 else ((totalSec % 86400L) / 3600L).toInt()
        val minsRem = if (isToday) 0 else ((totalSec % 3600L) / 60L).toInt()
        val secsRem = if (isToday) 0 else (totalSec % 60L).toInt()

        val upcomingNumber = (targetYear - originYear).coerceAtLeast(0)

        return NextAnnualOccurrence(
            nextOccurrenceMillis = targetCal.timeInMillis,
            daysRemaining = daysRem,
            hoursRemaining = hoursRem,
            minutesRemaining = minsRem,
            secondsRemaining = secsRem,
            upcomingNumber = upcomingNumber,
            isToday = isToday
        )
    }

    fun getWeekdayName(dateMillis: Long, locale: Locale = Locale.ENGLISH): String {
        val sdf = SimpleDateFormat("EEEE", locale)
        return sdf.format(Date(dateMillis))
    }

    fun formatDate(dateMillis: Long, locale: Locale = Locale.ENGLISH): String {
        if (dateMillis == 0L) return "—"
        val sdf = SimpleDateFormat("dd MMM yyyy", locale)
        return sdf.format(Date(dateMillis))
    }

    fun formatDateTime(dateMillis: Long, locale: Locale = Locale.ENGLISH): String {
        if (dateMillis == 0L) return "—"
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", locale)
        return sdf.format(Date(dateMillis))
    }

    fun addOrSubtract(
        baseMillis: Long,
        yearsDelta: Int = 0,
        monthsDelta: Int = 0,
        daysDelta: Int = 0,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val cal = GregorianCalendar(timeZone).apply {
            timeInMillis = baseMillis
            add(Calendar.YEAR, yearsDelta)
            add(Calendar.MONTH, monthsDelta)
            add(Calendar.DAY_OF_MONTH, daysDelta)
        }
        return cal.timeInMillis
    }

    fun isSameMonthAndDay(
        dateA: Long,
        dateB: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        val calA = GregorianCalendar(timeZone).apply { timeInMillis = dateA }
        val calB = GregorianCalendar(timeZone).apply { timeInMillis = dateB }
        return calA.get(Calendar.MONTH) == calB.get(Calendar.MONTH) &&
            calA.get(Calendar.DAY_OF_MONTH) == calB.get(Calendar.DAY_OF_MONTH)
    }

    fun yearsAgo(
        pastMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Int {
        val calA = GregorianCalendar(timeZone).apply { timeInMillis = pastMillis }
        val calB = GregorianCalendar(timeZone).apply { timeInMillis = nowMillis }
        return (calB.get(Calendar.YEAR) - calA.get(Calendar.YEAR)).coerceAtLeast(0)
    }

    fun upcomingAgeMilestones(
        birthMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<Pair<String, Long>> {
        val currentAge = calculateDuration(birthMillis, nowMillis, timeZone).years
        val targets = listOf(1, 5, 10, 18, 21, 25, 30, 40, 50, 60, 75, 80, 100)
            .filter { it > currentAge }
            .take(4)
        val list = targets.map { age ->
            val cal = GregorianCalendar(timeZone).apply {
                timeInMillis = birthMillis
                add(Calendar.YEAR, age)
            }
            "${age}th Birthday" to cal.timeInMillis
        }.toMutableList()

        // Also add next 1,000-day milestone
        val livedDays = calculateDuration(birthMillis, nowMillis, timeZone).totalDays
        val nextThousandDays = ((livedDays / 1000L) + 1L) * 1000L
        val thousandCal = GregorianCalendar(timeZone).apply {
            timeInMillis = birthMillis
            add(Calendar.DAY_OF_YEAR, nextThousandDays.toInt())
        }
        list.add("$nextThousandDays Days Alive" to thousandCal.timeInMillis)
        return list.sortedBy { it.second }
    }

    fun relationshipMilestones(startDateMillis: Long, nowMillis: Long = System.currentTimeMillis()): List<MilestoneBadge> {
        val totalDays = if (startDateMillis <= 0L) 0L else calculateDuration(startDateMillis, nowMillis).totalDays.coerceAtLeast(0L)
        fun unlockAt(days: Long): Long? = if (startDateMillis > 0L && totalDays >= days) startDateMillis + days * 86_400_000L else null

        return listOf(
            MilestoneBadge("day_1", "First Day", "প্রথম দিন", "The day your journey began", "আপনাদের যাত্রার শুরু", 1L, totalDays >= 1L, unlockAt(1L)),
            MilestoneBadge("month_1", "One Month", "এক মাস একসাথে", "30 days of shared smiles", "৩০ দিনের মিষ্টি স্মৃতি", 30L, totalDays >= 30L, unlockAt(30L)),
            MilestoneBadge("days_100", "100 Days Together", "১০০ দিন একসাথে", "A hundred golden sunrises", "একসাথে ১০০টি সুন্দর দিন", 100L, totalDays >= 100L, unlockAt(100L)),
            MilestoneBadge("months_6", "Six Months", "ছয় মাস", "Half a year of love & care", "ভালোবাসার অর্ধ-বছর", 182L, totalDays >= 182L, unlockAt(182L)),
            MilestoneBadge("year_1", "1st Anniversary", "প্রথম বার্ষিকী", "One full orbit around the sun", "একসাথে পূর্ণ ১টি বছর", 365L, totalDays >= 365L, unlockAt(365L)),
            MilestoneBadge("year_2", "2 Years Together", "২ বছর একসাথে", "Growing stronger every season", "প্রতিটি ঋতুতে আরও গভীর বন্ধন", 730L, totalDays >= 730L, unlockAt(730L)),
            MilestoneBadge("year_5", "5 Years Wood Anniversary", "৫ বছর পূর্তি", "Deep roots and lasting warmth", "ভালোবাসার পাঁচ বছরের দৃঢ় শিকড়", 1826L, totalDays >= 1826L, unlockAt(1826L)),
            MilestoneBadge("year_10", "10 Years Tin/Diamond", "১০ বছর পূর্তি", "A decade of cherished memories", "এক দশকের অমূল্য স্মৃতি", 3652L, totalDays >= 3652L, unlockAt(3652L)),
            MilestoneBadge("year_25", "25 Years Silver", "২৫ বছর রজত জয়ন্তী", "Silver Jubilee milestone", "রজত জয়ন্তী — ভালোবাসার ২৫ বছর", 9131L, totalDays >= 9131L, unlockAt(9131L)),
            MilestoneBadge("year_50", "50 Years Golden", "৫০ বছর সুবর্ণ জয়ন্তী", "Golden Jubilee milestone", "সুবর্ণ জয়ন্তী — ভালোবাসার ৫০ বছর", 18262L, totalDays >= 18262L, unlockAt(18262L))
        )
    }

    val anniversaryMilestonesYears = listOf(1, 2, 3, 4, 5, 10, 15, 20, 25, 50)
}
