/*
 * For Multi-D !!
 *  This Program generates the Virtual Contours using the gradient assumption.
 *  It also gives the number of optimizations required for getting all the contours.
 *  
 *  -- Pseudocode --
 *  Input : QT's apkt file
 *  		Cost of the contour to be find out.
 *  
 *  Output : Set of points in the plan diagram(and Plans at those points) which covers a specified contour.
 *  
 *  Algorithm :
 *  
 *  Parse the apkt file to get the resolution, selectivities and cost at each point.
 *  Fix Alpha, e -> Error due to discretization..
 *  x_min = minimum Selectivity of the dimension
 *  While x_act >=1
 *  	x_act = x_min
 *  	do binary search to find y_act, s.t Cost(x_act, y_act) = [C-e, C+e]
 *  	if(alpha*x_min is not greater than 1)
 *  		Put the point (alpha*x_min, y_act) to the output list.
 *  	else
 *  		put(1,y_act) to output list.
 *  	x_act = alpha*x_act;
 *  	loop
 *  	
 *  -- Pseudocode Ends --
 *  
 *  Keep track of number of optimizations been called.
 *  Keep track of the total investment benifit
 *  	Call a function which actually gets the contours and gets the total investment.(Brute force case)
 *  
 * */
// ---------------------------------------------------------------------
/*
 * Doubts :
 *  1. Whether the apkt and pkt files are same ??
 *  
 * */



import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
//import java.util.Set;
import java.util.Comparator;

import org.apache.commons.lang3.builder.CompareToBuilder;

import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;


public class CostGreedyGCI3D
{
	static double AllPlanCosts[][];
	static int nPlans;

	static int UNI = 1;
	static int EXP = 2;

	static double err = 0.03;//no use
	//Settings
	static double threshold = 20;

	int plans[];
	double OptimalCost[];
	int totalPlans;
	int dimension;
	static int resolution;
	DataValues[] data;
	static int totalPoints;
	double selectivity[];

	static ArrayList<point3D> contourPoints = new ArrayList<point3D>();
	
	//The following parameters has to be set manually for each query
	static String apktPath;
	static String plansPath = "/home/dsladmin/Srinivas/data/spillBound/temp/";
	static String qtName ;
	static String varyingJoins;
	static int JS_multiplier1;
	static int JS_multiplier2;
	static String query;
	static String cardinalityPath;
	
	double  minIndex [];
	double  maxIndex [];
	static boolean MSOCalculation = true;
	static boolean randomPredicateOrder = false; 
	static Connection conn = null;

	static ArrayList<Integer> remainingDim;
	static ArrayList<ArrayList<Integer>> allPermutations = new ArrayList<ArrayList<Integer>>();
	 static ArrayList<point_generic> final_points = new ArrayList<point_generic>();
	 static ArrayList<Integer> learntDim = new ArrayList<Integer>();
		//static ArrayList<Integer> learntDimIndices = new ArrayList<Integer>();
	 static HashMap<Integer,Integer> learntDimIndices = new HashMap<Integer,Integer>();
	 static HashMap<Integer,ArrayList<point_generic>> ContourPoints = new HashMap<Integer,ArrayList<point_generic>>();

	 static double learning_cost = 0;
	 static boolean done = false;
	 
	 double[] actual_sel;

	public static void main(String args[]) throws IOException, SQLException
	{
		
		int selConf = 3;

		CostGreedyGCI3D obj = new CostGreedyGCI3D();
		obj.loadPropertiesFile();
		String pktPath = apktPath + qtName + ".apkt" ;
		System.out.println("Query Template: "+qtName);
		
		
		ADiagramPacket gdp = obj.getGDP(new File(pktPath));
		
		
//		cg.run(threshold, gdp,apktPath);
		ADiagramPacket reducedgdp = obj.cgFpc(threshold, gdp,apktPath);

		//Settings
		//Populate the OptimalCost Matrix.
		//obj.readpkt(gdp);
		obj.readpkt(reducedgdp);

		//Populate the selectivity Matrix.
		obj.loadSelectivity(selConf);
		
		//Calculate Native MSO
		obj.findingNativeMSO();
		
		int i;
		double h_cost = obj.getOptimalCost(totalPoints-1);
		double min_cost = obj.getOptimalCost(0);
		double ratio = h_cost/min_cost;
	//	System.out.println("-------------------------  ------\n"+qtName+"    alpha="+alpha+"\n-------------------------  ------"+"\n"+"Highest Cost ="+h_cost+", \nRatio of highest cost to lowest cost ="+ratio);
		System.out.println("the ratio of C_max/c_min is "+ratio);
		
		i = 1;
		
		remainingDim.clear(); 
		for(int d=0;d<obj.dimension;d++){
			remainingDim.add(d);
		}
		learntDim.clear();
		
		learntDimIndices.clear();
		double cost = obj.getOptimalCost(0);
		//cost*=2;
		getAllPermuations(remainingDim,0);
		while(cost < 2*h_cost)
		{
		
			if(cost>h_cost)
				cost = h_cost;
			System.out.println("---------------------------------------------------------------------------------------------\n");
			System.out.println("Contour "+i+" cost : "+cost+"\n");
			
			final_points.clear();
			//final_points = new ArrayList<point_generic>();
			for(ArrayList<Integer> order:allPermutations){
				System.out.println("Entering the order"+order);
				learntDim.clear();
				learntDimIndices.clear();
				obj.getContourPoints(order,cost);
			}
			//writeContourPointstoFile(i);
			int size_of_contour = final_points.size();
			ContourPoints.put(i, new ArrayList<point_generic>(final_points)); //storing the contour points
			System.out.println("Size of contour"+size_of_contour );
				cost = cost*2;
				i = i+1;
		}
	

		/*
		 * running the plan bouquet algorithm 
		 */
		double MSO =0, ASO = 0,SO=0;
		
		//int max_point = 0; /*to not execute the spillBound algorithm*/
		int max_point = 1; /*to execute a specific q_a */
		//Settings
		if(MSOCalculation)
		//if(false)
			max_point = totalPoints;
		double[] subOpt = new double[max_point];
	  for (int  j = 0; j < max_point ; j++)
	  {
		System.out.println("Entering loop "+j);

		//initialization for every loop
		double algo_cost =0;
		SO =0;
		cost = obj.getOptimalCost(0);
		obj.intialize(j);
		int[] index = obj.getCoordinates(obj.dimension, obj.resolution, j);
//		if(index[0]%5 !=0 || index[1]%5!=0)
//			continue;
//		obj.actual_sel[0] = 0.31;obj.actual_sel[1] = 0.0;//obj.actual_sel[2] = 0.0; /*uncomment for single execution*/
		
		for(int d=0;d<obj.dimension;d++) obj.actual_sel[d] = obj.findNearestSelectivity(obj.actual_sel[d]);
		//----------------------------------------------------------
		i =1;
		while(i<=ContourPoints.size() && !done)
		{	
			if(cost<(double)10000){
				cost *= 2;
				i++;
				continue;
			}
			if(cost>h_cost)
				cost=h_cost;
			System.out.println("---------------------------------------------------------------------------------------------\n");
			System.out.println("Contour "+i+" cost : "+cost+"\n");
			obj.sortContourPoints(i);

			obj.planBouquetAlgo(i,cost);
			
			algo_cost = algo_cost+ (learning_cost);
			System.out.println("The current algo_cost is "+algo_cost);
			System.out.println("The cost expended in this contour is "+learning_cost);
			cost = cost*2;  
			i = i+1;
			System.out.println("---------------------------------------------------------------------------------------------\n");

		}  //end of while
		
		if(!done){
			System.out.println("ERROR: Still plan bouquet not completed");
		}

		
		/*
		 * printing the actual selectivity
		 */
		System.out.print("\nThe actual selectivity is original \t");
		for(int d=0;d<obj.dimension;d++) 
			System.out.print(obj.actual_sel[d]+",");
		
		/*
		 * storing the index of the actual selectivities. Using this printing the
		 * index (an approximation) of actual selectivities and its cost
		 */
		int [] index_actual_sel = new int[obj.dimension]; 
		for(int d=0;d<obj.dimension;d++) index_actual_sel[d] = obj.findNearestPoint(obj.actual_sel[d]);
		
		System.out.print("\nCost of actual_sel ="+obj.cost_generic(index_actual_sel)+" at ");
		for(int d=0;d<obj.dimension;d++) System.out.print(index_actual_sel[d]+",");

		SO = (algo_cost/obj.cost_generic(index_actual_sel));
		SO = SO * (1 + threshold/100);
		subOpt[j] = SO;
		ASO += SO;
		if(SO>MSO)
			MSO = SO;
		System.out.println("\nPlanBouquet The SubOptimaility  is "+SO);
	  } //end of for
	  	//Settings
	  	obj.writeSuboptToFile(subOpt, apktPath);
	  	
		System.out.println("PlanBouquet The MaxSubOptimaility  is "+MSO);
		System.out.println("PlanBouquet The AverageSubOptimaility  is "+(double)ASO/max_point);

	}
	
	 private void sortContourPoints(int contour_no) {

		 String funName  = "sortContourPoints";
		 
		 Collections.sort(ContourPoints.get(contour_no), new pointComparator());
	}

	 public void writeSuboptToFile(double[] subOpt,String path) throws IOException {

		 //settings
	       File file = new File(path+"planBouquet_20"+"suboptimality"+".txt");
		    if (!file.exists()) {
		        file.createNewFile();
		    }
		    FileWriter writer = new FileWriter(file, false);
		    PrintWriter pw = new PrintWriter(writer);

		    
		    for(int loc=0;loc<totalPoints;loc++){
		    	int[] index = getCoordinates(dimension, resolution, loc);
		    	for(int d=0;d<dimension;d++){
		    		pw.print(index[d]+",");
		    	}
		    	pw.print("\t is "+subOpt[loc]+"\n");
		    }
//			for(int i =0;i<resolution;i++){
//				for(int j=0;j<resolution;j++){
//					//Settings
//					for(int k=0;k<resolution;k++){
//					//if(i%5==0 && j%5==0){
//						int [] index = new int[3];
//						index[0] = i;
//						index[1] = j;
//						index[2] = k;
//						int ind = getIndex(index, resolution);
//						if(j!=0)
//							pw.print("\t"+subOpt[ind]);
//						else
//							pw.print(subOpt[ind]);
//					//}
//					}	
//				}
//				//if(i%2==0)
//					//pw.print("\n");
//			}
			pw.close();
			writer.close();
			
		}
	
	public void planBouquetAlgo(int contour_no, double cost) {

		String funName = "planBouquetAlgo";
		
		double last_exec_cost = 0;
		learning_cost =0;
		int [] arr = new int[dimension];
		HashSet<Integer> unique_plans = new HashSet();
		int unique_points =0;
		double max_cost =0 , min_cost = Double.MAX_VALUE;
		
		for(int c=0;c< ContourPoints.get(contour_no).size();c++){
			
			point_generic p = ContourPoints.get(contour_no).get(c);
			
			/*needed for testing the code*/
			unique_points ++;
			if(p.get_cost()>max_cost)
				max_cost = p.get_cost();
			if(p.get_cost() < min_cost)
				min_cost = p.get_cost();
			
			/*
			 * to check if p dominates actual selectivity
			 */
			boolean flag = true;
			for(int d=0;d<dimension;d++){
				if(p.get_dimension(d) < findNearestPoint(actual_sel[d])){
					flag = false;
					break;
				}
			}
			for(int d=0;d<dimension;d++){
				arr[d] = p.get_dimension(d);
				//System.out.print(arr[d]+",");
			}
			
			if(!unique_plans.contains(getPlanNumber_generic(arr))){
				learning_cost += cost;
				//Settings: learning_cost += p.get_cost();  changed to include only the contour cost and not the point
				unique_plans.add(getPlanNumber_generic(arr));
				last_exec_cost = cost;
			}
			if(flag == true){
				if(cost_generic(convertSelectivitytoIndex(actual_sel)) > 2*cost)
					flag = false;
			}
			if(flag){
				done = true;
				 System.out.println("The number unique points are "+unique_points);
				 System.out.println("The number unique plans are "+unique_plans.size());
				 System.out.println("The  unique plans are "+unique_plans);
				 System.out.print("The final execution cost is "+p.get_cost()+ "at :" );
				//Settings:  changed to include only the contour cost and not the point
//				 if(p.get_cost() > last_exec_cost ){
//					 learning_cost -= last_exec_cost;
//					 learning_cost += p.get_cost();
//				 }
				 learning_cost -= last_exec_cost;
				 int [] int_actual_sel = new int[dimension];
				 for(int d=0;d<dimension;d++)
					 int_actual_sel[d] = findNearestPoint(actual_sel[d]);
				 double oneDimCost = cost;
				 if(fpc_cost_generic(int_actual_sel, p.get_plan_no())<oneDimCost)
					 oneDimCost = fpc_cost_generic(int_actual_sel, p.get_plan_no());
				 if(cost_generic(int_actual_sel)> oneDimCost)
					 oneDimCost = cost_generic(int_actual_sel);
				 learning_cost  += oneDimCost;
	 
				 for(int d=0;d<dimension;d++){
						System.out.print(arr[d]+",");
					}
				System.out.println();
				return;
			}
		}	
		 System.out.println("The number unique points are "+unique_points);
		 System.out.println("The number unique plans are "+unique_plans.size());
		 System.out.println("The  unique plans are "+unique_plans);
		 System.out.println("Contour No. is "+contour_no+" : Max cost is "+max_cost+" and min cost is "+min_cost);


	}
	

private double[] convertIndextoSelectivity(int[] point_Index) {
	
	String funName = "convertIndextoSelectivity";
	
	double [] point_selec = new double[point_Index.length];
	assert (point_Index.length == dimension): funName+" ERROR: point index length not matching with dimension"; 
	for(int d=0; d<dimension;d++)
		point_selec[d] = selectivity[point_Index[d]];
	return point_selec;
}
private int[] convertSelectivitytoIndex (double[] point_sel) {
	
	String funName = "convertSelectivitytoIndex";
	
	int [] point_index = new int[point_sel.length];
	assert (point_sel.length == dimension): funName+" ERROR: point index length not matching with dimension"; 
	for(int d=0; d<dimension;d++)
		point_index[d] = findNearestPoint(point_sel[d]);
	return point_index;
}


public void intialize(int location) {

	String funName = "intialize";

	//updating the remaining dimensions data structure
	remainingDim.clear();
	for(int i=0;i<dimension;i++)
		remainingDim.add(i);
	
	learning_cost = 0;
	done = false;
	//updating the actual selectivities for each of the dimensions
	int index[] = getCoordinates(dimension, resolution, location);
	
	actual_sel = new double[dimension];
	for(int i=0;i<dimension;i++){
		actual_sel[i] = selectivity[index[i]];
	}
	
	
	//sanity check conditions
	assert(remainingDim.size() == dimension): funName+"ERROR: mismatch in remaining Dimensions";
	
	
}

	private static void writeContourPointstoFile(int contour_no) {

		try {
	    
//	    String content = "This is the content to write into file";


         File filex = new File("/home/dsladmin/Srinivas/data/others/contours/"+"x"+contour_no+".txt"); 
         File filey = new File("/home/dsladmin/Srinivas/data/others/contours/"+"y"+contour_no+".txt"); 
         //File filez = new File("/home/dsladmin/Srinivas/data/others/contours/"+"z"+contour_no+".txt"); 
	    // if file doesn't exists, then create it
	    if (!filex.exists()) {
	        filex.createNewFile();
	    }
	    if (!filey.exists()) {
	        filey.createNewFile();
	    }
//	    if (!filez.exists()) {
//	        filez.createNewFile();
//	    }

	    FileWriter writerax = new FileWriter(filex, false);
//	    FileWriter writeraz = new FileWriter(filez, false);
	    FileWriter writeray = new FileWriter(filey, false);

	    
	    PrintWriter pwax = new PrintWriter(writerax);
	    PrintWriter pway = new PrintWriter(writeray);
//	    PrintWriter pwaz = new PrintWriter(writeraz);
	    //Take iterator over the list
	    for(point_generic p : final_points) {
		    //        System.out.println(p.getX()+":"+p.getY()+": Plan ="+p.p_no);
	   	 pwax.print((int)p.get_dimension(0) + "\t");
	   	 pway.print((int)p.get_dimension(1)+ "\t");
//	   	pwaz.print((int)p.get_dimension(2)+ "\t");
	   	 
	    }
	    pwax.close();
	    pway.close();
//	    pwaz.close();
	    writerax.close();
	    writeray.close();
//	    writeraz.close();
	    
		} catch (IOException e) {
	    e.printStackTrace();
	}
		
	}


	private static void getAllPermuations(ArrayList<Integer> DimOrder,int k) {

	 //   static void permute(int[] a, int k) 
		if (k == DimOrder.size()) 
		{	ArrayList<Integer> tempList = new ArrayList<Integer>();
			for (int i = 0; i <  DimOrder.size(); i++) 
			{
				tempList.add(DimOrder.get(i));
			}
			allPermutations.add(tempList);
		} 
		else 
		{
			for (int i = k; i < DimOrder.size(); i++) 
			{
				int temp = DimOrder.get(k);
				DimOrder.set(k, DimOrder.get(i));
				DimOrder.set(i,temp);

				getAllPermuations(DimOrder, k + 1);

				temp = DimOrder.get(k);
				DimOrder.set(k, DimOrder.get(i));
				DimOrder.set(i,temp);

			}

		}

	}
	
	


	void getContourPoints(ArrayList<Integer> order,double cost) throws IOException
	{
		//Assume 
		//1. there is a List named "final_points";
		//2. Make sure you emptied "final_points" before calling this function; 
		
		
		ArrayList<Integer> remainingDimList = new ArrayList<Integer>();
		for(int i=0;i<order.size();i++)
		{
			if(learntDim.contains(order.get(i))!=true)
			{
				remainingDimList.add(order.get(i));
			}			
		}
		
		if(remainingDimList.size()==1 )
		{ 
			int last_dim=-1;
			int [] arr = new int[dimension];
			
			for(int i=0;i<dimension;i++)
			{
				if(learntDim.contains(i))
				{
					arr[i] = learntDimIndices.get(i);
					//System.out.print(arr[i]+",");
				}
				else
					last_dim = i;
			}
			
			//Search the whole line and return all the points which fall into the contour
			for(int i=0;i< resolution;i++)
			{
				arr[last_dim] = i;
				double cur_val = cost_generic(arr);
				double targetval = cost;

				if(cur_val == targetval)
				{

					if(!pointAlreadyExist(arr)){ //check if the point already exist
						point_generic p = new point_generic(arr,getPlanNumber_generic(arr),cur_val, remainingDim);
						final_points.add(p);
					}
				}
				else if(i!=0){
					arr[last_dim]--; //in order to look at the cost at lower value on the last index
					double cur_val_l = cost_generic(arr);
					arr[last_dim]++; //restore the index back 
					if( cur_val > targetval  && cur_val_l < targetval ) //NOTE : changed the inequality to strict inequality
					{
						if(!pointAlreadyExist(arr)){ //check if the point already exist
							point_generic p = new point_generic(arr,getPlanNumber_generic(arr), cur_val,remainingDim);
							final_points.add(p);
						}
					}
					
				}
			}


			return;
		}
		
		Integer curDim = remainingDimList.get(0); //index of 0 or size-1 does not matter
		Integer cur_index = resolution -1;
		
		while(cur_index >= 0)
		{
			learntDim.add(curDim);
			learntDimIndices.put(curDim,cur_index);
			getContourPoints(order,cost);
			learntDim.remove(learntDim.indexOf(curDim));
			learntDimIndices.remove(curDim);
			cur_index = cur_index - 1;
		}
		
	}
	
	private boolean pointAlreadyExist(int[] arr) {

		boolean flag = false;
		for(point_generic p: final_points){
			flag = true;
			for(int i=0;i<dimension;i++){
				if(p.get_dimension(i)!= arr[i]){
					flag = false;
					break;
				}
			}
			if(flag==true)
				return true;
		}
		
		return false;
	}


	
	
	
	// Function which does binary search to find the actual point !!
// Return the index near to the selecitivity=mid;
	public int findNearestPoint(double mid)
	{
		int i;
		int return_index = 0;
		double diff;
		if(mid >= selectivity[resolution-1])
			return resolution-1;
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
		return return_index;
	}
	

	// Return the selectivity just greater than selecitivity=mid;
		public double findNearestSelectivity(double mid)
		{
			int i;
			int return_index = 0;
			double diff;
			if(mid >= selectivity[resolution-1])
				return selectivity[resolution-1];
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

		/*-------------------------------------------------------------------------------------------------------------------------
		 * Populates -->
		 * 	dimension
		 * 	resolution
		 * 	totalPoints
		 * 	OptimalCost[][]
		 * 
		 * */
		void readpkt(ADiagramPacket gdp) throws IOException
		{
			//ADiagramPacket gdp = getGDP(new File(pktPath));
			totalPlans = gdp.getMaxPlanNumber();
			dimension = gdp.getDimension();
			resolution = gdp.getMaxResolution();
			data = gdp.getData();
			totalPoints = (int) Math.pow(resolution, dimension);
			//System.out.println("\nthe total plans are "+totalPlans+" with dimensions "+dimension+" and resolution "+resolution);

			assert (totalPoints==data.length) : "Data length and the resolution didn't match !";

			plans = new int [data.length];
			OptimalCost = new double [data.length]; 
			for (int i = 0;i < data.length;i++)
			{
				this.OptimalCost[i]= data[i].getCost();
				this.plans[i] = data[i].getPlanNumber();
				//System.out.println("At "+i+" plan is "+plans[i]);
				//System.out.println("Plan Number ="+plans[i]+"\n");
				//	System.out.println("\nOptimal_cost:["+i+"] ="+OptimalCost[i]+"\n");
			}

			//TO get the number of points for each plan
			int  [] plan_count = new int[totalPlans];
			for(int p=0;p<data.length;p++){
				plan_count[plans[p]]++;
			}
			//printing the above
			for(int p=0;p<plan_count.length;p++){
				System.out.println("Plan "+p+" has "+plan_count[p]+" points");
			}

			 remainingDim = new ArrayList<Integer>();
				for(int i=0;i<dimension;i++)
					remainingDim.add(i);
				minIndex = new double[dimension];
				maxIndex = new double[dimension];

				// ------------------------------------- Read pcst files
				nPlans = totalPlans;
				AllPlanCosts = new double[nPlans][totalPoints];
				//costBouquet = new double[total_points];
				for (int i = 0; i < nPlans; i++) {
					try {

						ObjectInputStream ip = new ObjectInputStream(new FileInputStream(new File(apktPath + i + ".pcst")));
						double[] cost = (double[]) ip.readObject();
						for (int j = 0; j < totalPoints; j++)
						{
						
							AllPlanCosts[i][j] = cost[j];
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			//writing the optimal cost matirx

			/*	File file = new File("C:\\Lohit\\data\\plans\\"+i+".txt");
			    if (!file.exists()) {
					file.createNewFile();
				}

			    FileWriter writer = new FileWriter(file, false);

			    PrintWriter pw = new PrintWriter(writer);

			for(int i=0;i<resolution;i++){
				for(int j=0;j<resolution;j++){
					int[] indexp = {j,i};
					pw.format("%f\t", OptimalCost[getIndex(indexp, resolution)]);;
				}
				pw.format("\n");
			}
			 */	

		}

	
	 double cost_generic(int arr[])
		{
		 
			int index = getIndex(arr,resolution);

			
			return OptimalCost[index];
		}
	 double fpc_cost_generic(int arr[], int plan)
		{
		 
			int index = getIndex(arr,resolution);

			
			return AllPlanCosts[plan][index];
		}

	//-------------------------------------------------------------------------------------------------------------------
		/*
		 * Populates the selectivity Matrix according to the input given
		 * */
		void loadSelectivity(int option)
		{
			String funName = "loadSelectivity: ";
			System.out.println(funName+" Resolution = "+resolution);
			double sel;
			this.selectivity = new double [resolution];
			
			//settings
			if(resolution == 10){
			selectivity[0] = 0.00005;	selectivity[1] = 0.0005;selectivity[2] = 0.005;	selectivity[3] = 0.02;
			selectivity[4] = 0.05;		selectivity[5] = 0.10;	selectivity[6] = 0.15;	selectivity[7] = 0.25;
			selectivity[8] = 0.50;		selectivity[9] = 0.95;                                 // oct - 2012
//				selectivity[0] = 0.0005;	selectivity[1] = 0.005;selectivity[2] = 0.01;	selectivity[3] = 0.02;
//				selectivity[4] = 0.05;		selectivity[5] = 0.1;	selectivity[6] = 0.2;	selectivity[7] = 0.4;
//				selectivity[8] = 0.6;		selectivity[9] = 0.95;                                 // oct - 2012

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
//			for(int i=0;i<resolution;i++)
//			System.out.println("\t"+selectivity[i]);
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
	double getOptimalCost(int index)
	{
		return this.OptimalCost[index];
	}

	int getPlanNumber_generic(int arr[])
	{
		int index = getIndex(arr,resolution);
		return plans[index];
	}
	
	public ADiagramPacket cgFpc(double threshold, ADiagramPacket gdp, String apktPath) throws IOException {

		String funName = "cgFpc";
		// First call the readApkt() function
		readpkt(gdp);
		System.out.println("CostGreedy:");
		ADiagramPacket ngdp = new ADiagramPacket(gdp);

		int n = nPlans;

		Set[] s = new Set[n];
		boolean[] notSwallowed = new boolean[n];
		DataValues[] newData = new DataValues[data.length];

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
				//TODO : should we change this?
				if(newData[i].getCost() < data[i].getCost()){
					newData[i].setCost(data[i].getCost());
//					newData[i].setPlanNumber(data[i].getPlanNumber());
				}

			}
		}
		ngdp.setDataPoints(newData);
		//setInfoValues(data, newData);


		// to test the data in newData---------------------------------------------

		/*if the new data actually contain plans whose is within the threshold*/
		for(int i=0;i<data.length;i++){
			if(newData[i].getCost() > (1+threshold/100)*data[i].getCost())
				System.out.println(funName+" ERROR: exceeding threshold at "+i);
		}

		/*to test the FPC functionality*/
		int fpc_count=0;
		for(int i=0;i<data.length;i++){		
			int p = data[i].getPlanNumber();
			double c1= data[i].getCost(); //optimal cost at i
			double c2 = AllPlanCosts[p][i]; //plan p's cost at i from pcst files
			//if(! (Math.abs(c1 - c2) < 0.05*c1 || Math.abs(c1 - c2) < 0.05*c2) ){
			if((c2-c1) > 0.05*c1){
				int [] ind = getCoordinates(dimension, resolution, i);
				System.out.printf("\nFPC ERROR: Plan: %4d, Loc(%3d, %3d,%3d): , pktCost: %10.1f, fpcOptCost: %10.1f, error: %4.2f", p, ind[0], ind[1],ind[2],c1, c2, (double)Math.abs(c1 - c2)*100/c1);
				fpc_count++;
			}
				
			//				else
			//					System.out.printf("\nFPC ERROR: Plan: %4d, Loc(%3d, %3d): (%6.4f, %6.4f), pktCost: %10.1f, fpcOptCost: %10.1f, error: %4.2f", p, i, selectivity[i], selectivity[j], c1, c2, (double)Math.abs(c1 - c2)*100/c1);
		}
		System.out.println(funName+": FPC ERROR: for 5% deviation is "+fpc_count);

		/*
		 * to print the minimum value of AllPlanCosts 
		 */
		double mincostval = Double.MAX_VALUE,maxcostval=Double.MIN_VALUE;
		for(int p=0;p<nPlans;p++){
			for(int l=0;l<totalPoints;l++){
				if(AllPlanCosts[p][l] < mincostval)
					mincostval = AllPlanCosts[p][l]; 
				if(AllPlanCosts[p][l] > maxcostval)
					maxcostval = AllPlanCosts[p][l]; 
			}
		}
		System.out.println("Min cost of AllPlanCosts "+mincostval+ " Max cost of AllPlanCosts "+maxcostval);
		/*to test whether we retain the original data in case of threshold=0*/
		if(threshold==(double)0){
			double count=0;
			for(int i=0;i<data.length;i++){
				if(data[i].getCost()!=newData[i].getCost())
					count++;
			}
			System.out.println("When threshold=0  newData and data files differ by"+count);
		}
		//-----------------------------------------------------------------------------
		return ngdp;
	}

	public void findingNativeMSO(){

		int [] newOptimalPlan = new int[totalPoints];
		for(int loc=0; loc < totalPoints; loc++) {
			newOptimalPlan[loc] = getPCSTOptimalPlan(loc);
		}
		
		int worstPlan[] = new int[totalPoints];
		for(int loc=0; loc < totalPoints; loc++) {
	
			worstPlan[loc] = getPCSTWorstPlan(loc);
		}
		//calculate really optimal plan at each location in the space -- because FPC costs may be different from the optimal costs
		
		double MSO = -1.0;
		double a;
		int location=0;
		for(int loc=0; loc < totalPoints; loc++)
		{
			a = AllPlanCosts[worstPlan[loc]][loc]/Math.max(1, AllPlanCosts[newOptimalPlan[loc]][loc]);
			/*
			 * TODO: Have used a sanity constant as 1 in the earlier line. 
			 * Assuming none of the plan cost less than 1
			 */
			
			if(MSO < a)
			{
				MSO = a;
				location = loc;
			}
		}
		System.out.println("\n Sumit MSO = "+MSO);
		System.out.println("\n loc:"+location+"\n Worst Value="+AllPlanCosts[worstPlan[location]][location]);
		System.out.println("\nOptimal_cost :"+AllPlanCosts[newOptimalPlan[location]][location]+"\n");

	}
  
	/*
	 * for each location get the cheapest plan using the pcst files. 
	 * This may be different from the optimizers choice due to imperfect
	 * FPC implementation
	 */
	private int getPCSTOptimalPlan(int loc) {
		
		double bestCost = Double.MAX_VALUE;
		int opt = -1;
		for(int p=0; p<nPlans; p++){
			if(bestCost > AllPlanCosts[p][loc]) {
				bestCost = AllPlanCosts[p][loc];
				opt = p;
			}
		}
		return opt;
	}
	
	private int getPCSTWorstPlan(int loc) {
		
		double worstCost = Double.MIN_VALUE;
		int opt = -1;
		for(int p=0; p<nPlans; p++){
			if(worstCost < AllPlanCosts[p][loc]) {
				worstCost = AllPlanCosts[p][loc];
				opt = p;
			}
		}
		return opt;
	}
	public void loadPropertiesFile() {

		Properties prop = new Properties();
		InputStream input = null;
	 
		try {
	 
			input = new FileInputStream("./src/Constants.properties");
	 
			// load a properties file
			prop.load(input);
	 
			// get the property value and print it out
			apktPath = prop.getProperty("apktPath");
			qtName = prop.getProperty("qtName");
//			varyingJoins = prop.getProperty("varyingJoins");
//			JS_multiplier1 = Integer.parseInt(prop.getProperty("JS_multiplier1"));
//			JS_multiplier2 = Integer.parseInt(prop.getProperty("JS_multiplier2"));
//			query = "explain analyze FPC(\"customer\")  (\"10000.0\") select c_custkey, c_name,	l_extendedprice * (1 - l_discount) as revenue, c_acctbal, n_name, c_address, c_phone, c_comment from customer, orders, lineitem,	nation where c_custkey = o_custkey and l_orderkey = o_orderkey and o_orderdate between '1993-01-20' and '1994-01-01' 	and c_nationkey = n_nationkey  order by	revenue desc";
			//query = prop.getProperty("query");
			cardinalityPath = prop.getProperty("cardinalityPath");
			
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}


 
}


