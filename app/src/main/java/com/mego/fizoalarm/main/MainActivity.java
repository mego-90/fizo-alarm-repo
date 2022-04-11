package com.mego.fizoalarm.main;

import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.mego.fizoalarm.BuildConfig;
import com.mego.fizoalarm.R;
import com.mego.fizoalarm.pojo.RemoteConfigKeys;
import com.mego.fizoalarm.receivers.AlarmDeviceAdmin;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


public class MainActivity extends AppCompatActivity {

    public static int REQUEST_NEW_ALARM_CODE = 100 ;
    public static int REQUEST_READ_PHONE_STATE_PERMISSION = 200 ;

    public static final int app_version_code = BuildConfig.VERSION_CODE ;

    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private RewardedAd mMandatoryRewardedAd;
    private RewardedAd mOptionalRewardedAd;
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;
    private boolean mShowOptionalRewardedAd = false;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d-MM-yyyy");
    private SharedPreferences mSettingsPreference;

    private ActivityResultLauncher<Intent> overlayPermissionActivityResultLauncher;

    private ConsentInformation consentInformation;
    private ConsentForm consentForm;

    private static DevicePolicyManager devicePolicyManager;
    private static ComponentName alarmDeviceAdminComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //We .show() only in AlarmListFragment onStart()
        getSupportActionBar().hide();

        initConsentRequestAndAd();

        devicePolicyManager = getSystemService(DevicePolicyManager.class);
        alarmDeviceAdminComponent = new ComponentName(this, AlarmDeviceAdmin.class);

        mSettingsPreference = PreferenceManager.getDefaultSharedPreferences(this);

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        /*
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);

         */
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        mFirebaseRemoteConfig.fetchAndActivate();

        initOptionalRewardedAd();
        //if ( isRewardVideoDateExpired() )
        //    initMandatoryRewardedAd();

        initInterstitialAd();
        initBannerAd();


        //NavController navController = Navigation.findNavController(MainActivity.this,R.id.nav_host_fragment);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration
                .Builder(navController.getGraph()).build();
        NavigationUI.setupWithNavController(toolbar,navController,appBarConfiguration);

        showMustUpdateAlertDialogIfNeeded();


        boolean onboardingDone = mSettingsPreference.getBoolean(SettingsActivity.SETTINGS_KEY_ONBOARDING_DONE, false);

        if ( !onboardingDone )
            startActivity(new Intent(this, OnboardingActivity.class));


        /*
        overlayPermissionActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if ( result.getResultCode() != Activity.RESULT_OK ) {

                            if (mSettingsPreference != null) {
                                mSettingsPreference.edit()
                                        .putBoolean(SettingsActivity.SETTINGS_KEY_ASKED_ALERT_WINDOW_PERMISSION, true)
                                        .apply();
                            }

                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle(R.string.permission_not_granted_title)
                                    .setMessage(R.string.permission_alert_window_need_description)
                                    .setNegativeButton(R.string.deny, null)
                                    .setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            checkAndRequestAlertWindowPermissions();
                                        }
                                    })
                                    .create()
                                    .show();
                        }
                    }
                }
        );
        */

        /*
        boolean askedForReadPhoneStatePermission
                = mSettingsPreference.getBoolean(SettingsActivity.SETTINGS_KEY_ASKED_PHONE_STATE_PERMISSION, false);
        if (! askedForReadPhoneStatePermission)
            checkAndRequestReadPhoneStatePermissions();

        boolean askedForAlertWindowPermission
                = mSettingsPreference.getBoolean(SettingsActivity.SETTINGS_KEY_ASKED_ALERT_WINDOW_PERMISSION, false);
        if ( !askedForAlertWindowPermission )
            checkAndRequestAlertWindowPermissions();
        */
    }

    private void initMandatoryRewardedAd() {

        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(this, getString(R.string.admob_mandatory_rewarded_ad_unit_id), adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mMandatoryRewardedAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        mMandatoryRewardedAd = rewardedAd;

                        mMandatoryRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdShowedFullScreenContent() {
                                // Called when ad is shown.

                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                // Called when ad fails to show.

                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                // Called when ad is dismissed.
                                // Set the ad reference to null so you don't show the ad a second time.
                                mMandatoryRewardedAd = null;
                                initMandatoryRewardedAd();
                            }
                        });


                    }
                });
    }

    private void initOptionalRewardedAd() {

        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(this, getString(R.string.admob_optional_rewarded_ad_unit_id), adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mOptionalRewardedAd = null;
                        mShowOptionalRewardedAd = false;
                        supportInvalidateOptionsMenu();
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        mOptionalRewardedAd = rewardedAd;
                        mShowOptionalRewardedAd = true;
                        supportInvalidateOptionsMenu();

                        mOptionalRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdShowedFullScreenContent() {
                                // Called when ad is shown.

                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                // Called when ad fails to show.

                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                // Called when ad is dismissed.
                                // Set the ad reference to null so you don't show the ad a second time.
                                mOptionalRewardedAd = null;
                                mShowOptionalRewardedAd = false;
                                supportInvalidateOptionsMenu();
                                initOptionalRewardedAd();
                            }
                        });


                    }
                });
    }

    private void initBannerAd() {
        mAdView = new AdView(this);

        // Set the adaptive ad size on the ad view.
        mAdView.setAdSize( getBannerAdAdaptiveSize() );
        mAdView.setAdUnitId( getString(R.string.admob_main_banner_ad_unit_id) );

        ConstraintLayout constraintLayout = findViewById(R.id.banner_ad_container);
        constraintLayout.addView(mAdView);

        AdRequest adRequest = new AdRequest.Builder().build();

        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                constraintLayout.setVisibility( View.VISIBLE );
            }
        });


        mAdView.loadAd(adRequest);
    }

    private void initInterstitialAd() {

        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this, getString(R.string.admob_interstitial_ad_unit_id), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;

                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                // Called when fullscreen content is dismissed.
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                // Called when fullscreen content failed to show.
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                // Called when fullscreen content is shown.
                                // Make sure to set your reference to null so you don't
                                // show it a second time.
                                mInterstitialAd = null;
                            }
                        });

                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        mInterstitialAd = null;
                    }
                });


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

    public boolean canNavigateIfNotShowAd() {

        if ( mMandatoryRewardedAd != null && isRewardVideoDateExpired() ) {

            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.reward_dialog_title)
                    .setMessage( getString(R.string.reward_dialog_message) )
                    .setPositiveButton(R.string.reward_dialog_positive_btn, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mMandatoryRewardedAd.show(MainActivity.this, new OnUserEarnedRewardListener() {
                                @Override
                                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                                    String newExpirationDateString =
                                            dateFormatter.format(LocalDate.now().plusDays(mFirebaseRemoteConfig.getLong(RemoteConfigKeys.KEY_REWARDED_VIDEO_EXPIRATION_IN_DAYS)) );
                                    mSettingsPreference.edit()
                                            .putString(SettingsActivity.SETTINGS_KEY_REWARDED_AD_EXPIRATION_DATE, newExpirationDateString)
                                            .apply();

                                    mMandatoryRewardedAd = null;
                                    mInterstitialAd = null;
                                }
                            });
                        }
                    })
                    .setNegativeButton(R.string.reward_dialog_negative_btn, null)
                    .create()
                    .show();

            return false;


        } else if (mInterstitialAd != null)
            mInterstitialAd.show(this);

        return true;
    }

    private void showOptionalRewardedAd() {

        if ( mOptionalRewardedAd != null ) {

            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.reward_dialog_title)
                    .setMessage( R.string.reward_dialog_optionally_opened_message)
                    .setPositiveButton(R.string.reward_dialog_positive_btn, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mOptionalRewardedAd.show(MainActivity.this, new OnUserEarnedRewardListener() {
                                @Override
                                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                                    String newExpirationDateString =
                                            dateFormatter.format(LocalDate.now().plusDays(mFirebaseRemoteConfig.getLong(RemoteConfigKeys.KEY_REWARDED_VIDEO_EXPIRATION_IN_DAYS)));
                                    mSettingsPreference.edit()
                                            .putString(SettingsActivity.SETTINGS_KEY_REWARDED_AD_EXPIRATION_DATE, newExpirationDateString)
                                            .apply();

                                    mOptionalRewardedAd = null;
                                    mInterstitialAd = null;
                                }
                            });
                        }
                    })
                    .setNegativeButton(R.string.reward_dialog_negative_btn, null)
                    .create()
                    .show();


        }
    }

    private boolean isRewardVideoDateExpired() {

        String rewardAdExpirationString = mSettingsPreference.getString(SettingsActivity.SETTINGS_KEY_REWARDED_AD_EXPIRATION_DATE, "");
        if ( rewardAdExpirationString.isEmpty() ) {
            String newExpirationDateString =
                    dateFormatter.format(LocalDate.now().plusDays(1) );
            mSettingsPreference.edit()
                    .putString(SettingsActivity.SETTINGS_KEY_REWARDED_AD_EXPIRATION_DATE, newExpirationDateString)
                    .apply();

            return false;
        }

        LocalDate expirationDate = LocalDate.parse(rewardAdExpirationString, dateFormatter);
        if ( expirationDate.isAfter(LocalDate.now().plusDays( mFirebaseRemoteConfig.getLong(RemoteConfigKeys.KEY_REWARDED_VIDEO_EXPIRATION_IN_DAYS))) )
            return true;

        return expirationDate.isBefore(LocalDate.now());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem showRewardVideoItem = menu.findItem(R.id.action_show_optional_reward_video);
        showRewardVideoItem.setVisible(mShowOptionalRewardedAd);
        return true;
        //return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {

            if ( RingingService.IS_RUNNING )
                Toast.makeText(this, R.string.dismiss_alarm_before_open_settings, Toast.LENGTH_LONG).show();
            else
                startActivity(new Intent(this,SettingsActivity.class));

            return true;
        } else if (id == R.id.action_about) {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            NavController navController = navHostFragment.getNavController();
            navController.navigate(R.id.aboutFragment);
            return true;
        } else if (id == R.id.action_show_optional_reward_video ) {
            showOptionalRewardedAd();
            return true;
        } else if (id== R.id.action_show_digital_clock) {
            lockIfIsDeviceAdminAndStartNightClock();
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    private void showMustUpdateAlertDialogIfNeeded() {

        int lastStableVersionCode = (int)mFirebaseRemoteConfig.getLong( RemoteConfigKeys.KEY_LAST_STABLE_VERSION_CODE ) ;

        if ( app_version_code < lastStableVersionCode ) {
            AlertDialog alertDialog = new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.new_version_available)
                    .setMessage(R.string.new_version_message)
                    .setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            final String appPackageName = getPackageName();
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                            } catch (ActivityNotFoundException ex) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                            }
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.no_thanks, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .create();

            alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    finish();
                }
            });

            alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    finish();
                }
            });

            alertDialog.show();
        }
    }

    private void initConsentRequestAndAd() {

        ConsentRequestParameters params = new ConsentRequestParameters
                .Builder()
                .setTagForUnderAgeOfConsent(false)
                .build();

        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        consentInformation.requestConsentInfoUpdate(
                this,
                params,
                new ConsentInformation.OnConsentInfoUpdateSuccessListener() {
                    @Override
                    public void onConsentInfoUpdateSuccess() {
                        // The consent information state was updated.
                        // You are now ready to check if a form is available.
                        if (consentInformation.getConsentStatus() != ConsentInformation.ConsentStatus.REQUIRED)
                            initAdMob();
                        else if (consentInformation.isConsentFormAvailable()) {
                            loadConsentForm();
                        }

                    }
                },
                new ConsentInformation.OnConsentInfoUpdateFailureListener() {
                    @Override
                    public void onConsentInfoUpdateFailure(FormError formError) {
                        // Handle the error.
                        if (consentInformation.getConsentStatus() != ConsentInformation.ConsentStatus.REQUIRED)
                            initAdMob();
                        FirebaseCrashlytics.getInstance().recordException(new IllegalStateException(formError.getMessage()));
                    }
                });
    }

    public void loadConsentForm() {

        UserMessagingPlatform.loadConsentForm(
                this,
                new UserMessagingPlatform.OnConsentFormLoadSuccessListener() {
                    @Override
                    public void onConsentFormLoadSuccess(ConsentForm consentForm) {
                        MainActivity.this.consentForm = consentForm;
                        if(consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
                            consentForm.show(
                                    MainActivity.this,
                                    new ConsentForm.OnConsentFormDismissedListener() {
                                        @Override
                                        public void onConsentFormDismissed(@Nullable FormError formError) {
                                            // Handle dismissal by reloading form.
                                            loadConsentForm();
                                        }
                                    });

                        }

                    }
                },
                new UserMessagingPlatform.OnConsentFormLoadFailureListener() {
                    @Override
                    public void onConsentFormLoadFailure(FormError formError) {
                        /// Handle Error.
                        FirebaseCrashlytics.getInstance().recordException(new IllegalStateException(formError.getMessage()));
                    }
                }
        );

    }

    private void initAdMob() {
        RequestConfiguration requestConfiguration =
                new RequestConfiguration
                        .Builder()
                        .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                        .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                        .build();

        MobileAds.setRequestConfiguration(requestConfiguration);

        MobileAds.initialize( MainActivity.this );
    }
    private void lockIfIsDeviceAdminAndStartNightClock() {
        PowerManager pm = getSystemService(PowerManager.class);
        if ( pm.isInteractive() ) {
            DevicePolicyManager policy = getSystemService(DevicePolicyManager.class);
            try {
                policy.lockNow();

                Intent intent = new Intent(this, NightClockActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

            } catch (SecurityException ex) {
                showRequestAddToDeviceAdminDialog();
            }
        }
    }

    private void showRequestAddToDeviceAdminDialog() {

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.device_admin_dialog_title)
                .setMessage( getString(R.string.device_admin_from_night_clock) )
                .setPositiveButton(R.string.device_admin_dialog_title, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, alarmDeviceAdminComponent);//ComponentName of the administrator component.

                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.no_thanks, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(MainActivity.this, NightClockActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                })
                .create()
                .show();

    }



    /*
    private  void checkAndRequestReadPhoneStatePermissions() {
        int readPhoneStatePermissionRequest = checkSelfPermission(Manifest.permission.READ_PHONE_STATE);

        if (readPhoneStatePermissionRequest != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE_PERMISSION);
    }

    private  void checkAndRequestAlertWindowPermissions() {

        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));

            overlayPermissionActivityResultLauncher.launch(intent);
        }


    }
    */

    /*
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_READ_PHONE_STATE_PERMISSION) {

            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {

                if (mSettingsPreference != null) {
                    mSettingsPreference.edit()
                            .putBoolean(SettingsActivity.SETTINGS_KEY_ASKED_PHONE_STATE_PERMISSION, true)
                            .apply();
                }

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.permission_not_granted_title)
                        .setMessage(R.string.permission_phone_state_need_description)
                        .setNegativeButton(R.string.deny, null)
                        .setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE_PERMISSION);

                            }
                        })
                        .create()
                        .show();


            }
        }
    }
    */
}