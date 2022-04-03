package com.mego.fizoalarm.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.mego.fizoalarm.pojo.RingtoneData;

import java.util.ArrayList;
import java.util.List;

public class UserRingtonesStorage {

    private final String SHARED_PREFERENCE_FILE = "com.mego.fizoalarm.user_ringtones";

    private SharedPreferences mSharedPreferences;
    private static UserRingtonesStorage sInstance;

    private UserRingtonesStorage(Context context) {
        mSharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE_FILE,Context.MODE_PRIVATE);
    }

    public static UserRingtonesStorage getInstance(Context context) {

        if (sInstance == null)
            sInstance = new UserRingtonesStorage(context);
        return sInstance;
    }

    public void saveRingtoneToList(Uri ringtoneURI,String title) {

        int ringtonesCount =mSharedPreferences.getAll().keySet().size();
        if ( ringtonesCount>10 ) {
            String keyToDelete = (String)mSharedPreferences.getAll().keySet().toArray()[9];
            mSharedPreferences.edit()
                    .remove(keyToDelete)
                    .putString(ringtoneURI.toString(),title)
                    .apply();
            return;
        }
        SharedPreferences.Editor editor = mSharedPreferences.edit() ;
        editor.putString(ringtoneURI.toString(),title).apply();
    }

    public void deleteUserRingtoneFromList(Uri uriToDelete) {
        mSharedPreferences.edit()
                .remove( uriToDelete.toString() )
                .apply();
    }

    public List<RingtoneData> getRingtonesList(int typeID) {
        List<RingtoneData> ringtonesList = new ArrayList<>();

        for ( String key :mSharedPreferences.getAll().keySet() )
            ringtonesList.add(0,new RingtoneData(Uri.parse(key),mSharedPreferences.getString(key,"No Title"),typeID));

        return ringtonesList;
    }
}