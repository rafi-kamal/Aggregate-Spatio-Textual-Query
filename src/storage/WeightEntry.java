package storage;

import java.io.Serializable;

public class WeightEntry implements Serializable{

	public int word;
	public double weight;

	
	public WeightEntry(int id, double w){
		word = id;
		weight = w;
	}
	
}
