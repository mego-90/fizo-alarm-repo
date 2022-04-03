package com.mego.fizoalarm.pojo;

import android.net.Uri;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Objects;

public class RingtoneData implements Serializable {

    //String for gson de/serialization , no default constructor
    //but getter/setter Uri
    @SerializedName("uri")
    private String uri;
    @SerializedName("title")
    private String title;
    //id of button in RingtoneTypeButtonGroup to be selected when selectRingtoneFragment Opened.
    @SerializedName("typeID")
    private int typeID;

    public RingtoneData (Uri uri,String title,int typeID) {
        this.uri = uri.toString();
        this.title = title;
        this.typeID = typeID;
    }

    public Uri getUri() {
        return Uri.parse(uri);
    }

    public void setUri(Uri uri) {
        this.uri = uri.toString();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getTypeID() {
        return typeID;
    }

    public void setTypeID(int typeID) {
        this.typeID = typeID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RingtoneData that = (RingtoneData) o;
        return uri.equals(that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }
}