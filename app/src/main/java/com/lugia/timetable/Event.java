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

final class Event implements Parcelable
{
    private String mName;
    private String mVenue;
    private String mNote;

    private long mId;

    private int mType;
    private int mDate;
    private int mTimeStart;
    private int mTimeEnd;

    public static final int TYPE_UNKNOWN      = 0; // invalid
    public static final int TYPE_REPLACEMENT  = 1; // Class replacement
    public static final int TYPE_HOMEWORK     = 2; // Homework
    public static final int TYPE_ASSIGNMENT   = 3; // Assignment due date
    public static final int TYPE_PRESENTATION = 4; // Presentation
    public static final int TYPE_TEST         = 5; // Quiz / Test / Exam

    public static final String JSON_ID          = "id";
    public static final String JSON_NAME        = "name";
    public static final String JSON_VENUE       = "venue";
    public static final String JSON_NOTE        = "note";
    public static final String JSON_TYPE        = "type";
    public static final String JSON_DATE        = "date";
    public static final String JSON_TIME_START  = "timeStart";
    public static final String JSON_TIME_END    = "timeEnd";

    private static final String TAG = "Event";

    public Event(String name, String venue, String note, int type, int date, int timeStart, int timeEnd)
    {
        this.mId        = System.currentTimeMillis();

        this.mName      = name;
        this.mVenue     = venue;
        this.mNote      = note;
        this.mType      = type;
        this.mDate      = date;
        this.mTimeStart = timeStart;
        this.mTimeEnd   = timeEnd;
    }

    public Event(long id, String name, String venue, String note, int type, int date, int timeStart, int timeEnd)
    {
        this.mId        = id;
        this.mName      = name;
        this.mVenue     = venue;
        this.mNote      = note;
        this.mType      = type;
        this.mDate      = date;
        this.mTimeStart = timeStart;
        this.mTimeEnd   = timeEnd;
    }
    
    public long getId()
    {
        return mId;
    }

    public String getName()
    {
        return mName;
    }

    public String getVenue()
    {
        return mVenue;
    }

    public String getNote()
    {
        return mNote;
    }

    public int getType()
    {
        return mType;
    }

    public int getDate()
    {
        return mDate;
    }

    public int getYear()
    {
        return mDate / 10000;
    }

    public int getMonth()
    {
        return mDate / 100 % 100;
    }

    public int getDay()
    {
        return mDate % 100;
    }

    public int getTimeStart()
    {
        return mTimeStart;
    }

    public int getStartHour()
    {
        return mTimeStart / 100;
    }

    public int getStartMinute()
    {
        return mTimeStart % 100;
    }

    public int getTimeEnd()
    {
        return mTimeEnd;
    }

    public int getEndHour()
    {
        return mTimeEnd / 100;
    }

    public int getEndMinute()
    {
        return mTimeEnd % 100;
    }

    public void setName(String name)
    {
        this.mName = name;
    }

    public void setVenue(String venue)
    {
        this.mVenue = venue;
    }

    public void setNote(String note)
    {
        this.mNote = note;
    }

    public void setType(int type)
    {
        this.mType = type;
    }

    public void setDate(int date)
    {
        this.mDate = date;
    }

    public void setTime(int timeStart, int timeEnd)
    {
        this.mTimeStart = timeStart;
        this.mTimeEnd   = timeEnd;
    }

    public JSONObject getJSONObject()
    {
        JSONObject json = new JSONObject();

        try
        {
            json.put(JSON_ID,         mId);
            json.put(JSON_NAME,       mName);
            json.put(JSON_VENUE,      mVenue);
            json.put(JSON_NOTE,       mNote);
            json.put(JSON_TYPE,       mType);
            json.put(JSON_DATE,       mDate);
            json.put(JSON_TIME_START, mTimeStart);
            json.put(JSON_TIME_END ,  mTimeEnd);
        }
        catch (Exception e)
        {
            // something went wrong
            return null;
        }

        return json;
    }

    public static Event restoreFromJSON(JSONObject json) throws JSONException
    {
        long id = json.getLong(JSON_ID);
        
        String name  = json.getString(JSON_NAME);
        String venue = json.getString(JSON_VENUE);
        String note  = json.getString(JSON_NOTE);
        
        int type      = json.getInt(JSON_TYPE);
        int date      = json.getInt(JSON_DATE);
        int timeStart = json.getInt(JSON_TIME_START);
        int timeEnd   = json.getInt(JSON_TIME_END);
        
        return new Event(id, name, venue, note, type, date, timeStart, timeEnd);
    }

    // =======================================
    // Parcelable
    // =======================================

    /**
     * Constructor for Parcelable.
     *
     * @param parcel the parcel.
     */
    private Event(Parcel parcel)
    {
        mId        = parcel.readLong();
        
        mName      = parcel.readString();
        mVenue     = parcel.readString();
        mNote      = parcel.readString();

        mType      = parcel.readInt();
        mDate      = parcel.readInt();
        mTimeStart = parcel.readInt();
        mTimeEnd   = parcel.readInt();
    }
    
    @Override
    public int describeContents() 
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeLong(mId);
        
        dest.writeString(mName);
        dest.writeString(mVenue);
        dest.writeString(mNote);
        
        dest.writeInt(mType);
        dest.writeInt(mDate);
        dest.writeInt(mTimeStart);
        dest.writeInt(mTimeEnd);
    }

    public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<Event>()
    {
        public Event createFromParcel(Parcel source)
        {
            return new Event(source);
        }

        public Event[] newArray(int size)
        {
            return new Event[size];
        }
    };
}
