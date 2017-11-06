package DataMining;

import java.util.ArrayList;

/**
 * 图结构类
 * 
 * @author cuilijuan
 * 
 */
public class Graph {
	// 图节点标号组
	ArrayList<Integer> nodeLabels;
	// 图的边标号组
	ArrayList<ArrayList<Integer>> edgeLabels;
	// 边2头的节点id号,在这里可以理解为下标号
	ArrayList<ArrayList<Integer>> edgeNexts;
	// 是否是图i的子图
    boolean[] isFromGroups;

	boolean[][] isVis;

	public Graph() {
		nodeLabels = new ArrayList<>();
		edgeLabels = new ArrayList<>();
		edgeNexts = new ArrayList<>();
	}

	public double getSup( ArrayList<Graph> totalGraphs ) {
	    int size = totalGraphs.size();
	    initIsFromGroups( size );
	    for( int i = 0; i < size; ++i ) {
	        Graph graph = totalGraphs.get( i );
	        for( int j = 0; j < nodeLabels.size(); ++j ) {
	            for( int k = 0; k < edgeLabels.get(j).size(); ++k ) {
	                int u = j;
	                int v = edgeLabels.get(j).get(k);
	                int w = edgeLabels.get(j).get(k);
	                if( !graph.hasEdge( u, v, w ) ) {
	                    isFromGroups[i] = false;
	                    break;
                    }
                }
                if( !isFromGroups[i] ) {
	                break;
                }
            }
        }
        int cnt = 0;
        for( int i = 0; i < size; ++i ) {
	        if( isFromGroups[i] ) {
	            cnt += 1;
            }
        }
        return 1.0 * cnt / size;
    }

    private void initIsFromGroups( int groupsSize ) {
	    isFromGroups = new boolean[groupsSize];
	    for( int i = 0; i < groupsSize; ++i ) {
	        isFromGroups[i] = true;
        }
    }

    public void initIsVis() {
	    int nodeNum = nodeLabels.size();
	    isVis = new boolean[nodeNum][nodeNum];
    }

	public ArrayList<Integer> getNodeLabels() {
		return nodeLabels;
	}

	public void setNodeLabels(ArrayList<Integer> nodeLabels) {
		this.nodeLabels = nodeLabels;
	}
	
	public ArrayList<ArrayList<Integer>> getEdgeLabels() {
		return edgeLabels;
	}

	public void setEdgeLabels(ArrayList<ArrayList<Integer>> edgeLabels) {
		this.edgeLabels = edgeLabels;
	}
	
	public ArrayList<ArrayList<Integer>> getEdgeNexts() {
		return edgeNexts;
	}

	public void setEdgeNexts(ArrayList<ArrayList<Integer>> edgeNexts) {
		this.edgeNexts = edgeNexts;
	}

	/**
	 * 判断图中是否存在某条边
	 * 
	 * @param x
	 *            边的一端的节点标号
	 * @param a
	 *            边的标号
	 * @param y
	 *            边的另外一端节点标号
	 * @return
	 */
	public boolean hasEdge(int x, int a, int y) {
		boolean isContained = false;
		int t;

		for (int i = 0; i < nodeLabels.size(); i++) {
			// 先寻找2个端点标号,t代表找到的点的另外一个端点标号
			if (nodeLabels.get(i) == x) {
				t = y;
			} else if (nodeLabels.get(i) == y) {
				t = x;
			} else {
				continue;
			}

			for (int j = 0; j < edgeNexts.get(i).size(); j++) {
				// 从此端点的所连接的点去比较对应的点和边
				if (edgeLabels.get(i).get(j) == a
						&& nodeLabels.get(edgeNexts.get(i).get(j)) == t) {
					isContained = true;
					return isContained;
				}
			}
		}

		return isContained;
	}

	/**
	 * 在图中移除某个边
	 * 
	 * @param x
	 *            边的某端的一个点标号
	 * @param a
	 *            边的标号
	 * @param y
	 *            边的另一端的一个点标号
	 */
	public void removeEdge(int x, int a, int y) {
		int t;

		for (int i = 0; i < nodeLabels.size(); i++) {
			// 先寻找2个端点标号,t代表找到的点的另外一个端点标号
			if (nodeLabels.get(i) == x) {
				t = y;
			} else if (nodeLabels.get(i) == y) {
				t = x;
			} else {
				continue;
			}

			for (int j = 0; j < edgeNexts.get(i).size(); j++) {
				// 从此端点的所连接的点去比较对应的点和边
				if (edgeLabels.get(i).get(j) == a
						&& nodeLabels.get(edgeNexts.get(i).get(j)) == t) {
					int id;
					// 在连接的点中去除该点
					edgeLabels.get(i).remove(j);

					id = edgeNexts.get(i).get(j);
					edgeNexts.get(i).remove(j);
					for (int k = 0; k < edgeNexts.get(id).size(); k++) {
						if (edgeNexts.get(id).get(k) == i) {
							edgeNexts.get(id).remove(k);
							break;
						}
					}
					break;
				}
			}
		}

	}

	/**
	 * 根据图数据构造一个图
	 * 
	 * @param gd
	 *            图数据
	 * @return
	 */
	public Graph constructGraph(GraphData gd) {
		Graph graph = new Graph();

		
		// 构造一个图需要知道3点，
		// 1.图中有哪些点
        // 2.图中的每个点周围连着哪些点
        // 3.每个点周围连着哪些边
		for (int i = 0; i < gd.getNodeVisibles().size(); i++) {
			if (gd.getNodeVisibles().get(i)) {
				graph.getNodeLabels().add(gd.getNodeLabels().get(i));
			}
			
			// 添加对应id下的集合
			// id节点后有多少相连的边的标号
			graph.edgeLabels.add(new ArrayList<Integer>());
			// id节点后有多少相连的节点的id
			graph.edgeNexts.add(new ArrayList<Integer>());
		}

		for (int i = 0; i < gd.getEdgeLabels().size(); i++) {
			if (gd.getEdgeVisibles().get(i)) {
				// 在此后面添加一个边标号
				graph.edgeLabels.get(gd.getEdgeX().get(i)).add(gd.getEdgeLabels().get(i));
				graph.edgeLabels.get(gd.getEdgeY().get(i)).add(gd.getEdgeLabels().get(i));
				graph.edgeNexts.get(gd.getEdgeX().get(i)).add(
						gd.getEdgeY().get(i));
				graph.edgeNexts.get(gd.getEdgeY().get(i)).add(
						gd.getEdgeX().get(i));
			}
		}

		return graph;
	}
}
