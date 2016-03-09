package utils;

public interface Parameters {
	
	// Parameters for Yelp dataset
//	final double latitudeStart = 32.871;
//	final double latitudeEnd = 56.03655;
//	final double longitudeStart = -111.253141;
//	final double longitudeEnd = -89.59568;
//	
//	final int uniqueKeywords = 783;
//	final double maxWeight = 0.82478;
	
//	 Parameters for Flickr dataset
	final double latitudeStart = 28;
	final double latitudeEnd = 47;
	final double longitudeStart = -120;
	final double longitudeEnd = -70;
	
	final int uniqueKeywords = 566432;
	final double maxWeight = .8015;
	
	final double maxD = Math.sqrt((latitudeEnd - latitudeStart) * (latitudeEnd - latitudeStart)
			+ (longitudeEnd - longitudeStart) * (longitudeEnd - longitudeStart));
	
}
