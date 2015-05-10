package de.lmu.ifi.medien.mime;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class PreferenceHelper {
	
	private static PreferenceHelper instance = null;
	private final SharedPreferences mSettings;
	
	private static final String STUDYMODE_KEY = "studymode";
	private static final boolean STUDYMODE_DEFAULT = false;
	private static final String SAVEDBG_KEY = "savedbg";
	private static final boolean SAVEDBG_DEFAULT = false;
	
	private static final String PREF_KEY = "pref";
	private static final int[] PREF_DEFAULT = new int[] { 30, 225, 1, 1, 1, 3, 3 };
	
	public static final int PREF_THRESHOLD_LOWER_H = 0;
	public static final int PREF_THRESHOLD_UPPER_H = 1;
	public static final int PREF_WEIGHTING_H = 2;
	public static final int PREF_WEIGHTING_S = 3;
	public static final int PREF_WEIGHTING_V = 4;
	public static final int PREF_WEIGHTING_B = 5;
	public static final int PREF_WEIGHTING_THRESHOLD = 6;
	
	
	private PreferenceHelper(Context ctx) {
		mSettings = ctx.getSharedPreferences(ctx.getString(R.string.app_name), Context.MODE_PRIVATE);
	}
	
	public static PreferenceHelper getInstance(Context ctx) {
		if (instance == null) {
			instance = new PreferenceHelper(ctx);
		}
		return instance;
	}
	
	public boolean isStudyModeEnabled() {
		return mSettings.getBoolean(STUDYMODE_KEY, STUDYMODE_DEFAULT);
	}
	public void setStudyModeEnabled(boolean enabled) {
		Editor editor = mSettings.edit();
		editor.putBoolean(STUDYMODE_KEY, enabled);
		editor.apply();
	}
	
	public boolean useSavedBackground() {
		return mSettings.getBoolean(SAVEDBG_KEY, SAVEDBG_DEFAULT);
	}
	public void setUseSavedBackground(boolean enabled) {
		Editor editor = mSettings.edit();
		editor.putBoolean(SAVEDBG_KEY, enabled);
		editor.apply();
	}
	
	public int[] getPrefs() {
		int[] prefs = new int[PREF_DEFAULT.length];
		for (int i = 0; i < PREF_DEFAULT.length; ++i) {
			prefs[i] = mSettings.getInt(PREF_KEY + i, PREF_DEFAULT[i]);
		}
		return prefs;
	}
	public void setPrefs(int[] prefs) {
		Editor editor = mSettings.edit();
		for (int i = 0; i < prefs.length; ++i) {
			editor.putInt(PREF_KEY + i, prefs[i]);
		}
		editor.apply();
	}
	
}