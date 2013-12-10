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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Calendar;

public class TimeTableFragment extends Fragment implements TimeTableLayout.OnItemClickListener
{
    private TimeTableLayout mTimeTable;
    
    private static final String TAG = "TimeTableFragment";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "onCreate()");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreateView()");
        
        View view = inflater.inflate(R.layout.fragment_time_table, null);
        
        mTimeTable = (TimeTableLayout)view.findViewById(R.id.time_table);
        
        SubjectList subjects = SubjectList.getInstance(getActivity());
        
        for (Subject subject : subjects)
        {
            for (Schedule schedule: subject.getSchedules())
            {
                View child = inflater.inflate(R.layout.item_time_table_schedule, mTimeTable, false);
                child.setBackgroundColor(subject.getColor());
                child.setTag(subject.getSubjectCode());
                
                TextView subjectCodeTextView = (TextView)child.findViewById(R.id.text_subject_code);
                TextView subjectDescriptionTextView = (TextView)child.findViewById(R.id.text_subject_description);
                TextView sectionTextView = (TextView)child.findViewById(R.id.text_section);
                TextView roomTextView = (TextView)child.findViewById(R.id.text_room);
                
                subjectCodeTextView.setText(subject.getSubjectCode());
                subjectDescriptionTextView.setText(subject.getSubjectDescription());
                sectionTextView.setText(subject.getSection(schedule.getSection()));
                roomTextView.setText(schedule.getRoom());
                
                mTimeTable.addView(child, schedule.getDay(), schedule.getTime(), schedule.getLength());
            }
        }

        mTimeTable.setOnDayChangedListener((MasterActivity)getActivity());
        mTimeTable.setOnItemClickListener(TimeTableFragment.this);

        // set the current day of this view initially according to real world day
        switch (Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        {
            case Calendar.SATURDAY:
            case Calendar.SUNDAY:
                mTimeTable.setCurrentDay(TimeTableLayout.MONDAY);
                break;

            default:
                mTimeTable.setCurrentDay(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1);
                
                // scroll to current time position for user convenient
                mTimeTable.scrollToCurrentTime();
                break;
        }
        
        return view;
    }
    
    public void setDisplayType(int type)
    {
        Log.d(TAG, "setDisplayType()");
        
        if (mTimeTable == null)
            return;
        
        mTimeTable.setDisplayType(type);
        mTimeTable.setHeaderVisibility(type == TimeTableLayout.TYPE_WEEK);
        
        // TODO: should be remove when the layout adapter is implemented
        final int count = mTimeTable.getChildCount();
        
        final int visibility = type == TimeTableLayout.TYPE_DAY ? View.VISIBLE : View.GONE;
        
        for (int i = 0; i < count; i++)
        {
            View child = mTimeTable.getChildAt(i);
            
            child.findViewById(R.id.text_subject_description).setVisibility(visibility);
            child.findViewById(R.id.text_section).setVisibility(visibility);
        }
    }
    
    @Override
    public void onItemClick(View view, int day, int time)
    {
        Intent intent = new Intent(getActivity(), SubjectDetailActivity.class);

        intent.putExtra(SubjectDetailActivity.EXTRA_SUBJECT_CODE, view.getTag().toString());

        startActivity(intent);
    }
}
