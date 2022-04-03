package com.mego.fizoalarm.startup;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.InitializationStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE;

public class MyApplication extends Application {

    private static final String KEY_FIRST_OPEN_DATE = "first_open_date";
    private final int NUM_OF_DAYS_TO_START_SHOW_OPEN_APP_ADD = 3;

    private static AppOpenManager appOpenManager;

    @Override
    public void onCreate() {
        super.onCreate();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-MM-yyyy");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this) ;
        String firstOpenString = sharedPreferences.getString(KEY_FIRST_OPEN_DATE, "");

        if ( firstOpenString.isEmpty() ) {
            sharedPreferences.edit().putString(KEY_FIRST_OPEN_DATE, formatter.format(LocalDate.now())).apply();
            return;
        }

        LocalDate firstOpenDate = LocalDate.parse(firstOpenString, formatter );
        if ( firstOpenDate.plusDays( NUM_OF_DAYS_TO_START_SHOW_OPEN_APP_ADD ).isBefore(LocalDate.now()) )
            appOpenManager = new AppOpenManager(this);
    }
}