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
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;

public class SettingActivity extends Activity
{
    public static final String KEY_NOTIFICATION  = "preference_notification";
    public static final String KEY_NOTIFY_BEFORE = "preference_notify_before";
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingPreferenceFragment()).commit();
    }
    
    // ======================================================
    // PreferenceFragment
    // ======================================================
    public static class SettingPreferenceFragment extends PreferenceFragment implements OnPreferenceChangeListener
    {
        private CheckBoxPreference mNotification;
        private ListPreference mNotifyBefore;
        
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            
            addPreferencesFromResource(R.xml.setting_preference);
            
            final PreferenceScreen preferenceScreen = getPreferenceScreen();
            
            mNotification = (CheckBoxPreference)preferenceScreen.findPreference("preference_notification");
            mNotifyBefore = (ListPreference)preferenceScreen.findPreference("preference_notify_before");
            
            mNotifyBefore.setSummary(mNotifyBefore.getEntry());
            
            mNotification.setOnPreferenceChangeListener(SettingPreferenceFragment.this);
            mNotifyBefore.setOnPreferenceChangeListener(SettingPreferenceFragment.this);
            
            updateChildPreference();
        }
        
        @Override
        public void onStop()
        {
            // unregister all listener
            mNotification.setOnPreferenceChangeListener(null);
            mNotifyBefore.setOnPreferenceChangeListener(null);
            
            super.onStop();
        }
        
        public boolean onPreferenceChange(Preference preference, Object newValue)
        {
            boolean updateReminder = false;
            
            if (preference == mNotification)
            {
                mNotification.setChecked((Boolean)newValue);
                
                updateReminder = true;
            }
            else if (preference == mNotifyBefore)
            {
                mNotifyBefore.setValue((String)newValue);
                mNotifyBefore.setSummary(mNotifyBefore.getEntry());
                
                updateReminder = true;
            }
            
            updateChildPreference();
            
            if (updateReminder)
            {
                Intent broadcastIntent = new Intent(getActivity(), ReminderReceiver.class);
                broadcastIntent.setAction(ReminderReceiver.ACTION_UPDATE_REMINDER);
                
                getActivity().sendBroadcast(broadcastIntent);
            }
            
            return true;
        }
        
        private void updateChildPreference()
        {
            if (mNotification.isChecked())
            {
                mNotifyBefore.setEnabled(true);
            }
            else
            {
                mNotifyBefore.setEnabled(false);
            }
        }
    }
}
