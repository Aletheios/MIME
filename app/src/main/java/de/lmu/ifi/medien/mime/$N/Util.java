package de.lmu.ifi.medien.mime.$N;

import java.util.ArrayList;
import java.util.Collections;

import org.opencv.core.Point;

public class Util {
	
	public static final int NUM_POINTS = 96;
	public static final double ONE_D_THRESHOLD = 0.25;	// customize to desired gesture set (usually 0.20 - 0.35)
	public static final Point ORIGIN = new Point(0, 0);
	public static final double PHI = 0.5 * (-1.0 + Math.sqrt(5.0));	// Golden Ratio
	public static final double ANGLE_RANGE = Math.toRadians(45.0);
	public static final double ANGLE_PRECISION = Math.toRadians(2.0);
	public static final int START_ANGLE_INDEX = NUM_POINTS / 8;	// eighth of gesture length
	public static final double ANGLE_SIMILARITY_THRESHOLD = Math.toRadians(30.0);

	public static double mDiagonal = Math.sqrt(NDollarRecognizer.mSquareSize * NDollarRecognizer.mSquareSize + NDollarRecognizer.mSquareSize * NDollarRecognizer.mSquareSize);
	public static double mHalfDiagonal = 0.5 * mDiagonal;
	
	
	public static void heapPermute(int n, int[] order, ArrayList<Integer[]> orders) {
		if (n == 1) {
			Integer[] orderClone = new Integer[order.length];
			for (int i = 0; i < order.length; ++i) {
				orderClone[i] = order[i];
			}
			orders.add(orderClone);
		}
		else {
			for (int i = 0; i < n; ++i) {
				heapPermute(n - 1, order, orders);
				if (n % 2 == 1) {
					int tmp = order[0];
					order[0] = order[n - 1];
					order[n - 1] = tmp;
				}
				else {
					int tmp = order[i];
					order[i] = order[n - 1];
					order[n - 1] = tmp;
				}
			}
		}
	}
	
	public static Point[][] makeUnistrokes(Point[][] strokes, int[][] orders) {
		ArrayList<Point[]> unistrokes = new ArrayList<>();
		for (int r = 0; r < orders.length; ++r) {
			for (int b = 0; b < Math.pow(2, orders[r].length); ++b) {
				ArrayList<Point> unistroke = new ArrayList<>();
				for (int i = 0; i < orders[r].length; ++i) {
					Point[] pts;
					// is b's bit at index i on?
					if (((b >> i) & 1) == 1) {
						// copy and reverse
						pts = strokes[orders[r][i]].clone();
						for (int a = 0; a < pts.length / 2; ++a) {
						    Point tmp = pts[a];
						    pts[a] = pts[pts.length - a - 1];
						    pts[pts.length - a - 1] = tmp;
						}
					}
					else {
						pts = strokes[orders[r][i]].clone();
					}
					// append points
					Collections.addAll(unistroke, pts);
				}
				// add one unistroke to set
				Point[] array = new Point[unistroke.size()];
				unistroke.toArray(array);
				unistrokes.add(array);
			}
		}
		Point[][] array = new Point[unistrokes.size()][];
		unistrokes.toArray(array);
		return array;
	}
	
	public static Point[] combineStrokes(Point[][] strokes) {
		ArrayList<Point> points = new ArrayList<>();
		for (int s = 0; s < strokes.length; ++s) {
			for (int p = 0; p < strokes[s].length; ++p) {
				points.add(new Point(strokes[s][p].x, strokes[s][p].y));
			}
		}
		Point[] ret = new Point[points.size()];
		return points.toArray(ret);
	}
	
	public static Point[] resample(Point[] points, int n) {
		double I = pathLength(points) / (n - 1);	// interval length
		double D = 0.0;
		ArrayList<Point> oldpoints = new ArrayList<>();
		Collections.addAll(oldpoints, points);
		ArrayList<Point> newpoints = new ArrayList<>();
		newpoints.add(oldpoints.get(0));
		int len = oldpoints.size();
		for (int i = 1; i < len; ++i) {
			double d = distance(oldpoints.get(i - 1), oldpoints.get(i));
			if ((D + d) >= I) {
				double qx = oldpoints.get(i - 1).x + ((I - D) / d) * (oldpoints.get(i).x - oldpoints.get(i - 1).x);
				double qy = oldpoints.get(i - 1).y + ((I - D) / d) * (oldpoints.get(i).y - oldpoints.get(i - 1).y);
				Point q = new Point(qx, qy);
				newpoints.add(q);
				// insert 'q' at position i in points s.t. 'q' will be the next i
				oldpoints.add(i, q);
				len = oldpoints.size();
				D = 0.0;
			}
			else {
				D += d;
			}
		}
		// somtimes we fall a rounding-error short of adding the last point, so add it if so
		if (newpoints.size() == n - 1) {
			newpoints.add(new Point(oldpoints.get(oldpoints.size() - 1).x, oldpoints.get(oldpoints.size() - 1).y));
		}
		Point[] array = new Point[newpoints.size()];
		newpoints.toArray(array);
		return array;
	}
	
	public static double indicativeAngle(Point[] points) {
		Point c = centroid(points);
		return Math.atan2(c.y - points[0].y, c.x - points[0].x);
	}
	
	// rotates points around centroid
	public static Point[] rotateBy(Point[] points, double radians) {
		Point c = centroid(points);
		double cos = Math.cos(radians);
		double sin = Math.sin(radians);
		ArrayList<Point> newpoints = new ArrayList<>();
		for (int i = 0; i < points.length; ++i) {
			double qx = (points[i].x - c.x) * cos - (points[i].y - c.y) * sin + c.x;
			double qy = (points[i].x - c.x) * sin + (points[i].y - c.y) * cos + c.y;
			newpoints.add(new Point(qx, qy));
		}
		Point[] array = new Point[newpoints.size()];
		newpoints.toArray(array);
		return array;
	}
	
	// scales bbox uniformly for 1D, non-uniformly for 2D
	public static Point[] scaleDimTo(Point[] points, double size, double ratio1D) {
		Rectangle b = boundingBox(points);
		boolean uniformly = Math.min(b.width / b.height, b.height / b.width) <= ratio1D; // 1D or 2D gesture test
		ArrayList<Point> newpoints = new ArrayList<>();
		for (int i = 0; i < points.length; ++i) {
			double qx = uniformly ? points[i].x * (size / Math.max(b.width, b.height)) : points[i].x * (size / b.width);
			double qy = uniformly ? points[i].y * (size / Math.max(b.width, b.height)) : points[i].y * (size / b.height);
			newpoints.add(new Point(qx, qy));
		}
		Point[] array = new Point[newpoints.size()];
		newpoints.toArray(array);
		return array;
	}
	
	// translates points' centroid
	public static Point[] translateTo(Point[] points, Point pt) {
		Point c = centroid(points);
		ArrayList<Point> newpoints = new ArrayList<>();
		for (int i = 0; i < points.length; ++i) {
			double qx = points[i].x + pt.x - c.x;
			double qy = points[i].y + pt.y - c.y;
			newpoints.add(new Point(qx, qy));
		}
		Point[] array = new Point[newpoints.size()];
		newpoints.toArray(array);
		return array;
	}
	
	// for Protractor
	public static double[] vectorize(Point[] points, boolean useBoundedRotationInvariance) {
		double cos = 1.0;
		double sin = 0.0;
		if (useBoundedRotationInvariance) {
			double iAngle = Math.atan2(points[0].y, points[0].x);
			double baseOrientation = (Math.PI / 4.0) * Math.floor((iAngle + Math.PI / 8.0) / (Math.PI / 4.0));
			cos = Math.cos(baseOrientation - iAngle);
			sin = Math.sin(baseOrientation - iAngle);
		}
		double sum = 0.0;
		ArrayList<Double> vector = new ArrayList<>();
		for (int i = 0; i < points.length; ++i) {
			double newX = points[i].x * cos - points[i].y * sin;
			double newY = points[i].y * cos + points[i].x * sin;
			vector.add(newX);
			vector.add(newY);
			sum += newX * newX + newY * newY;
		}
		double magnitude = Math.sqrt(sum);
		double[] vectorArray = new double[vector.size()];
		for (int i = 0; i < vector.size(); ++i) {
			vectorArray[i] = vector.get(i) / magnitude;
		}
		return vectorArray;
	}
	
	// for Protractor
	public static double optimalCosineDistance(double[] v1, double[] v2) {
		double a = 0.0;
		double b = 0.0;
		for (int i = 0; i < v1.length; i += 2) {
			a += v1[i] * v2[i] + v1[i + 1] * v2[i + 1];
			b += v1[i] * v2[i + 1] - v1[i + 1] * v2[i];
		}
		double angle = Math.atan(b / a);
		return Math.acos(a * Math.cos(angle) + b * Math.sin(angle));
	}
	
	public static double distanceAtBestAngle(Point[] points, Unistroke t, double a, double b, double threshold) {
		double x1 = PHI * a + (1.0 - PHI) * b;
		double f1 = distanceAtAngle(points, t, x1);
		double x2 = (1.0 - PHI) * a + PHI * b;
		double f2 = distanceAtAngle(points, t, x2);
		while (Math.abs(b - a) > threshold) {
			if (f1 < f2) {
				b = x2;
				x2 = x1;
				f2 = f1;
				x1 = PHI * a + (1.0 - PHI) * b;
				f1 = distanceAtAngle(points, t, x1);
			}
			else {
				a = x1;
				x1 = x2;
				f1 = f2;
				x2 = (1.0 - PHI) * a + PHI * b;
				f2 = distanceAtAngle(points, t, x2);
			}
		}
		return Math.min(f1, f2);
	}
	
	public static double distanceAtAngle(Point[] points, Unistroke t, double radians) {
		Point[] newpoints = rotateBy(points, radians);
		return pathDistance(newpoints, t.getPoints());
	}
	
	public static Point centroid(Point[] points) {
		double x = 0.0;
		double y = 0.0;
		for (int i = 0; i < points.length; ++i) {
			x += points[i].x;
			y += points[i].y;
		}
		x /= points.length;
		y /= points.length;
		return new Point(x, y);
	}
	
	public static Rectangle boundingBox(Point[] points) {
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < points.length; ++i) {
			minX = Math.min(minX, points[i].x);
			minY = Math.min(minY, points[i].y);
			maxX = Math.max(maxX, points[i].x);
			maxY = Math.max(maxY, points[i].y);
		}
		return new Rectangle(minX, minY, maxX - minX, maxY - minY);
	}
	
	// average distance between corresponding points in two paths
	public static double pathDistance(Point[] pts1, Point[] pts2) {
		double d = 0.0;
		for (int i = 0; i < pts1.length; ++i) { // assumes pts1.length == pts2.length
			d += distance(pts1[i], pts2[i]);
		}
		return d / pts1.length;
	}
	
	// length traversed by a point path
	public static double pathLength(Point[] points) {
		double d = 0.0;
		for (int i = 1; i < points.length; ++i) {
			d += distance(points[i - 1], points[i]);
		}
		return d;
	}
	
	// distance between two points
	public static double distance(Point p1, Point p2) {
		double dx = p2.x - p1.x;
		double dy = p2.y - p1.y;
		return Math.sqrt(dx * dx + dy * dy);
	}
	
	// start angle from points[0] to points[index] normalized as a unit vector
	public static Point calcStartUnitVector(Point[] points, int index) {
		if (index >= points.length) {
			index = points.length-1;
		}
		Point v = new Point(points[index].x - points[0].x, points[index].y - points[0].y);
		double len = Math.sqrt(v.x * v.x + v.y * v.y);
		return new Point(v.x / len, v.y / len);
	}
	
	// gives acute angle between unit vectors from (0,0) to v1, and (0,0) to v2
	public static double angleBetweenUnitVectors(Point v1, Point v2) {
		double n = (v1.x * v2.x + v1.y * v2.y);
		if (n < -1.0 || n > +1.0) {
			n = round(n, 5);
		}
		return Math.acos(n); // arc cosine of the vector dot product
	}
	
	// round 'n' to 'd' decimals
	public static double round(double n, int d) {
		double d2 = Math.pow(10, d);
		return Math.round(n*d2) / d2;
	}
	
	
	public static class Rectangle {
		public double x;
		public double y;
		public double width;
		public double height;
		
		public Rectangle(double x, double y, double width, double height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}
	}
	
}