package de.lmu.ifi.medien.mime.$N;

/**
 * The $N Multistroke Recognizer (Java version)
 * Translated from JavaScript and slightly adjusted by Simon Ismair.
 * 
 * Original version:
 * https://depts.washington.edu/aimgroup/proj/dollar/ndollar.html
 *
 *	Lisa Anthony, Ph.D.
 *      UMBC
 *      Information Systems Department
 *      1000 Hilltop Circle
 *      Baltimore, MD 21250
 *      lanthony@umbc.edu
 *
 *	Jacob O. Wobbrock, Ph.D.
 * 	    The Information School
 *	    University of Washington
 *	    Seattle, WA 98195-2840
 *	    wobbrock@uw.edu
 *
 * The academic publications for the $N recognizer, and what should be 
 * used to cite it, are:
 *
 *	Anthony, L. and Wobbrock, J.O. (2010). A lightweight multistroke 
 *	  recognizer for user interface prototypes. Proceedings of Graphics 
 *	  Interface (GI '10). Ottawa, Ontario (May 31-June 2, 2010). Toronto, 
 *	  Ontario: Canadian Information Processing Society, pp. 245-252.
 *
 *	Anthony, L. and Wobbrock, J.O. (2012). $N-Protractor: A fast and 
 *	  accurate multistroke recognizer. Proceedings of Graphics Interface 
 *	  (GI '12). Toronto, Ontario (May 28-30, 2012). Toronto, Ontario: 
 *	  Canadian Information Processing Society, pp. 117-120.
 *
 * The Protractor enhancement was separately published by Yang Li and programmed 
 * here by Jacob O. Wobbrock and Lisa Anthony:
 *
 *	Li, Y. (2010). Protractor: A fast and accurate gesture
 *	  recognizer. Proceedings of the ACM Conference on Human
 *	  Factors in Computing Systems (CHI '10). Atlanta, Georgia
 *	  (April 10-15, 2010). New York: ACM Press, pp. 2169-2172.
 *
 * This software is distributed under the "New BSD License" agreement:
 *
 * Copyright (C) 2007-2011, Jacob O. Wobbrock and Lisa Anthony.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the names of UMBC nor the University of Washington,
 *      nor the names of its contributors may be used to endorse or promote
 *      products derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Lisa Anthony OR Jacob O. Wobbrock
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
**/

import java.util.ArrayList;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import de.lmu.ifi.medien.mime.PoseRecognizer;

public class NDollarRecognizer {
	
	public static double mSquareSize = 200.0;
	private boolean mUseBoundedRotationInvariance;
	
	private volatile ArrayList<Multistroke> mMultistrokes = new ArrayList<>();
	private int[] mUseIndices = new int[0];
	
	
	/**
	 * Constructor
	 * @param squareSize Length of largest side of frame
	 * @param useBoundedRotationInvariance Use rotation invariance
	 */
	public NDollarRecognizer(double squareSize, boolean useBoundedRotationInvariance) {
		mSquareSize = squareSize;
		mUseBoundedRotationInvariance = useBoundedRotationInvariance;
	}
	
	public void setMultistrokes(ArrayList<Multistroke> multistrokes) {
		mMultistrokes = multistrokes;
	}
	
	public void setUseIndices(int[] indices) {
		mUseIndices = indices;
	}
	
	/**
	 * Update the square size
	 * @param size The square size
	 */
	public void setSquareSize(double size) {
		mSquareSize = size;
		Util.mDiagonal = Math.sqrt(mSquareSize * mSquareSize + mSquareSize * mSquareSize);
		Util.mHalfDiagonal = 0.5 * Util.mDiagonal;
	}
	
	/**
	 * Try to recognize a pose from the given set of strokes
	 * @param skeleton Strokes of points
	 * @return Recognized pose
	 */
	public Result recognize(MatOfPoint skeleton) {
		if (skeleton != null && mMultistrokes.size() > 0) {
			Point[][] strokes = new Point[1][];
			strokes[0] = skeleton.toArray();
			return this.recognize(strokes, false, false);
		}
		return new Result(PoseRecognizer.NO_POSE, 0.0);
	}

	/**
	 * Recognition routine
	 * @param strokes
	 * @param requireSameNoOfStrokes
	 * @param useProtractor
	 * @return Recognition result including pose and score
	 */
	private Result recognize(Point[][] strokes, boolean requireSameNoOfStrokes, boolean useProtractor) {
		// make one connected unistroke from the given strokes
		Point[] points = Util.combineStrokes(strokes);
		points = Util.resample(points, Util.NUM_POINTS);
		double radians = Util.indicativeAngle(points);
		points = Util.rotateBy(points, -radians);
		points = Util.scaleDimTo(points, mSquareSize, Util.ONE_D_THRESHOLD);
		if (mUseBoundedRotationInvariance) {
			points = Util.rotateBy(points, radians);	// restore
		}
		points = Util.translateTo(points, Util.ORIGIN);
		Point startv = Util.calcStartUnitVector(points, Util.START_ANGLE_INDEX);
		double[] vector = Util.vectorize(points, mUseBoundedRotationInvariance);	// for Protractor
		
		double b = Double.POSITIVE_INFINITY;
		int u = -1;
		for (int i : mUseIndices) {
			if (i >= mMultistrokes.size()) {
				continue;
			}
			// optional -- only attempt match when same # of component strokes
			if (!requireSameNoOfStrokes || strokes.length == mMultistrokes.get(i).getNumStrokes()) {
				// each unistroke within this multistroke
				for (int j = 0; j < mMultistrokes.get(i).getUnistrokes().length; ++j) {
					// strokes start in the same direction
					if (Util.angleBetweenUnitVectors(startv, mMultistrokes.get(i).getUnistrokes()[j].getStartUnitVector()) <= Util.ANGLE_SIMILARITY_THRESHOLD) {
						double d;
						if (useProtractor) {
							// for Protractor
							d = Util.optimalCosineDistance(mMultistrokes.get(i).getUnistrokes()[j].getVector(), vector);
						}
						else {
							// Golden Section Search (original $N)
							d = Util.distanceAtBestAngle(points, mMultistrokes.get(i).getUnistrokes()[j], -Util.ANGLE_RANGE, +Util.ANGLE_RANGE, Util.ANGLE_PRECISION);
						}
						if (d < b) {
							b = d;	// best (least) distance
							u = i;	// multistroke owner of unistroke
						}
					}
				}
			}
		}
		Result res;
		if (u == -1) {
			res = new Result(PoseRecognizer.NO_POSE, 0.0);
		}
		else {
			res = new Result(mMultistrokes.get(u).getType(), useProtractor ? 1.0 / b : 1.0 - b / Util.mHalfDiagonal);
		}
		return res;
	}


	/**
	 * Container for recognition result
	 */
	public static class Result {
		public int pose;
		public double score;
		public boolean result = false;
		
		public Result(int pose, double score) {
			this.pose = pose;
			this.score = score;
			this.result = true;
		}
	}
	
}