package com.mego.fizoalarm.main;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.mego.fizoalarm.R;
import com.mego.fizoalarm.pojo.Alarm;
import com.mego.fizoalarm.storage.AlarmsListStorage;

public class SnoozeNotificationReceiver extends BroadcastReceiver {

    public static final String SNOOZE_NOTIFICATION_CHANNEL_ID = "com.mego.fizoalarm.main.ringingService.snooze_channel_id";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getData() == null)
            return;
        String alarmID = intent.getData().toString();
        Alarm alarm = AlarmsListStorage.getInstance(context).getAlarmByID(alarmID);
        if (alarm == null)
            return;

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        String notificationTitle;
        if ( alarm.getLabel()==null || alarm.getLabel().isEmpty() )
            notificationTitle = context.getString( R.string.app_name);
        else
            notificationTitle = alarm.getLabel() ;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createForegroundChannel(notificationManager, context);

        NotificationCompat.Builder notificationBuilder =  new NotificationCompat.Builder(context, SNOOZE_NOTIFICATION_CHANNEL_ID);

        notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentTitle( notificationTitle )
                .setContentText(context.getString(R.string.alarm_snoozed) )
                .setSmallIcon(R.drawable.ic_baseline_alarm_24)
                .setShowWhen(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true);

        notificationManager.notify(alarm.getId(), notificationBuilder.build());

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createForegroundChannel(NotificationManager notificationManager, Context context) {

        String channel_name = context.getString(R.string.snooze_channel_name);

        NotificationChannel channel = new NotificationChannel(SNOOZE_NOTIFICATION_CHANNEL_ID,channel_name, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setLightColor(Color.BLUE);

        notificationManager.createNotificationChannel(channel);

    }

}
