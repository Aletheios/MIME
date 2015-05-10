package de.lmu.ifi.medien.mime;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.lmu.ifi.medien.mime.$N.Multistroke;

/**
 * Describes a hand pose, defined by a Multistroke
 */
public class Pose implements Externalizable, Comparable<Pose> {
	
	private int type;
	private Multistroke multistroke;
	

	public Pose() { }

	@Override
	public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
		this.type = input.readInt();
		this.multistroke = (Multistroke) input.readObject();
	}
	
	@Override
	public void writeExternal(ObjectOutput output) throws IOException {
		output.writeInt(type);
		output.writeObject(multistroke);
	}

	@Override
	public int compareTo(Pose another) {
		if (this.type > another.type) {
			return 1;
		}
		else if (this.type < another.type) {
			return -1;
		}
		return 0;
	}

	public int getType() { return this.type; }

	public void setType(int type) { this.type = type; }

	public Multistroke getMultistroke() { return this.multistroke; }

	public void setMultistroke(Multistroke multistroke) { this.multistroke = multistroke; }
	
}