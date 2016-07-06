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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
//import java.util.Set;
import java.util.Comparator;

import org.apache.commons.lang3.builder.CompareToBuilder;

import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;


public class checkingCostgreedyFPC {

	DataValues[] data; 
	DataValues[] data_red;
	int [] plans;
	int [] plans_red;
	double [] OptimalCost;
	double [] OptimalCost_red;
	String prefixPath = new String("/home/dsladmin/Srinivas/data/others/testingDSQT916D/");
	String folderPath = new String("/home/dsladmin/Srinivas/data/DSQT916DR10_E/");
	int resolution = 10;
	ArrayList<Integer> red_points = new ArrayList<Integer>();
	ArrayList<Integer> red_plans = new ArrayList<Integer>();
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
     checkingCostgreedyFPC obj = new checkingCostgreedyFPC();
     obj.run();
	}

	private void run() throws IOException {
		
		CostGreedyGCI3D gci3d = new CostGreedyGCI3D();
		String path =  new String(prefixPath+"DSQT916DR10_E.apkt");
		String path_red =  new String(prefixPath+"DSQT916DR10_E_Red.apkt");
		
		ADiagramPacket gdp = gci3d.getGDP(new File(path));
		ADiagramPacket gdp_red = gci3d.getGDP(new File(path_red));
		
		readpkt(gdp,false, 115);
		readpkt(gdp_red,true, 115);
		Integer [] interestingPlans = {98,185,207,416,431,505,553,721,1441}; 
		for(Integer plans : interestingPlans){
		if(red_points!=null)
			red_points.clear();
		ArrayList<Integer> bad_points = new ArrayList<Integer>();
		readpkt(gdp_red,true, plans.intValue());
		for(Integer p : red_plans){
			for(Integer pts : red_points){
				if(data_red[pts.intValue()].getPlanNumber()!=p.intValue()){
					if(fpc_cost_generic(pts.intValue(), p.intValue()) <= 1.3 * data[pts.intValue()].getCost()){
						double thr = fpc_cost_generic(pts.intValue(), p.intValue())/data[pts.intValue()].getCost();
						//System.out.print(" problem at location "+pts.intValue()+" from plan "+p.intValue()+" threshold "+thr);
						//System.out.println(" but the replaced plans threshold is "+data_red[pts.intValue()].getCost()/data[pts.intValue()].getCost());
						if(!bad_points.contains(pts))
							bad_points.add(pts);
					}
				}
			}
		}
		
		System.out.println("The no. of bad points is "+bad_points.size());
		}
		
		
	}
	
	private void readpkt(ADiagramPacket gdp, boolean red, int plan_no) throws IOException
	{
		//ADiagramPacket gdp = getGDP(new File(pktPath));
		int totalPlans = gdp.getMaxPlanNumber();
		int dimension = gdp.getDimension();
		int resolution = gdp.getMaxResolution();
		
		
		if(!red)
			data = gdp.getData();
		else
			data_red = gdp.getData();
		
		int totalPoints = (int) Math.pow(resolution, dimension);
		//System.out.println("\nthe total plans are "+totalPlans+" with dimensions "+dimension+" and resolution "+resolution);

		assert (totalPoints==data.length) : "Data length and the resolution didn't match !";

		if(!red)
			plans = new int [data.length];
		else 
			plans_red = new int [data_red.length];
		
		if(!red)
			OptimalCost = new double [data.length];
		else
			OptimalCost_red = new double [data_red.length];

		if(!red){
			for (int i = 0;i < data.length;i++)
			{
				this.OptimalCost[i]= data[i].getCost();
				this.plans[i] = data[i].getPlanNumber();
			}
		}
		else{
			for (int i = 0;i < data_red.length;i++)
			{
				this.OptimalCost_red[i]= data_red[i].getCost();
				this.plans_red[i] = data_red[i].getPlanNumber();
				if(data_red[i].getPlanNumber() == plan_no){
					red_points.add(new Integer(i));
				}
				if(!red_plans.contains(new Integer(data_red[i].getPlanNumber())))
						red_plans.add(new Integer(data_red[i].getPlanNumber()));
			}
		}
		System.out.println("The size of the points cover by plan no." + plan_no +" is "+red_points.size());
		
		// get all the points for a specific replaced plan
		
		
		
		
	}
	
	private double getOptimalCost(int[] index, boolean red){
		if(!red)
			return this.OptimalCost[getIndex(index,resolution)];
		else
			return this.OptimalCost_red[getIndex(index,resolution)];
	}
	
	
	
	double fpc_cost_generic(int index, int plan)
	{
	 
		//int index = getIndex(arr,resolution);
		double[] cost = new double[data.length];
		try {

			ObjectInputStream ip = new ObjectInputStream(new FileInputStream(new File(folderPath + plan + ".pcst")));
			cost = (double[]) ip.readObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return cost[index];
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
}
