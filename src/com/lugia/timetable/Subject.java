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

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class Subject implements Parcelable
{
    private String mSubjectCode;
    private String mSubjectDescription;
    private String mLectureSection;
    private String mTutorialSection;
    
    private int mCreditHours;
    private int mColor;
    
    private ArrayList<Schedule> mSchedule;
    private ArrayList<Event> mEvent;
    
    public static final String JSON_SUBJECT_CODE        = "subjectCode";
    public static final String JSON_SUBJECT_DESCRIPTION = "subjectDescription";
    public static final String JSON_LECTURE_SECTION     = "lecturerSection";
    public static final String JSON_TUTORIAL_SECTION    = "tutorialSection";
    public static final String JSON_CREDIT_HOUR         = "creditHours";
    public static final String JSON_COLOR               = "color";
    public static final String JSON_SUBJECT_SCHEDULE    = "schedule";
    public static final String JSON_SUBJECT_EVENT       = "event";
    
    private static final String TAG = "Subject";
    
    public Subject(String code, String description, String lecturer, String tutorial, int credit)
    {
        this.mSubjectCode        = code;
        this.mSubjectDescription = description;
        this.mLectureSection     = lecturer;
        this.mTutorialSection    = tutorial;
        this.mCreditHours        = credit;
        
        this.mSchedule = new ArrayList<Schedule>();
        this.mEvent    = new ArrayList<Event>();
    }
    
    public Subject(String code, String description, String lecturer, String tutorial, int credit, int color)
    {
        this.mSubjectCode        = code;
        this.mSubjectDescription = description;
        this.mLectureSection     = lecturer;
        this.mTutorialSection    = tutorial;
        this.mCreditHours        = credit;
        this.mColor              = color;
        
        this.mSchedule = new ArrayList<Schedule>();
        this.mEvent = new ArrayList<Event>();
    }
    
    public String getSubjectCode()
    {
        return mSubjectCode;
    }
    
    public String getSubjectDescription()
    {
        return mSubjectDescription;
    }
    
    public String getLectureSection()
    {
        return mLectureSection;
    }
    
    public String getSection(int type)
    {
        if (type == Schedule.LECTURE_SECTION)
            return mLectureSection;
        else if (type == Schedule.TUTORIAL_SECTION)
            return mTutorialSection;
        else
            return "";
    }
    
    public String getTutorialSection()
    {
        return mTutorialSection;
    }
    
    public int getCreditHours()
    {
        return mCreditHours;
    }
    
    public int getColor()
    {
        return mColor;
    }
    
    public ArrayList<Schedule> getSchedules()
    {
        return this.mSchedule;
    }

    public ArrayList<Event> getEvents()
    {
        return this.mEvent;
    }
    
    public void setSubjectCode(String mSubjectCode)
    {
        this.mSubjectCode = mSubjectCode;
    }
    
    public void setSubjectDescription(String mSubjectDescription)
    {
        this.mSubjectDescription = mSubjectDescription;
    }
    
    public void setLectureSection(String mLectureSection)
    {
        this.mLectureSection = mLectureSection;
    }
    
    public void setTutorialSection(String mTutorialSection)
    {
        this.mTutorialSection = mTutorialSection;
    }
    
    public void setCreditHours(int creditHours)
    {
        this.mCreditHours = creditHours;
    }
    
    public void setColor(int color)
    {
        this.mColor = color;
    }
    
    public boolean hasLectureSection()
    {
        return mLectureSection != null;
    }
    
    public boolean hasTutorialSection()
    {
        return mTutorialSection != null;
    }
    
    public void addSchedule(Schedule schedule)
    {
        mSchedule.add(schedule);
    }
    
    public void addSchedule(int section, int day, int time, String room)
    {
        for (Schedule sTime : mSchedule)
        {
            if (sTime.getDay() != day)
                continue;
            
            // actually time is display in ascending order, this check maybe a litter over. 
            if (Math.abs(time - sTime.getTime()) == 1)
            {
                if (time < sTime.getTime())
                    sTime.setTime(time);
                
                sTime.setLength(sTime.getLength() + 1);
                
                return;
            }
        }
        
        Schedule sTime = new Schedule(section, day, time, 1, room);
        mSchedule.add(sTime);
    }
    
    public void addEvent(Event event)
    {
        mEvent.add(event);
    }
    
    public void addEvent(String name, String venue, String note, int type, int date, int timeStart, int timeEnd)
    {
        mEvent.add(new Event(name, venue, note, type, date, timeStart, timeEnd));
    }
    
    public boolean deleteEvent(long eventId)
    {
        return mEvent.remove(findEvent(eventId));
    }
    
    public Event findEvent(long eventId)
    {
        for (Event event : mEvent)
            if (event.getId() == eventId)
                return event;
        
        return null;
    }
    
    public JSONObject getJSONObject()
    {
        JSONObject json = new JSONObject();
        
        try
        {
            json.put(JSON_SUBJECT_CODE,        mSubjectCode);
            json.put(JSON_SUBJECT_DESCRIPTION, mSubjectDescription);
            json.put(JSON_CREDIT_HOUR,         mCreditHours);
            json.put(JSON_COLOR,               mColor);
            
            json.put(JSON_LECTURE_SECTION,  hasLectureSection()  ? mLectureSection  : JSONObject.NULL);
            json.put(JSON_TUTORIAL_SECTION, hasTutorialSection() ? mTutorialSection : JSONObject.NULL);
            
            JSONArray timeArray = new JSONArray();
            
            for (Schedule time : mSchedule)
                timeArray.put(time.getJSONObject());
            
            json.put(JSON_SUBJECT_SCHEDULE, timeArray);

            JSONArray eventArray = new JSONArray();

            for (Event event : mEvent)
                eventArray.put(event.getJSONObject());

            json.put(JSON_SUBJECT_EVENT, eventArray);
        }
        catch (JSONException e)
        {
            // something went wrong
            return null;
        }
        
        return json;
    }
    
    public static Subject restoreFromJSON(JSONObject json) throws JSONException
    {
        String subjectCode        = json.getString(JSON_SUBJECT_CODE);
        String subjectDescription = json.getString(JSON_SUBJECT_DESCRIPTION);
        int creditHours           = json.getInt(JSON_CREDIT_HOUR);
        int color                 = json.getInt(JSON_COLOR);
        
        String lectureSection  = !json.isNull(JSON_LECTURE_SECTION)  ? json.getString(JSON_LECTURE_SECTION)  : null;
        String tutorialSection = !json.isNull(JSON_TUTORIAL_SECTION) ? json.getString(JSON_TUTORIAL_SECTION) : null;
        
        Subject subject = new Subject(subjectCode, subjectDescription, lectureSection, tutorialSection, creditHours, color);
        
        JSONArray timeArray = json.getJSONArray(JSON_SUBJECT_SCHEDULE);
        JSONArray eventArray = json.getJSONArray(JSON_SUBJECT_EVENT);
        
        for (int i = 0; i < timeArray.length(); i++)
            subject.addSchedule(Schedule.restoreFromJSON(timeArray.getJSONObject(i)));
        
        for (int i = 0; i < eventArray.length(); i++)
            subject.addEvent(Event.restoreFromJSON(eventArray.getJSONObject(i)));
        
        return subject;
    }

    //=======================================
    // Parcelable
    // =======================================

    /**
     * Constructor for Parcelable.
     *
     * @param parcel the parcel.
     */
    private Subject(Parcel parcel)
    {
        mSubjectCode        = parcel.readString();
        mSubjectDescription = parcel.readString();
        mLectureSection     = parcel.readString();
        mTutorialSection    = parcel.readString();
        
        mCreditHours        = parcel.readInt();
        mColor              = parcel.readInt();
        
        parcel.readTypedList(mSchedule = new ArrayList<Schedule>(), Schedule.CREATOR);
        parcel.readTypedList(mEvent = new ArrayList<Event>(), Event.CREATOR);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mSubjectCode);
        dest.writeString(mSubjectDescription);
        dest.writeString(mLectureSection);
        dest.writeString(mTutorialSection);

        dest.writeInt(mCreditHours);
        dest.writeInt(mColor);

        dest.writeTypedList(mSchedule);
        dest.writeTypedList(mEvent);
    }

    public static final Parcelable.Creator<Subject> CREATOR = new Parcelable.Creator<Subject>()
    {
        public Subject createFromParcel(Parcel source)
        {
            return new Subject(source);
        }

        public Subject[] newArray(int size)
        {
            return new Subject[size];
        }
    };
}
