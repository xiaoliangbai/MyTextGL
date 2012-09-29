package com.pacificHymalaya.MyTextGL;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.util.FloatMath;
import android.opengl.GLES20;
import javax.microedition.khronos.opengles.GL10;
import android.util.Log;

public class SpriteBatch {

	final private static String TAG ="SpriteBatch";  
   //--Constants--//
   final static int VERTEX_SIZE = 9;                  // Vertex Size (in Components) ie. (X,Y,Z, U,V,R,B,G,A)
   final static int VERTICES_PER_SPRITE = 4;      // Vertices Per Sprite
   final static int INDICES_PER_SPRITE = 6;        // Indices Per Sprite

   //--Members--//
   Vertices vertices;                                      // Vertices Instance Used for Rendering
   float[] vertexBuffer;                                  // Vertex Buffer
   float[] posBuffer;								      // Vertex position buffer
   float[] colorBuffer; 							          // Vertex color buffer
   float[] textureCoordBuffer;						  // Vertex texture coordinate buffer
   int posIndex;
   int colorIndex;
   int textureIndex;
   int bufferIndex;                                        // Vertex Buffer Start Index
   int maxSprites;                                        // Maximum Sprites Allowed in Buffer
   int numSprites;                                        // Number of Sprites Currently in Buffer
   
   

   float[] colorV; // color vector, RGBA
   float[] mMvpMatrix; //mvp matrix
   int mVertexProgramHandle; //program handler
   int mTextureId;
   
   //--Constructor--//
   // D: prepare the sprite batcher for specified maximum number of sprites
   // A: maxSprites - the maximum allowed sprites per batch
   //     programHandle - handle of the compiled and linked gl program
   //     mvpMatrix - reference to the MVP matrix
   public SpriteBatch( int maxSprites, int programHandle, float [] mvpMatrix)  {
	  this.mVertexProgramHandle = programHandle;
	  this.mMvpMatrix = mvpMatrix;
      this.vertexBuffer = new float[maxSprites * VERTICES_PER_SPRITE * VERTEX_SIZE];  // Create Vertex Buffer
      this.posBuffer = new float[maxSprites * INDICES_PER_SPRITE * 3];
      this.colorBuffer = new float[maxSprites * INDICES_PER_SPRITE *4];
      this.textureCoordBuffer= new float [maxSprites * INDICES_PER_SPRITE * 2];
      this.vertices = new Vertices( programHandle, mvpMatrix, maxSprites * VERTICES_PER_SPRITE, maxSprites * INDICES_PER_SPRITE, true, true, false );  // Create Rendering Vertices
      this.bufferIndex = 0;                            // Reset Buffer Index
      this.maxSprites = maxSprites;             // Save Maximum Sprites
      this.numSprites = 0;                            // Clear Sprite Counter

      short[] indices = new short[maxSprites * INDICES_PER_SPRITE];  // Create Temp Index Buffer
      int len = indices.length;                 // Get Index Buffer Length
      short j = 0;                                    // Counter
    // FOR Each Index Set (Per Sprite)
      for ( int i = 0; i < len; i+= INDICES_PER_SPRITE, j += VERTICES_PER_SPRITE )  {  
         indices[i + 0] = (short)( j + 0 );           // Calculate Index 0
         indices[i + 1] = (short)( j + 1 );           // Calculate Index 1
         indices[i + 2] = (short)( j + 2 );           // Calculate Index 2
         indices[i + 3] = (short)( j + 2 );           // Calculate Index 3
         indices[i + 4] = (short)( j + 3 );           // Calculate Index 4
         indices[i + 5] = (short)( j + 0 );           // Calculate Index 5
      }
      vertices.setIndices( indices, 0, len );         // Set Index Buffer for Rendering
   }

   //--Begin Batch--//
   // D: signal the start of a batch. set the texture and clear buffer
   // NOTE: the overloaded (non-texture) version assumes that the texture is already bound!
   // A: textureId - the ID of the texture to use for the batch
   // A: colorV - the color vector, R,B,G,A
   // R: [none]
   public void beginBatch(int textureId, float[] colorV)  {
   // GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
   // GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId); //Bind the Texture
   // gl.glBindTexture( GL10.GL_TEXTURE_2D, textureId );  // Bind the Texture
      numSprites = 0;                               // Empty Sprite Counter
      bufferIndex = 0;                               // Reset Buffer Index (Empty)
      this.colorV = colorV;							 //point to current color vector
      this.vertices.setTextureId(textureId);
      this.mTextureId = textureId;
      
      posIndex = 0;
      textureIndex = 0;
      colorIndex = 0;
   }
   

   //--End Batch--//
   // D: signal the end of a batch. render the batched sprites
   // A: [none]
   // R: [none]
   public void endBatch()  {
	// IF Any Sprites to Render
      if ( numSprites > 0 )  {
  		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
  		  //To debug, turned off either element or array method
  		 //element method (faster)
  		 // Set Vertices from Buffer
         vertices.setVertices( vertexBuffer, 0, bufferIndex );  
         vertices.bind();                             // Bind Vertices
         vertices.draw( GLES20.GL_TRIANGLES, 0, numSprites * INDICES_PER_SPRITE );  // Render Batched Sprites
         vertices.unbind();                           // Unbind Vertices
         
         //array method
         //draw();
 		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
      }
   }

   public void draw() {
		/** How many bytes per float. */
		int mBytesPerFloat = 4;
		FloatBuffer mPositions;
		FloatBuffer mColors;
		FloatBuffer mTextureCoordinates;
		int mvpMatrixHandle;
		int vertexPositionHandle;
		int vertexColorHandle;
		int textureCoordHandle;
		int textureUniformHandle;
		
		mPositions = ByteBuffer
				.allocateDirect(maxSprites * INDICES_PER_SPRITE * 3 * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mPositions.put(posBuffer).position(0);
		
		mColors = ByteBuffer
				.allocateDirect(maxSprites * INDICES_PER_SPRITE * 4  * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mColors.put(colorBuffer).position(0);
		
		mTextureCoordinates = ByteBuffer
				.allocateDirect(
						maxSprites * INDICES_PER_SPRITE * 2  * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mTextureCoordinates.put(textureCoordBuffer).position(0);
	   
		GLES20.glUseProgram(mVertexProgramHandle);
		mvpMatrixHandle = GLES20.glGetUniformLocation(mVertexProgramHandle,
					"u_mvpMatrix");
		vertexPositionHandle =  GLES20.glGetAttribLocation(mVertexProgramHandle,
					"a_position");
		vertexColorHandle =  GLES20.glGetAttribLocation(mVertexProgramHandle,
					"a_color");   
		textureCoordHandle =  GLES20.glGetAttribLocation(mVertexProgramHandle,
					"a_texCoord");   
		textureUniformHandle = GLES20.glGetUniformLocation(mVertexProgramHandle, "s_texture");
		// Pass in the mvp matrix
		GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mMvpMatrix, 0);
		TextGLRenderer.checkGlError("TextGL Array checking error");
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
		GLES20.glUniform1i(textureUniformHandle, 0);
		// Pass in the position information
		mPositions.position(0);
		GLES20.glVertexAttribPointer(vertexPositionHandle, 3,
				GLES20.GL_FLOAT, false, 0, mPositions);
		GLES20.glEnableVertexAttribArray(vertexPositionHandle);
		// Pass in the color information
		mColors.position(0);
		GLES20.glVertexAttribPointer(vertexColorHandle, 4,
				GLES20.GL_FLOAT, false, 0, mColors);
		GLES20.glEnableVertexAttribArray(vertexColorHandle);
		// Pass in the texture coordinate information
		mTextureCoordinates.position(0);
		GLES20.glVertexAttribPointer(textureCoordHandle,
				2, GLES20.GL_FLOAT, false, 0,
				mTextureCoordinates);
		GLES20.glEnableVertexAttribArray(textureCoordHandle);
		GLES20.glDrawArrays(GL10.GL_TRIANGLES, 0, numSprites*INDICES_PER_SPRITE); // Draw Direct (Array)
   }
   //--Draw Sprite to Batch--//
   // D: batch specified sprite to batch. adds vertices for sprite to vertex buffer
   //    NOTE: MUST be called after beginBatch(), and before endBatch()!
   //    NOTE: if the batch overflows, this will render the current batch, restart it,
   //          and then batch this sprite.
   // A: x, y - the x,y position of the sprite (center)
   //    width, height - the width and height of the sprite
   //    region - the texture region to use for sprite
   // R: [none]
   public void drawSprite(float x, float y, float width, float height, 
		   TextureRegion region)  {
      if ( numSprites == maxSprites )  {    // IF Sprite Buffer is Full
         endBatch();                                  // End Batch
         // NOTE: leave current texture bound!!
         numSprites = 0;                           // Empty Sprite Counter
         bufferIndex = 0;                           // Reset Buffer Index (Empty)
      }
      
      float halfWidth = width / 2.0f;                 // Calculate Half Width
      float halfHeight = height / 2.0f;               // Calculate Half Height
      float x1 = x - halfWidth;                       // Calculate Left X
      float y1 = y - halfHeight;                      // Calculate Bottom Y
      float x2 = x + halfWidth;                       // Calculate Right X
      float y2 = y + halfHeight;                      // Calculate Top Y
      float z = -2.0f;
//      Log.d(TAG, "drawSprite x1,x2,y1,y2,z = " 
//      + x1 + ", " + x2 + ", " + y1  + ", " + y2 + "," + z);
      
      // p3(x1,y2)------------p2(x2,y2)
      //     |			  	       |
      //     |				       |
      // p0(x1,y1)------------p1(x2,y1)
      
      //p0
      posBuffer[posIndex++] = x1;
      posBuffer[posIndex++] = y1;
      posBuffer[posIndex++] = z;
      //p1
      posBuffer[posIndex++] = x2;
      posBuffer[posIndex++] = y1;
      posBuffer[posIndex++] = z;
      //p2
      posBuffer[posIndex++] = x2;
      posBuffer[posIndex++] = y2;
      posBuffer[posIndex++] = z;
      //p2
      posBuffer[posIndex++] = x2;
      posBuffer[posIndex++] = y2;
      posBuffer[posIndex++] = z;
      //p3
      posBuffer[posIndex++] = x1;
      posBuffer[posIndex++] = y2;
      posBuffer[posIndex++] = z;
      //p0
      posBuffer[posIndex++] = x1;
      posBuffer[posIndex++] = y1;
      posBuffer[posIndex++] = z;
      
      //p0
      colorBuffer[colorIndex++]=colorV[0];
      colorBuffer[colorIndex++]=colorV[1];
      colorBuffer[colorIndex++]=colorV[2];
      colorBuffer[colorIndex++]=colorV[3];
      //p1
      colorBuffer[colorIndex++]=colorV[0];
      colorBuffer[colorIndex++]=colorV[1];
      colorBuffer[colorIndex++]=colorV[2];
      colorBuffer[colorIndex++]=colorV[3];
      //p2
      colorBuffer[colorIndex++]=colorV[0];
      colorBuffer[colorIndex++]=colorV[1];
      colorBuffer[colorIndex++]=colorV[2];
      colorBuffer[colorIndex++]=colorV[3];
      //p2
      colorBuffer[colorIndex++]=colorV[0];
      colorBuffer[colorIndex++]=colorV[1];
      colorBuffer[colorIndex++]=colorV[2];
      colorBuffer[colorIndex++]=colorV[3];
      //p3
      colorBuffer[colorIndex++]=colorV[0];
      colorBuffer[colorIndex++]=colorV[1];
      colorBuffer[colorIndex++]=colorV[2];
      colorBuffer[colorIndex++]=colorV[3];
      //p0
      colorBuffer[colorIndex++]=colorV[0];
      colorBuffer[colorIndex++]=colorV[1];
      colorBuffer[colorIndex++]=colorV[2];
      colorBuffer[colorIndex++]=colorV[3];
      
      //p0
      textureCoordBuffer[textureIndex++] = region.u1;
      textureCoordBuffer[textureIndex++] = region.v2;
      //p1
      textureCoordBuffer[textureIndex++] = region.u2;
      textureCoordBuffer[textureIndex++] = region.v2;
      //p2
      textureCoordBuffer[textureIndex++] = region.u2;
      textureCoordBuffer[textureIndex++] = region.v1;
      //p2
      textureCoordBuffer[textureIndex++] = region.u2;
      textureCoordBuffer[textureIndex++] = region.v1;
      //p3
      textureCoordBuffer[textureIndex++] = region.u1;
      textureCoordBuffer[textureIndex++] = region.v1;
      //p0
      textureCoordBuffer[textureIndex++] = region.u1;
      textureCoordBuffer[textureIndex++] = region.v2;
      
      
      //p0
      vertexBuffer[bufferIndex++] = x1;               // Add X for Vertex 0
      vertexBuffer[bufferIndex++] = y1;               // Add Y for Vertex 0
      vertexBuffer[bufferIndex++] = z;               // Add Z for Vertex 0
      vertexBuffer[bufferIndex++] = region.u1;        // Add U for Vertex 0
      vertexBuffer[bufferIndex++] = region.v2;        // Add V for Vertex 0
      vertexBuffer[bufferIndex++] = colorV[0];        // Add R for Vertex 0
      vertexBuffer[bufferIndex++] = colorV[1];        // Add G for Vertex 0
      vertexBuffer[bufferIndex++] = colorV[2];        // Add B for Vertex 0
      vertexBuffer[bufferIndex++] = colorV[3];        // Add A for Vertex 0
      
      //p1
      vertexBuffer[bufferIndex++] = x2;               // Add X for Vertex 1
      vertexBuffer[bufferIndex++] = y1;               // Add Y for Vertex 1
      vertexBuffer[bufferIndex++] = z;               // Add Z for Vertex 1
      vertexBuffer[bufferIndex++] = region.u2;        // Add U for Vertex 1
      vertexBuffer[bufferIndex++] = region.v2;        // Add V for Vertex 1
      vertexBuffer[bufferIndex++] = colorV[0];        // Add R for Vertex 1
      vertexBuffer[bufferIndex++] = colorV[1];        // Add G for Vertex 1
      vertexBuffer[bufferIndex++] = colorV[2];        // Add B for Vertex 1
      vertexBuffer[bufferIndex++] = colorV[3];        // Add A for Vertex 1

      //p2
      vertexBuffer[bufferIndex++] = x2;               // Add X for Vertex 2
      vertexBuffer[bufferIndex++] = y2;               // Add Y for Vertex 2
      vertexBuffer[bufferIndex++] = z;               // Add Z for Vertex 2
      vertexBuffer[bufferIndex++] = region.u2;        // Add U for Vertex 2
      vertexBuffer[bufferIndex++] = region.v1;        // Add V for Vertex 2
      vertexBuffer[bufferIndex++] = colorV[0];        // Add R for Vertex 2
      vertexBuffer[bufferIndex++] = colorV[1];        // Add G for Vertex 2
      vertexBuffer[bufferIndex++] = colorV[2];        // Add B for Vertex 2
      vertexBuffer[bufferIndex++] = colorV[3];        // Add A for Vertex 2

      //p3
      vertexBuffer[bufferIndex++] = x1;               // Add X for Vertex 3
      vertexBuffer[bufferIndex++] = y2;               // Add Y for Vertex 3
      vertexBuffer[bufferIndex++] = z;               // Add Z for Vertex 3
      vertexBuffer[bufferIndex++] = region.u1;        // Add U for Vertex 3
      vertexBuffer[bufferIndex++] = region.v1;        // Add V for Vertex 3
      vertexBuffer[bufferIndex++] = colorV[0];        // Add R for Vertex 3
      vertexBuffer[bufferIndex++] = colorV[1];        // Add G for Vertex 3
      vertexBuffer[bufferIndex++] = colorV[2];        // Add B for Vertex 3
      vertexBuffer[bufferIndex++] = colorV[3];        // Add A for Vertex 3
  
      numSprites++;                                   // Increment Sprite Count
   }
}
