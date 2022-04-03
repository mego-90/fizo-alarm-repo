package com.mego.fizoalarm.main;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioRouting;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.mego.fizoalarm.R;
import com.mego.fizoalarm.pojo.Alarm;
import com.mego.fizoalarm.pojo.RingtoneData;
import com.mego.fizoalarm.receivers.DismissOrSnoozeAlarmReceiver;
import com.mego.fizoalarm.storage.AlarmsListStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RingingService extends Service implements AudioManager.OnAudioFocusChangeListener {

    public static boolean IS_RUNNING = false;

    public static final String INTENT_EXTRA_ALARM = "com.mego.fizoalarm.main.ringingService.arg_alarm";
    public static final String INTENT_EXTRA_ALARM_ID = "com.mego.fizoalarm.main.ringingService.arg_alarm_ID";
    public static final String ACTION_DISMISS_ALARM = "com.mego.fizoalarm.ringingService.ACTION_DISMISS_ALARM";
    public static final String ACTION_SNOOZE_ALARM = "com.mego.fizoalarm.ringingService.ACTION_SNOOZE_ALARM";
    //public static final int FOREGROUND_SERVICE_NOTIFICATION_ID = 1991;
    //public static final int MISSED_ALARM_INSUFFICIENT_PERMISSION_NOTIFICATION_ID = 999999900;
    public static final int PENDING_INTENT_RINGING_ACTIVITY_ID = 999999901;
    public static final int PENDING_INTENT_RINGING_ACTIVITY_FULL_SCREEN_ID = 999999902;
    public static final int PENDING_INTENT_DISMISS_ID = 999999903;
    public static final int PENDING_INTENT_SNOOZE_ID = 999999904;
    public static final int PENDING_INTENT_OPEN_SETTINGS_ID = 999999905;

    public static final String RINGING_NOTIFICATION_CHANNEL_ID = "com.mego.fizoalarm.main.ringingService.ringing_channel_id";

    private Alarm mAlarm;

    private Vibrator mVibrator;
    private MediaPlayer mMediaPlayer;
    private ToneGenerator mToneGenerator ;
    private NotificationManager mNotificationManager;
    private Uri mDefaultAlarmRingtoneUri;

    private AudioManager mAudioManager;
    private AudioAttributes mAudioAttributes;
    private AudioFocusRequest mAudioFocusRequest;

    private Handler mRequestAudioFocusHandler;
    private Handler mScreenOnHandler;
    private Handler mVolumeUpHandler;

    private CountDownTimer mAutoDismissCountDownTimer;
    private CountDownTimer mFlashLightCountDownTimer;

    private BroadcastReceiver mCallBroadReceiver;
    private BroadcastReceiver mDismissOrSnoozeAlarmReceiver;
    private BroadcastReceiver mScreenOffBroadcastReceiver;

    private SharedPreferences mSharedPreferences;

    private List<Alarm> mAlarmsQueue = new ArrayList<>();

    private final IBinder localBinder = new MyBinder();

    private CameraManager mCameraManager;
    private String mCameraID;
    private boolean mIsFlashOn = false;

    private SoundPool soundPool;
    private int soundPoolFizoId;

    public RingingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    @Override
    public void onCreate() {

        super.onCreate();
        IS_RUNNING = true;

        soundPool = new SoundPool.Builder().build();
        soundPoolFizoId = soundPool.load(this, R.raw.fizo_voice ,1);
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                playFizoVoice();
            }
        });

        mVibrator = getSystemService(Vibrator.class);
        mAudioManager = getSystemService(AudioManager.class);
        mToneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM,ToneGenerator.MAX_VOLUME);
        mNotificationManager = getSystemService(NotificationManager.class);
        mDefaultAlarmRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        mScreenOnHandler = new Handler();
        mRequestAudioFocusHandler = new Handler();

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mDismissOrSnoozeAlarmReceiver = new DismissOrSnoozeAlarmReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_DISMISS_ALARM);
        intentFilter.addAction(ACTION_SNOOZE_ALARM);
        registerReceiver(mDismissOrSnoozeAlarmReceiver, intentFilter);

        assignPhoneStateBroadcastAndRegister();
        if (mAudioManager.getMode() == AudioManager.MODE_NORMAL)
            assignScreenOffBroadcastAndRegister();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createForegroundChannel();

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createForegroundChannel() {

        String channel_name = getString(R.string.ringing_service_channel_name);

        NotificationChannel channel = new NotificationChannel(RINGING_NOTIFICATION_CHANNEL_ID,channel_name, NotificationManager.IMPORTANCE_HIGH);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setLightColor(Color.BLUE);
        //channel.setVibrationPattern(new long[] {0,100,10,300});

        //NotificationManager notificationManager = getSystemService(NotificationManager.class);
        mNotificationManager.createNotificationChannel(channel);

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Alarm newPushedAlarm = (Alarm)intent.getSerializableExtra(INTENT_EXTRA_ALARM);

        if ( intent.hasExtra(DismissOrSnoozeAlarmReceiver.INTENT_EXTRA_FROM_DISMISS_OR_SNOOZE_RECEIVER) ) {
            int alarmIdToDismissOrSnooze = intent.getIntExtra(DismissOrSnoozeAlarmReceiver.INTENT_EXTRA_ALARM_ID, 0);

            if (mAlarm != null && alarmIdToDismissOrSnooze!=0 && mAlarm.getId()==alarmIdToDismissOrSnooze)
                runNextAlarmOrStopService();
            else
                stopSelf(startId);
            // stopSelf take some time to take effect. so it may continue to next statements.
            return START_NOT_STICKY;
        }

        if ( newPushedAlarm==null )
            throw new IllegalStateException("Ringing Service Started with no new Alarm.");

        mAlarmsQueue.add(newPushedAlarm);

        if ( mMediaPlayer == null) {
            mAlarm = mAlarmsQueue.get(0);
            mAlarmsQueue.remove(0);
            processAlarm();
        }

        return START_NOT_STICKY;
    }

    private void processAlarm() {

        //mAlarm = (Alarm)intent.getSerializableExtra(INTENT_EXTRA_ALARM);

        assignAutoDismissCountDownTimer();
        flashLightIfRequired();

        String alarmLabel = mAlarm.getLabel()==null || mAlarm.getLabel().isEmpty() ? getString(R.string.alarm_default_label_in_notification) :mAlarm.getLabel();

        Intent notificationIntent = new Intent(this, RingingActivity.class);
        notificationIntent.setData(Uri.parse( String.valueOf(mAlarm.getId()) ));
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        NotificationCompat.Builder notificationBuilder =  new NotificationCompat.Builder(this, RINGING_NOTIFICATION_CHANNEL_ID);

        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(getString( R.string.app_name) )
                .setContentText(alarmLabel)
                .setSmallIcon(R.drawable.ic_baseline_alarm_24)
                .setOngoing(true)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setShowWhen(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(PendingIntent.getActivity(this,PENDING_INTENT_RINGING_ACTIVITY_FULL_SCREEN_ID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT),true)
                .setContentIntent( PendingIntent.getActivity(this, PENDING_INTENT_RINGING_ACTIVITY_ID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT) );

        if ( mAlarm.getChallenge() == null && mAlarm.getSnooze() ) {

            Intent notificationDismissIntent = new Intent(RingingService.ACTION_DISMISS_ALARM);
            notificationDismissIntent.putExtra(INTENT_EXTRA_ALARM_ID, mAlarm.getId() );
            PendingIntent notificationDismissPendingIntent =
                    PendingIntent.getBroadcast(this, PENDING_INTENT_DISMISS_ID, notificationDismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent notificationSnoozeIntent = new Intent(RingingService.ACTION_SNOOZE_ALARM);
            notificationSnoozeIntent.putExtra(INTENT_EXTRA_ALARM_ID, mAlarm.getId() );
            PendingIntent notificationSnoozePendingIntent =
                    PendingIntent.getBroadcast(this, PENDING_INTENT_SNOOZE_ID , notificationSnoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT);


            notificationBuilder.addAction(R.drawable.ic_baseline_alarm_off_24, getString(R.string.dismiss), notificationDismissPendingIntent);
            notificationBuilder.addAction(R.drawable.ic_baseline_snooze_24, getString(R.string.snooze), notificationSnoozePendingIntent);
        }

        startForeground(mAlarm.getId(), notificationBuilder.build());

        PowerManager powerManager = this.getSystemService(PowerManager.class);
        //Screen is Off
        if ( ! powerManager.isInteractive()) {

            Intent ringingActivityIntent = new Intent(this, RingingActivity.class);
            ringingActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ringingActivityIntent.putExtra(RingingService.INTENT_EXTRA_ALARM, mAlarm);
            startActivity(ringingActivityIntent);
        }

        int focusRequestValue ;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            mAudioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

            mAudioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(mAudioAttributes)
                    .setOnAudioFocusChangeListener(this)
                    .setWillPauseWhenDucked(true)
                    .setAcceptsDelayedFocusGain(true)
                    .build();

            focusRequestValue = mAudioManager.requestAudioFocus(mAudioFocusRequest);

        } else
            focusRequestValue = mAudioManager.requestAudioFocus(this,AudioManager.STREAM_ALARM,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);


        try {
            // even in silent we need media player
            // we check if null , to check if alarm is ringing or 'vibrating in silent' to play next alarm
            if ( mMediaPlayer == null )
                mMediaPlayer = new MediaPlayer();

            if (mAlarm.getRingtoneData().getUri().equals(Uri.EMPTY) )
                throw new IOException();

            initMediaPlayer(focusRequestValue);

        } catch (IOException ex) { // Silent Alarm, Uri is EMPTY
            if ( focusRequestValue == AudioManager.AUDIOFOCUS_REQUEST_GRANTED )
                startVibration();
            else {
                makeTone();
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
                    int readPhoneStatePermissionRequest = checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
                    if (readPhoneStatePermissionRequest == PackageManager.PERMISSION_DENIED)
                        manageNoReadPhoneStatePermission();

                }
            }
        }


        if (mSharedPreferences.getBoolean(SettingsActivity.SETTINGS_KEY_INCREASE_SOUND_TO_MAX, true))
            overrideSoundVolumeToMax();

    }

    private void initMediaPlayer(int focusRequestValue) throws IOException {

        try {
            mMediaPlayer.setDataSource(this, mAlarm.getRingtoneData().getUri());
        } catch (IOException ex) {
            setLastSelectedRingtoneToDefault();
            mMediaPlayer.setDataSource(this, mDefaultAlarmRingtoneUri);
            //if default not found it throws the IO Exception so will vibrate only.
        }

        if (mSharedPreferences.getBoolean(SettingsActivity.SETTINGS_KEY_ALWAYS_USE_BUILT_IN_SPEAKER, true)) {

            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            else
                mMediaPlayer.setAudioAttributes(mAudioAttributes);

        }

        mMediaPlayer.prepareAsync();
        mMediaPlayer.setLooping(true);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            mMediaPlayer.setAudioAttributes(mAudioAttributes);

        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                if ( focusRequestValue == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ) {
                    mediaPlayer.start();
                    startVibration();
                } else {
                    makeTone();
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O ) {
                        int readPhoneStatePermissionRequest = checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
                        if (readPhoneStatePermissionRequest == PackageManager.PERMISSION_DENIED)
                            manageNoReadPhoneStatePermission();

                    }
                }
            }
        });

    }

    private void manageNoReadPhoneStatePermission() {

        Intent openSettingIntent = new Intent();
        openSettingIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        openSettingIntent.setData(uri);
        openSettingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent openSettingPendingIntent =
                PendingIntent.getActivity(RingingService.this,PENDING_INTENT_OPEN_SETTINGS_ID,openSettingIntent,PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder missedAlarmNotificationBuilder
                =  new NotificationCompat.Builder(RingingService.this, RINGING_NOTIFICATION_CHANNEL_ID);

        missedAlarmNotificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(getString( R.string.permission_phone_state_needed) )
                //.setContentText( getString(R.string.missed_alarm_no_read_phone_state_permission) )
                .setSmallIcon(R.drawable.ic_baseline_alarm_off_24)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.missed_alarm_no_read_phone_state_permission)) )
                .setShowWhen(true)
                .addAction(R.drawable.ic_baseline_tune_24, getString(R.string.open_settings), openSettingPendingIntent)
                .setContentIntent(openSettingPendingIntent);
        //same id to replace the notification
        mNotificationManager.notify(mAlarm.getId(), missedAlarmNotificationBuilder.build());

        if (mAlarm.getRepeat_days().size() == 0) {
            mAlarm.setEnabled(false);
            AlarmsListStorage.getInstance(RingingService.this).editAlarm(mAlarm);
        }

        sendBroadcast( new Intent(RingingActivity.ACTION_CLOSE_RINGING_ACTIVITY) );
        stopForegroundAndKeepServiceToShowNotification();
    }

    public void makeTone() {
        mToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2);
    }

    public void overrideSoundVolumeToMax() {

        if (mAlarm.getRingtoneData().getUri().equals(Uri.EMPTY))
            return;

        mVolumeUpHandler = new Handler();

        int musicMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int alarmMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);

        Runnable volumeUpRunnable = new Runnable() {
            @Override
            public void run() {
                int musicCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                int alarmCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                if (alarmCurrentVolume < alarmMaxVolume )
                    mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, alarmMaxVolume, 0);
                if (musicCurrentVolume < musicMaxVolume)
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, musicMaxVolume, 0);
                mVolumeUpHandler.postDelayed(this, 10*1000);

            }
        };
        mVolumeUpHandler.postDelayed(volumeUpRunnable, 10*1000);
    }

    //when insufficient phone state permission
    public void stopForegroundAndKeepServiceToShowNotification() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N )
            stopForeground(Service.STOP_FOREGROUND_DETACH);
        else
            stopForeground(false);

        cleanMeUp();
    }

    private void startVibration() {

        if ( !mAlarm.isVibration() )
            return;

        if ( mVibrator.hasVibrator() ) {
            long pattern[] = {0,1000,200,1000};

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                mVibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            else
                mVibrator.vibrate(pattern,0);
        }
    }

    private void stopVibration() {
        if ( mVibrator.hasVibrator()) {
            mVibrator.cancel();
        }
    }

    private void assignPhoneStateBroadcastAndRegister() {

        Runnable tryRequestAudioFocusRunnable = new Runnable() {
            @Override
            public void run() {
                int audioRequestValue = mAudioManager.requestAudioFocus(RingingService.this ,AudioManager.STREAM_ALARM,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                int repeat = 0 ;
                while (audioRequestValue != AudioManager.AUDIOFOCUS_REQUEST_GRANTED && repeat < 5) {
                    try {
                        Thread.sleep(2*1000);
                        repeat++;
                        audioRequestValue = mAudioManager.requestAudioFocus(RingingService.this ,AudioManager.STREAM_ALARM,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                    } catch (InterruptedException e) {
                    }
                }
                if (audioRequestValue == AudioManager.AUDIOFOCUS_REQUEST_GRANTED && mMediaPlayer != null) {
                    if (mAlarm.getRingtoneData().getUri() != Uri.EMPTY)
                        mMediaPlayer.start();
                    startVibration();
                }
            }
        };

        mCallBroadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String telephonyManagerState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

                if ( telephonyManagerState == null || telephonyManagerState.isEmpty() )
                    return;

                if ( telephonyManagerState.equals(TelephonyManager.EXTRA_STATE_IDLE) ) {

                    if (mScreenOffBroadcastReceiver != null)
                        registerReceiver(mScreenOffBroadcastReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF) );
                    else
                        assignScreenOffBroadcastAndRegister();

                    //OREO and above can get delayed state when request audio focus
                    if ( android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)
                        mRequestAudioFocusHandler.postDelayed(tryRequestAudioFocusRunnable,3*1000);
                }

                if ( telephonyManagerState.equals(TelephonyManager.EXTRA_STATE_RINGING)
                        || telephonyManagerState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK) ) {
                    try {
                        if (mScreenOffBroadcastReceiver != null)
                            unregisterReceiver(mScreenOffBroadcastReceiver);
                    } catch (IllegalArgumentException e) {
                        // when alarm fired and incoming call is ringing
                        // screenOffBroadcast not registered
                        // then when switched to offHook when answering
                        // it try to unregister it.
                        // so we surround 'unregister' with try-catch
                        // and do nothing in catch , because it is unregistered any way.
                    }
                }

            }
        };

        registerReceiver(mCallBroadReceiver, new IntentFilter("android.intent.action.PHONE_STATE") );

    }

    private void assignScreenOffBroadcastAndRegister() {

        Runnable startRingingActivityRunnable = new Runnable() {
            @Override
            public void run() {
                Intent ringingActivityIntent = new Intent(getApplicationContext(), RingingActivity.class);
                ringingActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                ringingActivityIntent.putExtra(RingingService.INTENT_EXTRA_ALARM, mAlarm);
                startActivity(ringingActivityIntent);
            }
        };
        mScreenOffBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                startVibration();
                mScreenOnHandler.postDelayed(startRingingActivityRunnable, 5*1000);
            }
        };

        registerReceiver(mScreenOffBroadcastReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF) );
    }

    private void assignAutoDismissCountDownTimer() {

        // reset counter for next alarm in queue
        if (mAutoDismissCountDownTimer != null)
            mAutoDismissCountDownTimer.cancel();

        boolean manageAlarmTimeout = mSharedPreferences.getBoolean(SettingsActivity.SETTINGS_KEY_AUTO_DISMISS_OR_SNOOZE,false);

        if ( !manageAlarmTimeout  )
            return;

        // SettingActivity save values as String in android M
        String alarmTimeoutInMinuteString = mSharedPreferences.getString(SettingsActivity.SETTINGS_KEY_ALARM_TIME_OUT,"5");
        int alarmTimeoutInMinute = Integer.parseInt(alarmTimeoutInMinuteString);
        String afterTimeout = mSharedPreferences.getString(SettingsActivity.SETTINGS_KEY_AFTER_ALARM_TIME_OUT, "SNOOZE");
        mAutoDismissCountDownTimer = new CountDownTimer(alarmTimeoutInMinute*60*1000, alarmTimeoutInMinute*60*1000) {
            @Override
            public void onTick(long l) { }

            @Override
            public void onFinish() {
                //Intent intent = new Intent();

                if (afterTimeout.equals("DISMISS"))
                    //intent.setAction(ACTION_DISMISS_ALARM);
                    dismissCurrentAlarm();
                else
                    //intent.setAction(ACTION_SNOOZE_ALARM);
                    snoozeCurrentAlarm();

                //intent.putExtra(RingingService.INTENT_EXTRA_ALARM,mAlarm);
                //sendBroadcast(intent);

                sendBroadcast( new Intent(RingingActivity.ACTION_CLOSE_RINGING_ACTIVITY) );
            }
        };
        mAutoDismissCountDownTimer.start();
    }

    public void flashLightIfRequired() {

        if (mFlashLightCountDownTimer != null) {
            mFlashLightCountDownTimer.cancel();
            try {
                if (mCameraManager != null)
                    mCameraManager.setTorchMode(mCameraID, false);
            } catch (CameraAccessException ignored) {
            }
        }

        if ( ! mAlarm.isFlash_light() )
            return;

        boolean hasCameraPermission = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if ( ! hasCameraPermission)
            return;

        mCameraManager = getSystemService(CameraManager.class);
        try {
            mCameraID = mCameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            return;
        }

        // SettingActivity save values as String in android M
        String flashLightTimeoutString = mSharedPreferences.getString(SettingsActivity.SETTINGS_KEY_FLASH_LIGHT_TIMEOUT,"30");
        int flashLightTimeout = Integer.parseInt(flashLightTimeoutString);
        mFlashLightCountDownTimer = new CountDownTimer(flashLightTimeout*1000, 800) {
            @Override
            public void onTick(long l) {
                mIsFlashOn = !mIsFlashOn;
                try {
                    mCameraManager.setTorchMode(mCameraID, mIsFlashOn);
                } catch (CameraAccessException e) {
                    mFlashLightCountDownTimer.cancel();
                }
            }

            @Override
            public void onFinish() {
                try {
                    mCameraManager.setTorchMode(mCameraID, false);
                } catch (CameraAccessException ignored) {

                }
                mCameraManager = null;
            }
        };
        mFlashLightCountDownTimer.start();

    }


    @Override
    public void onDestroy() {
        cleanMeUp();
        super.onDestroy();
    }

    private void cleanMeUp() {

        IS_RUNNING = false;

        if (mMediaPlayer !=null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        stopVibration();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest);
        else
            mAudioManager.abandonAudioFocus(this);

        if (mAutoDismissCountDownTimer != null) {
            mAutoDismissCountDownTimer.cancel();
            mAutoDismissCountDownTimer = null;
        }

        if (mFlashLightCountDownTimer != null) {
            mFlashLightCountDownTimer.cancel();
            try {
                if (mCameraManager != null)
                    mCameraManager.setTorchMode(mCameraID, false);
            } catch (CameraAccessException ignored) {

            }
            mFlashLightCountDownTimer = null;
        }

        if (mCallBroadReceiver != null)
            unregisterReceiver(mCallBroadReceiver);
        if (mDismissOrSnoozeAlarmReceiver != null)
            unregisterReceiver(mDismissOrSnoozeAlarmReceiver);
        if (mScreenOffBroadcastReceiver != null)
            unregisterReceiver(mScreenOffBroadcastReceiver);

        if (mVolumeUpHandler != null)
            mVolumeUpHandler.removeCallbacksAndMessages(null);

        soundPool.release();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {

        switch (focusChange) {

            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mMediaPlayer != null && mMediaPlayer.isPlaying())
                    mMediaPlayer.pause();
                stopVibration();

                break;

            case AudioManager.AUDIOFOCUS_GAIN:
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE:
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                //MediaPlayer.getDuration = -1 if 'NOT Prepared'
                if (mMediaPlayer !=null && mMediaPlayer.getDuration() > 0 )
                    mMediaPlayer.start();
                startVibration();

                break;
        }
    }

    private void runNextAlarmOrStopService() {

        if (mAlarmsQueue.size() > 0) {
            playFizoVoice();

            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mMediaPlayer.reset();
            }

            mAlarm = mAlarmsQueue.get(0);
            mAlarmsQueue.remove(0);
            processAlarm();
        } else
            stopSelf();

    }

    private void playFizoVoice() {
        if ( ! mAlarm.getRingtoneData().getUri().equals(Uri.EMPTY) )
            soundPool.play(soundPoolFizoId, 1, 1, 1, 0, 1);
    }

    public void dismissCurrentAlarm() {

        if (mAlarm == null)
            return;

        if (mAlarm.getRepeat_days().size() == 0) {
            mAlarm.setEnabled(false);
            mAlarm.setSnooze_repeat_count(0);
            AlarmsListStorage.getInstance(this).editAlarm(mAlarm);

            // to dismiss it on UI
            Intent dismissIntent = new Intent(AlarmsListFragment.ACTION_ALARM_DISMISSED);
            dismissIntent.putExtra(AlarmsListFragment.EXTRA_DISMISSED_ALARM_ID, mAlarm.getId());
            sendBroadcast(dismissIntent);
        } else {
            mAlarm.setSnooze_repeat_count(0);
            AlarmsListStorage.getInstance(this).editAlarm(mAlarm);
        }

        runNextAlarmOrStopService();

    }

    public void snoozeCurrentAlarm() {

        if (mAlarm == null)
            return;

        AlarmManagerBusiness.snoozeAlarm(this, mAlarm);

        //only in challenge alarm there is max allowed snooze count
        if ( mAlarm.getChallenge() != null)
            mAlarm.setSnooze_repeat_count( mAlarm.getSnooze_repeat_count()+1 );

        AlarmsListStorage.getInstance(this).editAlarm(mAlarm);

        runNextAlarmOrStopService();
    }

    public class MyBinder extends Binder {

        /*
        public void runNextOrStop() {
            RingingService.this.runNextAlarmOrStopService();
        }
        */

        public void dismissCurrentAlarm() {
            RingingService.this.dismissCurrentAlarm();
        }

        public void snoozeCurrentAlarm() {
            RingingService.this.snoozeCurrentAlarm();
        }

    }

    private void setLastSelectedRingtoneToDefault() {
        Uri defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        String defaultRingtoneTitle = RingtoneManager.getRingtone(this, defaultRingtoneUri).getTitle(this);
        RingtoneData defaultRingtoneData = new RingtoneData(defaultRingtoneUri, defaultRingtoneTitle, 0);
        Gson gson = new Gson();
        mSharedPreferences.edit()
                .putString(SettingsActivity.SETTINGS_KEY_LAST_RINGTONE_DATA,gson.toJson(defaultRingtoneData))
                .apply();
    }

}