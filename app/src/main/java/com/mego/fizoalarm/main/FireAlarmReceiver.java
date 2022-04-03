package com.mego.fizoalarm.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mego.fizoalarm.pojo.Alarm;
import com.mego.fizoalarm.storage.AlarmsListStorage;

public class FireAlarmReceiver extends BroadcastReceiver {

    public static final String INTENT_EXTRA_IS_SNOOZE_ALARM = "com.mego.hellAlarm.main.FireAlarmReceiver.intent_extra_is_snooze_alarm" ;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getData() == null)
            return;

        String alarmID = intent.getData().toString();
        Alarm alarm = AlarmsListStorage.getInstance(context).getAlarmByID(alarmID);
        if (alarm == null)
            return;

        if ( alarm.getRepeat_days().size() > 0 && !(intent.hasExtra(INTENT_EXTRA_IS_SNOOZE_ALARM)) )
            AlarmManagerBusiness.scheduleAlarm(context, alarm, false);

        Intent serviceIntent = new Intent(context, RingingService.class);
        serviceIntent.putExtra(RingingService.INTENT_EXTRA_ALARM, alarm);
        context.startService(serviceIntent);

    }


}