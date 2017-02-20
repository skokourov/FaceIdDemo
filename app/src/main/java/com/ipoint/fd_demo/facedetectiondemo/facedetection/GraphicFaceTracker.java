package com.ipoint.fd_demo.facedetectiondemo.facedetection;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.ipoint.fd_demo.facedetectiondemo.preview.FaceGraphic;
import com.ipoint.fd_demo.facedetectiondemo.preview.GraphicOverlay;

/**
 * Created by spe on 15.02.2017.
 */

public class GraphicFaceTracker  extends Tracker<Face> {
    private GraphicOverlay overlay;
    private FaceGraphic faceGraphic;

    GraphicFaceTracker(GraphicOverlay overlay) {
        this.overlay = overlay;
        faceGraphic = new FaceGraphic(overlay);
    }

    /**
     * Start tracking the detected face instance within the face overlay.
     */
    @Override
    public void onNewItem(int faceId, Face item) {
        faceGraphic.setId(faceId);

    }

    /**
     * Update the position/characteristics of the face within the overlay.
     */
    @Override
    public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
        overlay.add(faceGraphic);
        faceGraphic.updateFace(face);
    }

    /**
     * Hide the graphic when the corresponding face was not detected.  This can happen for
     * intermediate frames temporarily (e.g., if the face was momentarily blocked from
     * view).
     */
    @Override
    public void onMissing(FaceDetector.Detections<Face> detectionResults) {
        overlay.remove(faceGraphic);
    }

    /**
     * Called when the face is assumed to be gone for good. Remove the graphic annotation from
     * the overlay.
     */
    @Override
    public void onDone() {
        overlay.remove(faceGraphic);
    }
}
