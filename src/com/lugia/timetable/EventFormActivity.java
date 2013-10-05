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

import java.util.Calendar;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

public class EventFormActivity extends Activity
{
    private EditText mNameEditText;
    private EditText mVenueEditText;
    private EditText mNoteEditText;

    private Spinner mTypeSpinner;

    private Button mDateButton;
    private Button mTimeStartButton;
    private Button mTimeEndButton;

    private SubjectList mSubjectList;
    private Subject mSubject;
    
    private Event mEvent;
    
    private int[] mDate = new int[3];
    private int[] mTime = new int[4];

    private static final String DATE_PICKER_TAG = "date_picker";
    private static final String TIME_PICKER_TAG = "time_picker";

    public static final String EXTRA_SUBJECT_CODE = "com.lugia.timetable.SubjectCode";
    public static final String EXTRA_EVENT_ID     = "com.lugia.timetable.EventId";
    
    private static final String TAG = "EventFormActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_form);

        final Calendar calendar = Calendar.getInstance();

        final ActionBar actionBar = getActionBar();
        final LayoutInflater inflater = (LayoutInflater)actionBar.getThemedContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View actionbarDoneCancel = inflater.inflate(R.layout.action_bar_done_cancel, null);

        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setCustomView(actionbarDoneCancel, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.event_type_string, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Bundle intentExtra = getIntent().getExtras();

        String subjectCode = intentExtra.getString(EXTRA_SUBJECT_CODE);
        long eventId = intentExtra.getLong(EXTRA_EVENT_ID, 0);
        
        mSubjectList = SubjectList.getInstance(EventFormActivity.this);
        
        mSubject = mSubjectList.findSubject(subjectCode);
        mEvent = mSubject.findEvent(eventId);

        mNameEditText = (EditText)findViewById(R.id.input_event_name);
        mVenueEditText = (EditText)findViewById(R.id.input_event_venue);
        mNoteEditText = (EditText)findViewById(R.id.input_event_note);

        mTypeSpinner = (Spinner)findViewById(R.id.spinner_event_type);

        mDateButton = (Button)findViewById(R.id.button_event_date);
        mTimeStartButton = (Button)findViewById(R.id.button_event_time_start);
        mTimeEndButton = (Button)findViewById(R.id.button_event_time_end);

        mTypeSpinner.setAdapter(adapter);
        
        if (mEvent != null)
        {
            // mEvent not null, we are editing the event
            mNameEditText.setText(mEvent.getName());
            mVenueEditText.setText(mEvent.getVenue());
            mNoteEditText.setText(mEvent.getNote());
            
            mTypeSpinner.setSelection(mEvent.getType());
            
            mDate = new int[] { mEvent.getYear(), mEvent.getMonth(), mEvent.getDay() };
            
            mTime = new int[] { mEvent.getStartHour(), mEvent.getStartMinute(), 
                                mEvent.getEndHour(),   mEvent.getEndMinute()    };
        }
        else
        {
            // default value for time
            mDate[0] = calendar.get(Calendar.YEAR);
            mDate[1] = calendar.get(Calendar.MONTH);
            mDate[2] = calendar.get(Calendar.DAY_OF_MONTH);
            
            // more better default time
            mTime[0] = calendar.get(Calendar.HOUR_OF_DAY) + 1;
            mTime[1] = 0;

            mTime[2] = mTime[0] + 1;
            mTime[3] = 0;
        }

        mDateButton.setText(getFormattedDate(mDate[0], mDate[1], mDate[2]));
        mTimeStartButton.setText(getFormattedTime(mTime[0], mTime[1]));
        mTimeEndButton.setText(getFormattedTime(mTime[2], mTime[3]));
    }

    public void showDatePickerDialog(View view)
    {
        DialogFragment dialog = new DateDialogFragment();
        dialog.show(getFragmentManager(), DATE_PICKER_TAG);
    }

    public void showTimePickerDialog(View view)
    {
        Bundle args = new Bundle();

        args.putInt(TimeDialogFragment.EXTRA_VIEW_ID, view.getId());

        DialogFragment dialog = new TimeDialogFragment();
        dialog.setArguments(args);
        dialog.show(getFragmentManager(), TIME_PICKER_TAG);
    }

    public void onDoneClick(View v)
    {
        String eventName  = mNameEditText.getText().toString();
        String eventVenue = mVenueEditText.getText().toString();
        String eventNote  = mNoteEditText.getText().toString();

        if (eventName.isEmpty())
        {
            Toast.makeText(EventFormActivity.this, "Event name cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        int eventType = mTypeSpinner.getSelectedItemPosition();
        
        int eventDate      = (mDate[0] * 10000) + (mDate[1] * 100) + mDate[2];
        int eventTimeStart = (mTime[0] * 100) + mTime[1];
        int eventTimeEnd   = (mTime[2] * 100) + mTime[3];
        
        if (mEvent != null)
        {
            // update event
            mEvent.setName(eventName);
            mEvent.setVenue(eventVenue);
            mEvent.setNote(eventNote);
            mEvent.setType(eventType);
            mEvent.setDate(eventDate);
            mEvent.setTime(eventTimeStart, eventTimeEnd);
        }
        else
        {
            // New event
            mSubject.addEvent(eventName, eventVenue, eventNote, eventType, eventDate, eventTimeStart, eventTimeEnd);
        }

        mSubjectList.saveToFile(EventFormActivity.this);

        Toast.makeText(EventFormActivity.this, "Event Saved", Toast.LENGTH_SHORT).show();
        
        setResult(RESULT_OK);

        // update event reminder if user enable it
        if (SettingActivity.getBoolean(EventFormActivity.this, SettingActivity.KEY_EVENT_NOTIFICATION, false))
        {
            Intent broadcastIntent = new Intent(EventFormActivity.this, ReminderReceiver.class);
            broadcastIntent.setAction(ReminderReceiver.ACTION_UPDATE_EVENT_REMINDER);

            sendBroadcast(broadcastIntent);
        }
        
        finish();
    }

    public void onCancelClick(View v)
    {
        finish();
    }

    private void checkTime()
    {
        // revert invalid value
        if (mTime[2] < mTime[0] || (mTime[2] == mTime[0] && mTime[3] < mTime[1]))
        {
            mTime[2] = mTime[0];
            mTime[3] = mTime[1];

            mTimeEndButton.setText(getFormattedTime(mTime[2], mTime[3]));
        }
    }

    private CharSequence getFormattedDate(int year, int month, int day)
    {
        final Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        return DateFormat.format("EEEE, MMMM dd, yyyy", calendar);
    }

    private CharSequence getFormattedTime(int hour, int minute)
    {
        final Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        return DateFormat.format("h:mm aa", calendar);
    }

    private class DateDialogFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener
    {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            return new DatePickerDialog(getActivity(), DateDialogFragment.this, mDate[0], mDate[1], mDate[2]);
        }

        @Override
        public void onDateSet(DatePicker datePicker, int year, int month, int day)
        {
            mDate[0] = year;
            mDate[1] = month;
            mDate[2] = day;

            mDateButton.setText(getFormattedDate(year, month, day));
        }
    }

    private class TimeDialogFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener
    {
        public static final String EXTRA_VIEW_ID = "com.lugia.timetable.ViewID";

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            if (getArguments().getInt(EXTRA_VIEW_ID) == R.id.button_event_time_start)
                return new TimePickerDialog(getActivity(), TimeDialogFragment.this, mTime[0], mTime[1], DateFormat.is24HourFormat(getActivity()));
            else
                return new TimePickerDialog(getActivity(), TimeDialogFragment.this, mTime[2], mTime[3], DateFormat.is24HourFormat(getActivity()));
        }
        
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute)
        {
            if (getArguments().getInt(EXTRA_VIEW_ID) == R.id.button_event_time_start)
            {
                mTime[0] = hourOfDay;
                mTime[1] = minute;

                mTimeStartButton.setText(getFormattedTime(hourOfDay, minute));
            }
            else
            {
                mTime[2] = hourOfDay;
                mTime[3] = minute;

                mTimeEndButton.setText(getFormattedTime(hourOfDay, minute));
            }

            checkTime();
        }
    }
}
