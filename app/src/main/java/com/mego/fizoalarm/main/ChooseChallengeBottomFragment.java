package com.mego.fizoalarm.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mego.fizoalarm.R;
import com.mego.fizoalarm.databinding.FragmentChooseChallengeBottomSheetBinding;
import com.mego.fizoalarm.pojo.Alarm;
import com.mego.fizoalarm.pojo.challenges.SmileChallenge;

/**
 * <p>A fragment that shows a list of items as a modal bottom sheet.</p>
 * <p>You can show this modal bottom sheet from your activity like this:</p>
 * <pre>
 *     ChooseChallengeBottomFragment.newInstance(30).show(getSupportFragmentManager(), "dialog");
 * </pre>
 */
public class ChooseChallengeBottomFragment extends BottomSheetDialogFragment {

    public static final String TAG = "com.mego.hellalarm.choose_challenge_bottom_sheet_fragment";

    public static final String RESULT_REMOVE_CHALLENGE = "com.mego.hellalarm.choose_challenge_bottom_sheet_fragment.result_remove_challenge";
    //public static final String EXTRA_CHALLENGE = "com.mego.hellalarm.choose_challenge_bottom_sheet_fragment.extra.challenge";

    private static final String ARG_ALARM = "com.mego.hellalarm.choose_challenge_bottom_sheet_fragment.arg_alarm";

    private FragmentChooseChallengeBottomSheetBinding binding;

    private NavController navController;

    private ActivityResultLauncher<String> mRequestCameraPermForBarcode;
    private ActivityResultLauncher<String> mRequestCameraPermForSmile;

    private ChooseChallengeBottomFragment() { }


    public static ChooseChallengeBottomFragment newInstance(Alarm alarm) {
        final ChooseChallengeBottomFragment fragment = new ChooseChallengeBottomFragment();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_ALARM, alarm);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRequestCameraPermForBarcode = registerForActivityResult( new ActivityResultContracts.RequestPermission(), isGranted -> {
            if ( isGranted) {
                dismiss();
                navController.navigate(R.id.barcodeChallengeConfigDialog);
            }else
                showPermissionNotGrantedSnack();

        });

        mRequestCameraPermForSmile = registerForActivityResult( new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                dismiss();
                SmileChallenge smileChallenge = new SmileChallenge();
                Bundle bundle = new Bundle();
                bundle.putSerializable( NewEditAlarmFragment.EXTRA_CHALLENGE_CONFIG, smileChallenge );
                getParentFragmentManager().setFragmentResult(NewEditAlarmFragment.RESULT_UPDATE_CHALLENGE_CONFIG, bundle);
            } else
                showPermissionNotGrantedSnack();

        });

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChooseChallengeBottomSheetBinding.inflate(inflater, container, false);


        Fragment newEditAlarmFragment =getParentFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = NavHostFragment.findNavController(newEditAlarmFragment);

        binding.challengeNoneLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                getParentFragmentManager().setFragmentResult(RESULT_REMOVE_CHALLENGE, null);
                dismiss();
            }
        });


        binding.challengeMathLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                dismiss();
                navController.navigate(R.id.mathChallengeConfigDialog);
            }
        });


        binding.challengeShakeLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                dismiss();
                navController.navigate(R.id.shakeChallengeConfigDialog);
            }
        });


        binding.challengeBarcodeLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                int cameraPermission = getActivity().checkSelfPermission( Manifest.permission.CAMERA );
                if ( cameraPermission == PackageManager.PERMISSION_GRANTED) {
                    dismiss();
                    navController.navigate(R.id.barcodeChallengeConfigDialog);
                } else
                    mRequestCameraPermForBarcode.launch(Manifest.permission.CAMERA);


            }
        });


        binding.challengeSmileLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                SmileChallenge smileChallenge = new SmileChallenge();
                Bundle bundle = new Bundle();
                bundle.putSerializable( NewEditAlarmFragment.EXTRA_CHALLENGE_CONFIG, smileChallenge );

               int cameraPermission = getActivity().checkSelfPermission( Manifest.permission.CAMERA );
                if ( cameraPermission == PackageManager.PERMISSION_GRANTED) {
                    dismiss();
                    getParentFragmentManager().setFragmentResult(NewEditAlarmFragment.RESULT_UPDATE_CHALLENGE_CONFIG, bundle);
                } else
                    mRequestCameraPermForSmile.launch(Manifest.permission.CAMERA);

            }
        });

        return binding.getRoot();
    }


    public void showPermissionNotGrantedSnack() {

        Toast.makeText(getActivity(), R.string.camera_permission_not_granted, Toast.LENGTH_LONG).show();

        //TODO Snackbar
        //the problem is snackbar not take full width of screen,
        //and action text too long
        /*
        View rootLayout = getView().getRootView();
        Snackbar snackbar = Snackbar.make(rootLayout, R.string.camera_permission_not_granted, Snackbar.LENGTH_LONG)
                .setAction(R.string.open_settings, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent openSettingIntent = new Intent();
                        openSettingIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                        openSettingIntent.setData(uri);
                        openSettingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(openSettingIntent);
                    }
                });

        View snackbarView = snackbar.getView();
        snackbarView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        snackbarView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        TextView snackTextView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);

        snackTextView.setMaxLines(3);

        snackbar.show();
        */
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

}