/*
 * Copyright (c) 2013 Lugia Programming Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.lugia.timetable;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.util.Log;

/**
 * BroadcastReceiver for register and cancel reminder.
 */
public class ReminderReceiver extends BroadcastReceiver
{
    public static String[] mDayStrings;
    public static String[] mTimeStrings;
    
    public static final String ACTION_UPDATE_SCHEDULE_REMINDER = "com.lugia.timetable.UPDATE_SCHEDULE_REMINDER";
    public static final String ACTION_UPDATE_EVENT_REMINDER    = "com.lugia.timetable.UPDATE_EVENT_REMINDER";
    
    public static final int SCHEDULE_REMINDER_REQUEST_CODE = 1;
    public static final int EVENT_REMINDER_REQUEST_CODE    = 2;
    
    private static final String TAG = "ReminderManager";
    
    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        
        if (action.equals(Intent.ACTION_BOOT_COMPLETED) || action.equals(ACTION_UPDATE_SCHEDULE_REMINDER))
            registerScheduleReminder(context);

        if (action.equals(Intent.ACTION_BOOT_COMPLETED) || action.equals(ACTION_UPDATE_EVENT_REMINDER))
            registerEventReminder(context);
    }
    
    public void registerScheduleReminder(final Context context)
    {
        boolean notificationEnabled = SettingActivity.getBoolean(context, SettingActivity.KEY_SCHEDULE_NOTIFICATION, false);
        
        // cancel all schedule reminder if user didnt enable it
        if (!notificationEnabled)
        {
            Log.i(TAG, "Schedule notification disabled, cancel all reminder.");

            cancelReminder(context, ReminderService.ACTION_SCHEDULE_REMINDER, SCHEDULE_REMINDER_REQUEST_CODE);
            
            return;
        }
        
        Log.i(TAG, "Register schedule reminder");
        
        SubjectList subjectList = SubjectList.getInstance(context);

        // bail out if we cannot get a valid subject list
        if (subjectList == null || subjectList.size() == 0)
            return;

        mDayStrings = context.getResources().getStringArray(R.array.day_long_string);
        mTimeStrings = context.getResources().getStringArray(R.array.full_time_string);

        int notifyBefore = Integer.parseInt(SettingActivity.getString(context, SettingActivity.KEY_SCHEDULE_NOTIFY_BEFORE, "15"));
        
        long currentTimeMillies = System.currentTimeMillis();
        long targetTimeMillies = Long.MAX_VALUE;

        String subjectCode = "";
        String header = "";
        String content = "";

        // try to find the next nearest schedule
        for (Subject subject : subjectList)
        {
            for (Schedule schedule : subject.getSchedules())
            {
                Calendar scheduleCalendar = Calendar.getInstance();

                scheduleCalendar.set(Calendar.DAY_OF_WEEK, schedule.getDay() + 1);
                scheduleCalendar.set(Calendar.HOUR_OF_DAY, schedule.getTime());
                scheduleCalendar.set(Calendar.MINUTE,      0);
                scheduleCalendar.set(Calendar.SECOND,      0);
                scheduleCalendar.set(Calendar.MILLISECOND, 0);

                // notify in advance according to user setting
                scheduleCalendar.add(Calendar.MINUTE, -notifyBefore);

                // increase the week by 1 if the time is happen in past, this will wrap the time to next week
                if (scheduleCalendar.getTimeInMillis() < currentTimeMillies)
                    scheduleCalendar.add(Calendar.WEEK_OF_YEAR, 1);

                Log.i(TAG, "DIFF -> " + currentTimeMillies + ", " + scheduleCalendar.getTimeInMillis());

                if (scheduleCalendar.getTimeInMillis() < targetTimeMillies)
                {
                    targetTimeMillies = scheduleCalendar.getTimeInMillis();

                    subjectCode = subject.getSubjectCode();
                    header = subject.getSubjectCode() + " - " + subject.getSubjectDescription();
                    content = mDayStrings[schedule.getDay()] + ", " + mTimeStrings[schedule.getTime()] + " - " + schedule.getRoom();
                }
            }
        }

        // this is unlikely to be happen
        if (targetTimeMillies == Long.MAX_VALUE)
            return;

        // TODO: use for debugging, will be remove in future
        final Calendar targetCalendar = Calendar.getInstance();
        targetCalendar.setTimeInMillis(targetTimeMillies);
        
        String dateString = DateFormat.format("EE, MMM dd, yyyy, h:mm aa", targetCalendar).toString();
        
        Log.i(TAG, String.format("Register schedule reminder for %s, on %s", header, dateString));

        // register the alarm
        AlarmManager manager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        
        Intent intent = new Intent(context, ReminderService.class);
        
        intent.setAction(ReminderService.ACTION_SCHEDULE_REMINDER);
        
        intent.putExtra(ReminderService.EXTRA_SUBJECT_CODE, subjectCode)
              .putExtra(ReminderService.EXTRA_HEADER,       header)
              .putExtra(ReminderService.EXTRA_CONTENT,      content);

        PendingIntent pendingIntent = PendingIntent.getService(context, SCHEDULE_REMINDER_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        manager.set(AlarmManager.RTC_WAKEUP, targetTimeMillies, pendingIntent);
    }
    
    public void registerEventReminder(final Context context)
    {
        boolean notificationEnabled = SettingActivity.getBoolean(context, SettingActivity.KEY_EVENT_NOTIFICATION, false);
        
        // cancel all event reminder if user didnt enable it
        if (!notificationEnabled)
        {
            Log.i(TAG, "Event notification disabled, cancel all reminder.");
            
            cancelReminder(context, ReminderService.ACTION_EVENT_REMINDER, EVENT_REMINDER_REQUEST_CODE);
            
            return;
        }

        Log.i(TAG, "Register event reminder");
        
        SubjectList subjectList = SubjectList.getInstance(context);

        // bail out if we cannot get a valid subject list
        if (subjectList == null || subjectList.size() == 0)
            return;
        
        int notifyBefore = Integer.parseInt(SettingActivity.getString(context, SettingActivity.KEY_EVENT_NOTIFY_BEFORE, "15"));

        long currentTimeMillies = System.currentTimeMillis();
        long targetTimeMillies = Long.MAX_VALUE;

        String subjectCode = "";
        String header = "";
        String content = "";
        
        long eventId = -1;
        
        // try to find the next nearest event
        for (Subject subject : subjectList)
        {
            for (Event event : subject.getEvents())
            {
                Calendar eventCalendar = Calendar.getInstance();
                
                eventCalendar.set(Calendar.YEAR,         event.getYear());
                eventCalendar.set(Calendar.MONTH,        event.getMonth());
                eventCalendar.set(Calendar.DAY_OF_MONTH, event.getDay());
                eventCalendar.set(Calendar.HOUR_OF_DAY,  event.getStartHour());
                eventCalendar.set(Calendar.MINUTE,       event.getStartMinute());
                eventCalendar.set(Calendar.SECOND,       0);
                eventCalendar.set(Calendar.MILLISECOND,  0);

                // notify in advance according to user setting
                eventCalendar.add(Calendar.MINUTE, -notifyBefore);

                Log.i(TAG, "DIFF -> " + currentTimeMillies + ", " + eventCalendar.getTimeInMillis());
                
                // skip event happened in the past
                if (eventCalendar.getTimeInMillis() < currentTimeMillies)
                    continue;
                
                if (eventCalendar.getTimeInMillis() < targetTimeMillies)
                {
                    targetTimeMillies = eventCalendar.getTimeInMillis();

                    String date  = generateDateString(event.getYear(), event.getMonth(), event.getDay());
                    String start = generateTimeString(event.getStartHour(), event.getStartMinute());
                    String end   = generateTimeString(event.getEndHour(), event.getEndMinute());
                    
                    eventId = event.getId();
                    
                    subjectCode = subject.getSubjectCode();
                    header = event.getName() + "(" + subject.getSubjectCode() + ")";
                    content = date + ", " + start + " - " + end + " at " + event.getVenue();
                }
            }
        }

        // we didnt find any future event
        if (targetTimeMillies == Long.MAX_VALUE)
            return;

        // TODO: use for debugging, will be remove in future
        final Calendar targetCalendar = Calendar.getInstance();
        targetCalendar.setTimeInMillis(targetTimeMillies);
        
        String dateString = DateFormat.format("EE, MMM dd, yyyy, h:mm aa", targetCalendar).toString();

        Log.i(TAG, String.format("Register event reminder for %s, on %s", header, dateString));
        
        // register the alarm
        AlarmManager manager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, ReminderService.class);

        intent.setAction(ReminderService.ACTION_EVENT_REMINDER);

        intent.putExtra(ReminderService.EXTRA_EVENT_ID,     eventId)
              .putExtra(ReminderService.EXTRA_SUBJECT_CODE, subjectCode)
              .putExtra(ReminderService.EXTRA_HEADER,       header)
              .putExtra(ReminderService.EXTRA_CONTENT,      content);

        PendingIntent pendingIntent = PendingIntent.getService(context, EVENT_REMINDER_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        manager.set(AlarmManager.RTC_WAKEUP, targetTimeMillies, pendingIntent);
    }

    public void cancelReminder(final Context context, String action, int requestCode)
    {
        AlarmManager manager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, ReminderService.class);
        intent.setAction(action);

        PendingIntent pendingIntent = PendingIntent.getService(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        manager.cancel(pendingIntent);
    }

    private String generateDateString(int year, int month, int day)
    {
        final Calendar calendar = Calendar.getInstance();

        // get the date string
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        return DateFormat.format("EE, MMM dd", calendar).toString();
    }

    private String generateTimeString(int hour, int minute)
    {
        final Calendar calendar = Calendar.getInstance();

        // get the start time string
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        return DateFormat.format("h:mm aa", calendar).toString();
    }
}
