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

import android.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

/**
 * Main Activity
 */
public class MasterActivity extends FragmentActivity implements ActionBar.OnNavigationListener, DayView.OnDayChangeListener
{
    private String mFilename;
    
    /**
     * The serialization (saved instance state) Bundle key representing the
     * current dropdown position.
     */
    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
    
    public static final String EXTRA_FILE_NAME = "open_file";
    
    public static final int NAV_DAY  = 0;
    public static final int NAV_WEEK = 1;
    public static final int NAV_LIST = 2;
    
    public static final int REQUEST_CODE_OPEN_FILE     = 1;
    public static final int REQUEST_CODE_DOWNLOAD_DATA = 2;
    
    public static final String SAVEFILE = "data.ttg";
    
    private static final String TAG = "MasterActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);
        
        mFilename = null;
        
        SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(MasterActivity.this, R.array.navigation_action_list, android.R.layout.simple_spinner_dropdown_item);
        
        // Set up the action bar to show a dropdown list.
        final ActionBar actionBar = getActionBar();
        
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        
        // Set up the dropdown list navigation in the action bar.
        actionBar.setListNavigationCallbacks(spinnerAdapter, this);
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        
        // force fragment to reload every time we back to this activity
        /* TODO:
         * actually we should not force fragment to reload every time we back to this activity, 
         * fragment should be reloaded when user change file or something.
         */
        loadFragment(getActionBar().getSelectedNavigationIndex());
    }
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        // Restore the previously serialized current dropdown position.
        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM))
            getActionBar().setSelectedNavigationItem(savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        // Serialize the current dropdown position.
        outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar().getSelectedNavigationIndex());
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.master, menu);
        
        return true;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode != RESULT_OK)
        {
            Log.d(TAG, "Non RESULT_OK received: " + resultCode);
            
            return;
        }
        
        switch (requestCode)
        {
            case REQUEST_CODE_OPEN_FILE:
            {
                Uri selectedFileUri = data.getData();
                
                String path = selectedFileUri.getPath();
                
                // some file choose may need other method to get path
                if (path == null || path.equals(""))
                    return;
                
                mFilename = path;
                
                break;
            }
            
            case REQUEST_CODE_DOWNLOAD_DATA:
            {
                if (mFilename != null)
                    mFilename = null;
                
                break;
            }
        }
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item)
    {
        if (item.getItemId() == R.id.action_load_file)
        {
            Intent intent = new Intent();
            
            // create an intent to let user choose file to open
            intent.setAction(Intent.ACTION_GET_CONTENT)
                  .addCategory(Intent.CATEGORY_OPENABLE)
                  .setType("file/*");
            
            startActivityForResult(intent, REQUEST_CODE_OPEN_FILE);
            
            return true;
        }
        
        if (item.getItemId() == R.id.action_download_data)
        {
            Intent intent = new Intent(MasterActivity.this, LoginActivity.class);
            startActivityForResult(intent, REQUEST_CODE_DOWNLOAD_DATA);
            
            return true;
        }
        
        return false;
    }
    
    public void onDayChange(int day)
    {
        Log.d(TAG, "onDayChange: " + day);
    }
    
    private void loadFragment(int position)
    {
        Fragment fragment = null;
        
        switch (position)
        {
            case NAV_DAY:
                fragment = new DayFragment();
                break;
           
            case NAV_WEEK:
                fragment = new WeekFragment();
                break;
            
            case NAV_LIST:
                fragment = new SubjectListFragment();
                break;
                
            default:
                return;
        }
        
        Bundle bundle = new Bundle();
        
        if (mFilename != null)
            bundle.putString(EXTRA_FILE_NAME, mFilename);
        
        fragment.setArguments(bundle);
        
        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }
    
    public boolean onNavigationItemSelected(int position, long id)
    {
        // When the given dropdown item is selected, show its contents in the
        // container view.
        loadFragment(position);
               
        return true;
    }
}
