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
import java.util.ArrayList;
import java.util.Calendar;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

public class DayView extends View
{
    private Context mContext;
    private SubjectList mSubjectList;
    private Resources mResources;
    private Rect mTextBound;
    
    private OnDayChangeListener mDayChangeListener = null;
    
    private Handler mHandler;
    private OverScroller mScroller;
    private GestureDetector mGestureDetector;
    
    private final ContinueScroll mContinueScroll = new ContinueScroll();
    private final ContinueSwitch mContinueSwitch = new ContinueSwitch();
    
    private Paint mTimePaint;
    private Paint mSubjectPaint;
    private Paint mLinePaint;
    private Paint mBackgroundPaint;
    
    private int mCurrentDay;
    
    private int mGridLineColor;
    private int mTimeBackgroundColor;
    
    private int mWidth;
    private int mHeight;
    private int mActualHeight;
    private int mTimeGridWidth;
    private int mCellHeight;
    
    /**
     * Use to handle time diff for day switching animation
     */
    private long mLastTimeMillies = 0;
    
    private final int SWIPE_PAGE_MIN_DISTANCE;
    private final int SWIPE_MIN_VELOCITY;
    private final int SWIPE_OVERFLING_DISTANCE;
    
    private int mScrollMode;
    private int mSwitchMode;
    
    private float mScrollX;
    private float mScrollY;
    private float mScaleFactor;
    
    private boolean mScrolling;
    
    private String mFilename;
    
    private String[] mTimeStrings;
    
    public static final int SUNDAY    = 0;
    public static final int MONDAY    = 1;
    public static final int TUESDAY   = 2;
    public static final int WEDNESDAY = 3;
    public static final int THURSDAY  = 4;
    public static final int FRIDAY    = 5;
    public static final int SATURDAY  = 6;
    
    public static final int TEXT_PADDING = 6;
    
    /**
     * transition duration when user switch day
     */
    public static final float DAY_SWITCH_DURATION = 300f;
    
    public static final int SCROLL_MODE_NONE       = 1 << 0;
    public static final int SCROLL_MODE_VERTICAL   = 1 << 1;
    public static final int SCROLL_MODE_HORIZONTAL = 1 << 2;
    
    public static final int SWITCH_MODE_PREV = -1;
    public static final int SWITCH_MODE_NONE =  0;
    public static final int SWITCH_MODE_NEXT =  1;
    
    private static final String TAG = "DayView";
    
    public DayView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        
        final ViewConfiguration vc = ViewConfiguration.get(context);
        
        SWIPE_PAGE_MIN_DISTANCE   = vc.getScaledPagingTouchSlop();
        SWIPE_MIN_VELOCITY        = vc.getScaledMinimumFlingVelocity();
        SWIPE_OVERFLING_DISTANCE  = vc.getScaledOverflingDistance();
        
        init(context);
    }
    
    @Override
    protected void onAttachedToWindow()
    {
        if (mHandler == null)
            mHandler = getHandler();
    }
    
    private void init(Context context)
    {
        mContext = context;
        
        mResources = context.getResources();
        
        mGridLineColor       = mResources.getColor(R.color.timetable_grid_line_color);
        mTimeBackgroundColor = mResources.getColor(R.color.timetable_time_background_color);
        
        mTimeStrings = mResources.getStringArray(R.array.time_string);
        
        mScrollMode = SCROLL_MODE_NONE;
        mSwitchMode = SWITCH_MODE_NONE;
        
        mScrollX = 0;
        mScrollY = 0;
        mScaleFactor = 1;
        
        mScrolling = false;
        
        // set the current day of this view initially according to real world day
        switch (Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        {
            case Calendar.SATURDAY:
            case Calendar.SUNDAY:
                mCurrentDay = MONDAY;
                break;
                
            default:
                mCurrentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
        }
        
        mScroller = new OverScroller(context);
        mGestureDetector = new GestureDetector(context, new GestureListener());
        
        mTextBound = new Rect();
        
        mTimePaint = new Paint();
        mTimePaint.setAntiAlias(true);
        mTimePaint.setColor(Color.BLACK);
        mTimePaint.setTextSize(16);
        mTimePaint.setTextAlign(Align.CENTER);
        mTimePaint.setStyle(Style.STROKE);
        
        mSubjectPaint = new Paint();
        mSubjectPaint.setAntiAlias(true);
        mSubjectPaint.setColor(Color.WHITE);
        mSubjectPaint.setTextSize(16);
        mSubjectPaint.setTextAlign(Align.CENTER);
        mSubjectPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mSubjectPaint.setStyle(Style.STROKE);
        
        mLinePaint = new Paint();
        mLinePaint.setAntiAlias(true);
        mLinePaint.setColor(mGridLineColor);
        mLinePaint.setStyle(Style.STROKE);
        
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setStyle(Style.FILL);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        
        // measure text dimension of time text
        mTimeGridWidth = (int)mTimePaint.measureText("XX XX") + 6;
        
        setMeasuredDimension(mWidth, mHeight);
        
        remeasure();
    }
    
    private void remeasure()
    {
        mCellHeight = (int)(100 * mScaleFactor);
        mActualHeight = mCellHeight * mTimeStrings.length;
        
        // check for scroll position
        if (mScrollY < 0)
            mScrollY = 0;
        
        if (mScrollY + mHeight > mActualHeight)
            mScrollY = mActualHeight - mHeight;
    }
    
    public void setCurrentDay(int day)
    {
        this.mCurrentDay = day;
        
        // notify the listener
        if (mDayChangeListener != null)
            mDayChangeListener.onDayChange(day);
        
        invalidate();
    }
    
    public void setOnDayChangeListener(OnDayChangeListener listener)
    {
        this.mDayChangeListener = listener;
        
        // notify the listener
        if (mDayChangeListener != null)
            mDayChangeListener.onDayChange(mCurrentDay);
    }
    
    // ======================================================
    // UI Drawing
    // ======================================================
    
    @Override
    protected void onDraw(Canvas canvas)
    {
        canvas.drawColor(Color.WHITE);
        
        canvas.save();
        canvas.translate(-mScrollX, -mScrollY);
        
        drawTimeAndGridLine(canvas);
        drawSubject(canvas, mCurrentDay);
        
        canvas.restore();
        
        // if user are currently on switching day, we need do some extra work
        if (mScrollX != 0)
        {
            canvas.save();
            
            if (mScrollX < 0)
                canvas.translate(-(mWidth + mScrollX), -mScrollY);
            else
                canvas.translate(mWidth - mScrollX, -mScrollY);
            
            drawTimeAndGridLine(canvas);
            
            if (mScrollX < 0)
                drawSubject(canvas, mCurrentDay - 1);
            else
                drawSubject(canvas, mCurrentDay + 1);
            
            canvas.restore();
        }
    }
    
    private void drawTimeAndGridLine(Canvas canvas)
    {
     // time background
        mBackgroundPaint.setColor(mTimeBackgroundColor);
        canvas.drawRect(0, 0, mTimeGridWidth, mActualHeight, mBackgroundPaint);
        
        // vertical line
        canvas.drawLine(mTimeGridWidth, 0, mTimeGridWidth, mActualHeight, mLinePaint);
        
        // row line
        for (int i = 0; i < mTimeStrings.length; i++)
        {
            int lineY = mCellHeight * (i + 1);
            
            mTimePaint.getTextBounds(mTimeStrings[i], 0, mTimeStrings[i].length(), mTextBound);
            
            int textHeight = mTextBound.height();
            
            int textX = (mTimeGridWidth / 2);
            int textY = (mCellHeight / 2) + (textHeight / 2) + lineY - mCellHeight;
            
            canvas.drawLine(0, lineY, mWidth, lineY, mLinePaint);
            canvas.drawText(mTimeStrings[i], textX, textY, mTimePaint);
        }
    }
    
    private void drawSubject(Canvas canvas, int day)
    {
        if (mSubjectList == null)
            return;
        
        for (int i = 0; i < mSubjectList.size(); i++)
        {
            Subject subject = mSubjectList.get(i);
            
            for (Schedule time : subject.getSchedules())
            {
                if (time.getDay() == day)
                {
                    int hour   = time.getTime() - 8;
                    int length = time.getLength();
                    int height = length * mCellHeight;
                    
                    String code        = subject.getSubjectCode();
                    String description = subject.getSubjectDescription();
                    String room        = time.getRoom();
                    
                    String section = "";
                    
                    if (time.getSection() == Schedule.LECTURE_SECTION)
                        section = subject.getLectureSection();
                    else
                        section = subject.getTutorialSection();
                    
                    // eliminate lines
                    mBackgroundPaint.setColor(Color.WHITE);
                    canvas.drawRect(mTimeGridWidth + 1, (mCellHeight * hour) + 1, mWidth, mCellHeight * (hour + length) - 1, mBackgroundPaint);
                    
                    // draw the background
                    mBackgroundPaint.setColor(subject.getColor());
                    canvas.drawRect(mTimeGridWidth + 3, (mCellHeight * hour) + 3, mWidth - 3, mCellHeight * (hour + length) - 3, mBackgroundPaint);
                    
                    Bitmap bitmap = createSubjectTextBitmap(mWidth - mTimeGridWidth - 6, mCellHeight * length - 6, code, description, section, room);
                    canvas.drawBitmap(bitmap, mTimeGridWidth + 3, (mCellHeight * hour) + ((mCellHeight * length) / 2) - (bitmap.getHeight() / 2), null);
                }
            }
        }
    }
    
    private Bitmap createSubjectTextBitmap(int maxWidth, int maxHeight, String code, String description, String section, String room)
    {
        // TODO: we may not only handle description, we can handle more...
        ArrayList<String> descriptionTextList = new ArrayList<String>();
        
        mSubjectPaint.getTextBounds(code, 0, code.length(), mTextBound);
        int codeHeight = mTextBound.height();
        
        mSubjectPaint.getTextBounds(section, 0, section.length(), mTextBound);
        int sectionHeight = mTextBound.height();
        
        mSubjectPaint.getTextBounds(room, 0, room.length(), mTextBound);
        int roomHeight = mTextBound.height();
        
        float a[] = new float[description.length()];
        mSubjectPaint.getTextWidths(description, a);
        
        int textWidth = 0;
        int startPos = 0;
        int spacePos = 0;
        
        for (int i = 0; i < a.length; i++)
        {
            textWidth += a[i];
            
            if (description.charAt(i) == ' ')
            {
                spacePos = i;
                //Log.d(TAG, "space: " + i);
            }
            
            if (textWidth >= maxWidth)
            {
                if (spacePos > startPos)
                {
                    descriptionTextList.add(description.substring(startPos, spacePos));
                    
                    startPos = spacePos + 1;
                    i = spacePos;
                }
                else
                {
                    descriptionTextList.add(description.substring(startPos, i));
                    
                    startPos = i;
                }
                
                textWidth = 0;
            }
            
            if (i + 1 == a.length)
            {
                descriptionTextList.add(description.substring(startPos));
            }
        }
        
        int reqHeight = codeHeight + sectionHeight + roomHeight + (TEXT_PADDING + TEXT_PADDING);
        
        // try to see the result
        for (String str : descriptionTextList)
        {
            mSubjectPaint.getTextBounds(str, 0, str.length(), mTextBound);
            reqHeight += mTextBound.height() + TEXT_PADDING;
        }
        
        Bitmap bitmap = Bitmap.createBitmap(maxWidth, reqHeight, Bitmap.Config.ARGB_8888);
        
        int xPos = maxWidth / 2;
        
        Canvas canvas = new Canvas(bitmap);
        
        // For debugging purpose, draw the bound of this bitmap
        //canvas.drawRect(0, 0, maxWidth, reqHeight, mLinePaint);
        
        int yPos = codeHeight;
        
        // draw the text
        canvas.drawText(code, xPos, yPos, mSubjectPaint);
        
        for (String str : descriptionTextList)
        {
            mSubjectPaint.getTextBounds(code, 0, code.length(), mTextBound);
            yPos += mTextBound.height() + TEXT_PADDING;
            
            canvas.drawText(str, xPos, yPos, mSubjectPaint);
        }
        
        yPos += sectionHeight + TEXT_PADDING;
        
        canvas.drawText(section, xPos, yPos, mSubjectPaint);
        
        yPos += roomHeight + TEXT_PADDING;
        
        canvas.drawText(room, xPos, yPos, mSubjectPaint);
        
        return bitmap;
    }
    
    // ======================================================
    // FILE LOADING
    // ======================================================
    
    public void setFilename(final String filename)
    {
        this.mFilename = filename;
        
        if (filename != null)
            loadFile(filename);
        else
            loadFileFromSystem();
    }
    
    private boolean loadFileFromSystem()
    {
        File file = new File(mContext.getFilesDir(), MasterActivity.SAVEFILE);
        
        if (!file.exists())
            return false;
        
        try
        {
            FileInputStream in = mContext.openFileInput(MasterActivity.SAVEFILE);
            
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
    
    boolean loadFile(final String filepath)
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
            
            mSubjectList = new SubjectList(sb.toString());
        }
        catch (Exception e)
        {
            // fail to load
            Log.e(TAG, "Error on load", e);
            
            return false;
        }
        
        // load is complete successfully
        return true;
    }
    
    // ======================================================
    // TOUCH HANDLING
    // ======================================================
    
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_UP)
        {
            // reset the scroll mode if user finger leave the screen
            mScrollMode = SCROLL_MODE_NONE;
            
            // continue horizontal switching
            if (mScrollX != 0)
            {
                // check the direction of we are going to switching to
                Boolean condition = (Math.abs(mScrollX) > mTimeGridWidth) ^ (mScrollX > 0);
                
                mSwitchMode = condition ? SWITCH_MODE_PREV : SWITCH_MODE_NEXT;
                
                mLastTimeMillies = SystemClock.elapsedRealtime();
                mHandler.post(mContinueSwitch);
            }
        }
        
        mGestureDetector.onTouchEvent(event);
        
        return true;
    }
    
    private class GestureListener extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onDown(MotionEvent e)
        {
            Log.d(TAG, "onDown");
            
            mScrolling = false;
            
            return true;
        }
        
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e)
        {
            Log.d(TAG, "onSingleTapConfirm");
            
            mScrolling = false;
            
            // try to figure out where user click
            int value = (int)((mScrollY + e.getY()) / mCellHeight);
            
            // TODO: some hack
            value = value + 8;
            
            for (Subject subject : mSubjectList)
            {
                for (Schedule schedule : subject.getSchedules())
                {
                    if (schedule.getDay() != mCurrentDay)
                        continue;
                    
                    if (value >= schedule.getTime() && value <= schedule.getTime() + schedule.getLength())
                    {
                        Log.d(TAG, subject.getSubjectCode() + " - " + subject.getSubjectDescription());
                        
                        Intent intent = new Intent(mContext, SubjectDetailActivity.class);
                        
                        intent.putExtra("subjectCode", subject.getSubjectCode());
                        
                        if (mFilename != null)
                            intent.putExtra(MasterActivity.EXTRA_FILE_NAME, mFilename);
                  
                        mContext.startActivity(intent);
                    }
                }
            }
            
            return true;
        }
        
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            Log.d(TAG, "onScroll");
            
            if (mScrollMode == SCROLL_MODE_NONE)
            {
                // try to figure out we are in horizontal or vertical scroll
                float absDistX = Math.abs(distanceX);
                float absDistY = Math.abs(distanceY);
                
                if (absDistX > absDistY)
                    mScrollMode = SCROLL_MODE_HORIZONTAL;
                else
                    mScrollMode = SCROLL_MODE_VERTICAL;
            }
            
            if (mScrollMode == SCROLL_MODE_VERTICAL)
                mScrollY += distanceY;
            
            if (mScrollMode == SCROLL_MODE_HORIZONTAL)
            {
                mScrollX += distanceX;
                
                // don't allow user over scrolling Monday and Friday
                if ((mScrollX < 0 && mCurrentDay == MONDAY) || (mScrollX > 0 && mCurrentDay == FRIDAY))
                    mScrollX = 0;
            }
            
            remeasure();
            invalidate();
            
            return true;
        }
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
            Log.d(TAG, "onFling");
            
            float x1 = e1.getX();
            float y1 = e1.getY();
            
            float x2 = e2.getX(); 
            float y2 = e2.getY();
            
            // switch day logic is handle in onScroll
            if (Math.abs(x1 - x2) > SWIPE_PAGE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_MIN_VELOCITY)
                return true;
            
            // handle scroll
            if (Math.abs(y1 - y2) > SWIPE_PAGE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_MIN_VELOCITY)
            {
                mScrolling = true;
                
                mScroller.fling(0, (int)mScrollY, 0, (int)-velocityY, 0, 0, 0, (int)mActualHeight - mHeight, SWIPE_OVERFLING_DISTANCE, SWIPE_OVERFLING_DISTANCE);
                
                mHandler.post(mContinueScroll);
                
                return true;
            }
            
            invalidate();
            
            return true;
        }
    }
    
    class ContinueScroll implements Runnable
    {
        public void run()
        {
            boolean scrolling = mScrolling && mScroller.computeScrollOffset();
            
            if (!scrolling)
            {
                mScrolling = false;
                
                invalidate();
                return;
            }
            
            mScrollY = mScroller.getCurrY();
            
            Log.d(TAG, "Flinging");
            
            mHandler.post(this);
            invalidate();
        }
    }
    
    class ContinueSwitch implements Runnable
    {
        public void run()
        {
            if (mSwitchMode == SWITCH_MODE_NONE)
                return;
            
            // check for time different between last and current execution of this thread
            // so we can know how much to scroll the view, this help us maintain a constant
            // time between day switching animation.
            long diff = SystemClock.elapsedRealtime() - mLastTimeMillies;
            mLastTimeMillies = SystemClock.elapsedRealtime();
            
            float scrollAmount = mWidth / DAY_SWITCH_DURATION * diff;
            
            boolean switchFinish = false;
            
            if (mSwitchMode == SWITCH_MODE_PREV)
            {
                mScrollX -= scrollAmount;
                
                switchFinish = mScrollX > 0 || Math.abs(mScrollX) > mWidth;
            }
            else if (mSwitchMode == SWITCH_MODE_NEXT)
            {
                mScrollX += scrollAmount;
                
                switchFinish = mScrollX < 0 || Math.abs(mScrollX) > mWidth;
            }
            
            // day switch finish
            if (switchFinish)
            {
                Log.d(TAG, "Switch finish");
                
                if (Math.abs(mScrollX) >= mWidth)
                {
                    if (mSwitchMode == SWITCH_MODE_PREV)
                        mCurrentDay--;
                    else if (mSwitchMode == SWITCH_MODE_NEXT)
                        mCurrentDay++;
                    
                    // notify the listener
                    if (mDayChangeListener != null)
                        mDayChangeListener.onDayChange(mCurrentDay);
                }
                
                // reset the switch status
                mSwitchMode = SWITCH_MODE_NONE;
                mScrollX = 0;
                
                invalidate();
                
                return;
            }
            
            mHandler.post(this);
            invalidate();
        }
    }
    
    // ======================================================
    // CALLBACK INTERFACE
    // ======================================================
    
    /**
     * Simple callback when current day is changed.
     */
    public interface OnDayChangeListener
    {
        public void onDayChange(int day);
    }
}
