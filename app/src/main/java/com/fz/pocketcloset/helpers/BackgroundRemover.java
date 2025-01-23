package com.fz.pocketcloset.helpers;

import android.util.Log;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.Utils;

public class BackgroundRemover {
    private static final String TAG = "BackgroundRemover";

    public static Mat removeBackground(Mat sourceImage) {
        Log.d(TAG, "removeBackground() called");

        // Check for null or empty input
        if (sourceImage == null || sourceImage.empty()) {
            Log.e(TAG, "Input image is null or empty");
            throw new IllegalArgumentException("Input image is null or empty");
        }
        Log.d(TAG, "Input image loaded successfully. Size: " + sourceImage.size());

        // Resize image to 30% of its original size to improve speed
        Mat resizedImage = new Mat();
        Imgproc.resize(sourceImage, resizedImage, new Size(sourceImage.cols() * 0.3, sourceImage.rows() * 0.3));
        Log.d(TAG, "Resized image for faster processing. New size: " + resizedImage.size());

        // Ensure the image has 3 channels (CV_8UC3 format)
        if (resizedImage.channels() != 3) {
            Log.d(TAG, "Converting resized image to 3-channel format (CV_8UC3)");
            Imgproc.cvtColor(resizedImage, resizedImage, Imgproc.COLOR_BGRA2BGR);
        }

        Mat mask = new Mat();
        Mat bgModel = new Mat();
        Mat fgModel = new Mat();

        // Define a rectangle for GrabCut
        Rect rect = new Rect(10, 10, resizedImage.cols() - 20, resizedImage.rows() - 20);
        Log.d(TAG, "GrabCut rectangle defined: " + rect.toString());

        try {
            Log.d(TAG, "Running GrabCut...");
            Imgproc.grabCut(resizedImage, mask, rect, bgModel, fgModel, 5, Imgproc.GC_INIT_WITH_RECT);
            Log.d(TAG, "GrabCut completed successfully");
        } catch (CvException e) {
            Log.e(TAG, "Error during GrabCut execution: " + e.getMessage(), e);
            return null;
        }

        Log.d(TAG, "Processing mask to extract foreground...");

        // Create a transparent Mat with alpha channel (4 channels)
        Mat transparentImage = new Mat(resizedImage.size(), CvType.CV_8UC4);

        // Apply the mask and add transparency
        for (int row = 0; row < resizedImage.rows(); row++) {
            for (int col = 0; col < resizedImage.cols(); col++) {
                double[] pixel = resizedImage.get(row, col);
                double[] maskValue = mask.get(row, col);

                // If pixel is foreground (GC_PR_FGD), copy with alpha 255
                if (maskValue[0] == Imgproc.GC_PR_FGD || maskValue[0] == Imgproc.GC_FGD) {
                    transparentImage.put(row, col, new double[]{pixel[0], pixel[1], pixel[2], 255});
                } else {
                    // If background, set alpha to 0
                    transparentImage.put(row, col, new double[]{0, 0, 0, 0});
                }
            }
        }

        // Resize back to original size
        Mat finalImage = new Mat();
        Imgproc.resize(transparentImage, finalImage, new Size(sourceImage.cols(), sourceImage.rows()));
        Log.d(TAG, "Resized the processed image back to the original size.");

        return finalImage;
    }

}
