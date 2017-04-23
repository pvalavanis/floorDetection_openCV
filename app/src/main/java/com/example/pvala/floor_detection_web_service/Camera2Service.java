package com.example.pvala.floor_detection_web_service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import java.util.Random;

import static org.opencv.imgcodecs.Imgcodecs.imdecode;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;


/**
 * Created by Panos on 11/11/2016.
 */

/**************************************************************************************
 * Yueng. The code below allows for the continuous retrieval of frames
 * from the camera. Camera2 API by Google is used. The files are being saved as JPEG's. Also,
 * you will find something you might be unfamiliar with in this code. The unfamiliarity at hand
 * is known as... a... comment. Currently, what you are reading, is a comment. The gray lines
 * in the code, are comments.  They are used to help you understand unfamiliar code,
 * or code you have forgotten all about.
 *
 * This code will take you on a journey of self discovery, self loathing,
 * fear, anxiety, and finally, a false sense of accomplishment.
 *
 * Enjoy!
 * **********************************************************************************/

//class definition for the camera service, extending a service
public class Camera2Service extends Service {

    /*the following variables define the tag for logs, the camera, the camera device,
     * the session for the capture session of the camera,an image reader to handle the image,
     * a handler thread to run the service on a separate thread to not block the ui,
     * and a handler to run with the new thread*/
    private static final String TAG = "Camera2Service";
    private static final int CAMERA = CameraCharacteristics.LENS_FACING_BACK;
    private CameraDevice cameraDevice;
    private CameraCaptureSession session;
    private ImageReader imageReader;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    public static boolean active = false;
    ImageTools mat = new ImageTools();
    private boolean you_are_allowed_to_continue = false;

    private BaseLoaderCallback opencv_callback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCVLoad", "OpenCV loaded successfully");
                    you_are_allowed_to_continue = true;
                    Log.e(TAG,"Current thread is " + Thread.currentThread().getName());

                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);



    /*the following is a callback object for the states of the camera device*/
    private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.i(TAG, "onOpened");

            //sets the cameraDevice variable to the opened camera
            cameraDevice = camera;

            /*the following  gives a surface to the images captured by the camera.
            * this is important so we can display the images later*/
            try {
                cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), sessionStateCallback, null);
            } catch (CameraAccessException e) {
                Log.e(TAG, e.getMessage());
                stopBackgroundThread();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.i(TAG, "onDisconnected");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e(TAG, "onError");
        }

        /* onOpened, onDisconnected, and onError are states the camera
        * device can be in, similar to the states of an app*/
    };

    /*the following is a callback object about the state of the camera capture session*/
    private CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            Log.i(TAG, "onConfigured");

            /*the following creates a session for the camera to
             * repeatedly make requests to capture an image (will go until
             * stopped by the user or app crashes) */
            Camera2Service.this.session = session;
            try {
                session.setRepeatingRequest(createCaptureRequest(), null, null);
            } catch (CameraAccessException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.i(TAG, "onConfiguredFailed");
        }
    };

    /* the following acquires an image reader from the listener. this in turn lets us call the
    * method to acquire the latest image*/
    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {

            /* the following variables are used to convert the data we get to bytes,
            * and re-construct them to finally create and save an image file*/
            Image img;
            File imgFile;
            ByteBuffer buffer;
            byte[] bytes;

            //pretty self explanatory. like, c'mon now. read the line. lazy...
            img = reader.acquireLatestImage();
           /**  onCameraFrame(img);*/

            /*the full code below would also have "if-else" or "else" statements
            * to check for other types of retrieved images/files */
            if (img.getFormat() == ImageFormat.JPEG) {

                if (you_are_allowed_to_continue){
                   // Mat opencv_img = imdecode(Mat,int flags)

                    buffer = img.getPlanes()[0].getBuffer();
                    bytes = new byte[buffer.remaining()];

                    Mat imgMat = new Mat(500, 500, CvType.CV_8UC1);
                    imgMat.put(0, 0, bytes);

                    if (imgMat.empty())
                    {
                        Log.i("OpenCVLoad", "img is null");
                    }
                    else if (!imgMat.empty())
                    {
                        Log.i("OpenCVLoad", "img is NOT null");

                        final long timestamp = System.currentTimeMillis();
                        String dirName = "Cam 2 Pictures";
                        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), dirName);
                        String dirMat = dir.getAbsolutePath() + "/" + timestamp;
                        Log.i("OpenCVLoad", dirMat);

                        //imwrite(dirMat, imgMat);

                        mat.SaveImage(imgMat, timestamp);
                    }
                    /** max images acquired can be. will crash unless images start being removed */
                    img.close();


                }
                //check if we have external storage to write to. if we do, save acquired image
//                if (isExternalStorageWritable())
//                {
//                    /*method to create a new file/item/object and set
//                    it up for a JPEG assignment*/
//                    imgFile = CreateJPEG();
//                    try {
//
//                    /*bytebuffer is a class that allows us to read/write bytes
//                    * here it is used with "Save(... , ...)" to assign these
//                    * collected bytes from the image to the created file from "CreateJpeg()"*/
//                        buffer = img.getPlanes()[0].getBuffer();
//                        bytes = new byte[buffer.remaining()];
//                        buffer.get(bytes);
//                        Save(bytes, imgFile);
//                        img.close();
////                        ImposedImageArray(imgFile);
//                    } catch (Exception e) {
//                        Log.i("Exception e", "ImageFormat.JPEG,,,,,,,,Exception eException e");
//                        e.getStackTrace();
//                    }
//                }
            }
        }
    };

    private void ImposedImageArray(File imageFile) {

        Log.i("IIA", "ImposedImageArray code running");

        //create loop of 1250, and initialize to 0/1 randomly. Superimpose
        //all indexes that have a 1 to the main image
        Random rand = new Random();
        int  n = rand.nextInt(2);

        // Log.i("saveImg", "save code running");
        //  Log.i("abcd", imageFile.getName());

      /** mat.ReadImage(imageFile, imageFile.getName());*/

        onCameraFrame(imageFile, imageFile.getName());

        Log.i("IIA", "code after mat is called in ImposedImageArray");
        //READING


//        //sets up variable to receive bytes
//        OutputStream oOutputStream = null;
//        /* the following try-catch is used to write the bytes of the image to
//        * a file. a new outputstream is created of the imageFile which is passed
//        * into this method, and the bytes passed in are the ones used to make the imageFile a
//        * viewable image. */
//        try {
//            oOutputStream = new FileOutputStream(imageFile);
//            oOutputStream.write(bytes);
//        } catch (Exception ex) {
//            throw new RuntimeException(String.format("%s(%s)", ex.toString(), "ImageGenerator.SaveBytesToFile"));
//        } finally {
//            try {
//                if (oOutputStream != null) {
//                    oOutputStream.close();
//                }
//            } catch (Exception ex) {
//                throw new RuntimeException(String.format("%s(%s)", ex.toString(), "ImageGenerator.SaveBytesToFile"));
//            }
//        }
//
//        //save image to phone
//        try {
//            oOutputStream = new FileOutputStream(imageFile);
//            oOutputStream.write(bytes);
//        } catch (Exception ex) {
//            throw new RuntimeException(String.format("%s(%s)", ex.toString(), "ImageGenerator.SaveBytesToFile"));
//        } finally {
//            try {
//                if (oOutputStream != null) {
//                    oOutputStream.close();
//                }
//            } catch (Exception ex) {
//                throw new RuntimeException(String.format("%s(%s)", ex.toString(), "ImageGenerator.SaveBytesToFile"));
//            }
//        }
       // Log.i("saveImg", "save code running");
      //  Log.i("abcd", imageFile.getName());

       // mat.ReadImage(imageFile, imageFile.getName());
        //READING


    }




    /* the following method creates a file with a JPEG extension. this is done
    * in an attempt to create a file to be ready to receive the bytes of the
    * image acquired*/
    private File CreateJPEG() {

        //name for the future file-to-be
        final long timestamp = System.currentTimeMillis();
        String dirName = "Cam 2 Pictures";
//
//        //this is the location of the saved file
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), dirName);
        if (!dir.exists()) {
            dir.mkdir();
        }
        //formats the name of the file to a string
        String sFilename = String.format("%d.jpg", timestamp);

//        try {
//            mat.ReadImage(dir, sFilename);
//        }
//        catch (Exception ex) {
//            Log.i("READING", "mat failed");
//        }


        //returns the location of the created file and its name
        return new File(dir, sFilename);
    }


    /* the following method saves the acquired bytes to the newly created file from
    * CreateJPEG()*/
    private void Save(byte[] bytes, File imageFile) {

        //sets up variable to receive bytes
        OutputStream oOutputStream = null;

        /* the following try-catch is used to write the bytes of the image to
        * a file. a new outputstream is created of the imageFile which is passed
        * into this method, and the ytes passed in are the ones used to make the imageFile a
        * viewable image. */
        try {
            oOutputStream = new FileOutputStream(imageFile);
            oOutputStream.write(bytes);
        } catch (Exception ex) {
            throw new RuntimeException(String.format("%s(%s)", ex.toString(), "ImageGenerator.SaveBytesToFile"));
        } finally {
            try {
                if (oOutputStream != null) {
                    oOutputStream.close();
                }
            } catch (Exception ex) {
                throw new RuntimeException(String.format("%s(%s)", ex.toString(), "ImageGenerator.SaveBytesToFile"));
            }
        }

        //save image to phone
        try {
            oOutputStream = new FileOutputStream(imageFile);
            oOutputStream.write(bytes);
        } catch (Exception ex) {
            throw new RuntimeException(String.format("%s(%s)", ex.toString(), "ImageGenerator.SaveBytesToFile"));
        } finally {
            try {
                if (oOutputStream != null) {
                    oOutputStream.close();
                }
            } catch (Exception ex) {
                throw new RuntimeException(String.format("%s(%s)", ex.toString(), "ImageGenerator.SaveBytesToFile"));
            }
        }

        Log.i("saveImg", "save code running");


    }



//    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
//
//    static {
//        ORIENTATIONS.append(Surface.ROTATION_0, 90);
//        ORIENTATIONS.append(Surface.ROTATION_90, 0);
//        ORIENTATIONS.append(Surface.ROTATION_180, 270);
//        ORIENTATIONS.append(Surface.ROTATION_270, 180);
//    }


    //state of the app once tapped on
    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate from camera service");

        super.onCreate();
        //intent to be used with the "main" class of the app
        Intent mainActivity = new Intent(this, Camera_Activity.class);
        final Notification.Builder mBuilder = new Notification.Builder(this).setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Camera Service Notification")
                .setContentText("Processing images in the background");
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(Camera_Activity.class);
        stackBuilder.addNextIntent(mainActivity);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);
        startForeground(41413, mBuilder.build());
    }




    /**
     * Return the Camera Id which matches the field CAMERA.
     */
    public String getCamera(CameraManager manager) {
        Log.i(TAG, "getCamera from camera service");

        // getCameraIdList() returns a list of currently connected camera devices
        try {
            for (String cameraId : manager.getCameraIdList()) {

                //getCameraCharacteristics() presents the capabilities of the selected camera
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                Log.i("camera2id", cameraId);

                if (cOrientation == CAMERA) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }


    /* the following starts the intent for working with the main activity. if everything
    * gets setup as it should, our camera will start reading images at the below specified
    * desired format, size, and repeat interval. */
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG,"Current thread is " + Thread.currentThread().getName());
        Log.i(TAG, "onStartCommand from camera service");

        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, opencv_callback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            opencv_callback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        Log.e(TAG,"I am here");

        //background thread started, so we do not block ui thread
        startBackgroundThread();
        active = true;
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            imageReader = ImageReader.newInstance(500, 500, ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
            Log.i(TAG, "onStartCommand");
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            mCameraOpenCloseLock.release();

            /* this shows error because it is looking for a request made to the
            * user to allow access of the camera. this check is done in the main activity file,
            * so we can ignore the presented error.
            *
            * or not, whatever...*/
            manager.openCamera(getCamera(manager), cameraStateCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);
    }


    //state of the app if it is closed or has crashed
    @Override
    public void onDestroy() {
        closeCamera();

    }



    //@Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


//    public File getAlbumStorageDir(String albumName) {
//        // Path is Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),albumName);
//        if (!file.mkdirs()) {
//            // Shows this error also when directory already existed
//            Log.e("Error", "Directory not created");
//        }
//        return file;
//    }

    //checks if there is external storage to write to
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


    //a package of setting and outputs needed to capture an image from the camera device
    private CaptureRequest createCaptureRequest() {
        try {
            //"builds" a request to capture an image
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            /* from the built request to capture an image, here we get the surface
             to be used to project the image on*/
            builder.addTarget(imageReader.getSurface());

            /** this needs to be fixed*/
           // builder.set(CaptureRequest.JPEG_ORIENTATION, characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));


            //returns the "build" request made
            return builder.build();
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    /** Starts a background thread and its {@link Handler}. */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /** Stops the background thread and its {@link Handler}.*/
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * Closes the current {@link CameraDevice}.
     *
     * This method also terminates the CameraDevice and the ImageReader.
     */
    private void closeCamera() {
        Log.i(TAG,"Closing Camera");
        Toast.makeText(this, "stopping service", Toast.LENGTH_SHORT).show();

        try {
            mCameraOpenCloseLock.acquire();
            if (null != session) {
                session.close();
                session = null;
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != imageReader) {
                imageReader.close();
                imageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            stopBackgroundThread();
            mCameraOpenCloseLock.release();
            active = false;
            Log.i(TAG,"Closed Camera");
        }

    }


    public Mat onCameraFrame(File path, String name) {
        Log.i("onCamFrame","mat onCameraFrame called");

        File file = new File(path.getAbsolutePath(), name);
        String filename = file.toString();  //filename is correct

        Log.i("onCamFrame","path: " + path.getAbsolutePath());
        Log.i("onCamFrame","name: " + name);
        Log.i("onCamFrame","Filename: " + filename);

        final Mat img = imread(path.getAbsolutePath());

        if (img.empty())
        {
            Log.i("OpenCVLoad", "img is null");
            return null;
        }

         return (img);
    }
}