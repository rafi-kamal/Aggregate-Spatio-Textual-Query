package test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class DataGenerator {
	private static final Random RANDOM = new Random(1);
	
	private static final int DATA_SET_SIZE = 10000;
	
	private static final int NUMBER_OF_QUERIES = 1;
	private static final int NUMBER_OF_INDIVIDUAL_QUERIES = 10;
	
	private static final int MAX_NUMBER_OF_KEYWORDS = 4;
	private static final int MAX_VALUE_OF_KEYWORD = 566;
	
//	private static final double QUERY_AREA_COVERAGE = 0.1;
	
	/**
	 * Generate loc.txt (location file), wwords.txt (keywords with weight)
	 * and gnnk.txt (gnnk query file)
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: data_directory");
			System.exit(1);
		}
		
		File directory = new File(args[0]);
		
		File locationFile = new File(directory, "loc.txt");
		File keywordFile = new File(directory, "wwords.txt");
		File gnnkQueryFile = new File(directory, "gnnk.txt");
		File sgnnkQueryFile = new File(directory, "sgnnk.txt");
		File lktQueryFile = new File(directory, "LkT.txt");
		
//		generateLocationFile(locationFile, DATA_SET_SIZE);
//		generateKeywordFile(keywordFile, DATA_SET_SIZE);
		generateGNNKQueryFile(gnnkQueryFile, NUMBER_OF_QUERIES, NUMBER_OF_INDIVIDUAL_QUERIES, "SUM");
		generateSGNNKQueryFile(sgnnkQueryFile, NUMBER_OF_QUERIES, NUMBER_OF_INDIVIDUAL_QUERIES, "SUM");
//		generateLkTQueryFile(lktQueryFile, NUMBER_OF_QUERIES);
	}
	
	public static void generateLkTQueryFile(File queryFile, int numberOfLkTQueries) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(queryFile))) {
			for (int i = 0; i < numberOfLkTQueries; i++) {
				double x = RANDOM.nextDouble();
				double y = RANDOM.nextDouble();
				
				writer.write(i + "," + x + "," + y + ",");
				
				int numberOfKeywords = 1 + RANDOM.nextInt(MAX_NUMBER_OF_KEYWORDS);
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
			int numberOfIndividualQueries, String aggregatorName) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(queryFile))) {
			writer.write(numberOfGNNKQueries + "\n");
			
			for (int i = 0; i < numberOfGNNKQueries; i++) {
				writer.write(aggregatorName + "\n");
				writer.write(numberOfIndividualQueries + "\n");
				
				for (int j = 0; j < numberOfIndividualQueries; j++) {
					writeQuery(writer, j);
				}
			}
			writer.flush();
		}
	}
	
	public static void generateSGNNKQueryFile(File queryFile, int numberOfGNNKQueries, 
			int numberOfIndividualQueries, String aggregatorName) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(queryFile))) {
			writer.write(numberOfGNNKQueries + "\n");
			
			for (int i = 0; i < numberOfGNNKQueries; i++) {
				writer.write(aggregatorName + "\n");
				writer.write(String.format("%d\n%d\n", 
						numberOfIndividualQueries, numberOfIndividualQueries / 2));
				
				for (int j = 0; j < numberOfIndividualQueries; j++) {
					writeQuery(writer, j);
				}
			}
			writer.flush();
		}
	}

	private static void writeQuery(BufferedWriter writer, int queryId) throws IOException {
//		double x = RANDOM.nextDouble() * Math.sqrt(QUERY_AREA_COVERAGE);
//		double y = RANDOM.nextDouble() * Math.sqrt(QUERY_AREA_COVERAGE);
		
		// yelp
//		double x = 33 + RANDOM.nextDouble() * 7;
//		double y = -115 + RANDOM.nextDouble() * 35;
//		
		// flickr
		double x = 28 + RANDOM.nextDouble() * 19;
		double y = -70 - RANDOM.nextDouble() * 50;
		
		writer.write(queryId + "," + x + "," + y + ",");
		
		int numberOfKeywords = 1 + RANDOM.nextInt(MAX_NUMBER_OF_KEYWORDS);
		for (int k = 0; k < numberOfKeywords; k++) {
			int keyword = RANDOM.nextInt(MAX_VALUE_OF_KEYWORD);
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

				int numberOfKeywords = 1 + RANDOM.nextInt(MAX_NUMBER_OF_KEYWORDS);
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
