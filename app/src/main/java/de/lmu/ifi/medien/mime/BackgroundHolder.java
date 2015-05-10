package de.lmu.ifi.medien.mime;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.content.Context;

/**
 * Stores a previously recorded and averaged background frame permanently, so it doesn't have to be set when the app is in study mode.
 * Provides recording and averaging functionality as well as methods for loading and saving the background frame.
 */
public class BackgroundHolder {
	
	private byte[] mBackground = null;
	
	private boolean mUseSavedBackground = false;
	private boolean mBackgroundAveragingEnabled = false;
	
	private static final double ALPHA = 0.25;
	public static final String FILENAME = ".saved_background";
	
	private static BackgroundHolder instance = null;
	
	
	private BackgroundHolder() { }
	
	/**
	 * Returns the singleton instance of this class
	 * @return Instance
	 */
	public static BackgroundHolder getInstance() {
		if (instance == null) {
			instance = new BackgroundHolder();
		}
		return instance;
	}
	
	/**
	 * Adds a new frame to the averaged background in recording mode
	 * @param buffer Frame (preferably scaled down and converted to HSV, but not necessarily)
	 */
	public void addBackgroundFrame(byte[] buffer) {
		if (mBackground == null) {
			mBackground = buffer;
		}
		else {
			// Average each pixel; use 25% of current background frame and 75% of new background frame
			for (int i = 0; i < mBackground.length; ++i) {
				mBackground[i] = (byte) Math.floor(mBackground[i] * ALPHA + buffer[i] * (1.0-ALPHA));
			}
		}
	}
	
	/**
	 * Returns the current background (either loaded from permanent storage or freshly recorded)
	 * @return Background frame
	 */
	public byte[] getBackground() {
		return mBackground;
	}
	
	/**
	 * Checks if background recording is enabled
	 * @return Background recording enabeld
	 */
	public boolean isBackgroundRecordingEnabled() {
		return mBackgroundAveragingEnabled;
	}
	
	/**
	 * Stars background recording and resets the current background frame
	 */
	public void startBackgroundRecording() {
		mBackground = null;
		mBackgroundAveragingEnabled = true;
	}
	
	/**
	 * Stops background recording
	 */
	public void stopBackgroundRecording() {
		mBackgroundAveragingEnabled = false;
	}
	
	/**
	 * Enables/disables usage of the saved background; corresponds to PreferenceHelper.setUseSavedBackground()
	 * @param enable Enable usage of saved background
	 */
	public void setUseSavedBackground(boolean enable) {
		mUseSavedBackground = enable;
	}
	
	/**
	 * Checks if the saved background shall be used; corresponds to PreferenceHelper.userSavedBackground()
	 * @return Use saved background
	 */
	public boolean useSavedBackground() {
		return mUseSavedBackground;
	}
	
	/**
	 * Permanently saves the recorded background frame (internal storage)
	 * @param ctx Applications context
	 */
	public void save(Context ctx) {
	    try {
	        FileOutputStream fos = ctx.openFileOutput(FILENAME, Context.MODE_PRIVATE);
	        // Save buffer length first
	        int length = mBackground == null ? 0 : mBackground.length;
	        fos.write((length >> 24) & 0xFF);
	        fos.write((length >> 16) & 0xFF);
	        fos.write((length >> 8) & 0xFF);
	        fos.write((length) & 0xFF);
	        // Write the actual byte buffer
	        fos.write(mBackground == null ? new byte[0] : mBackground);
	        fos.close();
	    }
	    catch (Exception e) { }
	}
	
	/**
	 * Loads the saved background frame from internal storage
	 * @param ctx Application context
	 * @return Success
	 */
	public boolean load(Context ctx) {
		try {
			FileInputStream fis = ctx.openFileInput(FILENAME);
			// Read buffer length first
			int length = ((fis.read() & 0xFF) << 24) | ((fis.read() & 0xFF) << 16) | ((fis.read() & 0xFF) << 8) | ((fis.read() & 0xFF));
			if (length > 0) {
				// Read the actual byte buffer
				mBackground = new byte[length];
				fis.read(mBackground);
			}
			else {
				mBackground = null;
			}
			fis.close();
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Imports the background frame from internal storage
	 * @param ctx Application context
	 * @return Success
	 */
	public boolean importBackground(Context ctx) {
		return this.load(ctx);
	}
	
	/**
	 * Resets the background to null
	 */
	public void reset() {
		mBackground = null;
	}
	
}