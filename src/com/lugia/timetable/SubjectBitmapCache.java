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

import android.graphics.Bitmap;

/**
 * Helper class that store a copy of a schedule bitmap to allow faster drawing for DayView and WeekView.
 */
public class SubjectBitmapCache
{
    private int mDay;
    
    private float mX;
    private float mY;
    
    private Bitmap mBitmap;
    
    public SubjectBitmapCache(int day, float x, float y, int width, int height)
    {
        mDay = day;
        
        mX = x;
        mY = y;
        
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }
    
    public int getDay()
    {
        return mDay;
    }
    
    public float getX()
    {
        return mX;
    }
    
    public float getY()
    {
        return mY;
    }
    
    public int getWidth()
    {
        return mBitmap.getWidth();
    }
    
    public int getHeight()
    {
        return mBitmap.getHeight();
    }
    
    public Bitmap getBitmap()
    {
        return mBitmap;
    }
}
