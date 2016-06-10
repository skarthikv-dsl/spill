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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.ibm.db2.jcc.b.ac;

import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;


public class GCI3D
{
	static int UNI = 1;
	static int EXP = 2;

	double err = 0.0;

	double AllPlanCosts[][];
	int nPlans;
	int plans[];
	double OptimalCost[];
	int totalPlans;
	int dimension;
	int resolution;
	DataValues[] data;
	int totalPoints;
	double selectivity[];


	//The following parameters has to be set manually for each query
	static String apktPath;
	static String qtName ;
	static String varyingJoins;
	static double JS_multiplier [];
	static String query;
	static String cardinalityPath;

	double  minIndex [];
	double  maxIndex [];
	static boolean MSOCalculation = true;
	static boolean randomPredicateOrder = false;

	//Settings: 
	static int sel_distribution; 
	static boolean FROM_CLAUSE;
	static Connection conn = null;
	static int database_conn;

	static ArrayList<Integer> remainingDim;
	static ArrayList<ArrayList<Integer>> allPermutations = new ArrayList<ArrayList<Integer>>();
	ArrayList<point_generic> all_contour_points = new ArrayList<point_generic>();
	static ArrayList<Integer> learntDim = new ArrayList<Integer>();
	static HashMap<Integer,Integer> learntDimIndices = new HashMap<Integer,Integer>();
	HashMap<Integer,ArrayList<point_generic>> ContourPointsMap = new HashMap<Integer,ArrayList<point_generic>>();
	static ArrayList<Pair<Integer>> executions = new ArrayList<Pair<Integer>>();  
	static HashMap<Index,ArrayList<Double>> sel_for_point = new HashMap<Index,ArrayList<Double>>();
	static double learning_cost = 0;
	static double oneDimCost = 0;
	static int no_executions = 0;
	static int no_repeat_executions = 0;
	static int max_no_executions = 0;
	static int max_no_repeat_executions = 0;

	static boolean [] already_visited;  

	double[] actual_sel;

	//for ASO calculation 
	static double planCount[], planRelativeArea[];
	static double picsel[], locationWeight[];

	static double areaSpace =0,totalEstimatedArea = 0;




	public static void main(String args[]) throws IOException, SQLException
	{
		
		final long startTime = System.currentTimeMillis();

		GCI3D obj = new GCI3D();

		obj.loadPropertiesFile();
		String pktPath = apktPath + qtName + ".apkt" ;
		System.out.println("Query Template: "+qtName);


		ADiagramPacket gdp = obj.getGDP(new File(pktPath));

		//		CostGreedyGCI3D cg = new CostGreedyGCI3D();
		//		cg.run(threshold, gdp,apktPath);
		//		ADiagramPacket reducedgdp = cg.cgFpc(threshold, gdp,apktPath);

		//Populate the OptimalCost Matrix.
		obj.readpkt(gdp, true);

		//Populate the selectivity Matrix.
		obj.loadSelectivity();
		//obj.loadPropertiesFile();
		obj.checkGradientAssumption();
		
		int i;
		double h_cost = obj.getOptimalCost(obj.totalPoints-1);
		double min_cost = obj.getOptimalCost(0);
		double ratio = h_cost/min_cost;
		assert (h_cost >= min_cost) : "maximum cost is less than the minimum cost";
		System.out.println("the ratio of C_max/c_min is "+ratio);

		i = 1;

		//to generate contours
		remainingDim.clear(); 
		for(int d=0;d<obj.dimension;d++){
			remainingDim.add(d);
		}

		learntDim.clear();
		learntDimIndices.clear();
		double cost = obj.getOptimalCost(0);
		getAllPermuations(remainingDim,0);
		assert (allPermutations.size() == obj.factorial(obj.dimension)) : "all the permutations are not generated";

		while(cost < 2*h_cost)
		{
			if(cost>h_cost)
				cost = h_cost;
			System.out.println("---------------------------------------------------------------------------------------------\n");
			System.out.println("Contour "+i+" cost : "+cost+"\n");
			obj.all_contour_points.clear();
			for(ArrayList<Integer> order:allPermutations){
				System.out.println("Entering the order"+order);
				learntDim.clear();
				learntDimIndices.clear();
				obj.getContourPoints(order,cost);
			}

			int size_of_contour = obj.all_contour_points.size();
			obj.ContourPointsMap.put(i, new ArrayList<point_generic>(obj.all_contour_points)); //storing the contour points
			System.out.println("Size of contour"+size_of_contour );
			cost = cost*2;
			i = i+1;
		}

		/*
		 * Setting up the DB connection to Postgres/TPCH/TPCDS Benchmark. 
		 */
		try{
			System.out.println("entered DB conn1");
			Class.forName("org.postgresql.Driver");

			//Settings
			//System.out.println("entered DB conn2");
			if(database_conn==0){
				conn = DriverManager
						.getConnection("jdbc:postgresql://localhost:5431/tpch-ai",
								"sa", "database");
			}
			else{
				System.out.println("entered DB tpcds");
				conn = DriverManager
						.getConnection("jdbc:postgresql://localhost:5432/tpcds",
								"sa", "database");

			}
			System.out.println("Opened database successfully");
		}
		catch ( Exception e ) {
			System.out.println("entered DB err");
			System.err.println( e.getClass().getName()+": "+ e.getMessage() );

		}
		/*
		 * running the spillBound and plan bouquet algorithm 
		 */
		double MSO =0, ASO = 0,anshASO = 0,SO=0,MaxHarm=-1*Double.MAX_VALUE,Harm=Double.MIN_VALUE;
		int ASO_points=0;
		//int max_point = 0; /*not to execute the spillBound algorithm*/
		int max_point = 1; /*to execute a specific q_a */
		//Settings
		if(MSOCalculation)
			max_point = obj.totalPoints;
		double[] subOpt = new double[max_point];
		for (int  j = 0; j < max_point ; j++)
		{
			System.out.println("Entering loop "+j);

//					if(j==4)
//						System.out.println("Interesting");
//					else
//						continue;
			//initiliazation for every loop
			double algo_cost =0;
			SO =0;
			cost = obj.getOptimalCost(0);
			obj.initialize(j);
			int[] index = obj.getCoordinates(obj.dimension, obj.resolution, j);
			//		if(index[0]%5 !=0 || index[1]%5!=0)
			//			continue;
			//Settings:
			//		obj.actual_sel[0] = 0.02;obj.actual_sel[1] = 0.99;obj.actual_sel[2] = 0.1;obj.actual_sel[3] = 0.99; /*uncomment for single execution*/

			for(int d=0;d<obj.dimension;d++) obj.actual_sel[d] = obj.findNearestSelectivity(obj.actual_sel[d]);
			if(obj.cost_generic(obj.convertSelectivitytoIndex(obj.actual_sel))<10000)
				continue;
			//----------------------------------------------------------
			i =1;
			executions.clear();
			while(i<=obj.ContourPointsMap.size() && !obj.remainingDim.isEmpty())
			{	

				if(cost<(double)10000){
					cost *= 2;
					i++;
					continue;
				}
				assert (cost<=2*h_cost) : "cost limit exceeding";
				if(cost>h_cost)
					cost=h_cost;
				System.out.println("---------------------------------------------------------------------------------------------\n");
				System.out.println("Contour "+i+" cost : "+cost+"\n");
				int prev = obj.remainingDim.size();

//				if(prev==1){
//					obj.oneDimensionSearch(i,cost);
//					learning_cost = oneDimCost;
//				}
//				else
//					obj.spillBoundAlgo(i);

				int present = obj.remainingDim.size();
				if(present < prev - 1 || present > prev)
					System.out.println("ERROR");

				algo_cost = algo_cost+ (learning_cost);
				if(present == prev ){    // just to see whether any dimension was learnt
					// if no dimension is learnt then move on to the next contour

					/*
					 * capturing the properties of the query execution
					 */
					System.out.println("In Contour "+i+" has the following details");
					System.out.println("No of executions is "+no_executions);
					System.out.println("No of repeat moves is "+no_repeat_executions);
					if(no_executions>max_no_executions)
						max_no_executions = no_executions;
					if(no_repeat_executions > max_no_repeat_executions)
						max_no_repeat_executions = no_repeat_executions;


					cost = cost*2;  
					i = i+1;
					executions.clear();
					/*
					 * initialize the repeat moves and executions for the next contour
					 */
					for(int d=0;d<obj.dimension;d++){
						already_visited[d] = false;
						no_executions = 0;
						no_repeat_executions =0;
					}

				}

				System.out.println("---------------------------------------------------------------------------------------------\n");

			}  //end of while

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
			subOpt[j] = SO;
			
			ASO += SO;
			ASO_points++;
			anshASO += SO*locationWeight[j];
			if(SO>MSO)
				MSO = SO;
			System.out.println("\nSpillBound The SubOptimaility  is "+SO);
			System.out.println("\nSpillBound Harm  is "+Harm);
		} //end of for
		//Settings
		//obj.writeSuboptToFile(subOpt, apktPath);
		conn.close();
		System.out.println("SpillBound The MaxSubOptimaility  is "+MSO);
		System.out.println("SpillBound Anshuman average Suboptimality is "+(double)anshASO);
		System.out.println("SpillBound The AverageSubOptimaility  is "+(double)ASO/ASO_points);

		final long endTime = System.currentTimeMillis();
		System.out.println("The total time taken is (in mins) "+(endTime-startTime)/(1000*60));
	}

	private int factorial(int num) {

		int factorial = 1;
		for(int i=num;i>=1;i--){
			factorial *= i; 
		}
		return factorial;
	}

	
	public void checkGradientAssumption() {

		int violation_count =0;
		int [] coordinates = new int[dimension];
		for(int i=0;i<totalPoints;i++){
			coordinates = getCoordinates(dimension, resolution, i);
			int loc = getIndex(coordinates, resolution);
			for(int j=0;j<dimension;j++){
				if(coordinates[j]<dimension-1){
					coordinates[j]++;
					int loc_new = getIndex(coordinates, resolution);
					double costr = (double)(OptimalCost[loc_new]/OptimalCost[loc]);
					double selr = (double)(selectivity[coordinates[j]]/selectivity[coordinates[j]-1]);
					System.out.println("The cost at new location is "+OptimalCost[loc_new]+" with sel= "+selectivity[coordinates[j]]+" and cost at old location "+OptimalCost[loc]+" with sel = "+selectivity[coordinates[j]-1]);
					if(costr  > selr)
						violation_count++;
					coordinates[j]--;
				}
			}
		}
		System.out.println("the violation count is "+violation_count);
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

	

	private int getContourNumber(double cost) {
		// TODO Auto-generated method stub
		 int c_no = (int) (Math.floor((Math.log10(cost/getOptimalCost(0))/Math.log10(2)))+1);
		 
		 assert((cost <= 1.01* (Math.pow(2, c_no-1)*getOptimalCost(0))) || (cost >= 0.99* (Math.pow(2, c_no-1)*getOptimalCost(0)))): "problem in get contour number function";
		 return c_no;
	}

	public void initialize(int location) {

		String funName = "initialize";
		//updating the feasible region
		for(int i=0;i<dimension;i++){
			minIndex[i] =  findNearestSelectivity(0);
			maxIndex[i] = findNearestSelectivity(0.99);
		}
		//updating the remaining dimensions data structure
		remainingDim.clear();
		for(int i=0;i<dimension;i++)
			remainingDim.add(i);


		learning_cost = 0;
		oneDimCost = 0;
		no_executions = 0;
		no_repeat_executions = 0;
		max_no_executions = 0;
		max_no_repeat_executions =0;
		already_visited = new boolean[dimension];
		for(int i =0;i<dimension;i++)
			already_visited[i] = false;
		//updating the actual selectivities for each of the dimensions
		int index[] = getCoordinates(dimension, resolution, location);

		actual_sel = new double[dimension];
		for(int i=0;i<dimension;i++){
			actual_sel[i] = selectivity[index[i]];
		}


		//sanity check conditions
		assert(remainingDim.size() == dimension): funName+"ERROR: mismatch in remaining Dimensions";

		/*
		 * reload the order list before the start of  every contour
		 */
		for(int i=1;i<=ContourPointsMap.size();i++){
			for(int j=0;j<ContourPointsMap.get(i).size();j++){
				point_generic p = ContourPointsMap.get(i).get(j);
				p.reloadOrderList(remainingDim);
				assert(p.getPredicateOrder().size() == dimension) : " reLoading predicate Order not done properly";
			}
		}

	}


	private static void getAllPermuations(ArrayList<Integer> DimOrder,int k) {


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
		String funName = "getContourPoints";
		//learntDim contains the dimensions already learnt (which is null initially)
		//learntDimIndices contains the exact point in the space for the learnt dimensions

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

			assert (learntDim.size() == learntDimIndices.size()) : funName+" : learnt dimension data structures size not matching";
			assert (last_dim>=0 && last_dim<dimension) :funName+ " : index problem ";
			assert (remainingDimList.size() + learntDim.size() == dimension) : funName+" : learnt dimension data structures size not matching";

			//Search the whole line and return all the points which fall into the contour
			for(int i=0;i< resolution;i++)
			{
				arr[last_dim] = i;
				double cur_val = cost_generic(arr);
				double targetval = cost;

				if(cur_val == targetval)
				{
					if(!pointAlreadyExist(arr)){ //No need to check if the point already exist
						//if(true){								// its okay to have redundancy
						point_generic p;

						/*
						 * The following If condition checks whether any earlier point in all_contour_points 
						 * had the same plan. If so, no need to open the .../predicateOrder/plan.txt again
						 */

						if(planVisited(getPlanNumber_generic(arr))!=null)
							p = new point_generic(arr,getPlanNumber_generic(arr),cur_val, remainingDim,planVisited(getPlanNumber_generic(arr)).getPredicateOrder());
						else
							p = new point_generic(arr,getPlanNumber_generic(arr),cur_val, remainingDim);
						all_contour_points.add(p);

					}
				}
				else if(i!=0){
					arr[last_dim]--; //in order to look at the cost at lower value on the last index
					double cur_val_l = cost_generic(arr);
					arr[last_dim]++; //restore the index back 
					if( cur_val > targetval  && cur_val_l < targetval ) //NOTE : changed the inequality to strict inequality
					{
						if(!pointAlreadyExist(arr)){ //No need to check if the point already exist
							//if(true){								// its okay to have redundancy
							point_generic p; 
							if(planVisited(getPlanNumber_generic(arr))!=null)
								p = new point_generic(arr,getPlanNumber_generic(arr),cur_val, remainingDim,planVisited(getPlanNumber_generic(arr)).getPredicateOrder());
							else
								p = new point_generic(arr,getPlanNumber_generic(arr), cur_val,remainingDim);
							all_contour_points.add(p);
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

	private point_generic planVisited(int plan_no) {

		String funName = "planVisited";

		for(point_generic p: all_contour_points){
			if(p.get_plan_no()== plan_no){
				return p;
			}
		}
		return null;
	}

	private boolean pointAlreadyExist(int[] arr) {

		boolean flag = false;
		for(point_generic p: all_contour_points){
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

		/*
		 * sanity check
		 */


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
	void readpkt(ADiagramPacket gdp, boolean planCost) throws IOException
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

		}

		//To get the number of points for each plan
		int  [] plan_count = new int[totalPlans];
		for(int p=0;p<data.length;p++){
			plan_count[plans[p]]++;
		}
		//printing the above
		for(int p=0;p<plan_count.length;p++){
			System.out.println("Plan "+p+" has "+plan_count[p]+" points");
		}

		/*
		 * Reading the pcst files
		 */
		nPlans = totalPlans;
		
		if(planCost){
			AllPlanCosts = new double[nPlans][totalPoints];
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
		}

		remainingDim = new ArrayList<Integer>();
		for(int i=0;i<dimension;i++)
			remainingDim.add(i);
		minIndex = new double[dimension];
		maxIndex = new double[dimension];

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
	void loadSelectivity()
	{
		String funName = "loadSelectivity: ";
		System.out.println(funName+" Resolution = "+resolution);
		double sel;
		this.selectivity = new double [resolution];

		if(resolution == 10){
			if(sel_distribution == 0){

				//This is for TPCH queries 
				selectivity[0] = 0.0005;	selectivity[1] = 0.005;selectivity[2] = 0.01;	selectivity[3] = 0.02;
				selectivity[4] = 0.05;		selectivity[5] = 0.10;	selectivity[6] = 0.20;	selectivity[7] = 0.40;
				selectivity[8] = 0.60;		selectivity[9] = 0.95;                                   // oct - 2012
			}
			else if( sel_distribution ==1){

				//This is for TPCDS queries
				selectivity[0] = 0.00005;	selectivity[1] = 0.0005;selectivity[2] = 0.005;	selectivity[3] = 0.02;
				selectivity[4] = 0.05;		selectivity[5] = 0.10;	selectivity[6] = 0.15;	selectivity[7] = 0.25;
				selectivity[8] = 0.50;		selectivity[9] = 0.99;                                // dec - 2012
			}
			else
				assert (false) :funName+ "ERROR: should not come here";

		}		
		if(resolution == 20){

			if(sel_distribution == 0){

				selectivity[0] = 0.0005;   selectivity[1] = 0.0008;		selectivity[2] = 0.001;	selectivity[3] = 0.002;
				selectivity[4] = 0.004;   selectivity[5] = 0.006;		selectivity[6] = 0.008;	selectivity[7] = 0.01;
				selectivity[8] = 0.03;	selectivity[9] = 0.05;	selectivity[10] = 0.08;	selectivity[11] = 0.10;
				selectivity[12] = 0.200;	selectivity[13] = 0.300;	selectivity[14] = 0.400;	selectivity[15] = 0.500;
				selectivity[16] = 0.600;	selectivity[17] = 0.700;	selectivity[18] = 0.800;	selectivity[19] = 0.99;
			}
			else if( sel_distribution ==1){

				selectivity[0] = 0.00005;   selectivity[1] = 0.00008;		selectivity[2] = 0.0001;	selectivity[3] = 0.0002;
				selectivity[4] = 0.0004;   selectivity[5] = 0.0006;		selectivity[6] = 0.0008;	selectivity[7] = 0.001;
				selectivity[8] = 0.003;	selectivity[9] = 0.005;	selectivity[10] = 0.008;	selectivity[11] = 0.01;
				selectivity[12] = 0.05;	selectivity[13] = 0.1;	selectivity[14] = 0.15;	selectivity[15] = 0.25;
				selectivity[16] = 0.40;	selectivity[17] = 0.60;	selectivity[18] = 0.80;	selectivity[19] = 0.99;
			}
			else
				assert (false) :funName+ "ERROR: should not come here";


		}

		if(resolution == 30){

			if(sel_distribution == 0){
				//tpch
				selectivity[0] = 0.0005;  selectivity[1] = 0.0008;	selectivity[2] = 0.001;	selectivity[3] = 0.002;
				selectivity[4] = 0.004;   selectivity[5] = 0.006;	selectivity[6] = 0.008;	selectivity[7] = 0.01;
				selectivity[8] = 0.03;	selectivity[9] = 0.05;
				selectivity[10] = 0.07;	selectivity[11] = 0.1;	selectivity[12] = 0.15;	selectivity[13] = 0.20;
				selectivity[14] = 0.25;	selectivity[15] = 0.30;	selectivity[16] = 0.35;	selectivity[17] = 0.40;
				selectivity[18] = 0.45;	selectivity[19] = 0.50;	selectivity[20] = 0.55;	selectivity[21] = 0.60;
				selectivity[22] = 0.65;	selectivity[23] = 0.70;	selectivity[24] = 0.75;	selectivity[25] = 0.80;
				selectivity[26] = 0.85;	selectivity[27] = 0.90;	selectivity[28] = 0.95;	selectivity[29] = 0.99;
			}

			else if(sel_distribution == 1){
				selectivity[0] = 0.00001;  selectivity[1] = 0.00005;	selectivity[2] = 0.00010;	selectivity[3] = 0.00050;
				selectivity[4] = 0.0010;   selectivity[5] = 0.005;	selectivity[6] = 0.0100;	selectivity[7] = 0.0200;
				selectivity[8] = 0.0300;	selectivity[9] = 0.0400;	selectivity[10] = 0.0500;	selectivity[11] = 0.0600;
				selectivity[12] = 0.0700;	selectivity[13] = 0.0800;	selectivity[14] = 0.0900;	selectivity[15] = 0.1000;
				selectivity[16] = 0.1200;	selectivity[17] = 0.1400;	selectivity[18] = 0.1600;	selectivity[19] = 0.1800;
				selectivity[20] = 0.2000;	selectivity[21] = 0.2500;	selectivity[22] = 0.3000;	selectivity[23] = 0.4000;
				selectivity[24] = 0.5000;	selectivity[25] = 0.6000;	selectivity[26] = 0.7000;	selectivity[27] = 0.8000;
				selectivity[28] = 0.9000;	selectivity[29] = 0.9950;
			}

			else
				assert (false) :funName+ "ERROR: should not come here";
		}

if (resolution ==40){
				if(sel_distribution==1){
					
					selectivity[0] = 0.00005;   selectivity[1] = 0.00006;		selectivity[2] = 0.00008;	selectivity[3] = 0.00009;
					selectivity[4] = 0.0001;   selectivity[5] = 0.0002;		selectivity[6] = 0.0003;	selectivity[7] = 0.0005;
					selectivity[8] = 0.0006;	selectivity[9] = 0.0007;	selectivity[10] = 0.0008;	selectivity[11] = 0.0009;
					selectivity[12] = 0.001;	selectivity[13] = 0.002;	selectivity[14] = 0.003;	selectivity[15] = 0.004;
					selectivity[16] = 0.005;	selectivity[17] = 0.006;	selectivity[18] = 0.007;	selectivity[19] = 0.008;
					selectivity[20] = 0.009;   selectivity[21] = 0.01;		selectivity[22] = 0.02;	selectivity[23] = 0.03;
					selectivity[24] = 0.04;   selectivity[25] = 0.05;		selectivity[26] = 0.06;	selectivity[27] = 0.07;
					selectivity[28] = 0.08;	selectivity[29] = 0.09;	selectivity[30] = 0.1;	selectivity[31] = 0.2;
					selectivity[32] = 0.3;	selectivity[33] = 0.4;	selectivity[34] = 0.5;	selectivity[35] = 0.6;
					selectivity[36] = 0.7;	selectivity[37] = 0.8;	selectivity[38] = 0.9;	selectivity[39] = 0.99;
				}
			else
				assert (false) :funName+ "ERROR: should not come here";
			}

		if(resolution==100){

			if(sel_distribution == 1){
				selectivity[0] = 0.000064; 	selectivity[1] = 0.000093; 	selectivity[2] = 0.000126; 	selectivity[3] = 0.000161; 	selectivity[4] = 0.000198;
				selectivity[5] = 0.000239; 	selectivity[6] = 0.000284; 	selectivity[7] = 0.000332; 	selectivity[8] = 0.000384; 	selectivity[9] = 0.000440;
				selectivity[10] = 0.000501; 	selectivity[11] = 0.000567; 	selectivity[12] = 0.000638; 	selectivity[13] = 0.000716; 	selectivity[14] = 0.000800;
				selectivity[15] = 0.000890; 	selectivity[16] = 0.000989; 	selectivity[17] = 0.001095; 	selectivity[18] = 0.001211; 	selectivity[19] = 0.001335;
				selectivity[20] = 0.001471; 	selectivity[21] = 0.001617; 	selectivity[22] = 0.001776; 	selectivity[23] = 0.001948; 	selectivity[24] = 0.002134;
				selectivity[25] = 0.002335; 	selectivity[26] = 0.002554; 	selectivity[27] = 0.002790; 	selectivity[28] = 0.003046; 	selectivity[29] = 0.003323;
				selectivity[30] = 0.003624; 	selectivity[31] = 0.003949; 	selectivity[32] = 0.004301; 	selectivity[33] = 0.004683; 	selectivity[34] = 0.005096;
				selectivity[35] = 0.005543; 	selectivity[36] = 0.006028; 	selectivity[37] = 0.006552; 	selectivity[38] = 0.007121; 	selectivity[39] = 0.007736;
				selectivity[40] = 0.008403; 	selectivity[41] = 0.009125; 	selectivity[42] = 0.009907; 	selectivity[43] = 0.010753; 	selectivity[44] = 0.011670;
				selectivity[45] = 0.012663; 	selectivity[46] = 0.013739; 	selectivity[47] = 0.014904; 	selectivity[48] = 0.016165; 	selectivity[49] = 0.017531;
				selectivity[50] = 0.019011; 	selectivity[51] = 0.020613; 	selectivity[52] = 0.022348; 	selectivity[53] = 0.024228; 	selectivity[54] = 0.026263;
				selectivity[55] = 0.028467; 	selectivity[56] = 0.030854; 	selectivity[57] = 0.033440; 	selectivity[58] = 0.036240; 	selectivity[59] = 0.039272;
				selectivity[60] = 0.042556; 	selectivity[61] = 0.046113; 	selectivity[62] = 0.049965; 	selectivity[63] = 0.054136; 	selectivity[64] = 0.058654;
				selectivity[65] = 0.063547; 	selectivity[66] = 0.068845; 	selectivity[67] = 0.074584; 	selectivity[68] = 0.080799; 	selectivity[69] = 0.087530;
				selectivity[70] = 0.094819; 	selectivity[71] = 0.102714; 	selectivity[72] = 0.111263; 	selectivity[73] = 0.120523; 	selectivity[74] = 0.130550;
				selectivity[75] = 0.141411; 	selectivity[76] = 0.153172; 	selectivity[77] = 0.165910; 	selectivity[78] = 0.179705; 	selectivity[79] = 0.194645;
				selectivity[80] = 0.210825; 	selectivity[81] = 0.228348; 	selectivity[82] = 0.247325; 	selectivity[83] = 0.267877; 	selectivity[84] = 0.290136;
				selectivity[85] = 0.314241; 	selectivity[86] = 0.340348; 	selectivity[87] = 0.368621; 	selectivity[88] = 0.399241; 	selectivity[89] = 0.432403;
				selectivity[90] = 0.468316; 	selectivity[91] = 0.507211; 	selectivity[92] = 0.549334; 	selectivity[93] = 0.594953; 	selectivity[94] = 0.644359;
				selectivity[95] = 0.697865; 	selectivity[96] = 0.755812; 	selectivity[97] = 0.818569; 	selectivity[98] = 0.886535; 	selectivity[99] = 0.990142;

			}
			else if(sel_distribution == 0){
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

			else
				assert (false) :funName+ "ERROR: should not come here";
		}
		//the selectivity distribution
		//System.out.println("The selectivity distribution using is ");
		//			for(int i=0;i<resolution;i++)
		//			System.out.println("\t"+selectivity[i]);
	}


	public ADiagramPacket getGDP(File file) {
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




	public boolean inFeasibleRegion(double [] arr) {
		boolean flag = true;
		for(int d=0;d<dimension;d++){
			if(!(minIndex[d]<=arr[d] && arr[d]<=maxIndex[d])){
				flag = false;
				break;
			}
		}
		return flag; 			
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
			varyingJoins = prop.getProperty("varyingJoins");

			JS_multiplier = new double[dimension];
			for(int d=0;d<dimension;d++){
				String multiplierStr = new String("JS_multiplier"+(d+1));
				JS_multiplier[d] = Double.parseDouble(prop.getProperty(multiplierStr));
			}

			//Settings:Note dont forget to put analyze here
			//query = "explain analyze FPC(\"lineitem\") (\"104949\")  select	supp_nation,	cust_nation,	l_year,	volume from	(select n1.n_name as supp_nation, n2.n_name as cust_nation, 	DATE_PART('YEAR',l_shipdate) as l_year,	l_extendedprice * (1 - l_discount) as volume	from	supplier, lineitem, orders, 	customer, nation n1,	nation n2 where s_suppkey = l_suppkey	and o_orderkey = l_orderkey and c_custkey = o_custkey		and s_nationkey = n1.n_nationkey and c_nationkey = n2.n_nationkey	and  c_acctbal<=10000 and l_extendedprice<=22560 ) as temp";
			//query = "explain analyze FPC(\"catalog_sales\")  (\"150.5\") select ca_zip, cs_sales_price from catalog_sales,customer,customer_address,date_dim where cs_bill_customer_sk = c_customer_sk and c_current_addr_sk = ca_address_sk and cs_sold_date_sk = d_date_sk and ca_gmt_offset <= -7.0   and d_year <= 1900  and cs_list_price <= 150.5";
			query = prop.getProperty("query");

			cardinalityPath = prop.getProperty("cardinalityPath");


			int from_clause_int_val = Integer.parseInt(prop.getProperty("FROM_CLAUSE"));

			if(from_clause_int_val == 1)
				FROM_CLAUSE = true;
			else
				FROM_CLAUSE = false;

			/*
			 * 0 for sel_distribution is used for tpch type queries
			 */
			sel_distribution = Integer.parseInt(prop.getProperty("sel_distribution"));

			/*
			 * 0 for database_conn is used for tpch type queries
			 */
			database_conn = Integer.parseInt(prop.getProperty("database_conn"));

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}


	public int getPCSTOptimalPlan(int loc) {

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

	public int getPCSTWorstPlan(int loc) {

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



}



class point_generic
{
	int dimension;

	ArrayList<Integer> order;
	ArrayList<Integer> storedOrder;
	int value;
	int p_no;
	double cost;
	static String plansPath;

	int [] dim_values;
	point_generic(int arr[], int num, double cost,ArrayList<Integer> remainingDim) throws  IOException{

		loadPropertiesFile();
		System.out.println();
		dim_values = new int[dimension];
		for(int i=0;i<dimension;i++){
			dim_values[i] = arr[i];
			System.out.print(arr[i]+",");
		}
		System.out.println("   having cost = "+cost+" and plan "+num);
		this.p_no = num;
		this.cost = cost;

		order =  new ArrayList<Integer>();
		storedOrder = new ArrayList<Integer>();
		try{
			FileReader file = new FileReader(plansPath+num+".txt");

			BufferedReader br = new BufferedReader(file);
			String s;
			while((s = br.readLine()) != null) {
				//System.out.println(Integer.parseInt(s));
				value = Integer.parseInt(s);
				storedOrder.add(value);

			}
			br.close();
			file.close();
		}
		catch(FileNotFoundException e){
			if(plansPath.contains("SQL")){
				for(int i=0;i<dimension;i++){
					storedOrder.add(i);
				}
			}
		}


	}
	point_generic(int arr[], int num, double cost,ArrayList<Integer> remainingDim,ArrayList<Integer> predicateOrder ) throws  IOException{

		loadPropertiesFile();
		System.out.println();
		dim_values = new int[dimension];
		for(int i=0;i<dimension;i++){
			dim_values[i] = arr[i];
			System.out.print(arr[i]+",");
		}
		System.out.println("   having cost = "+cost+" and plan "+num);
		this.p_no = num;
		this.cost = cost;

		//check: if the order and stored order are being updated/populated
		order =  new ArrayList<Integer>(predicateOrder);
		storedOrder = new ArrayList<Integer>(predicateOrder);		
	}
	int getLearningDimension(){
		if(order.isEmpty())
			System.out.println("ERROR: all dimensions learnt");
		return order.get(0);
	}

	/*
	 * get the selectivity/index of the dimension
	 */
	public int get_dimension(int d){
		return dim_values[d];
	}

	/*
	 * get the plan number for this point
	 */
	public int get_plan_no(){
		return p_no;
	}

	public double get_cost(){
		return cost;
	}
	public int[] get_point_Index(){
		return dim_values;
	}
	public void remove_dimension(int d){
		String funName = "remove_dimension";
		if(order.isEmpty())
			System.out.println(funName+": ERROR: all dimensions learnt");
		if(order.contains(d))
			order.remove(order.indexOf(d));
		else 
			System.out.println(funName+": ERROR: removing a dimension that does not exist");

	}
	public void reloadOrderList(ArrayList<Integer> remainingDim) {

		order.clear();
		for(int itr=0;itr<storedOrder.size();itr++){
			if(remainingDim.contains(storedOrder.get(itr)))
				order.add(storedOrder.get(itr));
		}
	}
	public int get_no_of_dimension(){
		return dimension;
	}

	public ArrayList<Integer> getPredicateOrder(){
		return storedOrder;
	}

	public void loadPropertiesFile() {

		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream("./src/Constants.properties");

			// load a properties file
			prop.load(input);

			// get the property value and print it out
			plansPath = prop.getProperty("apktPath");
			plansPath = plansPath+"predicateOrder/";
			dimension = Integer.parseInt(prop.getProperty("dimension"));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

}

final class Pair<T> {

	final T left;
	final T right;

	public Pair(T left, T right)
	{
		if (left == null || right == null) { 
			throw new IllegalArgumentException("left and right must be non-null!");
		}
		this.left = left;
		this.right = right;
	}

	public boolean equals(Object o)
	{
		// see @maaartinus answer
		if (! (o instanceof Pair)) { return false; }
		Pair p = (Pair)o;
		return left.equals(p.left) && right.equals(p.right);
	} 

	public int hashCode()
	{
		return 7 * left.hashCode() + 13 * right.hashCode();
	} 
}

class Index {

	point_generic  p;
	Integer contour;
	Integer dim;

	public Index(point_generic  p, int contour, Integer dim) {
		this.p = p;
		this.contour = contour;
		this.dim = dim;
	}

	  @Override
	    public int hashCode() {
		  String s = new String(Integer.toString(dim) + Integer.toString(contour));
		  for(int i=0;i<p.dimension;i++)
			  s = s + Integer.toString(p.get_dimension(i));
		  return s.hashCode();
	    }

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Index other = (Index) obj;
		for(int i=0;i<p.dim_values.length;i++){
			if(p.get_dimension(i) != other.p.get_dimension(i))
				return false;
		}
		if (Integer.valueOf(contour) != Integer.valueOf(other.contour))
			return false;
		if(this.dim != other.dim)
			return false;
		return true;
	}
}
