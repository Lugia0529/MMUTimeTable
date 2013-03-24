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
import java.util.ArrayList;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;

public class SubjectDetailActivity extends Activity
{
    private RelativeLayout mHeaderLayout;
    
    private LinearLayout mLectureSectionLayout;
    private LinearLayout mTutorialSectionLayout;
    
    private View mLectureDividerView;
    private View mTutorialDividerView;
    
    private TextView mSubjectTitleTextView;
    private TextView mLectureSectionTextView;
    private TextView mTutorialSectionTextView;
    private TextView mCreditHoursTextView;
    
    private SubjectList mSubjectList;
    private Subject mSubject;
    
    private LayoutInflater mLayoutInflater;
    
    public static final String[] WEEKS = new String[]
    {
        "Sunday",
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday"
    };
    
    public static final String[] TIMES = new String[]
    {
        "12AM", "1AM", "2AM", "3AM", "4AM", "5AM", "6AM", "7AM", "8AM", "9AM", "10AM", "11AM",
        "12PM", "1PM", "2PM", "3PM", "4PM", "5PM", "6PM", "7PM", "8PM", "9PM", "10PM", "11PM",
    };
    
    private static final String TAG = "SubjectDetailActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_detail);
        
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        
        mLayoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        mHeaderLayout = (RelativeLayout)findViewById(R.id.layout_header);
        
        mLectureSectionLayout = (LinearLayout)findViewById(R.id.layout_lecture_section);
        mTutorialSectionLayout = (LinearLayout)findViewById(R.id.layout_tutorial_section);
        
        mLectureDividerView = (View)findViewById(R.id.view_lecture_divider);
        mTutorialDividerView = (View)findViewById(R.id.view_tutorial_divider);
        
        mSubjectTitleTextView    = (TextView)findViewById(R.id.text_subject_title);
        mLectureSectionTextView  = (TextView)findViewById(R.id.text_lecture_section);
        mTutorialSectionTextView = (TextView)findViewById(R.id.text_tutorial_section);
        mCreditHoursTextView     = (TextView)findViewById(R.id.text_credit_hour);
        
        Bundle intentExtra = getIntent().getExtras();
        
        if (intentExtra.containsKey(MasterActivity.EXTRA_FILE_NAME))
            loadFile(intentExtra.getString(MasterActivity.EXTRA_FILE_NAME));
        else
            loadFileFromSystem();
        
        String subjectCode = intentExtra.getString("subjectCode");
        
        mSubject = mSubjectList.findSubject(subjectCode);
        
        String subjectDescription = mSubject.getSubjectDescription();
        String lectureSection     = mSubject.getLectureSection();
        String tutorialSection    = mSubject.getTutorialSection();
        
        int creditHours = mSubject.getCreditHours();
        int color       = mSubject.getColor();
        
        mHeaderLayout.setBackgroundColor(color);
        
        mLectureDividerView.setBackgroundColor(color);
        mTutorialDividerView.setBackgroundColor(color);
        
        mSubjectTitleTextView.setText(subjectCode + " - " + subjectDescription);
        mLectureSectionTextView.setText(lectureSection);
        mTutorialSectionTextView.setText(tutorialSection);
        mCreditHoursTextView.setText(creditHours + " Credit Hours");
        
        // hide the lecture section detail if this course dont have lecture section
        if (!mSubject.hasLectureSection())
        {
            mLectureSectionTextView.setVisibility(View.GONE);
            mLectureSectionLayout.setVisibility(View.GONE);
        }
        
        // hide the tutorial section detail if this course dont have tutorial section
        if (!mSubject.hasTutorialSection())
        {
            mTutorialSectionTextView.setVisibility(View.GONE);
            mTutorialSectionLayout.setVisibility(View.GONE);
        }
        
        createTimeTableList();
    }
    
    private void createTimeTableList()
    {
        ArrayList<Schedule> schedules = mSubject.getSchedules();
        
        int lectureCount = 0;
        int tutorialCount = 0;
        
        for (Schedule schedule : schedules)
        {
            View view = mLayoutInflater.inflate(R.layout.item_time, null);
            
            TextView dayTextView = (TextView)view.findViewById(R.id.text_day);
            TextView timeTextView = (TextView)view.findViewById(R.id.text_time);
            TextView roomTextView = (TextView)view.findViewById(R.id.text_room);
            
            dayTextView.setText("Day: " + WEEKS[schedule.getDay()]);
            timeTextView.setText("Time: " + TIMES[schedule.getTime()] + " - " + TIMES[schedule.getTime() + schedule.getLength()]);
            roomTextView.setText("Room: " + schedule.getRoom());
            
            if (schedule.getSection() == Schedule.LECTURE_SECTION && mSubject.hasLectureSection())
            {
                if (lectureCount++ > 0)
                {
                    View divider = mLayoutInflater.inflate(R.layout.item_divider, null);
                    mLectureSectionLayout.addView(divider);
                }
                
                mLectureSectionLayout.addView(view);
                
                continue;
            }
            
            if (schedule.getSection() == Schedule.TUTORIAL_SECTION && mSubject.hasTutorialSection())
            {
                if (tutorialCount++ > 0)
                {
                    View divider = mLayoutInflater.inflate(R.layout.item_divider, null);
                    mTutorialSectionLayout.addView(divider);
                }
                
                mTutorialSectionLayout.addView(view);
                
                continue;
            }
        }
    }
    
    private boolean loadFileFromSystem()
    {
        File file = new File(getFilesDir(), MasterActivity.SAVEFILE);
        
        if (!file.exists())
            return false;
        
        try
        {
            FileInputStream in = openFileInput(MasterActivity.SAVEFILE);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            
            StringBuilder builder = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) 
                builder.append(line);
            
            reader.close();
            
            mSubjectList = new SubjectList(builder.toString());
        }
        catch (Exception e)
        {
            // something went wrong
            Log.e(TAG, "Error on load from system!", e);
            
            return false;
        }
        
        return true;
    }
    
    private boolean loadFile(final String filepath)
    {
        try
        {
            File file = new File(filepath);
            
            // check for file availability, if no exist, stop loading
            if (!file.exists())
            {
                Toast.makeText(SubjectDetailActivity.this, "Fail to open file because it is not exist.", Toast.LENGTH_LONG).show();
                
                return false;
            }
            
            // create reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            
            // require to read file
            StringBuilder sb = new StringBuilder();
            String line;
            
            // read the file line by line
            while ((line = reader.readLine()) != null) 
                sb.append(line);
            
            // done reading, close the file
            reader.close();
            
            mSubjectList = new SubjectList(sb.toString());
        }
        catch (Exception e)
        {
            // fail to load
            Log.e(TAG, "Error on load", e);
            
            return false;
        }
        
        return true;
    }
}
