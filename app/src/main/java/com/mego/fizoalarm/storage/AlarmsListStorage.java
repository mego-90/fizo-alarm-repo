package com.mego.fizoalarm.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.mego.fizoalarm.R;
import com.mego.fizoalarm.pojo.Alarm;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AlarmsListStorage {

    private final String SHARED_PREFERENCE_FILE = "com.mego.fizoalarm.alarms_list";

    private SharedPreferences mSharedPreferences;
    private static AlarmsListStorage sInstance;

    private AlarmsListStorage(Context context) {
        mSharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE_FILE,Context.MODE_PRIVATE);
    }

    public static AlarmsListStorage getInstance(Context context) {

        if (sInstance == null)
            sInstance = new AlarmsListStorage(context);
        return sInstance;
    }

    public void saveAlarm(Alarm alarm) {
        Random random = new Random();
        int generatedID = random.nextInt(10000000);//10 millions
        while ( generatedID==0 || mSharedPreferences.contains( String.valueOf(generatedID)) )
            generatedID = random.nextInt(Integer.MAX_VALUE);

        alarm.setId(generatedID);
        mSharedPreferences.edit().putString( String.valueOf(alarm.getId()), alarm.toJson()).apply();
    }

    public List<Alarm> getAllAlarms() {
        List<Alarm> alarmsList = new ArrayList<>();
        for (String key : mSharedPreferences.getAll().keySet() )
            alarmsList.add(Alarm.fromJson(mSharedPreferences.getString(key,"")));

        Collections.sort(alarmsList);

        return alarmsList;
    }

    public List<Alarm> getAllEnabledAlarms() {
        List<Alarm> alarmsList = new ArrayList<>();
        for (String key : mSharedPreferences.getAll().keySet() ) {
            Alarm alarm = Alarm.fromJson(mSharedPreferences.getString(key, ""));
            if (alarm.isEnabled())
                alarmsList.add(alarm);
        }
        return alarmsList;
    }

    public void editAlarm(Alarm alarm) {
        if ( !mSharedPreferences.contains( String.valueOf(alarm.getId())) )
            return;

        mSharedPreferences.edit().putString(String.valueOf(alarm.getId()), alarm.toJson()).apply();
    }

    public void deleteAlarms(List<Alarm> alarmsList) {
        if (alarmsList == null || alarmsList.size() == 0)
            return;

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        for (Alarm alarm : alarmsList) {
            editor.remove(String.valueOf(alarm.getId()));
            if (alarm.getRingtoneData().getTypeID() == R.id.recorded_voice_ringtone_data_type)
                deleteVoiceRecord( alarm.getRingtoneData().getUri().getPath() );
        }
        editor.apply();
    }

    public Alarm getAlarmByID(String alarmID) {
        String alarmJsonString = mSharedPreferences.getString(alarmID,null);
        if (alarmJsonString == null)
            return null;
        return Alarm.fromJson(alarmJsonString);

    }

    public int getAlarmsCount() {
        return mSharedPreferences.getAll().size();
    }

    private void deleteVoiceRecord(String path) {
        File recordedFile = new File(path);
        recordedFile.delete();
    }

}