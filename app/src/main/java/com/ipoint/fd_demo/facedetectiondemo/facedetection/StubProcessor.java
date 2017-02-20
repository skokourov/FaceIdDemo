package com.ipoint.fd_demo.facedetectiondemo.facedetection;

import com.google.android.gms.vision.Detector;

/**
 * Created by spe on 16.02.2017.
 */

public class StubProcessor implements Detector.Processor {

    @Override
    public void release() {
    }

    @Override
    public void receiveDetections(Detector.Detections detections) {
    }
}
