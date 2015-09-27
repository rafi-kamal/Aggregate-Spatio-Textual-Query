// Spatial Index Library
//
// Copyright (C) 2002  Navel Ltd.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License aint with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// Contact information:
//  Mailing address:
//    Marios Hadjieleftheriou
//    University of California, Riverside
//    Department of Computer Science
//    Surge Building, Room 310
//    Riverside, CA 92521
//
//  Email:
//    marioh@cs.ucr.edu

package spatialindex.rtree;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.Vector;

import documentindex.InvertedFile;
import jdbm.btree.BTree;
import query.Query;
import spatialindex.spatialindex.IData;
import spatialindex.spatialindex.IEntry;
import spatialindex.spatialindex.INearestNeighborComparator;
import spatialindex.spatialindex.INode;
import spatialindex.spatialindex.INodeCommand;
import spatialindex.spatialindex.IQueryStrategy;
import spatialindex.spatialindex.IShape;
import spatialindex.spatialindex.ISpatialIndex;
import spatialindex.spatialindex.IStatistics;
import spatialindex.spatialindex.IVisitor;
import spatialindex.spatialindex.NNEntry;
import spatialindex.spatialindex.NNEntryComparator;
import spatialindex.spatialindex.Point;
import spatialindex.spatialindex.RWLock;
import spatialindex.spatialindex.Region;
import spatialindex.spatialindex.RtreeEntry;
import spatialindex.spatialindex.SpatialIndex;
import spatialindex.storagemanager.IStorageManager;
import spatialindex.storagemanager.InvalidPageException;
import spatialindex.storagemanager.PropertySet;
import storage.DocumentStore;

public class RTree implements ISpatialIndex {
	//////////////////////////////////////
	public static double alpha_dist;
	public static int numOfClusters = 0;
	//////////////////////////////////////

	RWLock rwLock;

	IStorageManager m_pStorageManager;

	protected int rootID;
	int headerID;

	int treeVariant;

	double fillFactor;

	int indexCapacity;

	int leafCapacity;

	int nearMinimumOverlapFactor;
	// The R*-Tree 'p' constant, for calculating nearly minimum overlap cost.
	// [Beckmann, Kriegel, Schneider, Seeger 'The R*-tree: An efficient and
	// Robust Access Method
	// for Points and Rectangles, Section 4.1]

	double splitDistributionFactor;
	// The R*-Tree 'm' constant, for calculating spliting distributions.
	// [Beckmann, Kriegel, Schneider, Seeger 'The R*-tree: An efficient and
	// Robust Access Method
	// for Points and Rectangles, Section 4.2]

	double reinsertFactor;
	// The R*-Tree 'p' constant, for removing entries at reinserts.
	// [Beckmann, Kriegel, Schneider, Seeger 'The R*-tree: An efficient and
	// Robust Access Method
	// for Points and Rectangles, Section 4.3]

	int dimension;

	Region infiniteRegion;

	Statistics stats;
	
	public int noOfVisitedNodes;

	ArrayList<INodeCommand> writeNodeCommands = new ArrayList<INodeCommand>();
	ArrayList<INodeCommand> readNodeCommands = new ArrayList<INodeCommand>();
	ArrayList<INodeCommand> deleteNodeCommands = new ArrayList<INodeCommand>();

	public RTree(PropertySet ps, IStorageManager sm) {
		rwLock = new RWLock();
		m_pStorageManager = sm;
		rootID = IStorageManager.NewPage;
		headerID = IStorageManager.NewPage;
		treeVariant = SpatialIndex.RtreeVariantQuadratic;
		fillFactor = 0.7f;
		indexCapacity = 100;
		leafCapacity = 100;
		nearMinimumOverlapFactor = 32;
		splitDistributionFactor = 0.4f;
		reinsertFactor = 0.3f;
		dimension = 2;

		infiniteRegion = new Region();
		stats = new Statistics();

		Object var = ps.getProperty("IndexIdentifier");
		if (var != null) {
			if (!(var instanceof Integer))
				throw new IllegalArgumentException("Property IndexIdentifier must an Integer");
			headerID = ((Integer) var).intValue();
			try {
				initOld(ps);
			} catch (IOException e) {
				System.err.println(e);
				throw new IllegalStateException("initOld failed with IOException");
			}
		} else {
			try {
				initNew(ps);
			} catch (IOException e) {
				System.err.println(e);
				throw new IllegalStateException("initNew failed with IOException");
			}
			Integer i = new Integer(headerID);
			ps.setProperty("IndexIdentifier", i);
		}
	}

	//
	// ISpatialIndex interface
	//

	public void insertData(final byte[] data, final IShape shape, int id) {
		if (shape.getDimension() != dimension)
			throw new IllegalArgumentException("insertData: Shape has the wrong number of dimensions.");

		rwLock.write_lock();

		try {
			Region mbr = shape.getMBR();

			byte[] buffer = null;

			if (data != null && data.length > 0) {
				buffer = new byte[data.length];
				System.arraycopy(data, 0, buffer, 0, data.length);
			}

			insertData_impl(buffer, mbr, id);
			// the buffer is stored in the tree. Do not delete here.
		} finally {
			rwLock.write_unlock();
		}
	}

	public boolean deleteData(final IShape shape, int id) {
		if (shape.getDimension() != dimension)
			throw new IllegalArgumentException("deleteData: Shape has the wrong number of dimensions.");

		rwLock.write_lock();

		try {
			Region mbr = shape.getMBR();
			return deleteData_impl(mbr, id);
		} finally {
			rwLock.write_unlock();
		}
	}

	public void containmentQuery(final IShape query, final IVisitor v) {
		if (query.getDimension() != dimension)
			throw new IllegalArgumentException("containmentQuery: Shape has the wrong number of dimensions.");
		rangeQuery(SpatialIndex.ContainmentQuery, query, v);
	}

	public void intersectionQuery(final IShape query, final IVisitor v) {
		if (query.getDimension() != dimension)
			throw new IllegalArgumentException("intersectionQuery: Shape has the wrong number of dimensions.");
		rangeQuery(SpatialIndex.IntersectionQuery, query, v);
	}

	public void pointLocationQuery(final IShape query, final IVisitor v) {
		if (query.getDimension() != dimension)
			throw new IllegalArgumentException("pointLocationQuery: Shape has the wrong number of dimensions.");

		Region r = null;
		if (query instanceof Point) {
			r = new Region((Point) query, (Point) query);
		} else if (query instanceof Region) {
			r = (Region) query;
		} else {
			throw new IllegalArgumentException("pointLocationQuery: IShape can be Point or Region only.");
		}

		rangeQuery(SpatialIndex.IntersectionQuery, r, v);
	}

	public void nearestNeighborQuery(int k, final IShape query, final IVisitor v,
			final INearestNeighborComparator nnc) {
		if (query.getDimension() != dimension)
			throw new IllegalArgumentException("nearestNeighborQuery: Shape has the wrong number of dimensions.");

		rwLock.read_lock();

		try {
			// I need a priority queue here. It turns out that TreeSet sorts
			// unique keys only and since I am
			// sorting according to distances, it is not assured that all
			// distances will be unique. TreeMap
			// also sorts unique keys. Thus, I am simulating a priority queue
			// using an ArrayList and binarySearch.
			ArrayList<NNEntry> queue = new ArrayList<NNEntry>();

			Node n = readNode(rootID);
			queue.add(new NNEntry(n, 0.0));

			int count = 0;
			double knearest = 0.0;

			while (queue.size() != 0) {
				NNEntry first = (NNEntry) queue.remove(0);

				if (first.node instanceof Node) {
					n = (Node) first.node;
					v.visitNode((INode) n);

					for (int cChild = 0; cChild < n.children; cChild++) {
						IEntry e;

						if (n.level == 0) {
							e = new Data(n.m_pData[cChild], n.pMBR[cChild], n.pIdentifiers[cChild]);
						} else {
							e = (IEntry) readNode(n.pIdentifiers[cChild]);
						}

						NNEntry e2 = new NNEntry(e, nnc.getMinimumDistance(query, e));

						// Why don't I use a TreeSet here? See comment above...
						int loc = Collections.binarySearch(queue, e2, new NNEntryComparator());
						if (loc >= 0)
							queue.add(loc, e2);
						else
							queue.add((-loc - 1), e2);
					}
				} else {
					// report all nearest neighbors with equal furthest
					// distances.
					// (neighbors can be more than k, if many happen to have the
					// same
					// furthest distance).
					if (count >= k && first.cost > knearest)
						break;

					v.visitData((IData) first.node);
					stats.m_queryResults++;
					count++;
					knearest = first.cost;
				}
			}
		} finally {
			rwLock.read_unlock();
		}
	}

	public void nearestNeighborQuery(int k, final IShape query, final IVisitor v) {
		if (query.getDimension() != dimension)
			throw new IllegalArgumentException("nearestNeighborQuery: Shape has the wrong number of dimensions.");
		NNComparator nnc = new NNComparator();
		nearestNeighborQuery(k, query, v, nnc);
	}

	public void queryStrategy(final IQueryStrategy qs) {
		rwLock.read_lock();

		int[] next = new int[] { rootID };

		try {
			while (true) {
				Node n = readNode(next[0]);
				boolean[] hasNext = new boolean[] { false };
				qs.getNextEntry(n, next, hasNext);
				if (hasNext[0] == false)
					break;
			}
		} finally {
			rwLock.read_unlock();
		}
	}

	public PropertySet getIndexProperties() {
		PropertySet pRet = new PropertySet();

		// dimension
		pRet.setProperty("Dimension", new Integer(dimension));

		// index capacity
		pRet.setProperty("IndexCapacity", new Integer(indexCapacity));

		// leaf capacity
		pRet.setProperty("LeafCapacity", new Integer(leafCapacity));

		// R-tree variant
		pRet.setProperty("TreeVariant", new Integer(treeVariant));

		// fill factor
		pRet.setProperty("FillFactor", new Double(fillFactor));

		// near minimum overlap factor
		pRet.setProperty("NearMinimumOverlapFactor", new Integer(nearMinimumOverlapFactor));

		// split distribution factor
		pRet.setProperty("SplitDistributionFactor", new Double(splitDistributionFactor));

		// reinsert factor
		pRet.setProperty("ReinsertFactor", new Double(reinsertFactor));

		return pRet;
	}

	public void addWriteNodeCommand(INodeCommand nc) {
		writeNodeCommands.add(nc);
	}

	public void addReadNodeCommand(INodeCommand nc) {
		readNodeCommands.add(nc);
	}

	public void addDeleteNodeCommand(INodeCommand nc) {
		deleteNodeCommands.add(nc);
	}

	public boolean isIndexValid() {
		boolean ret = true;
		Stack<ValidateEntry> st = new Stack<ValidateEntry>();
		Node root = readNode(rootID);

		if (root.level != stats.m_treeHeight - 1) {
			System.err.println("Invalid tree height");
			return false;
		}

		HashMap<Integer, Integer> nodesInLevel = new HashMap<Integer, Integer>();
		nodesInLevel.put(new Integer(root.level), new Integer(1));

		ValidateEntry e = new ValidateEntry(root.m_nodeMBR, root);
		st.push(e);

		while (!st.empty()) {
			e = (ValidateEntry) st.pop();

			Region tmpRegion = (Region) infiniteRegion.clone();

			for (int cDim = 0; cDim < dimension; cDim++) {
				tmpRegion.m_pLow[cDim] = Double.POSITIVE_INFINITY;
				tmpRegion.m_pHigh[cDim] = Double.NEGATIVE_INFINITY;

				for (int cChild = 0; cChild < e.m_pNode.children; cChild++) {
					tmpRegion.m_pLow[cDim] = Math.min(tmpRegion.m_pLow[cDim], e.m_pNode.pMBR[cChild].m_pLow[cDim]);
					tmpRegion.m_pHigh[cDim] = Math.max(tmpRegion.m_pHigh[cDim], e.m_pNode.pMBR[cChild].m_pHigh[cDim]);
				}
			}

			if (!(tmpRegion.equals(e.m_pNode.m_nodeMBR))) {
				System.err.println("Invalid parent information");
				ret = false;
			} else if (!(tmpRegion.equals(e.m_parentMBR))) {
				System.err.println("Error in parent");
				ret = false;
			}

			if (e.m_pNode.level != 0) {
				for (int cChild = 0; cChild < e.m_pNode.children; cChild++) {
					ValidateEntry tmpEntry = new ValidateEntry(e.m_pNode.pMBR[cChild],
							readNode(e.m_pNode.pIdentifiers[cChild]));

					if (!nodesInLevel.containsKey(new Integer(tmpEntry.m_pNode.level))) {
						nodesInLevel.put(new Integer(tmpEntry.m_pNode.level), new Integer(1));
					} else {
						int i = ((Integer) nodesInLevel.get(new Integer(tmpEntry.m_pNode.level))).intValue();
						nodesInLevel.put(new Integer(tmpEntry.m_pNode.level), new Integer(i + 1));
					}

					st.push(tmpEntry);
				}
			}
		}

		int nodes = 0;
		for (int cLevel = 0; cLevel < stats.m_treeHeight; cLevel++) {
			int i1 = ((Integer) nodesInLevel.get(new Integer(cLevel))).intValue();
			int i2 = ((Integer) stats.m_nodesInLevel.get(cLevel)).intValue();
			if (i1 != i2) {
				System.err.println("Invalid nodesInLevel information");
				ret = false;
			}

			nodes += i2;
		}

		if (nodes != stats.m_nodes) {
			System.err.println("Invalid number of nodes information");
			ret = false;
		}

		return ret;
	}

	public IStatistics getStatistics() {
		return (IStatistics) stats.clone();
	}

	public void flush() throws IllegalStateException {
		try {
			storeHeader();
			m_pStorageManager.flush();
		} catch (IOException e) {
			System.err.println(e);
			throw new IllegalStateException("flush failed with IOException");
		}
	}

	//
	// Internals
	//

	private void initNew(PropertySet ps) throws IOException {
		Object var;

		// tree variant.
		var = ps.getProperty("TreeVariant");
		if (var != null) {
			if (var instanceof Integer) {
				int i = ((Integer) var).intValue();
				if (i != SpatialIndex.RtreeVariantLinear && i != SpatialIndex.RtreeVariantQuadratic
						&& i != SpatialIndex.RtreeVariantRstar)
					throw new IllegalArgumentException("Property TreeVariant not a valid variant");
				treeVariant = i;
			} else {
				throw new IllegalArgumentException("Property TreeVariant must be an Integer");
			}
		}

		// fill factor.
		var = ps.getProperty("FillFactor");
		if (var != null) {
			if (var instanceof Double) {
				double f = ((Double) var).doubleValue();
				if (f <= 0.0f || f >= 1.0f)
					throw new IllegalArgumentException("Property FillFactor must be in (0.0, 1.0)");
				fillFactor = f;
			} else {
				throw new IllegalArgumentException("Property FillFactor must be a Double");
			}
		}

		// index capacity.
		var = ps.getProperty("IndexCapacity");
		if (var != null) {
			if (var instanceof Integer) {
				int i = ((Integer) var).intValue();
				if (i < 3)
					throw new IllegalArgumentException("Property IndexCapacity must be >= 3");
				indexCapacity = i;
			} else {
				throw new IllegalArgumentException("Property IndexCapacity must be an Integer");
			}
		}

		// leaf capacity.
		var = ps.getProperty("LeafCapacity");
		if (var != null) {
			if (var instanceof Integer) {
				int i = ((Integer) var).intValue();
				if (i < 3)
					throw new IllegalArgumentException("Property LeafCapacity must be >= 3");
				leafCapacity = i;
			} else {
				throw new IllegalArgumentException("Property LeafCapacity must be an Integer");
			}
		}

		// near minimum overlap factor.
		var = ps.getProperty("NearMinimumOverlapFactor");
		if (var != null) {
			if (var instanceof Integer) {
				int i = ((Integer) var).intValue();
				if (i < 1 || i > indexCapacity || i > leafCapacity)
					throw new IllegalArgumentException(
							"Property NearMinimumOverlapFactor must be less than both index and leaf capacities");
				nearMinimumOverlapFactor = i;
			} else {
				throw new IllegalArgumentException("Property NearMinimumOverlapFactor must be an Integer");
			}
		}

		// split distribution factor.
		var = ps.getProperty("SplitDistributionFactor");
		if (var != null) {
			if (var instanceof Double) {
				double f = ((Double) var).doubleValue();
				if (f <= 0.0f || f >= 1.0f)
					throw new IllegalArgumentException("Property SplitDistributionFactor must be in (0.0, 1.0)");
				splitDistributionFactor = f;
			} else {
				throw new IllegalArgumentException("Property SplitDistriburionFactor must be a Double");
			}
		}

		// reinsert factor.
		var = ps.getProperty("ReinsertFactor");
		if (var != null) {
			if (var instanceof Double) {
				double f = ((Double) var).doubleValue();
				if (f <= 0.0f || f >= 1.0f)
					throw new IllegalArgumentException("Property ReinsertFactor must be in (0.0, 1.0)");
				reinsertFactor = f;
			} else {
				throw new IllegalArgumentException("Property ReinsertFactor must be a Double");
			}
		}

		// dimension
		var = ps.getProperty("Dimension");
		if (var != null) {
			if (var instanceof Integer) {
				int i = ((Integer) var).intValue();
				if (i <= 1)
					throw new IllegalArgumentException("Property Dimension must be >= 1");
				dimension = i;
			} else {
				throw new IllegalArgumentException("Property Dimension must be an Integer");
			}
		}

		infiniteRegion.m_pLow = new double[dimension];
		infiniteRegion.m_pHigh = new double[dimension];

		for (int cDim = 0; cDim < dimension; cDim++) {
			infiniteRegion.m_pLow[cDim] = Double.POSITIVE_INFINITY;
			infiniteRegion.m_pHigh[cDim] = Double.NEGATIVE_INFINITY;
		}

		stats.m_treeHeight = 1;
		stats.m_nodesInLevel.add(new Integer(0));

		Leaf root = new Leaf(this, -1);
		rootID = writeNode(root);

		storeHeader();
	}

	private void initOld(PropertySet ps) throws IOException {
		loadHeader();

		// only some of the properties may be changed.
		// the rest are just ignored.

		Object var;

		// tree variant.
		var = ps.getProperty("TreeVariant");
		if (var != null) {
			if (var instanceof Integer) {
				int i = ((Integer) var).intValue();
				if (i != SpatialIndex.RtreeVariantLinear && i != SpatialIndex.RtreeVariantQuadratic
						&& i != SpatialIndex.RtreeVariantRstar)
					throw new IllegalArgumentException("Property TreeVariant not a valid variant");
				treeVariant = i;
			} else {
				throw new IllegalArgumentException("Property TreeVariant must be an Integer");
			}
		}

		// near minimum overlap factor.
		var = ps.getProperty("NearMinimumOverlapFactor");
		if (var != null) {
			if (var instanceof Integer) {
				int i = ((Integer) var).intValue();
				if (i < 1 || i > indexCapacity || i > leafCapacity)
					throw new IllegalArgumentException(
							"Property NearMinimumOverlapFactor must be less than both index and leaf capacities");
				nearMinimumOverlapFactor = i;
			} else {
				throw new IllegalArgumentException("Property NearMinimumOverlapFactor must be an Integer");
			}
		}

		// split distribution factor.
		var = ps.getProperty("SplitDistributionFactor");
		if (var != null) {
			if (var instanceof Double) {
				double f = ((Double) var).doubleValue();
				if (f <= 0.0f || f >= 1.0f)
					throw new IllegalArgumentException("Property SplitDistributionFactor must be in (0.0, 1.0)");
				splitDistributionFactor = f;
			} else {
				throw new IllegalArgumentException("Property SplitDistriburionFactor must be a Double");
			}
		}

		// reinsert factor.
		var = ps.getProperty("ReinsertFactor");
		if (var != null) {
			if (var instanceof Double) {
				double f = ((Double) var).doubleValue();
				if (f <= 0.0f || f >= 1.0f)
					throw new IllegalArgumentException("Property ReinsertFactor must be in (0.0, 1.0)");
				reinsertFactor = f;
			} else {
				throw new IllegalArgumentException("Property ReinsertFactor must be a Double");
			}
		}

		infiniteRegion.m_pLow = new double[dimension];
		infiniteRegion.m_pHigh = new double[dimension];

		for (int cDim = 0; cDim < dimension; cDim++) {
			infiniteRegion.m_pLow[cDim] = Double.POSITIVE_INFINITY;
			infiniteRegion.m_pHigh[cDim] = Double.NEGATIVE_INFINITY;
		}
	}

	private void storeHeader() throws IOException {
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		DataOutputStream ds = new DataOutputStream(bs);

		ds.writeInt(rootID);
		ds.writeInt(treeVariant);
		ds.writeDouble(fillFactor);
		ds.writeInt(indexCapacity);
		ds.writeInt(leafCapacity);
		ds.writeInt(nearMinimumOverlapFactor);
		ds.writeDouble(splitDistributionFactor);
		ds.writeDouble(reinsertFactor);
		ds.writeInt(dimension);
		ds.writeLong(stats.m_nodes);
		ds.writeLong(stats.m_data);
		ds.writeInt(stats.m_treeHeight);

		for (int cLevel = 0; cLevel < stats.m_treeHeight; cLevel++) {
			ds.writeInt(((Integer) stats.m_nodesInLevel.get(cLevel)).intValue());
		}

		ds.flush();
		headerID = m_pStorageManager.storeByteArray(headerID, bs.toByteArray());
	}

	private void loadHeader() throws IOException {
		byte[] data = m_pStorageManager.loadByteArray(headerID);
		DataInputStream ds = new DataInputStream(new ByteArrayInputStream(data));

		rootID = ds.readInt();
		treeVariant = ds.readInt();
		fillFactor = ds.readDouble();
		indexCapacity = ds.readInt();
		leafCapacity = ds.readInt();
		nearMinimumOverlapFactor = ds.readInt();
		splitDistributionFactor = ds.readDouble();
		reinsertFactor = ds.readDouble();
		dimension = ds.readInt();
		stats.m_nodes = ds.readLong();
		stats.m_data = ds.readLong();
		stats.m_treeHeight = ds.readInt();

		for (int cLevel = 0; cLevel < stats.m_treeHeight; cLevel++) {
			stats.m_nodesInLevel.add(new Integer(ds.readInt()));
		}
	}

	protected void insertData_impl(byte[] pData, Region mbr, int id) {
		assert mbr.getDimension() == dimension;

		boolean[] overflowTable;

		Stack<?> pathBuffer = new Stack<Object>();

		Node root = readNode(rootID);

		overflowTable = new boolean[root.level];
		for (int cLevel = 0; cLevel < root.level; cLevel++)
			overflowTable[cLevel] = false;

		Node l = root.chooseSubtree(mbr, 0, pathBuffer);
		l.insertData(pData, mbr, id, pathBuffer, overflowTable);

		stats.m_data++;
	}

	protected void insertData_impl(byte[] pData, Region mbr, int id, int level, boolean[] overflowTable) {
		assert mbr.getDimension() == dimension;

		Stack<?> pathBuffer = new Stack<Object>();

		Node root = readNode(rootID);
		Node n = root.chooseSubtree(mbr, level, pathBuffer);
		n.insertData(pData, mbr, id, pathBuffer, overflowTable);
	}

	protected boolean deleteData_impl(final Region mbr, int id) {
		assert mbr.getDimension() == dimension;

		boolean bRet = false;

		Stack<?> pathBuffer = new Stack<Object>();

		Node root = readNode(rootID);
		Leaf l = root.findLeaf(mbr, id, pathBuffer);

		if (l != null) {
			l.deleteData(id, pathBuffer);
			stats.m_data--;
			bRet = true;
		}

		return bRet;
	}

	protected int writeNode(Node n) throws IllegalStateException {
		byte[] buffer = null;

		try {
			buffer = n.store();
		} catch (IOException e) {
			System.err.println(e);
			throw new IllegalStateException("writeNode failed with IOException");
		}

		int page;
		if (n.identifier < 0)
			page = IStorageManager.NewPage;
		else
			page = n.identifier;

		try {
			page = m_pStorageManager.storeByteArray(page, buffer);
		} catch (InvalidPageException e) {
			System.err.println(e);
			throw new IllegalStateException("writeNode failed with InvalidPageException");
		}

		if (n.identifier < 0) {
			n.identifier = page;
			stats.m_nodes++;
			int i = ((Integer) stats.m_nodesInLevel.get(n.level)).intValue();
			stats.m_nodesInLevel.set(n.level, new Integer(i + 1));
		}

		stats.m_writes++;

		for (int cIndex = 0; cIndex < writeNodeCommands.size(); cIndex++) {
			((INodeCommand) writeNodeCommands.get(cIndex)).execute(n);
		}

		return page;
	}

	protected Node readNode(int id) {
		byte[] buffer;
		DataInputStream ds = null;
		int nodeType = -1;
		Node n = null;

		try {
			buffer = m_pStorageManager.loadByteArray(id);
			ds = new DataInputStream(new ByteArrayInputStream(buffer));
			nodeType = ds.readInt();

			if (nodeType == SpatialIndex.PersistentIndex)
				n = new Index(this, -1, 0);
			else if (nodeType == SpatialIndex.PersistentLeaf)
				n = new Leaf(this, -1);
			else
				throw new IllegalStateException("readNode failed reading the correct node type information");

			n.m_pTree = this;
			n.identifier = id;
			n.load(buffer);

			stats.m_reads++;
		} catch (InvalidPageException e) {
			System.err.println(e);
			throw new IllegalStateException("readNode failed with InvalidPageException");
		} catch (IOException e) {
			System.err.println(e);
			throw new IllegalStateException("readNode failed with IOException");
		}

		for (int cIndex = 0; cIndex < readNodeCommands.size(); cIndex++) {
			((INodeCommand) readNodeCommands.get(cIndex)).execute(n);
		}

		return n;
	}

	protected void deleteNode(Node n) {
		try {
			m_pStorageManager.deleteByteArray(n.identifier);
		} catch (InvalidPageException e) {
			System.err.println(e);
			throw new IllegalStateException("deleteNode failed with InvalidPageException");
		}

		stats.m_nodes--;
		int i = ((Integer) stats.m_nodesInLevel.get(n.level)).intValue();
		stats.m_nodesInLevel.set(n.level, new Integer(i - 1));

		for (int cIndex = 0; cIndex < deleteNodeCommands.size(); cIndex++) {
			((INodeCommand) deleteNodeCommands.get(cIndex)).execute(n);
		}
	}

	private void rangeQuery(int type, final IShape query, final IVisitor v) {
		rwLock.read_lock();

		try {
			Stack<Node> st = new Stack<Node>();
			Node root = readNode(rootID);

			if (root.children > 0 && query.intersects(root.m_nodeMBR))
				st.push(root);

			while (!st.empty()) {
				Node n = (Node) st.pop();

				if (n.level == 0) {
					v.visitNode((INode) n);

					for (int cChild = 0; cChild < n.children; cChild++) {
						boolean b;
						if (type == SpatialIndex.ContainmentQuery)
							b = query.contains(n.pMBR[cChild]);
						else
							b = query.intersects(n.pMBR[cChild]);

						if (b) {
							Data data = new Data(n.m_pData[cChild], n.pMBR[cChild], n.pIdentifiers[cChild]);
							v.visitData(data);
							stats.m_queryResults++;
						}
					}
				} else {
					v.visitNode((INode) n);

					for (int cChild = 0; cChild < n.children; cChild++) {
						if (query.intersects(n.pMBR[cChild])) {
							st.push(readNode(n.pIdentifiers[cChild]));
						}
					}
				}
			}
		} finally {
			rwLock.read_unlock();
		}
	}

	public String toString() {
		String s = "Dimension: " + dimension + "\n" + "Fill factor: " + fillFactor + "\n" + "Index capacity: "
				+ indexCapacity + "\n" + "Leaf capacity: " + leafCapacity + "\n";

		if (treeVariant == SpatialIndex.RtreeVariantRstar) {
			s += "Near minimum overlap factor: " + nearMinimumOverlapFactor + "\n" + "Reinsert factor: "
					+ reinsertFactor + "\n" + "Split distribution factor: " + splitDistributionFactor + "\n";
		}

		s += "Utilization: " + 100 * stats.getNumberOfData() / (stats.getNumberOfNodesInLevel(0) * leafCapacity)
				+ "%" + "\n" + stats;

		return s;
	}

	class NNComparator implements INearestNeighborComparator {
		public double getMinimumDistance(IShape query, IEntry e) {
			IShape s = e.getShape();
			return query.getMinimumDistance(s);
		}
	}

	class ValidateEntry {
		Region m_parentMBR;
		Node m_pNode;

		ValidateEntry(Region r, Node pNode) {
			m_parentMBR = r;
			m_pNode = pNode;
		}
	}

	class Data implements IData {
		int m_id;
		Region m_shape;
		byte[] m_pData;

		Data(byte[] pData, Region mbr, int id) {
			m_id = id;
			m_shape = mbr;
			m_pData = pData;
		}

		public int getIdentifier() {
			return m_id;
		}

		public IShape getShape() {
			return new Region(m_shape);
		}

		public byte[] getData() {
			byte[] data = new byte[m_pData.length];
			System.arraycopy(m_pData, 0, data, 0, m_pData.length);
			return data;
		}

		@Override
		public String toString() {
			return "Data [m_id=" + m_id + ", m_shape=" + m_shape + ", m_pData=" + Arrays.toString(m_pData) + "]";
		}
		
	}

	public void ir(DocumentStore ds, InvertedFile invertedFile) throws Exception {

		Node n = readNode(rootID);

		ir_traversal(ds, invertedFile, n);

	}

	private Vector<?> ir_traversal(DocumentStore ds, InvertedFile invertedFile, Node n) throws Exception {

		if (n.level == 0) {

			invertedFile.create(n.identifier);

			int child;
			for (child = 0; child < n.children; child++) {
				int docID = n.pIdentifiers[child];

				Vector<?> document = ds.read(docID);
				if (document == null) {
					System.out.println("Can't find document " + docID);
					System.exit(-1);
				}
				invertedFile.addDocument(n.identifier, docID, document);
			}

			Vector<?> pseudoDoc = invertedFile.store(n.identifier);

			return pseudoDoc;

		} else {

			invertedFile.create(n.identifier);
			System.out.println("processing index node " + n.identifier);

			int child;
			for (child = 0; child < n.children; child++) {
				Node nn = readNode(n.pIdentifiers[child]);
				Vector<?> pseudoDoc = ir_traversal(ds, invertedFile, nn);
				int docID = n.pIdentifiers[child];

				if (pseudoDoc == null) {
					System.out.println("Can't find document " + docID);
					System.exit(-1);

				}
				invertedFile.addDocument(n.identifier, docID, pseudoDoc);

			}

			Vector<?> pseudoDoc = invertedFile.store(n.identifier);

			return pseudoDoc;

		}
	}

	public void clusterEnhance(BTree clustertree, DocumentStore ds, InvertedFile invertedFile) throws Exception {
		Node n = readNode(rootID);
		cluster_traversal(clustertree, ds, invertedFile, n);
	}

	private Vector[] cluster_traversal(BTree clustertree, DocumentStore ds, InvertedFile invertedFile, Node n)
			throws Exception {
		if (n.level == 0) {
			invertedFile.create(n.identifier);

			int child;
			for (child = 0; child < n.children; child++) {
				int docID = n.pIdentifiers[child];

				Vector<?> document = ds.read(docID);
				if (document == null) {
					System.out.println("Couldn't find document " + docID);
					System.exit(-1);
				}

				Object var = clustertree.find(docID);
				if (var == null) {
					System.out.println("Couldn't find cluster " + docID);
					System.exit(-1);
				}
				int cluster = (Integer) var;
				invertedFile.addDocument(n.identifier, docID, document, cluster);
			}
			Vector[] pseudoDoc = invertedFile.storeClusterEnhance(n.identifier);

			return pseudoDoc;
		} else {
			invertedFile.create(n.identifier);
			System.out.println("processing index node " + n.identifier);
			int child;
			for (child = 0; child < n.children; child++) {
				Node nn = readNode(n.pIdentifiers[child]);
				Vector[] pseudoDoc = cluster_traversal(clustertree, ds, invertedFile, nn);
				int docID = n.pIdentifiers[child];
				if (pseudoDoc == null) {
					System.out.println("Couldn't find document " + docID);
					System.exit(-1);

				}
				for (int i = 0; i < pseudoDoc.length; i++) {
					if (pseudoDoc[i].size() == 0)
						continue;
					invertedFile.addDocument(n.identifier, docID, pseudoDoc[i], i);
				}
			}
			Vector[] pseudoDoc = invertedFile.storeClusterEnhance(n.identifier);

			return pseudoDoc;
		}
	}

	public void lkt(InvertedFile invertedFile, Query q, int topk) throws Exception {

		PriorityQueue<NNEntry> queue = new PriorityQueue<NNEntry>(100, new NNEntryComparator());
		RtreeEntry e = new RtreeEntry(rootID, false);
		queue.add(new NNEntry(e, 0.0));
		
		int count = 0;
		double knearest = 0.0;
		while (queue.size() != 0) {
			NNEntry first = queue.poll();
			e = (RtreeEntry) first.node;
			
			noOfVisitedNodes++;

			if (e.isLeafEntry) {
				if (count >= topk && first.cost > knearest)
					break;

				count++;
				System.out.println(e.getIdentifier() + "," + first.cost);
				knearest = first.cost;
			} else {
				Node n = readNode(e.getIdentifier());

				HashMap<Integer, Double> filter;
				
				invertedFile.load(n.identifier);
				if (numOfClusters != 0)
					filter = invertedFile.rankingSumClusterEnhance(q.keywords);
				else
					filter = invertedFile.rankingSum(q.keywords);

				for (int child = 0; child < n.children; child++) {
					double irscore;
					Object var = filter.get(n.pIdentifiers[child]);
					if (var == null)
						continue;
					else
						irscore = (Double) var;

					if (n.level == 0) {
						e = new RtreeEntry(n.pIdentifiers[child], true);
					} else {
						e = new RtreeEntry(n.pIdentifiers[child], false);
					}
					double mind = combinedScore(n.pMBR[child].getMinimumDistance(q.location), irscore);

					queue.add(new NNEntry(e, mind));

				}
			}
		}

	}

	public static double combinedScore(double spatial, double ir) {
		// TODO divide with max value instead
		return (alpha_dist * spatial / 12 + (1 - alpha_dist) * (1 - ir));
	}

	public int getIO() {
		return m_pStorageManager.getIO();
	}
}
