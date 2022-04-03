package com.mego.fizoalarm.pojo.challenges;

import android.content.Context;

import androidx.annotation.NonNull;


import java.io.Serializable;

public abstract class Challenge implements Serializable,Cloneable {

    protected String challengeType;
    protected String classFullyQualifiedName;

    public void setChallengeType(String challengeType) {
        this.challengeType = challengeType;
    }

    public String getChallengeType() {
        return this.challengeType;
    }

    public String getClassFullyQualifiedName() {
        return classFullyQualifiedName;
    }

    public void setClassFullyQualifiedName(String classFullyQualifiedName) {
        this.classFullyQualifiedName = classFullyQualifiedName;
    }

    public abstract int getStringResourceID();

    public abstract int getIconResourceID();

    public abstract int getRingingFragmentResourceID();

    public abstract int getConfigFragmentResourceID();

    public abstract String generateDetailsText(Context context);

    @NonNull
    @Override
    public abstract Challenge clone();

}
