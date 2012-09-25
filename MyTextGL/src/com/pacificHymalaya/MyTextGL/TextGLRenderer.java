package com.pacificHymalaya.MyTextGL;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

public class TextGLRenderer implements GLSurfaceView.Renderer {

	private static final String TAG = "MyTestRenderer";
	private final Context mActivityContext;
	private GLText glText; // A GLText Instance

	private int width = 100; // Updated to the Current Width + Height in
								// onSurfaceChanged()
	private int height = 100;

	// model, view, projection matrix
	private final float[] mMVPMatrix = new float[16];
	private final float[] mModelMatrix = new float[16];
	private final float[] mProjMatrix = new float[16];
	private final float[] mVMatrix = new float[16];
	private final float[] mOrthProjMatrix = new float[16];
	
	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {

		// Set the OpenGL viewport to the same size as the surface.
		GLES20.glViewport(0, 0, width, height);
		// Save width and height
		this.width = width; // Save Current Width
		this.height = height; // Save Current Height
		// Create a new perspective projection matrix. The height will stay the
		// same while the width will vary as per aspect ratio.
		final float ratio = (float) width / height;
		final float left = -ratio;
		final float right = ratio;
		final float bottom = -1.0f;
		final float top = 1.0f;
		final float near = 2.0f;
		final float far = 10.0f;
		Matrix.frustumM(mProjMatrix, 0, left, right, bottom, top, near, far);
//		Matrix.orthoM(mOrthProjMatrix, 0, left, right, bottom, top, near, far);
		Matrix.orthoM(mOrthProjMatrix, 0, 0, width, 0, height, near, far);
	}

	@Override
	public void onDrawFrame(GL10 arg0) {
		//setup camera view
		setupCameraView();
		
        // Clear the color buffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, 0.7f*width, 0.2f*height, -2.0f);
		Matrix.multiplyMM(mModelMatrix, 0, mVMatrix, 0, mModelMatrix, 0);
		Matrix.multiplyMM(mMVPMatrix, 0, mOrthProjMatrix, 0, mModelMatrix, 0);
		glText.drawCB();
		
		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.multiplyMM(mModelMatrix, 0, mVMatrix, 0, mModelMatrix, 0);
		Matrix.multiplyMM(mMVPMatrix, 0, mOrthProjMatrix, 0, mModelMatrix, 0);
		glText.drawTexture(width, height); // Draw the Entire Texture
		glText.drawCB();
		
//		glText.drawTexture(1, height/width); // Draw the Entire Texture
		Log.d(TAG, "height = " + height + ", width = " + width);
		// TEST: render some strings with the font
		glText.begin(1.0f, 1.0f, 1.0f, 0.5f); // Begin Text Rendering (Set Color WHITE)
		glText.draw("Test String :)", 0, 0); // Draw Test String
		glText.draw("Line 1", 2, 2); // Draw Test String
		glText.draw("Line 2", 3, 3); // Draw Test String
		glText.end(); // End Text Rendering

		glText.begin(0.5f, 0.0f, 1.0f, 1.0f); // Begin Text Rendering (Set Color
												// BLUE)
		glText.draw("More Lines...", 5, 5); // Draw Test String
		glText.draw("The End.", 5, 5 + glText.getCharHeight()); // Draw Test
																	// String
		glText.end(); // End Text Rendering
		// disable blend
		GLES20.glDisable(GLES20.GL_BLEND);
	}

	private void setupCameraView() {
		// Position the eye in front of the origin.
		final float eyeX = 0.0f;
		final float eyeY = 0.0f;
		final float eyeZ = 1.0f;

		// We are looking toward the distance
		final float lookX = 0.0f;
		final float lookY = 0.0f;
		final float lookZ = -20.0f;

		// Set our up vector. This is where our head would be pointing were we
		// holding the camera.
		final float upX = 0.0f;
		final float upY = 1.0f;
		final float upZ = 0.0f;

		// Set the view matrix. This matrix can be said to represent the camera
		// position.
		// NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination
		// of a model and
		// view matrix. In OpenGL 2, we can keep track of these matrices
		// separately if we choose.
		Matrix.setLookAtM(mVMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ,
				upX, upY, upZ);
	}

	@Override
	public void onSurfaceCreated(GL10 arg0, EGLConfig arg1) {

		// Set the background clear color to black.
		GLES20.glClearColor(0.3f, 0.3f, 0.3f, 1.0f);

		// Use culling to remove back faces.
//		GLES20.glEnable(GLES20.GL_CULL_FACE);

		// Enable depth testing
//		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		glText = new GLText(mActivityContext.getAssets(), mMVPMatrix);

		// Load the font from file (set size + padding), creates the texture
		// NOTE: after a successful call to this the font is ready for
		// rendering!
		glText.load("Roboto-Regular.ttf", 6, 0, 0); // Create Font (Height: 14
														// Pixels / X+Y Padding
														// 2 Pixels)

	}

	// constructor
	TextGLRenderer(final Context activityContext) {
		mActivityContext = activityContext;

	}

	public static void checkGlError(String glOperation) {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			Log.e(TAG, glOperation + ": glError " + error);
			throw new RuntimeException(glOperation + ": glError " + error);
		}
	}

	/**
	 * Helper function to compile and link a program.
	 * 
	 * @param vertexShaderHandle
	 *            An OpenGL handle to an already-compiled vertex shader.
	 * @param fragmentShaderHandle
	 *            An OpenGL handle to an already-compiled fragment shader.
	 * @param attributes
	 *            Attributes that need to be bound to the program.
	 * @return An OpenGL handle to the program.
	 */
	public static int createAndLinkProgram(final int vertexShaderHandle,
			final int fragmentShaderHandle, final String[] attributes) {
		int programHandle = GLES20.glCreateProgram();

		if (programHandle != 0) {
			// Bind the vertex shader to the program.
			GLES20.glAttachShader(programHandle, vertexShaderHandle);

			// Bind the fragment shader to the program.
			GLES20.glAttachShader(programHandle, fragmentShaderHandle);

			// Bind attributes
			if (attributes != null) {
				final int size = attributes.length;
				for (int i = 0; i < size; i++) {
					GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
				}
			}

			// Link the two shaders together into a program.
			GLES20.glLinkProgram(programHandle);

			// Get the link status.
			final int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS,
					linkStatus, 0);

			// If the link failed, delete the program.
			if (linkStatus[0] == 0) {
				Log.e(TAG,
						"Error compiling program: "
								+ GLES20.glGetProgramInfoLog(programHandle));
				GLES20.glDeleteProgram(programHandle);
				programHandle = 0;
			}
		}

		if (programHandle == 0) {
			throw new RuntimeException("Error creating program.");
		}

		return programHandle;
	}

	/**
	 * Helper function to compile a shader.
	 * 
	 * @param shaderType
	 *            The shader type.
	 * @param shaderSource
	 *            The shader source code.
	 * @return An OpenGL handle to the shader.
	 */
	public static int compileShader(final int shaderType,
			final String shaderSource) {
		int shaderHandle = GLES20.glCreateShader(shaderType);
		if (shaderHandle != 0) {
			// Pass in the shader source.
			GLES20.glShaderSource(shaderHandle, shaderSource);

			// Compile the shader.
			GLES20.glCompileShader(shaderHandle);

			// Get the compilation status.
			final int[] compileStatus = new int[1];
			GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS,
					compileStatus, 0);
			// If the compilation failed, delete the shader.
			if (compileStatus[0] == 0) {
				Log.e("compileShader", "Error during compiling shader: "
						+ GLES20.glGetShaderInfoLog(shaderHandle));
				GLES20.glDeleteShader(shaderHandle);
				shaderHandle = 0;
			}
		}
		if (shaderHandle == 0) {
			throw new RuntimeException("Error creating shader.");
		}
		return shaderHandle;
	}

	public static FloatBuffer getFloatBuffer(float[] data) {
		FloatBuffer buf = ByteBuffer.allocateDirect(data.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		buf.put(data).position(0);
		return buf;
	}
}
