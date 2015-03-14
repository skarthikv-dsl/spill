import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;
public class MulDGradientAsmpValidation
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
	int k=2;
	double ALPHA_COST ;
	int  ZViolationGrid[];
	int XViolatoinGrid[];
	int YViolationGrid[];
	List<Integer> list = new ArrayList<Integer>();
	//double endpoint[];
	
	
	public static void main(String args[])
	{
		String bouquetPath = "D:\\Srinivas\\DBCode\\data\\";
		String QT_NAME = "SQL SERVER_H3DQT2_10";
		
		MulDGradientAsmpValidation obj = new MulDGradientAsmpValidation();
		obj.bouqData(bouquetPath+QT_NAME);
	}
	void bouqData(String bouquetPath)
	{
		String funName="bouqData";
		double startpoint = 0.0;
		double endpoint = 1.0;
		
		ALPHA_COST = 1.05*Math.pow(alpha,k);
		
		ADiagramPacket gdp = getGDP(new File( bouquetPath +  ".apkt"));
		totalPlans = gdp.getMaxPlanNumber();
		dimension = gdp.getDimension();
		resolution = gdp.getMaxResolution();
		data = gdp.getData();
		total_points = (int) Math.pow(resolution, dimension);
		//System.out.println("\nthe total plans are "+totalPlans+" with dimensions "+dimension+" and resolution "+resolution);
		
		assert (total_points==data.length) : "Data length and the resolution didn't match !";
		
		double [] OptimalCost = new double [data.length]; 
		for (int i = 0;i < data.length;i++)
		{
			OptimalCost[i]= data[i].getCost();
		}
		getViolationList(total_points, OptimalCost);
	//	OptimalCost = Smoothen(OptimalCost);
	//	System.out.println("\n\n###############################################################\n\n;");
	//	OptimalCost = Smoothen(OptimalCost);
		//float[] picsel = new float[resolution];

	//	Smoothen();
	//	System.out.println("\nSmoothen Function ends.\n");
		double sel;
		sel= startpoint + ((endpoint - startpoint)/(2*resolution));
		double [] selectivity = new double [resolution];
		for(int i=0;i<resolution;i++){
			
			selectivity[i] = sel;
			//System.out.println("\nSelectivity["+i+"] = "+selectivity[i]+"\t");
			sel += ((endpoint - startpoint)/resolution);
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
			curr_sel[0] = selectivity[curr_index[0]];
			curr_sel[1] = selectivity[curr_index[1]];
			curr_sel[2] = selectivity[curr_index[2]];
			
			XViolationGrid[curr_pos] = 0;
			ZViolationGrid[curr_pos] = 0;
			YViolationGrid[curr_pos] = 0;
			
			XYViolationGrid[curr_pos] = 0;
			YZViolationGrid[curr_pos] = 0;
			XZViolationGrid[curr_pos] = 0;
			
			// ------------------------------------------------------------------------------------------- X -----------------------------------------------
			low_pos = FindNearestPoint(curr_sel,curr_index,selectivity,X);
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
				System.out.println(funName+": should not come here for X Violoation");
			}
			else
			{
				/* START FROM HERE*/
				high_pos = getIndex(high_index, resolution);
				low_pos = getIndex(low_index, resolution);
				s1 = selectivity[low_index[0]];
				s2 = selectivity[high_index[0]];
				
				c1 = OptimalCost[low_pos];
				c2 = OptimalCost[high_pos];
				
				if(low_pos > high_pos || s1>s2)
					System.out.println(funName+": should not come here for X Violoation: low>high");
				if(c1>c2)
					System.out.println(funName+": should not come here for X Violoation: c1>c2");
				approx_cost = ((c2 - c1)/(s2 - s1))*(alpha*curr_sel[0] - s1) + c1;
			
				if(approx_cost > (ALPHA_COST*OptimalCost[curr_pos]))
			//	if(OptimalCost[new_pos] > (alpha*OptimalCost[curr_pos]))
				{
				//	System.out.println("\nright violation \n");
					XViolationGrid[curr_pos] = 1;
				}
				
			}
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
				System.out.println("should not come here Z Violoation");
			}
			else
			{
				high_pos = getIndex(high_index, resolution);
				low_pos = getIndex(low_index, resolution);
				
				s1 = selectivity[low_index[2]];
				s2 = selectivity[high_index[2]];
				
				c1 = OptimalCost[low_pos];
				c2 = OptimalCost[high_pos];

				if(low_pos > high_pos || s1>s2)
					System.out.println(funName+": should not come here for Z Violoation: low>high");
				if(c1>c2)
					System.out.println(funName+": should not come here for Z Violoation: c1>c2");
				
				approx_cost = ((c2 - c1)/(s2 - s1))*(alpha*curr_sel[2] - s1) + c1;
				if(approx_cost > (ALPHA_COST*OptimalCost[curr_pos]))
			//	if(OptimalCost[new_pos] > (alpha*OptimalCost[curr_pos]))
				{
			//		System.out.println("\nViolating !!!!!!!!\n");
					ZViolationGrid[curr_pos] = 1;
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
			
			if(low_index[1] == -1 || high_index[1] == -1 )
			{
		//		YViolationGrid[curr_pos] = 1;
				System.out.println("should not come here Y Violoation");
			}
			else
			{
				high_pos = getIndex(high_index, resolution);
				low_pos = getIndex(low_index, resolution);
				
				s1 = selectivity[low_index[1]];
				s2 = selectivity[high_index[1]];
				
				c1 = OptimalCost[low_pos];
				c2 = OptimalCost[high_pos];

				if(low_pos > high_pos || s1>s2)
					System.out.println(funName+": code should not come here for Y Violoation: low>high");
				if(c1>c2 )
					System.out.println(funName+": code should not come here for Y Violoation: c1>c2");
			
				approx_cost = ((c2 - c1)/(s2 - s1))*(alpha*curr_sel[1] - s1) + c1;
				if(approx_cost > (ALPHA_COST*OptimalCost[curr_pos]))
		//		if(OptimalCost[new_pos] > (alpha*OptimalCost[curr_pos]))
				{
		//			System.out.println("\nViolating !!!!!!!!\n");
					YViolationGrid[curr_pos] = 1;
				}
			}
			//------------------------------------------------------------------------------------------- XY -----------------------------------------------
			low_pos = FindNearestPoint(curr_sel,curr_index,selectivity,XY);
			low_index = getCoordinates(dimension,resolution,low_pos);
			high_index[0] = low_index[0];
			high_index[1] = low_index[1];
			high_index[2] = low_index[2];
			
			if(low_index[0] != resolution - 1 && low_index[1] != resolution -1)
			{
				high_index[0] = low_index[0]+1;
				high_index[1] = low_index[1]+1;
			}
			
			high_pos = getIndex(high_index, resolution);
			low_index = getActualLowIndex(low_index,XY);
			high_index = getActualHighIndex(high_index,XY);
			
			if(low_index[0] == -1 || high_index[0] == -1)
			{
		//		XYViolationGrid[curr_pos] = 1;
				System.out.println("should not come here XY Violoation");
			}
			else
			{
				high_pos = getIndex(high_index, resolution);
				low_pos = getIndex(low_index, resolution);
				
				x1 = selectivity[low_index[0]];
				y1 =selectivity[low_index[1]];
				
				x2 = selectivity[high_index[0]];
				y2 = selectivity[low_index[1]];
				
				c1 = OptimalCost[low_pos];
				c2 = OptimalCost[high_pos];

				if(low_pos > high_pos || x1>x2 || y1>y2 || !(x1<=alpha*curr_sel[0]&&alpha*curr_sel[0]<=x2) || !(y1<=alpha*curr_sel[1]&&alpha*curr_sel[1]<=y2))
					System.out.println(funName+": should not come here for XY Violoation: low>high");
				if(c1>c2)
					System.out.println(funName+": should not come here for XY Violoation: c1>c2");
				approx_cost = getEstimateCost(curr_sel[0],curr_sel[1],x1,y1,c1,x2,y2,c2);
				if(approx_cost > (ALPHA_COST*OptimalCost[curr_pos]))
			//	if(OptimalCost[new_pos] > (alpha*OptimalCost[curr_pos]))
				{
			//		System.out.println("\nViolating !!!!!!!!\n");
					XYViolationGrid[curr_pos] = 1;
				}
			}
		//	------------------------------------------------------------------------------------------- YZ -----------------------------------------------
			low_pos = FindNearestPoint(curr_sel,curr_index,selectivity,YZ);
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
				System.out.println("should not come here YZ Violoation");
			}
			else
			{
				high_pos = getIndex(high_index, resolution);
				low_pos = getIndex(low_index, resolution);

				
				y1 = selectivity[low_index[1]];
				z1 =selectivity[low_index[2]];
				
				y2 = selectivity[high_index[1]];
				z2 = selectivity[low_index[2]];
				
				c1 = OptimalCost[low_pos];
				c2 = OptimalCost[high_pos];


				if(low_pos > high_pos || y1>y2 || z1>z2 || !(y1<=alpha*curr_sel[1]&&alpha*curr_sel[1]<=y2) || !(z1<=alpha*curr_sel[2]&&alpha*curr_sel[2]<=z2))
					System.out.println(funName+": should not come here for YZ Violoation: low>high");
				if(c1>c2)
					System.out.println(funName+": should not come here for YZ Violoation: c1>c2");
				
				approx_cost = getEstimateCost(curr_sel[1],curr_sel[2],y1,z1,c1,y2,z2,c2);
				if(approx_cost > (ALPHA_COST*OptimalCost[curr_pos]))
			//	if(OptimalCost[new_pos] > (alpha*OptimalCost[curr_pos]))
				{
			//		System.out.println("\nViolating !!!!!!!!\n");
					YZViolationGrid[curr_pos] = 1;
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
				System.out.println("should not come here XZ Violoation");
			}
			else
			{
				high_pos = getIndex(high_index, resolution);
				low_pos = getIndex(low_index, resolution);

				x1 = selectivity[low_index[0]];
				z1 =selectivity[low_index[2]];
				
				x2 = selectivity[high_index[0]];
				z2 = selectivity[low_index[2]];
				
				c1 = OptimalCost[low_pos];
				c2 = OptimalCost[high_pos];

				if(low_pos > high_pos || !(x1<=alpha*curr_sel[0]&&alpha*curr_sel[0]<=x2) || !(z1<=alpha*curr_sel[2]&&alpha*curr_sel[2]<=z2))
					System.out.println(funName+": should not come here for XZ Violoation: low>high");
				if(c1>c2)
					System.out.println(funName+": should not come here for XZ Violoation: c1>c2");
				
				
				approx_cost = getEstimateCost(curr_sel[0],curr_sel[2],x1,z1,c1,x2,z2,c2);
				if(approx_cost > (ALPHA_COST*OptimalCost[curr_pos]))
			//	if(OptimalCost[new_pos] > (alpha*OptimalCost[curr_pos]))
				{
			//		System.out.println("\nViolating !!!!!!!!\n");
					XZViolationGrid[curr_pos] = 1;
				}
			}
				
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
					XZViolationGrid[i] == 0  //&&
					//YViolationGrid[i] == 1)
					)
			{
				no_of_bad_points ++;
			}
			
		}
		System.out.println("\nBiggest cost :"+OptimalCost[(resolution*resolution) - 1]+"\n");
		int den = total_points - list.size();
		double percent_error = ((double)(no_of_bad_points)*100)/den;
		System.out.println("\n#good_points="+no_of_bad_points+"\t den="+den+"\n Final Percent of points which is satisfies="+percent_error+ "\n");
		
		
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
		String funName="FindNearestPoint";
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
				if(i == base_index[0] && diff < 0)
					System.out.println(funName+": Code should not come here: X viloation");
					
				if(diff <= 0)
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
				if(i == base_index[1] && diff < 0)
					System.out.println(funName+": Code should not come here Y viloation");
				
				if(diff <= 0)
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
				if(i == base_index[2] && diff < 0)
					System.out.println(funName+": Code should not come here: Z viloation");
				
				if(diff <= 0)
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
				if(i == base_index[0] && j==base_index[1] && (diff1 < 0 || diff2<0))
					System.out.println(funName+": Code should not come here: XY viloation");				
				diff = Math.sqrt(Math.pow(diff1,2) + Math.pow(diff2, 2));
				if(diff1 <= 0 && diff2 <= 0)
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
				if(i == base_index[1] && j==base_index[2] && (diff1 < 0 || diff2<0))
					System.out.println(funName+": Code should not come here: YZ viloation");
				diff = Math.sqrt(Math.pow(diff1,2) + Math.pow(diff2, 2));
				if(diff1 <= 0 && diff2 <= 0)
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
				if(i == base_index[0] && j==base_index[2] && (diff1 < 0 || diff2<0))
					System.out.println(funName+": Code should not come here: XZ viloation");
				diff = Math.sqrt(Math.pow(diff1,2) + Math.pow(diff2, 2));
				if(diff1 <= 0 && diff2 <= 0) // changed to equality
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
		String funName="getViolationList";
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
				if(i > right_pos)
					System.out.println(funName+": code should not come here");
			}
			if(index[1] != resolution -1)
			{
				left_index[0] = index[0];
				left_index[1] = index[1] + 1;
				left_index[2] = index[2];
				left_pos = getIndex(left_index, resolution);
				if(i > left_pos)
					System.out.println(funName+": code should not come here");
				
			}
			if(index[2] != resolution -1)
			{
				top_index[0] = index[0];
				top_index[1] = index[1];
				top_index[2] = index[2] +1;
				top_pos = getIndex(top_index, resolution);
				if(i > top_pos)
					System.out.println(funName+": code should not come here");

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
		String funName="getEstimateCost";
		/*
		 * Srinivas's Algorithm
		 */
		double final_estimated_cost;
		double b;
		double alpha_x = curr_sel_x*alpha;
		double alpha_y = curr_sel_y*alpha;
		
		double term1 = Math.pow((alpha_x - x1), 2);
		double term2 = Math.pow((alpha_y - y1),2);
		
		double term3 = Math.pow((x2 - x1), 2);
		double term4 = Math.pow((y2 - y1), 2);
		
		b = Math.sqrt((term1 + term2)) / Math.sqrt((term3 + term4));
		
		final_estimated_cost = (1 - b)*c1 + b*c2;
		
		if(final_estimated_cost > c2 || final_estimated_cost<c1)
			System.out.println(funName+": code should not come here");
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
		String funName="getActualLowIndex";
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
//				index[0] = -1;
//				index[1] = -1;
//				index[2] = -1;
//				
//				return index;
				break;
			}
			index[0] = index[0] - a;
			index[1] = index[1] - b;
			index[2] = index[2] - c;
		}
		if(index[0]>low_index[0] || index[1] >low_index[1] || index[2]>low_index[2] || index[0] < 0 || index[1] < 0 || index[2] <0)
			System.out.println(funName+": code should not come here");
		return index;

			
	}
	
	int [] getActualHighIndex(int [] high_index, int option)
	{
		String funName="getActualHighIndex";
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
//				index[0] = -1;
//				index[1] = -1;
//				index[2] = -1;
//				
//				return index;
				break;
			}
			index[0] = index[0] + a;
			index[1] = index[1] + b;
			index[2] = index[2] + c;
		}
	//	System.out.println("\na = "+a+"\t b="+b+"\t c="+c+"\n");
	//	System.out.println("\n [0] = "+index[0]+"\t [1]="+index[1]+"\t [2]="+index[2]+"\n");
		
		if(index[0] < high_index[0] || index[1] < high_index[1] || index[2] < high_index[2] || index[0] >= resolution || index[1] >= resolution || index[2] >= resolution)
			System.out.println(funName+": code should not come here");
		assert(index[0] < resolution);
		assert(index[1] < resolution);
		assert(index[2] < resolution);
		return index;

			
	}
	
	//######################## Get GDP ############################################################################################################
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
		String funName="getCoordinates";
		int [] index = new int[dimensions];

		for(int i=0; i<dimensions; i++){
			index[i] = location % res;

			location /= res;
		}
		if(dimensions>6 || res>300 || Max(index)>res)
			System.out.println(funName+": code should not come here");
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
	public int Max(int [] V){
		int m=-1;
		for(int i=0;i<V.length;i++){
			if(V[i]>m)
				m=V[i];
		}
		return m;
	}
	public int Min(int [] V){
		int m=99999999;
		for(int i=0;i<V.length;i++){
			if(V[i]<m)
				m=V[i];
		}
		return m;
	}

}