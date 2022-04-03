package com.mego.fizoalarm.main;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.mego.fizoalarm.R;
import com.mego.fizoalarm.receivers.AlarmDeviceAdmin;

public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TITLE_TAG = "settingsActivityTitle";

    public static final String SETTINGS_KEY_FIRST_DAY_IN_WEEK = "first_day_of_week";
    public static final String SETTINGS_KEY_LAST_RINGTONE_DATA = "settings_key_last_ringtone_data";

    public static final String SETTINGS_KEY_ASKED_PHONE_STATE_PERMISSION = "settings_key_asked_phone_permission";
    public static final String SETTINGS_KEY_ASKED_ALERT_WINDOW_PERMISSION = "settings_key_asked_alert_window_permission";

    public static final String SETTINGS_KEY_FLASH_LIGHT_TIMEOUT = "flash_light_timeout";
    public static final String SETTINGS_KEY_AUTO_DISMISS_OR_SNOOZE = "auto_dismiss_or_snooze";
    public static final String SETTINGS_KEY_ALARM_TIME_OUT = "alarm_time_out";
    public static final String SETTINGS_KEY_AFTER_ALARM_TIME_OUT = "after_timeout_event";
    public static final String SETTINGS_KEY_ALLOWED_SNOOZE_TIMES = "allowed_snooze_times";
    public static final String SETTINGS_KEY_DEVICE_ADMIN_ENABLED = "device_admin_enabled";
    public static final String SETTINGS_KEY_INCREASE_SOUND_TO_MAX = "increase_sound_volume_to_max";
    public static final String SETTINGS_KEY_ALWAYS_USE_BUILT_IN_SPEAKER = "always_use_built_in_speaker";
    public static final String SETTINGS_KEY_ONBOARDING_DONE = "onboarding_done";
    public static final String SETTINGS_KEY_REWARDED_AD_EXPIRATION_DATE = "rewarded_ad_expiration_date";


    private static DevicePolicyManager devicePolicyManager;
    private static ComponentName alarmDeviceAdminComponent;
    private static SwitchPreference deviceAdminSwitch;
    private static SharedPreferences.OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new HeaderFragment())
                    .commit();
        } else {
            setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }
        getSupportFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                            setTitle(R.string.title_activity_settings);
                        }
                    }
                });
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        devicePolicyManager = getSystemService(DevicePolicyManager.class);
        alarmDeviceAdminComponent = new ComponentName(this, AlarmDeviceAdmin.class);


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        setTitle(pref.getTitle());
        return true;
    }

    public static class HeaderFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.header_preferences, rootKey);
        }
    }

    public static class GeneralFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.general_preferences, rootKey);
            PreferenceManager.setDefaultValues(getActivity(), R.xml.general_preferences, false);

            deviceAdminSwitch = findPreference(SETTINGS_KEY_DEVICE_ADMIN_ENABLED);
            deviceAdminSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    if (devicePolicyManager.isAdminActive(alarmDeviceAdminComponent)) {
                        devicePolicyManager.removeActiveAdmin(alarmDeviceAdminComponent);
                    } else {
                        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, alarmDeviceAdminComponent);//ComponentName of the administrator component.

                        startActivity(intent);
                    }
                    return false;
                }
            });


            mOnSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if (key.equals(SETTINGS_KEY_DEVICE_ADMIN_ENABLED) && deviceAdminSwitch!=null)
                        deviceAdminSwitch.setChecked(sharedPreferences.getBoolean(SETTINGS_KEY_DEVICE_ADMIN_ENABLED, false));
                }
            };

            PreferenceManager
                    .getDefaultSharedPreferences(getContext())
                    .registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);

        }
    }

    public static class AlarmSettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.alarm_preferences, rootKey);
            PreferenceManager.setDefaultValues(getActivity(), R.xml.alarm_preferences, false);
        }
    }

    @Override
    protected void onDestroy() {
        if (mOnSharedPreferenceChangeListener!=null)
            PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext())
                .unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        super.onDestroy();
    }
}