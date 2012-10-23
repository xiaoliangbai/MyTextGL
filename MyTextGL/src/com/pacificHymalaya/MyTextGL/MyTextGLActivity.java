package com.pacificHymalaya.MyTextGL;


import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;

public class MyTextGLActivity extends Activity {
	private GLSurfaceView mGLView;

	/** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

     // Check if the system supports OpenGL ES 2.0.
     		final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
     		final ConfigurationInfo configurationInfo = activityManager
     				.getDeviceConfigurationInfo();
     		final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;
     		if (supportsEs2) {
     			// Create a GLSurfaceView instance and set it
     			// as the ContentView for this Activity.
     			mGLView = new MyTextGLSurfaceView(this);
     			setContentView(mGLView);
     		}
    }

	@Override
	protected void onResume() {
		super.onResume();
		mGLView.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mGLView.onPause();
	}
}
	
	class MyTextGLSurfaceView extends GLSurfaceView  {

		private final TextGLRenderer mTextGLRenderer;
		private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
		private float mPreviousX;
		private float mPreviousY;
		private Vibrator mVibrator;
		private int mVibrationLength = 50; //50 milliseconds
		private float mZoomStep = 0.53f; //use prime number to avoid overlapping with control
		
		public MyTextGLSurfaceView(Context context) {
			super(context);

			setEGLContextClientVersion(2);
			// Set the Renderer for drawing on the GLSurfaceView
			mTextGLRenderer = new TextGLRenderer(context);
			setRenderer(mTextGLRenderer);
			mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

		}

		
	}