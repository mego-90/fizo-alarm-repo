package com.mego.fizoalarm.main;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.view.View;
import android.widget.NumberPicker;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mego.fizoalarm.R;
import com.mego.fizoalarm.pojo.challenges.ShakeChallenge;


public class ShakeChallengeConfigDialog extends DialogFragment {


    @Nullable
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState){
        View view = getActivity().getLayoutInflater().inflate(R.layout.shake_challenge_config, null);

        NumberPicker shakesNumberPicker = view.findViewById(R.id.shakes_number_picker);
        shakesNumberPicker.setMinValue(ShakeChallenge.SHAKES_COUNT_MIN);
        shakesNumberPicker.setMaxValue(ShakeChallenge.SHAKES_COUNT_MAX);

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(getContext())
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int requiredShakes = shakesNumberPicker.getValue();

                        ShakeChallenge shakeChallenge = new ShakeChallenge(requiredShakes);

                        Bundle bundle = new Bundle();
                        bundle.putSerializable( NewEditAlarmFragment.EXTRA_CHALLENGE_CONFIG, shakeChallenge );

                        getParentFragmentManager().setFragmentResult(NewEditAlarmFragment.RESULT_UPDATE_CHALLENGE_CONFIG, bundle);

                    }
                });

        return dialog.create();
    }
}