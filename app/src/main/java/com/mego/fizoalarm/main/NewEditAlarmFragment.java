package com.mego.fizoalarm.main;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.PopupMenu;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.ConfigurationCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.mego.fizoalarm.R;
import com.mego.fizoalarm.databinding.FragmentNewEditAlarmBinding;
import com.mego.fizoalarm.pojo.Alarm;
import com.mego.fizoalarm.pojo.RingtoneData;
import com.mego.fizoalarm.pojo.challenges.Challenge;
import com.mego.fizoalarm.storage.AlarmsListStorage;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.Locale;


public class NewEditAlarmFragment extends Fragment {

    private final String DATE_PATTERN_WITH_YEAR = "E, MMM d, yyyy";
    private final String DATE_PATTERN_WITHOUT_YEAR = "E, MMM d";

    public static final String REQUEST_NEW_ALARM = "com.mego.hellalarm.newEditAlarmFragment.request_new_alarm";
    public static final String REQUEST_EDIT_ALARM = "com.mego.hellalarm.newEditAlarmFragment.request_edit_alarm";
    public static final String EXTRA_RETURNED_ALARM = "com.mego.hellalarm.newEditAlarmFragment.extra_returnedAlarm";

    public static final String RESULT_UPDATE_CHALLENGE_CONFIG = "com.mego.hellalarm.newEditAlarmFragment.request_update_challenge_config";
    public static final String EXTRA_CHALLENGE_CONFIG = "com.mego.hellalarm.newEditAlarmFragment.extra_challenge_config";

    FragmentNewEditAlarmBinding binding;

    private DateTimeFormatter mDateFormatter ;
    private boolean mUse24Format;
    private boolean isCreatingNewAlarm;

    private SharedPreferences mSettingsPreference ;

    private DayOfWeek mFirstDayOfWeek;
    private Locale mCurrentLocale;
    private Alarm mAlarm;
    private boolean selectedSpecificDate = false;

    //remove work on json here ,make them in class
    private Gson gson = new Gson();

    private ActivityResultLauncher<String> cameraPermActivityResultLauncher;
    boolean mHasCameraPermission;

    public NewEditAlarmFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAlarm = NewEditAlarmFragmentArgs.fromBundle(getArguments()).getArgAlarmToEdit();
        if (mAlarm == null) {
            isCreatingNewAlarm = true ;
            // if after 22 set alarm for next day at 6 o'clock
            LocalDate dayOfNewAlarm;
            LocalDateTime now = LocalDateTime.now();
            if ( now.toLocalTime().isAfter( LocalTime.of(22,0)) ) {
                dayOfNewAlarm = now.toLocalDate().plusDays(1);
                mAlarm = new Alarm(dayOfNewAlarm, LocalTime.of(6,0), getLastOrDefaultRingtoneData());
            } else {
                dayOfNewAlarm = now.toLocalDate();
                mAlarm = new Alarm(dayOfNewAlarm, now.toLocalTime().plusMinutes(5), getLastOrDefaultRingtoneData());
            }
        }

        addFragmentResultListener();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNewEditAlarmBinding.inflate(inflater, container, false);

        String savedFirstDayOfWeek = mSettingsPreference.getString(SettingsActivity.SETTINGS_KEY_FIRST_DAY_IN_WEEK,"SUNDAY");
        mFirstDayOfWeek = DayOfWeek.valueOf(savedFirstDayOfWeek);

        mCurrentLocale = ConfigurationCompat.getLocales(getResources().getConfiguration()).get(0);

        mUse24Format = DateFormat.is24HourFormat(getContext());

        mDateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN_WITHOUT_YEAR);

        binding.ringtoneTitleTextView.setText(mAlarm.getRingtoneData().getTitle());

        if (mAlarm.getChallenge() != null)
            binding.challengeNameTextView.setText(mAlarm.getChallenge().getStringResourceID());

        if (mAlarm.getChallenge() != null) {
            binding.challengeDetailsTextView.setVisibility(View.VISIBLE);
            binding.challengeDetailsTextView.setText(mAlarm.getChallenge().generateDetailsText(getContext()));
        } else
            binding.challengeDetailsTextView.setVisibility(View.INVISIBLE);

        processDateDetailsTextView();

        //Cancel Button
        binding.cancelNewAlarmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(NewEditAlarmFragment.this)
                        .navigateUp();
            }
        });

        //Save Button
        binding.saveNewAlarmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if ( mAlarm.getRepeat_days().size()==0 && LocalDateTime.of(mAlarm.getDate(),mAlarm.getTime()).isBefore(LocalDateTime.now()) ) {
                    //Toast.makeText(requireActivity(), R.string.can_not_set_alarm_for_past, Toast.LENGTH_LONG).show();
                    new MaterialAlertDialogBuilder(requireActivity())
                            .setTitle(R.string.past_time)
                            .setMessage(R.string.can_not_set_alarm_for_past)
                            .setPositiveButton(android.R.string.ok, null)
                            .create()
                            .show();
                    return;
                }
                mAlarm.setEnabled(true);
                Bundle bundle = new Bundle();
                bundle.putSerializable(EXTRA_RETURNED_ALARM,mAlarm);
                if (isCreatingNewAlarm) {
                    AlarmsListStorage.getInstance(getContext()).saveAlarm(mAlarm);
                    getParentFragmentManager().setFragmentResult(REQUEST_NEW_ALARM,bundle);
                    AlarmManagerBusiness.scheduleAlarm(getContext(), mAlarm, true);
                } else {
                    AlarmsListStorage.getInstance(getContext()).editAlarm(mAlarm);
                    getParentFragmentManager().setFragmentResult(REQUEST_EDIT_ALARM,bundle);
                    AlarmManagerBusiness.editAlarm(getContext(),mAlarm);
                }
                NavHostFragment.findNavController(NewEditAlarmFragment.this)
                        .navigateUp();
            }
        });

        //Snooze Btn
        binding.changeSnoozeBtn.setText(getString(R.string.snooze_time_on_btn,mAlarm.getSnooze_in_minutes()));
        binding.changeSnoozeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(getActivity(),binding.changeSnoozeBtn);
                popupMenu.inflate(R.menu.menu_snooze_time);
                popupMenu.setGravity(Gravity.CENTER);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        int selectedItemID = menuItem.getItemId();
                        //populate all case independently for localization
                        switch (selectedItemID) {
                            case R.id.menu_item_snooze_off:
                                mAlarm.setSnooze(false);
                                mAlarm.setSnooze_in_minutes(0);
                                binding.changeSnoozeBtn.setText(getString(R.string.off));
                                return true;
                            case R.id.menu_item_snooze_3:
                                mAlarm.setSnooze(true);
                                mAlarm.setSnooze_in_minutes(3);
                                binding.changeSnoozeBtn.setText(getString(R.string.snooze_time_on_btn,3));
                                return true;
                            case R.id.menu_item_snooze_5:
                                mAlarm.setSnooze(true);
                                mAlarm.setSnooze_in_minutes(5);
                                binding.changeSnoozeBtn.setText(getString(R.string.snooze_time_on_btn,5));
                                return true;
                            case R.id.menu_item_snooze_8:
                                mAlarm.setSnooze(true);
                                mAlarm.setSnooze_in_minutes(8);
                                binding.changeSnoozeBtn.setText(getString(R.string.snooze_time_on_btn,8));
                                return true;
                            case R.id.menu_item_snooze_10:
                                mAlarm.setSnooze(true);
                                mAlarm.setSnooze_in_minutes(10);
                                binding.changeSnoozeBtn.setText(getString(R.string.snooze_time_on_btn,10));
                                return true;
                            case R.id.menu_item_snooze_15:
                                mAlarm.setSnooze(true);
                                mAlarm.setSnooze_in_minutes(15);
                                binding.changeSnoozeBtn.setText(getString(R.string.snooze_time_on_btn,15));
                                return true;
                            case R.id.menu_item_snooze_20:
                                mAlarm.setSnooze(true);
                                mAlarm.setSnooze_in_minutes(20);
                                binding.changeSnoozeBtn.setText(getString(R.string.snooze_time_on_btn,20));
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                popupMenu.show();
            }
        });

        binding.newAlarmVibrationSwitch.setChecked(mAlarm.isVibration());
        binding.newAlarmVibrationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mAlarm.setVibration(b);
                if (b)
                    ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE))
                            .vibrate(100);
            }
        });

        binding.alarmTimePicker.setIs24HourView(mUse24Format);
        binding.alarmTimePicker.setHour(mAlarm.getTime().getHour());
        binding.alarmTimePicker.setMinute(mAlarm.getTime().getMinute());
        binding.alarmTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker timePicker, int hour, int minute) {
                mAlarm.setTime(LocalTime.of(hour, minute));
            }
        });


        mHasCameraPermission = getActivity().checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        cameraPermActivityResultLauncher = registerForActivityResult( new ActivityResultContracts.RequestPermission(), isGranted -> {
            if ( isGranted) {
                mHasCameraPermission = true;
                binding.newAlarmFlashlightSwitch.setChecked(true);
            } else {
                Toast.makeText(getActivity(), R.string.camera_permission_for_flash_not_granted, Toast.LENGTH_LONG).show();
            }
        });

        if (mHasCameraPermission)
            binding.newAlarmFlashlightSwitch.setChecked(mAlarm.isFlash_light());
        else
            binding.newAlarmFlashlightSwitch.setChecked(false);
        binding.newAlarmFlashlightSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (mHasCameraPermission)
                    mAlarm.setFlash_light(b);
                else {
                    compoundButton.setChecked(false);
                    cameraPermActivityResultLauncher.launch(Manifest.permission.CAMERA);
                }
            }
        });


        binding.newAlarmLabelEditText.setText(mAlarm.getLabel());
        binding.newAlarmLabelEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                mAlarm.setLabel( editable.toString() );
            }
        });

        updateChipsLabels();
        for (int i = 0; i < binding.includedDaysChips.daysChipGroup.getChildCount(); i++) {
            Chip chip = (Chip)binding.includedDaysChips.daysChipGroup.getChildAt(i);
            chip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {

                    DayOfWeek day = (DayOfWeek) chip.getTag(R.id.dayOfWeek_in_chip);

                    if (checked && !mAlarm.getRepeat_days().contains(day)) {
                        if (selectedSpecificDate) {
                            selectedSpecificDate = false;
                            Toast.makeText(getActivity(), R.string.become_repeat_specific_alarm_no_date, Toast.LENGTH_SHORT)
                                    .show();
                        }
                        mAlarm.getRepeat_days().add(day);
                        //TODO sort algorithm according to first day of week
                        Collections.sort(mAlarm.getRepeat_days());
                    } else if (!checked) {
                        mAlarm.getRepeat_days().remove(day);
                        if (mAlarm.getRepeat_days().size() == 0)
                            mAlarm.setDate(LocalDate.now());
                    }
                    processDateDetailsTextView();
                }
            });
        }

        binding.changeDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                LocalDate initDateForDatePicker = null;
                if (mAlarm.getDate() != null)
                    initDateForDatePicker = mAlarm.getDate();

                Bundle bundle = new Bundle();
                bundle.putSerializable(DatePickerDialogFragment.ARG_INIT_DATE,initDateForDatePicker);
                bundle.putSerializable(DatePickerDialogFragment.ARG_FIRST_DAY_OF_WEEK,mFirstDayOfWeek);
                NavHostFragment.findNavController(NewEditAlarmFragment.this).navigate(R.id.datePickerDialogFragment,bundle);
            }
        });

        //Change Sound
        binding.soundLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                NewEditAlarmFragmentDirections.ActionNewAlarmToSelectRingtone action =
                        NewEditAlarmFragmentDirections.actionNewAlarmToSelectRingtone(mAlarm.getRingtoneData(),mAlarm.getVolume());
                NavHostFragment.findNavController(NewEditAlarmFragment.this)
                        .navigate(action);
            }
        });

        //Change Challenge
        binding.challengeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChooseChallengeBottomFragment chooseChallengeBottomFragment = ChooseChallengeBottomFragment.newInstance(mAlarm);
                chooseChallengeBottomFragment.show(getParentFragmentManager(), ChooseChallengeBottomFragment.TAG);
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        addObserverToChangedRingtoneAndDate(view);
    }

    private void updateChipsLabels() {

        binding.includedDaysChips.chip1stDay.setText(mFirstDayOfWeek.getDisplayName(TextStyle.SHORT,mCurrentLocale).toLowerCase() );
        binding.includedDaysChips.chip1stDay.setTag(R.id.dayOfWeek_in_chip,mFirstDayOfWeek);
        binding.includedDaysChips.chip1stDay.setClickable(true);
        binding.includedDaysChips.chip1stDay.setFocusable(false);
        if ( mAlarm.getRepeat_days().contains( (DayOfWeek)binding.includedDaysChips.chip1stDay.getTag(R.id.dayOfWeek_in_chip) ) )
            binding.includedDaysChips.chip1stDay.setChecked(true);

        binding.includedDaysChips.chip2ndDay.setText(mFirstDayOfWeek.plus(1).getDisplayName(TextStyle.SHORT,mCurrentLocale).toLowerCase() );
        binding.includedDaysChips.chip2ndDay.setTag(R.id.dayOfWeek_in_chip,mFirstDayOfWeek.plus(1));
        binding.includedDaysChips.chip2ndDay.setClickable(true);
        binding.includedDaysChips.chip2ndDay.setFocusable(false);
        if ( mAlarm.getRepeat_days().contains( (DayOfWeek)binding.includedDaysChips.chip2ndDay.getTag(R.id.dayOfWeek_in_chip) ) )
            binding.includedDaysChips.chip2ndDay.setChecked(true);

        binding.includedDaysChips.chip3rdDay.setText(mFirstDayOfWeek.plus(2).getDisplayName(TextStyle.SHORT,mCurrentLocale).toLowerCase() );
        binding.includedDaysChips.chip3rdDay.setTag(R.id.dayOfWeek_in_chip,mFirstDayOfWeek.plus(2));
        binding.includedDaysChips.chip3rdDay.setClickable(true);
        binding.includedDaysChips.chip3rdDay.setFocusable(false);
        if ( mAlarm.getRepeat_days().contains( (DayOfWeek)binding.includedDaysChips.chip3rdDay.getTag(R.id.dayOfWeek_in_chip) ) )
            binding.includedDaysChips.chip3rdDay.setChecked(true);

        binding.includedDaysChips.chip4thDay.setText(mFirstDayOfWeek.plus(3).getDisplayName(TextStyle.SHORT,mCurrentLocale).toLowerCase() );
        binding.includedDaysChips.chip4thDay.setTag(R.id.dayOfWeek_in_chip,mFirstDayOfWeek.plus(3));
        binding.includedDaysChips.chip4thDay.setClickable(true);
        binding.includedDaysChips.chip4thDay.setFocusable(false);
        if ( mAlarm.getRepeat_days().contains( (DayOfWeek)binding.includedDaysChips.chip4thDay.getTag(R.id.dayOfWeek_in_chip) ) )
            binding.includedDaysChips.chip4thDay.setChecked(true);

        binding.includedDaysChips.chip5thDay.setText(mFirstDayOfWeek.plus(4).getDisplayName(TextStyle.SHORT,mCurrentLocale).toLowerCase() );
        binding.includedDaysChips.chip5thDay.setTag(R.id.dayOfWeek_in_chip,mFirstDayOfWeek.plus(4));
        binding.includedDaysChips.chip5thDay.setClickable(true);
        binding.includedDaysChips.chip5thDay.setFocusable(false);
        if ( mAlarm.getRepeat_days().contains( (DayOfWeek)binding.includedDaysChips.chip5thDay.getTag(R.id.dayOfWeek_in_chip) ) )
            binding.includedDaysChips.chip5thDay.setChecked(true);

        binding.includedDaysChips.chip6thDay.setText(mFirstDayOfWeek.plus(5).getDisplayName(TextStyle.SHORT,mCurrentLocale).toLowerCase() );
        binding.includedDaysChips.chip6thDay.setTag(R.id.dayOfWeek_in_chip,mFirstDayOfWeek.plus(5));
        binding.includedDaysChips.chip6thDay.setClickable(true);
        binding.includedDaysChips.chip6thDay.setFocusable(false);
        if ( mAlarm.getRepeat_days().contains( (DayOfWeek)binding.includedDaysChips.chip6thDay.getTag(R.id.dayOfWeek_in_chip) ) )
            binding.includedDaysChips.chip6thDay.setChecked(true);

        binding.includedDaysChips.chip7thDay.setText(mFirstDayOfWeek.plus(6).getDisplayName(TextStyle.SHORT,mCurrentLocale).toLowerCase() );
        binding.includedDaysChips.chip7thDay.setTag(R.id.dayOfWeek_in_chip,mFirstDayOfWeek.plus(6));
        binding.includedDaysChips.chip7thDay.setClickable(true);
        binding.includedDaysChips.chip7thDay.setFocusable(false);
        if ( mAlarm.getRepeat_days().contains( (DayOfWeek)binding.includedDaysChips.chip7thDay.getTag(R.id.dayOfWeek_in_chip) ) )
            binding.includedDaysChips.chip7thDay.setChecked(true);

    }

    private void addObserverToChangedRingtoneAndDate(View rootView) {
        NavController navController = NavHostFragment.findNavController(this);
        NavBackStackEntry navBackStackEntry = navController.getBackStackEntry(R.id.newEditAlarmFragment);

        //Change Ringtone listener
        MutableLiveData<RingtoneData> ringtoneUriLiveData = navBackStackEntry.getSavedStateHandle()
                .getLiveData(SelectRingtoneFragment.EXTRA_SELECTED_RINGTONE_DATA);
        ringtoneUriLiveData.observe(getViewLifecycleOwner(), new Observer<RingtoneData>() {
            @Override
            public void onChanged(RingtoneData ringtoneData) {
                if (ringtoneData.getTypeID() != R.id.recorded_voice_ringtone_data_type && !ringtoneData.getUri().equals(Uri.EMPTY) )
                    mSettingsPreference.edit()
                        .putString(SettingsActivity.SETTINGS_KEY_LAST_RINGTONE_DATA,gson.toJson(ringtoneData))
                        .apply();
                mAlarm.setRingtoneData( ringtoneData );
                binding.ringtoneTitleTextView.setText(mAlarm.getRingtoneData().getTitle());
            }
        });

        //Change Sound Volume listener
        MutableLiveData<Float> soundVolumeLiveData = navBackStackEntry.getSavedStateHandle()
                .getLiveData(SelectRingtoneFragment.EXTRA_RINGTONE_VOLUME);
        soundVolumeLiveData.observe(getViewLifecycleOwner(), new Observer<Float>() {
            @Override
            public void onChanged(Float soundVolume) {
                mAlarm.setVolume(soundVolume);
                binding.soundValuePercent.setText( (int)(mAlarm.getVolume()*100) + "%");
            }
        });


        //Change Date using Navigation Component
        //Change Alarm Date Listener
        final LifecycleEventObserver observer = new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                if (event.equals(Lifecycle.Event.ON_RESUME)
                        && navBackStackEntry.getSavedStateHandle().contains(DatePickerDialogFragment.EXTRA_DATE)) {
                    LocalDate returnedDate = navBackStackEntry.getSavedStateHandle().get(DatePickerDialogFragment.EXTRA_DATE);
                    mAlarm.setDate(returnedDate);
                    selectedSpecificDate = true;
                    if ( mAlarm.getRepeat_days().size() > 0 ) {
                        Toast.makeText(getActivity(),R.string.become_date_specific_alarm_no_repeat,Toast.LENGTH_SHORT)
                                .show();
                        mAlarm.getRepeat_days().clear();
                    }
                    binding.includedDaysChips.daysChipGroup.clearCheck();
                    processDateDetailsTextView();
                }
            }
        };
        navBackStackEntry.getLifecycle().addObserver(observer);

        getViewLifecycleOwner().getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                if (event.equals(Lifecycle.Event.ON_DESTROY)) {
                    navBackStackEntry.getLifecycle().removeObserver(observer);
                }
            }
        });
    }

    private void processDateDetailsTextView() {

        if ( mAlarm.getRepeat_days().size() > 0 ) {
            StringBuilder repeatDaysText = new StringBuilder();
            repeatDaysText.append( getString(R.string.every) );
            for (DayOfWeek day : mAlarm.getRepeat_days())
                repeatDaysText.append(" "+ day.getDisplayName(TextStyle.SHORT,mCurrentLocale)+ "," );

            //remove last comma ,
            repeatDaysText.deleteCharAt(repeatDaysText.length()-1);

            binding.alarmDateDetailsTextView.setText( repeatDaysText.toString() );

        } else {

            LocalDate now = LocalDate.now();
            if (mAlarm.getDate().getYear() > now.getYear())
                mDateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN_WITH_YEAR);

            String prefixWord = "";
            if (mAlarm.getDate().getDayOfMonth() == now.getDayOfMonth())
                prefixWord = getString(R.string.today) + ", ";
            else if (mAlarm.getDate().getDayOfMonth() == now.plusDays(1).getDayOfMonth())
                prefixWord = getString(R.string.tomorrow) + ", ";

            binding.alarmDateDetailsTextView.setText( prefixWord + mDateFormatter.format(mAlarm.getDate()) );
        }
    }



    private RingtoneData getLastOrDefaultRingtoneData() {

        String ringtoneDataJson = mSettingsPreference.getString(SettingsActivity.SETTINGS_KEY_LAST_RINGTONE_DATA,"");
        if (ringtoneDataJson.equals("")) {
            Uri defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            String defaultRingtoneTitle = RingtoneManager.getRingtone(getActivity(), defaultRingtoneUri).getTitle(getActivity());
            return new RingtoneData(defaultRingtoneUri, defaultRingtoneTitle, 0);
        } else
            return gson.fromJson(ringtoneDataJson,RingtoneData.class);
    }

    public void addFragmentResultListener() {

        //ChooseChallengeBottomFragment
        getParentFragmentManager().setFragmentResultListener(ChooseChallengeBottomFragment.RESULT_REMOVE_CHALLENGE, this,
                (requestKey, result) -> {

                    mAlarm.setChallenge( null );
                    binding.challengeNameTextView.setText(R.string.challenge_none);
                    binding.challengeDetailsTextView.setVisibility(View.INVISIBLE);

                } );


        getParentFragmentManager().setFragmentResultListener(RESULT_UPDATE_CHALLENGE_CONFIG, this,
                (requestKey, result) -> {

                    Challenge challenge = (Challenge) result.getSerializable(EXTRA_CHALLENGE_CONFIG) ;
                    binding.challengeNameTextView.setText( challenge.getStringResourceID() );
                    mAlarm.setChallenge( challenge );
                    binding.challengeDetailsTextView.setText(challenge.generateDetailsText(getContext() ));
                    binding.challengeDetailsTextView.setVisibility(View.VISIBLE);
                });
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mSettingsPreference = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /*
    @Override
    public void onResume() {
        super.onResume();
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.hide();
    }

    @Override
    public void onStop() {
        super.onStop();
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.show();
    }
    */
    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

}