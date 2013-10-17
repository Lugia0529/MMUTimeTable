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

import android.text.format.DateFormat;

import java.util.Calendar;

public final class Utils
{
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
}
