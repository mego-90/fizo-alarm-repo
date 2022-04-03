package com.mego.fizoalarm.main;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.mego.fizoalarm.R;
import com.mego.fizoalarm.databinding.FragmentShakeRingingBinding;
import com.mego.fizoalarm.pojo.challenges.ShakeChallenge;
import com.mego.fizoalarm.pojo.challenges.ShakeEventListener;


public class ShakeRingingFragment extends Fragment {

    //private static final String ARG_REQUIRED_SHAKES = "com.mego.fizoalarm.shakeRingingFragment.arg_required_shakes";

    private FragmentShakeRingingBinding binding;

    private SensorManager mSensorManager;
    private ShakeEventListener mSensorListener;

    private ShakeChallenge mShakeChallenge;
    private int mRequiredShakes;

    private RingingFragment.Callbacks mCallbacks;

    public ShakeRingingFragment() {

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() == null)
            throw new IllegalStateException("start shake ringing without 'requiredShakes'");

        mShakeChallenge = (ShakeChallenge) getArguments().getSerializable(RingingActivity.ARG_CHALLENGE);
        mRequiredShakes = mShakeChallenge.getShakesCount();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentShakeRingingBinding.inflate(inflater, container, false);

        binding.doneShakesCountTextView.setText(getString(R.string.done_shakes, mRequiredShakes));

        binding.shakeDismissBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallbacks.dismissAlarm();
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSensorListener.setOnShakeListener(new ShakeEventListener.OnShakeListener() {

            public void onShake() {
                mRequiredShakes--;
                binding.doneShakesCountTextView.setText(getString(R.string.done_shakes, mRequiredShakes));
                if (mRequiredShakes <= 0) {
                    binding.shakeDismissBtn.setVisibility(View.VISIBLE);
                    mSensorManager.unregisterListener(mSensorListener);
                }
            }
        });

    }

    @Override
    public void onPause() {
        mSensorManager.unregisterListener(mSensorListener);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof RingingFragment.Callbacks)
            mCallbacks = (RingingFragment.Callbacks) context;
        else
            throw new UnsupportedOperationException("Not yet implemented");

        mSensorManager = getContext().getSystemService(SensorManager.class);
        mSensorListener = new ShakeEventListener();
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

}