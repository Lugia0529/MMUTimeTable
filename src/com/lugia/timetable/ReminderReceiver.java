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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BroadcastReceiver for register and cancel reminder.
 */
public class ReminderReceiver extends BroadcastReceiver
{
    public static String[] mDayStrings;
    public static String[] mTimeStrings;
    
    public static final String ACTION_UPDATE_REMINDER = "com.lugia.timetable.UPDATE_REMINDER";
    
    private static final String TAG = "ReminderManager";
    
    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        
        // check for valid action
        if (!(action.equals(Intent.ACTION_BOOT_COMPLETED) || action.equals(ACTION_UPDATE_REMINDER)))
        {
            Log.e(TAG, "Invalid action received: " + action);
            return;
        }
        
        boolean notificationEnabled = SettingActivity.getBoolean(context, SettingActivity.KEY_NOTIFICATION, false);
        
        // cancel all reminder if user didn't enable notification
        if (!notificationEnabled)
        {
            Log.i(TAG, "Notification disabled, cancel all reminder.");
            
            cancelReminder(context);
            return;
        }
        
        int notifyBefore = Integer.parseInt(SettingActivity.getString(context, SettingActivity.KEY_NOTIFY_BEFORE, "15"));
        
        registerReminder(context, notifyBefore);
    }
    
    public void cancelReminder(final Context context)
    {
        AlarmManager manager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        
        Intent intent = new Intent(context, ReminderService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        manager.cancel(pendingIntent);
    }
    
    public void registerReminder(final Context context, int notifyBefore)
    {
        SubjectList subjectList = loadFileFromSystem(context);
        
        // bail out if we cannot get a valid subject list
        if (subjectList == null || subjectList.size() == 0)
            return;
        
        mDayStrings = context.getResources().getStringArray(R.array.day_long_string); 
        mTimeStrings = context.getResources().getStringArray(R.array.full_time_string);
        
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
        DateFormat formatter = SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
        String dateString = formatter.format(new Date(targetTimeMillies));
        
        Log.i(TAG, String.format("Register reminder for %s, on %s", header, dateString));
        
        // register the alarm
        AlarmManager manager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        
        Intent intent = new Intent(context, ReminderService.class);
        
        intent.putExtra(ReminderService.EXTRA_SUBJECT_CODE, subjectCode)
              .putExtra(ReminderService.EXTRA_HEADER,       header)
              .putExtra(ReminderService.EXTRA_CONTENT,      content);
        
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        manager.set(AlarmManager.RTC_WAKEUP, targetTimeMillies, pendingIntent);
    }
    
    private SubjectList loadFileFromSystem(Context context)
    {
        File file = new File(context.getFilesDir(), MasterActivity.SAVEFILE);
        
        if (!file.exists())
            return null;
        
        try
        {
            FileInputStream in = context.openFileInput(MasterActivity.SAVEFILE);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            
            StringBuilder builder = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) 
                builder.append(line);
            
            reader.close();
            
            return new SubjectList(builder.toString());
        }
        catch (Exception e)
        {
            // something went wrong
            Log.e(TAG, "Error on load from system!", e);
            
            return null;
        }
    }
}
