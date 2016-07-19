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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

//import com.ibm.db2.jcc.b.ac;



import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.postgresql.jdbc3.Jdbc3PoolingDataSource;

import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;


public class GCI3D
{
	static int UNI = 1;
	static int EXP = 2;

	double err = 0.0;

	static double AllPlanCosts[][];
	static int nPlans;
	static int plans[];
	boolean isContourPoint[];
	static double OptimalCost[];
	static int totalPlans;
	static int dimension;
	static int resolution;
	static DataValues[] data;
	static int totalPoints;
	static double selectivity[];
	static String apktPath;
	static String qtName ;
	static Jdbc3PoolingDataSource source;
	static boolean MSOCalculation = true;
	static boolean randomPredicateOrder = false;

	//The following parameters has to be set manually for each query
	static String cardinalityPath;
	static ArrayList<ArrayList<Integer>> allPermutations = new ArrayList<ArrayList<Integer>>();
	static ArrayList<Integer> learntDim = new ArrayList<Integer>();
	static HashMap<Integer,Integer> learntDimIndices = new HashMap<Integer,Integer>();
	static ArrayList<point_generic> all_contour_points = new ArrayList<point_generic>();
	//for ASO calculation 
	static double planCount[], planRelativeArea[];
	static double picsel[], locationWeight[];
	static double areaSpace =0,totalEstimatedArea = 0;
	//	static Connection conn = null;
	static int database_conn;
	static int sel_distribution; 
	static boolean FROM_CLAUSE;
	static String varyingJoins; 
	static double JS_multiplier []; 
	static String query; 	
	static double h_cost;

	double  minIndex [];
	double  maxIndex [];

	ArrayList<Integer> remainingDim;
	HashMap<Integer,ArrayList<point_generic>> ContourPointsMap = new HashMap<Integer,ArrayList<point_generic>>();
	ArrayList<Pair<Integer>> executions = new ArrayList<Pair<Integer>>();  
	HashMap<Index,ArrayList<Double>> sel_for_point = new HashMap<Index,ArrayList<Double>>();
	double learning_cost = 0;
	double oneDimCost = 0;
	int no_executions = 0;
	int no_repeat_executions = 0;
	int max_no_executions = 0;
	int max_no_repeat_executions = 0;
	boolean [] already_visited;  
	double[] actual_sel;

	public GCI3D(){}
	
	public GCI3D(GCI3D obj){
		minIndex = new double[obj.dimension];
		maxIndex = new double[obj.dimension];		
		this.remainingDim = new ArrayList<Integer>(obj.remainingDim);
		this.ContourPointsMap = new HashMap<Integer, ArrayList<point_generic>>();
	/*	Iterator itr = obj.ContourPointsMap.keySet().iterator();
		Integer key;
		while(itr.hasNext())
		{
			key = (Integer)itr.next();
			ArrayList<point_generic> contourPoints = new ArrayList<point_generic>();
			for(int c=0;c<obj.ContourPointsMap.get(key).size();c++){
				contourPoints.add(new point_generic(obj.ContourPointsMap.get(key).get(c)));
			}
			this.ContourPointsMap.put(key, contourPoints);
		}
	*/	
		//this.ContourPointsMap = obj.ContourPointsMap;
		this.executions = new ArrayList<Pair<Integer>>();
		this.sel_for_point = obj.sel_for_point; 
		this.learning_cost = 0;
		this.oneDimCost = 0;
		this.no_executions = 0;
		this.no_repeat_executions = 0;
		this.max_no_executions = 0;
		this.max_no_repeat_executions = 0;
		this.already_visited = new boolean[obj.dimension];  
		this.actual_sel = new double[obj.dimension];
	}
	
	public static void main(String args[]) throws IOException, SQLException, InterruptedException, ExecutionException
	{
		
		final long startTime = System.currentTimeMillis();

		GCI3D obj = new GCI3D();
		final GCI3D global_obj = new GCI3D();
		
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
		h_cost = obj.getOptimalCost(obj.totalPoints-1);
		double min_cost = obj.getOptimalCost(0);
		double ratio = h_cost/min_cost;
		assert (h_cost >= min_cost) : "maximum cost is less than the minimum cost";
		System.out.println("the ratio of C_max/c_min is "+ratio);

		i = 1;

		//to generate contours
		obj.remainingDim.clear(); 
		for(int d=0;d<obj.dimension;d++){
			obj.remainingDim.add(d);
		}

		learntDim.clear();
		learntDimIndices.clear();
		double cost = obj.getOptimalCost(0);
		getAllPermuations(obj.remainingDim,0);
		assert (allPermutations.size() == obj.factorial(obj.dimension)) : "all the permutations are not generated";

		obj.isContourPoint = new boolean[obj.totalPoints];
		while(cost < 2*h_cost)
		{
			if(cost>h_cost)
				cost = h_cost;
			System.out.println("---------------------------------------------------------------------------------------------\n");
			System.out.println("Contour "+i+" cost : "+cost+"\n");
			obj.all_contour_points.clear();
			obj.nexusAlgoContour(cost);
			
//			for(ArrayList<Integer> order:allPermutations){
//				System.out.println("Entering the order"+order);
//				learntDim.clear();
//				learntDimIndices.clear();
//				obj.getContourPoints(order,cost);
//			}

			int size_of_contour = obj.all_contour_points.size();
			obj.ContourPointsMap.put(i, new ArrayList<point_generic>(obj.all_contour_points)); //storing the contour points
			global_obj.ContourPointsMap.put(i, new ArrayList<point_generic>(obj.all_contour_points)); //storing the contour points
			System.out.println("Size of contour"+size_of_contour );
			System.out.println("Its point are");
			for(point_generic pg : obj.all_contour_points){
				pg.printPoint();
			}
			cost = cost*2;
			i = i+1;
		}

//		obj.initialize(0);
//		GCI3D objTest = new GCI3D(obj);
//		obj.removeDimensionFromContourPoints(2);
//		objTest.removeDimensionFromContourPoints(2);
////		obj.removeDimensionFromContourPoints(2);
//		//checking alignment threshold
//		obj.checkAlignmentPenalty();
		
		/*
		 * Setting up the DB connection to Postgres/TPCH/TPCDS Benchmark. 
		 */
		try{
			System.out.println("entered DB conn1");
			Class.forName("org.postgresql.Driver");
			source = new Jdbc3PoolingDataSource();
			source.setDataSourceName("A Data Source");
			//Settings
			//System.out.println("entered DB conn2");
			if(database_conn==0){
				
				source.setServerName("localhost:5431");
				source.setDatabaseName("tpch-ai");
//				conn = DriverManager
//						.getConnection("jdbc:postgresql://localhost:5431/tpch-ai",
//								"sa", "database");
			}
			else{
				
				System.out.println("entered DB tpcds");
				source.setServerName("localhost:5431");
				source.setDatabaseName("tpcdscodd");
//				conn = DriverManager
//						.getConnection("jdbc:postgresql://localhost:5432/tpcds",
//								"sa", "database");

			}
			source.setUser("sa");
			source.setPassword("database");
			source.setMaxConnections(10);
			System.out.println("Opened database successfully");
		}
		catch ( Exception e ) {
			System.out.println("entered DB err");
			System.err.println( e.getClass().getName()+": "+ e.getMessage() );
			System.exit(1);

		}
	//	Connection conn = source.getConnection();
     //   Statement stmt = conn.createStatement();
      //  stmt.execute("EXPLAIN SELECT * FROM customer");
	//	System.exit(1);
		/*
		 * running the spillBound and plan bouquet algorithm 
		 */
		double MSO =0, ASO = 0,anshASO = 0,SO=0,MaxHarm=-1*Double.MAX_VALUE,Harm=Double.MIN_VALUE;
		int ASO_points=0;
		obj.getPlanCountArray();
		int min_point = 0; /*not to execute the spillBound algorithm*/
		int max_point = 1; /*to execute a specific q_a */
		//Settings
		if(MSOCalculation)
			max_point = obj.totalPoints;
		
		boolean singleThread = false;

/*		if (args.length > 0) {
    		//updating the min and the max index if available
    	    try {
    	        min_point = Integer.parseInt(args[0]);
    	        max_point = Integer.parseInt(args[1]);
    	    } catch (NumberFormatException e) {
    	        System.err.println("Argument" + args[0] + " must be an integer.");
    	        System.exit(1);
    	    }
    	}
		
*/
		double[] subOpt = new double[max_point];
		
		if(singleThread)
		{
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
			obj.executions.clear();
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

				if(prev==1){ 
					
					obj.oneDimensionSearch(i,cost);
					obj.learning_cost = obj.oneDimCost;
				}
				else
					obj.spillBoundAlgo(i);

				int present = obj.remainingDim.size();
				if(present < prev - 1 || present > prev)
					System.out.println("ERROR");

				algo_cost = algo_cost+ (obj.learning_cost);
				if(present == prev ){    // just to see whether any dimension was learnt
					// if no dimension is learnt then move on to the next contour

					/*
					 * capturing the properties of the query execution
					 */
					System.out.println("In Contour "+i+" has the following details");
					System.out.println("No of executions is "+obj.no_executions);
					System.out.println("No of repeat moves is "+obj.no_repeat_executions);
					if(obj.no_executions>obj.max_no_executions)
						obj.max_no_executions = obj.no_executions;
					if(obj.no_repeat_executions > obj.max_no_repeat_executions)
						obj.max_no_repeat_executions = obj.no_repeat_executions;


					cost = cost*2;  
					i = i+1;
					obj.executions.clear();
					/*
					 * initialize the repeat moves and executions for the next contour
					 */
					for(int d=0;d<obj.dimension;d++){
						obj.already_visited[d] = false;
						obj.no_executions = 0;
						obj.no_repeat_executions =0;
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
	}
		else{
			max_point = 1; 
			if(MSOCalculation)
				//if(false)
					max_point = obj.totalPoints;

			List<SpillBoundinputParamStruct> inputs = new ArrayList<SpillBoundinputParamStruct>();
			for (int j = 0; j < max_point; ++j) {
				SpillBoundinputParamStruct input = new SpillBoundinputParamStruct(new GCI3D(obj), j);
				inputs.add(input);
			}
			
			int threads = Runtime.getRuntime().availableProcessors();
			System.out.println("Available Cores " + threads);
		    ExecutorService service = Executors.newFixedThreadPool(threads);
		    List<Future<SpillBoundOutputParamStruct>> futures = new ArrayList<Future<SpillBoundOutputParamStruct>>();
		    
		    for (final SpillBoundinputParamStruct input : inputs) {
		        Callable<SpillBoundOutputParamStruct> callable = new Callable<SpillBoundOutputParamStruct>() {
		            public SpillBoundOutputParamStruct call() throws Exception {
		            	SpillBoundOutputParamStruct output = new SpillBoundOutputParamStruct();
		            	System.out.println("Begin execution loop "+ input.index);
		            	int j = input.index;
		            	GCI3D obj = input.obj;
	            
		            	Iterator itr = global_obj.ContourPointsMap.keySet().iterator();
		        		Integer key;
		        		while(itr.hasNext())
		        		{
		        			key = (Integer)itr.next();
		        			ArrayList<point_generic> contourPoints = new ArrayList<point_generic>();
		        			for(int c=0;c<global_obj.ContourPointsMap.get(key).size();c++){
		        				contourPoints.add(new point_generic(global_obj.ContourPointsMap.get(key).get(c)));
		        			}
		        			obj.ContourPointsMap.put(key, contourPoints);
		        		}
		            	
		            	
		            	
		    			double algo_cost =0;
		    			double SO =0;
		    			double cost = obj.getOptimalCost(0);
		    			obj.initialize(j);
		    			int[] index = obj.getCoordinates(obj.dimension, obj.resolution, j);
		    			//		if(index[0]%5 !=0 || index[1]%5!=0)
		    			//			continue;
		    			//Settings:
		    			//		obj.actual_sel[0] = 0.02;obj.actual_sel[1] = 0.99;obj.actual_sel[2] = 0.1;obj.actual_sel[3] = 0.99; /*uncomment for single execution*/

		    			for(int d=0;d<obj.dimension;d++) obj.actual_sel[d] = obj.findNearestSelectivity(obj.actual_sel[d]);
		    			if(obj.cost_generic(obj.convertSelectivitytoIndex(obj.actual_sel))<10000){
		    				output.flag = false;
		        			return output;
		    			}
		    			
		    			//----------------------------------------------------------
		    			int i =1;
		    			obj.executions.clear();
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

		    				if(prev==1){ 
		    					
		    					obj.oneDimensionSearch(i,cost);
		    					obj.learning_cost = obj.oneDimCost;
		    				}
		    				else
		    					obj.spillBoundAlgo(i);

		    				int present = obj.remainingDim.size();
		    				if(present < prev - 1 || present > prev)
		    					System.out.println("ERROR");

		    				algo_cost = algo_cost+ (obj.learning_cost);
		    				if(present == prev ){    // just to see whether any dimension was learnt
		    					// if no dimension is learnt then move on to the next contour

		    					/*
		    					 * capturing the properties of the query execution
		    					 */
		    					System.out.println("In Contour "+i+" has the following details");
		    					System.out.println("No of executions is "+obj.no_executions);
		    					System.out.println("No of repeat moves is "+obj.no_repeat_executions);
		    					if(obj.no_executions>obj.max_no_executions)
		    						obj.max_no_executions = obj.no_executions;
		    					if(obj.no_repeat_executions > obj.max_no_repeat_executions)
		    						obj.max_no_repeat_executions = obj.no_repeat_executions;


		    					cost = cost*2;  
		    					i = i+1;
		    					obj.executions.clear();
		    					/*
		    					 * initialize the repeat moves and executions for the next contour
		    					 */
		    					for(int d=0;d<obj.dimension;d++){
		    						obj.already_visited[d] = false;
		    						obj.no_executions = 0;
		    						obj.no_repeat_executions =0;
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
		    			output.SO = SO;
		    			
		    			output.anshASO += SO*locationWeight[j];
		    			
		    			System.out.println("\nSpillBound The SubOptimaility  is "+SO);
		    			return output;
		    			//System.out.println("\nSpillBound Harm  is "+Harm);
		            }
		        };
		        futures.add(service.submit(callable));
		    }
		
		    service.shutdown();
		    MSO =0;
			ASO = 0;
			anshASO = 0;
			MaxHarm=-1*Double.MAX_VALUE;
			ASO_points=0;

			int j=0;
		    for (Future<SpillBoundOutputParamStruct> future : futures) {
		    	SpillBoundOutputParamStruct output = future.get();
		    	if(output.flag)
		    	{
		    		subOpt[j] = output.SO;
		    		if(output.SO>MSO)
		    			MSO = output.SO;
		    		if(output.Harm > MaxHarm)
		    			MaxHarm = output.Harm;
		    		ASO += output.SO;
		    		ASO_points++;
		    		anshASO += output.anshASO;
		    		j++;
		    	}
		    }

		}
		//Settings
		//obj.writeSuboptToFile(subOpt, apktPath);
		System.out.println("SpillBound The MaxSubOptimaility  is "+MSO);
		System.out.println("SpillBound Anshuman average Suboptimality is "+(double)anshASO);
		System.out.println("SpillBound The AverageSubOptimaility  is "+(double)ASO/ASO_points);

		final long endTime = System.currentTimeMillis();
		System.out.println("The total time taken is (in mins) "+(endTime-startTime)/(1000*60));
	}

	public void oneDimensionSearch(int contour_no, double cost) {

		String funName = "oneDimensionSearch";
		/*
		 * just sanity check for findNearestPoint and findNearestSelectivity
		 */
		assert(findNearestSelectivity(actual_sel[0]) == findNearestSelectivity(findNearestSelectivity(actual_sel[0]))) : funName+ " : findNearSelectivity Error";
		assert (findNearestPoint(actual_sel[0]) == findNearestPoint(selectivity[findNearestPoint(actual_sel[0])])) : funName+ " : findNearPoint Error";

		// added code Feb28; 8:00 pm
		if(cost_generic(convertSelectivitytoIndex(actual_sel)) > 2*cost){
			oneDimCost = cost;
			return;
		}

		oneDimCost = 0;//initialization
		assert (remainingDim.size() == 1): funName+": more than one dimension left";
		int [] arr = new int[dimension];
		int remDim = -1;
		double sel_min = 2; //some value greater than 1
		for(int d=0;d<dimension;d++){
			if(remainingDim.contains(d))
				remDim = d;
			else
				arr[d] = findNearestPoint(actual_sel[d]);
		}

		//TODO: pick the sel_min in the contour in 1D
		for(int c=0;c< ContourPointsMap.get(contour_no).size();c++){

			point_generic p = ContourPointsMap.get(contour_no).get(c);

			if(inFeasibleRegion(convertIndextoSelectivity(p.get_point_Index()))){
				Integer learning_dim = new Integer(p.getLearningDimension());
				assert (learning_dim.intValue() == remDim) : funName+": ERROR plan's learning dimension not matching with remaining dimension";
				assert (remainingDim.contains(learning_dim)) : funName+": ERROR remaining dimension does not contain the learning dimension";

				if(selectivity[p.get_dimension(learning_dim.intValue())] >= actual_sel[remDim]){
					if(selectivity[p.get_dimension(learning_dim.intValue())] < sel_min){
						sel_min = selectivity[p.get_dimension(learning_dim.intValue())]; 

						oneDimCost = p.get_cost();
						//oneDimCost = cost;

						/*
						 * set it to plan's cost at q_a
						 */
						int fpc_plan = p.get_plan_no();
						int [] int_actual_sel = convertSelectivitytoIndex(actual_sel);
						if(fpc_cost_generic(int_actual_sel, fpc_plan)<oneDimCost)
							oneDimCost = fpc_cost_generic(int_actual_sel, fpc_plan);
						if(cost_generic(int_actual_sel)> oneDimCost)
							oneDimCost = cost_generic(int_actual_sel);

					}

				}
			}
		}

		if(sel_min < (double) 1 && sel_min >= actual_sel[remDim]){
			System.out.println(funName+" learnt "+ remDim+" dimension completely");
			remainingDim.remove(remainingDim.indexOf(remDim));
			System.out.println(funName+" Sel_min = "+sel_min+" and cost is "+oneDimCost);
			//assert (oneDimCost<=2*cost) :funName+": oneDimCost is not less than 2*cost when setting to resolution-1";
			return; //done if the sel_min is greater than actual selectivity
		}


		if(sel_min == (double)2){
			arr[remDim] = 0;
			/*
			 * The following If condition is needed to skip the contour. If the condition fails then 
			 * we can possibly we dominated by some point in the contour. Else, we can say the contour
			 * can be skipped. 
			 */
			if(cost_generic(arr) > cost){
				oneDimCost = 0;
				return;
			}
		}
		if(sel_min == (double)2){
			/*
			 * We turn to this case when there are no contour point for all the learnt
			 * selectivities. But check if the point at arr[remDim] = resolution-1 has
			 * cost less than the contour cost
			 */
			arr[remDim] = resolution-1;   //TODO is it okay to resolution -1 in 3D or higher
			sel_min = selectivity[resolution-1];
			oneDimCost = cost_generic(arr);
			/*
			 * use the fpc_cost at q_a for oneDimCost
			 */
			int fpc_plan = getPlanNumber_generic(arr);
			int [] int_actual_sel = convertSelectivitytoIndex(actual_sel);
			if(fpc_cost_generic(int_actual_sel, fpc_plan)<oneDimCost)
				oneDimCost = fpc_cost_generic(int_actual_sel, fpc_plan);
			if(cost_generic(int_actual_sel)> oneDimCost)
				oneDimCost = cost_generic(int_actual_sel);
			remainingDim.remove(remainingDim.indexOf(remDim));
			System.out.println(funName+" Sel_min = "+sel_min+" and cost is "+oneDimCost);
			if(oneDimCost>2*cost)
				oneDimCost = 2*cost-1;
			//assert (oneDimCost<=2*cost) :funName+": oneDimCost is not less than 2*cost when setting to resolution-1";
		}


	}

	
	public  void checkValidityofWeights() {
		double areaPlans =0;
		double relativeAreaPlans =0;
		areaSpace = 0.0;

		planRelativeArea = new double[totalPlans];

		for (int i=0; i< data.length; i++){
			areaSpace += locationWeight[i];
		}
		//	System.out.println(areaSpace);

		for (int i=0; i< totalPlans; i++){
			planRelativeArea[i] = planCount[i]/areaSpace;
			relativeAreaPlans += planRelativeArea[i];
			areaPlans += planCount[i];
		}
		//	System.out.println(areaPlans);
		//	System.out.println(relativeAreaPlans);

		if(relativeAreaPlans < 0.99) {
			System.out.println("ALERT! The area of plans add up to only " + relativeAreaPlans);
			//System.exit(0);
		}
	}



	public  void getPlanCountArray() {
		planCount = new double[totalPlans];
		locationWeight = new double[data.length];
		// Set dim depending on whether we are dealing with full packet or slice
		// int dim;


		int resln = resolution;
		int dim = dimension;

		int[] r = new int[dim];

		for (int i = 0; i < dim; i++)
			r[i] = resln;

		/*
		 * if(gdp.getDimension()==1) dim = 1; else if(data.length > r[0] * r[1])
		 * //full packet { dim = dim; //set to actual number of dimensions }
		 * else //slice { if(getDimension()==1) dim = 1; else dim = 2; //if
		 * actual dimension >= 2 }
		 */


		double locationWeightLocal[] = new double[resln];

		if(resolution==10 ){
			if(sel_distribution==0){
				//for tpch
				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1;
				locationWeightLocal[3] = 2;         locationWeightLocal[4] = 4;				locationWeightLocal[5] = 7;
				locationWeightLocal[6] = 15;        locationWeightLocal[7] = 20;				locationWeightLocal[8] = 30;
				locationWeightLocal[9] = 20;
			}

			//for tpcds
			if(sel_distribution == 1){
				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1;
				locationWeightLocal[3] = 2;         locationWeightLocal[4] = 4;				locationWeightLocal[5] = 6;
				locationWeightLocal[6] = 7;        locationWeightLocal[7] = 25;				locationWeightLocal[8] = 30;
				locationWeightLocal[9] = 20;
			}
		}


		else if (resolution == 20){
			if(sel_distribution == 0){
				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1;
				locationWeightLocal[3] = 1;         locationWeightLocal[4] = 1;				locationWeightLocal[5] = 1;
				locationWeightLocal[6] = 1;        locationWeightLocal[7] = 4;				locationWeightLocal[8] = 4;
				locationWeightLocal[9] = 5;
				locationWeightLocal[10] = 5;			locationWeightLocal[11] = 5;				locationWeightLocal[12] = 5;
				locationWeightLocal[13] = 6;         locationWeightLocal[14] = 6;				locationWeightLocal[15] = 10;
				locationWeightLocal[16] = 10;        locationWeightLocal[17] = 10;				locationWeightLocal[18] = 15;
				locationWeightLocal[19] = 10;

			}

			if(sel_distribution == 1){
				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1;
				locationWeightLocal[3] = 1;         locationWeightLocal[4] = 1;				locationWeightLocal[5] = 1;
				locationWeightLocal[6] = 1;        locationWeightLocal[7] = 1;				locationWeightLocal[8] = 4;
				locationWeightLocal[9] = 4;
				locationWeightLocal[10] = 5;			locationWeightLocal[11] = 5;				locationWeightLocal[12] = 5;
				locationWeightLocal[13] = 6;         locationWeightLocal[14] = 6;				locationWeightLocal[15] = 10;
				locationWeightLocal[16] = 10;        locationWeightLocal[17] = 15;				locationWeightLocal[18] = 15;
				locationWeightLocal[19] = 10;
			}

		}


		else if (resolution == 30){
			if(sel_distribution == 0){
				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1;
				locationWeightLocal[3] = 1;         locationWeightLocal[4] = 1;				locationWeightLocal[5] = 1;
				locationWeightLocal[6] = 1;        locationWeightLocal[7] = 1;				locationWeightLocal[8] = 1;
				locationWeightLocal[9] = 2;
				locationWeightLocal[10] = 3;			locationWeightLocal[11] = 3;				locationWeightLocal[12] = 5;
				locationWeightLocal[13] = 5;         locationWeightLocal[14] = 5;				locationWeightLocal[15] = 5;
				locationWeightLocal[16] = 5;        locationWeightLocal[17] = 5;				locationWeightLocal[18] = 5;
				locationWeightLocal[19] = 5;
				locationWeightLocal[20] = 5;			locationWeightLocal[21] = 5;				locationWeightLocal[22] = 5;
				locationWeightLocal[23] = 5;         locationWeightLocal[24] = 5;				locationWeightLocal[25] = 5;
				locationWeightLocal[26] = 5;        locationWeightLocal[27] = 5;				locationWeightLocal[28] = 5;
				locationWeightLocal[29] = 2;
			}

			if(sel_distribution == 1){
				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1;
				locationWeightLocal[3] = 1;         locationWeightLocal[4] = 1;				locationWeightLocal[5] = 1;
				locationWeightLocal[6] = 1;        locationWeightLocal[7] = 1;				locationWeightLocal[8] = 1;
				locationWeightLocal[9] = 2;
				locationWeightLocal[10] = 3;			locationWeightLocal[11] = 3;				locationWeightLocal[12] = 4;
				locationWeightLocal[13] = 4;         locationWeightLocal[14] = 4;				locationWeightLocal[15] = 4;
				locationWeightLocal[16] = 4;        locationWeightLocal[17] = 4;				locationWeightLocal[18] = 4;
				locationWeightLocal[19] = 4;
				locationWeightLocal[20] = 4;			locationWeightLocal[21] = 6;				locationWeightLocal[22] = 6;
				locationWeightLocal[23] = 6;         locationWeightLocal[24] = 6;				locationWeightLocal[25] = 6;
				locationWeightLocal[26] = 6;        locationWeightLocal[27] = 6;				locationWeightLocal[28] = 6;
				locationWeightLocal[29] = 5;
			}

		}
		else if (resolution == 40){
			if(sel_distribution == 0){
				//TODO
				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1;
				locationWeightLocal[3] = 1;         locationWeightLocal[4] = 1;				locationWeightLocal[5] = 1;
				locationWeightLocal[6] = 1;        locationWeightLocal[7] = 1;				locationWeightLocal[8] = 1;
				locationWeightLocal[9] = 2;
				locationWeightLocal[10] = 3;			locationWeightLocal[11] = 3;				locationWeightLocal[12] = 5;
				locationWeightLocal[13] = 5;         locationWeightLocal[14] = 5;				locationWeightLocal[15] = 5;
				locationWeightLocal[16] = 5;        locationWeightLocal[17] = 5;				locationWeightLocal[18] = 5;
				locationWeightLocal[19] = 5;
				locationWeightLocal[20] = 5;			locationWeightLocal[21] = 5;				locationWeightLocal[22] = 5;
				locationWeightLocal[23] = 5;         locationWeightLocal[24] = 5;				locationWeightLocal[25] = 5;
				locationWeightLocal[26] = 5;        locationWeightLocal[27] = 5;				locationWeightLocal[28] = 5;
				locationWeightLocal[29] = 2;		locationWeightLocal[20] = 5;			locationWeightLocal[21] = 5;				locationWeightLocal[22] = 5;
				locationWeightLocal[23] = 5;         locationWeightLocal[24] = 5;				locationWeightLocal[25] = 5;
				locationWeightLocal[26] = 5;        locationWeightLocal[27] = 5;				locationWeightLocal[28] = 5;
				locationWeightLocal[29] = 2;
			}

			if(sel_distribution == 1){
				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1;
				locationWeightLocal[3] = 1;         locationWeightLocal[4] = 1;				locationWeightLocal[5] = 1;
				locationWeightLocal[6] = 1;        locationWeightLocal[7] = 1;				locationWeightLocal[8] = 1;
				locationWeightLocal[9] = 1;
				locationWeightLocal[10] = 1;			locationWeightLocal[11] = 1;				locationWeightLocal[12] = 1;
				locationWeightLocal[13] = 2;         locationWeightLocal[14] = 2;				locationWeightLocal[15] = 2;
				locationWeightLocal[16] = 2;        locationWeightLocal[17] = 2;				locationWeightLocal[18] = 2;
				locationWeightLocal[19] = 2;
				locationWeightLocal[20] = 2;			locationWeightLocal[21] = 3;				locationWeightLocal[22] = 3;
				locationWeightLocal[23] = 3;         locationWeightLocal[24] = 3;				locationWeightLocal[25] = 3;
				locationWeightLocal[26] = 3;        locationWeightLocal[27] = 3;				locationWeightLocal[28] = 6;
				locationWeightLocal[29] = 3;		locationWeightLocal[30] = 4;			locationWeightLocal[31] = 4;				
				locationWeightLocal[32] = 3;
				locationWeightLocal[33] = 4;         locationWeightLocal[34] = 5;				locationWeightLocal[35] = 5;
				locationWeightLocal[36] = 5;        locationWeightLocal[37] = 5;				locationWeightLocal[38] = 5;
				locationWeightLocal[39] = 3;
			}
		}
		else if (resolution==100){
			for(int i=0;i<resolution;i++){
				locationWeightLocal[i] = 1;
			}
		}

		for (int loc=0; loc < data.length; loc++)
		{
			if(OptimalCost[loc]>=(double)10000){
				double weight = 1.0;
				int tempLoc = loc;
				for(int d=0;d<dim;d++){
					weight *= locationWeightLocal[tempLoc % resln];
					tempLoc = tempLoc/resln;
				}

				locationWeight[loc] = weight;
				planCount[data[loc].getPlanNumber()] += weight;
			}
			else
				locationWeight[loc] = (double) -1;

		}


		double totalWeight = 0,sumWeight=0;

		for (int i = 0; i < data.length; i++) {
			if(locationWeight[i]>=0)
				totalWeight += locationWeight[i];
		}

		for (int i = 0; i < data.length; i++) {
			if(locationWeight[i]>=0){
				locationWeight[i] /= totalWeight;
				sumWeight += locationWeight[i];


				System.out.println("The location weight is "+locationWeight[i]+" and total weight is "+totalWeight);
				assert (locationWeight[i]<= (double)1) : "In getPlanCountArray: locationWeight is not less than 1";
			}
			//		if(locationWeight[i]>(double)1){
			//			System.out.println("In getPlanCountArray: locationWeight is not less than 1");
			//			System.out.println("location weight : "+locationWeight[i]+" at i="+i+" total weight="+totalWeight);
			//		}
		}

		System.out.println("The sum weight is "+sumWeight);
		assert(sumWeight<=(double)1*1.01) : "In getPlanCountArray: sumWeight is not less than 1";

		/*
		 * if(scaleupflag) { for(int i = 0; i < planCount.length; i++)
		 * planCount[i] /= 100Math.pow(10, getDimension()); }
		 */


		checkValidityofWeights();

	}

	
	private void checkAlignmentPenalty() throws NumberFormatException, IOException {
		
		HashMap<Integer,ArrayList<Integer>> spillDimensionPlanMap = new HashMap<Integer,ArrayList<Integer>>();
		
		buildSpillDimensionMap(spillDimensionPlanMap);
	
		double max_penality = Double.MIN_VALUE;
		//find the extreme locations for each dimensions
		for(int c=1; c<=ContourPointsMap.size(); c++){
			
			System.out.println("------------------------------------Contour "+c+"---------------------------------");
			//get the extreme locations for each dimension in points_max for each contour
			HashMap<Integer,point_generic> points_max = new HashMap<Integer,point_generic>();

			for(int l=0; l <ContourPointsMap.get(c).size(); l++){
				point_generic p = ContourPointsMap.get(c).get(l);
				for(int d=0; d<dimension; d++){
					if(!points_max.containsKey(d))
						points_max.put(d, p);
					else{
						if(points_max.get(d).dim_values[d] < p.dim_values[d]){
							points_max.remove(new Integer(d));
							points_max.put(d, p);
						}
					}
				}
			}

			//calculate the penalty for aligning each dimension
			double penalty[] = new double[dimension];
			for(int d=0; d<dimension; d++){
				double original_cost = OptimalCost[getIndex(points_max.get(d).dim_values, resolution)];
				double min_fpc_cost = Double.MAX_VALUE;
				int min_fpc_plan = -1;
				for(int planno=0; spillDimensionPlanMap.containsKey(d) && planno<spillDimensionPlanMap.get(d).size(); planno++ ){
					if(AllPlanCosts[spillDimensionPlanMap.get(d).get(planno)][getIndex(points_max.get(d).dim_values, resolution)] < min_fpc_cost){
						min_fpc_cost = AllPlanCosts[spillDimensionPlanMap.get(d).get(planno)][getIndex(points_max.get(d).dim_values, resolution)];
						min_fpc_plan = spillDimensionPlanMap.get(d).get(planno);
					}
				}
				System.out.print("\nThe dimension is "+d+" at (max) point ");
				for(int dim=0; dim<dimension; dim++)
					System.out.print("\t"+points_max.get(d).dim_values[dim]);
				System.out.println(" with optimal cost "+original_cost+" while forcing plan "+min_fpc_plan+" having forced cost "+min_fpc_cost+" with penalty "+min_fpc_cost/original_cost);
				penalty[d] = min_fpc_cost/original_cost;
			}
			
			
			//update the max penalty of min penalty among these dimension
			double min_penalty = Double.MAX_VALUE;
			for(int d=0; d<dimension; d++)
				if(penalty[d] < min_penalty)
					min_penalty = penalty[d];
			
			if(min_penalty > max_penality)
				max_penality = min_penalty;
			System.out.println("-----------------------------------------------------------------------------------------------------");
		}
		
		System.out.println("The max penalty is "+max_penality);
	}

	private void buildSpillDimensionMap(HashMap<Integer,ArrayList<Integer>> spillDimensionPlanMap) throws NumberFormatException, IOException {

		for(int p=0;p<totalPlans;p++){
			try{
				String plansPath = apktPath+"predicateOrder/";
				FileReader file = new FileReader(plansPath+p+".txt");

				BufferedReader br = new BufferedReader(file);
				String s;
				while((s = br.readLine()) != null) {
					//System.out.println(Integer.parseInt(s));
					int value = Integer.parseInt(s);
					if(spillDimensionPlanMap.containsKey(value)){
						spillDimensionPlanMap.get(value).add(p);
					}
					else{
						ArrayList<Integer> al = new ArrayList<Integer>();
						al.add(p);
						spillDimensionPlanMap.put(value, al);
					}	
					break;
				}
				br.close();
				file.close();
			}
			catch(FileNotFoundException e){
				e.printStackTrace();
			}	
		}

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
					//System.out.println("The cost at new location is "+OptimalCost[loc_new]+" with sel= "+selectivity[coordinates[j]]+" and cost at old location "+OptimalCost[loc]+" with sel = "+selectivity[coordinates[j]-1]);
					if(costr  > selr)
						violation_count++;
					coordinates[j]--;
				}
			}
		}
		System.out.println("the violation count is "+violation_count);
	}
	
	private void spillBoundAlgo(int contour_no) throws IOException {

		String funName = "spillBoundAlgo"; 
		Set<Integer> unique_plans = new HashSet();

		System.out.println("\nContour number ="+contour_no+ " with its size being "+ContourPointsMap.get(contour_no).size());

		int i;

		//declaration and initialization of variables
		learning_cost = 0;
		oneDimCost = 0;
		double max_cost=0, min_cost = OptimalCost[totalPoints-1]+1;
		int [] min_cost_index = new int[dimension];
		double [] sel_max = new double[dimension];


		/*
		 * to store the max selectivity each dimension  can learn in a contour
		 */
		for(int d=0;d<dimension;d++) sel_max[d] = -1;

		/*
		 * store the plan/point corresponding to the sel_max locations in a hashmap
		 * for which the key is the dimension
		 */
		HashMap<Integer,point_generic> points_max = new HashMap<Integer,point_generic>();
		//end of declaration of variables
		int currentContourPoints = 0;

		for(int c=0; c <ContourPointsMap.get(contour_no).size();c++){

			point_generic p = ContourPointsMap.get(contour_no).get(c);

			/*
			 * update the max and the min cost seen for this contour
			 * also update the unique_plans for the contour
			 */
			unique_plans.add(p.get_plan_no());
			if(p.get_cost()>max_cost)
				max_cost = p.get_cost();
			if(p.get_cost()<min_cost){
				min_cost = p.get_cost();
				for(int d=0;d<dimension;d++)
					min_cost_index[d] = p.get_dimension(d);
			}

			assert(min_cost<=max_cost) : funName+"min cost is less than or equal to max. cost in the contour";


			//Settings: for higher just see if you want to comment this
			if(inFeasibleRegion(convertIndextoSelectivity(p.get_point_Index()))){
				currentContourPoints ++;
				Integer learning_dim = new Integer(p.getLearningDimension());
				assert (remainingDim.contains(learning_dim)) : "error: learning dimension already learnt";
				if(selectivity[p.get_dimension(learning_dim.intValue())] > sel_max[learning_dim.intValue()]){
					if(points_max.containsKey(learning_dim))
						points_max.remove(learning_dim);
					points_max.put(learning_dim, p);
					sel_max[learning_dim.intValue()] = selectivity[p.get_dimension(learning_dim.intValue())]; 	
				}
			} //end for inFeasibleRegion
		}
		//
		/*
		 * it might happen that due to grid issues that even though q_a lies below a
		 * contour, the contour points could be empty. Hence we put a check condition
		 * that if the cost of the contour is greater than cost(q_a) then add
		 * atleast one point which is the max for all the remaining dimensions
		 */
		System.out.println("Current Contour Points is "+currentContourPoints );
		double q_a_cost = cost_generic(convertSelectivitytoIndex(actual_sel));
		double c_min = getOptimalCost(0);
		//&& (Math.pow(2, contour_no-1)*c_min <= q_a_cost)
		if(currentContourPoints  ==0) {
			if((Math.pow(2, contour_no)*c_min >= q_a_cost)){
				int [] arr = new int[dimension];	
				//update the learnt dimensions selectivity
				for(int d=0;d<dimension;d++){
					if(remainingDim.contains(d))
						arr[d] = resolution-1;
					else 
						arr[d] = findNearestPoint(actual_sel[d]);
				}
				if(false){ //no use checking this. This is 
					//if(cost_generic(arr)>Math.pow(2, contour_no)*c_min){
					//learning_cost = 0;
					//return; //skipping the contour;
					//					System.out.println("The cost at  the error is "+ cost_generic(arr));
					//					assert(false) : funName+" ERROR from the boundary point";
				}
				else {
					point_generic p = new point_generic(arr, getPlanNumber_generic(arr), cost_generic(arr), remainingDim);
					//ContourPointsMap.get(contour_no).add(p);
					max_cost = min_cost = p.get_cost();
					unique_plans.add(p.get_plan_no());
					p.reloadOrderList(remainingDim);
					int learning_dim = p.getLearningDimension();
					sel_max[learning_dim] = selectivity[arr[learning_dim]];
					points_max.put(new Integer(learning_dim),p );
				}

			}
			else
				System.out.println(funName+" Skipping the contour");
		}	
		//TODO put in an assert saying that the same plan cannot be part of sel_max 
		// of different dimensions: Ans: Done
		int lastItr = -1;
		for(int d: remainingDim){
			if(lastItr == -1){
				lastItr = d;
				continue;
			}
			if(points_max.get(new Integer(d))!=null && points_max.get(new Integer(lastItr))!=null)
				assert(points_max.get(new Integer(d)).get_plan_no() != points_max.get(new Integer(lastItr)).get_plan_no()) : funName+" the same plan is spilling on different dimensions";			
				lastItr = d;
		}


		System.out.print("Max cost = "+max_cost+" Min cost = "+min_cost+" ");
		System.out.println("with Number of Unique plans = "+unique_plans.size());


		System.out.print("MinCostIndex = ");
		for(int d=0;d<dimension;d++)
			System.out.print(min_cost_index[d]+",");

		System.out.println();



		for(int d=0;d<dimension;d++)
			System.out.println(d+"_max : "+sel_max[d]);

		System.out.print("MaxIndex = ");
		for(int d=0;d<dimension;d++)
			System.out.print(maxIndex[d]+",");

		System.out.print("\nMinIndex = ");	
		for(int d=0;d<dimension;d++)
			System.out.print(minIndex[d]+",");
		System.out.println();
		for(int d=0;d<dimension;d++){

			if(remainingDim.contains(d))
			{				
				if(sel_max[d] != (double)-1){  //TODO is type casting Okay?: Ans: It is fine


					double sel = 0;
					point_generic p = points_max.get(new Integer(d));
					//sel = Simulated_Spilling(max_x_plan, cg_obj, 0, cur_val);

					/*
					 * checking if we had already executed the same plan on the same dimension before.
					 * 
					 */
					if(executions.contains(new Pair(new Integer(d),new Integer(p.get_plan_no()))))
						continue;


					sel = getLearntSelectivity(d,(Math.pow(2, contour_no-1)*getOptimalCost(0)), p);
					//					if(sel_max[d]<=sel)
					if(currentContourPoints!=0)
						sel_max[d] = sel;
					else{
						assert(sel_max[d]>=sel) : "When Contour Point is zero: getLearntSelectivity is higher than sel[resolution]";
					}

					//else
					//	System.out.println("GetLeantSelectivity: postgres selectivity is less");

					/*
					 * add the tuple (dimension,plan) to the executions data structure
					 */
					executions.add(new Pair(new Integer(d),new Integer(p.get_plan_no())));


					/*
					 * update the number of execution and repeat steps here
					 */
					no_executions ++;
					if(already_visited[d]==true)
						no_repeat_executions++;
					already_visited[d] = true;

					//					File file = new File(cardinalityPath+"spill_cost");
					//					FileReader fr = new FileReader(file);
					//					BufferedReader br = new BufferedReader(fr);
					//					learning_cost += Double.parseDouble(br.readLine());
					//					System.out.println("Cost of the spilled execution is "+learning_cost);
					//					br.close();
					//					fr.close();

					if(sel_max[d]>=actual_sel[d]){
						System.out.print("\n Plan "+p.get_plan_no()+" executed at ");
						for(int m=0;m<dimension;m++) System.out.print(p.get_dimension(m)+",");
						System.out.println(" and learnt "+d+" dimension completely");
						minIndex[d] = maxIndex[d] = findNearestSelectivity(actual_sel[d]);
						remainingDim.remove(remainingDim.indexOf(d));
						removeDimensionFromContourPoints(d);
						return;
					}

					System.out.print("\n Plan "+p.get_plan_no()+" executed at ");
					for(int m=0;m<dimension;m++) System.out.print(p.get_dimension(m)+",");
					System.out.println(" and learnt "+sel_max[d]+" selectivity for "+d+"dimension");
					//assert()
				}
			}


		}

		/* should this come here or at the end of the earlier for loop?
		 *  Ans: As per the theory algorithm, this should come here only since there is no pruning while executing intra contour plans
		 */   	

		//to avoid the inter contour pruning		
		for(int d=0;d<dimension;d++){
			if(remainingDim.contains(d) &&  sel_max[d]!=(double)-1 && findNearestSelectivity(sel_max[d])<findNearestSelectivity(actual_sel[d]))
				if(sel_max[d]>minIndex[d])
					minIndex[d] = sel_max[d];
		}

	}
	private void removeDimensionFromContourPoints(int d) {
		String funName = "removeDimensionFromContourPoints";
		//TODO should we have to check for empty contours and points? 
		for(int c=1;c<=ContourPointsMap.size();c++){
			for(point_generic p: ContourPointsMap.get(c)){
				p.remove_dimension(d);
			}
		}

	}

	
	
	public double getLearntSelectivity(int dim,  double cost,point_generic p) {

		Connection conn=null;
		
        boolean hashjoinFlag =false;
        String funName = "getLearntSelectivity";
        if(remainingDim.size()==1)
        {
                System.out.println(funName+"ERROR: entering one dimension condition");
                return 0;   //TODO dont do spilling in the 1D case, until we fix the INL case
        }
                
        
        double multiplier = 1,selLearnt = Double.MIN_VALUE,execCost=Double.MIN_VALUE, prevExecCost = Double.MIN_VALUE;
        boolean sel_completely_learnt = false;
        int plan = p.get_plan_no();
    Statement stmt = null;

    try {
    	
    	conn = source.getConnection();
        stmt = conn.createStatement();
        //System.out.println(funName+ " : database connection statement create successfully");
        //Settings: constants in BinaryTree   
                BinaryTree tree = new BinaryTree(new Vertex(0,-1,null,null),null,null);
                tree.FROM_CLAUSE = FROM_CLAUSE;
                int spill_values [] = tree.getSpillNode(dim,plan,"tpcds"); //[0] gives node id of the tree and [1] gives the spill_node for postgres
                int spill_node = spill_values[1];
                String spill_pred = tree.predicates[dim];
                String spill_pred_rev = tree.predicatesRev[dim];
                System.out.println("The spill node value in the tree is "+spill_node);
                System.out.println("The spill node predicate in the tree is "+ spill_pred);
                /*
                 *temp_act_sel to iterate from point p to  until either we exhaust the budget 
                 *or learn the actual selectivity. Note the change. 
                 */
                double[] temp_act_sel = new double[dimension];
                for(int d=0;d<dimension;d++){
                        if(dim==d){
//                              if(selectivity[p.get_dimension(d)] >= actual_sel[d])
//                                      temp_act_sel[d] = actual_sel[d]; //This line posed a problem, and hence commenting it. 
//                              else
                                        temp_act_sel[d] = selectivity[p.get_dimension(d)];
                        }
                        else
                                temp_act_sel[d] = actual_sel[d];
                }
                
                double budget = cost;   //if this contour is the last for point p set the budget to  point's cost
                if((p.get_cost() <2*cost) && (p.get_cost()>cost))
                        budget = p.get_cost();

                File file=null;
                FileReader fr=null;
                BufferedReader br=null;
                String str1;
                String regx;
                Pattern pattern;
                Matcher matcher;
                Double hash_rows=-1.0;
                int c_no = getContourNumber(cost);
                Index point_index = new Index(p,c_no,dim);
                System.out.println(point_index.hashCode());
                System.out.println(point_index.equals(new Index(p,c_no,dim)));
                if(sel_for_point.containsKey(point_index))
        			{
        				ArrayList<Double> al = sel_for_point.get(new Index(p,c_no,dim));
        				//arraylist al contains the <sel_learnt,cost_incurred> pair for the above inputs
        				learning_cost += al.get(1);  
        				return al.get(0);
        			}
                
                while((temp_act_sel[dim] <= actual_sel[dim]) || (execCost<=budget))
                {       
                        stmt.execute("set spill_node = "+ spill_node);
                        
                        stmt.execute("set work_mem = '100MB'");
                        //NOTE,Settings: 4GB for DS and 1GB for H
                        if(database_conn==0){
                                stmt.execute("set effective_cache_size='1GB'");
                        }
                        else{
                                stmt.execute("set effective_cache_size='4GB'");
                        }
                        
                        //NOTE,Settings: need not set the page cost's
                        stmt.execute("set  seq_page_cost = 1");
                        stmt.execute("set  random_page_cost=4");
                        stmt.execute("set cpu_operator_cost=0.0025");
                        stmt.execute("set cpu_index_tuple_cost=0.005");
                        stmt.execute("set cpu_tuple_cost=0.01");        
                        stmt.execute("set full_robustness = on");
                        stmt.execute("set oneFPCfull_robustness = on");
                        stmt.execute("set varyingJoins = "+varyingJoins);

                        for(int d=0;d<dimension;d++){
                                stmt.execute("set JS_multiplier"+(d+1)+ "= "+ JS_multiplier[d]);
                                stmt.execute("set robust_eqjoin_selec"+(d+1)+ "= "+ selectivity[p.get_dimension(d)]);
                                stmt.execute("set FPC_JS_multiplier"+(d+1)+ "= "+ JS_multiplier[d]);
                                stmt.execute("set FPCrobust_eqjoin_selec"+(d+1)+ "= "+ temp_act_sel[d]);
                                // essentially forcing the  plan optimal at (x,y) location to the query having (x_a,y_a) 
                                // as selectivities been injected 
                        }

                        ResultSet rs = stmt.executeQuery(query);
                        if(spill_node == 543){
                        	System.out.println("We are at node 23");
                        }
                        while(rs.next()){
                        	
                        			str1 = rs.getString(1);
                        			if(str1.contains(spill_pred) || str1.contains(spill_pred_rev)){
                        				//System.out.println(str1);
                        				hashjoinFlag = str1.contains("Hash Join");
                        				
                        				if(hashjoinFlag){
                        					regx = Pattern.quote("rows=") + "(.*?)" + Pattern.quote("width=");
                            				
                            				pattern = Pattern.compile(regx);
                            				 matcher = pattern.matcher(str1);
                            				while(matcher.find()){
                            					hash_rows = Double.parseDouble(matcher.group(1));	
                            				}
                        					
                        				}
                        				
                        				regx = Pattern.quote("..") + "(.*?)" + Pattern.quote("rows=");
                        				
                        				pattern = Pattern.compile(regx);
                        				matcher = pattern.matcher(str1);
                        				while(matcher.find()){
                        					execCost = Double.parseDouble(matcher.group(1));	
                        				}
                        				
                        						
                        						
                        				break;
                        			}
                        }
                        //read the selectivity returned
 //                       file = new File(cardinalityPath+"spill_cost");
//                        fr = new FileReader(file);
//                        br = new BufferedReader(fr);
                        //read the selectivity or the info needed for INL
                        //--------------------------------------------------------------
//                      double  execCost_old = Double.parseDouble(br.readLine());
//                      System.out.println("execCost = "+execCost+ ", execCost_old ="+execCost_old);
                        //if(execCost< execCost_old  || execCost > execCost_old) {System.out.println("old and new execution cost are not matching"); System.exit(1);}
                        //execCost ++;
//                       assert(execCost == execCost_old) : "exec costs are not matching";
                       
                        // assert(1==0);
                        // Commenting this assert since it is possible that execCost is greater than the budget due to Grid issues
                        //For instance, a 6K cost point can be part of 2K cost contour
                        // assert(execCost<=budget) : funName+" execution cost of spilling is greater than the optimal cost at that position";

                        String valstring = new String();
                        /*
                         * If there are two rows in the file then it is the hash join case. Where the second row is the number of rows
                         * fetched by the predicate at p.getDimension(dim) selectivity 
                         */
//                        if((valstring = br.readLine()) != null){
                    if(hashjoinFlag==true){
                                assert(hashjoinFlag == true);
//                                double rows_old = Double.parseDouble(valstring);
                        	    double rows = hash_rows;
//                        	    assert(rows_old == rows):  "old and new hash rows are not matching";
                                double remainingBudget = budget -execCost; 
                                
                                if(remainingBudget<=0)
                                        break;
                                //assert(newrows>=rows) : funName+" hashjoin case ";
                                //0.01 is used since this is the cost taken for outputting every tuple (=cpu_tuple_cost) 
                                double  budget_needed= (rows*0.01)*(actual_sel[dim]/selectivity[p.get_dimension(dim)] -1);
                                if(budget_needed <=0){
                                        sel_completely_learnt = true;
                                         //already p is at actual_sel[dim]
                                }
                                else if(budget_needed > remainingBudget){
        
                                        /*
                                         * temp_act_sel[dim] contains the selectivity that would have been learnt
                                         * with the remaining budget
                                         */
                                        temp_act_sel[dim] = selectivity[p.get_dimension(dim)] + (actual_sel[dim]- selectivity[p.get_dimension(dim)])*(remainingBudget/budget_needed);
                                
                                        assert(temp_act_sel[dim] > selectivity[p.get_dimension(dim)]) : funName+" there is no increment in learnt selectivity even with increase in budget";
                                        
                                        if(findNearestSelectivity(temp_act_sel[dim])!=temp_act_sel[dim]){
                                                //move to the lower indexed selectivity
                                                int idx = findNearestPoint(temp_act_sel[dim]);
                                                idx --;
                                                assert(idx>=0) : funName+" index going below 0";
                                                 temp_act_sel[dim] = selectivity[idx];
                                        }
                                        selLearnt = temp_act_sel[dim];
                                        double extra_budget_consumed = (rows*0.01)*(temp_act_sel[dim]/selectivity[p.get_dimension(dim)] -1);
                                        prevExecCost = execCost + extra_budget_consumed;
                                                
                                }
                                else if(budget_needed <= remainingBudget){
                                        sel_completely_learnt = true;
                                        execCost += budget_needed;
                                }
                                break;
                        }
                        
                        //-----------------------------------------------------------------
                        /*
                         * this is the case when the current execution exceeds the budget and learnt selectivity is as per the
                         * earlier loop's selectivity of dim. The learnt selectivity is strictly less than actual selectivity. This is 
                         * because if the learnt selectivity (earlier iteration's) is >= than the actual sel, the loop should have finished 
                         * in the earlier iteration itself. This is anyway conservative. 
                         */
                        if(execCost>budget) //TODO: should we add a small threshold? may be use the next point's cost
                                break;
                        /*
                         * this is the case when we learn the actual selectivity since the execCost (subplan's cost) does not 
                         * exceed the plan's total budget
                         */
                        if(findNearestPoint(temp_act_sel[dim]) >= findNearestPoint(actual_sel[dim])){
                                sel_completely_learnt = true;
                                break;
                        }
                        prevExecCost = execCost;
                        
                        //adding the code for oneshot learning of predicate
                        
                        int idx = findNearestPoint(temp_act_sel[dim]);
                        idx ++;
                        assert(idx<resolution) : funName+" index exceeding resolution";
                        temp_act_sel[dim] = selectivity[idx];
                }

//                if(br!=null)
//                        br.close();
//                if(fr!=null)
//                        fr.close();
        
                
        if(sel_completely_learnt){                      
                learning_cost += execCost;
                selLearnt = temp_act_sel[dim];
                        System.out.println("Cost of the spilled execution is "+execCost);
                prevExecCost = execCost;
        }
        else{
                if(prevExecCost==Double.MIN_VALUE){
                        learning_cost += cost; //just adding the cost of the contour in the while loop zips through in the starting iteration itself
                        prevExecCost = cost; //for the sake of printing
                        selLearnt = selectivity[p.get_dimension(dim)];
                }
                else{
                        //prevExecCost = cost;
                        learning_cost += prevExecCost; //actual cost taken for learning selecitvity
                }
                        
                System.out.println("Cost of the spilled execution (not sucessful) is "+prevExecCost);
                int idx = findNearestPoint(temp_act_sel[dim]);
                if(!hashjoinFlag && idx>0){     
                        idx --;
                        assert(idx>=0): funName+" idx going below 0";
                        selLearnt = selectivity[idx];
                }
                execCost = prevExecCost;
        }
    }
    catch ( Exception e ) {
        System.err.println( e.getClass().getName()+": "+ e.getMessage() );
         e.printStackTrace();

    } finally{
    	if (conn != null) {
	        try { conn.close(); } catch (SQLException e) {}
	    }
    }
    
        assert(dim<=dimension) : funName+" dim data structure more dimensions possible";
        assert(selLearnt<=(double)1):funName+"selectivity learnt is greater than 1!";
        System.out.println("Postgres: selectivity learnt  "+selLearnt+" with plan number "+plan);
       
        if(!sel_for_point.containsKey(new Index(p,getContourNumber(cost),dim)) && !sel_completely_learnt){
        	 ArrayList<Double> l = new ArrayList<Double>();
             l.add(selLearnt); l.add(execCost);
                sel_for_point.put(new Index(p,getContourNumber(cost),dim), l);
        }
        return selLearnt; //has to have single line in the file
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
						isContourPoint[getIndex(arr, resolution)] = true;

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
							isContourPoint[getIndex(arr, resolution)] = true;
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
				
//				Have used this for all the TPCDS templates with 100 res to ICDE paper 
				
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
				selectivity[95] = 0.697865; 	selectivity[96] = 0.755812; 	selectivity[97] = 0.818569; 	selectivity[98] = 0.886535; 	selectivity[99] = 0.960142; 	
				
				
				//using this for alignment
				
//				selectivity[0] = 0.00001006; 	selectivity[1] = 0.00001020; 	selectivity[2] = 0.00001035; 	selectivity[3] = 0.00001053; 	selectivity[4] = 0.00001074; 	
//				selectivity[5] = 0.00001098; 	selectivity[6] = 0.00001126; 	selectivity[7] = 0.00001157; 	selectivity[8] = 0.00001194; 	selectivity[9] = 0.00001235; 	
//				selectivity[10] = 0.00001283; 	selectivity[11] = 0.00001339; 	selectivity[12] = 0.00001402; 	selectivity[13] = 0.00001475; 	selectivity[14] = 0.00001559; 	
//				selectivity[15] = 0.00001656; 	selectivity[16] = 0.00001767; 	selectivity[17] = 0.00001895; 	selectivity[18] = 0.00002042; 	selectivity[19] = 0.00002211; 	
//				selectivity[20] = 0.00002406; 	selectivity[21] = 0.00002629; 	selectivity[22] = 0.00002887; 	selectivity[23] = 0.00003182; 	selectivity[24] = 0.00003522; 	
//				selectivity[25] = 0.00003914; 	selectivity[26] = 0.00004363; 	selectivity[27] = 0.00004881; 	selectivity[28] = 0.00005476; 	selectivity[29] = 0.00006160; 	
//				selectivity[30] = 0.00006947; 	selectivity[31] = 0.00007851; 	selectivity[32] = 0.00008892; 	selectivity[33] = 0.00010088; 	selectivity[34] = 0.00011464; 	
//				selectivity[35] = 0.00013047; 	selectivity[36] = 0.00014866; 	selectivity[37] = 0.00016959; 	selectivity[38] = 0.00019366; 	selectivity[39] = 0.00022133; 	
//				selectivity[40] = 0.00025316; 	selectivity[41] = 0.00028976; 	selectivity[42] = 0.00033186; 	selectivity[43] = 0.00038026; 	selectivity[44] = 0.00043593; 	
//				selectivity[45] = 0.00049995; 	selectivity[46] = 0.00057357; 	selectivity[47] = 0.00065823; 	selectivity[48] = 0.00075559; 	selectivity[49] = 0.00086756; 	
//				selectivity[50] = 0.00099632; 	selectivity[51] = 0.00114440; 	selectivity[52] = 0.00131469; 	selectivity[53] = 0.00151052; 	selectivity[54] = 0.00173572; 	
//				selectivity[55] = 0.00199471; 	selectivity[56] = 0.00229254; 	selectivity[57] = 0.00263505; 	selectivity[58] = 0.00302893; 	selectivity[59] = 0.00348190; 	
//				selectivity[60] = 0.00400281; 	selectivity[61] = 0.00460186; 	selectivity[62] = 0.00529077; 	selectivity[63] = 0.00608302; 	selectivity[64] = 0.00699410; 	
//				selectivity[65] = 0.00804184; 	selectivity[66] = 0.00924674; 	selectivity[67] = 0.01063238; 	selectivity[68] = 0.01222587; 	selectivity[69] = 0.01405837; 	
//				selectivity[70] = 0.01616576; 	selectivity[71] = 0.01858925; 	selectivity[72] = 0.02137626; 	selectivity[73] = 0.02458133; 	selectivity[74] = 0.02826716; 	
//				selectivity[75] = 0.03250586; 	selectivity[76] = 0.03738036; 	selectivity[77] = 0.04298605; 	selectivity[78] = 0.04943258; 	selectivity[79] = 0.05684610; 	
//				selectivity[80] = 0.06537164; 	selectivity[81] = 0.07517601; 	selectivity[82] = 0.08645104; 	selectivity[83] = 0.09941733; 	selectivity[84] = 0.11432855; 	
//				selectivity[85] = 0.13147646; 	selectivity[86] = 0.15119656; 	selectivity[87] = 0.17387467; 	selectivity[88] = 0.19995450; 	selectivity[89] = 0.22994631; 	
//				selectivity[90] = 0.26443688; 	selectivity[91] = 0.30410104; 	selectivity[92] = 0.34971482; 	selectivity[93] = 0.40217067; 	selectivity[94] = 0.50249490; 	
//				selectivity[95] = 0.60186777; 	selectivity[96] = 0.70164656; 	selectivity[97] = 0.80339217; 	selectivity[98] = 0.90889962; 	selectivity[99] = 0.99500000; 	

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
			dimension = Integer.parseInt(prop.getProperty("dimension").trim());
			
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

	public  void nexusAlgoContour(double searchCost) throws IOException 
	{
		int seed = 0;
	
		seed = findSeedLocation(searchCost);
		
		exploreSeed(seed, searchCost, dimension-1, dimension-2);
	}

	public int findSeedLocation(double seedCost) throws IOException  
	{
		point_generic leftInfo, rightInfo, midInfo;
		int leftcorner = 0;
		int[] left = createCorner(leftcorner);

		int rightcorner = 1;
		int[] right = createCorner(rightcorner);

		/*
		 * The following If condition checks whether any earlier point in all_contour_points 
		 * had the same plan. If so, no need to open the .../predicateOrder/plan.txt again
		 */

		leftInfo = new point_generic(left,getPlanNumber_generic(left),cost_generic(left), remainingDim);
		double leftCost = leftInfo.get_cost();
		
		rightInfo = new point_generic(right,getPlanNumber_generic(right),cost_generic(right), remainingDim);
		double rightCost = rightInfo.get_cost(); 
		
		int d=dimension-1;
				
		while(rightCost < seedCost && d>0)  
		{
			copyLoc(left, right);
			leftCost = rightCost;
			right[--d] = resolution-1;
			rightInfo = new point_generic(right,getPlanNumber_generic(right),cost_generic(right), remainingDim);
			rightCost = rightInfo.get_cost(); 
		}
	
		//now search between left and right using binary search
		int mid[] = new int[dimension];
		double midCost = -1;
		while(leftCost < rightCost)
		{
			for(d=0; d<dimension; d++)
				mid[d] = (left[d]+right[d])/2;
	
			if(sameLoc(mid,left)) 
			{
				break;
			}
			else
			{
				midInfo = new point_generic(mid,getPlanNumber_generic(mid),cost_generic(mid), remainingDim);
				midCost = midInfo.get_cost();
	
				if(midCost >= seedCost) 
				{
					copyLoc(right, mid);
					rightInfo = midInfo;
					rightCost = midCost;
				}
				else 
				{
					copyLoc(left, mid);
					leftInfo = midInfo;
					leftCost = midCost;
				}
			}
		}
	
		System.out.println("\n Found seed location with cost " + seedCost + " as "
							+ getIndex(right, resolution) + " with cost = " + rightCost);
		if(!pointAlreadyExist(rightInfo.dim_values))	
			all_contour_points.add(rightInfo);
		
		isContourPoint[getIndex(rightInfo.dim_values, resolution)] = true;		

		return getIndex(right, resolution);
	}
	
	
	public void exploreSeed(int cur_seed, double search_cost, int focus_dim, int swap_dim) throws IOException{
		int[] seed_coords;
		int[] candidate1_coords;
		int[] candidate2_coords;

		while(cur_seed >= 0 && focus_dim > 0) 
		{	
			seed_coords = getCoordinates(dimension, resolution, cur_seed);

			candidate1_coords = nextLocDim(seed_coords, swap_dim); // find next location by increasing value along swapDim

			while(candidate1_coords[0] < 0 && swap_dim > 0) // cant increase anymore along swapdim
			{
				swap_dim--;                               
				candidate1_coords = nextLocDim(seed_coords, swap_dim);
			}

			if(swap_dim == 0)
			{
				swap_dim = focus_dim-1;
			}

			// find candidate2 location
			candidate2_coords = prevLocDim(seed_coords, focus_dim);

			if(candidate2_coords[0] < 0)		// reached boundary - candidate2[0] < 0 at that scenario
			{
				return;
			}
			else if(candidate2_coords[0] >= 0 && candidate1_coords[0] < 0) 
			{					
				point_generic seedInfo =  new point_generic(candidate2_coords,getPlanNumber_generic(candidate2_coords),cost_generic(candidate2_coords), remainingDim);
				int newSeed = getIndex(seedInfo.dim_values,resolution);

				if( seedInfo.get_cost() < search_cost )
					return;
				else
				{
					if(!pointAlreadyExist(seedInfo.dim_values))
						all_contour_points.add(seedInfo);
					if(swap_dim > 0)
					{
						exploreSeed(newSeed, search_cost, focus_dim-1,swap_dim-1);					
					}
				}
				return;	
			}

			// get cost of candidate locations
			point_generic cand1Info = new point_generic(candidate1_coords,getPlanNumber_generic(candidate1_coords),cost_generic(candidate1_coords), remainingDim);
			point_generic cand2Info = new point_generic(candidate2_coords,getPlanNumber_generic(candidate2_coords),cost_generic(candidate2_coords), remainingDim);
			point_generic seedInfo;

			// assign next seed location
			if (cand1Info.get_cost() > search_cost && cand2Info.get_cost() >= search_cost)
			{			
				seedInfo = cand2Info;
			}
			else if (cand1Info.get_cost() >= search_cost && cand2Info.get_cost() < search_cost)
			{			
				seedInfo = cand1Info;
			}
			else
			{
				int d1 = (int)Math.abs(cand1Info.get_cost() - search_cost);
				int d2 = (int)Math.abs(cand2Info.get_cost() - search_cost);
				if(d1 < d2)
					seedInfo = cand1Info;
				else
					seedInfo = cand2Info;
			}

			if(!pointAlreadyExist(seedInfo.dim_values))
				all_contour_points.add(seedInfo);

			cur_seed = getIndex(seedInfo.dim_values,resolution);

			if(swap_dim > 0)
			{
				exploreSeed(getIndex(seedInfo.dim_values,resolution), search_cost, focus_dim-1, swap_dim-1);				
			}
		}
		System.out.println("Nexus exploration done !");
	}

	//find the next location wrt a specific dimension
	public int[] nextLocDim(int index[], int dim)
	{
		int newIndex[] = new int[dimension];
		copyLoc(newIndex, index);
		if(index[dim] < resolution-1)
			newIndex[dim] = index[dim] + 1;
		else
			newIndex[0] = -1;		// end - cant move anymore
	
		return newIndex;
	}

	//find the previous location wrt a specific dimension
	public int[] prevLocDim(int index[], int dim)
	{
		int newIndex[] = new int[dimension];
		copyLoc(newIndex,index);
		if(index[dim] > 0)
			newIndex[dim] = index[dim]-1;
		else
			newIndex[0] = -1;	// end - cant move anymore
	
		return newIndex;
	}

	
	public void copyLoc(int destIdx[], int srcIdx[])
	{
		for(int d=0; d<dimension; d++)
			destIdx[d] = srcIdx[d];
	}
	
	public boolean sameLoc(int[] index1, int[] index2) 
	{
		for(int d=0; d<dimension; d++)
			if(index1[d] != index2[d])
				return false;
	
		return true;
	}

	
	//required in findSeed() routine
	public int[] createCorner(int corner) 
	{
		int index[] = new int[dimension];
		int d=dimension-1;
		
		while(corner > 0) 
		{
			if((corner % 2) > 0) 
			{
				index[d] = resolution-1;
			}
			corner /= 2;
			d--;
		}
		return index;
	}
	

}

class SpillBoundOutputParamStruct {
	double SO = 0;
	double Harm = 0;
	double anshASO = 0;
	boolean flag = true;
}
class SpillBoundinputParamStruct {
	GCI3D obj;
	int index;
	public SpillBoundinputParamStruct(GCI3D obj, int index) {
		this.obj = obj;
		this.index = index;
	}
}

//class point_generic
//{
//	int dimension;
//
//	ArrayList<Integer> order;
//	ArrayList<Integer> storedOrder;
//	int value;
//	int p_no;
//	double cost;
//	static String plansPath;
//
//	int [] dim_values;
//	point_generic(int arr[], int num, double cost,ArrayList<Integer> remainingDim) throws  IOException{
//
//		loadPropertiesFile();
//		//System.out.println();
//		dim_values = new int[dimension];
//		for(int i=0;i<dimension;i++){
//			dim_values[i] = arr[i];
//			//System.out.print(arr[i]+",");
//		}
//		//System.out.println("   having cost = "+cost+" and plan "+num);
//		this.p_no = num;
//		this.cost = cost;
//
//		order =  new ArrayList<Integer>();
//		storedOrder = new ArrayList<Integer>();
//		try{
//			FileReader file = new FileReader(plansPath+num+".txt");
//
//			BufferedReader br = new BufferedReader(file);
//			String s;
//			while((s = br.readLine()) != null) {
//				//System.out.println(Integer.parseInt(s));
//				value = Integer.parseInt(s);
//				storedOrder.add(value);
//
//			}
//			br.close();
//			file.close();
//		}
//		catch(FileNotFoundException e){
//			if(plansPath.contains("SQL")){
//				for(int i=0;i<dimension;i++){
//					storedOrder.add(i);
//				}
//			}
//		}
//
//
//	}
//	
//	
//	point_generic(int arr[], int num, double cost,ArrayList<Integer> remainingDim,ArrayList<Integer> predicateOrder ) throws  IOException{
//
//		loadPropertiesFile();
//		System.out.println();
//		dim_values = new int[dimension];
//		for(int i=0;i<dimension;i++){
//			dim_values[i] = arr[i];
//			//System.out.print(arr[i]+",");
//		}
//		//System.out.println("   having cost = "+cost+" and plan "+num);
//		this.p_no = num;
//		this.cost = cost;
//
//		//check: if the order and stored order are being updated/populated
//		order =  new ArrayList<Integer>(predicateOrder);
//		storedOrder = new ArrayList<Integer>(predicateOrder);		
//	}
//	int getLearningDimension(){
//		if(order.isEmpty())
//			System.out.println("ERROR: all dimensions learnt");
//		return order.get(0);
//	}
//
//	/*
//	 * get the selectivity/index of the dimension
//	 */
//	public int get_dimension(int d){
//		return dim_values[d];
//	}
//
//	/*
//	 * get the plan number for this point
//	 */
//	public int get_plan_no(){
//		return p_no;
//	}
//
//	public double get_cost(){
//		return cost;
//	}
//	public int[] get_point_Index(){
//		return dim_values;
//	}
//	public void remove_dimension(int d){
//		String funName = "remove_dimension";
//		if(order.isEmpty())
//			System.out.println(funName+": ERROR: all dimensions learnt");
//		if(order.contains(d))
//			order.remove(order.indexOf(d));
//		else 
//			System.out.println(funName+": ERROR: removing a dimension that does not exist");
//
//	}
//	public void reloadOrderList(ArrayList<Integer> remainingDim) {
//
//		order.clear();
//		for(int itr=0;itr<storedOrder.size();itr++){
//			if(remainingDim.contains(storedOrder.get(itr)))
//				order.add(storedOrder.get(itr));
//		}
//	}
//	public int get_no_of_dimension(){
//		return dimension;
//	}
//
//	public ArrayList<Integer> getPredicateOrder(){
//		return storedOrder;
//	}
//
//	public void printPoint(){
//		
//		for(int i=0;i<dimension;i++){
//			System.out.print(dim_values[i]+",");
//		}
//		System.out.println("   having cost = "+cost+" and plan "+p_no);
//	}
//	
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
//			plansPath = prop.getProperty("apktPath");
//			plansPath = plansPath+"predicateOrder/";
//			dimension = Integer.parseInt(prop.getProperty("dimension"));
//		} catch (IOException ex) {
//			ex.printStackTrace();
//		}
//	}
//
//}

//
//final class Pair<T> {
//
//	final T left;
//	final T right;
//
//	public Pair(T left, T right)
//	{
//		if (left == null || right == null) { 
//			throw new IllegalArgumentException("left and right must be non-null!");
//		}
//		this.left = left;
//		this.right = right;
//	}
//
//	public boolean equals(Object o)
//	{
//		// see @maaartinus answer
//		if (! (o instanceof Pair)) { return false; }
//		Pair p = (Pair)o;
//		return left.equals(p.left) && right.equals(p.right);
//	} 
//
//	public int hashCode()
//	{
//		return 7 * left.hashCode() + 13 * right.hashCode();
//	} 
//}

