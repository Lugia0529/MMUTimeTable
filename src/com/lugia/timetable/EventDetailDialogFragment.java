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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class EventDetailDialogFragment extends DialogFragment implements View.OnClickListener
{
    private OnEventDeletedListener mEventUpdateListener = null;
    
    public static final String EXTRA_SUBJECT_CODE = "com.lugia.timetable.SubjectCode";
    public static final String EXTRA_EVENT_ID     = "com.lugia.timetable.EventId";
    
    private static final String TAG = "EventDetailDialogFragment";
    
    public static EventDetailDialogFragment newInstance(Bundle args) 
    {
        EventDetailDialogFragment fragment = new EventDetailDialogFragment();

        fragment.setArguments(args);
        
        return fragment;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        
        dialog.setView(createView());
        
        return dialog.create();
    }
    
    private View createView()
    {
        String subjectCode = getArguments().getString(EXTRA_SUBJECT_CODE);
        long eventId = getArguments().getLong(EXTRA_EVENT_ID, 0);
        
        Subject subject = SubjectList.getInstance(getActivity()).findSubject(subjectCode);
        Event event = subject.findEvent(eventId);
        
        int color = subject.getColor();
        
        String date  = generateDateString(event.getYear(), event.getMonth(), event.getDay());
        String start = generateTimeString(event.getStartHour(), event.getStartMinute());
        String end   = generateTimeString(event.getEndHour(), event.getEndMinute());
        
        String[] eventType = getActivity().getResources().getStringArray(R.array.event_type_string);
        
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_event_detail, null);

        View dividerView = view.findViewById(R.id.view_divider);
        
        RelativeLayout headerLayout = (RelativeLayout)view.findViewById(R.id.layout_header);

        ImageButton editEventButton   = (ImageButton)view.findViewById(R.id.button_edit_event);
        ImageButton deleteEventButton = (ImageButton)view.findViewById(R.id.button_delete_event);
        
        TextView nameTextView  = (TextView)view.findViewById(R.id.text_event_name);
        TextView venueTextView = (TextView)view.findViewById(R.id.text_event_venue);
        TextView timeTextView  = (TextView)view.findViewById(R.id.text_event_time);
        TextView typeTextView  = (TextView)view.findViewById(R.id.text_event_type);
        TextView noteTextView  = (TextView)view.findViewById(R.id.text_event_note);
        
        headerLayout.setBackgroundColor(color);
        dividerView.setBackgroundColor(color);
        
        editEventButton.setOnClickListener(EventDetailDialogFragment.this);
        deleteEventButton.setOnClickListener(EventDetailDialogFragment.this);
        
        nameTextView.setText(event.getName());
        venueTextView.setText(event.getVenue());
        timeTextView.setText(String.format("%s, %s - %s", date, start, end));
        typeTextView.setText(eventType[event.getType()]);
        noteTextView.setText(event.getNote());
        
        if (TextUtils.isEmpty(venueTextView.getText()))
            venueTextView.setVisibility(View.GONE);
        
        if (TextUtils.isEmpty(noteTextView.getText()))
            noteTextView.setText("No detail");
        
        return view;
    }
    
    private String generateDateString(int year, int month, int day)
    {
        final Calendar calendar = Calendar.getInstance();

        // get the date string
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        return DateFormat.format("EE, MMM dd, yyyy", calendar).toString();
    }
    
    private String generateTimeString(int hour, int minute)
    {
        final Calendar calendar = Calendar.getInstance();
        
        // get the start time string
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        return DateFormat.format("h:mm aa", calendar).toString();
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.button_edit_event:
            {
                Intent intent = new Intent(getActivity(), EventFormActivity.class);
                intent.putExtras(getArguments());
                
                startActivity(intent);
                
                dismiss();
                
                break;
            }
            
            case R.id.button_delete_event:
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                
                builder.setMessage("Delete this event?");
                
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        String subjectCode = getArguments().getString(EXTRA_SUBJECT_CODE);
                        long eventId = getArguments().getLong(EXTRA_EVENT_ID, 0);
                        
                        SubjectList subjectList = SubjectList.getInstance(getActivity());
                        Subject subject = subjectList.findSubject(subjectCode);
                        
                        if (subject.deleteEvent(eventId))
                            Toast.makeText(getActivity(), "Event deleted", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(getActivity(), "Fail to delete event!", Toast.LENGTH_SHORT).show();
                                                
                        dialog.dismiss();
                        
                        // this is use to dismiss the Event Detail Dialog 
                        dismiss();
                        
                        subjectList.saveToFile(getActivity());
                        
                        if (mEventUpdateListener != null)
                            mEventUpdateListener.onEventDeleted();
                    }
                });
                
                builder.setNegativeButton("No", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });
                
                builder.show();
                
                break;
            }
        }
    }
    
    public void setEventUpdateListener(OnEventDeletedListener listener)
    {
        mEventUpdateListener = listener;
    }
    
    // interface to notify for event data update
    public interface OnEventDeletedListener
    {
        public void onEventDeleted();
    }
}
