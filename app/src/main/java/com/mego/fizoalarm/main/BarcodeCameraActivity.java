package com.mego.fizoalarm.main;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.util.Size;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.mego.fizoalarm.R;
import com.mego.fizoalarm.databinding.ActivityBarcodeCameraBinding;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class BarcodeCameraActivity extends AppCompatActivity {

    public static final String TYPE_ADD_NEW_BARCODE = "com.mego.fizoalarm.main.barcodeCamera.type_add_new";
    public static final String TYPE_DISMISS_ALARM = "com.mego.fizoalarm.main.barcodeCamera.type_dismiss_alarm";

    public static final String ARG_REQUIRED_BARCODE = "com.mego.fizoalarm.main.barcodeCamera.arg_required_barcode";
    public static final String ARG_REQUIRED_BARCODE_LABEL = "com.mego.fizoalarm.main.barcodeCamera.arg_required_barcode_label";
    public static final String ARG_PROCESSING_TYPE = "com.mego.fizoalarm.main.barcodeCamera.arg_PROCESSING_TYPE";
    public static final String EXTRA_DETECTED_BARCODE = "com.mego.fizoalarm.main.barcodeCamera.arg_detected_barcode";

    private ActivityBarcodeCameraBinding binding;

    private ImageAnalysis mImageAnalysis;
    private BarcodeScanner mBarcodeScanner;

    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;
    private Camera mCamera;
    private CameraSelector mCameraSelector;

    private String mProcessingType;
    private String mRequiredBarcode;
    private String mRequiredBarcodeLabel;

    private boolean isFlashLightOn = false;

    //sometimes wrong read of barcode , so when add new barcode
    //it must verify two reads
    private String mVerifiedBarcode = "";
    private int mVerifyCount = 0;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            this.setShowWhenLocked(true);
            KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
            keyguardManager.requestDismissKeyguard(this,null);
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBarcodeCameraBinding.inflate( getLayoutInflater() );
        setContentView( binding.getRoot() );

        binding.toolbar.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        binding.toolbar.inflateMenu(R.menu.menu_camera_challenges);
        binding.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();

                if (id == R.id.action_flash_on_off) {
                    if ( mCamera.getCameraInfo().hasFlashUnit() ) {
                        isFlashLightOn = !isFlashLightOn;
                        mCamera.getCameraControl().enableTorch(isFlashLightOn);
                    } else
                        Toast.makeText(BarcodeCameraActivity.this, R.string.barcode_switch_camera_to_use_flash, Toast.LENGTH_LONG).show();

                    return true;
                } else if (id == R.id.action_switch_camera) {
                    flipCamera();
                    return true;
                }

                return false;
            }
        });

        int cameraPermission = checkSelfPermission( Manifest.permission.CAMERA );
        if ( cameraPermission == PackageManager.PERMISSION_DENIED)
            requestCameraPermission();


        mProcessingType = getIntent().getStringExtra(ARG_PROCESSING_TYPE);
        if (mProcessingType.equals(TYPE_DISMISS_ALARM) ) {
            mRequiredBarcode = getIntent().getStringExtra(ARG_REQUIRED_BARCODE);
            mRequiredBarcodeLabel = getIntent().getStringExtra(ARG_REQUIRED_BARCODE_LABEL);
            binding.toolbar.setTitle(mRequiredBarcodeLabel);
        } else
            binding.toolbar.setTitle(R.string.barcode_toolbar_default_label);

        //Barcode Section
        BarcodeScannerOptions barcodeScannerOptions = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        mBarcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions);
        // ---

        mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        mImageAnalysis = new ImageAnalysis.Builder()
                // 1280x720 or 1920x1080
                //.setTargetResolution(new Size(640, 480))
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        mImageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {

                Image mediaImage = imageProxy.getImage();
                if (mediaImage != null) {
                    detectBarcodeInImage(mBarcodeScanner, imageProxy);
                }
            }
        });

        startPreview();

    }

    private void startPreview() {

        mCameraProviderFuture = ProcessCameraProvider.getInstance(this);

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
        }, ContextCompat.getMainExecutor(this));


    }

    public void flipCamera() {
        if (mCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
            mCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
        else
            mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        startPreview();
    }

    private void detectBarcodeInImage(BarcodeScanner barcodeScanner, ImageProxy imageProxy) {

        InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

        barcodeScanner.process(inputImage)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> barcodes) {

                        if (barcodes.size() == 0)
                            return;

                        String detectedBarcode = barcodes.get(0).getDisplayValue();

                        if ( mProcessingType.equals( TYPE_ADD_NEW_BARCODE) ) {
                            if ( detectedBarcode.equals(mVerifiedBarcode) ) {
                                mVerifyCount++;
                                if (mVerifyCount == 6)
                                    onAddNewBarcode(detectedBarcode);
                            } else {
                                mVerifiedBarcode = detectedBarcode;
                                mVerifyCount = 0;
                            }
                        } else if ( mProcessingType.equals( TYPE_DISMISS_ALARM) )
                            isEqualRequired(detectedBarcode);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        FirebaseCrashlytics.getInstance().recordException(e);
                    }
                }).addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
                    @Override
                    public void onComplete(@NonNull Task<List<Barcode>> task) {
                        if (imageProxy.getImage() != null)
                            imageProxy.getImage().close();
                        imageProxy.close();
                    }
                });

    }

    private void onAddNewBarcode(String detectedBarcode) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DETECTED_BARCODE, detectedBarcode);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private void isEqualRequired(String detectedBarcode) {
        if ( detectedBarcode.equals(mRequiredBarcode)) {
            setResult(Activity.RESULT_OK);
            finish();
        }
    }

    public void requestCameraPermission() {
        registerForActivityResult( new ActivityResultContracts.RequestPermission(), isGranted -> {
            if ( ! isGranted) {
                Toast.makeText(this, R.string.camera_permission_not_granted, Toast.LENGTH_LONG).show();
                setResult(Activity.RESULT_OK);
                finish();
            }
        }) .launch(Manifest.permission.CAMERA);

    }

}