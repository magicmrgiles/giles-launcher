package com.giles.einklauncher.data.widget

import android.app.AlarmManager
import android.content.Context

/**
 * Reads the system's next scheduled alarm. [AlarmManager.getNextAlarmClock] needs no
 * permission, but it only reflects alarms registered as *alarm clocks* via AlarmManager
 * (which the stock Clock app and most alarm apps do correctly). Returns the trigger time
 * in epoch millis, or null when nothing is scheduled.
 */
class AlarmRepository(context: Context) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun nextAlarmTriggerTime(): Long? =
        alarmManager.nextAlarmClock?.triggerTime
}
