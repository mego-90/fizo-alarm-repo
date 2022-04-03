package com.mego.fizoalarm.main;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.widget.Toast;

import com.mego.fizoalarm.R;
import com.mego.fizoalarm.pojo.Alarm;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Calendar;

public class AlarmManagerBusiness {

    //private static int REQUEST_CODE_FOR_RINGING = 100;
    //private static int REQUEST_CODE_FOR_SNOOZE = 991151;

    public static void scheduleAlarm(Context context, Alarm alarm, boolean showToast) {

        Calendar alarmTriggerCalendar = Calendar.getInstance();

        if ( alarm.getRepeat_days().size()>0 )
            alarm.changeDateToNextRepeat();

        alarmTriggerCalendar
                .set(alarm.getDate().getYear(),alarm.getDate().getMonthValue()-1,alarm.getDate().getDayOfMonth(),alarm.getTime().getHour(),alarm.getTime().getMinute(),0);

        Intent intent = new Intent(context, FireAlarmReceiver.class);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.setData(Uri.parse( String.valueOf(alarm.getId()) ));
        PendingIntent sender = PendingIntent.getBroadcast(context, alarm.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTriggerCalendar.getTimeInMillis(), sender);

        if (showToast)
            Toast.makeText(context, getToastText(context, alarm), Toast.LENGTH_LONG).show();
    }

    private static String getToastText(Context context, Alarm alarm) {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(now,LocalDateTime.of(alarm.getDate(), alarm.getTime()) );

        if (duration.toMinutes() == 0)
            return context.getString(R.string.toast_alarm_schedule_duration_less_than_a_minute);
        else if (duration.toHours() < 1) {
            long minutes = duration.toMinutes();
            return context.getString(R.string.toast_alarm_schedule_duration_with_minutes, minutes);
        } else if ( duration.toHours() < 24 ) {
            long hours = duration.toHours();
            long minutes = duration.minusHours(hours).toMinutes();
            return context.getString(R.string.toast_alarm_schedule_duration_with_hours, hours, minutes);
        } else {
            long days = duration.toDays();
            long hours = duration.minusDays(days).toHours();
            long minutes = duration.minusDays(days).minusHours(hours).toMinutes();
            return context.getString(R.string.toast_alarm_schedule_duration_with_days, days, hours, minutes);
        }
    }

    public static void snoozeAlarm(Context context, Alarm alarm) {
        Calendar afterSnoozeCalendar = Calendar.getInstance();
        afterSnoozeCalendar.setTimeInMillis(SystemClock.elapsedRealtime());
        if ( alarm.getSnooze_in_minutes() > 0 )
            afterSnoozeCalendar.add(Calendar.MINUTE, alarm.getSnooze_in_minutes() );
        else //snooze from banner ad while snooze is not allowed
            afterSnoozeCalendar.add(Calendar.MINUTE, 2 );

        Intent intent = new Intent(context, FireAlarmReceiver.class);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.setData(Uri.parse( String.valueOf(alarm.getId()) ));
        intent.putExtra(FireAlarmReceiver.INTENT_EXTRA_IS_SNOOZE_ALARM,true);

        //if code = alarm.getID , alarm or snooze disabled when alarm switch disabled
        // so when there is challenge give it another request code
        // so if alarm switched off from main screen after snoozed, snooze still scheduled.
        int requestCode;
        if ( alarm.getChallenge() == null)
            requestCode = alarm.getId();
        else
            requestCode = -1 * alarm.getId();

        PendingIntent sender = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, afterSnoozeCalendar.getTimeInMillis(), sender);
    }

    public static void disableAlarm(Context context, Alarm alarm) {
        Intent intent = new Intent(context, FireAlarmReceiver.class);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.setData(Uri.parse( String.valueOf(alarm.getId()) ));
        PendingIntent sender = PendingIntent.getBroadcast(context, alarm.getId() , intent, PendingIntent.FLAG_NO_CREATE);
        if (sender != null) {
            AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
            alarmManager.cancel(sender);
            sender.cancel();
        }

    }

    public static void editAlarm(Context context, Alarm alarm) {

        disableAlarm(context, alarm);
        scheduleAlarm(context, alarm, true);
    }

}