package misc;
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


public class SB_FPC
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
	static int totalPlans;
	static int dimension;
	static int resolution;
	DataValues[] data;
	static int totalPoints;
	double selectivity[];

	static ArrayList<Integer> remainingDim;
	static ArrayList<ArrayList<Integer>> allPermutations = new ArrayList<ArrayList<Integer>>();
	static ArrayList<point_generic> all_contour_points = new ArrayList<point_generic>();
	
	static ArrayList<Integer> learntDim = new ArrayList<Integer>();
	static HashMap<Integer,Integer> learntDimIndices = new HashMap<Integer,Integer>();
	static HashMap<Integer,ArrayList<point_generic>> ContourPointsMap = new HashMap<Integer,ArrayList<point_generic>>();

	
	//The following parameters has to be set manually for each query
	

	static String QTName = "DSQT264DR20_E";
	static String apktPath= "/home/dsladmin/Srinivas/data/DSQT264DR20_E/DSQT264DR20_E.apkt";
	static String plansPath = "/home/dsladmin/Srinivas/data/DSQT264DR20_E/";
	
	static int cur_contour_number=0;
	static String qtName ;
	static String varyingJoins;
	static String query;
	static String cardinalityPath;
	
	static point_generic []maxPoints;
	double  minIndex [];
	double  maxIndex [];

	double[] actual_sel;
	
	point[][] points_list;
	 static plan[] plans_list;
	
	public static void main(String args[]) throws IOException, SQLException
	{
		SB_FPC obj = new SB_FPC();
		System.out.println("\nStarted!!\n");
		ADiagramPacket gdp = obj.getGDP(new File(apktPath));
		
		obj.readpkt(gdp); 
		obj.loadPlans();
	//	obj.loadSelectivity();
	//	System.out.println("\nlength of plans list="+plans_list.length+"\n");
	//	obj.dofpc();
		
		
		int i;
		double h_cost = obj.getOptimalCost(totalPoints-1);
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
		
		//System.out.println("\nContour_count="+contour_cnt);
		
		while(cost < 2*h_cost)
		{
			cur_contour_number=cur_contour_number+1;
			if(cost>h_cost)
				cost = h_cost;
			System.out.println("---------------------------------------------------------------------------------------------\n");
			System.out.println("Contour "+i+" cost : "+cost+"\n");
			all_contour_points.clear();
			for(ArrayList<Integer> order:allPermutations){
				System.out.println("Entering the order"+order);
				learntDim.clear();
				learntDimIndices.clear();
				obj.getContourPoints(order,cost);
			}
			//Settings
			//writeContourPointstoFile(i);
			int size_of_contour = all_contour_points.size();
			ContourPointsMap.put(i, new ArrayList<point_generic>(all_contour_points)); //storing the contour points
			System.out.println("Size of contour"+size_of_contour );
				cost = cost*2;
				i = i+1;
		}
		System.out.println("\nSize :----");
		for(i=1;i<=ContourPointsMap.size();i++)
		{
			int size;
			size=ContourPointsMap.get(i).size();
			System.out.println("Contour "+i+" ="+size);
		}
		int[] index = new int[dimension];
		int OptPlan, cur_category, category,loc,CurFpcPlan;
		maxPoints= new point_generic[dimension];
		double NewFpcCost, CurFpcCost, CurOptCost,error;
		double OptCost;
		
//		System.out.println("\n Contour 4 Coming up!!-----------------------------------------\n");
//		for(i=0;i<ContourPointsMap.get(4).size();i++)
//		{
//			maxPoints[0]=ContourPointsMap.get(4).get(i);
//			index=maxPoints[0].get_point_Index();
//			for(int k=0;k<dimension;k++)
//			{
//				System.out.print(index[k]+",");
//			}
//			System.out.println();
//		}
//		System.out.println("\n-------------------------------------------------------\n");
			
		
		
		for(i=1;i<=ContourPointsMap.size();i++)
		{
			System.out.println("\nContour "+i+ ", cost = "+ContourPointsMap.get(i).get(2).getopt_cost());
			for(int j=0;j<dimension;j++)
			{
				
				
				maxPoints[j]=findMaxPoint(ContourPointsMap.get(i),j);
				if(maxPoints[j] == null)
				{
					System.out.println("\nfindMaxPoint returned NULL\n");
				}
				index=maxPoints[j].get_point_Index();
				System.out.print(" Max "+j+"= ");
				for(int k=0;k<dimension;k++)
				{
					System.out.print(index[k]+",");
				}
				OptPlan=maxPoints[j].get_plan_no();
				OptCost=maxPoints[j].get_cost();
				System.out.print("  OptPlan="+ OptPlan+", OptCost="+OptCost);
				
				//Getting best FPC Plan!
				cur_category = plans_list[OptPlan].getcategory(remainingDim);
				if(cur_category==j)
				{
					System.out.print(", Good Guy!\n");
					continue;
				}
				maxPoints[j].putfpc_cost(-1.0);
				maxPoints[j].putfpc_plan(-1);
				for(int k=0;k<nPlans;k++)
				{
					if(k==OptPlan)
						continue;
					category=plans_list[k].getcategory(remainingDim);
					if(category == j)
					{
						loc= getIndex(index,resolution);
						NewFpcCost=AllPlanCosts[k][loc];
						CurFpcCost=maxPoints[j].getfpc_cost();
						CurOptCost=maxPoints[j].getopt_cost();
						
						if(CurFpcCost == -1 || CurFpcCost > NewFpcCost)
						{
							maxPoints[j].putfpc_cost(NewFpcCost);
							maxPoints[j].putfpc_plan(k);
							error=((NewFpcCost-CurOptCost)/CurOptCost)*100;
							maxPoints[j].putpercent_err(error);
							
						}
					}
				}
				CurFpcPlan=maxPoints[j].getfpc_plan();
				error=maxPoints[j].getpercent_err();
				CurFpcCost=maxPoints[j].getfpc_cost();
				System.out.print(", FPC Plan="+CurFpcPlan+", FPCCost="+CurFpcCost+", Error="+error);
				// End getting FPC Plan
				
				System.out.println();
				
			}
						
			//Print the maxPoints.
		}
		System.out.println("\n------------------------------------------------");
		index[0]=29;
		index[1]=29;
		index[2]=1;
		loc=getIndex(index,resolution);
		int spill_dim = 1;
		System.out.println("Spill Dimension="+spill_dim);
		System.out.print("Point=[");
		for(i=0;i<dimension;i++)
		{
			System.out.print(index[i]+",");
		}
		System.out.print("]\n");
		
		
		for(i=0;i<nPlans;i++)
		{
			category=plans_list[i].getcategory(remainingDim);
			if(category == spill_dim)
			{
				CurFpcCost=AllPlanCosts[i][loc];
				System.out.println("Plan "+i+", with Fpc cost="+CurFpcCost+"\n");
			}
			
		}
		
		
		
//		for(int i=0;i<totalPlans;i++)
//		{
//			int temp_cat=obj.plans_list[i].getcategory(remainingDim);
//			System.out.println("\nPlan Number:"+i+"\t, category : "+temp_cat+"\n");
//		}
		
		System.out.println("\nEnd "+QTName+"\n");
	}	
	double getOptimalCost(int index)
	{
		return this.OptimalCost[index];
	}
	static point_generic findMaxPoint(ArrayList<point_generic> contour_points, int dim_index)
	{
		int []index = new int[dimension];
		int [] max_index= new int [dimension];
		double min_error=100000.0;
		point_generic max_point=contour_points.get(0);
		max_index = max_point.get_point_Index();
		
		
		int OptPlan, cur_category, category,loc,CurFpcPlan;
		double NewFpcCost, CurFpcCost, CurOptCost,error;
		
		point_generic cur;
		ArrayList<point_generic> MaxList = new ArrayList<point_generic>();
		
		
		for(int i=1;i<contour_points.size();i++)
		{
			cur=contour_points.get(i);
			index=cur.get_point_Index();
			if(index[dim_index] > max_index[dim_index])
			{
				max_point = contour_points.get(i);
				max_index = max_point.get_point_Index();
			}
		}
		//Put all points having index[dim_index]=max_index[dim_index] into a list;
		for(int i=0;i<contour_points.size();i++)
		{
			cur=contour_points.get(i);
			index=cur.get_point_Index();
			if(index[dim_index] == max_index[dim_index])
			{
				MaxList.add(cur);
			}
		}
		//Iterating over the MaxList to find the lowest cost FPC.
		for(int i=0;i<MaxList.size();i++)
		{
			cur=MaxList.get(i);
			index=cur.get_point_Index();
			OptPlan=cur.get_plan_no();
			
			cur_category = plans_list[OptPlan].getcategory(remainingDim);
			if(cur_category==dim_index)
			{
				return cur;
			}
			for(int j=0;j < nPlans;j++)
			{
				if(j==OptPlan)
					continue;
				category=plans_list[j].getcategory(remainingDim);
				if(category == dim_index)
				{
					loc= getIndex(index,resolution);
					NewFpcCost=AllPlanCosts[j][loc];
					CurFpcCost=cur.getfpc_cost();
					CurOptCost=cur.getopt_cost();
					
					if(CurFpcCost == -1 || CurFpcCost > NewFpcCost)
					{
						cur.putfpc_cost(NewFpcCost);
						cur.putfpc_plan(j);
						error=((NewFpcCost-CurOptCost)/CurOptCost)*100;
						cur.putpercent_err(error);
						
					}
				}
			}
			
		}
		
		for(int i=0;i<MaxList.size();i++)
		{
			cur=MaxList.get(i);
			index=cur.get_point_Index();
			error = cur.getpercent_err();
			if(error < min_error)
			{
				min_error = error;
				max_point = cur;
			}
		}
		
	/*
	 * 1. Get the maxindex!
	 * 2. Put all points having index[dim_index]=max_index into a list;
	 * 3. Iterate over the list
	 * 4. Iterate over all the plans, get the best FPC (with dim) at the point. Maintain the point with lowest FPC Cost.
	 * 
	 *  5.Return the point
	 *  
	 *  ---> Count the FPC/Opt error while doing the above, and show it!!
	 */
		return max_point;
	}
	int getPlanNumber(int x, int y, int z)
	{
		 int arr[] = {x,y,z};
		int index = getIndex(arr,resolution);
		return plans[index];
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
	
	 private int factorial(int num) {

		 int factorial = 1;
		for(int i=num;i>=1;i--){
			factorial *= i; 
		}
		return factorial;
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
						if(!pointAlreadyExist(arr)){ //check if the point already exist
							point_generic p;
							
							/*
							 * The following If condition checks whether any earlier point in all_contour_points 
							 * had the same plan. If so no need to open the .../predicateOrder/plan.txt again
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
							if(!pointAlreadyExist(arr)){ //check if the point already exist
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
		
		int getPlanNumber_generic(int arr[])
		{
			int index = getIndex(arr,resolution);
			return plans[index];
		}
	 
		private boolean pointAlreadyExist(int[] arr) {

			return false;
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

		
	void dofpc()
	{
		int i,j,k;
		int category, OptPlan, cur_category;
		double CurFpcCost, CurOptCost, NewFpcCost, error;
		int [] index;
		double max_err=-1;
		index = new int [dimension];
		int loc;
		//Left Line-------------------------
		
		i=0;
			for(j=0;j<resolution;j++)
			{
				
				OptPlan = points_list[i][j].getopt_plan();
				cur_category = plans_list[OptPlan].getcategory(remainingDim);
				if(cur_category==1)
					continue;
				for(k=0;k<nPlans;k++)
				{
					if(k==OptPlan)
						continue;
					category=plans_list[k].getcategory(remainingDim);
					if(category == 1)
					{
						index[0]=i;
						index[1]=j;
						loc= getIndex(index,resolution);
						NewFpcCost=AllPlanCosts[k][loc];
						CurFpcCost=points_list[i][j].getfpc_cost();
						CurOptCost=points_list[i][j].getopt_cost();
						
						if(CurFpcCost == -1 || CurFpcCost > NewFpcCost)
						{
							points_list[i][j].putfpc_cost(NewFpcCost);
							points_list[i][j].putfpc_plan(k);
							error=((NewFpcCost-CurOptCost)/CurOptCost)*100;
							points_list[i][j].putpercent_err(error);
							//if(error>max_err)
							//	max_err=error;
						}
					}
				}
			}
		
		
		//Top Line----------------------------
			j=resolution-1;
		for(i=0;i<resolution;i++)
		{
			OptPlan = points_list[i][j].getopt_plan();
			cur_category = plans_list[OptPlan].getcategory(remainingDim);
			if(cur_category==1)
				continue;
			for(k=0;k<nPlans;k++)
			{
				if(k==OptPlan)
					continue;
				category=plans_list[k].getcategory(remainingDim);
				
				if(category == 1)
				{
					index[0]=i;
					index[1]=j;
					loc= getIndex(index,resolution);
					NewFpcCost=AllPlanCosts[k][loc];
					CurFpcCost=points_list[i][j].getfpc_cost();
					CurOptCost=points_list[i][j].getopt_cost();
					
					if(CurFpcCost == -1 || CurFpcCost > NewFpcCost)
					{
						points_list[i][j].putfpc_cost(NewFpcCost);
						points_list[i][j].putfpc_plan(k);
						error=((NewFpcCost-CurOptCost)/CurOptCost)*100;
						points_list[i][j].putpercent_err(error);
					//	if(error>max_err)
						//	max_err=error;
					}
				}
			}
		}
		
		
		//Right Line-----------------------------
		i=resolution-1;
		for(j=0;j<resolution;j++)
		{
			OptPlan = points_list[i][j].getopt_plan();
			cur_category = plans_list[OptPlan].getcategory(remainingDim);
			if(cur_category==0)
				continue;
			for(k=0;k<nPlans;k++)
			{
				
				if(k==OptPlan)
					continue;
				category=plans_list[k].getcategory(remainingDim);
				
				if(category == 0)
				{
					index[0]=i;
					index[1]=j;
					loc= getIndex(index,resolution);
					NewFpcCost=AllPlanCosts[k][loc];
					CurFpcCost=points_list[i][j].getfpc_cost();
					CurOptCost=points_list[i][j].getopt_cost();
					
					if(CurFpcCost == -1 || CurFpcCost > NewFpcCost)
					{
						points_list[i][j].putfpc_cost(NewFpcCost);
						points_list[i][j].putfpc_plan(k);
						error=((NewFpcCost-CurOptCost)/CurOptCost)*100;
						points_list[i][j].putpercent_err(error);
					//	if(error>max_err)
					//		max_err=error;
					}
				}
			}
		}
		//bottom Line-----------------------------
		j=0;
		for(i=0;i<resolution;i++)
		{
			
			OptPlan = points_list[i][j].getopt_plan();
			cur_category = plans_list[OptPlan].getcategory(remainingDim);
			if(cur_category==0)
				continue;
			
			for(k=0;k<nPlans;k++)
			{
				if(k==OptPlan)
					continue;
				category=plans_list[k].getcategory(remainingDim);
				
				if(category == 0)
				{
					index[0]=i;
					index[1]=j;
					loc= getIndex(index,resolution);
					NewFpcCost=AllPlanCosts[k][loc];
					CurFpcCost=points_list[i][j].getfpc_cost();
					CurOptCost=points_list[i][j].getopt_cost();
					
					if(CurFpcCost == -1 || CurFpcCost > NewFpcCost)
					{
						points_list[i][j].putfpc_cost(NewFpcCost);
						points_list[i][j].putfpc_plan(k);
						error=((NewFpcCost-CurOptCost)/CurOptCost)*100;
						points_list[i][j].putpercent_err(error);
					//	if(error>max_err)
					//		max_err=error;
					}
				}
			}
		}
		for( i=0;i<resolution;i++)
		{
			for(j=0;j<resolution;j++)
			{
				error=points_list[i][j].getpercent_err();
				if(error>max_err)
					max_err = error;
			}
		}
		
		System.out.println("\nMax_error="+max_err+"\n");
		
		i=0;
		System.out.println("\n---------------------------------------Left Line---------------------------------------------------------------\n");	
		for(j=0;j<resolution;j++)
		{
			print_details(i,j);
			
		}
		
		j=resolution-1;
		System.out.println("\n---------------------------------------Top Line---------------------------------------------------------------\n");	
		for(i=0;i<resolution;i++)
		{
		//	error=points_list[i][j].getpercent_err();
		//	System.out.println("\n"+error);
			print_details(i,j);
		}
		
		i=resolution-1;
		System.out.println("\n---------------------------------------Right Line---------------------------------------------------------------\n");	
		for(j=0;j<resolution;j++)
		{
		//	error=points_list[i][j].getpercent_err();
		//	System.out.println("\n"+error);
			print_details(i,j);
		}
		
		j=0;
		System.out.println("\n---------------------------------------Bottom Line---------------------------------------------------------------\n");	
		for(i=0;i<resolution;i++)
		{
			//error=points_list[i][j].getpercent_err();
			//System.out.println("\n"+error);
			print_details(i,j);
		}
	}
	
	void print_details(int i, int j)
	{
		double error,FpcCost, OptCost;
		int FpcPlan, OptPlan;
		error=points_list[i][j].getpercent_err();
		FpcPlan=points_list[i][j].getfpc_plan();
		FpcCost=points_list[i][j].getfpc_cost();
		OptPlan=points_list[i][j].getopt_plan();
		OptCost=points_list[i][j].getopt_cost();
		System.out.println("\ni="+i+",j="+j+", OptPlan="+OptPlan+", OptCost="+OptCost+", FpcPlan="+FpcPlan+", FpcCost="+FpcCost+", percent_error="+error);
	}
	void loadPlans() throws NumberFormatException, IOException
	{
	
		plans_list = new plan[totalPlans];
		for(int i=0;i<totalPlans;i++)
		{
			plans_list[i]=new plan(i);
			
		//	int temp_cat=plans_list[i].getcategory(remainingDim);
		//	System.out.println("\nPlan Number:"+i+"\t, category : "+temp_cat+"\n");
		}
	//	System.out.println("\nSelectivity="+selectivity[34]+"\n");
	}
	void loadSelectivity()
	{
		String funName = "loadSelectivity: ";
		System.out.println(funName+" Resolution = "+resolution);
		double sel;
		this.selectivity = new double [resolution];
		
		if(resolution == 10)
		{
			selectivity[0] = 0.0005;	selectivity[1] = 0.005;selectivity[2] = 0.01;	selectivity[3] = 0.02;
			selectivity[4] = 0.05;		selectivity[5] = 0.10;	selectivity[6] = 0.20;	selectivity[7] = 0.40;
			selectivity[8] = 0.60;		selectivity[9] = 0.95; 
		}
		if(resolution == 20)
		{
			selectivity[0] = 0.000005;		selectivity[1] = 0.00005;		selectivity[2] = 0.0005;	selectivity[3] = 0.002;
			selectivity[4] = 0.005;		selectivity[5] = 0.008;		selectivity[6] = 0.01;		selectivity[7] = 0.02;
			selectivity[8] = 0.03;			selectivity[9] = 0.04;			selectivity[10] = 0.05;	selectivity[11] = 0.08;
			selectivity[12] = 0.10; 		selectivity[13] = 0.15;		selectivity[14] = 0.20;	selectivity[15] = 0.30;
			selectivity[16] = 0.40;		selectivity[17] = 0.60;		selectivity[18]=0.80;		selectivity[19] = 0.99;
		}
		if(resolution == 30)
		{
			selectivity[0] = 0.0005;  selectivity[1] = 0.0008;	selectivity[2] = 0.001;	selectivity[3] = 0.002;
			selectivity[4] = 0.004;   selectivity[5] = 0.006;	selectivity[28] = 0.008;	selectivity[29] = 0.01;
			selectivity[8] = 0.03;	selectivity[9] = 0.05;
			selectivity[10] = 0.07;	selectivity[11] = 0.1;	selectivity[12] = 0.15;	selectivity[13] = 0.20;
			selectivity[14] = 0.25;	selectivity[15] = 0.30;	selectivity[16] = 0.35;	selectivity[17] = 0.40;
			selectivity[18] = 0.45;	selectivity[19] = 0.50;	selectivity[20] = 0.55;	selectivity[21] = 0.60;
			selectivity[22] = 0.65;	selectivity[23] = 0.70;	selectivity[24] = 0.75;	selectivity[25] = 0.80;
			selectivity[26] = 0.85;	selectivity[27] = 0.90;	selectivity[28] = 0.95;	selectivity[29] = 0.99;
		}
		
		if(resolution == 100){
			selectivity[0] = 0.000514; 	selectivity[1] = 0.000543; 	selectivity[2] = 0.000576; 	selectivity[3] = 0.000611; 	selectivity[4] = 0.000648;
			selectivity[5] = 0.000689; 	selectivity[6] = 0.000733; 	selectivity[7] = 0.000781; 	selectivity[8] = 0.000833; 	selectivity[9] = 0.000890;
			selectivity[10] = 0.000951; 	selectivity[11] = 0.001017; 	selectivity[12] = 0.001088; 	selectivity[13] = 0.001165; 	selectivity[14] = 0.001249;
			selectivity[15] = 0.001340; 	selectivity[16] = 0.001438; 	selectivity[17] = 0.001545; 	selectivity[18] = 0.001660; 	selectivity[19] = 0.001785;
			selectivity[20] = 0.001920; 	selectivity[21] = 0.002067; 	selectivity[22] = 0.002225; 	selectivity[23] = 0.002397; 	selectivity[24] = 0.002583;
			selectivity[25] = 0.002784; 	selectivity[26] = 0.003003; 	selectivity[27] = 0.003239; 	selectivity[28] = 0.003495; 	selectivity[29] = 0.003772;
			selectivity[30] = 0.004072; 	selectivity[31] = 0.004397; 	selectivity[32] = 0.004749; 	selectivity[33] = 0.005131; 	selectivity[34] = 0.005544;
			selectivity[35] = 0.005991; 	selectivity[36] = 0.006475; 	selectivity[37] = 0.007000; 	selectivity[38] = 0.007568; 	selectivity[39] = 0.008183;
			selectivity[40] = 0.008849; 	selectivity[41] = 0.009571; 	selectivity[42] = 0.010352; 	selectivity[43] = 0.011198; 	selectivity[44] = 0.012115;
			selectivity[45] = 0.013108; 	selectivity[46] = 0.014183; 	selectivity[47] = 0.015347; 	selectivity[48] = 0.016608; 	selectivity[49] = 0.017973;
			selectivity[50] = 0.019452; 	selectivity[51] = 0.021054; 	selectivity[52] = 0.022788; 	selectivity[53] = 0.024667; 	selectivity[54] = 0.026701;
			selectivity[55] = 0.028904; 	selectivity[56] = 0.031291; 	selectivity[57] = 0.033875; 	selectivity[58] = 0.036674; 	selectivity[59] = 0.039705;
			selectivity[60] = 0.042987; 	selectivity[61] = 0.046542; 	selectivity[62] = 0.050392; 	selectivity[63] = 0.054562; 	selectivity[64] = 0.059078;
			selectivity[65] = 0.063968; 	selectivity[66] = 0.069265; 	selectivity[67] = 0.075001; 	selectivity[68] = 0.081213; 	selectivity[69] = 0.087940;
			selectivity[70] = 0.095227; 	selectivity[71] = 0.103117; 	selectivity[72] = 0.111663; 	selectivity[73] = 0.120918; 	selectivity[74] = 0.130942;
			selectivity[75] = 0.141797; 	selectivity[76] = 0.153553; 	selectivity[77] = 0.166285; 	selectivity[78] = 0.180074; 	selectivity[79] = 0.195007;
			selectivity[80] = 0.211180; 	selectivity[81] = 0.228695; 	selectivity[82] = 0.247664; 	selectivity[83] = 0.268207; 	selectivity[84] = 0.290455;
			selectivity[85] = 0.314550; 	selectivity[86] = 0.340645; 	selectivity[87] = 0.368905; 	selectivity[88] = 0.399512; 	selectivity[89] = 0.432658;
			selectivity[90] = 0.468556; 	selectivity[91] = 0.507433; 	selectivity[92] = 0.549537; 	selectivity[93] = 0.595136; 	selectivity[94] = 0.644519;
			selectivity[95] = 0.698001; 	selectivity[96] = 0.775922; 	selectivity[97] = 0.858651; 	selectivity[98] = 0.926586; 	selectivity[99] = 0.990160;
		}
		
		}
	
	
	void readpkt(ADiagramPacket gdp) throws IOException
	{
		String funName="readpkt";
		//ADiagramPacket gdp = getGDP(new File(pktPath));
		totalPlans = gdp.getMaxPlanNumber();
		dimension = gdp.getDimension();
		resolution = gdp.getMaxResolution();
		data = gdp.getData();
		totalPoints = (int) Math.pow(resolution, dimension);
		//System.out.println("\nthe total plans are "+totalPlans+" with dimensions "+dimension+" and resolution "+resolution);

		assert (totalPoints==data.length) : "Data length and the resolution didn't match !";
		maxPoints = new point_generic[dimension];
		this.plans = new int [data.length];
		this.OptimalCost = new double [data.length]; 
		this.points_list = new point[resolution][resolution];
		int [] index = new int[dimension];
		
		
		this.remainingDim = new ArrayList<Integer>();
		for(int i=0;i<dimension;i++)
			remainingDim.add(i);
		
		
		for (int i = 0;i < data.length;i++)
		{
			index=getCoordinates(dimension,resolution,i);
			points_list[index[0]][index[1]] = new point(index[0],index[1],data[i].getPlanNumber(),remainingDim);
			points_list[index[0]][index[1]].putopt_cost(data[i].getCost());
			this.OptimalCost[i]= data[i].getCost();
			this.plans[i] = data[i].getPlanNumber();
			//System.out.println("At "+i+" plan is "+plans[i]);
			//System.out.println("Plan Number ="+plans[i]+"\n");
			//	System.out.println("\nOptimal_cost:["+i+"] ="+OptimalCost[i]+"\n");
		}

		

		 
			minIndex = new double[dimension];
			maxIndex = new double[dimension];

			// ------------------------------------- Read pcst files
			nPlans = totalPlans;
			AllPlanCosts = new double[nPlans][totalPoints];
			//costBouquet = new double[total_points];
			double CurFpcCost,CurOptCost;
			double error,max_err=-1;
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
	public static int getIndex(int[] index,int res)
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


class plan{
	ArrayList<Integer> order;
	int category;
	int value;

	static String plansPath = "/home/dsladmin/Srinivas/data/DSQT264DR20_E/";
	plan(int p_no) throws NumberFormatException, IOException{
		order =  new ArrayList<Integer>();
		FileReader file = new FileReader(plansPath+"predicateOrder/"+p_no+".txt");
		
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
	int getcategory(ArrayList<Integer> remainingDim){
		int len = order.size();
		int cur_dim;
		for(int i=0;i<len;i++)
		{
			cur_dim = order.get(i);
			if(remainingDim.contains(i))
			{
				category=cur_dim;
				return category;
			}
		}
		System.out.println("\nError in getcategory\n");
		return -1;
		
	}
}


	class point{
		int x;
		int y;
		int opt_plan;
		double opt_cost;
		
		int fpc_plan;
		double fpc_cost;
		double percent_err;
		
		ArrayList<Integer> order;
		int value;

		static String plansPath = "/home/dsladmin/Srinivas/data/DSQT264DR20_E/";
		point(int a, int b,int opt_plan_no, ArrayList<Integer> remainingDim) throws IOException{
			this.x = a;
			this.y = b;
			this.opt_plan = opt_plan_no;
			order =  new ArrayList<Integer>();
			
			opt_cost = -1;
			fpc_plan = -1;
			fpc_cost = -1;
			percent_err=-1;
			
			
			//populate the order list by reading from the plan files
			FileReader file = new FileReader(plansPath+"predicateOrder/"+opt_plan+".txt");
		
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
		double getX()
		{
			return this.x;
		}
		
		double getY(){
			return this.y;
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
		
		int getopt_plan(){
			return this.opt_plan;
		}
		double getopt_cost(){
			return this.opt_cost;
		}
		void putopt_cost(double cost){
			this.opt_cost=cost;
		}
		
		double getpercent_err(){
			return this.percent_err;
		}
		void putpercent_err(double p_err){
			this.percent_err=p_err;
		}
		
		
		
		int getLearningDimension(){
			if(order.isEmpty())
			{
				System.out.println("ERROR: all dimensions learnt");
				return -1;
			}
			return order.get(0);
		}
		void deleteLearningDimension(){
			order.remove(0);
		}
		void print(){
			System.out.print("("+x+","+y+") \t");
		}
		
	}
	
	class point_generic
	{
		int dimension;
		
		ArrayList<Integer> order;
		ArrayList<Integer> storedOrder;
		int value;
		
		int opt_plan=-1;
		double opt_cost=-1.0;
		
		int fpc_plan=-1;
		double fpc_cost=-1.0;
		double percent_err=-1.0;
		
		static String plansPath = "/home/dsladmin/Srinivas/data/DSQT264DR20_E/predicateOrder/";
		
		int [] dim_values;
		point_generic(int arr[], int num, double cost,ArrayList<Integer> remainingDim) throws  IOException{
			
			//loadPropertiesFile();
			dimension=4;
		//	System.out.println();
			dim_values = new int[dimension];
		//	System.out.println("\n In points_generic - Dimension="+dimension+"\n");
			for(int i=0;i<dimension;i++){
				dim_values[i] = arr[i];
			//	System.out.print(arr[i]+",");
			}
		//	System.out.println("   having cost = "+cost+" and plan "+num);
			this.opt_cost=cost;
			this.opt_plan=num;
			
			
			order =  new ArrayList<Integer>();
			storedOrder = new ArrayList<Integer>();
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
		point_generic(int arr[], int num, double cost,ArrayList<Integer> remainingDim,ArrayList<Integer> predicateOrder ) throws  IOException{
			
		//	loadPropertiesFile();
			dimension=4;
	//	System.out.println();
			dim_values = new int[dimension];
			for(int i=0;i<dimension;i++){
				dim_values[i] = arr[i];
	//			System.out.print(arr[i]+",");
			}
			//System.out.println("\n ----");
	//		System.out.println("   having cost = "+cost+" and plan "+num);
			this.opt_plan = num;
			this.opt_cost = cost;
			
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
			return opt_plan;
		}
		
		public double get_cost(){
			return opt_cost;
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
		
		int getopt_plan(){
			return this.opt_plan;
		}
		double getopt_cost(){
			return this.opt_cost;
		}
		void putopt_cost(double cost){
			this.opt_cost=cost;
		}
		
		double getpercent_err(){
			return this.percent_err;
		}
		void putpercent_err(double p_err){
			this.percent_err=p_err;
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
		
//		public void loadPropertiesFile() {
//
//			Properties prop = new Properties();
//			InputStream input = null;
//		 
//			try {
//		 
//				input = new FileInputStream("./src/Constants.properties");
//		 
//				// load a properties file
//				prop.load(input);
//		 
//				// get the property value and print it out
//				plansPath = prop.getProperty("apktPath");
//				plansPath = plansPath+"predicateOrder/";
//				dimension = Integer.parseInt(prop.getProperty("dimension"));
//			} catch (IOException ex) {
//				ex.printStackTrace();
//			}
//		}

	}	


	