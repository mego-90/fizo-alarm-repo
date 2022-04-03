package com.mego.fizoalarm.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.mego.fizoalarm.R;
import com.mego.fizoalarm.databinding.FragmentSelectRingtoneBinding;
import com.mego.fizoalarm.pojo.MyBarcode;
import com.mego.fizoalarm.pojo.RingtoneData;
import com.mego.fizoalarm.storage.BarcodeStorage;
import com.mego.fizoalarm.storage.UserRingtonesStorage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


public class SelectRingtoneFragment extends Fragment {

    public static final String EXTRA_SELECTED_RINGTONE_DATA = "com.mego.fizoalarm.selectRingtoneFragment.selectedRingtoneData";
    public static final String EXTRA_RINGTONE_VOLUME = "com.mego.fizoalarm.selectRingtoneFragment.ringtoneVolume";

    private final String VOICE_RECORDINGS_FOLDER = "voice_recordings";
    private final String PREFERENCE_RECORDINGS_FOLDER_CREATED = "com.mego.fizoalarm.main.VoiceRecorderFragment.PREFERENCE_RECORDINGS_FOLDER_CREATED";
    private final int VOICE_RECORDING_MAX_TIME_IN_SECONDS = 45 ;

    //private static final String ARG_SELECTED_RINGTONE_TYPE = "arg_selected_ringtone_type";
    //private static final String ARG_SELECTED_RINGTONE_URI =  "arg_selected_ringtone_uri";

    private static final String RES_PREFIX = "android.resource://com.mego.fizoalarm/";
    private static final int PICK_FILE_REQUEST_CODE = 100;

    private FragmentSelectRingtoneBinding binding;

    private RingtoneManager mRingtoneManager;

    private List<RingtoneData> mSystemRingtonesList;
    private List<RingtoneData> mAppRingtonesList;
    private List<RingtoneData> mUserRingtonesList;

    private RingtoneData mSelectedRingtoneData;
    //private RingtoneType mSelectedRingtoneType;

    private MediaPlayer mAudioPlayer;
    private float mSoundVolume;
    private Button addNewFileBtn;


    private MediaRecorder mMediaRecorder;
    private ToneGenerator mToneGenerator;
    private ActivityResultLauncher<String> mRequestRecordPermissionLauncher;
    private SharedPreferences mSharedPreferences;
    private boolean hasRecordVoicePermission;
    private String mVoiceRecordedFilePath = "";
    private RingtoneData mVoiceRecordedRingtoneData;
    private Animation mScaleRecordBtnAnim;

    private ActivityResultLauncher<Intent> mPickFileActivityResultLauncher;

    public SelectRingtoneFragment() {
        // Required empty public constructor
    }

    /*
    public static SelectRingtoneFragment newInstance(RingtoneType selectedRingtoneType, String selectedRingtoneURI) {
        SelectRingtoneFragment fragment = new SelectRingtoneFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SELECTED_RINGTONE_TYPE, selectedRingtoneType);
        args.putString(ARG_SELECTED_RINGTONE_URI, selectedRingtoneURI);
        fragment.setArguments(args);
        return fragment;
    }
    */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSelectedRingtoneData = SelectRingtoneFragmentArgs.fromBundle(getArguments()).getArgSelectedRingtoneData();
            mSoundVolume = SelectRingtoneFragmentArgs.fromBundle(getArguments()).getArgInitSoundVolume();
            if (mSelectedRingtoneData.getTypeID() == R.id.recorded_voice_ringtone_data_type)
                mVoiceRecordedRingtoneData = SelectRingtoneFragmentArgs.fromBundle(getArguments()).getArgSelectedRingtoneData();SelectRingtoneFragmentArgs.fromBundle(getArguments()).getArgSelectedRingtoneData();
        }

        if ( ! mSharedPreferences.getBoolean(PREFERENCE_RECORDINGS_FOLDER_CREATED,false) )
            createVoiceRecordingsFolder();

        mRequestRecordPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if ( ! isGranted ) {
                        Toast.makeText(getActivity(), R.string.permission_audio_record_needed, Toast.LENGTH_LONG).show();
                    }
                }

        );

        mToneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC,ToneGenerator.MAX_VOLUME);
    }

    // record button implements onTouch() , but not onClick() for blind people.
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentSelectRingtoneBinding.inflate(inflater, container, false);

        initPickFileActivityResultLauncher();

        binding.ringtonesRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedID) {
                if (checkedID == -1)
                    return;

                RadioButton selectedRadioButton = radioGroup.findViewById(checkedID);
                if ( ! selectedRadioButton.isChecked() )
                    return;

                binding.recordedSoundRadioBtn.setChecked(false);

                Uri uri = (Uri)selectedRadioButton.getTag(R.id.ringtone_uri);
                String title = selectedRadioButton.getText().toString();
                // before set as mSelectedRingtoneData play it, if set dataSource throw exception
                RingtoneData newSelectedRingtoneData = new RingtoneData(uri,title,binding.ringtonesTypeButtonGroup.getCheckedButtonId());
                playRingtone( newSelectedRingtoneData );
                //moved to play method
                //mSelectedRingtoneData = new RingtoneData(uri,title,ringtonesTypeButtonGroup.getCheckedButtonId());
                //NavHostFragment.findNavController(SelectRingtoneFragment.this)
                        //.getPreviousBackStackEntry().getSavedStateHandle()
                        //.set(EXTRA_SELECTED_RINGTONE_DATA,mSelectedRingtoneData);
            }
        });

        binding.volumeSlider.setValue(mSoundVolume);
        binding.volumeSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                mSoundVolume = value;
                if (mAudioPlayer == null) 
                    mAudioPlayer = new MediaPlayer();
                mAudioPlayer.setVolume(mSoundVolume, mSoundVolume);
                NavHostFragment.findNavController(SelectRingtoneFragment.this)
                        .getPreviousBackStackEntry().getSavedStateHandle()
                        .set(EXTRA_RINGTONE_VOLUME,mSoundVolume);
            }
        });

        addNewFileBtn = (Button)inflater.inflate(R.layout.add_new_music_file_btn, container, false);
        addNewFileBtn.setLayoutParams(new RadioGroup.LayoutParams(RadioGroup.LayoutParams.MATCH_PARENT,RadioGroup.LayoutParams.WRAP_CONTENT));
        addNewFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("audio/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                intent = Intent.createChooser(intent,getString(R.string.select_file_by));

                //startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
                mPickFileActivityResultLauncher.launch(intent);

            }
        });


        binding.ringtonesTypeButtonGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {

                if (!isChecked)
                    return;

                switch (checkedId) {
                    case R.id.classic_btn_in_group :
                        if (mAudioPlayer != null && mAudioPlayer.isPlaying() )
                            mAudioPlayer.pause();
                        loadSystemRingtones();
                        break;
                    case R.id.app_btn_in_group:
                        if (mAudioPlayer != null && mAudioPlayer.isPlaying() )
                            mAudioPlayer.pause();
                        loadAppRingtones();
                        break;
                    case R.id.file_btn_in_group:
                        if (mAudioPlayer != null && mAudioPlayer.isPlaying() )
                            mAudioPlayer.pause();
                        loadUserRingtones();
                        break;
                }
            }
        });

        mScaleRecordBtnAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.record_voice_btn_anim);
        binding.recordSoundBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(requireActivity(), R.string.record_sound_btn_onClick_toast, Toast.LENGTH_LONG).show();
            }
        });
        binding.recordSoundBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if ( hasRecordVoicePermission ) {

                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        if (mMediaRecorder == null) {
                            binding.recordSoundBtn.startAnimation(mScaleRecordBtnAnim);
                            startRecording();
                        }
                    } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        if (mMediaRecorder != null) {
                            binding.recordSoundBtn.clearAnimation();
                            stopRecording();
                        }
                    }
                } else if ( motionEvent.getAction() == MotionEvent.ACTION_DOWN )
                    mRequestRecordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);

                return false;
            }
        });


        binding.recordedSoundRadioBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    binding.ringtonesRadioGroup.clearCheck();
                    mSelectedRingtoneData = mVoiceRecordedRingtoneData;
                    playRingtone( mSelectedRingtoneData );

                    NavHostFragment.findNavController(SelectRingtoneFragment.this)
                            .getPreviousBackStackEntry().getSavedStateHandle()
                            .set(EXTRA_SELECTED_RINGTONE_DATA,mSelectedRingtoneData);
                }
            }
        });

        //-- keep them last lines
        if ( mSelectedRingtoneData.getTypeID() == R.id.recorded_voice_ringtone_data_type) {
            binding.recordedSoundRadioBtn.setText( mVoiceRecordedRingtoneData.getTitle() );
            binding.recordedSoundCardView.setVisibility(View.VISIBLE);
            binding.recordedSoundRadioBtn.setChecked(true);
        } else if (mSelectedRingtoneData.getTypeID() == 0)
            ((MaterialButton)binding.ringtonesTypeButtonGroup.getChildAt(0)).setChecked(true);
        else
            binding.ringtonesTypeButtonGroup.check(mSelectedRingtoneData.getTypeID());

        return binding.getRoot();
    }

    public void playRingtone(RingtoneData ringtoneDataToPlay) {

        try {
            if (mAudioPlayer == null) {
                mAudioPlayer = new MediaPlayer();
            } else {
                mAudioPlayer.stop();
                mAudioPlayer.reset();
            }

            if ( ringtoneDataToPlay.getUri().equals(Uri.EMPTY) ) {
                mSelectedRingtoneData = ringtoneDataToPlay ;
                NavHostFragment.findNavController(SelectRingtoneFragment.this)
                        .getPreviousBackStackEntry().getSavedStateHandle()
                        .set(EXTRA_SELECTED_RINGTONE_DATA,mSelectedRingtoneData);
                return;
            }

           if (ringtoneDataToPlay.getTypeID() != R.id.recorded_voice_ringtone_data_type)
               //with context in constructor ,uri to be of some form of ContentProvider
               mAudioPlayer.setDataSource(getContext(), ringtoneDataToPlay.getUri() );
           else
               //Sets the data source (file-path), no access private data folder with content provider
               mAudioPlayer.setDataSource(ringtoneDataToPlay.getUri().toString() );

            mAudioPlayer.prepareAsync();

            mSelectedRingtoneData = ringtoneDataToPlay ;
            NavHostFragment.findNavController(SelectRingtoneFragment.this)
                    .getPreviousBackStackEntry().getSavedStateHandle()
                    .set(EXTRA_SELECTED_RINGTONE_DATA,mSelectedRingtoneData);

            //Bug with OGG files ,Android_Looping metadata can NOT overriding.
            // can't stop looping ,even onCompletion event Not Working.
            mAudioPlayer.setLooping(false);
            mAudioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                }
            });

        } catch (IOException e) {

            Toast.makeText(requireActivity(), R.string.problem_play_ringtone, Toast.LENGTH_LONG).show();

            if (ringtoneDataToPlay.getTypeID() != R.id.file_btn_in_group)
                return;

            UserRingtonesStorage.getInstance(getActivity()).deleteUserRingtoneFromList( ringtoneDataToPlay.getUri() );
            //View corruptedView = ringtonesRadioGroup.findViewWithTag( ringtoneDataToPlay.getUri() );
            View corruptedView = binding.ringtonesRadioGroup.findViewById(binding.ringtonesRadioGroup.getCheckedRadioButtonId());
            binding.ringtonesRadioGroup.removeView(corruptedView);
            mUserRingtonesList.remove( ringtoneDataToPlay );

            binding.ringtonesTypeButtonGroup.check(R.id.classic_btn_in_group);
            makeDefaultRingtoneAsSelected();
        }
    }


    public void loadSystemRingtones() {

        if (mSystemRingtonesList == null) {
            mRingtoneManager = new RingtoneManager(getActivity());
            mRingtoneManager.setType(RingtoneManager.TYPE_ALARM);
            //TODO exception with sqlite no ringtones on TAB
            Cursor cursor = mRingtoneManager.getCursor();
            mSystemRingtonesList = new ArrayList<>();

            //add silent
            String title = getActivity().getString(R.string.silent);
            Uri uri = Uri.EMPTY;
            mSystemRingtonesList.add(new RingtoneData(uri,title,binding.ringtonesTypeButtonGroup.getCheckedButtonId()));

            while (cursor.moveToNext()) {
                title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
                uri = Uri.parse( cursor.getString(RingtoneManager.URI_COLUMN_INDEX)+"/"+cursor.getString(RingtoneManager.ID_COLUMN_INDEX));

                mSystemRingtonesList.add(new RingtoneData(uri,title,binding.ringtonesTypeButtonGroup.getCheckedButtonId()));
            }
        }

        renderRingtonesRadioButtons(mSystemRingtonesList);
    }


    public void loadAppRingtones() {

        if (mAppRingtonesList == null) {
            mAppRingtonesList = new ArrayList<>();

            Field[] fields = R.raw.class.getDeclaredFields();
            String res_prefix = "android.resource://" + requireActivity().getPackageName() + "/raw/";
            for (Field field : fields) {

                String fileName = field.getName();
                if (fileName.equalsIgnoreCase("fizo_voice"))
                    continue;
                Uri uri = Uri.parse(res_prefix + fileName);
                mAppRingtonesList.add(new RingtoneData(uri, fileName, binding.ringtonesTypeButtonGroup.getCheckedButtonId()));

            }
        }
        renderRingtonesRadioButtons(mAppRingtonesList);
    }

    public void loadUserRingtones() {
        if (mUserRingtonesList == null) {
            mUserRingtonesList = UserRingtonesStorage.getInstance(getContext()).getRingtonesList(binding.ringtonesTypeButtonGroup.getCheckedButtonId());
        }
        renderRingtonesRadioButtons(mUserRingtonesList);
    }

    @SuppressLint("ResourceType")
    private void renderRingtonesRadioButtons(List<RingtoneData> ringtonesList) {
        binding.ringtonesRadioGroup.removeAllViews();

        if (binding.ringtonesTypeButtonGroup.getCheckedButtonId() == R.id.file_btn_in_group)
            binding.ringtonesRadioGroup.addView(addNewFileBtn);

        int[] attrs= {android.R.attr.layout_marginTop,android.R.attr.layout_marginBottom,android.R.attr.textSize};
        TypedArray ta = getContext().obtainStyledAttributes(R.style.ringtones_radio_group_item, attrs);
        int radioMarginTop = ta.getDimensionPixelSize   (0, 10);
        int radioMarginBottom = ta.getDimensionPixelSize   (1, 10);

        int radioTextSizeInSP = ta.getInt(2, 24);

        RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.WRAP_CONTENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
        );

        int margin18InDP = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,18,getResources().getDisplayMetrics());
        params.setMargins(margin18InDP, radioMarginTop, margin18InDP, radioMarginBottom);

        for (int i = 0; i< ringtonesList.size(); i++) {
            ContextThemeWrapper themeContext = new ContextThemeWrapper(getContext(), R.style.ringtones_radio_group_item);
            RadioButton radioButton = new RadioButton(themeContext);
            radioButton.setId(View.generateViewId());
            radioButton.setSingleLine();
            radioButton.setText(ringtonesList.get(i).getTitle() );
            radioButton.setTag(R.id.ringtone_uri,ringtonesList.get(i).getUri());
            radioButton.setLayoutParams(params);
            radioButton.setTextSize(TypedValue.COMPLEX_UNIT_SP,radioTextSizeInSP);

            binding.ringtonesRadioGroup.addView(radioButton);

            if (mSelectedRingtoneData.getUri().equals(ringtonesList.get(i).getUri()) )
                radioButton.setChecked(true);
        }
    }

    /*
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_FILE_REQUEST_CODE) {
            Uri uri = data.getData();
            String fileName = DocumentFile.fromSingleUri(getContext(),uri).getName();
            UserRingtonesStorage.getInstance(getContext()).saveRingtoneToList(uri,fileName);
            mUserRingtonesList = UserRingtonesStorage.getInstance(getContext()).getRingtonesList(binding.ringtonesTypeButtonGroup.getCheckedButtonId());
            getActivity().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            renderRingtonesRadioButtons(mUserRingtonesList);
            //child[0] is "add new file" button
            ( (RadioButton)binding.ringtonesRadioGroup.getChildAt(1) ).setChecked(true);

        }
    }
    */
    //---

    public void createVoiceRecordingsFolder() {

        File file = new File(getActivity().getFilesDir(),VOICE_RECORDINGS_FOLDER);
        file.mkdir();
        mSharedPreferences.edit().putBoolean(PREFERENCE_RECORDINGS_FOLDER_CREATED, true).apply();

    }

    public void startRecording() {

        if ( mAudioPlayer != null )
            mAudioPlayer.pause();

        String timeStamp = String.valueOf( System.currentTimeMillis() );
        if ( mVoiceRecordedFilePath.isEmpty() )
            mVoiceRecordedFilePath = getActivity().getFilesDir() + "/" + VOICE_RECORDINGS_FOLDER + "/" + timeStamp + ".3gp";

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setOutputFile( mVoiceRecordedFilePath );
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setMaxDuration( VOICE_RECORDING_MAX_TIME_IN_SECONDS * 1000 );
        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED){
                    stopRecording();
                    Toast.makeText(getActivity(), getString(R.string.voice_recording_max_time_reached, VOICE_RECORDING_MAX_TIME_IN_SECONDS), Toast.LENGTH_LONG).show();
                }
            }
        });

        try {
            mMediaRecorder.prepare();
            mToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
            mMediaRecorder.start();
        } catch (IOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }

    }

    public void stopRecording() {

        try {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);

            mMediaRecorder = null;

            String fileName = mVoiceRecordedFilePath.split("/")[mVoiceRecordedFilePath.split("/").length - 1];
            mVoiceRecordedRingtoneData = new RingtoneData(Uri.parse(mVoiceRecordedFilePath), fileName, R.id.recorded_voice_ringtone_data_type);
            mSelectedRingtoneData = mVoiceRecordedRingtoneData;

            binding.recordedSoundRadioBtn.setText(fileName);

            if (binding.recordedSoundCardView.getVisibility() == View.GONE)
                binding.recordedSoundCardView.setVisibility(View.VISIBLE);

            //to call checkedChangeListener
            if (binding.recordedSoundRadioBtn.isChecked())
                binding.recordedSoundRadioBtn.setChecked(false);

            binding.recordedSoundRadioBtn.setChecked(true);
        } catch (RuntimeException ex) {

            mMediaRecorder.release();
            mMediaRecorder = null;
            new File(mVoiceRecordedFilePath).delete();

            mToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);

            binding.recordedSoundCardView.setVisibility(View.GONE);
            if ( binding.recordedSoundRadioBtn.isChecked() ) {
                binding.recordedSoundRadioBtn.setChecked(false);
                makeDefaultRingtoneAsSelected();
            }

        }
    }

    private void makeDefaultRingtoneAsSelected() {
        Uri defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        String defaultRingtoneTitle = RingtoneManager.getRingtone(getActivity(), defaultRingtoneUri).getTitle(getActivity());
        RingtoneData defaultRingtoneData = new RingtoneData(defaultRingtoneUri, defaultRingtoneTitle, 0);

        NavHostFragment.findNavController(SelectRingtoneFragment.this)
                .getPreviousBackStackEntry().getSavedStateHandle()
                .set(EXTRA_SELECTED_RINGTONE_DATA,defaultRingtoneData);
    }

    //---

    @Override
    public void onPause() {
        super.onPause();
        if (mAudioPlayer !=null) {
            mAudioPlayer.stop();
            mAudioPlayer.release();
            mAudioPlayer = null;
        }

        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;

            File recordedFile = new File(mVoiceRecordedFilePath);
            recordedFile.delete();
        }

        if (mVoiceRecordedRingtoneData != null && mSelectedRingtoneData != mVoiceRecordedRingtoneData) {
            new File(mVoiceRecordedFilePath).delete();
            mVoiceRecordedRingtoneData = null;
            binding.recordedSoundCardView.setVisibility(View.GONE);
        }
    }

    private void initPickFileActivityResultLauncher() {

        mPickFileActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Uri uri = result.getData().getData();
                        String fileName = DocumentFile.fromSingleUri(getContext(),uri).getName();
                        UserRingtonesStorage.getInstance(getContext()).saveRingtoneToList(uri,fileName);
                        mUserRingtonesList = UserRingtonesStorage.getInstance(getContext()).getRingtonesList(binding.ringtonesTypeButtonGroup.getCheckedButtonId());
                        getActivity().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        renderRingtonesRadioButtons(mUserRingtonesList);
                        //child[0] is "add new file" button
                        ( (RadioButton)binding.ringtonesRadioGroup.getChildAt(1) ).setChecked(true);

                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        int recordAudioPermission = getActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO);
        hasRecordVoicePermission = recordAudioPermission == PackageManager.PERMISSION_GRANTED ;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //OnBackPressed
        /*
        if ( isRemoving() && mSelectedRingtoneURI!=null) {
            String fileName = DocumentFile.fromSingleUri(getContext(),mSelectedRingtoneURI).getName();
            Intent intent = new Intent();
            intent.setData(mSelectedRingtoneURI);
            intent.putExtra(EXTRA_SELECTED_RINGTONE_TITLE,fileName);
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK,intent);
         }
         */
    }

    @Override
    public void onStart() {
        super.onStart();
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.show();
    }

    @Override
    public void onStop() {
        super.onStop();
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.hide();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

}