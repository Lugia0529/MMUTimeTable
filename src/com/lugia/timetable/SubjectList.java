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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class SubjectList extends ArrayList<Subject>
{
    public static final String JSON_SUBJECT_ARRAY = "timeTable";
    
    private static final long serialVersionUID = 1L;
    
    private static final String TAG = "SubjectList";
    
    public SubjectList()
    {
    }
    
    public SubjectList(String source)
    {
        JSONObject object;
        JSONArray array;
        
        try
        {
            object = new JSONObject(source);
            
            array = object.getJSONArray(JSON_SUBJECT_ARRAY);
            
            for (int i = 0; i < array.length(); i++)
                add(new Subject(array.getJSONObject(i)));
        }
        catch (Exception e)
        {
            // Something went wrong
            Log.e(TAG, "Error on restore from JSON", e);
        }
    }
    
    public Subject findSubject(String subjectCode)
    {
        for (Subject subject : this)
            if (subject.getSubjectCode().equalsIgnoreCase(subjectCode))
                return subject;
        
        return null;
    }
    
    public JSONObject generateJSON()
    {
        JSONObject rootObject  = new JSONObject();
        JSONArray subjectArray = new JSONArray();
        
        try
        {
            for (Subject subject : this)
                subjectArray.put(subject.getJSONObject());
            
            rootObject.put(JSON_SUBJECT_ARRAY, subjectArray);
        }
        catch (JSONException e)
        {
            // Something went wrong
            Log.e(TAG, "Error on generate JSON", e);
            
            return null;
        }
        
        return rootObject;
    }
    
    public void displaySubjectListContent()
    {
        Log.d(TAG ,String.format("%d subject in total.\n", size()));
        
        for (Subject subject : this)
        {
            Log.v(TAG ,String.format("Subject Code: %s",        subject.getSubjectCode()       ));
            Log.v(TAG ,String.format("Subject Description: %s", subject.getSubjectDescription()));
            Log.v(TAG ,String.format("Lecture Section: %s",     subject.getLectureSection()    ));
            Log.v(TAG ,String.format("Tutorial Section: %s",    subject.getTutorialSection()   ));
            Log.v(TAG ,String.format("Credits Hours: %d",       subject.getCreditHours()       ));
            
            ArrayList<Schedule> timeList = subject.getSchedules();
            
            Log.v(TAG, "Time:");
            
            for (Schedule time : timeList)
            {
                Log.v(TAG ,String.format("%s %d %d %d %s", time.getSection(),
                                                           time.getDay(),
                                                           time.getTime(),
                                                           time.getLength(),
                                                           time.getRoom()));
            }
        }
    }
}
