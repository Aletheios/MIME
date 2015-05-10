package de.lmu.ifi.medien.mime.$N;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.opencv.core.Point;

public class Unistroke implements Externalizable {
	
	private Point[] points;
	private Point startUnitVector;
	private double[] vector;
	

	public Unistroke() { }

	public Unistroke(boolean useBoundedRotationInvariance, Point[] points) {
		double radians = Util.indicativeAngle(points);
		points = Util.resample(points, Util.NUM_POINTS);
		points = Util.rotateBy(points, -radians);
		points = Util.scaleDimTo(points, NDollarRecognizer.mSquareSize, Util.ONE_D_THRESHOLD);
		if (useBoundedRotationInvariance) {
			points = Util.rotateBy(points, radians);	// restore
		}
		this.points = Util.translateTo(points, Util.ORIGIN);
		
		this.startUnitVector = Util.calcStartUnitVector(this.points, Util.START_ANGLE_INDEX);
		this.vector = Util.vectorize(this.points, useBoundedRotationInvariance);	// for Protractor
	}
	
	@Override
	public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
		this.points = new Point[input.readInt()];
		for (int i = 0; i < this.points.length; ++i) {
			this.points[i] = new Point(input.readDouble(), input.readDouble());
		}
		this.startUnitVector = new Point(input.readDouble(), input.readDouble());
		this.vector = new double[input.readInt()];
		for (int i = 0; i < this.vector.length; ++i) {
			this.vector[i] = input.readDouble();
		}
	}
	
	@Override
	public void writeExternal(ObjectOutput output) throws IOException {
		output.writeInt(this.points.length);
		for (Point p : this.points) {
			output.writeDouble(p.x);
			output.writeDouble(p.y);
		}
		output.writeDouble(this.startUnitVector.x);
		output.writeDouble(this.startUnitVector.y);
		output.writeInt(this.vector.length);
		for (double d : this.vector) {
			output.writeDouble(d);
		}
	}

	public Point[] getPoints() { return this.points; }

	public void setPoints(Point[] points) { this.points = points; }

	public Point getStartUnitVector() { return this.startUnitVector; }

	public void setStartUnitVector(Point startUnitVector) { this.startUnitVector = startUnitVector; }

	public double[] getVector() { return this.vector; }

	public void setVector(double[] vector) { this.vector = vector; }
	
}