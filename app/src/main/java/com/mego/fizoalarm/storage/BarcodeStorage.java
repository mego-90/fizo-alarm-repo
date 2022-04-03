package com.mego.fizoalarm.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.mego.fizoalarm.pojo.MyBarcode;

import java.util.ArrayList;
import java.util.List;

public class BarcodeStorage {

    private final String SHARED_PREFERENCE_FILE = "com.mego.fizoalarm.barcodes_list";

    public static final int MAX_BARCODES_ITEMS_COUNT = 10;

    private SharedPreferences mSharedPreferences;
    private static BarcodeStorage sInstance;

    private BarcodeStorage(Context context) {
        mSharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE_FILE,Context.MODE_PRIVATE);
    }

    public static BarcodeStorage getInstance(Context context) {

        if (sInstance == null)
            sInstance = new BarcodeStorage(context);
        return sInstance;
    }

    public void saveBarcode(MyBarcode barcode) {
        mSharedPreferences.edit()
                .putString(barcode.getCode(), barcode.getLabel())
                .apply();
    }

    public List<MyBarcode> getAllBarcodes() {
        ArrayList<MyBarcode> barcodes = new ArrayList<>();
        for (String code : mSharedPreferences.getAll().keySet()) {
            String label = mSharedPreferences.getString(code,"");
            if ( ! label.isEmpty())
                barcodes.add( new MyBarcode(label, code));
        }
        return barcodes;
    }

    public void deleteBarcode(MyBarcode barcode) {
        mSharedPreferences.edit()
                .remove(barcode.getCode())
                .apply();
    }

    public int getBarcodesCount() {
        return mSharedPreferences.getAll().size();
    }


}
