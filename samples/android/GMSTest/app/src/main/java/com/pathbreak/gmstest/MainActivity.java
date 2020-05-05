/**
 * Android app to demonstrate OpenCV xfeatures2d's GMS (Grid-based Motion Statistics)
 * feature correspondence. Ref: http://jwbian.net/gms
 *
 * This example is based on xfeatures2d GMS example at
 *   https://github.com/opencv/opencv_contrib/blob/master/modules/xfeatures2d/samples/gms_matcher.cpp.
 *
 * Sample images in res/raw are from https://github.com/JiawangBian/GMS-Feature-Matcher.
 */

package com.pathbreak.gmstest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.xfeatures2d.Xfeatures2d;

import java.util.concurrent.Callable;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "gmstest";

    private ImageView mImageView;
    private Button mLeftBtn;
    private Button mRightBtn;
    private Button mRunBtn;

    private Mat mLeftImg;
    private Bitmap mLeftBm;

    private Mat mRightImg;
    private Bitmap mRightBm;

    private Mat mResultImg;
    private Bitmap mResultBm;

    private Context mCtx;

    public MainActivity() {

        System.loadLibrary("opencv_java4");

        mCtx = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = findViewById(R.id.imageView);

        mLeftBtn = findViewById(R.id.btnLeft);
        mLeftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadLeftImage();
            }
        });

        mRightBtn = findViewById(R.id.btnRight);
        mRightBtn.setEnabled(false);
        mRightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadRightImage();
            }
        });

        mRunBtn = findViewById(R.id.btnRun);
        mRunBtn.setEnabled(false);
        mRunBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                match();
            }
        });


    }

    private void loadLeftImage() {
        if (mLeftImg != null) {
            mImageView.setImageBitmap(mLeftBm);
            return;
        }
        final LoadImageTask t = new LoadImageTask();
        t.rsrcId = R.raw.img1;
        t.callback = new Callable() {
            @Override
            public Object call() throws Exception {
                mLeftImg = t.m;
                mLeftBm = t.bm;
                mImageView.setImageBitmap(mLeftBm);
                mRightBtn.setEnabled(true);
                return null;
            }
        };
        loadAndDisplayImage(t);
    }

    private void loadRightImage() {
        if (mRightImg != null) {
            mImageView.setImageBitmap(mRightBm);
            return;
        }
        final LoadImageTask t = new LoadImageTask();
        t.rsrcId = R.raw.img2;
        t.callback = new Callable() {
            @Override
            public Object call() throws Exception {
                mRightImg = t.m;
                mRightBm = t.bm;
                mImageView.setImageBitmap(mRightBm);
                mRunBtn.setEnabled(true);
                return null;
            }
        };
        loadAndDisplayImage(t);
    }

    private void match() {
        if (mResultBm != null) {
            mImageView.setImageBitmap(mResultBm);
            return;
        }

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                int nFeatures = 10000;
                int fastThreshold = 20;
                boolean withRotation = false;
                boolean withScale = false;

                ORB orbDetector = ORB.create(
                        nFeatures,
                        1.2f, // scaleFactor=1.2f,
                        8, // int nlevels=8,
                        31, // int edgeThreshold=31,
                        0, //int firstLevel=0,
                        2, // int WTA_K=2,
                        ORB.HARRIS_SCORE, // ORB::ScoreType scoreType=ORB::HARRIS_SCORE,
                        31, // int patchSize=31,
                        fastThreshold
                );


                Mat noMask = new Mat();

                MatOfKeyPoint leftKP = new MatOfKeyPoint();
                Mat leftDesc = new Mat();
                orbDetector.detectAndCompute(mLeftImg, noMask, leftKP, leftDesc);

                MatOfKeyPoint rightKP = new MatOfKeyPoint();
                Mat rightDesc = new Mat();
                orbDetector.detectAndCompute(mRightImg, noMask, rightKP, rightDesc);

                DescriptorMatcher descMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
                MatOfDMatch matchesAll = new MatOfDMatch();
                descMatcher.match(leftDesc, rightDesc, matchesAll);

                MatOfDMatch matchesGMS = new MatOfDMatch();
                Xfeatures2d.matchGMS(mLeftImg.size(), mRightImg.size(), leftKP, rightKP,
                        matchesAll, matchesGMS,
                        withRotation, withScale);


                mResultImg = new Mat();
                Features2d.drawMatches(
                        mLeftImg, leftKP, mRightImg, rightKP, matchesGMS,
                        mResultImg,
                        Scalar.all(-1), //default matchColor
                        Scalar.all(-1), //default single point color
                        new MatOfByte() /*no mask*/,
                        Features2d.DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS);

                mResultBm = Bitmap.createBitmap(mResultImg.cols(), mResultImg.rows(), Bitmap.Config.ARGB_8888);

                Utils.matToBitmap(mResultImg, mResultBm);

                return null;

            }

            @Override
            protected void onPostExecute(Void aVoid) {
                mImageView.setImageBitmap(mResultBm);
            }
        }.execute();

    }

    private AsyncTask loadAndDisplayImage(final LoadImageTask theTask) {
        AsyncTask<LoadImageTask, Integer, LoadImageTask> task = new AsyncTask<LoadImageTask, Integer, LoadImageTask>() {
            @Override
            protected LoadImageTask doInBackground(LoadImageTask... tasks) {
                try {
                    theTask.m = Utils.loadResource(mCtx, theTask.rsrcId);

                    theTask.bm = Bitmap.createBitmap(theTask.m.cols(), theTask.m.rows(), Bitmap.Config.ARGB_8888);

                    Utils.matToBitmap(theTask.m, theTask.bm);

                    return theTask;
                } catch (Exception e) {
                    Log.e(TAG, "image loading failed", e);
                }

                return null;
            }

            @Override
            protected void onPostExecute(LoadImageTask theTask) {
                try {
                    theTask.callback.call();
                } catch (Exception e) {
                    Log.e(TAG, "error during callback", e);
                }

            }
        };

        task.execute(theTask);
        return task;
    }

    class LoadImageTask {
        int rsrcId;
        Mat m;
        Bitmap bm;
        Callable callback;
    }

    class MatchTask {
        Mat resImg;
        Bitmap resBm;
        Callable callback;
    }
}
