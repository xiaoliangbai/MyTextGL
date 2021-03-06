// This is a OpenGL ES 1.0 dynamic font rendering system. It loads actual font
// files, generates a font map (texture) from them, and allows rendering of
// text strings.
//
// NOTE: the rendering portions of this class uses a sprite batcher in order
// provide decent speed rendering. Also, rendering assumes a BOTTOM-LEFT
// origin, and the (x,y) positions are relative to that, as well as the
// bottom-left of the string to render.

package com.pacificHymalaya.MyTextGL;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.content.*;
import android.opengl.GLES20;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLUtils;
import android.os.Environment;
import android.util.Log;

public class GLText {

	private final String TAG = "GLText";
	// --Constants--//
	public final static int CHAR_START = 32; // First Character (ASCII Code)
	public final static int CHAR_END = 126; // Last Character (ASCII Code)
	public final static int CHAR_CNT = (((CHAR_END - CHAR_START) + 1) + 1);
	// Character Count (Including Character to use for Unknown)
	// Character to Use for Unknown (ASCII Code)
	public final static int CHAR_NONE = 32; 
	// Index of the Unknown Character
	public final static int CHAR_UNKNOWN = (CHAR_CNT - 1); 
	// Minumum Font Size (Pixels)
	public final static int FONT_SIZE_MIN = 6;
	// Maximum Font Size (Pixels)
	public final static int FONT_SIZE_MAX = 180; 
	// Number of Characters to Render Per Batch
	public final static int CHAR_BATCH_SIZE = 100; 

	// --Members--//
	Context mContext;
	AssetManager assets; // Asset Manager
	SpriteBatch batch; // Batch Renderer
	// Font Padding (Pixels; On Each Side
	// i.e. Doubled on Both X+Y Axis)
	int fontPadX, fontPadY; 

	float fontHeight; // Font Height (Actual; Pixels)
	float fontAscent; // Font Ascent (Above Baseline; Pixels)
	float fontDescent; // Font Descent (Below Baseline; Pixels)
	// Font Texture ID
	private int textureId;
	 // Texture Size for Font (Square)
	private int textureSize;
	// Full Texture Region
	TextureRegion textureRgn; 

	float charWidthMax; // Character Width (Maximum; Pixels)
	float charHeight; // Character Height (Maximum; Pixels)
	final float[] charWidths; // Width of Each Character (Actual; Pixels)
	TextureRegion[] charRgn; // Region of Each Character (Texture Coordinates)
	int cellWidth, cellHeight; // Character Cell Width/Height
	int rowCnt, colCnt; // Number of Rows/Columns

	float scaleX, scaleY; // Font Scale (X,Y Axis)
	float spaceX; // Additional (X,Y Axis) Spacing (Unscaled)
	float lastX, lastY; //book keeping of the last X,Y
	//holder for current color in {R G B A}
	float[] mCurrentColor; 


	final String mVertexShader = "uniform mat4 u_mvpMatrix; \n"
			+ "attribute vec4 a_position; \n" + "attribute vec2 a_texCoord; \n"
			+ "attribute vec4 a_color; \n" + "varying vec2 v_texCoord; \n"
			+ "varying vec4 v_color; \n" + "void main() { \n"
			+ "  v_texCoord = a_texCoord; \n" + "  v_color = a_color; \n"
			+ "  gl_Position = u_mvpMatrix * a_position; }\n";

	final String mFragmentShader = "precision mediump float; \n"
			+ "uniform sampler2D s_texture; \n" + "varying vec4 v_color;\n"
			+ "varying vec2 v_texCoord; \n" + "void main() { \n"
			+"   vec4 texture = texture2D(s_texture, v_texCoord);\n"
			+ "  gl_FragColor.rgb = v_color.rgb; \n"
			+ "  gl_FragColor.a = texture.a;"
			+ " }\n";

	private float[] mMVPMatrix;

	/** This is a handle to our per-vertex cube shading program. */
	private int mGLTextProgramHandle;

	Bitmap mBitmap;
	
	// --Constructor--//
	public GLText(Context context, float[] mvpMatrix) {
		this.mContext = context;
		this.mMVPMatrix = mvpMatrix;
		this.assets = mContext.getAssets(); 
		this.mCurrentColor = new float[4];
		//Default color -- Red
		this.mCurrentColor[0] = 1.0f; // R
		this.mCurrentColor[1] = 0.0f; // G
		this.mCurrentColor[2] = 0.0f; // B
		this.mCurrentColor[3] = 1.0f; // A
		// Create the Array of Character Widths
		charWidths = new float[CHAR_CNT];
		// Create the Array of Character Regions
		charRgn = new TextureRegion[CHAR_CNT]; 
		// initialize remaining members
		fontPadX = 0;
		fontPadY = 0;
		
		lastX = 0.0f;
		lastY = 0.0f;

		fontHeight = 0.0f;
		fontAscent = 0.0f;
		fontDescent = 0.0f;

		textureId = -1;
		textureSize = 0;

		charWidthMax = 0;
		charHeight = 0;

		cellWidth = 0;
		cellHeight = 0;
		rowCnt = 0;
		colCnt = 0;

		scaleX = 1.0f; // Default Scale = 1 (Unscaled)
		scaleY = 1.0f; // Default Scale = 1 (Unscaled)
		spaceX = 0.0f;
		// load and link shader
		int vertexShader = TextGLRenderer.compileShader(
				GLES20.GL_VERTEX_SHADER, mVertexShader);
		int fragmentShader = TextGLRenderer.compileShader(
				GLES20.GL_FRAGMENT_SHADER, mFragmentShader);
		mGLTextProgramHandle = TextGLRenderer.createAndLinkProgram(
				vertexShader, fragmentShader, new String[] { "a_position",
						"a_texcoord", "a_color" });
		batch = new SpriteBatch(CHAR_BATCH_SIZE, mGLTextProgramHandle,
				mMVPMatrix); // Create Sprite Batch
		// (with Defined Size)
		// Add program to OpenGL ES environment
//		GLES20.glUseProgram(mGLTextProgramHandle);
	}

	// --Load Font--//
	// description
	// this will load the specified font file, create a texture for the defined
	// character range, and setup all required values used to render with it.
	// arguments:
	// file - Filename of the font (.ttf, .otf) to use. In 'Assets' folder.
	// targetFontSize - Requested pixel size of font (height)
	// padX, padY - Extra padding per character (X+Y Axis); to prevent
	// overlapping characters.
	public boolean load(String file, int targetFontSize, int padX, int padY) {

		// setup requested values
		fontPadX = padX; // Set Requested X Axis Padding
		fontPadY = padY; // Set Requested Y Axis Padding

		// load the font and setup paint instance for drawing
		Typeface tf = Typeface.createFromAsset(assets, file); // Create the
																// Typeface from
																// Font File
		Paint paint = new Paint(); // Create Android Paint Instance
		paint.setAntiAlias(true); // Enable Anti Alias
		paint.setTextSize(targetFontSize); // Set Text Size
		paint.setColor(0xffffffff); // Set ARGB (White, Opaque)
		paint.setTypeface(tf); // Set Typeface

		// Get Font Metrics
		Paint.FontMetrics fm = paint.getFontMetrics();
		// Calculate Font Height
		fontHeight = (float) Math.ceil(Math.abs(fm.bottom) + Math.abs(fm.top));
		// Save Font Ascent
		fontAscent = (float) Math.ceil(Math.abs(fm.ascent));
		// Save Font Descent
		fontDescent = (float) Math.ceil(Math.abs(fm.descent));

		// determine the width of each character (including unknown character)
		// also determine the maximum character width
		char[] s = new char[2]; // Create Character Array
		charWidthMax = charHeight = 0; // Reset Character Width/Height Maximums
		float[] w = new float[2]; // Working Width Value
		int cnt = 0; // Array Counter
		for (char c = CHAR_START; c <= CHAR_END; c++) { // FOR Each Character
			s[0] = c; // Set Character
			paint.getTextWidths(s, 0, 1, w); // Get Character Bounds
			charWidths[cnt] = w[0]; // Get Width
			// IF Width Larger Than Max Width
			if (charWidths[cnt] > charWidthMax) 
				charWidthMax = charWidths[cnt]; // Save New Max Width
			cnt++; // Advance Array Counter
		}
		s[0] = CHAR_NONE; // Set Unknown Character
		paint.getTextWidths(s, 0, 1, w); // Get Character Bounds
		charWidths[cnt] = w[0]; // Get Width
		if (charWidths[cnt] > charWidthMax) // IF Width Larger Than Max Width
			charWidthMax = charWidths[cnt]; // Save New Max Width
		cnt++; // Advance Array Counter

		// set character height to font height
		charHeight = fontHeight; // Set Character Height

		// find the maximum size, validate, and setup cell sizes
		cellWidth = (int) charWidthMax + (2 * fontPadX); // Set Cell Width
		cellHeight = (int) charHeight + (2 * fontPadY); // Set Cell Height
		// Save Max Size (Width/Height)
		int maxSize = cellWidth > cellHeight ? cellWidth : cellHeight;
		// IF Maximum Size Outside Valid Bounds
		if (maxSize < FONT_SIZE_MIN || maxSize > FONT_SIZE_MAX)
			return false; // Return Error

		// set texture size based on max font size (width or height)
		// NOTE: these values are fixed, based on the defined characters. when
		// changing start/end characters (CHAR_START/CHAR_END) this will need
		// adjustment too!
		if (maxSize <= 24) // IF Max Size is 18 or Less
			textureSize = 256; // Set 256 Texture Size
		else if (maxSize <= 40) // ELSE IF Max Size is 40 or Less
			textureSize = 512; // Set 512 Texture Size
		else if (maxSize <= 80) // ELSE IF Max Size is 80 or Less
			textureSize = 1024; // Set 1024 Texture Size
		else
			// ELSE IF Max Size is Larger Than 80 (and Less than FONT_SIZE_MAX)
			textureSize = 2048; // Set 2048 Texture Size

		// create an empty bitmap (alpha only)
//		mBitmap = Bitmap.createBitmap(textureSize, textureSize, Bitmap.Config.ALPHA_8);
		mBitmap = Bitmap.createBitmap( textureSize, textureSize, Bitmap.Config.ARGB_8888 ); 
		// Create Canvas for Rendering to Bitmap
		Canvas canvas = new Canvas(mBitmap); 
		mBitmap.eraseColor(0x00000000); // Set Transparent Background (ARGB)
		//draw a rectangle to cover the whole area, for debug purpose
//		paint.setStyle(Paint.Style.STROKE);  
//		canvas.drawRect(0.0f, textureSize, textureSize, 0.0f, paint);
//		paint.setStyle(Paint.Style.FILL_AND_STROKE); 
		// calculate rows/columns
		// NOTE: while not required for anything, these may be useful to have :)
		colCnt = textureSize / cellWidth; // Calculate Number of Columns
		// Calculate Number of Rows
		rowCnt = (int) Math.ceil((float) CHAR_CNT / (float) colCnt);

		// render each of the characters to the canvas (ie. build the font map)
		float x = fontPadX; // Set Start Position (X)
		float y = (cellHeight - 1) - fontDescent - fontPadY; // Set Start
																// Position (Y)
		for (char c = CHAR_START; c <= CHAR_END; c++) { // FOR Each Character
			s[0] = c; // Set Character to Draw
			canvas.drawText(s, 0, 1, x, y, paint); // Draw Character
			x += cellWidth; // Move to Next Character
			// IF End of Line Reached
			if ((x + cellWidth - fontPadX) > textureSize) {
				x = fontPadX; // Set X for New Row
				y += cellHeight; // Move Down a Row
			}
			
		}
		s[0] = CHAR_NONE; // Set Character to Use for NONE
		canvas.drawText(s, 0, 1, x, y, paint); // Draw Character
		saveBitmap("fontMap.png");
		// generate a new texture
		int[] textureIds = new int[1]; // Array to Get Texture Id
		GLES20.glGenTextures(1, textureIds, 0); // Generate New Texture
		textureId = textureIds[0]; // Save Texture Id

		// setup filters for texture
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId); // Bind Texture
		// Set Minification Filter
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		// Set Magnification Filter
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		// Set U Wrapping
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
				GLES20.GL_CLAMP_TO_EDGE);
		// Set V Wrapping
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
				GLES20.GL_CLAMP_TO_EDGE); 
		// load the generated bitmap onto the texture
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
		

		// release the bitmap
		mBitmap.recycle();
		
		// Sanity check
		if (textureId == 0) {
			throw new RuntimeException(
					"Error found during generating texture for dynamic font display. TextureId == 0");
		}

		// setup the array of character texture regions
		x = 0; // Initialize X
		y = 0; // Initialize Y
		for (int c = 0; c < CHAR_CNT; c++) { // FOR Each Character (On Texture)
			 // Create Region for Character
			charRgn[c] = new TextureRegion(textureSize, textureSize, x, y,
					cellWidth - 1, cellHeight - 1);
			x += cellWidth; // Move to Next Char (Cell)
			if (x + cellWidth > textureSize) {
				x = 0; // Reset X Position to Start
				y += cellHeight; // Move to Next Row (Cell)
			}
		}

		// create full texture region
		textureRgn = new TextureRegion(textureSize, textureSize, 0, 0,
				textureSize, textureSize); // Create Full Texture Region

		// return success
		return true; // Return Success
	}

	// --Begin/End Text Drawing--//
	// D: call these methods before/after (respectively all draw() calls using a
	// text instance
	// NOTE: color is set on a per-batch basis, and fonts should be 8-bit alpha
	// only!!!
	// A: red, green, blue - RGB values for font (default = 1.0)
	// alpha - optional alpha value for font (default = 1.0)
	// R: [none]
	public void begin() {
		begin(1.0f, 1.0f, 1.0f, 1.0f); // Begin with White Opaque
	}

	public void begin(float alpha) {
		begin(1.0f, 1.0f, 1.0f, alpha); // Begin with White (Explicit Alpha)
	}

	public void begin(float red, float green, float blue, float alpha) {
		mCurrentColor[0] = red;
		mCurrentColor[1] = green;
		mCurrentColor[2] = blue;
		mCurrentColor[3] = alpha;
		batch.beginBatch(textureId, mCurrentColor); // Begin Batch
	}

	public void setColor(float red, float green, float blue, float alpha) {
		mCurrentColor[0] = red;
		mCurrentColor[1] = green;
		mCurrentColor[2] = blue;
		mCurrentColor[3] = alpha;
	}
	
	public void end() {
		batch.endBatch(); // End Batch
		// restore color to default white
		mCurrentColor[0] = 1.0f;
		mCurrentColor[1] = 1.0f;
		mCurrentColor[2] = 1.0f;
		mCurrentColor[3] = 1.0f;
	}

	// --Draw Text--//
	// D: draw text at the specified x,y position
	// A: text - the string to draw
	// x, y - the x,y position to draw text at (bottom left of text; including
	// descent)
	// R: [none]
	public void draw(String text, float x, float y) {
		// Calculate Scaled Character Height
		float chrHeight = cellHeight * scaleY; 
		// Calculate Scaled Character Width
		float chrWidth = cellWidth * scaleX; 
		int len = text.length(); // Get String Length
		x += (chrWidth / 2.0f) - (fontPadX * scaleX); // Adjust Start X
		y += (chrHeight / 2.0f) - (fontPadY * scaleY); // Adjust Start Y
		for (int i = 0; i < len; i++) { // FOR Each Character in String
			int c = (int) text.charAt(i) - CHAR_START; // Calculate Character
			// Index (Offset by First Char in Font)
			if (c < 0 || c >= CHAR_CNT) // IF Character Not In Font
				c = CHAR_UNKNOWN; // Set to Unknown Character Index
			// Draw the Character
			batch.drawSprite(x, y, chrWidth, chrHeight, charRgn[c]); 
			// Advance X Position by Scaled Character Width
			x += (charWidths[c] + spaceX) * scaleX;
		}
		lastX = x + (fontPadX * scaleX) - (chrWidth / 2.0f);
		lastY = y + (fontPadY * scaleY) - (chrHeight / 2.0f);
	}
	// --Draw Text--//
	// D: continue draw text from last location 
	// A: text - the string to draw
	// R: [none]
	public void draw(String text) {
		// Calculate Scaled Character Height
		float chrHeight = cellHeight * scaleY; 
		// Calculate Scaled Character Width
		float chrWidth = cellWidth * scaleX; 
		int len = text.length(); // Get String Length
		float x = lastX + (chrWidth / 2.0f) - (fontPadX * scaleX); // Adjust Start X
		float y = lastY + (chrHeight / 2.0f) - (fontPadY * scaleY); // Adjust Start Y
		for (int i = 0; i < len; i++) { // FOR Each Character in String
			int c = (int) text.charAt(i) - CHAR_START; // Calculate Character
			// Index (Offset by First Char in Font)
			if (c < 0 || c >= CHAR_CNT) // IF Character Not In Font
				c = CHAR_UNKNOWN; // Set to Unknown Character Index
			// Draw the Character
			batch.drawSprite(x, y, chrWidth, chrHeight, charRgn[c]); 
			// Advance X Position by Scaled Character Width
			x += (charWidths[c] + spaceX) * scaleX;
//			x += (chrWidth + spaceX) * scaleX;
		    Log.d(TAG, "charWidths, chrWidth, spaceX = "+ 
		    	      + charWidths[c] + ", " + chrWidth + ", " + spaceX);
		}
		lastX = x + (fontPadX * scaleX) - (chrWidth / 2.0f);
		lastY = y + (fontPadY * scaleY) - (chrHeight / 2.0f);
	}
	
	
	// --Draw Text Centered--//
	// D: draw text CENTERED at the specified x,y position
	// A: text - the string to draw
	// x, y - the x,y position to draw text at (bottom left of text)
	// R: the total width of the text that was drawn
	public float drawC(String text, float x, float y) {
		float len = getLength(text); // Get Text Length
		// Draw Text Centered
		draw(text, x - (len / 2.0f), y - (getCharHeight() / 2.0f)); 
		return len; // Return Length
	}

	public float drawCX(String text, float x, float y) {
		float len = getLength(text); // Get Text Length
		draw(text, x - (len / 2.0f), y); // Draw Text Centered (X-Axis Only)
		return len; // Return Length
	}

	public void drawCY(String text, float x, float y) {
		draw(text, x, y - (getCharHeight() / 2.0f)); // Draw Text Centered
														// (Y-Axis Only)
	}

	// --Set Scale--//
	// D: set the scaling to use for the font
	// A: scale - uniform scale for both x and y axis scaling
	// sx, sy - separate x and y axis scaling factors
	// R: [none]
	public void setScale(float scale) {
		scaleX = scaleY = scale; // Set Uniform Scale
	}

	public void setScale(float sx, float sy) {
		scaleX = sx; // Set X Scale
		scaleY = sy; // Set Y Scale
	}

	// --Get Scale--//
	// D: get the current scaling used for the font
	// A: [none]
	// R: the x/y scale currently used for scale
	public float getScaleX() {
		return scaleX; // Return X Scale
	}

	public float getScaleY() {
		return scaleY; // Return Y Scale
	}

	// --Set Space--//
	// D: set the spacing (unscaled; ie. pixel size) to use for the font
	// A: space - space for x axis spacing
	// R: [none]
	public void setSpace(float space) {
		spaceX = space; // Set Space
	}

	// --Get Space--//
	// D: get the current spacing used for the font
	// A: [none]
	// R: the x/y space currently used for scale
	public float getSpace() {
		return spaceX; // Return X Space
	}

	// --Get Length of a String--//
	// D: return the length of the specified string if rendered using current settings
	// A: text - the string to get length for
	// R: the length of the specified string (pixels)
	public float getLength(String text) {
		float len = 0.0f; // Working Length
		int strLen = text.length(); // Get String Length (Characters)
		// For Each Character in String (Except Last)
		for (int i = 0; i < strLen; i++) { 
			// Calculate Character Index (Offset by
			// First Char in Font)
			int c = (int) text.charAt(i) - CHAR_START; 
			// Add Scaled Character Width to Total Length
			len += (charWidths[c] * scaleX); 
		}
		// Add Space Length
		len += (strLen > 1 ? ((strLen - 1) * spaceX) * scaleX : 0); 
		return len; // Return Total Length
	}

	// --Get Width/Height of Character--//
	// D: return the scaled width/height of a character, or max character width
	// NOTE: since all characters are the same height, no character index is
	// required!
	// NOTE: excludes spacing!!
	// A: chr - the character to get width for
	// R: the requested character size (scaled)
	public float getCharWidth(char chr) {
		int c = chr - CHAR_START; // Calculate Character Index (Offset by First
									// Char in Font)
		return (charWidths[c] * scaleX); // Return Scaled Character Width
	}

	public float getCharWidthMax() {
		return (charWidthMax * scaleX); // Return Scaled Max Character Width
	}

	public float getCharHeight() {
		return (charHeight * scaleY); // Return Scaled Character Height
	}

	// --Get Font Metrics--//
	// D: return the specified (scaled) font metric
	// A: [none]
	// R: the requested font metric (scaled)
	public float getAscent() {
		return (fontAscent * scaleY); // Return Font Ascent
	}

	public float getDescent() {
		return (fontDescent * scaleY); // Return Font Descent
	}

	public float getHeight() {
		return (fontHeight * scaleY); // Return Font Height (Actual)
	}

	// --Draw Font Texture--//
	// D: draw the entire font texture (NOTE: for testing purposes only)
	// A: width, height - the width and height of the area to draw to. this is
	// used to draw the texture to the top-left corner.
	public void drawTexture(int width, int height) {
		// Begin Batch (Bind Texture)
		float [] colorV = {0.0f, 0.0f, 1.0f, 0.5f};
		batch.beginBatch(textureId, colorV); 
		batch.drawSprite(textureSize/2.0f, 0,
				textureSize, textureSize, textureRgn); // Draw
		batch.endBatch(); // End Batch
	}

	// Save bitmap into file
	// For debug purpose
	private void saveBitmap(String fileName) {
		String mFilePath = Environment.getExternalStorageDirectory().toString() + File.separator.toString() + fileName;
		//write the bytes in file
		try {
			FileOutputStream fo = new FileOutputStream(mFilePath);
			mBitmap.compress(Bitmap.CompressFormat.PNG, 60, fo);
			fo.flush();
			fo.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
}
