package com.mego.fizoalarm.pojo.challenges;

import android.content.Context;

import androidx.annotation.NonNull;

import com.mego.fizoalarm.R;

public class MathChallenge extends Challenge {

    private static final String CHALLENGE_TYPE = "MATH";

    public static final int DIFFICULTY_EASY = R.string.easy;
    public static final int DIFFICULTY_NORMAL = R.string.normal;
    public static final int DIFFICULTY_HARD = R.string.hard;

    public static final int PROBLEMS_COUNT_MIN = 3;
    public static final int PROBLEMS_COUNT_MAX = 30;

    private int problemCount;
    private int difficulty;

    public MathChallenge() {
        setChallengeType(CHALLENGE_TYPE);
        setClassFullyQualifiedName(MathChallenge.class.getCanonicalName());
    }

    public MathChallenge(int difficulty, int problemCount ) {
        this();
        this.problemCount = problemCount;
        this.difficulty = difficulty;
    }

    public int getProblemCount() {
        return problemCount;
    }

    public void setProblemCount(int problemCount) {
        this.problemCount = problemCount;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }


    @Override
    public int getStringResourceID() {
        return R.string.challenge_math;
    }

    @Override
    public int getIconResourceID() {
        return R.drawable.ic_baseline_iso_24;
    }

    @Override
    public int getRingingFragmentResourceID() { return R.id.mathRingingFragment; }

    @Override
    public int getConfigFragmentResourceID() {return R.id.mathChallengeConfigDialog; }

    @Override
    public String generateDetailsText(Context context) {
        return context.getString(R.string.math_challenge_details_text, context.getString(getDifficulty()), problemCount );
    }

    @NonNull
    @Override
    public MathChallenge clone() {
        MathChallenge clonedChallenge = new MathChallenge();
        clonedChallenge.setChallengeType(this.getChallengeType());
        clonedChallenge.setClassFullyQualifiedName(this.getClassFullyQualifiedName());
        clonedChallenge.setDifficulty(this.getDifficulty());
        clonedChallenge.setProblemCount(this.getProblemCount());

        return clonedChallenge;
    }

}