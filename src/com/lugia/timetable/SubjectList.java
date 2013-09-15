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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

/* Singleton Class
 * Initialize technique: Initialization-on-demand holder idiom (IODHI)
 */
public class SubjectList extends ArrayList<Subject>
{
    // only use on first initialization
    private static Context mContext;
    
    private static final String JSON_SUBJECT_ARRAY = "timeTable";
    private static final String SAVEFILE = "data.ttg";
    
    private static final String TAG = "SubjectList";
    
    /* SINGLETON HOLDER */
    private static class InstanceHolder
    {
        private static final SubjectList INSTANCE = new SubjectList();
    }

    public static synchronized SubjectList getInstance(Context context)
    {
        Log.i(TAG, "getIntance() called");
        
        if (mContext == null)
            mContext = context;
        
        return InstanceHolder.INSTANCE;
    }
    
    private SubjectList()
    {
        Log.i(TAG, "Construct Singleton");
        
        File file = new File(mContext.getFilesDir(), SAVEFILE);
        
        if (!file.exists())
            return;
    
        try
        {
            FileInputStream in = mContext.openFileInput(SAVEFILE);
    
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    
            StringBuilder builder = new StringBuilder();
            String line;
    
            while ((line = reader.readLine()) != null)
                builder.append(line);
    
            reader.close();
            
            // Dont hanging subject list in imcomplete state
            if (!extractJSON(builder.toString()))
                clear();
        }
        catch (Exception e)
        {
            // something went wrong
            Log.e(TAG, "Error on loading subject list!", e);
        }
    }
    
    public Subject findSubject(String subjectCode)
    {
        for (Subject subject : this)
            if (subject.getSubjectCode().equalsIgnoreCase(subjectCode))
                return subject;
        
        return null;
    }

    /**
     * Remove all subject in current list and copy the content of newList to current list.
     */
    public void replace(ArrayList<Subject> newList)
    {
        clear();
        addAll(newList);
    }
    
    public boolean saveToFile(Context context)
    {
        try
        {
            FileOutputStream out = context.openFileOutput(SAVEFILE, Context.MODE_PRIVATE);
            BufferedOutputStream stream = new BufferedOutputStream(out);

            stream.write(generateJSON().toString().getBytes());

            stream.flush();
            stream.close();

            out.close();
        }
        catch (Exception e)
        {
            // something went wrong
            Log.e(TAG, "Error on save!", e);

            return false;
        }

        return true;
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
    
    private boolean extractJSON(String source)
    {
        JSONObject object;
        JSONArray array;

        try
        {
            object = new JSONObject(source);

            array = object.getJSONArray(JSON_SUBJECT_ARRAY);

            for (int i = 0; i < array.length(); i++)
                add(Subject.restoreFromJSON(array.getJSONObject(i)));
        }
        catch (Exception e)
        {
            // Something went wrong
            Log.e(TAG, "Error on extract JSON", e);

            return false;
        }

        return true;
    }

    private JSONObject generateJSON()
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
}
