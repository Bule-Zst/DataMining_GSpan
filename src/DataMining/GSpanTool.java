package DataMining;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * gSpan频繁子图挖掘算法工具类
 * 
 * @author cuilijuan
 * 
 */
public class GSpanTool {
	// 文件数据类型
	//表示一个图的开始
	public final String INPUT_NEW_GRAPH = "t";
	//表示节点
	public final String INPUT_VERTICE = "v";
	//表示边
	public final String INPUT_EDGE = "e";
	// Label标号的最大数量，包括点标号和边标号
	public final int LABEL_MAX = 100;

	// 测试数据文件地址
	private String filePath;
	// 最小支持度率
	private double minSupportRate;
	// 最小支持度数，通过图总数与最小支持度率的乘积计算所得
	private int minSupportCount;
	// 初始所有图的数据
	private ArrayList<GraphData> totalGraphDatas;
	// 所有的图结构数据
	private ArrayList<Graph> totalGraphs;
	// 挖掘出的频繁子图
	private ArrayList<Graph> resultGraphs;
	// 边的频度统计
	private EdgeFrequency ef;
	// 节点的频度
	private int[] freqNodeLabel;
	// 两节点同时出现的频度
    private int[][] freqNodeLabels;
	// 边的频度
	private int[] freqEdgeLabel;
	// 重新标号之后的点的标号数
	private int newNodeLabelNum = 0;
	// 重新标号后的边的标号数
	private int newEdgeLabelNum = 0;
    private double meanWeight;
    // 重新编号后节点的个数
    private int nodeNum;

	public GSpanTool(String filePath, double minSupportRate) {
		this.filePath = filePath;
		this.minSupportRate = minSupportRate;
		readDataFile();
	}

	/**
	 * 从文件中读取数据
	 */
	private void readDataFile() {
        File file = new File(filePath);
		ArrayList<String[]> dataArray = new ArrayList<>();

		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String str;
			String[] tempArray;
			while ((str = in.readLine()) != null) {
                tempArray = str.split(" ");
				dataArray.add(tempArray);
			}
			in.close();
		} catch (IOException e) {
			e.getStackTrace();
		}

		calFrequentAndRemove(dataArray);
	}

	/**
	 * 统计边和点的频度，并移除不频繁的点边，以标号作为统计的变量
	 * 
	 * @param dataArray
	 *            原始数据
	 */
	private void calFrequentAndRemove(ArrayList<String[]> dataArray) {
		int tempCount = 0;
		freqNodeLabel = new int[LABEL_MAX];
		freqEdgeLabel = new int[LABEL_MAX];

		// 做初始化操作
		for (int i = 0; i < LABEL_MAX; i++) {
			// 代表标号为i的节点目前的数量为0
			freqNodeLabel[i] = 0;
			freqEdgeLabel[i] = 0;
		}

		//原始图的数据
		// 包含：
		//节点组标号
		// 节点是否可用,可能被移除
		
		// 边的集合标号
		// 边的一边点id
		// 边的另一边的点id
		// 边是否可用

		GraphData gd = null;
		//原始图数据的集合
		totalGraphDatas = new ArrayList<>();
		//读取输入文件，将其存放在一个数组集合中
		for (String[] array : dataArray) {
			if (array[0].equals(INPUT_NEW_GRAPH)) {
				if (gd != null) {
					//添加一个图信息
					totalGraphDatas.add(gd);
				}

				// 新建图（图的数据信息）
				gd = new GraphData();
			} else if (array[0].equals(INPUT_VERTICE)) {
				// 每个图中的每种图只统计一次
				if (!gd.getNodeLabels().contains(Integer.parseInt(array[2]))) {
					//节点的频度
					tempCount = freqNodeLabel[Integer.parseInt(array[2])];
					//节点的频度+1
					tempCount++;
					freqNodeLabel[Integer.parseInt(array[2])] = tempCount;
				}

				gd.getNodeLabels().add(Integer.parseInt(array[2]));
				gd.getNodeVisibles().add(true);
			} else if (array[0].equals(INPUT_EDGE)) {
				// 每个图中的每种图只统计一次
				if (!gd.getEdgeLabels().contains(Integer.parseInt(array[3]))) {
					//边的频度
					tempCount = freqEdgeLabel[Integer.parseInt(array[3])];
					//边点的频度+1
					tempCount++;
					freqEdgeLabel[Integer.parseInt(array[3])] = tempCount;
				}

				int i = Integer.parseInt(array[1]);
				int j = Integer.parseInt(array[2]);

				gd.getEdgeLabels().add(Integer.parseInt(array[3]));
				gd.getEdgeX().add(i);
				gd.getEdgeY().add(j);
				gd.getEdgeVisibles().add(true);
			}
		}
		// 把最后一个图数据信息gd数据加入
		//gd是图的相关数据信息
		//得到一个初始图集
		totalGraphDatas.add(gd);
		
		//最小支持度数，通过图总数与最小支持度率的乘积计算所得
		minSupportCount = (int) (minSupportRate * totalGraphDatas.size());

		for (GraphData g : totalGraphDatas) {
			//根据点边频繁度移除图中不频繁的点边
			g.removeInFreqNodeAndEdge(freqNodeLabel, freqEdgeLabel,
					minSupportCount);
		}
	}

	/**
	 * 根据标号频繁度进行排序并且重新标号
	 */
	private void sortAndReLabel() {
		int label1 = 0;
		int label2 = 0;
		int temp = 0;
		// 点排序名次
		int[] rankNodeLabels = new int[LABEL_MAX];
		// 边排序名次
		int[] rankEdgeLabels = new int[LABEL_MAX];
		// 标号对应排名
		int[] nodeLabel2Rank = new int[LABEL_MAX];
		int[] edgeLabel2Rank = new int[LABEL_MAX];

		for (int i = 0; i < LABEL_MAX; i++) {
			// 表示排名第i位的标号为i，[i]中的i表示排名
			rankNodeLabels[i] = i;
			rankEdgeLabels[i] = i;
		}

		//节点的频度数组：freqNodeLabel
		for (int i = 0; i < freqNodeLabel.length - 1; i++) {
			int k = 0;
			label1 = rankNodeLabels[i];
			temp = label1;
			for (int j = i + 1; j < freqNodeLabel.length; j++) {
				label2 = rankNodeLabels[j];

				if (freqNodeLabel[temp] < freqNodeLabel[label2]) {
					// 进行标号的互换
					temp = label2;
					k = j;
				}
			}

			if (temp != label1) {
				// 进行i，k排名下的标号对调
				temp = rankNodeLabels[k];
				rankNodeLabels[k] = rankNodeLabels[i];
				rankNodeLabels[i] = temp;
			}
		}

		// 对边同样进行排序
		//边的频度数组：freqEdgeLabel
		for (int i = 0; i < freqEdgeLabel.length - 1; i++) {
			int k = 0;
			label1 = rankEdgeLabels[i];
			temp = label1;
			for (int j = i + 1; j < freqEdgeLabel.length; j++) {
				label2 = rankEdgeLabels[j];

				if (freqEdgeLabel[temp] < freqEdgeLabel[label2]) {
					// 进行标号的互换
					temp = label2;
					k = j;
				}
			}

			if (temp != label1) {
				// 进行i，k排名下的标号对调
				temp = rankEdgeLabels[k];
				rankEdgeLabels[k] = rankEdgeLabels[i];
				rankEdgeLabels[i] = temp;
			}
		}

		// 将排名对标号转为标号对排名
		for (int i = 0; i < rankNodeLabels.length; i++) {
			nodeLabel2Rank[rankNodeLabels[i]] = i;
		}

		for (int i = 0; i < rankEdgeLabels.length; i++) {
			edgeLabel2Rank[rankEdgeLabels[i]] = i;
		}

		for (GraphData gd : totalGraphDatas) {
			gd.reLabelByRank( nodeLabel2Rank, edgeLabel2Rank );
		}

		// 根据排名找出小于支持度值的最大排名值
		for (int i = 0; i < rankNodeLabels.length; i++) {
			if (freqNodeLabel[rankNodeLabels[i]] > minSupportCount) {
				newNodeLabelNum = i;
			}
		}
		for (int i = 0; i < rankEdgeLabels.length; i++) {
			if (freqEdgeLabel[rankEdgeLabels[i]] > minSupportCount) {
				newEdgeLabelNum = i;
			}
		}
		//排名号比数量少1，所以要加回来
		newNodeLabelNum++;
		newEdgeLabelNum++;
	}

	/**
	 * 进行频繁子图的挖掘
	 */
	public void freqGraphMining() {
		long startTime =  System.currentTimeMillis();
		long endTime = 0;
		Graph g;
		
		//根据标号频繁度进行排序并且重新标号
		sortAndReLabel();

		resultGraphs = new ArrayList<>();
		totalGraphs = new ArrayList<>();
		// 通过图数据构造图结构
		// 得到初始图集
		for (GraphData gd : totalGraphDatas) {
			g = new Graph();
			g = g.constructGraph( gd );
			totalGraphs.add( g );
		}

		freqEdgeLabel = new int[newNodeLabelNum];
		freqNodeLabels = new int[newNodeLabelNum][newNodeLabelNum];
		// 根据新的点边的标号数初始化边频繁度对象
		//newNodeLabelNum：重新标号之后的点的标号数
		//newEdgeLabelNum:重新标号之后的边的标号数
		ef = new EdgeFrequency(newNodeLabelNum, newEdgeLabelNum);
		for (int i = 0; i < newNodeLabelNum; i++) {
			for (int j = 0; j < newEdgeLabelNum; j++) {
				for (int k = 0; k < newNodeLabelNum; k++) {
					for (Graph tempG : totalGraphs) {
						//	参数： 边的一端的节点标号， 边的标号，边的另外一端节点标号
						if (tempG.hasEdge(i, j, k)) {
							ef.edgeFreqCount[i][j][k]++;
							freqNodeLabel[i] += 1;
							freqNodeLabel[k] += 1;
							freqNodeLabels[i][k] += 1;
							freqNodeLabels[k][i] += 1;
						}
					}
				}
			}
		}
		calMeanWeight();

		Edge edge;
		GraphCode gc;
		int index = 0;
		resultGraphs.add( null );
		for (int i = 0; i < newNodeLabelNum; i++) {
			for (int j = 0; j < newEdgeLabelNum; j++) {
				for (int k = 0; k < newNodeLabelNum; k++) {
					//>最小支持度
					if (ef.edgeFreqCount[i][j][k] >= minSupportCount) {
						//图编码类
						gc = new GraphCode();
						
						//gSpan算法对图的边进行编码，采用E(v0,v1,A,B,a)的方式
						// （边的一端的id号标识，边的另一端的id号标识，边的一端的点标号，边的标号，边的另一端的点标号）
						//dfs编码的方式就是比里面的五元组的元素，我这里采用的规则是，从左往右依次比较大小，如果谁先小于另一方，谁就算小
						edge = new Edge(0, 1, i, j, k);
						
						//边的集合，边的排序代表着边的添加次序
						gc.getEdgeSeq().add(edge);

						// 将含有此边的图id加入到gc中
						for (int y = 0; y < totalGraphs.size(); y++) {
							//判断图中是否存在某条边
							if (totalGraphs.get(y).hasEdge(i, j, k)) {
								//拥有这些边的图的id
								//y图的id
								gc.getGs().add(y);
							}
						}
						// 对某条满足阈值的边进行挖掘
						subMining(gc, 2, index );
						if( resultGraphs.get(index) != null ) {
                            index += 1;
                            resultGraphs.add( null );
                        }
					}
				}
			}
		}
		if( resultGraphs.get(index) == null ) {
		    resultGraphs.remove( index );
        }
		
		endTime = System.currentTimeMillis();
		System.out.println("算法执行时间"+ (endTime-startTime) + "ms");
		printResultGraphInfo();
	}

    private void calMeanWeight() {
	    double maxEdgeWeight = -1;
	    double minEdgeWeight = Double.MAX_VALUE;
        for( int i = 0; i < newNodeLabelNum; ++i ) {
            for( int j = 0; j < newNodeLabelNum; ++j ) {
                double weight = calEdgeWeight( i, j );
                maxEdgeWeight = Math.max( maxEdgeWeight, weight );
                minEdgeWeight = Math.min( minEdgeWeight, weight );
            }
        }
        meanWeight = ( maxEdgeWeight + minEdgeWeight ) / 2;
    }

    private double calEdgeWeight(int i, int j) {
	    double dividend = freqNodeLabels[i][j] - freqNodeLabel[i] * freqNodeLabel[j];
	    double divisor = Math.sqrt( ( freqNodeLabels[i][j] ) * ( 1 - freqNodeLabel[i] * ( 1 - freqNodeLabel[j] ) ) );
	    return dividend / divisor;
    }

    /**
	 * 进行频繁子图的挖掘
	 * 
	 * @param gc
	 *            图编码
	 * @param next
	 *            图所含的点的个数
	 */
	private void subMining(GraphCode gc, int next, int index ) {
		Edge e;
		Graph graph = new Graph();
		int id1;
		int id2;

		for(int i=0; i<next; i++){
			graph.nodeLabels.add(-1);
			graph.edgeLabels.add(new ArrayList<Integer>());
			graph.edgeNexts.add(new ArrayList<Integer>());
		}

		// 首先根据图编码中的边五元组构造图
		for (int i = 0; i < gc.getEdgeSeq().size(); i++) {
			e = gc.getEdgeSeq().get(i);
			id1 = e.ix;
			id2 = e.iy;

			graph.nodeLabels.set(id1, e.x);
			graph.nodeLabels.set(id2, e.y);
			graph.edgeLabels.get(id1).add(e.a);
			graph.edgeLabels.get(id2).add(e.a);
			graph.edgeNexts.get(id1).add(id2);
			graph.edgeNexts.get(id2).add(id1);
		}

		DFSCodeTraveler dTraveler = new DFSCodeTraveler(gc.getEdgeSeq(), graph);
		dTraveler.traveler();
		if (!dTraveler.isMin) {
			return;
		}

		// 如果当前是最小编码则将此图加入到结果集中
		if( judgeIsMoreMeanWeight( graph ) ) {
            resultGraphs.set( index, graph );
        }

		
		Edge e1;
		ArrayList<Integer> gIds;
		SubChildTraveler sct;
		ArrayList<Edge> edgeArray;
		
		// 添加潜在的孩子边，每条孩子边所属的图id
		HashMap<Edge, ArrayList<Integer>> edge2GId = new HashMap<>();
		for (int i = 0; i < gc.gs.size(); i++) {
			int id = gc.gs.get(i);

			// 在此结构的条件下，在多加一条边构成子图继续挖掘
			sct = new SubChildTraveler(gc.edgeSeq, totalGraphs.get(id));
			sct.traveler();
			edgeArray = sct.getResultChildEdge();

			// 做边id的更新
			for (Edge e2 : edgeArray) {
				if (!edge2GId.containsKey(e2)) {
					gIds = new ArrayList<>();
				} else {
					gIds = edge2GId.get(e2);
				}

				gIds.add(id);
				edge2GId.put(e2, gIds);
			}
		}

		for (Map.Entry entry : edge2GId.entrySet()) {
			e1 = (Edge) entry.getKey();
			gIds = (ArrayList<Integer>) entry.getValue();

			// 如果此边的频度大于最小支持度值，则继续挖掘
			if (gIds.size() < minSupportCount) {
				continue;
			}

			GraphCode nGc = new GraphCode();
			nGc.edgeSeq.addAll(gc.edgeSeq);
			// 在当前图中新加入一条边，构成新的子图进行挖掘
			nGc.edgeSeq.add(e1);
			nGc.gs.addAll(gIds);

			if (e1.iy == next) {
				// 如果边的点id设置是为当前最大值的时候，则开始寻找下一个点
				subMining(nGc, next + 1, index);
			} else {
				// 如果此点已经存在，则next值不变
				subMining(nGc, next, index);
			}
		}
	}

    private boolean judgeIsMoreMeanWeight(Graph graph) {
	    if( calWeight( graph ) * graph.getSup(totalGraphs) >= meanWeight * minSupportRate ) {
	        return true;
        } else {
	        return false;
        }
    }

    private double calWeight(Graph graph) {
	    graph.initIsVis();
        int edgeNum = 0;
        double sum = 0;
        for( int i = 0; i < graph.edgeNexts.size(); ++i ) {
            for( int j = 0; j < graph.edgeNexts.get(i).size(); ++j ) {
                int u = i;
                int v = graph.edgeNexts.get(i).get(j);
                // 获取到的是id
                if( u > v ) {
                    int tmp = u;
                    u = v;
                    v = tmp;
                }
                int uLabel = graph.nodeLabels.get(u);
                int vLabel = graph.nodeLabels.get(v);
                // 转化成标号
                if( !graph.isVis[u][v] ) {
                    graph.isVis[u][v] = true;
                    sum += calEdgeWeight( uLabel, vLabel );
                    edgeNum += 1;
                }
            }
        }
        return sum / edgeNum;
    }

    /**
	 * 输出频繁子图结果信息
	 */
	 

	private void printResultGraphInfo(){
	    int size = resultGraphs.size();
		System.out.println(MessageFormat.format("挖掘出的频繁子图的个数为：{0}个", size));
		for( int i = 0; i < size; ++i ) {
            System.out.println( resultGraphs.get(i).nodeLabels );
        }
	}
	
	/*public void printResultGraphInfo(){
		//System.out.println(MessageFormat.format("构造的用户关系图有：{0}个", totalGraphs.size()));
		System.out.println(MessageFormat.format("挖掘出频繁子图的个数为：{0}个", resultGraphs.size()));
		//System.out.println("频繁子图列表如下:");
		for(int i=0;i<resultGraphs.size();i++){
			System.out.println(resultGraphs.get(i).getNodeLabels());
		}
		System.out.println("挖掘出网络水军团体如下:");
		ArrayList<Integer> result = null;
		for(int i=1;i<resultGraphs.size()-1;i=i+2){
			//System.out.println(resultGraphs.get(i).getNodeLabels()+resultGraphs.get(i).getEdgeLabels().toString()+resultGraphs.get(i).getEdgeNexts().toString());
//			if(i==0){
//				result=new ArrayList<Integer>();
//			}else{
			//result=new ArrayList<Integer>();
				//if(Collections.disjoint(resultGraphs.get(i-1).getNodeLabels(),resultGraphs.get(i-1).getNodeLabels())){
					result=new ArrayList<Integer>();
					for(int j=0;j<resultGraphs.get(i-1).getNodeLabels().size();j++){
						if(!result.contains(resultGraphs.get(i-1).getNodeLabels().get(j))){
						result.add(resultGraphs.get(i-1).getNodeLabels().get(j));
						}
					}
					for(int j=0;j<resultGraphs.get(i).getNodeLabels().size();j++){
						if(!result.contains(resultGraphs.get(i).getNodeLabels().get(j))){
							result.add(resultGraphs.get(i).getNodeLabels().get(j));
						}
						
					//}
					System.out.println(result.toString());
					
			
			}
//			if(Collections.disjoint(resultGraphs.get(i).getNodeLabels(),resultGraphs.get(i-1).getNodeLabels())){
//				resultGraphs.get(i+1).getNodeLabels().removeAll(resultGraphs.get(i).getNodeLabels());
//				resultGraphs.get(i).getNodeLabels().addAll(resultGraphs.get(i+1).getNodeLabels());
//				System.out.println(resultGraphs.get(i).getNodeLabels());
//			}else{
//				System.out.println(resultGraphs.get(i).getNodeLabels());
//			}
//			System.out.println(result);
		}
	}*/

}
