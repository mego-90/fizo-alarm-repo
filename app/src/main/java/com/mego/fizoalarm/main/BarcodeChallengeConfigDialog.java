package com.mego.fizoalarm.main;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TableLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.mego.fizoalarm.R;
import com.mego.fizoalarm.databinding.BarcodeChallengeConfigBinding;
import com.mego.fizoalarm.pojo.MyBarcode;
import com.mego.fizoalarm.pojo.challenges.BarcodeChallenge;
import com.mego.fizoalarm.storage.BarcodeStorage;

import java.util.ArrayList;
import java.util.List;

public class BarcodeChallengeConfigDialog extends DialogFragment {

    private Button mAddCodeBtn;
    private TableLayout mBarCodeContainer;
    private List<RadioButton> mAllBarcodesRadioBtn = new ArrayList<>();
    private MyBarcode mSelectedBarCode;
    private Button mDialogPositiveButton;

    private ActivityResultLauncher<Intent> mAddBarcodeActivityResultLauncher;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAddBarcodeActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if ( result.getResultCode()== Activity.RESULT_OK )
                            if ( result.getData().getStringExtra(BarcodeCameraActivity.EXTRA_DETECTED_BARCODE) != null) {
                                String detectedBarcode = result.getData().getStringExtra(BarcodeCameraActivity.EXTRA_DETECTED_BARCODE);
                                View view = getActivity().getLayoutInflater().inflate(R.layout.add_label_to_barcode_dialog, null);
                                TextInputLayout textInputLayout = view.findViewById(R.id.add_barcode_label);
                                textInputLayout.setHelperText( getString(R.string.add_label_to_code_helper_text, detectedBarcode) );
                                //MaterialAlertDialogBuilder dialog =
                                new MaterialAlertDialogBuilder(getActivity())
                                    .setView(view)
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            String input = textInputLayout.getEditText().getText().toString();
                                            if ( !input.isEmpty()) {
                                                MyBarcode barcode = new MyBarcode(input, detectedBarcode);
                                                BarcodeStorage.getInstance(getActivity()).saveBarcode(barcode);
                                                renderBarcodesRadioGroup();
                                            }
                                        }
                                    })
                                    .create()
                                    .show();
                            }
                    }
                });

    }

    @Nullable
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.barcode_challenge_config, null);

        mBarCodeContainer = view.findViewById(R.id.barcodes_labels_container);

        renderBarcodesRadioGroup();

        mAddCodeBtn = view.findViewById(R.id.add_new_barcode);
        mAddCodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int max_allowed_barcode_count = BarcodeStorage.MAX_BARCODES_ITEMS_COUNT;
                if (BarcodeStorage.getInstance(getActivity()).getBarcodesCount() >= max_allowed_barcode_count) {
                    Toast.makeText(getActivity(), getString(R.string.barcode_max_items_count_reached, max_allowed_barcode_count), Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent(getActivity(), BarcodeCameraActivity.class);
                intent.putExtra(BarcodeCameraActivity.ARG_PROCESSING_TYPE, BarcodeCameraActivity.TYPE_ADD_NEW_BARCODE);
                mAddBarcodeActivityResultLauncher.launch(intent);
            }
        });

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(getActivity())
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onPositiveButton();
                    }
                });

        AlertDialog alertDialog = dialogBuilder.create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                mDialogPositiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                mDialogPositiveButton.setEnabled(false);
            }
        });

        return alertDialog;
    }

    public void onPositiveButton() {

        BarcodeChallenge barcodeChallenge = new BarcodeChallenge(mSelectedBarCode);

        Bundle bundle = new Bundle();
        bundle.putSerializable( NewEditAlarmFragment.EXTRA_CHALLENGE_CONFIG, barcodeChallenge );

        getParentFragmentManager().setFragmentResult(NewEditAlarmFragment.RESULT_UPDATE_CHALLENGE_CONFIG, bundle);

    }


    private void renderBarcodesRadioGroup () {

        mBarCodeContainer.removeAllViews();
        mAllBarcodesRadioBtn.clear();

        List<MyBarcode> barcodesLabels = BarcodeStorage.getInstance(getActivity()).getAllBarcodes();

        LayoutInflater inflater =  getActivity().getLayoutInflater();

        for (MyBarcode barcode : barcodesLabels) {

            MaterialCardView cardView = (MaterialCardView) inflater.inflate(R.layout.card_barcode_label, null);

            RadioButton barcodeRadioBtn = cardView.findViewById(R.id.barcode_radio_button);
            Button deleteBarcodeBtn = cardView.findViewById(R.id.barcode_delete_btn);

            barcodeRadioBtn.setId(View.generateViewId());
            barcodeRadioBtn.setText(barcode.getLabel());
            barcodeRadioBtn.setTag(barcode.getCode());
            barcodeRadioBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    if (checked) {
                        mSelectedBarCode = barcode;
                        clearOtherCheckedRadioBtn(barcode.getCode());
                        mDialogPositiveButton.setEnabled(true);
                    }
                }
            });

            mAllBarcodesRadioBtn.add(barcodeRadioBtn);

            deleteBarcodeBtn.setId(View.generateViewId());
            deleteBarcodeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    BarcodeStorage.getInstance(getActivity()).deleteBarcode(barcode);
                    mBarCodeContainer.removeView((View)view.getParent());
                }
            });

            mBarCodeContainer.addView(cardView);
        }
    }

    private void clearOtherCheckedRadioBtn(String selectedRadioBtnTag) {

        for (RadioButton radioButton : mAllBarcodesRadioBtn) {
            String radioTag = (String) radioButton.getTag();
            if ( ! radioTag.equals(selectedRadioBtnTag) )
                radioButton.setChecked(false);
        }
    }

}