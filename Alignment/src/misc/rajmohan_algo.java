package misc;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Main {
	
	// allidx 157737(U1) noallidx 177255(U2)  3d allidx: 400409/2
	
	static double alpha = 2; 
	static int resolution = 100;								/* 3d vs 2d */
	static double error_bound = 0.01;
	static int saves = 0;
	static double targetvalarr[]; 
	static double error_bound_arr[];
	
	static int opt_calls = 0;
	static int points_found = 0;
	
	static Map<Key2D, Double> map = new HashMap<Key2D, Double>();
	static Map<Key3D, Double> map3d = new HashMap<Key3D, Double>();
	
	static double[] cost_arr;
	static int[] plans_arr;
	static XYSeries[] series;
	
	/* get number of contours and initialize related objects */
	static void get_contours()
	{
		double max_cost;
		double cur_cost;
		int noofcontours = 0;
		int i;
		
		max_cost = getcost2d(resolution - 1, resolution - 1);
//		max_cost = getcost3d(resolution - 1, resolution - 1, resolution - 1);		/* 3d vs 2d */
		cur_cost = max_cost;
		while(cur_cost > 2000)
		{
			cur_cost = cur_cost/alpha;
			noofcontours++;
		}
	
		targetvalarr = new double[noofcontours];
		error_bound_arr = new double[noofcontours];
		series = new XYSeries[noofcontours];
		
		cur_cost = max_cost;
		i = 0;
		while(cur_cost > 2000)
		{
			cur_cost = cur_cost/alpha;
			targetvalarr[i] = cur_cost;
			error_bound_arr[i] = cur_cost * error_bound;
			i++;
		}
	}
	public static void main(String[] args) throws FileNotFoundException
	{
		System.out.println("!");
		read_cost_file();
		get_contours();
		for(int i = 0; i < targetvalarr.length; i++)
		{
			series[i] = new XYSeries("Contour "+ i);

			System.out.println("\n\n****** QUEST *********");
			run_new_seed_2d_algorithm(targetvalarr[i], error_bound_arr[i],series[i]);	/* 3d vs 2d */
			System.out.println("\n\n****** END OF QUEST *********");

//			System.out.println("\n\n ****** NAIVE ********** ");
//			run_naive_2d_algorithm(targetvalarr[i], error_bound_arr[i], series[i]);  /* 3d vs 2d */
//			System.out.println("\n\n****** END OF NAIVE *********");
			
			System.out.println("# of optimizer calls = " + opt_calls);
			System.out.println("# of points found = " + points_found);
			System.out.println("# saves = " + saves);
//			function_evals = 0;
//			points_found = 0;
		}
		
//		//print size
//		System.out.println(map.size());
// 
//		//loop HashMap
//		for (Entry<Key2D, Double> entry : map.entrySet()) {
//			System.out.println(entry.getKey().toString() + " - " + entry.getValue());
//		}
		
		/* Plot the points */
		final XYSeriesPlot objXYSeriesPlot = new XYSeriesPlot();
        objXYSeriesPlot.pack();
        RefineryUtilities.centerFrameOnScreen(objXYSeriesPlot);
        objXYSeriesPlot.setVisible(true);
	}
	
	static void run_naive_2d_algorithm(double targetval, double errorboundval, XYSeries tseries)
	{
		int cur_x = 0, cur_y = resolution - 1;
		double cur_val;
		
		for(cur_x = 0; cur_x < resolution; cur_x++)
		{
			for(cur_y = 0; cur_y < resolution; cur_y++)
			{
				cur_val = getcost2d(cur_x, cur_y);
				if(cur_val >= targetval - errorboundval && cur_val <= targetval + errorboundval)
				{
					points_found++;
					System.out.println("x = " + cur_x + ", y = " + cur_y + ", F = " + cur_val);
					tseries.add(cur_x,cur_y);
				}	
			}
		}
	}
	
	
	static void run_naive_3d_algorithm(double targetval, double errorboundval, XYSeries tseries)
	{
		int cur_x = 0, cur_y = resolution - 1, cur_z = 0;
		double cur_val;
		
		for(cur_z = 0; cur_z < resolution; cur_z++)
		{
			for(cur_x = 0; cur_x < resolution; cur_x++)
			{
				for(cur_y = resolution-1; cur_y >= 0 ; cur_y--)
				{
					cur_val = getcost3d(cur_x, cur_y, cur_z);

					if(cur_val >= targetval - errorboundval && cur_val <= targetval + errorboundval)
					{
						points_found++;
						System.out.println("x = " + cur_x + ", y = " + cur_y + ", z = " + cur_z + ", F = " + cur_val);
						tseries.add(cur_x,cur_y);
					}	
				}
			}
		}
	}
	static void run_seed_3d_algorithm(double targetval,double errorboundval, XYSeries tseries)
	{
		int cur_x = 0, cur_y = resolution - 1, cur_z = 0;
		double cur_val;
		
		while(cur_z < resolution)
		{	
			cur_x = 0; cur_y = resolution - 1;
			
			/* find min and max of a constant z-slice. check targetval is within 
			 * this range. Otherwise skip this z-slice
			 */
			double min_cost_slice = getcost3d(0, 0, cur_z);
			double max_cost_slice = getcost3d(resolution-1, resolution-1, cur_z);
			if(!(targetval >= min_cost_slice && targetval <= max_cost_slice))
			{
				cur_z++;
				saves++;
				continue;
			}
			
			/* if top left cost is more than target do binary search on left edge of 2d not on top edge*/
			cur_val = getcost3d(0, resolution-1, cur_z);
			if(cur_val > targetval)
			{
				int low = 0, high = resolution - 1, mid = resolution - 1;
				/* do a binary search on top row to find out starting point */
				while(low < high)
				{
					mid = (low + high) / 2;
					cur_val = getcost3d(cur_x, mid, cur_z);
					if(cur_val >= targetval && cur_val <= targetval)
					{
						cur_y = mid;
						break;
					}
					else if(cur_val < targetval)
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
						cur_y = mid + 1;
				}
			}
			else
			{
				int low = 0, high = resolution - 1, mid = 0;
				/* do a binary search on top row to find out starting point */
				while(low < high)
				{
					mid = (low+high) / 2;
					cur_val = getcost3d(mid, cur_y, cur_z);
					if(cur_val >= targetval && cur_val <= targetval)
					{
						cur_x = mid;
						break;
					}
					else if(cur_val < targetval)
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
						cur_x = mid-1;
				}
			}
				
			
			/* do until you cross boundary starting from start point*/
			while(cur_x < resolution && cur_y >= 0)
			{
				
				cur_val = getcost3d(cur_x, cur_y, cur_z);
				
				if(cur_val >= targetval - errorboundval && cur_val <= targetval + errorboundval)
				{
					
					/* when you hit a interesting point, just keep going on x axis as long as it is interesting
					 * then backtrack to exactly where you started getting interesting points */
					int t_x = cur_x;
					while(cur_val >= targetval - errorboundval && cur_val <= targetval + errorboundval)
					{
						System.out.println("x = " + t_x + ", y = " + cur_y + ", z = " + cur_z + ", P" + get3dplannumber(t_x, cur_y, cur_z));
						tseries.add(t_x,cur_y);
						points_found++;
						
						t_x++;
						if(t_x < resolution) 
							cur_val = getcost3d(t_x, cur_y, cur_z);
						else
						{
							/* to break outer while loop as well set cur_x = resolution */
							cur_x = resolution;
							break;
						}
					}
				    cur_y--;
				}
				else if(cur_val < targetval)
				{
					cur_x++;
				}
				else if(cur_val > targetval)
				{
					cur_y--;
				}
			}
			
			cur_z++;
		}
	}
	static void run_new_seed_3d_algorithm(double targetval,double errorboundval, XYSeries tseries)
	{
		int cur_x1 = 0, cur_y1 = resolution - 1, cur_z = 0;

		double cur_val;
		
		while(cur_z < resolution)
		{		
			/* find min and max of a constant z-slice. check targetval is within 
			 * this range. Otherwise skip this z-slice
			 */
			double min_cost_slice = getcost3d(0, 0, cur_z);
			double max_cost_slice = getcost3d(resolution-1, resolution-1, cur_z);
			if(!(targetval >= min_cost_slice && targetval <= max_cost_slice))
			{
				cur_z++;
				saves++;
				continue;
			}
			
			cur_x1 = 0;
			cur_y1 = resolution - 1;
			
			/* if top left cost is more than target do binary search on left edge of 2d not on top edge*/
			cur_val =  getcost3d(0, resolution-1, cur_z);
			
			if(cur_val > targetval)
			{
				int low = 0, high = resolution - 1, mid = resolution - 1;
				
				/* do a binary search on TOP edge to find out starting Y point */
				while(low < high)
				{
					mid = (low + high) / 2;
					
					cur_val = getcost3d( cur_x1, mid,cur_z);
	
					if(cur_val >= targetval && cur_val <= targetval)
					{
						cur_y1 = mid;
						break;
					}
					else if(cur_val < targetval)
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
				
				/* do a binary search on LEFT edge to find out starting X point */
				while(low < high)
				{
					mid = (low+high) / 2;
					
					cur_val = getcost3d( mid, cur_y1,cur_z);
					
					if(cur_val >= targetval && cur_val <= targetval)
					{
						cur_x1 = mid;
						break;
					}
					else if(cur_val < targetval)
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
			
			/* Fix the last point of the contour by starting from backwards */
			int cur_x2 = resolution - 1, cur_y2 = 0;
			double cur_val2;

			cur_val2 = getcost3d( cur_x2, cur_y2,cur_z);
			
			if(cur_val2 > targetval)
			{
				int low = 0, high = resolution - 1, mid = resolution - 1;
				
				/* do a binary search on bottom row to find out starting point */
				while(low < high)
				{
					mid = (low + high) / 2;
					cur_val2 = getcost3d( mid, cur_y2, cur_z);
					
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
					cur_val2 = getcost3d( cur_x2, mid, cur_z);

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
				
			/* do until you cross boundary starting from start point*/
			while(cur_x1 < resolution && cur_y1 >= 0)
			{
//				System.out.println("x = " + cur_x + ", y = " + cur_y);

				cur_val = getcost3d(cur_x1, cur_y1, cur_z);
				
				if(cur_val >= targetval - errorboundval && cur_val <= targetval + errorboundval)
				{
					/* when you hit a interesting point, just keep going on x axis as long as it is interesting
					 * then backtrack to exactly where you started getting interesting points */
					int t_y1 = cur_y1;
								
					while(cur_val >= targetval - errorboundval && cur_val <= targetval + errorboundval)
					{
						cur_y1 = t_y1;
						tseries.add(cur_x1,cur_y1);
						System.out.println("x = " + cur_x1 + ", y = " + cur_y1 + ", P" + get2dplannumber(cur_x1, cur_y1));
						points_found++;
						
						if(t_y1 <= cur_y2)
							break;
						t_y1 = t_y1 - 1;
						cur_val = getcost3d(cur_x1, t_y1, cur_z);
					}
				    cur_x1++;
				}
				else if(cur_val > targetval & cur_y1 > cur_y2)
				{
					cur_y1--;
				}
				else
				{
					cur_x1++;
				}
				
			}
		
			cur_z++;
		}
	}
	static int get2dplannumber(int x, int y)
	{
		return plans_arr[x + y * resolution];
	}
	static int get3dplannumber(int x, int y, int z)
	{
		return plans_arr[x + y * resolution + z * resolution * resolution];
	}
	static void run_new_seed_2d_algorithm(double targetval,double errorboundval, XYSeries tseries)
	{
		/* Start from Top-Left Corner */
		int cur_x1 = 0, cur_y1 = resolution - 1;
		double cur_val1;

		/* if top left cost is more than target do binary search on left edge of 2d not on top edge*/
		cur_val1 = getcost2d(0, resolution-1);
		if(cur_val1 > targetval)
		{
			int low = 0, high = resolution - 1, mid = resolution - 1;
			/* do a binary search on top row to find out starting point */
			while(low < high)
			{
				mid = (low + high) / 2;
				cur_val1 = getcost2d(cur_x1, mid);
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
				cur_val1 = getcost2d(mid, cur_y1);
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
		cur_val2 = getcost2d(cur_x2, cur_y2);
		if(cur_val2 > targetval)
		{
			int low = 0, high = resolution - 1, mid = resolution - 1;
			
			/* do a binary search on bottom row to find out starting point */
			while(low < high)
			{
				mid = (low + high) / 2;
				cur_val2 = getcost2d(mid, cur_y2);
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
				cur_val2 = getcost2d(cur_x2, mid);
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
		
		/* do until you cross boundary starting from start point*/
		while(cur_x1 < resolution && cur_y1 >= 0)
		{
//			System.out.println("x = " + cur_x + ", y = " + cur_y);

			cur_val1 = getcost2d(cur_x1, cur_y1);
			
			if(cur_val1 >= targetval - errorboundval && cur_val1 <= targetval + errorboundval)
			{
				/* when you hit a interesting point, just keep going on x axis as long as it is interesting
				 * then backtrack to exactly where you started getting interesting points */
				int t_y1 = cur_y1;
							
				while(cur_val1 >= targetval - errorboundval && cur_val1 <= targetval + errorboundval)
				{
					cur_y1 = t_y1;
					tseries.add(cur_x1,cur_y1);
					System.out.println("x = " + cur_x1 + ", y = " + cur_y1 + ", P" + get2dplannumber(cur_x1, cur_y1));
					points_found++;
					
					if(t_y1 <= cur_y2)
						break;
					t_y1 = t_y1 - 1;
					cur_val1 = getcost2d(cur_x1, t_y1);
				}
			    cur_x1++;
			}
			else if(cur_val1 > targetval & cur_y1 > cur_y2)
			{
				cur_y1--;
			}
			else
			{
				cur_x1++;
			}
		}
	}
	
	static void run_modified_seed_2d_algorithm(double targetval,double errorboundval, XYSeries tseries)
	{
		/* Start from Top-Left Corner */
		int cur_x1 = 0, cur_y1 = resolution - 1;
		double cur_val1;

		/* if top left cost is more than target do binary search on left edge of 2d not on top edge*/
		cur_val1 = getcost2d(0, resolution-1);
		if(cur_val1 > targetval)
		{
			int low = 0, high = resolution - 1, mid = resolution - 1;
			/* do a binary search on top row to find out starting point */
			while(low < high)
			{
				mid = (low + high) / 2;
				cur_val1 = getcost2d(cur_x1, mid);
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
				cur_val1 = getcost2d(mid, cur_y1);
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

		/* if top left cost is more than target do binary search on left edge of 2d not on top edge*/
		cur_val2 = getcost2d(cur_x2, cur_y2);
		if(cur_val2 > targetval)
		{
			int low = 0, high = resolution - 1, mid = resolution - 1;
			
			/* do a binary search on bottom row to find out starting point */
			while(low < high)
			{
				mid = (low + high) / 2;
				cur_val2 = getcost2d(mid, cur_y2);
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
				cur_val2 = getcost2d(cur_x2, mid);
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
		
		/* get me the last point */
		while(cur_y2 < resolution && cur_x2 >= 0)
		{
//			System.out.println("x = " + cur_x + ", y = " + cur_y);

			cur_val2 = getcost2d(cur_x2, cur_y2);
			
			if(cur_val2 >= targetval - errorboundval && cur_val2 <= targetval + errorboundval)
			{
				/* when you hit a interesting point, just keep going on x axis as long as it is interesting
				 * then backtrack to exactly where you started getting interesting points */
//				int t_x = cur_x2;
//				while(cur_val2 >= targetval - errorboundval && cur_val2 <= targetval + errorboundval)
//				{
//					tseries.add(t_x,cur_y2);
//					System.out.println("x = " + t_x + ", y = " + cur_y2 + ", P" + get2dplannumber(t_x, cur_y2));
//					points_found++;
//					
//					t_x--;
//					if(t_x >= 0) 
//						cur_val2 = getcost2d(t_x, cur_y2);
//					else
//					{
//						/* to break outer while loop as well set cur_x = resolution */
//						cur_x2 = -1;
//						break;
//					}
//				}
//			    cur_y2++;
				break;
			}
			else if(cur_val2 < targetval)
			{
				cur_y2++;
			}
			else if(cur_val2 > targetval)
			{
				cur_x2--;
			}
		}
		
		
		/* do until you cross boundary starting from start point*/
		while(cur_x1 < resolution && cur_y1 >= 0)
		{
//			System.out.println("x = " + cur_x + ", y = " + cur_y);

			if(cur_y1 < cur_y2)
				break;
			cur_val1 = getcost2d(cur_x1, cur_y1);
			
			if(cur_val1 >= targetval - errorboundval && cur_val1 <= targetval + errorboundval)
			{

				/* when you hit a interesting point, just keep going on x axis as long as it is interesting
				 * then backtrack to exactly where you started getting interesting points */
				int t_x = cur_x1;
				
				tseries.add(t_x,cur_y1);
				System.out.println("x = " + t_x + ", y = " + cur_y1 + ", P" + get2dplannumber(t_x, cur_y1));
				points_found++;
				
				if(cur_y1 <= cur_y2+1)
				{
					while(cur_val1 >= targetval - errorboundval && cur_val1 <= targetval + errorboundval)
					{
						tseries.add(t_x,cur_y1);
						System.out.println("x = " + t_x + ", y = " + cur_y1 + ", P" + get2dplannumber(t_x, cur_y1));
						points_found++;
						
						t_x++;
						if(t_x < resolution) 
							cur_val1 = getcost2d(t_x, cur_y1);
						else
						{
							/* to break outer while loop as well set cur_x = resolution */
							cur_x1 = resolution;
							break;
						}
					}
					if(cur_y1 == cur_y2)
					{
						t_x = cur_x2-1;
						while(t_x > cur_x1 )
						{
							cur_val1 = getcost2d(t_x, cur_y1);
							if(cur_val1 >= targetval - errorboundval && cur_val1 <= targetval + errorboundval)
							{
								tseries.add(t_x,cur_y1);
								System.out.println("x = " + t_x + ", y = " + cur_y1 + ", P" + get2dplannumber(t_x, cur_y1));
								points_found++;
								t_x--;
							}
							else
								break;
						}
					}
				}
				
			    cur_y1--;
			}
			else if(cur_val1 < targetval)
			{
				cur_x1++;
			}
			else if(cur_val1 > targetval)
			{
				cur_y1--;
			}
		}
		
	}
	static void run_doublesided_seed_2d_algorithm(double targetval,double errorboundval, XYSeries tseries)
	{
		/* Start from Top-Left Corner */
		int cur_x1 = 0, cur_y1 = resolution - 1;
		double cur_val1;

		/* if top left cost is more than target do binary search on left edge of 2d not on top edge*/
		cur_val1 = getcost2d(0, resolution-1);
		if(cur_val1 > targetval)
		{
			int low = 0, high = resolution - 1, mid = resolution - 1;
			/* do a binary search on top row to find out starting point */
			while(low < high)
			{
				mid = (low + high) / 2;
				cur_val1 = getcost2d(cur_x1, mid);
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
				cur_val1 = getcost2d(mid, cur_y1);
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

		/* if top left cost is more than target do binary search on left edge of 2d not on top edge*/
		cur_val2 = getcost2d(cur_x2, cur_y2);
		if(cur_val2 > targetval)
		{
			int low = 0, high = resolution - 1, mid = resolution - 1;
			
			/* do a binary search on bottom row to find out starting point */
			while(low < high)
			{
				mid = (low + high) / 2;
				cur_val2 = getcost2d(mid, cur_y2);
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
				cur_val2 = getcost2d(cur_x2, mid);
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
		
		
		/* get me the last point */
		while(cur_y2 < resolution && cur_x2 >= 0)
		{
//			System.out.println("x = " + cur_x + ", y = " + cur_y);

			cur_val2 = getcost2d(cur_x2, cur_y2);
			
			if(cur_val2 >= targetval - errorboundval && cur_val2 <= targetval + errorboundval)
			{
				/* when you hit a interesting point, just keep going on x axis as long as it is interesting
				 * then backtrack to exactly where you started getting interesting points */
//				int t_x = cur_x2;
//				while(cur_val2 >= targetval - errorboundval && cur_val2 <= targetval + errorboundval)
//				{
//					tseries.add(t_x,cur_y2);
//					System.out.println("x = " + t_x + ", y = " + cur_y2 + ", P" + get2dplannumber(t_x, cur_y2));
//					points_found++;
//					
//					t_x--;
//					if(t_x >= 0) 
//						cur_val2 = getcost2d(t_x, cur_y2);
//					else
//					{
//						/* to break outer while loop as well set cur_x = resolution */
//						cur_x2 = -1;
//						break;
//					}
//				}
//			    cur_y2++;
				break;
			}
			else if(cur_val2 < targetval)
			{
				cur_y2++;
			}
			else if(cur_val2 > targetval)
			{
				cur_x2--;
			}
		}
		
		
		/* do until you cross boundary starting from start point*/
		while(cur_x1 < resolution && cur_y1 >= 0 && cur_x2 >=0 && cur_y2 < resolution)
		{
//			System.out.println("x = " + cur_x + ", y = " + cur_y);

			if(cur_y1 < cur_y2)
				break;
			
			cur_val1 = getcost2d(cur_x1, cur_y1);
			
			if(cur_val1 >= targetval - errorboundval && cur_val1 <= targetval + errorboundval)
			{

				/* when you hit a interesting point, just keep going on x axis as long as it is interesting
				 * then backtrack to exactly where you started getting interesting points */
				int t_x = cur_x1;
				
				tseries.add(t_x,cur_y1);
				System.out.println("x = " + t_x + ", y = " + cur_y1 + ", P" + get2dplannumber(t_x, cur_y1));
				points_found++;
				
				if(cur_y1 <= cur_y2+1)
				{
					double new_val1 = getcost2d(cur_x1, cur_y1-1);
					if(Math.abs(new_val1 - targetval) < Math.abs(cur_val1 - targetval) )
					{
						
					}
					else
					{
						while(cur_val1 >= targetval - errorboundval && cur_val1 <= targetval + errorboundval)
						{
							tseries.add(t_x,cur_y1);
							System.out.println("x = " + t_x + ", y = " + cur_y1 + ", P" + get2dplannumber(t_x, cur_y1));
							points_found++;
							
							t_x++;
							if(t_x < resolution) 
								cur_val1 = getcost2d(t_x, cur_y1);
							else
							{
								/* to break outer while loop as well set cur_x = resolution */
								cur_x1 = resolution;
								break;
							}
						}
					}
				}
			    cur_y1--;
			}
			else if(cur_val1 < targetval)
			{
				cur_x1++;
			}
			else if(cur_val1 > targetval)
			{
				cur_y1--;
			}
		}
		
	}
	static void run_seed_2d_algorithm(double targetval,double errorboundval, XYSeries tseries)
	{
		/* Start from Top-Left Corner */
		int cur_x = 0, cur_y = resolution - 1;
		double cur_val;

		/* if top left cost is more than target do binary search on left edge of 2d not on top edge*/
		cur_val = getcost2d(0, resolution-1);
		if(cur_val > targetval)
		{
			int low = 0, high = resolution - 1, mid = resolution - 1;
			/* do a binary search on top row to find out starting point */
			while(low < high)
			{
				mid = (low + high) / 2;
				cur_val = getcost2d(cur_x, mid);
				if(cur_val >= targetval && cur_val <= targetval)
				{
					cur_y = mid;
					break;
				}
				else if(cur_val < targetval)
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
					cur_y = mid + 1;
			}
		}
		else
		{
			int low = 0, high = resolution - 1, mid = 0;
			/* do a binary search on top row to find out starting point */
			while(low < high)
			{
				mid = (low+high) / 2;
				cur_val = getcost2d(mid, cur_y);
				if(cur_val >= targetval && cur_val <= targetval)
				{
					cur_x = mid;
					break;
				}
				else if(cur_val < targetval)
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
					cur_x = mid-1;
			}
		}
		
		/* do until you cross boundary starting from start point*/
		while(cur_x < resolution && cur_y >= 0)
		{
//			System.out.println("x = " + cur_x + ", y = " + cur_y);

			cur_val = getcost2d(cur_x, cur_y);
			
			if(cur_val >= targetval - errorboundval && cur_val <= targetval + errorboundval)
			{

				/* when you hit a interesting point, just keep going on x axis as long as it is interesting
				 * then backtrack to exactly where you started getting interesting points */
				int t_x = cur_x;
				while(cur_val >= targetval - errorboundval && cur_val <= targetval + errorboundval)
				{
					tseries.add(t_x,cur_y);
					System.out.println("x = " + t_x + ", y = " + cur_y + ", P" + get2dplannumber(t_x, cur_y));
					points_found++;
					
					t_x++;
					if(t_x < resolution) 
						cur_val = getcost2d(t_x, cur_y);
					else
					{
						/* to break outer while loop as well set cur_x = resolution */
						cur_x = resolution;
						break;
					}
				}
			    cur_y--;
			}
			else if(cur_val < targetval)
			{
				cur_x++;
			}
			else if(cur_val > targetval)
			{
				cur_y--;
			}
		}
	}
	
	static double getcost2d(int x, int y)
	{
		double function_val;

		if(map.containsKey(new Key2D(x,y)) == false)
		{
			function_val = cost_arr[x + y * resolution];
			opt_calls++;
			map.put(new Key2D(x,y), function_val);
		}
		else
		{
			saves++;
			function_val = map.get(new Key2D(x,y));
		}
		
		
		return function_val;
	}
	
	static double getcost3d(int x, int y, int z)
	{
		double function_val;

		if(map.containsKey(new Key3D(x,y,z)) == false)
		{
			function_val = cost_arr[x + y * resolution + (z * resolution * resolution )];
			opt_calls++;
			map3d.put(new Key3D(x,y,z), function_val);
		}
		else
			function_val = map.get(new Key3D(x,y,z));
		
		return function_val;
	}
	
	static void read_cost_file()
	{
		ObjectInputStream objISCost = null, objISPlan = null;
		try {
			objISCost = new ObjectInputStream(new FileInputStream
					(new File("/home/dsladmin/quest/picasso/packets/tpch2_exp_2d_100/tpch2_exp_2d_100.cost")));  /* 3d vs 2d */
			objISPlan = new ObjectInputStream(new FileInputStream
					(new File("/home/dsladmin/quest/picasso/packets/tpch2_exp_2d_100/tpch2_exp_2d_100.plan")));
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			cost_arr = (double[]) objISCost.readObject();
			plans_arr = (int[]) objISPlan.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Cost file read!");
	}
}


class XYSeriesPlot extends ApplicationFrame {
	
	private static final long serialVersionUID = 1L;

	public XYSeriesPlot() {
    	
        super("TPC-H Q3");

		IntervalXYDataset[] dataset = new IntervalXYDataset[Main.targetvalarr.length];
		XYSeriesCollection data = null;
		final XYLineAndShapeRenderer[] renderer= new XYLineAndShapeRenderer[Main.targetvalarr.length];
				
		for(int i = 0; i < Main.targetvalarr.length; i++)
		{	
		    final XYSeries xy_series = Main.series[i];
		    
		    data = new XYSeriesCollection(xy_series);
		    renderer[i] = new XYLineAndShapeRenderer(false, false);
		    renderer[i].setBaseShapesVisible(true);
		    dataset[i] = data;
		}
		
		final NumberAxis xAxis = new NumberAxis("orders");
		final ValueAxis yAxis = new NumberAxis("lineitem");
//		xAxis.setTickUnit(new NumberTickUnit(Main.resolution));
		xAxis.setUpperBound(Main.resolution);
		xAxis.setLowerBound(0);
		yAxis.setUpperBound(Main.resolution);
		yAxis.setLowerBound(0);
		
		// Assign it to the chart
		XYPlot plot = new XYPlot(dataset[0], xAxis, yAxis, renderer[0]);//(XYPlot) chart.getPlot();
		plot.setDomainAxis(xAxis);
		plot.setRangeAxis(yAxis);
		
		for(int i = 1; i < Main.targetvalarr.length; i++)
		{
			plot.setDataset(i, dataset[i]);
			plot.setRenderer(i, renderer[i]);
		}
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
		JFreeChart chart = new JFreeChart("Contours", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		setContentPane(chartPanel);

    }
}

class DataPoints
{
	public int location;
	public int contour_no;
	public int plan_no;
	public int cost;
}


