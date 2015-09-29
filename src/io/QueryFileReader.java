package io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import annk.aggregator.AggregatorFactory;
import annk.aggregator.IAggregator;
import annk.domain.GNNKQuery;
import annk.domain.SGNNKQuery;
import query.Query;
import spatialindex.spatialindex.Point;

public class QueryFileReader {
	private File queryFile;

	public QueryFileReader(File queryFile) {
		this.queryFile = queryFile;
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
		double x = Double.parseDouble(temp[1]);
		double y = Double.parseDouble(temp[2]);
		Point location = new Point(new double[] {x, y});

		List<Integer> keywords = new ArrayList<>();
		for (int j = 3; j <  temp.length; j++) {
			keywords.add(Integer.parseInt(temp[j]));
		}
		
		Query query = new Query(id, location, keywords);
		return query;
	}
}
