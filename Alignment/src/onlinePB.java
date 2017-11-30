import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;
import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.server.plan.Node;
import iisc.dsl.picasso.server.plan.Plan;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigDecimal;
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
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.postgresql.jdbc3.Jdbc3PoolingDataSource;


public class onlinePB {
	



	//parameters that share variables across objects
	static int plans[];
	static double OptimalCost[];
	static int totalPlans;
	static int dimension;
	static int resolution;
	static int totalPoints;
	static DataValues[] data;
	static float selectivity[];
	static String apktPath;
	static String qtName ;
	static String select_query;
	static String predicates;
	static float beta;
	static int database_conn=1;
	static int sel_distribution = 1;
	static File f_marwa;
	static float h_cost;
	static float min_cost;
	//for plan bouquet
	static float learning_cost_pb = 0;
	static boolean done_pb = false;
	static float[] actual_sel_pb; 
	static int num_of_usable_threads;
	HashMap<Short,Integer> uniquePlansMap = new HashMap<Short,Integer>();
	HashMap<Long,Short> planHashMap = new HashMap<Long,Short>();
	static HashMap<Short,Integer> plans_dist = new HashMap<Short,Integer>();
	static 	HashMap<Short,ArrayList<location>> ContourPointsMap = new HashMap<Short,ArrayList<location>>();
	static HashMap<Short,ArrayList<location>> non_ContourPointsMap = new HashMap<Short,ArrayList<location>>();
	static ArrayList<Float> outermost_dimension_sel;
	
	//parameters to set
	//static float minimum_selectivity = 0.00005f;
	static float minimum_selectivity = 0.0001f;
	//static float minimum_selectivity = 0.001f;
	static float alpha = 2;
	static float lambda = 20;
	static int decimalPrecision = 6;
	static boolean DEBUG_LEVEL_2 = false;
	static boolean DEBUG_LEVEL_1 = false;
	static boolean visualisation_2D = false;
	static boolean enhancement = true; //toggling between last_dim1 and last_dim2
	static boolean enhancement_end_contour = false; //if you are enabling this add opt++ in code
	static boolean memoization = true;
	static float cost_error = 0.12f;
	static float float_error ;
	static boolean contoursReadFromFile = false;
	static boolean cg_contoursReadFromFile = false;
	static boolean writeMapstoFile = false;
	static boolean singleThread = false;
	static boolean trie = true;

	static Jdbc3PoolingDataSource source;
	Connection conn = null;
	ArrayList<Integer> learntDim = new ArrayList<Integer>();
	float qrun_sel[];
	float qrun_sel_rev[];
	
	//----------------used for profiling code------------------------//
	int opt_call = 0;
	int fpc_call = 0;
	int location_hits = 0;
	float slope_time =0f;
	float location_finding_time = 0f;
	float opt_plan_path_time = 0f;
	int forward_jumps = 0;
	int backward_jumps = 0;
	int jump_size_10 = 0;
	int jump_size_1 = 0;
	int fpc_slope_count =0;
	int forward_jumps_zero_level =0;
	//----------------used for profiling code------------------------//
	
	//the following two are required for min. and max. selectivity to varying across outermost dimension
	int min_cc_sel_idx = 0;//comes into picture during parallelization 
	int max_cc_sel_idx ;
	ArrayList<location> contour_points = new ArrayList<location>();
	ArrayList<location> non_contour_points = new ArrayList<location>();
	static online_vertex map_root;
	online_vertex contour_root;
	online_vertex non_contour_root;
	
	public static void main(String[] args) throws IOException, SQLException, PicassoException, ClassNotFoundException {
	
		int threads = (int) ( Runtime.getRuntime().availableProcessors()*1);
		num_of_usable_threads = threads;
		//set the program arguments
		if(args.length > 0){
			alpha = Float.parseFloat(args[0]);
			System.out.println("Alpha = "+alpha);
			//writeMapstoFile = false;
		}
		
		float_error = (float)Math.pow(10, -1*decimalPrecision);
		long startTime = System.nanoTime();
		onlinePB obj = new onlinePB();  
		obj.loadPropertiesFile();
		String pktPath = apktPath + qtName + ".apkt" ;
		String pktPath_new = apktPath + qtName + "_new9.4.apkt";
		System.out.println("Query Template is "+qtName);
		minimum_selectivity = obj.roundToDouble(minimum_selectivity);
		
		ADiagramPacket gdp = obj.getGDP(new File(pktPath_new));
		dimension = gdp.getDimension();
		resolution = gdp.getMaxResolution();
		totalPoints = (int) Math.pow(resolution, dimension);
	
		
		obj.readpkt(gdp, true);
		obj.loadPropertiesFile();
				
		try{
			System.out.println("entered DB conn1");
			source = new Jdbc3PoolingDataSource();
			source.setDataSourceName("A Data Source");
			f_marwa = new File("/home/dsladmin/marwa");
			
			//Settings
			//System.out.println("entered DB conn2");
			if(database_conn==0){
//				conn = DriverManager
//						.getConnection("jdbc:postgresql://localhost:5431/tpch-ai",
//								"sa", "database");
			}
			else{
			
				if(f_marwa.exists() && !f_marwa.isDirectory()) { 
					System.out.println("entered DB tpcds");
//					conn = DriverManager
//							.getConnection("jdbc:postgresql://localhost:5431/tpcds-ai",
//									"sa", "database");
					source.setServerName("localhost:5431");
					source.setDatabaseName("tpcds-ai");
				}
				else{
				System.out.println("entered DB tpcds");
//				conn = DriverManager
//						.getConnection("jdbc:postgresql://localhost:5432/tpcds-ai",
//								"sa", "database");
				source.setServerName("localhost:5432");
				source.setDatabaseName("tpcds-ai");
				}
			}
			source.setUser("sa");
			source.setPassword("database");
			
			if(singleThread)
				source.setMaxConnections(1);
			else
				source.setMaxConnections(num_of_usable_threads);
			
			obj.conn = source.getConnection();
			System.out.println("Opened database successfully");
		}
		catch ( Exception e ) {
			System.out.println("entered DB err");
			System.err.println( e.getClass().getName()+": "+ e.getMessage() );
		}
		
		//obj.FPCMemoryLeaks();
		
		location loc = new location();
		loc.dimension = obj.dimension;
		loc.select_query = new String(obj.select_query);
		loc.predicates  = new String(obj.predicates);
		loc.database_conn = obj.database_conn;
		loc.apktPath = obj.apktPath;
		loc.decimalPrecision = obj.decimalPrecision;

		
		
		File ContoursFile = new File(apktPath+"online_contours/Contours.map");

		if(contoursReadFromFile && ContoursFile.exists()){
			obj.readContourPointsFromFile(false);
		}
		else{
			//generating the contours contourwise
			int i;


			if(visualisation_2D){
				obj.dimension = 2;

				float [] l_loc_arr = {minimum_selectivity,minimum_selectivity};
				location l_loc = new location(l_loc_arr,obj);
				min_cost = l_loc.get_cost();

				float [] h_loc_arr = {1.0f,1.0f};
				location h_loc = new location(h_loc_arr,obj);
				h_cost = h_loc.get_cost();


			}
			else{
				
				obj.qrun_sel = new float[dimension];
				for(int d=0;d<dimension;d++)
					obj.qrun_sel[d] = 1.0f;
				location loc_terminus = new location(obj.qrun_sel,obj);
				h_cost = loc_terminus.get_cost(); 
				//obj.contour_points.add(loc_terminus);
				
				for(int d=0;d<dimension;d++)
					obj.qrun_sel[d] = minimum_selectivity;
				min_cost = obj.getFPCCost(obj.qrun_sel, -1, null);
			}


			obj.qrun_sel = new float[dimension];
			obj.qrun_sel_rev = new float[dimension];
			for(int d=0;d<dimension;d++){
				obj.qrun_sel[d] = -1.0f;
				obj.qrun_sel_rev[d] = -1.0f;
			}

			beta = (float)Math.pow(alpha,(1.0 / dimension*1.0));
			float cost = min_cost;
			float ratio = h_cost/min_cost;
			assert (h_cost >= min_cost) : "maximum cost is less than the minimum cost";
			System.out.println("the ratio of C_max/c_min is "+ratio);


			/*set the selectivity array for the outermost dimension*/
			int step =0;
			outermost_dimension_sel = new ArrayList<Float>();
			float var = minimum_selectivity;
			boolean flag = false;
			while(true) {
				flag = false;
				step ++;
				if(var >= 1.0f) {
					var = 1.0f;
					outermost_dimension_sel.add(var);
					flag = true;
				}
				if(flag)
					break;
				outermost_dimension_sel.add(var);
				var = obj.roundToDouble(var * beta);
			}
			
			obj.max_cc_sel_idx = outermost_dimension_sel.size() - 1 ; 
			assert(obj.max_cc_sel_idx > 0): "outermostdimension is empty";

			//add the root vertex to graph which is used for membership testing of locations
			map_root = new online_vertex(-1);
			obj.contour_root = new online_vertex(-1);
			obj.non_contour_root = new online_vertex(-1);
			
			//clear the contents of onlinePB Planstructure directory
			File dir = new File(apktPath+"onlinePB/planStructureXML/");
			for(File file: dir.listFiles())  
			        file.delete();
			
			dir = new File(apktPath+"onlinePB/subContourPlans/");
				obj.recursiveDelete(dir);
			
			//System.exit(0);
			//reset the values to -1 for the rest of the code 
			for(int d=0;d<dimension;d++)
				obj.qrun_sel[d] = -1.0f;

			i = 1;

			ArrayList<Integer> order = new ArrayList<Integer>();
			order.clear(); 
			for(int d=0;d<obj.dimension;d++)
				order.add(d);
			
			//close the connection since the multithreaded version is starting
			if(!singleThread){
				if (obj.conn != null) {
					try { obj.conn.close(); } catch (SQLException e) {}
				}
			}

			obj.contour_points = new ArrayList<location>();			
			obj.non_contour_points = new ArrayList<location>();

			while(cost < 2*h_cost)
			{
				
				if(cost<(double)9000){
					cost *= 2;
					i++;
					continue;
				}
				
				if(cost>h_cost)
					cost = h_cost;
				System.out.println("---------------------------------------------------------------------------------------------\n");
				System.out.println("Contour "+i+" cost : "+cost);

				obj.contour_points.clear();	
				obj.non_contour_points.clear();

//							if(i > 2){
//								i++;
//								cost *=2;
//								continue;
//							}

				System.gc();
				obj.restartDatabase();
				if(!singleThread){
					if (obj.conn != null) {
						try { obj.conn.close(); } catch (SQLException e) {}
					}
				}
				
				
				if(cost < h_cost){
					if(singleThread)
						obj.generateCoveringContours(order,cost);
					else
						obj.getCoveringContoursParallel(order, cost);
				}
				else {
					//just add the terminus location to the contour
					if(!singleThread){
						obj.conn = source.getConnection();
					}
					for(int d=0;d<dimension;d++)
						obj.qrun_sel[d] = 1.0f;
					location loc_terminus = new location(obj.qrun_sel,obj);
					obj.contour_points.add(loc_terminus);
					if (obj.conn != null && !singleThread) {
						try { obj.conn.close(); } catch (SQLException e) {}
					}
				}
				
				
					//if(writeMapstoFile)
						obj.writeContourPointstoFile(i);
				System.out.println("The running optimization calls are "+obj.opt_call);
				System.out.println("The running forward jump are "+obj.forward_jumps);
				System.out.println("The running location hits are "+obj.location_hits);
				System.out.println("The running FPC calls are "+obj.fpc_call);
				
				if(!singleThread){
					obj.conn = source.getConnection();
				}
				
				for(location l: obj.contour_points){
					l.set_contour_no((short)i);
					if(l.get_plan_no() == -1){
						l.setPlanNumber(obj.conn);
					}
					if(trie)
						obj.addLocationtoGraph(l,0);
				}
				
				
				for(location l: obj.non_contour_points){
					if(l.get_plan_no() == -1){
						l.setPlanNumber(obj.conn);
					}
					if(trie)
						obj.addLocationtoGraph(l,0);
				}

				if (obj.conn != null && !singleThread) {
					try { obj.conn.close(); } catch (SQLException e) {}
				}
//				for(location l: non_contour_points)
//					l.set_contour_no(i);
				
				int size_of_contour = obj.contour_points.size();
				int size_of_non_contour = obj.non_contour_points.size();
				//ContourPointsMap.put((short)i, new ArrayList<location>(obj.contour_points)); //storing the contour points

				//non_ContourPointsMap.put((short)i, new ArrayList<location>(obj.non_contour_points)); //storing the contour points

				System.out.println("Size of contour: "+size_of_contour );
				System.out.println("Size of non-contour: "+size_of_non_contour );

				cost *=2;
				i++;
			}
			System.out.println("the number of optimization calls are "+obj.opt_call);
			System.out.println("the number of FPC calls are "+obj.fpc_call);
			System.out.println("location hits "+obj.location_hits);
			System.out.println("slope function time "+obj.slope_time);
			System.out.println("location finding time "+obj.location_finding_time);
			System.out.println("optimal plan path time "+obj.opt_plan_path_time);
			System.out.println("fpc slope count "+obj.fpc_slope_count);
			System.out.println("forward jumps count "+obj.forward_jumps);
			System.out.println("forward jumps count at zero level "+obj.forward_jumps_zero_level);
			System.out.println("backward jumps count "+obj.backward_jumps);
			System.out.println("jump size with 1% count "+obj.jump_size_1);
			System.out.println("jump size with 10% count "+obj.jump_size_10);
			
			long endTime = System.nanoTime();
			System.out.println("Took "+(endTime - startTime)/1000000000 + " sec");
			
			if(writeMapstoFile)
				obj.writeMaptoFile(false);
		}
		
		System.out.println("");
		if (obj.conn != null) {
			try { obj.conn.close(); } catch (SQLException e) {}
		}		
		
		obj.printPlanDistribution();
		System.exit(0);
		
		File fl_red =  new File(apktPath+"online_contours/Red_Contours.map");
		if(cg_contoursReadFromFile && fl_red.exists()){		
			
			obj.readContourPointsFromFile(true);
			
			Iterator itr = ContourPointsMap.keySet().iterator();
			while(itr.hasNext()){
				Short key = (Short)itr.next();
				for(int pt =0;pt<ContourPointsMap.get(key.shortValue()).size();pt++){
					location p = ContourPointsMap.get(key.shortValue()).get(pt);					
					assert(p.get_contour_no() == key.shortValue()) : "not matching contour no. with contourmap key value with contour no.= "+p.get_contour_no()+" key = "+key.intValue();
				}
			}
			
			
			//System.exit(0);
			
		}
		else
			obj.ContourCentricCostGreedy((short)-1);
		
		obj.conn = source.getConnection();
		obj.runPlanBouquetAlgo();
		if (obj.conn != null) {
			try { obj.conn.close(); } catch (SQLException e) {}
		}
		
		System.out.println("");
		
	}
	
	public void extractNonContourPointstoList(ArrayList<location> al, online_vertex node) {
		
		if(node.getChildern()!=null && node.getChildern().size() > 0) {
			Iterator valueSet =  node.getChildern().values().iterator();
			while(valueSet.hasNext()) {
				online_vertex temp = (online_vertex)valueSet.next();
				extractNonContourPointstoList(al,temp);
			}
		} else {
			if(node instanceof online_vertex_leaf) {
				online_vertex_leaf node_leaf = (online_vertex_leaf) node;
				al.add(node_leaf.loc);
			}
		}
		
		if(node.getChildern() != null)
			node.getChildern().clear();		
	}

	public  void FPCMemoryLeaks() throws SQLException {

		
		long startTime = System.nanoTime();
		long endTime;
		
		float arr[] = new float[dimension];
		for(int i=0; i< dimension; i++) {
			arr[i] = 0.5f;
		}
		for(int k=0; k < 10000000; k++) {
			if(k == 100000 || (k%1000000 == 0))
				System.out.println("Currently at "+k);

			getFPCCost(arr, 3, null);

			if(k == 50000) {
				String start = "/home/dsladmin/spillBound/postgresql-9.4.1/bin/pg_ctl -D /home/dsladmin/spillBound/tpch_9.4_DL_8.3/ -w start";
				String stop = "/home/dsladmin/spillBound/postgresql-9.4.1/bin/pg_ctl -D /home/dsladmin/spillBound/tpch_9.4_DL_8.3/ -w stop";
				String kill = "pkill -9 postgres";
				if (conn != null) {
					try { conn.close(); } catch (SQLException e) {}
				}

				try {
					Process p;
					Runtime r = Runtime.getRuntime();
					p = r.exec(kill);
					p.waitFor();
					p = r.exec(start);
					p.waitFor();
				}
				catch (Exception e) 
				{
					e.printStackTrace();
				}
				
				source.close();
				source = new Jdbc3PoolingDataSource();
				source.setDataSourceName("A Data Source");
				f_marwa = new File("/home/dsladmin/marwa");
				
				//Settings
				//System.out.println("entered DB conn2");
				if(database_conn==0){
//					conn = DriverManager
//							.getConnection("jdbc:postgresql://localhost:5431/tpch-ai",
//									"sa", "database");
				}
				else{
				
					if(f_marwa.exists() && !f_marwa.isDirectory()) { 
						System.out.println("entered DB tpcds");
//						conn = DriverManager
//								.getConnection("jdbc:postgresql://localhost:5431/tpcds-ai",
//										"sa", "database");
						source.setServerName("localhost:5431");
						source.setDatabaseName("tpcds-ai");
					}
					else{
					System.out.println("entered DB tpcds");
//					conn = DriverManager
//							.getConnection("jdbc:postgresql://localhost:5432/tpcds-ai",
//									"sa", "database");
					source.setServerName("localhost:5432");
					source.setDatabaseName("tpcds-ai");
					}
				}
				source.setUser("sa");
				source.setPassword("database");
				
				if(singleThread)
					source.setMaxConnections(1);
				else
					source.setMaxConnections(num_of_usable_threads);
				conn = source.getConnection();
			}
			
		}
		
		endTime = System.nanoTime();
		System.out.println((endTime*1.0f - startTime*1.0f)/1000000000f);
		System.exit(0);
	}

	public  void restartDatabase() throws SQLException {

		
		String start = "/home/dsladmin/spillBound/postgresql-9.4.1/bin/pg_ctl -D /home/dsladmin/spillBound/tpch_9.4_DL_8.3/ -w start";
		String stop = "/home/dsladmin/spillBound/postgresql-9.4.1/bin/pg_ctl -D /home/dsladmin/spillBound/tpch_9.4_DL_8.3/ -w stop";
		String kill = "sudo pkill -9 postgres";
		
		File f_marwa = new File("/home/dsladmin/marwa");
		if(f_marwa.exists()){
			start = "/home/dsladmin/Srinivas/postgresql-9.4.1/bin/pg_ctl -D /home/dsladmin/spillBound/tpch_9.4_DL_8.3/ -w start";
			stop = "/home/dsladmin/Srinivas/postgresql-9.4.1/bin/pg_ctl -D /home/dsladmin/spillBound/tpch_9.4_DL_8.3/ -w stop";
		}
		if (conn != null) {
			try { conn.close(); } catch (SQLException e) {}
		}

		try {
			Process p;
			Runtime r;
			while(checkIfPostgresServer()) {
			r= Runtime.getRuntime();
			p = r.exec(kill);
			p.waitFor();
			}
			while(!checkIfPostgresServer()) {
				r= Runtime.getRuntime();
				p = r.exec(start);
				p.waitFor();
				Thread.sleep(20 * 1000);
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		source.close();
		source = new Jdbc3PoolingDataSource();
		source.setDataSourceName("A Data Source");
		f_marwa = new File("/home/dsladmin/marwa");
		
		//Settings
		//System.out.println("entered DB conn2");
		if(database_conn==0){
//			conn = DriverManager
//					.getConnection("jdbc:postgresql://localhost:5431/tpch-ai",
//							"sa", "database");
		}
		else{
		
			if(f_marwa.exists() && !f_marwa.isDirectory()) { 
				System.out.println("entered DB tpcds");
//				conn = DriverManager
//						.getConnection("jdbc:postgresql://localhost:5431/tpcds-ai",
//								"sa", "database");
				source.setServerName("localhost:5431");
				source.setDatabaseName("tpcds-ai");
			}
			else{
			System.out.println("entered DB tpcds");
//			conn = DriverManager
//					.getConnection("jdbc:postgresql://localhost:5432/tpcds-ai",
//							"sa", "database");
			source.setServerName("localhost:5432");
			source.setDatabaseName("tpcds-ai");
			}
		}
		source.setUser("sa");
		source.setPassword("database");
		
		if(singleThread)
			source.setMaxConnections(1);
		else
			source.setMaxConnections(num_of_usable_threads);
		conn = source.getConnection();
	
	}

	public boolean checkIfPostgresServer() {
		
		String line;
	    try {
	      Process p = Runtime.getRuntime().exec("pidof postgres");
	      BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
	      while ((line = input.readLine()) != null)
	      {
	        //System.out.println(line);
	        if(line.split(" ").length >= 5)
	        	return true;
	        else
	        	return false;
	      }
	    } catch (Exception err) {
	      System.out.println(err);
	    }
	    return false;
	}
	
	
	public onlinePB(onlinePB obj){

		//to just make sure that static variables are already assigned values
		assert((dimension == obj.dimension) && (resolution == obj.resolution) && (Math.abs(beta - obj.beta) < float_error) && select_query.equals(obj.select_query) && (outermost_dimension_sel.size() == obj.outermost_dimension_sel.size()) ) : "static variables are not set at onlinePB obj initialization";
		this.conn = null;
		this.learntDim = new ArrayList<Integer>();
		this.qrun_sel = new float[dimension];
		this.qrun_sel_rev = new float[dimension];
		this.opt_call = 0;
		this.fpc_call = 0;
		
		//the following two are required for min. and max. selectivity to varying across outermost dimension
		this.min_cc_sel_idx = obj.min_cc_sel_idx;//comes into picture during parallelization 
		this.max_cc_sel_idx = obj.max_cc_sel_idx;	
	}
	
	public onlinePB() {
		
	}

	
	public void recursiveDelete(File file) {
        //to end the recursive loop
        if (!file.exists())
            return;
         
        //if directory, go inside and call recursively
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                //call recursively
                recursiveDelete(f);
            }
        }
        //call delete to delete files and empty directory
        file.delete();
//        System.out.println("Deleted file/folder: "+file.getAbsolutePath());
    }
	
	public void printPlanDistribution() {
		
		
		Iterator itr = ContourPointsMap.keySet().iterator();
		while(itr.hasNext()){
			Short key = (Short)itr.next();
			for(int pt =0;pt<ContourPointsMap.get(key.shortValue()).size();pt++){
				location p = ContourPointsMap.get(key.shortValue()).get(pt);
				Integer freq = plans_dist.get(p.get_plan_no());
				plans_dist.put(p.get_plan_no(), (freq == null ? 1 : freq + 1));
			}
		}
		
		itr = non_ContourPointsMap.keySet().iterator();
		while(itr.hasNext()){
			Short key = (Short)itr.next();
			for(int pt =0;pt<non_ContourPointsMap.get(key.shortValue()).size();pt++){
				location p = non_ContourPointsMap.get(key.shortValue()).get(pt);
				Integer freq = plans_dist.get(p.get_plan_no());
				plans_dist.put(p.get_plan_no(), (freq == null ? 1 : freq + 1));
			}
		}
		
		System.out.println("The plans distributions are");
		System.out.println(plans_dist);
		System.exit(0);
	}


	public void runPlanBouquetAlgo() throws SQLException {
		
		/*
		 * running the plan bouquet algorithm 
		 */
		double MSO =0, ASO = 0,SO=0,anshASO = 0;
		int ASO_points=0;
		int min_point =0;

		//getPlanCountArray();
		
		
		int max_point = totalPoints;
		float[] q_sel = new float[dimension];
		for(int d=0;d<dimension;d++)
			q_sel[d] = minimum_selectivity;
		min_cost = getFPCCost(q_sel, -1, null);
		
		for(int d=0;d<dimension;d++)
			q_sel[d] = 1.0f;
		h_cost = getFPCCost(q_sel, -1, null);
		
		double[] subOpt = new double[max_point];
		
		  for (int  j = min_point; j < max_point ; j++)
		  {
			System.out.println("Entering loop "+j);

//			if(j != 98378)
//				continue;
			//initialization for every loop
			
			float algo_cost = 0;
			SO =0;

			float cost = min_cost;
			initialize(j);
			int[] index = getCoordinates(dimension, resolution, j);
//			if(index[0]%5 !=0 || index[1]%5!=0)
//				continue;
//			obj.actual_sel_pb[0] = 0.31;obj.actual_sel_pb[1] = 0.3;obj.actual_sel_pb[2] = 0.6; /*uncomment for single execution*/
			
			// TODO: not required for(int d=0;d<dimension;d++) actual_sel_pb[d] = findNearestSelectivity(actual_sel_pb[d]);
			
			float cost_act_sel_pb = getFPCCost(actual_sel_pb, -1, null);
			if(cost_act_sel_pb < 10000f)
				continue;
			
			//----------------------------------------------------------
			int i = 1;
			while(i<=ContourPointsMap.size() && !done_pb)
			{	
				if(cost< 10000f){
					cost *= 2;
					i++;
					continue;
				}
				assert (cost<=2*h_cost) : "cost limit exceeding in loop = "+j;
				
				if(cost>h_cost)
					cost=h_cost;
				System.out.println("---------------------------------------------------------------------------------------------\n");
				System.out.println("Contour "+i+" cost : "+cost+"\n");
				
				run_PB_Algo_for_qa((short)i,cost,cost_act_sel_pb);
				
				algo_cost = algo_cost+ (learning_cost_pb);
				System.out.println("The current algo_cost is "+algo_cost);
				System.out.println("The cost expended in this contour is "+learning_cost_pb);
				cost = cost*2;  
				i = i+1;
				System.out.println("---------------------------------------------------------------------------------------------\n");

			}  //end of while
			
			assert(done_pb) : "In Main done_pb variable not true even when the while loop is broken out in Index = "+j;
			
			/*
			 * printing the actual selectivity
			 */
			System.out.print("\nThe actual selectivity is original \t");
			for(int d=0;d<dimension;d++) 
				System.out.print(actual_sel_pb[d]+",");
			
			//calculateMSOBound();
			/*
			 * storing the index of the actual selectivities. Using this printing the
			 * index (an approximation) of actual selectivities and its cost
			 */
			int [] index_actual_sel = new int[dimension]; 
			for(int d=0;d<dimension;d++) index_actual_sel[d] = findNearestPoint(actual_sel_pb[d]);
			
			System.out.print("\nCost of actual_sel_pb ="+cost_act_sel_pb+" at ");
			for(int d=0;d<dimension;d++) System.out.print(index_actual_sel[d]+",");

			SO = (algo_cost/cost_act_sel_pb);
			SO = SO * (1 + lambda/100);		
			subOpt[j] = SO;

			ASO += SO;
			ASO_points++;
			//anshASO += SO*locationWeight[j];
			if(SO>MSO)
				MSO = SO;
			System.out.println("\nOnline PB The SubOptimaility  is "+SO);
		  } //end of for
		  System.out.println("\nOnline PB MSO is "+MSO);

	}

	
	public void run_PB_Algo_for_qa(Short contour_no, float cost, float cost_act_sel_pb) {

		String funName = "planBouquetAlgo";
		
		float last_exec_cost = 0;
		learning_cost_pb =0;
		int [] arr = new int[dimension];
		HashSet<Short> unique_plans = new HashSet();
		int unique_points =0;
		float max_cost =0 , min_cost = Float.MAX_VALUE;
		
		if(cost > (2+cost_error)*cost_act_sel_pb) {
			//return if the cost of the contour is twice more than cost of actual selectivity
			done_pb = true;
			learning_cost_pb = 1;//to make sure none of the asserts fail
			return;
		}
		
		for(int c=0;c< ContourPointsMap.get(contour_no).size();c++){
			
			location p = ContourPointsMap.get(contour_no).get(c);
			
			/*needed for testing the code*/
			unique_points ++;
			if(p.get_cost() > max_cost)
				max_cost = p.get_cost();
			if(p.get_cost() < min_cost)
				min_cost = p.get_cost();
			
			/*
			 * to check if p dominates actual selectivity
			 */

			boolean flag = true;
			for(int d=0;d<dimension;d++){
				if(p.get_dimension(d) <= (actual_sel_pb[d]) ){
					flag = false;
					break;
				}
			}
			
			
			
			if(!unique_plans.contains(p.reduced_planNumber)){				
				learning_cost_pb += p.get_cost(); //TODO: see if its correct
				last_exec_cost = p.get_cost();
				unique_plans.add(p.reduced_planNumber);
			}
			
			if(flag == true){
				if(cost_act_sel_pb >= 4*cost)
					flag = false;
			}
			
//			if(!flag && cost_generic(convertSelectivitytoIndex(actual_sel_pb)) < 2*cost)
//				flag = checkFPC(p.get_plan_no(),contour_no);
			

			if(flag){
				done_pb = true;
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
//					 learning_cost_pb -= last_exec_cost;
//					 learning_cost_pb += p.get_cost();
//				 }
//				 learning_cost_pb -= last_exec_cost;
//				 int [] int_actual_sel = new int[dimension];
//				 for(int d=0;d<dimension;d++)
//					 int_actual_sel[d] = findNearestPoint(actual_sel_pb[d]);
//				 double oneDimCost=0;
////				 if(cost_generic(convertSelectivitytoIndex(actual_sel_pb)) < 2*cost)
//				 
//				 
//				 if(fpc_cost_generic(int_actual_sel, p.get_plan_no())<oneDimCost)
//					 oneDimCost = fpc_cost_generic(int_actual_sel, p.get_plan_no());
//				 if(cost_generic(int_actual_sel)> oneDimCost)
//					 oneDimCost = cost_generic(int_actual_sel);
//				 learning_cost_pb  += oneDimCost;
//	 
				
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
		 System.out.println("Contour No. is "+contour_no+" : Max cost is "+max_cost+" and min cost is "+min_cost+" with learning cost "+learning_cost_pb);
//		 if(cost_generic(convertSelectivitytoIndex(actual_sel_pb)) < 2*cost)

	}

//	private boolean checkFPC(int plan_no, int contour_no) {
//		
//		int last_contour = (int)(Math.ceil(Math.log(cost_generic(convertSelectivitytoIndex(actual_sel_pb))/OptimalCost[0])/Math.log(2)));
//		last_contour++;
//		double cost_q_a = cost_generic(convertSelectivitytoIndex(actual_sel_pb));
//		double budget = Math.pow(2,last_contour-1)*OptimalCost[0];
//		if(budget>OptimalCost[totalPoints-1])
//			budget = OptimalCost[totalPoints-1];
//		if(last_contour==contour_no && cost_q_a!=budget){
//			if(cost_q_a<=OptimalCost[totalPoints-1])
//				assert(budget>cost_q_a):" last contour cost is less than actual selectivity cost";
//			if(fpc_cost_generic(convertSelectivitytoIndex(actual_sel_pb), plan_no)<budget){
//				return true;
//			}
//			else
//				return false;
//		}
//		else 
//			return false;
//	}

	
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
				if(Math.abs(diff) <= 0.0000001)
				{
					return_index = i;
					break;
				}
			}
			//System.out.println("return_index="+return_index);
			return return_index;
		}

	
	public void calculateMSOBound() {

		 float algo_cost = 0;
		 double max_pb_so = Double.MIN_VALUE;
		 float cost = (float)OptimalCost[0];
		 int skip =0;
		 for(int i=1;i<=uniquePlansMap.size();i++){
			 if(cost< 10000f && !apktPath.contains("SQL")){
					cost *= 2;
					skip++;
					i--;
					continue;
				}
			 else if(cost> (float)OptimalCost[totalPoints-1])
				 cost = (float) OptimalCost[totalPoints-1];
			 algo_cost += Math.pow(2,i-1)*uniquePlansMap.get(i+skip);
			 if(i>1){
			 double so = algo_cost/Math.pow(2,i-2);
			 if(so > max_pb_so)
				 max_pb_so = so;
			 }
		 }
		 System.out.println("PB MSO is "+max_pb_so*(1+lambda/100));
	}

	
	public void initialize(int location) {

		String funName = "intialize";
		loadSelectivity();
		learning_cost_pb = 0;
		done_pb = false;
		//updating the actual selectivities for each of the dimensions
		int index[] = getCoordinates(dimension, resolution, location);
		
		actual_sel_pb = new float[dimension];
		for(int i=0;i<dimension;i++){
			actual_sel_pb[i] = selectivity[index[i]];
		}
	}


	public void readContourPointsFromFile(boolean CG_done) throws ClassNotFoundException {

		try {
			ObjectInputStream ip= null;
			onlineLocationsMap obj;
			
			if(!CG_done){
				ip = new ObjectInputStream(new FileInputStream(new File(apktPath +"online_contours/Contours.map")));
			}
			else{
				ip = new ObjectInputStream(new FileInputStream(new File(apktPath +"online_contours/Red_Contours.map")));
			}
			
			obj = (onlineLocationsMap)ip.readObject();
			ContourPointsMap = obj.getContourMap();
			Iterator itr = ContourPointsMap.keySet().iterator();
			while(itr.hasNext()){
				Short key = (Short) itr.next();
				
				//System.out.println("The no. of Anshuman locations on contour "+(st+1)+" is "+contourLocs[st].size());
				System.out.println("The no. of locations on contour "+(key.shortValue())+" is "+ContourPointsMap.get(key.shortValue()).size());
				System.out.println("--------------------------------------------------------------------------------------");
				
			}
			
			ip = new ObjectInputStream(new FileInputStream(new File(apktPath +"online_contours/non_Contours.map")));
			obj = (onlineLocationsMap)ip.readObject();
			non_ContourPointsMap = obj.getContourMap();
			itr = non_ContourPointsMap.keySet().iterator();
			while(itr.hasNext()){
				Short key = (Short) itr.next();
				
				//System.out.println("The no. of Anshuman locations on contour "+(st+1)+" is "+contourLocs[st].size());
				System.out.println("The no. of locations on Non contour "+(key.shortValue())+" is "+non_ContourPointsMap.get(key.shortValue()).size());
				System.out.println("--------------------------------------------------------------------------------------");
			}

			
			if(CG_done){
				//iterator of 
				HashMap<Short,HashSet<Short>> contourPlansReduced = new HashMap<Short,HashSet<Short>>();
				// update what plans are in which contour
				HashSet<Short> reducedPlansSet = new HashSet<Short>();
				for (short k = 1; k <= ContourPointsMap.size(); k++) {
					Iterator iter = ContourPointsMap.get(k).iterator();
					reducedPlansSet.clear();
					while (iter.hasNext()) {
						location objContourPt = (location) iter.next();
						if (objContourPt.contour_no == k && !reducedPlansSet.contains(objContourPt.reduced_planNumber)) {
							assert(objContourPt.reduced_planNumber != -1) : "contour location not reduced";
							reducedPlansSet.add(objContourPt.reduced_planNumber);
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
	
	private void printSelectivityCost(float  sel_array[], float cost){
		
		System.out.println();
		for(int i=0;i<dimension;i++)
			System.out.print("\t"+sel_array[i]);
		
		System.out.println("\t has cost = "+cost);
		
	}
	
	
	private void writeContourPointstoFile(int contour_no) {

		try {
	    
//	    String content = "This is the content to write into file";
			File file_cs, file_contour;
			File file;
			
			
			
			if(f_marwa.exists() && !f_marwa.isDirectory()) {

				file_cs = new File("/home/dsladmin/Srinivas/data/others/covering_contours/"+qtName+contour_no+".txt");
			}
			else{
				file_cs = new File("/home/dsladmin/Srinivas/data/others/covering_contours/"+qtName+contour_no+".txt");
			}
          
			if(f_marwa.exists() && !f_marwa.isDirectory()) {
				file_contour = new File("/home/dsladmin/Srinivas/data/others/contours/"+qtName+contour_no+".txt");
			}
			else{
				file_contour = new File("/home/dsladmin/Srinivas/data/others/contours/"+qtName+contour_no+".txt");
			}

          
           
	    // if file doesn't exists, then create it
	    if (!file_cs.exists()) {
	        file_cs.createNewFile();
	    }
	    
	    if (!file_contour.exists()) {
	        file_contour.createNewFile();
	    }
	    

	    FileWriter writer_cs = new FileWriter(file_cs, false);
	    FileWriter writer_contour = new FileWriter(file_contour, false);

	    
	    PrintWriter pw_cs = new PrintWriter(writer_cs);
	    PrintWriter pw_contour = new PrintWriter(writer_contour);
	    
	    //Take iterator over the list
	    for(location p : contour_points) {		 
	   	 	for(int d=0; d < dimension; d++)
	   	 		if(d < dimension -1)
	   	 			pw_cs.print(p.get_dimension(d) + "\t");
	   	 		else
	   	 			pw_cs.print(p.get_dimension(d) + "\t"+p.get_cost()+"\n");
	    }

	    for(location p : non_contour_points) {		 
	    	for(int d=0; d < dimension; d++)
	   	 		if(d < dimension -1)
	   	 		pw_contour.print(p.get_dimension(d) + "\t");
	   	 		else
	   	 		pw_contour.print(p.get_dimension(d) + "\n");
		 }
	    
	    pw_cs.close();
	    writer_cs.close();

	    pw_contour.close();
	    writer_contour.close();

		} catch (IOException e) {
	    e.printStackTrace();
	}
		
	}

	
	public void writeMaptoFile(boolean CG_Done){

		try {
			String path;
			FileOutputStream fos;
			ObjectOutputStream oos;

			File path_dir  = new File(apktPath+"online_contours/");
			if(!path_dir.exists())
				path_dir.mkdirs();
			
			//for writing the contours map
			if(!CG_Done)
				path = new String (apktPath+"online_contours/Contours.map");
			else
				path = new String (apktPath+"online_contours/Red_Contours.map");
			
			fos = new FileOutputStream (path);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(new onlineLocationsMap(ContourPointsMap));

			//for writing the non contours map
			path = new String (apktPath+"online_contours/non_Contours.map");
			fos = new FileOutputStream (path);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(new onlineLocationsMap(non_ContourPointsMap));
			
			oos.flush();
			oos.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			System.exit(0);
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

		if(resolution == 10){
			if(sel_distribution == 0){

				//This is for TPCH queries 
				selectivity[0] = 0.0005f;	selectivity[1] = 0.005f;selectivity[2] = 0.01f;	selectivity[3] = 0.02f;
				selectivity[4] = 0.05f;		selectivity[5] = 0.10f;	selectivity[6] = 0.20f;	selectivity[7] = 0.40f;
				selectivity[8] = 0.60f;		selectivity[9] = 0.95f;                                   // oct - 2012
			}
			else if( sel_distribution ==1){

				//This is for TPCDS queries
				selectivity[0] = 0.00005f;	selectivity[1] = 0.0005f;selectivity[2] = 0.005f;	selectivity[3] = 0.02f;
				selectivity[4] = 0.05f;		selectivity[5] = 0.10f;	selectivity[6] = 0.15f;	selectivity[7] = 0.25f;
				selectivity[8] = 0.50f;		selectivity[9] = 0.99f;                                // dec - 2012
			}
			else
				assert (false) :funName+ "ERROR: should not come here";

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

				selectivity[0] = 0.0001f;	selectivity[1] = 0.0002f;
				selectivity[2] = 0.0004f;   selectivity[3] = 0.0006f;		selectivity[4] = 0.0008f;	selectivity[5] = 0.001f;
				selectivity[6] = 0.002f;	selectivity[7] = 0.004f;   selectivity[8] = 0.005f;	selectivity[9] = 0.008f;	selectivity[10] = 0.01f;
				selectivity[11] = 0.05f;	selectivity[12] = 0.1f;	selectivity[13] = 0.15f;	selectivity[14] = 0.25f;
				selectivity[15] = 0.40f;	selectivity[16] = 0.60f;	selectivity[17] = 0.80f;	selectivity[18] = 0.9f; selectivity[19] = 0.99f;
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
				selectivity[0] = 0.0001f;  selectivity[1] = 0.0002f;	selectivity[2] = 0.0005f;	selectivity[3] = 0.0007f;
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

		if(resolution==100){

			if(sel_distribution == 1){
				selectivity[0] = 0.000064f; 	selectivity[1] = 0.000093f; 	selectivity[2] = 0.000126f; 	selectivity[3] = 0.000161f; 	selectivity[4] = 0.000198f;
				selectivity[5] = 0.000239f; 	selectivity[6] = 0.000284f; 	selectivity[7] = 0.000332f; 	selectivity[8] = 0.000384f; 	selectivity[9] = 0.000440f;
				selectivity[10] = 0.000501f; 	selectivity[11] = 0.000567f; 	selectivity[12] = 0.000638f; 	selectivity[13] = 0.000716f; 	selectivity[14] = 0.000800f;
				selectivity[15] = 0.000890f; 	selectivity[16] = 0.000989f; 	selectivity[17] = 0.001095f; 	selectivity[18] = 0.001211f; 	selectivity[19] = 0.001335f;
				selectivity[20] = 0.001471f; 	selectivity[21] = 0.001617f; 	selectivity[22] = 0.001776f; 	selectivity[23] = 0.001948f; 	selectivity[24] = 0.002134f;
				selectivity[25] = 0.002335f; 	selectivity[26] = 0.002554f; 	selectivity[27] = 0.002790f; 	selectivity[28] = 0.003046f; 	selectivity[29] = 0.003323f;
				selectivity[30] = 0.003624f; 	selectivity[31] = 0.003949f; 	selectivity[32] = 0.004301f; 	selectivity[33] = 0.004683f; 	selectivity[34] = 0.005096f;
				selectivity[35] = 0.005543f; 	selectivity[36] = 0.006028f; 	selectivity[37] = 0.006552f; 	selectivity[38] = 0.007121f; 	selectivity[39] = 0.007736f;
				selectivity[40] = 0.008403f; 	selectivity[41] = 0.009125f; 	selectivity[42] = 0.009907f; 	selectivity[43] = 0.010753f; 	selectivity[44] = 0.011670f;
				selectivity[45] = 0.012663f; 	selectivity[46] = 0.013739f; 	selectivity[47] = 0.014904f; 	selectivity[48] = 0.016165f; 	selectivity[49] = 0.017531f;
				selectivity[50] = 0.019011f; 	selectivity[51] = 0.020613f; 	selectivity[52] = 0.022348f; 	selectivity[53] = 0.024228f; 	selectivity[54] = 0.026263f;
				selectivity[55] = 0.028467f; 	selectivity[56] = 0.030854f; 	selectivity[57] = 0.033440f; 	selectivity[58] = 0.036240f; 	selectivity[59] = 0.039272f;
				selectivity[60] = 0.042556f; 	selectivity[61] = 0.046113f; 	selectivity[62] = 0.049965f; 	selectivity[63] = 0.054136f; 	selectivity[64] = 0.058654f;
				selectivity[65] = 0.063547f; 	selectivity[66] = 0.068845f; 	selectivity[67] = 0.074584f; 	selectivity[68] = 0.080799f; 	selectivity[69] = 0.087530f;
				selectivity[70] = 0.094819f; 	selectivity[71] = 0.102714f; 	selectivity[72] = 0.111263f; 	selectivity[73] = 0.120523f; 	selectivity[74] = 0.130550f;
				selectivity[75] = 0.141411f; 	selectivity[76] = 0.153172f; 	selectivity[77] = 0.165910f; 	selectivity[78] = 0.179705f; 	selectivity[79] = 0.194645f;
				selectivity[80] = 0.210825f; 	selectivity[81] = 0.228348f; 	selectivity[82] = 0.247325f; 	selectivity[83] = 0.267877f; 	selectivity[84] = 0.290136f;
				selectivity[85] = 0.314241f; 	selectivity[86] = 0.340348f; 	selectivity[87] = 0.368621f; 	selectivity[88] = 0.399241f; 	selectivity[89] = 0.432403f;
				selectivity[90] = 0.468316f; 	selectivity[91] = 0.507211f; 	selectivity[92] = 0.549334f; 	selectivity[93] = 0.594953f; 	selectivity[94] = 0.644359f;
				selectivity[95] = 0.697865f; 	selectivity[96] = 0.755812f; 	selectivity[97] = 0.818569f; 	selectivity[98] = 0.886535f; 	selectivity[99] = 0.990142f;

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

	
	public void generateCoveringContours(ArrayList<Integer> order,float cost) throws IOException, SQLException, PicassoException
	{
		
		String funName = "generateCoveringContours";
		if(DEBUG_LEVEL_1)
			System.out.println("Entered function "+funName);
		
		//learntDim contains the dimensions already learnt (which is null initially)
		//learntDimIndices contains the exact point in the space for the learnt dimensions
		
		ArrayList<Integer> remainingDimList = new ArrayList<Integer>();
		for(int i=0;i<order.size();i++)
		{
			if(learntDim.contains(order.get(i))!=true)
			{
				remainingDimList.add(order.get(i));
				qrun_sel[order.get(i)] = -1.0f;
			}			
		}
		
		float temp_qrun[] = new float[dimension];
		//following code was used to test the recursion
//		if(remainingDimList.size()==2 ){
//			for(int i=0;i<dimension;i++)
//				System.out.print("\t"+qrun_sel[i]);
//			System.out.println();
//			return;
//		}
		
		if(remainingDimList.size()==2)
		{ 
			int last_dim1=-1, last_dim2=-1;
			
//			if((opt_call % 500) ==0 )
//				System.out.println("running optimiation for debug along thread  ["+min_cc_sel_idx+"-"+max_cc_sel_idx+"] opt_call = "+opt_call+" fpc call = "+fpc_call+" location hits = "+location_hits);
			
			for(int i=0;i<dimension;i++)
			{
				if(learntDim.contains(i))
				{
					assert(qrun_sel[i] != -1.0) : "the selectivity values are assigned"; 
					//System.out.print(arr[i]+",");
				}
				else if(last_dim1 == -1)
				{
					assert(last_dim2 == -1) : last_dim2+" dimension should not be set at this point of time";
					assert(qrun_sel[i] == -1.0) : "the selectivity values are not yet assigned";
					last_dim1 = i;
				}
				else 
				{ 	 
					assert(last_dim1 != -1) : last_dim1+" dimension should be set at this point of time";
					assert(qrun_sel[i] == -1.0) : "the selectivity values are not yet assigned";
					last_dim2 = i;
				}
			}

			assert (last_dim1>=0 && last_dim1<dimension) :funName+ " : last dim1 index problem ";
			assert (last_dim2>=0 && last_dim2<dimension) :funName+ " : last dim2  index problem ";
			assert (remainingDimList.size() + learntDim.size() == dimension) : funName+" : learnt dimension data structures size not matching";

			//for the last two dimensions last_dim1 and last_dim2
			location loc1, loc2;
			
			
			qrun_sel[last_dim1] = qrun_sel[last_dim2] = minimum_selectivity;
			
			if(trie){
				loc1 = searchLocationInGraph(qrun_sel,0);
				if(loc1 == null){
					loc1 = searchLocationInGraph(qrun_sel,2);
				}
			}
			else
				loc1 = locationAlreadyExist(qrun_sel);
			if(loc1 == null){
				loc1 = new location(qrun_sel,this);
//				if(Math.abs(qrun_sel[0] - minimum_selectivity) < 0.00001f)
//					printSelectivityCost(qrun_sel, -1);
				//non_contour_points.add(loc1);
				if(trie)
					addLocationtoGraph(loc1, 2);
				opt_call++;
			}
			
			assert(loc1 != null): "loc1 is null";
			float optimization_cost_low = loc1.get_cost();
			
			
			qrun_sel[last_dim1] = qrun_sel[last_dim2] = 1.0f;
			if(trie){
				loc2 = searchLocationInGraph(qrun_sel,0);
				if(loc2 == null){
					loc2 = searchLocationInGraph(qrun_sel,2);
				}

			}
			else
				loc2 = locationAlreadyExist(qrun_sel);
			
			if(loc2 == null){
				loc2 = new location(qrun_sel,this);
				//non_contour_points.add(loc2);
//				if(Math.abs(qrun_sel[0] - minimum_selectivity) < 0.00001f)
//					printSelectivityCost(qrun_sel, -1);
				if(trie)
					addLocationtoGraph(loc2, 2);
				opt_call++;
			}
			
			assert(loc2 !=null): "loc2 is null";
			float optimization_cost_high = loc2.get_cost();
			//non_contour_points.add(loc2);
			
			assert(optimization_cost_low <= optimization_cost_high): "violation of PCM";
			//check if the origin of this 2D slice is greater cost
			//OR
			//check if the terminal of this 2D slice has lesser cost
			
			if(optimization_cost_low > cost || optimization_cost_high < cost){
				//do not process the 2D plane
						return;
			}
			
			
			//now we need to explore the 2D surface
			else{
				
				//just the initialize the last two dimensions;
				qrun_sel[last_dim2] = 1.0f;
				qrun_sel[last_dim1] = minimum_selectivity;
				location loc;
				
				if(trie){
					loc = searchLocationInGraph(qrun_sel,0);
					if(loc == null){
						loc = searchLocationInGraph(qrun_sel,2);
					}
				}
				else
					loc = locationAlreadyExist(qrun_sel);
				if(loc == null){
					loc = new location(qrun_sel, this);
					//non_contour_points.add(loc);
//					if(Math.abs(qrun_sel[0] - minimum_selectivity) < 0.00001f)
//						printSelectivityCost(qrun_sel, -1);
					if(trie)
						addLocationtoGraph(loc, 2);
					opt_call++;
				}
				assert(loc != null): "location is null";
				float optimization_cost = loc.get_cost(); 				
				
				location loc_rev;
				float optimization_cost_rev = Float.MIN_VALUE;
				
				if(enhancement){
				//just the initialize the last two dimensions;
				qrun_sel_rev[last_dim2] = minimum_selectivity;
				qrun_sel_rev[last_dim1] = 1.0f;
				
				//set the first two dimensions as per qrun
				for(int i=0;i<dimension; i++){
					if(learntDim.contains(i))
						qrun_sel_rev[i] = qrun_sel[i];																					
				}
				
				if(trie){
					loc = searchLocationInGraph(qrun_sel_rev,0);
					if(loc == null){
						loc = searchLocationInGraph(qrun_sel_rev,2);
					}
				}
				else
					loc = locationAlreadyExist(qrun_sel_rev);
				
				if(loc == null){
					loc = new location(qrun_sel_rev, this);
					//non_contour_points.add(loc);
//					if(Math.abs(qrun_sel_rev[0] - minimum_selectivity) < 0.00001f)
//						printSelectivityCost(qrun_sel_rev, -1);
					if(trie)
						addLocationtoGraph(loc, 2);
					opt_call++;
				}
				
				assert(loc != null): "location is null";
				optimization_cost_rev = loc.get_cost(); 
				if(DEBUG_LEVEL_2)
					printSelectivityCost(qrun_sel_rev, optimization_cost);
				
				}
				
				boolean increase_along_dim1 = true;
		
				//we are reverse jumping along last_dim2 dimension
				
				float target_cost1 = (float)Math.pow(beta, dimension-2)*cost;
				float target_cost2 = (float)Math.pow(beta, dimension-1)*cost;
				float target_cost3 = (float)Math.pow(beta, dimension)*cost;
				int orig_dim1 = last_dim1;
				int orig_dim2 = last_dim2;
				float opt_cost_copy = Float.MIN_VALUE;
				boolean contour_done = false; //to capture the finishing status of contour processing
				
				float [] qrun_copy = new float [dimension];
				for(;;){
					
					
					for(int i=0; i < dimension ; i++){
						if(increase_along_dim1){
							qrun_copy[i] = qrun_sel[i];
							last_dim1 = orig_dim1;
							last_dim2 = orig_dim2;
							opt_cost_copy = optimization_cost; 
						}
						else{ 
							qrun_copy[i] = qrun_sel_rev[i];
							last_dim1 = orig_dim2;
							last_dim2 = orig_dim1;
							opt_cost_copy = optimization_cost_rev;
						}
					}
					
					boolean came_inside_dim2_loop = false;
					contour_done = false;
					while((qrun_copy[last_dim2] > minimum_selectivity) && (target_cost1 <= opt_cost_copy))
					{
						
//						if((Math.abs(qrun_copy[0] - 0.001271) < 0.00001f) && (Math.abs(qrun_copy[1] - 1.0) < 0.00001f) && (Math.abs(qrun_copy[2] - 2.0E-4) < 0.00001f)  )
//							System.out.println("intereseting");
						
						
						came_inside_dim2_loop = true;
						
						if (opt_cost_copy <=  target_cost2)
							break;
						
						//second condition for breaking from loop
						if(enhancement){

							for(int i=0; i < dimension ; i++){
								if(increase_along_dim1){
									qrun_sel[i] = qrun_copy[i];
									optimization_cost = opt_cost_copy; 
								}
								else{ 
									qrun_sel_rev[i] = qrun_copy[i];
									optimization_cost_rev  = opt_cost_copy;
								}
							}

							if((qrun_sel[orig_dim1] >= qrun_sel_rev[orig_dim1]) || (qrun_sel[orig_dim2] <= qrun_sel_rev[orig_dim2])){

								//setting 
								contour_done = true;
								break;
							}
						}							
						float old_sel = qrun_copy[last_dim2];
						
						qrun_copy[last_dim2] = roundToDouble(old_sel/beta);

						
						if(qrun_copy[last_dim2] <= minimum_selectivity)
							qrun_copy[last_dim2] = minimum_selectivity;
						
						assert(qrun_copy[last_dim2] <= old_sel) : "selectivity not decreasing, even if it has to";
						
						if(DEBUG_LEVEL_2)
						System.out.println("Selectivity learnt "+old_sel/(qrun_copy[last_dim2]*beta));
						
						if(trie){
							loc = searchLocationInGraph(qrun_copy,0);
							if(loc == null){
								loc = searchLocationInGraph(qrun_copy,2);
							}

						}
						else
							loc = locationAlreadyExist(qrun_copy);
						
						if(loc == null){
							loc = new location(qrun_copy, this);
							//non_contour_points.add(loc);
//							if(Math.abs(qrun_copy[0] - minimum_selectivity) < 0.00001f)
//								printSelectivityCost(qrun_copy, -1);
							if(trie)
								addLocationtoGraph(loc, 2);
							//counting the optimization calls
							opt_call++;
							backward_jumps ++;
						}
						
						//non_contour_points.add(loc);
						assert(loc != null) : "location is null";
						opt_cost_copy = loc.get_cost();
						
						if(qrun_copy[last_dim2] <= minimum_selectivity) {
							contour_done = true;
							break;
						}
						
						
						if(qrun_copy[last_dim1] > minimum_selectivity)
							assert (opt_cost_copy >= cost): "covering locaiton has less than contour cost: i.e. covering_cost = "+opt_cost_copy+" and contour cost = "+cost;
						
						if(DEBUG_LEVEL_2)
							printSelectivityCost(qrun_copy, opt_cost_copy);
						
						
					}
					
					//if we hit one of the boundary then we are done
					if(contour_done){
						break;
					}
					
					
					//we are forward jumping along last_dim1 dimension
					//qrun_sel[last_dim1] = minimum_selectivity;
//					if((loc = locationAlreadyExist(qrun_sel)) == null){
//						loc = new location(qrun_sel, this);
//						contour_points.add(loc);
//						opt_call++;
//					}
//					assert(loc != null) : "location is null";
//					optimization_cost = loc.get_cost();
					
					boolean came_inside_dim1_loop = false;
					contour_done = false;
					while((qrun_copy[last_dim1] < 1.0f) && (opt_cost_copy <= target_cost3))
					{

//						if((Math.abs(qrun_copy[0] - 0.001271) < 0.00001f) && (Math.abs(qrun_copy[1] - 1.0) < 0.00001f) && (Math.abs(qrun_copy[2] - 2.0E-4) < 0.00001f)  )
//							System.out.println("intereseting");

						came_inside_dim1_loop = true;
						
						if (opt_cost_copy >=  target_cost2){
							//found a covering contour point
							
							if(trie){
								loc = searchLocationInGraph(qrun_copy,0);
								if(loc == null){
									loc = searchLocationInGraph(qrun_copy,1);
								}
							}
							else
								loc = locationAlreadyExist(qrun_copy);
							
							if(loc == null){
								loc = new location(qrun_copy, this);
								opt_call ++;
//								if(Math.abs(qrun_copy[0] - minimum_selectivity) < 0.00001f)
//									printSelectivityCost(qrun_copy, -1);
								forward_jumps ++;
								if(qrun_copy[0] >= ((float)Math.pow(beta, 3)*minimum_selectivity) && qrun_copy[0] <= ((float)Math.pow(beta, 10)*minimum_selectivity))
									forward_jumps_zero_level++;
							}
	
							
							if(!ContourLocationAlreadyExist(loc.dim_values))
								if(loc.get_contour_no() > 0) //again checking if the loc already exist in earlier contours									
									contour_points.add(new location(loc.dim_values,this));
								else 
									contour_points.add(loc);
								if(trie)
									addLocationtoGraph(loc, 1);
								
								
							if(enhancement_end_contour){	
								for(int d=0; d < dimension; d++)
									temp_qrun[d] = qrun_copy[d];
								
								temp_qrun[last_dim2] = minimum_selectivity;
								
								location temp_loc;
								if(trie){
									temp_loc = searchLocationInGraph(temp_qrun,0);
									if(temp_loc == null){
										temp_loc = searchLocationInGraph(temp_qrun,2);
									}

								}
								else
									temp_loc = locationAlreadyExist(temp_qrun);
								
								if(temp_loc == null){
									temp_loc = new location(temp_qrun, this);
									//non_contour_points.add(temp_loc);
									if(trie)
										addLocationtoGraph(temp_loc, 2);
								}
								
								//assert(temp_loc.get_cost() <= loc.get_cost()*(1+cost_error)) : "cost of the covering location is not higher than that of its boundary location";
								if(temp_loc.get_cost() >= cost) {
									contour_done = true;
									return;
								}
							}
							break;
						}
						
						//second condition for breaking from loop
						if(enhancement){

							for(int i=0; i < dimension ; i++){
								if(increase_along_dim1){
									qrun_sel[i] = qrun_copy[i];
									optimization_cost = opt_cost_copy; 
								}
								else{ 
									qrun_sel_rev[i] = qrun_copy[i];
									optimization_cost_rev  = opt_cost_copy;
								}
							}

							if((qrun_sel[orig_dim1] >= qrun_sel_rev[orig_dim1]) || (qrun_sel[orig_dim2] <= qrun_sel_rev[orig_dim2])){

								contour_done = true;
								//setting 
								break;
							}
						}	
						
//						if((Math.abs(qrun_copy[0] - 1.0E-4) < 0.00001f) && (Math.abs(qrun_copy[1] - 0.013906) < 0.00001f) && (Math.abs(qrun_copy[2] - 1.0) < 0.00001f) )
//							System.out.println("intereseting");

						// the second argument for calculate jump size is the dimension along which we need to traverse				
						double forward_jump = calculateJumpSize(qrun_copy,last_dim1,opt_cost_copy);

						if(DEBUG_LEVEL_2)
							System.out.println("The forward jump size is "+(beta -1)*forward_jump+" from selectivity "+qrun_copy[last_dim1]);
						
						float old_sel = qrun_copy[last_dim1]; 
						qrun_copy[last_dim1] += (beta -1)*forward_jump; //check this again!
						
						if((beta -1)*forward_jump <= 0.0f)
							printSelectivityCost(qrun_copy,opt_cost_copy);
						
						assert((beta -1)*forward_jump > 0.0f) : "jump in the negative direction";
						
						if(qrun_copy[last_dim1]/(old_sel*beta) < 1.0f)
							qrun_copy[last_dim1] = old_sel*beta;
						
						if(qrun_copy[last_dim1]/old_sel < 1.01*beta)
							jump_size_1 ++;
						else if(qrun_copy[last_dim1]/old_sel < 1.1*beta)
							jump_size_10 ++;
						
						//just rounding up the float value
						qrun_copy[last_dim1] = roundToDouble(qrun_copy[last_dim1]); 
						//System.out.println("The jumps sizes are "+(qrun_copy[last_dim1]/old_sel)+" against "+beta);
						assert(qrun_copy[last_dim1] >= old_sel) : "selectivity not increasing, even if it has to";
						
						if(qrun_copy[last_dim1] >= 1.0f){
							
							qrun_copy[last_dim1] = 1.0f;
							if(trie){
								loc = searchLocationInGraph(qrun_copy,0);
								if(loc == null){
									loc = searchLocationInGraph(qrun_copy,1);
								}
							}
							else
								loc = locationAlreadyExist(qrun_copy);
							
							if(loc == null){
								loc = new location(qrun_copy, this);
								opt_call++;
//								if(Math.abs(qrun_copy[0] - minimum_selectivity) < 0.00001f){
//									printSelectivityCost(qrun_copy, -1);
//									System.out.println(" [ "+min_cc_sel_idx+"-"+max_cc_sel_idx+"]"+" jump size "+forward_jump);
//								}
								forward_jumps ++;
								if(qrun_copy[0] >= minimum_selectivity && qrun_copy[0] <= ((float)Math.pow(beta, 2)*minimum_selectivity))
									forward_jumps_zero_level++;
							}
							if(!ContourLocationAlreadyExist(loc.dim_values))
								if(loc.get_contour_no() > 0) //again checking if the loc already exist in earlier contours									
									contour_points.add(new location(loc.dim_values,this));
								else 
									contour_points.add(loc);
								if(trie)
									addLocationtoGraph(loc, 1);
							// check to see if the terminus point is reached for the 2D slice 
							if(qrun_copy[last_dim2] >= 1.0f)
								return; 
							
							contour_done = true;
							break;
						}
						
						if(DEBUG_LEVEL_2)
						System.out.println("Selectivity learnt "+qrun_copy[last_dim1]/(old_sel*beta));
						
						if(trie){
							loc = searchLocationInGraph(qrun_copy,0);
							if(loc == null){
								loc = searchLocationInGraph(qrun_copy,2);
							}
						}
						else
							loc = locationAlreadyExist(qrun_copy);
						
						if(loc == null){
							loc = new location(qrun_copy, this);
							//non_contour_points.add(loc);
							if(trie)
								addLocationtoGraph(loc, 2);
							//counting the optimization calls
							opt_call++;
//							if(Math.abs(qrun_copy[0] - minimum_selectivity) < 0.00001f){
//								printSelectivityCost(qrun_copy, -1);
//								System.out.println(" jump size "+forward_jump);
//							}
							forward_jumps ++;
							if(qrun_copy[0] >= minimum_selectivity && qrun_copy[0] <= ((float)Math.pow(beta, 2)*minimum_selectivity))
								forward_jumps_zero_level++;
						}
						
						//non_contour_points.add(loc);
						assert(loc != null) : "location is null";
						opt_cost_copy = loc.get_cost();
						
						if(qrun_copy[last_dim2] < 1.0)
							assert (opt_cost_copy >= cost): "covering locaiton has less than contour cost: i.e. covering_cost = "+optimization_cost+" and contour cost = "+cost;
						
							if(DEBUG_LEVEL_2)
							printSelectivityCost(qrun_copy, opt_cost_copy);
						
					}
					
					//if we hit the boundary then we are done
					if(contour_done){

						// check to see if the terminus point is reached for the 2D slice 
						if(qrun_copy[last_dim2] >= 1.0f)
							return; 

						break;
					}
					
					
//					if(came_inside_dim1_loop)
//						assert(opt_cost_copy <= target_cost3*(1+cost_error) && (1+cost_error)*opt_cost_copy >= target_cost2) : "dim1 is not in the range: opt_cost = "+opt_cost_copy+" target_cost2 = "+ target_cost2+ " target_cost3 = "+target_cost3; 
						
					
					//
					if(opt_cost_copy > (Math.pow(beta, dimension)*cost)){
						
						opt_cost_copy = (float)Math.pow(beta, dimension)*cost;
						
						if(opt_cost_copy > (Math.pow(beta, dimension)*cost*(1+cost_error))){
							System.out.print("How is this possible? and cost increase is "+opt_cost_copy/(Math.pow(beta, dimension)*cost));
							System.out.println(" \t opt_cost_copy "+opt_cost_copy+" max possible cost "+(Math.pow(beta, dimension)*cost));
						}

					}
					
					
					//copy back the copy-variable to the original data structure
					for(int i=0; i < dimension ; i++){
						if(increase_along_dim1){
							qrun_sel[i] = qrun_copy[i];
							optimization_cost = opt_cost_copy; 
						}
						else{ 
							qrun_sel_rev[i] = qrun_copy[i];
							optimization_cost_rev  = opt_cost_copy;
						}
					}
					
					if(enhancement){
						increase_along_dim1 = increase_along_dim1 ? false : true;
						
						if((qrun_sel[orig_dim1] >= qrun_sel_rev[orig_dim1]) || (qrun_sel[orig_dim2] <= qrun_sel_rev[orig_dim2]) || contour_done)
							break;
					}
				}
			}	

			return;
		}

		
		Integer curDim = remainingDimList.get(0); 
		
		if(curDim == order.get(0)) //to check if it is the outer most dimension
			qrun_sel[curDim] = outermost_dimension_sel.get(min_cc_sel_idx);
		else 
			qrun_sel[curDim] = minimum_selectivity;
		
		int itr = min_cc_sel_idx;
		while(true)
		{	
//			if(qrun_sel[0] == minimum_selectivity)
//				continue;
			boolean flag = false;
			
			if(curDim == order.get(0)) {
				qrun_sel[curDim] = outermost_dimension_sel.get(itr);
				if(itr == max_cc_sel_idx){
					flag = true;
				}
			}
			else{
				if(qrun_sel[curDim] > 1.0f){
					qrun_sel[curDim] = 1.0f;
					flag = true;
				}
			}
				
			learntDim.add(curDim);

			generateCoveringContours(order, cost);
			learntDim.remove(learntDim.indexOf(curDim));
			
			if(curDim == order.get(0))
				itr++;
			else
				qrun_sel[curDim] = roundToDouble(qrun_sel[curDim]*beta);
						
			if(flag)
				break;
		}
	}
	
	
	public void getCoveringContoursParallel(ArrayList<Integer> order, float cost) throws SQLException {

		System.out.println("Number of Usable threads are : "+num_of_usable_threads + " with selec range size: "+outermost_dimension_sel.size());
		
		// 1. Divide the contour_locs into usable threads-1
		
		int step_size = outermost_dimension_sel.size()/num_of_usable_threads;
		int residue = outermost_dimension_sel.size() % num_of_usable_threads;
		int cur_min_val = 0;
		int cur_max_val =  -1;
				
		ArrayList<CoveringContourinputParamStruct> inputs = new ArrayList<CoveringContourinputParamStruct>();
		for (int j = 0; j < num_of_usable_threads ; ++j) {

			cur_min_val = cur_max_val + 1;
			cur_max_val = cur_min_val + step_size ;
			
			if(j==num_of_usable_threads-1 || (outermost_dimension_sel.size() < num_of_usable_threads))
				cur_max_val = outermost_dimension_sel.size();

			CoveringContourinputParamStruct input = null;
			if((residue--) > 0 )
				input = new CoveringContourinputParamStruct(source, cur_min_val, cur_max_val, this,order, cost);
			else{
				cur_max_val --;
				input = new CoveringContourinputParamStruct(source, cur_min_val, cur_max_val, this, order, cost);
			}
			System.out.println("Contour generation for the range : "+"["+cur_min_val+","+cur_max_val+"]");
			inputs.add(input);
			
			if(outermost_dimension_sel.size() < num_of_usable_threads)
				break;
		}
		
		//System.out.println("after spltting");
		// 2. execute them in parallel
	    ExecutorService service = Executors.newFixedThreadPool(num_of_usable_threads);
	    ArrayList<Future<CoveringContourOutputParamStruct>> futures = new ArrayList<Future<CoveringContourOutputParamStruct>>();
	    		
	     for (final CoveringContourinputParamStruct input : inputs) {
	        Callable<CoveringContourOutputParamStruct> callable = new Callable<CoveringContourOutputParamStruct>() {
	            public CoveringContourOutputParamStruct call() throws Exception {
	            	CoveringContourOutputParamStruct output = new CoveringContourOutputParamStruct();
	            	//System.out.println("before getting the connection");
	            	
	            	input.setPropertiesFileConstants(apktPath, qtName, select_query, predicates, dimension, database_conn);
	            	input.doGenerateContours();
	            	//System.out.println("after getting the connection");
	            	
	    			//assert(input.obj.contour_points.size() > 0 || input.obj.non_contour_points.size() > 0): "empty contour or non_contour points";
	            	
	    			output.contour_locs_parallel = input.obj.contour_points;
	    			output.non_contour_locs_parallel = new ArrayList<location>(); 
	            	extractNonContourPointstoList(output.non_contour_locs_parallel, input.obj.non_contour_root);
            		output.opt_call_count = input.obj.opt_call;
            		output.fpc_call_count = input.obj.fpc_call;
            		output.slope_time = input.obj.slope_time;
            		output.location_finding_time = input.obj.location_finding_time;
            		output.location_hits = input.obj.location_hits;
            		output.forward_jumps = input.obj.forward_jumps;
            		output.backward_jumps = input.obj.backward_jumps;
            		output.forward_jump_zero_level = input.obj.forward_jumps_zero_level;
            		output.jump_size_1 = input.obj.jump_size_1;
            		output.jump_size_10 = input.obj.jump_size_10;
            		//assert(input.obj.fpc_call > 0 || input.obj.opt_call > 0): "fpc calls or opt calls are zero even after contor generation";
	            	
        			return output;
	            }
	        };
		       futures.add(service.submit(callable));			   
			    
		}
	     service.shutdown();
	     //System.out.println("after shutdown of service"); 
	     //3. aggregating the results back
	     contour_points.clear();
	     non_contour_points.clear();
	     float max_slope_time = 0f;
	     float max_location_search_time = 0f;
	     for (Future<CoveringContourOutputParamStruct> future : futures) {
		    	try {
		    		CoveringContourOutputParamStruct output = future.get();
					contour_points.addAll(output.contour_locs_parallel);
					non_contour_points.addAll(output.non_contour_locs_parallel);
					output.contour_locs_parallel.clear();
					output.non_contour_locs_parallel.clear();
					fpc_call += output.fpc_call_count;
					opt_call += output.opt_call_count;
					location_hits += output.location_hits;
					forward_jumps += output.forward_jumps;
					backward_jumps += output.backward_jumps;
					jump_size_1 += output.jump_size_1;
					jump_size_10 += output.jump_size_10;
					forward_jumps_zero_level += output.forward_jump_zero_level;
					if(output.slope_time > max_slope_time)
						max_slope_time = output.slope_time;
					
					if(output.location_finding_time > max_location_search_time)
						max_location_search_time = output.location_finding_time;
					
				} catch (InterruptedException | ExecutionException e) {	
					e.printStackTrace();
				}
	     }
	     slope_time += max_slope_time;
	     location_finding_time += max_location_search_time;
	     //System.out.println("after aggregation");
	     
	     if (conn != null) {
				try { conn.close(); } catch (SQLException e) {}
			}
	     
	     return ;
	} 

	
	private int getContourNo(float cost) {
		
		double ratio = cost/min_cost;
		return (int)(Math.ceil(Math.log(ratio)/Math.log(2)))+1;
	}
	
	
	private location locationAlreadyExist(float[] arr) {
		
		long startTime = System.nanoTime();
		long endTime;
		
		String funName = "locationAlreadyExist";
		if(DEBUG_LEVEL_1)
			System.out.println("Entered function "+funName);
		if(!memoization)
			return null;
		boolean flag = false;
		assert(ContourPointsMap.keySet().size() == non_ContourPointsMap.keySet().size()) : "sizes mismatch for the contour and non_contoumaps";
		for(short c = 1; c<=ContourPointsMap.keySet().size(); c++){
			for(location loc: ContourPointsMap.get(c)){
				flag = true;
				for(int i=0;i<dimension;i++){
					//if(loc.get_dimension(i) != arr[i]){
					if(Math.abs(loc.get_dimension(i) - arr[i]) > float_error){
						flag = false;
						break;
					}
				}
				if(flag==true) {
					location_hits ++;
					
					//create a new location since it might exist as part of an older contour
					location loc_temp = new location(loc);
					
					endTime = System.nanoTime();
					location_finding_time += (float)((endTime*1.0f - startTime*1.0f)/1000000000f);
					return loc_temp;
				}
			}
			
			for(location loc: non_ContourPointsMap.get(c)){
				flag = true;
				for(int i=0;i<dimension;i++){
					//if(loc.get_dimension(i) != arr[i]){
					if(Math.abs(loc.get_dimension(i) - arr[i]) > float_error){
						flag = false;
						break;
					}
				}
				if(flag==true) {
					location_hits ++;
					
					endTime = System.nanoTime();
					location_finding_time += (float)((endTime*1.0f - startTime*1.0f)/1000000000f);
					return loc;				}
			}
		}
		
		endTime = System.nanoTime();
		location_finding_time += (float)((endTime*1.0f - startTime*1.0f)/1000000000f);
		return null;
	}

	private boolean ContourLocationAlreadyExist(float[] arr) {
		//TODO: need to test this
		boolean flag = false;
		for(location loc: contour_points){
			flag = true;
			for(int i=0;i<dimension;i++){
				if(Math.abs(loc.get_dimension(i) - arr[i]) > float_error){
					flag = false;
					break;
				}
			}
			if(flag==true)
				return true;
		}
		
		return false;
}

	
	
	private double calculateJumpSize(float[] qrun_copy, int dim, float base_cost) throws SQLException {

		//printSelectivityCost(qrun_copy, -1);

		String opt_path = getOptimalPlanPath(qrun_copy);
		//getFPCCost(qrun_copy, -3, null); // -3 argument is to just dump the xml optimal plan at qrun_copy for slope calculations
		
		//System.out.println("["+min_cc_sel_idx+"-"+max_cc_sel_idx+"]"+" plan paths are "+opt_path);
		
		long startTime = System.nanoTime();
		float delta[] = {0.1f,0.3f};
		int count = 0;
		float sum_slope = 0, divFactor =0;
		float sel[] = new float[dimension];
		for(float del: delta){

			if(count > 0 && qtName.contains("5D")){
				continue;
			}
			for(int d=0; d<dimension;d++)
				sel[d] = qrun_copy[d];

			sel[dim] = sel[dim]*(1+del);
			float fpc_cost = 0;
			fpc_cost = getFPCCost(sel, -2, opt_path);
			
			fpc_slope_count++;
			//to remove the effects of jumps but this satisfies the concavity assumption
			if(fpc_cost >=  (1+del)*base_cost)
				fpc_cost = (1+del)*base_cost;

			float sel_diff = del*(sel[dim]/(1+del));

			if(fpc_cost > base_cost){
				sum_slope += (fpc_cost - base_cost)/(sel_diff);
				divFactor ++;
			}

			//System.out.println("Dim = "+dim+" fpc = "+(fpc_cost)+" base cost = "+base_cost+" neighbour location = "+sel[dim]+" base location = "+sel[dim]/(1+del)+" plan = "+opt_path);
			if(DEBUG_LEVEL_2)
			{
				System.out.println("Dim = "+dim+" fpc = "+(fpc_cost)+" base cost = "+base_cost+" neighbour location = "+sel[dim]+" base location = "+sel[dim]/(1+del));
			}
			count ++;
		}

		//to just check the boundary conditions
//		sel[dim] = minimum_selectivity;
//		double origin_cost = getFPCCost(sel, -2, opt_path);
//		System.out.println("Dim = "+dim+" origin cost  = "+(origin_cost)+" at sel = "+sel[dim]+" plan = "+opt_path);
		double avg_slope =1;double jump_size = minimum_selectivity;
		if(divFactor >0 ){
			avg_slope = sum_slope/divFactor; 			 //TODO: really need to do something else for it!

			//now return the jump_size which is cost/slope or F/m;
			jump_size = base_cost/avg_slope;
		}			

		if(DEBUG_LEVEL_1)
			System.out.println("jump size is "+jump_size);

		fpc_call ++;

		long endTime = System.nanoTime();


		slope_time += (float)((endTime*1.0f - startTime*1.0f)/1000000000f);
		return jump_size;

	}


//	public void concavityValidation(boolean useFPC, boolean optimalPlan, int fpc_plan) throws SQLException{
//	String funName = "concavityValidation";
//	System.out.println(funName+" enterring");
//	
//	double delta[] = {0.1,0.2,0.3}, tolerance =200;
//	
//	double slope[][] = new double[dimension][totalPoints];
//	File f_slope = new File(apktPath+"slope.dat");
//	
//	
//	if(f_slope.exists() && READSLOPE){
//
//		try {
//			FileInputStream fis = new FileInputStream(f_slope);
//			ObjectInputStream iis = new ObjectInputStream(fis);
//			slope = (double[][]) iis.readObject();
//
//		} catch (Exception e) {
//
//		}
//
//	}
//	else{
//
//		for(int loc=0; loc < data.length; loc++){
//			System.out.println("loc = "+loc);
////			if(loc < 2135)
////				continue;
//			int plan = plans[loc];
//			int arr [] = getCoordinates(dimension, resolution, loc);
//			double base_cost =0;
//
//			if(optimalPlan)
//				base_cost = getOptimalCost(loc);
//			else if(fpc_plan > 0)
//				base_cost = fpc_cost_generic(arr, fpc_plan);
//			else
//				assert(true) : "this should not come here"; 
//
//
//			for(int dim =0; dim < dimension; dim++){
//
//				if(useFPC && arr[dim]<resolution-1){
//					double sum_slope = 0, divFactor =0;
//					for(double del: delta){
//						double sel[] = new double[dimension];
//
//						for(int d=0; d<dimension;d++)
//							sel[d] = selectivity[arr[d]];
//
//						sel[dim] = sel[dim]*(1+del);
//						double fpc_cost = 0;
//						if(optimalPlan)
//							fpc_cost = getFPCCost(sel, plan);
//						else 
//							fpc_cost = getFPCCost(sel, fpc_plan);
//						
//						sum_slope += (fpc_cost - base_cost)/(del*(sel[dim]/((1+del))));
//						if(slope[dim][loc] > (double)1 && DEBUG)
//						{
//							System.out.println("Dim = "+dim+" loc ="+loc+" fpc = "+(fpc_cost)+" base cost = "+base_cost+" neighbour location = "+sel[dim]+" base location = "+sel[dim]/(1+del));
//						}
//						if(fpc_cost != base_cost)
//							divFactor ++;
//					}
//					if(divFactor >0 )
//						slope[dim][loc] = sum_slope/divFactor;
//					
//				}				
//				else if(arr[dim]<resolution-1 ){
//					arr[dim]++;
//					if(loc ==9300 && dim==1 && DEBUG)
//						System.out.println("interesting");
//					double fpc_cost = 0;
//					if(optimalPlan)
//						fpc_cost = fpc_cost_generic(arr, plan);
//					else
//						fpc_cost = fpc_cost_generic(arr, fpc_plan);
//					
//					slope[dim][loc] = (fpc_cost - base_cost)/(selectivity[arr[dim]]- selectivity[arr[dim]-1]);
//					if(slope[dim][loc] > (double)1 && DEBUG)
//					{  if(optimalPlan)
//						System.out.println("loc ="+loc+" fpc = "+(fpc_cost_generic(arr, plan))+" base cost = "+base_cost+" neighbour location = "+selectivity[arr[dim]]+" base location = "+selectivity[arr[dim]-1]);
//					else
//						System.out.println("loc ="+loc+" fpc = "+(fpc_cost_generic(arr, fpc_plan))+" base cost = "+base_cost+" neighbour location = "+selectivity[arr[dim]]+" base location = "+selectivity[arr[dim]-1]);
//					}
//					
//					arr[dim]--;
//				}
//			}
//		}
//
//		if(!f_slope.exists() && WRITESLOPE)
//			writeSlopeObject(slope);
//
//
//	}
//	//checking violation
//	int violation5 =0, violation20 =0, violation50 =0, totalCount = 0;
//	for(int loc =0; loc < data.length; loc++){
//		int arr [] = getCoordinates(dimension, resolution, loc);
//		for(int dim =0; dim < dimension; dim++){
//			if(arr[dim]<resolution-1 && selectivity[arr[dim]] > (double)0.00001){
//				arr[dim]++;
//				int locN = getIndex(arr, resolution);
//				if(slope[dim][loc]>0) {
//					if ((slope[dim][loc]*1.5) < (slope[dim][locN])){
//						violation50++;
//						violation20++;
//						violation5++;
//						System.out.println("Dim = "+dim+" loc = "+loc+" slope = "+(slope[dim][loc]*1)+" locN ="+locN+" slope = "+slope[dim][locN]);
//					}
//					else if((slope[dim][loc]*1.2) < (slope[dim][locN])){
//						violation20++;
//						violation5++;
//					}
//					else if((slope[dim][loc]*1.05) < (slope[dim][locN])){
//						violation5++;
//					}
//					
//					
//				}
//				totalCount ++;
//				arr[dim]--;
//			}	
//		}
//	}
//	System.out.println("total count = "+totalCount+" with violation50 = "+violation50+" violation20 = "+violation20+" violation5 ="+violation5);
//	
//	checkJumpAssumption(slope);
////	viewslope(slope, 0);
////	viewslope(slope, 1);
//
//	System.exit(0);
//}
//

	public String getOptimalPlanPath(float[] qrun_copy) throws SQLException {

		long startTime = System.nanoTime();
		location loc;
		String path;

	
		if(trie){
			loc = searchLocationInGraph(qrun_copy,0);
			if(loc == null){
				loc = searchLocationInGraph(qrun_copy,1);
				if(loc == null)
					loc = searchLocationInGraph(qrun_copy,2);
			}
		}
		else
			loc = locationAlreadyExist(qrun_copy);

	
//		if(loc == null)
//			loc = searchLocationInGraph(qrun_copy,2);
		
		
		assert(loc !=null): "location should not be null";
		
		if(loc.get_plan_no() >= 0){
			path = new String(apktPath+"onlinePB/planStructureXML/"+loc.get_plan_no()+".xml");
			long endTime = System.nanoTime();
			opt_plan_path_time += (float)((endTime*1.0f - startTime*1.0f)/1000000000f);
			return path;
		}
		else{			
			File dir = new File(apktPath+"onlinePB/subContourPlans/"+(new Integer(min_cc_sel_idx).toString())+(new Integer(max_cc_sel_idx).toString())+"/");
			if(!dir.exists())
				dir.mkdirs();
			assert(dir.exists()): "unable to create the path for subContourPlans";
			
			Long plan_hash = new Long(loc.plan_hash_val);
			if(!planHashMap.containsKey(plan_hash)){
//			if(true){
				short plan_number = (short)planHashMap.size();
				planHashMap.put(plan_hash,plan_number);
				path = new String(apktPath+"onlinePB/subContourPlans/"+(new Integer(min_cc_sel_idx).toString())+(new Integer(max_cc_sel_idx).toString())+"/"+plan_number+".xml");
				getFPCCost(qrun_copy, -3, path); //writing the xml plan to file
				long endTime = System.nanoTime();
				opt_plan_path_time += (float)((endTime*1.0f - startTime*1.0f)/1000000000f);
				return path;
			}
			else{
				short plan_number = planHashMap.get(plan_hash);
				path = new String(apktPath+"onlinePB/subContourPlans/"+(new Integer(min_cc_sel_idx).toString())+(new Integer(max_cc_sel_idx).toString())+"/"+plan_number+".xml");
				long endTime = System.nanoTime();
				opt_plan_path_time += (float)((endTime*1.0f - startTime*1.0f)/1000000000f);
				return path;
			}
		}
	}

	private void ContourCentricCostGreedy(short contour_no) throws SQLException
	{
		ArrayList<location> contour_locs = new ArrayList<location>();
		
		if(contour_no == -1){
			//get all the location of all the contours
			
			for(short i=1; i<=ContourPointsMap.size();i++) {
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
		
		short originalPlanCnt = (short) new File(apktPath+"onlinePB/planStructureXML").listFiles().length ;
		short plansArray[] = new short[originalPlanCnt];
		HashSet<Short> chosenPlanSet = new HashSet<Short>();
		int remainingSpace = 0;

		Iterator iter;
		short ix;

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
		
		while(iter.hasNext()) {
			location objContourPt = (location) iter.next();
			objContourPt.fpc_plan_cost = new ArrayList<Float>(Collections.nCopies(originalPlanCnt, Float.MIN_VALUE));
		}
		
		//get costs of all plans at all contour_locs
		for (short i=0; i<originalPlanCnt; i++)
			contour_locs = getFPCCostParallel(contour_locs, i);
		
		for(location lc : contour_locs)
			assert(lc.fpc_plan_cost.size() == originalPlanCnt) : "for not all plans in the non_reduced_contour_loc_structure with fpc costs";

		
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
						
					
					double foreign_cost = objContourPt.fpc_plan_cost.get(i);
					// foreign_cost  = getFPCCost(objContourPt.dim_values, i);
					
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
			short maxCoveragePlan = -1;
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
					objContourPt.reduced_planNumber = maxCoveragePlan;	
				}
			}
		}
		
			System.out.println("After Reduction:");
		
			HashMap<Short,HashSet<Short>> contourPlansReduced = new HashMap<Short,HashSet<Short>>();
		// update what plans are in which contour
			HashSet<Short> reducedPlansSet = new HashSet<Short>();
			for (short k = 1; k <= ContourPointsMap.size(); k++) {
				reducedPlansSet.clear();
				iter = contour_locs.iterator();
				while (iter.hasNext()) {
					location objContourPt = (location) iter.next();
					if (objContourPt.contour_no == k && !reducedPlansSet.contains(objContourPt.reduced_planNumber)) {
						reducedPlansSet.add(objContourPt.reduced_planNumber);
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
			
			//following snippet required for making sure that certain static variables are fine.
			location loc  = new location();
			loc.apktPath = this.apktPath;
			loc.select_query = this.select_query;
			loc.predicates = this.predicates;
			loc.dimension = this.dimension;

			if(writeMapstoFile)
				writeMaptoFile(true);
			
			
			Iterator itr = ContourPointsMap.keySet().iterator();
			while(itr.hasNext()){
				Short key = (Short)itr.next();
				for(int pt =0;pt<ContourPointsMap.get(key.shortValue()).size();pt++){
					location p = ContourPointsMap.get(key.shortValue()).get(pt);
					if(p.get_contour_no() != key.shortValue())
						System.out.println("problem");
					assert(p.get_contour_no() == key.shortValue()) : "not matching contour no. with contourmap key value with contour no.= "+p.get_contour_no()+" key = "+key.intValue();
				}
			}
			
		}
	
	
	public ArrayList<location> getFPCCostParallel(ArrayList<location> contour_locs, short plan) throws SQLException {

		System.out.println("Number of Usable threads are : "+num_of_usable_threads + " with contour locs size "+contour_locs.size()+" with plan "+plan);
		
		// 1. Divide the contour_locs into usable threads-1
		
		
		int step_size = contour_locs.size()/num_of_usable_threads;
		int cur_min_val = 0;
		int cur_max_val =  0;
		
		ArrayList<CGinputParamStruct> inputs = new ArrayList<CGinputParamStruct>();
		for (int j = 0; j < num_of_usable_threads ; ++j) {

			cur_min_val = cur_max_val;
			cur_max_val = cur_min_val + step_size ;
			
			if(j==num_of_usable_threads-1 || (contour_locs.size() < num_of_usable_threads))
				cur_max_val = contour_locs.size();

			CGinputParamStruct input = new CGinputParamStruct(new ArrayList<location>(contour_locs.subList(cur_min_val, cur_max_val)), source, plan);
			inputs.add(input);
			
			if(contour_locs.size() < num_of_usable_threads)
				break;
		}
		
		//System.out.println("after spltting");
		// 2. execute them in parallel
		ExecutorService service = Executors.newFixedThreadPool(num_of_usable_threads);
	    ArrayList<Future<CGOutputParamStruct>> futures = new ArrayList<Future<CGOutputParamStruct>>();
	    		
	     for (final CGinputParamStruct input : inputs) {
	        Callable<CGOutputParamStruct> callable = new Callable<CGOutputParamStruct>() {
	            public CGOutputParamStruct call() throws Exception {
	            	CGOutputParamStruct output = new CGOutputParamStruct();
	            	//System.out.println("before getting the connection");
	            	
	            	input.getForiegnCost_all_locations(apktPath, qtName, select_query, predicates, dimension, database_conn);
	            	
	            	//System.out.println("after getting the connection");
	            	output.contour_fpc_done_locs = new ArrayList<location>(input.contour_fpc_locs);
            		return output;
	            }
	        };
		       futures.add(service.submit(callable));			   
			    
		}
	     service.shutdown();
	     //System.out.println("after shutdown of service"); 
	     //3. aggregating the results back
	     ArrayList<location> returning_locs = new ArrayList<location>();
	     for (Future<CGOutputParamStruct> future : futures) {
	    	 
		    	try {
					CGOutputParamStruct output = future.get();
					returning_locs.addAll(output.contour_fpc_done_locs);
				} catch (InterruptedException | ExecutionException e) {
					
					e.printStackTrace();
				}
		    	
		    	
	     }
	     //System.out.println("after aggregation");
	     
	     if (conn != null) {
				try { conn.close(); } catch (SQLException e) {}
			}
	     
	     assert (returning_locs.size() == contour_locs.size()) : "returning locs is same size as contour locs";
	     return returning_locs;//for safety check
	} 
//the first argument denotes the selectivity injected
// the second argument denotes the plan number to be forced, -1 for the optimial plan
// the third argument denotes the path of the plan to be forced, if this is null then the path is constructed using plan_no
	
	public float getFPCCost(float selectivity[], int p_no, String path) throws SQLException{
		//Get the path to p_no.xml
		String funName = "getFPCCost";
		
		if(DEBUG_LEVEL_1) {
			System.out.println("Enterred "+funName);
		}
		
		if(p_no == -2 || p_no == -3)
			assert(path != null): "this situation cannot happen";

		String xml_path;
		if(path == null)
			xml_path = new String(apktPath+"/onlinePB/planStructureXML/"+p_no+".xml");
		else
			xml_path = path;
		
		
		String regx;
	     Pattern pattern;
	     Matcher matcher;
	     float execCost=-1f;
	     
		//create the FPC query : 
	     //Connection conn = null;

	     //System.out.println("Plan No = "+p_no);
			try{      	
				
				
				Statement stmt = conn.createStatement();
				
				String exp_query_inj = new String("Selectivity ( "+predicates+ ") (");
				for(int i=0;i<dimension;i++){
					if(i !=dimension-1){
						exp_query_inj = exp_query_inj + (selectivity[i])+ ", ";
					}
					else{
						exp_query_inj = exp_query_inj + (selectivity[i]) + " )";
					}
				}
				//this is for selectivity injection plus fpc
				exp_query_inj = exp_query_inj + select_query;
				String xml_query = null;
				String exp_query = null;
				//this is for pure fpc
				//exp_query = select_query;
				if((p_no == -2 ) && xml_path!=null){
					exp_query = "explain " + exp_query_inj + " fpc "+xml_path;
					xml_query = "explain (format xml) "+ exp_query_inj+ " fpc "+xml_path;
					//assert(xml_path.contains("subContourPlans")): "subContour plans are not there in xmlPath";
				}
				else if(p_no ==-1 || p_no == -3){
					exp_query = "explain " + exp_query_inj ;
					xml_query = "explain (format xml) "+ exp_query_inj;
				}
				else {
					assert(p_no >=0): "plan number is less than zero";
					assert(xml_path.contains("planStructureXML")): "planStructureXML are not there in xmlPath";
					exp_query = "explain " + exp_query_inj + " fpc "+xml_path;
				}
					
				//exp_query = new String("explain Selectivity ( customer_demographics.cd_demo_sk = catalog_sales.cs_bill_cdemo_sk and date_dim.d_date_sk = catalog_sales.cs_sold_date_sk  and item.i_item_sk = catalog_sales.cs_item_sk and promotion.p_promo_sk = catalog_sales.cs_promo_sk) (0.005, 0.99, 0.001, 0.00005 ) select i_item_id,  avg(cs_quantity) , avg(cs_list_price) ,  avg(cs_coupon_amt) ,  avg(cs_sales_price)  from catalog_sales, customer_demographics, date_dim, item, promotion where cs_sold_date_sk = d_date_sk and cs_item_sk = i_item_sk and   cs_bill_cdemo_sk = cd_demo_sk and   cs_promo_sk = p_promo_sk and cd_gender = 'F' and  cd_marital_status = 'U' and  cd_education_status = 'Unknown' and  (p_channel_email = 'N' or p_channel_event = 'N') and  d_year = 2002 and i_current_price <= 100 group by i_item_id order by i_item_id  fpc /home/lohitkrishnan/ssd256g/data/DSQT264DR20_E/planStructureXML/3.xml");
				//System.out.println(exp_query);
				//System.exit(1);
				//String exp_query = new String(query_opt_spill);
				
//				stmt.execute("set work_mem = '100MB'");
//				//NOTE,Settings: 4GB for DS and 1GB for H
//				if(database_conn==0){
//					stmt.execute("set effective_cache_size='1GB'");
//				}
//				else{
//					stmt.execute("set effective_cache_size='4GB'");
//				}
//
//				//NOTE,Settings: need not set the page cost's
//				stmt.execute("set  seq_page_cost = 1");
//				stmt.execute("set  random_page_cost=4");
//				stmt.execute("set cpu_operator_cost=0.0025");
//				stmt.execute("set cpu_index_tuple_cost=0.005");
//				stmt.execute("set cpu_tuple_cost=0.01");
				
				
				//XML Query

				if(p_no == -3 && xml_query!=null){
//					System.out.println(selectivity[0]+","+selectivity[1]+","+selectivity[2]+": path = "+path);
//					System.out.println(" Query : "+xml_query);
					ResultSet rs_xml = stmt.executeQuery(xml_query);
					StringBuilder xplan = new StringBuilder();
					while(rs_xml.next())  {
						xplan.append(rs_xml.getString(1)); 
					}
					assert(xml_path.contains("subContour")): "subContour plans are not there in xmlPath";
					rs_xml.close();
					if(xplan!=null){
						
						try{
							FileWriter fw_xml = new FileWriter(xml_path, false); 
							fw_xml.write(xplan.toString());
							fw_xml.close();
						}
						catch(Exception e){
							e.printStackTrace();
							ServerMessageUtil.SPrintToConsole("Cannot get plan from postgres: "+e);
							throw new PicassoException("Error getting plan: "+e);
						}
					}
					return -1;
				}
			
				
				ResultSet rs = stmt.executeQuery(exp_query);
				rs.next();
				String str1 = rs.getString(1);
				
				

				
				//System.out.println(str1);
				
				//System.out.println(str1);
				//Do the pattern match to get the cost here
				regx = Pattern.quote("..") + "(.*?)" + Pattern.quote("rows=");
				
				pattern = Pattern.compile(regx);
				matcher = pattern.matcher(str1);
				while(matcher.find()){
					execCost = Float.parseFloat(matcher.group(1));
				//	System.out.println("execCost = "+execCost);
				}
				rs.close();
				stmt.close();	
				
				

			}
			catch(Exception e){
				e.printStackTrace();
				ServerMessageUtil.SPrintToConsole("Cannot get plan from postgres: "+e);
				
			}

			if(DEBUG_LEVEL_1) {
				System.out.println("Exitting "+funName);
			}
	
			assert (execCost > 1 ): "execCost is less than 1";
			return execCost; 
		
	}

	public  float roundToDouble(float d) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPrecision, BigDecimal.ROUND_HALF_UP);
        float f = bd.floatValue(); 
        return f;
    }
	
	double getOptimalCost(int index)
	{
		return this.OptimalCost[index];
	}

	void setXMLPath(){
		assert(min_cc_sel_idx >=0 && max_cc_sel_idx <= outermost_dimension_sel.size() && outermost_dimension_sel.size() > 0) : "min and max outerdimensionselectivity index is not set correctly";
		
		File path_dir  = new File(apktPath+"onlinePB/forcingPlans/");
		if(!path_dir.exists())
			path_dir.mkdirs();		
		//XMLPath = new String(apktPath+"onlinePB/forcingPlans/"+(new Integer(min_cc_sel_idx).toString())+(new Integer(max_cc_sel_idx).toString())+".xml");
	}
	
	public void loadPropertiesFile() {
		/*
		 * Need dimension.
		 */

		Properties prop = new Properties();
		InputStream input = null;

		try {
			input = new FileInputStream("./src/Constants.properties");
			// load a properties file
			prop.load(input);

			// get the property value and print it out
			apktPath = prop.getProperty("apktPath");
			qtName = prop.getProperty("qtName");


			select_query = prop.getProperty("select_query");
			predicates= prop.getProperty("predicates");


			/*
			 * 0 for database_conn is used for tpch type queries
			 */
			database_conn = Integer.parseInt(prop.getProperty("database_conn"));
			input.close();

		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
	}
	
	void readpkt(ADiagramPacket gdp, boolean allPlanCost) throws IOException
	{
		String funName="readpkt";
		//ADiagramPacket gdp = getGDP(new File(pktPath));
		totalPlans = gdp.getMaxPlanNumber();
		dimension = gdp.getDimension();
		resolution = gdp.getMaxResolution();
		
		return;
		
//		data = gdp.getData();
//		totalPoints = (int) Math.pow(resolution, dimension);
//		System.out.println("\nthe total plans are "+totalPlans+" with dimensions "+dimension+" and resolution "+resolution);

		
//		assert (totalPoints==data.length) : "Data length and the resolution didn't match !";
//
//		plans = new int [data.length];
//		OptimalCost = new double [data.length]; 
//		for (int i = 0;i < data.length;i++)
//		{
//			this.OptimalCost[i]= data[i].getCost();
//			this.plans[i] = data[i].getPlanNumber();
//
//		}
//
//		//To get the number of points for each plan
//		int  [] plan_count = new int[totalPlans];
//		for(int p=0;p<data.length;p++){
//			plan_count[plans[p]]++;
//		}
//		//printing the above
//		for(int p=0;p<plan_count.length;p++){
//			System.out.println("Plan "+p+" has "+plan_count[p]+" points");
//		}

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
	// option=0 for map, option=1 for contour, option=2 for non_contour
	void addLocationtoGraph(location loc, int option){

		//System.out.println("["+min_cc_sel_idx+"-"+max_cc_sel_idx+"]");
		online_vertex crawl;
		
		if(option == 0)
			crawl = map_root;
		else if(option == 1)
			crawl = contour_root;
		else if(option == 2)
			crawl = non_contour_root;
		else
			crawl = null;
		
//		if((Math.abs(loc.dim_values[0] - 0.004033) < 0.000001f) && (Math.abs(loc.dim_values[1] - 3.77E-4) < 0.000001f) && (Math.abs(loc.dim_values[2] - 1.0) < 0.00001f)  )
//			System.out.println("Option = "+option+" ["+min_cc_sel_idx+"-"+max_cc_sel_idx+"]"+loc.dim_values[0]+","+loc.dim_values[1]+","+loc.dim_values[2]+" adding location to graph");

		// Traverse through all characters of given word
		for( int level = 0; level < dimension; level++)
		{
			TreeMap<Integer,online_vertex> child = crawl.getChildern();
			float power = (float)Math.pow(10, decimalPrecision);
			//the value 11 is chosen to combat the imprecision in the float and decimal representation
			// moreover it will add a value < 1 which would eventually get rounded due to int'f floor
			int val = (int)(roundToDouble(loc.dim_values[level])*power*(1+Math.pow(11,-1*decimalPrecision)));

			// If there is no such child for current character of given word
			if( (child == null) || !child.containsKey(val)){ // create a child
				
				online_vertex temp;

				if(level == dimension - 1) //then this is a leaf node
                    temp = new online_vertex_leaf(val, loc);
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
	
	location searchLocationInGraph(float [] arr, int option){

		long startTime = System.nanoTime();
		long endTime;
		assert(arr.length == dimension) : "location dim values not matching";
		
		online_vertex crawl;
		
		if(option == 0)
			crawl = map_root;
		else if(option == 1)
			crawl =  contour_root;
		else if(option == 2)
			crawl =  non_contour_root;
		else
			crawl = null;

//		if((Math.abs(arr[0] - 0.004033) < 0.000001f) && (Math.abs(arr[1] - 3.77E-4) < 0.000001f) && (Math.abs(arr[2] - 1.0) < 0.00001f)  )
//			System.out.println("Option = "+option+" [ "+min_cc_sel_idx+"-"+max_cc_sel_idx+"]"+arr[0]+","+arr[1]+","+arr[2]+" searching location from graph");

		// Traverse through all characters of given word
		for( int level = 0; level < dimension; level++)
		{
			if(crawl == null)
				System.out.println("null crawl");
			TreeMap<Integer,online_vertex> child = crawl.getChildern();
			float power = (float)Math.pow(10, decimalPrecision);
			//the value 11 is chosen to combat the imprecision in the float and decimal representation
			// moreover it will add a value < 1 which would eventually get rounded due to int'f floor
			int val = (int)(roundToDouble(arr[level])*power*(1+Math.pow(11,-1*decimalPrecision)));
			
			if(child == null){
				endTime = System.nanoTime();
				location_finding_time += (float)((endTime*1.0f - startTime*1.0f)/1000000000f);
				return null;
			}
			
			if( child.containsKey(val))
				crawl = child.get(val);
			else   // Else create a child
			{
				endTime = System.nanoTime();
				location_finding_time += (float)((endTime*1.0f - startTime*1.0f)/1000000000f);
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
		location_hits ++;
		endTime = System.nanoTime();
		location_finding_time += (float)((endTime*1.0f - startTime*1.0f)/1000000000f);
		return crawl_leaf.loc;
	}
	
}

class CoveringContourinputParamStruct {
	
	Jdbc3PoolingDataSource source;
	String apktPath, qtName, select_query, predicates; 
	int dimension, database_conn;
	Connection conn;
	int min_index, max_index;
	onlinePB obj;
	ArrayList<Integer> order;
	float cost;
	 
	public CoveringContourinputParamStruct(Jdbc3PoolingDataSource source, int min_index, int max_index, onlinePB obj, ArrayList<Integer> order, float cost) throws SQLException {
		this.source = source;
		this.min_index = min_index;
		this.max_index = max_index;
		this.obj = new onlinePB(obj);
		this.order = new ArrayList<Integer>(order);
		this.cost = cost;
	}
	
	public void setPropertiesFileConstants(String apktPath, String qtName, String select_query, String predicates, int dimension, int database_conn) throws SQLException{
		if(apktPath!= null && qtName!= null && predicates!= null && select_query!=null){ 
			this.apktPath = apktPath;
			this.qtName = qtName;
			this.select_query = select_query;
			this.predicates = predicates;
			this.dimension = dimension;
			this.database_conn = database_conn;
		}
		else 
			assert(false) : "either one of apktPath, qtName, predicates, dimension or database_conn is null";
			
	}
	
	public void doGenerateContours(){
				
		try {			
			conn = source.getConnection();
			obj.fpc_call =0;
			obj.opt_call =0;
			obj.slope_time = 0f;
			obj.location_finding_time = 0f;
			obj.location_hits =0;
			obj.min_cc_sel_idx = min_index;
			obj.max_cc_sel_idx = max_index;
			obj.conn = conn;
			obj.contour_points.clear();
			obj.non_contour_points.clear();
			obj.contour_root = new online_vertex(-1);
			obj.non_contour_root = new online_vertex(-1);
			
			assert(order.size() == dimension): "size of order not matching with dimensions";
			assert(cost > 0): "contour searching cost is less than or equal to 0";

			obj.generateCoveringContours(order, cost);
			
			//assert(obj.contour_points.size() > 0 || obj.non_contour_points.size() > 0): "empty contour or non_contour points";
			
			//assert(obj.fpc_call > 0 || obj.opt_call > 0): "fpc calls or opt calls are zero even after contor generation";

		} catch (IOException | SQLException | PicassoException e1) {
			e1.printStackTrace();
		}
		
		if (conn != null) {
			try { conn.close(); } catch (SQLException e) {}
		}
	}
}

	class CGinputParamStruct {
		
		Jdbc3PoolingDataSource source;
		ArrayList<location> contour_fpc_locs;
		String apktPath, qtName, select_query, predicates; 
		int dimension, database_conn; short plan;
		 Connection conn;
		 
		public CGinputParamStruct(ArrayList<location> locs, Jdbc3PoolingDataSource source, short plan) throws SQLException {
			this.source = source;
			this.contour_fpc_locs  = new ArrayList<location>(locs);
			this.plan  = plan; 
		}
		
		public void getForiegnCost_all_locations(String apktPath, String qtName, String select_query, String predicates, int dimension, int database_conn) throws SQLException{
			if(apktPath!= null && qtName!= null && predicates!= null && select_query!=null){ 
				this.apktPath = apktPath;
				this.qtName = qtName;
				this.select_query = select_query;
				this.predicates = predicates;
				this.dimension = dimension;
				this.database_conn = database_conn;
			}
				
			conn = source.getConnection();
			
			for(location loc : contour_fpc_locs) {
				loc.fpc_plan_cost.set(plan,getFPCCost(loc.dim_values, plan));
			}
			
			if (conn != null) {
				try { conn.close(); } catch (SQLException e) {}
			}
		}
		
		
		public float getFPCCost(float selectivity[], short p_no) throws SQLException{
			//Get the path to p_no.xml
			String funName = "getFPCCost";
			
			
			String xml_path = apktPath+"/onlinePB/planStructureXML/"+p_no+".xml";
			
			 String regx;
		     Pattern pattern;
		     Matcher matcher;
		     float execCost=-1f;
		     
			//create the FPC query : 
		    

		     
		     
		     //System.out.println("Plan No = "+p_no);
				try{      	
					
					
					Statement stmt = conn.createStatement();
					String exp_query = new String("Selectivity ( "+predicates+ ") (");
					for(int i=0;i<dimension;i++){
						if(i !=dimension-1){
							exp_query = exp_query + (selectivity[i])+ ", ";
						}
						else{
							exp_query = exp_query + (selectivity[i]) + " )";
						}
					}
					//this is for selectivity injection plus fpc
					exp_query = exp_query + select_query;
					//this is for pure fpc
					//exp_query = select_query;
					if(p_no ==-1){
						exp_query = "explain " + exp_query ;
					}
					else {
						assert(p_no >=0): "plan number is less than zero";
						exp_query = "explain " + exp_query + " fpc "+xml_path;
					}
						
					//exp_query = new String("explain Selectivity ( customer_demographics.cd_demo_sk = catalog_sales.cs_bill_cdemo_sk and date_dim.d_date_sk = catalog_sales.cs_sold_date_sk  and item.i_item_sk = catalog_sales.cs_item_sk and promotion.p_promo_sk = catalog_sales.cs_promo_sk) (0.005, 0.99, 0.001, 0.00005 ) select i_item_id,  avg(cs_quantity) , avg(cs_list_price) ,  avg(cs_coupon_amt) ,  avg(cs_sales_price)  from catalog_sales, customer_demographics, date_dim, item, promotion where cs_sold_date_sk = d_date_sk and cs_item_sk = i_item_sk and   cs_bill_cdemo_sk = cd_demo_sk and   cs_promo_sk = p_promo_sk and cd_gender = 'F' and  cd_marital_status = 'U' and  cd_education_status = 'Unknown' and  (p_channel_email = 'N' or p_channel_event = 'N') and  d_year = 2002 and i_current_price <= 100 group by i_item_id order by i_item_id  fpc /home/lohitkrishnan/ssd256g/data/DSQT264DR20_E/planStructureXML/3.xml");
					//System.out.println(exp_query);
					//System.exit(1);
					//String exp_query = new String(query_opt_spill);
					
//					stmt.execute("set work_mem = '100MB'");
//					//NOTE,Settings: 4GB for DS and 1GB for H
//					if(database_conn == 0){
//						stmt.execute("set effective_cache_size='1GB'");
//					}
//					else{
//						stmt.execute("set effective_cache_size='4GB'");
//					}
//
//					//NOTE,Settings: need not set the page cost's
//					stmt.execute("set  seq_page_cost = 1");
//					stmt.execute("set  random_page_cost=4");
//					stmt.execute("set cpu_operator_cost=0.0025");
//					stmt.execute("set cpu_index_tuple_cost=0.005");
//					stmt.execute("set cpu_tuple_cost=0.01");
					
					
					
					
					ResultSet rs = stmt.executeQuery(exp_query);
					rs.next();
					String str1 = rs.getString(1);
					
					

					//System.out.println(str1);
					
					//System.out.println(str1);
					//Do the pattern match to get the cost here
					regx = Pattern.quote("..") + "(.*?)" + Pattern.quote("rows=");
					
					pattern = Pattern.compile(regx);
					matcher = pattern.matcher(str1);
					while(matcher.find()){
						execCost = Float.parseFloat(matcher.group(1));
					//	System.out.println("execCost = "+execCost);
					}
					rs.close();
					stmt.close();	
					
					

				}
				catch(Exception e){
					e.printStackTrace();
					ServerMessageUtil.SPrintToConsole("Cannot get plan from postgres: "+e);
					
				}

		
				assert (execCost > 1 ): "execCost is less than 1";
				return execCost; 
			
		}
	}
	
	class CGOutputParamStruct {
		ArrayList<location> contour_fpc_done_locs;
	}
	
	class CoveringContourOutputParamStruct {
		ArrayList<location> contour_locs_parallel;
		ArrayList<location> non_contour_locs_parallel;
		int fpc_call_count = 0;
		int opt_call_count = 0;
		int location_hits = 0;
		float slope_time=0f;
		float location_finding_time =0f;
		int forward_jumps = 0;
		int forward_jump_zero_level = 0;
		int backward_jumps = 0;
		int jump_size_10 = 0;
		int jump_size_1 = 0;
 
	}
	
class location implements Serializable
{
	private static final long serialVersionUID = 223L;
	static int dimension;
	static int database_conn;
	static String predicates;
	
	static String select_query;
	ArrayList<Float> fpc_plan_cost;
//	double fpc_cost = Double.MAX_VALUE;
	short opt_plan_no = -1;
	float opt_cost;
	static String apktPath;
	short leafid=0;
	float [] dim_values;
	static Vector<Plan> plans_vector = new Vector<Plan>();
	static int decimalPrecision;
	boolean is_within_threshold[];
	short reduced_planNumber;
	short contour_no = -1;
	long plan_hash_val = -1;
	//Plan plan = new Plan();
	
	location(){
		
	}
	
	location(location loc) {
	
		this.apktPath = loc.apktPath;
		this.dimension = loc.dimension;
		
		if(loc.fpc_plan_cost != null)
			this.fpc_plan_cost = new ArrayList<Float>(loc.fpc_plan_cost);
		else
			this.fpc_plan_cost = new ArrayList<Float>();
		
//		this.fpc_cost = loc.fpc_cost;
		this.opt_plan_no = loc.opt_plan_no;
		this.opt_cost = loc.opt_cost;
		this.decimalPrecision = loc.decimalPrecision;
		this.contour_no = loc.contour_no;
		this.dim_values = new float[dimension];
		this.reduced_planNumber = loc.reduced_planNumber;
		//System.arraycopy(pg.dim_values, 0, dim_values, 0, dimension);
		for(int it=0;it<dimension;it++)
			this.dim_values[it] = loc.dim_values[it];

	}
	
	location(float arr[],  onlinePB obj) throws  IOException, PicassoException{
		
		Connection conn = obj.conn;
		dim_values = new float[dimension];
		for(int i=0;i<dimension;i++){
			dim_values[i] = roundToDouble(arr[i]);
			//		System.out.print(arr[i]+",");
		}
		
		try {
			getPlan(conn);
		} catch (SQLException e) {
			
			e.printStackTrace();
		}
		
	}
	
	location(float arr[], OfflinePB obj) throws  IOException, PicassoException{
		
		
		Connection conn = obj.conn;
		dim_values = new float[dimension];
		//System.out.println();
		for(int i=0;i<dimension;i++){
			dim_values[i] = (arr[i]);
			//System.out.print(arr[i]+",");
		}
		
		if(obj.using_packets){
			
			int idx = obj.getIndex(obj.convertSelectivitytoIndex(arr), OfflinePB.resolution);
	        //System.out.println(Arrays.toString(ind)+"  "+idx+"	"+OfflinePB.resolution );
	        this.opt_cost = (float) obj.OptimalCost[idx];
	        this.opt_plan_no = (short)obj.plans[idx];
		}
		else{
			try {
				getPlan(conn);
			} catch (SQLException e) {

				e.printStackTrace();
			}
		}
	}

	location(double arr[], PlanGen obj, Connection cntion) throws  IOException, PicassoException{
		
		Connection conn = cntion;
		this.dim_values = new float[obj.dimension];
		for(int i=0;i<obj.dimension;i++){
			this.dim_values[i] = (float)(arr[i]);
			//		System.out.print(arr[i]+",");
		}
		
		try {
			getPlan(conn);
		} catch (SQLException e) {
			
			e.printStackTrace();
		}
	}

	
	public void set_contour_no(short c) {
		contour_no = c;
	}
	
	public short get_contour_no() {
		return contour_no;
	}
	
	/*
	 * get the selectivity of the dimension
	 */
	public float get_dimension(int d){
		return dim_values[d];
	}

	/*
	 * get the plan number for this point
	 */
	public Short get_plan_no(){
		return opt_plan_no;

	}

	public float get_cost(){
		return opt_cost;
	}
	

//	double getfpc_cost(){
//		return this.fpc_cost;
//	}
//	
//	void putfpc_cost(double cost){
//		this.fpc_cost=cost;
//	}

	public int get_no_of_dimension(){
		return dimension;
	}

	public void printLocation(){

		for(int i=0;i<dimension;i++){
			System.out.print(dim_values[i]+",");
		}
		System.out.println("   having cost = "+opt_cost+" and plan "+opt_plan_no);
	}

	public  float roundToDouble(float d) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPrecision, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }
	
	public void getPlan(Connection conn) throws PicassoException, IOException, SQLException {

//		if((Math.abs(dim_values[0] - 0.258116) < 0.00001f) && (Math.abs(dim_values[1] - 0.001162) < 0.00001f) && (Math.abs(dim_values[2] - 0.000194) < 0.00001f)  )
//			System.out.println("intereseting");
//
//		if((Math.abs(dim_values[0] - 0.258116) < 0.00001f) && (Math.abs(dim_values[1] - 0.001579) < 0.00001f) && (Math.abs(dim_values[2] - 0.000194) < 0.00001f)  )
//			System.out.println("intereseting");
//
//		if((Math.abs(dim_values[0] - 0.325206) < 0.00001f) && (Math.abs(dim_values[1] - 0.0001) < 0.00001f) && (Math.abs(dim_values[2] - 0.198425) < 0.00001f)  )
//			System.out.println("intereseting");
		Plan plan = new Plan();
		Vector textualPlan = new Vector();
		String cost_str = null;
		
		try{      	
			Statement stmt = conn.createStatement();
			String exp_query = new String("Selectivity ( "+predicates+ ") ( ");
			for(int i=0;i<dimension;i++){
				if(i !=dimension-1){
					exp_query = exp_query + dim_values[i]+ ", ";
				}
				else{
					exp_query = exp_query + dim_values[i] + " ) ";
				}
			}
			exp_query = exp_query + select_query;
			exp_query = "explain " + exp_query ;
			//String exp_query = new String(query_opt_spill);
			//System.out.println(exp_query);
//			stmt.execute("set work_mem = '100MB'");
//			//NOTE,Settings: 4GB for DS and 1GB for H
//			if(database_conn==0){
//				stmt.execute("set effective_cache_size='1GB'");
//			}
//			else{
//				stmt.execute("set effective_cache_size='4GB'");
//			}
//
//			stmt.execute("set  seq_page_cost = 1");
//			stmt.execute("set  random_page_cost=4");
//			stmt.execute("set cpu_operator_cost=0.0025");
//			stmt.execute("set cpu_index_tuple_cost=0.005");
//			stmt.execute("set cpu_tuple_cost=0.01");
			
			ResultSet rs = stmt.executeQuery(exp_query);
			//System.out.println("coming here");
			while(rs.next())  {
				textualPlan.add(rs.getString(1)); 
			}
			


			
			cost_str = new String(textualPlan.get(0).toString());
			
//			if((Math.abs(dim_values[0] - 1.0E-4) < 0.00001f) && (Math.abs(dim_values[1] - 0.013906) < 0.00001f) && (Math.abs(dim_values[2] - 1.0) < 0.00001f) ){
//				System.out.println("Query: "+exp_query);
//				System.out.println(cost_str);
//			}
			String regx;
		     Pattern pattern;
		     Matcher matcher;
		     float execCost=-1f;
			regx = Pattern.quote("..") + "(.*?)" + Pattern.quote("rows=");
			assert(textualPlan.get(0).toString().contains("rows")): "this string does not rows! should be a mistake";
			pattern = Pattern.compile(regx);
			matcher = pattern.matcher(cost_str);
			while(matcher.find()){
				execCost = Float.parseFloat(matcher.group(1));
			//	System.out.println("execCost = "+execCost);
			}
			
			assert(execCost>0): "execution cost is less than or equal to zero";
			
			this.opt_cost = execCost;
			assert(textualPlan.size()>=0): "Empty plan from the optimizer call";
				
			rs.close();
			stmt.close();
		}
		catch(Exception e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Cannot get plan from postgres: "+e);
			throw new PicassoException("Error getting plan: "+e);
		}

		String str = (String)textualPlan.remove(0);
		CreateNode(plan, str, 0, -1);
		//plan.isOptimal = true;
		FindChilds(plan, 0, 1, textualPlan, 2);
		//if(PicassoConstants.saveExtraPlans == false ||  PicassoConstants.topkquery == false)
		SwapSORTChilds(plan);
		 
		int planNumber = -1;
		//plan = database.getPlan(newQuery,query);
		String planDiffLevel = "SUB-OPERATOR";
			plan.computeHash(planDiffLevel);
		
		plan_hash_val = plan.getHash();	
			//System.out.println(dim_values[0]+","+dim_values[2]+","+dim_values[2]+" hash value = "+plan.getHash());	
		//in order to save space clear the plan tree that is saved in plan/node
		plan.nodes.clear();	
		assert(plan.getHash() != 0): "plan hash value not computed";
	}
			
	void setPlanNumber(Connection conn) throws PicassoException{		
		
		assert(this.opt_plan_no == -1) : "already assinged a plan number";
		
		StringBuilder XML_Plan = new StringBuilder();
		Plan plan = new Plan();
		plan.hash = plan_hash_val;
		assert(plan_hash_val != 0): "plan hash value not computed";
		int planNumber = plan.getIndexInVector(plans_vector);                  // see if the plan is new or already seen
		plan.setPlanNo(planNumber);

		if(planNumber == -1) {
			plans_vector.add(plan);
			planNumber=plans_vector.size() - 1;
			plan.setPlanNo(planNumber);
		
			//Dump xml plan
			String xml_path = apktPath+"onlinePB/"+"planStructureXML/"+plan.getPlanNo()+".xml";
			File dir = new File(apktPath+"onlinePB/"+"planStructureXML/");
			//check if the dir exists
			if(!dir.exists())
				dir.mkdirs();
			
			//to write the xml plan into file
			try{      	
				Statement stmt = conn.createStatement();
				String exp_query = new String("Selectivity ( "+predicates+ ") ( ");
				for(int i=0;i<dimension;i++){
					if(i !=dimension-1){
						exp_query = exp_query + dim_values[i]+ ", ";
					}
					else{
						exp_query = exp_query + dim_values[i] + " ) ";
					}
				}
				exp_query = exp_query + select_query;
				String xml_query = "explain (format xml) "+ exp_query ;
//				stmt.execute("set work_mem = '100MB'");
//				//NOTE,Settings: 4GB for DS and 1GB for H
//				if(database_conn==0){
//					stmt.execute("set effective_cache_size='1GB'");
//				}
//				else{
//					stmt.execute("set effective_cache_size='4GB'");
//				}
//
//				stmt.execute("set  seq_page_cost = 1");
//				stmt.execute("set  random_page_cost=4");
//				stmt.execute("set cpu_operator_cost=0.0025");
//				stmt.execute("set cpu_index_tuple_cost=0.005");
//				stmt.execute("set cpu_tuple_cost=0.01");					
				
				ResultSet rs_xml = stmt.executeQuery(xml_query);
				while(rs_xml.next())  {
					XML_Plan.append(rs_xml.getString(1)); 
				}			
				
				rs_xml.close();
				stmt.close();								
			}
			catch(Exception e){
				e.printStackTrace();
				ServerMessageUtil.SPrintToConsole("Cannot get plan from postgres: "+e);
				throw new PicassoException("Error getting plan: "+e);
			}

			File xml_file =new File(xml_path);
			//Execute the xml query

			try{
				FileWriter fw_xml = new FileWriter(xml_file, false); 
				fw_xml.write(XML_Plan.toString());
				fw_xml.close();
			}
			catch(Exception e){
				e.printStackTrace();
				ServerMessageUtil.SPrintToConsole("Cannot get plan from postgres: "+e);
				throw new PicassoException("Error getting plan: "+e);
			}

		}

		//set the optimal plan and cost here
		this.opt_plan_no = (short)planNumber;
	}
	
	
	int CreateNode(Plan plan, String str, int id, int parentid) {

		int id1; //by Srinivas
		if(id==1)
			leafid=-1;
		Node node = new Node();

		if(str.indexOf("->")>=0)
			str=str.substring(str.indexOf("->")+2).trim();
		String cost = str.substring(str.indexOf("..") + 2, str.indexOf("rows") - 1);
		String card = str.substring(str.indexOf("rows") + 5, str.indexOf("width")-1);
		//by Srinivas
		//-------------------------------------------------------------------
		//	        String actual_substring = str.substring(str.indexOf("actual") + 2);
		//	        String actual_cost = actual_substring.substring(str.indexOf("..") + 2, str.indexOf("rows") - 1);
		//	        String actual_card = actual_substring.substring(str.indexOf("rows") + 5, str.indexOf("width")-1);

		//-------------------------------------------------------------------

		if(str.indexOf(" on ") != -1 ||str.startsWith("Subquery Scan")) {
			node.setId(id++);
			node.setParentId(parentid);
			node.setCost(Double.parseDouble(cost));
			node.setCard(Double.parseDouble(card));
			//By Srinivas
			//------------------------------------------------------
			id1 = id-1;
			node.setCost(Double.parseDouble(cost));
			node.setCard(Double.parseDouble(card));
			//-------------------------------------------------------
			if(str.startsWith("Index Scan")){
				node.setName("Index Scan");
			}
			else if(str.startsWith("Subquery Scan")){
				node.setName("Subquery Scan");
			}
			else{
				node.setName(str.substring(0,str.indexOf(" on ")).trim());
			}
			plan.setNode(node,plan.getSize());
			node = new Node();
			node.setId(leafid--);
			node.setParentId(id-1);
			node.setCost(0.0);
			node.setCard(0.0);
			if(str.startsWith("Subquery Scan"))
				node.setName(str.trim().substring("Subquery Scan".length(),str.indexOf("(")).trim());
			else
				node.setName(str.substring(str.indexOf(" on ")+3,str.indexOf("(")).trim());
			plan.setNode(node,plan.getSize());
		} else {
			node.setId(id++);
			node.setParentId(parentid);
			node.setCost(Double.parseDouble(cost));
			node.setCard(Double.parseDouble(card));
			if(!str.substring(str.indexOf("("),str.indexOf(")")+1).trim().contains("cost"))
				node.setPredicate(str.substring(str.indexOf("("),str.indexOf(")")+1).trim());
			node.setName(str.substring(0,str.indexOf("(")).trim());
			//node.setName(str.substring(0,str.indexOf(")")).trim()); //changed by Srinivas
			//id1 = id -1;
			//node.setName(str.substring(0,str.indexOf("(")).trim()+id1); //changed by Srinivas
			plan.setNode(node,plan.getSize());
		}

		return id;
	}


	boolean optFlag;
	int FindChilds(Plan plan, int parentid, int id, Vector text, int childindex) {
		String str ="";
		int oldchildindex=-5;
		while(text.size()>0) {
			int stindex;            
			str = (String)text.remove(0);

			//  System.out.println("finding_childs");

			if (str.indexOf("Plan Type: STABLE")>=0)
				optFlag = false;
			if(str.trim().startsWith("InitPlan"))
				stindex=str.indexOf("InitPlan");
			else if(str.trim().startsWith("SubPlan"))
				stindex=str.indexOf("SubPlan");
			else
				stindex=str.indexOf("->");


			if(stindex==-1)
				continue;
			if(stindex==oldchildindex) {
				childindex=oldchildindex;
				oldchildindex=-5;
			}
			if(stindex < childindex) {
				text.add(0,str);
				break;
			}


			if(stindex>childindex) {
				if(str.trim().startsWith("InitPlan")||str.trim().startsWith("SubPlan")) {
					str = (String)text.remove(0);
					stindex=str.indexOf("->");
					oldchildindex=childindex;
					childindex=str.indexOf("->");
				}
				text.add(0,str);
				id = FindChilds(plan, id-1, id, text, stindex);
				continue;
			}

			if(str.trim().startsWith("InitPlan")||str.trim().startsWith("SubPlan")) {
				str = (String)text.remove(0);
				stindex=str.indexOf("->");
				oldchildindex=childindex;
				childindex=str.indexOf("->");
			}



			if(stindex==childindex)
				id = CreateNode(plan,str, id, parentid);
		}
		return id;
	}

	void SwapSORTChilds(Plan plan) {
		for(int i =0;i<plan.getSize();i++) {
			Node node = plan.getNode(i);
			if(node.getName().equals("Sort")) {
				int k=0;
				Node[] chnodes = new Node[2];
				for(int j=0;j<plan.getSize();j++) {
					if(plan.getNode(j).getParentId() == node.getId()) {
						if(k==0)chnodes[0]=plan.getNode(j);
						else chnodes[1]=plan.getNode(j);
						k++;
					}
				}
				if(k>=2) {
					k=chnodes[0].getId();
					chnodes[0].setId(chnodes[1].getId());
					chnodes[1].setId(k);

					for(int j=0;j<plan.getSize();j++) {
						if(plan.getNode(j).getParentId() == chnodes[0].getId())
							plan.getNode(j).setParentId(chnodes[1].getId());
						else if(plan.getNode(j).getParentId() == chnodes[1].getId())
							plan.getNode(j).setParentId(chnodes[0].getId());
					}
				}
			}
		}
	}

	
}


class online_vertex {

	int value;
	TreeMap<Integer,online_vertex> children;
	
	public online_vertex(int val) {
		value = val;
	}
	
	online_vertex(){
		
	}
	
	public void setValue(int value){
		this.value = value;
	}
	
	public TreeMap<Integer,online_vertex> getChildern(){
		return children;
	}
	
	public void setChildern(TreeMap<Integer,online_vertex> hm){
		children = hm;
	}
}

class online_vertex_leaf  extends online_vertex{

	location loc;

	public online_vertex_leaf(int val, location l) {
		value = val;
		loc = l;
	}
	
	online_vertex_leaf(){
		
	}
}

class Edge {
	online_vertex source;
	online_vertex target;
	public Edge(online_vertex s, online_vertex t) {
		source = s;
		target = t;
	}
	
	@Override
    public int hashCode() {
        final int prime = 31;
        int result = prime * source.hashCode();
        result += prime * target.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Edge other = (Edge) obj;
        if (!source.equals(other.source) || !target.equals(other.target))
            return false;
        return true;
    }
    
    @Override
    public String toString() {
        return "("+source+":"+target+")";
    }
}