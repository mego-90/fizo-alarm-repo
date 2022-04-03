package com.mego.fizoalarm.receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.mego.fizoalarm.R;
import com.mego.fizoalarm.main.SettingsActivity;


public class AlarmDeviceAdmin extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Toast.makeText(context, R.string.device_admin_enabled, Toast.LENGTH_LONG).show();

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(SettingsActivity.SETTINGS_KEY_DEVICE_ADMIN_ENABLED, true)
                .apply();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Toast.makeText(context, R.string.device_admin_disabled, Toast.LENGTH_LONG).show();

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(SettingsActivity.SETTINGS_KEY_DEVICE_ADMIN_ENABLED, false)
                .apply();
    }

}