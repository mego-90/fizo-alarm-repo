package com.mego.fizoalarm.pojo.challenges;

import android.content.Context;

import androidx.annotation.NonNull;

import com.mego.fizoalarm.R;

public class SmileChallenge extends Challenge {

    private static final String CHALLENGE_TYPE = "SMILE";

    public SmileChallenge() {
        setChallengeType(CHALLENGE_TYPE);
        setClassFullyQualifiedName(SmileChallenge.class.getCanonicalName());
    }



    @Override
    public int getStringResourceID() { return R.string.challenge_smile; }

    @Override
    public int getIconResourceID() {
        return R.drawable.ic_baseline_tag_faces_24;
    }

    @Override
    public int getRingingFragmentResourceID() {
        return R.id.smileRingingFragment;
    }

    @Override
    public int getConfigFragmentResourceID() {
        return -1;
    }

    @Override
    public String generateDetailsText(Context context) {
        return context.getString(R.string.smile_challenge_details_text);
    }

    @NonNull
    @Override
    public Challenge clone() {
        return this;
    }
}
