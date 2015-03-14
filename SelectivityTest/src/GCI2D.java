/*
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



import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.postgres.PostgresDatabase;

import javax.swing.plaf.metal.MetalIconFactory.FolderIcon16;
import javax.swing.text.html.MinimalHTMLWriter;

import org.omg.CORBA.portable.RemarshalException;

import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;


public class GCI2D
{
	static int UNI = 1;
	static int EXP = 2;
	static double error = 0.03;
	static int selConf;
	static double x_a ;
	static double y_a ;
	static double threshold = 0; //in %
	int plans[];
	double OptimalCost[];
	int totalPlans;
	int dimension;
	int resolution;
	DataValues[] data;
	static int totalPoints;
	double selectivity[];

	//The following parameters has to be set manually for each query
	static String apktPath;
	static String plansPath = "/home/dsladmin/Srinivas/data/spillBound/temp/";
	static String qtName ;
	static String varyingJoins;
	static int JS_multiplier1;
	static int JS_multiplier2;
	static String query;
	static String cardinalityPath;
	
	static boolean randomPredicateOrder = false; //true unless we need to generate random predicate orders
	static boolean MSOCalculation = true;
	static Connection c = null;
	
	ArrayList<Integer> remainingDim;
	static int learning_cost=0;
	static double oneDimCost = 0;

	double minIndex[];
	double maxIndex[];
	
	//This is for giving the order of predicates
 
	
	public static void main(String args[]) throws IOException, SQLException
	{
		selConf = EXP;
		

		double SO =0;
		GCI2D obj = new GCI2D();
		obj.loadPropertiesFile();
		String pktPath = apktPath + qtName + ".apkt" ;
		System.out.println("Query Template: "+qtName);
	//	obj.clearCache();	/* to clear the OS/DB cache*/
	
		ADiagramPacket gdp = obj.getGDP(new File(pktPath));
	
		/*
		 * following 3 lines to run the basic Plan Bouquet
		 */
		CostGreedy2D cg = new CostGreedy2D();
//		threshold = 20; // set the threshold here
//		ADiagramPacket reducedgdp= cg.run(threshold, gdp,apktPath);


		
		//Populate the OptimalCost Matrix.
		obj.readpkt(gdp); 			/* for spillbound without reduction*/
		//obj.readpkt(reducedgdp);  /* for spillbound with CG-FPC*/
		
		//Populate the selectivity Matrix.
		obj.loadSelectivity(EXP); 	
		
		//obj.testFunction(cg); 	/* to test if FPC is working*/

		
		try{
			Class.forName("org.postgresql.Driver");

			c = DriverManager
					.getConnection("jdbc:postgresql://localhost:5431/tpch",
							"sa", "database");
			 System.out.println("Opened database successfully");
		}
		catch ( Exception e ) {
			System.err.println( e.getClass().getName()+": "+ e.getMessage() );

		}

		int i;
		
		double h_cost = obj.getOptimalCost(totalPoints-1);
		double cost = obj.getOptimalCost(0);
		double ratio = h_cost/cost;
		System.out.println("the ratio of C_max/c_min is "+ratio);
		
		
		x_a = obj.findNearestSelectivity(x_a);
		y_a = obj.findNearestSelectivity(y_a);
		System.out.println("The original (x_a,y_a)= ("+x_a+","+y_a+")");
		System.out.println("After nearest selectivity (x_a,y_a)= ("+x_a+","+y_a+")");

		double MSO =0, ASO = 0;
		int max_point = 1;
		if(MSOCalculation)
		//if(false)
			max_point = totalPoints;
		double[] subOpt = new double[max_point];
	  for (int  j = 0; j < max_point ; j++)
	  {
		System.out.println("Entering loop "+j);

		//initialization for every loop
		i = 1;
		double algo_cost =0;
		SO =0;
		cost = obj.getOptimalCost(0);
		obj.intialize(j);
		int[] index = obj.getCoordinates(obj.dimension, obj.resolution, j);
//		if(index[0]%5 !=0 || index[1]%5!=0)
//			continue;
		//x_a=0.9;y_a=0.9; /*uncomment for single execution*/
		
		//----------------------------------------------------------
		while(cost < 2*h_cost && !obj.remainingDim.isEmpty())
		{	
			if(cost>h_cost)
				cost = h_cost;
			System.out.println("---------------------------------------------------------------------------------------------\n");
			System.out.println("Contour "+i+" cost : "+cost+"\n");
			int prev = obj.remainingDim.size();

			//obj.run_new_seed_2d_algorithm(cost,error*cost); 
			if(prev==1){
				obj.oneDimensionSearch(cost);
				algo_cost += oneDimCost;
				learning_cost =0;
			}
			else  /*uncomment for spillBound*/
				obj.genBruteForce(cost,i,cg);
			int present = obj.remainingDim.size();
			if(present < prev - 1 || present > prev)
				System.out.println("ERROR");
		  
		
			//algo_cost = algo_cost+ (learning_cost)*cost;
			algo_cost = algo_cost+ (learning_cost);
			if(present == prev ){    // just to see whether any dimension was learnt

				cost = cost*2;  //current and following line for SpillBound
				i = i+1;
			}
			System.out.println("---------------------------------------------------------------------------------------------\n");

		}  //end of while
		if(!obj.remainingDim.isEmpty()){
			System.out.println("Main: end of while loop: ERROR");
			algo_cost += h_cost;
		}
		System.out.println("The original (x_a,y_a)= ("+x_a+","+y_a+")");
		System.out.print("Cost of actual_sel ="+obj.cost(obj.findNearestPoint(x_a),obj.findNearestPoint(y_a))+"\t");
		System.out.println("at ("+obj.findNearestPoint(x_a)+","+obj.findNearestPoint(y_a)+")"+"=== ("+obj.findNearestSelectivity(x_a)+","+obj.findNearestSelectivity(y_a)+")");

		SO = (algo_cost/obj.cost(obj.findNearestPoint(x_a),obj.findNearestPoint(y_a)));
		subOpt[j] = SO;
		ASO += SO;
		if(SO>MSO)
			MSO = SO;
		System.out.println("SpillBound The SubOptimaility  is "+SO);
	  } //end of for
	  	obj.writeSuboptToFile(subOpt, apktPath);
	  	c.close();
		System.out.println("Plan Bouquet The MaxSubOptimaility  is "+MSO);
		System.out.println("Plan Bouquet The AverageSubOptimaility  is "+(double)ASO/max_point);

	}
	 public void writeSuboptToFile(double[] subOpt,String path) throws IOException {
		// TODO Auto-generated method stub
        File file = new File(path+"spillBound_"+"suboptimality"+".txt");
	    if (!file.exists()) {
	        file.createNewFile();
	    }
	    FileWriter writer = new FileWriter(file, false);
	    PrintWriter pw = new PrintWriter(writer);
	    
	    
		for(int i =0;i<resolution;i++){
			for(int j=0;j<resolution;j++){
				//if(i%5==0 && j%5==0){
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


	public void oneDimensionSearch(double cost) {
		// TODO Auto-generated method stub
		String funName = "oneDimensionSearch";
		if(remainingDim.contains(1)){
			int y = getContourYPoint(findNearestPoint(x_a), cost, null);
			if(y==-1 && cost(findNearestPoint(x_a),0)>cost){
				oneDimCost = 0;
				System.out.println(funName+" Y_max = -1");
				return;
			}
			else if(y==-1 && cost(findNearestPoint(x_a),0)<=cost)
				y = resolution -1;
			oneDimCost = cost(findNearestPoint(x_a),y);
			if(selectivity[y]>=y_a)
				remainingDim.remove(remainingDim.indexOf(1));
			System.out.println(funName+" Y_max = "+y);
			System.out.println(funName+" Cost = "+oneDimCost);
		}
		else if(remainingDim.contains(0)){
			int x = getContourXPoint(findNearestPoint(y_a), cost, null);
			if(x==-1 && cost(0,findNearestPoint(y_a))>cost){
				oneDimCost = 0;
				System.out.println(funName+" X_max = -1");
				return;
			}
			else if(x==-1 && cost(0,findNearestPoint(y_a))<=cost)
				x = resolution -1;
			oneDimCost = cost(x,findNearestPoint(y_a));
			if(selectivity[x]>=x_a)
				remainingDim.remove(remainingDim.indexOf(0));
			System.out.println(funName+" X_max = "+x);
		}

	}

	private void intialize(int location) {
		// TODO Auto-generated method stub
		String funName = "intialize";
		//updating the feasible region
		minIndex[0] = minIndex[1] = 0;
		maxIndex[0] = maxIndex[1] = 0.99;
		
		//updating the remaining dimensions data structure
		remainingDim.clear();
		for(int i=0;i<dimension;i++)
			remainingDim.add(i);
		
		learning_cost = 0;
		oneDimCost = 0;
		//updating the x_a and y_a
		int index[] = getCoordinates(dimension, resolution, location);
		x_a = selectivity[index[0]];
		y_a = selectivity[index[1]];
		
		//sanity check conditions
		assert(remainingDim.size() == dimension): funName+"ERROR: mismatch in remaining Dimensions";
	}

	private void testFunction(CostGreedy2D cg_obj) {
		// TODO Auto-generated method stub
		String funName = "testFunction";
		System.out.println(funName+"plan at (22.99) is "+getPlanNumber(22,99));
		int count=0;
		int total=0;
		for(int i=0;i<resolution;i++){
			for(int j=0;j<resolution;j++){
				
				int p = getPlanNumber(i, j);
				double[] matrix = cg_obj.getAllPlanCost(p);
				double c1= cost(i,j);
				double c2 = cost_matrix(i, j, matrix);
				total++;
				//if((int)c1 == (int)c2)				
				if( Math.abs(c1 - c2) < 0.1*c1 || Math.abs(c1 - c2) < 0.1*c2 )
					count++;
				else
					System.out.printf("\ntestFunction: Plan: %4d, Loc(%3d, %3d): (%6.4f, %6.4f), pktCost: %10.1f, fpcOptCost: %10.1f, error: %4.2f", p, i, j, selectivity[i], selectivity[j], c1, c2, (double)Math.abs(c1 - c2)*100/c1);
			}
		}
		System.out.println(funName+" The fraction is "+count/total);
	}

	public void loadPropertiesFile() {
		// TODO Auto-generated method stub
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
			JS_multiplier1 = Integer.parseInt(prop.getProperty("JS_multiplier1"));
			JS_multiplier2 = Integer.parseInt(prop.getProperty("JS_multiplier2"));
			query = "explain analyze FPC(\"customer\")  (\"10000.0\") select c_custkey, c_name,	l_extendedprice * (1 - l_discount) as revenue, c_acctbal, n_name, c_address, c_phone, c_comment from customer, orders, lineitem,	nation where c_custkey = o_custkey and l_orderkey = o_orderkey and o_orderdate between '1993-01-20' and '1994-01-01' 	and c_nationkey = n_nationkey  order by	revenue desc";
			//query = prop.getProperty("query");
			cardinalityPath = prop.getProperty("cardinalityPath");
			x_a = Double.parseDouble(prop.getProperty("xa"));
			y_a = Double.parseDouble(prop.getProperty("ya"));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	void GCI2D(){
		this.remainingDim = new ArrayList<Integer>();
		for(int i=0;i<dimension;i++)
			this.remainingDim.add(i);
	}

	public void genBruteForce(double cost,int contour_no, CostGreedy2D cg_obj) throws IOException
	{
		Set<Integer> unique_plans = new HashSet();
		
		System.out.println("\nContour number ="+contour_no+",Cost : "+cost);
		// ---------------------------------------------------------------------- Generate Brute Force..
		ArrayList<point> original = new ArrayList<point>();
		int i;
			double x, y;
			int x_index, y_index;
			int max_x_index=-1;
			
			//
			//declaration and initialization of variables
			learning_cost = 0;
			oneDimCost = 0;
			double max_cost=0, min_cost = cost(resolution-1,resolution-1)+1;
			double X_max = -1 ,Y_max=-1;
			
			double max_x_pt[];
			max_x_pt = new double[2];
			max_x_pt[0] = 0;
			max_x_pt[1] = 0;
			
			double max_y_pt[];
			max_y_pt = new double[2];
			max_y_pt[0] = 0;
			max_y_pt[1] = 0;
			int max_x_plan=-1, max_y_plan=-1;
			double cur_val=0;
			//end of declaration of variables

			//searching for specific y's
			for(y_index = resolution-1;y_index >=0 ; y_index--)
			{
				x_index = getContourXPoint(y_index,cost,null);
				if(x_index == -1)
				{
					continue;
				}

				
				
				int temp_x_index = max_x_index+1;
				if(max_x_index==x_index || max_x_index==-1) //in the case when the portion of contours is 
					temp_x_index= x_index;  //parallel to Y-Axis
				//if(true){  //for generating contours
				for(;(temp_x_index<=x_index && temp_x_index<resolution);temp_x_index++)
				{
					cur_val=cost(temp_x_index,y_index);
					x = selectivity[temp_x_index];
					y = selectivity[y_index];

					if(inFeasibleRegion(x,y)){
						if(remainingDim.size()==1){
							if(y_index < getContourYPoint(temp_x_index, cost, null))
								y_index = getContourYPoint(temp_x_index, cost, null);
							x = selectivity[temp_x_index];
							y = selectivity[y_index];
						}
						point p2;
						point prevPoint = isPlanVisited(original,getPlanNumber(temp_x_index,y_index));
						if(prevPoint==null)
							p2 = new point(temp_x_index,y_index,getPlanNumber(temp_x_index,y_index), remainingDim);
						else
							p2 = new point(temp_x_index,y_index,getPlanNumber(temp_x_index,y_index),prevPoint.getOrdering(), remainingDim);
						
						//oneDimCost = cost(temp_x_index,y_index);
						original.add(p2);

						if(cur_val > max_cost)
							max_cost = cur_val;
						if(cur_val < min_cost)
							min_cost = cur_val;

						if(p2.getLearningDimension()==0 && remainingDim.contains(0)){
							if(x>X_max)
							{
								max_x_pt[0] = x;
								max_x_pt[1] = y;
								max_x_plan = getPlanNumber(temp_x_index, y_index);

								X_max=x;
							}
						}
						if(p2.getLearningDimension()==1 && remainingDim.contains(1)){
							if(y>Y_max)
							{
								max_y_pt[0] = x;
								max_y_pt[1] = y;
								max_y_plan = getPlanNumber(temp_x_index, y_index);

								Y_max=y;
							}
						}	
					} //end for inFeasibleRegion
				} //end of temp_index for loop
				if(x_index>max_x_index)
					max_x_index = x_index;

			}

			//searching for specific x's
			if(max_x_index<resolution-1){
				for(x_index = max_x_index+1;x_index < resolution; x_index++)
				{
					y_index = getContourYPoint(x_index,cost,null);
					if(y_index == -1)
					{
						continue;
					}
					x = selectivity[x_index];
					y = selectivity[y_index];

					cur_val = cost(x_index,y_index);
					if(inFeasibleRegion(x,y)){
						
						point p2;
						point prevPoint = isPlanVisited(original,getPlanNumber(x_index,y_index));
						if(prevPoint==null)
							p2 = new point(x_index,y_index,getPlanNumber(x_index,y_index), remainingDim);
						else
							p2 = new point(x_index,y_index,getPlanNumber(x_index,y_index),prevPoint.getOrdering(), remainingDim);
						//oneDimCost = cost(x_index,y_index);

					if(cur_val > max_cost)
						max_cost = cur_val;
					if(cur_val < min_cost)
						min_cost = cur_val;
					
					if(p2.getLearningDimension()==0 && remainingDim.contains(0)){
						if(x>X_max)
						{
						max_x_pt[0] = x;
						max_x_pt[1] = y;
						max_x_plan = getPlanNumber(x_index, y_index);
						
							X_max=x;
						}
					}
					if(p2.getLearningDimension()==1 && remainingDim.contains(1)){
						if(y>Y_max)
						{
						max_y_pt[0] = x;
						max_y_pt[1] = y;
						max_y_plan = getPlanNumber(x_index, y_index);
						
							Y_max=y;
						}
					}	
				} //end for inFeasibleRegion

				}
			}
			//print the no. of points in the contour
			System.out.println("cost of contour is "+ cost+", no of points if is "+original.size());
			//----------------------------------------------------------------------

			//------------------------------------------------------- Writing  the brute force solution
//		try {
//		    
////		    String content = "This is the content to write into file";
//
//	
//             File filex = new File("/home/dsladmin/Srinivas/data/others/contours/"+"x"+contour_no+".txt"); 
//             File filey = new File("/home/dsladmin/Srinivas/data/others/contours/"+"y"+contour_no+".txt");  
//  	    // if file doesn't exists, then create it
//		    if (!filex.exists()) {
//		        filex.createNewFile();
//		    }
//		    if (!filey.exists()) {
//		        filey.createNewFile();
//		    }
//		    FileWriter writerax = new FileWriter(filex, false);
//		    FileWriter writeray = new FileWriter(filey, false);
//		    
//		    PrintWriter pwax = new PrintWriter(writerax);
//		    PrintWriter pway = new PrintWriter(writeray);
//		    //Take iterator over the list
//		    for(point p : original) {
//			    //        System.out.println(p.getX()+":"+p.getY()+": Plan ="+p.p_no);
//		   	 pwax.print((int)p.getX() + "\t");
//		   	 pway.print((int)p.getY()+ "\t");
//		   	 
//		    }
//		    pwax.close();
//		    pway.close();
//		    writerax.close();
//		    writeray.close();
//		    
//			} catch (IOException e) {
//		    e.printStackTrace();
//		}
		//------------------------------------------------------- END of Writing  the brute force solution
		
		//---------------------uncomment the following code to generate contours
//		max_x_plan = max_y_plan = -1; 
//		X_max = Y_max = 0;
		//------------------------------------------------
		
		System.out.println("X_max : "+max_x_plan+" at ("+max_x_pt[0]+","+max_x_pt[1]+")"+" = ("+findNearestPoint(max_x_pt[0])+","+findNearestPoint(max_x_pt[1])+")");
		System.out.println("Y_max : "+max_y_plan+" at ("+max_y_pt[0]+","+max_y_pt[1]+")"+" = ("+findNearestPoint(max_y_pt[0])+","+findNearestPoint(max_y_pt[1])+")");
		
		//To print the max and min cost selected by the contour
		System.out.println("Cost_max : "+max_cost+" Cost_min:  "+min_cost+" having "+original.size()+" points");
		System.out.println("MinIndex = ("+minIndex[0]+","+minIndex[1]+")"+" MaxIndex =("+maxIndex[0]+","+maxIndex[1]+")");
		System.out.println("MinIndex = ("+findNearestPoint(minIndex[0])+","+findNearestPoint(minIndex[1])+")"+" MaxIndex =("+findNearestPoint(maxIndex[0])+","+findNearestPoint(maxIndex[1])+")");

		System.out.println("The points in the contour are");
		for(point p : original) {
			 
			 System.out.print("("+p.getX()+","+p.getY()+") Plan ="+p.p_no+" \t");
			 // Insert the plan number into a set.
			 unique_plans.add(p.p_no);
			 //p.print();
		 }
		 System.out.println("Number of Unique Plans ="+unique_plans.size());
		 unique_plans.clear();

		 original.clear();

//		 if(remainingDim.contains(0) && X_max >= x_a ){
//			System.out.println("Plan "+max_x_plan+" executed at "+max_x_pt[0]+","+max_x_pt[1]+" and learnt x_a completely");
//			System.out.println();
//			minIndex[0] = maxIndex[0] = findNearestSelectivity(x_a);
//			remainingDim.remove(remainingDim.indexOf(0));
//			learning_cost = learning_cost + 1;
//			return;
//		}
//		else 
			if(remainingDim.contains(0))
		{
			
			
			 if(max_x_plan!=-1){
				//update X_max  to contain the actual selectivity learnt from the postgres
				 //double sel = 0;getLearntSelectivity(0,max_x_plan,targetval, max_x_pt[0], max_x_pt[1]);
				 double sel = 0;
				 //sel = Simulated_Spilling(max_x_plan, cg_obj, 0, cur_val);
				 if(X_max <= sel)
					 X_max = sel;
				 sel = getLearntSelectivity(0,max_x_plan,cur_val, max_x_pt[0], max_x_pt[1]);
				 if(X_max<=sel)
					 X_max = sel;
				 else
					 System.out.println("GetLeantSelectivity: postgres selectivity is less");
				 
				 File file = new File(cardinalityPath+"spill_cost");
				 FileReader fr = new FileReader(file);
				 BufferedReader br = new BufferedReader(fr);
				 learning_cost += Double.parseDouble(br.readLine());
				 br.close();
				 fr.close();
				 
				 if(X_max>=x_a){
						System.out.println("Plan "+max_x_plan+" executed at "+max_x_pt[0]+","+max_x_pt[1]+" and learnt x_a completely");
						System.out.println();
						minIndex[0] = maxIndex[0] = findNearestSelectivity(x_a);
						remainingDim.remove(remainingDim.indexOf(0));
						return;
				 }
				 
				 //assert()
			 }
		}
		
			
//		if(remainingDim.contains(1) && Y_max >= y_a ){
//			System.out.println("Plan "+max_y_plan+" executed at "+max_y_pt[0]+","+max_y_pt[1]+" and learnt y_a completely");
//			
//			minIndex[1] = maxIndex[1] = findNearestSelectivity(y_a);
//			remainingDim.remove(remainingDim.indexOf(1));
//			learning_cost++;
//			return;
//		}
//		else 
			if(remainingDim.contains(1))
		{
			if(max_y_plan!=-1){
				//update X_max  to contain the actual selectivity learnt from the postgres
				//double sel =0;// = getLearntSelectivity(1,max_y_plan,targetval, max_y_pt[0], max_y_pt[1]);
				double sel = 0;
				//sel = Simulated_Spilling(max_y_plan, cg_obj, 1,cur_val);
				if(Y_max <= sel)
				Y_max = sel;
				sel  = getLearntSelectivity(1,max_y_plan,cur_val, max_y_pt[0], max_y_pt[1]);
				if(Y_max <= sel)
					Y_max = sel;
				else
					 System.out.println("GetLeantSelectivity: ERROR : postgres selectivity is less");
			//learning cost captures how much cost did the spilled execution actually incur
				File file = new File(cardinalityPath+"spill_cost");
				FileReader fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);
				learning_cost += Double.parseDouble(br.readLine());
				br.close();
				fr.close();
				System.out.println("The cost incurred for spilling is "+learning_cost);
			if(Y_max >= y_a){
				System.out.println("Plan "+max_y_plan+" executed at "+max_y_pt[0]+","+max_y_pt[1]+" and learnt y_a completely");
				
				minIndex[1] = maxIndex[1] = findNearestSelectivity(y_a);
				remainingDim.remove(remainingDim.indexOf(1));
				return;
			}
			}
		}
		
		if(remainingDim.contains(0) && X_max < x_a && max_x_plan!=-1)
			minIndex[0] = X_max;
		if(remainingDim.contains(1) && Y_max < y_a && max_y_plan!=-1)
			minIndex[1] = Y_max;

		    
	}
	
	private point isPlanVisited(ArrayList<point> original, int planNumber) {
		for(point p : original){
			if(p.getPlanNumber()==planNumber)
				return p;
		}
		
		return null;
	}

	// Function which does binary search to find the actual X point given Y!!
	//Return the x co-ordinate *index*
	public int getContourXPoint(int y_act_index, double budget, double[] matrix)
	{
		//TODO : need to get the right most for the budget
		int min_index = 0;
		int max_index = resolution -1;
		int mid_index;
		double mid_cost,mid_cost_r, mid_cost_l;
		int [] coord = new int[2];
		coord[1] = y_act_index;
		
		
		if(matrix!=null && cost_matrix(resolution-1, 0,matrix) <= budget) 
			return resolution-1;

		while (min_index <= max_index)
		{
			mid_index = (int) Math.floor((min_index + max_index)/2);
			// mid_index is the index of selectivity near mid
		//	mid_index = findNearestPoint(selectivity[mid]);
			//Check the cost at index, y_act
			if(matrix==null)
				mid_cost = cost(mid_index, y_act_index);
			else
				mid_cost = cost_matrix(mid_index, y_act_index, matrix);
	//		System.out.println("\n mid_cost : "+mid_cost);
			if(mid_index != resolution - 1)
			{
				if(matrix==null)
					mid_cost_r = cost(mid_index + 1, y_act_index);
				else
					mid_cost_r = cost_matrix(mid_index + 1, y_act_index,matrix);
			}
			else
			{
				mid_cost_r = mid_cost;
			}
			
			if(mid_index != 0)
			{
				if(matrix==null)
					mid_cost_l = cost(mid_index - 1, y_act_index);
				else
					mid_cost_l = cost_matrix(mid_index - 1, y_act_index, matrix);
			}
			else
			{
				mid_cost_l = mid_cost;
			}
			
			
			if(mid_cost >= budget)
			{
				if(mid_cost_l <= budget)
				{
					//Cost lies between mid and mid-1 so be conservative and return mid_index as the x coordinate of the contour point.
					return mid_index;
				}
				//Then we have to Look from min to mid (left-half)
				max_index = mid_index - 1;
			}
			if(mid_cost < budget)
			{
				if(mid_cost_r >= budget)
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
	public int getContourYPoint(int x_act_index, double budget, double[] matrix)
	{
		int min_index = 0;
		int max_index = resolution -1;
		int mid_index;
		double mid_cost,mid_cost_r, mid_cost_l;
		int [] coord = new int[2];
		coord[0] = x_act_index;
		
		if(matrix!=null && cost_matrix(0, resolution-1, matrix) <= budget) 
			return resolution-1;
		
		while (min_index <= max_index)
		{
			mid_index = (int) Math.floor((min_index + max_index)/2);
			// mid_index is the index of selectivity near mid
		//	mid_index = findNearestPoint(selectivity[mid]);
			//Check the cost at index, y_act
			if(matrix == null)
				mid_cost = cost(x_act_index,mid_index );
			else
				mid_cost = cost_matrix(x_act_index,mid_index,matrix );
	//		System.out.println("\n mid_cost : "+mid_cost);
			if(mid_index != resolution - 1)
			{
				if(matrix==null)
					mid_cost_r = cost( x_act_index, mid_index + 1);
				else
					mid_cost_r = cost_matrix( x_act_index, mid_index + 1,matrix);
			}
			else
			{
				mid_cost_r = mid_cost;
			}
			
			if(mid_index != 0)
			{
				if(matrix==null)
					mid_cost_l = cost(x_act_index,mid_index - 1 );
				else
					mid_cost_l = cost_matrix(x_act_index,mid_index - 1,matrix );
			}
			else
			{
				mid_cost_l = mid_cost;
			}
			
			
			if(mid_cost >= budget)
			{
				if(mid_cost_l <= budget)
				{
					//Cost lies between mid and mid-1 so be conservative and return mid_index as the x coordinate of the contour point.
					return mid_index;
				}
				//Then we have to Look from min to mid (left-half)
				max_index = mid_index - 1;
			}
			if(mid_cost < budget)
			{
				if(mid_cost_r >= budget)
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
	
	public int getPlanNumber(int x, int y)
	{
		 int arr[] = {x,y};
		int index = getIndex(arr,resolution);
		return plans[index];
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
		//to write the order of execution of EPPs for each plan in a file
		if(randomPredicateOrder){
			for (int p = 0; p < data.length;p++)
			{

				File file = new File(plansPath+plans[p]+".txt");
				if (!file.exists()) 
					file.createNewFile();
				else
					continue;

				FileWriter writer = new FileWriter(file, false);

				PrintWriter pw = new PrintWriter(writer);

				Random randomGenerator = new Random();
				ArrayList<Integer> tempList = new ArrayList<Integer>();
				int d=0;
				while(d<2){
					Integer t = randomGenerator.nextInt(2);
					if(!tempList.contains(t)){
						tempList.add(t);
						pw.println(t);
						d++;
					}
				}
				pw.close();
				writer.close();

			}
		}
		 	remainingDim = new ArrayList<Integer>();
		for(int i=0;i<dimension;i++)
			remainingDim.add(i);
		minIndex = new double[2];
		maxIndex = new double[2];
		minIndex[0] = minIndex[1] = 0;
		maxIndex[0] = maxIndex[1] = 0.99;


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
	void readpktOld(String pktPath) throws IOException
	{
		ADiagramPacket gdp = getGDP(new File(pktPath));
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
			//System.out.println("Plan Number ="+plans[i]+"\n");
		//	System.out.println("\nOptimal_cost:["+i+"] ="+OptimalCost[i]+"\n");
		}
	
		//to write the order of execution of EPPs for each plan in a file
	
		for (int p = 0; p < data.length;p++)
		{
			
			File file = new File(plansPath+plans[p]+".txt");
		    if (!file.exists()) 
				file.createNewFile();
		    else
		    	continue;
		    
		    FileWriter writer = new FileWriter(file, false);
		    
		    PrintWriter pw = new PrintWriter(writer);
		    
		    Random randomGenerator = new Random();
		    ArrayList<Integer> tempList = new ArrayList<Integer>();
		    int d=0;
		    while(d<2){
		    	Integer t = randomGenerator.nextInt(2);
		    	if(!tempList.contains(t)){
		    		tempList.add(t);
		    		pw.println(t);
		    		d++;
		    	}
		    }
		    pw.close();
		    writer.close();

		}
	
		 	remainingDim = new ArrayList<Integer>();
		for(int i=0;i<dimension;i++)
			remainingDim.add(i);
		minIndex = new double[2];
		maxIndex = new double[2];
		minIndex[0] = minIndex[1] = 0;
		maxIndex[0] = maxIndex[1] = 1;

	   
				
	}
	
	double cost(int x, int y)
	{
		int [] arr = new int [2];
		arr[0] = x;
		arr[1] = y;
		int index = getIndex(arr,resolution);

		/*if(costed_points[index] == -1)
		{
			this.costed_points[index] = this.OptimalCost[index];
			no_of_optimizations = no_of_optimizations + 1;
		}*/
		
		return OptimalCost[index];
	}
	double cost_matrix(int x, int y, double[] cost_matrix)
	{
		int [] arr = new int [2];
		arr[0] = x;
		arr[1] = y;
		int index = getIndex(arr,resolution);

		/*if(costed_points[index] == -1)
		{
			this.costed_points[index] = this.OptimalCost[index];
			no_of_optimizations = no_of_optimizations + 1;
		}*/
		
		return cost_matrix[index];
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
	
	 void run_new_seed_2d_algorithm(double targetval, double errorboundval) throws IOException, SQLException
	{
		/* Start from Top-Left Corner */
		 
		 double max_cost=0, min_cost=cost(resolution-1,resolution-1)+1;
		int cur_x1 = 0, cur_y1 = resolution - 1;
		double cur_val1;
		ArrayList<point> original = new ArrayList<point>();
		Set<Integer> unique_plans = new HashSet();
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
		boolean ContourStarted= false;
		while(cur_x1 < resolution && cur_y1 >= 0)
		{
//			System.out.println("x = " + cur_x + ", y = " + cur_y);

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
					if(inFeasibleRegion(x,y)){
					point p2 = new point(cur_x1,cur_y1,getPlanNumber(cur_x1,cur_y1), remainingDim);
					original.add(p2);

					//update choosen X and Y plans
					choosenXplans[cur_x1] = true; choosenYplans[cur_y1] = true; ContourStarted = true;
					//to get the max and the min cost for the contour
					if(cur_val1 > max_cost)
						max_cost = cur_val1;
					if(cur_val1 < min_cost)
						min_cost = cur_val1;
					
					if(p2.getLearningDimension()==0 && remainingDim.contains(0)){
						if(x>X_max)
						{
						max_x_pt[0] = x;
						max_x_pt[1] = y;
						max_x_plan = getPlanNumber(cur_x1, cur_y1);
						
							X_max=x;
						}
					}
					if(p2.getLearningDimension()==1 && remainingDim.contains(1)){
						if(y>Y_max)
						{
						max_y_pt[0] = x;
						max_y_pt[1] = y;
						max_y_plan = getPlanNumber(cur_x1, cur_y1);
						
							Y_max=y;
						}
					}	
					//System.out.println("x = " + cur_x1 + ", y = " + cur_y1 + ", P" + get2dplannumber(cur_x1, cur_y1));
					//points_found++;
				} //for infeasible region
					if(t_y1 <= cur_y2)
						break;
					t_y1 = t_y1 - 1;
					cur_val1 = cost(cur_x1, t_y1);
				}
			    cur_x1++;
			}
			else if(cur_val1 > targetval -errorboundval && cur_y1 > cur_y2 )
			{

				
				//by Srinivas changes to contour identification code
	//----------------------------------------------------------------------------------------------------------------------
				//enter this code block if none of the no plan plan in either cur_x1 or cur_y1 is added earlier (for continuity of contours)
				if(choosenXplans[cur_x1]==false && choosenYplans[cur_y1]==false && ContourStarted==true){
					double x = selectivity[cur_x1];
					double y = selectivity[cur_y1];
					//SpillBound Algorithm Starts
					if(inFeasibleRegion(x,y)){
					point p2 = new point(cur_x1,cur_y1,getPlanNumber(cur_x1,cur_y1), remainingDim);
					original.add(p2);
					choosenXplans[cur_x1] = true; choosenYplans[cur_y1] = true;
					//to get the max and the min cost for the contour
					if(cur_val1 > max_cost)
						max_cost = cur_val1;
					if(cur_val1 < min_cost)
						min_cost = cur_val1;

					if(p2.getLearningDimension()==0 && remainingDim.contains(0)){
						if(x>X_max)
						{
							max_x_pt[0] = x;
							max_x_pt[1] = y;
							max_x_plan = getPlanNumber(cur_x1, cur_y1);

							X_max=x;
						}
					}
					if(p2.getLearningDimension()==1 && remainingDim.contains(1)){
						if(y>Y_max)
						{
							max_y_pt[0] = x;
							max_y_pt[1] = y;
							max_y_plan = getPlanNumber(cur_x1, cur_y1);

							Y_max=y;
						}
					}
				}
				}
//----------------------------------------------------------------------------------------------------------------------
				cur_y1--;
			}
			else if(cur_val1 < targetval +errorboundval && cur_x1 < cur_x2)
			{
				
				//by Srinivas changes to contour identification code
	//----------------------------------------------------------------------------------------------------------------------
				//enter this code block if none of the no plan plan in either cur_x1 or cur_y1 is added earlier (for continuity of contours)
				if(choosenXplans[cur_x1]==false && choosenYplans[cur_y1]==false  && ContourStarted==true){
					double x = selectivity[cur_x1];
					double y = selectivity[cur_y1];
					//SpillBound Algorithm Starts
						if(inFeasibleRegion(x,y)){
					point p2 = new point(cur_x1,cur_y1,getPlanNumber(cur_x1,cur_y1), remainingDim);
					original.add(p2);
					choosenXplans[cur_x1] = true; choosenYplans[cur_y1] = true;
					//to get the max and the min cost for the contour
					if(cur_val1 > max_cost)
						max_cost = cur_val1;
					if(cur_val1 < min_cost)
						min_cost = cur_val1;

					if(p2.getLearningDimension()==0 && remainingDim.contains(0)){
						if(x>X_max)
						{
							max_x_pt[0] = x;
							max_x_pt[1] = y;
							max_x_plan = getPlanNumber(cur_x1, cur_y1);

							X_max=x;
						}
					}
					if(p2.getLearningDimension()==1 && remainingDim.contains(1)){
						if(y>Y_max)
						{
							max_y_pt[0] = x;
							max_y_pt[1] = y;
							max_y_plan = getPlanNumber(cur_x1, cur_y1);

							Y_max=y;
						}
					}	
				}
				}
//----------------------------------------------------------------------------------------------------------------------				
				
				cur_x1++;
			}
			else 
				break;
			
		}
		learning_cost = 0;
		
		//To print the max and min coordinates selected by spiillBound
		System.out.println("X_max : "+max_x_plan+" at ("+max_x_pt[0]+","+max_x_pt[1]+")");
		System.out.println("Y_max : "+max_y_plan+" at ("+max_y_pt[0]+","+max_y_pt[1]+")");
		
		//To print the max and min cost selected by the contour
		System.out.println("Cost_max : "+max_cost+" Cost_min:  "+min_cost+" having "+original.size()+" points");
		System.out.println("The points in the contour are");
		for(point p : original) {
			 
			 System.out.print("("+p.getX()+","+p.getY()+") Plan ="+p.p_no+" \t");
			 // Insert the plan number into a set.
			 unique_plans.add(p.p_no);
			 //p.print();
		 }
		 System.out.println("Number of Unique Plans ="+unique_plans.size());
		 //		 for(int k : unique_plans){
		 //	            System.out.println(" Plan ="+k);
		 //	        }
		 unique_plans.clear();

		 original.clear();
		 
		  //Create the Database Connection
		 
		if(remainingDim.contains(0) && X_max >= x_a ){
			System.out.println("Plan "+max_x_plan+" executed at "+max_x_pt[0]+","+max_x_pt[1]+" and learnt x_a completely");
			System.out.println();
			minIndex[0] = maxIndex[0] = findNearestSelectivity(x_a);
			remainingDim.remove(remainingDim.indexOf(0));
			learning_cost = learning_cost + 1;
			return;
		}
		else if(remainingDim.contains(0))
		{
			
			
			 if(max_x_plan!=-1){
				//update X_max  to contain the actual selectivity learnt from the postgres
				 double sel = 0;//getLearntSelectivity(0,max_x_plan,targetval, max_x_pt[0], max_x_pt[1]);
				 if(X_max < sel)
					 X_max = sel;
				 else
					 System.out.println("GetLeantSelectivity: postgres selectivity is less");
		            File file = new File(cardinalityPath+"spill_cost");
		        	FileReader fr = new FileReader(file);
		        	BufferedReader br = new BufferedReader(fr);
					 
				 learning_cost++;
				 //assert()
			 }
		}
		
			
		if(remainingDim.contains(1) && Y_max >= y_a ){
			System.out.println("Plan "+max_y_plan+" executed at "+max_y_pt[0]+","+max_y_pt[1]+" and learnt y_a completely");
			
			minIndex[1] = maxIndex[1] = findNearestSelectivity(y_a);
			remainingDim.remove(remainingDim.indexOf(1));
			learning_cost++;
			return;
		}
		else if(remainingDim.contains(1))
		{
			if(max_y_plan!=-1){
				//update X_max  to contain the actual selectivity learnt from the postgres
				double sel =0;// = getLearntSelectivity(1,max_y_plan,targetval, max_y_pt[0], max_y_pt[1]);
				if(Y_max < sel)
				Y_max = sel;
				else
					 System.out.println("GetLeantSelectivity: postgres selectivity is less");
			learning_cost++;
			}
		}
		
		if(remainingDim.contains(0) && X_max < x_a)
			minIndex[0] = X_max;
		if(remainingDim.contains(1) && Y_max < y_a)
			minIndex[1] = Y_max;
		
		

	}

	public double getLearntSelectivity(int dim, int plan, double cost,double x , double y) {
		// TODO Auto-generated method stub
		
		if(remainingDim.size()==1)
			return 0;   //TODO dont do spilling in the 1D case, until we fix the INL case
			
		double multiplier = 1,selLearnt = Double.MIN_VALUE,selDim;
		double est_rows=1, rows_learnt=1, outer_rows =1;
    	if(dim==0){
    		multiplier = (double)150000/JS_multiplier1;
    		selDim = x_a;
    	}
    	else{
    		multiplier = (double)1500000/JS_multiplier2;
    		selDim = y_a;
    	}

    	
	      //Connection c = null;
	       Statement stmt = null;
	       Double val = new Double(0);
	       //String cardinalityPath = "/home/dsladmin/Srinivas/data/PostgresCardinality/";
	       try {
	     
	         
	        
	         stmt = c.createStatement();
	         
	         
	 		BinaryTree tree = new BinaryTree(new Vertex(0,-1,null,null),null,null);
	 		int spill_values [] = tree.getSpillNode(dim,plan); //[0] gives node id of the tree and [1] gives the spill_node for postgres
	 		int spill_node = spill_values[1];
	        stmt.execute("set spill_node = "+ spill_node);
	        stmt.execute("set spill_join = 1");
	        stmt.execute("set work_mem = '100MB'");
	        stmt.execute("set effective_cache_size='1GB'");
	        stmt.execute("set  seq_page_cost = 0");
	        stmt.execute("set  random_page_cost=0");
	        stmt.execute("set limit_cost = "+ cost);
	        stmt.execute("set full_robustness = on");
	        stmt.execute("set oneFPCfull_robustness = on");
	        stmt.execute("set varyingJoins = "+varyingJoins);
	        stmt.execute("set JS_multiplier1 = "+ JS_multiplier1);
	        stmt.execute("set JS_multiplier2 = "+ JS_multiplier2);
	        stmt.execute("set robust_eqjoin_selec1 = "+ x);
	        stmt.execute("set robust_eqjoin_selec2 = "+ y);
	        stmt.execute("set FPC_JS_multiplier1 = "+ JS_multiplier1);
	        stmt.execute("set FPC_JS_multiplier2 = "+ JS_multiplier2);
	        stmt.execute("set FPCrobust_eqjoin_selec1 = "+ x_a);
	        stmt.execute("set FPCrobust_eqjoin_selec2 = "+ y_a);
	        // essentially forcing the  plan optimal at (x,y) location to the query having (x_a,y_a) 
	        // as selectivities been injected 
	        
	        stmt.execute(query);
	        //read the selectivity returned
            File file = new File(cardinalityPath+"spill_cardinality");
        	FileReader fr = new FileReader(file);
        	BufferedReader br = new BufferedReader(fr);
        	
        	//read the selectivity or the info needed for INL
        	val = Double.parseDouble(br.readLine());
        	if(br.readLine()==null)
        		selLearnt = val; //this is the Non-INL case
        	else {
        		rows_learnt = val; //rows learnt for this budget
        		est_rows = Double.parseDouble(br.readLine()); //estimated rows of the join node 
            	outer_rows = Double.parseDouble(br.readLine()); //actual outer rows of the join node
            	
            	//selectivity calculations
            	double inner_rows = (est_rows*multiplier)/(selDim*outer_rows);
            	val = rows_learnt/(inner_rows*outer_rows);
        	}
        	        	
        	br.close();
        	fr.close();
	       }
	       catch ( Exception e ) {
				System.err.println( e.getClass().getName()+": "+ e.getMessage() );

			}


        	        	
        	selLearnt = val * multiplier *(1+error);
        	selLearnt = findNearestSelectivity(selLearnt);
        	assert(selLearnt<=1):"error in multiplier";
        	System.out.println("Postgres: selectivity learnt  "+selLearnt+" with plan number "+plan);
        	return selLearnt; //has to have single line in the file

	}

	public double getLearntSelectivityOld(int dim, int plan, double cost,double x , double y) {
		// TODO Auto-generated method stub
		
		if(remainingDim.size()==1)
			return 0;   //TODO dont do spilling in the 1D case, until we fix the INL case
			while(!clearCache());
				
	      Connection c = null;
	       Statement stmt = null;
	       Double val_l,val_r,val;
	       //String cardinalityPath = "/home/dsladmin/Srinivas/data/PostgresCardinality/";
	       try {
	         Class.forName("org.postgresql.Driver");
	         c = DriverManager
	            .getConnection("jdbc:postgresql://localhost:5431/tpch",
	            "sa", "database");
	         System.out.println("Opened database successfully");
	         stmt = c.createStatement();
	         
	         
	 		BinaryTree tree = new BinaryTree(new Vertex(0,-1,null,null),null,null);
	 		int spill_values [] = tree.getSpillNode(dim,plan); //[0] gives node id of the tree and [1] gives the spill_node for postgres
	 		int spill_node = spill_values[1];
	        stmt.execute("set spill_node = "+ spill_node);
	        stmt.execute("set limit_cost = "+ cost);
	        stmt.execute("set full_robustness = on");
	        stmt.execute("set oneFPCfull_robustness = on");
	        stmt.execute("set varyingJoins = "+varyingJoins);
	        stmt.execute("set JS_multiplier1 = "+ JS_multiplier1);
	        stmt.execute("set JS_multiplier2 = "+ JS_multiplier2);
	        stmt.execute("set robust_eqjoin_selec1 = "+ x);
	        stmt.execute("set robust_eqjoin_selec2 = "+ y);
	        stmt.execute(query);
	        //read the selectivity returned
            File file = new File(cardinalityPath+"spill_cardinality");
        	FileReader fr = new FileReader(file);
        	BufferedReader br = new BufferedReader(fr);
        	val = Double.parseDouble(br.readLine());
        	br.close();
        	fr.close();

        	//now also read the left and right child cardinality
        	int temp1 = spill_node;
        	tree = tree.getVertexById(spill_values[0]);
        	//int temp1 = 10;
        	if(temp1 <100)
        	{

        		int spill_node_l = temp1%10; temp1 /= 10;
        		int spill_node_r = temp1;
        	
	        stmt.execute("set spill_node = "+ spill_node_l);
	        //stmt.execute("set limit_cost = "+ cost);
	        stmt.execute("set full_robustness = on");
	        stmt.execute("set FPCfull_robustness = on");
	        stmt.execute("set varyingJoins = "+varyingJoins);
	        stmt.execute("set JS_multiplier1 = "+ JS_multiplier1);
	        stmt.execute("set JS_multiplier2 = "+ JS_multiplier2);
	        stmt.execute("set robust_eqjoin_selec1 = "+ x);
	        stmt.execute("set robust_eqjoin_selec2 = "+ y);
	        stmt.execute(query);
	        //read the selectivity returned
            file = new File(cardinalityPath+"spill_cardinality");
        	fr = new FileReader(file);
        	br = new BufferedReader(fr);
        	val_l = Double.parseDouble(br.readLine());
        	br.close();
        	fr.close();

         	//build the spill_node number for this predicate

	        stmt.execute("set spill_node = "+ spill_node_r);
	        //stmt.execute("set limit_cost = "+ cost);
	        stmt.execute("set full_robustness = on");
	        stmt.execute("set FPCfull_robustness = on");
	        stmt.execute("set varyingJoins = "+varyingJoins);
	        stmt.execute("set JS_multiplier1 = "+ JS_multiplier1);
	        stmt.execute("set JS_multiplier2 = "+ JS_multiplier2);
	        stmt.execute("set robust_eqjoin_selec1 = "+ x);
	        stmt.execute("set robust_eqjoin_selec2 = "+ y);
	        stmt.execute(query);
	        //read the selectivity returned
            file = new File(cardinalityPath+"spill_cardinality");
        	fr = new FileReader(file);
        	br = new BufferedReader(fr);
        	val_r = Double.parseDouble(br.readLine());
        	br.close();
        	fr.close();
        	}
        	else {
        	//creating the spill_node if for left and right child
        		HashSet<String> hashStringsLeft = new HashSet<String>();
            	tree.left_child.getRelationNames(hashStringsLeft);
            	
        		HashSet<String> hashStringsParent = new HashSet<String>();
            	tree.getRelationNames(hashStringsParent);
            	
            	HashSet<String> hashStringsRight = new HashSet<String>();
            	tree.right_child.getRelationNames(hashStringsRight);

             	System.out.println("Left Hash Strings are "+hashStringsLeft);
             	System.out.println("Right Hash Strings are "+hashStringsRight);
             	System.out.println("Parent Hash Strings are "+hashStringsParent);
             	//System.out.println("The relationMap is "+tree.left_child.relationMap);
             	//build the spill_node number for this predicate
             	
             	if(hashStringsLeft.isEmpty()){
            		for(String str: hashStringsParent){
            			if(!hashStringsRight.contains(str))
            				hashStringsLeft.add(str);
            		}
            	}
             	else if(hashStringsRight.isEmpty()){
            		for(String str: hashStringsParent){
            			if(!hashStringsLeft.contains(str))
            				hashStringsRight.add(str);
            		}
            	}

            	
             	int spill_node_l =0;
             	for(String str : hashStringsLeft){
             		spill_node_l = spill_node_l * 10	+ tree.left_child.relationMap.get(str).intValue()	;
             	}
             	System.out.println("The Left spillnode id is "+spill_node_l);
             	
             	int spill_node_r = 0;
             	for(String str : hashStringsRight){
             		spill_node_r = spill_node_r * 10	+ tree.right_child.relationMap.get(str).intValue()	;
             	}
             	System.out.println("The Right spillnode id is "+spill_node_r);
        	
        	
        	
	        stmt.execute("set spill_node = "+ spill_node_l);
	        //stmt.execute("set limit_cost = "+ cost);
	        stmt.execute("set full_robustness = on");
	        stmt.execute("set FPCfull_robustness = on");
	        stmt.execute("set varyingJoins = "+varyingJoins);
	        stmt.execute("set JS_multiplier1 = "+ JS_multiplier1);
	        stmt.execute("set JS_multiplier2 = "+ JS_multiplier2);
	        stmt.execute("set robust_eqjoin_selec1 = "+ x);
	        stmt.execute("set robust_eqjoin_selec2 = "+ y);
	        stmt.execute(query);
	        //read the selectivity returned
            file = new File(cardinalityPath+"spill_cardinality");
        	fr = new FileReader(file);
        	br = new BufferedReader(fr);
        	val_l = Double.parseDouble(br.readLine());
        	br.close();
        	fr.close();

        	
	        stmt.execute("set spill_node = "+ spill_node_r);
	        //stmt.execute("set limit_cost = "+ cost);
	        stmt.execute("set full_robustness = on");
	        stmt.execute("set FPCfull_robustness = on");
	        stmt.execute("set varyingJoins = "+varyingJoins);
	        stmt.execute("set JS_multiplier1 = "+ JS_multiplier1);
	        stmt.execute("set JS_multiplier2 = "+ JS_multiplier2);
	        stmt.execute("set robust_eqjoin_selec1 = "+ x);
	        stmt.execute("set robust_eqjoin_selec2 = "+ y);
	        stmt.execute(query);
	        //read the selectivity returned
            file = new File(cardinalityPath+"spill_cardinality");
        	fr = new FileReader(file);
        	br = new BufferedReader(fr);
        	val_r = Double.parseDouble(br.readLine());
        	br.close();
        	fr.close();

        	}
	        stmt.close();
	         c.close();
	         System.out.println("Postgres: left child rows "+val_l.doubleValue()+" with plan number "+plan);
	         System.out.println("Postgres: right child rows"+val_r.doubleValue()+" with plan number "+plan);
        	System.out.println("Postgres: rows of current node_id "+val.doubleValue()+" with plan number "+plan);
        	//System.out.println("Postgres: selectivity learnt "+val.doubleValue()+" with plan number "+plan);
        	double selectivity = val.doubleValue()/(val_l.doubleValue()*val_r.doubleValue());
        	double multiplier = 1;
        	if(dim==0)
        		multiplier = 150000/JS_multiplier1;
        	else
        		multiplier = 1500000/JS_multiplier2;
        	selectivity *= multiplier;
        	selectivity = findNearestSelectivity(selectivity);
        	assert(selectivity<=1):"error in multiplier";
        	System.out.println("Postgres: selectivity learnt  "+selectivity+" with plan number "+plan);
        	return selectivity; //has to have single line in the file
	       }
	       catch ( Exception e ) {
	           System.err.println( e.getClass().getName()+": "+ e.getMessage() );
	           return 0;
	         }

	}

	public boolean inFeasibleRegion(double x,double y) {
		if(remainingDim.size()==2)  //if all the dimensions are remaining then 
			return true; //dont prune within the contours
		if(minIndex[0]<=x && x<=maxIndex[0] && minIndex[1]<=y && y<=maxIndex[1])
			return true;
		else
			return false;
			
	}

	public static boolean clearCache() 
	{
		boolean success = false;
//		String start = "/home/dsladmin/Srinivas/AnshPG/bin/pg_ctl -D /home/dsladmin/Srinivas/AnshPG/tpch/ -w start";
//		String stop = "/home/dsladmin/Srinivas/AnshPG/bin/pg_ctl -D /home/dsladmin/Srinivas/AnshPG/tpch/ -w stop";

			String[] cmd = {
					"/bin/sh",
					"-c",
					"echo 3 | sudo tee /proc/sys/vm/drop_caches"
			};
			Process p;
			try 
			{
				
				Runtime r = Runtime.getRuntime();
//				p = r.exec(stop);
//				p.waitFor();
				p = r.exec(cmd);
				p.waitFor();
				BufferedReader reader = 
						new BufferedReader(new InputStreamReader(p.getInputStream()));

				String line = "";			
				while ((line = reader.readLine())!= null) 
				{
					if(line.equals("3"))
					{
						success = true;
					}
					System.out.println(line);
				}
				
//				p = r.exec(start);
//				p.waitFor();

			} catch (Exception e) 
			{
				e.printStackTrace();
			}
			
		
		return(success);
	}

	public double Simulated_Spilling(int planno, CostGreedy2D cg_obj, int dim, double budget) {
		// this method returns the selectivity
		if(remainingDim.size()==1)
			return 0;
		String funName = "spillBound: ";
		double err = 1.01;
		double planCost[] = cg_obj.getAllPlanCost(planno);
		if(dim==0){
			int x = getContourXPoint(0,budget,planCost);
			if(x==-1)
				System.out.println(funName+"ERROR");
			if(cost_matrix(x,0,planCost) > err*budget && x>0 )
				return selectivity[x-1];
			else{
				if(x==0)
					return selectivity[x];
				while(cost_matrix(x,0,planCost)<=err*budget){
					x++;
					if(x==resolution)
						break;
				}
				return selectivity[x-1];
			}
			
			}
		
		else{
			int y = getContourYPoint(0,budget,planCost);
			if(y==-1)
				System.out.println(funName+"ERROR");
			if(cost_matrix(0,y,planCost) > err*budget && y>1 )
				return selectivity[y-1];
			else{
				if(y==0)
					return selectivity[0];
				while(cost_matrix(0,y,planCost)<=err*budget){
					y++;
					if(y==resolution)
						break;
				}
				return selectivity[y-1];
			}

		}
			
	}
	

}


class point{
	double x;
	double y;
	int p_no;
	ArrayList<Integer> order;
	int value;
	static String apktPath; //= "/home/dsladmin/Srinivas/data/HQT102DR100/";
	point(double a, double b,int plan_no, ArrayList<Integer> remainingDim) throws IOException{
		this.x = a;
		this.y = b;
		this.p_no = plan_no;
		order =  new ArrayList<Integer>();
		
	    loadPropertiesFile();
		
		//populate the order list by reading from the plan files
		FileReader file = new FileReader(apktPath+"predicateOrder/"+plan_no+".txt");
	
	    BufferedReader br = new BufferedReader(file);
	    String s;
	    while((s = br.readLine()) != null) {
	    	//System.out.println(Integer.parseInt(s));
	    	// If the number is the remaining dimensions then only add it to the order
	    	value = Integer.parseInt(s);
	    	if(remainingDim.contains(value))
	    	{
	    		order.add(value);
	    	}
	    }
	    br.close();
	    file.close();

		
	}
	point(double a, double b,int plan_no, ArrayList<Integer> ordering, ArrayList<Integer> remainingDim) throws IOException{
		this.x = a;
		this.y = b;
		this.p_no = plan_no;
		order =  new ArrayList<Integer>();
		
		for(int i=0;i<ordering.size();i++){
			if(remainingDim.contains(ordering.get(i))){
				order.add(ordering.get(i));
			}
		}
		
		
	}

	public void loadPropertiesFile() {
		// TODO Auto-generated method stub
		Properties prop = new Properties();
		InputStream input = null;
	 
		try {
	 
			input = new FileInputStream("./src/Constants.properties");
	 
			// load a properties file
			prop.load(input);
	 
			// get the property value and print it out
			apktPath = prop.getProperty("apktPath");
	 
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	ArrayList<Integer> getOrdering(){
		return order;
	}
	double getX()
	{
		return this.x;
	}
	
	double getY(){
		return this.y;
	}
	int getPlanNumber(){
		return this.p_no;
	}
	int getLearningDimension(){
		if(order.isEmpty())
			System.out.println("ERROR: all dimensions learnt");
		return order.get(0);
	}
	void deleteLearningDimension(){
		order.remove(0);
	}
	void print(){
		System.out.print("("+x+","+y+") \t");
	}
}