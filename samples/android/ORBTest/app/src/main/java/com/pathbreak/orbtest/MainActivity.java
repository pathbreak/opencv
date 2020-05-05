/**
 * Android app to demonstrate OpenCV features2d's ORB feature extraction and correspondence.
 *
 * This example is based on features2d scala example at
 *   https://github.com/opencv/opencv/blob/master/samples/java/sbt/src/main/scala/ScalaCorrespondenceMatchingDemo.scala
 *
 * Sample images in res/raw are from https://github.com/opencv/opencv/tree/master/samples/java/sbt/src/main/resources
 */

package com.pathbreak.orbtest;

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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;

import java.io.IOException;
import java.util.concurrent.Callable;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "orbtest";

    private ImageView mImageView;

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

        Button leftBtn = findViewById(R.id.btnLeft);
        leftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadLeftImage();
            }
        });

        Button rightBtn = findViewById(R.id.btnRight);
        rightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadRightImage();
            }
        });

        Button runBtn = findViewById(R.id.btnRun);
        runBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                match();            }
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
                ORB orbDetector = ORB.create(30);

                MatOfKeyPoint leftKP = new MatOfKeyPoint();
                orbDetector.detect(mLeftImg, leftKP);
                Mat leftDesc = new Mat();
                orbDetector.compute(mLeftImg, leftKP, leftDesc);

                MatOfKeyPoint rightKP = new MatOfKeyPoint();
                orbDetector.detect(mRightImg, rightKP);
                Mat rightDesc = new Mat();
                orbDetector.compute(mRightImg, rightKP, rightDesc);

                Log.i(TAG, "# of left descriptors=" + leftDesc.rows()+" x " + leftDesc.cols());
                Log.i(TAG, "# of right descriptors=" + rightDesc.rows()+" x " + rightDesc.cols());

                DescriptorMatcher descMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
                MatOfDMatch dmatches = new MatOfDMatch();
                descMatcher.match(leftDesc, rightDesc, dmatches);

                Log.i(TAG, "# of matches=" + dmatches.rows() + " x " + dmatches.cols());

                mResultImg = new Mat();
                Features2d.drawMatches(mLeftImg, leftKP, mRightImg, rightKP, dmatches, mResultImg);

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
