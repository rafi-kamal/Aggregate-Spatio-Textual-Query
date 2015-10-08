package utils;

public interface Parameters {
	
	// Parameters for Yelp dataset
	final double latitudeStart = 32.871;
	final double latitudeEnd = 56.03655;
	final double longitudeStart = -111.253141;
	final double longitudeEnd = -89.59568;
	
	final int uniqueKeywords = 783;
	final double maxWeight = 0.82478;
	
	final double maxD = Math.sqrt((latitudeEnd - latitudeStart) * (latitudeEnd - latitudeStart)
			+ (longitudeEnd - longitudeStart) * (longitudeEnd - longitudeStart));
	
}
