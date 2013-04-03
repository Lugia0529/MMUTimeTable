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
    private Handler mHandler;
    private Resources mResources;
    private OverScroller mScroller;
    private GestureDetector mGestureDetector;
    
    private Paint mTimePaint;
    private Paint mSubjectPaint;
    private Paint mLinePaint;
    private Paint mBackgroundPaint;

    private Rect mTextBound;
    
    private SubjectList mSubjectList;
    private ArrayList<SubjectBitmapCache> mBitmapCache;
    
    private final ContinueScroll mContinueScroll = new ContinueScroll();
    
    private int mWidth;
    private int mHeight;
    private int mActualHeight;
    
    private int mTimeCellWidth;
    private int mDayCellWidth;
    private int mHeaderHeight;
    private int mCellHeight;
    private int mTextHeight;
    
    private int mGridLineColor;
    private int mTimeBackgroundColor;
    
    private int mScrollMode;
    
    private float mScrollY;

    private boolean mScrolling;
    
    private String mFilename;
    private String[] mTimeStrings;
    private String[] mDayStrings;
    
    private final int SWIPE_PAGE_MIN_DISTANCE;
    private final int SWIPE_MIN_VELOCITY;
    private final int SWIPE_OVERFLING_DISTANCE;
    
    public static final int SCROLL_MODE_NONE       = 1 << 0;
    public static final int SCROLL_MODE_VERTICAL   = 1 << 1;
    public static final int SCROLL_MODE_HORIZONTAL = 1 << 2;
    
    public static final int TEXT_PADDING = 6;
    
    private static final String TAG = "WeekView";
    
    public WeekView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        
        mScroller = new OverScroller(context);
        mGestureDetector = new GestureDetector(context, new GestureListener());
        mBitmapCache = new ArrayList<SubjectBitmapCache>();
        mTextBound = new Rect();
        
        final ViewConfiguration vc = ViewConfiguration.get(context);
        
        SWIPE_PAGE_MIN_DISTANCE  = vc.getScaledPagingTouchSlop();
        SWIPE_MIN_VELOCITY       = vc.getScaledMinimumFlingVelocity();
        SWIPE_OVERFLING_DISTANCE = vc.getScaledOverflingDistance();
        
        mContext = context;
        mResources = context.getResources();
        
        mGridLineColor       = mResources.getColor(R.color.timetable_grid_line_color);
        mTimeBackgroundColor = mResources.getColor(R.color.timetable_time_background_color);
        
        mTimeStrings = mResources.getStringArray(R.array.time_string);
        mDayStrings  = mResources.getStringArray(R.array.day_string);
        
        mScrollY = 0;
        
        mCellHeight = 100;
        
        mScrollMode = SCROLL_MODE_NONE;
        
        mScrolling = false;
        
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
    protected void onAttachedToWindow()
    {
        if (mHandler == null)
            mHandler = getHandler();
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        
        // measure the dimension of time text
        mTimeCellWidth = (int)mTimePaint.measureText("XX XX") + 6;
        mDayCellWidth = (int)(mWidth - mTimeCellWidth) / 5;
        
        // measure the common text height
        mTimePaint.getTextBounds("X", 0, 1, mTextBound);
        mTextHeight = mTextBound.height();
        
        mHeaderHeight = mTextHeight + 10;
        
        mActualHeight = mCellHeight * mTimeStrings.length + mHeaderHeight;
        
        setMeasuredDimension(mWidth, mHeight);
    }
    
    private void validateScrollPosition()
    {
        if (mScrollY < 0)
            mScrollY = 0;
        
        if (mScrollY + mHeight > mActualHeight)
            mScrollY = mActualHeight - mHeight;
    }
    
    // ======================================================
    // UI Drawing
    // ======================================================
    
    @Override
    protected void onDraw(Canvas canvas)
    {
        canvas.drawColor(Color.WHITE);
        
        canvas.save();
        canvas.translate(0, -mScrollY + mHeaderHeight);
        
        drawTimeAndGridLine(canvas);
        
        // create bitmap cache of each subject if it is empty
        if (mBitmapCache.isEmpty())
            createSubjectBitmapCache();
        
        // in week view, we draw all of the bitmap in the bitmap cache
        for (SubjectBitmapCache cache : mBitmapCache)
            canvas.drawBitmap(cache.getBitmap(), cache.getX(), cache.getY(), null);
        
        canvas.restore();
        
        // draw the week header at last so the time table wont cover it
        drawWeekHeader(canvas);
    }
    
    private void drawWeekHeader(Canvas canvas)
    {
        mBackgroundPaint.setColor(mTimeBackgroundColor);
        canvas.drawRect(0, 0, mWidth, mHeaderHeight, mBackgroundPaint);
        
        canvas.drawLine(0, mHeaderHeight, mWidth, mHeaderHeight, mLinePaint);
        
        for (int i = 0; i < 5; i++)
        {
            int x = mTimeCellWidth + (mDayCellWidth * i) + (mDayCellWidth / 2);
            canvas.drawText(mDayStrings[i + 1], x, mHeaderHeight - 5, mTimePaint);
        }
    }
    
    private void drawTimeAndGridLine(Canvas canvas)
    {
        // time background
        mBackgroundPaint.setColor(mTimeBackgroundColor);
        canvas.drawRect(0, 0, mTimeCellWidth, mActualHeight, mBackgroundPaint);
        
        // vertical line
        canvas.drawLine(mTimeCellWidth, 0, mTimeCellWidth, mActualHeight, mLinePaint);
        
        // day line
        for (int i = 1; i <= 4; i++)
            canvas.drawLine(mTimeCellWidth + (mDayCellWidth * i), 0, mTimeCellWidth + (mDayCellWidth * i), mActualHeight, mLinePaint);
        
        // row line
        for (int i = 0; i < mTimeStrings.length; i++)
        {
            int lineY = mCellHeight * (i + 1);
            
            mTimePaint.getTextBounds(mTimeStrings[i], 0, mTimeStrings[i].length(), mTextBound);
            
            int textHeight = mTextBound.height();
            
            int textX = (mTimeCellWidth / 2);
            int textY = (mCellHeight / 2) + (textHeight / 2) + lineY - mCellHeight;
            
            canvas.drawLine(0, lineY, mWidth, lineY, mLinePaint);
            canvas.drawText(mTimeStrings[i], textX, textY, mTimePaint);
        }
    }
    
    private void createSubjectBitmapCache()
    {
        if (mSubjectList == null)
            return;
        
        for (int i = 0; i < mSubjectList.size(); i++)
        {
            Subject subject = mSubjectList.get(i);
            
            for (Schedule schedule : subject.getSchedules())
            {
                String code        = subject.getSubjectCode();
                String room        = schedule.getRoom();
                
                int day    = schedule.getDay();
                int hour   = schedule.getTime() - 8;
                int length = schedule.getLength();
                
                int x = mTimeCellWidth + (mDayCellWidth * (day - 1)) + 1;
                int y = (mCellHeight * hour) + 1;
                
                int width  = mDayCellWidth - 1;
                int height = (length * mCellHeight) - 1;
                
                SubjectBitmapCache bitmapCache = new SubjectBitmapCache(day, x, y, width, height);
                Canvas c = new Canvas(bitmapCache.getBitmap());
                
                // eliminate lines
                mBackgroundPaint.setColor(Color.WHITE);
                c.drawRect(0, 0, width, height, mBackgroundPaint);
                
                // draw the background
                mBackgroundPaint.setColor(subject.getColor());
                c.drawRect(3, 3, width - 3, height - 3, mBackgroundPaint);
                
                // draw the text
                Bitmap bitmap = createSubjectTextBitmap(width, height, code, room);
                c.drawBitmap(bitmap, 0, (height / 2) - (bitmap.getHeight() / 2), null);
                
                mBitmapCache.add(bitmapCache);
            }
        }
    }
    
    private Bitmap createSubjectTextBitmap(int maxWidth, int maxHeight, String code, String room)
    {
        ArrayList<String> textList = new ArrayList<String>();
        
        textList.add(code);
        textList.add(room);
        
        int requireHeight = (mTextHeight * 2) + TEXT_PADDING;
        
        Bitmap bitmap = Bitmap.createBitmap(maxWidth, requireHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        int xPos = maxWidth / 2;
        int yPos = mTextHeight;
        
        for (String str : textList)
        {
            canvas.drawText(str, xPos, yPos, mSubjectPaint);
            yPos += mTextHeight + TEXT_PADDING;
        }
        
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
        boolean fileLoaded = filename != null ? loadFile(filename) : loadFileFromSystem();
        
        // exit method if we are failed to load file
        if (!fileLoaded)
            return;
        
        this.mFilename = filename;
        
        mBitmapCache.clear();
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
            if (e.getY() <= mHeaderHeight || e.getX() <= mTimeCellWidth)
                return true;
            
            // try to figure out where user click
            int hour = (int)((mScrollY - mHeaderHeight + e.getY()) / mCellHeight) + 8;
            int day = (int)((e.getX() - mTimeCellWidth) / mDayCellWidth) + 1;
            
            for (Subject subject : mSubjectList)
            {
                for (Schedule schedule : subject.getSchedules())
                {
                    if (schedule.getDay() != day)
                        continue;
                    
                    if (hour >= schedule.getTime() && hour <= schedule.getTime() + schedule.getLength() - 1)
                    {
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
            
            validateScrollPosition();
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
            
            mHandler.post(this);
            invalidate();
        }
    }
}
