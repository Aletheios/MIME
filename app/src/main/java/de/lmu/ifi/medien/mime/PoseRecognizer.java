package de.lmu.ifi.medien.mime;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import de.lmu.ifi.medien.mime.HandDetector.PoseFeatures;
import de.lmu.ifi.medien.mime.$N.Multistroke;
import de.lmu.ifi.medien.mime.$N.NDollarRecognizer;

/**
 * Recognize a given hand pose based on a set of extracted features, with the $N recognizer as fallback
 */
public class PoseRecognizer {
	
	public static final String FILENAME = ".saved_poses";
	
	public static final int NO_POSE = -1;
	public static final int POSE_O = 0;
	public static final int POSE_MINUS = 1;
	public static final int POSE_I = 2;
	public static final int POSE_V = 3;
	public static final int POSE_U = 4;
	public static final int POSE_C = 5;
	public static final int POSE_OK = 6;
	public static final int POSE_L = 7;
	public static final int POSE_INV_L = 8;
	public static final int POSE_TD_L = 9;
	public static final int POSE_W = 10;
	public static final int POSE_E = 11;
	
	private static String[] mDescriptions = new String[] { "O", "Minus", "I", "V", "U", "C", "OK", "L", "Inverse L", "Top down L", "W", "E" };
	
	private int[] mIndexSubset0 = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
	private int[] mIndexSubset1 = new int[] {POSE_L, POSE_INV_L, POSE_TD_L, POSE_U};
	private int[] mIndexSubset2 = new int[] {POSE_C, POSE_MINUS};
	private int[] mIndexSubset3 = new int[] {POSE_L, POSE_INV_L, POSE_TD_L, POSE_W};
	private int[] mIndexSubset4 = new int[] {POSE_L, POSE_W};
	private int[] mIndexSubset5 = new int[] {POSE_INV_L, POSE_W};
	private int[] mIndexSubset6 = new int[] {POSE_TD_L, POSE_W};
	private int[] mIndexSubset7 = new int[] {POSE_I, POSE_MINUS};
	
	private NDollarRecognizer mNDollarRecognizer;
	private ArrayList<Pose> mPoses;
	private double mSquareSize;
	
	public PoseRecognizer(Context ctx, double squareSize) {
		this.restore(ctx);
		Collections.sort(mPoses);
		mSquareSize = squareSize;
		mNDollarRecognizer = new NDollarRecognizer(squareSize, true);
		mNDollarRecognizer.setMultistrokes(this.getMultistrokePoses());
	}
	
	public Result recognize(PoseFeatures pf) {
		int likelyPose = NO_POSE;
		NDollarRecognizer.Result result = null;
		
		Result recognitionResult = new Result();

		// Primitive heuristic recognition based on the known set of poses
		if (pf.fingerDefects == 0 && pf.numHoles >= 1) {
			likelyPose = POSE_O;
		}
		else if (pf.fingerDefects == 0) {
			if (pf.skeleton != null) {
				try {
					Rect boundingRect = Imgproc.boundingRect(pf.skeleton);
					if (boundingRect.width > boundingRect.height) {
						likelyPose = POSE_MINUS;
					}
					else {
						likelyPose = POSE_I;
					}
				}
				catch (Exception e) {
					mNDollarRecognizer.setUseIndices(mIndexSubset7);
					result = mNDollarRecognizer.recognize(pf.skeleton);
				}
			}
			else {
				mNDollarRecognizer.setUseIndices(mIndexSubset7);
				result = mNDollarRecognizer.recognize(pf.skeleton);
			}
		}
		else if (pf.fingerDefects <= 1) {
			if (pf.numHoles >= 1) {
				likelyPose = POSE_OK;
			}
			else if (pf.narrowVertAngle) {
				likelyPose = POSE_V;
			}
			else if (pf.checkForU()) {
				likelyPose = POSE_U;
			}
			else if (pf.topScreen && pf.leftScreen) {
				likelyPose = POSE_TD_L;
			}
			else if (pf.getDirection() == PoseFeatures.HORIZONTAL) {
				mNDollarRecognizer.setUseIndices(mIndexSubset2);
				result = mNDollarRecognizer.recognize(pf.skeleton);
			}
			else if (!pf.topScreen && pf.leftScreen) {
				likelyPose = POSE_L;
			}
			else if (!pf.topScreen && !pf.leftScreen) {
				likelyPose = POSE_INV_L;
			}
			else {
				mNDollarRecognizer.setUseIndices(mIndexSubset1);
				result = mNDollarRecognizer.recognize(pf.skeleton);
			}
		}
		else if (pf.fingerDefects <= 2) {
			int direction = pf.getDirection();
			if (direction == PoseFeatures.HORIZONTAL) {
				likelyPose = POSE_E;
			}
			else if (pf.topScreen && pf.leftScreen) {
				mNDollarRecognizer.setUseIndices(mIndexSubset6);
				result = mNDollarRecognizer.recognize(pf.skeleton);
			}
			else if (!pf.topScreen && pf.leftScreen) {
				mNDollarRecognizer.setUseIndices(mIndexSubset4);
				result = mNDollarRecognizer.recognize(pf.skeleton);
			}
			else if (!pf.topScreen && !pf.leftScreen) {
				mNDollarRecognizer.setUseIndices(mIndexSubset5);
				result = mNDollarRecognizer.recognize(pf.skeleton);
			}
			else {
				mNDollarRecognizer.setUseIndices(mIndexSubset3);
				result = mNDollarRecognizer.recognize(pf.skeleton);
			}
		}
		else if (pf.fingerDefects == 3) {
			if (pf.getDirection() == PoseFeatures.HORIZONTAL) {
				likelyPose = POSE_E;
			}
			else {
				likelyPose = POSE_W;
			}
		}
		
		if (likelyPose != NO_POSE) {
			recognitionResult.pose = likelyPose;
			return recognitionResult;
		}
		if (result != null) {
			recognitionResult.pose = result.pose;
			return recognitionResult;
		}
		
		// Worst case, nothing else works
		mNDollarRecognizer.setUseIndices(mIndexSubset0);
		result = mNDollarRecognizer.recognize(pf.skeleton);
		recognitionResult.pose = result.pose;
		return recognitionResult;
	}
	
	public boolean addPose(int type, MatOfPoint skeleton) {
		// Check if type is unique
		int delete = -1;
		for (int i = 0; i < mPoses.size(); ++i) {
			if (mPoses.get(i).getType() == type) {
				delete = i;
				break;
			}
		}
		if (delete >= 0) {
			if (!this.deletePose(delete)) {
				return false;
			}
		}
		
		if (!skeleton.empty()) {
			Point[][] strokes = new Point[1][];
			strokes[0] = skeleton.toArray();
			Pose pose = new Pose();
			pose.setMultistroke(new Multistroke(type, true, strokes));
			pose.setType(type);
			mPoses.add(pose);
			Collections.sort(mPoses);
			mNDollarRecognizer.setMultistrokes(this.getMultistrokePoses());
			return true;
		}
		return false;
	}
	
	public boolean deletePose(int index) {
		if (index >= mPoses.size()) {
			return false;
		}
		mPoses.remove(index);
		mNDollarRecognizer.setMultistrokes(this.getMultistrokePoses());
		return true;
	}
	
	public void setSquareSize(double size) {
		mNDollarRecognizer.setSquareSize(size);
	}
	
	public static String[] getAllDescriptions() {
		return mDescriptions.clone();
	}
	
	public static String getDescription(int type) {
		if (type == NO_POSE) {
			return "n/a";
		}
		if (type < 0 || type >= mDescriptions.length) {
			return "";
		}
		return mDescriptions[type];
	}
	
	public ArrayList<Pose> getPoses() {
		return mPoses;
	}

	/**
	 * Get Multistrokes from poses
	 * @return List of pose multistrokes
	 */
	private ArrayList<Multistroke> getMultistrokePoses() {
		ArrayList<Multistroke> poses = new ArrayList<>();
		for (Pose p : mPoses) {
			poses.add(p.getMultistroke());
		}
		return poses;
	}
	
	/**
	 * Restore saved poses from file
	 * @param ctx Application context
	 */
	public boolean restore(Context ctx) {
		boolean success = true;
		try {
			FileInputStream fis = ctx.openFileInput(FILENAME);
			ObjectInputStream ois = new ObjectInputStream(fis);
			int length = ois.readInt();
			mPoses = new ArrayList<>();
			for (int i = 0; i < length; ++i) {
				Pose p = (Pose) ois.readObject();
				mPoses.add(p);
			}
			fis.close();
		}
		catch (Exception e) {
			success = false;
		}
		if (mPoses == null) {
			mPoses = new ArrayList<>();
		}
		return success;
	}
	
	/**
	 * Save gesture collection to file
	 * @param ctx Application context
	 */
	public void save(Context ctx) {
		try {
	        FileOutputStream fos = ctx.openFileOutput(FILENAME, Context.MODE_PRIVATE);
	        ObjectOutputStream oos = new ObjectOutputStream(fos);
	        oos.writeInt(mPoses.size());
		    for (Pose p : mPoses) {
		    	oos.writeObject(p);
		    }
		    oos.flush();
	        fos.close();
	    }
	    catch (Exception e) { }
	}

	/**
	 * Import poses while the app is running
	 * @param ctx Application context
	 * @return Poses successfully imported
	 */
	public boolean importPoses(Context ctx) {
		boolean success = this.restore(ctx);
		Collections.sort(mPoses);
		mNDollarRecognizer = new NDollarRecognizer(mSquareSize, true);
		mNDollarRecognizer.setMultistrokes(this.getMultistrokePoses());
		return success;
	}
	
	/**
	 * Reset all poses
	 */
	public void reset() {
		mPoses.clear();
		mNDollarRecognizer.setMultistrokes(new ArrayList<Multistroke>());
	}
	
	
	public static class Result {
		public int pose = NO_POSE;			//type
		public boolean nonZero = false;		//set by HandDetector
		public Mat frame;					//set by HandDetector
	}
	
}