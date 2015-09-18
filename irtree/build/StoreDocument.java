package build;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.Vector;

import storage.DocumentStore;
import storage.WeightEntry;

public class StoreDocument {

	public static void main(String[] args) throws Exception{
		
		if (args.length != 2)
		{
			System.err.println("Usage: StoreDocument input_file pagesize.");
			System.exit(-1);
		}		
		
		//input file format: 
		//one document per line,
		//each line: wordid weight,wordid weight,...
		//           integer double
		
		int pagesize = Integer.parseInt(args[1]);
		DocumentStore ds = new DocumentStore(args[0], pagesize);		
		ds.create(0);
		LineNumberReader lr = new LineNumberReader(new FileReader(args[0]));
		load(lr, ds);
	}
	
	public static void load(LineNumberReader lr, storage.DocumentStore ds) throws Exception{
		int count = 0;		
		int id;
		Vector words;
		long start = System.currentTimeMillis();
		String line = lr.readLine();
		String[] temp, tt;
		while (line != null)
		{
			temp = line.split(",");			
			id = Integer.parseInt(temp[0]);
			words = new Vector();
			for(int j = 1; j < temp.length; j++){
				tt = temp[j].split(" ");
				if(tt.length != 2)
					continue;
				WeightEntry de = new WeightEntry(Integer.parseInt(tt[0]), Double.parseDouble(tt[1]));
				words.add(de);
			}
				
			ds.write(id, words);

			if ((count % 1000) == 0) System.err.println(count);

			count++;
			line = lr.readLine();
		}

		long end = System.currentTimeMillis();

		ds.flush();
		
		System.err.println("Operations: " + count);
		
		System.err.println("Minutes: " + ((end - start) / 1000.0f) / 60.0f);
	}

}
