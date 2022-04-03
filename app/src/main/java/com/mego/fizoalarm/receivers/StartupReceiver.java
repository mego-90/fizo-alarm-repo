package com.mego.fizoalarm.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mego.fizoalarm.main.AlarmManagerBusiness;
import com.mego.fizoalarm.pojo.Alarm;
import com.mego.fizoalarm.storage.AlarmsListStorage;

import java.time.LocalDateTime;
import java.util.List;

public class StartupReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        LocalDateTime now = LocalDateTime.now();

        List<Alarm> allEnabledAlarms = AlarmsListStorage.getInstance(context).getAllEnabledAlarms();

        for (Alarm alarm : allEnabledAlarms ) {

            if ( alarm.getRepeat_days().size()>0 || isInFuture(now, alarm) )
                AlarmManagerBusiness.scheduleAlarm(context, alarm, false);
            else {
                alarm.setEnabled(false);
                AlarmsListStorage.getInstance(context).editAlarm(alarm);
            }
        }
    }

    public boolean isInFuture(LocalDateTime now, Alarm alarm) {
        return LocalDateTime.of(alarm.getDate(), alarm.getTime()).isAfter(now);
    }

}