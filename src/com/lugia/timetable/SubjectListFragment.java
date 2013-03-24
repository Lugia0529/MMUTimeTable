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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.content.Context;
import android.content.Intent;

public class SubjectListFragment extends Fragment implements AdapterView.OnItemClickListener
{
    private LayoutInflater mLayoutInflater;
    
    private ListView mListView;
    private SubjectAdaptor mSubjectAdaptor;
    
    private String fileName;
    
    private static final String TAG = "SubjectListActivity";
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_subject_list, container, false);
        
        mLayoutInflater = inflater;
        
        mSubjectAdaptor = new SubjectAdaptor(getActivity(), R.id.text_subject_code);
        
        mListView = (ListView)view.findViewById(R.id.list_subject);
        mListView.setAdapter(mSubjectAdaptor);
        mListView.setOnItemClickListener(SubjectListFragment.this);
        
        Bundle bundle = getArguments();
        
        if (bundle.containsKey(MasterActivity.EXTRA_FILE_NAME))
        {
            loadFile(bundle.getString(MasterActivity.EXTRA_FILE_NAME));
            fileName = bundle.getString(MasterActivity.EXTRA_FILE_NAME);
        }
        else
            loadFileFromSystem();
        
        return view;
    }
    
    public void onItemClick(AdapterView<?> parent, View v, int position, long id)
    {
        Subject subject = mSubjectAdaptor.getItem(position);
        
        Intent intent = new Intent(getActivity(), SubjectDetailActivity.class);
        
        intent.putExtra("subjectCode", subject.getSubjectCode());
        
        if (fileName != null)
            intent.putExtra(MasterActivity.EXTRA_FILE_NAME, fileName);
        
        startActivity(intent);
    }
    
    private boolean loadFileFromSystem()
    {
        File file = new File(getActivity().getFilesDir(), MasterActivity.SAVEFILE);
        
        if (!file.exists())
            return false;
        
        try
        {
            FileInputStream in = getActivity().openFileInput(MasterActivity.SAVEFILE);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            
            StringBuilder builder = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) 
                builder.append(line);
            
            reader.close();
            
            SubjectList subjectList = new SubjectList(builder.toString());
            
            for (Subject subject : subjectList)
                mSubjectAdaptor.add(subject);
        }
        catch (Exception e)
        {
            // something went wrong
            Log.e(TAG, "Error on load from system!", e);
            
            return false;
        }
        
        return true;
    }
    
    private boolean loadFile(final String filepath)
    {
        try
        {
            File file = new File(filepath);
            
            // check for file availability, if no exist, stop loading
            if (!file.exists())
                return false;
            
            // create reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            
            // require to read file
            StringBuilder sb = new StringBuilder();
            String line;
            
            // read the file line by line
            while ((line = reader.readLine()) != null) 
                sb.append(line);
            
            // done reading, close the file
            reader.close();
            
            SubjectList subjectList = new SubjectList(sb.toString());
            
            for (Subject subject : subjectList)
                mSubjectAdaptor.add(subject);
        }
        catch (Exception e)
        {
            // fail to load
            Log.e(TAG, "Error on load", e);
            
            return false;
        }
        
        mSubjectAdaptor.notifyDataSetChanged();
        mListView.invalidate();
        
        // load is complete successfully
        return true;
    }
    
    class SubjectAdaptor extends ArrayAdapter<Subject>
    {
        public SubjectAdaptor(Context context, int textViewResourceId)
        {
            super(context, textViewResourceId);
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            Subject subject = getItem(position);
            
            if (convertView == null)
                convertView = mLayoutInflater.inflate(R.layout.item_subject, null);
            
            if (subject != null)
            {
                View colorView = (View)convertView.findViewById(R.id.view_color);
                TextView subjectCodeTextView = (TextView)convertView.findViewById(R.id.text_subject_code);
                TextView subjectDescTextView = (TextView)convertView.findViewById(R.id.text_subject_description);
                
                colorView.setBackgroundColor(subject.getColor());
                subjectCodeTextView.setText(subject.getSubjectCode());
                subjectDescTextView.setText(subject.getSubjectDescription());
            }
            
            return convertView;
        }
    }
}
