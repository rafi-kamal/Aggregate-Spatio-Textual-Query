package utils;

public interface Parameters {
	
	final double latitudeStart = 31;
	final double latitudeEnd = 49;
	final double longitudeStart = -118;
	final double longitudeEnd = -81;
	
// 	Parameters for Yelp dataset
	
	final int uniqueKeywords = 783;
	final double maxWeight = 0.82478;
	
//	 Parameters for Flickr dataset
	
//	final int uniqueKeywords = 566432;
//	final double maxWeight = 8014422059718357;
	
	final double maxD = Math.sqrt((latitudeEnd - latitudeStart) * (latitudeEnd - latitudeStart)
			+ (longitudeEnd - longitudeStart) * (longitudeEnd - longitudeStart));
	
}
