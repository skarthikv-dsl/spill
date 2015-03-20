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

//import java.util.Set;

//import iisc.dsl.picasso.client.panel.ReducedPlanPanel.ReducePlan.Set;
//import iisc.dsl.picasso.client.util.PicassoUtil;
//import iisc.dsl.picasso.client.panel.ReducedPlanPanel.ReducePlan.Set;
import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.common.ds.DiagramPacket;
import iisc.dsl.picasso.server.ADiagramPacket;


public class CostGreedy
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
	static double lambda=20;
	static double x_a;
	static double y_a;
	static double z_a;
	static double error;
	static boolean done = false;
	
//	static String QT_NAME = "PG_QT5_100_EXP";
//	static String bouquetPath = "/home/dsladmin/Documents/Project/post_lab_pres_2/packets/PG/QT5/PG_QT5_100_EXP/";
	
	static String QT_NAME = "PG_QT8_100_EXP";
	static String bouquetPath = "/home/dsladmin/Documents/Project/post_lab_pres_2/packets/PG/QT8/PG_QT8_100_EXP/";
	
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

	public void run(double threshold, ADiagramPacket gdp, String apktPath) throws IOException{
		ADiagramPacket reducedgdp = cgFpc(threshold, gdp,apktPath);
		readpkt(reducedgdp);
		loadSelectivity(3);
		double h_cost = getOptimalCost(totalPoints-1);
		double cost = getOptimalCost(0);
		double ratio = h_cost/cost;
	//	System.out.println("-------------------------  ------\n"+qtName+"    alpha="+alpha+"\n-------------------------  ------"+"\n"+"Highest Cost ="+h_cost+", \nRatio of highest cost to lowest cost ="+ratio);
		System.out.println("the ratio of C_max/c_min is "+ratio);
		GCI3D obj = new GCI3D();
		//setting the actual values of x_a, y_a and z_a
		x_a = 0;//obj.x_a;
		y_a = 0;//obj.y_a;
		z_a = 0; //obj.z_a;
		error = 0;//obj.error;
		System.out.println("The original (x_a,y_a,z_a)= ("+x_a+","+y_a+","+z_a+")");
		
		x_a = findNearestSelectivity(x_a);
		y_a = findNearestSelectivity(y_a);
		z_a = findNearestSelectivity(z_a);
		System.out.println("After nearest selectivity (x_a,y_a,z_a)= ("+x_a+","+y_a+","+z_a+")");
	//	System.out.println("After nearest selectivity again (x_a,y_a)= ("+obj.findNearestSelectivity(x_a)+","+obj.findNearestSelectivity(y_a)+")");
		
	 // for ( i = 1; i < 4 ; i++)
		int i = 1;
		double algo_cost =0;
		//cost*=2;
		while(cost < h_cost & !done)
		{
			System.out.println("---------------------------------------------------------------------------------------------\n");
			System.out.println("Contour "+i+" cost : "+cost+"\n");
			
			int num = run_new_seed_3d_algorithm(cost,error*cost);
			algo_cost += num*cost;
			cost = cost*2;
			i = i+1;
			System.out.println("---------------------------------------------------------------------------------------------\n");
		}
		double MSO;
		if(!done){
			algo_cost += h_cost;
			MSO = (algo_cost/cost(findNearestPoint(x_a),findNearestPoint(y_a),findNearestPoint(z_a)));
		}
		else
			MSO = (algo_cost/cost(findNearestPoint(x_a),findNearestPoint(y_a),findNearestPoint(z_a)));
			
		System.out.println("Cost of actual_sel ="+cost(findNearestPoint(x_a),findNearestPoint(y_a),findNearestPoint(z_a)));
		
		System.out.println("The Plan Bouquet MSO is "+MSO);
		
		
		
		//System.out.println("\nCost[23] = "+OptimalCost[1]+ ", Selectivity[3] = "+selectivity[1]+"\n");
//		System.out.println("\n Path :"+ pktPath + "\n");
		System.out.println("\nMainFinished\n");

		return;
	}
		
	
	 double cost(int x, int y, int z)
	{
		int [] arr = new int [3];
		arr[0] = x;
		arr[1] = y;
		arr[2] = z;
		int index = getIndex(arr,resolution);

		
		return OptimalCost[index];
	}
	
	private int run_new_seed_3d_algorithm(double targetval, double errorboundval) {
		// TODO Auto-generated method stub
		 int cur_x1 = 0, cur_y1 = resolution - 1, cur_z = 0;
		 ArrayList<point> original = new ArrayList<point>();
		 double cur_val;
		 double X_max=0,Y_max=0,Z_max=0; 

		 ArrayList<Integer> unique_plans = new ArrayList<Integer>();

		 double max_x_pt[];
		 max_x_pt = new double[3];
		 max_x_pt[0] = 0;
		 max_x_pt[1] = 0;
		 max_x_pt[2] = 0;

		 double max_y_pt[];
		 max_y_pt = new double[3];
		 max_y_pt[0] = 0;
		 max_y_pt[1] = 0;
		 max_y_pt[2] = 0;

		 double max_z_pt[];
		 max_z_pt = new double[3];
		 max_z_pt[0] = 0;
		 max_z_pt[1] = 0;
		 max_z_pt[2] = 0;

		 int max_x_plan=-1, max_y_plan=-1, max_z_plan=-1;


		 while(cur_z < resolution)
		 {		
			 /* find min and max of a constant z-slice. check targetval is within 
			  * this range. Otherwise skip this z-slice
			  */
			 double min_cost_slice = cost(0, 0, cur_z);
			 double max_cost_slice = cost(resolution-1, resolution-1, cur_z);
			 if(!(targetval >= min_cost_slice && targetval <= max_cost_slice))
			 {
				 cur_z++;
				 //		saves++;
				 continue;
			 }

			 cur_x1 = 0;
			 cur_y1 = resolution - 1;

			 /* if top left cost is more than target do binary search on left edge of 2d not on top edge*/
			 cur_val =  cost(0, resolution-1, cur_z);

			 if(cur_val > targetval)
			 {
				 int low = 0, high = resolution - 1, mid = resolution - 1;

				 /* do a binary search on TOP edge to find out starting Y point */
				 while(low < high)
				 {
					 mid = (low + high) / 2;

					 cur_val = cost( cur_x1, mid,cur_z);

					 if(cur_val >= targetval && cur_val <= targetval)
					 {
						 cur_y1 = mid;
						 break;
					 }
					 else if(cur_val < targetval)
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

				 /* do a binary search on LEFT edge to find out starting X point */
				 while(low < high)
				 {
					 mid = (low+high) / 2;

					 cur_val = cost( mid, cur_y1,cur_z);

					 if(cur_val >= targetval && cur_val <= targetval)
					 {
						 cur_x1 = mid;
						 break;
					 }
					 else if(cur_val < targetval)
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

			 /* Fix the last point of the contour by starting from backwards */
			 int cur_x2 = resolution - 1, cur_y2 = 0;
			 double cur_val2;

			 cur_val2 = cost( cur_x2, cur_y2,cur_z);

			 if(cur_val2 > targetval)
			 {
				 int low = 0, high = resolution - 1, mid = resolution - 1;

				 /* do a binary search on bottom row to find out starting point */
				 while(low < high)
				 {
					 mid = (low + high) / 2;
					 cur_val2 = cost( mid, cur_y2, cur_z);

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
					 cur_val2 = cost( cur_x2, mid, cur_z);

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

			 /* do until you cross boundary starting from start point*/
			 
			 while(cur_x1 < resolution && cur_y1 >= 0)
			 {
				 //			System.out.println("x = " + cur_x + ", y = " + cur_y);

				 cur_val = cost(cur_x1, cur_y1, cur_z);

				 if(cur_val >= targetval - errorboundval && cur_val <= targetval + errorboundval)
				 {
					 /* when you hit a interesting point, just keep going on x axis as long as it is interesting
					  * then backtrack to exactly where you started getting interesting points */
					 int t_y1 = cur_y1;

					 while(cur_val >= targetval - errorboundval && cur_val <= targetval + errorboundval)
					 {
						 cur_y1 = t_y1;
						 cur_y1 = t_y1;
						 double x = selectivity[cur_x1];
						 double y = selectivity[cur_y1];
						 double z = selectivity[cur_z];
						 //plan bouquet Algorithm Starts
						 if(x>=x_a && y>=y_a && z>=z_a){
							 done = true;
							 unique_plans.add(getPlanNumber(cur_x1, cur_y1,cur_z));
							 return unique_plans.size();
						 }
						 else{
							 if(!unique_plans.contains(getPlanNumber(cur_x1, cur_y1,cur_z)))
								 unique_plans.add(getPlanNumber(cur_x1, cur_y1,cur_z));
						 }
						 
						 //soriginal.add(p2);


					 if(t_y1 <= cur_y2)
						 break;
					 t_y1 = t_y1 - 1;
					 cur_val = cost(cur_x1, t_y1, cur_z);
				 }
				 cur_x1++;
			 }
			 else if(cur_val > targetval && cur_y1 > cur_y2)
			 {
				 cur_y1--;
			 }
			 else
			 {
				 cur_x1++;
			 }

		 }

		 cur_z++;
	}
	 // Number of plans


	 System.out.println("Number of Unique Plans ="+unique_plans.size());
	 //		 for(int k : unique_plans){
	 //	            System.out.println(" Plan ="+k);
	 //	        }
	 return unique_plans.size();


	}


	double getOptimalCost(int index)
	{
		return OptimalCost[index];
	}
	
	int getPlanNumber(int x, int y, int z)
	{
		 int arr[] = {x,y,z};
		int index = getIndex(arr,resolution);
		return plans[index];
	}
	

	
	void loadSelectivity(int option)
	{
	//	System.out.println("\n **********************************************************Resolution = "+resolution);
		double sel;
		this.selectivity = new double [resolution];
		double startpoint = 0.0;
		double endpoint = 1.0;
		
		double r = 1.0;
		double diff =0;
		
		double QDIST_SKEW_10 = 2.0;
		double QDIST_SKEW_30 = 1.33;
		double QDIST_SKEW_100 = 1.083;
		double QDIST_SKEW_300 = 1.027;
		double QDIST_SKEW_1000 = 1.00808;
		
		
		//assert(option == UNI || option == EXP): "Wrong input to loadSelectivity";
		if(option == UNI)
		{
			
			sel= startpoint + ((endpoint - startpoint)/(2*resolution));
			for(int i=0;i<resolution;i++){
				this.selectivity[i] = sel;
				//System.out.println("\nSelectivity["+i+"] = "+selectivity[i]+"\t");
				sel += ((endpoint - startpoint)/resolution);
			}
		}
		else if (option == EXP)
		{
			switch(resolution)
			{
				case 10:
					r=QDIST_SKEW_10;
					break;
				case 30:
					r=QDIST_SKEW_30;
					break;
				case 100:
					r=QDIST_SKEW_100;
					break;
				case 300:
					r=QDIST_SKEW_300;
					break;
				case 1000:
					r=QDIST_SKEW_1000;
					break;
			}
			int i,j=0;
			
			int popu=resolution;
			double a=1; //startval
			double curval=a,sum=a/2;
			
			for(i=1;i<=popu;i++)
			{
				curval*=r;
				if(i!=popu)
				sum+=curval;
				else
					sum+=curval/2;
			}
			a=1/sum;
			curval=a;
			sum=a/2;
			
			for(i=1;i<=popu;i++)
			{
				
				selectivity[i-1] = startpoint + sum;
				//System.out.println("\n"+Math.abs(diff - selectivity[i-1]));
				diff = selectivity[i-1];
			//	System.out.println("\n Sel["+(i-1)+"]="+selectivity[i-1]);
				curval*=r;
				if(i!=popu)
					sum+=(curval * (endpoint - startpoint));
				else
					sum+=(curval * (endpoint - startpoint))/2;
			}
		}
		else{
//			selectivity[0] = 0.0000000001;  selectivity[1] = 0.000000004;	selectivity[2] = 0.000000007;	selectivity[3] = 0.0000001;
//			selectivity[4] = 0.0000005;   selectivity[5] = 0.0000008;	selectivity[6] = 0.000001;	selectivity[7] = 0.000005;
//			selectivity[8] = 0.000009;	selectivity[9] = 0.00004;	selectivity[10] = 0.00007;	selectivity[11] = 0.0001;
//			selectivity[12] = 0.0003;	selectivity[13] = 0.0005;	selectivity[14] = 0.0008;	selectivity[15] = 0.001;
//			selectivity[16] = 0.003;	selectivity[17] = 0.006;	selectivity[18] = 0.009;	selectivity[19] = 0.02;
//			selectivity[20] = 0.04;	selectivity[21] = 0.06;	selectivity[22] = 0.08;	selectivity[23] = 0.1;
//			selectivity[24] = 0.2;	selectivity[25] = 0.3;	selectivity[26] = 0.4;	selectivity[27] = 0.6;
//			selectivity[28] = 0.8;	selectivity[29] = 0.9950;
			
			selectivity[0] = 0.00005;  selectivity[1] = 0.0001;	selectivity[2] = 0.0003;	selectivity[3] = 0.0005;
			selectivity[4] = 0.0007;   selectivity[5] = 0.0010;	selectivity[6] = 0.0014;	selectivity[7] = 0.0019;
			selectivity[8] = 0.0026;	selectivity[9] = 0.0036;	selectivity[10] = 0.0048;	selectivity[11] = 0.0065;
			selectivity[12] = 0.0087;	selectivity[13] = 0.0117;	selectivity[14] = 0.0156;	selectivity[15] = 0.0208;
			selectivity[16] = 0.0278;	selectivity[17] = 0.0370;	selectivity[18] = 0.0493;	selectivity[19] = 0.0657;
			selectivity[20] = 0.0874;	selectivity[21] = 0.1164;	selectivity[22] = 0.1549;	selectivity[23] = 0.2061;
			selectivity[24] = 0.2741;	selectivity[25] = 0.3647;	selectivity[26] = 0.48515;	selectivity[27] = 0.6453;
			selectivity[28] = 0.8583;	selectivity[29] = 0.9950;		

		}
	}

	// Return the index near to the selecitivity=mid;
		int findNearestPoint(double mid)
		{
			int i;
			double min_diff = Double.MAX_VALUE;
			int return_index = resolution - 1;
			double diff;
			double return_value;
			for(i=0;i<resolution;i++)
			{	
				diff = mid - selectivity[i];
				if(diff < 0)
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
			//System.out.println("return_index="+return_index);
			return return_index;
		}
		// Return the index near to the selecitivity=mid;
			public double findNearestSelectivity(double mid)
			{
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
				//System.out.println("return_index="+return_index);
				return selectivity[return_index];
			}

	public ADiagramPacket cgFpc(double threshold, ADiagramPacket gdp, String apktPath) {
		
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
			ObjectInputStream ip = new ObjectInputStream(new FileInputStream(new File(apktPath + i + ".pcst")));
			double[] cost = (double[]) ip.readObject();
			for (int j = 0; j < total_points; j++)
			{
				if(i==18 && j==total_points-1)
					System.out.println("This is interesting");
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


}
	class Set {
		// HashSet<Integer> elements = new HashSet<Integer>();
		HashSet elements = new HashSet();
	}
