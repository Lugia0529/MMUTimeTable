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
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

public class WeekView extends View
{
    private Context mContext;
    private SubjectList mSubjectList;
    private Resources mResources;
    private Rect mTextBound;
    
    private Handler mHandler;
    private OverScroller mScroller;
    private GestureDetector mGestureDetector;
    
    private final ContinueScroll mContinueScroll = new ContinueScroll();
    
    private Paint mTimePaint;
    private Paint mSubjectPaint;
    private Paint mLinePaint;
    private Paint mBackgroundPaint;
    
    private int mGridLineColor;
    private int mTimeBackgroundColor;
    
    private int mWidth;
    private int mHeight;
    private int mActualHeight;
    private int mTimeGridWidth;
    private int mDayGridWidth;
    private int mHeaderHeight;
    private int mCellHeight;
    
    private final int SWIPE_PAGE_MIN_DISTANCE;
    private final int SWIPE_MIN_VELOCITY;
    private final int SWIPE_OVERFLING_DISTANCE;
    
    private int mScrollMode;
    
    private float mScrollY;
    private float mScaleFactor;
    
    private boolean mScrolling;
    
    private String mFilename;
    
    private String[] mTimeStrings;
    private String[] mDayStrings;
    
    public static final int TEXT_PADDING = 6;
    
    public static final int SCROLL_MODE_NONE       = 1 << 0;
    public static final int SCROLL_MODE_VERTICAL   = 1 << 1;
    public static final int SCROLL_MODE_HORIZONTAL = 1 << 2;
    
    private static final String TAG = "WeekView";
    
    public WeekView(Context context, AttributeSet attrs)
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
        mDayStrings = mResources.getStringArray(R.array.day_string);
        
        mScrollMode = SCROLL_MODE_NONE;
        mScrollY = 0;
        mScaleFactor = 1;
        
        mScrolling = false;
        
        mCellHeight = 100;
        
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
    
    private void remeasure()
    {
        mCellHeight = (int)(100 * mScaleFactor);
        mActualHeight = mCellHeight * mTimeStrings.length + mHeaderHeight;
        
        // check for scroll position
        if (mScrollY < 0)
            mScrollY = 0;
        
        if (mScrollY + mHeight > mActualHeight)
            mScrollY = mActualHeight - mHeight;
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        
        // measure the dimension of time text
        mTimeGridWidth = (int)mTimePaint.measureText("XX XX") + 6;
        
        // TODO: this is not a good way to know the actual day width
        mDayGridWidth = (int)(mWidth - mTimeGridWidth) / 5;
        
        // measure the dimension of day text
        mTimePaint.getTextBounds("XXX", 0, 3, mTextBound);
        
        mHeaderHeight = mTextBound.height() + 10;
        
        setMeasuredDimension(mWidth, mHeight);
        
        remeasure();
    }
    
    @Override
    protected void onDraw(Canvas canvas)
    {
        canvas.drawColor(Color.WHITE);
        
        // draw the header first
        drawWeekHeader(canvas);
        
        canvas.save();
        canvas.translate(0, -mScrollY + mHeaderHeight);
        
        // time background
        mBackgroundPaint.setColor(mTimeBackgroundColor);
        canvas.drawRect(0, 0, mTimeGridWidth, mActualHeight, mBackgroundPaint);
        
        // vertical line
        canvas.drawLine(mTimeGridWidth, 0, mTimeGridWidth, mActualHeight, mLinePaint);
        
        // day line
        for (int i = 1; i <= 4; i++)
            canvas.drawLine(mTimeGridWidth + (mDayGridWidth * i), 0, mTimeGridWidth + (mDayGridWidth * i), mActualHeight, mLinePaint);
        
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
        
        drawSubject(canvas);
        
        canvas.restore();
        
        drawWeekHeader(canvas);
    }
    
    private void drawWeekHeader(Canvas canvas)
    {
        mBackgroundPaint.setColor(mTimeBackgroundColor);
        canvas.drawRect(0, 0, mWidth, mHeaderHeight, mBackgroundPaint);
        
        canvas.drawLine(0, mHeaderHeight, mWidth, mHeaderHeight, mLinePaint);
        
        for (int i = 0; i < 5; i++)
        {
            int x = mTimeGridWidth + (mDayGridWidth * i) + (mDayGridWidth / 2);
            canvas.drawText(mDayStrings[i + 1], x, mHeaderHeight - 5, mTimePaint);
        }
    }
    
    private void drawSubject(Canvas canvas)
    {
        if (mSubjectList == null)
            return;
        
        for (int i = 0; i < mSubjectList.size(); i++)
        {
            Subject subject = mSubjectList.get(i);
            
            for (Schedule time : subject.getSchedules())
            {
                int day    = time.getDay();
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
                
                int startX = mTimeGridWidth + (mDayGridWidth * (day - 1));
                int startY = mCellHeight * hour;
                int endX   = startX + mDayGridWidth;
                int endY   = mCellHeight * (hour + length);
                
                // eliminate lines
                mBackgroundPaint.setColor(Color.WHITE);
                canvas.drawRect(startX + 1, startY + 1, endX, endY, mBackgroundPaint);
                
                // draw the background
                mBackgroundPaint.setColor(subject.getColor());
                canvas.drawRect(startX + 3, startY + 3, endX - 2, endY - 2, mBackgroundPaint);
                
                Bitmap bitmap = createSubjectTextBitmap(endX - startX - 5, endY - startY - 5, code, description, section, room);
                canvas.drawBitmap(bitmap, startX + 3, (mCellHeight * hour) + ((mCellHeight * length) / 2) - (bitmap.getHeight() / 2), null);
            }
        }
    }
    
    private Bitmap createSubjectTextBitmap(int maxWidth, int maxHeight, String code, String description, String section, String room)
    {
        mSubjectPaint.getTextBounds(code, 0, code.length(), mTextBound);
        int codeHeight = mTextBound.height();
        
        mSubjectPaint.getTextBounds(section, 0, section.length(), mTextBound);
        int sectionHeight = mTextBound.height();
        
        mSubjectPaint.getTextBounds(room, 0, room.length(), mTextBound);
        int roomHeight = mTextBound.height();
        
        int reqHeight = codeHeight + sectionHeight + roomHeight + (TEXT_PADDING + TEXT_PADDING);
        
        Bitmap bitmap = Bitmap.createBitmap(maxWidth, reqHeight, Bitmap.Config.ARGB_8888);
        
        int xPos = maxWidth / 2;
        
        Canvas canvas = new Canvas(bitmap);
        
        // For debugging purpose, draw the bound of this bitmap
        //canvas.drawRect(0, 0, maxWidth, reqHeight, mLinePaint);
        
        int yPos = codeHeight;
        
        // draw the text
        canvas.drawText(code, xPos, yPos, mSubjectPaint);
        
        yPos += sectionHeight + TEXT_PADDING;
        
        canvas.drawText(section, xPos, yPos, mSubjectPaint);
        
        yPos += roomHeight + TEXT_PADDING;
        
        canvas.drawText(room, xPos, yPos, mSubjectPaint);
        
        return bitmap;
    }
    
    public SubjectList getSubjectList()
    {
        return this.mSubjectList;
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
            
            // don't handle event if is tapping on header or time cell
            if (e.getY() <= mHeaderHeight || e.getX() <= mTimeGridWidth)
                return true;
            
            // try to figure out where user click
            int value = (int)((mScrollY - mHeaderHeight + e.getY()) / mCellHeight);
            int day = (int)((e.getX() - mTimeGridWidth) / mDayGridWidth) + 1;
            
            Log.d(TAG, "x = " + e.getX() + ", DAY = " + day);
            
            // TODO: some hack
            value = value + 8;
            
            for (Subject subject : mSubjectList)
            {
                for (Schedule schedule : subject.getSchedules())
                {
                    if (schedule.getDay() != day)
                        continue;
                    
                    if (value >= schedule.getTime() && value <= schedule.getTime() + schedule.getLength() - 1)
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
            
            // we only have vertical scrolling on week view
            if (mScrollMode == SCROLL_MODE_NONE)
                mScrollMode = SCROLL_MODE_VERTICAL;
            
            if (mScrollMode == SCROLL_MODE_VERTICAL)
                mScrollY += distanceY;
            
            remeasure();
            invalidate();
            
            return true;
        }
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
            Log.d(TAG, "onFling");
            
            float y1 = e1.getY();
            float y2 = e2.getY();
            
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
}
