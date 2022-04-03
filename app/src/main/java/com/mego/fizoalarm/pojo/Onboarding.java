package com.mego.fizoalarm.pojo;

import android.view.View;

import java.io.Serializable;
import java.util.concurrent.Callable;

public class Onboarding implements Serializable {

    private int image;
    private int title;
    private int description;
    private int permissionBtnVisibility = View.INVISIBLE;
    private transient Callable<Void> callable;

    public Onboarding() {
    }

    public Onboarding(int image, int title, int description) {
        this.image = image;
        this.title = title;
        this.description = description;
    }

    public Onboarding(int image, int title, int description, Callable<Void> callable) {
        this.image = image;
        this.title = title;
        this.description = description;
        this.permissionBtnVisibility = View.VISIBLE;
        this.callable = callable;
    }



    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public int getTitle() {
        return title;
    }

    public void setTitle(int title) {
        this.title = title;
    }

    public int getDescription() {
        return description;
    }

    public void setDescription(int description) {
        this.description = description;
    }

    public int getPermissionBtnVisibility() {
        return permissionBtnVisibility;
    }

    public void setPermissionBtnVisibility(int permissionBtnVisibility) {
        this.permissionBtnVisibility = permissionBtnVisibility;
    }

    public Callable<Void> getCallable() {
        return callable;
    }

    public void setCallable(Callable<Void> callable) {
        this.callable = callable;
    }
}