package com.ipoint.fd_demo.facedetectiondemo.facedetection;

import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.ipoint.fd_demo.facedetectiondemo.FaceDetectManager;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by spe on 15.02.2017.
 */

public class FacesProcessor<T> implements Detector.Processor<T> {

    private Factory<T> trackerFactory;

    private SparseArray<FacesProcessor.TrackedFace> trackedFaces;

    private int maxGapFrames;

    private FaceDetectManager faceDetectManager = null;

    private FacesProcessor() {
        trackedFaces = new SparseArray<>();
        maxGapFrames = 3;
    }

    @Override
    public void release() {
        for (int index = 0; index < trackedFaces.size(); ++index) {
            Tracker tracker = ((TrackedFace) this.trackedFaces.valueAt(index)).tracker;
            tracker.onDone();
        }
        trackedFaces.clear();
    }

    @Override
    public void receiveDetections(Detector.Detections<T> detections) {
        createNewTrackedFaces(detections);
        clearUnusedTrackedFaces(detections);
        updateTrackedFaces(detections);
    }

    private void createNewTrackedFaces(Detector.Detections<T> detections) {
        SparseArray detectedItems = detections.getDetectedItems();

        for (int index = 0; index < detectedItems.size(); ++index) {
            int detectedItemKey = detectedItems.keyAt(index);
            Object detectedItem = detectedItems.valueAt(index);
            if (trackedFaces.get(detectedItemKey) == null) {
                TrackedFace trackedFace = new TrackedFace();
                trackedFace.tracker = this.trackerFactory.create((T) detectedItem);
                trackedFace.tracker.onNewItem(detectedItemKey, (T) detectedItem);
                trackedFaces.append(detectedItemKey, trackedFace);
                faceDetectManager.newFaceDetected(detectedItemKey, detections.getFrameMetadata().getTimestampMillis());
            }
        }
    }

    private void clearUnusedTrackedFaces(Detector.Detections<T> detections) {
        SparseArray detectedItems = detections.getDetectedItems();
        HashSet toDelete = new HashSet();

        for (int index = 0; index < trackedFaces.size(); ++index) {
            int detectedItemKey = trackedFaces.keyAt(index);
            if (detectedItems.get(detectedItemKey) == null) {
                TrackedFace trackedFace = (TrackedFace) trackedFaces.valueAt(index);
                trackedFace.gap++;
                if (trackedFace.gap >= maxGapFrames) {
                    trackedFace.tracker.onDone();
                    toDelete.add(Integer.valueOf(detectedItemKey));
                } else {
                    trackedFace.tracker.onMissing(detections);
                    faceDetectManager.missedFace();
                }
            }
        }
        Iterator iterator = toDelete.iterator();
        while (iterator.hasNext()) {
            Integer toDeleteIndex = (Integer) iterator.next();
            this.trackedFaces.delete(toDeleteIndex.intValue());
        }
    }

    private void updateTrackedFaces(Detector.Detections<T> detections) {
        SparseArray detectedItems = detections.getDetectedItems();

        for (int index = 0; index < detectedItems.size(); ++index) {
            int detectedItemKey = detectedItems.keyAt(index);
            Object o = detectedItems.valueAt(index);
            TrackedFace trackedFace = (TrackedFace) trackedFaces.get(detectedItemKey);
            trackedFace.gap = 0;
            trackedFace.tracker.onUpdate(detections, (T) o);
            faceDetectManager.updateFace(detectedItemKey, detections.getFrameMetadata().getTimestampMillis());
        }

    }

    public void setTrackerFactory(Factory<T> trackerFactory) {
        this.trackerFactory = trackerFactory;
    }

    public void setMaxGapFrames(int maxGapFrames) {
        this.maxGapFrames = maxGapFrames;
    }

    public void setFaceDetectManager(FaceDetectManager faceDetectManager) {
        this.faceDetectManager = faceDetectManager;
    }

    private class TrackedFace {
        private Tracker<T> tracker;
        private int gap;

        private TrackedFace() {
            this.gap = 0;
        }

        public void setTracker(Tracker<T> tracker) {
            this.tracker = tracker;
        }
    }

    public static class Builder<T> {
        private FacesProcessor<T> facesProcessor = new FacesProcessor();

        public Builder(FacesProcessor.Factory<T> trackerFactory) {
            if (trackerFactory == null) {
                throw new IllegalArgumentException("No factory supplied.");
            } else {
                this.facesProcessor.setTrackerFactory(trackerFactory);
            }
        }

        public FacesProcessor.Builder<T> setMaxGapFrames(int maxGapFrames) {
            if (maxGapFrames < 0) {
                throw new IllegalArgumentException("Invalid max gap: " + maxGapFrames);
            } else {
                this.facesProcessor.setMaxGapFrames(maxGapFrames);
                return this;
            }
        }

        public FacesProcessor.Builder<T> setFaceDetectManager(FaceDetectManager faceDetectManager) {
            this.facesProcessor.setFaceDetectManager(faceDetectManager);
            return this;
        }

        public FacesProcessor<T> build() {
            return this.facesProcessor;
        }
    }


    public interface Factory<T> {
        Tracker<T> create(T item);
    }
}
