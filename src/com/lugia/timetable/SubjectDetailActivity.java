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
import java.util.Calendar;

import android.app.ActionBar;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SubjectDetailActivity extends FragmentActivity
{
    private RelativeLayout mHeaderLayout;

    private ViewPager mViewPager;
    private PagerTabStrip mTabStrip;

    private TextView mSubjectTitleTextView;
    private TextView mLectureSectionTextView;
    private TextView mTutorialSectionTextView;
    private TextView mCreditHoursTextView;

    private PagerAdapter mAdapter;

    private SubjectList mSubjectList;
    private Subject mSubject;

    private int mColor;

    public static final String EXTRA_SUBJECT_CODE = "com.lugia.timetable.SubjectCode";
    
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

        mAdapter = new PagerAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager)findViewById(R.id.pager);
        mTabStrip = (PagerTabStrip)findViewById(R.id.pager_tab_strip);

        mHeaderLayout = (RelativeLayout)findViewById(R.id.layout_header);

        mSubjectTitleTextView    = (TextView)findViewById(R.id.text_subject_title);
        mLectureSectionTextView  = (TextView)findViewById(R.id.text_lecture_section);
        mTutorialSectionTextView = (TextView)findViewById(R.id.text_tutorial_section);
        mCreditHoursTextView     = (TextView)findViewById(R.id.text_credit_hour);

        Bundle intentExtra = getIntent().getExtras();

        if (intentExtra.containsKey(MasterActivity.EXTRA_FILE_NAME))
            loadFile(intentExtra.getString(MasterActivity.EXTRA_FILE_NAME));
        else
            loadFileFromSystem();

        String subjectCode = intentExtra.getString(EXTRA_SUBJECT_CODE);

        mSubject = mSubjectList.findSubject(subjectCode);
        mColor   = mSubject.getColor();

        String subjectDescription = mSubject.getSubjectDescription();
        String lectureSection     = mSubject.getLectureSection();
        String tutorialSection    = mSubject.getTutorialSection();

        int creditHours = mSubject.getCreditHours();

        mViewPager.setAdapter(mAdapter);

        mHeaderLayout.setBackgroundColor(mColor);

        mTabStrip.setTextColor(mColor);
        mTabStrip.setTabIndicatorColor(mColor);

        mSubjectTitleTextView.setText(subjectCode + " - " + subjectDescription);
        mLectureSectionTextView.setText(lectureSection);
        mTutorialSectionTextView.setText(tutorialSection);
        mCreditHoursTextView.setText(creditHours + " Credit Hours");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.subject_detail, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            // This is called when the Home (Up) button is pressed in the Action Bar.
            Intent intent = new Intent(this, MasterActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            
            startActivity(intent);
            
            finish();
            
            return true;
        }

        if (item.getItemId() == R.id.action_new_event)
        {
            Intent intent = new Intent(SubjectDetailActivity.this, EditEventActivity.class);

            intent.putExtra(EditEventActivity.EXTRA_SUBJECT_CODE, mSubject.getSubjectCode());

            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
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

    public class PagerAdapter extends FragmentPagerAdapter
    {
        public PagerAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int i)
        {
            Fragment fragment;

            if (i == 0)
                fragment = new ScheduleFragment();
            else
                fragment = new EventFragment();

            return fragment;
        }

        @Override
        public int getCount()
        {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            if (position == 0)
                return "Schedule";
            else
                return "Event";
        }
    }

    // Fragment class for schedule list
    public class ScheduleFragment extends Fragment
    {
        private LinearLayout mLectureSectionLayout;
        private LinearLayout mTutorialSectionLayout;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            View view = inflater.inflate(R.layout.fragment_schedule, container, false);

            mLectureSectionLayout = (LinearLayout)view.findViewById(R.id.layout_lecture_section);
            mTutorialSectionLayout = (LinearLayout)view.findViewById(R.id.layout_tutorial_section);

            view.findViewById(R.id.view_lecture_divider).setBackgroundColor(mColor);
            view.findViewById(R.id.view_tutorial_divider).setBackgroundColor(mColor);

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

            createTimeTableList(inflater);

            return view;
        }

        private void createTimeTableList(LayoutInflater inflater)
        {
            ArrayList<Schedule> schedules = mSubject.getSchedules();

            int lectureCount = 0;
            int tutorialCount = 0;

            for (Schedule schedule : schedules)
            {
                View view = inflater.inflate(R.layout.item_time, null);

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
                        View divider = inflater.inflate(R.layout.item_divider, null);
                        mLectureSectionLayout.addView(divider);
                    }

                    mLectureSectionLayout.addView(view);

                    continue;
                }

                if (schedule.getSection() == Schedule.TUTORIAL_SECTION && mSubject.hasTutorialSection())
                {
                    if (tutorialCount++ > 0)
                    {
                        View divider = inflater.inflate(R.layout.item_divider, null);
                        mTutorialSectionLayout.addView(divider);
                    }

                    mTutorialSectionLayout.addView(view);

                    continue;
                }
            }
        }
    }

    // Fragment class for event list
    public class EventFragment extends Fragment
    {
        private LayoutInflater mLayoutInflater;
        private EventAdapter mEventAdapter;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            mLayoutInflater = inflater;

            View view = inflater.inflate(R.layout.fragment_event, container, false);

            ListView listView = (ListView)view.findViewById(R.id.list_event);

            mEventAdapter = new EventAdapter(SubjectDetailActivity.this, R.id.text_name);
            mEventAdapter.addAll(mSubject.getEvents());

            listView.setEmptyView(view.findViewById(R.id.empty));
            listView.setAdapter(mEventAdapter);

            return view;
        }

        class EventAdapter extends ArrayAdapter<Event>
        {
            public EventAdapter(Context context, int textViewResourceId)
            {
                super(context, textViewResourceId);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                Event event = getItem(position);

                if (convertView == null)
                    convertView = mLayoutInflater.inflate(R.layout.item_event, null);

                if (event != null)
                {
                    final Calendar calendar = Calendar.getInstance();

                    // get the date string
                    calendar.set(Calendar.YEAR, event.getYear());
                    calendar.set(Calendar.MONTH, event.getMonth());
                    calendar.set(Calendar.DAY_OF_MONTH, event.getDay());

                    String dateStr = DateFormat.format("EE, MMM dd, yyyy", calendar).toString();

                    // get the start time string
                    calendar.set(Calendar.HOUR_OF_DAY, event.getStartHour());
                    calendar.set(Calendar.MINUTE, event.getStartMinute());

                    String timeStartStr = DateFormat.format("h:mm aa", calendar).toString();

                    // get the end time string
                    calendar.set(Calendar.HOUR_OF_DAY, event.getEndHour());
                    calendar.set(Calendar.MINUTE, event.getEndMinute());

                    String timeEndStr = DateFormat.format("h:mm aa", calendar).toString();

                    TextView nameTextView = (TextView)convertView.findViewById(R.id.text_name);
                    TextView venueTextView = (TextView)convertView.findViewById(R.id.text_venue);
                    TextView timeTextView = (TextView)convertView.findViewById(R.id.text_time);

                    if (event.getVenue().isEmpty())
                        venueTextView.setVisibility(View.GONE);

                    nameTextView.setText(event.getName());
                    venueTextView.setText(event.getVenue());
                    timeTextView.setText(dateStr + ", " + timeStartStr + " - " + timeEndStr);
                }

                return convertView;
            }
        }
    }
}
