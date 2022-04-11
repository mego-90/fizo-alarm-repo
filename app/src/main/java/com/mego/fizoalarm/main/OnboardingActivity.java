package com.mego.fizoalarm.main;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.mego.fizoalarm.R;
import com.mego.fizoalarm.databinding.ActivityOnboardingBinding;
import com.mego.fizoalarm.pojo.Onboarding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class OnboardingActivity extends AppCompatActivity {

    private ActivityOnboardingBinding binding;

    private ViewPager2.OnPageChangeCallback mViewPagerCallback;
    private List<Onboarding> mOnboardingItems = new ArrayList<>();

    private ActivityResultLauncher<String> mRequestPhonePerm;

    public OnboardingActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initOnboardingItems();

        binding.onboardingLowerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isLastPage = binding.onboardingViewPager.getCurrentItem() == mOnboardingItems.size()-1;
                if (isLastPage) {
                    markOnboardingAsShown();
                    finish();
                } else {
                    binding.onboardingViewPager.setCurrentItem( binding.onboardingViewPager.getCurrentItem()+1 );
                }

            }
        });

        binding.onboardingViewPager.setAdapter( new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return OnboardingFragment.newInstance(mOnboardingItems.get(position));
            }

            @Override
            public int getItemCount() {
                return mOnboardingItems.size();
            }
        });


        mViewPagerCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position != mOnboardingItems.size()-1)
                    binding.onboardingLowerBtn.setText(R.string.next);
                else
                    binding.onboardingLowerBtn.setText(R.string.onboarding_got_it);
            }
        };

        binding.onboardingViewPager.registerOnPageChangeCallback(mViewPagerCallback);

        mRequestPhonePerm = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted)
                        binding.onboardingViewPager.setCurrentItem( binding.onboardingViewPager.getCurrentItem()+1 );
                    else
                        Toast.makeText(this, R.string.permission_phone_state_needed_description, Toast.LENGTH_LONG).show();;
                });

    }

    private void markOnboardingAsShown() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(OnboardingActivity.this);
        sharedPreferences.edit()
                .putBoolean(SettingsActivity.SETTINGS_KEY_ONBOARDING_DONE, true)
                .apply();
    }

    private void initOnboardingItems() {
        mOnboardingItems.add(new Onboarding(R.drawable.ic_baseline_alarm_24, R.string.onboarding_title_1,R.string.onboarding_description_1));

        mOnboardingItems.add(new Onboarding(R.drawable.ic_baseline_call_24, R.string.permission_phone_state_needed, R.string.permission_phone_state_needed_description, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                checkAndRequestPhonePermission();
                return null;
            }
        }));

        mOnboardingItems.add(new Onboarding(R.drawable.ic_baseline_flip_to_front_24, R.string.permission_alert_window_needed, R.string.permission_alert_window_needed_description, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                checkAndRequestOverlayPermission();
                return null;
            }
        }));

        mOnboardingItems.add(new Onboarding(R.drawable.ic_baseline_check_circle_24, R.string.ready, R.string.onboarding_lets_go));
    }


    private void checkAndRequestPhonePermission() {
        int readPhoneStatePermissionRequest = checkSelfPermission( Manifest.permission.READ_PHONE_STATE );

        if (readPhoneStatePermissionRequest != PackageManager.PERMISSION_GRANTED)
            //requestPermissions(new String[]{ Manifest.permission.READ_PHONE_STATE },0);
            mRequestPhonePerm.launch(Manifest.permission.READ_PHONE_STATE);
        else
            new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_granted)
                    //.setMessage(R.string.permission_phone_state_need_description)
                    .setPositiveButton(android.R.string.ok, null)
                    .create()
                    .show();
    }

    private void checkAndRequestOverlayPermission() {

        if (!Settings.canDrawOverlays(this)) {
            Intent requestOverlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivity(requestOverlayIntent);
        } else
            new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_granted)
                    .setPositiveButton(android.R.string.ok, null)
                    .create()
                    .show();
    }

    @Override
    protected void onDestroy() {
        binding.onboardingViewPager.unregisterOnPageChangeCallback(mViewPagerCallback);
        super.onDestroy();
    }

}