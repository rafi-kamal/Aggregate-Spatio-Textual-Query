import spatialindex.dirtree.DIRTree;
import spatialindex.dirtree.Node;
import spatialindex.spatialindex.IData;
import spatialindex.spatialindex.INode;
import spatialindex.spatialindex.IVisitor;
import spatialindex.spatialindex.Point;
import spatialindex.storagemanager.DiskStorageManager;
import spatialindex.storagemanager.IStorageManager;
import spatialindex.storagemanager.PropertySet;

public class TestDIRTree {
	public static void main(String[] args) throws Exception {
		
		PropertySet ps = new PropertySet();
		ps.setProperty("FileName", "index_file0.7.rtree");
		IStorageManager diskfile = new DiskStorageManager(ps);
		
		PropertySet ps2 = new PropertySet();
		ps2.setProperty("IndexIdentifier", 1);
		DIRTree dirTree = new DIRTree(ps, diskfile, 0.7);
		
		Point point = new Point(new double[] {1.0, 1.0});
		for (int i = 1; i <= 5; i++) {
			try {
				Node node = dirTree.readNode(i);
				System.out.println(node.getIdentifier() + " " + node.getChildrenCount());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		dirTree.nearestNeighborQuery(3, point, new IVisitor() {
			
			@Override
			public void visitNode(INode n) {
				System.out.println("Visiting node: " + n.getIdentifier());
			}
			
			@Override
			public void visitData(IData d) {
				System.out.println("Visiting data: " + new String(d.getData()));
			}
		});
	}
}
