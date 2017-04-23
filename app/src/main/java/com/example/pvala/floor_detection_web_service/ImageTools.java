package com.example.pvala.floor_detection_web_service;

/**
 * Created by Panos on 11/14/2016.
 */

import android.graphics.ImageFormat;
import android.media.Image;
import android.os.Environment;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.nio.ByteBuffer;

import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.cvtColor;

public class ImageTools {

    private final String TAG_S = "SAVING";
    private final String TAG_R = "READING";
    public static int kval = 0;

//    public Mat ReadImage(File path, String name) {
//
//        File file = new File(path, name);
//        Mat src = null;
//        String filename = file.toString();
//        // 2.4.11
//        src = Imgcodecs.imread(filename, Imgcodecs.CV_LOAD_IMAGE_COLOR);
//        // 3.0.0
//        // src = Imgcodecs.imread(filename, Imgcodecs.CV_LOAD_IMAGE_COLOR);
//
//        if (!src.empty()) {
//            Log.i(TAG_R, "SUCCESS Reading the image " + name);
//            Imgproc.resize(src, src, new Size(320, 240));
//        } else {
//            Log.d(TAG_R, "Fail Reading the image " + name);
//            return null;
//        }
//        return src;
//
//    }

    public  Mat ReadImage(File path, String name) {
        Log.i("readImage", "ReadImage executed");
        String dirName = "Cam 2 Pictures";

        //File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), dirName);

        File file = new File(path.getAbsolutePath(), dirName);


        Mat src = null;
       // Mat src = new Mat();
        src = null;
        String filename = file.toString();  //filename is correct

        /** THIS CAUSES ERROR */
       // src = imread(filename);

        if (!src.empty()) {
            Log.i(TAG_R, "SUCCESS Reading the image " + name);
            Imgproc.resize(src, src, new Size(500, 500));
        } else if (src == null){
            Log.d("wsad", "Fail Reading the image " + name);
            return null;
        }
  //      return src;
        return null;

    }


    public void SaveImage(Mat img, long name) { //type of 'name' was String. Changed to long
        Log.i("OpenCVLoad", "ImageTools: SaveImage");
        String dirName = "Cam 2 Pictures";
        File ph = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), dirName);

        Mat img2 = new Mat(img.size(), img.type());
        String val = "";
        if (kval < 10)
            val = "000" + kval;
        else if (kval >= 10 && kval < 100)
            val = "00" + kval;
        else if (kval >= 100 && kval < 1000)
            val = "0" + kval;
        else
            val = "" + kval;
        String filename = name + val + ".jpg";
        kval++;
        File file = new File(ph, filename);
        if (file.exists())
            file.delete();
        Boolean bool = null;
        String filenm = file.toString();

       // Imgproc.cvtColor(img, img2, Imgproc.COLOR_RGBA2BGR);
       // cvtColor(img, img2, COLOR_BGR2GRAY);

        bool = Imgcodecs.imwrite(filenm, img);

        if (bool == true) {
            Log.i("OpenCVLoad", "ImageTools: SaveImage: Success writing image");
        } else
            Log.d("OpenCVLoad", "ImageTools: SaveImage: Failed to write image");

    }

    public Mat YUV2RGB(Image image){
        Image.Plane[] planes = image.getPlanes();

        byte[] imageData = new byte[image.getWidth() * image.getHeight()
                * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];

        ByteBuffer buffer = planes[0].getBuffer();
        int lastIndex = buffer.remaining();
        buffer.get(imageData, 0, lastIndex);
        int pixelStride = planes[1].getPixelStride();

        for (int i = 1; i < planes.length; i++) {
            buffer = planes[i].getBuffer();
            byte[] planeData = new byte[buffer.remaining()];
            buffer.get(planeData);

            for (int j = 0; j < planeData.length; j += pixelStride) {
                imageData[lastIndex++] = planeData[j];
            }
        }

        Mat yuvMat = new Mat(image.getHeight() + image.getHeight() / 2,
                image.getWidth(), CvType.CV_8UC1);
        yuvMat.put(0, 0, imageData);

        Mat rgbMat = new Mat();
        cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV420p2RGBA);
        return rgbMat;
    }

    public double Distance(Point one, Point two) {
        double theDistance = 0;

        theDistance = Math.sqrt((Math.pow((two.x - one.x), 2)) + (Math.pow((two.y - one.y), 2)));

        return Math.abs(theDistance);
    }

}