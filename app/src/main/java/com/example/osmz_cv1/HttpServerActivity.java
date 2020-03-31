package com.example.osmz_cv1;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
//import android.graphics.ImageFormat;
//import android.graphics.Matrix;
//import android.graphics.Rect;
//import android.graphics.YuvImage;
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;

public class HttpServerActivity extends Activity implements OnClickListener{

	private SocketServer s;
	private double sendSize = 0;
	public static int maxAvailable = 1;
	private Intent intent;
	public static Handler handler;
	public static FrameLayout preview;
    public static final Camera mCamera = getCameraInstance();
    public static CameraPreview mPreview;
	//private Handler cameraHandler = new Handler();
	
    @SuppressLint("HandlerLeak")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_http_server);
        
        Button btn1 = (Button)findViewById(R.id.button1);
        Button btn2 = (Button)findViewById(R.id.button2);


        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, this.mCamera, this.mPicture);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        mCamera.startPreview();

		preview = (FrameLayout) findViewById(R.id.camera_preview);

		intent = new Intent(this, SocketServerService.class);

		//boundary fix
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		TextView handleTitle = (TextView)findViewById(R.id.handleTitle);
		handleTitle.setTextColor(Color.parseColor("#111111"));

		final TextView text = (TextView) findViewById(R.id.threadsLog);
		final TextView textSize = (TextView) findViewById(R.id.textSize);
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

		//region Message Handler
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				Bundle bundle = msg.getData();
				String typeOfRequest = bundle.getString("REQUEST");
				String name = bundle.getString("NAME");
				Float size = bundle.getFloat("SIZE");

				// get current time
				Date currentTime = Calendar.getInstance().getTime();

				// Total size counter
				sendSize += size;
				double totalSize = 0;
				String totalSizeExtension;
				if (sendSize > 999) {
					totalSize = (sendSize / 1024);
					totalSizeExtension = "KB";
				} else if (sendSize / 1024 > 999) {
					totalSize = (sendSize / 1024) / 1024;
					totalSizeExtension = "MB";
				} else {
					totalSizeExtension = "B";
				}
				textSize.setText("Velikost přenesených dat: " + roundTwoDecimals(totalSize) + totalSizeExtension);

				// Thread log
				String sizeExtension;
				if (size > 999) {
					size = (size / 1024);
					sizeExtension = "KB";
				} else if (size / 1024 > 999) {
					size = (size / 1024) / 1024;
					sizeExtension = "MB";
				} else {
					sizeExtension = "B";
				}
				text.setText("At: " + currentTime + "\n" + typeOfRequest + "\t" + name + "\n" + "Velikost souboru" + "\t" + roundTwoDecimals(size) + sizeExtension + "\t" + "\n" + "\n" + text.getText());
			}
		};
		//endregion
    }

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (v.getId() == R.id.button1) {
			startService(intent);
			//savePictures();
		}
		if (v.getId() == R.id.button2) {

			//stop services
			stopService(intent);
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
/* ukol 1 ukládání obrázku
	public void savePictures(){
		try {
			mCamera.takePicture(null, null, mPicture);
		} catch (Exception e) {
			Log.d("ERROR", "save error: " + e.getLocalizedMessage());
		}

		cameraHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				savePictures();
			}
		}, 5000);
	}

 */
}

