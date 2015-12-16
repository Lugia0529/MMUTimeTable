/*
 * Copyright (c) 2014 Lugia Programming Team
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Log;

import java.util.ArrayList;

// IMPORTATNT CORE FILE
// DO NOT MODIFY THIS IF YOU NOT SURE WHAT YOU'RE DOING

// This file is not guarantee to work for long term, as it strictly follow the structure of MMU ICEMS
// website to parse the course detail, any small change in MMU ICEMS website could cause a great havoc
// effect to the parsing procedure, update of this file is likely happen in such situation.

/**
 * Core class file for parsing course detail we get from MMU ICEMS.
 */
public class CourseParser
{
    public static final int TYPE_NEW_TIME    = 4;
    public static final int TYPE_NEW_SECTION = 5;
    public static final int TYPE_NEW_COURSE  = 6;
    
    public static final String[] DAYS = new String[]
    {
        "SUN",
        "MON",
        "TUE",
        "WED",
        "THU",
        "FRI",
        "SAT"
    };
    
    private static final String TAG = "CourseParser";
    
    // prevent user to instantiate using default constructor
    private CourseParser() { }
    
    public static ArrayList<Subject> tryParse(String source)
    {
        Document document = Jsoup.parse(source);
        
        Elements courseElements = findCourseElements(document);
        Elements scheduleElements = findScheduleElements(document);
        
        // make sure both of the elements are exists
        if (courseElements == null || scheduleElements == null)
            return null;
        
        ArrayList<Subject> subjectList = new ArrayList<Subject>();
        
        parseCourseList(courseElements, subjectList);
        parseScheduleList(scheduleElements, subjectList);
        
        return subjectList;
    }
    
    private static Elements findCourseElements(final Document document)
    {
        // get the registered course
        Elements elems = document.getElementsContainingOwnText("Registered Course");
        
        // should have only 1 element
        if (elems.size() != 1)
        {
            Log.e(TAG, String.format("Expected 1 element for reg course, %d retrieved now.", elems.size()));
            return null;
        }
        
        Element elem = elems.first();
        
        // try to find the parent table
        while (true)
        {
            elem = elem.parent();
            
            if (elem.tagName().equals("table"))
                break;
        }
        
        // course detail are on second table
        elem = elem.nextElementSibling();
        
        return elem.getElementsByTag("table").get(1).select("tr[bgcolor*=#ffffff]");
    }
    
    private static Elements findScheduleElements(final Document document)
    {
        // get the schedule
        Elements elems = document.getElementsContainingOwnText("Schedule");
        
        // should have only 1 element
        if (elems.size() != 1)
        {
            Log.e(TAG, String.format("Expected 1 element for schedule, %d retrieved now.", elems.size()));
            return null;
        }
        
        Element elem = elems.first();
        
        // try to find the parent table
        while (true)
        {
            elem = elem.parent();
            
            if (elem.tagName().equals("table"))
                break;
        }
        
        // schedule detail are on second table
        elem = elem.nextElementSibling();
        
        return elem.getElementsByTag("table").get(1).select("tr[bgcolor*=#ffffff]");
    }
    
    private static void parseCourseList(Elements courseElements, ArrayList<Subject> list)
    {
        int count = 0;
        
        for (Element e : courseElements)
        {
            Elements c = e.children();
            
            /* 0 - index
             * 1 - subject code
             * 2 - subject description
             * 3 - lecture section
             * 4 - tutorial section
             * 5 - credit hour
             */
            if (c.size() != 6)
                continue;
            
            String subjectCode        = c.get(1).text().trim();
            String subjectDescription = c.get(2).text().trim(); 
            String lectureSection     = c.get(3).text().trim();
            String tutorialSection    = c.get(4).text().trim(); 
            String creditsHourString  = c.get(5).text().trim();
            
            if (lectureSection.equals("-"))
                lectureSection = null;
            
            if (tutorialSection.equals("-"))
                tutorialSection = null;
            
            int creditsHour = Integer.parseInt(creditsHourString);
            
            int color = count++; 
            
            Subject subject = new Subject(subjectCode, subjectDescription, lectureSection, tutorialSection, creditsHour, color);
            
            list.add(subject);
        }
    }
    
    private static void parseScheduleList(Elements scheduleElements, ArrayList<Subject> list)
    {
        Subject currentSubject = null;
        int currentSection  = -1;
        
        for (Element e : scheduleElements)
        {
            Elements c = e.children();
            
            /* 0 | 0 | 0 - index
             * 1 |   |   - subject code
             * 2 | 1 |   - section
             * 3 | 2 | 1 - day
             * 4 | 3 | 2 - time
             * 5 | 4 | 3 - room
             */
            switch (c.size())
            {
                case TYPE_NEW_COURSE:
                {
                    String subjectCode = c.get(1).text().trim();
                    String section     = c.get(2).text().trim();
                    String dayString   = c.get(3).text().trim();
                    String timeString  = c.get(4).text().trim();
                    String room        = c.get(5).text().trim();
                    
                    int day = convertDay(dayString);
                    int time = convertTime(timeString);

                    for (Subject subject : list)
                    {
                        if (subject.getSubjectCode().equalsIgnoreCase(subjectCode))
                        {
                            currentSubject = subject;
                            break;
                        }
                    }
                    
                    if (currentSubject != null)
                    {
                        if (section.contains("LEC"))
                            currentSection = Schedule.LECTURE_SECTION;
                        else if (section.contains("TUT"))
                            currentSection = Schedule.TUTORIAL_SECTION;
                        
                        currentSubject.addSchedule(currentSection, day, time, room);
                    }
                    
                    break;
                }
                
                case TYPE_NEW_SECTION:
                {
                    String section     = c.get(1).text().trim();
                    String dayString   = c.get(2).text().trim();
                    String timeString  = c.get(3).text().trim();
                    String room        = c.get(4).text().trim();
                    
                    int day = convertDay(dayString);
                    int time = convertTime(timeString);
                    
                    if (currentSubject != null)
                    {
                        if (section.contains("LEC"))
                            currentSection = Schedule.LECTURE_SECTION;
                        else if (section.contains("TUT"))
                            currentSection = Schedule.TUTORIAL_SECTION;
                        
                        currentSubject.addSchedule(currentSection, day, time, room);
                    }
                    
                    break;
                }
                
                case TYPE_NEW_TIME:
                {
                    String dayString   = c.get(1).text().trim();
                    String timeString  = c.get(2).text().trim();
                    String room        = c.get(3).text().trim();
                    
                    int day = convertDay(dayString);
                    int time = convertTime(timeString);
                    
                    if (currentSubject != null)
                        currentSubject.addSchedule(currentSection, day, time, room);
                    
                    break;
                }
            }
        }
    }
    
    private static int convertDay(String dayString)
    {
        for (int i = 0; i < 7; i++)
            if (dayString.equalsIgnoreCase(DAYS[i]))
                return i;
        
        return -1;
    }
    
    private static int convertTime(String timeString)
    {
        String hourString = timeString.substring(0, 2);
        
        int hour = Integer.parseInt(hourString);
        
        if (timeString.contains("PM") && hour != 12)
            hour += 12;
        
        return hour;
    }
}
