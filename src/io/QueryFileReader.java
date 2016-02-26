package io;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import annk.aggregator.AggregatorFactory;
import annk.aggregator.IAggregator;
import annk.domain.GNNKQuery;
import annk.domain.SGNNKQuery;
import query.Query;
import spatialindex.spatialindex.Point;

public class QueryFileReader {
	private File queryFile;
	private int keywordDropPercentage;

	public QueryFileReader(File queryFile, int keywordDropPercentage) {
		this.queryFile = queryFile;
		this.keywordDropPercentage = keywordDropPercentage;
	}

	public QueryFileReader(File queryFile) {
		this(queryFile, 0);
	}
	
	public QueryFileReader(String fileName, int keywordDropPercentage) {
		this(new File(fileName), keywordDropPercentage);
	}
	
	public QueryFileReader(String fileName) {
		this(new File(fileName));
	}
	
	public List<GNNKQuery> readGNNKQueries() {
		try (Scanner scanner = new Scanner(queryFile)) {
			List<GNNKQuery> gnnkQueries = new ArrayList<>();
			
			int noOfTestCases = Integer.parseInt(scanner.nextLine());
			for (int i = 0; i < noOfTestCases; i++) {
				String aggregatorName = scanner.nextLine();
				IAggregator aggregator = AggregatorFactory.getAggregator(aggregatorName);
				
				int noOfQueries = Integer.parseInt(scanner.nextLine());
				List<Query> queries = new ArrayList<>();
				for (int j = 0; j < noOfQueries; j++) {
					String queryLine = scanner.nextLine();
					queries.add(parseQuery(queryLine));
				}
				
				GNNKQuery gnnkQuery = new GNNKQuery(queries, aggregator);
				gnnkQueries.add(gnnkQuery);
			}
			
			return gnnkQueries;
		} catch (Exception exception) {
			exception.printStackTrace();
			return null;
		}
	}
	
	public List<SGNNKQuery> readSGNNKQueries() {
		try (Scanner scanner = new Scanner(queryFile)) {
			List<SGNNKQuery> sgnnkQueries = new ArrayList<>();
			
			int noOfTestCases = Integer.parseInt(scanner.nextLine());
			for (int i = 0; i < noOfTestCases; i++) {
				String aggregatorName = scanner.nextLine();
				IAggregator aggregator = AggregatorFactory.getAggregator(aggregatorName);
				
				int groupSize = Integer.parseInt(scanner.nextLine());
				int subgroupSize = Integer.parseInt(scanner.nextLine());
				List<Query> queries = new ArrayList<>();
				for (int j = 0; j < groupSize; j++) {
					String queryLine = scanner.nextLine();
					queries.add(parseQuery(queryLine));
				}
				
				SGNNKQuery sgnnkQuery = new SGNNKQuery(queries, subgroupSize, aggregator);
				sgnnkQueries.add(sgnnkQuery);
			}
			
			return sgnnkQueries;
		} catch (Exception exception) {
			exception.printStackTrace();
			return null;
		}
	}
	
	public Query parseQuery(final String queryLine) {
		String[] temp = queryLine.split(",");

		int id = Integer.parseInt(temp[0]);
		double weight = Double.parseDouble(temp[1]);
		double x = Double.parseDouble(temp[2]);
		double y = Double.parseDouble(temp[3]);
		Point location = new Point(new double[] {x, y});

        class Keyword {
        	int id;
        	double weight;
        	
			public Keyword(int id, double weight) {
				this.id = id;
				this.weight = weight;
			}
		}
        
        List<Keyword> keywords = new ArrayList<>();
        
		for (int j = 4; j < temp.length; j++) {
			String[] temp2 = temp[j].split(" ");
            int keyword = Integer.parseInt(temp2[0]);
            double keywordWeight = Double.parseDouble(temp2[1]);
            keywords.add(new Keyword(keyword, keywordWeight));
        }
		Collections.sort(keywords, Comparator.comparing((Keyword keyword) -> keyword.weight).reversed());
		
		int limit = keywords.size() - keywords.size() * keywordDropPercentage / 100;
		
		List<Integer> keywordIds = keywords.stream()
				.map((keyword) -> keyword.id)
				.limit(limit)
				.collect(Collectors.toList());
		
		List<Double> keywordWeights = keywords.stream()
				.map((keyword) -> keyword.weight)
				.limit(limit)
				.collect(Collectors.toList());
		
		Query query = new Query(id, weight, location, keywordIds, keywordWeights);
		return query;
	}
}
