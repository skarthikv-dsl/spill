///*
// * For Multi-D !!
// *  This Program generates the Virtual Contours using the gradient assumption.
// *  It also gives the number of optimizations required for getting all the contours.
// *  
// *  -- Pseudocode --
// *  Input : QT's apkt file
// *  		Cost of the contour to be find out.
// *  
// *  Output : Set of points in the plan diagram(and Plans at those points) which covers a specified contour.
// *  
// *  Algorithm :
// *  
// *  Parse the apkt file to get the resolution, selectivities and cost at each point.
// *  Fix Alpha, e -> Error due to discretization..
// *  x_min = minimum Selectivity of the dimension
// *  While x_act >=1
// *  	x_act = x_min
// *  	do binary search to find y_act, s.t Cost(x_act, y_act) = [C-e, C+e]
// *  	if(alpha*x_min is not greater than 1)
// *  		Put the point (alpha*x_min, y_act) to the output list.
// *  	else
// *  		put(1,y_act) to output list.
// *  	x_act = alpha*x_act;
// *  	loop
// *  
// *  -- Pseudocode Ends --
// *  
// *  Keep track of number of optimizations been called.
// *  Keep track of the total investment benifit
// *  	Call a function which actually gets the contours and gets the total investment.(Brute force case)
// *  
// * */
//// ---------------------------------------------------------------------
///*
// * Doubts :
// *  1. Whether the apkt and pkt files are same ??
// *  
// * */
//
//
//
//import java.awt.Dimension;
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.ObjectInputStream;
//import java.io.PrintWriter;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.SQLException;
//import java.sql.Statement;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Properties;
//import java.util.Random;
////import java.util.Set;
//import java.util.Comparator;
//import java.util.concurrent.*;
//
//import org.apache.commons.lang3.builder.CompareToBuilder;
//
//import iisc.dsl.picasso.common.ds.DataValues;
//import iisc.dsl.picasso.server.ADiagramPacket;
//
//import org.apache.log4j.Logger;
//
//public class CostGreedyGCI3D
//{
//	static double AllPlanCosts[][];
//	static int nPlans;
//
//	static int UNI = 1;
//	static int EXP = 2;
//
//	static double err = 0.03;//no use
//	//Settings
//	static double threshold = 20;
//
//	static int plans[];
//	static double OptimalCost[];
//
//	int totalPlans;
//	int dimension;
//	int resolution;
//	DataValues[] data;
//	int totalPoints;
//	double selectivity[];
//
//
//
//	//The following parameters has to be set manually for each query
//	static String apktPath;
//	static String qtName ;
//	static String cardinalityPath;
//
//	static int sel_distribution;
//	static boolean MSOCalculation = true;
//	static Connection conn = null;
//
//	ArrayList<Integer> remainingDim; //change
//	static ArrayList<ArrayList<Integer>> allPermutations = new ArrayList<ArrayList<Integer>>();
//	 ArrayList<point_generic> all_contour_points = new ArrayList<point_generic>();//change
//	 ArrayList<Integer> learntDim = new ArrayList<Integer>();//change
//		//static ArrayList<Integer> learntDimIndices = new ArrayList<Integer>();
//	 static HashMap<Integer,Integer> learntDimIndices = new HashMap<Integer,Integer>();
//	 
//	 static HashMap<Integer,ArrayList<point_generic>> ContourPointsMap = new HashMap<Integer,ArrayList<point_generic>>();
//	 HashMap<Integer,Integer> uniquePlansMap = new HashMap<Integer,Integer>();//change
//	 static HashMap<Integer,Double> minContourCostMap = new HashMap<Integer,Double>();
//	 
//	 double learning_cost = 0;//change
//	 boolean done = false;
//	 
//	 double[] actual_sel;
//	 
//	 //for ASO calculation 
//	 static double planCount[], planRelativeArea[];//change
//	 static float picsel[], locationWeight[];
//
//	 static double areaSpace =0,totalEstimatedArea = 0;
//
//	 final static Logger logger = Logger.getLogger(CostGreedyGCI3D.class);
//	 
//	 public CostGreedyGCI3D() {}
//	 public CostGreedyGCI3D(CostGreedyGCI3D another) {
//		 this.totalPlans = another.totalPlans;
//		 this.dimension = another.dimension;
//		 this.resolution = another.resolution;
//		 this.totalPoints = another.totalPoints;
//		 this.data = another.data;
//		 this.learning_cost = another.learning_cost;
//		 this.remainingDim = another.remainingDim;
//		 this.selectivity = another.selectivity;
//	 }
//
//
//
//
//	 
//	public static void main(String args[]) throws IOException, SQLException, InterruptedException, ExecutionException
//	{
//		long start = System.currentTimeMillis();;
//		CostGreedyGCI3D obj = new CostGreedyGCI3D();
//		obj.loadPropertiesFile();
//		String pktPath = apktPath + qtName + ".apkt" ;
//		System.out.println("Query Template: "+qtName);
//
//
//		ADiagramPacket gdp = obj.getGDP(new File(pktPath));
//
//
////		cg.run(threshold, gdp,apktPath);
//		ADiagramPacket reducedgdp = obj.cgFpc(threshold, gdp,apktPath);
//
//		//Settings
//		//Populate the OptimalCost Matrix.
//		//obj.readpkt(gdp);
//		obj.readpkt(reducedgdp);
//
//		//Populate the selectivity Matrix.
//		obj.loadSelectivity();
//
//		//Calculate Native MSO
//		obj.findingNativeMSO();
//
//		int i;
//		double h_cost = obj.getOptimalCost(obj.totalPoints-1);
//		double min_cost = obj.getOptimalCost(0);
//		double ratio = h_cost/min_cost;
//	//	System.out.println("-------------------------  ------\n"+qtName+"    alpha="+alpha+"\n-------------------------  ------"+"\n"+"Highest Cost ="+h_cost+", \nRatio of highest cost to lowest cost ="+ratio);
//		System.out.println("the ratio of C_max/c_min is "+ratio);
//
//		i = 1;
//
//		obj.remainingDim.clear(); 
//		for(int d=0;d<obj.dimension;d++){
//			obj.remainingDim.add(d);
//		}
//		obj.learntDim.clear();
//
//		learntDimIndices.clear();
//		double cost = obj.getOptimalCost(0);
//		//cost*=2;
//		getAllPermuations(obj.remainingDim,0);
//
//		assert (allPermutations.size() == obj.factorial(obj.dimension)) : "all the permutations are not generated";
//
//		while(cost < 2*h_cost)
//		{
//
//			if(cost>h_cost)
//				cost = h_cost;
//			System.out.println("---------------------------------------------------------------------------------------------\n");
//			System.out.println("Contour "+i+" cost : "+cost+"\n");
//
//			obj.all_contour_points.clear();
//			//final_points = new ArrayList<point_generic>();
//			for(ArrayList<Integer> order:allPermutations){
//				System.out.println("Entering the order"+order);
//				obj.learntDim.clear();
//				learntDimIndices.clear();
//				obj.getContourPoints(order,cost,i);
//			}
//			//obj.writeContourPointstoFile(i);
//			int size_of_contour = obj.all_contour_points.size();
//			obj.ContourPointsMap.put(i, new ArrayList<point_generic>(obj.all_contour_points)); //storing the contour points
//			System.out.println("Size of contour"+size_of_contour );
//				cost = cost*2;
//				i = i+1;
//		}
//
//		System.out.println("The minimum cost contour map is "+obj.minContourCostMap);
//
//		/*
//		 * running the plan bouquet algorithm 
//		 */
//
//		obj.getPlanCountArray();
//
//		double MSO =0, ASO = 0,SO=0,anshASO = 0,MaxHarm=-1*Double.MAX_VALUE,Harm=Double.MIN_VALUE;
//		int ASO_points=0;
//
//		//int max_point = 0; /*to not execute the spillBound algorithm*/
//		int max_point = 1; /*to execute a specific q_a */
//		//Settings
//		if(MSOCalculation)
//		//if(false)
//			max_point = obj.totalPoints;
//		double[] subOpt = new double[max_point];
//
//		boolean singleThread = false;
//
//		if(singleThread)
//		{
//		for (int  j = 0; j < max_point ; j++)
//		  {
//			System.out.println("Entering loop "+j);
//
////			if(j!=totalPoints-1)
////				continue;
//			//initialization for every loop
//			double algo_cost =0;
//			SO =0;
//			cost = obj.getOptimalCost(0);
//			obj.initialize(j);
//			int[] index = obj.getCoordinates(obj.dimension, obj.resolution, j);
////			if(index[0]%5 !=0 || index[1]%5!=0)
////				continue;
////			obj.actual_sel[0] = 0.31;obj.actual_sel[1] = 0.3;obj.actual_sel[2] = 0.6; /*uncomment for single execution*/
//
//			for(int d=0;d<obj.dimension;d++) obj.actual_sel[d] = obj.findNearestSelectivity(obj.actual_sel[d]);
//			if(obj.cost_generic(obj.convertSelectivitytoIndex(obj.actual_sel))<10000 && !apktPath.contains("SQL"))
//				continue;
//			//----------------------------------------------------------
//			i =1;
//			while(i<=obj.ContourPointsMap.size() && !obj.done)
//			{
//				if(cost<(double)10000 && !apktPath.contains("SQL")){
//					cost *= 2;
//					i++;
//					continue;
//				}
//				assert (cost<=2*h_cost) : "cost limit exceeding";
//
//				if(cost>h_cost)
//					cost=h_cost;
//				System.out.println("---------------------------------------------------------------------------------------------\n");
//				System.out.println("Contour "+i+" cost : "+cost+"\n");
//				obj.sortContourPoints(i);
//
//				obj.planBouquetAlgo(i,cost);
//
//				algo_cost = algo_cost+ (obj.learning_cost);
//				System.out.println("The current algo_cost is "+algo_cost);
//				System.out.println("The cost expended in this contour is "+obj.learning_cost);
//				cost = cost*2;  
//				i = i+1;
//				System.out.println("---------------------------------------------------------------------------------------------\n");
//
//			}  //end of while
//
//			assert(obj.done) : "In Main done variable not true even when the while loop is broken out";
////
////			/*
////			 * printing the actual selectivity
////			 */
//			System.out.print("\nThe actual selectivity is original \t");
//			for(int d=0;d<obj.dimension;d++) 
//				System.out.print(obj.actual_sel[d]+",");
//
//			obj.calculateMSOBound();
////			/*
////			 * storing the index of the actual selectivities. Using this printing the
////			 * index (an approximation) of actual selectivities and its cost
////			 */
//			int [] index_actual_sel = new int[obj.dimension]; 
//			for(int d=0;d<obj.dimension;d++) index_actual_sel[d] = obj.findNearestPoint(obj.actual_sel[d]);
//
//			System.out.print("\nCost of actual_sel ="+obj.cost_generic(index_actual_sel)+" at ");
//			for(int d=0;d<obj.dimension;d++) System.out.print(index_actual_sel[d]+",");
//
//			SO = (algo_cost/obj.cost_generic(index_actual_sel));
//			SO = SO * (1 + threshold/100);
//			subOpt[j] = SO;
//			Harm = obj.maxHarmCalculation(j, SO);
//			if(Harm > MaxHarm)
//				MaxHarm = Harm;
//			ASO += SO;
//			ASO_points++;
//			anshASO += SO*locationWeight[j];
//			if(SO>MSO)
//				MSO = SO;
//			System.out.println("\nSpillBound The SubOptimaility  is "+SO);
//			System.out.println("\nSpillBound Harm  is "+Harm);
//		  } //end of for
////		  	//Settings
//		  	obj.writeSuboptToFile(subOpt, apktPath);
//		  
//		  	System.out.println("SpillBound The MaxSubOptimaility  is "+MSO);
//		  	System.out.println("SpillBound The MaxHarm  is "+MaxHarm);
//		  	System.out.println("SpillBound Anshuman average Suboptimality is "+(double)anshASO);
//		  	System.out.println("SpillBound The AverageSubOptimaility  is "+(double)ASO/ASO_points);
//
//			System.out.println("---------------------------------------------------------------------------------------");
//		}
//		else
//		{
//		max_point = 1; 
//		if(MSOCalculation)
//			//if(false)
//				max_point = obj.totalPoints;
//
//		List<PlanBouquetinputParamStruct> inputs = new ArrayList<PlanBouquetinputParamStruct>();
//		for (int j = 0; j < max_point; ++j) {
//			PlanBouquetinputParamStruct input = new PlanBouquetinputParamStruct(new CostGreedyGCI3D(obj), j, h_cost);
//			inputs.add(input);
//		}
//
//		int threads = Runtime.getRuntime().availableProcessors();
//		System.out.println("Available Cores " + threads);
//	    ExecutorService service = Executors.newFixedThreadPool(threads);
//	    List<Future<PlanBouquetOutputParamStruct>> futures = new ArrayList<Future<PlanBouquetOutputParamStruct>>();
//	    for (final PlanBouquetinputParamStruct input : inputs) {
//	        Callable<PlanBouquetOutputParamStruct> callable = new Callable<PlanBouquetOutputParamStruct>() {
//	            public PlanBouquetOutputParamStruct call() throws Exception {
//	            	PlanBouquetOutputParamStruct output = new PlanBouquetOutputParamStruct();
//	            	System.out.println("Begin execution loop "+ input.index);
//	            	int j = input.index;
//	            	CostGreedyGCI3D obj = input.obj;
//            
////	        		if(j!=totalPoints-1)
////	        			continue;
//	        		//initialization for every loop
//	        		double algo_cost =0;
//	        		double SO =0;
//	        		double cost = obj.getOptimalCost(0);
//	        		double h_cost = input.h_cost;
//	        
//	        		obj.initialize(j);
//	        		int[] index = obj.getCoordinates(obj.dimension, obj.resolution, j);
////	        		if(index[0]%5 !=0 || index[1]%5!=0)
////	        			continue;
////	        		obj.actual_sel[0] = 0.31;obj.actual_sel[1] = 0.3;obj.actual_sel[2] = 0.6; /*uncomment for single execution*/
//	        
//	        		for(int d=0;d<obj.dimension;d++) obj.actual_sel[d] = obj.findNearestSelectivity(obj.actual_sel[d]);
//	        		if(obj.cost_generic(obj.convertSelectivitytoIndex(obj.actual_sel))<10000 && !apktPath.contains("SQL"))
//	        		{
//	        			output.flag = false;
//	        			return output;
//	        		}
//	        		//----------------------------------------------------------
//	        		int i =1;
//	        		while(i<=obj.ContourPointsMap.size() && !obj.done)
//	        		{
//	        			if(cost<(double)10000 && !apktPath.contains("SQL")){
//	        				cost *= 2;
//	        				i++;
//	        				continue;
//	        			}
//	        			//assert (cost<=2*h_cost) : "cost limit exceeding";
//	        
//	        			if(cost>h_cost)
//	        				cost=h_cost;
//	        			//System.out.println("---------------------------------------------------------------------------------------------\n");
//	        			//System.out.println("Contour "+i+" cost : "+cost+"\n");
//	        			obj.sortContourPoints(i);
//
//	        			obj.planBouquetAlgo(i,cost);
//	        
//	        			algo_cost = algo_cost+ (obj.learning_cost);
//	        			//System.out.println("The current algo_cost is "+algo_cost);
//	        			//System.out.println("The cost expended in this contour is "+learning_cost);
//	        			cost = cost*2;  
//	        			i = i+1;
//	        			//System.out.println("---------------------------------------------------------------------------------------------\n");
//
//	        		}  //end of while
//	        
//	        		//assert(done) : "In Main done variable not true even when the while loop is broken out";
//	        
//	        		/*
//	        		 * printing the actual selectivity
//	        		 */
//	        		//System.out.print("\nThe actual selectivity is original \t");
//	        		//for(int d=0;d<obj.dimension;d++) 
//	        			//System.out.print(obj.actual_sel[d]+",");
//	        
//	        		obj.calculateMSOBound();
//	        		/*
//	        		 * storing the index of the actual selectivities. Using this printing the
//	        		 * index (an approximation) of actual selectivities and its cost
//	        		 */
//	        		int [] index_actual_sel = new int[obj.dimension]; 
//	        		for(int d=0;d<obj.dimension;d++) index_actual_sel[d] = obj.findNearestPoint(obj.actual_sel[d]);
//	        
//	        		//System.out.print("\nCost of actual_sel ="+obj.cost_generic(index_actual_sel)+" at ");
//	        		//for(int d=0;d<obj.dimension;d++) System.out.print(index_actual_sel[d]+",");
//
//	        		SO = (algo_cost/obj.cost_generic(index_actual_sel));
//	        		SO = SO * (1 + threshold/100);
//	        		output.SO = SO;	        
//	        		output.Harm = obj.maxHarmCalculation(j, SO);
//	        
//	        
//	        		output.anshSO = SO*locationWeight[j];
//	        		System.out.println("\nSpillBound The SubOptimaility  is "+SO);
//	        		System.out.println("\nSpillBound Harm  is "+output.Harm);
//	        		System.out.println("End execution loop "+ input.index);
//	        		if(input.index %1000 == 0)
//	        			System.out.println("End execution loop "+ input.index);
//
//	        		System.out.println();
//	                return output;
//	            }
//	        };
//	        futures.add(service.submit(callable));
//	    }
//
//	    service.shutdown();
//	    
//	    MSO =0;
//		ASO = 0;
//		anshASO = 0;
//		MaxHarm=-1*Double.MAX_VALUE;
//		ASO_points=0;
//
//		int j=0;
//	    for (Future<PlanBouquetOutputParamStruct> future : futures) {
//	    	PlanBouquetOutputParamStruct output = future.get();
//	    	if(output.flag)
//	    	{
//	    		subOpt[j] = output.SO;
//	    		if(output.SO>MSO)
//	    			MSO = output.SO;
//	    		if(output.Harm > MaxHarm)
//	    			MaxHarm = output.Harm;
//	    		ASO += output.SO;
//	    		ASO_points++;
//	    		anshASO += output.anshSO;
//	    		j++;
//	    	}
//	    }
//	    
//	    System.out.println("SpillBound The MaxSubOptimaility  is "+MSO);
//	    System.out.println("SpillBound The MaxHarm  is "+MaxHarm);
//	    System.out.println("SpillBound Anshuman average Suboptimality is "+(double)anshASO);
//	    System.out.println("SpillBound The AverageSubOptimaility  is "+(double)ASO/ASO_points);
//		}
//		long end = System.currentTimeMillis();;
//
//		System.out.println((end - start) + " ms");
//	}
//
//	private void calculateMSOBound() {
//
//		 double algo_cost = 0;
//		 double max_pb_so = Double.MIN_VALUE;
//		 double cost = OptimalCost[0];
//		 int skip =0;
//		 for(int i=1;i<=uniquePlansMap.size();i++){
//			 if(cost<(double)10000 && !apktPath.contains("SQL")){
//					cost *= 2;
//					skip++;
//					i--;
//					continue;
//				}
//			 else if(cost>OptimalCost[totalPoints-1])
//				 cost = OptimalCost[totalPoints-1];
//			 algo_cost += Math.pow(2,i-1)*uniquePlansMap.get(i+skip);
//			 if(i>1){
//			 double so = algo_cost/Math.pow(2,i-2);
//			 if(so > max_pb_so)
//				 max_pb_so = so;
//			 }
//		 }
//		 System.out.println("PB MSO is "+max_pb_so*(1+threshold/100));
//	}
//
//	private void sortContourPoints(int contour_no) {
//
//		 String funName  = "sortContourPoints";
//		 
//		 Collections.sort(ContourPointsMap.get(contour_no), new pointComparator());
//	}
//
//	 public void writeSuboptToFile(double[] subOpt,String path) throws IOException {
//
//		 //settings
//	       File file = new File(path+"planBouquet_20"+"suboptimality"+".txt");
//		    if (!file.exists()) {
//		        file.createNewFile();
//		    }
//		    FileWriter writer = new FileWriter(file, false);
//		    PrintWriter pw = new PrintWriter(writer);
//
//		    
//		    for(int loc=0;loc<totalPoints;loc++){
//		    	int[] index = getCoordinates(dimension, resolution, loc);
//		    	for(int d=0;d<dimension;d++){
//		    		pw.print(index[d]+",");
//		    	}
//		    	pw.print("\t is "+subOpt[loc]+"\n");
//		    }
////			for(int i =0;i<resolution;i++){
////				for(int j=0;j<resolution;j++){
////					//Settings
////					for(int k=0;k<resolution;k++){
////					//if(i%5==0 && j%5==0){
////						int [] index = new int[3];
////						index[0] = i;
////						index[1] = j;
////						index[2] = k;
////						int ind = getIndex(index, resolution);
////						if(j!=0)
////							pw.print("\t"+subOpt[ind]);
////						else
////							pw.print(subOpt[ind]);
////					//}
////					}
////				}
////				//if(i%2==0)
////					//pw.print("\n");
////			}
//			pw.close();
//			writer.close();
//
//		}
//	 
//	 private int factorial(int num) {
//
//		 int factorial = 1;
//		for(int i=num;i>=1;i--){
//			factorial *= i; 
//		}
//		return factorial;
//	}
//
//	 
//
//public  void getPlanCountArray() {
//	planCount = new double[totalPlans];
//	locationWeight = new float[data.length];
//	// Set dim depending on whether we are dealing with full packet or slice
//	// int dim;
//
//
//	int resln = resolution;
//	int dim = dimension;
//
//	int[] r = new int[dim];
//
//	for (int i = 0; i < dim; i++)
//		r[i] = resln;
//
//	/*
//	 * if(gdp.getDimension()==1) dim = 1; else if(data.length > r[0] * r[1])
//	 * //full packet { dim = dim; //set to actual number of dimensions }
//	 * else //slice { if(getDimension()==1) dim = 1; else dim = 2; //if
//	 * actual dimension >= 2 }
//	 */
//
//
//		double locationWeightLocal[] = new double[resln];
//
//		if(resolution==10 ){
//			if(sel_distribution==0){
//				//for tpch
//				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1;
//				locationWeightLocal[3] = 2;         locationWeightLocal[4] = 4;				locationWeightLocal[5] = 7;
//				locationWeightLocal[6] = 15;        locationWeightLocal[7] = 20;				locationWeightLocal[8] = 30;
//				locationWeightLocal[9] = 20;
//			}
//
//			//for tpcds
//			if(sel_distribution == 1){
//				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1;
//				locationWeightLocal[3] = 2;         locationWeightLocal[4] = 4;				locationWeightLocal[5] = 6;
//				locationWeightLocal[6] = 7;        locationWeightLocal[7] = 25;				locationWeightLocal[8] = 30;
//				locationWeightLocal[9] = 20;
//			}
//		}
//
//		else if (resolution == 30){
//			if(sel_distribution == 0){
//				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1;
//				locationWeightLocal[3] = 1;         locationWeightLocal[4] = 1;				locationWeightLocal[5] = 1;
//				locationWeightLocal[6] = 1;        locationWeightLocal[7] = 1;				locationWeightLocal[8] = 1;
//				locationWeightLocal[9] = 2;
//				locationWeightLocal[10] = 3;			locationWeightLocal[11] = 3;				locationWeightLocal[12] = 5;
//				locationWeightLocal[13] = 5;         locationWeightLocal[14] = 5;				locationWeightLocal[15] = 5;
//				locationWeightLocal[16] = 5;        locationWeightLocal[17] = 5;				locationWeightLocal[18] = 5;
//				locationWeightLocal[19] = 5;
//				locationWeightLocal[20] = 5;			locationWeightLocal[21] = 5;				locationWeightLocal[22] = 5;
//				locationWeightLocal[23] = 5;         locationWeightLocal[24] = 5;				locationWeightLocal[25] = 5;
//				locationWeightLocal[26] = 5;        locationWeightLocal[27] = 5;				locationWeightLocal[28] = 5;
//				locationWeightLocal[29] = 2;
//			}
//
//			if(sel_distribution == 1){
//				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1;
//				locationWeightLocal[3] = 1;         locationWeightLocal[4] = 1;				locationWeightLocal[5] = 1;
//				locationWeightLocal[6] = 1;        locationWeightLocal[7] = 1;				locationWeightLocal[8] = 1;
//				locationWeightLocal[9] = 2;
//				locationWeightLocal[10] = 3;			locationWeightLocal[11] = 3;				locationWeightLocal[12] = 4;
//				locationWeightLocal[13] = 4;         locationWeightLocal[14] = 4;				locationWeightLocal[15] = 4;
//				locationWeightLocal[16] = 4;        locationWeightLocal[17] = 4;				locationWeightLocal[18] = 4;
//				locationWeightLocal[19] = 4;
//				locationWeightLocal[20] = 4;			locationWeightLocal[21] = 6;				locationWeightLocal[22] = 6;
//				locationWeightLocal[23] = 6;         locationWeightLocal[24] = 6;				locationWeightLocal[25] = 6;
//				locationWeightLocal[26] = 6;        locationWeightLocal[27] = 6;				locationWeightLocal[28] = 6;
//				locationWeightLocal[29] = 5;
//			}
//		}
//			else if (resolution == 20){
//				if(sel_distribution == 0){
//					locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1;
//					locationWeightLocal[3] = 1;         locationWeightLocal[4] = 1;				locationWeightLocal[5] = 1;
//					locationWeightLocal[6] = 1;        locationWeightLocal[7] = 4;				locationWeightLocal[8] = 4;
//					locationWeightLocal[9] = 5;
//					locationWeightLocal[10] = 5;			locationWeightLocal[11] = 5;				locationWeightLocal[12] = 5;
//					locationWeightLocal[13] = 6;         locationWeightLocal[14] = 6;				locationWeightLocal[15] = 10;
//					locationWeightLocal[16] = 10;        locationWeightLocal[17] = 10;				locationWeightLocal[18] = 15;
//					locationWeightLocal[19] = 10;
//
//				}
//
//				if(sel_distribution == 1){
//					locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1;
//					locationWeightLocal[3] = 1;         locationWeightLocal[4] = 1;				locationWeightLocal[5] = 1;
//					locationWeightLocal[6] = 1;        locationWeightLocal[7] = 1;				locationWeightLocal[8] = 4;
//					locationWeightLocal[9] = 4;
//					locationWeightLocal[10] = 5;			locationWeightLocal[11] = 5;				locationWeightLocal[12] = 5;
//					locationWeightLocal[13] = 6;         locationWeightLocal[14] = 6;				locationWeightLocal[15] = 10;
//					locationWeightLocal[16] = 10;        locationWeightLocal[17] = 15;				locationWeightLocal[18] = 15;
//					locationWeightLocal[19] = 10;
//				}
//
//		}
//
//			else if (resolution==100){
//				for(int i=0;i<resolution;i++){
//					locationWeightLocal[i] = 1;
//				}
//			}
//
//		for (int loc=0; loc < data.length; loc++)
//		{
//			if(OptimalCost[loc]>=(double)10000){
//				double weight = 1.0;
//				int tempLoc = loc;
//				for(int d=0;d<dim;d++){
//					weight *= locationWeightLocal[tempLoc % resln];
//					tempLoc = tempLoc/resln;
//				}
//
//				locationWeight[loc] = (float) weight;
//				planCount[data[loc].getPlanNumber()] += weight;
//			}
//			else
//				locationWeight[loc] = (float) -1;
//
//		}
//
//		double totalWeight = 0,sumWeight=0;
//
//	for (int i = 0; i < data.length; i++) {
//		if(locationWeight[i]>=0)
//			totalWeight += locationWeight[i];
//	}
//
//	for (int i = 0; i < data.length; i++) {
//		if(locationWeight[i]>=0){
//			locationWeight[i] /= totalWeight;
//			sumWeight += locationWeight[i];
//		}
//
//		assert (locationWeight[i]<= (double)1) : "In getPlanCountArray: locationWeight is not less than 1";
////		if(locationWeight[i]>(double)1){
////			System.out.println("In getPlanCountArray: locationWeight is not less than 1");
////			System.out.println("location weight : "+locationWeight[i]+" at i="+i+" total weight="+totalWeight);
////		}
//	}
//
//	System.out.println("The sum weight is "+sumWeight);
//	assert(sumWeight<=(double)1.01) : "In getPlanCountArray: sumWeight is not less than 1";
//
//
//	/*
//	 * if(scaleupflag) { for(int i = 0; i < planCount.length; i++)
//	 * planCount[i] /= 100Math.pow(10, getDimension()); }
//	 */
//
//
//	checkValidityofWeights();
//
//}
//	 
//	 
//	public void planBouquetAlgo(int contour_no, double cost) {
//
//		String funName = "planBouquetAlgo";
//
//		double last_exec_cost = 0;
//		learning_cost =0;
//		int [] arr = new int[dimension];
//		HashSet<Integer> unique_plans = new HashSet();
//		int unique_points =0;
//		double max_cost =0 , min_cost = Double.MAX_VALUE;
//
//		for(int c=0;c< ContourPointsMap.get(contour_no).size();c++){
//
//			point_generic p = ContourPointsMap.get(contour_no).get(c);
//
//			/*needed for testing the code*/
//			unique_points ++;
//			if(p.get_cost()>max_cost)
//				max_cost = p.get_cost();
//			if(p.get_cost() < min_cost)
//				min_cost = p.get_cost();
//
//			/*
//			 * to check if p dominates actual selectivity
//			 */
//			boolean flag = true;
//			for(int d=0;d<dimension;d++){
//				if(p.get_dimension(d) < findNearestPoint(actual_sel[d]) ){
//					flag = false;
//					break;
//				}
//			}
//
//
//
//			for(int d=0;d<dimension;d++){
//				arr[d] = p.get_dimension(d);
//				//System.out.print(arr[d]+",");
//			}
//
//			if(!unique_plans.contains(getPlanNumber_generic(arr))){
//
//				//if(cost_generic(convertSelectivitytoIndex(actual_sel)) < 2*cost)
//				if(true)
//					learning_cost += minContourCostMap.get(contour_no);
//				else 
//					learning_cost += cost;
//
//				//Settings: learning_cost += p.get_cost();  changed to include only the contour cost and not the point
//				unique_plans.add(getPlanNumber_generic(arr));
//				//last_exec_cost = cost;
////				if(cost_generic(convertSelectivitytoIndex(actual_sel)) < 2*cost)
//				if(true)
//					last_exec_cost = minContourCostMap.get(contour_no);
//				else 
//					last_exec_cost = cost;
//			}
//			if(flag == true){
//				if(cost_generic(convertSelectivitytoIndex(actual_sel)) >= 2*cost)
//					flag = false;
//			}
//
//			if(!flag && cost_generic(convertSelectivitytoIndex(actual_sel)) < 2*cost)
//				flag = checkFPC(p.get_plan_no(),contour_no);
//
//			if(flag){
//				done = true;
//				 System.out.println("The number unique points are "+unique_points);
//				 System.out.println("The number unique plans are "+unique_plans.size());
//				 System.out.println("The  unique plans are "+unique_plans);
//				 //System.out.print("The final execution cost is "+p.get_cost()+ "at :" );
//				 if(!uniquePlansMap.containsKey(contour_no))
//					 uniquePlansMap.put(contour_no, unique_plans.size());
//				 else if(uniquePlansMap.get(contour_no)<unique_plans.size() && uniquePlansMap.containsKey(contour_no)){
//					 uniquePlansMap.remove(contour_no);
//					 uniquePlansMap.put(contour_no, unique_plans.size());
//				 }
//				//Settings:  changed to include only the contour cost and not the point
////				 if(p.get_cost() > last_exec_cost ){
////					 learning_cost -= last_exec_cost;
////					 learning_cost += p.get_cost();
////				 }
//				 learning_cost -= last_exec_cost;
//				 int [] int_actual_sel = new int[dimension];
//				 for(int d=0;d<dimension;d++)
//					 int_actual_sel[d] = findNearestPoint(actual_sel[d]);
//				 double oneDimCost=0;
////				 if(cost_generic(convertSelectivitytoIndex(actual_sel)) < 2*cost)
//				 if(true)
//						oneDimCost += minContourCostMap.get(contour_no);
//					else 
//						oneDimCost += cost;
//				 
//				 if(fpc_cost_generic(int_actual_sel, p.get_plan_no())<oneDimCost)
//					 oneDimCost = fpc_cost_generic(int_actual_sel, p.get_plan_no());
//				 if(cost_generic(int_actual_sel)> oneDimCost)
//					 oneDimCost = cost_generic(int_actual_sel);
//				 learning_cost  += oneDimCost;
//	 
//				 for(int d=0;d<dimension;d++){
//						System.out.print(arr[d]+",");
//					}
//				//assert (unique_points <= Math.pow(resolution, dimension-1)) : funName+" : total points is execeeding the max possible points";
////				if(cost_generic(convertSelectivitytoIndex(actual_sel)) < 2*cost)
//				if(true)
//					assert (learning_cost <= (unique_plans.size()+1)*minContourCostMap.get(contour_no)*1.01) : funName+" : learning cost exceeding its cap";
//				else 
//					assert (learning_cost <= (unique_plans.size()+1)*cost*1.01) : funName+" : learning cost exceeding its cap";
//
//				return;
//			}
//		}
//		 //assert (unique_points <= Math.pow(resolution, dimension-1)) : funName+" : total points is execeeding the max possible points";
//
//		if(!uniquePlansMap.containsKey(contour_no))
//			 uniquePlansMap.put(contour_no, unique_plans.size());
//		 else if(uniquePlansMap.get(contour_no)<unique_plans.size() && uniquePlansMap.containsKey(contour_no)){
//			 uniquePlansMap.remove(contour_no);
//			 uniquePlansMap.put(contour_no, unique_plans.size());
//		 }
//
//		 System.out.println("The number of unique points are "+unique_points);
//		 System.out.println("The number of unique plans are "+unique_plans.size());
//		 System.out.println("The  unique plans are "+unique_plans);
//		 System.out.println("Contour No. is "+contour_no+" : Max cost is "+max_cost+" and min cost is "+min_cost+" with learning cost "+learning_cost);
////		 if(cost_generic(convertSelectivitytoIndex(actual_sel)) < 2*cost)
//		 if(true)
//				assert (learning_cost <= (unique_plans.size()+1)*minContourCostMap.get(contour_no)*1.01) : funName+" : learning cost exceeding its cap";
//			else 
//				assert (learning_cost <= (unique_plans.size()+1)*cost*1.01) : funName+" : learning cost exceeding its cap";
//
//	}
//
//
//	private boolean checkFPC(int plan_no, int contour_no) {
//
//		int last_contour = (int)(Math.ceil(Math.log(cost_generic(convertSelectivitytoIndex(actual_sel))/OptimalCost[0])/Math.log(2)));
//		last_contour++;
//		double cost_q_a = cost_generic(convertSelectivitytoIndex(actual_sel));
//		double budget = Math.pow(2,last_contour-1)*OptimalCost[0];
//		if(budget>OptimalCost[totalPoints-1])
//			budget = OptimalCost[totalPoints-1];
//		if(last_contour==contour_no && cost_q_a!=budget){
//			if(cost_q_a<=OptimalCost[totalPoints-1])
//				assert(budget>cost_q_a):" last contour cost is less than actual selectivity cost";
//			if(fpc_cost_generic(convertSelectivitytoIndex(actual_sel), plan_no)<budget){
//				return true;
//			}
//			else
//				return false;
//		}
//		else 
//			return false;
//	}
//
//	public  void checkValidityofWeights() {
//		double areaPlans =0;
//		double relativeAreaPlans =0;
//		areaSpace = 0.0;
//
//		planRelativeArea = new double[totalPlans];
//
//		for (int i=0; i< data.length; i++){
//			areaSpace += locationWeight[i];
//		}
//		//	System.out.println(areaSpace);
//
//		for (int i=0; i< totalPlans; i++){
//			planRelativeArea[i] = planCount[i]/areaSpace;
//			relativeAreaPlans += planRelativeArea[i];
//			areaPlans += planCount[i];
//		}
//		//	System.out.println(areaPlans);
//		//	System.out.println(relativeAreaPlans);
//
//		if(relativeAreaPlans < 0.99) {
//			System.out.println("ALERT! The area of plans add up to only " + relativeAreaPlans);
//			//System.exit(0);
//		}
//	}
//
//
//	public double maxHarmCalculation(int loc,double SO){
//		int bestPlan  = getPCSTOptimalPlan(loc);
//		double bestCost = AllPlanCosts[bestPlan][loc];
//		int worstPlan = getPCSTWorstPlan(loc);
//		double worstCost = AllPlanCosts[worstPlan][loc];
//		double worst_native = (worstCost/bestCost);
//		if(worst_native<1)
//			System.out.println("MaxharmCalculation : error ");
//		return (SO/worst_native - 1);
//		//assert that this ratio is > 1
//	}
//
//private double[] convertIndextoSelectivity(int[] point_Index) {
//
//	String funName = "convertIndextoSelectivity";
//
//	double [] point_selec = new double[point_Index.length];
//	assert (point_Index.length == dimension): funName+" ERROR: point index length not matching with dimension"; 
//	for(int d=0; d<dimension;d++)
//		point_selec[d] = selectivity[point_Index[d]];
//	return point_selec;
//}
//private int[] convertSelectivitytoIndex (double[] point_sel) {
//
//	String funName = "convertSelectivitytoIndex";
//
//	int [] point_index = new int[point_sel.length];
//	assert (point_sel.length == dimension): funName+" ERROR: point index length not matching with dimension"; 
//	for(int d=0; d<dimension;d++)
//		point_index[d] = findNearestPoint(point_sel[d]);
//	return point_index;
//}
//
//
//public void initialize(int location) {
//
//	String funName = "intialize";
//
//	//updating the remaining dimensions data structure
//	remainingDim = new ArrayList<Integer>();
//	for(int i=0;i<dimension;i++)
//		remainingDim.add(i);
//
//	learning_cost = 0;
//	done = false;
//	//updating the actual selectivities for each of the dimensions
//	int index[] = getCoordinates(dimension, resolution, location);
//
//	actual_sel = new double[dimension];
//	for(int i=0;i<dimension;i++){
//		actual_sel[i] = selectivity[index[i]];
//	}
//
//
//	//sanity check conditions
//	assert(remainingDim.size() == dimension): funName+"ERROR: mismatch in remaining Dimensions";
//
//
//}
//
//	private void writeContourPointstoFile(int contour_no) {
//
//		try {
//	    
////	    String content = "This is the content to write into file";
//
//
//         File filex = new File("/home/dsladmin/Srinivas/data/others/contours/"+"x"+contour_no+".txt"); 
//         File filey = new File("/home/dsladmin/Srinivas/data/others/contours/"+"y"+contour_no+".txt"); 
//         //File filez = new File("/home/dsladmin/Srinivas/data/others/contours/"+"z"+contour_no+".txt"); 
//	    // if file doesn't exists, then create it
//	    if (!filex.exists()) {
//	        filex.createNewFile();
//	    }
//	    if (!filey.exists()) {
//	        filey.createNewFile();
//	    }
////	    if (!filez.exists()) {
////	        filez.createNewFile();
////	    }
//
//	    FileWriter writerax = new FileWriter(filex, false);
////	    FileWriter writeraz = new FileWriter(filez, false);
//	    FileWriter writeray = new FileWriter(filey, false);
//
//	    
//	    PrintWriter pwax = new PrintWriter(writerax);
//	    PrintWriter pway = new PrintWriter(writeray);
////	    PrintWriter pwaz = new PrintWriter(writeraz);
//	    //Take iterator over the list
//	    for(point_generic p : this.all_contour_points) {
//		    //        System.out.println(p.getX()+":"+p.getY()+": Plan ="+p.p_no);
//	   	 pwax.print((int)p.get_dimension(0) + "\t");
//	   	 pway.print((int)p.get_dimension(1)+ "\t");
////	   	pwaz.print((int)p.get_dimension(2)+ "\t");
//	   	 
//	    }
//	    pwax.close();
//	    pway.close();
////	    pwaz.close();
//	    writerax.close();
//	    writeray.close();
////	    writeraz.close();
//	    
//		} catch (IOException e) {
//	    e.printStackTrace();
//	}
//
//	}
//
//
//	private static void getAllPermuations(ArrayList<Integer> DimOrder,int k) {
//
//	 //   static void permute(int[] a, int k) 
//		if (k == DimOrder.size()) 
//		{	ArrayList<Integer> tempList = new ArrayList<Integer>();
//			for (int i = 0; i <  DimOrder.size(); i++) 
//			{
//				tempList.add(DimOrder.get(i));
//			}
//			allPermutations.add(tempList);
//		} 
//		else 
//		{
//			for (int i = k; i < DimOrder.size(); i++) 
//			{
//				int temp = DimOrder.get(k);
//				DimOrder.set(k, DimOrder.get(i));
//				DimOrder.set(i,temp);
//
//				getAllPermuations(DimOrder, k + 1);
//
//				temp = DimOrder.get(k);
//				DimOrder.set(k, DimOrder.get(i));
//				DimOrder.set(i,temp);
//
//			}
//
//		}
//
//	}
//
//
//
//
//	void getContourPoints(ArrayList<Integer> order,double cost, int contour_no) throws IOException
//	{
//		String funName = "getContourPoints";
//		//learntDim contains the dimensions already learnt (which is null initially)
//		//learntDimIndices contains the exact point in the space for the learnt dimensions
//
//		ArrayList<Integer> remainingDimList = new ArrayList<Integer>();
//		for(int i=0;i<order.size();i++)
//		{
//			if(learntDim.contains(order.get(i))!=true)
//			{
//				remainingDimList.add(order.get(i));
//			}
//		}
//
//		if(remainingDimList.size()==1 )
//		{ 
//			int last_dim=-1;
//			int [] arr = new int[dimension];
//
//			for(int i=0;i<dimension;i++)
//			{
//				if(learntDim.contains(i))
//				{
//					arr[i] = learntDimIndices.get(i);
//					//System.out.print(arr[i]+",");
//				}
//				else
//					last_dim = i;
//			}
//
//			assert (learntDim.size() == learntDimIndices.size()) : funName+" : learnt dimension data structures size not matching";
//			assert (last_dim>=0 && last_dim<dimension) :funName+ " : index problem ";
//			assert (remainingDimList.size() + learntDim.size() == dimension) : funName+" : learnt dimension data structures size not matching";
//
//			//Search the whole line and return all the points which fall into the contour
//			for(int i=0;i< resolution;i++)
//			{
//				arr[last_dim] = i;
//				double cur_val = cost_generic(arr);
//				double targetval = cost;
//
//				if(cur_val == targetval)
//				{
//					if(!pointAlreadyExist(arr)){ //No need to check if the point already exist
//					//if(true){								// its okay to have redundancy
//						point_generic p;
//
//						/*
//						 * The following If condition checks whether any earlier point in all_contour_points 
//						 * had the same plan. If so no need to open the .../predicateOrder/plan.txt again
//						 */
//
//						if(planVisited(getPlanNumber_generic(arr))!=null)
//							p = new point_generic(arr,getPlanNumber_generic(arr),cur_val, remainingDim,planVisited(getPlanNumber_generic(arr)).getPredicateOrder());
//						else
//							p = new point_generic(arr,getPlanNumber_generic(arr),cur_val, remainingDim);
//
//						if(minContourCostMap.containsKey(contour_no) && p.get_cost()<minContourCostMap.get(contour_no)){
//							minContourCostMap.remove(contour_no);
//							minContourCostMap.put(contour_no, p.get_cost());
//						}
//						else if(!minContourCostMap.containsKey(contour_no))
//							minContourCostMap.put(contour_no, p.get_cost());
//
//						all_contour_points.add(p);
//
//					}
//				}
//				else if(i!=0){
//					arr[last_dim]--; //in order to look at the cost at lower value on the last index
//					double cur_val_l = cost_generic(arr);
//					arr[last_dim]++; //restore the index back 
//					if( cur_val > targetval  && cur_val_l < targetval ) //NOTE : changed the inequality to strict inequality
//					{
//						if(!pointAlreadyExist(arr)){ //check if the point already exist
//						//if(true){
//							point_generic p; 
//							if(planVisited(getPlanNumber_generic(arr))!=null)
//								p = new point_generic(arr,getPlanNumber_generic(arr),cur_val, remainingDim,planVisited(getPlanNumber_generic(arr)).getPredicateOrder());
//							else
//								p = new point_generic(arr,getPlanNumber_generic(arr), cur_val,remainingDim);
//
//							if(minContourCostMap.containsKey(contour_no) && p.get_cost()<minContourCostMap.get(contour_no)){
//								minContourCostMap.remove(contour_no);
//								minContourCostMap.put(contour_no, p.get_cost());
//							}
//							else if(!minContourCostMap.containsKey(contour_no))
//								minContourCostMap.put(contour_no, p.get_cost());
//
//							all_contour_points.add(p);
//
//						}
//					}
//
//				}
//			}
//
//
//			return;
//		}
//
//		Integer curDim = remainingDimList.get(0); //index of 0 or size-1 does not matter
//		Integer cur_index = resolution -1;
//
//		while(cur_index >= 0)
//		{
//			learntDim.add(curDim);
//			learntDimIndices.put(curDim,cur_index);
//			getContourPoints(order,cost,contour_no);
//			learntDim.remove(learntDim.indexOf(curDim));
//			learntDimIndices.remove(curDim);
//			cur_index = cur_index - 1;
//		}
//
//	}
//
//	private point_generic planVisited(int plan_no) {
//
//		String funName = "planVisited";
//
//		for(point_generic p: all_contour_points){
//				if(p.get_plan_no()== plan_no){
//					return p;
//				}
//		}
//		return null;
//	}
//
//	private boolean pointAlreadyExist(int[] arr) {
//
//		boolean flag = false;
//		for(point_generic p: all_contour_points){
//			flag = true;
//			for(int i=0;i<dimension;i++){
//				if(p.get_dimension(i)!= arr[i]){
//					flag = false;
//					break;
//				}
//			}
//			if(flag==true)
//				return true;
//		}
//
//		return false;
//	}
//
//
//
//
//
//	// Function which does binary search to find the actual point !!
//// Return the index near to the selecitivity=mid;
//	public int findNearestPoint(double mid)
//	{
//		int i;
//		int return_index = 0;
//		double diff;
//		if(mid >= selectivity[resolution-1])
//			return resolution-1;
//		for(i=0;i<resolution;i++)
//		{
//			diff = mid - selectivity[i];
//			if(diff <= 0)
//			{
//				return_index = i;
//				break;
//			}
//		}
//		//System.out.println("return_index="+return_index);
//		return return_index;
//	}
//
//
//	// Return the selectivity just greater than selecitivity=mid;
//		public double findNearestSelectivity(double mid)
//		{
//			int i;
//			int return_index = 0;
//			double diff;
//			if(mid >= selectivity[resolution-1])
//				return selectivity[resolution-1];
//			for(i=0;i<resolution;i++)
//			{
//				diff = mid - selectivity[i];
//				if(diff <= 0)
//				{
//					return_index = i;
//					break;
//				}
//			}
//			//System.out.println("return_index="+return_index);
//			return selectivity[return_index];
//		}
//
//		/*-------------------------------------------------------------------------------------------------------------------------
//		 * Populates -->
//		 * 	dimension
//		 * 	resolution
//		 * 	totalPoints
//		 * 	OptimalCost[][]
//		 * 
//		 * */
//		void readpkt(ADiagramPacket gdp) throws IOException
//		{
//			//ADiagramPacket gdp = getGDP(new File(pktPath));
//			totalPlans = gdp.getMaxPlanNumber();
//			dimension = gdp.getDimension();
//			resolution = gdp.getMaxResolution();
//			data = gdp.getData();
//			totalPoints = (int) Math.pow(resolution, dimension);
//			//System.out.println("\nthe total plans are "+totalPlans+" with dimensions "+dimension+" and resolution "+resolution);
//
//			assert (totalPoints==data.length) : "Data length and the resolution didn't match !";
//
//			plans = new int [data.length];
//			OptimalCost = new double [data.length]; 
//			for (int i = 0;i < data.length;i++)
//			{
//				OptimalCost[i]= data[i].getCost();
//				plans[i] = data[i].getPlanNumber();
//				//System.out.println("At "+i+" plan is "+plans[i]);
//				//System.out.println("Plan Number ="+plans[i]+"\n");
//				//	System.out.println("\nOptimal_cost:["+i+"] ="+OptimalCost[i]+"\n");
//			}
//
//			//TO get the number of points for each plan
//			int  [] plan_count = new int[totalPlans];
//			for(int p=0;p<data.length;p++){
//				plan_count[plans[p]]++;
//			}
//			//printing the above
//			for(int p=0;p<plan_count.length;p++){
//				System.out.println("Plan "+p+" has "+plan_count[p]+" points");
//			}
//
//			 remainingDim = new ArrayList<Integer>();
//				for(int i=0;i<dimension;i++)
//					remainingDim.add(i);
//
//				// ------------------------------------- Read pcst files
//				nPlans = totalPlans;
//				AllPlanCosts = new double[nPlans][totalPoints];
//				//costBouquet = new double[total_points];
//				for (int i = 0; i < nPlans; i++) {
//					try {
//
//						ObjectInputStream ip = new ObjectInputStream(new FileInputStream(new File(apktPath + i + ".pcst")));
//						double[] cost = (double[]) ip.readObject();
//						for (int j = 0; j < totalPoints; j++)
//						{
//
//							AllPlanCosts[i][j] = cost[j];
//						}
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				}
//
//			//writing the optimal cost matirx
//
//			/*	File file = new File("C:\\Lohit\\data\\plans\\"+i+".txt");
//			    if (!file.exists()) {
//					file.createNewFile();
//				}
//
//			    FileWriter writer = new FileWriter(file, false);
//
//			    PrintWriter pw = new PrintWriter(writer);
//
//			for(int i=0;i<resolution;i++){
//				for(int j=0;j<resolution;j++){
//					int[] indexp = {j,i};
//					pw.format("%f\t", OptimalCost[getIndex(indexp, resolution)]);;
//				}
//				pw.format("\n");
//			}
//			 */
//
//		}
//
//
//	 double cost_generic(int arr[])
//		{
//		 
//			int index = getIndex(arr,resolution);
//
//
//			return OptimalCost[index];
//		}
//	 double fpc_cost_generic(int arr[], int plan)
//		{
//		 
//			int index = getIndex(arr,resolution);
//
//
//			return AllPlanCosts[plan][index];
//		}
//
//	//-------------------------------------------------------------------------------------------------------------------
//		/*
//		 * Populates the selectivity Matrix according to the input given
//		 * */
//		void loadSelectivity()
//		{
//			String funName = "loadSelectivity: ";
//			System.out.println(funName+" Resolution = "+resolution);
//			double sel;
//			this.selectivity = new double [resolution];
//
//			if(resolution == 10){
//				if(sel_distribution == 0){
//
//					//This is for TPCH queries 
//					selectivity[0] = 0.0005;	selectivity[1] = 0.005;selectivity[2] = 0.01;	selectivity[3] = 0.02;
//					selectivity[4] = 0.05;		selectivity[5] = 0.10;	selectivity[6] = 0.20;	selectivity[7] = 0.40;
//					selectivity[8] = 0.60;		selectivity[9] = 0.95;                               // oct - 2012
//				}
//				else if( sel_distribution ==1){
//
//					//This is for TPCDS queries
//					selectivity[0] = 0.00005;	selectivity[1] = 0.0005;selectivity[2] = 0.005;	selectivity[3] = 0.02;
//					selectivity[4] = 0.05;		selectivity[5] = 0.10;	selectivity[6] = 0.15;	selectivity[7] = 0.25;
//					selectivity[8] = 0.50;		selectivity[9] = 0.99;                            // dec - 2012
//				}
//				else
//					assert (false) : "should not come here";
//
//			}
//
//
//			if(resolution == 20){
//				if(sel_distribution == 0){
//
//					selectivity[0] = 0.0005;   selectivity[1] = 0.0008;		selectivity[2] = 0.001;	selectivity[3] = 0.002;
//					selectivity[4] = 0.004;   selectivity[5] = 0.006;		selectivity[6] = 0.008;	selectivity[7] = 0.01;
//					selectivity[8] = 0.03;	selectivity[9] = 0.05;	selectivity[10] = 0.08;	selectivity[11] = 0.10;
//					selectivity[12] = 0.200;	selectivity[13] = 0.300;	selectivity[14] = 0.400;	selectivity[15] = 0.500;
//					selectivity[16] = 0.600;	selectivity[17] = 0.700;	selectivity[18] = 0.800;	selectivity[19] = 0.99;
//				}
//				else if( sel_distribution ==1){
//
//					selectivity[0] = 0.00005;   selectivity[1] = 0.00008;		selectivity[2] = 0.0001;	selectivity[3] = 0.0002;
//					selectivity[4] = 0.0004;   selectivity[5] = 0.0006;		selectivity[6] = 0.0008;	selectivity[7] = 0.001;
//					selectivity[8] = 0.003;	selectivity[9] = 0.005;	selectivity[10] = 0.008;	selectivity[11] = 0.01;
//					selectivity[12] = 0.05;	selectivity[13] = 0.1;	selectivity[14] = 0.15;	selectivity[15] = 0.25;
//					selectivity[16] = 0.40;	selectivity[17] = 0.60;	selectivity[18] = 0.80;	selectivity[19] = 0.99;
//				}
//				else
//					assert (false) :funName+ "ERROR: should not come here";
//			}
//
//			if(resolution == 30){
//				if(sel_distribution == 0){
//				//tpch
//					selectivity[0] = 0.0005;  selectivity[1] = 0.0008;	selectivity[2] = 0.001;	selectivity[3] = 0.002;
//					selectivity[4] = 0.004;   selectivity[5] = 0.006;	selectivity[6] = 0.008;	selectivity[7] = 0.01;
//					selectivity[8] = 0.03;	selectivity[9] = 0.05;
//					selectivity[10] = 0.07;	selectivity[11] = 0.1;	selectivity[12] = 0.15;	selectivity[13] = 0.20;
//					selectivity[14] = 0.25;	selectivity[15] = 0.30;	selectivity[16] = 0.35;	selectivity[17] = 0.40;
//					selectivity[18] = 0.45;	selectivity[19] = 0.50;	selectivity[20] = 0.55;	selectivity[21] = 0.60;
//					selectivity[22] = 0.65;	selectivity[23] = 0.70;	selectivity[24] = 0.75;	selectivity[25] = 0.80;
//					selectivity[26] = 0.85;	selectivity[27] = 0.90;	selectivity[28] = 0.95;	selectivity[29] = 0.99;
//				}
//
//				else if(sel_distribution == 1){
//					selectivity[0] = 0.00001;  selectivity[1] = 0.00005;	selectivity[2] = 0.00010;	selectivity[3] = 0.00050;
//					selectivity[4] = 0.0010;   selectivity[5] = 0.005;	selectivity[6] = 0.0100;	selectivity[7] = 0.0200;
//					selectivity[8] = 0.0300;	selectivity[9] = 0.0400;	selectivity[10] = 0.0500;	selectivity[11] = 0.0600;
//					selectivity[12] = 0.0700;	selectivity[13] = 0.0800;	selectivity[14] = 0.0900;	selectivity[15] = 0.1000;
//					selectivity[16] = 0.1200;	selectivity[17] = 0.1400;	selectivity[18] = 0.1600;	selectivity[19] = 0.1800;
//					selectivity[20] = 0.2000;	selectivity[21] = 0.2500;	selectivity[22] = 0.3000;	selectivity[23] = 0.4000;
//					selectivity[24] = 0.5000;	selectivity[25] = 0.6000;	selectivity[26] = 0.7000;	selectivity[27] = 0.8000;
//					selectivity[28] = 0.9000;	selectivity[29] = 0.9950;
//				}
//
//				else
//					assert (false) :funName+ "ERROR: should not come here";
//			}
//
//			if(resolution==100){
//				if(sel_distribution == 1){
//					selectivity[0] = 0.000064; 	selectivity[1] = 0.000093; 	selectivity[2] = 0.000126; 	selectivity[3] = 0.000161; 	selectivity[4] = 0.000198;
//					selectivity[5] = 0.000239; 	selectivity[6] = 0.000284; 	selectivity[7] = 0.000332; 	selectivity[8] = 0.000384; 	selectivity[9] = 0.000440;
//					selectivity[10] = 0.000501; 	selectivity[11] = 0.000567; 	selectivity[12] = 0.000638; 	selectivity[13] = 0.000716; 	selectivity[14] = 0.000800;
//					selectivity[15] = 0.000890; 	selectivity[16] = 0.000989; 	selectivity[17] = 0.001095; 	selectivity[18] = 0.001211; 	selectivity[19] = 0.001335;
//					selectivity[20] = 0.001471; 	selectivity[21] = 0.001617; 	selectivity[22] = 0.001776; 	selectivity[23] = 0.001948; 	selectivity[24] = 0.002134;
//					selectivity[25] = 0.002335; 	selectivity[26] = 0.002554; 	selectivity[27] = 0.002790; 	selectivity[28] = 0.003046; 	selectivity[29] = 0.003323;
//					selectivity[30] = 0.003624; 	selectivity[31] = 0.003949; 	selectivity[32] = 0.004301; 	selectivity[33] = 0.004683; 	selectivity[34] = 0.005096;
//					selectivity[35] = 0.005543; 	selectivity[36] = 0.006028; 	selectivity[37] = 0.006552; 	selectivity[38] = 0.007121; 	selectivity[39] = 0.007736;
//					selectivity[40] = 0.008403; 	selectivity[41] = 0.009125; 	selectivity[42] = 0.009907; 	selectivity[43] = 0.010753; 	selectivity[44] = 0.011670;
//					selectivity[45] = 0.012663; 	selectivity[46] = 0.013739; 	selectivity[47] = 0.014904; 	selectivity[48] = 0.016165; 	selectivity[49] = 0.017531;
//					selectivity[50] = 0.019011; 	selectivity[51] = 0.020613; 	selectivity[52] = 0.022348; 	selectivity[53] = 0.024228; 	selectivity[54] = 0.026263;
//					selectivity[55] = 0.028467; 	selectivity[56] = 0.030854; 	selectivity[57] = 0.033440; 	selectivity[58] = 0.036240; 	selectivity[59] = 0.039272;
//					selectivity[60] = 0.042556; 	selectivity[61] = 0.046113; 	selectivity[62] = 0.049965; 	selectivity[63] = 0.054136; 	selectivity[64] = 0.058654;
//					selectivity[65] = 0.063547; 	selectivity[66] = 0.068845; 	selectivity[67] = 0.074584; 	selectivity[68] = 0.080799; 	selectivity[69] = 0.087530;
//					selectivity[70] = 0.094819; 	selectivity[71] = 0.102714; 	selectivity[72] = 0.111263; 	selectivity[73] = 0.120523; 	selectivity[74] = 0.130550;
//					selectivity[75] = 0.141411; 	selectivity[76] = 0.153172; 	selectivity[77] = 0.165910; 	selectivity[78] = 0.179705; 	selectivity[79] = 0.194645;
//					selectivity[80] = 0.210825; 	selectivity[81] = 0.228348; 	selectivity[82] = 0.247325; 	selectivity[83] = 0.267877; 	selectivity[84] = 0.290136;
//					selectivity[85] = 0.314241; 	selectivity[86] = 0.340348; 	selectivity[87] = 0.368621; 	selectivity[88] = 0.399241; 	selectivity[89] = 0.432403;
//					selectivity[90] = 0.468316; 	selectivity[91] = 0.507211; 	selectivity[92] = 0.549334; 	selectivity[93] = 0.594953; 	selectivity[94] = 0.644359;
//					selectivity[95] = 0.697865; 	selectivity[96] = 0.755812; 	selectivity[97] = 0.818569; 	selectivity[98] = 0.886535; 	selectivity[99] = 0.990142;
//
//				}
//				else if(sel_distribution == 0){
//					selectivity[0] = 0.005995; 	selectivity[1] = 0.015985; 	selectivity[2] = 0.025975; 	selectivity[3] = 0.035965; 	selectivity[4] = 0.045955; 
//					selectivity[5] = 0.055945; 	selectivity[6] = 0.065935; 	selectivity[7] = 0.075925; 	selectivity[8] = 0.085915; 	selectivity[9] = 0.095905; 
//					selectivity[10] = 0.105895; 	selectivity[11] = 0.115885; 	selectivity[12] = 0.125875; 	selectivity[13] = 0.135865; 	selectivity[14] = 0.145855; 
//					selectivity[15] = 0.155845; 	selectivity[16] = 0.165835; 	selectivity[17] = 0.175825; 	selectivity[18] = 0.185815; 	selectivity[19] = 0.195805; 
//					selectivity[20] = 0.205795; 	selectivity[21] = 0.215785; 	selectivity[22] = 0.225775; 	selectivity[23] = 0.235765; 	selectivity[24] = 0.245755; 
//					selectivity[25] = 0.255745; 	selectivity[26] = 0.265735; 	selectivity[27] = 0.275725; 	selectivity[28] = 0.285715; 	selectivity[29] = 0.295705; 
//					selectivity[30] = 0.305695; 	selectivity[31] = 0.315685; 	selectivity[32] = 0.325675; 	selectivity[33] = 0.335665; 	selectivity[34] = 0.345655; 
//					selectivity[35] = 0.355645; 	selectivity[36] = 0.365635; 	selectivity[37] = 0.375625; 	selectivity[38] = 0.385615; 	selectivity[39] = 0.395605; 
//					selectivity[40] = 0.405595; 	selectivity[41] = 0.415585; 	selectivity[42] = 0.425575; 	selectivity[43] = 0.435565; 	selectivity[44] = 0.445555; 
//					selectivity[45] = 0.455545; 	selectivity[46] = 0.465535; 	selectivity[47] = 0.475525; 	selectivity[48] = 0.485515; 	selectivity[49] = 0.495505; 
//					selectivity[50] = 0.505495; 	selectivity[51] = 0.515485; 	selectivity[52] = 0.525475; 	selectivity[53] = 0.535465; 	selectivity[54] = 0.545455; 
//					selectivity[55] = 0.555445; 	selectivity[56] = 0.565435; 	selectivity[57] = 0.575425; 	selectivity[58] = 0.585415; 	selectivity[59] = 0.595405; 
//					selectivity[60] = 0.605395; 	selectivity[61] = 0.615385; 	selectivity[62] = 0.625375; 	selectivity[63] = 0.635365; 	selectivity[64] = 0.645355; 
//					selectivity[65] = 0.655345; 	selectivity[66] = 0.665335; 	selectivity[67] = 0.675325; 	selectivity[68] = 0.685315; 	selectivity[69] = 0.695305; 
//					selectivity[70] = 0.705295; 	selectivity[71] = 0.715285; 	selectivity[72] = 0.725275; 	selectivity[73] = 0.735265; 	selectivity[74] = 0.745255; 
//					selectivity[75] = 0.755245; 	selectivity[76] = 0.765235; 	selectivity[77] = 0.775225; 	selectivity[78] = 0.785215; 	selectivity[79] = 0.795205; 
//					selectivity[80] = 0.805195; 	selectivity[81] = 0.815185; 	selectivity[82] = 0.825175; 	selectivity[83] = 0.835165; 	selectivity[84] = 0.845155; 
//					selectivity[85] = 0.855145; 	selectivity[86] = 0.865135; 	selectivity[87] = 0.875125; 	selectivity[88] = 0.885115; 	selectivity[89] = 0.895105; 
//					selectivity[90] = 0.905095; 	selectivity[91] = 0.915085; 	selectivity[92] = 0.925075; 	selectivity[93] = 0.935065; 	selectivity[94] = 0.945055; 
//					selectivity[95] = 0.955045; 	selectivity[96] = 0.965035; 	selectivity[97] = 0.975025; 	selectivity[98] = 0.985015; 	selectivity[99] = 0.995005;
//				}
//
//				else
//					assert (false) :funName+ "ERROR: should not come here";
//			}
//			//the selectivity distribution
//			//System.out.println("The selectivity distribution using is ");
////			for(int i=0;i<resolution;i++)
////			System.out.println("\t"+selectivity[i]);
//		}
//
//
//	private ADiagramPacket getGDP(File file) {
//		ADiagramPacket gdp = null;
//		FileInputStream fis;
//		try {
//			fis = new FileInputStream(file);
//			ObjectInputStream ois = new ObjectInputStream(fis);
//			Object obj = ois.readObject();
//			gdp = (ADiagramPacket) obj;
//
//			ois.close();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
//		}
//		return gdp;
//	}
//	public int[] getCoordinates(int dimensions, int res, int location){
//		int [] index = new int[dimensions];
//
//		for(int i=0; i<dimensions; i++){
//			index[i] = location % res;
//
//			location /= res;
//		}
//		return index;
//	}
//	public int getIndex(int[] index,int res)
//	{
//		int tmp=0;
//
//		for(int i=index.length-1; i>=0; i--){
//			if(index[i] > 0)
//				tmp=tmp * res + index[i];
//			else
//				tmp=tmp * res;
//		}
//
//		return tmp;
//	}
//	double getOptimalCost(int index)
//	{
//		return OptimalCost[index];
//	}
//
//	int getPlanNumber_generic(int arr[])
//	{
//		int index = getIndex(arr,resolution);
//		return plans[index];
//	}
//
//	public ADiagramPacket cgFpc(double threshold, ADiagramPacket gdp, String apktPath) throws IOException {
//
//		String funName = "cgFpc";
//		// First call the readApkt() function
//		readpkt(gdp);
//		System.out.println("CostGreedy:");
//		ADiagramPacket ngdp = new ADiagramPacket(gdp);
//
//		int n = nPlans;
//
//		Set[] s = new Set[n];
//		boolean[] notSwallowed = new boolean[n];
//		DataValues[] newData = new DataValues[data.length];
//
//		for(int i = 0;i < data.length;i ++) {
//			Integer xI = new Integer(i);
//			int p = data[i].getPlanNumber();
//			if (s[p] == null) {
//				s[p] = new Set();
//			}
//			s[p].elements.add(xI);
//			if (notSwallowed[p]) {
//				continue;
//			}
//
//			double cost = AllPlanCosts[p][i];
//			double lt = cost * (1 + threshold / 100);
//			boolean flag = false;
//			for(int j = 0;j < n;j ++) {
//				if(p != j) {
//					double cst = AllPlanCosts[j][i];//getCost(j,i);
//					if(cst <= lt) {
//						if(s[j] == null) {
//							s[j] = new Set();
//						}
//						s[j].elements.add(xI);
//						flag = true;
//					}
//				}
//			}
//			if (!flag) {
//				notSwallowed[p] = true;
//			}
//
//		}
//
//		ArrayList soln = new ArrayList();
//		Set temp = new Set();
//		for (int i = 0; i < n; i++) {
//			if (notSwallowed[i] && s[i] != null) {
//				temp.elements.addAll(s[i].elements);
//				s[i].elements.clear();
//				s[i] = null;
//				soln.add(new Integer(i));
//			}
//		}
//		int cct = 0;
//		for (int i = 0; i < n; i++) {
//			if (s[i] != null) {
//				s[i].elements.removeAll(temp.elements);
//				if(s[i].elements.size() != 0) {
//					cct ++;
//				}
//			}
//		}
//
//		while (true) {
//			int max = 0;
//			int p = -1;
//			for (int i = 0; i < n; i++) {
//				if (s[i] != null) {
//					int size = s[i].elements.size();
//					if (size > max) {
//						max = size;
//						p = i;
//					}
//				}
//			}
//			if (p == -1) {
//				break;
//			}
//			soln.add(new Integer(p));
//			for (int i = 0; i < n; i++) {
//				if (s[i] != null && i != p) {
//					s[i].elements.removeAll(s[p].elements);
//					if (s[i].elements.size() == 0) {
//						s[i] = null;
//					}
//				}
//			}
//			s[p] = null;
//		}
//		for (int i = 0; i < data.length; i++) {
//
//			int p = data[i].getPlanNumber();
//			Integer pI = new Integer(p);
//			newData[i] = new DataValues();
//			newData[i].setCard(data[i].getCard());
//
//			double cost = AllPlanCosts[p][i];
//			double lt = cost * (1 + threshold / 100);
//
//			if(soln.contains(pI)) {
//				newData[i].setPlanNumber(p);
//				newData[i].setCost(data[i].getCost());
//			} else {
//				int plan = -1;
//				double newcost = Double.MAX_VALUE; 
//				for (int xx = 0; xx < n; xx++) {
//					double cst = AllPlanCosts[xx][i];//getCost(xx,i);
//					if (soln.contains(new Integer(xx)) && xx != p && cst <= lt) {
//						// another redundant check for xx != p
//						if(cst <= newcost) {
//							newcost = cst;
//							plan = xx;
//						}
//					}
//				}
//				newData[i].setPlanNumber(plan);
//				newData[i].setCost(newcost);
//				//TODO : should we change this?
//				if(newData[i].getCost() <= data[i].getCost()){
//					newData[i].setCost(data[i].getCost()*1.01);
//					/*
//					 * We are adding 1% more to the cost of the replacement plan, in case 
//					 * when the replacement plan has lesser cost than the original plan
//					 */
////					newData[i].setPlanNumber(data[i].getPlanNumber());
//				}
//
//			}
//		}
//		ngdp.setDataPoints(newData);
//		//setInfoValues(data, newData);
//
//
//		// to test the data in newData---------------------------------------------
//
//		/*if the new data actually contain plans whose is within the threshold*/
//		for(int i=0;i<data.length;i++){
//			if(newData[i].getCost() > (1+threshold/100)*data[i].getCost())
//				System.out.println(funName+" ERROR: exceeding threshold at "+i);
//		}
//
//		/*to test the FPC functionality*/
//		int fpc_count=0;
//		for(int i=0;i<data.length;i++){
//			int p = data[i].getPlanNumber();
//			double c1= data[i].getCost(); //optimal cost at i
//			double c2 = AllPlanCosts[p][i]; //plan p's cost at i from pcst files
//			//if(! (Math.abs(c1 - c2) < 0.05*c1 || Math.abs(c1 - c2) < 0.05*c2) ){
//			if((c2-c1) > 0.05*c1){
//				int [] ind = getCoordinates(dimension, resolution, i);
//				//System.out.printf("\nFPC ERROR: Plan: %4d, Loc(%3d, %3d,%3d): , pktCost: %10.1f, fpcOptCost: %10.1f, error: %4.2f", p, ind[0], ind[1],ind[2],c1, c2, (double)Math.abs(c1 - c2)*100/c1);
//				fpc_count++;
//			}
//
//			//				else
//			//					System.out.printf("\nFPC ERROR: Plan: %4d, Loc(%3d, %3d): (%6.4f, %6.4f), pktCost: %10.1f, fpcOptCost: %10.1f, error: %4.2f", p, i, selectivity[i], selectivity[j], c1, c2, (double)Math.abs(c1 - c2)*100/c1);
//		}
//		System.out.println(funName+": FPC ERROR: for 5% deviation is "+fpc_count);
//
//		/*
//		 * to print the minimum value of AllPlanCosts 
//		 */
//		double mincostval = Double.MAX_VALUE,maxcostval=Double.MIN_VALUE;
//		for(int p=0;p<nPlans;p++){
//			for(int l=0;l<totalPoints;l++){
//				if(AllPlanCosts[p][l] < mincostval)
//					mincostval = AllPlanCosts[p][l]; 
//				if(AllPlanCosts[p][l] > maxcostval)
//					maxcostval = AllPlanCosts[p][l]; 
//			}
//		}
//		System.out.println("Min cost of AllPlanCosts "+mincostval+ " Max cost of AllPlanCosts "+maxcostval);
//		/*to test whether we retain the original data in case of threshold=0*/
//		if(threshold==(double)0){
//			double count=0;
//			for(int i=0;i<data.length;i++){
//				if(data[i].getCost()!=newData[i].getCost())
//					count++;
//			}
//			System.out.println("When threshold=0  newData and data files differ by"+count);
//		}
//		//-----------------------------------------------------------------------------
//		return ngdp;
//	}
//
//	public void findingNativeMSO(){
//
//		int [] newOptimalPlan = new int[totalPoints];
//		for(int loc=0; loc < totalPoints; loc++) {
//			newOptimalPlan[loc] = getPCSTOptimalPlan(loc);
//		}
//
//		int worstPlan[] = new int[totalPoints];
//		for(int loc=0; loc < totalPoints; loc++) {
//
//			worstPlan[loc] = getPCSTWorstPlan(loc);
//		}
//		//calculate really optimal plan at each location in the space -- because FPC costs may be different from the optimal costs
//
//		double MSO = -1.0;
//		double a;
//		int location=0;
//		for(int loc=0; loc < totalPoints; loc++)
//		{
//			a = AllPlanCosts[worstPlan[loc]][loc]/Math.max(1, AllPlanCosts[newOptimalPlan[loc]][loc]);
//			/*
//			 * TODO: Have used a sanity constant as 1 in the earlier line. 
//			 * Assuming none of the plan cost less than 1
//			 */
//
//			if(MSO < a)
//			{
//				MSO = a;
//				location = loc;
//			}
//		}
//		System.out.println("\n Sumit MSO = "+MSO);
//		System.out.println("\n loc:"+location+"\n Worst Value="+AllPlanCosts[worstPlan[location]][location]);
//		System.out.println("\nOptimal_cost :"+AllPlanCosts[newOptimalPlan[location]][location]+"\n");
//
//	}
//  
//	/*
//	 * for each location get the cheapest plan using the pcst files. 
//	 * This may be different from the optimizers choice due to imperfect
//	 * FPC implementation
//	 */
//	private int getPCSTOptimalPlan(int loc) {
//
//		double bestCost = Double.MAX_VALUE;
//		int opt = -1;
//		for(int p=0; p<nPlans; p++){
//			if(bestCost > AllPlanCosts[p][loc]) {
//				bestCost = AllPlanCosts[p][loc];
//				opt = p;
//			}
//		}
//		return opt;
//	}
//
//	private int getPCSTWorstPlan(int loc) {
//
//		double worstCost = Double.MIN_VALUE;
//		int opt = -1;
//		for(int p=0; p<nPlans; p++){
//			if(worstCost < AllPlanCosts[p][loc]) {
//				worstCost = AllPlanCosts[p][loc];
//				opt = p;
//			}
//		}
//		return opt;
//	}
//	public void loadPropertiesFile() {
//
//		Properties prop = new Properties();
//		InputStream input = null;
//	 
//		try {
//	 
//			input = new FileInputStream("./src/Constants.properties");
//	 
//			// load a properties file
//			prop.load(input);
//	 
//			// get the property value and print it out
//			apktPath = prop.getProperty("apktPath");
//			qtName = prop.getProperty("qtName");
//			cardinalityPath = prop.getProperty("cardinalityPath");
//			sel_distribution = Integer.parseInt(prop.getProperty("sel_distribution"));
//
//		} catch (IOException ex) {
//			ex.printStackTrace();
//		}
//	}
// 
//}
//
//class PlanBouquetOutputParamStruct {
//	double SO = 0;
//	double Harm = 0;
//	double anshSO = 0;
//	boolean flag = true;
//}
//class PlanBouquetinputParamStruct {
//	CostGreedyGCI3D obj;
//	int index;
//	double h_cost;
//	public PlanBouquetinputParamStruct(CostGreedyGCI3D obj, int index, double h_cost) {
//		this.obj = obj;
//		this.index = index;
//		this.h_cost = h_cost;
//	}
//}
