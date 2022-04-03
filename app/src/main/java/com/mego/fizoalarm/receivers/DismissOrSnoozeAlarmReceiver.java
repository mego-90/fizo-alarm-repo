package com.mego.fizoalarm.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.mego.fizoalarm.main.AlarmManagerBusiness;
import com.mego.fizoalarm.main.AlarmsListFragment;
import com.mego.fizoalarm.main.RingingActivity;
import com.mego.fizoalarm.main.RingingService;
import com.mego.fizoalarm.pojo.Alarm;
import com.mego.fizoalarm.storage.AlarmsListStorage;

//the usage of this broadcast
//is to use pending intent getBroadcast() to stop or snooze alarm from Notification Action
public class DismissOrSnoozeAlarmReceiver extends BroadcastReceiver {

    public static final String INTENT_EXTRA_ALARM_ID = "com.mego.fizoalarm.receivers.DismissOrSnoozeAlarmReceiver.ALARM_ID";
    public static final String INTENT_EXTRA_FROM_DISMISS_OR_SNOOZE_RECEIVER = "com.mego.fizoalarm.receivers.DismissOrSnoozeAlarmReceiver.FROM_DISMISS_OR_SNOOZE_RECEIVER";

    @Override
    public void onReceive(Context context, Intent intent) {

        Alarm alarm = (Alarm) intent.getSerializableExtra(RingingService.INTENT_EXTRA_ALARM);

        //PendingIntent alarmID come with data
        if (alarm == null) {
            if ( ! intent.hasExtra(RingingService.INTENT_EXTRA_ALARM_ID) ) {
                return;
            }

            int alarmID = intent.getIntExtra( RingingService.INTENT_EXTRA_ALARM_ID , 0);
            alarm = AlarmsListStorage.getInstance(context).getAlarmByID(alarmID+"");

            if (alarm == null) {
                return;
            }
        }

        if ( intent.getAction().equals(RingingService.ACTION_DISMISS_ALARM) ) {
            if (alarm.getRepeat_days().size() == 0) {
                alarm.setEnabled(false);
                AlarmsListStorage.getInstance(context).editAlarm(alarm);

                // to dismiss it on UI
                Intent dismissIntent = new Intent(AlarmsListFragment.ACTION_ALARM_DISMISSED);
                dismissIntent.putExtra(AlarmsListFragment.EXTRA_DISMISSED_ALARM_ID, alarm.getId());
                context.sendBroadcast(dismissIntent);
            } else {
                alarm.setSnooze_repeat_count(0);
                AlarmsListStorage.getInstance(context).editAlarm(alarm);
            }

        } else if ( intent.getAction().equals(RingingService.ACTION_SNOOZE_ALARM) ) {
            AlarmManagerBusiness.snoozeAlarm(context, alarm);

            //only in challenge alarm there is max allowed snooze count
            if ( alarm.getChallenge() != null)
                alarm.setSnooze_repeat_count( alarm.getSnooze_repeat_count()+1 );

            AlarmsListStorage.getInstance(context).editAlarm(alarm);
            
            //context.stopService(new Intent(context, RingingService.class) );
        }

        Intent stopOrRunNextAlarmIntent = new Intent(context, RingingService.class);
        //INTENT_EXTRA_ALARM_ID to check in service if equal call next or stop it
        stopOrRunNextAlarmIntent.putExtra(INTENT_EXTRA_ALARM_ID, alarm.getId());
        stopOrRunNextAlarmIntent.putExtra(INTENT_EXTRA_FROM_DISMISS_OR_SNOOZE_RECEIVER, true);
        context.startService( stopOrRunNextAlarmIntent);

        context.sendBroadcast( new Intent(RingingActivity.ACTION_CLOSE_RINGING_ACTIVITY) );
    }

}