package de.lmu.ifi.medien.mime;

import java.util.ArrayList;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import android.content.Context;
import de.lmu.ifi.medien.mime.PoseRecognizer.Result;

/**
 * Performs hand detection using background subtraction and color blob detection (each in HSV color space) on a camera preview frame using OpenCV
 */
public class HandDetector {
	
	// Preferences
	private int mPrefTargetWidth = 160;			// Desired width of frame being processed
	private double mPrefThresholdH = 50.0;		// Distance threshold for H channel
	private double mPrefThresholdS = 45.0;		// Distance threshold for S channel
	private double mPrefThresholdV = 10.0;		// Distance threshold for V channel
	private int mPrefThresholdLowerH = 30;		// Lower threshold (H channel) for color blob detection (0 - mPrefThresholdLowerH)
	private int mPrefThresholdUpperH = 225;		// Upper threshold (H channel) for color blob detection (mPrefThresholdUpperH - 255)
	private int mPrefThresholdBlobS = 128;		// Threshold (S channel) for color bloc detection (0 - mPrefThresholdBlobS)
	private int mPrefWeightingH = 1;			// Weighting for result from H channel
	private int mPrefWeightingS = 1;			// Weighting for result from S channel
	private int mPrefWeightingV = 1;			// Weighting for result from V channel
	private int mPrefWeightingB = 3;			// Weighting for result from blob detection
	private int mPrefWeightingThreshold = 3;	// Weighting threshold
	private double mPrefMinimumArea = 200.0;	// Minimum area of a detected contour
	private double mPrefMinFingerDepth = 20.0;	// Minimum distance between inner convexity defect point and border of convex hull
	private double mPrefMaxFingerAngle = 120.0;	// Maximum angle between two fingers
	private double mPrefMinDepth = 20.0;		// Threshold depth for a convexity defect to be flood filled
	private double mPrefMinHoleArea = 750.0;	// Minimum hole area (about 1000-1200 if mPrefTargetWidth = 200)
	
	// Fields used for background subtraction
	private byte[] mBackground;
    private boolean mSetBackground = false;
    
    // Cached values and objects
    private boolean mInitialized = false;
    private Size mFrameSize;
    private Size mFrameSizeRot;
    private Size mScaledSize;
    private Size mScaledSizeRot;
    private Mat mEmptyMatRot;
    private Mat mOpeningKernel;
    private Mat mCachedRotFrame;
    private Mat mCachedResultFrame;
    private Mat mCachedScaledFrame;
    
    // Colors
    private static final Scalar BLACK = new Scalar(0);
    private static final Scalar WHITE = new Scalar(255);
    private static final Scalar GRAY = new Scalar(192);
    
    // Pose recognizing and recording
    private PoseRecognizer mRecognizer = null;
    private MatOfPoint mRecordedSkeleton;
    private volatile boolean mRecordPose = false;
    private volatile boolean mRecordMode = false;
    
    private BackgroundHolder mBackgroundHolder;
    
    private static HandDetector instance = null;
    
    
    private HandDetector() {
    	mBackgroundHolder = BackgroundHolder.getInstance();
    }
    
    /**
	 * Returns the singleton instance of this class
	 * @return Instance of this class
	 */
    public static HandDetector getInstance() {
    	if (instance == null) {
    		instance = new HandDetector();
    	}
    	return instance;
    }
	
    
    /**
     * Detects hand shape in a OpenCV image (= camera frame); optionally also finds number of fingers
     * @param frame Camera frame encoded as OpenCV Mat
     * @param recognize Run recognizer
     * @return Container with detected pose (if any), processed frame and additional info
     */
	public Result detect(Mat frame, boolean recognize) {
		// Initialize cached settings
		if (!mInitialized) {
			mInitialized = true;
			
			mFrameSize = frame.size();
			mFrameSizeRot = new Size(mFrameSize.height, mFrameSize.width);
			int scaleFactor = Math.max(1, (int) Math.floor(mFrameSize.width / mPrefTargetWidth));
			mScaledSize = new Size(mFrameSize.width/scaleFactor, mFrameSize.height/scaleFactor);
			mScaledSizeRot = new Size(mScaledSize.height, mScaledSize.width);
			
			if (mRecognizer != null) {
				mRecognizer.setSquareSize(this.getFrameSize());
			}
			
			mEmptyMatRot = OpenCVUtil.getFrame(mFrameSizeRot, BLACK);
			mOpeningKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
			
			mCachedRotFrame = new Mat();
			mCachedResultFrame = new Mat();
			mCachedScaledFrame = new Mat();
		}
    	
    	// No background set yet, do nothing
    	if (mBackground == null && !mSetBackground) {
    		OpenCVUtil.rotate(frame, mCachedRotFrame);
    		Result result = new Result();
    		result.frame = mCachedRotFrame;
    		return result;
    	}
		
		// Scale down
    	Imgproc.resize(frame, mCachedScaledFrame, mScaledSize, 0, 0, Imgproc.INTER_CUBIC);
    	frame.release();
    	frame = null;
    	
    	// Convert to HSV
        Imgproc.cvtColor(mCachedScaledFrame, mCachedScaledFrame, Imgproc.COLOR_RGB2HSV, 3);
        
        // Set background
    	if (mSetBackground) {
    		mSetBackground = false;
    		if (mBackgroundHolder.useSavedBackground()) {
    			mBackground = mBackgroundHolder.getBackground();
    			if (mBackground == null) {
    				mBackground = new byte[(int) mCachedScaledFrame.total() * mCachedScaledFrame.channels()];
        			mCachedScaledFrame.get(0, 0, mBackground);
        			mBackgroundHolder.addBackgroundFrame(mBackground);
    			}
    		}
    		else {
    			mBackground = new byte[(int) mCachedScaledFrame.total() * mCachedScaledFrame.channels()];
    			mCachedScaledFrame.get(0, 0, mBackground);
    		}
    	}
    	
    	// Get raw image data
        int channels = mCachedScaledFrame.channels();
    	byte[] inputBuffer = new byte[(int) mCachedScaledFrame.total() * channels];
    	byte[] diffBuffer = new byte[(int) mCachedScaledFrame.total()];
    	mCachedScaledFrame.get(0, 0, inputBuffer);
    	
    	// Background averaging
    	if (mBackgroundHolder.isBackgroundRecordingEnabled()) {
    		mBackgroundHolder.addBackgroundFrame(inputBuffer);
    	}
    	
    	// Background subtraction with thresholding
        for (int i = 0; i < inputBuffer.length; i += channels) {
        	byte value = 0;
        	
        	// Different thresholds for each channel
        	if (Math.sqrt((inputBuffer[i] - mBackground[i]) * (inputBuffer[i] - mBackground[i])) > mPrefThresholdH) {
        		value += mPrefWeightingH;
        	}
        	if (Math.sqrt((inputBuffer[i+1] - mBackground[i+1]) * (inputBuffer[i+1] - mBackground[i+1])) > mPrefThresholdS) {
        		value += mPrefWeightingS;
        	}
    		if (Math.sqrt((inputBuffer[i+2] - mBackground[i+2]) * (inputBuffer[i+2] - mBackground[i+2])) > mPrefThresholdV) {
    			value += mPrefWeightingV;
    		}
    		// Color blob detection
        	if ((inputBuffer[i] <= mPrefThresholdLowerH || inputBuffer[i] >= mPrefThresholdUpperH) && inputBuffer[i+1] <= mPrefThresholdBlobS) {
        		value += mPrefWeightingB;
        	}
        	
        	// Set pixel in 8 bit single channel image
        	diffBuffer[i/channels] = value >= mPrefWeightingThreshold ? (byte) 255 : 0;
        }
        
        // Create image from buffer
        Mat diff = new Mat(mScaledSize, CvType.CV_8U);
        diff.put(0, 0, diffBuffer);
        
        // Opening (remove smaller specks)
        Imgproc.morphologyEx(diff, diff, Imgproc.MORPH_OPEN, mOpeningKernel);
        
        // Find contours, including holes
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(diff, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
       	diff.release();
       	diff = null;
        
        // Nothing found, return empty frame
        if (contours.isEmpty()) {
        	Result result = new Result();
    		result.frame = mEmptyMatRot;
    		return result;
        }
        
        // Find all contours that exceed minimum area threshold
        ArrayList<MatOfPoint2f> biggestContours2f = new ArrayList<>();
        for (MatOfPoint contour : contours) {
        	if (Imgproc.contourArea(contour) > mPrefMinimumArea) {
        		// Rotate points so we don't have to rotate an entire rendered frame
        		biggestContours2f.add(OpenCVUtil.convert(OpenCVUtil.rotatePoints(contour, mScaledSize)));
        	}
        }
        contours.clear();
        contours = null;
        
        // No detected area exceeds minimum area threshold
        if (biggestContours2f.size() == 0) {
        	Result result = new Result();
    		result.frame = mEmptyMatRot;
    		return result;
        }
        
    	// Convert contours (these different incompatible Mat types in OpenCV are just stupid oO)
        ArrayList<MatOfPoint> biggestContours = new ArrayList<>();
        ArrayList<MatOfPoint> biggestContoursToDraw = new ArrayList<>();
        for (MatOfPoint2f contour2f : biggestContours2f) {
        	MatOfPoint2f newContour2f = new MatOfPoint2f();
        	Imgproc.approxPolyDP(contour2f, newContour2f, 4.5, true);
        	biggestContours.add(OpenCVUtil.convert(newContour2f));
        	biggestContoursToDraw.add(OpenCVUtil.convert(contour2f));
        	contour2f.release();
        }
        biggestContours2f.clear();
        biggestContours2f = null;
        
        // Render resulting contours
    	Mat contourFrame;
    	Mat skeletonFrame;
    	if (mRecordMode) {
    		contourFrame = OpenCVUtil.getFrame(mScaledSizeRot, BLACK);
	    	Imgproc.drawContours(contourFrame, biggestContours, -1, WHITE, -1);
	        skeletonFrame = contourFrame.clone();
    	}
    	else {
    		contourFrame = OpenCVUtil.getFrame(mScaledSizeRot, BLACK);
    		Imgproc.drawContours(contourFrame, biggestContoursToDraw, -1, WHITE, -1);
	        skeletonFrame = OpenCVUtil.getFrame(mScaledSizeRot, BLACK);
	        Imgproc.drawContours(skeletonFrame, biggestContours, -1, WHITE, -1);
    	}
    	
    	PoseFeatures mergedFeatures = null;
    	if (mRecordMode || recognize) {
	    	// Extract features
	    	ArrayList<PoseFeatures> features = new ArrayList<>();
	    	for (MatOfPoint contour : biggestContours) {
	    		PoseFeatures f = this.extractFeatures(contour, skeletonFrame);
	    		if (f != null) {
	    			features.add(f);
	    		}
	        	contour.release();
	        }
	        for (MatOfPoint contour : biggestContoursToDraw) {
	        	contour.release();
	        }
	        biggestContours.clear();
	        biggestContours = null;
	        biggestContoursToDraw.clear();
	        biggestContoursToDraw = null;
	        
	        // Merge feature vectors
	        if (features.size() == 1) {
	        	mergedFeatures = features.get(0);
	        }
	        else if (features.size() == 0) {
	        	mergedFeatures = new PoseFeatures();
	        }
	        else {
	        	mergedFeatures = new PoseFeatures();
	        	mergedFeatures.topScreen = features.get(0).topScreen;
	        	mergedFeatures.leftScreen = features.get(0).leftScreen;
	        	for (PoseFeatures f : features) {
	        		mergedFeatures.fingerDefects += f.fingerDefects;
	        		mergedFeatures.defectBisectAngles.addAll(f.defectBisectAngles);
	        		mergedFeatures.numHoles += f.numHoles;
	        		mergedFeatures.narrowVertAngle |= f.narrowVertAngle;
	        	}
	        }
    	}
        
        // Get skeleton of shapes, then get coordinates of skeleton lines
        OpenCVUtil.zhangSuenThinning(skeletonFrame);
        ArrayList<MatOfPoint> skeletonContours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(skeletonFrame, skeletonContours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        skeletonFrame.release();
        skeletonFrame = null;
        
        Result result = null;
        if (mRecordMode || recognize) {
	        // Count number of holes
	        int holeCount = 0;
	        if (!hierarchy.empty()) {
	        	int[] hierarchyBuffer = new int[(int) (hierarchy.total() * hierarchy.channels())];
	        	hierarchy.get(0, 0, hierarchyBuffer);
		        for (int i = 0; i < hierarchyBuffer.length; i += hierarchy.channels()) {
		        	if (hierarchyBuffer[i + 3] >= 0 && Imgproc.contourArea(skeletonContours.get(i % 4)) > mPrefMinHoleArea) {
		        		++holeCount;
		        	}
		        }
	        }
	        mergedFeatures.numHoles = holeCount;
        }
	        
        // Find longest contour
        MatOfPoint skeleton = new MatOfPoint();
        if (skeletonContours.size() > 0) {
        	double maxL = Imgproc.arcLength(OpenCVUtil.convert(skeletonContours.get(0)), true);
        	MatOfPoint max = skeletonContours.get(0);
	        for (MatOfPoint mop : skeletonContours) {
		        double l = Imgproc.arcLength(OpenCVUtil.convert(mop), true);
		        if (l > maxL) {
		        	max = mop;
		        	maxL = l;
		        }
		    }
	        skeleton = max;
	        
	        if (mRecordMode || recognize) {
	        	mergedFeatures.skeleton = max;
	        }
        }
	    
        // Record the current pose
        if (mRecordPose) {
        	mRecordPose = false;
        	mRecordedSkeleton = skeleton;
        }
	        
        // Try to recognize poses
        if ((mRecordMode || recognize) && mRecognizer != null) {
        	result = mRecognizer.recognize(mergedFeatures);
        }
        
        if (result == null) {
        	result = new Result();
        }
        
        // There must have been detected shapes, otherwise we couldn't have reached this code
        result.nonZero = true;
        
        // Draw skeleton lines
        OpenCVUtil.drawContour(contourFrame, skeleton, GRAY, false);
       	
        // Scale up
        Imgproc.resize(contourFrame, mCachedResultFrame, mFrameSizeRot, 0, 0, Imgproc.INTER_CUBIC);
        contourFrame.release();
        contourFrame = null;
        
        // Whew! :)
        result.frame = mCachedResultFrame;
        return result;
	}

	/**
	 * Extracts various features from a given contour
	 * @param contour
	 * @param frame
	 * @return Extracted features
	 */
	private PoseFeatures extractFeatures(MatOfPoint contour, Mat frame) {
		MatOfInt4 convexityDefects = new MatOfInt4();
		MatOfInt hullIndices = new MatOfInt();
		
		// Fill shallow convexity defects
		Imgproc.convexHull(contour, hullIndices);
		MatOfPoint convexHull = OpenCVUtil.getNewContourFromIndices(contour, hullIndices);
		if (hullIndices.size().height <= 3) {
			return null;
		}
		try {
			Imgproc.convexityDefects(contour, hullIndices, convexityDefects);
		}
		catch (Exception e) { }
		hullIndices.release();
		hullIndices = null;
		
		PoseFeatures features = new PoseFeatures();
		
		if (convexityDefects.size().width > 0) {
			// Draw convex hull as boundary
			OpenCVUtil.drawContour(frame, convexHull, WHITE, false);
			
			// Retrieve array of joined 4 element vectors describing a convexity defect (start point, end point, defect point, defect depth)
			int[] convexityDefectsInt = convexityDefects.toArray();
			convexityDefects.release();
			convexityDefects = null;
			
			Point[] contourPoints = contour.toArray();
			for (int i = 0; i < convexityDefectsInt.length; i += 4) {
				// Check distance to convex hull
				double convexityDepth = (double) convexityDefectsInt[i+3] / 256.0;
				
				// Get points and check angle
				Point start = contourPoints[convexityDefectsInt[i]];
				Point end = contourPoints[convexityDefectsInt[i+1]];
				Point inner = contourPoints[convexityDefectsInt[i+2]];
				
				double defAngle = OpenCVUtil.angle(start, inner, end);
				if (convexityDepth < mPrefMinDepth) {
					// Get centroid for flood filling this defect
					double centroidX = (start.x + end.x + inner.x) / 3;
					double centroidY = (start.y + end.y + inner.y) / 3;
					Imgproc.floodFill(frame, new Mat(), new Point(centroidX, centroidY), WHITE);
				}
				else if (convexityDepth > mPrefMinFingerDepth && defAngle < mPrefMaxFingerAngle) {
					// Count fingers and get angles
					++features.fingerDefects;
					double angle = OpenCVUtil.angleBisectionAngle(start, inner, end);
					features.defectBisectAngles.add(angle);
					features.defectAngles.add(defAngle);
					
					if (angle > 75 && angle < 105 && defAngle < 60) {
						features.narrowVertAngle = true;
					}
				}
			}
			
			// Reset boundary
			OpenCVUtil.drawContour(frame, convexHull, BLACK, false);
		}
		
		// Calculate moments to find center of gravity (and the corresponding screen quadrant)
		Moments m = Imgproc.moments(contour, true);
		double gravityCenterX = m.get_m10() / m.get_m00();
		double gravityCenterY = m.get_m01() / m.get_m00();
		if (gravityCenterX < mScaledSizeRot.width/2) {
			features.leftScreen = true;
		}
		if (gravityCenterY < mScaledSizeRot.height/2) {
			features.topScreen = true;
		}
		
		return features;
	}
	
	
	/**
	 * Indicates that a new background should be set
	 * @param value Set a new background
	 */
	public void setBackground(boolean value) {
		mSetBackground = value;
	}
	
	/**
	 * Checks if the background is set
	 * @return Background is set
	 */
	public boolean isBackgroundSet() {
		return mBackground != null;
	}
	
	/**
	 * Record the current pose
	 */
	public void recordPose() {
		mRecordPose = true;
	}
	
	/**
	 * Save the recorded pose
	 * @param type Pose type
	 */
	public void savePose(int type) {
		mRecognizer.addPose(type, mRecordedSkeleton);
	}
	
	/**
	 * Enable/disable recording mode
	 * @param enable Enable recording mode
	 */
	public void setRecordingMode(boolean enable) {
		mRecordMode = enable;
		if (enable && mBackground == null && !mSetBackground) {
			mSetBackground = true;
		}
	}
	
	/**
	 * Returns the length of the largest side of the frames being processed
	 * @return Length of largest size of scaled down frames  
	 */
	public double getFrameSize() {
		if (!mInitialized) {
			return (double) mPrefTargetWidth;
		}
		return Math.max(mScaledSize.width, mScaledSize.height);
	}
	
	/**
	 * Adds reference to an instance of the PoseRecognizer
	 * @param recognizer The recognizer
	 */
	public void setRecognizer(PoseRecognizer recognizer) {
		mRecognizer = recognizer;
		mRecognizer.setSquareSize(this.getFrameSize());
	}
	
	/**
	 * Cleans local fields and frees resources
	 */
	public void clean() {
		if (mBackground != null) {
        	mBackground = null;
        }
		if (mEmptyMatRot != null) {
			mEmptyMatRot.release();
			mEmptyMatRot = null;
		}
		if (mOpeningKernel != null) {
			mOpeningKernel.release();
			mOpeningKernel = null;
		}
		if (mCachedRotFrame != null) {
			mCachedRotFrame.release();
			mCachedRotFrame = null;
		}
		if (mCachedResultFrame != null) {
			mCachedResultFrame.release();
			mCachedResultFrame = null;
		}
		if (mCachedScaledFrame != null) {
			mCachedScaledFrame.release();
			mCachedScaledFrame = null;
		}
		mInitialized = false;
        mSetBackground = false;
	}
	
	public void updatePrefs(Context ctx) {
		int[] prefs = PreferenceHelper.getInstance(ctx).getPrefs();
		mPrefThresholdLowerH = prefs[PreferenceHelper.PREF_THRESHOLD_LOWER_H];
		mPrefThresholdUpperH = prefs[PreferenceHelper.PREF_THRESHOLD_UPPER_H];
		mPrefWeightingH = prefs[PreferenceHelper.PREF_WEIGHTING_H];
		mPrefWeightingS = prefs[PreferenceHelper.PREF_WEIGHTING_S];
		mPrefWeightingV = prefs[PreferenceHelper.PREF_WEIGHTING_V];
		mPrefWeightingB = prefs[PreferenceHelper.PREF_WEIGHTING_B];
		mPrefWeightingThreshold = prefs[PreferenceHelper.PREF_WEIGHTING_THRESHOLD];
	}
	
	public boolean importPoses(Context ctx) {
		if (mRecognizer != null) {
			return mRecognizer.importPoses(ctx);
		}
		return false;
	}
	
	
	public class PoseFeatures {
		public static final int NO_DIRECTION = -1;
		public static final int HORIZONTAL   = 0;
		public static final int VERTICAL     = 1;
		
		public int fingerDefects = 0;
		public ArrayList<Double> defectBisectAngles = new ArrayList<>();
		public ArrayList<Double> defectAngles = new ArrayList<>();
		public int numHoles = 0;
		public MatOfPoint skeleton = null;
		public boolean topScreen = false;
		public boolean leftScreen = false;
		public boolean narrowVertAngle = false;
		
		public int getDirection() {
			if (defectBisectAngles.size() == 0) {
				return NO_DIRECTION;
			}
			ArrayList<Integer> dirs = new ArrayList<>();
			for (double d : defectBisectAngles) {
				if (d <= 45 || d >= 135) {
					dirs.add(HORIZONTAL);
				}
				else {
					dirs.add(VERTICAL);
				}
			}
			if (dirs.size() == 1) {
				return dirs.get(0);
			}
			double avgDir = 0;
			for (int dir : dirs) {
				avgDir += (double) dir;
			}
			return (int) Math.round(avgDir / dirs.size());
		}
		
		public boolean checkForU() {
			for (int i = 0; i < defectAngles.size(); ++i) {
				if (defectBisectAngles.get(i) > 65 && defectBisectAngles.get(i) < 100 && defectAngles.get(i) > 60 && defectAngles.get(i) < 90) {
					return true;
				}
			}
			return false;
		}
	}
	
}