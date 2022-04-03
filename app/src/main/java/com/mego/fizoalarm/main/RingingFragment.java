package com.mego.fizoalarm.main;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.mego.fizoalarm.R;
import com.mego.fizoalarm.databinding.FragmentRingingBinding;
import com.mego.fizoalarm.pojo.challenges.Challenge;


public class RingingFragment extends Fragment {

    public static final String ARG_ALARM_LABEL = "com.mego.fizoalarm.main.ringingFragment.arg_alarm_label";
    public static final String ARG_IS_CHALLENGE_ALARM = "com.mego.fizoalarm.main.ringingFragment.arg_is_challenge_alarm";
    public static final String ARG_SNOOZE_ALLOWED = "com.mego.fizoalarm.main.ringingFragment.arg_snooze_allowed";

    private FragmentRingingBinding binding;

    private RingingFragment.Callbacks mCallbacks;
    private String mAlarmLabel;
    private boolean mIsChallengeAlarm;
    private boolean mSnoozeAllowed;

    public RingingFragment() { }

    /*
    public static RingingFragment newInstance(String alarmLabel, ChallengeType challengeType) {
        RingingFragment fragment = new RingingFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ALARM_LABEL, alarmLabel);
        args.putSerializable(ARG_CHALLENGE_TYPE, challengeType);
        fragment.setArguments(args);
        return fragment;
    }

    */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            //mAlarm = (Alarm) getArguments().getSerializable(ARG_ALARM);
            mAlarmLabel = getArguments().getString(ARG_ALARM_LABEL);
            mIsChallengeAlarm = getArguments().getBoolean(ARG_IS_CHALLENGE_ALARM);
            mSnoozeAllowed = getArguments().getBoolean(ARG_SNOOZE_ALLOWED);
        }

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRingingBinding.inflate(inflater, container, false);

        binding.alarmLabelInRingingFragment.setText( mAlarmLabel );

        if ( mIsChallengeAlarm ) {
            binding.dismissAlarmBtn.setText(R.string.open_challenge);
            binding.dismissAlarmBtn.setIconResource(R.drawable.ic_baseline_open_in_new_24);
        }
        binding.dismissAlarmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsChallengeAlarm)
                    mCallbacks.openChallenge();
                else
                    mCallbacks.dismissAlarm();
            }
        });

        if ( ! mSnoozeAllowed)
            binding.snoozeAlarmBtn.setVisibility(View.INVISIBLE);
        binding.snoozeAlarmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallbacks.snoozeAlarm();
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof RingingFragment.Callbacks)
            mCallbacks = (RingingFragment.Callbacks) context;
        else
            throw new UnsupportedOperationException("Not yet implemented");


    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    public interface Callbacks {
        void dismissAlarm();
        void snoozeAlarm();
        void openChallenge();
        void correctToDefaultChallengeAndReportProblem(Challenge challenge, RuntimeException ex);
    }

}