package com.example.osmz_cv1;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.os.Message;
import android.os.StrictMode;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class HttpServerActivity extends Activity implements OnClickListener{

	private SocketServer s;
	private double sendSize = 0;
	private int maxAvailable = 1;
	private Camera mCamera;
	private CameraPreview mPreview;
	private Timer timer;
	private TimerTask timerTask;
	private byte[] imageBuffer;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_http_server);
        
        Button btn1 = (Button)findViewById(R.id.button1);
        Button btn2 = (Button)findViewById(R.id.button2);

        /*
         * Camera init
         */
		mCamera = getCameraInstance();
		mCamera.setPreviewCallback(mPrevCall);
		// Create our Preview view and set it as the content of our activity.
		mPreview = new CameraPreview(this, this.mCamera, this.mPicture);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mPreview);
		mCamera.startPreview();
		//boundary fix
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		// Task with save images
		Button captureButton = (Button) findViewById(R.id.button_capture);
		captureButton.setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						startTimer();}
				}
		);

		Button stopButton = (Button) findViewById(R.id.button_capture_stop);
		stopButton.setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						stoptimertask(v);}
				}
		);

		TextView handleTitle = (TextView)findViewById(R.id.handleTitle);
		handleTitle.setTextColor(Color.parseColor("#111111"));

		TextView textSize = (TextView) findViewById(R.id.textSize);
		textSize.setTextColor(Color.parseColor("#111111"));

		TextView threadsText = (TextView) findViewById(R.id.numberThreads);
		threadsText.setTextColor(Color.parseColor("#111111"));
         
        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);

        // Set number of threads
		NumberPicker np = (NumberPicker) findViewById(R.id.np);
		np.setMinValue(1);
		np.setMaxValue(10);
		np.setWrapSelectorWheel(true);
		np.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
			@Override
			public void onValueChange(NumberPicker picker, int oldVal, int newVal){
				maxAvailable = newVal;
			}
		});
    }

    //region Message Handler
	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();

			TextView text = (TextView)findViewById(R.id.threadsLog);
			TextView textSize = (TextView) findViewById(R.id.textSize);

			String typeOfRequest = bundle.getString("REQUEST");
			String name = bundle.getString("NAME");
			Float size = bundle.getFloat("SIZE");

			// get current time
			Date currentTime = Calendar.getInstance().getTime();

			// Total size counter
			sendSize += size;
			double totalSize = 0;
			String totalSizeExtension;
			if(sendSize > 999) {
				totalSize = (sendSize/1024);
				totalSizeExtension = "KB";
			}
			else if (sendSize/1024 > 999){
				totalSize = (sendSize/1024)/1024;
				totalSizeExtension = "MB";
			}
			else {
				totalSizeExtension = "B";
			}
			textSize.setText("Velikost přenesených dat: " +  roundTwoDecimals(totalSize) + totalSizeExtension);

			// Thread log
			String sizeExtension;
			if(size > 999) {
				size = (size/1024);
				sizeExtension = "KB";
			}
			else if (size/1024 > 999){
				size = (size/1024)/1024;
				sizeExtension = "MB";
			}
			else {
				sizeExtension = "B";
			}
			text.setText("At: " + currentTime + "\n" + typeOfRequest + "\t" + name + "\n" + "Velikost souboru" + "\t" + roundTwoDecimals(size) + sizeExtension + "\t" + "\n" + "\n" + text.getText());
		}
	};
	//endregion

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (v.getId() == R.id.button1) {
			s = new SocketServer(this.handler, this.maxAvailable, this);
			s.start();
		}
		if (v.getId() == R.id.button2) {
			s.close();
			try {
				s.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	double roundTwoDecimals(double d) {
		DecimalFormat twoDForm = new DecimalFormat("#.##");
		return Double.valueOf(twoDForm.format(d));
	}

	public static Camera getCameraInstance(){
		Camera c = null;
		try {
			c = Camera.open();
		}
		catch (Exception e){
			Log.d("SERVER", "Camera fail");
		}
		return c;
	}

	//region Timer for take and save picture
	public void startTimer() {
		timer = new Timer();
		initializeTimerTask();
		timer.schedule(timerTask, 1000, 5000); //
	}

	public void stoptimertask(View v) {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	public void initializeTimerTask() {
		timerTask = new TimerTask() {
			public void run() {
				Log.d("SERVER", "Picture taken");
				mCamera.takePicture(null, null, mPicture);

				//use a handler to run a toast that shows the current timestamp
				handler.post(new Runnable() {
					public void run() {
						//get the current timeStamp
						Calendar calendar = Calendar.getInstance();
						SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd:MMMM:yyyy HH:mm:ss a");
						final String strDate = simpleDateFormat.format(calendar.getTime());
						//show the toast
						int duration = Toast.LENGTH_SHORT;
						Toast toast = Toast.makeText(getApplicationContext(), strDate, duration);
						toast.show();
					}
				});
			}
		};
	}
	//endregion

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

	//region /PictureCallback
	private Camera.PictureCallback mPicture = new Camera.PictureCallback()
	{
		@Override
		public void onPictureTaken(byte[] data, Camera camera)
		{
			mCamera.startPreview();

			File pictureFile = new File(Environment.getExternalStorageDirectory().getPath() + "/OSMZ" + File.separator + "snapchot.jpg");

			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();
			} catch (FileNotFoundException e) {
				Log.d("CAMERA", "File not found: " + e.getMessage());
			} catch (IOException e) {
				Log.d("CAMERA", "Error accessing file: " + e.getMessage());
			}
		}
	};
	//endregion

	public byte[] takePicture() {
		Bitmap rotateImageData = BitmapFactory.decodeByteArray(imageBuffer, 0, imageBuffer.length);
		rotateImageData = rotate(rotateImageData, 90);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		rotateImageData.compress(Bitmap.CompressFormat.JPEG,100,stream);
		return stream.toByteArray();
	}
}

