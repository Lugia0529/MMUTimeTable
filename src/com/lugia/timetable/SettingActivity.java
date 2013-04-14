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
    public static final String KEY_NOTIFICATION   = "preference_notification";
    public static final String KEY_NOTIFY_SOUND   = "preference_notify_sound";
    public static final String KEY_NOTIFY_VIBRATE = "preference_notify_vibrate";
    public static final String KEY_NOTIFY_BEFORE  = "preference_notify_before";
    
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
        private CheckBoxPreference mNotification;
        private RingtonePreference mNotifySound;
        private CheckBoxPreference mNotifyVibrate;
        private ListPreference mNotifyBefore;
        
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            final PreferenceManager preferenceManager = getPreferenceManager();
            preferenceManager.setSharedPreferencesName(SHARED_PREFERENCES_NAME);
            
            addPreferencesFromResource(R.xml.setting_preference);
            
            final PreferenceScreen preferenceScreen = getPreferenceScreen();
            
            mNotification  = (CheckBoxPreference)preferenceScreen.findPreference(KEY_NOTIFICATION);
            mNotifySound   = (RingtonePreference)preferenceScreen.findPreference(KEY_NOTIFY_SOUND);
            mNotifyVibrate = (CheckBoxPreference)preferenceScreen.findPreference(KEY_NOTIFY_VIBRATE);
            mNotifyBefore  = (ListPreference)preferenceScreen.findPreference(KEY_NOTIFY_BEFORE);
            
            String soundUri = SettingActivity.getString(getActivity(), KEY_NOTIFY_SOUND, "");
            
            mNotifySound.setSummary(getRingtoneSummary(soundUri));
            mNotifyBefore.setSummary(mNotifyBefore.getEntry());
            
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
            boolean updateReminder = false;
            
            if (preference == mNotification)
            {
                mNotification.setChecked((Boolean)newValue);
                
                // update reminder, either enable or cancel
                updateReminder = true;
            }
            else if (preference == mNotifySound)
            {
                mNotifySound.setSummary(getRingtoneSummary((String)newValue));
            }
            else if (preference == mNotifyBefore)
            {
                mNotifyBefore.setValue((String)newValue);
                mNotifyBefore.setSummary(mNotifyBefore.getEntry());
                
                // update reminder, to match user setting
                updateReminder = true;
            }
            
            updateChildPreference();
            
            // update the reminder if it is require
            if (updateReminder)
            {
                Intent broadcastIntent = new Intent(getActivity(), ReminderReceiver.class);
                broadcastIntent.setAction(ReminderReceiver.ACTION_UPDATE_REMINDER);
                
                getActivity().sendBroadcast(broadcastIntent);
            }
            
            return true;
        }
        
        public void setOnPreferenceChangeListener(OnPreferenceChangeListener listener)
        {
            mNotification.setOnPreferenceChangeListener(listener);
            mNotifySound.setOnPreferenceChangeListener(listener);
            mNotifyBefore.setOnPreferenceChangeListener(listener);
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
            if (mNotification.isChecked())
            {
                mNotifySound.setEnabled(true);
                mNotifyVibrate.setEnabled(true);
                mNotifyBefore.setEnabled(true);
            }
            else
            {
                mNotifySound.setEnabled(false);
                mNotifyVibrate.setEnabled(false);
                mNotifyBefore.setEnabled(false);
            }
        }
    }
}
