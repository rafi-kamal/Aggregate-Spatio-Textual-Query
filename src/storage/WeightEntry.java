package storage;

import java.io.Serializable;

public class WeightEntry implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public int word;
	public double weight;

	public WeightEntry(int id, double w) {
		word = id;
		weight = w;
	}

}
