package de.lmu.ifi.medien.mime;


public class DrawableHelper {
	
	public static final int NUM_DRAWABLES = 12;
	
	private static int[] mIconModeDrawables = new int[] {
		R.drawable.icon_addbookmark,
		R.drawable.icon_back,
		R.drawable.icon_bookmarks,
		R.drawable.icon_downloads,
		R.drawable.icon_forward,
		R.drawable.icon_history,
		R.drawable.icon_homepage,
		R.drawable.icon_newtab,
		R.drawable.icon_print,
		R.drawable.icon_reload,
		R.drawable.icon_search,
		R.drawable.icon_settings
	};
	private static int[] mTextModeDrawables = new int[] {
		R.drawable.text_addbookmark,
		R.drawable.text_back,
		R.drawable.text_bookmarks,
		R.drawable.text_downloads,
		R.drawable.text_forward,
		R.drawable.text_history,
		R.drawable.text_homepage,
		R.drawable.text_newtab,
		R.drawable.text_print,
		R.drawable.text_reload,
		R.drawable.text_search,
		R.drawable.text_settings
	};
	private static int[] mBaseModeDrawables = new int[] {
		R.drawable.base_addbookmark,
		R.drawable.base_back,
		R.drawable.base_bookmarks,
		R.drawable.base_downloads,
		R.drawable.base_forward,
		R.drawable.base_history,
		R.drawable.base_homepage,
		R.drawable.base_newtab,
		R.drawable.base_print,
		R.drawable.base_reload,
		R.drawable.base_search,
		R.drawable.base_settings
	};
	private static int[] mTestPhaseDrawables = new int[] {
		R.drawable.test_addbookmark,
		R.drawable.test_back,
		R.drawable.test_bookmarks,
		R.drawable.test_downloads,
		R.drawable.test_forward,
		R.drawable.test_history,
		R.drawable.test_homepage,
		R.drawable.test_newtab,
		R.drawable.test_print,
		R.drawable.test_reload,
		R.drawable.test_search,
		R.drawable.test_settings
	};
	
	
	public static int[] getIconModeDrawables() {
		return mIconModeDrawables.clone();
	}
	public static int[] getTextModeDrawables() {
		return mTextModeDrawables.clone();
	}
	public static int[] getBaseModeDrawables() {
		return mBaseModeDrawables.clone();
	}
	public static int[] getTestPhaseDrawables() {
		return mTestPhaseDrawables.clone();
	}
	
	public static int translate(int testDrawable, int toMode) {
		if (toMode == ActionManager.MODE_ICONIC) {
			switch (testDrawable) {
				case R.drawable.test_addbookmark: return R.drawable.icon_addbookmark;
				case R.drawable.test_back:        return R.drawable.icon_back;
				case R.drawable.test_bookmarks:   return R.drawable.icon_bookmarks;
				case R.drawable.test_downloads:   return R.drawable.icon_downloads;
				case R.drawable.test_forward:     return R.drawable.icon_forward;
				case R.drawable.test_history:     return R.drawable.icon_history;
				case R.drawable.test_homepage:    return R.drawable.icon_homepage;
				case R.drawable.test_newtab:      return R.drawable.icon_newtab;
				case R.drawable.test_print:       return R.drawable.icon_print;
				case R.drawable.test_reload:      return R.drawable.icon_reload;
				case R.drawable.test_search:      return R.drawable.icon_search;
				case R.drawable.test_settings:    return R.drawable.icon_settings;
			}
		}
		else if (toMode == ActionManager.MODE_TEXTUAL) {
			switch (testDrawable) {
				case R.drawable.test_addbookmark: return R.drawable.text_addbookmark;
				case R.drawable.test_back:        return R.drawable.text_back;
				case R.drawable.test_bookmarks:   return R.drawable.text_bookmarks;
				case R.drawable.test_downloads:   return R.drawable.text_downloads;
				case R.drawable.test_forward:     return R.drawable.text_forward;
				case R.drawable.test_history:     return R.drawable.text_history;
				case R.drawable.test_homepage:    return R.drawable.text_homepage;
				case R.drawable.test_newtab:      return R.drawable.text_newtab;
				case R.drawable.test_print:       return R.drawable.text_print;
				case R.drawable.test_reload:      return R.drawable.text_reload;
				case R.drawable.test_search:      return R.drawable.text_search;
				case R.drawable.test_settings:    return R.drawable.text_settings;
			}
		}
		else if (toMode == ActionManager.MODE_BASELINE) {
			switch (testDrawable) {
				case R.drawable.test_addbookmark: return R.drawable.base_addbookmark;
				case R.drawable.test_back:        return R.drawable.base_back;
				case R.drawable.test_bookmarks:   return R.drawable.base_bookmarks;
				case R.drawable.test_downloads:   return R.drawable.base_downloads;
				case R.drawable.test_forward:     return R.drawable.base_forward;
				case R.drawable.test_history:     return R.drawable.base_history;
				case R.drawable.test_homepage:    return R.drawable.base_homepage;
				case R.drawable.test_newtab:      return R.drawable.base_newtab;
				case R.drawable.test_print:       return R.drawable.base_print;
				case R.drawable.test_reload:      return R.drawable.base_reload;
				case R.drawable.test_search:      return R.drawable.base_search;
				case R.drawable.test_settings:    return R.drawable.base_settings;
			}
		}
		return -1;
	}
	
	public static int getDrawableFromType(int type, int mode) {
		if (mode == ActionManager.MODE_ICONIC) {
			switch (type) {
				case PoseRecognizer.POSE_W:     return R.drawable.icon_addbookmark;
				case PoseRecognizer.POSE_INV_L: return R.drawable.icon_back;
				case PoseRecognizer.POSE_V:     return R.drawable.icon_bookmarks;
				case PoseRecognizer.POSE_I:     return R.drawable.icon_downloads;
				case PoseRecognizer.POSE_L:     return R.drawable.icon_forward;
				case PoseRecognizer.POSE_C:     return R.drawable.icon_history;
				case PoseRecognizer.POSE_U:     return R.drawable.icon_homepage;
				case PoseRecognizer.POSE_TD_L:  return R.drawable.icon_newtab;
				case PoseRecognizer.POSE_MINUS: return R.drawable.icon_print;
				case PoseRecognizer.POSE_OK:    return R.drawable.icon_reload;
				case PoseRecognizer.POSE_O:     return R.drawable.icon_search;
				case PoseRecognizer.POSE_E:     return R.drawable.icon_settings;
			}
		}
		else if (mode == ActionManager.MODE_TEXTUAL) {
			switch (type) {
				case PoseRecognizer.POSE_TD_L:  return R.drawable.text_addbookmark;
				case PoseRecognizer.POSE_INV_L: return R.drawable.text_back;
				case PoseRecognizer.POSE_L:     return R.drawable.text_bookmarks;
				case PoseRecognizer.POSE_W:     return R.drawable.text_downloads;
				case PoseRecognizer.POSE_I:     return R.drawable.text_forward;
				case PoseRecognizer.POSE_V:     return R.drawable.text_history;
				case PoseRecognizer.POSE_OK:    return R.drawable.text_homepage;
				case PoseRecognizer.POSE_MINUS: return R.drawable.text_newtab;
				case PoseRecognizer.POSE_O:     return R.drawable.text_print;
				case PoseRecognizer.POSE_U:     return R.drawable.text_reload;
				case PoseRecognizer.POSE_C:     return R.drawable.text_search;
				case PoseRecognizer.POSE_E:     return R.drawable.text_settings;
			}
		}
		else if (mode == ActionManager.MODE_BASELINE) {
			switch (type) {
				case PoseRecognizer.POSE_W:     return R.drawable.base_addbookmark;
				case PoseRecognizer.POSE_V:     return R.drawable.base_back;
				case PoseRecognizer.POSE_I:     return R.drawable.base_bookmarks;
				case PoseRecognizer.POSE_U:     return R.drawable.base_downloads;
				case PoseRecognizer.POSE_C:     return R.drawable.base_forward;
				case PoseRecognizer.POSE_OK:    return R.drawable.base_history;
				case PoseRecognizer.POSE_TD_L:  return R.drawable.base_homepage;
				case PoseRecognizer.POSE_O:     return R.drawable.base_newtab;
				case PoseRecognizer.POSE_L:     return R.drawable.base_print;
				case PoseRecognizer.POSE_E:     return R.drawable.base_reload;
				case PoseRecognizer.POSE_MINUS: return R.drawable.base_search;
				case PoseRecognizer.POSE_INV_L: return R.drawable.base_settings;
			}
		}
		return -1;
	}
	
	public static String getDescription(int index) {
		switch (index) {
			case 0:  return "Add bookmark";
			case 1:  return "Back";
			case 2:  return "Bookmarks";
			case 3:  return "Downloads";
			case 4:  return "Forward";
			case 5:  return "History";
			case 6:  return "Homepage";
			case 7:  return "New tab";
			case 8:  return "Print";
			case 9:  return "Reload";
			case 10: return "Search";
			case 11: return "Settings";
		}
		return "";
	}
	
	public static int find(int mode, int drawable) {
		int[] array;
		if (mode == ActionManager.MODE_ICONIC) {
			array = mIconModeDrawables;
		}
		else if (mode == ActionManager.MODE_TEXTUAL) {
			array = mTextModeDrawables;
		}
		else {
			array = mBaseModeDrawables;
		}
		for (int i = 0; i < array.length; ++i) {
			if (array[i] == drawable) {
				return i;
			}
		}
		return 0;
	}
	
}