package com.example.tamatoa.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

//since we store dates in millis because thats what room uses, these helpers convert between
//the display strings and millis.

object DateUtils {

    //normal display (dd-mm-yyyy)
    private val displayFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    fun formatShort(millis: Long): String = displayFormat.format(Date(millis))

    //defining the start of the month to the millisecond
    fun startOfMonth(millis: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    //defining the end of the month to the millisecond
    fun endOfMonth(millis: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return c.timeInMillis
    }

    //get the original date, if it is to late in the month ie. 31 of July trim it down
    //to the lastest date of current month ie. 28 Feb
    fun sameDayThisMonth(originalMillis: Long): Long {
        val original = Calendar.getInstance().apply { timeInMillis = originalMillis }
        val originalDay = original.get(Calendar.DAY_OF_MONTH)

        val now = Calendar.getInstance()
        val maxDayThisMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)
        val targetDay = minOf(originalDay, maxDayThisMonth)

        now.set(Calendar.DAY_OF_MONTH, targetDay)
        now.set(Calendar.HOUR_OF_DAY, 12)   //set recurring time at midday to avoid time zone stuff
        now.set(Calendar.MINUTE, 0)
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)
        return now.timeInMillis
    }

    //convert different date/time formats into yyyy-mm-dd so that we can properly track days
    //for badges
    private val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    fun dayKey(millis: Long): String = dayKeyFormat.format(Date(millis))
}