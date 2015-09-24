package io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import spatialindex.spatialindex.NNEntry;

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
	
	public void writeResult(List<NNEntry> results) throws IOException {
		write("Query " + queryCount);
		for (NNEntry result : results) {
			write(String.format("%d %.3f", result.node.getIdentifier(), result.cost));
		}
		queryCount++;
		write("");
	}
	
	private void write(String str) throws IOException {
		if (printInConsole)
			System.out.println(str);
		writer.write(str + "\n");
	}
	
	public void close() throws IOException {
		writer.close();
	}
}
