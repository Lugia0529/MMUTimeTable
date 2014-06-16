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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TimeTableSpinnerAdapter extends BaseAdapter
{
    private final LayoutInflater mInflater;
    
    private String[] mViewTypeStrings;
    private String[] mDayStrings;
    
    /**
     * current view type.
     */
    private int mViewType;
    
    /**
     * current viewing day for DayView.
     */
    private int mCurrentDay;
    
    /* View Type */
    public static final int VIEW_TYPE_DAY  = 0;
    public static final int VIEW_TYPE_WEEK = 1;
    public static final int VIEW_TYPE_LIST = 2;
    
    public TimeTableSpinnerAdapter(Context context, int viewType)
    {
        super();
        
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        mViewTypeStrings = context.getResources().getStringArray(R.array.navigation_action_list);
        mDayStrings = context.getResources().getStringArray(R.array.long_day_string);
        
        mViewType = viewType;
    }
    
    public int getCount()
    {
        return mViewTypeStrings.length;
    }

    public Object getItem(int position)
    {
        if (position < mViewTypeStrings.length)
            return mViewTypeStrings[position];
        
        return null;
    }

    public long getItemId(int position)
    {
        return position;
    }
    
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // check we need to inflate the view or not
        if (convertView == null || ((Integer)convertView.getTag()).intValue() != R.layout.action_bar_master)
        {
            convertView = mInflater.inflate(R.layout.action_bar_master, parent, false);
            convertView.setTag(Integer.valueOf(R.layout.action_bar_master));
        }
        
        TextView textView = (TextView)convertView.findViewById(R.id.text_view);
        
        // try to show the current showing day if current view type is DayView
        if (mViewType == VIEW_TYPE_DAY)
            textView.setText(mDayStrings[mCurrentDay]);
        else
            textView.setText(mViewTypeStrings[mViewType]);
        
        return convertView;
    }
    
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent)
    {
        View view = mInflater.inflate(R.layout.action_bar_master_item, parent, false);
        
        TextView textView = (TextView)view.findViewById(R.id.text_view);
        textView.setText(mViewTypeStrings[position]);
        
        return view;
    }
    
    @Override
    public boolean hasStableIds()
    {
        return false;
    }
    
    public void setViewType(int viewType)
    {
        mViewType = viewType;
        notifyDataSetChanged();
    }
    
    public void setCurrentDay(int currentDay)
    {
        mCurrentDay = currentDay;
        notifyDataSetChanged();
    }
}
