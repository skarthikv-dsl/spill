
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

import org.postgresql.jdbc3.Jdbc3PoolingDataSource;

import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;


public class stitchContourMaps {
	static String apktPath;
	static String qtName ;
	HashMap<Integer,ArrayList<point_generic>> ContourPointsMap = new HashMap<Integer,ArrayList<point_generic>>();
	
	public static void main(String[] args) throws IOException {
		
		stitchContourMaps obj = new stitchContourMaps();
		obj.loadPropertiesFile();
		obj.doStitchingMaps();
	}

	private void doStitchingMaps() throws IOException {
		
		
		String[] myFiles;    
		File filelog =  new File(apktPath+"contours/");
		if(filelog.isDirectory()){
				myFiles = filelog.list();
				for (int hi=0; hi<myFiles.length; hi++) {
					File myFile = new File(filelog, myFiles[hi]); 
					try {

						ObjectInputStream ip = new ObjectInputStream(new FileInputStream(myFile));
						ContourLocationsMap obj = (ContourLocationsMap)ip.readObject();
						//ContourPointsMap = obj.getContourMap();
						Iterator itr = obj.getContourMap().keySet().iterator();
						
						while(itr.hasNext()){
							Integer key = (Integer) itr.next();
							
							assert(!ContourPointsMap.containsKey(key)): "already the key exists in the contour points map";
							ContourPointsMap.put(key, obj.getContourMap().get(key));
							//System.out.println("The no. of Anshuman locations on contour "+(st+1)+" is "+contourLocs[st].size());
							System.out.println("The no. of locations on contour "+(key.intValue())+" is "+ContourPointsMap.get(key).size());
							System.out.println("--------------------------------------------------------------------------------------");

						}
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		
		FileOutputStream fos = new FileOutputStream (new File(apktPath+"contours/Contours.map"));
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(new ContourLocationsMap(ContourPointsMap));
		oos.flush();
		oos.close();

		
		
	}

	private void loadPropertiesFile() throws IOException {
		// TODO Auto-generated method stub
		Properties prop = new Properties();
		InputStream input = null;
		input = new FileInputStream("./src/Constants.properties");
		prop.load(input);

		// get the property value and print it out
		apktPath = prop.getProperty("apktPath");
		qtName = prop.getProperty("qtName");
	}

}
