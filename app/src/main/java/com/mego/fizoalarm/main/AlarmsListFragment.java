package com.mego.fizoalarm.main;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mego.fizoalarm.R;
import com.mego.fizoalarm.databinding.FragmentAlarmsListBinding;
import com.mego.fizoalarm.pojo.Alarm;
import com.mego.fizoalarm.pojo.RemoteConfigKeys;
import com.mego.fizoalarm.receivers.AlarmDeviceAdmin;
import com.mego.fizoalarm.storage.AlarmsListStorage;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class AlarmsListFragment extends Fragment {

    public static final String ACTION_ALARM_DISMISSED = "com.mego.fizoalarm.ringingService.ACTION_ALARM_DISMISSED";
    public static final String EXTRA_DISMISSED_ALARM_ID = "com.mego.fizoalarm.ringingService.EXTRA_DISMISSED_ALARM_ID";

    private List<Alarm> mValues;
    private BroadcastReceiver mAlarmDismissedBroadcastReceiver;

    private ActivityResultLauncher<String> mRequestPhonePerm;

    private SharedPreferences mSettingsPreference ;

    private FragmentAlarmsListBinding binding;

    private static DevicePolicyManager devicePolicyManager;
    private static ComponentName alarmDeviceAdminComponent;

    public AlarmsListFragment() { }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mValues = AlarmsListStorage.getInstance(getContext()).getAllAlarms();

        devicePolicyManager = requireActivity().getSystemService(DevicePolicyManager.class);
        alarmDeviceAdminComponent = new ComponentName(requireActivity(), AlarmDeviceAdmin.class);

        mRequestPhonePerm = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted)
                        Toast.makeText(requireActivity(), R.string.permission_phone_state_needed_description, Toast.LENGTH_LONG).show();;
                });

        getParentFragmentManager().setFragmentResultListener(
                NewEditAlarmFragment.REQUEST_NEW_ALARM, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                Alarm newAlarm = (Alarm)result.getSerializable(NewEditAlarmFragment.EXTRA_RETURNED_ALARM);
                mValues.add(newAlarm);
                Collections.sort(mValues);
                binding.alarmsListRecyclerView.getAdapter().notifyDataSetChanged();

                int phoneStatePerm = requireActivity().checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
                //request only one each time
                if ( phoneStatePerm == PackageManager.PERMISSION_DENIED )
                    mRequestPhonePerm.launch(Manifest.permission.READ_PHONE_STATE);
                else if ( !Settings.canDrawOverlays(requireActivity()) )
                    checkAndRequestOverlayPermission();
                else
                    checkAndRequestDeviceAdmin();
            }
        });

        getParentFragmentManager().setFragmentResultListener(
                NewEditAlarmFragment.REQUEST_EDIT_ALARM, this, new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                        Alarm editedAlarm = (Alarm)result.getSerializable(NewEditAlarmFragment.EXTRA_RETURNED_ALARM);
                        mValues.removeIf(alarm -> alarm.equals(editedAlarm) );
                        mValues.add(editedAlarm);
                        Collections.sort(mValues);
                        binding.alarmsListRecyclerView.getAdapter().notifyDataSetChanged();
                    }
                });

        mAlarmDismissedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int dismissedAlarmID = intent.getIntExtra(EXTRA_DISMISSED_ALARM_ID,0);
                if ( binding==null || binding.alarmsListRecyclerView.getAdapter()== null || dismissedAlarmID==0)
                    return;

                mValues.stream()
                        .filter( a -> a.getId() == dismissedAlarmID )
                        .findFirst()
                        .get()
                        .setEnabled(false);
                binding.alarmsListRecyclerView.getAdapter().notifyDataSetChanged();
            }
        };

        getActivity().getApplicationContext().registerReceiver(mAlarmDismissedBroadcastReceiver, new IntentFilter(ACTION_ALARM_DISMISSED));

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAlarmsListBinding.inflate(inflater, container, false);

        binding.alarmsListRecyclerView.setAdapter(new MyAlarmRecyclerViewAdapter(getContext(),mValues,this) );
        binding.alarmsListRecyclerView.addItemDecoration(new RecycleViewItemDivider(getContext()));

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (AlarmsListStorage.getInstance(getContext()).getAlarmsCount() > 50) {
                    Toast.makeText(getContext(), R.string.maximum_number_of_alarms_reached, Toast.LENGTH_LONG).show();
                    return;
                }

                ((MyAlarmRecyclerViewAdapter) binding.alarmsListRecyclerView.getAdapter()).finishActionModeIfActive();

                if ( ((MainActivity) getActivity()).canNavigateIfNotShowAd() ) {

                    AlarmsListFragmentDirections.ActionAlarmsListFragmentToNewEditAlarmFragment action =
                            AlarmsListFragmentDirections.actionAlarmsListFragmentToNewEditAlarmFragment();
                    NavHostFragment.findNavController(AlarmsListFragment.this).navigate(action);
                }
            }
        });

        return binding.getRoot();
    }

    public void navigateToEditAlarm(Alarm alarm) {
        AlarmsListFragmentDirections.ActionAlarmsListFragmentToNewEditAlarmFragment action =
                AlarmsListFragmentDirections.actionAlarmsListFragmentToNewEditAlarmFragment();
        action.setArgAlarmToEdit(alarm);
        NavHostFragment.findNavController(this).navigate(action);
    }

    @Override
    public void onDestroy() {
        if (mAlarmDismissedBroadcastReceiver != null)
            getActivity().getApplicationContext().unregisterReceiver(mAlarmDismissedBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mSettingsPreference = PreferenceManager.getDefaultSharedPreferences(context);
    }


    private void checkAndRequestOverlayPermission() {

        if (!Settings.canDrawOverlays( requireActivity() )) {

            new AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.permission_alert_window_needed)
                    .setMessage(R.string.permission_alert_window_needed_description)
                    .setPositiveButton(R.string.grant_permission, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent requestOverlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + requireActivity().getPackageName()));
                            startActivity(requestOverlayIntent);
                        }
                    })
                    .setNegativeButton(R.string.no_thanks, null)
                    .create()
                    .show();
        }
    }

    private void checkAndRequestDeviceAdmin() {

        if ( !devicePolicyManager.isAdminActive(alarmDeviceAdminComponent)) {
            new MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.device_admin_dialog_title)
                    .setMessage( getString(R.string.device_admin_dialog_message) )
                    .setPositiveButton(R.string.device_admin_dialog_title, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, alarmDeviceAdminComponent);//ComponentName of the administrator component.

                            startActivity(intent);
                        }
                    })
                    .setNegativeButton(R.string.no_thanks, null)
                    .create()
                    .show();
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.show();
    }

    @Override
    public void onStop() {
        super.onStop();
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.hide();
    }

}