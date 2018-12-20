package com.example.android.harjoitus7_8;

import android.icu.text.LocaleDisplayNames;
import android.icu.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Roope on 07-Mar-18.
 */

public class TrainingEntry {
    private long mTime;
    private String mDate;
    private int mDuration;
    private int mRpe;
    private int mSharpness;
    private String mSport;

    public TrainingEntry() {}

    public TrainingEntry(long time, int duration, int rpe, int sharpness, String sport) {
        mTime = time;
        //mDate = date;
        mDuration = duration;
        mRpe = rpe;
        mSharpness = sharpness;
        mSport = sport;
    }

    public long getTime() { return mTime; }
    public void setTime(long time) { mTime = time; }

  /*  public String getDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(mTime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        return dateFormat.format(calendar.getTime());
    }
    public void setDate(String date) { mDate = date; }*/

    public int getDuration() { return mDuration; }
    public void setDuration(int duration) { mDuration = duration; }

    public int getRpe() { return mRpe; }
    public void setRpe(int rpe) { mRpe = rpe; }

    public int getSharpness() { return mSharpness; }
    public void setSharpness(int sharpness) { mSharpness = sharpness; }

    public String getSport() { return mSport; }
    public void setSport(String sport) { mSport = sport; }
}
