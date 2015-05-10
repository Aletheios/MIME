package de.lmu.ifi.medien.mime;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;

public class MainActivity extends Activity implements CvCameraViewListener2 {
	
	private CameraBridgeViewBase mOpenCvCameraView;
	
	private boolean mCameraViewInitialized = false;
	private boolean mShowFPS = false;
	private boolean mShowPoses = false;
	private boolean mShowRecognition = false;
	private volatile boolean mRecordPose = false;
	private volatile boolean mCheckPoseNow = false;
	
	private HandDetector mHandDetector;
	private PoseRecognizer mPoseRecognizer;
	private BackgroundHolder mBackgroundHolder;
	private PoseListHelper mPoseListHelper;
	private PreferenceHelper mPrefs;
	private SQLiteManager mDatabase;
	private ActionManager mActionManager;
	
	private Button mConfirmButton;
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
            }
        }
    };
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		setContentView(R.layout.activity_main);
		
        // Initialize helper objects
		mPrefs = PreferenceHelper.getInstance(this);
		
		mBackgroundHolder = BackgroundHolder.getInstance();
		mBackgroundHolder.setUseSavedBackground(mPrefs.useSavedBackground());
		mBackgroundHolder.load(this);
		
        mHandDetector = HandDetector.getInstance();
        mHandDetector.updatePrefs(MainActivity.this);
        mDatabase = new SQLiteManager(this);
        mActionManager = new ActionManager(this, mDatabase);
        
        // Initialize button for confirming or recording poses, respectively
        final String[] allDrawableDescriptions = PoseRecognizer.getAllDescriptions();
        mConfirmButton = (Button) findViewById(R.id.button_record);
        mConfirmButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mRecordPose) {
					// Record current pose
					mHandDetector.recordPose();
					Util.select(MainActivity.this, R.string.title_link_pose, allDrawableDescriptions, 0, new Util.Callback<Integer>(){
						@Override
						public void call(Integer param) {
							mHandDetector.savePose(param);
							Util.toast(MainActivity.this, R.string.message_pose_saved);
						}
					});
				}
				else {
					// Confirm
					mCheckPoseNow = true;
					mConfirmButton.setEnabled(false);
				}
			}
		});
        
        // Initialize camera view
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        
        // Enable study mode
        if (mPrefs.isStudyModeEnabled()) {
        	// Workaround for whatever causes faulty background to be set
			Handler h = new Handler();
			h.postDelayed(new Runnable(){
				@Override
				public void run() {
					mHandDetector.setBackground(true);
				}
			}, 500);
        }
        
        // Select mode
        Util.select(this, R.string.mode_select, new int[] { R.string.mode_icon, R.string.mode_text, R.string.mode_base }, 0, new Util.Callback<Integer>() {
			@Override
			public void call(final Integer mode) {
				if (mode >= 0 && mode <= 2) {
					mActionManager.setMode(mode);
					
					// Finish initialization
					mPoseRecognizer = new PoseRecognizer(MainActivity.this, mHandDetector.getFrameSize());
			        mHandDetector.setRecognizer(mPoseRecognizer);
			        
			        // Initialize list of poses
			        mPoseListHelper = PoseListHelper.getInstance();
			        mPoseListHelper.init(MainActivity.this, mPoseRecognizer, (ListView) findViewById(R.id.poses), new Util.Callback<Integer>(){
						@Override
						public void call(Integer param) {
							boolean success = mPoseRecognizer.deletePose(param);
							Util.toast(MainActivity.this, success ? R.string.message_pose_deleted : R.string.message_pose_delete_error);
						}
			        });
				}
				
				// Select phase
				Util.select(MainActivity.this, R.string.phase_select, new int[] { R.string.phase_trial, R.string.phase_training, R.string.phase_test, R.string.phase_test2 }, 0, new Util.Callback<Integer>() {
					@Override
					public void call(final Integer phase) {
						if (phase >= 0 && phase <= 3) {
							mActionManager.setPhase(phase);
						}
					
						// Get user ID
						Util.prompt(MainActivity.this, R.string.text_enter_user_id, mDatabase.getNextUserId(phase), new Util.Callback<String>() {
							@Override
							public void call(String id) {
								mActionManager.setUserId(id);
								
								// Show begin text
								Util.continueAlert(MainActivity.this, -1, R.string.text_lets_go, 0, new Util.Callback<Void>() {
									@Override
									public void call(Void param) {
										if (!mHandDetector.isBackgroundSet()) {
											// Yay for pyramid code! :P
											mHandDetector.setBackground(true);
										}
										mActionManager.start();
									}
								});
							}
						});
					}
				});
			}
		});
	}
	
	@Override
    public void onResume() {
        super.onResume();
        if (!mCameraViewInitialized) {
        	mCameraViewInitialized = true;
        	OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
        }
    }
	
	@Override
	public void onPause() {
		super.onPause();
		if (mBackgroundHolder != null) {
			mBackgroundHolder.save(this);
		}
		if (mPoseRecognizer != null) {
			mPoseRecognizer.save(this);
		}
	}
	
	@Override
	public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        if (mHandDetector != null) {
        	mHandDetector.clean();
        }
        if (mPoseListHelper != null) {
        	mPoseListHelper.clean();
        }
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.main, menu);
		return true;
    }
	
	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	menu.findItem(R.id.menu_savedbg).setTitle(mPrefs.useSavedBackground() ? R.string.menu_savedbg2 : R.string.menu_savedbg1);
    	menu.findItem(R.id.menu_recordpos).setTitle(mRecordPose ? R.string.menu_recordpos2 : R.string.menu_recordpos1);
    	
    	boolean studyModeEnabled = mPrefs.isStudyModeEnabled();
    	menu.findItem(R.id.menu_studymode).setTitle(studyModeEnabled ? R.string.menu_studymode2 : R.string.menu_studymode1);
    	
    	menu.findItem(R.id.menu_setbg).setVisible(!studyModeEnabled);
    	menu.findItem(R.id.menu_togglefps).setVisible(!studyModeEnabled);
    	menu.findItem(R.id.menu_recordbg).setVisible(!studyModeEnabled);
    	menu.findItem(R.id.menu_savedbg).setVisible(!studyModeEnabled);
    	menu.findItem(R.id.menu_recordpos).setVisible(!studyModeEnabled);
    	menu.findItem(R.id.menu_showpos).setVisible(!studyModeEnabled);
    	menu.findItem(R.id.menu_showrecognition).setVisible(!studyModeEnabled);
		menu.findItem(R.id.menu_import).setVisible(!studyModeEnabled);
    	
    	return super.onPrepareOptionsMenu(menu);
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			// Start or reset recognizer by setting a new frame for background subtraction 
			case R.id.menu_setbg:
				mHandDetector.setBackground(true);
				return true;
			
			// Show/hide FPS meter
			case R.id.menu_togglefps:
				if (!mShowFPS) {
					mOpenCvCameraView.enableFpsMeter();
					item.setTitle(R.string.menu_togglefps2);
					findViewById(R.id.task).setVisibility(View.GONE);
				}
				else {
					mOpenCvCameraView.disableFpsMeter();
					item.setTitle(R.string.menu_togglefps1);
					findViewById(R.id.task).setVisibility(View.VISIBLE);
				}
				mShowFPS = !mShowFPS;
				return true;
			
			// Enable/disable study mode
			case R.id.menu_studymode:
				boolean enable = !mPrefs.isStudyModeEnabled();
				mPrefs.setStudyModeEnabled(enable);
				item.setTitle(enable ? R.string.menu_studymode2 : R.string.menu_studymode1);
				invalidateOptionsMenu();
				return true;
	    		
	   		// Record background
			case R.id.menu_recordbg:
				mHandDetector.setBackground(true);
				mBackgroundHolder.startBackgroundRecording();
				Util.alert(this, -1, R.string.text_bgrecording, Util.CENTER_MENU, new Util.Callback<Void>(){
					@Override
					public void call(Void param) {
						mBackgroundHolder.stopBackgroundRecording();
					}
				});
				return true;
			
			// Enable/disable saved background
			case R.id.menu_savedbg:
				boolean enable2 = !mPrefs.useSavedBackground();
				mPrefs.setUseSavedBackground(enable2);
				mBackgroundHolder.setUseSavedBackground(enable2);
				item.setTitle(enable2 ? R.string.menu_savedbg2 : R.string.menu_savedbg1);
				invalidateOptionsMenu();
				return true;
			
			// Show/hide button for recording poses
			case R.id.menu_recordpos:
				mRecordPose = !mRecordPose;
				mHandDetector.setRecordingMode(mRecordPose);
				findViewById(R.id.task).setVisibility(mRecordPose ? View.GONE : View.VISIBLE);
				mConfirmButton.setText(mRecordPose ? R.string.button_record : R.string.button_confirm);
				item.setTitle(mRecordPose ? R.string.menu_recordpos2 : R.string.menu_recordpos1);
				invalidateOptionsMenu();
				return true;
			
			// Show list of recorded poses
			case R.id.menu_showpos:
				mShowPoses = !mShowPoses;
				mPoseListHelper.setVisible(mShowPoses);
				return true;
			
			// Show toast with recognized pose
			case R.id.menu_showrecognition:
				mShowRecognition = !mShowRecognition;
				item.setTitle(mShowRecognition ? R.string.menu_showrecognition2 : R.string.menu_showrecognition1);
				return true;
			
			// Try to import poses and saved background from SD card
			case R.id.menu_import:
				String posesPath = Util.getSDCardDirPath() + "/" + PoseRecognizer.FILENAME;
				String backgroundPath = Util.getSDCardDirPath() + "/" + BackgroundHolder.FILENAME;
				File poses = new File(posesPath);
				File background = new File(backgroundPath);
				try {
					Util.copyFile(new FileInputStream(poses), this.openFileOutput(PoseRecognizer.FILENAME, Context.MODE_PRIVATE));
					Util.copyFile(new FileInputStream(background), this.openFileOutput(BackgroundHolder.FILENAME, Context.MODE_PRIVATE));
				}
				catch (Exception e) { }
				
				if (!mHandDetector.importPoses(this)) {
					Util.toast(this, R.string.message_error_importing_poses);
					return true;
				}
				if (!mBackgroundHolder.importBackground(this)) {
					Util.toast(this, R.string.message_error_importing_background);
					return true;
				}
				Util.toast(this, R.string.message_import_successful);
				return true;
				
			// Reset application data
			case R.id.menu_reset:
				Util.vibrate(this, 200);
				Util.confirm(this, R.string.message_confirm, new Util.Callback<Void>(){
					@Override
					public void call(Void param) {
						mDatabase.reset();
						mBackgroundHolder.reset();
						mPoseRecognizer.reset();
						deleteFile(BackgroundHolder.FILENAME);
						deleteFile(PoseRecognizer.FILENAME);
					}
				});
				return true;
			
			// Settings
			case R.id.menu_settings:
				Util.prefPrompt(this, new Util.Callback<ArrayList<String>>() {
					@Override
					public void call(ArrayList<String> values) {
						int[] prefs = new int[values.size()];
						for (int i = 0; i < values.size(); ++i) {
							prefs[i] = Integer.parseInt(values.get(i));
						}
						mPrefs.setPrefs(prefs);
						mHandDetector.updatePrefs(MainActivity.this);
					}
				});
				return true;
		}
		
    	return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed() {
		if (mShowPoses) {
			mShowPoses = false;
			mPoseListHelper.setVisible(false);
			return;
		}
		
		Util.confirm(this, R.string.message_exit_app, new Util.Callback<Void>(){
			@Override
			public void call(Void param) {
				MainActivity.super.onBackPressed();
			}
		});
	}
	
	public void onCameraViewStarted(int width, int height) {
		if (mPrefs.useSavedBackground()) {
			mHandDetector.setBackground(true);
		}
    }

    public void onCameraViewStopped() { }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	final PoseRecognizer.Result result = mHandDetector.detect(inputFrame.rgba(), true);
    	
    	if (result.nonZero) {
    		mActionManager.reaction();
    	}
    	if (mCheckPoseNow) {
	    	// Note: We are not in UI thread here!
	    	MainActivity.this.runOnUiThread(new Runnable(){
			    public void run(){
			    	// Now we're on the UI thread...
		    		mCheckPoseNow = false;
		    		mConfirmButton.setEnabled(true);
			    	mActionManager.check(result.pose);
			    	if (mShowRecognition) {
			    		Util.toast(MainActivity.this, "ID:" + result.pose + ", Pose: " + PoseRecognizer.getDescription(result.pose));
			    	}
			    }
	    	});
    	}
    	
    	return result.frame;
    }
	
}