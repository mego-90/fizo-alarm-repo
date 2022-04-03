package com.mego.fizoalarm.main;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mego.fizoalarm.R;
import com.mego.fizoalarm.pojo.challenges.MathChallenge;

public class MathChallengeConfigDialog extends DialogFragment {

    //public static final String ARG_CHALLENGE_TYPE = "com.mego.fizoalarm.challenge_config_dialog.arg_challenge_type";

    private NumberPicker mProblemsCountNumberPicker;
    private MaterialButtonToggleGroup mDifficultyBtnGroup;
    private View mView;
    private TextView mExampleTextView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        if (getArguments() == null) {
            dismiss();
            return;
        }
        */

        //challengeType = ChallengeType.valueOf( getArguments().getString(ARG_CHALLENGE_TYPE) );
    }

    @Nullable
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState){

        mView = getActivity().getLayoutInflater().inflate(R.layout.math_challenge_config, null);

        mExampleTextView = mView.findViewById(R.id.math_difficulty_example);

        Button easyButton = mView.findViewById(R.id.easy_btn_in_group);
        easyButton.setTag(MathChallenge.DIFFICULTY_EASY);

        Button normalButton = mView.findViewById(R.id.normal_btn_in_group);
        normalButton.setTag(MathChallenge.DIFFICULTY_NORMAL);

        Button hardButton = mView.findViewById(R.id.hard_btn_in_group);
        hardButton.setTag(MathChallenge.DIFFICULTY_HARD);

        mProblemsCountNumberPicker = mView.findViewById(R.id.number_of_problems_picker);
        mProblemsCountNumberPicker.setMinValue(MathChallenge.PROBLEMS_COUNT_MIN);
        mProblemsCountNumberPicker.setMaxValue(MathChallenge.PROBLEMS_COUNT_MAX);

        mDifficultyBtnGroup = mView.findViewById(R.id.difficulty_button_group);
        mDifficultyBtnGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (isChecked) {

                    Button selectedBtn = group.findViewById(checkedId);
                    switch ( (int)selectedBtn.getTag() ) {
                        case MathChallenge.DIFFICULTY_EASY :
                            mExampleTextView.setText(R.string.difficulty_example_easy);
                            break;
                        case MathChallenge.DIFFICULTY_NORMAL :
                            mExampleTextView.setText(R.string.difficulty_example_normal);
                            break;
                        case MathChallenge.DIFFICULTY_HARD :
                            mExampleTextView.setText(R.string.difficulty_example_hard);
                            break;
                    }
                }
            }
        });

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(getContext())
                .setView(mView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onPositiveButton();
                    }
                });

        return dialog.create();
    }

    public void onPositiveButton() {

        int problemCount = mProblemsCountNumberPicker.getValue();
        int selectedBtnID = mDifficultyBtnGroup.getCheckedButtonId();
        //mDifficultyBtnGroup = mView.findViewById(selectedBtnID);
        Button selectedBtn = mDifficultyBtnGroup.findViewById(selectedBtnID);

        MathChallenge mathChallenge = new MathChallenge((int)selectedBtn.getTag(), problemCount);

        Bundle bundle = new Bundle();
        bundle.putSerializable( NewEditAlarmFragment.EXTRA_CHALLENGE_CONFIG, mathChallenge );

        getParentFragmentManager().setFragmentResult(NewEditAlarmFragment.RESULT_UPDATE_CHALLENGE_CONFIG, bundle);
    }

}