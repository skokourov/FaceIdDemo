package com.ipoint.fd_demo.facedetectiondemo.facedetection;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.ipoint.fd_demo.facedetectiondemo.preview.GraphicOverlay;

/**
 * Created by spe on 15.02.2017.
 */

public class GraphicFaceTrackerFactory implements FacesProcessor.Factory<Face> {

    private GraphicOverlay graphicOverlay;

    public GraphicFaceTrackerFactory(GraphicOverlay graphicOverlay) {
        this.graphicOverlay = graphicOverlay;
    }

    @Override
    public Tracker<Face> create(Face face) {
        return new GraphicFaceTracker(graphicOverlay);
    }
}
