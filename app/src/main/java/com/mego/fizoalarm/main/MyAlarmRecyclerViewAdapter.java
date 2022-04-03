package com.mego.fizoalarm.main;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.os.ConfigurationCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.mego.fizoalarm.R;
import com.mego.fizoalarm.databinding.FragmentAlarmsListItemBinding;
import com.mego.fizoalarm.pojo.Alarm;
import com.mego.fizoalarm.storage.AlarmsListStorage;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MyAlarmRecyclerViewAdapter extends RecyclerView.Adapter<MyAlarmRecyclerViewAdapter.ViewHolder> {

    private final String DATE_PATTERN_WITH_YEAR = "E, MMM d, yyyy";
    private final String DATE_PATTERN_WITHOUT_YEAR = "E, MMM d";

    private DateTimeFormatter mDateFormatterWithYear = DateTimeFormatter.ofPattern(DATE_PATTERN_WITH_YEAR);
    private DateTimeFormatter mDateFormatterWithoutYear = DateTimeFormatter.ofPattern(DATE_PATTERN_WITHOUT_YEAR);

    Context mContext;
    private final List<Alarm> mValues;
    private List<Alarm> mAlarmsToDelete = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private AlarmsListFragment mAlarmsListFragment;
    private DayOfWeek mFirstDayOfWeek;
    private Locale currentLocale;
    private DateTimeFormatter timeFormatter ;
    private boolean mUse24Format;
    private SharedPreferences mSettingsPreference ;
    private ActionMode mActionMode;

    public MyAlarmRecyclerViewAdapter(Context context, List<Alarm> items, AlarmsListFragment alarmsListFragment) {
        mContext = context;
        mValues = items;
        mAlarmsListFragment = alarmsListFragment;

        mSettingsPreference = PreferenceManager.getDefaultSharedPreferences(context);

        String savedFirstDayOfWeek = mSettingsPreference.getString(SettingsActivity.SETTINGS_KEY_FIRST_DAY_IN_WEEK,"SUNDAY");
        mFirstDayOfWeek = DayOfWeek.valueOf(savedFirstDayOfWeek);

        currentLocale = ConfigurationCompat.getLocales(mContext.getResources().getConfiguration()).get(0);

        mUse24Format = DateFormat.is24HourFormat(context);

        String timePattern = mUse24Format ? "HH:mm" : "hh:mm a";
        timeFormatter = DateTimeFormatter.ofPattern(timePattern);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        FragmentAlarmsListItemBinding binding =
                FragmentAlarmsListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        binding.includedDaysChips.chip1stDay.setText(mFirstDayOfWeek.getDisplayName(TextStyle.SHORT,currentLocale).toLowerCase() );
        binding.includedDaysChips.chip1stDay.setTag(R.id.dayOfWeek_in_chip,mFirstDayOfWeek);

        binding.includedDaysChips.chip2ndDay.setText(mFirstDayOfWeek.plus(1).getDisplayName(TextStyle.SHORT,currentLocale).toLowerCase() );
        binding.includedDaysChips.chip2ndDay.setTag(R.id.dayOfWeek_in_chip,mFirstDayOfWeek.plus(1));

        binding.includedDaysChips.chip3rdDay.setText(mFirstDayOfWeek.plus(2).getDisplayName(TextStyle.SHORT,currentLocale).toLowerCase() );
        binding.includedDaysChips.chip3rdDay.setTag(R.id.dayOfWeek_in_chip,mFirstDayOfWeek.plus(2));

        binding.includedDaysChips.chip4thDay.setText(mFirstDayOfWeek.plus(3).getDisplayName(TextStyle.SHORT,currentLocale).toLowerCase() );
        binding.includedDaysChips.chip4thDay.setTag(R.id.dayOfWeek_in_chip,mFirstDayOfWeek.plus(3));

        binding.includedDaysChips.chip5thDay.setText(mFirstDayOfWeek.plus(4).getDisplayName(TextStyle.SHORT,currentLocale).toLowerCase() );
        binding.includedDaysChips.chip5thDay.setTag(R.id.dayOfWeek_in_chip,mFirstDayOfWeek.plus(4));

        binding.includedDaysChips.chip6thDay.setText(mFirstDayOfWeek.plus(5).getDisplayName(TextStyle.SHORT,currentLocale).toLowerCase() );
        binding.includedDaysChips.chip6thDay.setTag(R.id.dayOfWeek_in_chip,mFirstDayOfWeek.plus(5));

        binding.includedDaysChips.chip7thDay.setText(mFirstDayOfWeek.plus(6).getDisplayName(TextStyle.SHORT,currentLocale).toLowerCase() );
        binding.includedDaysChips.chip7thDay.setTag(R.id.dayOfWeek_in_chip,mFirstDayOfWeek.plus(6));

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Alarm currentAlarm = mValues.get(position);
        holder.alarmBinding.alarmTime.setText( timeFormatter.format(currentAlarm.getTime()) );
        holder.alarmBinding.alarmLabel.setText( currentAlarm.getLabel()) ;

        holder.alarmBinding.alarmSwitch.setOnCheckedChangeListener( null );
        holder.alarmBinding.alarmSwitch.setChecked( currentAlarm.isEnabled() );

        if (currentAlarm.getChallenge() == null)
            holder.alarmBinding.alarmChallengeIcon.setVisibility(View.INVISIBLE);
        else {
            holder.alarmBinding.alarmChallengeIcon.setVisibility(View.VISIBLE);
            holder.alarmBinding.alarmChallengeIcon.setImageResource(currentAlarm.getChallenge().getIconResourceID());
        }

        if (currentAlarm.getRepeat_days().size() > 0) {
            holder.alarmBinding.includedDaysChips.daysChipGroup.setVisibility(View.VISIBLE);
            holder.alarmBinding.alarmDateInList.setVisibility(View.INVISIBLE);

            holder.alarmBinding.includedDaysChips.chip1stDay.setChecked(currentAlarm.getRepeat_days().contains((DayOfWeek) holder.alarmBinding.includedDaysChips.chip1stDay.getTag(R.id.dayOfWeek_in_chip)));
            holder.alarmBinding.includedDaysChips.chip2ndDay.setChecked(currentAlarm.getRepeat_days().contains((DayOfWeek) holder.alarmBinding.includedDaysChips.chip2ndDay.getTag(R.id.dayOfWeek_in_chip)));
            holder.alarmBinding.includedDaysChips.chip3rdDay.setChecked(currentAlarm.getRepeat_days().contains((DayOfWeek) holder.alarmBinding.includedDaysChips.chip3rdDay.getTag(R.id.dayOfWeek_in_chip)));
            holder.alarmBinding.includedDaysChips.chip4thDay.setChecked(currentAlarm.getRepeat_days().contains((DayOfWeek) holder.alarmBinding.includedDaysChips.chip4thDay.getTag(R.id.dayOfWeek_in_chip)));
            holder.alarmBinding.includedDaysChips.chip5thDay.setChecked(currentAlarm.getRepeat_days().contains((DayOfWeek) holder.alarmBinding.includedDaysChips.chip5thDay.getTag(R.id.dayOfWeek_in_chip)));
            holder.alarmBinding.includedDaysChips.chip6thDay.setChecked(currentAlarm.getRepeat_days().contains((DayOfWeek) holder.alarmBinding.includedDaysChips.chip6thDay.getTag(R.id.dayOfWeek_in_chip)));
            holder.alarmBinding.includedDaysChips.chip7thDay.setChecked(currentAlarm.getRepeat_days().contains((DayOfWeek) holder.alarmBinding.includedDaysChips.chip7thDay.getTag(R.id.dayOfWeek_in_chip)));
        } else {
            holder.alarmBinding.alarmDateInList.setVisibility(View.VISIBLE);
            holder.alarmBinding.includedDaysChips.daysChipGroup.setVisibility(View.INVISIBLE);
            holder.alarmBinding.alarmDateInList.setText(adjustDateAndDetailsTextView(currentAlarm));
        }

        holder.alarmBinding.getRoot().setSelected( mAlarmsToDelete.contains( currentAlarm ) );

        holder.alarmBinding.alarmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isEnabled) {

                currentAlarm.setEnabled(isEnabled);
                AlarmsListStorage.getInstance(mAlarmsListFragment.getContext()).editAlarm(currentAlarm);
                if (isEnabled)
                    AlarmManagerBusiness.scheduleAlarm(mContext, currentAlarm, true);
                else {
                    AlarmManagerBusiness.disableAlarm(mContext, currentAlarm);
                    holder.alarmBinding.alarmDateInList.setText(adjustDateAndDetailsTextView(currentAlarm));
                }
            }
        });

        holder.alarmBinding.getRoot().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mActionMode == null)
                    mAlarmsListFragment.navigateToEditAlarm(currentAlarm.clone());
                else {
                    if (view.isSelected()) {
                        view.setSelected(false);
                        mAlarmsToDelete.remove(currentAlarm);
                        if (mAlarmsToDelete.size()==0) {
                            mActionMode.finish();
                            return ;
                        }
                    } else {
                        view.setSelected(true);
                        mAlarmsToDelete.add(currentAlarm);
                    }
                    mActionMode.setTitle(mAlarmsListFragment.getString(R.string.action_mode_x_items_selected, mAlarmsToDelete.size()));
                    mActionMode.invalidate();
                }
            }
        });

        holder.alarmBinding.getRoot().setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mActionMode == null) {
                    mActionMode = ((AppCompatActivity)mAlarmsListFragment.getActivity()).startSupportActionMode(new ActionModeCallbacks());
                    view.setSelected(true);
                    mAlarmsToDelete.add( currentAlarm );
                    mActionMode.setTitle(mAlarmsListFragment.getString(R.string.action_mode_x_items_selected,mAlarmsToDelete.size()));
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    private String adjustDateAndDetailsTextView(Alarm alarm) {

        LocalDateTime now = LocalDateTime.now();

        //Edit date to view when to schedule alarm , without save that edit
        if ( !alarm.isEnabled() && (alarm.getDate().isBefore(now.toLocalDate()) || alarm.getDate().isEqual(now.toLocalDate()) )) {
            alarm.setDate(now.toLocalDate());
            if (alarm.getTime().isBefore(now.toLocalTime()) || alarm.getTime().equals(now.toLocalTime()) )
                alarm.setDate(now.toLocalDate().plusDays(1));
        }
        String prefixWord = "";
        if (alarm.getDate().isEqual( now.toLocalDate()) )
            prefixWord = mContext.getString(R.string.today) + ", ";
        else if (alarm.getDate().isEqual( now.toLocalDate().plusDays(1)) )
            prefixWord = mContext.getString(R.string.tomorrow) + ", ";

        if (alarm.getDate().getYear() == now.getYear())
            return prefixWord + mDateFormatterWithoutYear.format(alarm.getDate());
        else
            return prefixWord + mDateFormatterWithYear.format(alarm.getDate());
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        FragmentAlarmsListItemBinding alarmBinding;

        public ViewHolder(FragmentAlarmsListItemBinding binding) {
            super(binding.getRoot());
            alarmBinding = binding;
        }

    }

    public void finishActionModeIfActive() {
        if ( mActionMode != null )
            mActionMode.finish();
    }

    public class ActionModeCallbacks implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            //mAlarmsListFragment.inflateToolbar();
            mode.getMenuInflater().inflate(R.menu.menu_action_mode,menu);
            //((AppCompatActivity)mAlarmsListFragment.getActivity()).getSupportActionBar().hide();

            //mAlarmsToDelete = new ArrayList<>();
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            switch (item.getItemId()) {

                case R.id.action_delete_alarms:

                    new AlertDialog.Builder(mAlarmsListFragment.getContext())
                            .setTitle(mAlarmsListFragment.getString(R.string.delete_confirmation_title, mAlarmsToDelete.size()))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    AlarmsListStorage.getInstance(mAlarmsListFragment.getContext())
                                            .deleteAlarms(mAlarmsToDelete);
                                    mValues.removeAll(mAlarmsToDelete);
                                    for (Alarm alarm : mAlarmsToDelete)
                                        AlarmManagerBusiness.disableAlarm(mContext,alarm);
                                    notifyDataSetChanged();
                                    mActionMode.finish();
                                }
                            })
                            .setNegativeButton(android.R.string.no,null)
                            .create()
                            .show();
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            //((AppCompatActivity)mAlarmsListFragment.getActivity()).getSupportActionBar().show();
            mActionMode = null;
            for (int i = 0; i < mRecyclerView.getChildCount(); i++)
                mRecyclerView.getChildAt(i).setSelected(false);

            //mAlarmsToDelete = null;
            mAlarmsToDelete.clear();
        }
    }
}