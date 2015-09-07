import java.io.FileReader;
import java.io.FileWriter;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;


public class Weightcompute {
	
	static void ComputeTermWeights(String infile, String outfile)
	{
		double lmd = 0.2;		//smoothing factor
		Hashtable<String, Integer> dic = new Hashtable<String, Integer>();
		try{
			LineNumberReader lr = new LineNumberReader(new FileReader(infile));
			FileWriter fw = new FileWriter(outfile);
			PrintWriter out = new PrintWriter(fw);  
			
			int totalLength = 0;
			String line = lr.readLine();
			while( line != null){
				String[] cols = line.split(",");
				for(int i=1;i<cols.length;i++){
					totalLength++;
					if(dic.containsKey(cols[i])){
						int count = (dic.get(cols[i])).intValue();
						dic.put(cols[i], new Integer(count + 1));
					}else{
						dic.put(cols[i], new Integer(1));
					}	
				} 
				line = lr.readLine();
			}			
			lr.close();
			
			lr = new LineNumberReader(new FileReader(infile));
			line = lr.readLine();
			while( line != null){
				String[] cols = line.split(",");
				Hashtable<String, Integer> sent = new Hashtable<String, Integer>();
				String wordID = cols[0];
				
				for(int i=1;i<cols.length;i++){
					if(sent.contains(cols[i])){
						int count = (sent.get(cols[i])).intValue();
						sent.put(cols[i], new Integer(count + 1));
					}else{
						sent.put(cols[i], 1);
					}	
				} 
				
				Iterator<Entry<String, Integer>> iter = sent.entrySet().iterator();
				String buf = "";
				while (iter.hasNext()) {
					Entry<String, Integer> entry = (Entry<String, Integer> ) iter.next();
					String word = entry.getKey();
					int count_in_sent = entry.getValue().intValue();
					int count_in_coll = dic.get(word).intValue();
					
					double weight = (1-lmd) * (double)count_in_sent / (double)(cols.length-1) + 
							lmd * (double)count_in_coll/(double) totalLength;
					weight = Math.pow(weight, count_in_sent);
					buf += word + " " + weight + ",";
				}
				buf = buf.substring(0, buf.length()-1);
				out.println(wordID + "," + buf);
				line = lr.readLine();
			}			
			lr.close();		
			out.close();
			fw.close();
					
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public static void main(String arg[]){
		String infile = "Y:\\EVALUATION\\IRTrees\\doc";
		String outfile = "Y:\\EVALUATION\\IRTrees\\doc_tw";
		ComputeTermWeights(infile, outfile);
		/*String infile = "C:\\temp\\route query\\flickrdata\\cluster.final";
		String outfile = "C:\\temp\\route query\\flickrdata\\UTM";
		CoordinateConversion cc = new CoordinateConversion(); 			
		
		try{
			LineNumberReader lr = new LineNumberReader(new FileReader(infile));
			FileWriter fw = new FileWriter(outfile);
			PrintWriter out = new PrintWriter(fw);
			
			String line = lr.readLine();
			while( line != null){
				String[] cols = line.split(":");
				String[] coor = cols[0].split(",");
				double lat = Double.parseDouble(coor[0]);
				double lon = Double.parseDouble(coor[1]);
				String res = cc.latLon2UTM(lat, lon);
				out.println(res);
				line = lr.readLine();
			}			
			lr.close();
			out.close();
			fw.close();
		}catch(Exception e){
			e.printStackTrace();
		}*/
	}
}
