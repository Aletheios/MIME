package de.lmu.ifi.medien.mime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

public class Util {
	
	public static final String DIR_NAME = "MIME";
	public static final int CENTER_MENU = -1;
	
	
	/**
	 * Vibrate for the specified period of time
	 * @param ctx Application context
	 * @param milliSeconds Time
	 */
	public static void vibrate(Context ctx, int milliSeconds) {
		try {
			Vibrator vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(milliSeconds);
		}
		catch (Exception e) { }
	}
	
	/**
	 * Hides the soft keyboard
	 * @param a Current activity
	 * @param view Any view attached to the window that contains the keyboard; ideally the view that caused the keyboard to appear
	 */
	public static void hideSoftKeyboard(Activity a, View view) {
		InputMethodManager imm = (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}
	
	/**
	 * Displays an alert dialog
	 * @param ctx Application context
	 * @param titleId Title ID from R.string (-1 to omit the title)
	 * @param messageId Message ID from R.string
	 * @param topOffset y position of the dialog box; -1 to get the default center position
	 * @param cb Action that will be executed when OK is clicked
	 */
	public static void alert(Context ctx, int titleId, int messageId, int topOffset, final Callback<Void> cb) {
		alert(ctx, titleId, messageId, R.string.button_ok, topOffset, cb);
	}
	public static void continueAlert(Context ctx, int titleId, int messageId, int topOffset, final Callback<Void> cb) {
		alert(ctx, titleId, messageId, topOffset == -1 ? R.string.button_continue : R.string.button_continue_left, topOffset, cb);
	}
	private static void alert(Context ctx, int titleId, int messageId, int buttonLabel, int topOffset, final Callback<Void> cb) {
		AlertDialog ad = new AlertDialog.Builder(ctx)
			.setMessage(messageId)
			.setCancelable(false)
			.setNeutralButton(buttonLabel, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
					cb.call(null);
				}
			})
			.create();
		if (titleId != -1) {
			ad.setTitle(titleId);
		}
		if (topOffset != CENTER_MENU) {
			WindowManager.LayoutParams wmlp = ad.getWindow().getAttributes();
			wmlp.gravity = Gravity.TOP;
			wmlp.y = topOffset;
		}
		ad.show();
	}
	
	/**
	 * Displays a confirm dialog
	 * @param ctx Application context
	 * @param messageId Message ID from R.string
	 * @param cb Action that will be executed when OK is clicked
	 */
	public static void confirm(Context ctx, int messageId, final Callback<Void> cb) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx)
			.setMessage(messageId)
			.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					cb.call(null);
				}
			})
			.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
				}
			});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}
	
	/**
	 * Displays a confirm dialog with an explicitly defined "cancel" function
	 * @param ctx Application context
	 * @param titleId Title ID from R.string (-1 to omit the title)
	 * @param messageId Message ID from R.string
	 * @param okCb Action that will be executed when OK is clicked
	 * @param cancelCb Action that will be executed when Cancel is clicked
	 */
	public static void choose(Context ctx, int titleId, int messageId, final Callback<Void> okCb, final Callback<Void> cancelCb) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx)
			.setMessage(messageId)
			.setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					okCb.call(null);
				}
			})
			.setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					cancelCb.call(null);
				}
			});
		AlertDialog alertDialog = alertDialogBuilder.create();
		if (titleId != -1) {
			alertDialog.setTitle(titleId);
		}
		alertDialog.show();
	}
	
	/**
	 * Display a confirm dialog with a text field
	 * @param a Activity
	 * @param messageId Message ID from R.string
	 * @param defaultText Default value for text field
	 * @param cb Action that will be executed when OK is clicked; receives the entered string as parameter
	 */
	public static void prompt(final Activity a, int messageId, String defaultText, final Callback<String> cb) {
		final EditText input = new EditText(a);
		if (defaultText != null) {
			input.setText(defaultText);
			input.setSelection(defaultText.length());
		}
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(a)
			.setMessage(messageId)
			.setCancelable(false)
			.setView(input)
			.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					hideSoftKeyboard(a, input);
					String value = input.getText().toString().trim();
					cb.call(value);
				}
			});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}
	
	/**
	 * Display preference panel
	 * @param a Activity
	 * @param cb The usual callback
	 */
	public static void prefPrompt(Activity a, final Callback<ArrayList<String>> cb) {
		LayoutInflater inflater = a.getLayoutInflater();
		View dialogLayout = inflater.inflate(R.layout.prefs, null);
		int[] ids = new int[] { R.id.editText1, R.id.editText2, R.id.editText3, R.id.editText4, R.id.editText5, R.id.editText6, R.id.editText7 };
		final EditText[] inputs = new EditText[ids.length];
		int[] prefs = PreferenceHelper.getInstance(a).getPrefs();
		for (int i = 0; i < ids.length; ++i) {
			inputs[i] = (EditText) dialogLayout.findViewById(ids[i]);
			inputs[i].setText(prefs[i] + "");
		}
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(a)
			.setView(dialogLayout)
			.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					ArrayList<String> ret = new ArrayList<>();
					for (EditText input : inputs) {
						ret.add(input.getText().toString().trim());
					}
					cb.call(ret);
				}
			})
			.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
				}
			});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}
	
	/**
	 * Display an alert dialog with an image
	 * @param a Activity
	 * @param titleId Title ID from R.string (-1 to omit the title)
	 * @param messageId Message ID from R.string
	 * @param drawable Drawable to display
	 * @param yesCb Action that will be executed when Yes is clicked
	 * @param noCb Action that will be executed when No is clicked
	 */
	@SuppressLint("InflateParams")
	public static void imageConfirm(Activity a, int titleId, int messageId, int drawable, final Callback<Void> yesCb, final Callback<Void> noCb) {
		LayoutInflater inflater = a.getLayoutInflater();
		View dialogLayout = inflater.inflate(R.layout.image_alert, null);
		dialogLayout.findViewById(R.id.image).setBackgroundResource(drawable);
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(a)
			.setMessage(messageId)
			.setCancelable(false)
			.setView(dialogLayout)
			.setNegativeButton(R.string.button_yes, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
					if (yesCb != null) {
						yesCb.call(null);
					}
				}
			})
			.setPositiveButton(R.string.button_no, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
					if (noCb != null) {
						noCb.call(null);
					}
				}
			});
		AlertDialog alertDialog = alertDialogBuilder.create();
		if (titleId != -1) {
			alertDialog.setTitle(titleId);
		}
		alertDialog.show();
	}
	
	/**
	 * Displays a dialog with a list of options to select
	 * @param ctx Application context
	 * @param titleId Title ID from R.string
	 * @param options List of options
	 * @param selected Index of pre-selected option
	 * @param cb Action that will be executed when a option is selected; receives the index of the clicked option
	 */
	public static void select(Context ctx, int titleId, String[] options, int selected, final Callback<Integer> cb) {
		final AtomicInteger selectedItem = new AtomicInteger();
		selectedItem.set(selected);
		CharSequence[] items = new CharSequence[options.length];
		System.arraycopy(options, 0, items, 0, options.length);
		final AlertDialog dialog;
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx)
			.setTitle(titleId)
			.setCancelable(false)
			.setSingleChoiceItems(items, selected, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int item) {
					selectedItem.set(item);
				}
			})
			.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
					cb.call(selectedItem.get());
					dialog.dismiss();
				}
			});
		dialog = alertDialogBuilder.create();
		dialog.show();
	}
	public static void select(Context ctx, int titleId, int[] optionIds, int selected, final Callback<Integer> cb) {
		String[] options = new String[optionIds.length];
		for (int i = 0; i < optionIds.length; ++i) {
			options[i] = ctx.getString(optionIds[i]);
		}
		select(ctx, titleId, options, selected, cb);
	}
	
	/**
	 * Throws a ninja death toast ;)
	 * @link https://www.youtube.com/watch?v=_ZjW_OSUYH4
	 * @param ctx Application context
	 * @param stringId ID from R.string
	 */
	public static void toast(Context ctx, int stringId) {
		Toast.makeText(ctx, stringId, Toast.LENGTH_LONG).show();
	}
	public static void toast(Context ctx, String str) {
		Toast.makeText(ctx, str, Toast.LENGTH_LONG).show();
	}
	
	/**
	 * Implements the Fisher-Yates shuffle algorithm
	 * @param ar Array to shuffle
	 */
	public static void shuffle(int[] ar) {
		Random rnd = new Random();
		for (int i = ar.length - 1; i > 0; --i) {
			int index = rnd.nextInt(i + 1);
			int a = ar[index];
			ar[index] = ar[i];
			ar[i] = a;
		}
	}
	
	/**
	 * Returns the current UNIX timestamp
	 * @return UNIX timestamp
	 */
	public static int getTimestamp() {
		return (int) (System.currentTimeMillis() / 1000);
	}
	
	/**
	 * Copies a file
	 * @param fromFile Origin
	 * @param toFile Destination
	 * @throws IOException
	 * @link http://stackoverflow.com/a/6542214/1350193
	 */
	public static void copyFile(FileInputStream fromFile, FileOutputStream toFile) throws IOException {
		FileChannel fromChannel = null;
		FileChannel toChannel = null;
		try {
			fromChannel = fromFile.getChannel();
			toChannel = toFile.getChannel();
			fromChannel.transferTo(0, fromChannel.size(), toChannel);
		}
		finally {
			try {
				if (fromChannel != null) {
					fromChannel.close();
				}
			}
			finally {
				if (toChannel != null) {
					toChannel.close();
				}
			}
		}
	}
	
	/**
	 * Returns the path to this app's own directory on the SD card (creates it if necessary) 
	 * @return Path to directory
	 */
	public static String getSDCardDirPath() {
		File dir = new File(Environment.getExternalStorageDirectory().toString() + "/" + DIR_NAME);
		dir.mkdirs();
		return dir.toString();
	}
	
	
	public interface Callback<T> {
		void call(T param);
	}
	
}