import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

//import java.util.Set;


import java.util.Random;



//import java.util.Set;







//import iisc.dsl.picasso.client.panel.ReducedPlanPanel.ReducePlan.Set;
//import iisc.dsl.picasso.client.util.PicassoUtil;
//import iisc.dsl.picasso.client.panel.ReducedPlanPanel.ReducePlan.Set;
import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.common.ds.DiagramPacket;
import iisc.dsl.picasso.server.ADiagramPacket;


public class CostGreedy2D
{
	static double AllPlanCosts[][];
	static int nPlans;
	static int totalPlans;
	static int totalPoints;
	static int dimension;
	static int resolution;
	DataValues[] data;
	DataValues[] newData;
	
	
	static int total_points;
	static int plans[];
	static double [] OptimalCost;
	
	static int replacement[];

	static double x_a;
	static double y_a;
	static double z_a;
	static double error;
	static boolean done = false;
	
//	static String QT_NAME = "PG_QT5_100_EXP";
//	static String bouquetPath = "/home/dsladmin/Documents/Project/post_lab_pres_2/packets/PG/QT5/PG_QT5_100_EXP/";
	

	double selectivity[];
	static double no_of_optimizations = 0;
	static double costed_points[] = new double [10000];
	
	static int UNI=1;
	static int EXP=2;
	
//	public static void main(String args[])
//	{
//		
//		
//		
//		CostGreedy obj = new CostGreedy();
//		obj.readpkt();
//		
//		//For cost greedy

//		
//		
//		System.out.println("\nMain Finished\n");
//	}

	public ADiagramPacket run(double threshold, ADiagramPacket gdp, String apktPath) throws IOException, SQLException{
		ADiagramPacket reducedgdp = cgFpc(threshold, gdp,apktPath);
		readpkt(reducedgdp);
		loadSelectivity(3);
		double h_cost = getOptimalCost(totalPoints-1);
		double cost = getOptimalCost(0);
		double ratio = h_cost/cost;
	//	System.out.println("-------------------------  ------\n"+qtName+"    alpha="+alpha+"\n-------------------------  ------"+"\n"+"Highest Cost ="+h_cost+", \nRatio of highest cost to lowest cost ="+ratio);
		System.out.println("the ratio of C_max/c_min is "+ratio);
		GCI2D obj = new GCI2D();
		obj.readpkt(gdp);
		//setting the actual values of x_a, y_a and z_a
		x_a = obj.x_a;
		y_a = obj.y_a;
		error = obj.error;
		System.out.println("The original (x_a,y_a)= ("+x_a+","+y_a+")");
		
		x_a = findNearestSelectivity(x_a);
		y_a = findNearestSelectivity(y_a);
		System.out.println("After nearest selectivity (x_a,y_a,z_a)= ("+x_a+","+y_a+","+z_a+")");
	//	System.out.println("After nearest selectivity again (x_a,y_a)= ("+obj.findNearestSelectivity(x_a)+","+obj.findNearestSelectivity(y_a)+")");
		
		double MSO =0, ASO = 0,SO=0;
		int max_point = 1;
		if(obj.MSOCalculation)
			max_point = totalPoints;

		double[] subOpt = new double[max_point];

	  for (int  j = 0; j < max_point ; j++)
	  {
		
		//initialization for every loop
		cost = getOptimalCost(0);
		initialize(j);
		x_a=0.99;y_a=0.99;
		int i = 1;
		double algo_cost =0;
		SO =0;

		//cost*=2;
		done = false;
		while(cost < h_cost && !done)
		{
			System.out.println("---------------------------------------------------------------------------------------------\n");
			System.out.println("Contour "+i+" cost : "+cost+"\n");
			
			//int num = run_new_seed_2d_algorithm(cost,error*cost);
			int num = genBruteForce(cost, i);
			algo_cost += num*cost;
			cost = cost*2;
			i = i+1;
			System.out.println("---------------------------------------------------------------------------------------------\n");
		}

		if(!done){
			algo_cost += h_cost;
			SO = (algo_cost/cost(findNearestPoint(x_a),findNearestPoint(y_a)));
		}
		else
			SO = (algo_cost/cost(findNearestPoint(x_a),findNearestPoint(y_a)));
			
		SO = SO *(1+threshold/100);
		System.out.print("Cost of actual_sel ="+cost(findNearestPoint(x_a),findNearestPoint(y_a))+"\t");
		System.out.println("at ("+findNearestPoint(x_a)+","+findNearestPoint(y_a)+")"+"=== ("+findNearestSelectivity(x_a)+","+findNearestSelectivity(y_a)+")");
		subOpt[j] = SO;
		ASO += SO;
		if(SO>MSO)
			MSO = SO;
		System.out.println(" Plan Bouquet The SubOptimaility  is "+SO);
	  }
		System.out.println("Plan Bouquet The MaxSubOptimaility  is "+MSO);
		System.out.println("Plan Bouquet  The AverageSubOptimaility  is "+(double)ASO/max_point);

		//writeSuboptToFile(subOpt,apktPath);
		
		System.out.println("\nMainFinished\n");

		return reducedgdp;
	}
		
	
	 public void writeSuboptToFile(double[] subOpt,String path) throws IOException {
		// TODO Auto-generated method stub
         File file = new File(path+"PB_"+"suboptimality"+".txt");
 	    if (!file.exists()) {
	        file.createNewFile();
	    }
	    FileWriter writer = new FileWriter(file, false);
	    PrintWriter pw = new PrintWriter(writer);
	    
	    
		for(int i =0;i<resolution;i++){
			for(int j=0;j<resolution;j++){
				//if(i%2 ==0 && j%2 ==0){
					int [] index = new int[2];
					index[0] = i;
					index[1] = j;
					int ind = getIndex(index, resolution);
					if(j!=0)
						pw.print("\t"+subOpt[ind]);
					else
						pw.print(subOpt[ind]);
				//}
			}
			//if(i%2==0)
			pw.print("\n");
		}
		pw.close();
		writer.close();
		
	}


	private void initialize(int location) {
		// TODO Auto-generated method stub
			int index[] = getCoordinates(dimension, resolution, location);
			x_a = selectivity[index[0]];
			y_a = selectivity[index[1]];

			done =false;
	}


	double cost(int x, int y)
	{
		int [] arr = new int [resolution];
		arr[0] = x;
		arr[1] = y;
		int index = getIndex(arr,resolution);

		
		return OptimalCost[index];
	}
	


	double getOptimalCost(int index)
	{
		return OptimalCost[index];
	}
	
	int getPlanNumber(int x, int y)
	{
		 int arr[] = {x,y};
		int index = getIndex(arr,resolution);
		return plans[index];
	}
	

	
	void loadSelectivity(int option)
	{
		String funName = "loadSelectivity: ";
		System.out.println(funName+" Resolution = "+resolution);
		double sel;
		this.selectivity = new double [resolution];
		
		if(resolution == 10){
		selectivity[0] = 0.00005;	selectivity[1] = 0.0005;selectivity[2] = 0.005;	selectivity[3] = 0.02;
		selectivity[4] = 0.05;		selectivity[5] = 0.10;	selectivity[6] = 0.15;	selectivity[7] = 0.25;
		selectivity[8] = 0.50;		selectivity[9] = 0.99;                                 // oct - 2012
		}


		if(resolution == 20){
		selectivity[0] = 0.000005;		selectivity[1] = 0.00005;		selectivity[2] = 0.0005;	selectivity[3] = 0.002;
		selectivity[4] = 0.005;		selectivity[5] = 0.008;		selectivity[6] = 0.01;		selectivity[7] = 0.02;
		selectivity[8] = 0.03;			selectivity[9] = 0.04;			selectivity[10] = 0.05;	selectivity[11] = 0.08;
		selectivity[12] = 0.10; 		selectivity[13] = 0.15;		selectivity[14] = 0.20;	selectivity[15] = 0.30;
		selectivity[16] = 0.40;		selectivity[17] = 0.60;		selectivity[18]=0.80;		selectivity[19] = 0.99;
		}

		if(resolution == 30){
		selectivity[0] = 0.00002;  selectivity[1] = 0.00009;	selectivity[2] = 0.0002;	selectivity[3] = 0.0005;
		selectivity[4] = 0.0007;   selectivity[5] = 0.0010;	selectivity[6] = 0.0014;	selectivity[7] = 0.0019;
		selectivity[8] = 0.0026;	selectivity[9] = 0.0036;	selectivity[10] = 0.0048;	selectivity[11] = 0.0065;
		selectivity[12] = 0.0087;	selectivity[13] = 0.0117;	selectivity[14] = 0.0156;	selectivity[15] = 0.0208;
		selectivity[16] = 0.0278;	selectivity[17] = 0.0370;	selectivity[18] = 0.0493;	selectivity[19] = 0.0657;
		selectivity[20] = 0.0874;	selectivity[21] = 0.1164;	selectivity[22] = 0.1549;	selectivity[23] = 0.2061;
		selectivity[24] = 0.2741;	selectivity[25] = 0.3647;	selectivity[26] = 0.48515;	selectivity[27] = 0.6453;
		selectivity[28] = 0.8583;	selectivity[29] = 0.9950;		
		}
		
		if(resolution==100){
			selectivity[0] = 0.005995; 	selectivity[1] = 0.015985; 	selectivity[2] = 0.025975; 	selectivity[3] = 0.035965; 	selectivity[4] = 0.045955; 	
			selectivity[5] = 0.055945; 	selectivity[6] = 0.065935; 	selectivity[7] = 0.075925; 	selectivity[8] = 0.085915; 	selectivity[9] = 0.095905; 	
			selectivity[10] = 0.105895; 	selectivity[11] = 0.115885; 	selectivity[12] = 0.125875; 	selectivity[13] = 0.135865; 	selectivity[14] = 0.145855; 	
			selectivity[15] = 0.155845; 	selectivity[16] = 0.165835; 	selectivity[17] = 0.175825; 	selectivity[18] = 0.185815; 	selectivity[19] = 0.195805; 	
			selectivity[20] = 0.205795; 	selectivity[21] = 0.215785; 	selectivity[22] = 0.225775; 	selectivity[23] = 0.235765; 	selectivity[24] = 0.245755; 	
			selectivity[25] = 0.255745; 	selectivity[26] = 0.265735; 	selectivity[27] = 0.275725; 	selectivity[28] = 0.285715; 	selectivity[29] = 0.295705; 	
			selectivity[30] = 0.305695; 	selectivity[31] = 0.315685; 	selectivity[32] = 0.325675; 	selectivity[33] = 0.335665; 	selectivity[34] = 0.345655; 	
			selectivity[35] = 0.355645; 	selectivity[36] = 0.365635; 	selectivity[37] = 0.375625; 	selectivity[38] = 0.385615; 	selectivity[39] = 0.395605; 	
			selectivity[40] = 0.405595; 	selectivity[41] = 0.415585; 	selectivity[42] = 0.425575; 	selectivity[43] = 0.435565; 	selectivity[44] = 0.445555; 	
			selectivity[45] = 0.455545; 	selectivity[46] = 0.465535; 	selectivity[47] = 0.475525; 	selectivity[48] = 0.485515; 	selectivity[49] = 0.495505; 	
			selectivity[50] = 0.505495; 	selectivity[51] = 0.515485; 	selectivity[52] = 0.525475; 	selectivity[53] = 0.535465; 	selectivity[54] = 0.545455; 	
			selectivity[55] = 0.555445; 	selectivity[56] = 0.565435; 	selectivity[57] = 0.575425; 	selectivity[58] = 0.585415; 	selectivity[59] = 0.595405; 	
			selectivity[60] = 0.605395; 	selectivity[61] = 0.615385; 	selectivity[62] = 0.625375; 	selectivity[63] = 0.635365; 	selectivity[64] = 0.645355; 	
			selectivity[65] = 0.655345; 	selectivity[66] = 0.665335; 	selectivity[67] = 0.675325; 	selectivity[68] = 0.685315; 	selectivity[69] = 0.695305; 	
			selectivity[70] = 0.705295; 	selectivity[71] = 0.715285; 	selectivity[72] = 0.725275; 	selectivity[73] = 0.735265; 	selectivity[74] = 0.745255; 	
			selectivity[75] = 0.755245; 	selectivity[76] = 0.765235; 	selectivity[77] = 0.775225; 	selectivity[78] = 0.785215; 	selectivity[79] = 0.795205; 	
			selectivity[80] = 0.805195; 	selectivity[81] = 0.815185; 	selectivity[82] = 0.825175; 	selectivity[83] = 0.835165; 	selectivity[84] = 0.845155; 	
			selectivity[85] = 0.855145; 	selectivity[86] = 0.865135; 	selectivity[87] = 0.875125; 	selectivity[88] = 0.885115; 	selectivity[89] = 0.895105; 	
			selectivity[90] = 0.905095; 	selectivity[91] = 0.915085; 	selectivity[92] = 0.925075; 	selectivity[93] = 0.935065; 	selectivity[94] = 0.945055; 	
			selectivity[95] = 0.955045; 	selectivity[96] = 0.965035; 	selectivity[97] = 0.975025; 	selectivity[98] = 0.985015; 	selectivity[99] = 0.995005;
		}
		//the selectivity distribution
		//System.out.println("The selectivity distribution using is ");
//		for(int i=0;i<resolution;i++)
//		System.out.println("\t"+selectivity[i]);
	}

	// Return the index near to the selecitivity=mid;
		int findNearestPoint(double mid)
		{
			String funName = "findNearestPoint";
			int i;
			double min_diff = Double.MAX_VALUE;
			int return_index = resolution - 1;
			double diff;
			double return_value;
			for(i=0;i<resolution;i++)
			{	
				diff = mid - selectivity[i];
				if(diff <= 0)
				{
					return_index = i;
					break;
				}
				
				/*diff = Math.abs(selectivity[i]-mid);
				if(diff < min_diff)
				{
					min_diff = diff;
					return_index = i;
				}*/
			}
			if(return_index!=0)
				assert (selectivity[return_index-1]<=mid && mid<=selectivity[return_index]): funName+" ERROR";
			//System.out.println("return_index="+return_index);
			return return_index;
		}
		// Return the index near to the selecitivity=mid;
			public double findNearestSelectivity(double mid)
			{
				String funName = "findNearestSelectivity";
				int i;
				double min_diff = Double.MAX_VALUE;
				int return_index = resolution - 1;
				double diff;
				double return_value;
				for(i=0;i<resolution;i++)
				{	
					diff = mid - selectivity[i];
					if(diff <= 0)
					{
						return_index = i;
						break;
					}
					
					
				}
				if(return_index!=0)
				assert (selectivity[return_index-1]<=mid && mid<=selectivity[return_index]): funName+" ERROR";
				//System.out.println("return_index="+return_index);
				return selectivity[return_index];
			}
			
			
			

	public ADiagramPacket cgFpc(double threshold, ADiagramPacket gdp, String apktPath) {
		String funName = "cgFpc";
		// First call the readApkt() function
		readpktcg(gdp, apktPath);
		System.out.println("CostGreedy:");
		ADiagramPacket ngdp = new ADiagramPacket(gdp);
		
		int n = nPlans;
		// System.out.println(n);
		Set[] s = new Set[n];
		boolean[] notSwallowed = new boolean[n];
		//DataValues[] data = data;
		DataValues[] newData = new DataValues[data.length];
		//float[] sel = gdp.getPicassoSelectivity();

		//getPlanStrings(data,n);
		

		
		for(int i = 0;i < data.length;i ++) {
			Integer xI = new Integer(i);
			int p = data[i].getPlanNumber();
			if (s[p] == null) {
				s[p] = new Set();
			}
			s[p].elements.add(xI);
			if (notSwallowed[p]) {
				continue;
			}
			
			double cost = AllPlanCosts[p][i];
			double lt = cost * (1 + threshold / 100);
			boolean flag = false;
			for(int j = 0;j < n;j ++) {
				if(p != j) {
					double cst = AllPlanCosts[j][i];//getCost(j,i);
					if(cst <= lt) {
						if(s[j] == null) {
							s[j] = new Set();
						}
						s[j].elements.add(xI);
						flag = true;
					}
				}
			}
			if (!flag) {
				notSwallowed[p] = true;
			}
//			if(i%100==0)
//				System.out.println("In First loop: "+i);
			
		}
		
		ArrayList soln = new ArrayList();
		Set temp = new Set();
		for (int i = 0; i < n; i++) {
			if (notSwallowed[i] && s[i] != null) {
				temp.elements.addAll(s[i].elements);
				s[i].elements.clear();
				s[i] = null;
				soln.add(new Integer(i));
			}
		}
		int cct = 0;
		for (int i = 0; i < n; i++) {
			if (s[i] != null) {
				s[i].elements.removeAll(temp.elements);
				if(s[i].elements.size() != 0) {
					cct ++;
				}
			}
		}

		while (true) {
			int max = 0;
			int p = -1;
			for (int i = 0; i < n; i++) {
				if (s[i] != null) {
					int size = s[i].elements.size();
					if (size > max) {
						max = size;
						p = i;
					}
				}
			}
			if (p == -1) {
				break;
			}
			soln.add(new Integer(p));
			for (int i = 0; i < n; i++) {
				if (s[i] != null && i != p) {
					s[i].elements.removeAll(s[p].elements);
					if (s[i].elements.size() == 0) {
						s[i] = null;
					}
				}
			}
			s[p] = null;
		}
		for (int i = 0; i < data.length; i++) {

			int p = data[i].getPlanNumber();
			Integer pI = new Integer(p);
			newData[i] = new DataValues();
			newData[i].setCard(data[i].getCard());
			
			double cost = AllPlanCosts[p][i];
			double lt = cost * (1 + threshold / 100);
			
			if(soln.contains(pI)) {
				newData[i].setPlanNumber(p);
				newData[i].setCost(data[i].getCost());
			} else {
				int plan = -1;
				double newcost = Double.MAX_VALUE; 
				for (int xx = 0; xx < n; xx++) {
					double cst = AllPlanCosts[xx][i];//getCost(xx,i);
					if (soln.contains(new Integer(xx)) && xx != p && cst <= lt) {
						// another redundant check for xx != p
						if(cst <= newcost) {
							newcost = cst;
							plan = xx;
						}
					}
				}
				newData[i].setPlanNumber(plan);
				newData[i].setCost(newcost);
				if(newData[i].getCost() < data[i].getCost()){
					newData[i].setCost(data[i].getCost());
					newData[i].setPlanNumber(data[i].getPlanNumber());
				}
					
			}
//			if(i%100==0)
//				System.out.println("In Second loop: "+i);
		}
		ngdp.setDataPoints(newData);
		//setInfoValues(data, newData);
		
		
		// to test the data in newData
		
		for(int i=0;i<data.length;i++){
			if(newData[i].getCost() > (1+threshold/100)*data[i].getCost())
				System.out.println(funName+"threshold");
		}
		if(threshold==(double)0){
			double count=0;
			for(int i=0;i<data.length;i++){
				if(data[i].getCost()!=newData[i].getCost())
					count++;
			}
			System.out.println("the count of bad point is "+count);
		}
		//-------------------------------------------
		return ngdp;
	}

	
	

void readpktcg(ADiagramPacket gdp, String apktPath)
{
	//ADiagramPacket gdp = getGDP(new File(bouquetPath + QT_NAME+ ".apkt"));
	totalPlans = gdp.getMaxPlanNumber();
	dimension = gdp.getDimension();
	resolution = gdp.getMaxResolution();
	data = gdp.getData();
	total_points = (int) Math.pow(resolution, dimension);
	System.out.println("\nthe total plans are "+totalPlans+" with dimensions "+dimension+" and resolution "+resolution);
	
	assert (total_points==data.length) : "Data length and the resolution didn't match !";
	
	OptimalCost = new double [data.length]; 
	plans = new int [data.length];
	for (int i = 0;i < data.length;i++)
	{
		OptimalCost[i]= data[i].getCost();
		this.plans[i] = data[i].getPlanNumber();
//		System.out.println("\nOptimal_cost:["+i+"] ="+OptimalCost[i]+", Plan["+i+"]="+plans[i]+"\n");
	}
	// ------------------------------------- Read pcst files
	nPlans = totalPlans;
	AllPlanCosts = new double[nPlans][total_points];
	//costBouquet = new double[total_points];
	for (int i = 0; i < nPlans; i++) {
		try {
			System.out.println("The pcst files located at "+apktPath);
			ObjectInputStream ip = new ObjectInputStream(new FileInputStream(new File(apktPath + i + ".pcst")));
			double[] cost = (double[]) ip.readObject();
			for (int j = 0; j < total_points; j++)
			{
			
				AllPlanCosts[i][j] = cost[j];
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
//-------------------------------------------------- Reading pcst done !!
}


void readpkt(ADiagramPacket gdp) throws IOException
{
	//ADiagramPacket gdp = getGDP(new File(pktPath));
	totalPlans = gdp.getMaxPlanNumber();
	dimension = gdp.getDimension();
	resolution = gdp.getMaxResolution();
	data = gdp.getData();
	totalPoints = (int) Math.pow(resolution, dimension);
	ArrayList<Integer> distinctPlans = new ArrayList<Integer>();
	
	assert (totalPoints==data.length) : "Data length and the resolution didn't match !";
	
	plans = new int [data.length];
	OptimalCost = new double [data.length]; 
	for (int i = 0;i < data.length;i++)
	{
		OptimalCost[i]= data[i].getCost();
		plans[i] = data[i].getPlanNumber();
		if(!distinctPlans.contains(plans[i]))
			distinctPlans.add(plans[i]);
		//System.out.println("Plan Number ="+plans[i]+"\n");
	//	System.out.println("\nOptimal_cost:["+i+"] ="+OptimalCost[i]+"\n");
	}
	//
	System.out.println("\nthe total plans after reduction are "+distinctPlans.size()+" with dimensions "+dimension+" and resolution "+resolution);
			
}

void readpktnew()
{
	/*ADiagramPacket gdp = getGDP(new File(bouquetPath + QT_NAME+ ".apkt"));
	totalPlans = gdp.getMaxPlanNumber();
	dimension = gdp.getDimension();
	resolution = gdp.getMaxResolution();*/
//	data = gdp.getData();
	total_points = (int) Math.pow(resolution, dimension);
	System.out.println("\nthe total plans are "+totalPlans+" with dimensions "+dimension+" and resolution "+resolution);
	
	assert (total_points==newData.length) : "Data length and the resolution didn't match !";
	
//	OptimalCost = new double [data.length]; 
//	plans = new int [data.length];
	System.out.println("\nnewData="+newData+"\n");
	for (int i = 0;i < newData.length;i++)
	{
		this.OptimalCost[i]= newData[i].getCost();
		this.plans[i] = newData[i].getPlanNumber();
//		System.out.println("\nOptimal_cost:["+i+"] ="+OptimalCost[i]+", Plan["+i+"]="+plans[i]+"\n");
	}
}

private ADiagramPacket getGDP(File file) {
	ADiagramPacket gdp = null;
	FileInputStream fis;
	try {
		fis = new FileInputStream(file);
		ObjectInputStream ois = new ObjectInputStream(fis);
		Object obj = ois.readObject();
		gdp = (ADiagramPacket) obj;
		
		ois.close();
	} catch (FileNotFoundException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	} catch (ClassNotFoundException e) {
		e.printStackTrace();
	}
	return gdp;
}
public int[] getCoordinates(int dimensions, int res, int location){
	int [] index = new int[dimensions];

	for(int i=0; i<dimensions; i++){
		index[i] = location % res;

		location /= res;
	}
	return index;
}
public int getIndex(int[] index,int res)
{
	int tmp=0;

	for(int i=index.length-1; i>=0; i--){
		if(index[i] > 0)
			tmp=tmp * res + index[i];
		else
			tmp=tmp * res;
	}

	return tmp;
}
int run_new_seed_2d_algorithm(double targetval, double errorboundval) throws IOException, SQLException
{
	/* Start from Top-Left Corner */
	 
	 double max_cost=0, min_cost=cost(resolution-1,resolution-1)+1;
	int cur_x1 = 0, cur_y1 = resolution - 1;
	double cur_val1;
	ArrayList<point> original = new ArrayList<point>();
	HashSet<Integer> unique_plans = new HashSet();
	double X_max=0,Y_max=0;
	double max_x_pt[];
	max_x_pt = new double[2];
	max_x_pt[0] = 0;
	max_x_pt[1] = 0;
	
	double max_y_pt[];
	max_y_pt = new double[2];
	max_y_pt[0] = 0;
	max_y_pt[1] = 0;
	
	int max_x_plan=-1, max_y_plan=-1;
	
	//to document which all indices of x and y axes are picked by the contour identification
	boolean choosenXplans [] = new boolean [resolution];
	boolean choosenYplans [] = new boolean [resolution];
	for(int r =0; r<resolution;r++)
		choosenXplans[r] = choosenYplans[r] = false;
	
	/* if top left cost is more than target do binary search on left edge of 2d not on top edge*/
	cur_val1 = cost(0, resolution-1);
	if(cur_val1 > targetval)
	{
		int low = 0, high = resolution - 1, mid = resolution - 1;
		/* do a binary search on top row to find out starting point */
		while(low < high)
		{
			mid = (low + high) / 2;
			cur_val1 = cost(cur_x1, mid);
			if(cur_val1 >= targetval && cur_val1 <= targetval)
			{
				cur_y1 = mid;
				break;
			}
			else if(cur_val1 < targetval)
				low = mid + 1;
			else
				high = mid - 1;
		}
		/* If could not find any point within targetval+-errorboundval by binary search
		 * then take last used mid as starting point
		 */
		if(!(low < high))
		{
			if(mid < resolution - 1)
				cur_y1 = mid + 1;
		}
	}
	else
	{
		int low = 0, high = resolution - 1, mid = 0;
		/* do a binary search on top row to find out starting point */
		while(low < high)
		{
			mid = (low+high) / 2;
			cur_val1 = cost(mid, cur_y1);
			if(cur_val1 >= targetval && cur_val1 <= targetval)
			{
				cur_x1 = mid;
				break;
			}
			else if(cur_val1 < targetval)
				low = mid + 1;
			else
				high = mid - 1;
		}
		/* If could not find any point within targetval+-errorboundval by binary search
		 * then take last used mid as starting point
		 */
		if(!(low < high))
		{
			/* taking mid may start from a point whose val > target val. so start one point 
			 * less than last mid used
			 */
			if(mid > 0)
				cur_x1 = mid-1;
		}
	}
	
	
	
	int cur_x2 = resolution - 1, cur_y2 = 0;
	double cur_val2;

	/* if bottom right cost is more than target do binary search on left edge of 2d not on top edge*/
	cur_val2 = cost(cur_x2, cur_y2);
	if(cur_val2 > targetval)
	{
		int low = 0, high = resolution - 1, mid = resolution - 1;
		
		/* do a binary search on bottom row to find out starting point */
		while(low < high)
		{
			mid = (low + high) / 2;
			cur_val2 = cost(mid, cur_y2);
			if(cur_val2 >= targetval && cur_val2 <= targetval)
			{
				cur_x2 = mid;
				break;
			}
			else if(cur_val2 < targetval)
				low = mid + 1;
			else
				high = mid - 1;
		}
		/* If could not find any point within targetval+-errorboundval by binary search
		 * then take last used mid as starting point
		 */
		if(!(low < high))
		{
			if(mid < resolution - 1)
				cur_x2 = mid + 1;
		}
	}
	else
	{
		int low = 0, high = resolution - 1, mid = 0;
		/* do a binary search on top row to find out starting point */
		while(low < high)
		{
			mid = (low+high) / 2;
			cur_val2 = cost(cur_x2, mid);
			if(cur_val2 >= targetval && cur_val2 <= targetval)
			{
				cur_y2 = mid;
				break;
			}
			else if(cur_val2 < targetval)
				low = mid + 1;
			else
				high = mid - 1;
		}
		/* If could not find any point within targetval+-errorboundval by binary search
		 * then take last used mid as starting point
		 */
		if(!(low < high))
		{
			/* taking mid may start from a point whose val > target val. so start one point 
			 * less than last mid used
			 */
			if(mid > 0)
				cur_y2 = mid-1;
		}
	}
	
	//cur_x1 and cur_y1 denotes the starting seed point which is either on x=0  or y=resolution-1 line 
	
	/* do until you cross boundary starting from start point*/
	boolean ContourStarted = true;
	int unique_points =0;
	while(cur_x1 < resolution && cur_y1 >= 0)
	{
//		System.out.println("x = " + cur_x + ", y = " + cur_y);

		cur_val1 = cost(cur_x1, cur_y1);
		
		if(cur_val1 >= targetval - errorboundval && cur_val1 <= targetval + errorboundval)
		{
			/* when you hit a interesting point, just keep going on x axis as long as it is interesting
			 * then backtrack to exactly where you started getting interesting points */
			int t_y1 = cur_y1;		
			while(cur_val1 >= targetval - errorboundval && cur_val1 <= targetval + errorboundval)
			{
				cur_y1 = t_y1;
				double x = selectivity[cur_x1];
				double y = selectivity[cur_y1];
				//SpillBound Algorithm Starts
		//		if(inFeasibleRegion(x,y)){
				
						
				unique_points ++;
				 if(x>=x_a && y>=y_a ){
					 done = true;
					 unique_plans.add(getPlanNumber(cur_x1, cur_y1));
					 System.out.println("The number unique points are "+unique_points);
					 System.out.println("The number unique plans are "+unique_plans.size());
					 return unique_plans.size();
				 }
				 else{
					 if(!unique_plans.contains(getPlanNumber(cur_x1, cur_y1)))
						 unique_plans.add(getPlanNumber(cur_x1, cur_y1));
				 }

				//update choosen X and Y plans
				choosenXplans[cur_x1] = true; choosenYplans[cur_y1] = true;
		
		//		}
				if(t_y1 <= cur_y2)
					break;
				t_y1 = t_y1 - 1;
				cur_val1 = cost(cur_x1, t_y1);
			}
		    cur_x1++;
		}
		else if(cur_val1 > targetval -errorboundval && cur_y1 > cur_y2)
		{

			
			//by Srinivas changes to contour identification code
//----------------------------------------------------------------------------------------------------------------------
			//enter this code block if none of the no plan plan in either cur_x1 or cur_y1 is added earlier (for continuity of contours)
			if(choosenXplans[cur_x1]==false && choosenYplans[cur_y1]==false && ContourStarted == true){
				double x = selectivity[cur_x1];
				double y = selectivity[cur_y1];
				unique_points ++;
				 if(x>=x_a && y>=y_a ){
					 done = true;
					 unique_plans.add(getPlanNumber(cur_x1, cur_y1));
					 System.out.println("The number unique points are "+unique_points);
					 System.out.println("The number unique plans are "+unique_plans.size());
					 return unique_plans.size();
				 }
				 else{
					 if(!unique_plans.contains(getPlanNumber(cur_x1, cur_y1)))
						 unique_plans.add(getPlanNumber(cur_x1, cur_y1));
				 }

				//update choosen X and Y plans
				choosenXplans[cur_x1] = true; choosenYplans[cur_y1] = true;

			}
//----------------------------------------------------------------------------------------------------------------------
			cur_y1--;
		}
		else if(cur_val1 < targetval +errorboundval && cur_x1 < cur_x2)
		{
			
			//by Srinivas changes to contour identification code
//----------------------------------------------------------------------------------------------------------------------
			//enter this code block if none of the no plan plan in either cur_x1 or cur_y1 is added earlier (for continuity of contours)
			if(choosenXplans[cur_x1]==false && choosenYplans[cur_y1]==false  && ContourStarted == true){
				double x = selectivity[cur_x1];
				double y = selectivity[cur_y1];
				unique_points ++;
				 if(x>=x_a && y>=y_a ){
					 done = true;
					 
					 unique_plans.add(getPlanNumber(cur_x1, cur_y1));
					 System.out.println("The number unique points are "+unique_points);
					 System.out.println("The number unique plans are "+unique_plans.size());

					 return unique_plans.size();
				 }
				 else{
					 if(!unique_plans.contains(getPlanNumber(cur_x1, cur_y1)))
						 unique_plans.add(getPlanNumber(cur_x1, cur_y1));
				 }

				//update choosen X and Y plans
				choosenXplans[cur_x1] = true; choosenYplans[cur_y1] = true;

			
			}
//----------------------------------------------------------------------------------------------------------------------				
			
			cur_x1++;
		}
		else 
			break;
		
	}
	
	 System.out.println("Number of Unique Plans ="+unique_plans.size());
	 System.out.println("Number of Unique Points ="+unique_points);
	 //		 for(int k : unique_plans){
	 //	            System.out.println(" Plan ="+k);
	 //	        }
	 return unique_plans.size();

	  //Create the Database Connection
}

public int genBruteForce(double cost,int contour_no) throws IOException
{
	HashSet<Integer> unique_plans = new HashSet();
	System.out.println("\nContour number ="+contour_no+",Cost : "+cost);
	double min_cost = cost(resolution-1,resolution-1)+1, max_cost = -1;
	// ---------------------------------------------------------------------- Generate Brute Force..
	ArrayList<point> original = new ArrayList<point>();
	int i;
		double x, y;
		int x_index, y_index;
		int max_x_index=-1;
		int unique_points=0;
		//searching for specific y's
		for(y_index = resolution-1;y_index >=0; y_index--)
		{
			x_index = getContourXPoint(y_index,cost);
			
			if(x_index == -1)
			{
				continue;
			}
			
			int temp_x_index = max_x_index+1;
			if(max_x_index==x_index || max_x_index==-1) //in the case when the portion of contours is 
				temp_x_index= x_index;  //parallel to Y-Axis

			for(;(temp_x_index<=x_index && temp_x_index<resolution);temp_x_index++)
			{
				
				x = selectivity[temp_x_index];
				y = selectivity[y_index];
			
			unique_plans.add(getPlanNumber(temp_x_index,y_index));
			unique_points ++;
			if(cost(temp_x_index,y_index) > max_cost)
				max_cost = cost(temp_x_index,y_index);
			if(cost(temp_x_index,y_index) < min_cost)
				min_cost = cost(temp_x_index,y_index);

			if(x>=x_a && y>=y_a ){
				 done = true;
				 //unique_plans.add(getPlanNumber(x_index,y_index));
				 System.out.println("The number unique points are "+unique_points);
				 System.out.println("The number unique plans are "+unique_plans.size());
				 return unique_plans.size();
			 	}
			
			}
			if(x_index>max_x_index)
				max_x_index = x_index;
			
		}
		//searching for specific x's
		if(max_x_index<resolution-1){
			for(x_index = max_x_index+1;x_index < resolution; x_index++)
			{
				y_index = getContourYPoint(x_index,cost);
				if(y_index == -1)
				{
					continue;
				}
				x = selectivity[x_index];
				y = selectivity[y_index];
				unique_plans.add(getPlanNumber(x_index,y_index));
				unique_points ++;
				if(cost(x_index,y_index) > max_cost)
					max_cost = cost(x_index,y_index);
				if(cost(x_index,y_index) < min_cost)
					min_cost = cost(x_index,y_index);

				if(x>=x_a && y>=y_a ){
					 done = true;
					 //unique_plans.add(getPlanNumber(x_index,y_index));
					 System.out.println("The number unique points are "+unique_points);
					 System.out.println("The number unique plans are "+unique_plans.size());
					 return unique_plans.size();
				 }

				
				}
		}
		//----------------------------------------------------------------------
		/*				
		 for(point p : original) {
			    //        System.out.println(p.getX()+":"+p.getY()+": Plan ="+p.p_no);
			            // Insert the plan number into a set.
			            unique_plans.add(p.p_no);
			            //System.out.println(+p.getX()+":"+p.getY());
			        }
				 System.out.println("Number of Unique Plans ="+unique_plans.size());
				 for(int k : unique_plans){
		            System.out.println("Plan number: "+k);
			        }
			        unique_plans.clear();
		
		
*/
	//------------------------------------------------------- Writing  the brute force solution
//		try {
//	    
////	    String content = "This is the content to write into file";
//
//
//         File filex = new File("/home/dsladmin/Srinivas/data/others/contours/"+"x"+contour_no+".txt"); 
//         File filey = new File("/home/dsladmin/Srinivas/data/others/contours/"+"y"+contour_no+".txt");  
//	    // if file doesn't exists, then create it
//	    if (!filex.exists()) {
//	        filex.createNewFile();
//	    }
//	    if (!filey.exists()) {
//	        filey.createNewFile();
//	    }
//	    FileWriter writerax = new FileWriter(filex, false);
//	    FileWriter writeray = new FileWriter(filey, false);
//	    
//	    PrintWriter pwax = new PrintWriter(writerax);
//	    PrintWriter pway = new PrintWriter(writeray);
//	    //Take iterator over the list
//	    for(point p : original) {
//		    //        System.out.println(p.getX()+":"+p.getY()+": Plan ="+p.p_no);
//	   	 pwax.print((int)p.getX() + "\t");
//	   	 pway.print((int)p.getY()+ "\t");
//	   	 
//	    }
//	    pwax.close();
//	    pway.close();
//	    writerax.close();
//	    writeray.close();
//	    
//		} catch (IOException e) {
//	    e.printStackTrace();
//	}
//		
		//print the no. of points in the contour
		System.out.println("cost of contour is "+ cost+", no of points is "+unique_points);
		System.out.println("cost of contour is "+ cost+", no of plans is "+unique_plans.size());
		System.out.println("Cost_max : "+max_cost+" Cost_min:  "+min_cost);
		return unique_plans.size();

	
}

// Function which does binary search to find the actual point !!
//Return the x co-ordinate *index*
public int getContourXPoint(int y_act_index, double cost)
{
	int min_index = 0;
	int max_index = resolution -1;
	int mid_index;
	double mid_cost,mid_cost_r, mid_cost_l;
	int [] coord = new int[2];
	coord[1] = y_act_index;
	
	
	while (min_index <= max_index)
	{
		mid_index = (int) Math.floor((min_index + max_index)/2);
		// mid_index is the index of selectivity near mid
	//	mid_index = findNearestPoint(selectivity[mid]);
		//Check the cost at index, y_act
		mid_cost = cost(mid_index, y_act_index);
//		System.out.println("\n mid_cost : "+mid_cost);
		if(mid_index != resolution - 1)
		{
			mid_cost_r = cost(mid_index + 1, y_act_index);
		}
		else
		{
			mid_cost_r = mid_cost;
		}
		
		if(mid_index != 0)
		{
			mid_cost_l = cost(mid_index - 1, y_act_index);
		}
		else
		{
			mid_cost_l = mid_cost;
		}
		
		
		if(mid_cost >= cost)
		{
			if(mid_cost_l <= cost)
			{
				//Cost lies between mid and mid-1 so be conservative and return mid_index as the x coordinate of the contour point.
				return mid_index;
			}
			//Then we have to Look from min to mid (left-half)
			max_index = mid_index - 1;
		}
		if(mid_cost < cost)
		{
			if(mid_cost_r >= cost)
			{
				return mid_index + 1;
			}
			//Then we have to look from mid to max(right-half)
			min_index = mid_index + 1;
		}
		
		
	}
	//While ends above
	return -1;

	
}

// Function which does binary search to find the actual point !!
//Return the y co-ordinate *index*
public int getContourYPoint(int x_act_index, double cost)
{
	int min_index = 0;
	int max_index = resolution -1;
	int mid_index;
	double mid_cost,mid_cost_r, mid_cost_l;
	int [] coord = new int[2];
	coord[0] = x_act_index;
	
	
	while (min_index <= max_index)
	{
		mid_index = (int) Math.floor((min_index + max_index)/2);
		// mid_index is the index of selectivity near mid
	//	mid_index = findNearestPoint(selectivity[mid]);
		//Check the cost at index, y_act
		mid_cost = cost(x_act_index,mid_index );
//		System.out.println("\n mid_cost : "+mid_cost);
		if(mid_index != resolution - 1)
		{
			mid_cost_r = cost( x_act_index, mid_index + 1);
		}
		else
		{
			mid_cost_r = mid_cost;
		}
		
		if(mid_index != 0)
		{
			mid_cost_l = cost(x_act_index,mid_index - 1 );
		}
		else
		{
			mid_cost_l = mid_cost;
		}
		
		
		if(mid_cost >= cost)
		{
			if(mid_cost_l <= cost)
			{
				//Cost lies between mid and mid-1 so be conservative and return mid_index as the x coordinate of the contour point.
				return mid_index;
			}
			//Then we have to Look from min to mid (left-half)
			max_index = mid_index - 1;
		}
		if(mid_cost < cost)
		{
			if(mid_cost_r >= cost)
			{
				return mid_index + 1;
			}
			//Then we have to look from mid to max(right-half)
			min_index = mid_index + 1;
		}
		
		
	}
	//While ends above
	return -1;

	
}
double [] getAllPlanCost(int planno){
	return AllPlanCosts[planno];
}


}
//	class Set {
//		HashSet<Integer> elements = new HashSet<Integer>();
//		HashSet elements = new HashSet();
//	}
