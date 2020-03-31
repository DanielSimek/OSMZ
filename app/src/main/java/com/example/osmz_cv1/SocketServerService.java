package com.example.osmz_cv1;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class SocketServerService extends Service {

    //private Looper serviceLooper;
    //private ServiceHandler serviceHandler;
    private Handler handler;
    private SocketServer s;
    private int maxThreads;
    private Camera mCamera;
    private byte[] imageBuffer;

    @Override
    public void onCreate() {
        super.onCreate();
        this.handler = HttpServerActivity.handler;
        this.maxThreads = HttpServerActivity.maxAvailable;
        this.mCamera = HttpServerActivity.mCamera;

        mCamera.setPreviewCallback(mPrevCall);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        this.s = new SocketServer(this.handler, this.maxThreads, this);
        this.s.start();

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.s.close();

        try {
            this.s.join();
        } catch (InterruptedException e) {
            Log.d("Background Service", e.getMessage());
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //region PreviewCallback
    private Camera.PreviewCallback mPrevCall = new Camera.PreviewCallback()
    {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera)
        {
            try {
                imageBuffer = convertoToJpeg(bytes, camera);
            } catch (Exception e) {
                Log.d("ERROR", "convert image error");
            }
        }
    };
    //endregion

    public byte[] convertoToJpeg(byte[] data, Camera camera) {
        YuvImage image = new YuvImage(data, ImageFormat.NV21, camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compressToJpeg(new Rect(0, 0, camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height), 100, baos);//this line decreases the image quality

        return baos.toByteArray();
    }

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.setRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    public byte[] takePicture() {
        Bitmap rotateImageData = BitmapFactory.decodeByteArray(imageBuffer, 0, imageBuffer.length);
        rotateImageData = rotate(rotateImageData, 90);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotateImageData.compress(Bitmap.CompressFormat.JPEG,100,stream);
        return stream.toByteArray();
    }
}
