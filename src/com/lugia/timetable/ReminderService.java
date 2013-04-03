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
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * IntentService for reminder notification.
 */
public class ReminderService extends IntentService
{
    public static final String EXTRA_SUBJECT_CODE = "com.lugia.timetable.SubjectCode";
    public static final String EXTRA_HEADER       = "com.lugia.timetable.Header";
    public static final String EXTRA_CONTENT      = "com.lugia.timetable.Content";
    
    public static final int NOTIFICATION_ID = 1;
    
    public ReminderService()
    {
        super("ReminderService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        Log.d("ReminderService", "Handle intent");
        
        Bundle extra = intent.getExtras();
        
        String subjectCode = extra.getString(EXTRA_SUBJECT_CODE);
        String header      = extra.getString(EXTRA_HEADER);
        String content     = extra.getString(EXTRA_CONTENT);
        
        NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        
        Intent notificationIntent = new Intent(ReminderService.this, SubjectDetailActivity.class);
        notificationIntent.putExtra(SubjectDetailActivity.EXTRA_SUBJECT_CODE, subjectCode);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(ReminderService.this, 0, notificationIntent, 0);
        
        Notification notification = new NotificationCompat.Builder(ReminderService.this)
                                                          .setSmallIcon(R.drawable.ic_notification_reminder)
                                                          .setTicker(header)
                                                          .setContentTitle(header)
                                                          .setContentText(content)
                                                          .setContentIntent(pendingIntent)
                                                          .setDefaults(Notification.DEFAULT_ALL)
                                                          .setAutoCancel(true)
                                                          .setWhen(System.currentTimeMillis())
                                                          .build();
        
        manager.notify(NOTIFICATION_ID, notification);
        
        // update the reminder, so it will notify user again on next schedule
        Intent broadcastIntent = new Intent(ReminderService.this, ReminderReceiver.class);
        broadcastIntent.setAction(ReminderReceiver.ACTION_UPDATE_REMINDER);
        
        sendBroadcast(broadcastIntent);
    }
}
