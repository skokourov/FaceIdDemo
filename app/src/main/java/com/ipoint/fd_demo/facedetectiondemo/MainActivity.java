package com.ipoint.fd_demo.facedetectiondemo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiDetector;
import com.google.android.gms.vision.face.FaceDetector;
import com.ipoint.fd_demo.facedetectiondemo.facedetection.FacesProcessor;
import com.ipoint.fd_demo.facedetectiondemo.facedetection.GraphicFaceTrackerFactory;
import com.ipoint.fd_demo.facedetectiondemo.facedetection.SaveFramesDetector;
import com.ipoint.fd_demo.facedetectiondemo.preview.CameraSourcePreview;
import com.ipoint.fd_demo.facedetectiondemo.preview.GraphicOverlay;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "FaceDetectDemo";

    private static final String FRAGMENT_DIALOG = "dialog";

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private static final int RC_HANDLE_GMS = 9001;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final int MAX_PREVIEW_WIDTH = 1920;

    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    private static final int STATE_PICTURE_TAKEN = 4;

    private int state = STATE_PREVIEW;

    private CameraSource cameraSource = null;

    private CameraSourcePreview preview;

    private GraphicOverlay graphicOverlay;


    private FaceDetectManager faceDetectManager;

    private TextView personIdValue;

    private TextView personInfoValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        personIdValue = (TextView) findViewById(R.id.person_id_value);
        personInfoValue = (TextView) findViewById(R.id.person_info_value);
        final Button saveButton = (Button) findViewById(R.id.save);
        saveButton.setEnabled(false);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePerson();
            }
        });
        faceDetectManager = new FaceDetectManager(this, new FaceDetectManager.FaceDetectAction() {
            @Override
            public void onSavePersonAvailable(boolean enabled) {
                saveButton.setEnabled(enabled);
                if (!enabled) {
                    personIdValue.setText("Unknown");
                    personInfoValue.setText("Unknown");
                }
            }

            @Override
            public void onPersonRecognized(String personId) {
                personIdValue.setText(personId);
            }

            @Override
            public void onPersonInfoReceived(String personInfo) {
                personInfoValue.setText(personInfo);
            }

        });

        preview = (CameraSourcePreview) findViewById(R.id.preview);
        graphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    protected void onPause() {
        preview.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
    }


    private void createCameraSource() {

        Context context = getApplicationContext();

        FaceDetector faceDetector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                .build();
        faceDetector.setProcessor(
                new FacesProcessor.Builder<>(new GraphicFaceTrackerFactory(graphicOverlay)).
                        setFaceDetectManager(faceDetectManager).build());

        if (!faceDetector.isOperational()) {
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }
        SaveFramesDetector saveFramesDetector = new SaveFramesDetector.Builder().
                setFaceDetectManager(faceDetectManager).build();

        MultiDetector multiDetector = new MultiDetector.Builder()
                .add(saveFramesDetector).add(faceDetector).build();

        cameraSource = new CameraSource.Builder(context, multiDetector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();
    }


    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (cameraSource != null) {
            try {
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(getFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.request_permission))
                        .show(getFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void savePerson() {
        faceDetectManager.savePerson();
    }


}
