package test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import utils.Parameters;

public class QueryGenerator {
	private static final Random RANDOM = new Random(1);
	
	private static final int DATA_SET_SIZE = 10000;
	private static final int NUMBER_OF_QUERIES = 20;
	private static final int MAX_VALUE_OF_KEYWORD = 566;
	
//	private static final double QUERY_AREA_COVERAGE = 0.1;
	
	/**
	 * Generate loc.txt (location file), wwords.txt (keywords with weight)
	 * and gnnk.txt (gnnk query file)
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 6) {
			System.err.println("Usage: data_directory group_size subgroup_size% no_of_query_keywords query_space_area% keyword_space_size%");
			System.exit(1);
		}
		
		File directory = new File(args[0]);
		int groupSize = Integer.parseInt(args[1]);
		double subgroupSizePercentage = Double.parseDouble(args[2]);
		int numberOfKeywords = Integer.parseInt(args[3]);
		double querySpaceAreaPercentage = Double.parseDouble(args[4]);
		double keywordSpaceSizePercentage = Double.parseDouble(args[5]);
		
		File locationFile = new File(directory, "loc.txt");
		File keywordFile = new File(directory, "wwords.txt");
		File gnnkQueryFile = new File(directory, "gnnk.txt");
		File sgnnkQueryFile = new File(directory, "sgnnk.txt");
		File lktQueryFile = new File(directory, "LkT.txt");
		
//		generateLocationFile(locationFile, DATA_SET_SIZE);
//		generateKeywordFile(keywordFile, DATA_SET_SIZE);
		generateGNNKQueryFile(gnnkQueryFile, NUMBER_OF_QUERIES, groupSize, numberOfKeywords, querySpaceAreaPercentage, keywordSpaceSizePercentage, "SUM");
		generateSGNNKQueryFile(sgnnkQueryFile, NUMBER_OF_QUERIES, groupSize, subgroupSizePercentage, numberOfKeywords, querySpaceAreaPercentage, keywordSpaceSizePercentage, "SUM");
//		generateLkTQueryFile(lktQueryFile, NUMBER_OF_QUERIES);
	}
	
	public static void generateLkTQueryFile(File queryFile, int numberOfLkTQueries, int numberOfKeywords) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(queryFile))) {
			for (int i = 0; i < numberOfLkTQueries; i++) {
				double x = RANDOM.nextDouble();
				double y = RANDOM.nextDouble();
				
				writer.write(i + "," + x + "," + y + ",");
				
				for (int k = 0; k < numberOfKeywords; k++) {
					int keyword = RANDOM.nextInt(MAX_VALUE_OF_KEYWORD);
					writer.write(Integer.toString(keyword));
					
					if (k == numberOfKeywords - 1) writer.write("\n");
					else writer.write(",");
				}
			}
			writer.flush();
		}
	}
	
	public static void generateGNNKQueryFile(File queryFile, int numberOfGNNKQueries, 
			int groupSize, int numberOfKeywords, double querySpaceAreaPercentage,
			double keywordSpaceSizePercentage, String aggregatorName) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(queryFile))) {
			writer.write(numberOfGNNKQueries + "\n");
			
			for (int i = 0; i < numberOfGNNKQueries; i++) {
				writer.write(aggregatorName + "\n");
				writer.write(groupSize + "\n");
				
				double latitudeSpan = (Parameters.latitudeEnd - Parameters.latitudeStart) 
						* Math.sqrt(querySpaceAreaPercentage / 100);
				double longtitudeSpan = (Parameters.longitudeEnd - Parameters.longitudeStart) 
						* Math.sqrt(querySpaceAreaPercentage / 100);
				
				double centroidLatitude = Parameters.latitudeStart + 
						RANDOM.nextDouble() * (Parameters.latitudeEnd - Parameters.latitudeStart);
				double centroidLongtitude = Parameters.longitudeStart + 
						RANDOM.nextDouble() * (Parameters.longitudeEnd - Parameters.latitudeStart);
				
				int keywordSpaceSpan = (int) (Parameters.uniqueKeywords * keywordSpaceSizePercentage / 100);
				int keywordSpaceMiddle = RANDOM.nextInt(Parameters.uniqueKeywords - keywordSpaceSpan); 
				
				for (int j = 0; j < groupSize; j++) {
					writeQuery(writer, j, numberOfKeywords, keywordSpaceMiddle, keywordSpaceSpan,
							centroidLatitude, centroidLongtitude, latitudeSpan, longtitudeSpan);
				}
			}
			writer.flush();
		}
	}
	
	public static void generateSGNNKQueryFile(File queryFile, int numberOfGNNKQueries, 
			int groupSize, double subgroupSize, int numberOfKeywords, double querySpaceAreaPercentage,
			double keywordSpaceSizePercentage, String aggregatorName) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(queryFile))) {
			writer.write(numberOfGNNKQueries + "\n");
			
			for (int i = 0; i < numberOfGNNKQueries; i++) {
				writer.write(aggregatorName + "\n");
				writer.write(String.format("%d\n%d\n", 
						groupSize, (int) (groupSize * subgroupSize / 100)));
				
				double latitudeSpan = (Parameters.latitudeEnd - Parameters.latitudeStart) 
						* Math.sqrt(querySpaceAreaPercentage / 100);
				double longtitudeSpan = (Parameters.longitudeEnd - Parameters.longitudeStart) 
						* Math.sqrt(querySpaceAreaPercentage / 100);
				
				double centroidLatitude = Parameters.latitudeStart + 
						RANDOM.nextDouble() * (Parameters.latitudeEnd - Parameters.latitudeStart);
				double centroidLongtitude = Parameters.longitudeStart + 
						RANDOM.nextDouble() * (Parameters.longitudeEnd - Parameters.latitudeStart);
				
				int keywordSpaceSpan = (int) (Parameters.uniqueKeywords * keywordSpaceSizePercentage / 100);
				int keywordSpaceMiddle = RANDOM.nextInt(Parameters.uniqueKeywords - keywordSpaceSpan); 
				
				for (int j = 0; j < groupSize; j++) {
					writeQuery(writer, j, numberOfKeywords, keywordSpaceMiddle, keywordSpaceSpan,
							centroidLatitude, centroidLongtitude, latitudeSpan, longtitudeSpan);
				}
			}
			writer.flush();
		}
	}

	private static void writeQuery(BufferedWriter writer, int queryId, 
			int numberOfKeywords, int keywordSpaceMiddle, int keywordSpaceSpan,
			double centroidLatitude, double centroidLongtitude, double latitudeSpan, double longtitudeSpan) 
					throws IOException {
		double x = (centroidLatitude - latitudeSpan / 2) + RANDOM.nextDouble() * latitudeSpan;
		double y = (centroidLongtitude - longtitudeSpan / 2) + RANDOM.nextDouble() * longtitudeSpan;
		
		writer.write(queryId + "," + x + "," + y + ",");
		
		for (int k = 0; k < numberOfKeywords; k++) {
			int keyword = keywordSpaceMiddle + RANDOM.nextInt(keywordSpaceSpan);
			writer.write(Integer.toString(keyword));
			
			if (k == numberOfKeywords - 1) writer.write("\n");
			else writer.write(",");
		}
	}
	
	public static void generateLocationFile(File locationFile, int dataSetSize) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(locationFile))) {
			for (int i = 0; i < dataSetSize; i++) {
				double x = RANDOM.nextDouble();
				double y = RANDOM.nextDouble();
				
				writer.write(i + "," + x + "," + y + "\n");
			}
			writer.flush();
		}
	}
	
	public static void generateKeywordFile(File keywordFile, int dataSetSize) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(keywordFile))) {
			for (int i = 0; i < dataSetSize; i++) {
				writer.write(i+ ",");

				int numberOfKeywords = 1 + RANDOM.nextInt(Parameters.uniqueKeywords + 1);
				for (int k = 0; k < numberOfKeywords; k++) {
					int keyword = RANDOM.nextInt(MAX_VALUE_OF_KEYWORD);
					writer.write(keyword + " " + 0.1);

					if (k == numberOfKeywords - 1) writer.write("\n");
					else writer.write(",");
				}
			}
			writer.flush();
		}
	}
}
