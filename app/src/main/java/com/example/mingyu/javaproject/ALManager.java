package com.example.mingyu.javaproject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/**
 * Created by melon on 2017-05-24.
 */

public class ALManager{

    public void setAlarm(Context context, int hour, int min) {
        Calendar mCalender = Calendar.getInstance();
        mCalender.set(Calendar.HOUR_OF_DAY, hour);
        mCalender.set(Calendar.MINUTE, min);

        Intent mAlarmIntent = new Intent("com.test.alarm.ALARM_START");

        PendingIntent mPendingIntent = PendingIntent.getBroadcast(context, 0, mAlarmIntent, PendingIntent.FLAG_ONE_SHOT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, mCalender.getTimeInMillis(),mPendingIntent);
    }


}
