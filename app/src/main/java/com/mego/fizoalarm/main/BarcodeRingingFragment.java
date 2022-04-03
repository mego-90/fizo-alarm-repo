package com.mego.fizoalarm.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.mego.fizoalarm.pojo.challenges.BarcodeChallenge;

public class BarcodeRingingFragment extends Fragment {

    private BarcodeChallenge mBarcodeChallenge;

    private RingingFragment.Callbacks mCallbacks;

    private ActivityResultLauncher<Intent> mDetectBarcodeActivityResultLauncher;


    public BarcodeRingingFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() == null)
            throw new IllegalStateException("start barcode ringing without 'requiredBarcode'");

        mBarcodeChallenge = (BarcodeChallenge) getArguments().getSerializable(RingingActivity.ARG_CHALLENGE);

        //we use specific Activity because Barcode may start from BarcodeConfig to add new barcode
        mDetectBarcodeActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if ( result.getResultCode()== Activity.RESULT_OK )
                            mCallbacks.dismissAlarm();
                        else
                            requireActivity().finish();
                    }
                });

        Intent intent = new Intent(getActivity(), BarcodeCameraActivity.class);
        intent.putExtra(BarcodeCameraActivity.ARG_PROCESSING_TYPE, BarcodeCameraActivity.TYPE_DISMISS_ALARM);
        intent.putExtra(BarcodeCameraActivity.ARG_REQUIRED_BARCODE, mBarcodeChallenge.getBarcode().getCode() );
        intent.putExtra(BarcodeCameraActivity.ARG_REQUIRED_BARCODE_LABEL, mBarcodeChallenge.getBarcode().getLabel() );

        mDetectBarcodeActivityResultLauncher.launch(intent);
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

}