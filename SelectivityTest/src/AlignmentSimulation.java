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

import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;

public class AlignmentSimulation {

	GCI3D obj;
	double selectivity [];
	static int matching_count = 0;
	
	public static void main(String[] args) {
		
		AlignmentSimulation as = new AlignmentSimulation();
		
		as.run();
	}
	
	public void run() {
		
		initialize();
		
		loadSelectivity();
		
		checkGradientAssumption();
		
		checkNearbySelectivity();
	}
	
	public void checkGradientAssumption() {

		int violation_count =0;
		int [] coordinates = new int[obj.dimension];
		for(int i=0;i<obj.totalPoints;i++){
			coordinates = obj.getCoordinates(obj.dimension, obj.resolution, i);
			int loc = obj.getIndex(coordinates, obj.resolution);
			for(int j=0;j<obj.dimension;j++){
				if(coordinates[j]<obj.dimension-1){
					coordinates[j]++;
					int loc_new = obj.getIndex(coordinates, obj.resolution);
					if((double)(obj.OptimalCost[loc_new]/obj.OptimalCost[loc])>(double)(obj.selectivity[coordinates[j]]/obj.selectivity[coordinates[j]-1]))
						violation_count++;
					coordinates[j]--;
				}
			}
		}
		System.out.println("the violation count is "+violation_count);
	}
	
	public void checkNearbySelectivity(int location, int dim, double alpha) {
		
		int [] coordinates = new int[obj.dimension];
		coordinates = obj.getCoordinates(obj.dimension, obj.resolution, location);
		int[] min_index = new int[obj.dimension];
		int[] max_index = new int[obj.dimension];
		
		//updating the min_index
		for(int j=0;j<obj.dimension;j++){
			if(j!=dim)
				min_index[j] = coordinates[j];
			else
				min_index[j] = obj.findNearestPoint(obj.findNearestSelectivity((double)(obj.selectivity[coordinates[j]]/alpha)));
		}
		
		//updating the max_index
		for(int j=0;j<obj.dimension;j++){
				max_index[j] = obj.findNearestPoint(obj.findNearestSelectivity((double)(obj.selectivity[coordinates[j]]*alpha)));
		}
		
		//check if there is a point (plan) spilling on dimension 'dim' in between min and max index
		matching_count = 0;
		ArrayList<Integer> dims_exhausted = new ArrayList<Integer>();  
		int [] point = new int[obj.dimension]; 
		checkAllPoints(min_index,max_index,dims_exhausted,point, dim);
	}
	
	public void checkAllPoints(int[] min_index, int[] max_index, ArrayList<Integer> dims_exhausted, int[] point, int dim) {
		

		ArrayList<Integer> remainingDimList = new ArrayList<Integer>();
		for(int i=0;i<obj.dimension;i++)
		{
			if(!dims_exhausted.contains(new Integer(i)))
			{
				remainingDimList.add(new Integer(i));
			}
		}

		if(remainingDimList.size() > 0) 
		{	
			int curr_dim = remainingDimList.get(0);
			int curr_index = max_index[curr_dim];
			while(curr_index >= min_index[curr_dim])
			{
				dims_exhausted.add(curr_dim);
				point[curr_dim] = curr_index;
				if(dims_exhausted.size()==obj.dimension)
				{	//check if the point is spilling on dimension 'dim' 
					int loc = obj.getIndex(point, obj.resolution);
					if(obj.plans[loc] == dim)
						matching_count ++;
				}
				checkAllPoints(min_index, max_index, dims_exhausted, point, dim);
				dims_exhausted.remove(dims_exhausted.indexOf(curr_dim));
				curr_index--;
			}
		}
	}

	public void loadSelectivity() {

		double r=1.0;
		obj.selectivity = new double[obj.resolution];
		double startpoint = 0.0, endpoint = 1.0;
		
		switch(obj.resolution)
		{
			case 10:
				r=PicassoConstants.QDIST_SKEW_10;
				break;
			case 30:
				r=PicassoConstants.QDIST_SKEW_30;
				break;
			case 100:
				r=PicassoConstants.QDIST_SKEW_100;
				break;
			case 300:
				r=PicassoConstants.QDIST_SKEW_300;
				break;
			case 1000:
				r=PicassoConstants.QDIST_SKEW_1000;
				break;
		}
		int i;
		
		int popu= obj.resolution;
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
			obj.selectivity[i-1] = startpoint + sum;
		//	constants[i-1] = getConstant(selectivity[i-1]);
			//System.out.println("Sel="+df.format(100*selectivity[i-1])+"\tConstant="+constants[i-1]);
			curval*=r;
			if(i!=popu)
			sum+=(curval * (endpoint - startpoint));
			else
				sum+=(curval * (endpoint - startpoint))/2;
		}

		
	}
	private void initialize() {

		obj.dimension = 2;
		obj.resolution = 100;
		obj.totalPoints = (int)Math.pow(obj.resolution, obj.dimension);
		obj.totalPlans = obj.dimension;
		
		//populating the cost function
		Random random = new Random();
		for (int i = 0;i < obj.totalPoints;i++)
		{
			obj.OptimalCost[i]= costFunction(i,obj.dimension,obj.resolution);
			obj.plans[i] = random.nextInt(obj.dimension);

		}
		
		

	}
	private double costFunction(int i, int dimension, int resolution) {
		
		int coordinates [] = new int[dimension];
		coordinates = obj.getCoordinates(dimension, resolution, i);
		double cost = 0;
		for (int j=0;j<dimension;j++)
			cost += 100*obj.selectivity[coordinates[j]];
		return cost;
	}

}
