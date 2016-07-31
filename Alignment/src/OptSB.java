/*
 * OptSB
 * To run :
 * 1. Check the Flags section and set the appropriate flags.
 * 2. Change the QT template name in the global variables "QTName" and "plansPath"
 * 3. Check whether constants.property file is correct in the folder of the queryTemplate to be used.
 * 
 */



import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
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
//import  org.apache.commons.io.FileUtils;








import org.postgresql.jdbc3.Jdbc3PoolingDataSource;

import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;


public class OptSB
{
	static int UNI = 1;
	static int EXP = 2;

	static double err = 0.0;

	static double AllPlanCosts[][];
	static int nPlans;
	static int plans[];
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
	static String varyingJoins;
	static double JS_multiplier [];
	static String query;
	static String query_opt_spill;
	static String cardinalityPath;
	static String QTName = "HQT75DR10_E";
	static String plansPath = "/media/dsladmin/dslssd512gb/Lohit/QT_April_idea/HQT75DR10_E/";
	static String sourcePath = "./src/";
	static boolean MSOCalculation = true;
	static boolean randomPredicateOrder = false;
	static ArrayList<ArrayList<Integer>> allPermutations = new ArrayList<ArrayList<Integer>>();
	static ArrayList<point_generic> all_contour_points = new ArrayList<point_generic>();
	static ArrayList<Integer> learntDim = new ArrayList<Integer>();
	static HashMap<Integer,Integer> learntDimIndices = new HashMap<Integer,Integer>();
	//	point[][] points_list;
	static plan[] plans_list;
	//Settings: 
	static int sel_distribution; 
	static boolean FROM_CLAUSE;
	static Connection conn = null;
	static int database_conn=1;
	static double h_cost;
	static double planCount[], planRelativeArea[];
	static double picsel[], locationWeight[];

	static double areaSpace =0,totalEstimatedArea = 0;

	//-----------------------------------FLAGS
	// Use the plansPath as ./src/ --> This is for making jar and running it
	static boolean src_flag=true;
	
	// If true then all the printf would be displayed
	static boolean print_flag = true;
	
	//Print the loop number in  the getContourPoints function!.
	static boolean print_loop_flag = true;
	
	//The hash join optimization in the getLearntSelectivity
	static boolean hash_join_optimization_flag=true;
	
	//Use the best local partitioning if set to true.
	static boolean local_partition_flag = true;
	
	//The class memo would be used to do memoization of the points of execution. It is used in getLearntSelectivity. If set to true, it will speed up the process of finding MSO.
	static boolean memoization_flag=false;
	static boolean singleThread = false;
	static boolean allPlanCost = true;
	static boolean generateSpecificContour = false;	
	static boolean Nexus_algo = false;
	static boolean FPC_for_Alignment = true;
	static int genContourNumber = -1;
	static boolean AlignmentPenaltyCode = false;
	static boolean DEBUG = true;
	static boolean spill_opt_for_Alignment = false;
	static boolean contoursReadFromFile = false;
	//---------------------------------------------------------
	
	
	
	//The following parameters has to be set manually for each query
	double tmax = 0;
	double pmax = 0;


	double  minIndex [];
	double  maxIndex [];

	int currentContourPoints=0;
	double min_cost=Double.MAX_VALUE;
	double max_cost=Double.MIN_VALUE;


	ArrayList<Integer> remainingDim;
	HashMap<Integer,ArrayList<point_generic>> ContourPointsMap = new HashMap<Integer,ArrayList<point_generic>>();
	ArrayList<Pair<Integer>> executions = new ArrayList<Pair<Integer>>();  
	HashMap<Index,ArrayList<Double>> sel_for_point = new HashMap<Index,ArrayList<Double>>();
	 HashMap<Integer,ArrayList<Integer>> cur_partition = new HashMap<Integer,ArrayList<Integer>>();
//	static HashMap<Index,memo> sel_for_point = new HashMap<Index,memo>();
	double learning_cost = 0;
	double oneDimCost = 0;
	int no_executions = 0;
	int no_repeat_executions = 0;
	int max_no_executions = 0;
	int max_no_repeat_executions = 0;

	boolean [] already_visited;  

	point_generic []maxPoints;

	double[] actual_sel;

	int n_partition_best = 0;

	int n_partition ;
	int[][] p ;
	String part_str ;
	
//	static HashMap<Integer,ArrayList<Integer>> local_part = new HashMap<Integer,ArrayList<Integer>>();
	
	int [] p_encoding = new int[10];
	int [] prev_encoding = new int[10];
	int [] best_partitioning = new int[10];
	int [] until_max = new int [10];	
	ArrayList<Integer> temp_list = new ArrayList<Integer>();
	 partition partition_info[];




	
public OptSB(){}
	
	public OptSB(OptSB obj){
		minIndex = new double[obj.dimension];
		maxIndex = new double[obj.dimension];		
		this.remainingDim = new ArrayList<Integer>(obj.remainingDim);
		this.ContourPointsMap = new HashMap<Integer, ArrayList<point_generic>>();
		HashMap<Integer,ArrayList<Integer>> cur_partition = new HashMap<Integer,ArrayList<Integer>>();
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
		this.sel_for_point = new HashMap<Index, ArrayList<Double>>(); 
		this.learning_cost = 0;
		this.oneDimCost = 0;
		this.no_executions = 0;
		this.no_repeat_executions = 0;
		this.max_no_executions = 0;
		this.max_no_repeat_executions = 0;
		this.already_visited = new boolean[obj.dimension];  
		this.actual_sel = new double[obj.dimension];
		this.maxPoints= new point_generic[dimension];
		//point_generic []maxPoints; 
	}
	public static void main(String args[]) throws IOException, SQLException, InterruptedException, ExecutionException, ClassNotFoundException
	{
		long startTime = System.nanoTime();

		System.out.println("src_flag ="+src_flag);
		System.out.println("print_flag ="+print_flag);
		System.out.println("print_loop_flag ="+print_loop_flag);
		System.out.println("hash_join_optimization_flag ="+hash_join_optimization_flag);
		int threads = Runtime.getRuntime().availableProcessors();
		int num_of_usable_threads = threads;
		//System.out.println("Partitions = "+part_str);
		//int genContourNumber = -1;
		if(generateSpecificContour)
			if(args.length>0)
				genContourNumber = Integer.parseInt(args[0]);
		
		if(src_flag)
		{
			paths_init();
		}

//		for(int i=0;i<n_partition;i++)
//		{
//			temp_list = new ArrayList<Integer>();
//			for(int j=0;j<p[i].length;j++)
//			{
//				int k = p[i][j];
//				temp_list.add(k);
//			}
//			cur_partition.put(i, temp_list);
//			//			temp_list.clear();
//		}

		OptSB obj = new OptSB();
		final OptSB global_obj = new OptSB();
		obj.maxPoints= new point_generic[dimension];

		String pktPath = apktPath + qtName + ".apkt" ;
		System.out.println("Query Template: "+QTName);


		ADiagramPacket gdp = obj.getGDP(new File(pktPath));

		//		CostGreedyOptSB cg = new CostGreedyOptSB();
		//		cg.run(threshold, gdp,apktPath);
		//		ADiagramPacket reducedgdp = cg.cgFpc(threshold, gdp,apktPath);

		//Populate the OptimalCost Matrix.
		if(FPC_for_Alignment && !Nexus_algo)
			obj.readpkt(gdp, true);
		else
			obj.readpkt(gdp, false);
		
		obj.loadPropertiesFile();
		//Populate the selectivity Matrix.
		obj.loadSelectivity();
		//	obj.loadPropertiesFile();
		int i;
		h_cost = obj.getOptimalCost(totalPoints-1);
		double min_cost = obj.getOptimalCost(0);
		System.out.println("Min Cost = "+min_cost);
		double ratio = h_cost/min_cost;
		assert (h_cost >= min_cost) : "maximum cost is less than the minimum cost";
		System.out.println("the ratio of C_max/c_min is "+ratio);
		i = 1;

		//to generate contours
		obj.remainingDim.clear(); 
		for(int d=0;d<obj.dimension;d++){
			obj.remainingDim.add(d);
		}
		
		double cost = obj.getOptimalCost(0);
		
		boolean contoursReadFromFile = true;
		if(!Nexus_algo && !generateSpecificContour && contoursReadFromFile){
			File ContoursFile = new File(apktPath+"contours/Contours.map");
			if(ContoursFile.exists()){
				obj.readContourPointsFromFile();
				global_obj.readContourPointsFromFile();
				contoursReadFromFile = true;
			}
		}
		
		if(!contoursReadFromFile){
			learntDim.clear();
			learntDimIndices.clear();
			
			getAllPermuations(obj.remainingDim,0);
			assert (allPermutations.size() == obj.factorial(obj.dimension)) : "all the permutations are not generated";

			while(cost < 2*h_cost)
			{
				if(cost>h_cost)
					cost = h_cost;
				if(print_flag)
				{
					System.out.println("---------------------------------------------------------------------------------------------\n");
					System.out.println("Contour "+i+" cost : "+cost+"\n");
				}
//				if(i!=genContourNumber && generateSpecificContour){
//					cost = cost*2;
//					i++;
//					continue;
//					
//				}
				all_contour_points.clear();

				if(Nexus_algo)
					obj.nexusAlgoContour(cost); 

				else{
					for(ArrayList<Integer> order:allPermutations){
						//			System.out.println("Entering the order"+order);
						learntDim.clear();
						learntDimIndices.clear();
						obj.getContourPoints(order,cost);
					}
				}
				//Settings
				//writeContourPointstoFile(i);
				//writeContourPlanstoFile(i);
				int size_of_contour = all_contour_points.size();
				obj.ContourPointsMap.put(i, new ArrayList<point_generic>(all_contour_points)); //storing the contour points
				global_obj.ContourPointsMap.put(i, new ArrayList<point_generic>(obj.all_contour_points)); //storing the contour points
				System.out.println("Size of contour"+size_of_contour );
				cost = cost*2;
				i = i+1;
			
			}
			if(generateSpecificContour)
				obj.writeContourMaptoFile();
		}
		
//System.exit(1);
		/*
		 * Setting up the DB connection to Postgres/TPCH/TPCDS Benchmark. 
		 */
		try{
			System.out.println("entered DB conn1");
			Class.forName("org.postgresql.Driver");
			source = new Jdbc3PoolingDataSource();
			source.setDataSourceName("A Data Source");
			File f_ibm = new File("/home/ijcai/spillBound/data/settings/settings.conf");
			File f_multani = new File("/home/dsluser/Srinivas/data/settings/settings.conf");
			File f_pahadi = new File("/home/dsladmin/Srinivas/data/settings/settings.conf");
			File f_ibm84 = new File("/data/spillBound/data/settings/settings.conf");
			File f_tkde2 = new File("/media/ssd256gg/data/settings/settings.conf");
			File f_tkde1 = new File("/media/ssd256g/data/settings/settings.conf");
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
				if(f_pahadi.exists()){
					source.setServerName("localhost:5432");
					source.setDatabaseName("tpcds");
				}
				else if(f_ibm.exists()){
					source.setServerName("localhost:5431");
					source.setDatabaseName("tpcdscodd");
				}
				else if(f_multani.exists()){
					source.setServerName("localhost:5432");
					source.setDatabaseName("tpcdscodd");					
				}
				else if(f_ibm84.exists()){
					source.setServerName("localhost:5431");
					source.setDatabaseName("tpcdscodd");
					}
				else if(f_tkde2.exists()){
					source.setServerName("localhost:5431");
					source.setDatabaseName("tpcdscodd");
					}
				else if(f_tkde1.exists()){
					source.setServerName("localhost:5431");
					source.setDatabaseName("tpcdscodd");
					}
//				conn = DriverManager
//						.getConnection("jdbc:postgresql://localhost:5432/tpcds",
//								"sa", "database");

			}
			source.setUser("sa");
			source.setPassword("database");
			if(singleThread)
				source.setMaxConnections(1);
			else
				source.setMaxConnections(num_of_usable_threads);
			System.out.println("Opened database successfully");
		}
		catch ( Exception e ) {
			System.out.println("entered DB err");
			System.err.println( e.getClass().getName()+": "+ e.getMessage() );
			System.exit(1);

		}
		/*
		 * running the spillBound and plan bouquet algorithm 
		 */
		
		if(AlignmentPenaltyCode)
			obj.checkAlignmentPenalty();

		double MSO =0, ASO = 0,anshASO = 0,SO=0,MaxHarm=-1*Double.MAX_VALUE,Harm=Double.MIN_VALUE;
		double pmax_final=0, tmax_final=0;
		int ASO_points=0;
		obj.getPlanCountArray();
		//int max_point = 0; /*not to execute the spillBound algorithm*/
		int max_point = 1; /*to execute a specific q_a */
		int min_point = 0;
		//Settings
		if(MSOCalculation)
			//if(false)
			max_point = totalPoints;
		double[] subOpt = new double[max_point];
		
		//create logs directory if it does not exist
		File filelog =  new File(apktPath+"logs/");
		if(!filelog.exists())
			assert(filelog.mkdir()) : "was not able to create log directory";
			//FileUtils.cleanDirectory(filelog);

						String[] myFiles;    
						if(filelog.isDirectory()){
							myFiles = filelog.list();
							for (int hi=0; hi<myFiles.length; hi++) {
								File myFile = new File(filelog, myFiles[hi]); 
								myFile.delete();
							}
						}


			if (args.length > 0) {
				/*updating the min and the max index if available*/
				try {
					min_point = Integer.parseInt(args[0]);
					max_point = Integer.parseInt(args[1]);
				} catch (NumberFormatException e) {
					System.err.println("Argument" + args[0] + " must be an integer.");
					System.exit(1);
				}
			}

			
	if(singleThread)
		
	{	
//		min_point = 0;
//		max_point = 1000;
		PrintWriter writer = new PrintWriter(apktPath+"logs/SB_serial_log("+min_point+"-"+max_point+").txt", "UTF-8");
		for (int  j = min_point ; j < max_point ; j++)
//					for (int  j = 21893 ; j < 21984; j++)
		{
			if(print_flag || print_loop_flag)
			{
				 writer.println("Entering loop "+j);
			}
			//		if(j==320)
			//			System.out.println("Interesting");
			//		else
			//			continue;
			//initiliazation for every loop
			//	int j=totalPoints -1;

			double algo_cost =0;
			SO =0;
			//cost = obj.getOptimalCost(0);
			obj.initialize(writer,j);
			int[] index = obj.getCoordinates(obj.dimension, obj.resolution, j);
			//		if(index[0]%5 !=0 || index[1]%5!=0)
			//			continue;
			//Settings:
			//		obj.actual_sel[0] = 0.005;obj.actual_sel[1] = 0.02;obj.actual_sel[2] = 0.005; /*uncomment for single execution*/

			for(int d=0;d<obj.dimension;d++) obj.actual_sel[d] = obj.findNearestSelectivity(obj.actual_sel[d]);

			//If cost of the optimal plan at actual_sel is less than 10000, then skip that point.
			if(obj.cost_generic(obj.convertSelectivitytoIndex(obj.actual_sel))<10000)
				continue;
			//----------------------------------------------------------
			i =1;
			obj.executions.clear();


			int k;
			
			//For each contours -
			h_cost = obj.getOptimalCost(totalPoints-1);
			cost = obj.getOptimalCost(0);
			i=1;
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


				if(print_flag)
				{
					 writer.println("---------------------------------------------------------------------------------------------\n");
					 writer.println("Contour "+i+" cost : "+cost+"\n");
				}
				int prev = obj.remainingDim.size();

				if(prev==1){
					obj.oneDimensionSearch(writer,i,cost);
					obj.learning_cost = obj.oneDimCost;
				}
				else
					obj.spillBoundAlgo(writer,i,j);

				int present = obj.remainingDim.size();
				if(present < prev - 1 || present > prev)
					 writer.println("ERROR");

				algo_cost = algo_cost+ (obj.learning_cost);
				if(present == prev ){    // just to see whether any dimension was learnt
					// if no dimension is learnt then move on to the next contour

					/*
					 * capturing the properties of the query execution
					 */
					if(print_flag)
					{
						 writer.println("In Contour "+i+" has the following details");
						 writer.println("No of executions is "+obj.no_executions);
						 writer.println("No of repeat moves is "+obj.no_repeat_executions);
					}
					if(obj.no_executions>obj.max_no_executions)
						obj.max_no_executions = obj.no_executions;
					if(obj.no_repeat_executions > obj.max_no_repeat_executions)
						obj.max_no_repeat_executions = obj.no_repeat_executions;


					cost = cost*2;  
					i = i+1;
					obj.executions.clear();
					/*
					 * initialize the repeat moves and exections for the next contour
					 */
					for(int d=0;d<obj.dimension;d++){
						obj.already_visited[d] = false;
						obj.no_executions = 0;
						obj.no_repeat_executions =0;
					}

				}
				if(print_flag)
				{
					System.out.println("---------------------------------------------------------------------------------------------\n");
				}

			}  //end of while

			/*
			 * printing the actual selectivity
			 */
			//		System.out.print("\nThe actual selectivity is original \t");
			if(print_flag)
			{
				for(int d=0;d<obj.dimension;d++) 
					 writer.print(obj.actual_sel[d]+",");
			}
			/*
			 * storing the index of the actual selectivities. Using this printing the
			 * index (an approximation) of actual selectivities and its cost
			 */
			int [] index_actual_sel = new int[obj.dimension]; 
			for(int d=0;d<obj.dimension;d++) index_actual_sel[d] = obj.findNearestPoint(obj.actual_sel[d]);

			if(print_flag)
			{
				 writer.print("\nCost of actual_sel ="+obj.cost_generic(index_actual_sel)+" at ");
				for(int d=0;d<obj.dimension;d++) writer.print(index_actual_sel[d]+",");
			}
			SO = (algo_cost/obj.cost_generic(index_actual_sel));
			subOpt[j] = SO;
			if(FPC_for_Alignment)
				Harm = obj.maxHarmCalculation(j, SO);
			if(Harm > MaxHarm)
				MaxHarm = Harm;
			ASO += SO;
			ASO_points++;
			anshASO += SO*locationWeight[j];
			if(SO>MSO)
				MSO = SO;
			if(print_flag)
			{
				writer.println("\nSpillBound The SubOptimaility  is "+SO);
				 writer.println("\nSpillBound Harm  is "+Harm);
			}
		} //end of for
		//Settings
		writer.close();
	}
	else{
	
			max_point = 1; 
			File fileDel = new File(apktPath+"subOpt.txt");
			    if (fileDel.exists()) {
					fileDel.delete();
				}

			if(MSOCalculation)
				//if(false)
					max_point = obj.totalPoints;
			
			int step_size = max_point/num_of_usable_threads;
			
			List<OptSBinputParamStruct> inputs = new ArrayList<OptSBinputParamStruct>();
			int cur_min_val = 0;
			int cur_max_val =  -1;
			for (int j = 0; j < num_of_usable_threads ; ++j) {

				cur_min_val = cur_max_val+1;
				cur_max_val = cur_min_val + step_size -1;
				
				if(j==num_of_usable_threads-1)
					cur_max_val = max_point -1;

				OptSBinputParamStruct input = new OptSBinputParamStruct(new OptSB(obj), cur_min_val, cur_max_val);
				inputs.add(input);
			}
			

			System.out.println("Available Cores " + threads);
		    ExecutorService service = Executors.newFixedThreadPool(num_of_usable_threads);
		    List<Future<OptSBOutputParamStruct>> futures = new ArrayList<Future<OptSBOutputParamStruct>>();

		     for (final OptSBinputParamStruct input : inputs) {
		        Callable<OptSBOutputParamStruct> callable = new Callable<OptSBOutputParamStruct>() {
		            public OptSBOutputParamStruct call() throws Exception {
		            	OptSBOutputParamStruct output = new OptSBOutputParamStruct();
		            	int min_index = input.min_index;
		            	int max_index = input.max_index;
		            	PrintWriter writer = new PrintWriter(apktPath+"logs/SB_serial_log("+min_index+"-"+max_index+").txt", "UTF-8");

		            	OptSB obj = input.obj;

		            	Iterator itr = global_obj.ContourPointsMap.keySet().iterator();

		            	Integer key;
		            	while(itr.hasNext())
		            	{
		            		key = (Integer)itr.next();
		            		ArrayList<point_generic> contourPoints = new ArrayList<point_generic>();
		            		for(int c=0;c<global_obj.ContourPointsMap.get(key).size();c++){
		            			point_generic pg = new point_generic(global_obj.ContourPointsMap.get(key).get(c));
		            			contourPoints.add(pg);
		            			//pg.printPoint();
		            		}
		            		obj.ContourPointsMap.put(key, contourPoints);
		            	}

		            	for(int j= min_index;j<=max_index;j++){
		            		writer.println("Begin execution loop "+ j);
		            //		if(j%1000==0){
		            	//		System.gc();
		            		//}

		            		//		            	if( j!=7217){
		            		//		    				output.flag = false;
		            		//		        			return output;
		            		//		    			}


		            		double algo_cost =0;
		            		double	SO =0;
		            		double cost = obj.getOptimalCost(0);
		            		obj.initialize(writer,j);
		            		int[] index = obj.getCoordinates(obj.dimension, obj.resolution, j);
		            		//		if(index[0]%5 !=0 || index[1]%5!=0)
		            		//			continue;
		            		//Settings:
		            		//		obj.actual_sel[0] = 0.005;obj.actual_sel[1] = 0.02;obj.actual_sel[2] = 0.005; /*uncomment for single execution*/

		            		for(int d=0;d<obj.dimension;d++) obj.actual_sel[d] = obj.findNearestSelectivity(obj.actual_sel[d]);

		            		//If cost of the optimal plan at actual_sel is less than 10000, then skip that point.
		            		if(obj.cost_generic(obj.convertSelectivitytoIndex(obj.actual_sel))<10000){
		            			continue;
		            		}
		            		//----------------------------------------------------------
		            		int i =1;
		            		obj.executions.clear();


		            		int k;
		            		output.num_of_points ++; //keeping track of number of qa's > 10000 cost for this thread

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


		            			if(print_flag)
		            			{
		            				writer.println("---------------------------------------------------------------------------------------------\n");
		            				writer.println("Contour "+i+" cost : "+cost+"\n");
		            			}
		            			int prev = obj.remainingDim.size();

		            			if(prev==1){
		            				obj.oneDimensionSearch(writer,i,cost);
		            				obj.learning_cost = obj.oneDimCost;
		            			}
		            			else
		            				obj.spillBoundAlgo(writer, i, j);

		            			int present = obj.remainingDim.size();
		            			if(present < prev - 1 || present > prev)
		            				System.out.println("ERROR");

		            			algo_cost = algo_cost+ (obj.learning_cost);
		            			if(present == prev ){    // just to see whether any dimension was learnt
		            				// if no dimension is learnt then move on to the next contour

		            				/*
		            				 * capturing the properties of the query execution
		            				 */
		            				if(print_flag)
		            				{
		            					writer.println("In Contour "+i+" has the following details");
		            					writer.println("No of executions is "+obj.no_executions);
		            					writer.println("No of repeat moves is "+obj.no_repeat_executions);
		            				}
		            				if(obj.no_executions>obj.max_no_executions)
		            					obj.max_no_executions = obj.no_executions;
		            				if(obj.no_repeat_executions > obj.max_no_repeat_executions)
		            					obj.max_no_repeat_executions = obj.no_repeat_executions;


		            				cost = cost*2;  
		            				i = i+1;
		            				obj.executions.clear();
		            				/*
		            				 * initialize the repeat moves and exections for the next contour
		            				 */
		            				for(int d=0;d<obj.dimension;d++){
		            					obj.already_visited[d] = false;
		            					obj.no_executions = 0;
		            					obj.no_repeat_executions =0;
		            				}

		            			}
		            			if(print_flag)
		            			{
		            				writer.println("---------------------------------------------------------------------------------------------\n");
		            			}

		            		}  //end of while

		            		/*
		            		 * printing the actual selectivity
		            		 */
		            		//		System.out.print("\nThe actual selectivity is original \t");
		            		if(print_flag)
		            		{
		            			for(int d=0;d<obj.dimension;d++) 
		            				writer.print(obj.actual_sel[d]+",");
		            		}
		            		/*
		            		 * storing the index of the actual selectivities. Using this printing the
		            		 * index (an approximation) of actual selectivities and its cost
		            		 */
		            		int [] index_actual_sel = new int[obj.dimension]; 
		            		for(int d=0;d<obj.dimension;d++) index_actual_sel[d] = obj.findNearestPoint(obj.actual_sel[d]);

		            		if(print_flag)
		            		{
		            			writer.print("\nCost of actual_sel ="+obj.cost_generic(index_actual_sel)+" at ");
		            			for(int d=0;d<obj.dimension;d++) writer.print(index_actual_sel[d]+",");
		            		}
		            		SO = (algo_cost/obj.cost_generic(index_actual_sel));
		            		if(SO > output.MSO)
		            			output.MSO = SO;
		            		output.sumSo += SO;
		            		output.anshASO += SO*locationWeight[j];

		            		if(obj.pmax > output.pmax)
		            			output.pmax = obj.pmax;

		            		if(obj.tmax > output.tmax)
		            			output.tmax = obj.tmax;
		            		//obj.ContourPointsMap.clear();
		            		if(print_flag)
		            		{
		            			writer.println("\nSpillBound The SubOptimaility  is "+SO);
		            			//				System.out.println("\nSpillBound Harm  is "+Harm);
		            		}

		            	}		

		            	//		    			File file = new File(apktPath+"subOpt.txt");
		            	//					    if (!file.exists()) {
		            	//							file.createNewFile();
		            	//						}
		            	//					    FileWriter writerSubOpt = new FileWriter(file, true);
		            	//					    PrintWriter pw = new PrintWriter(writerSubOpt);
		            	//					    pw.format("%d = %f\n",output.MSO);
		            	//					    pw.close();writerSubOpt.close();
		            	//					    writer.close();
		            	if(output.num_of_points > 0)
		            		output.flag = true;
		            	return output;   	
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
			pmax_final = 0;
			tmax_final = 0;
			int j=0;
		    for (Future<OptSBOutputParamStruct> future : futures) {
		    	OptSBOutputParamStruct output = future.get();
		    	if(output.flag)
		    	{
		    		if(output.pmax > pmax_final){
		    			pmax_final = output.pmax;
		    		}
		    		if(output.tmax > tmax_final){
		    			tmax_final = output.tmax;
		    		}
		    		//subOpt[j] = output.SO;
		    		if(output.MSO>MSO)
		    			MSO = output.MSO;
		    		if(output.Harm > MaxHarm)
		    			MaxHarm = output.Harm;
		    		ASO += output.sumSo;
		    		ASO_points += output.num_of_points;
		    		anshASO += output.anshASO;
		    		j++;
		    	}
		    }

		}

	//obj.writeSuboptToFile(subOpt, apktPath);
		//conn.close();
		System.out.println("max no of partitions = "+ pmax_final);
		System.out.println("maximum Tolerance(actual_fraction) = "+tmax_final+" (should be ideally < 1)");
//		File SubOptfile = new File(apktPath+"subOpt.txt");
//	    if (!SubOptfile.exists()) {
//	    	SubOptfile.createNewFile();
//		}
//	    FileWriter SubOptfileWriter = new FileWriter(SubOptfile, true);
//	    PrintWriter pw = new PrintWriter(SubOptfileWriter);
//	    pw.format("SO=%f\n",MSO);
//	    pw.close();SubOptfileWriter.close();
		System.out.println("SpillBound The MaxSubOptimaility  is "+MSO);
		System.out.println("SpillBound The MaxHarm  is "+MaxHarm);
		System.out.println("SpillBound Anshuman average Suboptimality is "+(double)anshASO);
		System.out.println("SpillBound The AverageSubOptimaility  is "+(double)(ASO*1.0)/(ASO_points*1.0));

		long endTime = System.nanoTime();
		System.out.println("Took "+(endTime - startTime)/1000000000 + " sec");

	}

	public void readContourPointsFromFile() throws ClassNotFoundException {

		try {
			
			ObjectInputStream ip = new ObjectInputStream(new FileInputStream(new File(apktPath +"contours/Contours.map")));
			ContourLocationsMap obj = (ContourLocationsMap)ip.readObject();
			ContourPointsMap = obj.getContourMap();
			Iterator itr = ContourPointsMap.keySet().iterator();
			while(itr.hasNext()){
				Integer key = (Integer) itr.next();
				
				//System.out.println("The no. of Anshuman locations on contour "+(st+1)+" is "+contourLocs[st].size());
				System.out.println("The no. of locations on contour "+(key.intValue())+" is "+ContourPointsMap.get(key).size());
				System.out.println("--------------------------------------------------------------------------------------");
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.exit(0);
	}
	
	public void writeContourMaptoFile(){

		try {
			String path;
			if(generateSpecificContour)
				path = new String (apktPath+"contours/Contour"+genContourNumber+".map");
			else
				path = new String (apktPath+"contours/Contours.map");
			FileOutputStream fos = new FileOutputStream (path);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(new ContourLocationsMap(ContourPointsMap));
			oos.flush();
			oos.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(generateSpecificContour)
			System.exit(0);
	}
	
	
	void doPartition(ArrayList<point_generic> contour_points, int [] min_cost_index, Set<Integer> unique_plans)
	{
		//--For counting

		String funName = "doPartition";
		int[]spillDim_planCnt = new int[dimension];
		double []spillDim_pointPercent = new double[dimension];
		double cur_dim_weight;
		point_generic p;
		int plan;
		int category;
		double avg=0;
		for(int l=0;l<dimension;l++)
		{
			spillDim_planCnt[l] = 0;

			spillDim_pointPercent[l] = 0.0;
		}

		ArrayList<point_generic> cur_contour_points = new ArrayList<point_generic>();
		//--
		/*
		 * Iterate over the List
		 * check the spill node
		 * put it to specific partition_info[k]
		 */
		int learning_dim,plan_no;
		int partition_no;
		for(int i=0;i<contour_points.size();i++)
		{

			p = contour_points.get(i);
			plan_no = p.get_plan_no();

			p.set_badguy();

			//max_cost and min_cost are used to get the maximum and minimum cost of points in the contour for sanity check purposes
			unique_plans.add(plan_no);

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

//				if(local_partition_flag==true)
//				{
//					cur_contour_points.add(p);
//				}
				currentContourPoints ++;
				//-- For counting
				category=plans_list[plan_no].getcategory(remainingDim);
				if(p.getLearningDimension()!=category && DEBUG==true){
					System.out.println("debug");
					category=plans_list[plan_no].getcategory(remainingDim);
					System.out.println("remaining Dim"+remainingDim);
					System.out.println("Predicate Order"+p.getPredicateOrder());
				}
				assert(p.getLearningDimension()==category): "getLearning Dimension of spillBound is not equal to category of opt-SB";
				
				spillDim_planCnt[category]++;
				// --
				learning_dim = category;
				partition_no=getPartitionNumber(learning_dim);
				if(partition_no == -1)
				{
					System.out.println("Wrong Partition Number");
					System.exit(1);
				}
				this.partition_info[partition_no].addPoint(p);
			}
		}
		
		
//		if(local_partition_flag == true)
//		{
//
//			avg = 100.0/(double)dimension;
//			int cur_part_no =0;
//			cur_partition.clear();
//			n_partition = 0;
//			Integer l;
//			Iterator itr2 = remainingDim.iterator();
//			//	for(int k=0;k<remainingDim.size();k++)
//			while(itr2.hasNext())
//			{
//				l = (Integer)itr2.next();
//				spillDim_pointPercent[l] = (double)spillDim_planCnt[l]*100/cur_contour_points.size();
//				cur_dim_weight = spillDim_pointPercent[l];
//				if(cur_dim_weight> 0.7*avg)
//				{
//
//					if(cur_partition.containsKey(cur_part_no))
//					{
//						cur_partition.get(cur_part_no).add(l);
//					}
//					else
//					{
//						n_partition = n_partition+1;
//						temp_list = new ArrayList<Integer>();
//						temp_list.add(l);
//						cur_partition.put(cur_part_no, temp_list);
//					}
//					cur_part_no = cur_part_no + 1;
//				}
//				else
//				{
//					if(n_partition == 0)
//					{
//						n_partition = n_partition+1;
//						temp_list = new ArrayList<Integer>();
//						temp_list.add(l);
//						cur_partition.put(cur_part_no, temp_list);
//					}
//					else
//					{
//						if(cur_part_no == 0)
//							cur_partition.get((cur_part_no)).add(l);
//						else
//							cur_partition.get((cur_part_no-1)).add(l);
//					}
//				}
//
//			}
//			partition_info = new partition[n_partition];
//
//			Iterator itr = cur_partition.keySet().iterator();
//			Integer i_key;
//			for(int i=0;i<n_partition;i++)
//			{
//				assert(itr.hasNext()==true):"cur_partition doesn't have enough keySet!!";
//				//		partition_info[i] = new partition(i, p[i]);
//				i_key = (Integer)itr.next();
//				partition_info[i_key] = new partition(i_key, convertIntegers(cur_partition.get(i_key)));
//			}
//		}
		//Partition here!
		int l;
        //      System.out.println("\n SpillDim_pointCount-----");
//      for(int l=0;l<dimension;l++)
//      {
//              System.out.println("Points spilling on dim "+l+" = "+spillDim_pointPercent[l]+"%");
//      }

    for(int y=0;y<dimension;y++)
    {
            spillDim_pointPercent[y] = (double)spillDim_planCnt[y]*100/currentContourPoints;
    }


		
//		System.out.println("\n SpillDim_pointCount-----");
//		for(l=0;l<dimension;l++)
//		{
//			System.out.println("Points spilling on dim "+l+" = "+spillDim_pointPercent[l]+"%");
//		}
//		if(local_partition_flag == true)
//		{
//			System.out.println("Avg ="+avg+", Number of partitions= "+n_partition);
//			if(n_partition > pmax)
//				pmax = n_partition;
//		}
		Integer l_key;
		Iterator itr = cur_partition.keySet().iterator();
//		for(l=0;l<n_partition;l++)
//		{
//			l_key = (Integer)itr.next();
//			System.out.println("Parttion "+l_key+" : "+cur_partition.get(l_key).toString());
//
//		}


		//Put points in the partition info!
//		if(local_partition_flag)
//		{
//			for(l=0;l<cur_contour_points.size();l++)
//			{
//				p = cur_contour_points.get(l);
//				plan_no=p.get_plan_no();
//				learning_dim=plans_list[plan_no].getcategory(remainingDim);
//				partition_no=getPartitionNumber(learning_dim);
//				if(partition_no == -1)
//				{
//					System.out.println("Wrong Partition Number");
//					System.exit(1);
//				}
//				partition_info[partition_no].addPoint(p);
//			}
//		}

		//---> If there are no contour Points!



		//-- Empty contour case ends



	}

	int getPartitionNumber(int dim)
	{
		String funName = "getPartitionNumber"; 
		int i,j,k;
		ArrayList<Integer> i_list;
		Integer i_key;
		Iterator itr = cur_partition.keySet().iterator();
		for(i=0;i<n_partition;i++)
		{
			assert(itr.hasNext()==true):"cur_partition doesn't have enough keySet!!";
			//	i_list = cur_partition.get(itr.next());
			i_key = (Integer)itr.next();
			//			if(i_list.size()==2)
			//			{
			//				System.out.println("HERE");
			//			}
			for(j=0;j<cur_partition.get(i_key).size();j++)
			{
				k = cur_partition.get(i_key).get(j);
				if(k == dim)
				{
					assert(i_key<=remainingDim.size()-1):funName+" parition number is more than number of remaining dimensions";
					assert(i_key<=n_partition-1):funName+" parition number is more than number of remaining dimensions";
					return i_key;
					
				}
			}
		}
		return -1;
	}

	private int factorial(int num) {

		int factorial = 1;
		for(int i=num;i>=1;i--){
			factorial *= i; 
		}
		return factorial;
	}

	public void writeSuboptToFile(double[] subOpt,String path) throws IOException {

		File file = new File(path+"spillBound_"+"suboptimality"+".txt");
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
		//		for(int i =0;i<resolution;i++){
		//			for(int j=0;j<resolution;j++){
		//				//Settings
		//				for(int k=0;k<resolution;k++){
		//				//if(i%5==0 && j%5==0){
		//					int [] index = new int[3];
		//					index[0] = i;
		//					index[1] = j;
		//					index[2] = k;
		//					int ind = getIndex(index, resolution);
		//					if(j!=0)
		//						pw.print("\t"+subOpt[ind]);
		//					else
		//						pw.print(subOpt[ind]);
		//				//}
		//				}	
		//			}
		//			//if(i%2==0)
		//				//pw.print("\n");
		//		}
		pw.close();
		writer.close();

	}

	  private void checkAlignmentPenalty() throws NumberFormatException, IOException {
	       
	        HashMap<Integer,ArrayList<Integer>> spillDimensionPlanMap = new HashMap<Integer,ArrayList<Integer>>();
	        double[] penalty_threshold = {1, 1.2, 1.5, 2};
	        boolean aligned = false;
	        int [] contour_penalty_count = new int[penalty_threshold.length];
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

	            aligned = false;
	            //calculate the penalty for aligning each dimension
	            double penalty[] = new double[dimension];
	            for(int d=0; d<dimension; d++){
	            	
	            	 double original_cost = OptimalCost[getIndex(points_max.get(d).dim_values, resolution)];
	                 double min_fpc_cost = Double.MAX_VALUE;
	                 int min_fpc_plan = -1;
	                 for(int planno=0; spillDimensionPlanMap.containsKey(d) && planno<spillDimensionPlanMap.get(d).size(); planno++ ){
	                	 if(FPC_for_Alignment && !Nexus_algo){
	                     if(AllPlanCosts[spillDimensionPlanMap.get(d).get(planno)][getIndex(points_max.get(d).dim_values, resolution)] < min_fpc_cost){
	                         min_fpc_cost = AllPlanCosts[spillDimensionPlanMap.get(d).get(planno)][getIndex(points_max.get(d).dim_values, resolution)];
	                         min_fpc_plan = spillDimensionPlanMap.get(d).get(planno);
	                     	}
	                     }
	                 }
	                 System.out.print("\nThe dimension is "+d+" at (max) point ");
	                 for(int dim=0; dim<dimension; dim++)
	                     System.out.print("\t"+points_max.get(d).dim_values[dim]);
	                 System.out.println(" with optimal cost "+original_cost+" while forcing plan "+min_fpc_plan+" having forced cost "+min_fpc_cost+" with penalty "+min_fpc_cost/original_cost);
	 
	                

	                double error = ((min_fpc_cost-original_cost)/original_cost)*100;
	                points_max.get(d).putpercent_err(error);
	                if(spill_opt_for_Alignment){
	                getBestSpillingPlan(points_max.get(d), d);
	                if(points_max.get(d).getfpc_cost() < min_fpc_cost ){
	                	System.out.println(" Spilling Optimal plan improved cost from "+min_fpc_cost+" to "+points_max.get(d).getfpc_cost());
	                	min_fpc_cost = points_max.get(d).getfpc_cost();
	                	//System.out.println(" After Spilling: with optimal cost "+original_cost+" while forcing plan "+min_fpc_plan+" having forced cost "+min_fpc_cost+" with penalty "+min_fpc_cost/original_cost);
	               
	            	}   
	                }
	                //see if the contour is aligned along dimension d
	                if(points_max.get(d).storedOrder.get(0) == d)
	                    aligned = true;
	                
	                if(aligned)
	                	penalty[d] = 1.0;
	                else
	                penalty[d] = min_fpc_cost/original_cost;
	               
	                //see if the contour is aligned along dimension d
	               
	            }
	           
	           
	            //update the max penalty of min penalty among these dimension
	            double min_penalty = Double.MAX_VALUE;
	            for(int d=0; d<dimension; d++)
	                if(penalty[d] < min_penalty)
	                    min_penalty = penalty[d];
	           
	            for(int i=0;i<penalty_threshold.length;i++){
	                if(aligned){
	                    contour_penalty_count[i]++;
	                }
	                else if(min_penalty <= penalty_threshold[i])
	                    contour_penalty_count[i]++;
	            }
	           
	            if(min_penalty > max_penality)
	                max_penality = min_penalty;
	           
	            System.out.println("-----------------------------------------------------------------------------------------------------");
	        }
	       
	        System.out.println("The max penalty is "+max_penality);
	        for(int i=0;i<penalty_threshold.length;i++){
	            System.out.println("% of contour within the threshold of "+(double)(penalty_threshold[i]-1)*100.0+" is "+(double)(contour_penalty_count[i]*1.0/(ContourPointsMap.size()*1.0)));
	        }
	        System.exit(1);
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

	public void oneDimensionSearch(PrintWriter writer, int contour_no, double cost) {

		String funName = "oneDimensionSearch";
		/*
		 * just sanity check for findNearestPoint and findNearestSelectivity
		 */
		int ld;
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
				//Integer learning_dim = new Integer(p.getLearningDimension());
				ld = plans_list[p.get_plan_no()].getcategory(remainingDim);
				Integer learning_dim = new Integer(ld);
				if(learning_dim.intValue() != remDim)
				{
					writer.println("ld= "+ld+", remDim="+remDim+", opt_plan_no= "+p.get_plan_no());
				}
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
						double fpc_cost_generic = Double.MAX_VALUE;
						if(FPC_for_Alignment){
							fpc_cost_generic = fpc_cost_generic(int_actual_sel, fpc_plan); 
						}
						else{
							try {

								ObjectInputStream ip = new ObjectInputStream(new FileInputStream(new File(apktPath + fpc_plan + ".pcst")));
								double[] fpc_cost_array = (double[]) ip.readObject();
								fpc_cost_generic = fpc_cost_array[getIndex(int_actual_sel, resolution)];
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						if(fpc_cost_generic<oneDimCost)
							oneDimCost = fpc_cost_generic;
						if(cost_generic(int_actual_sel)> oneDimCost)
							oneDimCost = cost_generic(int_actual_sel);
					}	
				}
			}
		}

		if(sel_min < (double) 1 && sel_min >= actual_sel[remDim]){

			if(print_flag)
			{
				writer.println(funName+" learnt "+ remDim+" dimension completely");
			}
			remainingDim.remove(remainingDim.indexOf(remDim));
			remove_from_partition(remDim);
			if(print_flag)
			{
				writer.println(funName+" Sel_min = "+sel_min+" and cost is "+oneDimCost);
			}
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
			double fpc_cost_generic = Double.MAX_VALUE;
			if(FPC_for_Alignment){
				fpc_cost_generic = fpc_cost_generic(int_actual_sel, fpc_plan); 
			}
			else{
				try {

					ObjectInputStream ip = new ObjectInputStream(new FileInputStream(new File(apktPath + fpc_plan + ".pcst")));
					double[] fpc_cost_array = (double[]) ip.readObject();
					fpc_cost_generic = fpc_cost_array[getIndex(int_actual_sel, resolution)];
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if(fpc_cost_generic<oneDimCost)
				oneDimCost = fpc_cost_generic;
			if(cost_generic(int_actual_sel)> oneDimCost)
				oneDimCost = cost_generic(int_actual_sel);
			
			remainingDim.remove(remainingDim.indexOf(remDim));
			remove_from_partition(remDim);
			if(print_flag)
			{
				writer.println(funName+" Sel_min = "+sel_min+" and cost is "+oneDimCost);
			}
			if(oneDimCost>2*cost)
				oneDimCost = 2*cost-1;
			//assert (oneDimCost<=2*cost) :funName+": oneDimCost is not less than 2*cost when setting to resolution-1";
		}


	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void spillBoundAlgo(PrintWriter writer, int contour_no, int loop) throws IOException {


		String funName = "spillBoundAlgo";
		int learning_dim;
		double tolerance = -1;
		int fpc_plan;
		double cur_val=0;
		 
		Set<Integer> unique_plans = new HashSet();
		if(print_flag)
		{
			writer.println("\nContour number ="+contour_no+ " with its size being "+ContourPointsMap.get(contour_no).size());
		}
		int j;

		//declaration and initialization of variables
		learning_cost = 0;
		oneDimCost = 0;
		double cur_tol = 0;
		max_cost=0;
		min_cost = OptimalCost[totalPoints-1]+1;
		int ld;
		int [] min_cost_index = new int[dimension];

		double [] sel_max = new double[dimension];
		//		double min_fpc_cost = Double.MAX_VALUE;
		//		int min_fpc_plan = Integer.MAX_VALUE;
		//		int min_fpc_dim =Integer.MAX_VALUE;


		ArrayList<point_generic>  max_pts = new ArrayList<point_generic>(); 
		point_generic temp_pt1 = null;
		point_generic max_pt = null;
		/*
		 * to store the max selectivity each dimension  can learn in a contour
		 */
		for(int d=0;d<dimension;d++) sel_max[d] = -1;

		/*
		 * store the plan/point corresponding to the sel_max locations in a hashmap
		 * for which the key is the dimension
		 */
		HashMap<Integer,point_generic> local_points_max = new HashMap<Integer,point_generic>();
		HashMap<Integer,point_generic> points_max = new HashMap<Integer,point_generic>();
		//	HashMap<Integer, point_generic> fpc_points_max = new HashMap<Integer, point_generic>();

		//end of declaration of variables
		
		if(local_partition_flag==false)
		{
			for(int k=0;k<partition_info.length;k++)
			{
				partition_info[k].clearPartition();
			}
		}
		//----------------------------------Initialize variables-----------------------------------------------------
		double local_tmax = 0;
		double min_val = Double.MAX_VALUE;
		double temp_tol = Double.MAX_VALUE;
		
		boolean first_flag = true;
		int loc_dimension = remainingDim.size();
		
//		for(int i =0; i< loc_dimension; i++)
//		{
//			p_encoding[i] = 1;
//			until_max[i] = 1;
//		}
			//	System.arraycopy(p_encoding, 0, prev_encoding, 0, loc_dimension);		
		int partition_count =0;
		int cnt =0;
		List<List<List<Integer>>> part = new ArrayList<>();
		for(int i=1; i<=remainingDim.size(); i++) {
            List<List<List<Integer>>> ret = helper(remainingDim, i);
            //Iterate over ret and add to part
            for (int w =0;w<ret.size();w++){
            	part.add(ret.get(w));
            }
            cnt += ret.size();
          //  writer.println(cnt);
            
        }
		int part_idx = 0;
		while(part_idx < part.size())
		{
			
		/*	if(first_flag == false )
			{
				//		break; 
				if(next(p_encoding, until_max ,loc_dimension )==false)
				{
					if(dimension==5){
					p_encoding[0] = 4;p_encoding[1] = 2;p_encoding[2] = 3;p_encoding[3] = 2;p_encoding[4] = 1;
					System.arraycopy( prev_encoding, 0, p_encoding, 0,loc_dimension);
					}					
					partition_count++;
					break;
				}
			}
			System.arraycopy(p_encoding, 0, prev_encoding, 0, loc_dimension);
			*/
			cur_tol = 0;
			first_flag = false;
			currentContourPoints = 0;
		make_local_partition(part.get(part_idx));
		part_idx = part_idx+1;
		assert(n_partition<=remainingDim.size()): funName+" no. of partitions is more than the number of remaining dimensions";
		doPartition(ContourPointsMap.get(contour_no), min_cost_index, unique_plans);
		point_generic [] min_fpc_point = new point_generic[n_partition];

	//	points_max.clear();
		local_points_max.clear();
		//old code
		Integer m_key;
		Iterator itr = cur_partition.keySet().iterator();
		local_tmax = 0;
		cur_val = 0;
		for(int m =0;m<n_partition;m++)
		{
			assert(itr.hasNext()==true):"cur_partition doesn't have enough keySet!!";
			//-- old
			m_key = (Integer)itr.next();
			if(this.partition_info[m_key].getList().isEmpty())
			{
				//TODO: this code can be reached!
			//	if(print_flag)
           //         System.out.println("Partition "+m_key+" is empty");
				continue;
			}

			for(int q=0;q<cur_partition.get(m_key).size();q++)
			{
				//	j = p[m][q];
				j = cur_partition.get(m_key).get(q); 

				max_pts = this.partition_info[m_key].getMaxPoints(j);
				if(max_pts == null)
				{
					System.out.println("\nNo Max point for returned NULL\n");
					System.exit(1);
				}
				
				double min_fpc_cost = Double.MAX_VALUE;
				for(int t=0;t<max_pts.size();t++){
					temp_pt1 = max_pts.get(t); 
					point_generic extreme_pt = new point_generic(temp_pt1);
//					if(t==1)
//						System.out.print("interseting");
					if(FPC_for_Alignment && (t!=0 && (t != max_pts.size()-1) && t != (max_pts.size()-1)/2))
						continue;
					if(plans_list[extreme_pt.get_plan_no()].getcategory(remainingDim)==j)
					{
						extreme_pt.set_goodguy();
						local_points_max.put(j, extreme_pt);
						max_pt = extreme_pt;
						cur_val = cur_val + 1;  // TODO: curval += max_pt.getOptCost();
						break;
					}
					else
					{
						extreme_pt.set_badguy();
						double temp_fpc_cost = Double.MAX_VALUE;
						if(FPC_for_Alignment)
							temp_fpc_cost = getBestFPC(extreme_pt,j);
						else if(spill_opt_for_Alignment)
							temp_fpc_cost = getBestSpillingPlan(extreme_pt, j);
						if(temp_fpc_cost < min_fpc_cost){
							min_fpc_cost = temp_fpc_cost;
							max_pt = extreme_pt;
						}
						if(max_pt.getfpc_cost() < Double.MAX_VALUE/10)
						assert( max_pt.get_fpcSpillDim() == j): "for loop "+loop+" max_pt fpc_dim is not matching"; 
					}
					//		maxPoints[j]=findMaxPoint(partition_info[m].getList(),j);
				}
				
				if(max_pt.get_goodguy())
					break;
				if(spill_opt_for_Alignment){
					point_generic temp_pt = new point_generic(max_pt);
					double best_fpc_pt_err = getBestSpillingPlan(temp_pt, j);
					if(best_fpc_pt_err > max_pt.getpercent_err())
						max_pt = temp_pt; 
				}
				
				this.maxPoints[j] = max_pt;

				//				if(maxPoints[j].get_goodguy()==true)
				//				{
				//					points_max.put(j,maxPoints[j]);
				//					//put into points_max
				//					break;
				//				}
				if(q==0)
				{
					min_fpc_point[m] = this.maxPoints[j];
				}

				
				if(this.maxPoints[j].getpercent_err() < min_fpc_point[m].getpercent_err())
				{
					min_fpc_point[m] = this.maxPoints[j];
				}
				
				if(q==(cur_partition.get(m_key).size()-1))
				{
					ld = min_fpc_point[m].get_fpcSpillDim();   //are you getting the real minimum cost partition?
					assert(ld!=-1):"Spill Dimension is -1";
					min_fpc_point[m].set_badguy();
					local_points_max.put(ld, min_fpc_point[m]);  
					cur_tol = (min_fpc_point[m].getpercent_err()/100.0);
					cur_val = cur_val + (1+cur_tol); //cost*(1+cur_tol)
					if( cur_tol > local_tmax)
						local_tmax = cur_tol;
			//		 if(print_flag)
                  //       System.out.println("BG: t="+cur_tol+" max_point for partition "+m_key+ " for dimension "+ld+" is "+min_fpc_point[m].get_point_Index()[ld]);

				}

			}

		}
		
		double dnp = (double) n_partition;
//		if(cur_tol > 25)
	//		System.out.println("d");
		//double cur_val = ((2*dnp*loc_dimension) - (dnp*dnp) + (3*dnp) )*(1+local_tmax);
	//	 cur_val = ((loc_dimension-dnp)*dnp + (dnp*(dnp+1)*0.5))*(1+local_tmax);
	//	double cur_val = (n_partition)*(1+local_tmax);
		if(cur_val < min_val)
		{
			min_val = cur_val;
			points_max = new HashMap<Integer, point_generic>(local_points_max);
			temp_tol = local_tmax;
			n_partition_best = n_partition;
			System.arraycopy(p_encoding, 0, best_partitioning, 0, loc_dimension);
		}
	}
		assert(temp_tol != Double.MAX_VALUE && n_partition_best!=0):"temp_tol is untouched";
			n_partition = n_partition_best;
		if(temp_tol > this.tmax)
		{
			this.tmax = temp_tol;
		}
		print_points_max(points_max,writer);
		print_partition(best_partitioning,writer);
		if(n_partition > this.pmax)
		{
			this.pmax = n_partition;
		}
		/*
		 * 1. Identify the correct max_point.
		 * 2. Put them serially in points_max.
		 */

		/*
		 * Obj : Identify correct max_point for each partition.
		 * for each partition
		 * 		for each dimension
		 * 			findMaxpoint and save the min_error
		 * 		select the point with minimum error
		 * 		put the point in points_max
		 */


		//		
		//		for(int c=0; c <ContourPointsMap.get(contour_no).size();c++){
		//
		//			point_generic p = ContourPointsMap.get(contour_no).get(c);
		//
		//			/*
		//			 * update the max and the min cost seen for this contour
		//			 * also update the unique_plans for the contour
		//			 */
		//			unique_plans.add(p.get_plan_no());
		//			if(p.get_cost()>max_cost)
		//				max_cost = p.get_cost();
		//			if(p.get_cost()<min_cost){
		//				min_cost = p.get_cost();
		//				for(int d=0;d<dimension;d++)
		//					min_cost_index[d] = p.get_dimension(d);
		//			}
		//
		//			assert(min_cost<=max_cost) : funName+"min cost is less than or equal to max. cost in the contour";
		//
		//
		//			//Settings: for higher just see if you want to comment this
		//			if(inFeasibleRegion(convertIndextoSelectivity(p.get_point_Index()))){
		//				currentContourPoints ++;
		//				Integer learning_dim = new Integer(p.getLearningDimension());
		//				assert (remainingDim.contains(learning_dim)) : "error: learning dimension already learnt";
		//				if(selectivity[p.get_dimension(learning_dim.intValue())] > sel_max[learning_dim.intValue()]){
		//					if(points_max.containsKey(learning_dim))
		//						points_max.remove(learning_dim);
		//					points_max.put(learning_dim, p);
		//					sel_max[learning_dim.intValue()] = selectivity[p.get_dimension(learning_dim.intValue())]; 	
		//				}
		//			} //end for inFeasibleRegion
		//		}
		//
		/*
		 * it might happen that due to grid issues that even though q_a lies below a
		 * contour, the contour points could be empty. Hence we put a check condition
		 * that if the cost of the contour is greater than cost(q_a) then add
		 * atleast one point which is the max for all the remaining dimensions
		 */

		//		System.out.println("Current Contour Points is "+currentContourPoints );
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

				point_generic p = new point_generic(arr, getPlanNumber_generic(arr), cost_generic(arr), remainingDim);
				//ContourPointsMap.get(contour_no).add(p);
				max_cost = min_cost = p.get_cost();
				unique_plans.add(p.get_plan_no());
				p.reloadOrderList(remainingDim);
				//learning_dim = p.getLearningDimension();
				learning_dim = plans_list[p.get_plan_no()].getcategory(remainingDim);
				sel_max[learning_dim] = selectivity[arr[learning_dim]];
				p.set_goodguy();
				points_max.put(new Integer(learning_dim),p );

			}
			else
			{
				if(print_flag)
				{
					writer.println(funName+" Skipping the contour");
				}
			}
		}	


		//-----------------------------------------------------------------------------
		//TODO put in an assert saying that the same plan cannot be part of sel_max 
		// of different dimensions: Ans: Done
		/*
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
		 */
		//---------------------------------------------


		//		System.out.print("Max cost = "+max_cost+" Min cost = "+min_cost+" ");
		//		System.out.println("with Number of Unique plans = "+unique_plans.size());


		//		System.out.print("MinCostIndex = ");
		//		for(int d=0;d<dimension;d++)
		//			System.out.print(min_cost_index[d]+",");
		//
		//		System.out.println();
		//
		//
		//
		//		for(int d=0;d<dimension;d++)
		//			System.out.println(d+"_max : "+sel_max[d]);
		//
		//		System.out.print("MaxIndex = ");
		//		for(int d=0;d<dimension;d++)
		//			System.out.print(maxIndex[d]+",");
		//
		//		System.out.print("\nMinIndex = ");	
		//		for(int d=0;d<dimension;d++)
		//			System.out.print(minIndex[d]+",");
		//		System.out.println();
		Integer key;
		Iterator itr1 = points_max.keySet().iterator();
		int d;
		for(int y=0;y<n_partition;y++){
			if(itr1.hasNext() == false)
			{
				break;
			}
			//assert(itr.hasNext()==true):"Points_max doesnt have enough points as the number of partitions";
			key = (Integer)itr1.next();
			d = key;
			point_generic p = points_max.get(key);
			if(plans_list[p.get_plan_no()].getcategory(remainingDim)==key)
				p.set_goodguy();
			else
				p.set_badguy();
			if(p.get_goodguy())
			{
				//If it is a good guy
				assert(plans_list[p.get_plan_no()].getcategory(remainingDim)==key):"Error in assignment of good guy!";

				//learning_dim = p.getLearningDimension();
				learning_dim = plans_list[p.get_plan_no()].getcategory(remainingDim);
				tolerance = 1;
			}
			else
			{
				assert(plans_list[p.get_plan_no()].getcategory(remainingDim)!=key):"Error in assignment of good guy!";
				fpc_plan = p.getfpc_plan();
				//assert(fpc_plan != -1): "bad guy doesn't have a fpc plan!!";
				//learning_dim = plans_list[fpc_plan].getcategory(remainingDim);// should contain the forced dimension
				//assert(learning_dim == p.get_fpcSpillDim()) : funName+" FPC learning dimension at max. point";
				learning_dim = p.get_fpcSpillDim();
				assert(learning_dim == key) : funName+" for loop no.= "+loop+" FPC learning dimension is not key for points_max data structure";
				
				tolerance = 1+(p.getpercent_err()/100.0);
				if(print_flag)
				{
					writer.println("\nTolerance = "+tolerance);
				}
			}
			
			assert(tolerance<=(tmax+1)): funName+" the tolerance is beyond the max. tolerance during execution";
			assert(points_max.containsKey(learning_dim )): funName+" not learning a leader dimension";
			
			
			if(remainingDim.contains(learning_dim))
			{				
				//		if(sel_max[d] != (double)-1){  //TODO is type casting Okay?: Ans: It is fine


				double sel = 0;
				//		point_generic p = points_max.get(new Integer(d));
				//sel = Simulated_Spilling(max_x_plan, cg_obj, 0, cur_val);

				/*
				 * checking if we had already executed the same plan on the same dimension before.
				 * 
				 */
				if(p.get_goodguy()){
					//if(executions.contains(new Pair(new Integer(learning_dim),new Integer(p.getopt_plan()))))
					if(executions.contains(new Pair(new Integer(learning_dim),new Integer(p.get_plan_no()))))
						
						continue;
				}
				else
				{
					if(executions.contains(new Pair(new Integer(learning_dim),new Integer(p.getfpc_plan()))) && p.getfpc_plan()!=-2 )
						continue;
				}


				sel = getLearntSelectivity(writer,learning_dim,(Math.pow(2, contour_no-1)*getOptimalCost(0)*tolerance), p, contour_no);
		//		sel = getLearntSelectivity(learning_dim,(Math.pow(2, contour_no-1)*getOptimalCost(0)), p, contour_no);
				//					if(sel_max[d]<=sel)
				d= learning_dim;
				if(currentContourPoints!=0)
					sel_max[d] = sel;
				
				else{
					if(print_flag)
					{
						writer.println("Sel_max["+d+"]= "+sel_max[d]+", sel= "+sel);
					}
					assert(sel_max[d]>=sel) : "When Contour Point is zero: getLearntSelectivity is higher than sel[resolution-1]";
				}

				//else
				//	System.out.println("GetLeantSelectivity: postgres selectivity is less");

				/*
				 * add the tuple (dimension,plan) to the executions data structure
				 */
				//WRONG: TODO: change this to p.getfpc_plan
				if(p.get_goodguy())
					executions.add(new Pair(new Integer(d),new Integer(p.get_plan_no())));
				else
					executions.add(new Pair(new Integer(d),new Integer(p.getfpc_plan())));


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
					if(p.get_goodguy()==true)
					{
						if(print_flag)
						{
							writer.print("\n Plan "+p.get_plan_no()+" executed at ");
							for(int m=0;m<dimension;m++) writer.print(p.get_dimension(m)+",");
							writer.println(" and learnt "+d+" dimension completely");
						}
					}
					else
					{
						if(print_flag)
						{
							//	System.out.println("--------------FPC CASE-------------");
							//	System.out.println("Point p");
							writer.print("\n FPC Plan "+p.getfpc_plan()+" with fpc_percent_err= "+p.getpercent_err()+" executed at ");
							for(int m=0;m<dimension;m++) writer.print(p.get_dimension(m)+",");
							writer.println(" and learnt "+d+" dimension completely");
						}
					}

					minIndex[d] = maxIndex[d] = findNearestSelectivity(actual_sel[d]);
					remainingDim.remove(remainingDim.indexOf(d));
					assert(!remainingDim.contains(d)) : funName+ " even after removing the dimension the dimension remains in remaining_dimension";
					remove_from_partition(d);
					test_if_dimension_in_partition(d);
					removeDimensionFromContourPoints(d);
					return;
				}

				if(p.get_goodguy()==true)
				{
					if(print_flag)
					{
						writer.print("\n Plan "+p.get_plan_no()+" executed at ");
						for(int m=0;m<dimension;m++) writer.print(p.get_dimension(m)+",");
						writer.println(" and learnt "+sel_max[d]+" selectivity for "+d+"dimension");
					}
				}
				else
				{
					if(print_flag)
					{
						writer.print("\n FPCPlan "+p.getfpc_plan()+" executed at ");
						for(int m=0;m<dimension;m++) writer.print(p.get_dimension(m)+",");
						writer.println(" and learnt "+sel_max[d]+" selectivity for "+d+"dimension");
					}
				}
				//assert()
			}
		}


		//	}

		/* should this come here or at the end of the earlier for loop?
		 *  Ans: As per the theory algorithm, this should come here only since there is no pruning while executing intra contour plans
		 */   	

		//to avoid the inter contour pruning		
		for(d=0;d<dimension;d++){
			if(remainingDim.contains(d) &&  sel_max[d]!=(double)-1 && findNearestSelectivity(sel_max[d])<findNearestSelectivity(actual_sel[d]))
				if(sel_max[d]>minIndex[d])
					minIndex[d] = sel_max[d];
		}

	}

	public double getBestSpillingPlan(point_generic p, int dim_index) {

		String funName = "getBestSpillingPlan";
		Connection conn = null;
		Statement stmt = null;
		double execCost = -1;
		//set the selectedEPP and nonSelectedEPP variables
		String selectedEPP=null, nonSelectedEPP=null;
		for(int i=0;i<varyingJoins.length();i+=2){
			if(i/2 == dim_index){
				selectedEPP = new String(varyingJoins.substring(i, i+2));
			}
			else if(remainingDim.contains(i/2)){
				if(nonSelectedEPP==null)
					nonSelectedEPP = new String(varyingJoins.substring(i, i+2));
				else{
					String str = new String(varyingJoins.substring(i, i+2));
					nonSelectedEPP = nonSelectedEPP.concat(str);
				}					
			}
		}

		try {
			conn = source.getConnection();
			stmt = conn.createStatement();
			//NOTE,Settings: need not set the page cost's
	         if(database_conn==0){
                  stmt.execute("set effective_cache_size='1GB'");
              }
              else{
                 stmt.execute("set effective_cache_size='4GB'");
              }       
			stmt.execute("set  seq_page_cost = 1");
			stmt.execute("set  random_page_cost=4");
			stmt.execute("set cpu_operator_cost=0.0025");
			stmt.execute("set cpu_index_tuple_cost=0.005");
			stmt.execute("set cpu_tuple_cost=0.01");	
			stmt.execute("set full_robustness = on");
			//stmt.execute("set oneFPCfull_robustness = on");
			stmt.execute("set spill_optimization = on");
			stmt.execute("set varyingJoins = "+varyingJoins);
			stmt.execute("set selectedEPP = "+selectedEPP);
			stmt.execute("set nonSelectedEPP = "+nonSelectedEPP);


			for(int d=0;d<dimension;d++){
				stmt.execute("set JS_multiplier"+(d+1)+ "= "+ JS_multiplier[d]);
				stmt.execute("set robust_eqjoin_selec"+(d+1)+ "= "+ selectivity[p.get_dimension(d)]); 
			}			
			ResultSet rs = stmt.executeQuery(query_opt_spill);
			if(rs.next()){
            	
				String regx = Pattern.quote("..") + "(.*?)" + Pattern.quote("rows=");
				String str1 =  rs.getString(1);
				Pattern pattern = Pattern.compile(regx);
				Matcher matcher = pattern.matcher(str1);
				while(matcher.find()){
					execCost = Double.parseDouble(matcher.group(1));	
				}
			}
			//System.out.println(rs);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
	    	if (conn != null) {
	        try { conn.close(); } catch (SQLException e) {}
	    		}
    		}
	
		//assert(p.getfpc_cost()>0) : funName+" fpc_cost is not set by lohit's getBestFPC function";
			double p_opt_cost = p.get_cost();
		double error=((execCost-p_opt_cost)/p_opt_cost)*100; //TODO: p.get_cost can be more than contour cost but its okay since error  
		if(error <= p.getpercent_err()){
			p.putfpc_cost(execCost);
			p.putpercent_err(error);
			p.set_fpcSpillDim(dim_index);
			//p.putfpc_plan(-2);
		}
//		else{
//			assert(false) : funName+ "the optimal spilling plan cannot have lesser cost than the fpc plan with error = "+error+" but earlier fpcError = "+p.getpercent_err();
//		}
		//p.set_fpcSpillDim(dim_index);
		return p.getfpc_cost();
	}



	void print_points_max(HashMap<Integer,point_generic>points_max, PrintWriter writer)
	{
		Iterator itr = points_max.keySet().iterator();
		Integer key;
		while(itr.hasNext())
		{
			key = (Integer)itr.next();
			writer.println("Max "+key+" = "+points_max.get(key).get_dimension(key));
		}
	}
	void print_partition(int[]best_partitioning, PrintWriter writer){
		int n = remainingDim.size();
	   	int part_num = 1;
        int i;
        for (i = 0; i < n; ++i)
            if (best_partitioning[i] > part_num)
                part_num = best_partitioning[i];
     
        /* Print the p partitions. */
        int p;
        for (p = part_num; p >= 1; --p) {
            writer.printf("{");
            /* If s[i] == p, then i + 1 is part of the pth partition. */
            for (i = 0; i < n; ++i)
                if (best_partitioning[i] == p)
                    writer.printf("%d, ",remainingDim.get(i) );
         //   System.out.printf("\b\b} ");
            writer.printf("} ");
        }
        writer.printf("\n");
	}
	
//	static  boolean next(int []s, int []m, int n) {
//        /* Update s: 1 1 1 1 -> 2 1 1 1 -> 1 2 1 1 -> 2 2 1 1 -> 3 2 1 1 ->
//        1 1 2 1 ... */
//        /*int j;
//        printf(" -> (");
//        for (j = 0; j &lt; n; ++j)
//            printf("%d, ", s[j]);
//        printf("\\b\\b)\\n");*/
//        int i = 0;
//        ++s[i];
//        while ((i < n - 1) && (s[i] > m[i] + 1)) {
//            s[i] = 1;
//            ++i;
//            ++s[i];
//        }
//     
//        /* If i is has reached n-1 th element, then the last unique partitiong
//        has been found*/
//        if (i == n - 1)
//            return false;
//     
//        /* Because all the first i elements are now 1, s[i] (i + 1 th element)
//        is the largest. So we update max by copying it to all the first i
//        positions in m.*/
//        int max = s[i];
//        for (i = i - 1; i >= 0; --i)
//            m[i] = max;
//     
//    /*  for (i = 0; i &lt; n; ++i)
//            printf("%d ", m[i]);
//        getchar();*/
//        return true;
//    }
	
//	static point_generic findMaxPoint(ArrayList<point_generic> contour_points, int dim_index)
//	{
//
//		int loc_opt;
//		int [] index_opt = new int[dimension];
//		int []index = new int[dimension];
//		int [] max_index= new int [dimension];
//		double min_error=100000.0;
//		point_generic max_point=contour_points.get(0);
//		max_index = max_point.get_point_Index();
//
//
//		int OptPlan, cur_category, category,loc,CurFpcPlan;
//		double NewFpcCost, CurFpcCost, CurOptCost,error;
//
//		point_generic cur;
//		ArrayList<point_generic> MaxList = new ArrayList<point_generic>();
//
//
//		for(int i=1;i<contour_points.size();i++)
//		{
//			cur=contour_points.get(i);
//			index=cur.get_point_Index();
//			if(index[dim_index] > max_index[dim_index])
//			{
//				max_point = contour_points.get(i);
//				max_index = max_point.get_point_Index();
//			}
//		}
//
//		/*
//		 * 1. Get the maxindex!
//		 * 2. Put all points having index[dim_index]=max_index into a list;
//		 * 3. Iterate over the list
//		 * 4. Iterate over all the plans, get the best FPC (with dim) at the point. Maintain the point with lowest FPC Cost.
//		 * 
//		 *  5.Return the point
//		 *  
//		 *  ---> Count the FPC/Opt error while doing the above, and show it!!
//		 */
//		//Put all points having index[dim_index]=max_index[dim_index] into a list;
//		for(int i=0;i<contour_points.size();i++)
//		{
//			cur=contour_points.get(i);
//			index=cur.get_point_Index();
//			if(index[dim_index] == max_index[dim_index])
//			{
//				MaxList.add(cur);
//			}
//		}
//		//Iterating over the MaxList to find the lowest cost FPC.
//		for(int i=0;i<MaxList.size();i++)
//		{
//			cur=MaxList.get(i);
//			index=cur.get_point_Index();
//			OptPlan=cur.get_plan_no();
//
//			cur_category = plans_list[OptPlan].getcategory(remainingDim);
//			if(cur_category==dim_index)
//			{
//				cur.set_goodguy();
//				return cur;
//			}
//			for(int j=0;j < nPlans;j++)
//			{
//				if(j==OptPlan)
//					continue;
//				category=plans_list[j].getcategory(remainingDim);
//				if(category == dim_index)
//				{
//					loc= getIndex(index,resolution);
//					NewFpcCost=AllPlanCosts[j][loc];
//
//					/*----------------*/
//					//					loc_opt = plans_list[j].getPoints().get(0);
//					//					index_opt=getCoordinates(dimension, resolution, loc_opt);
//					//					NewFpcCost=getSpillCost(dim_index, j, index_opt, index);
//					/*------------------*/
//
//
//					CurFpcCost=cur.getfpc_cost();
//					CurOptCost=cur.getopt_cost();
//
//					if(CurFpcCost == -1 || CurFpcCost > NewFpcCost)
//					{
//						cur.putfpc_cost(NewFpcCost);
//						cur.putfpc_plan(j);
//						error=((NewFpcCost-CurOptCost)/CurOptCost)*100;
//						cur.putpercent_err(error);
//
//					}
//				}
//			}
//
//		}
//
//		for(int i=0;i<MaxList.size();i++)
//		{
//			cur=MaxList.get(i);
//			index=cur.get_point_Index();
//			error = cur.getpercent_err();
//			if(error < min_error)
//			{
//				min_error = error;
//				max_point = cur;
//			}
//		}
//		/*
//		 * 1. We need to take the first point in case of a line.
//		 */
//		int cur_index,final_index =-1;
//		int[] m_index = new int [dimension];
//		m_index = max_point.get_point_Index();
//
//
//		cur_index = m_index[dim_index];
//		boolean flag;
//		for(int i=1;i<contour_points.size();i++)
//		{
//			flag = true;
//			cur=contour_points.get(i);
//			index=cur.get_point_Index();
//			for(int j=0;j<dimension;j++)
//			{
//				if(j!= dim_index && index[j] != m_index[j])
//				{
//					flag = false;
//					break;
//				}
//			}
//			if(flag == true && index[dim_index] < cur_index && max_point.get_cost() == cur.get_cost())
//				//			if(flag == true && index[dim_index] < cur_index)
//			{
//				//System.out.print("O");
//				//TODO : Try to invalidate the others from MSO calculation!
//				cur_index = index[dim_index];
//				max_point = cur;
//			}
//		}
//
//		max_point.set_fpcSpillDim(dim_index);
//		return max_point;
//	}


	double getBestFPC(point_generic p, int dim_index)
	{
		String funName = "getBestFPC";
		int category;
		int loc;
		double NewFpcCost,CurFpcCost, CurOptCost;
		int OptPlan = p.get_plan_no();
		assert(plans_list[OptPlan].getcategory(remainingDim)!=dim_index):"Trying for fpc for a good guy!!\n";
		int []index = new int[dimension];
		//p.putfpc_cost(-1);
		//	point_generic cur = p;
		index = p.get_point_Index();
		double error;
		loc= getIndex(index,resolution);

		for(int j=0;j < nPlans;j++)
		{
			if(j==OptPlan)
				continue;
			category=plans_list[j].getcategory(remainingDim);
			if(category == dim_index)
			{

				NewFpcCost=AllPlanCosts[j][loc];

				/*----------------*/
				//				loc_opt = plans_list[j].getPoints().get(0);
				//				index_opt=getCoordinates(dimension, resolution, loc_opt);
				//				NewFpcCost=getSpillCost(dim_index, j, index_opt, index);
				/*------------------*/


				CurFpcCost=p.getfpc_cost();
				double p_opt_cost = p.get_cost();
				CurOptCost=p_opt_cost;

				if(CurFpcCost == -1 || CurFpcCost > NewFpcCost)
				{
					p.putfpc_cost(NewFpcCost);
					p.putfpc_plan(j);
					error=((NewFpcCost-CurOptCost)/CurOptCost)*100;
					p.putpercent_err(error);
					p.set_fpcSpillDim(dim_index);
				}
			}
		}
		if(p.getfpc_cost() == Double.MAX_VALUE){
			//p.putfpc_cost(Double.MAX_VALUE);
			p.putfpc_plan(-1);
			double p_opt_cost = p.get_cost();
			error=((p.getfpc_cost()-p_opt_cost)/p_opt_cost)*100;
			p.putpercent_err(error);
		}
		//p.set_fpcSpillDim(dim_index);
		assert(dim_index!=p.getLearningDimension()) : funName+" it should not the aligned scenario";
		return p.getfpc_cost();
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

	void make_local_partition(List<List<Integer>> part){
		int n = this.remainingDim.size();
		//Iterate over part 
		int i=0;
		n_partition = part.size();
		cur_partition.clear();
		
		while(i < part.size()){
			temp_list = new ArrayList<Integer>(part.get(i));
			cur_partition.put(i,temp_list);
			i= i+1;
		}
		
	    int tot_part_size =0;
        int part_size = 0;
     for(int it=0;it<cur_partition.size();it++){
    	 part_size = cur_partition.get(it).size();
    	 tot_part_size += part_size;
    	 for(int it1=0;it1<part_size; it1++){
    		 for(int it2=it+1;it2<cur_partition.size();it2++)
            	 assert(!cur_partition.get(it2).contains(cur_partition.get(it).get(it1))): "the dimensions are not disjoint in partitions"; 
    	 }
     }
     assert(tot_part_size == n): "union of the elements in the partition is the total elements in the remaining dimension";
     
     partition_info = new partition[n_partition];

		Iterator itr = cur_partition.keySet().iterator();
		Integer i_key;
		for(i=0;i<n_partition;i++)
		{
			assert(itr.hasNext()==true):"cur_partition doesn't have enough keySet!!";
			//				partition_info[i] = new partition(i, p[i]);
			i_key = (Integer)itr.next();
			partition_info[i_key] = new partition(i_key, convertIntegers(cur_partition.get(i_key)));  
		}
		
	}
	void make_local_partition_old(int[] p_encoding, int dimension)
	{
		int n = this.remainingDim.size();
		
		int part_num = 1;
        int i;
        for (i = 0; i < n; ++i)
            if (p_encoding[i] > part_num)
                part_num = p_encoding[i];
      
        /* Print the p partitions. */
        int p;
        n_partition = part_num;
        cur_partition.clear();
        
        for (p = part_num; p >= 1; --p) {
       //     System.out.printf("{");
            /* If s[i] == p, then i + 1 is part of the pth partition. */
        	temp_list = new ArrayList<Integer>();
            for (i = 0; i < n; ++i)
                if (p_encoding[i] == p)
                {
                	temp_list.add(remainingDim.get(i));
                }
            cur_partition.put(p-1, temp_list);  
                //    System.out.printf("%d, ", i + 1);
         //   System.out.printf("\b\b} ");
         //   System.out.printf("} ");
        }
        
      //following code is for checking whether the elements in the partition are disjoint and union is the remaining dimensions
        int tot_part_size =0;
        int part_size = 0;
     for(int it=0;it<cur_partition.size();it++){
    	 part_size = cur_partition.get(it).size();
    	 tot_part_size += part_size;
    	 for(int it1=0;it1<part_size; it1++){
    		 for(int it2=it+1;it2<cur_partition.size();it2++)
            	 assert(!cur_partition.get(it2).contains(cur_partition.get(it).get(it1))): "the dimensions are not disjoint in partitions"; 
    	 }
     }
     assert(tot_part_size == n): "union of the elements in the partition is the total elements in the remaining dimension";
        
        //---------------
//        for(i=0;i<n_partition;i++)
//		{
//			temp_list = new ArrayList<Integer>();
//			for(int j=0;j<p[i].length;j++)
//			{
//				int k = p[i][j];
//				temp_list.add(k);
//			}
//			cur_partition.put(i, temp_list);
//			//			temp_list.clear();
//		}
        
        //----------------
        partition_info = new partition[n_partition];

		Iterator itr = cur_partition.keySet().iterator();
		Integer i_key;
		for(i=0;i<n_partition;i++)
		{
			assert(itr.hasNext()==true):"cur_partition doesn't have enough keySet!!";
			//				partition_info[i] = new partition(i, p[i]);
			i_key = (Integer)itr.next();
			partition_info[i_key] = new partition(i_key, convertIntegers(cur_partition.get(i_key)));  
		}
		
        
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



	public double getLearntSelectivity(PrintWriter writer, int dim, double cost,point_generic p, int contour_no) {

		int loc;
		memo m;
		boolean hashjoinFlag =false;
		String funName = "getLearntSelectivity";
		int []q_index = new int[dimension];
		int plan = p.get_plan_no();
		if(remainingDim.size()==1)
		{
			assert(false) : funName+" ERROR: entering one dimension condition";
			System.out.println(funName+"ERROR: entering one dimension condition");
			return 0;   //TODO dont do spilling in the 1D case, until we fix the INL case
		}
		double temp_cost;
		int p_loc;
		boolean minus_flag=false;
		double dir_cost;
		int dir_sel_index;
		p_loc = getIndex(p.get_point_Index(),resolution);
		if(p.get_goodguy() || !FPC_for_Alignment)
		{
			//If it is a good guy
			plan = p.get_plan_no();
			q_index = p.get_point_Index();
		}
		else if(FPC_for_Alignment)
		{	
			plan = p.getfpc_plan();
			loc = plans_list[plan].getPoints().get(0);
			q_index=getCoordinates(dimension, resolution, loc);
		}

		double multiplier = 1,selLearnt = Double.MIN_VALUE,execCost=Double.MIN_VALUE, prevExecCost = Double.MIN_VALUE;
		boolean sel_completely_learnt = false;
		int [] actual_sel_index = convertSelectivitytoIndex(actual_sel);
		Connection conn = null;
		Statement stmt = null;

		try {
			conn = source.getConnection();
			stmt = conn.createStatement();
			//System.out.println(funName+ " : database connection statement create successfully");
			//Settings: constants in BinaryTree   
			BinaryTree tree = new BinaryTree();
    		tree.initialize(qtName);
			tree.FROM_CLAUSE = FROM_CLAUSE;
//			int spill_values [] = tree.getSpillNode(dim,plan,"tpcds"); //[0] gives node id of the tree and [1] gives the spill_node for postgres
//			int spill_node = spill_values[1];
			String spill_pred = tree.predicates[dim];
            String spill_pred_rev = tree.predicatesRev[dim];
			/*
			 *temp_act_sel to iterate from point p to  until either we exhaust the budget 
			 *or learn the actual selectivity. Note the change. 
			 */
			//double[] temp_act_sel = new double[dimension];
			int [] temp_actual_index = new int[dimension];
			for(int d=0;d<dimension;d++){
				if(dim==d){
					//				if(selectivity[p.get_dimension(d)] >= actual_sel[d])
					//					temp_act_sel[d] = actual_sel[d]; //This line posed a problem, and hence commenting it. 
					//				else
					temp_actual_index[d] = p.get_dimension(d);
				}
				else
					temp_actual_index[d] = actual_sel_index[d];
			}


			double budget = cost;   //if this contour is the last for point p set the budget to  point's cost
			//			Lohit : what is the use of this? NEED TO CHANGE

			//
			if(p.get_goodguy())
				temp_cost = p.get_cost();
			else{
				//temp_cost = AllPlanCosts[plan][p_loc];
				temp_cost = p.getfpc_cost();
				//assert(temp_cost == p.getfpc_cost()) : funName+ "temp_cost wrt fpc is not set correctly";
			}
			
			if(temp_cost < 2*cost && (temp_cost > cost) )
				budget = temp_cost;

			//			if((p.get_cost() <2*cost) && (p.get_cost()>cost))
			//				budget = p.get_cost();


			File file=null;
			FileReader fr=null;
			BufferedReader br=null;
            String str1;
            String regx;
            Pattern pattern;
            Matcher matcher;
            Double hash_rows=-1.0;
			Index point_index = new Index(p,contour_no,dim);

			//      System.out.println(point_index.hashCode());
			//      System.out.println(point_index.equals(new Index(p,contour_no,dim)));
//			if(memoization_flag==true)
//			{
//				if(sel_for_point.containsKey(point_index))
//				{
//					m = sel_for_point.get(new Index(p,contour_no,dim));
//					dir_cost = m.getCost(actual_sel_index[dim]);
//					dir_sel_index = actual_sel_index[dim];
//					if( dir_cost == -1)
//					{
//						dir_cost = m.getHCost();
//						dir_sel_index = m.getHSel();
//					}
//					
//					learning_cost += dir_cost; 
//					if(print_flag)
//					{
//						System.out.println("Short circuit : Cost of the spilled execution is "+ dir_cost);
//					}
//					return selectivity[dir_sel_index];
//				}
//			}
			
			//m = new memo();
			//			if(sel_for_point.containsKey(new Index(p,contour_no,dim)))
			//			{
			//				selLearnt= sel_for_point.get(new Index(p,contour_no,dim));
			//				learning_cost = cost;
			//				System.out.println("Short circuit : Cost of the spilled execution is "+ cost);
			//				return selLearnt;
			//			}

			while((temp_actual_index[dim] <= actual_sel_index[dim]) || (execCost<=budget))
			{	
				minus_flag = false;
				stmt.execute("set spill_node = 999");

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
				if(FPC_for_Alignment)
					stmt.execute("set spill_optimization = off");
				else if (spill_opt_for_Alignment){
					stmt.execute("set spill_optimization = on");
					String selectedEPP=null, nonSelectedEPP=null;
					for(int i=0;i<varyingJoins.length();i+=2){
						if(i/2 == dim){
							selectedEPP = new String(varyingJoins.substring(i, i+2));
						}
						else if(remainingDim.contains(i/2)){
							if(nonSelectedEPP==null)
								nonSelectedEPP = new String(varyingJoins.substring(i, i+2));
							else{
								String str = new String(varyingJoins.substring(i, i+2));
								nonSelectedEPP = nonSelectedEPP.concat(str);
							}					
						}
					}
					stmt.execute("set selectedEPP = "+selectedEPP);
					stmt.execute("set nonSelectedEPP = "+nonSelectedEPP);
					
				}
				stmt.execute("set varyingJoins = "+varyingJoins);

				for(int d=0;d<dimension;d++){
					stmt.execute("set JS_multiplier"+(d+1)+ "= "+ JS_multiplier[d]);
					stmt.execute("set robust_eqjoin_selec"+(d+1)+ "= "+ selectivity[q_index[d]]);
					stmt.execute("set FPC_JS_multiplier"+(d+1)+ "= "+ JS_multiplier[d]);
					stmt.execute("set FPCrobust_eqjoin_selec"+(d+1)+ "= "+ selectivity[temp_actual_index[d]]);
					// essentially forcing the  plan optimal at (x,y) location to the query having (x_a,y_a) 
					// as selectivities been injected 
				}

				ResultSet rs = stmt.executeQuery(query);
				//   if(spill_node == 543){
				// 	System.out.println("We are at node 23");
				// }
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
				// Commenting this assert since it is possible that execCost is greater than the budget due to Grid issues
				//For instance, a 6K cost point can be part of 2K cost contour
				// assert(execCost<=budget) : funName+" execution cost of spilling is greater than the optimal cost at that position";

				String valstring = new String();
				/*
				 * If there are two rows in the file then it is the hash join case. Where the second row is the number of rows
				 * fetched by the predicate at p.getDimension(dim) selectivity 
				 */
//				if(hash_join_optimization_flag)				{
					if(hashjoinFlag){
						double rows = hash_rows;
						double remainingBudget = budget -execCost; 
						double  budget_needed=Double.MIN_VALUE;
						if(remainingBudget<=0)
						{
							temp_actual_index[dim] = temp_actual_index[dim] -1;
							//m.add(temp_actual_index[dim], cost);
							break;
						}
						//assert(newrows>=rows) : funName+" hashjoin case ";
						//0.01 is used since this is the cost taken for outputting every tuple (=cpu_tuple_cost)
						//						int k;
						//						for( k=actual_sel_index[dim];k<resolution;k++)
						//						{
						//							budget_needed= (rows*0.01)*(selectivity[k]/selectivity[p.get_dimension(dim)] -1);
						//							if(budget_needed > remainingBudget)
						//								break;
						//						}

						//	budget_needed= (rows*0.01)*(actual_sel[dim]/selectivity[p.get_dimension(dim)] -1);
						//					if(budget_needed <=0){
						//						sel_completely_learnt = true;
						//						temp_actual_index[dim] = actual_sel_index[dim];
						//						//already p is at actual_sel[dim]
						//					}
						//	assert(budget_needed !=Double.MIN_VALUE):"Budget_needed is not updated properly!!";
						//	if(budget_needed > remainingBudget){

						/*
						 * temp_act_sel[dim] contains the selectivity that would have been learnt
						 * with the remaining budget
						 */
						double temp_sel_learnt = Double.MIN_VALUE;
						//			temp_sel_learnt = selectivity[p.get_dimension(dim)] + (selectivity[k]- selectivity[p.get_dimension(dim)])*(remainingBudget/budget_needed);

						temp_sel_learnt = selectivity[p.get_dimension(dim)]*(1+(remainingBudget/(rows*0.01)));

						assert(temp_sel_learnt > selectivity[p.get_dimension(dim)]) : funName+" there is no increment in learnt selectivity even with increase in budget";
						temp_actual_index[dim] = findNearestPoint(temp_sel_learnt);
						if(selectivity[temp_actual_index[dim]]> temp_sel_learnt){					
							temp_actual_index[dim]--;
							assert(temp_actual_index[dim]>=0) : funName+" index going below 0";
						}
						selLearnt = selectivity[temp_actual_index[dim]];
						
						double extra_budget_consumed = (rows*0.01)*(selectivity[temp_actual_index[dim]]/selectivity[p.get_dimension(dim)] -1);
						prevExecCost = execCost + extra_budget_consumed;
						if(temp_actual_index[dim] >= actual_sel_index[dim])
						{
							sel_completely_learnt=true;
						}
						//m.add(temp_actual_index[dim], prevExecCost);
						//	execCost = prevExecCost;	
						//	}
						//						else if(budget_needed <= remainingBudget){
						//							sel_completely_learnt = true;
						//							//	temp_actual_index[dim] = actual_sel_index[dim];
						//							execCost += budget_needed;
						//							prevExecCost=execCost;
						//						}
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
				{
					//	temp_actual_index[dim];
					minus_flag = true;
					break;
				}
				/*
				 * this is the case when we learn the actual selectivity since the execCost (subplan's cost) does not 
				 * exceed the plan's total budget
				 */


				//m.add(temp_actual_index[dim], execCost);
				

				prevExecCost = execCost;
				if(temp_actual_index[dim] == resolution-1)
				{
					sel_completely_learnt=true;
					break;
				}
				//adding the code for oneshot learning of predicate

				temp_actual_index[dim] = temp_actual_index[dim]+1;



				assert(temp_actual_index[dim]<resolution) : funName+" index exceeding resolution";

			
			if(prevExecCost!=Double.MIN_VALUE && temp_actual_index[dim] > actual_sel_index[dim]){

				sel_completely_learnt = true;
				//			assert(prevExecCost!=Double.MIN_VALUE):"PrevExec Cost is still the default value";
				//	break;
			}

			}
			
			
			if(sel_completely_learnt){			
				learning_cost += prevExecCost;
				if(minus_flag == true)
					selLearnt = selectivity[temp_actual_index[dim]-1];
				else
					selLearnt = selectivity[temp_actual_index[dim]];
				//				if(temp_actual_index[dim]!=actual_sel_index[dim])
				//				{
				//				System.out.println("dim= "+dim+", temp= "+temp_actual_index[dim]+", Actual= "+actual_sel_index[dim]);
				//				}
				//				
				//				assert(temp_actual_index[dim]==actual_sel_index[dim]) : "learnt selectivity is not actually complete";
				assert(prevExecCost!=Double.MIN_VALUE):"PrevExec Cost is still the default value";
				if(print_flag)
				{
					writer.println("hashjoinFlag= "+hashjoinFlag);
					writer.println("Cost of the spilled execution is "+prevExecCost);
				}
				//prevExecCost = execCost;
			}
			else{
				if(prevExecCost==Double.MIN_VALUE){
					learning_cost += cost; //just adding the cost of the contour in the while loop zips through in the starting iteration itself
					prevExecCost = cost; //for the sake of printing
					selLearnt = selectivity[p.get_dimension(dim)];
					if(hashjoinFlag)
						selLearnt = selectivity[temp_actual_index[dim]];
					int idx = temp_actual_index[dim];
					if(!hashjoinFlag && idx>0){	
						idx --;
						assert(idx>=0): funName+" idx going below 0";
						selLearnt = selectivity[idx];
					}
//					if(!hashjoinFlag)
//						m.add(idx, cost);
					if(print_flag)
					{
						writer.println("hashjoinFlag= "+hashjoinFlag);
						writer.println("Zip-through - Cost of the spilled execution (not sucessful) is "+cost);
					}
				}
				else{
					//prevExecCost = cost;
					learning_cost += prevExecCost; //actual cost taken for learning selecitvity
					assert(prevExecCost!=Double.MIN_VALUE):"PrevExec Cost is still the default value";
					if(print_flag)
					{
						writer.println("hashjoinFlag= "+hashjoinFlag);
						writer.println("Cost of the spilled execution (not sucessful) is "+prevExecCost);
					}
					int idx = temp_actual_index[dim];
					if(!hashjoinFlag && idx>0){	
						idx --;
						assert(idx>=0): funName+" idx going below 0";
						selLearnt = selectivity[idx];
					}
				}
				execCost = prevExecCost;
			}
			//sel_for_point.put(new Index(p,contour_no,dim), m);
		}
		catch ( Exception e ) {
			System.err.println( e.getClass().getName()+": "+ e.getMessage() );
			e.printStackTrace();

		}finally{
    	if (conn != null) {
	        try { conn.close(); } catch (SQLException e) {}
	    }
    } 		
		assert(dim<=dimension) : funName+" dim data structure more dimensions possible";
		assert(selLearnt<=(double)1 && selLearnt >= selectivity[0]):funName+"selectivity learnt ["+selLearnt+"] is greater than 1!";
		if(print_flag)
		{
			writer.println("Postgres: selectivity learnt  "+selLearnt+" with plan number "+plan);
		}

		
		//	 if(!sel_for_point.containsKey(new Index(p,contour_no,dim)) && !sel_completely_learnt){
//		if(memoization_flag==true)
//		{
//			if(!sel_for_point.containsKey(new Index(p,contour_no,dim))){
//				ArrayList<Double> l = new ArrayList<Double>();
//				l.add(selLearnt); l.add(prevExecCost);
//				sel_for_point.put(new Index(p,contour_no,dim), l);
//			}
//		}
		
		return selLearnt; //has to have single line in the file

	}

	//Input : ori -> [1,2,3], m : Block size
	// Output : All partitioning of ori with number of partitions = m
	    List<List<List<Integer>>> helper(List<Integer> ori, int m) {
	        List<List<List<Integer>>> ret = new ArrayList<>();
	        if(ori.size() < m || m < 1) return ret;

	        if(m == 1) {
	            List<List<Integer>> partition = new ArrayList<>();
	            partition.add(new ArrayList<>(ori));
	            ret.add(partition);
	            return ret;
	        }

	        // f(n-1, m)
	        List<List<List<Integer>>> prev1 = helper(ori.subList(0, ori.size() - 1), m);
	        for(int i=0; i<prev1.size(); i++) {
	            for(int j=0; j<prev1.get(i).size(); j++) {
	                // Deep copy from prev1.get(i) to l
	                List<List<Integer>> l = new ArrayList<>();
	                for(List<Integer> inner : prev1.get(i)) {
	                    l.add(new ArrayList<>(inner));
	                }

	                l.get(j).add(ori.get(ori.size()-1));
	                ret.add(l);
	            }
	        }

	        List<Integer> set = new ArrayList<>();
	        set.add(ori.get(ori.size() - 1));
	        // f(n-1, m-1)
	        List<List<List<Integer>>> prev2 = helper(ori.subList(0, ori.size() - 1), m - 1);
	        for(int i=0; i<prev2.size(); i++) {
	            List<List<Integer>> l = new ArrayList<>(prev2.get(i));
	            l.add(set);
	            ret.add(l);
	        }

	        return ret;
	    }

	
	
	
	
	void remove_from_partition(int dim)
	{
		int i,j,k;
		Integer i_key;
		Iterator itr = cur_partition.keySet().iterator();
		for(i=0;i<n_partition;i++)
		{
			assert(itr.hasNext()==true):"cur_partition doesn't have enough keySet!!";
			i_key = (Integer) itr.next();
			for(j=0;j<cur_partition.get(i_key).size();j++)
			{
				k = cur_partition.get(i_key).get(j);
				if(k==dim)
				{
					cur_partition.get(i_key).remove(new Integer(dim));
					if(cur_partition.get(i_key).size()==0)
					{
						n_partition = n_partition -1;
						cur_partition.remove(i_key);
					}

					return;
				}
			}
		}
	//	assert(1==0):"The dimension is not removed from the paritions!!";
	}
	
	void test_if_dimension_in_partition(int dim)
	{
		int i,j,k;
		Integer i_key;
		String funName = "test_if_dimension_in_partition";
		Iterator itr = cur_partition.keySet().iterator();
		for(i=0;i<n_partition;i++)
		{
			assert(itr.hasNext()==true):"cur_partition doesn't have enough keySet!!";
			i_key = (Integer) itr.next();
			for(j=0;j<cur_partition.get(i_key).size();j++)
			{
				assert(!cur_partition.get(i_key).contains(dim)) : funName+" removed dimension still present even after removing";
			}
		}
	//	assert(1==0):"The dimension is not removed from the paritions!!";
	}
	
	
	public void initialize(PrintWriter writer, int location) {

		//		ArrayList<Integer> a1 = new ArrayList<Integer>();
		//    	ArrayList<Integer> a2 = new ArrayList<Integer>();
		//    	ArrayList<Integer> a3 = new ArrayList<Integer>();
		//		ArrayList<Integer> r;
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
		//n_partition = p.length;
		cur_partition.clear();
		//		temp_list.clear();
		//	Iterator itr = cur_partition.keySet().iterator();
		int k;
//		for(int i=0;i<n_partition;i++)
//		{
//			//assert(itr.hasNext()==true):"cur_partition doesn't have enough keySet!!";
//			temp_list = new ArrayList<Integer>();
//			for(int j=0;j<p[i].length;j++)
//			{
//				k = p[i][j];
//				temp_list.add(k);
//			}
//			cur_partition.put(i, temp_list);
//			//			temp_list.clear();
//		}
		//		a1 = cur_partition.get(0);
		//    	a2 = cur_partition.get(1);
		//    	a3 = cur_partition.get(2);
		//    	r = OptSB.remainingDim;
		//    	System.out.println("HERE");

		learning_cost = 0;
		tmax = 0;
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
		if(print_flag)
		{
			writer.print("\nactual_sel=[");
		}
		for(int i=0;i<dimension;i++){
			actual_sel[i] = selectivity[index[i]];
			if(print_flag)
			{
				writer.print(actual_sel[i]+",");
			}
		}
		if(print_flag)
		{
			writer.print("]\n");
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


	public static int[] convertIntegers(List<Integer> integers)
	{
		int[] ret = new int[integers.size()];
		Iterator<Integer> iterator = integers.iterator();
		for (int i = 0; i < ret.length; i++)
		{
			ret[i] = iterator.next().intValue();
		}
		return ret;
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


	void readpkt(ADiagramPacket gdp, boolean allPlanCost) throws IOException
	{
		String funName="readpkt";
		//ADiagramPacket gdp = getGDP(new File(pktPath));
		totalPlans = gdp.getMaxPlanNumber();
		dimension = gdp.getDimension();
		resolution = gdp.getMaxResolution();
		data = gdp.getData();
		totalPoints = (int) Math.pow(resolution, dimension);
		//System.out.println("\nthe total plans are "+totalPlans+" with dimensions "+dimension+" and resolution "+resolution);
		int plan_no;
		this.loadPlans();

		assert (totalPoints==data.length) : "Data length and the resolution didn't match !";
		this.maxPoints = new point_generic[dimension];
		this.plans = new int [data.length];
		this.OptimalCost = new double [data.length]; 
		//	this.points_list = new point[resolution][resolution];
		int [] index = new int[dimension];


		int[]spillDim_planCnt = new int[dimension];
		double []spillDim_pointPercent = new double[dimension];

		int cat;
		for(int l=0;l<dimension;l++)
		{
			spillDim_planCnt[l] = 0;

			spillDim_pointPercent[l] = 0.0;
		}


		this.remainingDim = new ArrayList<Integer>();
		for(int i=0;i<dimension;i++)
			remainingDim.add(i);


		for (int i = 0;i < data.length;i++)
		{
			index=getCoordinates(dimension,resolution,i);
			//			points_list[index[0]][index[1]] = new point(index[0],index[1],data[i].getPlanNumber(),remainingDim);
			//			points_list[index[0]][index[1]].putopt_cost(data[i].getCost());
			this.OptimalCost[i]= data[i].getCost();
			this.plans[i] = data[i].getPlanNumber();
			plan_no = data[i].getPlanNumber();
			plans_list[plan_no].addPoint(i);

			cat = plans_list[plan_no].getcategory(remainingDim);
			//			if(cat == 3)
			//			{
			//				System.out.println("Here");
			//			}
			spillDim_planCnt[cat] = spillDim_planCnt[cat]+1;
			//System.out.println("At "+i+" plan is "+plans[i]);
			//System.out.println("Plan Number ="+plans[i]+"\n");
			//	System.out.println("\nOptimal_cost:["+i+"] ="+OptimalCost[i]+"\n");
		}
		for(int l=0;l<dimension;l++)
		{
			spillDim_pointPercent[l] = (double)spillDim_planCnt[l]*100/data.length;
		}
		//	System.out.println("\n SpillDim_pointCount-----");
		for(int l=0;l<dimension;l++)
		{
			System.out.println("Points spilling on dim "+l+" = "+spillDim_pointPercent[l]+"%");
		}



		minIndex = new double[dimension];
		maxIndex = new double[dimension];

		// ------------------------------------- Read pcst files
		nPlans = totalPlans;
		if(allPlanCost){
			AllPlanCosts = new double[nPlans][totalPoints];
			//costBouquet = new double[total_points];
			
			int x,y;
			for (int i = 0; i < nPlans; i++) {
				try {

					ObjectInputStream ip = new ObjectInputStream(new FileInputStream(new File(plansPath + i + ".pcst")));
					double[] cost = (double[]) ip.readObject();
					for (int j = 0; j < totalPoints; j++)
					{
						/*
							index=getCoordinates(dimension,resolution,j);
							x=index[0];
							y=index[1];
							CurFpcCost=points_list[x][y].getfpc_cost();
							CurOptCost=points_list[x][y].getopt_cost();

							if(CurFpcCost == -1 || CurFpcCost > cost[j])
							{
								points_list[x][y].putfpc_cost(cost[j]);
								points_list[x][y].putfpc_plan(i);
								error=((cost[j]-CurOptCost)/CurOptCost)*100;
								points_list[x][y].putpercent_err(error);
							}
						 */

						AllPlanCosts[i][j] = cost[j];
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		
		/*
				 for(int i=0;i<resolution;i++)
				{
					for(int j=0;j<resolution;j++)
					{
						error=points_list[i][j].getpercent_err();
						if(error>max_err)
							max_err = error;
					}
				}
				System.out.println("\nMax_error="+max_err+"\n");
		 */

		//Srinivas's code for checking FPC
		/*to test the FPC functionality*/
		int fpc_count=0;
		//double error;
		double CurFpcCost,CurOptCost;
		double error,max_error=-1;
		for(int i=0;i<data.length;i++){		
			int p = data[i].getPlanNumber();
			double c1= data[i].getCost(); //optimal cost at i
			double c2 = AllPlanCosts[p][i]; //plan p's cost at i from pcst files
			//if(! (Math.abs(c1 - c2) < 0.05*c1 || Math.abs(c1 - c2) < 0.05*c2) ){
			if((c2-c1) > 0.05*c1){
				int [] ind = getCoordinates(dimension, resolution, i);
				error = (double)Math.abs(c1 - c2)*100/c1;
				if(error > max_error)
				{
					max_error = error;
				}
				//			System.out.printf("\nFPC ERROR: Plan: %4d, Loc(%3d, %3d,%3d): , pktCost: %10.1f, fpcOptCost: %10.1f, error: %4.2f", p, ind[0], ind[1],ind[2],c1, c2, (double)Math.abs(c1 - c2)*100/c1);
				fpc_count++;
			}

			//				else
			//					System.out.printf("\nFPC ERROR: Plan: %4d, Loc(%3d, %3d): (%6.4f, %6.4f), pktCost: %10.1f, fpcOptCost: %10.1f, error: %4.2f", p, i, selectivity[i], selectivity[j], c1, c2, (double)Math.abs(c1 - c2)*100/c1);
		}
		System.out.println(funName+": FPC ERROR: for 5% deviation is "+fpc_count+" Max Error%="+max_error);

		}


	}
	void loadPlans() throws NumberFormatException, IOException
	{

		plans_list = new plan[totalPlans];
		for(int i=0;i<totalPlans;i++)
		{
			plans_list[i]=new plan(i);
		}


	}

	/*-------------------------------------------------------------------------------------------------------------------------
	 * Populates -->
	 * 	dimension
	 * 	resolution
	 * 	totalPoints
	 * 	OptimalCost[][]
	 * 
	 * */
	// OLD readpkt function
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
	//				this.OptimalCost[i]= data[i].getCost();
	//				this.plans[i] = data[i].getPlanNumber();
	//			
	//			}
	//
	//			//To get the number of points for each plan
	//			int  [] plan_count = new int[totalPlans];
	//			for(int p=0;p<data.length;p++){
	//				plan_count[plans[p]]++;
	//			}
	//			//printing the above
	//			for(int p=0;p<plan_count.length;p++){
	//				System.out.println("Plan "+p+" has "+plan_count[p]+" points");
	//			}
	//
	//			/*
	//			 * Reading the pcst files
	//			 */
	//			nPlans = totalPlans;
	//			AllPlanCosts = new double[nPlans][totalPoints];
	//			for (int i = 0; i < nPlans; i++) {
	//				try {
	//
	//					ObjectInputStream ip = new ObjectInputStream(new FileInputStream(new File(apktPath + i + ".pcst")));
	//					double[] cost = (double[]) ip.readObject();
	//					for (int j = 0; j < totalPoints; j++)
	//					{					
	//						AllPlanCosts[i][j] = cost[j];
	//					}
	//				} catch (Exception e) {
	//					e.printStackTrace();
	//				}
	//			}
	//
	//			 remainingDim = new ArrayList<Integer>();
	//				for(int i=0;i<dimension;i++)
	//					remainingDim.add(i);
	//				minIndex = new double[dimension];
	//				maxIndex = new double[dimension];
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
	static public int getIndex(int[] index,int res)
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
		/*
		 * Need dimension.
		 */


		Properties prop = new Properties();
		InputStream input = null;

		try {
			if(src_flag)
			{
				input = new FileInputStream(OptSB.sourcePath+"Constants.properties");
			}
			else
			{
				input = new FileInputStream(OptSB.plansPath+"Constants.properties");
			}
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
			if(spill_opt_for_Alignment == true){
				query_opt_spill = prop.getProperty("query_opt_spill");
			}
			
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
			input.close();

		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	static void paths_init()
	{
		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream(OptSB.sourcePath+"Constants.properties");

			// load a properties file
			prop.load(input);

			// get the property value and print it out
			apktPath = prop.getProperty("apktPath");
			qtName = prop.getProperty("qtName");
			QTName = prop.getProperty("qtName");
			plansPath = apktPath;
			cardinalityPath = prop.getProperty("cardinalityPath");
			input.close();

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	// OLD LOAD PROPERTIES FUNCTION
	//	public void loadPropertiesFile() {
	//
	//		Properties prop = new Properties();
	//		InputStream input = null;
	//	 
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
	//			varyingJoins = prop.getProperty("varyingJoins");
	//			
	//			JS_multiplier = new double[dimension];
	//			for(int d=0;d<dimension;d++){
	//				String multiplierStr = new String("JS_multiplier"+(d+1));
	//				JS_multiplier[d] = Double.parseDouble(prop.getProperty(multiplierStr));
	//			}
	//			
	//			//Settings:Note dont forget to put analyze here
	//			//query = "explain analyze FPC(\"lineitem\") (\"104949\")  select	supp_nation,	cust_nation,	l_year,	volume from	(select n1.n_name as supp_nation, n2.n_name as cust_nation, 	DATE_PART('YEAR',l_shipdate) as l_year,	l_extendedprice * (1 - l_discount) as volume	from	supplier, lineitem, orders, 	customer, nation n1,	nation n2 where s_suppkey = l_suppkey	and o_orderkey = l_orderkey and c_custkey = o_custkey		and s_nationkey = n1.n_nationkey and c_nationkey = n2.n_nationkey	and  c_acctbal<=10000 and l_extendedprice<=22560 ) as temp";
	//			//query = "explain analyze FPC(\"catalog_sales\")  (\"150.5\") select ca_zip, cs_sales_price from catalog_sales,customer,customer_address,date_dim where cs_bill_customer_sk = c_customer_sk and c_current_addr_sk = ca_address_sk and cs_sold_date_sk = d_date_sk and ca_gmt_offset <= -7.0   and d_year <= 1900  and cs_list_price <= 150.5";
	//			query = prop.getProperty("query");
	//			query_opt_spill = prop.getProperty("query_opt_spill");
	//			
	//			cardinalityPath = prop.getProperty("cardinalityPath");
	//			
	//			
	//			int from_clause_int_val = Integer.parseInt(prop.getProperty("FROM_CLAUSE"));
	//			
	//			if(from_clause_int_val == 1)
	//				FROM_CLAUSE = true;
	//			else
	//				FROM_CLAUSE = false;
	//			
	//			/*
	//			 * 0 for sel_distribution is used for tpch type queries
	//			 */
	//			sel_distribution = Integer.parseInt(prop.getProperty("sel_distribution"));
	//			
	//			/*
	//			 * 0 for database_conn is used for tpch type queries
	//			 */
	//			database_conn = Integer.parseInt(prop.getProperty("database_conn"));
	//			
	//		} catch (IOException ex) {
	//			ex.printStackTrace();
	//		}
	//	}


	void getContourPointsLohitOld(double cost, double errorboundval) throws IOException
	{
		//Assume 
		//1. there is a List named "final_points";
		//2. Make sure you emptied "final_points" before calling this function; 


		ArrayList<Integer> remainingDimList = new ArrayList<Integer>();
		for(int i=0;i<dimension;i++)
		{
			if(learntDim.contains(i)!=true)
			{
				remainingDimList.add(i);
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
					System.out.println("i="+i+", indexof(i)= "+learntDim.indexOf(i)+"\n");
					arr[i] = learntDimIndices.get(i);
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

				if(cur_val >= targetval - errorboundval && cur_val <= targetval + errorboundval)
				{

					point_generic p = new point_generic(arr,getPlanNumber_generic(arr),cur_val, remainingDim);
					all_contour_points.add(p);
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
			getContourPointsLohitOld(cost, err*cost);
			learntDim.remove(learntDim.indexOf(curDim));
			learntDimIndices.remove(curDim);
			cur_index = cur_index - 1;
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


				//	System.out.println("The location weight is "+locationWeight[i]+" and total weight is "+totalWeight);
				assert (locationWeight[i]<= (double)1) : "In getPlanCountArray: locationWeight is not less than 1";
			}
			//		if(locationWeight[i]>(double)1){
			//			System.out.println("In getPlanCountArray: locationWeight is not less than 1");
			//			System.out.println("location weight : "+locationWeight[i]+" at i="+i+" total weight="+totalWeight);
			//		}
		}

		//	System.out.println("The sum weight is "+sumWeight);
		assert(sumWeight<=(double)1*1.01) : "In getPlanCountArray: sumWeight is not less than 1";

		/*
		 * if(scaleupflag) { for(int i = 0; i < planCount.length; i++)
		 * planCount[i] /= 100Math.pow(10, getDimension()); }
		 */


		checkValidityofWeights();

	}

	// functions for location weight calculations and the checking for validity

	//////////////////////////////   LOCATION WEIGHT CALCULATIONS and PCM & POSP VALIDITY CHECKING CODE  //////////////////////////////////////////////////////////////

	public double maxHarmCalculation(int loc,double SO){
		int bestPlan  = getPCSTOptimalPlan(loc);
		double bestCost = AllPlanCosts[bestPlan][loc];
		int worstPlan = getPCSTWorstPlan(loc);
		double worstCost = AllPlanCosts[worstPlan][loc];
		double worst_native = (worstCost/bestCost);
		if(worst_native<1)
			System.out.println("MaxharmCalculation : error ");
		return (SO/worst_native - 1);
		//assert that this ratio is > 1
	}

	/*
	 * for each location get the cheapest plan using the pcst files. 
	 * This may be different from the optimizers choice due to imperfect
	 * FPC implementation
	 */
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
		
		//isContourPoint[getIndex(rightInfo.dim_values, resolution)] = true;		

		return getIndex(right, resolution);
	}
	
	public  void nexusAlgoContour(double searchCost) throws IOException 
	{
		int seed = 0;
	
		seed = findSeedLocation(searchCost);
		
		exploreSeed(seed, searchCost, dimension-1, dimension-2);
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

class OptSBOutputParamStruct {
	double pmax = 0;
	double tmax = 0;
	double MSO = 0;
	int sumSo = 0;
	double Harm = 0;
	double anshASO = 0;
	boolean flag = false;
	int num_of_points =0;
}
class OptSBinputParamStruct {
	OptSB obj;
	int min_index;
	int max_index;
	public OptSBinputParamStruct(OptSB obj, int min_index, int max_index) {
		this.obj = obj;
		this.min_index = min_index;
		this.max_index = max_index;
	}
}


class point_generic implements Serializable
{
	private static final long serialVersionUID = 223L;
	int dimension;
	static boolean load_flag = false;
//	int opt_plan=-1;
//	double opt_cost=-1.0;
	int fpcSpillDim = -1;
	boolean good_guy=false;
	int fpc_plan=-1;
	double fpc_cost = Double.MAX_VALUE;
	double percent_err = Double.MAX_VALUE;
	ArrayList<Integer> order;
	ArrayList<Integer> storedOrder;
	int value;
	int p_no;
	double cost;
	static String plansPath;
	int idx;
	int [] dim_values;
	

	public point_generic(point_generic pg) {
	
		this.dimension = pg.dimension;
		this.load_flag = pg.load_flag;
	//	this.opt_plan = pg.opt_plan;
	//	this.opt_cost = pg.opt_cost;
		this.fpcSpillDim = pg.fpcSpillDim;
		this.good_guy = pg.good_guy;
		this.fpc_plan = pg.fpc_plan;
		this.fpc_cost = pg.fpc_cost;
		this.percent_err = pg.percent_err;
		this.order = new ArrayList<Integer>();
		this.storedOrder = new ArrayList<Integer>();
		for(this.idx=0;this.idx < pg.order.size();this.idx++){
			this.order.add(pg.order.get(this.idx));
		}
		for(this.idx=0;this.idx < pg.storedOrder.size();this.idx++){
			this.storedOrder.add(pg.storedOrder.get(this.idx));
		}
		this.value = pg.value;
		this.p_no = pg.p_no;
		this.cost = pg.cost;
		this.plansPath = pg.plansPath;

		this.dim_values = new int[dimension];
		//System.arraycopy(pg.dim_values, 0, dim_values, 0, dimension);
		for(int it=0;it<dimension;it++)
			this.dim_values[it] = pg.dim_values[it];

	}
	point_generic(int arr[], int num, double cost,ArrayList<Integer> remainingDim) throws  IOException{

//		if(load_flag == false)
//		{
			loadPropertiesFile();
//			load_flag = true;
//		}
		//	System.out.println();
		dim_values = new int[dimension];
		for(int i=0;i<dimension;i++){
			dim_values[i] = arr[i];
			//		System.out.print(arr[i]+",");
		}
		//	System.out.println("   having cost = "+cost+" and plan "+num);
		this.p_no = num;
		this.cost = cost;

//		this.opt_cost=cost;
//		this.opt_plan=num;

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

//		if(load_flag == false)
//		{
			loadPropertiesFile();
//			load_flag = true;
//		}
		//	System.out.println();

		dim_values = new int[dimension];
		for(int i=0;i<dimension;i++){
			dim_values[i] = arr[i];
			//			System.out.print(arr[i]+",");
		}
		//	System.out.println("   having cost = "+cost+" and plan "+num);
		this.p_no = num;
		this.cost = cost;

//		this.opt_cost=cost;
//		this.opt_plan=num;

		//check: if the order and stored order are being updated/populated
		//order =  new ArrayList<Integer>(predicateOrder);
		//storedOrder = new ArrayList<Integer>(predicateOrder);	
		this.order = new ArrayList<Integer>();
		this.storedOrder = new ArrayList<Integer>();
		for(this.idx=0;this.idx < predicateOrder.size();this.idx++){
			this.order.add(predicateOrder.get(this.idx));
		}
		for(this.idx=0;this.idx < predicateOrder.size();this.idx++){
			this.storedOrder.add(predicateOrder.get(this.idx));
		}
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
	int getfpc_plan(){
		return this.fpc_plan;
	}
	void putfpc_plan(int p_no){
		this.fpc_plan=p_no;
	}

	double getfpc_cost(){
		return this.fpc_cost;
	}
	void putfpc_cost(double cost){
		this.fpc_cost=cost;
	}
//
//	int getopt_plan(){
//		return this.opt_plan;
//	}
//	double getopt_cost(){
//		return this.opt_cost;
//	}
//	void putopt_cost(double cost){
//		this.opt_cost=cost;
//	}

	double getpercent_err(){
		return this.percent_err;
	}
	void putpercent_err(double p_err){
		this.percent_err=p_err;
	}
	void set_goodguy(){
		this.good_guy = true;
	}
	void set_badguy(){
		this.good_guy = false;
	}
	boolean get_goodguy(){
		return this.good_guy;
	}
	void set_fpcSpillDim(int dim)
	{
		this.fpcSpillDim = dim;
	}
	int get_fpcSpillDim()
	{
		return this.fpcSpillDim;
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

	public void printPoint(){

		for(int i=0;i<dimension;i++){
			System.out.print(dim_values[i]+",");
		}
		System.out.println("   having cost = "+cost+" and plan "+p_no);
	}

	public void loadPropertiesFile() {

		Properties prop = new Properties();
		InputStream input = null;

		try {

			//	input = new FileInputStream("./src/Constants.properties");
			if(OptSB.src_flag)
			{
				input = new FileInputStream(OptSB.sourcePath+"Constants.properties");
			}
			else
			{
				input = new FileInputStream(OptSB.plansPath+"Constants.properties");
			}
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


	/*
	 * public void loadPropertiesFile() {

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
	 * */

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



class partition
{
	//While adding into the partition, make a track of the max_points!
	int p_number= -1;
	int no_of_dims;
	int [] dim_list ;
	ArrayList<point_generic> cur_partition_local = new ArrayList<point_generic>();

	HashMap<Integer,ArrayList<point_generic>> points_max_local = new HashMap<Integer,ArrayList<point_generic>>();

	double [] sel_max = new double[OptSB.dimension];

	//Constructor
	partition(int partition_number, int d_list[])
	{
		this.p_number = partition_number;
		no_of_dims = d_list.length;
		this.dim_list = new int[no_of_dims];
		System.arraycopy(d_list, 0, dim_list, 0, no_of_dims);
		//	System.out.println("\nDimension from the main class is "+OptSB.dimension+"\n");
	}
	//Functions
	void clearPartition()
	{
		cur_partition_local.clear();
	}

	void addPoint(point_generic p)
	{
		int i;
		//While adding maintain the the max points.
		/*
		 * Get the learningDim
		 */
		//		int learning_dim;
		//		learning_dim = p.getLearningDimension();
		//		for(int i=0;i<OptSB.dimension;i++)
		for(int j=0;j<dim_list.length;j++)
		{
			i = dim_list[j];

			if(OptSB.selectivity[p.get_dimension(i)] >= sel_max[i]){
			
				//If it is strictly greater the existing points, then add only the new point, else append into the list
				if(OptSB.selectivity[p.get_dimension(i)] > sel_max[i]){
					if(points_max_local.containsKey(i))
					{
						points_max_local.remove(i);
						
					}
				}
				if(!points_max_local.containsKey(i)){
					points_max_local.put(i, new ArrayList<point_generic>());
				}
			//	}
				points_max_local.get(i).add(p);
				sel_max[i] = OptSB.selectivity[p.get_dimension(i)]; 
			}

		}
		this.cur_partition_local.add(p);
	}

	ArrayList<point_generic> getList()
	{
		return this.cur_partition_local;
	}
	
	
	ArrayList<point_generic> getMaxPoints(int dim)
	{
		Boolean flag=false;
		for(int i=0;i<no_of_dims;i++)
		{
			if(dim_list[i] == dim)
			{
				flag = true;
				break;
			}
		}
		assert(flag==true): "\nDimension not in this partition\n";

		return points_max_local.get(dim);
	}
}

class plan{
	ArrayList<Integer> order;
	int category;
	int value;
	ArrayList<Integer> plan_locs;

	plan(int p_no) throws NumberFormatException, IOException{
		plan_locs = new ArrayList <Integer>();
		order =  new ArrayList<Integer>();

		FileReader file = new FileReader(OptSB.plansPath+"predicateOrder/"+p_no+".txt");

		BufferedReader br = new BufferedReader(file);
		String s;
		while((s = br.readLine()) != null) {
			//System.out.println(Integer.parseInt(s));
			// If the number is the remaining dimensions then only add it to the order

			value = Integer.parseInt(s);
			//System.out.println("\nvalue="+value+"\n");
			order.add(value);
		}
		category = order.get(0);
		br.close();
		file.close();
	}
	void addPoint(int loc)
	{
		this.plan_locs.add(loc);
		return;
	}
	ArrayList<Integer> getPoints()
	{
		return this.plan_locs;
	}
	int getcategory(ArrayList<Integer> remainingDim){
		int len = order.size();
		int cur_dim;
		for(int i=0;i<len;i++)
		{
			cur_dim = order.get(i);
			if(remainingDim.contains(cur_dim))
			{
				//category=cur_dim;
				//return category;
				return(cur_dim);
			}
		}
		System.out.println("\nError in getcategory\n");
		return -1;

	}
}

class memo
{
	HashMap<Integer,Double> map; 
	ArrayList<Integer> sel_loc_indexes; 
	ArrayList<Double> cost;
	double h_cost=Double.MIN_VALUE;
	int h_sel_index=Integer.MIN_VALUE;
	memo()
	{
		this.sel_loc_indexes = new ArrayList<Integer>();
		this.cost = new ArrayList<Double>();
		this.map = new HashMap<Integer,Double>();
	}
	void add(int sel_index, double c)
	{
		sel_loc_indexes.add(sel_index);
		cost.add(c);
		map.put(sel_index, c);
		if(c > h_cost)
		{
			this.h_cost = c;
			assert(sel_index >= h_sel_index):"Higher cost for lesser selectivity!!";
			this.h_sel_index = sel_index;
		}
	}
	double getCost(int sel)
	{
		if(this.map.containsKey(sel))
		{
			return this.map.get(sel);
		}
		else
		{
			return -1;
		}
	}
	int getHSel()
	{
		assert(h_sel_index!=Integer.MIN_VALUE):"highest sel has default value";
		return this.h_sel_index;
	}
	double getHCost()
	{
		assert(h_cost!=Double.MIN_VALUE):"highest cost has default value";
		return this.h_cost;
	}
	ArrayList<Integer> getSelArray()
	{
		return this.sel_loc_indexes;
	}
	ArrayList<Double> getCostArray()
	{
		return this.cost;
	}
}
