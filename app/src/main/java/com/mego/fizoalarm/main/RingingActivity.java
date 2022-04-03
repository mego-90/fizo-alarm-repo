package com.mego.fizoalarm.main;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.mego.fizoalarm.R;
import com.mego.fizoalarm.pojo.Alarm;
import com.mego.fizoalarm.pojo.challenges.Challenge;
import com.mego.fizoalarm.storage.AlarmsListStorage;

public class RingingActivity extends AppCompatActivity implements RingingFragment.Callbacks {

    public static final int REQUEST_CODE_REOPEN_RINGING_ACTIVITY = 11011;
    public static final String ACTION_CLOSE_RINGING_ACTIVITY = "com.mego.fizoalarm.ringingActivity.action_close_ringing_activity";

    public static final String ARG_CHALLENGE = "com.mego.fizoalarm.mathRingingFragment.arg_challenge";


    private Alarm mAlarm;
    private BroadcastReceiver mCloseActivity;
    private SharedPreferences mSharedPreferences;

    private ServiceConnection mBoundServiceConnection;
    private RingingService.MyBinder mRingingServiceBinder;
    private boolean mIsBoundToService = false;

    private boolean mIsDismissCalled = false;
    private boolean mIsSnoozeCalled = false;

    private AdView mAdView;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            this.setTurnScreenOn(true);
            this.setShowWhenLocked(true);
            KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
            keyguardManager.requestDismissKeyguard(this,null);
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
    }

    /*
    //after screen on ,not gain focus immediately
    //so onPause() and onStop() called immediately,then onResume() after hasFocus=true
    //so if (hasFocus == true ) Do
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            //Fragment mainRingingFragment = RingingFragment.newInstance(mAlarm.getLabel() );
            Fragment mathFragment = MathRingingFragment.newInstance(mAlarm.getChallengeConfig() );
            getSupportFragmentManager()
                    .beginTransaction()
                    //.add(R.id.main_ringing_fragment,mainRingingFragment)
                    .add(R.id.ringing_nav_host_fragment, mathFragment)
                    .commitAllowingStateLoss();


        }

    }
    */



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_ringing);

        initBannerAd();

        assignCloseActivityBroadcast();

        mBoundServiceConnection = getServiceConnection();

        mAlarm = (Alarm) getIntent().getSerializableExtra(RingingService.INTENT_EXTRA_ALARM);

        if (mAlarm == null) {
            if (getIntent().getData() == null) {
                finish();
                return;
            }

            String alarmID = getIntent().getData().toString();
            mAlarm = AlarmsListStorage.getInstance(this).getAlarmByID(alarmID);

            if (mAlarm == null) {
                finish();
                return;
            }
        }

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String allowedSnoozeTimes = mSharedPreferences.getString(SettingsActivity.SETTINGS_KEY_ALLOWED_SNOOZE_TIMES, "3");
        boolean snoozeAllowed = true;

        if ( !mAlarm.getSnooze() || mAlarm.getSnooze_repeat_count() >= Integer.parseInt(allowedSnoozeTimes) )
            snoozeAllowed = false;

        NavHostFragment navHostFragment = (NavHostFragment)getSupportFragmentManager().findFragmentById(R.id.ringing_nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        Bundle bundle = new Bundle();
        bundle.putString(RingingFragment.ARG_ALARM_LABEL, mAlarm.getLabel());
        bundle.putSerializable(RingingFragment.ARG_IS_CHALLENGE_ALARM, mAlarm.getChallenge()!=null );
        bundle.putBoolean(RingingFragment.ARG_SNOOZE_ALLOWED, snoozeAllowed );

        navController.setGraph(R.navigation.ringing_nav_graph, bundle);

    }

    @Override
    public void dismissAlarm() {
        /*
        Intent intent = new Intent(RingingService.ACTION_DISMISS_ALARM);
        intent.putExtra(RingingService.INTENT_EXTRA_ALARM,mAlarm);
        sendBroadcast(intent);
        finish();
        */
        mIsDismissCalled = true;
        if (mIsBoundToService) {
            mRingingServiceBinder.dismissCurrentAlarm();
            finish();

        }

    }

    @Override
    public void snoozeAlarm() {
        /*
        Intent intent = new Intent(RingingService.ACTION_SNOOZE_ALARM);
        intent.putExtra(RingingService.INTENT_EXTRA_ALARM,mAlarm);
        sendBroadcast(intent);
        finish();
        */
        mIsSnoozeCalled = true;
        if (mIsBoundToService) {
            mRingingServiceBinder.snoozeCurrentAlarm();
            finish();

        }
    }

    private void initBannerAd() {
        mAdView = new AdView(this);

        // Set the adaptive ad size on the ad view.
        mAdView.setAdSize( getBannerAdAdaptiveSize() );
        mAdView.setAdUnitId( getString(R.string.admob_ringing_banner_ad_unit_id) );

        ConstraintLayout constraintLayout = findViewById(R.id.banner_ad_container);
        constraintLayout.addView(mAdView);

        AdRequest adRequest = new AdRequest.Builder().build();

        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                constraintLayout.setVisibility( View.VISIBLE );
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                Toast.makeText(RingingActivity.this, R.string.alarm_snoozed, Toast.LENGTH_LONG).show();
                snoozeAlarm();
            }

        });

        mAdView.loadAd(adRequest);
    }

    private AdSize getBannerAdAdaptiveSize() {
        // Determine the screen width (less decorations) to use for the ad width.
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);

        // Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }

    @Override
    public void correctToDefaultChallengeAndReportProblem(Challenge challenge, RuntimeException ex) {
        mAlarm.setChallenge(challenge);
        AlarmsListStorage.getInstance(this).editAlarm(mAlarm);
        FirebaseCrashlytics.getInstance().recordException(ex);
    }

    @Override
    public void openChallenge() {

        if (mAlarm.getChallenge() == null ) {
            dismissAlarm();
            throw new IllegalStateException("OpenChallenge() called when getChallenge == null");
        }

        Fragment ringingFragment = getSupportFragmentManager().findFragmentById(R.id.ringing_nav_host_fragment);
        NavController navController = NavHostFragment.findNavController(ringingFragment);

        Bundle bundle = new Bundle();
        bundle.putSerializable(ARG_CHALLENGE, mAlarm.getChallenge());
        navController.navigate(mAlarm.getChallenge().getRingingFragmentResourceID(), bundle);

    }


    private void assignCloseActivityBroadcast() {
        mCloseActivity = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };
        getApplicationContext().registerReceiver(mCloseActivity, new IntentFilter(ACTION_CLOSE_RINGING_ACTIVITY) );
    }

    private ServiceConnection getServiceConnection() {
        ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                mRingingServiceBinder = (RingingService.MyBinder) iBinder;
                mIsBoundToService = true;
                if (mIsDismissCalled)
                    dismissAlarm();
                else if (mIsSnoozeCalled)
                    snoozeAlarm();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mIsBoundToService = false;
                mRingingServiceBinder = null;
            }
        };
        return  serviceConnection;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ( !mIsBoundToService) {
            Intent bindServiceIntent = new Intent(this, RingingService.class);
            bindService(bindServiceIntent, mBoundServiceConnection, 0);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if( mIsBoundToService ) {
            unbindService(mBoundServiceConnection);
            mIsBoundToService = false;
        }
    }

    @Override
    protected void onDestroy() {
        getApplicationContext().unregisterReceiver(mCloseActivity);
        super.onDestroy();
    }

}