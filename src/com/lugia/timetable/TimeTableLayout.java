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

/*
 * Thing to do:
 * 1. Implement adapter for this layout
 * 2. Fix text overflow issue
 */

package com.lugia.timetable;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.OverScroller;

import java.util.Calendar;

public class TimeTableLayout extends ViewGroup
{
    // AttributeSet
    private int mDisplayType;
    private int mCurrentDay;
    private int mStartTime;
    private int mEndTime;
    
    private int mHeaderColor;
    private int mLineColor;
    
    private int mCellWidth;
    private int mCellHeight;
    
    private int mWidth;
    private int mHeight;
    private int mActualHeight;
    
    private int mTimeCellWidth;
    
    private int mScrollMode;
    private int mScrollX;
    private int mScrollY;
    
    private int mHeaderHeight;
    
    private int mTextSize;
    private int mTextHeight;
    private int mTextMargin;
    
    // screen density
    private float mDensity;
    
    private boolean mShowHeader;
    
    // boolean to control whether to send on click event to OnClickListener even clicked cell has no view, default is false
    private boolean mClickOnEmptyCell;
    
    private Paint mTextPaint;
    private Paint mLinePaint;
    private Paint mBackgroundPaint;
    
    private GestureDetector mGestureDetector;
    
    private ScrollRunnable mScrollRunnable;
    private DayChangeRunnable mDayChangeRunnable;
    
    // listener
    private OnItemClickListener mOnItemClickListener = null;
    private OnDayChangedListener mOnDayChangedListener = null;
    
    private final String[] DAY_STRING = new String[]
    {
        "MON", "TUE", "WED", "THU", "FRI"
    };
    
    private final String[] DAY_FULL_STRING = new String[]
    {
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"
    };
    
    private final String[] TIME_STRING = new String[]
    {
        "12AM", "1AM", "2AM", "3AM", "4AM", "5AM", "6AM", "7AM", "8AM", "9AM", "10AM", "11AM",
        "12PM", "1PM", "2PM", "3PM", "4PM", "5PM", "6PM", "7PM", "8PM", "9PM", "10PM", "11PM"
    };
    
    public static final int TYPE_DAY = 1;
    public static final int TYPE_WEEK = 2;
    
    public static final int SCROLL_MODE_HORIZONTAL = -1;
    public static final int SCROLL_MODE_NONE       =  0;
    public static final int SCROLL_MODE_VERTICAL   =  1;
    
    public static final int MONDAY    = 1;
    public static final int TUESDAY   = 2;
    public static final int WEDNESDAY = 3;
    public static final int THURSDAY  = 4;
    public static final int FRIDAY    = 5;
    
    private static final String TAG = "TimeTableLayout";
    
    public TimeTableLayout(Context context) 
    {
        super(context);
        
        init(context);
    }
    
    public TimeTableLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.TimeTableLayout);

        mDensity = getResources().getDisplayMetrics().density;
        
        int defaultCellHeight = isInEditMode() ? (int)(mDensity * 100) : getResources().getDimensionPixelSize(R.dimen.TimeTableLayout_DefaultCellHeight);
        int defaultTextSize   = isInEditMode() ? (int)(mDensity * 16)  : getResources().getDimensionPixelSize(R.dimen.TimeTableLayout_DefaultTextSize);
        int defaultTextMargin = isInEditMode() ? (int)(mDensity * 3)   : getResources().getDimensionPixelSize(R.dimen.TimeTableLayout_DefaultTextMargin);
        
        mTextMargin = defaultTextMargin;
        
        mDisplayType = array.getInt(R.styleable.TimeTableLayout_displayType, TYPE_DAY);
        
        mStartTime = array.getInt(R.styleable.TimeTableLayout_startTime, 8 );
        mEndTime   = array.getInt(R.styleable.TimeTableLayout_endTime,   22);
        
        mCurrentDay = array.getInt(R.styleable.TimeTableLayout_currentDay, MONDAY);
        
        mCellHeight = array.getDimensionPixelSize(R.styleable.TimeTableLayout_cellHeight, defaultCellHeight);
        
        mTextSize = array.getDimensionPixelSize(R.styleable.TimeTableLayout_textSize, defaultTextSize);
        
        mHeaderColor = array.getColor(R.styleable.TimeTableLayout_headerColor, 0xFFE6E6E6);
        mLineColor   = array.getColor(R.styleable.TimeTableLayout_lineColor,   0xFFC6C6C6);
        
        mShowHeader = array.getBoolean(R.styleable.TimeTableLayout_showHeader, mDisplayType == TYPE_WEEK);
        
        array.recycle();
        
        checkParam();
        init(context);
    }
    
    protected void checkParam()
    {
        if (mStartTime < 0)
            throw new IllegalArgumentException("Invalid startTime: " + mStartTime);
        
        if (mEndTime > 23)
            throw new IllegalArgumentException("Invalid endTime: " + mEndTime);
            
        if (mEndTime < mStartTime)
            throw new IllegalArgumentException("endTime cannot be less than startTime!");
        
        if (mCellHeight <= 0)
            throw new IllegalArgumentException("cellHeight cannot be negative or 0");
    }
    
    protected void init(Context context)
    {
        setFocusable(true);
        
        setFocusableInTouchMode(true);
        
        Typeface font = Typeface.createFromAsset(context.getAssets(), context.getResources().getString(R.string.font_medium));
        
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setStyle(Paint.Style.STROKE);
        mTextPaint.setTypeface(font);
        
        mLinePaint = new Paint();
        mLinePaint.setAntiAlias(true);
        mLinePaint.setColor(mLineColor);
        mLinePaint.setStyle(Paint.Style.STROKE);
        
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        
        mGestureDetector = new GestureDetector(context, new GestureListener());
        
        mScrollRunnable = new ScrollRunnable(context);
        mDayChangeRunnable = new DayChangeRunnable();
        
        mClickOnEmptyCell = false;

        mScrollY = 0;
        mScrollX = 0;
        
        mScrollMode = SCROLL_MODE_NONE;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        
        // measure text dimension
        final Rect textBound = new Rect();
        
        // measure the width needed to display the time text 
        mTextPaint.getTextBounds("XXXX", 0, 4, textBound);
        mTextHeight = textBound.height();
        
        // calculate cell width for the time text
        mTimeCellWidth = textBound.width() + (mTextMargin * 2);
        
        // calculate cell width to accumulate child view
        if (mDisplayType == TYPE_DAY)
            mCellWidth = mWidth - mTimeCellWidth;
        else
            mCellWidth = (mWidth - mTimeCellWidth) / 5;
        
        // real height for entire time table
        mActualHeight = mCellHeight * (mEndTime - mStartTime + 1);
        
        if (mShowHeader)
        {
            mHeaderHeight = mTextHeight + (mTextMargin * 2);
            mActualHeight += mHeaderHeight;
        }
        
        // scroll x is only for day display
        if (mDisplayType == TYPE_DAY)
            mScrollX = (mCurrentDay - 1) * mCellWidth;
        
        // prevention for scroll y overflow
        if (mScrollY + mHeight >= mActualHeight)
            mScrollY = mActualHeight - mHeight;
        
        // record our dimensions
        setMeasuredDimension(MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY));
        
        // processing child layout
        final int chileCount = getChildCount();
        
        for (int i = 0; i < chileCount; i++)
        {
            View child = getChildAt(i);
            
            // dont process hidden view
            if (child == null || child.getVisibility() == View.GONE)
                continue;
            
            LayoutParams params = (LayoutParams)child.getLayoutParams();
            
            if (params.time > mEndTime)
                continue;
            
            // shrink the view if it exceed the end time
            int duration = Math.min(params.duration, mEndTime - params.time + 1);
            
            int w = mCellWidth - params.leftMargin - params.rightMargin;
            int h = (mCellHeight * duration) - params.topMargin - params.bottomMargin;
            
            child.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
        }
    }
    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        final int chileCount = getChildCount();
        
        for (int i = 0; i < chileCount; i++)
        {
            View child = getChildAt(i);
            
            // dont process hidden view
            if (child == null || child.getVisibility() == View.GONE)
                continue;
            
            LayoutParams params = (LayoutParams)child.getLayoutParams();
            
            if (params.time > mEndTime)
                continue;
            
            int duration = params.time - mStartTime;
            
            int x = mTimeCellWidth + params.leftMargin;
            int y = (mCellHeight * duration) + params.topMargin;
            
            // further processing x position
            x += (params.day - 1) * mCellWidth;
            
            if (mDisplayType == TYPE_DAY)
                x -= mScrollX;
            
            // adjust y position if header is visible
            if (mShowHeader)
                y += mHeaderHeight;
            
            // adjust y position to match current scroll position
            y -= mScrollY;
            
            // for easy resize
            child.setPivotX(0);
            child.setPivotY(0);
            
            child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
        }
    }
    
    @Override
    protected TimeTableLayout.LayoutParams generateDefaultLayoutParams() 
    {
        return new TimeTableLayout.LayoutParams(TimeTableLayout.LayoutParams.MATCH_PARENT, TimeTableLayout.LayoutParams.MATCH_PARENT);
    }
    
    @Override
    public TimeTableLayout.LayoutParams generateLayoutParams(AttributeSet attrs)
    {
        return new TimeTableLayout.LayoutParams(getContext(), attrs);
    }
    
    @Override
    protected TimeTableLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p)
    {
        return new TimeTableLayout.LayoutParams(p);
    }
    
    // Override to allow type-checking of LayoutParams.
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p)
    {
        return p instanceof TimeTableLayout.LayoutParams;
    }
    
    @Override
    protected void dispatchDraw(Canvas canvas)
    {
        // Horizontal line to separate day
        for (int i = 1; i < 5; i++)
            canvas.drawLine(mTimeCellWidth + (mCellWidth * i) - mScrollX, 0, mTimeCellWidth + (mCellWidth * i) - mScrollX, mHeight, mLinePaint);
        
        // row lines on child view area
        for (int i = 0; i <= mEndTime - mStartTime; i++)
        {
            float lineY = (mCellHeight * (i + 1)) - mScrollY;

            if (mShowHeader)
                lineY += mHeaderHeight;
            
            canvas.drawLine(mTimeCellWidth, lineY, mWidth, lineY, mLinePaint);
        }
        
        // draw all the children
        super.dispatchDraw(canvas);
        
        // header background
        mBackgroundPaint.setColor(mHeaderColor);
        canvas.drawRect(0, 0, mTimeCellWidth, mActualHeight, mBackgroundPaint);
        
        // vertical line
        canvas.drawLine(mTimeCellWidth, 0, mTimeCellWidth, mActualHeight, mLinePaint);
        
        // position x of time text on left side
        int textX = (mTimeCellWidth / 2);
        
        // row line and time text
        for (int i = 0; i <= mEndTime - mStartTime; i++)
        {
            float lineY = (mCellHeight * (i + 1)) - mScrollY;
            float textY = ((mCellHeight / 2) + (mTextHeight / 2) + lineY - mCellHeight);
            
            if (mShowHeader)
            {
                lineY += mHeaderHeight;
                textY += mHeaderHeight;
            }
            
            canvas.drawLine(0, lineY, mTimeCellWidth, lineY, mLinePaint);
            canvas.drawText(TIME_STRING[mStartTime + i], textX, textY, mTextPaint);
        }
        
        // header
        if (mShowHeader)
        {
            canvas.drawRect(0, 0, mWidth, mHeaderHeight, mBackgroundPaint);
            
            canvas.drawLine(0, mHeaderHeight, mWidth, mHeaderHeight, mLinePaint);
            
            if (mDisplayType == TYPE_DAY)
            {
                int x = mTimeCellWidth + (mCellWidth / 2);
                
                canvas.drawText(DAY_FULL_STRING[mCurrentDay - 1], x, mHeaderHeight - mTextMargin, mTextPaint);
            }
            else
            {            
                for (int i = 0; i < 5; i++)
                {
                    int x = mTimeCellWidth + (mCellWidth * i) + (mCellWidth / 2);
        
                    canvas.drawText(DAY_STRING[i], x, mHeaderHeight - mTextMargin, mTextPaint);
                }
            }
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_UP)
        {
            // special handle for day switching in day display
            if (mDisplayType == TYPE_DAY && mScrollMode == SCROLL_MODE_HORIZONTAL)
            {
                double threshold = mCellWidth * 0.15f;
                
                double amount = (mCellWidth * (mCurrentDay - 1)) - mScrollX;
                
                int targetDay = mCurrentDay;
                
                // only change day if user scroll exceed the threshold
                if (Math.abs(amount) > threshold)
                    targetDay = (amount < 0) ? mCurrentDay + 1 : mCurrentDay - 1;

                mDayChangeRunnable.start(targetDay);
                
                mScrollMode = SCROLL_MODE_NONE;
                
                return true;
            }
            
            // reset the scroll mode if user finger leave the screen
            mScrollMode = SCROLL_MODE_NONE;
        }
        
        return mGestureDetector.onTouchEvent(event);
    }

    private void positionChildren()
    {
        final int chileCount = getChildCount();

        for (int i = 0; i < chileCount; i++)
        {
            View child = getChildAt(i);

            // dont process hidden view
            if (child == null || child.getVisibility() == View.GONE)
                continue;

            LayoutParams params = (LayoutParams)child.getLayoutParams();

            if (params.time > mEndTime)
                continue;

            int adjustment = params.time - mStartTime;

            int x = mTimeCellWidth + params.leftMargin;
            int y = (mCellHeight * adjustment) + params.topMargin;

            // further processing x position
            x += (params.day - 1) * mCellWidth;

            if (mDisplayType == TYPE_DAY)
                x -= mScrollX;

            // further processing
            if (mShowHeader)
                y += mHeaderHeight;

            y -= mScrollY;
            
            child.layout(x, y, x + child.getWidth(), y + child.getHeight());
        }
    }
    
    private void validateScrollPosition()
    {
        // check for scroll position
        if (mScrollY < 0)
            mScrollY = 0;
        
        // dont allow over scrolling
        if (mScrollY + mHeight > mActualHeight)
            mScrollY = mActualHeight - mHeight;
    }

    // -----------------------------------------
    // Getter and Setter
    // -----------------------------------------
    
    public void setDisplayType(int type)
    {
        // validate type
        if (type != TYPE_DAY && type != TYPE_WEEK)
            throw new IllegalArgumentException("Display type must be one of the TYPE_DAY or TYPE_WEEK!");
        
        // dont do anything if the type is same as current type
        if (mDisplayType == type)
            return;
        
        mDisplayType = type;

        if (mDisplayType == TYPE_DAY)
        {
            mCellWidth = mWidth - mTimeCellWidth;
            mScrollX = (mCurrentDay - 1) * mCellWidth;
        }
        else
        {
            mCellWidth = (mWidth - mTimeCellWidth) / 5;
            mScrollX = 0;
        }
        
        
        // processing child layout
        final int chileCount = getChildCount();

        for (int i = 0; i < chileCount; i++)
        {
            View child = getChildAt(i);

            // dont process hidden view
            if (child == null || child.getVisibility() == View.GONE)
                continue;

            LayoutParams params = (LayoutParams)child.getLayoutParams();

            if (params.time > mEndTime)
                continue;

            int x = mTimeCellWidth + params.leftMargin;

            // further processing x position
            x += (params.day - 1) * mCellWidth;

            if (mDisplayType == TYPE_DAY)
                x -= mScrollX;

            // calculate the new width of the child
            int w = mCellWidth - params.leftMargin - params.rightMargin;

            DisplayTypeChangeAnimation anim = new DisplayTypeChangeAnimation(child, x, w);
            anim.setDuration(500);
            child.startAnimation(anim);
        }
        
        invalidate();
    }
    
    public void setCurrentDay(int day)
    {
        // do nothing is target day is same as current day
        if (day == mCurrentDay)
            return;
        
        // reject invalid value
        if (day < MONDAY || day > FRIDAY)
            throw new IllegalArgumentException("Invalid day value: " + day);
        
        mCurrentDay = day;
        invalidate();

        // inform the listener
        if (mOnDayChangedListener != null)
            mOnDayChangedListener.onDayChanged(mCurrentDay);
    }
    
    public void setHeaderVisibility(boolean enabled)
    {
        mShowHeader = enabled;
        
        requestLayout();
        invalidate();
    }
    
    public void setReportClickOnEmptyCell(boolean enabled)
    {
        mClickOnEmptyCell = enabled;
    }
    
    public void setOnItemClickListener(OnItemClickListener listener)
    {
        mOnItemClickListener = listener;
    }
    
    public void setOnDayChangedListener(OnDayChangedListener listener)
    {
        mOnDayChangedListener = listener;
        
        // fire up the listener once so the listener know the current day
        if (mOnDayChangedListener != null)
            mOnDayChangedListener.onDayChanged(mCurrentDay);
    }
    
    public int getStartTime()
    {
        return mStartTime;
    }
    
    public int getEndTime()
    {
        return mEndTime;
    }
    
    public int getCurrentDay()
    {
        return mDisplayType == TYPE_DAY ? mCurrentDay : -1;
    }

    public boolean getHeaderVisibility()
    {
        return mShowHeader;
    }
    
    public boolean getReportClickOnEmptyCell()
    {
        return mClickOnEmptyCell;
    }

    // -----------------------------------------
    // Public Method
    // -----------------------------------------
    
    public void addView(View child, int day, int time, int duration)
    {
        TimeTableLayout.LayoutParams params = (TimeTableLayout.LayoutParams)child.getLayoutParams();
        
        if (params == null)
        {
            params = generateDefaultLayoutParams();
            
            if (params == null)
                throw new IllegalArgumentException("generateDefaultLayoutParams() cannot return null");
        }
        
        params.day = day;
        params.time = time;
        params.duration = duration;
        
        addView(child, params);
    }
    
    public View getChildAtTime(int day, int time)
    {
        final int childCount = getChildCount();
        
        for (int i = 0; i < childCount; i++)
        {
            View child = getChildAt(i);
            
            // avoid NPE
            if (child == null)
                continue;
            
            LayoutParams params = (LayoutParams)child.getLayoutParams();
            
            if (day == params.day && time >= params.time && time <= params.time + params.duration - 1)
                return child;
        }
        
        return null;
    }
    
    public void scrollToCurrentTime()
    {
        Calendar calendar = Calendar.getInstance();
        
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        
        if (hour >= mStartTime)
        {
            mScrollY = (hour - mStartTime) * mCellHeight;
            
            mScrollY += (mCellHeight / 60) * minute;
            
            invalidate();
        }
    }
    
    // -----------------------------------------
    // Gesture Listener
    // -----------------------------------------
    
    private final class GestureListener extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onDown(MotionEvent e)
        {
            mScrollRunnable.stop();
            
            return true;
        }
        
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e)
        {
            // don't handle event if is tapping on time cell
            if (e.getX() <= mTimeCellWidth)
                return true;
            
            float y = e.getY();
            
            // adjust the y position if header is visible
            if (mShowHeader)
                y -= mHeaderHeight;
            
            // dont handle click event if the click is happen on the header area
            if (mShowHeader && y <= 0)
                return true;
            
            // try to figure out where user click
            int day = (((int)e.getX() - mTimeCellWidth) / mCellWidth) + 1;
            int time = ((mScrollY + (int)y) / mCellHeight) + mStartTime;
            
            if (mDisplayType == TYPE_DAY)
                day = mCurrentDay;
            
            View clickedView = getChildAtTime(day, time);
            
            if (mOnItemClickListener != null && (clickedView != null || mClickOnEmptyCell))
                mOnItemClickListener.onItemClick(clickedView, day, time);
            
            return true;
        }
        
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            if (mScrollMode == SCROLL_MODE_NONE)
            {
                // only day display have 2 types of scroll
                if (mDisplayType == TYPE_DAY)
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
                else if (mDisplayType == TYPE_WEEK)
                    mScrollMode = SCROLL_MODE_VERTICAL;
            }
            
            if (mScrollMode == SCROLL_MODE_HORIZONTAL)
            {
                mScrollX += distanceX;
                
                // don't allow user over scrolling Monday and Friday
                mScrollX = Math.max(0, mScrollX);
                mScrollX = Math.min(mScrollX, mCellWidth * 4);
            }
            else if (mScrollMode == SCROLL_MODE_VERTICAL)
                mScrollY += distanceY;
            
            validateScrollPosition();
            
            positionChildren();
            invalidate();
            
            return true;
        }
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
            mScrollRunnable.start((int)velocityY);
            
            return true;
        }
    }
    
    // -----------------------------------------
    // Scroll Runnable
    // -----------------------------------------
    
    private final class ScrollRunnable implements Runnable
    {
        private boolean mScrolling;
        
        private OverScroller mScroller;
        
        public ScrollRunnable(Context context)
        {
            mScroller = new OverScroller(context);
            
            mScrolling = false;
        }
        
        public void start(int velocity)
        {
            mScroller.fling(0, mScrollY,
                            0, -velocity,
                            0, 0,
                            0, mActualHeight - mHeight,
                            0, 0);
            
            mScrolling = true;
            
            postOnAnimation(this);
        }
        
        public void stop()
        {
            mScrolling = false;
        }
        
        public void run()
        {
            boolean newOffset = mScrolling && mScroller.computeScrollOffset();
            
            mScrollY = mScroller.getCurrY();
            
            positionChildren();
            invalidate();
            
            if (newOffset)
                postOnAnimation(this);
            else
                mScrolling = false;
        }
    }
    
    // -----------------------------------------
    // Display Type Change Animation
    // -----------------------------------------
    
    private final class DisplayTypeChangeAnimation extends Animation
    {
        private View mView;
        
        private int mStartX;
        private int mTargetX;
        
        private int mStartWidth;
        private int mTargetWidth;
        
        public DisplayTypeChangeAnimation(View view, int targetX, int targetWidth)
        {
            mView = view;
            
            mStartX = (int)view.getX();
            mTargetX = targetX;
            
            mStartWidth = view.getWidth();
            mTargetWidth = targetWidth;
        }
        
        @Override
        public void initialize(int width, int height, int parentWidth, int parentHeight)
        {
            super.initialize(width, height, parentWidth, parentHeight);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t)
        {
            int newX = (int)(mStartX + (mTargetX - mStartX) * interpolatedTime);
            int newWidth = (int)(mStartWidth + (mTargetWidth - mStartWidth) * interpolatedTime);
            
            mView.layout(newX, mView.getTop(), newX + newWidth, mView.getBottom());
        }
        
        @Override
        public boolean willChangeBounds()
        {
            return true;
        }
    }
    
    // -----------------------------------------
    // Day Change Runnable
    // -----------------------------------------
    
    private final class DayChangeRunnable implements Runnable
    {
        private int mTargetDay;
        
        private long mLastTimeMillies;
        
        public DayChangeRunnable()
        {
            mLastTimeMillies = -1;
        }
        
        public boolean isRunning()
        {
            return mLastTimeMillies != -1;
        }
        
        public void start(int targetDay)
        {
            // avoid starting multiple runnable
            if (isRunning())
                return;
            
            mTargetDay = targetDay;
            mLastTimeMillies = SystemClock.elapsedRealtime();
            
            postOnAnimation(this);
        }
        
        public void stop()
        {
            mLastTimeMillies = -1;
        }
        
        public void run()
        {
            // check for time different between last and current execution of this thread
            // so we can know how much to scroll the view, this help us maintain a constant
            // speed between day switching animation.
            long diff = SystemClock.elapsedRealtime() - mLastTimeMillies;
            mLastTimeMillies = SystemClock.elapsedRealtime();
            
            float scrollAmount = mCellWidth / 300f * diff;
            
            boolean switchFinish;
            
            int target = (mCellWidth * (mTargetDay - 1));
            
            if (mTargetDay < mCurrentDay)
            {
                mScrollX -= scrollAmount;
                
                switchFinish = mScrollX < target;
            }
            else
            {
                mScrollX += scrollAmount;
                
                switchFinish = mScrollX > target;
            }
            
            positionChildren();
            invalidate();
            
            if (switchFinish)
            {
                mScrollX = target;
                mCurrentDay = mTargetDay;
                
                mLastTimeMillies = -1;
                
                // notify the listener
                if (mOnDayChangedListener != null)
                    mOnDayChangedListener.onDayChanged(mCurrentDay);
                
                positionChildren();
                invalidate();
            }
            else
                postOnAnimation(this);
        }
    }
    
    // -----------------------------------------
    // Layout Parameters
    // -----------------------------------------
    
    public static class LayoutParams extends ViewGroup.MarginLayoutParams
    {
        public int day;
        public int time;
        public int duration;
        
        public LayoutParams(int width, int height)
        {
            super(width, height);
            
            this.day      = 0;
            this.time     = 0;
            this.duration = 1;
        }
        
        public LayoutParams(int width, int height, int day, int time, int duration)
        {
            super(width, height);
            
            this.day = day;
            this.time = time;
            this.duration = duration;
        }

        public LayoutParams(Context context, AttributeSet attrs)
        {
            super(context, attrs);
            
            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.TimeTableLayoutParams);
            
            day      = array.getInt(R.styleable.TimeTableLayoutParams_day,      1);
            time     = array.getInt(R.styleable.TimeTableLayoutParams_time,     1);
            duration = array.getInt(R.styleable.TimeTableLayoutParams_duration, 1);
            
            array.recycle();
        }
        
        public LayoutParams(ViewGroup.LayoutParams source)
        {
            super(source);
        }
    }
    
    // -----------------------------------------
    // Callback Interface
    // -----------------------------------------
    
    /**
     * Interface definition for a callback to be invoked when an item in this TimeTableLayout has been clicked.
     */
    public interface OnItemClickListener
    {
        /**
         * Callback method to be invoked when an item in the TimeTableLayout has been clicked.
         * 
         * @param view The view within the TimeTableLayout that was clicked, maybe null if click on empty cell is enabled.
         * @param day The clicked day clicked item from range 1 to 5.
         * @param time The clicked time hour of the clicked item, in 24 hour format.
         */
        void onItemClick(View view, int day, int time);
    }
    
    /**
     * Interface definition for a callback to be invoked when current day of TimeTableLayout has changed.
     */
    public interface OnDayChangedListener
    {
        /**
         * Callback method to be invoked when current day of TimeTableLayout has changed.
         * 
         * @param day the new current day.
         */
        void onDayChanged(int day);
    }
}
