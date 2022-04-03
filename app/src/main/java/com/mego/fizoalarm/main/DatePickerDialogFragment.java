package com.mego.fizoalarm.main;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.fragment.NavHostFragment;

import com.mego.fizoalarm.R;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Calendar;


public class DatePickerDialogFragment extends DialogFragment {

    public static final String ARG_INIT_DATE = "com.mego.fizoalarm.datePickerDialogFragment.arg_init_date" ;
    public static final String ARG_FIRST_DAY_OF_WEEK = "com.mego.fizoalarm.datePickerDialogFragment.arg_first_day_of_week" ;
    public static final String EXTRA_DATE = "com.mego.fizoalarm.datePickerDialogFragment.extra_date" ;


    private LocalDate mDate;
    private DayOfWeek mFirstDayOfWeek;

    public DatePickerDialogFragment() { }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDate = (LocalDate) getArguments().getSerializable( ARG_INIT_DATE );
            mFirstDayOfWeek = (DayOfWeek) getArguments().getSerializable( ARG_FIRST_DAY_OF_WEEK );
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_date_picker,null);

        AlertDialog alertDialog = new AlertDialog.Builder (getActivity()).setView(v)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //sendResultOK();
                        NavHostFragment.findNavController(DatePickerDialogFragment.this)
                                .getPreviousBackStackEntry().getSavedStateHandle()
                                .set(EXTRA_DATE,mDate);
                    }
                })
                .setTitle(R.string.datePicker_title)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        NavHostFragment.findNavController(DatePickerDialogFragment.this)
                                .navigateUp();
                    }
                })
                .create();

        if ( mDate == null || mDate.isBefore(LocalDate.now()) )
            mDate = LocalDate.now();

        DatePicker datePicker = v.findViewById(R.id.dialog_date_datePicker);
        datePicker.setMinDate(Calendar.getInstance().getTimeInMillis());

        datePicker.init(mDate.getYear(), mDate.getMonthValue()-1, mDate.getDayOfMonth(),
                new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mDate = LocalDate.of(year,monthOfYear+1,dayOfMonth);
                NavHostFragment.findNavController(DatePickerDialogFragment.this)
                        .getPreviousBackStackEntry().getSavedStateHandle()
                        .set(EXTRA_DATE,mDate);
            }
        });

        return alertDialog;
    }
}