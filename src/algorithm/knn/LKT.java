package algorithm.knn;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import documentindex.InvertedFile;
import query.Query;
import spatialindex.rtree.RTree;
import spatialindex.spatialindex.Point;
import spatialindex.storagemanager.DiskStorageManager;
import spatialindex.storagemanager.IStorageManager;
import spatialindex.storagemanager.PropertySet;

public class LKT {

	public static void main(String[] args)throws Exception
	{
		if(args.length != 5){
			System.out.println("Usage: LKT index_file query_file topk alpha_dist cluster");
			System.exit(-1);
		}
		
		//query file format:
		//one query per line,
		//each line: id,x,y,word1,word2,...
		
		String index_file = args[0];
		String query_file = args[1];
		int topk = Integer.parseInt(args[2]);
		RTree.alpha_dist = Double.parseDouble(args[3]);		
		RTree.numOfClusters = Integer.parseInt(args[4]);
		
		PropertySet ps = new PropertySet();
		ps.setProperty("FileName", index_file + ".rtree");
		IStorageManager diskfile = new DiskStorageManager(ps);
		PropertySet ps2 = new PropertySet();
		Integer i = new Integer(1); // INDEX_IDENTIFIER_GOES_HERE (suppose I know that in this case it is equal to 1);
		ps2.setProperty("IndexIdentifier", i);
		
		RTree tree = new RTree(ps2, diskfile);
		
		InvertedFile invertedFile;
		if(RTree.numOfClusters != 0)
			invertedFile = new InvertedFile(index_file + "." + RTree.numOfClusters, 4096);
		else
			invertedFile = new InvertedFile(index_file, 4096);
				
		FileInputStream fis = new FileInputStream(query_file);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		int count = 0;
		String line;
		String[] temp;
		int id = -1;
		double x, y;
		double[] f = new double[2];
		long start = 0; long end = 0; long acc = 0;
		
	//	long start = System.currentTimeMillis();
		
		int ivIO = 0;
		
		while((line = br.readLine()) != null){
			temp = line.split(",");
			
			id = Integer.parseInt(temp[0]);
			x = Double.parseDouble(temp[1]);
			y = Double.parseDouble(temp[2]);
			f[0] = x; f[1] = y;
			
			Query q = new Query(id);
			for(int j = 3; j < temp.length; j++){
				q.keywords.add(Integer.parseInt(temp[j]));
			}
			q.point = new Point(f);
			
			System.out.println("query " + count);
			count++;
			
			invertedFile.resetIO();
			
			start = System.currentTimeMillis();
			
			tree.lkt(invertedFile, q, topk);
			
			end = System.currentTimeMillis();
			acc += (end - start);
			
			ivIO += invertedFile.getIO();
		}
	//	long end = System.currentTimeMillis();
		
		System.out.println("Average time millisecond: " + acc*1.0/count);
		System.out.println("Average total IO: " + (tree.getIO() + ivIO)*1.0/count);
		System.out.println("Average tree IO: " + tree.getIO()*1.0/count);
		System.out.println("Average inverted index IO: " + ivIO*1.0/count);
		
	}
}
