package com.mego.fizoalarm.pojo.challenges;

import android.content.Context;

import androidx.annotation.NonNull;

import com.mego.fizoalarm.R;
import com.mego.fizoalarm.pojo.MyBarcode;

public class BarcodeChallenge extends Challenge {

    private static final String CHALLENGE_TYPE = "BARCODE";

    private MyBarcode mBarcode;

    public BarcodeChallenge() {
        setChallengeType(CHALLENGE_TYPE);
        setClassFullyQualifiedName(BarcodeChallenge.class.getCanonicalName());
    }

    public BarcodeChallenge(MyBarcode barcode) {
        this();
        this.mBarcode = barcode;
    }

    public MyBarcode getBarcode() {
        return mBarcode;
    }

    public void setBarcode(MyBarcode barcode) {
        this.mBarcode = barcode;
    }

    @Override
    public int getStringResourceID() {
        return R.string.challenge_barcode;
    }

    @Override
    public int getIconResourceID() {
        return R.drawable.ic_baseline_photo_camera_24;
    }

    @Override
    public int getRingingFragmentResourceID() {
        return R.id.barcodeRingingFragment;
    }

    @Override
    public int getConfigFragmentResourceID() { return R.id.barcodeChallengeConfigDialog; }

    @Override
    public String generateDetailsText(Context context) {
        return context.getString(R.string.barcode_challenge_details_text, mBarcode.getLabel());
    }

    @NonNull
    @Override
    public Challenge clone() {
        BarcodeChallenge clonedChallenge = new BarcodeChallenge();
        clonedChallenge.setChallengeType(this.getChallengeType());
        clonedChallenge.setClassFullyQualifiedName(this.getClassFullyQualifiedName());
        clonedChallenge.setBarcode(new MyBarcode(getBarcode().getLabel(), getBarcode().getCode()));

        return clonedChallenge;
    }
}