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
    
    private OnDayChangeListener mDayChangeListener = null;
    
    private final ContinueScroll mContinueScroll = new ContinueScroll();
    private final ContinueSwitch mContinueSwitch = new ContinueSwitch();
    
    private int mWidth;
    private int mHeight;
    private int mActualHeight;
    
    private int mTimeCellWidth;
    private int mTextCellWidth;
    private int mCellHeight;
    private int mTextHeight;
    
    private int mCurrentDay;
    
    private int mGridLineColor;
    private int mTimeBackgroundColor;
    
    private int mScrollMode;
    private int mSwitchMode;
    
    private float mScrollX;
    private float mScrollY;
    
    private boolean mScrolling;
    
    /**
     * Use to handle time diff for day switching animation
     */
    private long mLastTimeMillies = 0;
    
    private String[] mTimeStrings;
    
    private final int SWIPE_PAGE_MIN_DISTANCE;
    private final int SWIPE_MIN_VELOCITY;
    private final int SWIPE_OVERFLING_DISTANCE;
    
    public static final int SUNDAY    = 0;
    public static final int MONDAY    = 1;
    public static final int TUESDAY   = 2;
    public static final int WEDNESDAY = 3;
    public static final int THURSDAY  = 4;
    public static final int FRIDAY    = 5;
    public static final int SATURDAY  = 6;
    
    public static final int SCROLL_MODE_NONE       = 1 << 0;
    public static final int SCROLL_MODE_VERTICAL   = 1 << 1;
    public static final int SCROLL_MODE_HORIZONTAL = 1 << 2;
    
    public static final int SWITCH_MODE_PREV = -1;
    public static final int SWITCH_MODE_NONE =  0;
    public static final int SWITCH_MODE_NEXT =  1;
    
    public static final int TEXT_PADDING = 6;
    
    /**
     * transition duration when user switch day
     */
    public static final float DAY_SWITCH_DURATION = 300f;
    
    private static final String TAG = "DayView";
    
    public DayView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        
        mSubjectList = SubjectList.getInstance(context);
        
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
        
        mScrollX = 0;
        mScrollY = 0;
        
        mCellHeight = 100;
        
        mScrollMode = SCROLL_MODE_NONE;
        mSwitchMode = SWITCH_MODE_NONE;
        
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
        
        // measure text dimension of time text
        mTimeCellWidth = (int)mTimePaint.measureText("XX XX") + 6;
        mTextCellWidth = mWidth - mTimeCellWidth;
        
        // measure the common text height
        mTimePaint.getTextBounds("X", 0, 1, mTextBound);
        mTextHeight = mTextBound.height();
        
        mActualHeight = mCellHeight * mTimeStrings.length;
        
        setMeasuredDimension(mWidth, mHeight);
    }
    
    private void validateScrollPosition()
    {
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
        
        // create bitmap cache of each subject if it is empty
        if (mBitmapCache.isEmpty())
            createSubjectBitmapCache();
        
        // only draw bitmap cache of current day
        for (SubjectBitmapCache cache : mBitmapCache)
            if (cache.getDay() == mCurrentDay)
                canvas.drawBitmap(cache.getBitmap(), cache.getX(), cache.getY(), null);
        
        canvas.restore();
        
        // if user are currently on switching day, we need do some extra work
        if (mScrollX != 0)
        {
            canvas.save();
            
            // find the day we need to draw and the X translation
            int day = mScrollX < 0 ? mCurrentDay - 1 : mCurrentDay + 1;
            float translateX = mScrollX < 0 ? -(mWidth + mScrollX) : (mWidth - mScrollX);

            canvas.translate(translateX, -mScrollY);
            
            drawTimeAndGridLine(canvas);
            
            for (SubjectBitmapCache cache : mBitmapCache)
                if (cache.getDay() == day)
                    canvas.drawBitmap(cache.getBitmap(), cache.getX(), cache.getY(), null);
            
            canvas.restore();
        }
    }
    
    private void drawTimeAndGridLine(Canvas canvas)
    {
        // time background
        mBackgroundPaint.setColor(mTimeBackgroundColor);
        canvas.drawRect(0, 0, mTimeCellWidth, mActualHeight, mBackgroundPaint);
        
        // vertical line
        canvas.drawLine(mTimeCellWidth, 0, mTimeCellWidth, mActualHeight, mLinePaint);
        
        int textX = (mTimeCellWidth / 2);
        
        // row line
        for (int i = 0; i < mTimeStrings.length; i++)
        {
            int lineY = mCellHeight * (i + 1);
            int textY = (mCellHeight / 2) + (mTextHeight / 2) + lineY - mCellHeight;
            
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
                String description = subject.getSubjectDescription();
                String room        = schedule.getRoom();
                String section     = schedule.getSection() == Schedule.LECTURE_SECTION ? subject.getLectureSection() : subject.getTutorialSection();
                
                int day    = schedule.getDay();
                int hour   = schedule.getTime() - 8;
                int length = schedule.getLength();
                
                int x = mTimeCellWidth + 1;
                int y = (mCellHeight * hour) + 1;
                
                int width  = mTextCellWidth - 1;
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
                Bitmap bitmap = createSubjectTextBitmap(width - 6, height - 6, code, description, section, room);
                c.drawBitmap(bitmap, 0, (height / 2) - (bitmap.getHeight() / 2), null);
                
                mBitmapCache.add(bitmapCache);
            }
        }
    }
    
    private Bitmap createSubjectTextBitmap(int maxWidth, int maxHeight, String code, String description, String section, String room)
    {
        /*    Code
         * Description
         *   Section
         *    Room
         */
        
        // use to calculate description text width, so we can wrap it to next line if it is too long
        float textWidthArray[] = new float[description.length()];
        mSubjectPaint.getTextWidths(description, textWidthArray);
        
        int textWidth = 0;
        int startPos = 0;
        int spacePos = 0;
        
        ArrayList<String> textList = new ArrayList<String>();
        
        textList.add(code);
        
        for (int pos = 0; pos < textWidthArray.length; pos++)
        {
            textWidth += textWidthArray[pos];
            
            if (description.charAt(pos) == ' ')
                spacePos = pos;
            
            if (textWidth >= maxWidth)
            {
                if (spacePos > startPos)
                {
                    textList.add(description.substring(startPos, spacePos));
                    
                    startPos = spacePos + 1;
                    pos = spacePos;
                }
                else
                {
                    textList.add(description.substring(startPos, pos));
                    
                    startPos = pos;
                }
                
                textWidth = 0;
            }
            
            if (pos + 1 == textWidthArray.length)
                textList.add(description.substring(startPos));
        }
        
        textList.add(section);
        textList.add(room);
        
        int requireHeight = (mTextHeight * textList.size()) + (TEXT_PADDING * (textList.size() - 1));
        
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
                Boolean condition = (Math.abs(mScrollX) > mTimeCellWidth) ^ (mScrollX > 0);
                
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
            
            // don't handle event if is tapping on time cell
            if (e.getX() <= mTimeCellWidth)
                return true;
            
            // try to figure out where user click
            int hour = (int)((mScrollY + e.getY()) / mCellHeight) + 8;
            
            for (Subject subject : mSubjectList)
            {
                for (Schedule schedule : subject.getSchedules())
                {
                    if (schedule.getDay() != mCurrentDay)
                        continue;
                    
                    if (hour >= schedule.getTime() && hour <= schedule.getTime() + schedule.getLength() - 1)
                    {
                        Intent intent = new Intent(mContext, SubjectDetailActivity.class);
                        
                        intent.putExtra(SubjectDetailActivity.EXTRA_SUBJECT_CODE, subject.getSubjectCode());
                  
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
                
                // give vertical scroll a higher priority
                if (absDistY >= absDistX)
                    mScrollMode = SCROLL_MODE_VERTICAL;
                else
                    mScrollMode = SCROLL_MODE_HORIZONTAL;
            }
            
            if (mScrollMode == SCROLL_MODE_VERTICAL)
                mScrollY += distanceY;
            else if (mScrollMode == SCROLL_MODE_HORIZONTAL)
            {
                mScrollX += distanceX;
                
                // don't allow user over scrolling Monday and Friday
                if ((mScrollX < 0 && mCurrentDay == MONDAY) || (mScrollX > 0 && mCurrentDay == FRIDAY))
                    mScrollX = 0;
            }
            
            validateScrollPosition();
            invalidate();
            
            return true;
        }
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
            Log.d(TAG, "onFling");
            
            // don't handle fling if we are on horizontal scrolling
            if (mScrollMode == SCROLL_MODE_HORIZONTAL || mSwitchMode != SWITCH_MODE_NONE)
            {
                mScrollMode = SCROLL_MODE_NONE;
                return true;
            }
            
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
    
    class ContinueSwitch implements Runnable
    {
        public void run()
        {
            if (mSwitchMode == SWITCH_MODE_NONE)
                return;
            
            // check for time different between last and current execution of this thread
            // so we can know how much to scroll the view, this help us maintain a constant
            // speed between day switching animation.
            long diff = SystemClock.elapsedRealtime() - mLastTimeMillies;
            mLastTimeMillies = SystemClock.elapsedRealtime();
            
            float scrollAmount = mWidth / DAY_SWITCH_DURATION * diff;
            
            boolean switchFinish = false;
            
            if (mSwitchMode == SWITCH_MODE_PREV)
            {
                mScrollX -= scrollAmount;
                
                switchFinish = mScrollX > 0 || Math.abs(mScrollX) > mWidth || mCurrentDay == MONDAY;
            }
            else if (mSwitchMode == SWITCH_MODE_NEXT)
            {
                mScrollX += scrollAmount;
                
                switchFinish = mScrollX < 0 || Math.abs(mScrollX) > mWidth || mCurrentDay == FRIDAY;
            }
            
            // day switch finish
            if (switchFinish)
            {
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
