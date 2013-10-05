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

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

/**
 * IntentService for reminder notification.
 */
public class ReminderService extends IntentService
{
    public static final String EXTRA_EVENT_ID     = "com.lugia.timetable.EventId";
    public static final String EXTRA_SUBJECT_CODE = "com.lugia.timetable.SubjectCode";
    public static final String EXTRA_HEADER       = "com.lugia.timetable.Header";
    public static final String EXTRA_CONTENT      = "com.lugia.timetable.Content";

    public static final String ACTION_SCHEDULE_REMINDER = "com.lugia.timetable.SCHEDULE_REMINDER";
    public static final String ACTION_EVENT_REMINDER    = "com.lugia.timetable.EVENT_REMINDER";
    
    public static final int SCHEDULE_NOTIFICATION_ID = 1;
    public static final int EVENT_NOTIFICATION_ID    = 2;
    
    private static final String TAG = "ReminderService";
    
    public ReminderService()
    {
        super("ReminderService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        String action = intent.getAction();
        
        Log.d("ReminderService", "Handle intent : " + action);
        
        Bundle extra = intent.getExtras();
        
        String subjectCode = extra.getString(EXTRA_SUBJECT_CODE);
        String header      = extra.getString(EXTRA_HEADER);
        String content     = extra.getString(EXTRA_CONTENT);

        String boardcastIntent = null;
        
        Intent notificationIntent = new Intent();
        Uri soundUri = null;
        
        int notificationId;
        boolean vibrate;
        
        if (action.equals(ACTION_SCHEDULE_REMINDER))
        {
            notificationId = SCHEDULE_NOTIFICATION_ID;
            
            vibrate = SettingActivity.getBoolean(ReminderService.this, SettingActivity.KEY_SCHEDULE_NOTIFY_VIBRATE, false);
            
            String soundUriStr = SettingActivity.getString(ReminderService.this, SettingActivity.KEY_SCHEDULE_NOTIFY_SOUND, "");
            
            notificationIntent.setClass(ReminderService.this, SubjectDetailActivity.class)
                              .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                              .putExtra(SubjectDetailActivity.EXTRA_SUBJECT_CODE, subjectCode);
            
            soundUri = !TextUtils.isEmpty(soundUriStr) ? Uri.parse(soundUriStr) : null;
            
            boardcastIntent = ReminderReceiver.ACTION_UPDATE_SCHEDULE_REMINDER;
        }
        else if (action.equals(ACTION_EVENT_REMINDER))
        {
            long eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1);
            
            notificationId = EVENT_NOTIFICATION_ID;
            
            vibrate = SettingActivity.getBoolean(ReminderService.this, SettingActivity.KEY_EVENT_NOTIFY_VIBRATE, false);

            String soundUriStr = SettingActivity.getString(ReminderService.this, SettingActivity.KEY_EVENT_NOTIFY_SOUND, "");

            notificationIntent.setClass(ReminderService.this, SubjectDetailActivity.class)
                              .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                              .setAction(SubjectDetailActivity.ACTION_VIEW_EVENT)
                              .putExtra(SubjectDetailActivity.EXTRA_EVENT_ID, eventId)
                              .putExtra(SubjectDetailActivity.EXTRA_SUBJECT_CODE, subjectCode);

            soundUri = !TextUtils.isEmpty(soundUriStr) ? Uri.parse(soundUriStr) : null;

            boardcastIntent = ReminderReceiver.ACTION_UPDATE_EVENT_REMINDER;
        }
        else
        {
            Log.e(TAG, "Unknow action!");
            
            return;
        }
        
        PendingIntent pendingIntent = PendingIntent.getActivity(ReminderService.this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(ReminderService.this)
                                                          .setSmallIcon(R.drawable.ic_notification_reminder)
                                                          .setTicker(header)
                                                          .setContentTitle(header)
                                                          .setContentText(content)
                                                          .setContentIntent(pendingIntent)
                                                          .setAutoCancel(true)
                                                          .setWhen(System.currentTimeMillis())
                                                          .build();
        
        // always show the notification light
        notification.defaults |= Notification.DEFAULT_LIGHTS;
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        
        // only vibrate when user enable it
        if (vibrate)
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        
        // set the notification sound
        notification.sound = soundUri;

        NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        
        manager.notify(notificationId, notification);
        
        // update the reminder, so it will notify user again on next schedule
        Intent broadcastIntent = new Intent(ReminderService.this, ReminderReceiver.class);
        broadcastIntent.setAction(boardcastIntent);
        
        sendBroadcast(broadcastIntent);
    }
}
