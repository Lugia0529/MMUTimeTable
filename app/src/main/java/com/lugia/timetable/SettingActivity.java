/*
 * Copyright (c) 2014 Lugia Programming Team
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.RingtonePreference;
import android.text.TextUtils;

public class SettingActivity extends Activity
{
    // Schedule
    public static final String KEY_SCHEDULE_NOTIFICATION   = "preference_schedule_notification";
    public static final String KEY_SCHEDULE_NOTIFY_SOUND   = "preference_schedule_notify_sound";
    public static final String KEY_SCHEDULE_NOTIFY_VIBRATE = "preference_schedule_notify_vibrate";
    public static final String KEY_SCHEDULE_NOTIFY_BEFORE  = "preference_schedule_notify_before";
    
    // Event
    public static final String KEY_EVENT_NOTIFICATION   = "preference_event_notification";
    public static final String KEY_EVENT_NOTIFY_SOUND   = "preference_event_notify_sound";
    public static final String KEY_EVENT_NOTIFY_VIBRATE = "preference_event_notify_vibrate";
    public static final String KEY_EVENT_NOTIFY_BEFORE  = "preference_event_notify_before";
    
    public static final String SHARED_PREFERENCES_NAME = "com.lugia.timetable_preferences";
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingPreferenceFragment()).commit();
    }
    
    public static SharedPreferences getSharedPreferences(Context context)
    {
        return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
    
    public static boolean getBoolean(Context context, String key, boolean defValue)
    {
        return getSharedPreferences(context).getBoolean(key, defValue);
    }
    
    public static float getFloat(Context context, String key, float defValue)
    {
        return getSharedPreferences(context).getFloat(key, defValue);
    }
    
    public static int getInt(Context context, String key, int defValue)
    {
        return getSharedPreferences(context).getInt(key, defValue);
    }
    
    public static long getLong(Context context, String key, long defValue)
    {
        return getSharedPreferences(context).getLong(key, defValue);
    }
    
    public static String getString(Context context, String key, String defValue)
    {
        return getSharedPreferences(context).getString(key, defValue);
    }
    
    // ======================================================
    // PreferenceFragment
    // ======================================================
    public static class SettingPreferenceFragment extends PreferenceFragment implements OnPreferenceChangeListener
    {
        // schedule
        private CheckBoxPreference mScheduleNotification;
        private RingtonePreference mScheduleNotifySound;
        private CheckBoxPreference mScheduleNotifyVibrate;
        private ListPreference     mScheduleNotifyBefore;
        
        // Event
        private CheckBoxPreference mEventNotification;
        private RingtonePreference mEventNotifySound;
        private CheckBoxPreference mEventNotifyVibrate;
        private ListPreference     mEventNotifyBefore;
        
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            final PreferenceManager preferenceManager = getPreferenceManager();
            preferenceManager.setSharedPreferencesName(SHARED_PREFERENCES_NAME);
            
            addPreferencesFromResource(R.xml.setting_preference);
            
            final PreferenceScreen preferenceScreen = getPreferenceScreen();
            
            String soundUri;
            
            // Schedule
            mScheduleNotification  = (CheckBoxPreference)preferenceScreen.findPreference(KEY_SCHEDULE_NOTIFICATION);
            mScheduleNotifySound   = (RingtonePreference)preferenceScreen.findPreference(KEY_SCHEDULE_NOTIFY_SOUND);
            mScheduleNotifyVibrate = (CheckBoxPreference)preferenceScreen.findPreference(KEY_SCHEDULE_NOTIFY_VIBRATE);
            mScheduleNotifyBefore  = (ListPreference)preferenceScreen.findPreference(KEY_SCHEDULE_NOTIFY_BEFORE);
            
            soundUri = SettingActivity.getString(getActivity(), KEY_SCHEDULE_NOTIFY_SOUND, "");
            
            mScheduleNotifySound.setSummary(getRingtoneSummary(soundUri));
            mScheduleNotifyBefore.setSummary(mScheduleNotifyBefore.getEntry());
            
            // Event
            mEventNotification  = (CheckBoxPreference)preferenceScreen.findPreference(KEY_EVENT_NOTIFICATION);
            mEventNotifySound   = (RingtonePreference)preferenceScreen.findPreference(KEY_EVENT_NOTIFY_SOUND);
            mEventNotifyVibrate = (CheckBoxPreference)preferenceScreen.findPreference(KEY_EVENT_NOTIFY_VIBRATE);
            mEventNotifyBefore  = (ListPreference)preferenceScreen.findPreference(KEY_EVENT_NOTIFY_BEFORE);

            soundUri = SettingActivity.getString(getActivity(), KEY_EVENT_NOTIFY_SOUND, "");

            mEventNotifySound.setSummary(getRingtoneSummary(soundUri));
            mEventNotifyBefore.setSummary(mEventNotifyBefore.getEntry());
            
            setOnPreferenceChangeListener(SettingPreferenceFragment.this);
            
            updateChildPreference();
        }
        
        @Override
        public void onStop()
        {
            // unregister all listener
            setOnPreferenceChangeListener(null);
            
            super.onStop();
        }
        
        public boolean onPreferenceChange(Preference preference, Object newValue)
        {
            // use to determine we need setup the reminder again or not
            boolean updateScheduleReminder = false;
            boolean updateEventReminder = false;
            
            // Schedule
            if (preference == mScheduleNotification)
            {
                mScheduleNotification.setChecked((Boolean) newValue);
                
                // update reminder, either enable or cancel
                updateScheduleReminder = true;
            }
            else if (preference == mScheduleNotifySound)
            {
                mScheduleNotifySound.setSummary(getRingtoneSummary((String) newValue));
            }
            else if (preference == mScheduleNotifyBefore)
            {
                mScheduleNotifyBefore.setValue((String) newValue);
                mScheduleNotifyBefore.setSummary(mScheduleNotifyBefore.getEntry());
                
                // update reminder, to match user setting
                updateScheduleReminder = true;
            }
            
            // Event
            if (preference == mEventNotification)
            {
                mEventNotification.setChecked((Boolean) newValue);

                // update reminder, either enable or cancel
                updateEventReminder = true;
            }
            else if (preference == mEventNotifySound)
            {
                mEventNotifySound.setSummary(getRingtoneSummary((String) newValue));
            }
            else if (preference == mEventNotifyBefore)
            {
                mEventNotifyBefore.setValue((String) newValue);
                mEventNotifyBefore.setSummary(mEventNotifyBefore.getEntry());

                // update reminder, to match user setting
                updateEventReminder = true;
            }
            
            updateChildPreference();
            
            // update the reminder if it is require
            if (updateScheduleReminder)
            {
                Intent broadcastIntent = new Intent(getActivity(), ReminderReceiver.class);
                broadcastIntent.setAction(ReminderReceiver.ACTION_UPDATE_SCHEDULE_REMINDER);
                
                getActivity().sendBroadcast(broadcastIntent);
            }
            
            if (updateEventReminder)
            {
                Intent broadcastIntent = new Intent(getActivity(), ReminderReceiver.class);
                broadcastIntent.setAction(ReminderReceiver.ACTION_UPDATE_EVENT_REMINDER);

                getActivity().sendBroadcast(broadcastIntent);
            }
            
            return true;
        }
        
        public void setOnPreferenceChangeListener(OnPreferenceChangeListener listener)
        {
            mScheduleNotification.setOnPreferenceChangeListener(listener);
            mScheduleNotifySound.setOnPreferenceChangeListener(listener);
            mScheduleNotifyBefore.setOnPreferenceChangeListener(listener);

            mEventNotification.setOnPreferenceChangeListener(listener);
            mEventNotifySound.setOnPreferenceChangeListener(listener);
            mEventNotifyBefore.setOnPreferenceChangeListener(listener);
        }
        
        private String getRingtoneSummary(String soundUri)
        {
            if (TextUtils.isEmpty(soundUri))
                return "";
            
            Ringtone ring = RingtoneManager.getRingtone(getActivity(), Uri.parse(soundUri));
            
            if (ring != null)
                return ring.getTitle(getActivity());
            
            return "";
        }
        
        private void updateChildPreference()
        {
            final boolean scheduleNotificationEnabled = mScheduleNotification.isChecked();
            final boolean eventNotificationEnabled = mEventNotification.isChecked();

            mScheduleNotifySound.setEnabled(scheduleNotificationEnabled);
            mScheduleNotifyVibrate.setEnabled(scheduleNotificationEnabled);
            mScheduleNotifyBefore.setEnabled(scheduleNotificationEnabled);

            mEventNotifySound.setEnabled(eventNotificationEnabled);
            mEventNotifyVibrate.setEnabled(eventNotificationEnabled);
            mEventNotifyBefore.setEnabled(eventNotificationEnabled);
        }
    }
}
