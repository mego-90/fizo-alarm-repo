package com.mego.fizoalarm.main;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.mego.fizoalarm.R;
import com.mego.fizoalarm.databinding.FragmentSmileRingingBinding;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;


public class SmileRingingFragment extends Fragment {

    private FragmentSmileRingingBinding binding;

    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;
    private ImageAnalysis mImageAnalysis;
    private FaceDetector mFaceDetector;
    private Camera mCamera;
    private CameraSelector mCameraSelector;

    private RingingFragment.Callbacks mCallbacks;

    private boolean isFlashLightOn = false;

    public SmileRingingFragment() {

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentSmileRingingBinding.inflate(inflater, container, false);

        binding.toolbar.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        binding.toolbar.inflateMenu(R.menu.menu_camera_challenges);
        binding.toolbar.setTitle(R.string.challenge_smile);
        binding.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();

                if (id == R.id.action_flash_on_off) {
                    if ( mCamera.getCameraInfo().hasFlashUnit() ) {
                        isFlashLightOn = !isFlashLightOn;
                        mCamera.getCameraControl().enableTorch(isFlashLightOn);
                    } else
                        Toast.makeText(getActivity(), R.string.barcode_switch_camera_to_use_flash, Toast.LENGTH_LONG).show();

                    return true;
                } else if (id == R.id.action_switch_camera) {
                    flipCamera();
                    return true;
                }

                return false;
            }
        });

        int cameraPermission = getActivity().checkSelfPermission( Manifest.permission.CAMERA );
        if ( cameraPermission == PackageManager.PERMISSION_DENIED)
            requestCameraPermission();

        //Face detection Section
        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .build();

        mFaceDetector = FaceDetection.getClient(highAccuracyOpts);
        //------

        mCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

        mImageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        mImageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {

                Image mediaImage = imageProxy.getImage();
                if (mediaImage != null) {
                    detectFaceInImage(mFaceDetector, imageProxy);
                }
            }
        });

        startPreview();

        return binding.getRoot();
    }


    private void startPreview() {

        mCameraProviderFuture = ProcessCameraProvider.getInstance(getActivity());

        mCameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = mCameraProviderFuture.get();

                cameraProvider.unbindAll();

                Preview preview = new Preview.Builder()
                        .build();

                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                mCamera = cameraProvider.bindToLifecycle((LifecycleOwner)this, mCameraSelector, mImageAnalysis,  preview);


            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(getActivity()));

    }

    public void flipCamera() {
        if (mCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
            mCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
        else
            mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        startPreview();
    }


    private void detectFaceInImage(FaceDetector faceDetector, ImageProxy imageProxy) {

        InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());


        faceDetector.process(inputImage)
                .addOnSuccessListener(
                        new OnSuccessListener<List<Face>>() {
                            @Override
                            public void onSuccess(List<Face> faces) {
                                if (faces.size() == 0)
                                    return;

                                if ( isOpenEyesAndSmile(faces.get(0) )) {
                                    faceDetector.close();
                                    mCallbacks.dismissAlarm();
                                }
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                FirebaseCrashlytics.getInstance().recordException(e);
                            }
                        })
                .addOnCompleteListener(new OnCompleteListener<List<Face>>() {
                    @Override
                    public void onComplete(@NonNull Task<List<Face>> task) {
                        if (imageProxy.getImage() != null)
                            imageProxy.getImage().close();
                        imageProxy.close();
                    }
                });

    }

    public boolean isOpenEyesAndSmile(Face face) {

        float smileProb = face.getSmilingProbability();
        float rightEyeOpenProb = face.getRightEyeOpenProbability();
        float leftEyeOpenProb = face.getLeftEyeOpenProbability();

        return smileProb > 0.75 && rightEyeOpenProb > 0.75 && leftEyeOpenProb > 0.75;

    }


    public void requestCameraPermission() {
        registerForActivityResult( new ActivityResultContracts.RequestPermission(), isGranted -> {
            if ( ! isGranted) {
                Toast.makeText(getActivity(), R.string.camera_permission_not_granted, Toast.LENGTH_LONG).show();
                mCallbacks.dismissAlarm();
                getActivity().finish();
            }
        }) .launch(Manifest.permission.CAMERA);

    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof RingingFragment.Callbacks)
            mCallbacks = (RingingFragment.Callbacks) context;
        else
            throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

}