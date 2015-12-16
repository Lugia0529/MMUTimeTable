/*
 * Copyright (c) 2014 Lugia Programming Team
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

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

final class Schedule implements Parcelable
{
    private int mSection;
    private int mDay;
    private int mTime;
    private int mLength;
    
    private String mRoom;
    
    public static final int SUNDAY    = 0;
    public static final int MONDAY    = 1;
    public static final int TUESDAY   = 2;
    public static final int WEDNESDAY = 3;
    public static final int THURSDAY  = 4;
    public static final int FRIDAY    = 5;
    public static final int SATURDAY  = 6;
    
    public static final int LECTURE_SECTION  = 0;
    public static final int TUTORIAL_SECTION = 1;
    
    public static final String JSON_DAY     = "day";
    public static final String JSON_TIME    = "time";
    public static final String JSON_LENGTH  = "length";
    public static final String JSON_SECTION = "section";
    public static final String JSON_ROOM    = "room";
    
    private static final String TAG = "Schedule";
    
    public Schedule(int section, int day, int time, int length, String room)
    {
        if (section != LECTURE_SECTION && section != TUTORIAL_SECTION)
            throw new IllegalArgumentException("Invalid section value, should be either lecture or tutorial section");
        
        if (day < 1 || day > 7)
            throw new IllegalArgumentException("Invalid day value, should between 1 to 7.");
        
        if (time < 0 || time > 23)
            throw new IllegalArgumentException("Invalid time value, should between 0 to 23.");
        
        if (length < 1)
            throw new IllegalArgumentException("Invalid length value, should larger than or equal to 1.");
        
        this.mDay     = day;
        this.mTime    = time;
        this.mLength  = length;
        this.mSection = section;
        this.mRoom    = room;
    }
    
    public int getDay()
    {
        return this.mDay;
    }
    
    public int getTime()
    {
        return this.mTime;
    }
    
    public int getLength()
    {
        return this.mLength;
    }
    
    public int getSection()
    {
        return mSection;
    }
    
    public String getRoom()
    {
        return this.mRoom;
    }
    
    public void setDay(int day)
    {
        if (day < 1 || day > 7)
            throw new IllegalArgumentException("Invalid day value, should between 1 to 7.");
        
        this.mDay = day;
    }
    
    public void setTime(int time)
    {
        if (time < 0 || time > 23)
            throw new IllegalArgumentException("Invalid time value, should between 0 to 23.");
        
        this.mTime = time;
    }
    
    public void setLength(int length)
    {
        if (length < 1)
            throw new IllegalArgumentException("Invalid length value, should larger than or equal to 1.");
        
        this.mLength = length;
    }
    
    public void setSection(int section)
    {
        this.mSection = section;
    }
    
    public void setRoom(String room)
    {
        this.mRoom = room;
    }
    
    public JSONObject getJSONObject()
    {
        JSONObject json = new JSONObject();
        
        try
        {
            json.put(JSON_DAY,     mDay);
            json.put(JSON_TIME,    mTime);
            json.put(JSON_LENGTH,  mLength);
            json.put(JSON_SECTION, mSection);
            json.put(JSON_ROOM,    mRoom);
        }
        catch (JSONException e)
        {
            // Something went wrong
            return null;
        }
        
        return json;
    }
    
    public static Schedule restoreFromJSON(JSONObject json) throws JSONException
    {
        int day     = json.getInt(JSON_DAY);
        int time    = json.getInt(JSON_TIME);
        int length  = json.getInt(JSON_LENGTH);
        int section = json.getInt(JSON_SECTION);
        
        String room = json.getString(JSON_ROOM);

        return new Schedule(section, day, time, length, room);
    }

    //=======================================
    // Parcelable
    // =======================================

    /**
     * Constructor for Parcelable.
     *
     * @param parcel the parcel.
     */
    private Schedule(Parcel parcel)
    {
        mSection = parcel.readInt();
        mDay     = parcel.readInt();
        mTime    = parcel.readInt();
        mLength  = parcel.readInt();
        
        mRoom    = parcel.readString();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mSection);
        dest.writeInt(mDay);
        dest.writeInt(mTime);
        dest.writeInt(mLength);
        
        dest.writeString(mRoom);
    }

    public static final Parcelable.Creator<Schedule> CREATOR = new Parcelable.Creator<Schedule>()
    {
        public Schedule createFromParcel(Parcel source)
        {
            return new Schedule(source);
        }

        public Schedule[] newArray(int size)
        {
            return new Schedule[size];
        }
    };
}
