package de.lmu.ifi.medien.mime.$N;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;

import org.opencv.core.Point;

public class Multistroke implements Externalizable {
	
	private int type;
	private int numStrokes;
	private Unistroke[] unistrokes;
	private Point[][] origStrokes;
	

	public Multistroke() { }

	public Multistroke(int type, boolean useBoundedRotationInvariance, Point[][] strokes) {
		this.type = type;
		this.numStrokes = strokes.length;	// number of individual strokes
		this.origStrokes = strokes;
		
		int[] order = new int[strokes.length];
		for (int i = 0; i < strokes.length; ++i) {
			order[i] = i;	// initialize
		}
		ArrayList<Integer[]> ordersList = new ArrayList<>();
		Util.heapPermute(strokes.length, order, ordersList);
		int[][] orders = new int[ordersList.size()][];
		int count = 0;
		for (Integer[] array : ordersList) {
			int[] inner = new int[array.length];
			for (int i = 0; i < array.length; ++i) {
				inner[i] = array[i];
			}
			orders[count++] = inner;
		}

		Point[][] unistrokes = Util.makeUnistrokes(strokes, orders);
		this.unistrokes = new Unistroke[unistrokes.length];	// unistrokes for this multistroke
		for (int j = 0; j < unistrokes.length; ++j) {
			this.unistrokes[j] = new Unistroke(useBoundedRotationInvariance, unistrokes[j]);
		}
	}
	
	@Override
	public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
		this.type = input.readInt();
		this.numStrokes = input.readInt();
		this.unistrokes = new Unistroke[input.readInt()];
		for (int i = 0; i < this.unistrokes.length; ++i) {
			this.unistrokes[i] = (Unistroke) input.readObject();
		}
		this.origStrokes = new Point[input.readInt()][];
		for (int i = 0; i < this.origStrokes.length; ++i) {
			this.origStrokes[i] = new Point[input.readInt()];
			for (int j = 0; j < this.origStrokes[i].length; ++j) {
				this.origStrokes[i][j] = new Point(input.readDouble(), input.readDouble());
			}
		}
	}
	
	@Override
	public void writeExternal(ObjectOutput output) throws IOException {
		output.writeInt(this.type);
		output.writeInt(this.numStrokes);
		output.writeInt(this.unistrokes.length);
		for (Unistroke u : this.unistrokes) {
			output.writeObject(u);
		}
		output.writeInt(this.origStrokes.length);
		for (Point[] points : this.origStrokes) {
			output.writeInt(points.length);
			for (Point p : points) {
				output.writeDouble(p.x);
				output.writeDouble(p.y);
			}
		}
	}

	public int getType() { return this.type; }

	public void setType(int type) { this.type = type; }

	public int getNumStrokes() { return this.numStrokes; }

	public void setNumStrokes(int numStrokes) { this.numStrokes = numStrokes; }

	public Unistroke[] getUnistrokes() { return this.unistrokes; }

	public void setUnistrokes(Unistroke[] unistrokes) { this.unistrokes = unistrokes; }

	public Point[][] getOrigStrokes() { return this.origStrokes; }

	public void setOrigStrokes(Point[][] origStrokes) { this.origStrokes = origStrokes; }
	
}