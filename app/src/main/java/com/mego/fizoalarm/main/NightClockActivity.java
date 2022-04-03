package com.mego.fizoalarm.main;

import android.app.KeyguardManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.mego.fizoalarm.R;
import com.mego.fizoalarm.databinding.ActivityNightClockBinding;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;

public class NightClockActivity extends AppCompatActivity {

    private ActivityNightClockBinding binding;



    private Disposable batteryLevelDisposable;
    private Disposable currentDateDisposable;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        binding = ActivityNightClockBinding.inflate( getLayoutInflater() );

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        setContentView( binding.getRoot() );

        binding.textClock.setTypeface( ResourcesCompat.getFont(this, R.font.libre_baskerville_bold) );



        binding.batteryLevelText.setText( getCurrentBatteryLevelPercent() + "%" );
        batteryLevelDisposable = Observable
                .interval(10, TimeUnit.MINUTES)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Throwable {
                        binding.batteryLevelText.setText( getCurrentBatteryLevelPercent() + "%" );
                    }
                           }
                );




        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E, MMM d");
        binding.currentDate.setText( formatter.format(LocalDate.now()) );
        Duration durationToMidiNight = Duration.between(LocalTime.now(),LocalTime.of(23,59,59)).plusSeconds(1);
        currentDateDisposable = Observable
                .interval(durationToMidiNight.toMillis(), TimeUnit.HOURS.toMillis(24), TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                               @Override
                               public void accept(Long aLong) throws Throwable {
                                   binding.currentDate.setText(formatter.format(LocalDate.now()));
                               }
                           }
                );

    }

    private int getCurrentBatteryLevelPercent() {

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);

        return batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    }


    @Override
    protected void onDestroy() {
        batteryLevelDisposable.dispose();
        currentDateDisposable.dispose();
        super.onDestroy();
    }
}