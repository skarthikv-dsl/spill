import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
//import java.util.Queue;
//import java.io.LineNumberReader;
//import java.io.FileReader;

import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;
public class MulDContourPlansReduction 
{
	int SANITY_CONSTANT = 0;
	int big_cnt =0;
	//private static 	String bouqPath;
	int X = 0;
	int Z = 1; 
	int Y = 2;
	int XY = 3;
	int YZ = 4;
	int XZ = 5;
	
	public static double QDIST_SKEW_10 = 2.0;
	public static double QDIST_SKEW_30 = 1.33;
	public static double QDIST_SKEW_100 = 1.083;
	public static double QDIST_SKEW_300 = 1.027;
	public static double QDIST_SKEW_1000 = 1.00808;

	
	int resolution;
	int dimension;
	int totalPlans;
	int total_points;
	DataValues[] data;
	double AllPlanCosts[][];
	int newOptimalPlan[];
	int allPlanSet[];
	int contourLocation[];
	double threshold = 20.0;
	double OptimalCost [];
	double selectivity[];
	double alpha = 1.388;
	double ALPHA_COST ;
	int  ZViolationGrid[];
	int XViolatoinGrid[];
	int YViolationGrid[];
	List<Integer> list = new ArrayList<Integer>();
	//double endpoint[];
	
	int UNI = 0;
	int EXP = 1;
	int DISTRIBUTION;
	
	public static void main(String args[])
	{
	//	String bouquetPath = "C:\\Lohit\\work_1\\H2DQT21_30";
// --> This is running on 12:28 pm July 5.
	//String bouquetPath = "C:\\Lohit\\SQLSERVER_APKT\\SQL SERVER_H3DQT7_100";
		
	//	String bouquetPath = "C:\\Lohit\\H3DQT2Sample_10";
	//	String bouquetPath = "C:\\Lohit\\PG_APKT\\POSTGRES_H3DQT5_100";
		
		String QT_NAME = "POSTGRES_H3DQT7_100";
		String bouquetPath = "/home/dsladmin/Lohit/PG_APKT/UNI/3D_100/"+QT_NAME;
		
	//	String QT_NAME = "SQL SERVER_H3DQT7_100";
	//	String bouquetPath = "C:\\Lohit\\SQLSERVER_APKT\\"+QT_NAME;
		
		System.out.print("\n"+QT_NAME+",");
		MulDContourPlansReduction obj = new MulDContourPlansReduction();
		obj.bouqData(bouquetPath);
	}
	void bouqData(String bouquetPath)
	{
		DISTRIBUTION = EXP;
		
		
		double startpoint = 0.0;
		double endpoint = 1.0;
		
		ALPHA_COST = Math.pow(alpha,2);
		
		ADiagramPacket gdp = getGDP(new File( bouquetPath +  ".apkt"));
		totalPlans = gdp.getMaxPlanNumber();
		dimension = gdp.getDimension();
		resolution = gdp.getMaxResolution();
		data = gdp.getData();
		total_points = (int) Math.pow(resolution, dimension);
		//System.out.println("\nthe total plans are "+totalPlans+" with dimensions "+dimension+" and resolution "+resolution);
		
		assert (total_points==data.length) : "Data length and the resolution didn't match !";
		
	//	System.out.println("\n#"+Thread.currentThread().getStackTrace()[1].getLineNumber());
		double biggest_cost= 0;
		double [] OptimalCost = new double [data.length]; 
		for (int i = 0;i < data.length;i++)
		{
			
			OptimalCost[i]= data[i].getCost();
			if(OptimalCost[i] > biggest_cost)
				biggest_cost = OptimalCost[i];
		}
	//	System.out.println("\nBiggest cost :"+biggest_cost+"\n");
		
		double ratio = OptimalCost[total_points -1]/OptimalCost[0];
		
		System.out.println("\n MIN = "+OptimalCost[0]+"\tMAX:"+OptimalCost[total_points -1]+"\tcmax_cmin_ration ="+ratio+"\n");
		
	//	System.exit(1);
		getViolationList(total_points, OptimalCost);
		
		
		//System.out.println("\n#"+Thread.currentThread().getStackTrace()[1].getLineNumber());
	//	OptimalCost = Smoothen(OptimalCost);
	//	System.out.println("\n\n###############################################################\n\n;");
	//	OptimalCost = Smoothen(OptimalCost);
		//float[] picsel = new float[resolution];

	//	Smoothen();
	//	System.out.println("\nSmoothen Function ends.\n");
		double [] selectivity = new double [resolution];
		
		if(DISTRIBUTION == UNI)
		{
			System.out.println("\n DISTRIBUTION : UNI \n");
			double sel;
			sel= startpoint + ((endpoint - startpoint)/(2*resolution));
			
			for(int i=0;i<resolution;i++){
				
				selectivity[i] = sel;
				System.out.println("\nSelectivity["+i+"] = "+selectivity[i]+"\t");
				sel += ((endpoint - startpoint)/resolution);
			}
		}
		else if(DISTRIBUTION == EXP)
		{
			System.out.println("\n DISTRIBUTION : EXP \n");
			double diff =0;
			double r=1.0;
		//	double [] selectivity = new double [resolution];
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
				System.out.println("\n Sel["+(i-1)+"]="+selectivity[i-1]);
				curval*=r;
				if(i!=popu)
					sum+=(curval * (endpoint - startpoint));
				else
					sum+=(curval * (endpoint - startpoint))/2;
			}
		}
		else
		{
			System.out.println("\nProblem with DISTRIBUTION\n");
			System.exit(1);
		}
		
		
		//System.out.println("\nLength of selectivity array ="+selectivity.length+"\n");
		//Checking Violations !
		double [] curr_sel = new double [3];
		int [] curr_index = new int [3];
		int [] new_index = new int [3];
		
		int [] low_index = new int [3];
		int [] high_index = new int [3];
		
		
		int [] ZViolationGrid = new int [total_points];
		int [] XViolationGrid = new int [total_points];
		int [] YViolationGrid = new int [total_points];
		
		int [] XYViolationGrid = new int [total_points];
		int [] YZViolationGrid = new int [total_points];
		int [] XZViolationGrid = new int [total_points];
		
		int [] Endpoint = new int [3];
		
		int new_pos,low_pos, high_pos;
		double c1,c2,s1,s2;
		double x1,y1,z1,x2,y2,z2;
		double approx_cost;
		double CostAtNewPos;
		
		//System.out.println("\n#"+Thread.currentThread().getStackTrace()[1].getLineNumber());
		
		for(int curr_pos=0;curr_pos<total_points;curr_pos++)
		{
			if(curr_pos%10000 == 0)
				System.out.println("\ncurr_pos ="+curr_pos);
			/*
			 * For each point in the grid, look at the nearest point at alpha*x and alpha*y 
			 * and put it into three 2D array which will have 1 or 0 corresponding to vertical,horizontal or Y.
			 * option : X, Z, Y
			 */
			//indx = findNearest(final_selectivity[2],index[2],option);
			
			if (list.indexOf(curr_pos) != -1)
				continue;
			
			curr_index = getCoordinates(dimension, resolution, curr_pos);
		//	System.out.println("\n"+curr_index[0]+"\n");
			//int test = curr_index[0];
			//System.out.println("\nsel"+selectivity[test]+"\n");
			curr_sel[0] = 12;
			curr_sel[0] = selectivity[curr_index[0]];
			curr_sel[1] = selectivity[curr_index[1]];
			curr_sel[2] = selectivity[curr_index[2]];
			
			XViolationGrid[curr_pos] = 0;
			ZViolationGrid[curr_pos] = 0;
			YViolationGrid[curr_pos] = 0;
			
			XYViolationGrid[curr_pos] = 0;
			YZViolationGrid[curr_pos] = 0;
			XZViolationGrid[curr_pos] = 0;
			
			// COMMENTING STARTS .....
			
			
		//	System.out.println("\n#"+Thread.currentThread().getStackTrace()[1].getLineNumber());
			
			// ------------------------------------------------------------------------------------------- X -----------------------------------------------
			/*low_pos = FindNearestPoint(curr_sel,curr_index,selectivity,X);
			low_index = getCoordinates(dimension,resolution,low_pos);
			
			high_index[0] = low_index[0];
			high_index[1] = low_index[1];
			high_index[2] = low_index[2];
			
			if(low_index[0] != resolution - 1)
			{
				high_index[0] = low_index[0]+1;
			}
			
			high_pos = getIndex(high_index, resolution);
			low_index = getActualLowIndex(low_index,X);
			high_index = getActualHighIndex(high_index,X);
			if(low_index[0] == -1 || high_index[0] == -1)
			{
		//		XViolationGrid[curr_pos] = 1;
			}
			else
			{
				 START FROM HERE
				high_pos = getIndex(high_index, resolution);
				low_pos = getIndex(low_index, resolution);
				
				s1 = selectivity[low_index[0]];
				s2 = selectivity[high_index[0]];
				
				c1 = OptimalCost[low_pos];
				c2 = OptimalCost[high_pos];
				
				
				
				if(alpha*curr_sel[0] > s2)
				{
					approx_cost = c2;
				}
				else
				{
					approx_cost = ((c2 - c1)/(s2 - s1))*(alpha*curr_sel[0] - s1) + c1;
				}
			
				assert(low_pos <= high_pos);
				assert(s2 >= s1);
				assert(c1 <= c2);
				assert(alpha*curr_sel[0] >= s1);
				assert(approx_cost >= c1);
				assert(approx_cost <= c2);
				
				if(approx_cost > (ALPHA_COST*OptimalCost[curr_pos]))
			//	if(OptimalCost[new_pos] > (alpha*OptimalCost[curr_pos]))
				{
				//	System.out.println("\nright violation \n");
		//			XViolationGrid[curr_pos] = 1;
				}
				
			}
			
		//	System.out.println("\n#"+Thread.currentThread().getStackTrace()[1].getLineNumber());
			//------------------------------------------------------------------------------------------- Z -----------------------------------------------
			low_pos = FindNearestPoint(curr_sel,curr_index,selectivity,Z);
			low_index = getCoordinates(dimension,resolution,low_pos);
			high_index[0] = low_index[0];
			high_index[1] = low_index[1];
			high_index[2] = low_index[2];
			
			if(low_index[2] != resolution - 1)
			{
				high_index[2] = low_index[2]+1;
			}
			
			high_pos = getIndex(high_index, resolution);
			low_index = getActualLowIndex(low_index,Z);
			high_index = getActualHighIndex(high_index,Z);
			
			if(low_index[2] == -1 || high_index[2] == -1)
			{
	//			ZViolationGrid[curr_pos] = 1;
			}
			else
			{
				high_pos = getIndex(high_index, resolution);
				low_pos = getIndex(low_index, resolution);
				
				s1 = selectivity[low_index[2]];
				s2 = selectivity[high_index[2]];
				
				c1 = OptimalCost[low_pos];
				c2 = OptimalCost[high_pos];
				
				if(alpha*curr_sel[2] > s2)
				{
					approx_cost = c2;
				}
				else
				{
				
				
				approx_cost = ((c2 - c1)/(s2 - s1))*(alpha*curr_sel[2] - s1) + c1;
				}
				
				assert(low_pos <= high_pos);
				assert(s2 >= s1);
				assert(c1 <= c2);
				assert(alpha*curr_sel[2] >= s1);
				assert(approx_cost >= c1);
				assert(approx_cost <= c2);
				
				if(approx_cost > (ALPHA_COST*OptimalCost[curr_pos]))
			//	if(OptimalCost[new_pos] > (alpha*OptimalCost[curr_pos]))
				{
			//		System.out.println("\nViolating !!!!!!!!\n");
		//			ZViolationGrid[curr_pos] = 1;
				}
			}
			//------------------------------------------------------------------------------------------- Y -----------------------------------------------
			low_pos = FindNearestPoint(curr_sel,curr_index,selectivity,Y);
			low_index = getCoordinates(dimension,resolution,low_pos);
			high_index[0] = low_index[0];
			high_index[1] = low_index[1];
			high_index[2] = low_index[2];
			
			if(low_index[1] != resolution - 1)
			{
				high_index[1] = low_index[1]+1;
			}
			
			high_pos = getIndex(high_index, resolution);
			low_index = getActualLowIndex(low_index,Y);
			high_index = getActualHighIndex(high_index,Y);
			
			if(low_index[1] == -1 || high_index[1] == -1)
			{
		//		YViolationGrid[curr_pos] = 1;
			}
			else
			{
				high_pos = getIndex(high_index, resolution);
				low_pos = getIndex(low_index, resolution);
				
				s1 = selectivity[low_index[1]];
				s2 = selectivity[high_index[1]];
				
				c1 = OptimalCost[low_pos];
				c2 = OptimalCost[high_pos];
			
				if(alpha*curr_sel[1] > s2)
				{
					approx_cost = c2;
				}
				else
				{
					approx_cost = ((c2 - c1)/(s2 - s1))*(alpha*curr_sel[1] - s1) + c1;
				}
				assert(low_pos <= high_pos);
				assert(s2 >= s1);
				assert(c1 <= c2);
				assert(alpha*curr_sel[1] >= s1);
				assert(approx_cost >= c1);
				assert(approx_cost <= c2);
				
				
				if(approx_cost > (ALPHA_COST*OptimalCost[curr_pos]))
		//		if(OptimalCost[new_pos] > (alpha*OptimalCost[curr_pos]))
				{
		//			System.out.println("\nViolating !!!!!!!!\n");
		//			YViolationGrid[curr_pos] = 1;
				}
			}*/
			//------------------------------------------------------------------------------------------- XY -----------------------------------------------
			low_pos = FindNearestPoint(curr_sel,curr_index,selectivity,XY);
			low_index = getCoordinates(dimension,resolution,low_pos);
			
			high_index[0] = low_index[0];
			high_index[1] = low_index[1];
			high_index[2] = low_index[2];
			
			if(low_index[0] != resolution -1 && low_index[1] != resolution -1)
			{
		//		System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
				high_index[0] = low_index[0]+1;
				high_index[1] = low_index[1]+1;
			}
			else
			{
			//	System.out.println("\nELSE : "+curr_index[0]+curr_index[1]+curr_index[2]+"\n");
			}
			
			high_pos = getIndex(high_index, resolution);
			low_index = getActualLowIndex(low_index,XY);
			high_index = getActualHighIndex(high_index,XY);
			
			if(low_index[0] == -1 || high_index[0] == -1)
			{
		//		XYViolationGrid[curr_pos] = 1;
			}
			else
			{
				high_pos = getIndex(high_index, resolution);
				low_pos = getIndex(low_index, resolution);
				//System.out.println("\n E:: low_index[0] > high_index[0]");
				
				
				x1 = selectivity[low_index[0]];
				y1 = selectivity[low_index[1]];
				
				x2 = selectivity[high_index[0]];
				y2 = selectivity[high_index[1]];
				
				c1 = OptimalCost[low_pos];
				c2 = OptimalCost[high_pos];
				
				
				if(alpha*curr_sel[0] > x2 || alpha*curr_sel[1] > y2)
				{
					approx_cost = c2;
				}
				else
				{
					approx_cost = getEstimateCost(curr_sel[0],curr_sel[1],x1,y1,c1,x2,y2,c2);
				}
				
				assert(low_pos <= high_pos);
				assert(x2 >= x1);
				assert(y2 >= y1);
				assert(c1 <= c2);
				assert(alpha*curr_sel[0] >= x1);
				assert(alpha*curr_sel[1] >= y1);
				assert(approx_cost >= c1);
				assert(approx_cost <= c2);
				
				
				if(approx_cost > (ALPHA_COST*OptimalCost[curr_pos]))
			//	if(OptimalCost[new_pos] > (alpha*OptimalCost[curr_pos]))
				{
			//		System.out.println("\nViolating !!!!!!!!\n");
					XYViolationGrid[curr_pos] = 1;
				}
			}
		//	------------------------------------------------------------------------------------------- YZ -----------------------------------------------
			/*low_pos = FindNearestPoint(curr_sel,curr_index,selectivity,YZ);
			low_index = getCoordinates(dimension,resolution,low_pos);
			high_index[0] = low_index[0];
			high_index[1] = low_index[1];
			high_index[2] = low_index[2];
			
			if(low_index[1] != resolution - 1 && low_index[2] != resolution -1)
			{
				high_index[1] = low_index[1]+1;
				high_index[2] = low_index[2]+1;
			}
			
			high_pos = getIndex(high_index, resolution);
			low_index = getActualLowIndex(low_index,YZ);
			high_index = getActualHighIndex(high_index,YZ);
			
			if(low_index[0] == -1 || high_index[0] == -1)
			{
		//		YZViolationGrid[curr_pos] = 1;
			}
			else
			{
				high_pos = getIndex(high_index, resolution);
				low_pos = getIndex(low_index, resolution);
				
				y1 = selectivity[low_index[1]];
				z1 =selectivity[low_index[2]];
				
				y2 = selectivity[high_index[1]];
				z2 = selectivity[high_index[2]];
				
				c1 = OptimalCost[low_pos];
				c2 = OptimalCost[high_pos];
			
				
			
				
				if(alpha*curr_sel[1] > y2 || alpha*curr_sel[2] > z2)
				{
					approx_cost = c2;
				}
				else
				{
					approx_cost = getEstimateCost(curr_sel[1],curr_sel[2],y1,z1,c1,y2,z2,c2);
				}
				
				assert(low_pos <= high_pos);
				assert(y2 >= y1);
				assert(z2 >= z1);
				assert(c1 <= c2);
				assert(alpha*curr_sel[1] >= y1);
				assert(alpha*curr_sel[2] >= z1);
				assert(approx_cost >= c1);
				assert(approx_cost <= c2);
				
				if(approx_cost > (ALPHA_COST*OptimalCost[curr_pos]))
			//	if(OptimalCost[new_pos] > (alpha*OptimalCost[curr_pos]))
				{
			//		System.out.println("\nViolating !!!!!!!!\n");
			//		YZViolationGrid[curr_pos] = 1;
				}
			}
	//		------------------------------------------------------------------------------------------- XZ -----------------------------------------------
			low_pos = FindNearestPoint(curr_sel,curr_index,selectivity,XZ);
			low_index = getCoordinates(dimension,resolution,low_pos);
			high_index[0] = low_index[0];
			high_index[1] = low_index[1];
			high_index[2] = low_index[2];
			
			if(low_index[0] != resolution - 1 && low_index[2] != resolution -1)
			{
				high_index[0] = low_index[0]+1;
				high_index[2] = low_index[2]+1;
			}
			
			high_pos = getIndex(high_index, resolution);
			low_index = getActualLowIndex(low_index,XZ);
			high_index = getActualHighIndex(high_index,XZ);
			
			if(low_index[0] == -1 || high_index[0] == -1)
			{
		//		XZViolationGrid[curr_pos] = 1;
			}
			else
			{
				high_pos = getIndex(high_index, resolution);
				low_pos = getIndex(low_index, resolution);
				
				x1 = selectivity[low_index[0]];
				z1 =selectivity[low_index[2]];
				
				x2 = selectivity[high_index[0]];
				z2 = selectivity[high_index[2]];
				
				c1 = OptimalCost[low_pos];
				c2 = OptimalCost[high_pos];
				
				if(alpha*curr_sel[0] > x2 || alpha*curr_sel[2] > z2)
				{
					approx_cost = c2;
				}
				else
				{
					approx_cost = getEstimateCost(curr_sel[0],curr_sel[2],x1,z1,c1,x2,z2,c2);
				}
				
				assert(low_pos <= high_pos);
				assert(x2 >= x1);
				assert(z2 >= z1);
				assert(c1 <= c2);
				assert(alpha*curr_sel[0] >= x1);
				assert(alpha*curr_sel[2] >= z1);
				assert(approx_cost >= c1);
				assert(approx_cost <= c2);
				
				if(approx_cost > (ALPHA_COST*OptimalCost[curr_pos]))
			//	if(OptimalCost[new_pos] > (alpha*OptimalCost[curr_pos]))
				{
			//		System.out.println("\nViolating !!!!!!!!\n");
			//		XZViolationGrid[curr_pos] = 1;
				}
			}*/
				
//			------------------------------------------------------------------------------------------- DONE -----------------------------------------------	
			
			
		}
		//System.out.println("\nAll violation grid obtained!\n");
		
		int no_of_bad_points =0;
		for(int i = 0; i< total_points; i++)
		{
			if (list.indexOf(i) != -1) 
				continue;
			if(XViolationGrid[i] == 0 &&
					YViolationGrid[i] == 0 && 
					ZViolationGrid[i] == 0 &&
					XYViolationGrid[i] == 0  &&
					YZViolationGrid[i] == 0  &&
					XZViolationGrid[i] == 0 ) //&&
					//YViolationGrid[i] == 1)
			{
				no_of_bad_points ++;
			}
			
		}
		
//		System.out.println("\nBiggest cost :"+OptimalCost[Math.pow(resolution, dimension) - 1]+"\n");
		System.out.println("\nBiggest cost :"+biggest_cost+"\n");
		int den = total_points - list.size();
		double percent_error = ((double)(no_of_bad_points)*100)/den;
		double pcm_violate = ((double)(total_points - den))*100 / (total_points);
		System.out.println("\n#good_points"+no_of_bad_points+"\t Percent PCM violation="+pcm_violate+"\t Final Percent of points which is satisfies"+percent_error+ "\n");
		
		
	}
	
	
	
	
	
	

//Added by Lohit
/*
 * 1. Get AllPlanCost[r][r]
 * 1.1 Smoothen the PCF's 
 * 2. Construct OptimalCost[resolution], which will contain the optimal cost at each point in the discrete selectivity space. 
 * 3. Write function CheckViolations :
 * 		It uses queue to do a BFS and find out the violations.
 * 		* Get selectivity from the index using uniform or exponential distribution(The pcst files is such)
 * 		three Data Structures which will be having each entry as zero or one (or any number in the second strategy): 
 * 			1. ZViolationGrid[resolution]
 * 			2. XViolatoinGrid[resolution]
 * 			3. YViolationGrid[resolution]
 * 4. Write three funcitons :
 * 		1. CheckVerticalViolation
 * 		2. CheckHorizontalViolation	
 * 		3. CheckYViolation
 * 5. Write function GetTotalViolations
 */
// ############################### Written by Lohit ########################################

	/*
	 * Return the new_pos
	 */
	int FindNearestPoint(double [] sel,int []base_index,double []selectivity,int option)
	{
		double min_diff = 1000000;
	//	int min_index[];
		double alpha_x = alpha*sel[0];
		double alpha_y = alpha*sel[1];
		double alpha_z = alpha*sel[2];
		double diff,diff1,diff2;
		int [] min_index = new int [3];
		
		min_index[0] = base_index[0];
		min_index[1] = base_index[1];
		min_index[2] = base_index[2];
		if(option == X)
		{
			for(int i = base_index[0]; i<=(resolution-1); i++)
			{
				
				diff = alpha_x - selectivity[i];
				if(diff < 0)
					break;
				if(Math.abs(diff) < min_diff && diff >= 0)
				{
					min_diff = diff;
					min_index[0] = i;
				}
			}
		}
		if(option == Y)
		{
			for(int i = base_index[1]; i<=(resolution-1); i++)
			{
				
				diff = alpha_y - selectivity[i];
				if(diff < 0)
					break;
				if(Math.abs(diff) < min_diff && diff >= 0)
				{
					min_diff = diff;
					min_index[1] = i;
				}
			}
		}
		if(option == Z)
		{
			for(int i = base_index[2]; i<=(resolution-1); i++)
			{
				
				diff = alpha_z - selectivity[i];
				if(diff < 0)
					break;
				if(Math.abs(diff) < min_diff && diff >= 0)
				{
					min_diff = diff;
					min_index[2] = i;
				}
			}
		}
		if(option == XY)
		{
			int i = base_index[0];
			int j = base_index[1];
			while(i<=(resolution-1) && j<=(resolution-1))
			{
				
				diff1 = alpha_x - selectivity[i];
				diff2 = alpha_y - selectivity[j];
				diff = Math.sqrt(Math.pow(diff1,2) + Math.pow(diff2, 2));
				if(diff1 < 0 && diff2 < 0)
					break;
				if(Math.abs(diff) < min_diff && (diff1 >= 0) && (diff2 >= 0) )
				{
					min_diff = diff;
					min_index[0] = i;
					min_index[1] = j;
				}
				i++;
				j++;
			}
		}
		if(option == YZ)
		{
			int i = base_index[1];
			int j = base_index[2];
			while(i<=(resolution-1) && j<=(resolution-1))
			{
				
				diff1 = alpha_y - selectivity[i];
				diff2 = alpha_z - selectivity[j];
				diff = Math.sqrt(Math.pow(diff1,2) + Math.pow(diff2, 2));
				if(diff1 < 0 && diff2 < 0)
					break;
				if(Math.abs(diff) < min_diff && (diff1 >= 0) && (diff2 >= 0) )
				{
					min_diff = diff;
					min_index[1] = i;
					min_index[2] = j;
				}
				i++;
				j++;
			}
		}
		if(option == XZ)
		{
			int i = base_index[0];
			int j = base_index[2];
			while(i<=(resolution-1) && j<=(resolution-1))
			{
				
				diff1 = alpha_x - selectivity[i];
				diff2 = alpha_z - selectivity[j];
				diff = Math.sqrt(Math.pow(diff1,2) + Math.pow(diff2, 2));
				if(diff1 < 0 && diff2 < 0)
					break;
				if(Math.abs(diff) < min_diff && (diff1 >= 0) && (diff2 >= 0) )
				{
					min_diff = diff;
					min_index[0] = i;
					min_index[2] = j;
				}
				i++;
				j++;
			}
		}
		return getIndex(min_index,resolution);
	}
	
	void getViolationList(int total_points, double [] Cost)
	{
		int [] index = new int [3];
		int [] left_index = new int [3];
		int [] right_index = new int [3];
		int [] top_index = new int [3];
		int right_pos, top_pos, left_pos;
		
		for(int i=0; i<total_points;i++)
		{
			right_pos = i;
			top_pos = i;
			left_pos = i;
			index = getCoordinates(dimension,resolution,i);
			if (index[0] != resolution -1 )
			{
				right_index[0] = index[0] + 1 ;
				right_index[1] = index[1];
				right_index[2] = index[2];
				right_pos = getIndex(right_index,resolution);
				assert(i <= right_pos);
			}
			if(index[1] != resolution -1)
			{
				left_index[0] = index[0];
				left_index[1] = index[1] + 1;
				left_index[2] = index[2];
				left_pos = getIndex(left_index, resolution);
				assert(i <= left_pos);
			}
			if(index[2] != resolution -1)
			{
				top_index[0] = index[0];
				top_index[1] = index[1];
				top_index[2] = index[2] +1;
				top_pos = getIndex(top_index, resolution);
				assert(i <= top_pos);
			}
			if(Cost[right_pos] < Cost[i] || Cost[left_pos] < Cost[i] || Cost[top_pos] < Cost[i])
			{
				//System.out.println("\n"+i+"added into the violation list\n");
				list.add(i);
			}
		}
		return;
	}
	
	double [] Smoothen(double [] OptimalCost)
	{
		/*
		 * FOR 3D !!
		 * Get the AllPlanCosts
		 * For (i = 0;i < total_plans ;i++)
		 * 	Put the lowest point in queue.
		 * 	while queue not empty
		 * 		Pull out a item and put it to current_item from queue, and insert point  to its right and top (if any).
		 * 		If cost[current_item] > cost [right of it] || cost [current_item] < cost[left of it]
		 * 			Equalize the cost  to the current_item.
		 * 		 
		 */
		
	//	System.out.println("\n size of array ="+AllPlanCosts.length+"and"+AllPlanCosts[1].length+"\n");
		int loc;
		int current_pos;
		int indx[] = new int[3]; 
		int next_x_pt[] = new int[3];
		int next_z_pt[] = new int[3];
		int next_y_pt[] = new int[3];
		int x_pos;
		int z_pos;
		int y_pos;
		
	//	System.out.println("\n total_points = "+total_points+"\t resolution ="+resolution+"\n");
		for (int i=0;i<total_points;i++)
		{
			
			indx = getCoordinates(dimension,resolution,i);
			//System.out.println("\n #"+i+"\t ["+indx[0]+"] \t ["+indx[1]+"]\n");
			if(indx[0] != (resolution-1))
			{
				next_x_pt[0]= indx[0]+1;
				next_x_pt[1] = indx[1];
				next_x_pt[2] = indx[2];
				x_pos = getIndex(next_x_pt,resolution);
			//	System.out.println("\n right_pos = "+right_pos);
				if (OptimalCost[x_pos] < OptimalCost[i])
				{
					big_cnt++;
					//System.out.println("\n Here");
					OptimalCost[x_pos] = OptimalCost[i];
				}
			}
			if(indx[2]!=(resolution -1 ))
			{
				next_z_pt[0]= indx[0];
				next_z_pt[1] = indx[1];
				next_z_pt[2] = indx[2] +1;
				z_pos = getIndex(next_z_pt,resolution);
				if (OptimalCost[z_pos] < OptimalCost[i])
				{
					big_cnt++;
					//System.out.println("\n Here2");
					OptimalCost[z_pos] = OptimalCost[i];
				}
			}
			if(indx[1] != (resolution-1))
			{
				next_y_pt[0]= indx[0];
				next_y_pt[1] = indx[1] +1;
				next_y_pt[2] = indx[2];
				y_pos = getIndex(next_y_pt,resolution);
			//	System.out.println("\n right_pos = "+right_pos);
				if (OptimalCost[y_pos] < OptimalCost[i])
				{
					big_cnt++;
					//System.out.println("\n Here");
					OptimalCost[y_pos] = OptimalCost[i];
				}
			}
		}
		System.out.println("\n>>>>>> Big Count ="+big_cnt+"\n");
		return OptimalCost;
		
	}

	double getEstimateCost(double curr_sel_x,double curr_sel_y, double x1,double y1,double c1,double x2,double y2,double c2 )
	{
		/*
		 * Srinivas's Algorithm
		 */
		double final_estimated_cost;
		double b;
		if(c1 != c2)
		{
			
			double alpha_x = curr_sel_x*alpha;
			double alpha_y = curr_sel_y*alpha;
			
			double term1 = Math.pow((alpha_x - x1), 2);
			double term2 = Math.pow((alpha_y - y1),2);
			
			double term3 = Math.pow((x2 - x1), 2);
			double term4 = Math.pow((y2 - y1), 2);
			
			b = Math.sqrt((term1 + term2)) / Math.sqrt((term3 + term4));
			
			final_estimated_cost = (1 - b)*c1 + b*c2;
		}
		else
		{
			final_estimated_cost = c1;
		}
		//System.out.println("\n"+x1+y1+","+x2+y2+";"+final_estimated_cost+","+c1+","+c2+"\n");
		assert(final_estimated_cost <=c2 );
		assert(final_estimated_cost >= c1);
		return final_estimated_cost;
	}
	
	int IsViolating(int [] index)
	{
		int pos = getIndex(index,resolution);
		if(list.indexOf(pos) != -1)
			return 1;
		return 0;
	}
	
	int [] getActualLowIndex(int [] low_index, int option)
	{
		int [] index = new int [3];
		int a, b, c;
		

		index[0] = low_index[0];
		index[1] = low_index[1];
		index[2] = low_index[2];
		
		a = 0;  b =0; c =0;
		if(option == X)
			a = 1;
		if(option == Y)
			b = 1;
		if(option == Z)
			c = 1;
		if(option == XY)
		{
			a = 1;
			b = 1;
		}
		if(option == YZ)
		{
			b = 1;
			c = 1;
		}
		if(option == XZ)
		{
			a = 1;
			c = 1;
		}
		
		while(IsViolating(index)==1)
		{
			if((a == 1 && index[0] == 0) 
					|| (b == 1 && index[1] == 0) 
					|| (c == 1 && index[2] == 0))
			{
				index[0] = -1;
				index[1] = -1;
				index[2] = -1;
				
				return index;
			}
			index[0] = index[0] - a;
			index[1] = index[1] - b;
			index[2] = index[2] - c;
		}
		
		assert(index[0] <= low_index[0]);
		assert(index[1] <= low_index[1]);
		assert(index[2] <= low_index[2]);
		
		assert(index[0] >= 0);
		assert(index[0] >= 0);
		assert(index[0] >= 0);
		
		return index;

			
	}
	
	int [] getActualHighIndex(int [] high_index, int option)
	{
		int [] index = new int [3];
		int a, b, c;
		

		index[0] = high_index[0];
		index[1] = high_index[1];
		index[2] = high_index[2];
	//	System.out.println("\nPrior.. [0] = "+index[0]+"\t [1]="+index[1]+"\t [2]="+index[2]+"\n");
		
		a = 0;  b =0; c =0;
		if(option == X)
			a = 1;
		if(option == Y)
			b = 1;
		if(option == Z)
			c = 1;
		if(option == XY)
		{
			a = 1;
			b = 1;
		}
		if(option == YZ)
		{
			b = 1;
			c = 1;
		}
		if(option == XZ)
		{
			a = 1;
			c = 1;
		}
		
		while(IsViolating(index)==1)
		{
			if((a == 1 && index[0] == resolution -1) 
					|| (b == 1 && index[1] == resolution -1) 
					|| (c == 1 && index[2] == resolution -1))
			{
				index[0] = -1;
				index[1] = -1;
				index[2] = -1;
				
				return index;
			}
			index[0] = index[0] + a;
			index[1] = index[1] + b;
			index[2] = index[2] + c;
		}
	//	System.out.println("\na = "+a+"\t b="+b+"\t c="+c+"\n");
	//	System.out.println("\n [0] = "+index[0]+"\t [1]="+index[1]+"\t [2]="+index[2]+"\n");
		
		assert(index[0] < resolution);
		assert(index[1] < resolution);
		assert(index[2] < resolution);
		
		assert(index[0] >= high_index[0]);
		assert(index[1] >= high_index[1]);
		assert(index[2] >= high_index[2]);
		return index;

			
	}
	
	//######################## Get GDP ############################################################################################################
	void findContours()
	{
		SANITY_CONSTANT = 10000;
		double minCostPacket = Math.max(AllPlanCosts[newOptimalPlan[0]][0],SANITY_CONSTANT);
		double maxPacketCost = AllPlanCosts[newOptimalPlan[data.length-1]][data.length-1];       //assumed to be max
		int steps = 0;				
		double limit = maxPacketCost;
		while(limit > minCostPacket){		limit /= 2;		steps++;}
		double firstCostLimit = limit * 2;
		
		double cost_limit[] = new double[steps];
		for(int s=0; s<steps; s++)					
			cost_limit[s] = firstCostLimit * Math.pow(2, s);
		
		int doneRec[] = new int[data.length];
		for(int loc=0;loc<data.length;loc++){
			int optimalPlan = newOptimalPlan[loc];
			double optCost = Math.max(AllPlanCosts[optimalPlan][loc], SANITY_CONSTANT);
			
			if(optCost > data[data.length-1].getCost())
				optCost = Math.floor(data[data.length-1].getCost());
//			optCost = Math.min(AllPlanCosts[optimalPlan][loc], data[data.length-1].getCost());
			
			
			//find out under which contour the current location lies
			int s=0;
			while(optCost > cost_limit[s]) { 
				
				s++;
				if(s==steps+1)
					System.out.printf("caught");
			}
			doneRec[loc] = s;
		}
		
		doneRec[data.length-1] = steps;
		for(int loc=data.length-2;loc>=0;loc--){
			int d =0, correct = 0;
			int minMark = 99999;
			for(d=0;d<dimension;d++){
				int nloc = loc - (int) Math.pow(resolution, d);
				if(nloc > 0 && nloc < data.length - 1){
					correct++;
					minMark = Math.min(minMark, doneRec[nloc]);
				}
			}
			if(correct == 0)   minMark = 0;
			if(doneRec[loc]==minMark) doneRec[loc] = 0;
		}
		
		
		int contourPlansTotalLocation[][]=new int[steps][totalPlans]; 
		
		int contourPlansMaxLocation[][][]=new int[steps][totalPlans][dimension];
		int contourPlansMinLocation[][][]=new int[steps][totalPlans][dimension];
		
		int allContourPlans[] = new int[totalPlans];
		int allContPlansCount = 0;
		
		for(int i=0;i<steps;i++)
		{
			for(int j=0;j<totalPlans;j++)
			{
				Arrays.fill(contourPlansMaxLocation[i][j], Integer.MIN_VALUE);
				Arrays.fill(contourPlansMinLocation[i][j], Integer.MAX_VALUE);
				Arrays.fill(allContourPlans, -1);
			}
			Arrays.fill(contourPlansTotalLocation[i], 0);
		}
		int optimalPlan;
	
		for(int loc=0;loc<data.length;loc++)
		{
			if(doneRec[loc]>=1)
			{
				optimalPlan = newOptimalPlan[loc];
				contourPlansTotalLocation[doneRec[loc]-1][optimalPlan] = 1;
			}
		}
		int totalPlansOnCountour=0;
		for(int i=0;i<steps;i++)
		{
			for(int j=0;j<totalPlans;j++)
			{
				if(contourPlansTotalLocation[i][j]!=0)
				{
					System.out.print(j+",");
					totalPlansOnCountour++;
					int c=0;
					for(c=0;c<allContPlansCount;c++)
					{
						if(allContourPlans[c]==j)
							break;
					}
					if(c>=allContPlansCount)
						allContourPlans[allContPlansCount++]=j;
				}
			}
			System.out.println();
		}
		
		int[] coordinate = new int[dimension];
		for(int loc=0;loc<data.length;loc++)
		{
			if(doneRec[loc]>=1)
			{
				coordinate = getCoordinates(dimension,resolution,loc);
				optimalPlan = newOptimalPlan[loc];
//				contourPlansTotalLocation[doneRec[loc]-1][optimalPlan]++;
				for(int i=0;i<dimension;i++)
				{
					if(contourPlansMaxLocation[doneRec[loc]-1][optimalPlan][i]<coordinate[i])
					{
						contourPlansMaxLocation[doneRec[loc]-1][optimalPlan][i]=coordinate[i];
					}
					if(contourPlansMinLocation[doneRec[loc]-1][optimalPlan][i]>coordinate[i])
					{
						contourPlansMinLocation[doneRec[loc]-1][optimalPlan][i]=coordinate[i];
					}
				}
			}
		}
		
		int reducedPlans[] = new int[totalPlans];
		int counter=0;
		int temp = totalPlansOnCountour;
//		while(true)
		while(temp>0)
		{
			int eatenPlans[] = new int[totalPlans];
			Arrays.fill(eatenPlans, 0);
			for(int i=0;i<steps;i++)
			{
				for(int j=0;j<totalPlans;j++)
				{
					if(contourPlansTotalLocation[i][j]!=0)
					{
						int c=0;
						for(c=0;c<counter;c++)
						{
							int index = getIndex(contourPlansMaxLocation[i][j], resolution);
//							if(AllPlanCosts[reducedPlans[c]][index] <= cost_limit[i] * (1.2))
							if(reducedPlans[c]==j)
							{
								break;
							}
//							if(Math.abs(AllPlanCosts[reducedPlans[c]][index] -cost_limit[i])/AllPlanCosts[reducedPlans[c]][index]<= threshold/100.0)
							if(AllPlanCosts[reducedPlans[c]][index]<=cost_limit[i]*(1+threshold/100.0))
							{
								break;
							}
						}
						if(c<counter)
							continue;
						
						int index = getIndex(contourPlansMaxLocation[i][j], resolution);
						for(int k=0;k<allContPlansCount;k++)
//						for(int k=0;k<totalPlans;k++)
						{
							if(allContourPlans[k]!=j)
							{
//								currentRelativeDiff = Math.abs(cost_limit[i]-AllPlanCosts[allContourPlans[k]][index]);
//								currentRelativeDiff = currentRelativeDiff * 100.0/AllPlanCosts[allContourPlans[k]][index];
//								if(currentRelativeDiff<=threshold)
								if(AllPlanCosts[allContourPlans[k]][index]<=cost_limit[i]*(1+threshold/100.0))
								{
									eatenPlans[allContourPlans[k]]++;
//									if(k==6)
//									System.out.println("contour="+i+", plan="+j+", eaten="+k+"");
								}
							}
							else
							{
								eatenPlans[allContourPlans[k]]++;
//								if(k==6)
//								System.out.println("contour="+i+", plan="+j+", eaten="+k+"");
							}
						}
					}
				}
			}
			int maxEatenPlan = -1;
			int chosenPlan=-1;
			for(int i=0;i<allContPlansCount;i++)
//			for(int i=0;i<totalPlans;i++)
			{
				if(maxEatenPlan<eatenPlans[allContourPlans[i]])
				{
					maxEatenPlan = eatenPlans[allContourPlans[i]];
					chosenPlan = allContourPlans[i];
				}
			}
			if(maxEatenPlan == -1)
				break;
			reducedPlans[counter++] = chosenPlan;
			temp = temp - maxEatenPlan;
			
			for(int i=0;i<steps;i++)
			{
				for(int j=0;j<totalPlans;j++)
				{
					if(contourPlansTotalLocation[i][j]!=0)
					{
						int c=0;
						for(c=0;c<counter-1;c++)
						{
							int index = getIndex(contourPlansMaxLocation[i][j], resolution);
//							if(AllPlanCosts[reducedPlans[c]][index] <= cost_limit[i] * (1.2))
							if(reducedPlans[c]==j)
							{
								break;
							}
//							if(Math.abs(AllPlanCosts[reducedPlans[c]][index] -cost_limit[i])/AllPlanCosts[reducedPlans[c]][index]<= threshold/100.0)
							if(AllPlanCosts[reducedPlans[c]][index]<=cost_limit[i]*(1+threshold/100.0))
							{
								break;
							}
						}
						if(c<counter-1)
							continue;
						int index = getIndex(contourPlansMaxLocation[i][j], resolution);
						if(chosenPlan!=j)
						{
							if(AllPlanCosts[chosenPlan][index]<=cost_limit[i]*(1+threshold/100.0))
							{
								System.out.println("contour="+i+", plan="+j+", eaten by="+chosenPlan+"");
								contourPlansTotalLocation[i][j] = 0;
								contourPlansTotalLocation[i][chosenPlan] = 1;
							}
						}
						else
						{
							System.out.println("contour="+i+", plan="+j+", eaten by="+chosenPlan+"");
						}
					}
				}
			}
			System.out.println();
		}
		
		for(int i=0;i<counter;i++)
		{
			System.out.print(reducedPlans[i]+",");
		}
		System.out.println();
		System.out.println("Reduced Contour Plans=");
		int a = 0;
		int rho=0;
		for(int i=0;i<steps;i++)
		{ 
			a=0;
			for(int j=0;j<totalPlans;j++)
			{
				if(contourPlansTotalLocation[i][j]!=0)
				{
					a++;
					System.out.print(j+",");
				}
				if(rho<a)
					rho=a;
			}
			System.out.println();
		}
		System.out.println("rho="+rho);
		
//		for(int i=0;i<steps;i++)
//		{
//			for(int j=0;j<totalPlans;j++)
//			{
//				if(contourPlans[i][j]!=0)
//				{
//					for(int k=0;k<dimension;k++)
//					{
//						System.out.print(contourPlansMinLocation[i][j][k]+",");
//					}
//					System.out.print(" :");
//					for(int k=0;k<dimension;k++)
//					{
//						System.out.print(contourPlansMaxLocation[i][j][k]+",");
//					}
//					System.out.print("\t\t");
//				}
//			}
//			System.out.println();
//		}
		
//		for(int i=99;i>=0;i--)
//		{
//			int j=100*i;
//			while(j<(100*i+100))
//			{
//				if(doneRec[j]==0)
//					System.out.print(" ");
//				else
////				System.out.print(newOptimalPlan[j]);
//					System.out.print(getOptimalPlan(j, reducedPlans));
//				j++;
//			}
//			System.out.println();
//		}
	}
	void findContoursAndReduceContourWise()
	{
		SANITY_CONSTANT = 10000;
		double minCostPacket = Math.max(AllPlanCosts[newOptimalPlan[0]][0],SANITY_CONSTANT);
		double maxPacketCost = AllPlanCosts[newOptimalPlan[data.length-1]][data.length-1];       //assumed to be max
		int steps = 0;				
		double limit = maxPacketCost;
		while(limit > minCostPacket){		limit /= 2;		steps++;}
		double firstCostLimit = limit * 2;
		
		double cost_limit[] = new double[steps];
		for(int s=0; s<steps; s++)					
			cost_limit[s] = firstCostLimit * Math.pow(2, s);
		
		int doneRec[] = new int[data.length];
		for(int loc=0;loc<data.length;loc++){
			int optimalPlan = newOptimalPlan[loc];
			double optCost = Math.max(AllPlanCosts[optimalPlan][loc], SANITY_CONSTANT);
			
			if(optCost > data[data.length-1].getCost())
				optCost = Math.floor(data[data.length-1].getCost());
//			optCost = Math.min(AllPlanCosts[optimalPlan][loc], data[data.length-1].getCost());
			
			
			//find out under which contour the current location lies
			int s=0;
			while(optCost > cost_limit[s]) { 
				
				s++;
				if(s==steps+1)
					System.out.printf("caught");
			}
			doneRec[loc] = s;
		}
		
		doneRec[data.length-1] = steps;
		for(int loc=data.length-2;loc>=0;loc--){
			int d =0, correct = 0;
			int minMark = 99999;
			for(d=0;d<dimension;d++){
				int nloc = loc - (int) Math.pow(resolution, d);
				if(nloc > 0 && nloc < data.length - 1){
					correct++;
					minMark = Math.min(minMark, doneRec[nloc]);
				}
			}
			if(correct == 0)   minMark = 0;
			if(doneRec[loc]==minMark) doneRec[loc] = 0;
		}
		
		int contourPlansCount[] = new int[steps];
		Arrays.fill(contourPlansCount, 0);
		
		int contourPlansTotalLocation[][]=new int[steps][totalPlans]; 
		
		int contourPlansMaxLocation[][][]=new int[steps][totalPlans][dimension];
		int contourPlansMinLocation[][][]=new int[steps][totalPlans][dimension];
		
		int allContourPlans[] = new int[totalPlans];
		int allContPlansCount = 0;
		
		for(int i=0;i<steps;i++)
		{
			for(int j=0;j<totalPlans;j++)
			{
				Arrays.fill(contourPlansMaxLocation[i][j], Integer.MIN_VALUE);
				Arrays.fill(contourPlansMinLocation[i][j], Integer.MAX_VALUE);
				Arrays.fill(allContourPlans, -1);
			}
			Arrays.fill(contourPlansTotalLocation[i], 0);
		}
		int optimalPlan;
	
		for(int loc=0;loc<data.length;loc++)
		{
			if(doneRec[loc]>=1)
			{
				optimalPlan = newOptimalPlan[loc];
				contourPlansTotalLocation[doneRec[loc]-1][optimalPlan]++;
			}
		}
		int totalPlansOnCountour=0;
		for(int i=0;i<steps;i++)
		{
			for(int j=0;j<totalPlans;j++)
			{
				if(contourPlansTotalLocation[i][j]!=0)
				{
					contourPlansCount[i]++;
					System.out.print(j+",");
					totalPlansOnCountour++;
					int c=0;
					for(c=0;c<allContPlansCount;c++)
					{
						if(allContourPlans[c]==j)
							break;
					}
					if(c>=allContPlansCount)
						allContourPlans[allContPlansCount++]=j;
				}
			}
			System.out.println();
		}
		
		int[] coordinate = new int[dimension];
		for(int loc=0;loc<data.length;loc++)
		{
			if(doneRec[loc]>=1)
			{
				coordinate = getCoordinates(dimension,resolution,loc);
				optimalPlan = newOptimalPlan[loc];
//				contourPlansTotalLocation[doneRec[loc]-1][optimalPlan]++;
				for(int i=0;i<dimension;i++)
				{
					if(contourPlansMaxLocation[doneRec[loc]-1][optimalPlan][i]<coordinate[i])
					{
						contourPlansMaxLocation[doneRec[loc]-1][optimalPlan][i]=coordinate[i];
					}
					if(contourPlansMinLocation[doneRec[loc]-1][optimalPlan][i]>coordinate[i])
					{
						contourPlansMinLocation[doneRec[loc]-1][optimalPlan][i]=coordinate[i];
					}
				}
			}
		}
		
		int reducedPlans[][]= new int[steps][totalPlans];
		int reducedPlansCount[] = new int[steps];
		Arrays.fill(reducedPlansCount, 0);
		for(int i=0;i<steps;i++)
		{
			while(contourPlansCount[i]>0)
			{
				int eatenPlans[] = new int[totalPlans];
				Arrays.fill(eatenPlans, 0);
				for(int j=0;j<totalPlans;j++)
				{
					if(contourPlansTotalLocation[i][j]!=0)
					{
						int c=0;
						for(c=0;c<reducedPlansCount[i];c++)
						{
							int index = getIndex(contourPlansMaxLocation[i][j], resolution);
							if(reducedPlans[i][c]==j)
							{
								break;
							}
							if(AllPlanCosts[reducedPlans[i][c]][index]<=cost_limit[i]*(1+threshold/100.0))
							{
								break;
							}
						}
						if(c<reducedPlansCount[i])
							continue;
						
						double currentRelativeDiff = 0.0;
						int index = getIndex(contourPlansMaxLocation[i][j], resolution);
						for(int k=0;k<allContPlansCount;k++)
//						for(int k=0;k<totalPlans;k++)
						{
							if(allContourPlans[k]!=j)
							{
//								currentRelativeDiff = Math.abs(cost_limit[i]-AllPlanCosts[allContourPlans[k]][index]);
//								currentRelativeDiff = currentRelativeDiff * 100.0/AllPlanCosts[allContourPlans[k]][index];
//								if(currentRelativeDiff<=threshold)
								if(AllPlanCosts[allContourPlans[k]][index]<=cost_limit[i]*(1+threshold/100.0))
								{
									eatenPlans[allContourPlans[k]]++;
//									System.out.println("contour="+i+", plan="+j+", eaten="+k+"");
								}
							}
							else
							{
								eatenPlans[allContourPlans[k]]++;
//								System.out.println("contour="+i+", plan="+j+", eaten="+k+"");
							}
						}
					}
				}
				int maxEatenPlan = -1;
				int chosenPlan=-1;
				for(int j=0;j<allContPlansCount;j++)
				{
					if(maxEatenPlan<eatenPlans[allContourPlans[j]])
					{
						maxEatenPlan = eatenPlans[allContourPlans[j]];
						chosenPlan = allContourPlans[j];
					}
				}
				if(maxEatenPlan == -1)
					break;
				reducedPlans[i][reducedPlansCount[i]++] = chosenPlan;
				contourPlansCount[i] = contourPlansCount[i] - maxEatenPlan;
			}
//			for(int i=0;i<steps;i++)
//			{
//				for(int j=0;j<totalPlans;j++)
//				{
//					if(contourPlansTotalLocation[i][j]!=0)
//					{
//						double currentRelativeDiff = 0.0;
//						int index = getIndex(contourPlansMaxLocation[i][j], resolution);
//						if(chosenPlan!=j)
//						{
//							if(AllPlanCosts[chosenPlan][index]<=cost_limit[i]*(1+threshold/100.0))
//							{
//								System.out.println("contour="+i+", plan="+j+", eaten by="+chosenPlan+"");
//							}
//						}
//						else
//						{
//							System.out.println("contour="+i+", plan="+j+", eaten by="+chosenPlan+"");
//						}
//					}
//				}
//			}
		}
		
		System.out.println();
		int rho = 0;
		for(int i=0;i<steps;i++)
		{
			if(rho<reducedPlansCount[i])
				rho = reducedPlansCount[i];
			for(int j=0;j<reducedPlansCount[i];j++)
			{
				System.out.print(reducedPlans[i][j]+",");
			}
			System.out.println();
		}
		System.out.println("rho="+rho);
//		for(int i=0;i<steps;i++)
//		{
//			for(int j=0;j<totalPlans;j++)
//			{
//				if(contourPlans[i][j]!=0)
//				{
//					for(int k=0;k<dimension;k++)
//					{
//						System.out.print(contourPlansMinLocation[i][j][k]+",");
//					}
//					System.out.print(" :");
//					for(int k=0;k<dimension;k++)
//					{
//						System.out.print(contourPlansMaxLocation[i][j][k]+",");
//					}
//					System.out.print("\t\t");
//				}
//			}
//			System.out.println();
//		}
		
//		for(int i=99;i>=0;i--)
//		{
//			int j=100*i;
//			while(j<(100*i+100))
//			{
//				if(doneRec[j]==0)
//					System.out.print(" ");
//				else
////				System.out.print(newOptimalPlan[j]);
//					System.out.print(getOptimalPlan(j, reducedPlans));
//				j++;
//			}
//			System.out.println();
//		}
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
	public int getOptimalPlan(int loc, int[] plans) {
		
		double bestCost = Double.MAX_VALUE;
		int opt = -1;
		for(int p=0; p<plans.length; p++){
			if(bestCost > AllPlanCosts[plans[p]][loc]) {
				bestCost = AllPlanCosts[plans[p]][loc];
				opt = p;
			}
		}
		return opt;
	}
	public float Max(float a, float b){
		if(a>b)
			return a;
		else
			return b;
	}
}