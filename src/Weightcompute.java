import java.io.FileReader;
import java.io.FileWriter;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;

public class Weightcompute {

	static void ComputeTermWeights(String infile, String outfile) {
		double lmd = 0.2; // smoothing factor
		
		// Contains <term, frequency> pairs for each term in the input file 
		Hashtable<String, Integer> dic = new Hashtable<String, Integer>();
		try {
			LineNumberReader lr = new LineNumberReader(new FileReader(infile));
			FileWriter fw = new FileWriter(outfile);
			PrintWriter out = new PrintWriter(fw);

			int totalLength = 0;
			String line = lr.readLine();
			while (line != null) {
				String[] cols = line.split(",");
				for (int i = 1; i < cols.length; i++) {
					totalLength++;
					if (dic.containsKey(cols[i])) {
						int count = (dic.get(cols[i])).intValue();
						dic.put(cols[i], count + 1);
					} else {
						dic.put(cols[i], 1);
					}
				}
				line = lr.readLine();
			}
			lr.close();

			lr = new LineNumberReader(new FileReader(infile));
			line = lr.readLine();
			while (line != null) {
				String[] cols = line.split(",");
				
				// Contains <term, frequency> pairs for each term in individual document
				Hashtable<String, Integer> sent = new Hashtable<String, Integer>();
				String wordID = cols[0];

				for (int i = 1; i < cols.length; i++) {
					if (sent.contains(cols[i])) {
						int count = sent.get(cols[i]);
						sent.put(cols[i], count + 1);
					} else {
						sent.put(cols[i], 1);
					}
				}

				Iterator<Entry<String, Integer>> iter = sent.entrySet().iterator();
				String buf = "";
				while (iter.hasNext()) {
					Entry<String, Integer> entry = (Entry<String, Integer>) iter.next();
					String word = entry.getKey();
					double documentFrequency = entry.getValue();	// Term frequency in this document
					double totalFrequency = dic.get(word);		// Term frequency in all documents

					double weight = (1 - lmd) * documentFrequency / (cols.length - 1)
							+ lmd * totalFrequency / totalLength;
					weight = Math.pow(weight, documentFrequency);
					buf += word + " " + weight + ",";
				}
				buf = buf.substring(0, buf.length() - 1);
				out.println(wordID + "," + buf);
				line = lr.readLine();
			}
			lr.close();
			out.close();
			fw.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void main(String arg[]) {
		ComputeTermWeights(arg[0], arg[1]);
	}
}
