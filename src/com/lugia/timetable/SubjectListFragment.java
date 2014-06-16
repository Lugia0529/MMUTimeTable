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

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

    private int[] mBackgrounds;
    private int[] mColors;
    
    private static final String TAG = "SubjectListActivity";
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Resources res = getResources();

        mColors = Utils.getForegroundColorArrays(getActivity());
        mBackgrounds = Utils.getBackgroundDrawableResourceIds();

        View view = inflater.inflate(R.layout.fragment_subject_list, container, false);
        
        mLayoutInflater = inflater;
        
        mSubjectAdaptor = new SubjectAdaptor(getActivity(), R.id.text_subject_code);
        
        mListView = (ListView)view.findViewById(R.id.list_subject);
        mListView.setAdapter(mSubjectAdaptor);
        mListView.setOnItemClickListener(SubjectListFragment.this);
        
        SubjectList subjectList = SubjectList.getInstance(getActivity());
        
        for (Subject subject : subjectList)
            mSubjectAdaptor.add(subject);
        
        return view;
    }
    
    public void onItemClick(AdapterView<?> parent, View v, int position, long id)
    {
        Subject subject = mSubjectAdaptor.getItem(position);
        
        Intent intent = new Intent(getActivity(), SubjectDetailActivity.class);
        
        intent.putExtra(SubjectDetailActivity.EXTRA_SUBJECT_CODE, subject.getSubjectCode());
        
        startActivity(intent);
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
                int colorIndex = subject.getColor();
                
                convertView.setBackgroundResource(mBackgrounds[colorIndex]);
                
                TextView subjectCodeTextView = (TextView)convertView.findViewById(R.id.text_subject_code);
                TextView subjectDescTextView = (TextView)convertView.findViewById(R.id.text_subject_description);
                
                subjectCodeTextView.setText(subject.getSubjectCode());
                subjectDescTextView.setText(subject.getSubjectDescription());

                subjectCodeTextView.setTextColor(mColors[colorIndex]);
                subjectDescTextView.setTextColor(mColors[colorIndex]);
            }
            
            return convertView;
        }
    }
}
