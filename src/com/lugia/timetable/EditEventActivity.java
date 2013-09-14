package com.lugia.timetable;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Calendar;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
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

public class EditEventActivity extends Activity
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

    private int[] mDate = new int[3];
    private int[] mTime = new int[4];

    private static final String DATE_PICKER_TAG = "date_picker";
    private static final String TIME_PICKER_TAG = "time_picker";

    public static final String EXTRA_SUBJECT_CODE = "com.lugia.timetable.SubjectCode";

    private static final String TAG = "EditEventActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        final Calendar calendar = Calendar.getInstance();

        final ActionBar actionBar = getActionBar();
        final LayoutInflater inflater = (LayoutInflater)actionBar.getThemedContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View actionbarDoneCancel = inflater.inflate(R.layout.action_bar_done_cancel, null);

        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setCustomView(actionbarDoneCancel, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.event_type_string, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Bundle intentExtra = getIntent().getExtras();

        if (intentExtra.containsKey(MasterActivity.EXTRA_FILE_NAME))
            loadFile(intentExtra.getString(MasterActivity.EXTRA_FILE_NAME));
        else
            loadFileFromSystem();

        String subjectCode = intentExtra.getString(EXTRA_SUBJECT_CODE);

        mSubject = mSubjectList.findSubject(subjectCode);

        mNameEditText = (EditText)findViewById(R.id.input_event_name);
        mVenueEditText = (EditText)findViewById(R.id.input_event_venue);
        mNoteEditText = (EditText)findViewById(R.id.input_event_note);

        mTypeSpinner = (Spinner)findViewById(R.id.spinner_event_type);

        mDateButton = (Button)findViewById(R.id.button_event_date);
        mTimeStartButton = (Button)findViewById(R.id.button_event_time_start);
        mTimeEndButton = (Button)findViewById(R.id.button_event_time_end);

        mTypeSpinner.setAdapter(adapter);

        mDate[0] = calendar.get(Calendar.YEAR);
        mDate[1] = calendar.get(Calendar.MONTH);
        mDate[2] = calendar.get(Calendar.DAY_OF_MONTH);

        mTime[0] = calendar.get(Calendar.HOUR_OF_DAY);
        mTime[1] = calendar.get(Calendar.MINUTE);

        mTime[2] = calendar.get(Calendar.HOUR_OF_DAY) + 1;
        mTime[3] = calendar.get(Calendar.MINUTE);

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
            Toast.makeText(EditEventActivity.this, "Event name cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        int eventType = mTypeSpinner.getSelectedItemPosition();

        int eventDate      = (mDate[0] * 10000) + (mDate[1] * 100) + mDate[2];
        int eventTimeStart = (mTime[0] * 100) + mTime[1];
        int eventTimeEnd   = (mTime[2] * 100) + mTime[3];

        mSubject.addEvent(eventName, eventVenue, eventNote, eventType, eventDate, eventTimeStart, eventTimeEnd);

        saveToFile();

        Toast.makeText(EditEventActivity.this, "Event Saved", Toast.LENGTH_SHORT).show();

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

    private boolean loadFileFromSystem()
    {
        File file = new File(getFilesDir(), MasterActivity.SAVEFILE);

        if (!file.exists())
            return false;

        try
        {
            FileInputStream in = openFileInput(MasterActivity.SAVEFILE);

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            StringBuilder builder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null)
                builder.append(line);

            reader.close();

            mSubjectList = new SubjectList(builder.toString());
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
            {
                Toast.makeText(EditEventActivity.this, "Fail to open file because it is not exist.", Toast.LENGTH_LONG).show();

                return false;
            }

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

            mSubjectList = new SubjectList(sb.toString());
        }
        catch (Exception e)
        {
            // fail to load
            Log.e(TAG, "Error on load", e);

            return false;
        }

        return true;
    }

    private boolean saveToFile()
    {
        try
        {
            FileOutputStream out = openFileOutput(MasterActivity.SAVEFILE, Context.MODE_PRIVATE);
            BufferedOutputStream stream = new BufferedOutputStream(out);

            stream.write(mSubjectList.generateJSON().toString().getBytes());

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
