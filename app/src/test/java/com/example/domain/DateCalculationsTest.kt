package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class DateCalculationsTest {

    @Test
    fun leapYear_calculatesCorrectly() {
        assertTrue(DateCalculations.isLeapYear(2000))
        assertTrue(DateCalculations.isLeapYear(2024))
        assertFalse(DateCalculations.isLeapYear(1900))
        assertFalse(DateCalculations.isLeapYear(2025))
    }

    @Test
    fun durationBreakdown_exactYearsMonthsDays() {
        val tz = TimeZone.getTimeZone("UTC")
        val start = GregorianCalendar(tz).apply { set(2000, Calendar.FEBRUARY, 29, 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val end = GregorianCalendar(tz).apply { set(2026, Calendar.MARCH, 1, 12, 30, 15); set(Calendar.MILLISECOND, 0) }.timeInMillis

        val breakdown = DateCalculations.calculateDuration(start, end, tz)
        assertEquals(26, breakdown.years)
        assertEquals(0, breakdown.months)
        assertEquals(12, breakdown.hours)
        assertEquals(30, breakdown.minutes)
        assertEquals(15, breakdown.seconds)
    }

    @Test
    fun nextAnnualOccurrence_handlesFeb29OnNonLeapYear() {
        val tz = TimeZone.getTimeZone("UTC")
        val feb29 = GregorianCalendar(tz).apply { set(2020, Calendar.FEBRUARY, 29, 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val now = GregorianCalendar(tz).apply { set(2025, Calendar.FEBRUARY, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis

        val next = DateCalculations.calculateNextAnnualOccurrence(feb29, now, tz)
        val cal = GregorianCalendar(tz).apply { timeInMillis = next.nextOccurrenceMillis }
        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, cal.get(Calendar.MONTH))
        assertEquals(28, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(5, next.upcomingNumber)
    }
}
