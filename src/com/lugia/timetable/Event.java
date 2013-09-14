package com.lugia.timetable;

import android.util.Log;
import org.json.JSONObject;

public class Event
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

    public Event(JSONObject json)
    {
        restoreFromJSON(json);
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

    public void restoreFromJSON(JSONObject json)
    {
        try
        {
            this.mId = json.getLong(JSON_ID);

            this.mName  = json.getString(JSON_NAME);
            this.mVenue = json.getString(JSON_VENUE);
            this.mNote  = json.getString(JSON_NOTE);

            this.mType      = json.getInt(JSON_TYPE);
            this.mDate      = json.getInt(JSON_DATE);
            this.mTimeStart = json.getInt(JSON_TIME_START);
            this.mTimeEnd   = json.getInt(JSON_TIME_END);
        }
        catch (Exception e)
        {
            // Something went wrong, so we need revert all change we made just now
            Log.e(TAG, "Error on restore", e);

            this.mId = 0;

            this.mName  = "";
            this.mVenue = "";
            this.mNote  = "";

            this.mType  = TYPE_UNKNOWN;

            this.mDate      = 0;
            this.mTimeStart = 0;
            this.mTimeEnd   = 0;
        }
    }
}
