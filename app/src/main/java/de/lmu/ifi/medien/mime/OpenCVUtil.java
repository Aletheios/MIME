package de.lmu.ifi.medien.mime;

import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class OpenCVUtil {

	/**
	 * Returns a frame of the specified size and color (image with a single 8 bit channel)
	 * @param size Desired frame size
	 * @param color Desired fill color
	 * @return The frame
	 */
	public static Mat getFrame(Size size, Scalar color) {
		Mat frame = new Mat(size, CvType.CV_8UC1);
    	frame.setTo(color);
    	return frame;
	}
	
	/**
	 * Rotates a Mat and releases the source Mat
	 * @param src Input Mat, will be released
	 * @param dest Rotated output Mat
	 */
	public static void rotate(Mat src, Mat dest) {
		// Rotate = transpose + flip
		Core.transpose(src, dest);
		Core.flip(dest, dest, 1);
		src.release();
		src = null;
	}
	
	/**
	 * Manually rotates points of a MatOfPoint(2f) and converts it into a MatOfPoint for further usage
	 * @param mop MatOfPoint(2f)
	 * @param size Size of frame before rotation
	 * @return Rotated MatOfPoint
	 */
	public static MatOfPoint rotatePoints(MatOfPoint mop, Size size) {
		return rotatePoints(mop.toArray(), size);
	}
	private static MatOfPoint rotatePoints(Point[] points, Size size) {
		double height = size.height;
		for (Point p : points) {
			// Rotate = transpose + flip
			double tmp = p.x;
			p.x = height - p.y;
			p.y = tmp;
		}
		return new MatOfPoint(points);
	}
	
	/**
	 * Wrapper function around Imgproc.drawContours to draw a single contour
	 * @param image The image
	 * @param contour The contour
	 * @param color The color
	 * @param fill Fill the contour or paint strokes only
	 */
	public static void drawContour(Mat image, MatOfPoint contour, Scalar color, boolean fill) {
		ArrayList<MatOfPoint> tmp = new ArrayList<>();
		tmp.add(contour);
		Imgproc.drawContours(image, tmp, 0, color, fill ? -1 : 1);
	}
	
	/**
	 * Creates a new simplified contour from an original contour by extracting points defined by their indices in the original contour
	 * @param origContour The original contour
	 * @param indices Indices of points to extract
	 */
	public static MatOfPoint getNewContourFromIndices(MatOfPoint origContour, MatOfInt indices) {
		int height = (int) indices.size().height;
		MatOfPoint2f newContour = new MatOfPoint2f();
		newContour.create(height, 1, CvType.CV_32FC2);
		for (int i = 0; i < height; ++i) {
			int index = (int) indices.get(i, 0)[0];
			double[] point = new double[] {
				origContour.get(index, 0)[0], 
				origContour.get(index, 0)[1]
			};
			newContour.put(i, 0, point);
		}
		return convert(newContour);
	}
	
	/**
	 * Converts from MatOfPoint to MatOfPoint2f and vice versa
	 * @param mat Input Mat
	 * @return Converted Mat
	 */
	public static MatOfPoint2f convert(MatOfPoint mat) {
		return new MatOfPoint2f(mat.toArray());
	}
	public static MatOfPoint convert(MatOfPoint2f mat) {
		return new MatOfPoint(mat.toArray());
	}
	
	/**
	 * Calculates the angle described by two points and one center point
	 * @param p1 First point
	 * @param center Center point
	 * @param p2 Second point
	 * @return Angle in degrees
	 */
	public static double angle(Point p1, Point center, Point p2) {
		Point v1 = new Point(p1.x-center.x, p1.y-center.y);
		Point v2 = new Point(p2.x-center.x, p2.y-center.y);
		
		double len1 = Math.sqrt(v1.x*v1.x + v1.y*v1.y);
		double len2 = Math.sqrt(v2.x*v2.x + v2.y*v2.y);
		double dot = v1.x*v2.x + v1.y*v2.y;
		
		double a = dot / (len1*len2);

		if (a >= 1.0) {
			return 0.0;
		}
		if (a <= -1.0) {
			return 180.0;
		}
		return Math.acos(a) * (180.0/Math.PI);
	}

	/**
	 * Calculates the angle described by a vector (p1 p2) and a third point, with the bisection of (p1 p2) as center of the angle
	 * @param p1 First point
	 * @param center Third point
	 * @param p2 Second point
	 * @return Angle in degrees
	 */
	public static double angleBisectionAngle(Point p1, Point center, Point p2) {
		Point vector = new Point(p2.x-p1.x, p2.y-p1.y);
		Point bisection = new Point(p1.x + 0.5*vector.x, p1.y + 0.5*vector.y);
		return angle(new Point(bisection.x + 1.0, bisection.y), bisection, center);
	}
	
	/**
	 * Implements the Zhang-Suen thinning algorithm to get the topological skeleton of a binary image
	 * @param img The binary image to process (filled pixels = white); method works in-place
	 * @link http://dl.acm.org/citation.cfm?id=358023
	 * @link http://nayefreza.wordpress.com/2013/05/11/zhang-suen-thinning-algorithm-java-implementation/
	 * @link http://opencv-code.com/quick-tips/implementation-of-thinning-algorithm-in-opencv/
	 * @link http://en.wikipedia.org/wiki/Topological_skeleton
	 */
	public static void zhangSuenThinning(Mat img) {
		Mat prev = Mat.zeros(img.size(), CvType.CV_8UC1);
		Mat diff = new Mat();
		do {
			zhangSuenThinningIteration(img, 0);
			zhangSuenThinningIteration(img, 1);
			Core.absdiff(img, prev, diff);
	        img.copyTo(prev);
	    }
	    while (Core.countNonZero(diff) > 0);
	}

	/**
	 * Iteration step for the Zhang-Suen thinning algorithm
	 * @param img
	 * @param step
	 */
	private static void zhangSuenThinningIteration(Mat img, int step) {
		// Get image pixels
	    byte[] buffer = new byte[(int) img.total() * img.channels()];
	    img.get(0, 0, buffer);
	    
	    byte[] markerBuffer = new byte[buffer.length];
	    
		int rows = img.rows();
		int cols = img.cols();
		
		// Process all pixels
	    for (int y = 1; y < rows-1; ++y) {
			for (int x = 1; x < cols-1; ++x) {
				// Pre-calculate offsets (indices in buffer)
				int prev = cols*(y-1) + x;
				int cur  = cols*y     + x;
				int next = cols*(y+1) + x;
				
				// Get 8-neighborhood of current pixel (center = p1; p2 = top middle, counting clockwise)
				byte p2 = buffer[prev];
				byte p3 = buffer[prev + 1];
				byte p4 = buffer[cur  + 1];
				byte p5 = buffer[next + 1];
				byte p6 = buffer[next];
				byte p7 = buffer[next - 1];
				byte p8 = buffer[cur  - 1];
				byte p9 = buffer[prev - 1];
				
				// Get number of black-white transitions in ordered sequence of points in the 8-neighborhood; note: a filled pixel (white) has a value of -1
				int a = 0;
				if (p2 == 0 && p3 == -1) {
					++a;
				}
				if (p3 == 0 && p4 == -1) {
					++a;
				}
				if (p4 == 0 && p5 == -1) {
					++a;
				}
				if (p5 == 0 && p6 == -1) {
					++a;
				}
				if (p6 == 0 && p7 == -1) {
					++a;
				}
				if (p7 == 0 && p8 == -1) {
					++a;
				}
				if (p8 == 0 && p9 == -1) {
					++a;
				}
				if (p9 == 0 && p2 == -1) {
					++a;
				}
				
				// Number of filled pixels in the 8-neighborhood
				int b  = Math.abs(p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9);
				
				// Condition 3 and 4
				int c3 = step == 0 ? (p2 * p4 * p6) : (p2 * p4 * p8);
				int c4 = step == 0 ? (p4 * p6 * p8) : (p2 * p6 * p8);
				
				// Determine if the current pixel has to be eliminated; 0 = "delete pixel", -1 = "keep pixel"
				markerBuffer[cur] = (byte) ((a == 1 && b >= 2 && b <= 6 && c3 == 0 && c4 == 0) ? 0 : -1);
			}
		}
	    
	    // Eliminate pixels and save result
	    for (int i = 0; i < buffer.length; ++i) {
	    	buffer[i] = (byte) ((buffer[i] == -1 && markerBuffer[i] == -1) ? -1 : 0);
	    }
	    img.put(0, 0, buffer);
	}
	
}