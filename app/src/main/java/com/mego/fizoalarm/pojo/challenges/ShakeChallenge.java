package com.mego.fizoalarm.pojo.challenges;

import android.content.Context;

import androidx.annotation.NonNull;

import com.mego.fizoalarm.R;

public class ShakeChallenge extends Challenge {

    private static final String CHALLENGE_TYPE = "SHAKE";

    public static final int SHAKES_COUNT_MIN = 20;
    public static final int SHAKES_COUNT_MAX = 500;

    private int shakesCount;

    public ShakeChallenge() {
        setChallengeType(CHALLENGE_TYPE);
        setClassFullyQualifiedName(ShakeChallenge.class.getCanonicalName());
    }

    public ShakeChallenge(int shakesCount ) {
        this();
        this.shakesCount = shakesCount;
    }

    public int getShakesCount() {
        return shakesCount;
    }

    public void setShakesCount(int shakesCount) {
        this.shakesCount = shakesCount;
    }

    @Override
    public int getStringResourceID() {
        return R.string.challenge_shake;
    }

    @Override
    public int getIconResourceID() {
        return R.drawable.ic_baseline_screen_rotation_24;
    }

    @Override
    public int getRingingFragmentResourceID() {
        return R.id.shakeRingingFragment;
    }

    @Override
    public int getConfigFragmentResourceID() {return R.id.shakeChallengeConfigDialog; }

    @Override
    public String generateDetailsText(Context context) {
        return context.getString(R.string.shake_challenge_details_text, getShakesCount() );
    }

    @NonNull
    @Override
    public Challenge clone() {
        ShakeChallenge clonedChallenge = new ShakeChallenge();
        clonedChallenge.setChallengeType(this.getChallengeType());
        clonedChallenge.setClassFullyQualifiedName(this.getClassFullyQualifiedName());
        clonedChallenge.setShakesCount( this.getShakesCount() );

        return clonedChallenge;
    }
}