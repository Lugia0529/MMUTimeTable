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
import android.content.res.Resources;
import android.text.format.DateFormat;

import java.util.Calendar;

public final class Utils
{
    public static final int SHORT_WEEK_NAME = 0;
    public static final int LONG_WEEK_NAME  = 1;
    
    public static String milliesToDateTimeString(String format, long timeMillies)
    {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillies);
        
        return DateFormat.format(format, calendar).toString();
    }
    
    public static String getDateString(String format, int year, int month, int day)
    {
        final Calendar calendar = Calendar.getInstance();
        
        calendar.set(Calendar.YEAR,         year );
        calendar.set(Calendar.MONTH,        month);
        calendar.set(Calendar.DAY_OF_MONTH, day  );
        
        return DateFormat.format(format, calendar).toString();
    }
    
    public static String getWeekdayString(String format, int day)
    {
        final Calendar calendar = Calendar.getInstance();
        
        calendar.set(Calendar.DAY_OF_WEEK, day);
        
        return DateFormat.format(format, calendar).toString();
    }
    
    public static String getTimeString(String format, int hour)
    {
        return getTimeString(format, hour, 0, 0);
    }
    
    public static String getTimeString(String format, int hour, int minutes)
    {
        return getTimeString(format, hour, minutes, 0);
    }
    
    public static String getTimeString(String format, int hour, int minute, int second)
    {
        final Calendar calendar = Calendar.getInstance();
        
        calendar.set(Calendar.HOUR_OF_DAY, hour  );
        calendar.set(Calendar.MINUTE,      minute);
        calendar.set(Calendar.SECOND,      second);
        
        return DateFormat.format(format, calendar).toString();
    }
    
    // array
    public static String[] getWeekNameString(Context context, int type)
    {
        if (type == SHORT_WEEK_NAME)
            return context.getResources().getStringArray(R.array.short_day_string);
        else
            return context.getResources().getStringArray(R.array.long_day_string);
    }
    
    public static String[] getTimeNameString(Context context)
    {
        return context.getResources().getStringArray(R.array.time_string);
    }
    
    public static int[] getBackgroundColorArrays(Context context)
    {
        Resources res = context.getResources();
        
        return new int[]
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
    }
    
    public static int[] getForegroundColorArrays(Context context)
    {
        Resources res = context.getResources();

        return new int[]
        {
            res.getColor(R.color.foreground_1),
            res.getColor(R.color.foreground_2),
            res.getColor(R.color.foreground_3),
            res.getColor(R.color.foreground_4),
            res.getColor(R.color.foreground_5),
            res.getColor(R.color.foreground_6),
            res.getColor(R.color.foreground_7),
            res.getColor(R.color.foreground_8)
        };
    }
    
    public static int[] getBackgroundDrawableResourceIds()
    {
        return new int[]
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
    }
    
    public static int getBackgroundColor(Context context, int index)
    {
        return getBackgroundColorArrays(context)[index];
    }
    
    public static int getForegroundColor(Context context, int index)
    {
        return getForegroundColorArrays(context)[index];
    }
    
    public static int getBackgroundDrawableResourceId(int index)
    {
        return getBackgroundDrawableResourceIds()[index];
    }
}
