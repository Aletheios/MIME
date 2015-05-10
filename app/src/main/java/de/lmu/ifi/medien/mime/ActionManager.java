package de.lmu.ifi.medien.mime;

import android.view.View;
import android.widget.ImageView;

/**
 * Manages the study process including different phases (e.g. short term retention test) and block repetitions
 */
public class ActionManager {
	
	private static final int INTRO_REPETITIONS = 2;		//repetitions in intro phase
	private static final int POSE_MAX_REPETITIONS = 3;	//repetitions of a single pose in intro phase
	private static final int TRAINING_MAX_REPETITIONS = Integer.MAX_VALUE;	//infinite repetitions
	private static final int MIN_CORRECT_SERIES = 2;	//objective end criterion
	
	public static final int MODE_ICONIC    = 0;
	public static final int MODE_TEXTUAL   = 1;
	public static final int MODE_BASELINE  = 2;
	public static final int PHASE_INTRO    = 0;
	public static final int PHASE_TRAINING = 1;
	public static final int PHASE_TEST     = 2;
	public static final int PHASE_TEST2    = 3;
	
	private int mMode  = MODE_ICONIC;
	private int mPhase = PHASE_INTRO;
	
	private int[] mModeDrawables = null;
	private int[] mTestDrawables = DrawableHelper.getTestPhaseDrawables();
	
	private int mCurrentIndex = 0;
	private int mPhaseCounter = 0;
	private int mCurrentTask = 0;
	private int mCorrectSeries = 0;
	private int mRepeatCount = 0;
	private boolean mAllCorrect = true;
	private boolean mAlreadyEnded = false;
	
	private long mBeginTime = 0;
	private long mRepeatTime = 0;
	private Measurement mMeasurement;
	private boolean mGetReactionTime = false;
	
	private MainActivity mOwner;
	private ImageView mTaskImage;
	private SQLiteManager mDatabase;
	
	private boolean mIsStarted = false;
	
	
	public ActionManager(MainActivity a, SQLiteManager db) {
		mOwner = a;
		mDatabase = db;
		mTaskImage = (ImageView) mOwner.findViewById(R.id.task_image);
	}
	
	/**
	 * Start the experiment
	 */
	public void start() {
		if (!mIsStarted) {
			mIsStarted = true;
			mOwner.findViewById(R.id.task).setVisibility(View.VISIBLE);
			Util.shuffle(mTestDrawables);
			this.nextAction(false);
		}
	}

	/**
	 * Execute next action in experiment
	 * @param repeatCurrent Repeat current pose
	 */
	public void nextAction(boolean repeatCurrent) {
		if (repeatCurrent && mPhase == PHASE_INTRO) {
			if (++mRepeatCount > POSE_MAX_REPETITIONS) {
				mRepeatCount = 0;
				repeatCurrent = false;
			}
		}
		else {
			mRepeatCount = 0;
		}
		
		if (repeatCurrent) {
			--mCurrentIndex;
		}
		
		if (mCurrentIndex >= DrawableHelper.NUM_DRAWABLES) {
			// Current phase completed
			mCurrentIndex = 0;
			
			Util.Callback<Void> defaultCb = new Util.Callback<Void>() {
				@Override
				public void call(Void param) {
					nextAction(false);
				}
			};
			
			if (mPhase == PHASE_INTRO) {
				++mPhaseCounter;
				Util.shuffle(mModeDrawables);
				if (mPhaseCounter >= INTRO_REPETITIONS) {
					// Intro completed
					mPhaseCounter = 0;
					++mPhase;
					Util.continueAlert(mOwner, R.string.title_phase_complete, R.string.text_intro_phase_complete, 0, defaultCb);
					return;
				}
				else {
					Util.continueAlert(mOwner, R.string.title_block_phase_complete, R.string.text_block_phase_complete, 0, defaultCb);
					return;
				}
			}
			else if (mPhase == PHASE_TRAINING) {
				++mPhaseCounter;
				Util.shuffle(mTestDrawables);
				
				mCorrectSeries = mAllCorrect ? mCorrectSeries+1 : 0;
				mAllCorrect = true;
				
				if (mPhaseCounter >= TRAINING_MAX_REPETITIONS || mCorrectSeries >= MIN_CORRECT_SERIES || mAlreadyEnded) {
					// Training completed
					mAlreadyEnded = true;
					final int phaseNum = mPhaseCounter;
					if (mPhaseCounter < 100) {
						mPhaseCounter += 100;
					}
					++mPhase;
					Util.choose(mOwner, R.string.title_phase_complete, R.string.text_training_phase_complete, new Util.Callback<Void>() {
						@Override
						public void call(Void param) {
							--mPhase;
							Util.shuffle(mModeDrawables);
							nextAction(false);
						}
					}, new Util.Callback<Void>() {
						@Override
						public void call(Void param) {
							mDatabase.writeTrainingCount(phaseNum % 100);
							mOwner.finish();
						}
					});
					return;
				}
				else {
					// One training iteration completed, but not reached objective end criterion yet
					Util.continueAlert(mOwner, R.string.title_block_phase_complete, R.string.text_block_phase_complete, 0, defaultCb);
					return;
				}
			}
			else {
				// Test phase completed
				Util.alert(mOwner, R.string.title_phase_complete, R.string.text_test_phase_complete, 0, new Util.Callback<Void>() {
					@Override
					public void call(Void param) {
						// We're done here, so export database and close the app
						mDatabase.export(mOwner);
						mOwner.finish();
					}
				});
				return;
			}
		}
		
		int drawable = mPhase == PHASE_INTRO ? mModeDrawables[mCurrentIndex] : mTestDrawables[mCurrentIndex];
		mTaskImage.setBackgroundResource(drawable);
		mCurrentTask = drawable;
		
		if (!repeatCurrent) {
			mBeginTime = System.nanoTime();
			mMeasurement = new Measurement();
			mGetReactionTime = true;
		}
		else {
			// Don't count time user needed to read alert dialog
			int timeDiff = (int) (System.nanoTime() - mRepeatTime);
			mBeginTime += timeDiff;
		}
		
		++mCurrentIndex;
	}

	/**
	 * Check the recognized pose
	 * @param pose Pose ID
	 */
	public void check(int pose) {
		mMeasurement.trialTime = getTime();
		
		final int poseDrawable = DrawableHelper.getDrawableFromType(pose, mMode);
		final int translatedDrawable = mPhase == PHASE_INTRO ? mCurrentTask : DrawableHelper.translate(mCurrentTask, mMode);
		final boolean correct = poseDrawable == translatedDrawable;
		
		if (!correct) {
			++mMeasurement.errorCount;
			mRepeatTime = System.nanoTime();
		}
		
		// Only save to database if correct pose detected; however, always save if in test mode
		if (correct || mPhase == PHASE_TEST || mPhase == PHASE_TEST2) {
			mDatabase.addResult(mMode, mPhase, mPhaseCounter+1, translatedDrawable, 0, mMeasurement);
		}
		
		final Util.Callback<Void> cb = new Util.Callback<Void>() {
			@Override
			public void call(Void param) {
				nextAction(!correct);
			}
		};
		
		if (mPhase == PHASE_INTRO) {
			Util.continueAlert(mOwner, -1, correct ? R.string.text_trial_correct : (mRepeatCount >= POSE_MAX_REPETITIONS ? R.string.text_trial_incorrect_but_never_mind : R.string.text_trial_incorrect_nice), 0, cb);
			if (mRepeatCount >= POSE_MAX_REPETITIONS) {
				mDatabase.addResult(mMode, mPhase, mPhaseCounter+1, translatedDrawable, 0, mMeasurement);
			}
		}
		else if (mPhase == PHASE_TRAINING) {
			if (!correct) {
				Util.imageConfirm(mOwner, -1, R.string.text_trial_incorrect_short, translatedDrawable, new Util.Callback<Void>() {
					@Override
					public void call(Void param) {
						--mMeasurement.errorCount;
						mDatabase.addResult(mMode, mPhase, mPhaseCounter+1, translatedDrawable, 1, mMeasurement);
						Util.continueAlert(mOwner, -1, R.string.text_trial_next, 0, new Util.Callback<Void>() {
							@Override
							public void call(Void param) {
								nextAction(false);
							}
						});
					}
				}, new Util.Callback<Void>() {
					@Override
					public void call(Void param) {
						mAllCorrect = false;
						Util.continueAlert(mOwner, -1, R.string.text_trial_try_again, 0, cb);
					}
				});
			}
			else {
				Util.continueAlert(mOwner, -1, R.string.text_trial_correct, 0, cb);
			}
		}
		else if (mPhase == PHASE_TEST || mPhase == PHASE_TEST2) {
			Util.continueAlert(mOwner, -1, R.string.text_test_ok, 0, new Util.Callback<Void>() {
				@Override
				public void call(Void param) {
					// Never repeat task if in test mode
					nextAction(false);
				}
			});
		}
	}

	/**
	 * Set reaction time
	 */
	public void reaction() {
		if (mGetReactionTime) {
			mGetReactionTime = false;
			mMeasurement.reactionTime = getTime();
		}
	}
	
	public void setUserId(String id) {
		mDatabase.setUserId(id);
	}
	
	public void setPhase(int phase) {
		mPhase = phase;
	}
	
	public void setMode(int mode) {
		mMode = mode;
		
		switch (mMode) {
			case MODE_ICONIC:
				mModeDrawables = DrawableHelper.getIconModeDrawables();
				break;
			case MODE_TEXTUAL:
				mModeDrawables = DrawableHelper.getTextModeDrawables();
				break;
			case MODE_BASELINE:
				mModeDrawables = DrawableHelper.getBaseModeDrawables();
				break;
		}
		
		Util.shuffle(mModeDrawables);
	}

	/**
	 * Get time in milliseconds, counting from begin of task
	 * @return Time in ms
	 */
	private int getTime() {
		return (int) ((System.nanoTime() - mBeginTime) / 1000000L);
	}


	/**
	 * Container class for all measured values
	 */
	public class Measurement {
		public int trialTime = 0;
		public int reactionTime = 0;
		public int errorCount = 0;
	}
	
}