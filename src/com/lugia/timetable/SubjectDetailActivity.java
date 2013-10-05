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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SubjectDetailActivity extends FragmentActivity
{
    private static Subject mSubject;
    
    public static final String EXTRA_EVENT_ID     = "com.lugia.timetable.EventId";
    public static final String EXTRA_SUBJECT_CODE = "com.lugia.timetable.SubjectCode";
    
    public static final String ACTION_VIEW_EVENT  = "com.lugia.timetabele.ViewEvent";
    
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

        PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager());
        
        ViewPager viewPager = (ViewPager)findViewById(R.id.pager);
        PagerTabStrip tabStrip = (PagerTabStrip)findViewById(R.id.pager_tab_strip);
        
        RelativeLayout headerLayout = (RelativeLayout)findViewById(R.id.layout_header);
        
        TextView subjectTitleTextView    = (TextView)findViewById(R.id.text_subject_title);
        TextView lectureSectionTextView  = (TextView)findViewById(R.id.text_lecture_section);
        TextView tutorialSectionTextView = (TextView)findViewById(R.id.text_tutorial_section);
        TextView creditHoursTextView     = (TextView)findViewById(R.id.text_credit_hour);

        Bundle intentExtra = getIntent().getExtras();
        
        SubjectList subjectList = SubjectList.getInstance(SubjectDetailActivity.this);
        
        String subjectCode = intentExtra.getString(EXTRA_SUBJECT_CODE);

        mSubject = subjectList.findSubject(subjectCode);
        
        int color = mSubject.getColor();

        String subjectDescription = mSubject.getSubjectDescription();
        String lectureSection     = mSubject.getLectureSection();
        String tutorialSection    = mSubject.getTutorialSection();

        int creditHours = mSubject.getCreditHours();

        viewPager.setAdapter(adapter);
        headerLayout.setBackgroundColor(color);

        tabStrip.setTextColor(color);
        tabStrip.setTabIndicatorColor(color);

        subjectTitleTextView.setText(subjectCode + " - " + subjectDescription);
        lectureSectionTextView.setText(lectureSection);
        tutorialSectionTextView.setText(tutorialSection);
        creditHoursTextView.setText(creditHours + " Credit Hours");
        
        // user click the event reminder notification, show the event detail
        if (getIntent().getAction() != null && getIntent().getAction().equals(ACTION_VIEW_EVENT))
        {
            long eventId = intentExtra.getLong(EXTRA_EVENT_ID, -1);
            
            Event event = mSubject.findEvent(eventId);
            
            if (event != null)
            {
                Bundle args = new Bundle();

                args.putString(EventDetailDialogFragment.EXTRA_SUBJECT_CODE, mSubject.getSubjectCode());
                args.putLong(EventDetailDialogFragment.EXTRA_EVENT_ID, event.getId());
                
                // dont allow event editing in such situation
                args.putBoolean(EventDetailDialogFragment.EXTRA_EDITABLE, false);
                
                EventDetailDialogFragment f = EventDetailDialogFragment.newInstance(args);
                
                f.show(getFragmentManager(), event.getName());
            }
            else
                Toast.makeText(SubjectDetailActivity.this, "No such event.", Toast.LENGTH_SHORT).show();
        }
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
            Intent intent = new Intent(SubjectDetailActivity.this, EventFormActivity.class);

            intent.putExtra(EventFormActivity.EXTRA_SUBJECT_CODE, mSubject.getSubjectCode());

            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
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
            
            Bundle args = new Bundle();
            
            if (i == 0)
                fragment = ScheduleFragment.newInstance(args);
            else
                fragment = EventFragment.newInstance(args);

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
    public static class ScheduleFragment extends Fragment
    {
        public static ScheduleFragment newInstance(Bundle args)
        {
            ScheduleFragment fragment = new ScheduleFragment();
            
            fragment.setArguments(args);
            
            return fragment;
        }
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            View view = inflater.inflate(R.layout.fragment_schedule, container, false);
            
            view.findViewById(R.id.view_lecture_divider).setBackgroundColor(mSubject.getColor());
            view.findViewById(R.id.view_tutorial_divider).setBackgroundColor(mSubject.getColor());
            
            LinearLayout lectureSectionLayout = (LinearLayout)view.findViewById(R.id.layout_lecture_section);
            LinearLayout tutorialSectionLayout = (LinearLayout)view.findViewById(R.id.layout_tutorial_section);
            
            // hide the lecture section detail if this course dont have lecture section
            if (!mSubject.hasLectureSection())
                lectureSectionLayout.setVisibility(View.GONE);

            // hide the tutorial section detail if this course dont have tutorial section
            if (!mSubject.hasTutorialSection())
                tutorialSectionLayout.setVisibility(View.GONE);

            createTimeTableList(mSubject, lectureSectionLayout, tutorialSectionLayout);
            
            return view;
        }

        private void createTimeTableList(Subject subject, LinearLayout lectureSectionLayout, LinearLayout tutorialSectionLayout)
        {
            ArrayList<Schedule> schedules = subject.getSchedules();
            
            int lectureCount = 0;
            int tutorialCount = 0;
            
            LayoutInflater inflater = getActivity().getLayoutInflater();
            
            for (Schedule schedule : schedules)
            {
                View view = inflater.inflate(R.layout.item_time, null);

                TextView dayTextView = (TextView)view.findViewById(R.id.text_day);
                TextView timeTextView = (TextView)view.findViewById(R.id.text_time);
                TextView roomTextView = (TextView)view.findViewById(R.id.text_room);

                dayTextView.setText("Day: " + WEEKS[schedule.getDay()]);
                timeTextView.setText("Time: " + TIMES[schedule.getTime()] + " - " + TIMES[schedule.getTime() + schedule.getLength()]);
                roomTextView.setText("Room: " + schedule.getRoom());

                if (schedule.getSection() == Schedule.LECTURE_SECTION && subject.hasLectureSection())
                {
                    if (lectureCount++ > 0)
                    {
                        View divider = inflater.inflate(R.layout.item_divider, null);
                        lectureSectionLayout.addView(divider);
                    }

                    lectureSectionLayout.addView(view);

                    continue;
                }

                if (schedule.getSection() == Schedule.TUTORIAL_SECTION && subject.hasTutorialSection())
                {
                    if (tutorialCount++ > 0)
                    {
                        View divider = inflater.inflate(R.layout.item_divider, null);
                        tutorialSectionLayout.addView(divider);
                    }

                    tutorialSectionLayout.addView(view);

                    continue;
                }
            }
        }
    }

    // Fragment class for event list
    public static class EventFragment extends Fragment implements AdapterView.OnItemClickListener, EventDetailDialogFragment.OnEventDeletedListener
    {
        private EventAdapter mEventAdapter;
        
        public static EventFragment newInstance(Bundle args)
        {
            EventFragment fragment = new EventFragment();

            fragment.setArguments(args);

            return fragment;
        }
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            View view = inflater.inflate(R.layout.fragment_event, container, false);
            
            ListView listView = (ListView)view.findViewById(R.id.list_event);

            mEventAdapter = new EventAdapter(getActivity(), R.id.text_name);
            mEventAdapter.addAll(mSubject.getEvents());
            
            listView.setEmptyView(view.findViewById(R.id.empty));
            listView.setAdapter(mEventAdapter);
            listView.setOnItemClickListener(EventFragment.this);

            return view;
        }

        @Override
        public void onResume()
        {
            super.onResume();
            
            // update the event list
            if (mEventAdapter != null)
            {
                mEventAdapter.clear();
                mEventAdapter.addAll(mSubject.getEvents());
                mEventAdapter.notifyDataSetChanged();
            }
        }
        
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            Event event = mEventAdapter.getItem(position);
            
            Bundle args = new Bundle();
            
            args.putString(EventDetailDialogFragment.EXTRA_SUBJECT_CODE, mSubject.getSubjectCode());
            args.putLong(EventDetailDialogFragment.EXTRA_EVENT_ID, event.getId());
            
            EventDetailDialogFragment f = EventDetailDialogFragment.newInstance(args);
            f.setEventUpdateListener(EventFragment.this);
            
            f.show(getActivity().getFragmentManager(), event.getName());
        }

        @Override
        public void onEventDeleted()
        {
            // update the event list
            if (mEventAdapter != null)
            {
                mEventAdapter.clear();
                mEventAdapter.addAll(mSubject.getEvents());
                mEventAdapter.notifyDataSetChanged();
            }
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
                    convertView = getActivity().getLayoutInflater().inflate(R.layout.item_event, null);

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
