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
import java.util.List;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
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

    private static int[] mBackgroundColors;
    private static int[] mTextColors;
    private static int[] mBackgrounds;
    
    public static final String[] WEEKS = new String[]
    {
        "SUN",
        "MON",
        "TUE",
        "WED",
        "THU",
        "FRI",
        "SAT"
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

        Resources res = getResources();

        mBackgroundColors = new int[]
        {
            res.getColor(R.color.background_1),
            res.getColor(R.color.background_2),
            res.getColor(R.color.background_3),
            res.getColor(R.color.background_4),
            res.getColor(R.color.background_5),
            res.getColor(R.color.background_6),
            res.getColor(R.color.background_7),
            res.getColor(R.color.background_8)
        };
        
        mTextColors = new int[]
        {
            res.getColor(R.color.border_1),
            res.getColor(R.color.border_2),
            res.getColor(R.color.border_3),
            res.getColor(R.color.border_4),
            res.getColor(R.color.border_5),
            res.getColor(R.color.border_6),
            res.getColor(R.color.border_7),
            res.getColor(R.color.border_8)
        };

        mBackgrounds = new int[]
        {
            R.drawable.subject_background_1,
            R.drawable.subject_background_2,
            R.drawable.subject_background_3,
            R.drawable.subject_background_4,
            R.drawable.subject_background_5,
            R.drawable.subject_background_6,
            R.drawable.subject_background_7,
            R.drawable.subject_background_8
        };
        
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
        
        int colorIndex = mSubject.getColor();

        String subjectDescription = mSubject.getSubjectDescription();
        String lectureSection     = mSubject.getLectureSection();
        String tutorialSection    = mSubject.getTutorialSection();

        int creditHours = mSubject.getCreditHours();

        viewPager.setAdapter(adapter);
        headerLayout.setBackgroundColor(mTextColors[colorIndex]);
        
        tabStrip.setTextColor(mTextColors[colorIndex]);
        tabStrip.setTabIndicatorColor(mTextColors[colorIndex]);

        subjectTitleTextView.setText(subjectCode + " - " + subjectDescription);
        lectureSectionTextView.setText(lectureSection);
        tutorialSectionTextView.setText(tutorialSection);
        creditHoursTextView.setText(creditHours + " Credit Hours");
        
        if (tutorialSection == null)
            tutorialSectionTextView.setVisibility(View.GONE);
        
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
        private ScheduleAdapter mScheduleAdapter;
        
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

            ListView listView = (ListView)view.findViewById(R.id.list_schedule);

            mScheduleAdapter = new ScheduleAdapter(getActivity());
            mScheduleAdapter.addSchedules(mSubject.getSchedules());
            
            listView.setEmptyView(view.findViewById(R.id.empty));
            listView.setAdapter(mScheduleAdapter);
            
            return view;
        }
        
        class ScheduleAdapter extends BaseAdapter
        {
            private int mLectureCount;
            private int mTutorialCount;
            
            private Context mContext;
            
            private List<Schedule> mList;
            
            public ScheduleAdapter(Context context)
            {
                mList = new ArrayList<Schedule>();
                
                mContext = context;
            }
            
            public void addSchedule(Schedule schedule)
            {
                if (schedule == null)
                    throw new IllegalArgumentException("schedule cannot be null!");
                
                if (schedule.getSection() == Schedule.LECTURE_SECTION)
                {
                    // lazily adding the header object
                    if (mLectureCount == 0)
                        mList.add(0, null);
                    
                    mList.add(mLectureCount + 1, schedule);
                    mLectureCount++;
                }
                else if (schedule.getSection() == Schedule.TUTORIAL_SECTION)
                {
                    int pos = getBasePosition(Schedule.TUTORIAL_SECTION);
                    
                    // lazily adding the header object
                    if (mTutorialCount == 0)
                        mList.add(pos, null);
                    
                    mList.add(pos + mTutorialCount + 1, schedule);
                    mTutorialCount++;
                }
                else
                    throw new IllegalArgumentException("Invalid schedule section: " + schedule.getSection());
            }
            
            public void addSchedules(List<Schedule> schedules)
            {
                for (Schedule schedule : schedules)
                    addSchedule(schedule);
            }
            
            @Override
            public int getCount()
            {
                return mList.size();
            }
            
            @Override
            public Object getItem(int position)
            {
                return mList.get(position);
            }

            @Override
            public long getItemId(int position)
            {
                return position;
            }

            @Override
            public int getViewTypeCount()
            {
                return 2;
            }
            
            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                if (convertView == null)
                {
                    LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    
                    Schedule schedule = mList.get(position);
                    
                    // separator
                    if (schedule == null)
                    {
                        convertView = inflater.inflate(R.layout.item_section_divider, null);
                        
                        TextView headerTextView = (TextView)convertView.findViewById(R.id.text_header);
                        
                        headerTextView.setTextColor(mTextColors[mSubject.getColor()]);
                        
                        if (position == getBasePosition(Schedule.LECTURE_SECTION))
                            headerTextView.setText("Lecture Section");
                        else if (position == getBasePosition(Schedule.TUTORIAL_SECTION))
                            headerTextView.setText("Tutorial Section");
                    }
                    else
                    {
                        convertView = inflater.inflate(R.layout.item_schedule, null);
                        
                        convertView.setBackgroundResource(mBackgrounds[mSubject.getColor()]);
                        
                        TextView dayTextView = (TextView)convertView.findViewById(R.id.text_day);
                        TextView timeTextView = (TextView)convertView.findViewById(R.id.text_time);
                        TextView roomTextView = (TextView)convertView.findViewById(R.id.text_room);
                        
                        dayTextView.setText(WEEKS[schedule.getDay()]);
                        timeTextView.setText(TIMES[schedule.getTime()] + " - " + TIMES[schedule.getTime() + schedule.getLength()]);
                        roomTextView.setText(schedule.getRoom());
                        
                        dayTextView.setTextColor(mTextColors[mSubject.getColor()]);
                        timeTextView.setTextColor(mTextColors[mSubject.getColor()]);
                        roomTextView.setTextColor(mTextColors[mSubject.getColor()]);
                    }
                }
                
                return convertView;
            }
            
            @Override
            public boolean hasStableIds()
            {
                return false;
            }

            @Override
            public boolean isEnabled(int position)
            {
                return false;
            }
            
            public boolean hasLectureSection()
            {
                return mLectureCount > 0;
            }
            
            public boolean hasTutorialSection()
            {
                return mTutorialCount > 0;
            }
            
            public int getBasePosition(int section)
            {
                if (section == Schedule.LECTURE_SECTION)
                    return 0;
                
                if (section == Schedule.TUTORIAL_SECTION)
                {
                    if (hasLectureSection())
                        return mLectureCount + 1;
                    else
                        return 0;
                }
                
                return -1;
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
                    convertView.setBackgroundResource(mBackgrounds[mSubject.getColor()]);
                    
                    String dateStr = Utils.getDateString("EE, MMM dd, yyyy", event.getYear(), event.getMonth(), event.getDay());
                    String timeStartStr = Utils.getTimeString("h:mm aa", event.getStartHour(), event.getEndMinute());
                    String timeEndStr = Utils.getTimeString("h:mm aa", event.getEndHour(), event.getEndMinute());
                    
                    TextView nameTextView = (TextView)convertView.findViewById(R.id.text_name);
                    TextView venueTextView = (TextView)convertView.findViewById(R.id.text_venue);
                    TextView timeTextView = (TextView)convertView.findViewById(R.id.text_time);

                    if (event.getVenue().isEmpty())
                        venueTextView.setVisibility(View.GONE);

                    nameTextView.setText(event.getName());
                    venueTextView.setText(event.getVenue());
                    timeTextView.setText(dateStr + ", " + timeStartStr + " - " + timeEndStr);
                    
                    nameTextView.setTextColor(mTextColors[mSubject.getColor()]);
                    venueTextView.setTextColor(mTextColors[mSubject.getColor()]);
                    timeTextView.setTextColor(mTextColors[mSubject.getColor()]);
                }

                return convertView;
            }
        }
    }
}
