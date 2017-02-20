package com.ipoint.fd_demo.facedetectiondemo;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import com.ipoint.faceid.lib.FaceId;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.ipoint.fd_demo.facedetectiondemo.MainActivity.TAG;

/**
 * Created by spe on 30.01.2017.
 */

public class FaceDetectManager {

    private static final String FACE_ID_USER = "demo";
    private static final String FACE_ID_PASSWORD = "demo1234";
    private static final String FACE_ID_CLIENT_ID = "demo";
    private static final String FACE_ID_CLIENT_SECRET = "demo";

    private static long MAX_QUEUE_SIZE = 100;

    private static int MIN_PERSON_IMAGES_COUNT = 10;

    public interface FaceDetectAction {
        void onSavePersonAvailable(boolean enabled);

        void onPersonRecognized(String personId);

        void onPersonInfoReceived(String personInfo);
    }

    public static class StoredImage {
        long timestamp;
        byte[] bytes;

        public StoredImage(long timestamp, byte[] bytes) {
            this.timestamp = timestamp;
            this.bytes = bytes;
        }
    }

    private List<byte[]> collectedImages;

    private String currentPersonId = null;

    private Activity activity;

    private FaceDetectAction imageCaptureAction;

    private FaceId faceId = null;

    private int currentFaceId = -1;

    private ConcurrentLinkedQueue<StoredImage> imagesQueue;

    public FaceDetectManager(Activity activity, FaceDetectAction imageCaptureAction) {
        this.activity = activity;
        this.imageCaptureAction = imageCaptureAction;
        this.collectedImages = new ArrayList<>();
        this.imagesQueue = new ConcurrentLinkedQueue<>();
        FaceId.getInstance(activity, FACE_ID_USER, FACE_ID_PASSWORD, FACE_ID_CLIENT_ID, FACE_ID_CLIENT_SECRET,
                new FaceId.ReadyCallback() {
                    @Override
                    public void onReady(FaceId faceId) {
                        FaceDetectManager.this.faceId = faceId;
                    }

                    @Override
                    public void onError(String error) {
                        Log.d(TAG, "Error on init FaceId lib:" + error);
                    }
                });
    }

    public void storeImage(byte[] bytes, long imageTimestamp) {
        imagesQueue.add(new StoredImage(imageTimestamp, bytes));
        if (imagesQueue.size() > MAX_QUEUE_SIZE) {
            imagesQueue.poll();
        }
    }

    public void newFaceDetected(int faceId, long imageTimestamp) {
        collectedImages.clear();
        currentPersonId = null;
        callOnSavePersonAvailable(false);
        currentFaceId = faceId;
        byte[] image = getImage(imageTimestamp);
        if (image != null) {
            callGetPersonId(image);
            collectedImages.add(image);
            if (collectedImages.size() == MIN_PERSON_IMAGES_COUNT) {
                callOnSavePersonAvailable(true);
            }
        }
    }

    public void updateFace(int faceId, long imageTimestamp) {
        byte[] image = getImage(imageTimestamp);
        if (image != null && faceId == currentFaceId) {
            if (collectedImages.size() < MIN_PERSON_IMAGES_COUNT) {
                collectedImages.add(image);
                if (collectedImages.size() == MIN_PERSON_IMAGES_COUNT) {
                    callOnSavePersonAvailable(true);
                }
            }
        }
    }

    public void missedFace(){
        collectedImages.clear();
        currentPersonId = null;
        callOnSavePersonAvailable(false);
        currentFaceId = -1;
    }

    private byte[] getImage(long timestamp) {
        do {
            StoredImage storedImage = imagesQueue.peek();
            if (storedImage == null) {
                return null;
            }
            if (storedImage.timestamp < timestamp) {
                imagesQueue.poll();
            } else if (storedImage.timestamp == timestamp) {
                return imagesQueue.poll().bytes;
            } else {
                Log.d(TAG, "Image not found for ts = " + timestamp);
                return null;
            }
        } while (true);
    }

    private void callOnSavePersonAvailable(final boolean value) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageCaptureAction.onSavePersonAvailable(value);
            }
        });
    }

    private void callGetPersonId(byte[] imageBytes) {
        if (faceId != null && currentPersonId == null) {
            faceId.getPersonId(imageBytes, new FaceId.PersonIdCallback() {
                @Override
                public void onSuccess(final String personId) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            currentPersonId = personId;
                            imageCaptureAction.onPersonRecognized(personId);
                        }
                    });

                    faceId.getPersonInfo(personId, new FaceId.PersonInfoCallback() {
                        @Override
                        public void onSuccess(JSONObject personInfo) {
                            final String info;
                            try {
                                info = personInfo.getString("info");
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        imageCaptureAction.onPersonInfoReceived(info);
                                    }
                                });
                            } catch (JSONException e) {
                                Log.d(TAG, "Error on call FaceId getPersonInfo:" + e.getMessage());
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Log.d(TAG, "Error on call FaceId getPersonInfo:" + error);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    Log.d(TAG, "Error on call FaceId getPersonId:" + error);
                }
            });
        }
    }

    public void savePerson() {
        if (faceId != null && collectedImages.size() >= MIN_PERSON_IMAGES_COUNT) {
            final List<byte[]> list = new ArrayList<>(collectedImages);
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Title");
            final EditText input = new EditText(activity);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final String text = input.getText().toString();
                    faceId.addPerson(list, true, new FaceId.AddPersonCallback() {
                        @Override
                        public void onSuccess(String personId) {
                            currentPersonId = personId;
                            imageCaptureAction.onPersonRecognized(personId);
                            JSONObject info = new JSONObject();
                            try {
                                info.put("info", text);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            faceId.setPersonInfo(personId, info, new FaceId.PersonInfoCallback() {
                                @Override
                                public void onSuccess(JSONObject personInfo) {
                                    if (personInfo.has("info")) {
                                        try {
                                            imageCaptureAction.onPersonInfoReceived(personInfo.getString("info"));
                                        } catch (JSONException e) {
                                            Log.d(TAG, "Error on call FaceId setPersonInfo:" + e.getMessage());
                                        }
                                    } else {
                                        Log.d(TAG, "Error on call FaceId setPersonInfo: Incorrect json data in server response.");
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    Log.d(TAG, "Error on call FaceId setPersonInfo:" + error);
                                }
                            });


                        }

                        @Override
                        public void onError(String error) {
                            Log.d(TAG, "Error on call FaceId addPerson:" + error);
                        }
                    });

                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();

        }
    }
}
