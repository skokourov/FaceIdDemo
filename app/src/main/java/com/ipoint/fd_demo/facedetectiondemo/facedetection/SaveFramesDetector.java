package com.ipoint.fd_demo.facedetectiondemo.facedetection;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.ipoint.fd_demo.facedetectiondemo.FaceDetectManager;

import java.io.ByteArrayOutputStream;

/**
 * Created by spe on 16.02.2017.
 */

public class SaveFramesDetector extends Detector {

    private FaceDetectManager faceDetectManager = null;

    @Override
    public SparseArray detect(Frame frame) {
        if (faceDetectManager != null) {
            long frameTimestamp = frame.getMetadata().getTimestampMillis();

            final int pixelsCount = frame.getMetadata().getWidth() * frame.getMetadata().getHeight();
            byte[] bytesGrey = frame.getGrayscaleImageData().array();
            int[] intGreyBuffer = new int[pixelsCount];

            for(int i=0; i < pixelsCount; i++)
            {
                int greyValue = (int)bytesGrey[i] & 0xff;
                intGreyBuffer[i] = 0xff000000 | (greyValue << 16) | (greyValue << 8) | greyValue;
            }
            Bitmap bitmap = Bitmap.createBitmap(intGreyBuffer, frame.getMetadata().getWidth(), frame.getMetadata().getHeight(), Bitmap.Config.ARGB_8888);


            Matrix matrix = new Matrix();

            matrix.postRotate(-90);

            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap , 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
            byte[] byteArray = stream.toByteArray();
            faceDetectManager.storeImage(byteArray, frameTimestamp);
        }
        return null;
    }

    public void setFaceDetectManager(FaceDetectManager faceDetectManager) {
        this.faceDetectManager = faceDetectManager;
    }

    public static class Builder {

        private SaveFramesDetector saveFramesDetector = new SaveFramesDetector();

        public Builder(){
            saveFramesDetector.setProcessor(new StubProcessor());
        }

        public Builder setFaceDetectManager(FaceDetectManager faceDetectManager) {
            this.saveFramesDetector.setFaceDetectManager(faceDetectManager);
            return this;
        }

        public SaveFramesDetector build(){
            return this.saveFramesDetector;
        }
    }
}

