package DataMining;

/**
 * gSpan频繁子图挖掘算法
 * @author cuilijuan
 *
 */
public class Client {
	public static void main(String[] args){
		//测试数据文件
		//t # 0 ：t表示图；#固定；0表示图几
		//v 1 0：v表示节点；  1表示节点的id号标识；0表示点标号
		//e 4 5 3：e表示边；4和5分别是边的id号标识；3表示边的标号
//		String filePath = "D:\\reallyData.txt";
		String filePath = "E:\\projectTeam\\挖掘\\DataMining_GSpan\\src\\DataMining\\input.txt";
		//最小支持度率
		double minSupportRate = 0.3;
		//GSpanTool tool = new GSpanTool();
		GSpanTool tool = new GSpanTool(filePath, minSupportRate);
		tool.freqGraphMining();
	}
}
