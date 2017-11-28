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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
//import java.util.Set;
import java.util.Comparator;

import org.apache.commons.lang3.builder.CompareToBuilder;

import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;
import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.server.plan.Node;
import iisc.dsl.picasso.server.plan.Plan;


public class OfflinePB
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
	float selectivity[];
	static String select_query;
	static String predicates;
	static int database_conn=1;
	static boolean loadAllPlanCosts = true;
	
	static Vector<Plan> plans_vector = new Vector<Plan>();
	
	//The following parameters has to be set manually for each query
	static String apktPath;
	static String qtName ;
	static String cardinalityPath;
	
	static int sel_distribution;
	static boolean MSOCalculation = true;
	static boolean memoization = true;
	static Connection conn = null;
	static float beta;
	static ArrayList<Integer> remainingDim;
	static ArrayList<ArrayList<Integer>> allPermutations = new ArrayList<ArrayList<Integer>>();
	 static ArrayList<location> all_contour_points = new ArrayList<location>();
	 static ArrayList<Integer> learntDim = new ArrayList<Integer>();
		//static ArrayList<Integer> learntDimIndices = new ArrayList<Integer>();
	 static HashMap<Integer,Integer> learntDimIndices = new HashMap<Integer,Integer>();
	 static HashMap<Integer,ArrayList<location>> ContourPointsMap = new HashMap<Integer,ArrayList<location>>();
	 static HashMap<Integer,Integer> uniquePlansMap = new HashMap<Integer,Integer>();
	 static HashMap<Integer,Float> minContourCostMap = new HashMap<Integer,Float>();
	 static double learning_cost = 0;
	 static boolean done = false;

	 float[] actual_sel;
	 static int opt_calls = 0;
	 
	 //for ASO calculation 
	 static double planCount[], planRelativeArea[];
	 static float picsel[], locationWeight[];

	 static double areaSpace =0,totalEstimatedArea = 0;
	 
	 static onlinePB opb = new onlinePB(); 
	 //parameters to set
	 static boolean visualisation_2D = false;
	 static int decimalPrecision = 6;
	 static float alpha = 2;
	 static boolean contoursReadFromFile = false;
	 static boolean nexus_bcg = false;
	 static float minimum_selectivity = 0.0001f;
	 static int location_hits =0;
	 static int explore_seed_count =0;
	 static boolean using_packets = true;
	 static float lambda = 20;
	 static boolean trie = false;
	 static online_vertex root;
	 
	public static void main(String args[]) throws IOException, SQLException, PicassoException, ClassNotFoundException
	{
		long startTime = System.nanoTime();
		OfflinePB obj = new OfflinePB();
		obj.loadPropertiesFile();
		String pktPath = apktPath + qtName + "_new9.4.apkt" ;
		//String pktPath = apktPath + qtName + ".apkt" ;
		System.out.println("Query Template: "+qtName);
		
		File f_marwa = new File("/home/dsladmin/marwa");
		ADiagramPacket gdp = obj.getGDP(new File(pktPath));
		
		//ADiagramPacket reducedgdp = obj.cgFpc(threshold, gdp,apktPath);

		//Settings
		//Populate the OptimalCost Matrix.
		
		root = new online_vertex(-1);
		
		obj.readpkt(gdp);
		
		//obj.readpkt(reducedgdp);

		//obj.replaceUnWantedPlans();
		//Populate the selectivity Matrix.
		
		
		//Calculate Native MSO
		//obj.findingNativeMSO();
		
		obj.loadPropertiesFile();
		
		if(nexus_bcg)
			obj.loadSelectivity_nexus_bcg();
		else
			obj.loadSelectivity();

		
		try{
			System.out.println("entered DB conn1");
			Class.forName("org.postgresql.Driver");

			//Settings
			//System.out.println("entered DB conn2");
			if(database_conn==0){
				conn = DriverManager
						.getConnection("jdbc:postgresql://localhost:5432/tpch9.4",
								"sa", "database");
			}
			else{
				if(f_marwa.exists() && !f_marwa.isDirectory()) { 
				System.out.println("entered DB tpcds");
				conn = DriverManager
						.getConnection("jdbc:postgresql://localhost:5431/tpcds-ai",
								"sa", "database");
				}
				else{
					System.out.println("entered DB tpcds");
					conn = DriverManager
							.getConnection("jdbc:postgresql://localhost:5432/tpcds-ai",
									"sa", "database");
				}
			}
			System.out.println("Opened database successfully");
		}
		catch ( Exception e ) {
			System.out.println("entered DB err");
			System.err.println( e.getClass().getName()+": "+ e.getMessage() );
		}

		//just to initialize the static variable of location object
		location loc = new location();
		loc.dimension = obj.dimension;
		loc.select_query = new String(obj.select_query);
		loc.predicates  = new String(obj.predicates);
		loc.database_conn = obj.database_conn;
		loc.apktPath = obj.apktPath;
		loc.decimalPrecision = obj.decimalPrecision;
		
		int i;
		double h_cost, min_cost; 
		
		if(visualisation_2D){
			obj.dimension = 2;
			int [] h_loc_arr = {resolution-1,resolution-1};
			location h_loc = new location(obj.convertIndextoSelectivity(h_loc_arr),obj);
			h_cost = h_loc.get_cost();
			if(trie)
				obj.addLocationtoGraph(h_loc);
			
			int [] l_loc_arr = {0,0};
			location l_loc = new location(obj.convertIndextoSelectivity(l_loc_arr),obj);
			min_cost = l_loc.get_cost();
			if(trie)
				obj.addLocationtoGraph(l_loc);

		}
		else{
			int [] h_loc_arr = new int[obj.dimension];
			int [] l_loc_arr = new int[obj.dimension];
			for(int d=0;d < obj.dimension; d++){
				h_loc_arr[d] = resolution-1;
				l_loc_arr[d] = 0;
			}
			location h_loc = new location(obj.convertIndextoSelectivity(h_loc_arr),obj);
			if(trie)
				obj.addLocationtoGraph(h_loc);
			h_cost = h_loc.get_cost();
						
			location l_loc = new location(obj.convertIndextoSelectivity(l_loc_arr),obj);
			if(trie)
				obj.addLocationtoGraph(l_loc);
			min_cost = l_loc.get_cost();
		}
			
		
		
		double ratio = h_cost/min_cost;
	//	System.out.println("-------------------------  ------\n"+qtName+"    alpha="+alpha+"\n-------------------------  ------"+"\n"+"Highest Cost ="+h_cost+", \nRatio of highest cost to lowest cost ="+ratio);
		System.out.println("the ratio of C_max/c_min is "+ratio);
		
		i = 1;
		
		double cost = min_cost;
		//cost*=2;
		
		//reload the properties file for the new resolution and total points
		
		File ContoursFile = new File(apktPath+"offlinePB/Contours.map");

		if(contoursReadFromFile && ContoursFile.exists()){
			obj.readContourPointsFromFile(false);
		}
		else
		{	
			while(cost < 2*h_cost)
			{
				if(cost>h_cost)
					cost = h_cost;
				System.out.println("---------------------------------------------------------------------------------------------\n");
				System.out.println("Contour "+i+" cost : "+cost+"\n");

				all_contour_points.clear();

				obj.nexusAlgoContour(cost); 
				
				for(location l: all_contour_points){
					if(minContourCostMap.containsKey(i) && l.get_cost()<minContourCostMap.get(i)){
						minContourCostMap.remove(i);
						minContourCostMap.put(i, l.get_cost());
					}
					else if(!minContourCostMap.containsKey(i))
						minContourCostMap.put(i, l.get_cost());

					l.set_contour_no((short) i);
				}
				
				if(visualisation_2D)
					writeContourPointstoFile(i);
				System.out.println("The running optimization calls are "+opt_calls);
				int size_of_contour = all_contour_points.size();
				ContourPointsMap.put(i, new ArrayList<location>(all_contour_points)); //storing the contour points
				System.out.println("Size of contour"+size_of_contour );
				cost = cost*2;
				i = i+1;
			}
			long endTime = System.nanoTime();
			System.out.println("Took "+(endTime - startTime)/1000000000 + " sec");
			System.out.println("The location hits are "+location_hits);
			System.out.println("The explore see count hits are "+explore_seed_count);
			

		}
		
		obj.ContourCentricCostGreedy(-1);
		
		System.exit(0);
		
		/*
		 * running the plan bouquet algorithm 
		 */
		double MSO =0, ASO = 0,SO=0,anshASO = 0,MaxHarm=-1*Double.MAX_VALUE,Harm=Double.MIN_VALUE;
		int ASO_points=0;
		int min_point =0;
		obj.getPlanCountArray();
		//int max_point = 0; /*to not execute the spillBound algorithm*/
		int max_point = 1; /*to execute a specific q_a */
		//Settings
		if(MSOCalculation)
		//if(false)
			max_point = totalPoints;
		double[] subOpt = new double[max_point];
		
    	
		
	  for (int  j = min_point; j < max_point ; j++)
	  {
		System.out.println("Entering loop "+j);

//		if(j<5007)
//			continue;
		//initialization for every loop
		double algo_cost =0;
		SO =0;
		cost = obj.getOptimalCost(0);
		obj.initialize(j);
		int[] index = obj.getCoordinates(obj.dimension, obj.resolution, j);
//		if(index[0]%5 !=0 || index[1]%5!=0)
//			continue;
//		obj.actual_sel[0] = 0.31;obj.actual_sel[1] = 0.3;obj.actual_sel[2] = 0.6; /*uncomment for single execution*/
		
		for(int d=0;d<obj.dimension;d++) obj.actual_sel[d] = obj.findNearestSelectivity(obj.actual_sel[d]);
		if(obj.cost_generic(obj.convertSelectivitytoIndex(obj.actual_sel))<10000 && !apktPath.contains("SQL"))
			continue;
		//----------------------------------------------------------
		i =1;
		while(i<=ContourPointsMap.size() && !done)
		{	
			if(cost<(double)10000 && !apktPath.contains("SQL")){
				cost *= 2;
				i++;
				continue;
			}
			assert (cost<=2*h_cost) : "cost limit exceeding";
			
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
		
		assert(done) : "In Main done variable not true even when the while loop is broken out";
		
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
//		Harm = obj.maxHarmCalculation(j, SO);
		if(Harm > MaxHarm)
			MaxHarm = Harm;
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
	  	
	  	System.out.println("OfflinePB The MaxSubOptimaility  is "+MSO);
	  	System.out.println("OfflinePB The MaxHarm  is "+MaxHarm);
	  	System.out.println("OfflinePB Anshuman average Suboptimality is "+(double)anshASO);
		System.out.println("OfflinePB The AverageSubOptimaility  is "+(double)ASO/ASO_points);


	}
	
	
	public void readContourPointsFromFile(boolean CG_done) throws ClassNotFoundException {

		try {
			ObjectInputStream ip= null;
			onlineLocationsMap obj;
			
			if(!CG_done){
				ip = new ObjectInputStream(new FileInputStream(new File(apktPath +"offlinePB/Contours.map")));
			}
			else{
				ip = new ObjectInputStream(new FileInputStream(new File(apktPath +"offlinePB/Red_Contours.map")));
			}
			
			obj = (onlineLocationsMap)ip.readObject();
			
			//TODO: need to change the below
			//ContourPointsMap = obj.getContourMap();
			Iterator itr = ContourPointsMap.keySet().iterator();
			while(itr.hasNext()){
				Integer key = (Integer) itr.next();
				
				//System.out.println("The no. of Anshuman locations on contour "+(st+1)+" is "+contourLocs[st].size());
				System.out.println("The no. of locations on contour "+(key.intValue())+" is "+ContourPointsMap.get(key).size());
				System.out.println("--------------------------------------------------------------------------------------");
				
			}
			

			
			if(CG_done){
				//iterator of 
				HashMap<Integer,HashSet<Integer>> contourPlansReduced = new HashMap<Integer,HashSet<Integer>>();
				// update what plans are in which contour
				HashSet<Integer> reducedPlansSet = new HashSet<Integer>();
				for (int k = 1; k <= ContourPointsMap.size(); k++) {
					Iterator iter = ContourPointsMap.get(k).iterator();
					reducedPlansSet.clear();
					while (iter.hasNext()) {
						location objContourPt = (location) iter.next();
						if (!reducedPlansSet.contains(objContourPt.reduced_planNumber)) {
							//TODO: removed the contour_no == k check condition; check again!
						//if (objContourPt.contour_no == k && !reducedPlansSet.contains(objContourPt.reduced_planNumber)) {
							assert(objContourPt.reduced_planNumber != -1) : "contour location not reduced";
							reducedPlansSet.add((int) objContourPt.reduced_planNumber);
						}
					}

					//				@SuppressWarnings("rawtypes")
					//				Iterator it = reducedPlansSet.iterator();
					//				while (it.hasNext()) {
					//					int p = (Short) it.next();
					//					contourPlansReduced.get(k).add(p);
					//				}
					contourPlansReduced.put(k, reducedPlansSet);

					System.out.println("Contour"+k+" = "+reducedPlansSet.size());
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.exit(0);
	}
	
	
	public void writeMaptoFile(boolean CG_Done){

		try {
			String path;
			FileOutputStream fos;
			ObjectOutputStream oos;

			//for writing the contours map
			if(!CG_Done)
				path = new String (apktPath+"offlinePB/Contours.map");
			else
				path = new String (apktPath+"offlinePB/Red_Contours.map");
			
			fos = new FileOutputStream (path);
			oos = new ObjectOutputStream(fos);
			//TODO: Need to change it
			//oos.writeObject(new onlineLocationsMap(ContourPointsMap));
			
			oos.flush();
			oos.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			System.exit(0);
	}
	
	public int findSeedLocation(double seedCost) throws IOException, PicassoException  
	{
		location leftInfo, rightInfo, midInfo;
		int leftcorner = 0;
		int[] left = createCorner(leftcorner);

		int rightcorner = 1;
		int[] right = createCorner(rightcorner);

		/*
		 * The following If condition checks whether any earlier point in all_contour_points 
		 * had the same plan. If so, no need to open the .../predicateOrder/plan.txt again
		 */

		leftInfo = locationAlreadyExist(left);
		
		if(leftInfo == null){
			leftInfo = new location(convertIndextoSelectivity(left),this);
			if(trie)
				addLocationtoGraph(leftInfo);
			opt_calls++;
		}
		double leftCost = leftInfo.get_cost();
		
		
		rightInfo = locationAlreadyExist(right);
		
		if(rightInfo == null){
			rightInfo = new location(convertIndextoSelectivity(right),this);
			if(trie)
				addLocationtoGraph(rightInfo);
			opt_calls++;
		}
		double rightCost = rightInfo.get_cost(); 
		
		int d=dimension-1;
				
		while(rightCost < seedCost && d>0)  
		{
			copyLoc(left, right);
			leftCost = rightCost;
			right[--d] = resolution-1;

			rightInfo = locationAlreadyExist(right);			
			if(rightInfo == null){
				rightInfo = new location(convertIndextoSelectivity(right),this);
				if(trie)
					addLocationtoGraph(rightInfo);
				opt_calls++;
			}
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
				midInfo = locationAlreadyExist(mid);
				
				if(midInfo == null){
					midInfo = new location(convertIndextoSelectivity(mid),this);
					if(trie)
						addLocationtoGraph(midInfo);
					opt_calls++;
				}
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
		if(!ContourLocationAlreadyExist(rightInfo.dim_values))	
			all_contour_points.add(rightInfo);
		
		//isContourPoint[getIndex(rightInfo.dim_values, resolution)] = true;		

		return getIndex(right, resolution);
	}
	
	public  void nexusAlgoContour(double searchCost) throws IOException, PicassoException 
	{
		int seed = 0;
	
		seed = findSeedLocation(searchCost);
		
		exploreSeed(seed, searchCost, dimension-1, dimension-2);
	}
	
	public void exploreSeed(int cur_seed, double search_cost, int focus_dim, int swap_dim) throws IOException, PicassoException{
		int[] seed_coords;
		int[] candidate1_coords;
		int[] candidate2_coords;
		int iteration_no=0;
		while(cur_seed >= 0 && focus_dim > 0) 
		{	
			//System.out.println("Iteration number is "+iteration_no++);
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
				location seedInfo;
				
				seedInfo = locationAlreadyExist(candidate2_coords);
				if(seedInfo == null){
					seedInfo =  new location(convertIndextoSelectivity(candidate2_coords),this);
					if(trie)
						addLocationtoGraph(seedInfo);
					opt_calls++;
					explore_seed_count ++;
				}
				int newSeed = getIndex(convertSelectivitytoIndex(seedInfo.dim_values),resolution);

				if( seedInfo.get_cost() < search_cost )
					return;
				else
				{
					if(!ContourLocationAlreadyExist(seedInfo.dim_values))
						all_contour_points.add(seedInfo);
					if(swap_dim > 0)
					{
						exploreSeed(newSeed, search_cost, focus_dim-1,swap_dim-1);					
					}
				}
				return;	
			}

			// get cost of candidate locations
			location cand1Info, cand2Info;
			cand1Info = locationAlreadyExist(candidate1_coords);
			if(cand1Info == null){
				cand1Info = new location(convertIndextoSelectivity(candidate1_coords), this);
				if(trie)
					addLocationtoGraph(cand1Info);
				opt_calls ++;
				explore_seed_count ++;
			}
			
			cand2Info = locationAlreadyExist(candidate2_coords);
			if(cand2Info == null){
				cand2Info = new location(convertIndextoSelectivity(candidate2_coords),this);
				if(trie)
					addLocationtoGraph(cand2Info);
				opt_calls ++;
				explore_seed_count ++;
			}
			
			location seedInfo;

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

			if(!ContourLocationAlreadyExist(seedInfo.dim_values))
				all_contour_points.add(seedInfo);

			cur_seed = getIndex(convertSelectivitytoIndex(seedInfo.dim_values), resolution);

			if(swap_dim > 0)
			{
				exploreSeed(getIndex(convertSelectivitytoIndex(seedInfo.dim_values),resolution), search_cost, focus_dim-1, swap_dim-1);				
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

	private boolean ContourLocationAlreadyExist(float[] arr) {
		//TODO: need to test this
		boolean flag = false;
		for(location loc: all_contour_points){
			flag = true;
			for(int i=0;i<dimension;i++){
				if(loc.get_dimension(i)!= arr[i]){
					flag = false;
					break;
				}
			}
			if(flag==true)
				return true;
		}
		
		return false;
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



	
	private void replaceUnWantedPlans() {

		ArrayList<Integer> unWantedPlans = new ArrayList<Integer>();
		ArrayList<Integer> badLocations = new ArrayList<Integer>();
		unWantedPlans.add(0);
		unWantedPlans.add(3);
		unWantedPlans.add(8);
		unWantedPlans.add(11);
		unWantedPlans.add(12);
		double worstRatio = Double.MIN_VALUE;
		int worstLoc=-1,worstBestOtherPlan = -1;
		int count=0;
		for(int loc=0; loc < totalPoints; loc++)
		{
			int bestPlan = getPCSTOptimalPlan(loc);
			if(unWantedPlans.contains(bestPlan)){
				int otherBestPlan = getPCSTBestOtherPlan(bestPlan, loc);
				double a = AllPlanCosts[otherBestPlan][loc]/getOptimalCost(loc);
				if(worstRatio<a){
					worstRatio = a;
					worstBestOtherPlan = otherBestPlan;
					worstLoc = loc;
				}
				if(a>(double)1.1)
					badLocations.add(loc);
				if(true){
					boolean flag = true;
					int int_array[] = new int[dimension];
					int_array = getCoordinates(dimension, resolution, loc);
					for(int i=0;i<dimension;i++)
						if(int_array[i]==0)
							flag = false;
					//if(flag)
						count++;
				}
			}
					
		}
		
		System.out.println("The worst ratio is "+worstRatio+" with loc "+worstLoc+" and best other plan "+worstBestOtherPlan+" best plan "+ getPCSTOptimalPlan(worstLoc));
		
	}


	private void sortContourPoints(int contour_no) {

		 String funName  = "sortContourPoints";
		 
		 Collections.sort(ContourPointsMap.get(contour_no), new locationComparator());
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
	 
	 private int factorial(int num) {

		 int factorial = 1;
		for(int i=num;i>=1;i--){
			factorial *= i; 
		}
		return factorial;
	}
	
	 

public  void getPlanCountArray() {
	planCount = new double[totalPlans];
	locationWeight = new float[data.length];
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

		if(resolution==9 ){
			if(sel_distribution==0){
				//for tpch
				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 3;
				locationWeightLocal[3] = 5;         locationWeightLocal[4] = 10;				locationWeightLocal[5] = 15;
				locationWeightLocal[6] = 15;        locationWeightLocal[7] = 20;				locationWeightLocal[8] = 30;
				
			}

			//for tpcds
			if(sel_distribution == 1){
				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 3;
				locationWeightLocal[3] = 5;         locationWeightLocal[4] = 10;				locationWeightLocal[5] = 15;
				locationWeightLocal[6] = 15;        locationWeightLocal[7] = 20;				locationWeightLocal[8] = 30;
				
			}
		}
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

				locationWeight[loc] = (float) weight;
				planCount[data[loc].getPlanNumber()] += weight;
			}
			else
				locationWeight[loc] = (float) -1;
			
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
		}
		
		assert (locationWeight[i]<= (double)1) : "In getPlanCountArray: locationWeight is not less than 1";
//		if(locationWeight[i]>(double)1){
//			System.out.println("In getPlanCountArray: locationWeight is not less than 1");
//			System.out.println("location weight : "+locationWeight[i]+" at i="+i+" total weight="+totalWeight);
//		}
	}

	System.out.println("The sum weight is "+sumWeight);
	assert(sumWeight<=(double)1.01) : "In getPlanCountArray: sumWeight is not less than 1";
	

	/*
	 * if(scaleupflag) { for(int i = 0; i < planCount.length; i++)
	 * planCount[i] /= 100Math.pow(10, getDimension()); }
	 */


	checkValidityofWeights();

}
	 
	 
	public void planBouquetAlgo(int contour_no, double cost) {

		String funName = "planBouquetAlgo";
		
		double last_exec_cost = 0;
		learning_cost =0;
		int [] arr = new int[dimension];
		HashSet<Integer> unique_plans = new HashSet();
		int unique_points =0;
		double max_cost =0 , min_cost = Double.MAX_VALUE;
		
		for(int c=0;c< ContourPointsMap.get(contour_no).size();c++){
			
			location p = ContourPointsMap.get(contour_no).get(c);
			
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
				if(p.get_dimension(d) < findNearestPoint(actual_sel[d]) ){
					flag = false;
					break;
				}
			}
			
			
			
			for(int d=0;d<dimension;d++){
				arr[d] = findNearestPoint(p.get_dimension(d));
				//System.out.print(arr[d]+",");
			}
			
			if(!unique_plans.contains(getPlanNumber_generic(arr))){
				
				//if(cost_generic(convertSelectivitytoIndex(actual_sel)) < 2*cost)
				if(true)
					learning_cost += minContourCostMap.get(contour_no);
				else 
					learning_cost += cost;
				
				//Settings: learning_cost += p.get_cost();  changed to include only the contour cost and not the point
				unique_plans.add(getPlanNumber_generic(arr));
				//last_exec_cost = cost;
//				if(cost_generic(convertSelectivitytoIndex(actual_sel)) < 2*cost)
				if(true)
					last_exec_cost = minContourCostMap.get(contour_no);
				else 
					last_exec_cost = cost;
			}
			if(flag == true){
				if(cost_generic(convertSelectivitytoIndex(actual_sel)) >= 2*cost)
					flag = false;
			}
			
			if(!flag && cost_generic(convertSelectivitytoIndex(actual_sel)) < 2*cost)
				flag = checkFPC(p.get_plan_no(),contour_no);
			
			//Major change in code: please check once before running
//			if(cost_generic(convertSelectivitytoIndex(actual_sel)) > cost && getOptimalCost(totalPoints-1)>=cost_generic(convertSelectivitytoIndex(actual_sel)) )
//				flag = false;
//			
			if(flag){
				done = true;
				 System.out.println("The number unique points are "+unique_points);
				 System.out.println("The number unique plans are "+unique_plans.size());
				 System.out.println("The  unique plans are "+unique_plans);
				 //System.out.print("The final execution cost is "+p.get_cost()+ "at :" );
				 if(!uniquePlansMap.containsKey(contour_no))
					 uniquePlansMap.put(contour_no, unique_plans.size());
				 else if(uniquePlansMap.get(contour_no)<unique_plans.size() && uniquePlansMap.containsKey(contour_no)){
					 uniquePlansMap.remove(contour_no);
					 uniquePlansMap.put(contour_no, unique_plans.size());
				 }
				//Settings:  changed to include only the contour cost and not the point
//				 if(p.get_cost() > last_exec_cost ){
//					 learning_cost -= last_exec_cost;
//					 learning_cost += p.get_cost();
//				 }
				 learning_cost -= last_exec_cost;
				 int [] int_actual_sel = new int[dimension];
				 for(int d=0;d<dimension;d++)
					 int_actual_sel[d] = findNearestPoint(actual_sel[d]);
				 double oneDimCost=0;
//				 if(cost_generic(convertSelectivitytoIndex(actual_sel)) < 2*cost)
				 if(true)
						oneDimCost += minContourCostMap.get(contour_no);
					else 
						oneDimCost += cost;
				 
				 if(fpc_cost_generic(int_actual_sel, p.get_plan_no())<oneDimCost)
					 oneDimCost = fpc_cost_generic(int_actual_sel, p.get_plan_no());
				 if(cost_generic(int_actual_sel)> oneDimCost)
					 oneDimCost = cost_generic(int_actual_sel);
				 learning_cost  += oneDimCost;
	 
				 for(int d=0;d<dimension;d++){
						System.out.print(arr[d]+",");
					}
				System.out.println();
				//assert (unique_points <= Math.pow(resolution, dimension-1)) : funName+" : total points is execeeding the max possible points";
//				if(cost_generic(convertSelectivitytoIndex(actual_sel)) < 2*cost)
				if(true)
					assert (learning_cost <= (unique_plans.size()+1)*minContourCostMap.get(contour_no)*1.01) : funName+" : learning cost exceeding its cap";
				else 
					assert (learning_cost <= (unique_plans.size()+1)*cost*1.01) : funName+" : learning cost exceeding its cap";
				
				return;
			}
		}
		 //assert (unique_points <= Math.pow(resolution, dimension-1)) : funName+" : total points is execeeding the max possible points";
		
		if(!uniquePlansMap.containsKey(contour_no))
			 uniquePlansMap.put(contour_no, unique_plans.size());
		 else if(uniquePlansMap.get(contour_no)<unique_plans.size() && uniquePlansMap.containsKey(contour_no)){
			 uniquePlansMap.remove(contour_no);
			 uniquePlansMap.put(contour_no, unique_plans.size());
		 }
		
		 System.out.println("The number of unique points are "+unique_points);
		 System.out.println("The number of unique plans are "+unique_plans.size());
		 System.out.println("The  unique plans are "+unique_plans);
		 System.out.println("Contour No. is "+contour_no+" : Max cost is "+max_cost+" and min cost is "+min_cost+" with learning cost "+learning_cost);
//		 if(cost_generic(convertSelectivitytoIndex(actual_sel)) < 2*cost)
		 if(true)
				assert (learning_cost <= (unique_plans.size()+1)*minContourCostMap.get(contour_no)*1.01) : funName+" : learning cost exceeding its cap";
			else 
				assert (learning_cost <= (unique_plans.size()+1)*cost*1.01) : funName+" : learning cost exceeding its cap";

	}
	

	private boolean checkFPC(int plan_no, int contour_no) {
		
		int last_contour = (int)(Math.ceil(Math.log(cost_generic(convertSelectivitytoIndex(actual_sel))/OptimalCost[0])/Math.log(2)));
		last_contour++;
		double cost_q_a = cost_generic(convertSelectivitytoIndex(actual_sel));
		double budget = Math.pow(2,last_contour-1)*OptimalCost[0];
		if(budget>OptimalCost[totalPoints-1])
			budget = OptimalCost[totalPoints-1];
		if(last_contour==contour_no && cost_q_a!=budget){
			if(cost_q_a<=OptimalCost[totalPoints-1])
				assert(budget>cost_q_a):" last contour cost is less than actual selectivity cost";
			if(fpc_cost_generic(convertSelectivitytoIndex(actual_sel), plan_no)<budget){
				return true;
			}
			else
				return false;
		}
		else 
			return false;
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
	
private float[] convertIndextoSelectivity(int[] point_Index) {
	
	String funName = "convertIndextoSelectivity";
	
	float [] point_selec = new float[point_Index.length];
	assert (point_Index.length == dimension): funName+" ERROR: point index length not matching with dimension"; 
	for(int d=0; d<dimension;d++)
		point_selec[d] = selectivity[point_Index[d]];
	return point_selec;
}

public int[] convertSelectivitytoIndex (float[] point_sel) {
	
	String funName = "convertSelectivitytoIndex";
	
	int [] point_index = new int[point_sel.length];
	assert (point_sel.length == dimension): funName+" ERROR: point index length not matching with dimension"; 
	for(int d=0; d<dimension;d++)
		point_index[d] = findNearestPoint(point_sel[d]);
	return point_index;
}


public void initialize(int location) {

	String funName = "intialize";

	learning_cost = 0;
	done = false;
	//updating the actual selectivities for each of the dimensions
	int index[] = getCoordinates(dimension, resolution, location);
	
	actual_sel = new float[dimension];
	for(int i=0;i<dimension;i++){
		actual_sel[i] = selectivity[index[i]];
	}	
	
}

	private static void writeContourPointstoFile(int contour_no) {

		try {
	    
//	    String content = "This is the content to write into file";


         File file = new File("/home/srinivas/spillBound/data/others/contours/"+qtName+contour_no+".txt"); 
           
	    // if file doesn't exists, then create it
	    if (!file.exists()) {
	        file.createNewFile();
	    }
	    

	    FileWriter writer = new FileWriter(file, false);

	    
	    PrintWriter pw = new PrintWriter(writer);
	    //Take iterator over the list
	    for(location p : all_contour_points) {		 
	   	 pw.print(p.get_dimension(0) + "\t"+p.get_dimension(1)+"\n");
	    }
	    pw.close();
//	    pwaz.close();
	    writer.close();
	    
		} catch (IOException e) {
	    e.printStackTrace();
	}
		
	}

	
	private location planVisited(int plan_no) {

		String funName = "planVisited";
		
		for(location p: all_contour_points){
				if(p.get_plan_no()== plan_no){
					return p;
				}
		}
		return null;
	}

	private location locationAlreadyExist(int[] arr) {
		//TODO: need to test this
		if(!memoization)
			return null;

		if(trie)
			return searchLocationInGraph(arr);
		
		boolean flag = false;
		for(int c = 1; c<=ContourPointsMap.keySet().size(); c++){
			for(location loc: ContourPointsMap.get(c)){
				flag = true;
				for(int i=0;i<dimension;i++){
					if(Math.abs(loc.get_dimension(i) - selectivity[arr[i]]) > 0.00001){
						flag = false;
						break;
					}
				}
				if(flag==true) {
					location_hits ++;
					return loc;
				}
			}
		}
		return null;
}
	

	void addLocationtoGraph(location loc){

		//System.out.println("["+min_cc_sel_idx+"-"+max_cc_sel_idx+"]");
		online_vertex crawl = root;
		
		// Traverse through all characters of given word
		for( int level = 0; level < dimension; level++)
		{
			TreeMap<Integer, online_vertex> child = crawl.getChildern();

			int val = findNearestPoint(loc.dim_values[level]);

			// If there is no such child for current character of given word
			if( (child == null) || !child.containsKey(val)){ // create a child
				
				online_vertex temp;

				if(level == dimension - 1) //then this is a leaf node
                    temp = new online_vertex_leaf(val,  loc);
                else
                    temp = new online_vertex(val);
				
				if(child == null)
					child = new TreeMap<Integer,online_vertex>();
				
				child.put( val, temp );
				
				if(crawl.getChildern() == null)
					crawl.setChildern(child);
				
				crawl = temp;
			}
			else   
			{	
				crawl = child.get(val);
			}
		}
	}
	
	location searchLocationInGraph(int [] arr){

		long startTime = System.nanoTime();
		long endTime;
		assert(arr.length == dimension) : "location dim values not matching";
		
		online_vertex crawl;
		
		crawl = root;

//		if((Math.abs(arr[0] - 0.004033) < 0.000001f) && (Math.abs(arr[1] - 3.77E-4) < 0.000001f) && (Math.abs(arr[2] - 1.0) < 0.00001f)  )
//			System.out.println("Option = "+option+" [ "+min_cc_sel_idx+"-"+max_cc_sel_idx+"]"+arr[0]+","+arr[1]+","+arr[2]+" searching location from graph");

		// Traverse through all characters of given word
		for( int level = 0; level < dimension; level++)
		{
			if(crawl == null)
				System.out.println("null crawl");
			TreeMap<Integer,online_vertex> child = crawl.getChildern();
			
			int val = arr[level];
			
			if(child == null){
				endTime = System.nanoTime();
				return null;
			}
			
			if( child.containsKey(val))
				crawl = child.get(val);
			else   // Else create a child
			{
				endTime = System.nanoTime();
				return null;
			}

//			for(int itr = val; itr <=val; itr++){
//				// If there is already a child for current character of given word
//				if( child.containsKey(itr)){
//					crawl = child.get(itr);
//					break;
//				}
//				else   
//				{
//					if(itr == val+1){
//						endTime = System.nanoTime();
//						location_finding_time += (float)((endTime*1.0f - startTime*1.0f)/1000000000f);
//						return null;
//					}
//				}
//			}
	}
		online_vertex_leaf crawl_leaf = (online_vertex_leaf) crawl;
		if(crawl_leaf.loc == null){
			System.out.println("the crawl location is null");
		}
		assert(crawl_leaf.loc != null): "crawl.loc is null which should not be the case";
		endTime = System.nanoTime();
		location_hits ++;
		return crawl_leaf.loc;
	}
	
	
	// Function which does binary search to find the actual point !!
// Return the index near to the selecitivity=mid;
	public int findNearestPoint(float mid)
	{
		int i;
		int return_index = 0;
		float diff;
		if(mid >= selectivity[resolution-1])
			return resolution-1;
		for(i=0;i<resolution;i++)
		{
			diff = mid - selectivity[i];
			if(diff <= 0.00000001f)
			{
				return_index = i;
				break;
			}
		}
		//System.out.println("return_index="+return_index);
		return return_index;
	}
	

	// Return the selectivity just greater than selecitivity=mid;
		public float findNearestSelectivity(float mid)
		{
			int i;
			int return_index = 0;
			double diff;
			if(mid >= selectivity[resolution-1])
				return selectivity[resolution-1];
			for(i=0;i<resolution;i++)
			{
				diff = mid - selectivity[i];
				if(diff <= 0.00000001f)
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
			
			if(loadAllPlanCosts){
				// ------------------------------------- Read pcst files
				nPlans = totalPlans;
				AllPlanCosts = new double[nPlans][totalPoints];
				//costBouquet = new double[total_points];
				for (int i = 0; i < nPlans; i++) {
					try {

						ObjectInputStream ip = new ObjectInputStream(new FileInputStream(new File(apktPath +"pcstFiles/"+ i + ".pcst")));
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

	 
	 void loadSelectivity_nexus_bcg() {
		 
				String funName = "loadSelectivity_nexus_bcg: ";
				beta = (float)Math.pow(alpha,(1.0 / dimension*1.0));
				float var = minimum_selectivity;
				ArrayList<Float> sel_arr = new ArrayList<Float>();
				boolean flag = false;
				int step =0;
				while(true) {
					flag = false;
					step ++;
					if(var >= 1.0f) {
						var = 1.0f;
						sel_arr.add(var);
						flag = true;
					}
					if(flag)
						break;
					sel_arr.add(var);
					opb.decimalPrecision = decimalPrecision;
					var = opb.roundToDouble(var * beta);
				}
				
				assert(step == sel_arr.size()) : "not matching array sizes";
				resolution = step;
				System.out.println(funName+" Resolution = "+resolution);
				
				selectivity = new float [resolution];
				
				for(int i =0; i < sel_arr.size(); i++)
						selectivity[i] = opb.roundToDouble(sel_arr.get(i));		                              
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
			this.selectivity = new float [resolution];
			if(resolution == 9){
				if(sel_distribution == 0){
					
					//This is for TPCH queries 
					selectivity[0] = 0.0006f;	selectivity[1] = 0.003f;selectivity[2] = 0.0084f;	selectivity[3] = 0.02f;
					selectivity[4] = 0.04f;		selectivity[5] = 0.82f;	selectivity[6] = 0.17f;	selectivity[7] = 0.33f;
					selectivity[8] = 0.66f;		                              // oct - 2012
				}
				else if( sel_distribution ==1){
					
					//This is for TPCDS queries
					selectivity[0] = 0.0006f;	selectivity[1] = 0.003f;selectivity[2] = 0.0084f;	selectivity[3] = 0.02f;
					selectivity[4] = 0.04f;		selectivity[5] = 0.82f;	selectivity[6] = 0.17f;	selectivity[7] = 0.33f;
					selectivity[8] =  0.66f;	                         // dec - 2012
				}
				else
					assert (false) : "should not come here";

			}
			
			if(resolution == 10){
				if(sel_distribution == 0){
					
					//This is for TPCH queries 
					selectivity[0] = 0.0005f;	selectivity[1] = 0.005f;selectivity[2] = 0.01f;	selectivity[3] = 0.02f;
					selectivity[4] = 0.05f;		selectivity[5] = 0.10f;	selectivity[6] = 0.20f;	selectivity[7] = 0.40f;
					selectivity[8] = 0.60f;		selectivity[9] = 0.95f;                               // oct - 2012
				}
				else if( sel_distribution ==1){
					
					//This is for TPCDS queries
					selectivity[0] = 0.00005f;	selectivity[1] = 0.0005f;selectivity[2] = 0.005f;	selectivity[3] = 0.02f;
					selectivity[4] = 0.05f;		selectivity[5] = 0.10f;	selectivity[6] = 0.15f;	selectivity[7] = 0.25f;
					selectivity[8] = 0.50f;		selectivity[9] = 0.99f;                            // dec - 2012
				}
				else
					assert (false) : "should not come here";

			}		


			if(resolution == 20){
				if(sel_distribution == 0){

					selectivity[0] = 0.0005f;   selectivity[1] = 0.0008f;		selectivity[2] = 0.001f;	selectivity[3] = 0.002f;
					selectivity[4] = 0.004f;   selectivity[5] = 0.006f;		selectivity[6] = 0.008f;	selectivity[7] = 0.01f;
					selectivity[8] = 0.03f;	selectivity[9] = 0.05f;	selectivity[10] = 0.08f;	selectivity[11] = 0.10f;
					selectivity[12] = 0.200f;	selectivity[13] = 0.300f;	selectivity[14] = 0.400f;	selectivity[15] = 0.500f;
					selectivity[16] = 0.600f;	selectivity[17] = 0.700f;	selectivity[18] = 0.800f;	selectivity[19] = 0.99f;
				}
				else if( sel_distribution ==1){

					selectivity[0] = 0.00005f;   selectivity[1] = 0.00008f;		selectivity[2] = 0.0001f;	selectivity[3] = 0.0002f;
					selectivity[4] = 0.0004f;   selectivity[5] = 0.0006f;		selectivity[6] = 0.0008f;	selectivity[7] = 0.001f;
					selectivity[8] = 0.003f;	selectivity[9] = 0.005f;	selectivity[10] = 0.008f;	selectivity[11] = 0.01f;
					selectivity[12] = 0.05f;	selectivity[13] = 0.1f;	selectivity[14] = 0.15f;	selectivity[15] = 0.25f;
					selectivity[16] = 0.40f;	selectivity[17] = 0.60f;	selectivity[18] = 0.80f;	selectivity[19] = 0.99f;
				}
				else
					assert (false) :funName+ "ERROR: should not come here";
			}

			if(resolution == 30){
				if(sel_distribution == 0){
				//tpch
					selectivity[0] = 0.0005f;  selectivity[1] = 0.0008f;	selectivity[2] = 0.001f;	selectivity[3] = 0.002f;
					selectivity[4] = 0.004f;   selectivity[5] = 0.006f;	selectivity[6] = 0.008f;	selectivity[7] = 0.01f;
					selectivity[8] = 0.03f;	selectivity[9] = 0.05f;
					selectivity[10] = 0.07f;	selectivity[11] = 0.1f;	selectivity[12] = 0.15f;	selectivity[13] = 0.20f;
					selectivity[14] = 0.25f;	selectivity[15] = 0.30f;	selectivity[16] = 0.35f;	selectivity[17] = 0.40f;
					selectivity[18] = 0.45f;	selectivity[19] = 0.50f;	selectivity[20] = 0.55f;	selectivity[21] = 0.60f;
					selectivity[22] = 0.65f;	selectivity[23] = 0.70f;	selectivity[24] = 0.75f;	selectivity[25] = 0.80f;
					selectivity[26] = 0.85f;	selectivity[27] = 0.90f;	selectivity[28] = 0.95f;	selectivity[29] = 0.99f;
				}
				
				else if(sel_distribution == 1){
					selectivity[0] = 0.00001f;  selectivity[1] = 0.00005f;	selectivity[2] = 0.00010f;	selectivity[3] = 0.00050f;
					selectivity[4] = 0.0010f;   selectivity[5] = 0.005f;	selectivity[6] = 0.0100f;	selectivity[7] = 0.0200f;
					selectivity[8] = 0.0300f;	selectivity[9] = 0.0400f;	selectivity[10] = 0.0500f;	selectivity[11] = 0.0600f;
					selectivity[12] = 0.0700f;	selectivity[13] = 0.0800f;	selectivity[14] = 0.0900f;	selectivity[15] = 0.1000f;
					selectivity[16] = 0.1200f;	selectivity[17] = 0.1400f;	selectivity[18] = 0.1600f;	selectivity[19] = 0.1800f;
					selectivity[20] = 0.2000f;	selectivity[21] = 0.2500f;	selectivity[22] = 0.3000f;	selectivity[23] = 0.4000f;
					selectivity[24] = 0.5000f;	selectivity[25] = 0.6000f;	selectivity[26] = 0.7000f;	selectivity[27] = 0.8000f;
					selectivity[28] = 0.9000f;	selectivity[29] = 0.9950f;
				}
				
				else
					assert (false) :funName+ "ERROR: should not come here";
			}
			if (resolution ==40){
				if(sel_distribution==1){
					
					selectivity[0] = 0.00005f;   selectivity[1] = 0.00006f;		selectivity[2] = 0.00008f;	selectivity[3] = 0.00009f;
					selectivity[4] = 0.0001f;   selectivity[5] = 0.0002f;		selectivity[6] = 0.0003f;	selectivity[7] = 0.0005f;
					selectivity[8] = 0.0006f;	selectivity[9] = 0.0007f;	selectivity[10] = 0.0008f;	selectivity[11] = 0.0009f;
					selectivity[12] = 0.001f;	selectivity[13] = 0.002f;	selectivity[14] = 0.003f;	selectivity[15] = 0.004f;
					selectivity[16] = 0.005f;	selectivity[17] = 0.006f;	selectivity[18] = 0.007f;	selectivity[19] = 0.008f;
					selectivity[20] = 0.009f;   selectivity[21] = 0.01f;		selectivity[22] = 0.02f;	selectivity[23] = 0.03f;
					selectivity[24] = 0.04f;   selectivity[25] = 0.05f;		selectivity[26] = 0.06f;	selectivity[27] = 0.07f;
					selectivity[28] = 0.08f;	selectivity[29] = 0.09f;	selectivity[30] = 0.1f;	selectivity[31] = 0.2f;
					selectivity[32] = 0.3f;	selectivity[33] = 0.4f;	selectivity[34] = 0.5f;	selectivity[35] = 0.6f;
					selectivity[36] = 0.7f;	selectivity[37] = 0.8f;	selectivity[38] = 0.9f;	selectivity[39] = 0.99f;
				}
				else
					assert (false) :funName+ "ERROR: should not come here";
			}
			
			if(resolution==100){
				if(sel_distribution == 1){
					
					//used for SPILLBOUND
//					selectivity[0] = 0.000064f; 	selectivity[1] = 0.000093f; 	selectivity[2] = 0.000126f; 	selectivity[3] = 0.000161f; 	selectivity[4] = 0.000198f;
//					selectivity[5] = 0.000239f; 	selectivity[6] = 0.000284f; 	selectivity[7] = 0.000332f; 	selectivity[8] = 0.000384f; 	selectivity[9] = 0.000440f;
//					selectivity[10] = 0.000501f; 	selectivity[11] = 0.000567f; 	selectivity[12] = 0.000638f; 	selectivity[13] = 0.000716f; 	selectivity[14] = 0.000800f;
//					selectivity[15] = 0.000890f; 	selectivity[16] = 0.000989f; 	selectivity[17] = 0.001095f; 	selectivity[18] = 0.001211f; 	selectivity[19] = 0.001335f;
//					selectivity[20] = 0.001471f; 	selectivity[21] = 0.001617f; 	selectivity[22] = 0.001776f; 	selectivity[23] = 0.001948f; 	selectivity[24] = 0.002134f;
//					selectivity[25] = 0.002335f; 	selectivity[26] = 0.002554f; 	selectivity[27] = 0.002790f; 	selectivity[28] = 0.003046f; 	selectivity[29] = 0.003323f;
//					selectivity[30] = 0.003624f; 	selectivity[31] = 0.003949f; 	selectivity[32] = 0.004301f; 	selectivity[33] = 0.004683f; 	selectivity[34] = 0.005096f;
//					selectivity[35] = 0.005543f; 	selectivity[36] = 0.006028f; 	selectivity[37] = 0.006552f; 	selectivity[38] = 0.007121f; 	selectivity[39] = 0.007736f;
//					selectivity[40] = 0.008403f; 	selectivity[41] = 0.009125f; 	selectivity[42] = 0.009907f; 	selectivity[43] = 0.010753f; 	selectivity[44] = 0.011670f;
//					selectivity[45] = 0.012663f; 	selectivity[46] = 0.013739f; 	selectivity[47] = 0.014904f; 	selectivity[48] = 0.016165f; 	selectivity[49] = 0.017531f;
//					selectivity[50] = 0.019011f; 	selectivity[51] = 0.020613f; 	selectivity[52] = 0.022348f; 	selectivity[53] = 0.024228f; 	selectivity[54] = 0.026263f;
//					selectivity[55] = 0.028467f; 	selectivity[56] = 0.030854f; 	selectivity[57] = 0.033440f; 	selectivity[58] = 0.036240f; 	selectivity[59] = 0.039272f;
//					selectivity[60] = 0.042556f; 	selectivity[61] = 0.046113f; 	selectivity[62] = 0.049965f; 	selectivity[63] = 0.054136f; 	selectivity[64] = 0.058654f;
//					selectivity[65] = 0.063547f; 	selectivity[66] = 0.068845f; 	selectivity[67] = 0.074584f; 	selectivity[68] = 0.080799f; 	selectivity[69] = 0.087530f;
//					selectivity[70] = 0.094819f; 	selectivity[71] = 0.102714f; 	selectivity[72] = 0.111263f; 	selectivity[73] = 0.120523f; 	selectivity[74] = 0.130550f;
//					selectivity[75] = 0.141411f; 	selectivity[76] = 0.153172f; 	selectivity[77] = 0.165910f; 	selectivity[78] = 0.179705f; 	selectivity[79] = 0.194645f;
//					selectivity[80] = 0.210825f; 	selectivity[81] = 0.228348f; 	selectivity[82] = 0.247325f; 	selectivity[83] = 0.267877f; 	selectivity[84] = 0.290136f;
//					selectivity[85] = 0.314241f; 	selectivity[86] = 0.340348f; 	selectivity[87] = 0.368621f; 	selectivity[88] = 0.399241f; 	selectivity[89] = 0.432403f;
//					selectivity[90] = 0.468316f; 	selectivity[91] = 0.507211f; 	selectivity[92] = 0.549334f; 	selectivity[93] = 0.594953f; 	selectivity[94] = 0.644359f;
//					selectivity[95] = 0.697865f; 	selectivity[96] = 0.755812f; 	selectivity[97] = 0.818569f; 	selectivity[98] = 0.886535f; 	selectivity[99] = 0.990142f;
					
					//for OnlinePB
					selectivity[0] = 0.00010073f; 	selectivity[1] = 0.00030557f; 	selectivity[2] = 0.00051594f; 	selectivity[3] = 0.00073200f; 	selectivity[4] = 0.00095388f; 	
					selectivity[5] = 0.0118176f; 	selectivity[6] = 0.00141579f; 	selectivity[7] = 0.00165613f; 	selectivity[8] = 0.00190297f; 	selectivity[9] = 0.00215647f; 	
					selectivity[10] = 0.0241682f; 	selectivity[11] = 0.00268420f; 	selectivity[12] = 0.00295879f; 	selectivity[13] = 0.00324080f; 	selectivity[14] = 0.00353042f; 	
					selectivity[15] = 0.0382787f; 	selectivity[16] = 0.00413334f; 	selectivity[17] = 0.00444706f; 	selectivity[18] = 0.00476926f; 	selectivity[19] = 0.00510015f; 	
					selectivity[20] = 0.0543997f; 	selectivity[21] = 0.00578898f; 	selectivity[22] = 0.00614740f; 	selectivity[23] = 0.00651550f; 	selectivity[24] = 0.00689354f; 	
					selectivity[25] = 0.0728179f; 	selectivity[26] = 0.00768052f; 	selectivity[27] = 0.00809002f; 	selectivity[28] = 0.00851057f; 	selectivity[29] = 0.00894247f; 	
					selectivity[30] = 0.0938604f; 	selectivity[31] = 0.00984159f; 	selectivity[32] = 0.01030943f; 	selectivity[33] = 0.01078991f; 	selectivity[34] = 0.01128336f; 	
					selectivity[35] = 0.1179013f; 	selectivity[36] = 0.01231059f; 	selectivity[37] = 0.01284509f; 	selectivity[38] = 0.01339403f; 	selectivity[39] = 0.01395779f; 	
					selectivity[40] = 0.1453678f; 	selectivity[41] = 0.01513139f; 	selectivity[42] = 0.01574206f; 	selectivity[43] = 0.01636922f; 	selectivity[44] = 0.01701331f; 	
					selectivity[45] = 0.1767479f; 	selectivity[46] = 0.01835413f; 	selectivity[47] = 0.01905182f; 	selectivity[48] = 0.01976834f; 	selectivity[49] = 0.02050420f; 	
					selectivity[50] = 0.2125994f; 	selectivity[51] = 0.02203608f; 	selectivity[52] = 0.02283317f; 	selectivity[53] = 0.02365179f; 	selectivity[54] = 0.02449251f; 	
					selectivity[55] = 0.2535593f; 	selectivity[56] = 0.02624266f; 	selectivity[57] = 0.02715334f; 	selectivity[58] = 0.02808860f; 	selectivity[59] = 0.02904911f; 	
					selectivity[60] = 0.3003556f; 	selectivity[61] = 0.03104864f; 	selectivity[62] = 0.03208908f; 	selectivity[63] = 0.03315761f; 	selectivity[64] = 0.03425498f; 	
					selectivity[65] = 0.3538199f; 	selectivity[66] = 0.03653942f; 	selectivity[67] = 0.03772811f; 	selectivity[68] = 0.03894889f; 	selectivity[69] = 0.04020263f; 	
					selectivity[70] = 0.4149023f; 	selectivity[71] = 0.04281258f; 	selectivity[72] = 0.04417065f; 	selectivity[73] = 0.14556538f; 	selectivity[74] = 0.24699776f; 	
					selectivity[75] = 0.4846882f; 	selectivity[76] = 0.34997960f; 	selectivity[77] = 0.45153117f; 	selectivity[78] = 0.5312464f; 	selectivity[79] = 0.5476113f; 	
					selectivity[80] = 0.5644180f; 	selectivity[81] = 0.5816785f; 	selectivity[82] = 0.5994050f; 	selectivity[83] = 0.6176102f; 	selectivity[84] = 0.6363069f; 	
					selectivity[85] = 0.6555084f; 	selectivity[86] = 0.6752283f; 	selectivity[87] = 0.6954807f; 	selectivity[88] = 0.7162799f; 	selectivity[89] = 0.7376407f; 	
					selectivity[90] = 0.7595782f; 	selectivity[91] = 0.7821080f; 	selectivity[92] = 0.8052461f; 	selectivity[93] = 0.8290090f; 	selectivity[94] = 0.8534135f; 	
					selectivity[95] = 0.8784769f; 	selectivity[96] = 0.9042169f; 	selectivity[97] = 0.9306520f; 	selectivity[98] = 0.9578008f; 	selectivity[99] = 0.9856827f; 	

				}
				else if(sel_distribution == 0){
					selectivity[0] = 0.005995f; 	selectivity[1] = 0.015985f; 	selectivity[2] = 0.025975f; 	selectivity[3] = 0.035965f; 	selectivity[4] = 0.045955f; 	
					selectivity[5] = 0.055945f; 	selectivity[6] = 0.065935f; 	selectivity[7] = 0.075925f; 	selectivity[8] = 0.085915f; 	selectivity[9] = 0.095905f; 	
					selectivity[10] = 0.105895f; 	selectivity[11] = 0.115885f; 	selectivity[12] = 0.125875f; 	selectivity[13] = 0.135865f; 	selectivity[14] = 0.145855f; 	
					selectivity[15] = 0.155845f; 	selectivity[16] = 0.165835f; 	selectivity[17] = 0.175825f; 	selectivity[18] = 0.185815f; 	selectivity[19] = 0.195805f; 	
					selectivity[20] = 0.205795f; 	selectivity[21] = 0.215785f; 	selectivity[22] = 0.225775f; 	selectivity[23] = 0.235765f; 	selectivity[24] = 0.245755f; 	
					selectivity[25] = 0.255745f; 	selectivity[26] = 0.265735f; 	selectivity[27] = 0.275725f; 	selectivity[28] = 0.285715f; 	selectivity[29] = 0.295705f; 	
					selectivity[30] = 0.305695f; 	selectivity[31] = 0.315685f; 	selectivity[32] = 0.325675f; 	selectivity[33] = 0.335665f; 	selectivity[34] = 0.345655f; 	
					selectivity[35] = 0.355645f; 	selectivity[36] = 0.365635f; 	selectivity[37] = 0.375625f; 	selectivity[38] = 0.385615f; 	selectivity[39] = 0.395605f; 	
					selectivity[40] = 0.405595f; 	selectivity[41] = 0.415585f; 	selectivity[42] = 0.425575f; 	selectivity[43] = 0.435565f; 	selectivity[44] = 0.445555f; 	
					selectivity[45] = 0.455545f; 	selectivity[46] = 0.465535f; 	selectivity[47] = 0.475525f; 	selectivity[48] = 0.485515f; 	selectivity[49] = 0.495505f; 	
					selectivity[50] = 0.505495f; 	selectivity[51] = 0.515485f; 	selectivity[52] = 0.525475f; 	selectivity[53] = 0.535465f; 	selectivity[54] = 0.545455f; 	
					selectivity[55] = 0.555445f; 	selectivity[56] = 0.565435f; 	selectivity[57] = 0.575425f; 	selectivity[58] = 0.585415f; 	selectivity[59] = 0.595405f; 	
					selectivity[60] = 0.605395f; 	selectivity[61] = 0.615385f; 	selectivity[62] = 0.625375f; 	selectivity[63] = 0.635365f; 	selectivity[64] = 0.645355f; 	
					selectivity[65] = 0.655345f; 	selectivity[66] = 0.665335f; 	selectivity[67] = 0.675325f; 	selectivity[68] = 0.685315f; 	selectivity[69] = 0.695305f; 	
					selectivity[70] = 0.705295f; 	selectivity[71] = 0.715285f; 	selectivity[72] = 0.725275f; 	selectivity[73] = 0.735265f; 	selectivity[74] = 0.745255f; 	
					selectivity[75] = 0.755245f; 	selectivity[76] = 0.765235f; 	selectivity[77] = 0.775225f; 	selectivity[78] = 0.785215f; 	selectivity[79] = 0.795205f; 	
					selectivity[80] = 0.805195f; 	selectivity[81] = 0.815185f; 	selectivity[82] = 0.825175f; 	selectivity[83] = 0.835165f; 	selectivity[84] = 0.845155f; 	
					selectivity[85] = 0.855145f; 	selectivity[86] = 0.865135f; 	selectivity[87] = 0.875125f; 	selectivity[88] = 0.885115f; 	selectivity[89] = 0.895105f; 	
					selectivity[90] = 0.905095f; 	selectivity[91] = 0.915085f; 	selectivity[92] = 0.925075f; 	selectivity[93] = 0.935065f; 	selectivity[94] = 0.945055f; 	
					selectivity[95] = 0.955045f; 	selectivity[96] = 0.965035f; 	selectivity[97] = 0.975025f; 	selectivity[98] = 0.985015f; 	selectivity[99] = 0.995005f;
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
	
	
	private void ContourCentricCostGreedy(int contour_no) throws SQLException
	{
		ArrayList<location> contour_locs = new ArrayList<location>();
		
		if(contour_no == -1){
			//get all the location of all the contours
			
			for(int i=1; i<=ContourPointsMap.size();i++) {
					if(ContourPointsMap.containsKey(i))
				contour_locs.addAll(ContourPointsMap.get(i));
					
					//intensionally not trying to cover non_contour points because 
					//1. costgreedy is taking time as no. of fpc calls are |contour_locs|*|plans|
					//2. |contour_locs| drastically if it is just covering contour
					//3. quality of reduction also might improve
					
//					if(non_ContourPointsMap.containsKey(i))
//				contour_locs.addAll(non_ContourPointsMap.get(i));
			}
		}
		else{
			contour_locs.addAll(ContourPointsMap.get(contour_no));
//			contour_locs.addAll(non_ContourPointsMap.get(contour_no));
		}
		
		int[] index = new int[dimension];
		
		//get the POSP count for the query
		int originalPlanCnt = new File(apktPath+"planStructureXML").listFiles().length ;
		int plansArray[] = new int[originalPlanCnt];
		HashSet<Integer> chosenPlanSet = new HashSet<Integer>();
		int remainingSpace = 0;

		Iterator iter;
		int ix;

		System.out.println(" < contour plans reduction > ");

		ix = 0;
		while(ix < originalPlanCnt)
		{
			plansArray[ix] = ix;
			ix++;
		}

		iter = contour_locs.iterator();
		while(iter.hasNext())
		{
			location loc = (location)iter.next();
			loc.is_within_threshold = new boolean[originalPlanCnt];
			loc.reduced_planNumber = -1;
			remainingSpace++;
		}

		String newQuery;
		Plan plan = null;
		int countCoverageLocations[];
		int total = remainingSpace;int outerForLoopCnt =0;
		int maxCoverage = -1;
		iter = contour_locs.iterator();
		
		while(remainingSpace > 0)
		{
			countCoverageLocations = new int[originalPlanCnt];

			outerForLoopCnt++;
			
			iter = contour_locs.iterator();
			while(iter.hasNext())
			{	
				location objContourPt = (location) iter.next();
				if(objContourPt.reduced_planNumber != -1)
					continue;
				
//				conn = source.getConnection();
				for (int i=0; i<originalPlanCnt; i++)
				{
					int cnt = 0;
					
					assert(objContourPt.reduced_planNumber == -1) : "should not come here";
						
					
					double foreign_cost = fpc_cost_generic(convertSelectivitytoIndex(objContourPt.dim_values), i);
					
					assert(foreign_cost > 0): "getFPCParallel is not working: less than zero cost";
					
//					double cst = getFPCCost(objContourPt.dim_values, i);
//					assert(Math.abs(foreign_cost - cst) < 0.000001 ) : "getFPCParallel is not working: not same cost: foreign_cost = "+foreign_cost+" cst = "+cst;

					if(foreign_cost < (1 + (lambda/100.0)) * objContourPt.opt_cost)
					{
						objContourPt.is_within_threshold[i] = true;
						countCoverageLocations[i] += 1;
						if(maxCoverage < countCoverageLocations[i])
						{
							maxCoverage = countCoverageLocations[i];
							if(contour_no!=-1) {
								double perc = (((double)maxCoverage/(double)total)*100.0);
							}
						}
					}
					
					//objContourPt.fpc_cost = Double.MIN_VALUE;
				}
//				 if (conn != null) {
//						try { conn.close(); } catch (SQLException e) {}
//					}
				
				
			}

			//System.out.println("Finished step 1");
			//2.find the plan that covers max area
			maxCoverage = 0;
			int maxCoveragePlan = -1;
			int maxCoveragePlanIndex = -1;
			
			for (int i = 0; i < originalPlanCnt; i++)
			{
				if(maxCoverage < countCoverageLocations[i])
				{
					maxCoverage = countCoverageLocations[i];
					maxCoveragePlanIndex = i;
					maxCoveragePlan = plansArray[i];
				}
			}
			if(maxCoveragePlan == -1)
				break;

			//3.remember the plan that covers max area & update the remaining space
			chosenPlanSet.add(maxCoveragePlan);
			remainingSpace -= countCoverageLocations[maxCoveragePlanIndex];			


			//4. update reduced plan # of all locations where picked plan is lambda-optimal
			iter = contour_locs.iterator();
			while(iter.hasNext())
			{
				location objContourPt = (location)iter.next();
				if(objContourPt.is_within_threshold[maxCoveragePlanIndex] == true)
				{
					objContourPt.reduced_planNumber = (short) maxCoveragePlan;	
				}
			}
		}
		
			System.out.println("After Reduction:");
		
			HashMap<Integer,HashSet<Integer>> contourPlansReduced = new HashMap<Integer,HashSet<Integer>>();
		// update what plans are in which contour
			HashSet<Integer> reducedPlansSet = new HashSet<Integer>();
			
			if(contour_no == -1){
				for (int k = 1; k <= ContourPointsMap.size(); k++) {
					reducedPlansSet.clear();
					iter = ContourPointsMap.get(k).iterator();
					while (iter.hasNext()) {
						location objContourPt = (location) iter.next();
						if (!reducedPlansSet.contains(objContourPt.reduced_planNumber)) {
							reducedPlansSet.add((int) objContourPt.reduced_planNumber);
						}
					}

					//				@SuppressWarnings("rawtypes")
					//				Iterator it = reducedPlansSet.iterator();
					//				while (it.hasNext()) {
					//					int p = (Short) it.next();
					//					contourPlansReduced.get(k).add(p);
					//				}
					contourPlansReduced.put(k, reducedPlansSet);

					System.out.println("Contour"+k+" = "+reducedPlansSet.size());
				}
			}
			else{				
					reducedPlansSet.clear();
					iter = ContourPointsMap.get(contour_no).iterator();
					while (iter.hasNext()) {
						location objContourPt = (location) iter.next();
						if (!reducedPlansSet.contains(objContourPt.reduced_planNumber)) {
							reducedPlansSet.add((int) objContourPt.reduced_planNumber);
						}
					}
					contourPlansReduced.put(contour_no, reducedPlansSet);
					System.out.println("Contour"+contour_no+" = "+reducedPlansSet.size());
			}
			
			//following snippet required for making sure that certain static variables are fine.
			location loc  = new location();
			loc.apktPath = apktPath;
			loc.select_query = this.select_query;
			loc.predicates = this.predicates;
			loc.dimension = this.dimension;

//			if(writeMapstoFile)
//				writeMaptoFile(true);
			
			
			Iterator itr = ContourPointsMap.keySet().iterator();
			while(itr.hasNext()){
				Integer key = (Integer)itr.next();
				for(int pt =0;pt<ContourPointsMap.get(key).size();pt++){
					location p = ContourPointsMap.get(key).get(pt);
					if(p.get_contour_no() != key.intValue())
						//System.out.println("problem");
					assert(p.get_contour_no() == key.intValue()) : "not matching contour no. with contourmap key value with contour no.= "+p.get_contour_no()+" key = "+key.intValue();
				}
			}
			
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
				if(newData[i].getCost() <= data[i].getCost()){
					newData[i].setCost(data[i].getCost()*1.01);
					/*
					 * We are adding 1% more to the cost of the replacement plan, in case 
					 * when the replacement plan has lesser cost than the original plan
					 */
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
			if(! (Math.abs(c1 - c2) < 0.05*c1 || Math.abs(c1 - c2) < 0.05*c2) ){
			//if((c2-c1) > 1*c1){
				//int [] ind = getCoordinates(dimension, resolution, i);
				//System.out.printf("\nFPC ERROR: Plan: %4d, Loc(%3d, %3d,%3d): , pktCost: %10.1f, fpcOptCost: %10.1f, error: %4.2f", p, ind[0], ind[1],ind[2],c1, c2, (double)Math.abs(c1 - c2)*100/c1);
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
		int [] count = new int[6];
		long  [] avg_cost = new long [6];
		long  [] avg_sub_opt = new long [6];
		int low_cost_count = 0;
		int condition[] = {10, 100,1000, 10000, 100000};
		for(int loc=0; loc < totalPoints; loc++) {
			newOptimalPlan[loc] = getPCSTOptimalPlan(loc);
		}
		
		int worstPlan[] = new int[totalPoints];
		for(int loc=0; loc < totalPoints; loc++) {
	
			worstPlan[loc] = getPCSTWorstPlan(loc);
//;			worstPlan[loc] = 13;
		}
		//calculate really optimal plan at each location in the space -- because FPC costs may be different from the optimal costs
		
		double MSO = -1.0;
		double a;
		int location=0;
		for(int loc=0; loc < totalPoints; loc++)
		{
			a = AllPlanCosts[worstPlan[loc]][loc]/Math.max(1, getOptimalCost(loc));
			/*
			 * TODO: Have used a sanity constant as 1 in the earlier line. 
			 * Assuming none of the plan cost less than 1
			 */
			
			if(MSO < a)
			{
				MSO = a;
				location = loc;
			}
			
			//if(loc==13625)
			//	System.out.println(" The suboptimality at this interseting location is"+a);
			// to get the histogram of suboptimalities
			/*
			if(getOptimalCost(loc)<(double)10000)
				low_cost_count++;
			else if(a >=1 && a<condition[0]){
				count[0]++;
				avg_cost[0] = avg_cost[0] + (long)getOptimalCost(loc);
				avg_sub_opt[0] = avg_sub_opt[0] + (long) a;
			}
			else if(a >=condition[0] && a<condition[1]){
				count[1]++;
				avg_cost[1] = avg_cost[1]+ (long) getOptimalCost(loc);
				avg_sub_opt[1] = avg_sub_opt[1] + (long) a;
			}
			else if(a >=condition[1] && a<condition[2]){
				count[2]++;
				avg_cost[2] = avg_cost[2] + (long)getOptimalCost(loc);
				avg_sub_opt[2] = avg_sub_opt[2] + (long)a;
			}
			else if(a >=condition[2] && a<condition[3]){
				count[3]++;
				avg_cost[3] = avg_cost[3] + (long)getOptimalCost(loc);
				avg_sub_opt[3] = avg_sub_opt[3] + (long)a;
			}
			else if(a >=condition[3] && a<condition[4]){
				count[4]++;
				avg_cost[4] = avg_cost[4] + (long)getOptimalCost(loc);
				avg_sub_opt[4] = avg_sub_opt[4] + (long)a;
			}
			else{
				count[5]++;
				avg_cost[5] = avg_cost[5] + (long)getOptimalCost(loc);
				avg_sub_opt[5] = avg_sub_opt[5] + (long)a;
			}
			*/
		}
		System.out.println("\n Sumit MSO = "+MSO);
		printLocation(location);
		System.out.println("\n loc:"+location+"\n Worst Value="+AllPlanCosts[worstPlan[location]][location]);
		System.out.println("\nOptimal_cost :"+getOptimalCost(location)+"with least cost = "+getOptimalCost(0)+"\n");
//		System.out.println("\nHistogram of suboptimality distribution :\n");
//		System.out.println("\nLow Cost Count is :\t"+low_cost_count);
//		for(int i=0;i<6;i++){
//			System.out.print("\n"+count[i]);
//			System.out.print("\tAvg Cost is :\t"+avg_cost[i]/count[i]);
//			System.out.print("\tAvg SubOpt is :\t"+avg_sub_opt[i]/count[i]);
//		}
		System.exit(0);
		
	}
  
	private void printLocation(int location) {
		int [] arr = getCoordinates(dimension, resolution, location);
		for(int d=0; d<dimension; d++){
			System.out.print(arr[d]+"\t");
		}
		
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
	
	private int getPCSTBestOtherPlan(int plan,int loc) {
		
		double bestCost = Double.MAX_VALUE;
		int opt = -1;
		for(int p=0; p<nPlans; p++){
			if(bestCost > AllPlanCosts[p][loc] && p!=plan) {
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
			cardinalityPath = prop.getProperty("cardinalityPath");
			sel_distribution = Integer.parseInt(prop.getProperty("sel_distribution"));
			dimension =  Integer.parseInt(prop.getProperty("dimension"));
			resolution = Integer.parseInt(prop.getProperty("resolution"));
			select_query = prop.getProperty("select_query");
			predicates = prop.getProperty("predicates");
			totalPoints = (int) Math.pow(resolution, dimension);
			
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}


 
}



