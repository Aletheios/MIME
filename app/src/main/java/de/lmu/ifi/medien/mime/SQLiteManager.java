package de.lmu.ifi.medien.mime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Write measured results to a SQLite database
 */
public class SQLiteManager extends SQLiteOpenHelper {
	
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "mime.db";
	
    private static final String TABLE_RESULTS    = "results";
    private static final String KEY_ID           = "id";
    private static final String KEY_USER_ID      = "userId";
    private static final String KEY_TIMESTAMP    = "timestamp";
    private static final String KEY_CONDITION    = "condition";
    private static final String KEY_PHASE        = "phase";
    private static final String KEY_BLOCK        = "block";
    private static final String KEY_ITEM         = "item";
    private static final String KEY_ITEM_DESC    = "itemDescription";
    private static final String KEY_TRIALTIME    = "trialTime";
    private static final String KEY_REACTIONTIME = "reactionTime";
    private static final String KEY_ERRORS       = "errors";
    private static final String KEY_RECOG_FAIL   = "recognizerFailed";
    
    private static final String TABLE_TRAINING   = "training";
    private static final String KEY_COUNT        = "count";
	
	private String mUserId = "";
	
	
	public SQLiteManager(Context ctx) {
		super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public void setUserId(String id) {
		mUserId = id;
	}

	/**
	 * Store new measurement
	 * @param mode Study mode
	 * @param phase Study phase
	 * @param block Block count
	 * @param drawable Pose ID
	 * @param recognizerFailed Indicates failure of the recognizer
	 * @param measurement Time measurement and error count
	 * @return Row ID
	 */
	public long addResult(int mode, int phase, int block, int drawable, int recognizerFailed, ActionManager.Measurement measurement) {
		int timestamp = Util.getTimestamp();
		
		String modeDesc;
		if (mode == ActionManager.MODE_ICONIC) {
			modeDesc = "Iconic";
		}
		else if (mode == ActionManager.MODE_TEXTUAL) {
			modeDesc = "Textual";
		}
		else {
			modeDesc = "Baseline";
		}
		
		String phaseDesc;
		if (phase == ActionManager.PHASE_INTRO) {
			phaseDesc = "Intro";
		}
		else if (phase == ActionManager.PHASE_TRAINING) {
			phaseDesc = "Training";
		}
		else if (phase == ActionManager.PHASE_TEST) {
			phaseDesc = "Short term retention";
		}
		else {
			phaseDesc = "Long term retention";
		}
		
		int item = DrawableHelper.find(mode, drawable);
		String itemName = "Item" + item;
		String itemDesc = DrawableHelper.getDescription(item);
		
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_USER_ID, mUserId);
		values.put(KEY_TIMESTAMP, timestamp);
		values.put(KEY_CONDITION, modeDesc);
		values.put(KEY_PHASE, phaseDesc);
		values.put(KEY_BLOCK, block);
		values.put(KEY_ITEM, itemName);
		values.put(KEY_ITEM_DESC, itemDesc);
		values.put(KEY_TRIALTIME, measurement.trialTime);
		values.put(KEY_REACTIONTIME, measurement.reactionTime);
		values.put(KEY_ERRORS, measurement.errorCount);
		values.put(KEY_RECOG_FAIL, recognizerFailed);
		
		long id = db.insert(TABLE_RESULTS, null, values);
		db.close();
		
		return id;
	}

	/**
	 * Store number of block repetitions during the training phase
	 * @param count Block repetitions
	 * @return Row ID
	 */
	public long writeTrainingCount(int count) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_USER_ID, mUserId);
		values.put(KEY_TIMESTAMP, Util.getTimestamp());
		values.put(KEY_COUNT, count);
		
		long id = db.insert(TABLE_TRAINING, null, values);
		db.close();
		
		return id;
	}

	/**
	 * Get ID for the next study participant
	 * @param phase Study phase
	 * @return User ID (default 1)
	 */
	public String getNextUserId(int phase) {
		if (phase == ActionManager.PHASE_TEST2) {
			return "";
		}
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.rawQuery("SELECT " + KEY_USER_ID + " FROM " + TABLE_RESULTS + " ORDER BY " + KEY_ID + " DESC LIMIT 1", null);
		try {
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				int maxId = Integer.parseInt(c.getString(0));
				if (phase == ActionManager.PHASE_INTRO) {
					++maxId;
				}
				return maxId + "";
			}
		}
		catch (Exception e) { }
		finally {
			if (c != null) {
				c.close();
			}
		}
		return "1";
	}

	/**
	 * Export the database to local storage
	 * @param ctx Application context
	 * @return Success
	 */
	public boolean export(Context ctx) {
		close();
		
		String toPath = Util.getSDCardDirPath() + "/" + "mime-" + Util.getTimestamp() + ".db";
	    File newDb = new File(toPath);
	    File oldDb = ctx.getDatabasePath(DATABASE_NAME);
	    
	    try {
		    if (!newDb.exists()) {
				if (!newDb.createNewFile()) {
					return false;
				}
		    }
		    Util.copyFile(new FileInputStream(oldDb), new FileOutputStream(newDb));
	    }
	    catch (Exception e) {
	    	return false;
	    }
    	
        getWritableDatabase().close();
        return true;
	}

	/**
	 * Reset internal database
	 */
	public void reset() {
		SQLiteDatabase db = getWritableDatabase();
		onUpgrade(db, 1, 1);
	}
	
	@Override
    public void onCreate(SQLiteDatabase db) {
        String createTable1 = "CREATE TABLE " + TABLE_RESULTS + "("
            + KEY_ID + " INTEGER PRIMARY KEY," 
    		+ KEY_USER_ID + " TEXT,"
            + KEY_TIMESTAMP + " INTEGER,"
            + KEY_CONDITION + " TEXT,"
            + KEY_PHASE + " TEXT,"
            + KEY_BLOCK + " INTEGER,"
            + KEY_ITEM + " TEXT,"
            + KEY_ITEM_DESC + " TEXT,"
            + KEY_TRIALTIME + " INTEGER,"
            + KEY_REACTIONTIME + " INTEGER,"
            + KEY_ERRORS + " INTEGER,"
            + KEY_RECOG_FAIL + " INTEGER" + ")";
        db.execSQL(createTable1);
        
        String createTable2 = "CREATE TABLE " + TABLE_TRAINING + "("
            + KEY_ID + " INTEGER PRIMARY KEY," 
    		+ KEY_USER_ID + " TEXT,"
    		+ KEY_TIMESTAMP + " INTEGER,"
            + KEY_COUNT + " INTEGER" + ")";
        db.execSQL(createTable2);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESULTS);
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRAINING);
        onCreate(db);
    }
	
}