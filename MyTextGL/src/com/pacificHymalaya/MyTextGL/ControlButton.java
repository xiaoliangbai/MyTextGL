package com.pacificHymalaya.MyTextGL;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.opengl.GLES20;
import android.util.Log;


public class ControlButton {

	/** Used for debug logs. */
	private static final String TAG = "ControlButton";

	/** Store model data in float buffers */
	private final FloatBuffer mPositions;
	private final FloatBuffer mColors;
	
	/** This will be used to pass in the transformation matrix. */
	private int mMVPMatrixHandle;
	/** This will be used to pass in model position information. */
	private int mPositionHandle;
	private int mColorHandle;
	/** How many bytes per float. */
	private final int mBytesPerFloat = 4;
	/** Size of the position data in elements. */
	private final int mPositionDataSize = 3;
	/** This is a handle to our per-vertex cube shading program. */
	private int mProgramHandle;
	
	float [] mvpMatrix;
	final String mVertexShader = "uniform mat4 u_mvpMatrix; \n"
			+ "attribute vec4 a_position; \n"
			+ "attribute vec4 a_color; \n"
			+ "varying vec4 v_color; \n"
			+ "void main() { \n"
			+ "  v_color = a_color; \n"
			+ "  gl_Position = u_mvpMatrix * a_position; }\n";

	final String mFragmentShader = "precision mediump float; \n" 
			+ "varying vec4 v_color;\n"
			+ "void main() { \n"
			+ "  gl_FragColor = v_color; \n" 
			+ " }\n";
	
	// X, Y, Z
	final float[] mPositionData = {
			// Front face
			-1.0f, 1.0f, 0.0f, 
			-1.0f, -1.0f, 0.0f, 
			1.0f, -1.0f, 0.0f, 
			1.0f, 1.0f, 0.0f };
	//R,G, B, A
	final float[] mColorData = {
			1.0f, 0.0f, 0.0f, 0.5f,
			1.0f, 0.0f, 0.0f, 0.5f,
			1.0f, 0.0f, 1.0f, 0.5f,
			1.0f, 1.0f, 0.0f, 0.5f };
			



	// constructor
	ControlButton() {
		// load and link shader
		int vertexShader = TextGLRenderer.compileShader(
				GLES20.GL_VERTEX_SHADER, mVertexShader);
		int fragmentShader = TextGLRenderer.compileShader(
				GLES20.GL_FRAGMENT_SHADER, mFragmentShader);
		mProgramHandle = TextGLRenderer.createAndLinkProgram(
				vertexShader, fragmentShader, 
				new String[] { "a_position", "a_color" });


		
		// Initialize the buffers.
		mPositions = ByteBuffer
				.allocateDirect(mPositionData.length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mPositions.put(mPositionData).position(0);
		
		mColors = ByteBuffer
				.allocateDirect(mColorData.length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mColors.put(mColorData).position(0);
		

	}// constructor

	public void draw(float[] mvpMatrix, float sizeFactor) {
		// Add program to OpenGL ES environment
		GLES20.glUseProgram(mProgramHandle);
		// Set program handles for cube drawing.
		mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle,
				"u_mvpMatrix");
		mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle,
				"a_position");
		mColorHandle = GLES20.glGetAttribLocation(mProgramHandle,
				"a_color");
		
		float[] finalPosData = new float[12];
		for (int i = 0; i < 12; i++) {
			finalPosData[i] = mPositionData[i] * sizeFactor;
		}
		FloatBuffer posBuf = TextGLRenderer.getFloatBuffer(finalPosData);
		posBuf.position(0);
		GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize,
				GLES20.GL_FLOAT, false, 0, posBuf);
		GLES20.glEnableVertexAttribArray(mPositionHandle);
		mColors.position(0);
		GLES20.glVertexAttribPointer(mColorHandle, 4,
				GLES20.GL_FLOAT, false, 0, mColors);
		GLES20.glEnableVertexAttribArray(mColorHandle);
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
		
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
	}

}
