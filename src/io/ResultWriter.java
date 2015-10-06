package io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import annk.domain.GNNKQuery;
import annk.domain.SGNNKQuery;

public class ResultWriter {
	private BufferedWriter writer;
	private int queryCount = 0;
	private boolean printInConsole;
	
	public ResultWriter(int noOfQueries, boolean printInConsole) throws IOException {
		this.printInConsole = printInConsole;
		
		Date date = new Date();
		SimpleDateFormat format = new SimpleDateFormat("MM-dd-HH:mm:ss:SS");
		String outputFileName = "result-(" + noOfQueries + ")-" + format.format(date) + ".txt";
		writer = new BufferedWriter(new FileWriter(outputFileName));
	}
	
	public ResultWriter(int noOfQueries) throws IOException {
		this(noOfQueries, false);
	}
	
	public void writeGNNKResult(List<GNNKQuery.Result> results) throws IOException {
		write("Query " + queryCount);
		for (GNNKQuery.Result result : results) {
			write(String.format("%d %.3f", result.id, result.cost));
		}
		queryCount++;
		write("");
	}
	
	public void writeSGNNKResult(List<SGNNKQuery.Result> results) throws IOException {
		write("Query " + queryCount);
		for (SGNNKQuery.Result result : results) {
			write(String.format("%d %.3f %s", result.id, result.cost, result.queryIds.toString()));
		}
		queryCount++;
		write("");
	}
	
	public void write(String str) throws IOException {
		if (printInConsole)
			System.out.println(str);
		writer.write(str + "\n");
	}
	
	public void close() throws IOException {
		writer.close();
	}
}
