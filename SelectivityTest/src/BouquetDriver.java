//package iisc.dsl.picasso.server;
import iisc.dsl.picasso.common.PicassoConstants;
//import iisc.dsl.picasso.common.ServerPacket;
import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
//import java.util.HashSet;
//import java.util.zip.GZIPOutputStream;

public class BouquetDriver{

	static class Set {
		HashSet elements = new HashSet();
	}
	
		

	
	
	//connection to DB code
	static Connection conn;
	public static void connectDB() {
		try {
			Class.forName("org.postgresql.Driver").newInstance();
		}
		catch (Exception e) {
			System.out.println("Database: " + e);
			System.out.println("Postgres Driver Issue");
		}

		try {
			conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/tpch-ai", "sa", "database");
		}
		catch (Exception e) {
			System.out.println("Database: " + e);
			System.out.println("Database Engine Postgres is not accepting connections");
		}
	}	


	
	
	
	
	// GLOBAL variables
	
	//DATA RELATED INFORMATION

	//the number, names and dimensions of queries
	static int numPackets = -1;
	
	static String qname[];                    
	static int dimension[][];
	static int DPnodes[][][];
	
	static int currentDim;
	static String currentQueryPath; 

	
	// path to data
	
	//	static String jpath = "/home/dsladmin/Desktop/2012/planCosts/base+join(5tpch+2tpcds)/";
	static String jpath1 = "/media/Databases/backup/Desktop/2012/planCosts/onlyjoinvariation/";
	static String jpath2 = "/media/Databases/backup/Desktop/2012/planCosts/tpcds/";
//	static String jpath3 = "/home/dsladmin/project/newData/";
//	static String jpath3 = "/media/Backups/bouquet_project/newData/paper/";
	
	
	
	
	static String jpath3 = "/home/dsladmin/Sumit/MainProject/Bouquet_Driver/data/";
//	static String jpath4 = "/home/dsladmin/project/newData/new/";
	static String jpath4 = "/media/Backups/bouquet_project/newData/new/";
	static String aipath = "/home/dsladmin/project/Exp/2D/";
	static String jpath5 = "/media/Backups/bouquet_project/newData/";
	
	
	
	
	
	
    
	/////////////////////////////////////////////// FULL ROBUSTNESS ///////////////////////////////////////////////////////////////////////////

	
	//INPUT
	// Information about Plan Diagram and Individual Plans
	static int dim, res;
	static ADiagramPacket gdp = null;
	static DataValues[] data;
	static double[][] AllPlanCosts;
	static float[] sel;
	static double[] costBouquet;
	static int nPlans = 0, countLSEERPlans = 0;
	static int SANITY_CONSTANT = 0;
	// Information about Plan Diagram and Individual Plans

	
	static double dep_th =20.0;  // th for dep cases
	static double threshold =0.0;
	static double switch_threshold =0.0;

	static boolean LOGICAL_COSTS = true;



	
	
	
	//INTERMEDIATE DATA
//	static double highres_sel[] =   {0.00005, 0.00008, 0.0001, 0.0002, 0.0005, 0.001, 0.002, 0.005, 0.01, 0.02, 0.04, 0.06, 0.08, 0.10, 0.15, 0.20, 0.25, 0.30, 0.40, 0.50, 0.60, 0.70, 0.80, 0.90, 0.99};
	static double highres_sel[] = {0.00005, 0.0001, 0.0005, 0.001, 0.005, 0.01, 0.05, 0.10, 0.15, 0.25, 0.40, 0.75, 0.99};
	
	static double selecContours3D[] = {                                                                                                                        1E-13,5E-13,1E-12,5E-12,1E-11,5E-11,1E-10,5E-10,1E-09,5E-09,1E-08,5E-08,1E-07,5E-07,1E-06,5E-06,1E-05,5E-05,1E-04,5E-04,1E-03,5E-03,1E-02,0.02,0.04,0.08,0.10,0.15,0.20,0.25,0.30,0.35,0.40,0.45,0.5,0.6,0.7,0.8,0.9,1.0};
	static double selecContours4D[] = {                                                            1E-18,5E-18,1E-17,5E-17,1E-16,5E-16,1E-15,5E-15,1E-14,5E-14,1E-13,5E-13,1E-12,5E-12,1E-11,5E-11,1E-10,5E-10,1E-09,5E-09,1E-08,5E-08,1E-07,5E-07,1E-06,5E-06,1E-05,5E-05,1E-04,5E-04,1E-03,5E-03,1E-02,0.02,0.04,0.08,0.10,0.15,0.20,0.25,0.30,0.35,0.40,0.45,0.5,0.6,0.7,0.8,0.9,1.0};
	static double selecContours5D[] = {1E-23,5E-23,1E-22,5E-22,1E-21,5E-21,1E-20,5E-20,1E-19,5E-19,1E-18,5E-18,1E-17,5E-17,1E-16,5E-16,1E-15,5E-15,1E-14,5E-14,1E-13,5E-13,1E-12,5E-12,1E-11,5E-11,1E-10,5E-10,1E-09,5E-09,1E-08,5E-08,1E-07,5E-07,1E-06,5E-06,1E-05,5E-05,1E-04,5E-04,1E-03,5E-03,1E-02,0.02,0.04,0.08,0.10,0.15,0.20,0.25,0.30,0.35,0.40,0.45,0.5,0.6,0.7,0.8,0.9,1.0};
	
	static int primes[] = {2,3,5,7,11,13,17,19,23,29,31,37,41,43,47,53,59,61,67,71,73,79,83,89,97,101,
		103,107,109,113,127,131,137,139,149,151,157,163,167,173,179,181,191,193,197,199,
		211,223,227,229,233,239,241,251,257,263,269,271,277,281,283,293,307,311,313,317,331,
		337,347,349,353,359,367,373,379,383,389,397,401,409,419,421,431,433,439,443,449,457,461,463,
		467,479,487,491,499,503,509,521,523,541,547,557,563,569,571,577,587,593,599,601,607,
		613,617,619,631,641,643,647,653,659,661,673,677,683,691,701,709,719,727,733,739,743,751,
		757,761,769,773,787,797,809,811,821,823,827,829,839,853,857,859,863,877,881,883,887,
		907,911,919,929,937,941,947,953,967,971,977,983,991,997,1009};

	
	static String  planStrings[];
	static String[] planStructures, subplans;
	
	static int numPlans[];
	static int planSet[], SEERplanSet[], cornerRobustSet[], cornerReducedSet[], cornerDisSimilarSet[], allPlanSet[], newOptimalPlan[];
	static int planDims[], planDimsSequence[][], subplanDims[];
	
	static long planSim[][];
	static HashSet subplansInPlan[];
	static SortedSet subplanCommonCluster[];
	static int countSubplans = 0;

	static double sumCorners[];
	static double sumCornersLocal[];
	static double ratioToOptimal[][];
	
	
	static int optimal_PO_loc[][];
	
	
	
	static int canExecuteWithCost[][];
	static double contour_costs[];
	static int commonDimsPOPair[][][];
	
	
	
	//OUTPUT
	static double subOptData[][];                         // SUB-OPT OUTPUT DATA PER EXPERIMENT
	static double worstPerformanceOfPlan[];
	static double worstPerformanceSEERPlans[];

	
	
	
	
	
	
	
	
	
	//SWITCHES
	static boolean filebased = false;
	static boolean checkPOSPandPCM = false;          // information validation switch
	static boolean shortcircuit = true;              // shortcircuiting switch
//	static boolean shortcircuit = false;              // shortcircuiting switch

	static boolean freshRestart = true;
	
	static boolean SQLSERVER = false;
//	static boolean SQLSERVER = true;

	static int UNIFORM_DISTRIBUTION = 0; 							// 0 means exponential distribution
//	static int JSP_ExpoDistributionBase = 2;						// for different distribution for location weights
	static int JSP_ExpoDistributionBase = -1;						// for different distribution for location weights

	
	
	
	
	
	
	
	
	
	//VERBOSE-SETTINGS
	
	static boolean subOptInfo = true;                    // SUBOPT
	static boolean OverheadDiagrams = false;             // diagram
	
	static boolean planSetDetails = true;              // details about bouquet identified - corner robust
	static boolean planCornerDetails = false;           // details about corner performance of bouquet
	static boolean planSetDetailsSEER = false;          // details about SEER selected bouquet    
	static boolean planSetDetailsCG = true;             // details about bouquet identified using CG

	
	static boolean subplanDetails = false, clusterDetails = false;  //details about details of selected plans structure 	
	
	// per location  run-time performance of bouquet algo
//	static boolean detailsSelection = true, planChangeDetails = true,  crossingInfo = true, infoPlanDecision = true;
	static boolean detailsSelection = false, planChangeDetails = false,  crossingInfo = false, infoPlanDecision = false;
		
	static boolean performanceDetails = false;		//run-time bouquet performance details at the end        
	static boolean 	onlyPlanBouquet = false;         // details of only bouquet OR best plan
	
	
	
//	static boolean detailsPredicateOrder = true;
	static boolean detailsPredicateOrder = false;
	
	
	//DEBUGGING
	static boolean debugCommonSubPlans = false;
	static boolean debugPredicateOrder = false;
	
	
	
	
	// MULTIPLE EXPERIMENT SETTING PARAMETERS AND OUTPUT COLLECTORS
	static int ct = -1, st = -1, settingMode = -1;
	static double avgSubOptBouquet[][][][];
	static double maxSubOptBouquet[][][][];

	
	
	
	
	//UNUSED
	static boolean topDown = true;
	static boolean dynamic_switching = true;
	static boolean clusterPlanSet = false, allowBackSwitch = true,  reusePlan = false, worstSerf = true;
	static boolean dynamic = true,  doNOP = false, printWeightedOverheads = false;
	static boolean BigBossSwitch = true, immediateSwitch = false;
	
	
	



	
	


	public static void main(String[] args) throws FileNotFoundException {
	
		
		
		
	
		
		
		numPackets = 53;

		if(SQLSERVER)			SANITY_CONSTANT = 8;
		else 					SANITY_CONSTANT = 10000;

		String qnameL[] = {  
				 jpath1 + "EQ5_3d_10"           //0
				,jpath1 + "EQ7_3d_10"			//1
				,jpath1 + "EQ7_5d_10"			//2
				,jpath1 + "EQ8_4d_10"			//3
				,jpath2 + "EDS7_5d_10"			//4
				,jpath2 + "EDS15_5d_10"			//5
				,jpath2 + "DS19_5jd"			//6
				,jpath2 + "DS96_3jd"			//7
				,jpath3 + "donotuse/SQL_EAIQ10_3D"		//8
				,jpath3 + "EAIQ8_4D"			//9
				,jpath3 + "EAIQ5_3D"			//10
				,jpath3 + "EAIQ7_3D"			//11
				,jpath3 + "EAIQ7_5D"			//12
				,jpath3 + "EAIQ10_2Dj"			//13
				,jpath3 + "EAIQ10_2D"			//14
				,jpath3 + "EAIQ8_2D"			//15
				,jpath3 + "LOP_1D"				//16
				,jpath3 + "LOP_1D_30"			//17
				,jpath3 + "LOP_1D_2"			//18
				,jpath3 + "LOP_2D"				//19
				,jpath5 + "EAIQ8_2D"			//20
				,jpath3 + "LOP_2D_LO"           //21             // specially for 2D example with zigzag in learning dimensions  - has special resolution 20
				,jpath3 + "EAIQ10_OL" 			//22
				,jpath3 + "DS7_4D"				//23
				,jpath3 + "DS15_3D"				//24
				,jpath3 + "DS19_5D"				//25
				,jpath3 + "DS96_3D"				//26
				,jpath4 +"EAIQ3_3D_b"			//27	
				,jpath4 +"EAIQ5_4D_b"			//28	
				,jpath4 +"EAIQ7_4D_b"			//29
				,jpath4 +"EAIQ8_5D_b"			//30
				,jpath4 +"EAIQ9_5D_b"			//31
				,jpath4 +"EAIQ10_3D_b"			//32
				,jpath4 +"EQ5_4Db_10"			//33
				,jpath3 +"Example_2D_2j"		//34	
				,jpath3 +"EQ_2D"				//35
				,jpath3 +"LOP_2D_LO_new"		//36	
				,jpath3 + "PLOC" 			//37
				,"/home/dsladmin/project/output/" + "Q5P_2D"   //38
				,jpath3 + "SQLSERVER/SQL_EAIQ5_3D"		//39
				,jpath3 + "EAIQ10_3D_b"		//40
				,jpath3 + "Q5_10"		//41
				,jpath3 + "Q5_10g"		//42
				,jpath3 + "Q8_10"		//43
				,jpath3 + "PITPCH_Q7"		//44
				,jpath3 + "PIDS7_4D"		//45
				,jpath3 + "DS26_4D"		//46
				,jpath3 + "DS91_4D"		//47
				,jpath3 + "PIDS15_3D"		//48
				,jpath3 + "PIDS19_5D"		//49
				,jpath3 + "SQLSERVER/SQL_EAIQ8_4D"		//50
				,jpath3 + "SQLSERVER/SQL_EAIQ7_3D"		//51
				,jpath3 + "EAIQ8_4D_b"		//52           // execution example special
				,"/home/dsladmin/project/PB_Q5_2D_100exp"		//53		//Plan Bouquet for Q5 2D
				,"/home/dsladmin/project/DS26_3D_30E_b"			//54		//TPC-DS Q26 3D 30E
				,"/home/dsladmin/project/DS26_3D_30E_OverEst"			//55		//TPC-DS Q26 3D 30E Over Estimation Example
				,"/home/dsladmin/project/SIGMOD_Q8_2D_100exp"			//56
				,"/home/dsladmin/project/SIGMOD_Q8_4D_10exp"			//57
		};
		qname = qnameL;

		int dimL[][] = {
				{12,23,34}, 			//0
				{23,34,45},  			//1
				{23,34,45,26,57}, 		//2
				{24,34,45,56},			//3
				{12,13,14,15,2},		//4
				{14,12,13,4,1},			//5
				{12,13,14,16,45},		//6
				{12,13,14},				//7
				{1,2,3},				//8
				{24,34,45,56},			//9
				{12,23,34},				//10
				{23,34,45},				//11
				{23,34,45,26,57},		//12
				{12,23},				//13
				{23,3},					//14
				{24,45},				//15
				{1},					//16
				{1},					//17
				{2},					//18
				{2,1},					//19
				{24,45},				//20
				{2,3},					//21
				{23},					//22
				{12,13,14,15},			//23
				{14,12,23},				//24
				{12,13,14,16,45},		//25
				{12,13,14},				//26
				{1,2,3},				//27
				{1,2,3,4},				//28
				{2,3,4,5},				//29
				{2,3,4,5,6},			//30
				{2,3,4,5,6},			//31
				{1,2,3},				//32
				{1,2,3,4},				//33
				{12,23},				//34
				{12,34},				//35
				{2,3},					//36
				{12,34},				//37
				{4,5},					//38
				{1,2,3},				//39
				{1,2,3},				//40
				{12,23,34},				//41
				{12,23,34},				//42
				{24,34,45,56},			//43
				{23,34,45,26,57},		//44
				{12,13,14,15},			//45
				{12,13,14,15},			//46  
				{23,45,46,47},		    //47
				{14,12,23},              //48 
				{12,13,14,16,45},        //49
				{2,5,3,4},                //50
				{2,4,5} ,               //51
				{2,3,4,6},                //52
				{1,3},						//53
				{1,4,2},				//54
				{1,4,2},					//55
				{6,4},						//56
				{2,3,4,6}                 //57
		};
		dimension = dimL;


		int DPnodesL[][][] = {
				{{12,23,34}}, 				//0
				{{23,34,45}},  				//1
				{{23,34,45,26,57}}, 		//2
				{{24,34,45,56}},			//3
				{{12,13,14,15,2}},			//4
				{{14,12,13,4,1}},			//5
				{{12,13,14,16,45}},			//6
				{{12,13,14}},				//7
				{{1,2,3}},					//8
				{{24,34,45,56},{234, 245, 345, 456}, {2345, 2456, 3456}, {23456}},			//9
				{{12,23,34}, {123, 234}, {1234}},				//10
				{{23,34,45}, {234, 345}, {2345}},				//11
				{{23,34,45,26,57}, {236, 234, 345, 457}, {2346, 2345, 3457}, {23456, 23457}, {234567}},			//12
				{{12,23}},					//13
				{{23,3}},					//14
				{{24,45}},					//15
				{{1}},						//16
				{{1}},						//17
				{{2}},						//18
				{{2,1}},					//19
				{{24,45}},					//20
				{{2,3}},					//21
				{{23}},						//22
				{{12,13,14,15}, {123, 124, 125, 134, 135, 145}, {1234, 1235, 1245, 1345}, {12345}},			//23
				{{14,12,23}, {124, 123}, {1234}},				//24
				{{12,13,14,16,45}, {123, 124, 126, 134, 136, 145, 146}, {1234, 1236, 1246, 1245, 1345, 1346, 1456}, {12345, 12346, 12456, 13456}, {123456}},								//25
				{{12,13,14}, {123, 124, 134}, {1234}},				//26
				{{1,2,3}},					//27
				{{1,2,3,4}},				//28
				{{2,3,4,5}},				//29
				{{2,3,4,5,6}},				//30
				{{2,3,4,5,6}},				//31
				{{1,2,3}},					//32
				{{1,2,3,4}},				//33
				{{12,23}},					//34
				{{12,34}},					//35
				{{2,3}},					//36
				{{12,34}},					//37
				{{4,5}},					//38
				{{1,2,3}},					//39
				{{1,2,3}},					//40
				{{12,23,34}},				//41
				{{12,23,34}},				//42
				{{24,34,45,56}},			//43
				{{23,34,45,26,57}},			//44
				{{12,13,14,15}},			//45
				{{12,13,14,15}, {123, 124, 125, 134, 135, 145}, {1234, 1235, 1245, 1345}, {12345}},			//46
				{{23,45,46,47},{456, 457}, {4567, 2457, 2456, 2467}, {23456, 23457, 23467},{234567}},		    //47
				{{14,12,23}},               //48 
				{{12,13,14,16,45}},         //49
				{{2,5,3,4}},                //50
				{{2,4,5}},               	//51
				{{2,3,4,6}},               	//52
				{{1,3}},					//53
				{{1,4,2}},					//54
				{{1,4,2}},					//55
				{{6,4}},					//56
				{{2,3,4,6}}               	//57
		};
		
		DPnodes = DPnodesL;



//		int q[] = {9,10,11,12,23,24,25,26,46,47};
		//int q[] = {46,47};
//		int q[] = {39,50};
		int q[] = {56};
		//int q[] = {13,9,10,11,12};
		//int q[] = {24,25,26,27,28};

//		int q[] = {28};          // execution example SIGMOD   Q5
//		int q[] = {30};          // execution example SIGMOD   Q8 _5D
//		int q[] = {52};          // execution example SIGMOD   Q8_4D

		avgSubOptBouquet = new double[101][100][7][numPackets];
		maxSubOptBouquet = new double[101][100][7][numPackets];			

		for(int i=0;i<q.length;i++) {
		//for(int i=0;i<8;i++) {
		//for(int i=9;i<12;i++) {
		//int i=numPackets-2;{
		//int i=27;{
		//for(settingMode = 3; settingMode>=2; settingMode--)
		//for(settingMode = 5; settingMode>=0; settingMode--)
			//settingMode = 6;
			settingMode = 2;
			{

				System.out.print("\n\n\n\n**********************************************************************************************\n\n\n\n");

				if(settingMode <= 2) {
					//Set-1
					BigBossSwitch = true;immediateSwitch = false; freshRestart = true;  //Smart-Bouquet setting + corner-Robust + corner-Optimal (2 -> 1 -> 0)

				}
				else if(settingMode == 3) {	
					//Set-2

					BigBossSwitch = true;immediateSwitch = false; freshRestart = true;  //Lazy-Bouquet setting


					//					BigBossSwitch = false;immediateSwitch = false; freshRestart = true;  //Lazy-Bouquet setting
					//					subPlanOverheads = false;useTotalOverheads = true;
					//					useOnePlan = false;
				}
				else if(settingMode == 4) {
					//Set-3
					BigBossSwitch = true;immediateSwitch = false;  freshRestart = true;  //Exact-Bouquet setting

					//					BigBossSwitch = false;immediateSwitch = false;  freshRestart = true;  //Exact-Bouquet setting
					//					subPlanOverheads = false;useTotalOverheads = true;
					//					useOnePlan = false;
				}
				else if(settingMode == 5) {
					//Set-3
					BigBossSwitch = false;immediateSwitch = false;  freshRestart = true;  //Exact-POSP setting

				}
				else if(settingMode == 6){

				}

				System.out.print("\nBigboss_switch   = " + BigBossSwitch);
				System.out.print("\nImmediate_switch = " + immediateSwitch);
				System.out.print("\nFresh_restart    = " + freshRestart);

				System.out.print("\n\n");

				for( int t1 = 0; t1 < 101; t1++) {
					for( int t2 = 0; t2 < 101; t2++) {
						ct = t1;
						st = t2;

						threshold        =  10 * t1 + 0.000000000000005;	
						switch_threshold =  10 * t2 + 0.000000000000005;

						if (
								//								t1 == 10 && t2 == 10  && settingMode < 4               // corner-optimal - cornerRobust - bigboss - lazy ( 0 -> 1 -> 2 -> 3) 
								//						||	t1 == 10 && t2 == 5  && settingMode < 4             
								//						||	t1 == 10 && t2 == 2  && settingMode < 4             
								//						||	t1 == 10 && t2 == 0  && settingMode < 4              
								//						||  t1 == 5  && t2 == 10  && settingMode < 4            
								//						||  t1 == 5  && t2 == 5  && settingMode < 4             
								//						||  t1 == 5  && t2 == 2  && settingMode < 4             
								//						||  t1 == 5  && t2 == 0  && settingMode < 4             
								//						||  t1 == 2  && t2 == 10 && settingMode < 4             
								//						||  t1 == 2  && t2 == 5  && settingMode < 4             
//								(t1 == 1 || t1 == 10 || t1 == 20 || t1 == 80)  && t2 == 10 && settingMode < 4             
								(t1 == 2)  && t2 == 10 && settingMode < 4
								//						  t1 == 2 && t2 == 0  && settingMode < 4             
								|| t1 == 2 && t2 == 5 && settingMode == 4 
								|| t1 == 0 && t2 == 0 && settingMode == 5 
								|| t1 == 2 && t2 == 0 && settingMode == 6)

							singleRun(q[i]);
						//						singleRun(i);
					}
				}


				//				for (int i1 = 0; i1 <numPackets; i1++){
				//
				//					System.out.print("\n\n");
				//
				//
				//					for (int i2 = 0; i2 <5; i2++){
				//						for (int i3 = 0; i3 <5; i3++){
				//							System.out.format("\t%4.1f",maxSubOptBouquet[i3][i2][settingMode][i1]);
				//						}
				//						System.out.print("\n");
				//					}
				//
				//					System.out.print("\n\n");
				//
				//
				//					for (int i2 = 0; i2 <5; i2++){
				//						for (int i3 = 0; i3 <5; i3++){
				//							System.out.format("\t%4.1f",avgSubOptBouquet[i3][i2][settingMode][i1]);
				//						}
				//						System.out.print("\n");
				//					}
				//
				//					System.out.print("\n\n\n\n\n");
				//				}


			}
		}

	}


	

	public static void singleRun(int i) {
		try {		

			currentDim = dimension[i].length;
			currentQueryPath = qname[i];
			
			System.out.format("\ncost_threshold   = %.2f", threshold);
			System.out.format("\nswitch_threshold = %.2f", switch_threshold);
			System.out.format("\nsanity used =  %d", SANITY_CONSTANT);

			//findTreeStructure();
			{ 
				analyzePlans_generic(i);
			}
			//System.out.println("ret");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	public static void analyzePlans_generic(int index) throws IOException {


		//		1. read the packet and extract/calculate global information
		gdp = getGDP(new File( qname[index] +  ".apkt"));
		nPlans = gdp.getMaxPlanNumber();

		res = gdp.getMaxResolution();
		dim = gdp.getDimension(); 
		data = gdp.getData();		
		int total_points = (int) Math.pow(res, dim);		


		//		2. read all the cost files
		AllPlanCosts = new double[nPlans][total_points];
		costBouquet = new double[total_points];
		for (int i = 0; i < nPlans; i++) {
			try {
				ObjectInputStream ip = new ObjectInputStream(new FileInputStream(new File(qname[index] + "/" + i + ".pcst")));
				double[] cost = (double[]) ip.readObject();
				for (int j = 0; j < total_points; j++)
					AllPlanCosts[i][j] = cost[j];
			} catch (Exception e) {
				e.printStackTrace();
			}
		}


		
		System.out.print("\n\n The number of plans in the packet = " + nPlans + " for template " + qname[index]);
		// is it the case that - the slope is worse when sel < 0.05% for most of these templates ? - why not start with 0.005%
		// or is it because the tables are too small ?


		//	 3. check the correctness of generated diagram and cost files
		if(checkPOSPandPCM == true && filebased == false ){
			checkPOSP(dim,  res, nPlans);
			checkPCM(dim, res, nPlans);
			//System.exit(0);
			return;
		}


		System.out.print("\nThe dimensions to vary are:\n");
		for(int i=0; i<dimension[index].length; i++){
			System.out.print("\t" + dimension[index][i]);
		}


		// convert .pcst to .txt
		rewriteFilesGNUCompatiable(index,true,false);    // (....., skip the function, Exit from program?)


		allPlanSet = new int[nPlans];
		for(int i=0; i<nPlans; i++)  			allPlanSet[i] = i;
		
		//calculate really optimal plan at each location in the space -- because FPC costs may be different from the optimal costs
		newOptimalPlan = new int[data.length];
		for(int loc=0; loc < data.length; loc++) {

			int[] coordinate = new int[dim];
			coordinate = getCoordinates(dim,res,loc);		
			newOptimalPlan[loc] = getOptimalPlan(loc, allPlanSet);
		}
		
		int worstPlan[] = new int[data.length];
		for(int loc=0; loc < data.length; loc++) {
	
			worstPlan[loc] = getWorstPlan(loc, allPlanSet);
		}
		//calculate really optimal plan at each location in the space -- because FPC costs may be different from the optimal costs
		
		double MSO = -1.0;
		double a;
		int location=0;
		for(int loc=0; loc < data.length; loc++)
		{
			a = AllPlanCosts[worstPlan[loc]][loc]/Math.max(SANITY_CONSTANT, AllPlanCosts[newOptimalPlan[loc]][loc]);
			if(MSO < a)
			{
				MSO = a;
//				System.out.print(MSO+",");
				location = loc;
			}
		}
		System.out.println("Sumit MSO = "+MSO);
		System.out.println("Worst Value="+AllPlanCosts[worstPlan[location]][location]);
		
		
		identifySOSP(threshold, index);			

		
				
		/*
		ADiagramPacket agdp = null;
		agdp = liteSeer_genericBenefit(threshold,gdp);

		//for(int ip = 0; ip<SEERplanSet.length; ip++) { 
		for(int ip = 0; ip<planSet.length; ip++) {
			//planDimOrder_new(index,SEERplanSet[ip]);
			planDimOrder_new(index,planSet[ip]);
		}
		*/

		worstCG_Solution_expo(threshold, planSet, index);

	}

	





	public static void identifySOSP(double sim_threshold, int qt){

		// get location weights for exponential space
		getPlanCountArray();                   
	
		
		//reading plan-string representations from file
		readPlanStrings(qname[qt]);

		
		//convert to plan structures -- removes operators (if present in the string)///////////////////////////
		if(SQLSERVER)			
			planStructures = planStrings;
		else {
			planStructures = new String[nPlans];
			getPlanSkeletons();
		}
		////////////////////////////////////////////////////////////////////////////
		
	
		// find subplans (in subplans[]) and the common subplans (in planSim[][])///////////////////////
		planSim = new long[nPlans][nPlans];
		subplans = new String[200];
		countSubplans = 0;
		findCommonsSubplansInPlans();             
		////////////////////////////////////////////////////////////////////////////
		
		
		

		// cluster plans on the basis of their deepmost subplan (start-subplan clustering)
		// can be implemented as - if two plans have same substring at base-level- then they lie in same cluster
		createSubplanClusters_and_findSubplanBaseDims(qt);

		
		

		
		for(int c=0; c<countSubplans; c++){
			Iterator itr_plans = subplanCommonCluster[c].iterator();
			if(subplans[c].length() > 5) {
				//				System.out.print("\n\n" + c + "\t" + subplans[c] + "\t" + subplanCommonCluster[c].size() + " plans\t" );
				while(itr_plans.hasNext()) {
					Object element = itr_plans.next();
					int plan = ((Integer)element).intValue();
					//					System.out.print("\n" + ((Integer)element).intValue());

					//					System.out.format("\t%40s",planStrings[plan]);
					for(int cn=0; cn< Math.pow(2, dim);cn++) {
						int cornerIndex = getIndex(getCornerCoordinate(cn,dim,res),res);
						double ratio = Math.max(SANITY_CONSTANT,AllPlanCosts[plan][cornerIndex])/Math.max(SANITY_CONSTANT, data[cornerIndex].getCost());
						//						System.out.format("\t%10.1f",ratio);
						//sumCorners[plan] += ratio;
					}
				}
			}
		}
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////





		//find the error dimensions for each plan////////////////////////////////////////////////////////////////

		//		System.out.print("\n\n\n");
		planDims = new int[nPlans];

		subplansInPlan = new HashSet[nPlans];
		//System.out.print("\n\n");

		for(int p=0; p<nPlans; p++){

			//System.out.print("\nFor plan " + p + "\t" + planStrings[p] + "\t" );
			subplansInPlan[p] = new HashSet();

			for(int c=0; c<countSubplans; c++){
				Iterator itr_plans = subplanCommonCluster[c].iterator();

				if(subplanCommonCluster[c].contains(p)){
					if(subplans[c].length() > 5) {
						subplansInPlan[p].add(c);
						planDims[p] = planDims[p] | subplanDims[c];
						//						System.out.print("\t" + c);
						//System.out.print("\t" + subplans[c]);
					}
				}

				//				while(itr_plans.hasNext()) {
				//					int element = ((Integer)itr_plans.next()).intValue();
				//					if(element == p) {
				//						
				//						if(subplans[c].length() > 5) {
				//							subplansInPlan[p].add(c);
				//							planDims[p] = planDims[p] | subplanDims[c];
				//							//System.out.print("\t" + c);
				//							System.out.print("\t" + subplans[c]);
				//						}
				//					}
				//				}
			}
		}		

		//		System.out.print("\n\n\n");

		///////////////////////////////////////////////////////////////////////////////////////////////////////////











		// find optimal cost at each corner/////////////////////////////////////////
		double minCostCorners[] = new double[(int) Math.pow(2, dim)];

		for(int c=0; c< Math.pow(2, dim);c++) {
			int cornerIndex = getIndex(getCornerCoordinate(c,dim,res),res);
			minCostCorners[c] = Math.max(SANITY_CONSTANT, AllPlanCosts[newOptimalPlan[cornerIndex]][cornerIndex]);
			//	System.out.format("\t%10.0f", minCostCorners[c]);
		}
		/////////////////////////////////////////////////////////////////////////		


		
		
		
		

		
		//for each plan and each space corner: find ratio to optimal cost /////////////////////////////////////////
		sumCorners = new double[nPlans];
		ratioToOptimal = new double[nPlans][(int)Math.pow(2, dim)];
		
		for(int p=0; p<nPlans;p++) {

			if(planCornerDetails)				System.out.format("\n%40s",planStrings[p]);
			
			for(int c=0; c< Math.pow(2, dim);c++) {
				int cornerIndex = getIndex(getCornerCoordinate(c,dim,res),res);
				ratioToOptimal[p][c] = Math.max(SANITY_CONSTANT, AllPlanCosts[p][cornerIndex])/minCostCorners[c];
				if(planCornerDetails) 	  System.out.format("\t%10.1f",ratioToOptimal[p][c]);
				sumCorners[p] += ratioToOptimal[p][c];
			}
			if(planCornerDetails)				
				System.out.format("\tSumCorners = %10.1f",sumCorners[p]);
		}


		for(int n=0; n< Math.pow(2, dim);n++) { 
			int corner = getIndex(getCornerCoordinate(n,dim,res),res);
			if(planCornerDetails)				System.out.print("\n" + n  + "\t"+corner + "\t" + planStrings[data[corner].getPlanNumber()]);
		}


		///////////////////////////////////////////////////////////////////////////////
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		//choose a subset of POSP plans according to Foreign cost based CostGreedy algorithm i.e. CG-FPC/////////////////////////// 
		
		
		int chosenPlanSet[] = new int[300];
		for(int i=0; i<300; i++)			chosenPlanSet[i] = -1;

		int i=0, count = 0, selectedPlans = 0;
		double remainingSpace = areaSpace ;

		if(true) {
			while(remainingSpace > 0){
				double countBoringLocations[] = new double[nPlans];
				double countNearOptimalPlans[] = new double[data.length];			
		
				
				
				//1. find the area covered by different plans in the REMAINING area
				for(int loc = 0; loc < data.length; loc++) {
					int c=0, p = data[loc].getPlanNumber();
					
					
					//1.1skip a location that can be eaten by a already selected plan
					for(c=0;c<selectedPlans;c++) {
						if(AllPlanCosts[chosenPlanSet[c]][loc] <= AllPlanCosts[p][loc] * (1+sim_threshold/100)) {
							break;
						}
					}
					if(c < selectedPlans)
						continue;
					
					
					//1.2find the area (near-optimally) covered by each plan
					double currentRelativeDiff = 0.0;
					for (i=0; i<nPlans; i++) {
						currentRelativeDiff = 0.0;
						currentRelativeDiff = Math.abs((AllPlanCosts[p][loc]-AllPlanCosts[i][loc]));
						currentRelativeDiff = currentRelativeDiff * 100.0/AllPlanCosts[p][loc];

						if(currentRelativeDiff <= sim_threshold) {
							countBoringLocations[i]+=locationWeight[loc];
							//countBoringLocations[i]++;
							countNearOptimalPlans[loc]++;
						}
					}
				}

				
				
				//2.find the plan that covers max area
				double maxCoverage = 0;
				int maxCoveragePlan = -1;

				for (i=0; i<nPlans; i++) {
					if(maxCoverage < countBoringLocations[i]){
						maxCoverage = countBoringLocations[i];
						maxCoveragePlan = i;
					}
				}
				if(maxCoveragePlan == -1)					break;

				//3.remember the plan that covers max area & update the remaining space
				chosenPlanSet[selectedPlans++] = maxCoveragePlan;	
				remainingSpace -= countBoringLocations[maxCoveragePlan];
			}

			planSet = new int[selectedPlans];

			for(int p=0;p<300;p++) {
				if(chosenPlanSet[p] >= 0) {
					planSet[p] = chosenPlanSet[p];
				}
			}
		}
		else 
			planSet = Costgreedy(sim_threshold, qt);    // CostGreedy using Learning-potential of plans    

		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		
		System.out.print("#Selected Plans" + planSet.length);
		
			
		// print performance of selected plans on corners and performance distribution across space and MaxPenalty across space		
		//detailsCornerRobustPlans(planSet, false);
		
	}

	








	static double currentCostLimit = 0, crossedCostLimit = 0;

	static int plansPerLimit[][];
	static double cost_budget[];
	static double cost_limit[];
	static double contour_area[];
	static int contour_plansSteps[][];

	public static void worstCG_Solution_expo(double sim_threshold, int SetPlans[], int qt){

		double minCostPacket = Math.max(AllPlanCosts[newOptimalPlan[0]][0],SANITY_CONSTANT);
		double maxPacketCost = AllPlanCosts[newOptimalPlan[data.length-1]][data.length-1];       //assumed to be max

		//		int steps = (int) Math.ceil(Math.log10(maxPacketCost)/Math.log10(2));
		int steps = 0;				
		double limit = maxPacketCost;
		while(limit > minCostPacket){		limit /= 2;		steps++;}
		double firstCostLimit = limit * 2;

		plansPerLimit = new int[data.length][steps];

		
			System.out.print("\n");

			cost_budget = new double[steps+1];
			cost_limit = new double[steps+1];
			contour_area = new double[steps+1];
			
			
			contour_plansSteps = new int[steps+1][nPlans];
			for(int s=0; s<steps; s++)					cost_limit[s] = firstCostLimit * Math.pow(2, s);

		
			contourSetDetails(steps, SetPlans);
	
			
			//for(int s=0; s<steps; s++)					cost_budget[s] = SetPlans.length * firstCostLimit * Math.pow(2, s);
			for(int s=0; s<steps; s++)					cost_budget[s] = contourPlanCount[s+1] * cost_limit[s];
			
			
			//// Now we have the information that which contours have which plan numbers and the budget cost for each contour

						
			System.out.print("\n\n\n");										


	}




	/////////////////////////////////////////////// FULL ROBUSTNESS ///////////////////////////////////////////////////////////////////////////




	
	
	
	
	
	
	
	
	
	
	
	
	

	////////////////////////// SPACE UTILITY CODE ///////////////////////////////////////
	

	// this function finds the location of  		coordinate = (index[max], ..., index[0]) 
	// in a space with 						   		resolution = res
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

	// given corner number (0 to 2^dim) - it finds the coordinates of the corner
	
	
	
		
	
	public static int[] getCornerCoordinate(int corner, int dimension, int res){
		int[] coordinate = new int[dimension];

		int xx = corner;
		for (int j = 0; j < dimension; j++) {
			coordinate[j] = xx & 1;                          // see the last bit of the corner-number
			coordinate[j] *= (res - 1);                     // if last bit = 1, see the last point in that dimension

			xx >>= 1;                                // divide by 2
		}
		return coordinate;
	}

	
	
	
	// given corner number (0 to 2^dim) - it finds  8 -> (1,0,0)
	
	
	
	public static int[] getBinary(int corner, int dimension){
		int[] coordinate = new int[dimension];

		int xx = corner;
		for (int j = 0; j < dimension; j++) {
			coordinate[j] = xx & 1;                          // see the last bit of the corner-number
			//			coordinate[j] *= (res - 1);                     // if last bit = 1, see the last point in that dimension

			xx >>= 1;                                // divide by 2		
		}
		return coordinate;
	}
	
	
	

	// given binary array - find corner number (1,0,0) -> 8
	public static int getDecimal(int corner[], int dimension){

		int cornerLoc = 0;
		for (int j = 0; j < dimension; j++) 			
			cornerLoc += (int) corner[j] * Math.pow(2,j);

		return cornerLoc;
	}
	
	
	

	// given any location - it prints the coordinates in space (res,dim)	
	public static void printIndex(int dim, int res, int location){

		if(dim == 0){
			return;
		}

		if(location < res) {
			printIndex(dim-1,res,location/res);
			System.out.print(" " + location);
		}

		else{	
			printIndex(dim-1,res,location/res);
			System.out.print(" " + location % res);
		}
	}
	
	
	

	//given any location - it finds the coordinates in space (res,dim)
	public static int[] getCoordinates(int dimensions, int res, int location){
		int [] index = new int[dimensions];

		for(int i=0; i<dimensions; i++){
			index[i] = location % res;

			location /= res;
		}
		return index;
	}


	
	
	// function that gives mincost plan from the specified set of plans for a given location 
	public static int getOptimalPlan(int loc, int[] plans) {
		
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

	
	public static int getWorstPlan(int loc, int[] plans) {
		
		double bestCost = Double.MIN_VALUE;
		int opt = -1;
		for(int p=0; p<plans.length; p++){
			if(bestCost < AllPlanCosts[plans[p]][loc]) {
				bestCost = AllPlanCosts[plans[p]][loc];
				opt = p;
			}
		}
		return opt;
	}

	////////////////////////// SPACE UTILITY CODE ///////////////////////////////////////	
	
	
	
	
	
	
	
	
	
	
	   //////////////////////////////////PLAN-STRUCTURE ANALYSIS RELATED CODE//////////////////////////////////////////

	public static void readPlanStrings(String planFileLocation) {
		File planStringFile = new File( planFileLocation + "/plans");
		planStrings= new String[nPlans];

		try {
			BufferedReader reader =  new BufferedReader( new FileReader(planStringFile));
			String text = "";
			int l = 0;

			while((text = reader.readLine()) != null){
				if(text.length() > 1 && l < nPlans)	{
					//					System.out.println(l + "\t" + text);
					planStrings[l++] = text;

				}
				else 									break;
			}

			reader.close();
		}
		catch (Exception e){
			e.printStackTrace();
		}

	}
	
	

	public static boolean balanced(String check){

		int count = 0;
		char [] charcheck = check.toCharArray();

		for(int t=0; t<check.length(); t++){
			if(charcheck[t] == '(')
				count++;
			else if(charcheck[t] == ')')
				count--;

			if(count < 0)
				break;
		}

		if(count == 0)
			return true;
		else
			return false;
	}

		

	public static boolean isSubPlan(String substring, String plan2){

		boolean flag = false;

		if(plan2.indexOf(substring) >= 0){
			//System.out.print("\t" + substring);
			flag = true;
		}

		return flag;
	}


	// finds how many elements of set1 are present in set2 and return true if more than zero
	
	
	
	public static boolean clusterSimilar(HashSet set1, SortedSet set2) {

		Iterator itr1 = set1.iterator();
		int commonElements = 0, totalElements1 = 0, totalElements2 = 0;

		totalElements1 = set1.size();
		totalElements2 = set2.size();



		while(itr1.hasNext()){
			if(set2.contains((Object)itr1.next())){
				commonElements++;
			}
		}

		//if(commonElements > 0.75 * totalElements1 || commonElements > 0.75 * totalElements2)
		if(commonElements > 0)
			return true;
		else		
			return false;
	}

	
	
	public static void getPlanSkeletons()	{

		//remove operators
		int p = -1;
		for(p=0; p<nPlans; p++)
		{
			int l=0;
			planStructures[p] = "";
			for(int i=0; i<planStrings[p].length();i++){
				if((planStrings[p].charAt(i) >= 48 && planStrings[p].charAt(i) < 58) || (planStrings[p].charAt(i) == '(') || (planStrings[p].charAt(i) == ')') ){
					planStructures[p] += planStrings[p].charAt(i);
				}
			}

		}

	
		
		//remove extra paranthesis around base relations
		for(p=0; p<nPlans; p++)
		{
			StringBuffer s = new StringBuffer(planStructures[p].length());

			for(int i=0; i<planStructures[p].length()-1;i++){
				/*if(!(planStructures[p].charAt(i+1) >=48 && planStructures[p].charAt(i+1) < 58)) {
					s.append(planStructures[p].charAt(i));
				}			
				else */ 
				if(planStructures[p].charAt(i) == '(' && planStructures[p].charAt(i+2) == ')' && planStructures[p].charAt(i+1) >=48 && planStructures[p].charAt(i+1) < 58){
					s.append(planStructures[p].charAt(i+1));
					i+=2;
				}
				else if(planStructures[p].charAt(i) == '(' && planStructures[p].charAt(i+1) == '(' && planStructures[p].charAt(i+2) >=48 && planStructures[p].charAt(i+2) < 58
						&& planStructures[p].charAt(i+3) == ')' && planStructures[p].charAt(i+4) == ')'){
					s.append(planStructures[p].charAt(i+2));
					i+=4;					
				}
				else {
					s.append(planStructures[p].charAt(i));
				}
			}
			planStructures[p] = s.toString();
			planStructures[p] += ")";
			//System.out.print("\n" + p + "\t" + planStructures[p]);
		}
		

	}
	


	public static void findCommonsSubplansInPlans() {

		//find which plans have a common subplan 

		for(int p1=0; p1<nPlans; p1++)
		{	
			String plan1 = planStrings[p1];
			for(int i = 4 ; i <= plan1.length(); i++ )         // length of substring
			{
				for( int c = 0 ; c < plan1.length()-i ; c++ )   //start of substring
				{
					String subs = plan1.substring(c, c+i);					// find a substring				

					if(subs.startsWith("(") && subs.endsWith(")") && (subs.length() == 4 || subs.length() == 5 || (subs.length() > 5 &&  subs.length() < 14)) 
							&& (balanced(subs) == true && balanced(subs.substring(1, subs.length()-1)) == true))								    // check whether it makes a subplan
					{					
						int p=0;
						for( p=0; p<countSubplans; p++){             //check whether already exist ?
							if(subplans[p].equals(subs))
								break;
						}

						if(p == countSubplans)	{
							if(debugCommonSubPlans)
								System.out.print("\n" + countSubplans + "\t" + subs + "\t" + primes[countSubplans]);
							subplans[countSubplans++] = subs;
						}

						//checked whether it already exist - and made a new one if not
						for(int p2=p1+1; p2<nPlans; p2++)
						{
							if(planSim[p1][p2] == 0) {              // initialize to 1
								planSim[p1][p2] = 1;
								planSim[p2][p1] = 1;
							}

							if(isSubPlan(subs,planStrings[p2]))
							{
								for( p=0; p<countSubplans; p++){
									if(subs.equals(subplans[p])) {
										planSim[p1][p2] *= primes[p];
										planSim[p2][p1] *= primes[p];
										break;
									}
								}
								if(debugCommonSubPlans) {
									System.out.print("\n\t" + p1 + "\t" + p2 + "\t" + planSim[p1][p2] + "\t");
									System.out.print("\t\t" + planStrings[p1] + "\t\t\t" + planStrings[p2] +  "");
								}
							}
						}
					}
				}
				if(debugCommonSubPlans)
					System.out.print("\n");
			}
		}
	}

	

	public static void createSubplanClusters_and_findSubplanBaseDims(int qt) {


		// each cluster contains the plans with same subplan 
		subplanCommonCluster = new SortedSet[countSubplans];
		subplanDims = new int[countSubplans];


		if(subplanDetails)			System.out.print("\n\n");		
		for(int c=0; c<countSubplans; c++){
			subplanCommonCluster[c] = new TreeSet();
			subplanDims[c] = 0;
			if(subplanDetails)			System.out.print("\n" + c + "\t" + primes[c] + "\t" + subplans[c] + "\t" );
			for(int d=0;d<dimension[qt].length;d++){
				if(dimension[qt][d] / 10 == 0){
					for(int s=0;s<subplans[c].length();s++){
						if(subplans[c].charAt(s) == (char) (dimension[qt][d] + 48)){
							if(subplanDetails)			System.out.print("\t" + d);
							subplanDims[c] = subplanDims[c] | (1 << d);
							break;
						}
					}
				}
				else if(dimension[qt][d] / 10 >= 1){
					int dim1 = dimension[qt][d]/10;
					int dim2 = dimension[qt][d]%10;
					int found = 0;

					for(int s=0;s<subplans[c].length();s++){
						char ch = subplans[c].charAt(s); 
						if(ch == (char) (dim1+48) || ch == (char) (dim2+48)){
							found++;
						}
					}

					if(found == 2) {
						if(subplanDetails)			System.out.print("\t" + d);
						subplanDims[c] = subplanDims[c] | (1 << d);
					}
				}
			}

		}





		for(int p1=0; p1<nPlans; p1++)
		{	
			for(int p2=p1+1; p2<nPlans; p2++)
			{	
				for(int n=0; n<countSubplans;n++){
					if(planSim[p1][p2] % primes[n] == 0){
						subplanCommonCluster[n].add(p1);
						subplanCommonCluster[n].add(p2);
						planSim[p1][p2] /= primes[n];
					}
					else {
						if (planStrings[p1].indexOf(subplans[n]) > 0)							subplanCommonCluster[n].add(p1);
						if (planStrings[p2].indexOf(subplans[n]) > 0)							subplanCommonCluster[n].add(p2);						
					}

					//if(planSim[p1][p2] < n)						break;
				}
			}

		}



	}

		

	private static int calculateLearningPotential(int qt, int plan) {

		int count = 0;

		for(int d=0;d < dim; d++) {

			if(dimension[qt][d]/10 > 0) {
				int search1 = dimension[qt][d];
				int search2 = (dimension[qt][d]/10) + (dimension[qt][d]%10)*10;
				if(planStructures[plan].indexOf(search1 + "") > 0 || planStructures[plan].indexOf(search2 + "") > 0){
					count++;
				}
			}
			else {
				int search = dimension[qt][d];
				int index = planStructures[plan].indexOf(search + "");

				if((planStructures[plan].charAt(index+1) > 48 &&  planStructures[plan].charAt(index+1) < 58) 
						|| (planStructures[plan].charAt(index-1) > 48 &&  planStructures[plan].charAt(index-1) < 58)){
					count++;
				}
			}

		}

		return count;
	}


	

	public static void planDimOrder_new(int qt, int planc) {
		HashSet bitChanges = new HashSet();
		int countNeighbors = 0;

		System.out.print("\n\n" +  planStrings[planc]);
		System.out.print("\n" + planStructures[planc]);


		int level[] = new int[planStructures[planc].length()];
		boolean increasedLevel[] = new boolean[nPlans];


		int countLeft = 0;
		for(int ii=0; ii<planStructures[planc].length(); ii++) {

			if(planStructures[planc].charAt(ii) == '(')					countLeft++;
			else if(planStructures[planc].charAt(ii) == ')')				countLeft--;

			level[ii] = countLeft;

		}

		int max = 0;
		for(int ii=0; ii<planStructures[planc].length(); ii++) 
			max = Math.max(max, level[ii]);


		System.out.print("\n");
		for(int ii=0; ii<planStructures[planc].length(); ii++) {
			level[ii] = max - level[ii];
			System.out.print("" + level[ii]);
		}




		int start = 0, end = -1;
		for(int ii=start+1; ii<planStructures[planc].length(); ii++) {
			if(level[ii] == level[start]) {
				end = ii;
				break;
			}
		}

		while(start >= 0 && end >0 ) {
			int lmin = 9999;
			for(int ii= start; ii<= end; ii++) 
				lmin = Math.min(lmin, level[ii]);

			for(int ii=start+1; ii< end; ii++) {
				level[ii] = level[ii] - lmin;
			}

			start = end;
			end = -1;
			for(int ii=start+1; ii<planStructures[planc].length(); ii++) {
				if(level[ii] == level[start]) {
					end = ii;
					break;
				}
			}


		}


		System.out.print("\n");
		for(int ii=0; ii<planStructures[planc].length(); ii++) {
			System.out.print("" + level[ii]);
		}


		int changedBitPlan = 0;                  // to record which bits have been changed for this plan
		int doneDims = 0;
		int indicesDims[] = new int[dimension[qt].length];

		// do the dimensions occuring at base (level-1) first


		//		while (changedBitPlan < Math.pow(2, dimension[qt].length) - 1)
		{

			for(int t=0;t<dimension[qt].length;t++){

				if((changedBitPlan & (1 << t)) != 0 )
					continue;

				int loc1 = -1, loc2 = -1;

				if(dimension[qt][t]/10 > 0) {
					loc1 = planStructures[planc].indexOf(dimension[qt][t]/10 + 48);
					loc2 = planStructures[planc].indexOf(dimension[qt][t]%10 + 48);
				}
				else {
					loc1 = planStructures[planc].indexOf(dimension[qt][t]%10 + 48);
					loc2 = planStructures[planc].indexOf(dimension[qt][t]%10 + 48);	
				}


				int lvl = 0;
				for(int k=Math.min(loc1, loc2); k <=Math.max(loc1, loc2); k++){
					lvl = Math.max(lvl, level[k]);
				}


				if(lvl != 0 && increasedLevel[planc] == true)
					lvl++;


				if(loc1 == loc2 && lvl == 0)  // base dim
				{
					if(level[loc1+1] == 0) // first rel - invalid if Hash Join
					{
						int place = planStrings[planc].indexOf(planStructures[planc].charAt(loc1) + "");
						while(planStrings[planc].charAt(place+1) != ')'){
							place++;
						}
						if(planStrings[planc].charAt(place+2) == 'H'){
							lvl = 1;
							increasedLevel[planc] = true;
						}								
					}
					else if(level[loc1-1] == 0) // second rel - invalid if NL Join 
					{
						int place = planStrings[planc].indexOf(planStructures[planc].charAt(loc1) + "");
						while(planStrings[planc].charAt(place-1) != '('){
							place--;
						}
						if(planStrings[planc].charAt(place-2) == 'N'){
							lvl = 1;
							increasedLevel[planc] = true;
						}
					} 
				}
				else if((loc1 == loc2 + 1) || (loc2 == loc1 + 1)) {                  // join-dim - at base
					int place = planStrings[planc].indexOf(planStructures[planc].charAt(Math.max(loc1, loc2)) + "");
					while(planStrings[planc].charAt(place+1) != ')'){
						place++;
					}
					if(planStrings[planc].charAt(place+3) == 'H'){								
						lvl = 1;
						increasedLevel[planc] = true;
					}													


					place = planStrings[planc].indexOf(planStructures[planc].charAt(Math.min(loc1, loc2)) + "");
					while(planStrings[planc].charAt(place+1) != '('){
						place++;
					}
					if(planStrings[planc].charAt(place-3) == 'N'){								
						lvl = 1;
						increasedLevel[planc] = true;
					}													

				}



				indicesDims[t] = lvl;

				System.out.print("\t" + dimension[qt][t] + "(" + lvl + ")");
			}

		}


	}



	public static void planDimOrder(int qt, int plan) {
		HashSet bitChanges = new HashSet();
		int countNeighbors = 0;

		System.out.print("\n\n" +  planStrings[plan]);
		System.out.print("\n" + planStructures[plan]);

		int currentPlan = plan;		
		int changedBitPlan = 0;                  // to record which bits have been changed for this plan
		int indicesDims[] = new int[dimension[qt].length];
		//find next dimension to vary in the current plan
		// add those dimensions which are at base


		while (changedBitPlan < Math.pow(2, dimension[qt].length) - 1){

			for(int t=0;t<dimension[qt].length;t++){
				int index = planStructures[currentPlan].indexOf(dimension[qt][t] +"");

				//if join dimension not found as specified try other way -- i.e. if 23 not found try 32
				if(index < 0 && dimension[qt][t]/10 > 0){
					int p1 = dimension[qt][t]/10;
					int p2 = dimension[qt][t]%10;
					int d = p2 * 10 + p1;

					index = planStructures[currentPlan].indexOf(d + "");
				}

				//
				if(index > 0){
					if(dimension[qt][t]/10 > 0 && (changedBitPlan & (1 << t)) == 0) {
						bitChanges.add(t);
						indicesDims[t] = index;
					}
					else if(dimension[qt][t]/10 == 0 && (changedBitPlan & (1 << t)) == 0){
						if((planStructures[currentPlan].charAt(index-1) >= 48 &&  planStructures[currentPlan].charAt(index-1) < 48) 
								|| (planStructures[currentPlan].charAt(index+1) >= 48 &&  planStructures[currentPlan].charAt(index+1) < 48)){
							bitChanges.add(t);
							indicesDims[t] = index;
						}
					}
				}
			}



			// if no base dimension found (which is not already tried)
			//find which dimension has been already tried for current plan and its place in the planStructure
			if(bitChanges.size() == 0){ 

				int index = -1, minDist = 100000000, minDistDim = -1;
				for(int t=0;t<dimension[qt].length;t++){
					if(indicesDims[t] > 0)
						index = indicesDims[t];
				}


				for(int t=0;t<dimension[qt].length;t++){
					if( (changedBitPlan & (1 << t)) == 0){
						int loc1 = planStructures[currentPlan].indexOf(dimension[qt][t]/10 + 48);
						int loc2 = planStructures[currentPlan].indexOf(dimension[qt][t]%10 + 48);

						int distance = Math.max(Math.abs(index - loc1),Math.abs(index-loc2));

						if(minDist > distance) {
							minDist = distance;
							minDistDim = t;
						}
					}
				}

				if(minDistDim >= 0)
					bitChanges.add(minDistDim);
			}
			//find next dimension to vary in the current plan

			Iterator bitChange_itr = bitChanges.iterator();

			while(bitChanges.size() > 0){
				Object bitO = null;
				int bit = -1;

				bitO = bitChange_itr.next();
				bit = ((Integer)bitO).intValue();
				bitChange_itr.remove();
				changedBitPlan |= (1 << bit);

				System.out.print("\t" + dimension[qt][bit]);

			}
		}


	}


	
   //////////////////////////////////PLAN-STRUCTURE ANALYSIS RELATED CODE//////////////////////////////////////////
	
	
	
	
	
	
	
	
	
	
	
	
	







	/////////////////////////////////// PRE-PROCESSING DATA TO FIND CONTOUR INFORMATION //////////////////////////////////////

	

	public static void contourSetDetails(int steps, int[] SetPlans) {
	
		doneRec = new int[data.length];
		mark = new int[data.length];
		contourPlan = new int[data.length];
		totalPlans = new int[steps+1][nPlans];
		totalPlansCG = new int[steps+1][nPlans];
		
		contourPlanCount = new int[steps+1];					//change steps + 1 to steps
		
		subOptData = new double[SetPlans.length + 3][data.length];
		
		int contPlans[][];
		
		contPlans = new int[steps+1][30];						//change steps +1 to steps
		Arrays.fill(doneRec,0);
		Arrays.fill(mark,-1);
		Arrays.fill(contourPlan,-1);
		
		
		
		
		if(true){
		
			for(int loc=0;loc<data.length;loc++){
				int optimalPlan = newOptimalPlan[loc];
				double optCost = Math.max(AllPlanCosts[optimalPlan][loc], SANITY_CONSTANT);
				
				if(optCost > data[data.length-1].getCost())
					optCost = Math.floor(data[data.length-1].getCost());
//				optCost = Math.min(AllPlanCosts[optimalPlan][loc], data[data.length-1].getCost());
				
				
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
				for(d=0;d<dim;d++){
					int nloc = loc - (int) Math.pow(res, d);
					if(nloc > 0 && nloc < data.length - 1){
						correct++;
						minMark = Math.min(minMark, doneRec[nloc]);
					}
				}
				if(correct == 0)   minMark = 0;
				if(doneRec[loc]==minMark) doneRec[loc] = 0;
			}
			
//			int a=0;
//			for(int loc =0 ; loc<data.length;loc++)
//			{
//				if(doneRec[loc]!=0)
//				{
//					int contour = doneRec[loc];
//					double cont_cost = cost_limit[contour-1];
//					int optimalPlan = newOptimalPlan[loc];
//					double optCost = Math.max(AllPlanCosts[optimalPlan][loc], SANITY_CONSTANT);
//					if(Math.abs(optCost-cont_cost)>0.1*cont_cost)
//					{
//						doneRec[loc] = 0;
//						a++;
//					}
//				}
//			}
			
//			System.out.println("modified donerec = "+a);
			for(int loc=0;loc<data.length;loc++){
				int optimalPlan = newOptimalPlan[loc];
				int CGplanindex = getOptimalPlan(loc,SetPlans);
				if(CGplanindex == -1)
					getOptimalPlan(loc,SetPlans);
				int CGplan = SetPlans[CGplanindex];
				
//				System.out.format("(%3d)(%3d)(%8.1f)",optimalPlan,mark[loc],AllPlanCosts[optimalPlan][loc]);
//				System.out.format("%3d",optimalPlan);
//				System.out.format("%3d",mark[loc]);
//				System.out.format("%3d",doneRec[loc]);
//				System.out.format("%3d(%3d)",mark[loc],doneRec[loc]);
//				System.out.format("%8.1f",AllPlanCosts[optimalPlan][loc]);
				
//				if(loc % res == res-1)													System.out.print("\n");
//				if((loc/res) % res == res-1 && loc % res == res-1)						System.out.print("\n");
				
				if(doneRec[loc] >= 1){
					totalPlans[doneRec[loc]][optimalPlan]++;
					totalPlansCG[doneRec[loc]][CGplan]++;
				}
				
				contour_area[doneRec[loc]] +=  locationWeight[loc];
			}

		
//			System.out.println();
//			System.out.println();
//			System.out.println("doneRec ------------------------------------------------------------------------------------------");
//			for(int i=99;i>=0;i--)
//			{
//				int j=100*i;
//				while(j<(100*i+100))
//				{
//					if(doneRec[j]==0)
////						System.err.print(doneRec[j]);
//						System.out.print(" ");
//					else
//						System.out.print(doneRec[j]);
////						System.out.print(planSet[getOptimalPlan(j, planSet)]);
////						System.out.println(AllPlanCosts[getOptimalPlan(j, planSet)][j]);
//					j++;
//				}
//				System.out.println();
//			
//			}
//			System.out.println();
//			System.out.println("------------------------------------------------------------------------------------------");
//			DrawContours.temp(doneRec, dim, res, steps);
			
			for(int s=1;s<steps+1;s++){  // since the contour counting has started from 1 while marking contours i.e. doneRec[]
				System.out.printf("\nFor contour  %2d [%12.2f]   #plans = ", s, cost_limit[s-1]);
				int numPlans = 0;
				for(int ps=0;ps<nPlans;ps++){
					if(totalPlansCG[s][ps] > 0){
						System.out.print(" " + ps);
						contPlans[s-1][numPlans] = ps;  
						numPlans++;
					}
				}
				System.out.print("\t\t\t" + numPlans);
//				contourPlanCount[s] = numPlans;
				contourPlanCount[s-1] = numPlans;
				System.out.printf("\t\t\tAREA = %4.3f", contour_area[s-1]/areaSpace);
			}
			
			System.out.print("\n\n");

			for(int s=1;s<steps;s++){		
				int numPlans = 0;

				System.out.print("\nCommon contour plans(CG) between " + s + " and " + (s+1) + ":: ");
				
				numPlans = 0;
				for(int ps=0;ps<nPlans;ps++){
					if(totalPlansCG[s][ps] > 0 && totalPlansCG[s+1][ps] > 0){
						System.out.print(" " + ps);
						numPlans++;
					}
				}
				System.out.print("\t\t\t" + numPlans + "\n");
				
			}
//			PostgresRun obj =new PostgresRun();
//			obj.run_postgres(contPlans, steps, contourPlanCount, cost_limit, planSet.length);
		}
	}



  //unused
	static int doneRec[];
	static int mark[];
	static int contourPlan[];
	static int totalPlans[][];
	static int totalPlansCG[][];
	static int contourPlanCount[];
	static int minPlan = -1;


	
	

	private static int[] Costgreedy(double th, int qt) {
	
		int selectedSetPlans[] = new int[30];
		int numSelected = 0;

		int eatingCapacity[] = new int[nPlans];

		int replaces[][] = new int[nPlans][nPlans];   // y replaces x

		for(int p=0; p<nPlans; p++) {

			for(int loc = 0; loc < data.length; loc++){
				if(newOptimalPlan[loc] != p)				continue;    // skip the locations not optimal for p

				double costLoc = AllPlanCosts[p][loc];
				Arrays.fill(replaces[p], 1);

				for(int rep = 0; rep < nPlans; rep ++){
					if(AllPlanCosts[rep][loc] > (1+ th/100) * costLoc){
						replaces[p][rep] = -1;
					}
				}	
			}

			if(replaces[p][0] == 0) {
				//				System.out.print("\n Plan " + p + " was thrown out in FPC world");
				continue;
			}

			//			System.out.print("\n" + p + " : " );
			for(int rep = 0; rep < nPlans; rep ++){
				if(replaces[p][rep] == 1){
					//					System.out.print(" " + rep + " " );
					eatingCapacity[rep]++;
				}
			}
		}



		boolean allReplaced = false;
		int maxEater = -1, maxEaten = -1;
		while(allReplaced == false) {

			System.out.print("\n\n");
			for(int rep = 0; rep < nPlans; rep ++){
				//				System.out.print("\n" + rep + " can eat " + eatingCapacity[rep] + " plans" );
			}

			maxEater = -1; maxEaten = -1;
			for(int rep = 0; rep < nPlans ; rep++){
				if(maxEaten < eatingCapacity[rep]) {
					maxEaten = eatingCapacity[rep];
					maxEater = rep;
				}
			}

			if(maxEaten <= 0) {
				allReplaced = true;
				break;
			}


			int maxPotential = -1;
			int maxPotentialPlan = -1;
			System.out.print("\n Plans who can eat " + maxEaten + " plans:");
			for(int otherEaters = 0; otherEaters < nPlans; otherEaters++){	
				if(eatingCapacity[otherEaters] == maxEaten) {
					int potential = calculateLearningPotential(qt, otherEaters);
					System.out.print("\n" + otherEaters + ": " + planStrings[otherEaters] + "\t" + planStructures[otherEaters] + " with potential = " + potential);

					if(maxPotential < potential){
						maxPotential = potential;
						maxPotentialPlan = otherEaters;
					}

				}
			}




			//			selectedSetPlans[numSelected++] = maxEater;
			selectedSetPlans[numSelected++] = maxPotentialPlan;

			System.out.print("\n" + maxPotentialPlan + " is selected against " + maxEater);

			for(int p = 0; p < nPlans ; p++){
				if(replaces[p][maxEater] == 1){
					for(int rp = 0; rp < nPlans ; rp++){
						if(replaces[p][rp] == 1) {
							eatingCapacity[rp]--;
							replaces[p][rp] = -1;
						}
					}
				}
			}

		}


		int chosenPlans[] = new int[numSelected];

		for(int i=0; i<numSelected; i++){
			chosenPlans[i] = selectedSetPlans[i];
		}

		return chosenPlans;
	}


	

	
	
	
	
			
	
	//////////////////////////////   LOCATION WEIGHT CALCULATIONS and PCM & POSP VALIDITY CHECKING CODE  //////////////////////////////////////////////////////////////
	
	// for the new calculation for exponential distribution
	static double planCount[], planRelativeArea[];
	static float picsel[], locationWeight[];

	static double areaSpace =0,totalEstimatedArea = 0;
	
	
	// for the new calculation for exponential distribution


	// check whether the FPC space of plans is same (with area) as POSP 
	public static void checkPOSP(int dim, int res, int plans){

		int totalPoints = (int) Math.pow(res, dim);
		int area[] = new int[plans];
		int areaOriginal[] = new int[plans];

		int count = 0;

		System.out.format("\n%15s\t%25s\t%15s\t%25s\t%15s\t%25s\n","location","diff%", "true optimal","true optimal cost","observed optimal","observed optimal cost");
		for(int l=0; l<totalPoints; l++){
			int opt = 0;
			double optCost = 0;

			opt = 0;
			optCost = AllPlanCosts[0][l];


			for(int p = 1; p < plans; p++){

				if(optCost > AllPlanCosts[p][l])
					opt = p;

				optCost = Math.min(optCost, AllPlanCosts[p][l]);				
			}

			//			if( (AllPlanCosts[data[l].getPlanNumber()][l] - optCost) * 100.0/AllPlanCosts[data[l].getPlanNumber()][l]  < 1.0 && (AllPlanCosts[data[l].getPlanNumber()][l] - optCost) * 100.0/AllPlanCosts[data[l].getPlanNumber()][l]  > 0.0   ){
			//				//area[opt]++;
			//				area[data[l].getPlanNumber()]++;
			//				areaOriginal[data[l].getPlanNumber()]++;
			//				continue;
			//			}

			//			if(optCost < AllPlanCosts[data[l].getPlanNumber()][l] && opt != data[l].getPlanNumber()){
			//			double trueCost = AllPlanCosts[data[l].getPlanNumber()][l];
			double trueCost = data[l].getCost();
			//double trueCost = AllPlanCosts[data[l].getPlanNumber()][l];


			//			if(data[l].getPlanNumber() == 1)
			//				System.out.format("\n%10d\t%25.2f\t%15d",l,data[l].getCost(), data[l].getPlanNumber());
			//			else 
			if(opt != data[l].getPlanNumber() && 100.0 * (optCost - trueCost)/trueCost > 1.0){
				System.out.format("\n%15d\t%25.2f\t%15d\t%25.2f\t%15d\t%25.2f",l,100.0 * (optCost - trueCost)/trueCost, data[l].getPlanNumber(),trueCost,opt,optCost);


				//				System.out.println("Optimal Plan Differs at location " + l +"(" + opt + ") (" + data[l].getPlanNumber() +")" + " cost-diff = " + 100.0 * (optCost - AllPlanCosts[data[l].getPlanNumber()][l])/optCost) ;
				//				System.out.println("Optimal Plan Differs at location " + l +"(" + opt + ") " + data[l].getPlanNumber() +")" + " costest = " + AllPlanCosts[data[l].getPlanNumber()][l] + "\t optCost =" + optCost ) ;
			}

			area[opt]++;
			areaOriginal[data[l].getPlanNumber()]++;
		}

		System.out.println("\n");
		for(int p = 0; p < plans; p++){
			if(area[p] == 0) {
				count++;
				System.out.println("plan " + p + " = " + area[p] + "\t " + areaOriginal[p] + "\t waste");
			}
			else 
				System.out.println("plan " + p + " = " + area[p] + "\t " + areaOriginal[p]);
		}

		System.out.println(count + " plans are waste");
	}
	
	
	
	//check whether all the plans follow PCM (individually)
	public static void checkPCM(int dimensions, int res, int plans){

		int nextLocs[] = new int [dimensions];

		int coord[] = new int [dimensions];
		int coordLoc[] = new int [dimensions]; 

		int currentLoc = 0;

		int totalPoints = (int) Math.pow(res, dimensions);

		int violations = 0;

		double diff = 0;

		for (int p=0 ; p < plans; p++) {
			for (int i=totalPoints - 1; i >= 0; i--) {
				coord  = getCoordinates(dimensions, res, i);

				currentLoc = getIndex(coord,res);

				for (int j=0 ; j < dimensions; j++) {
					coordLoc = coord;

					if(coordLoc[j] > 0)
						coordLoc[j] = coordLoc[j] - 1;

					nextLocs[j] = getIndex(coordLoc,res);

					if(AllPlanCosts[p][currentLoc] < AllPlanCosts[p][nextLocs[j]]) {
						double newdiff = 100.0 * (AllPlanCosts[p][nextLocs[j]] - AllPlanCosts[p][currentLoc])/AllPlanCosts[p][currentLoc];

						if(newdiff < 1)		continue;

						violations++;
						if(diff <  newdiff ) {
							System.out.println("\nPCM violated:" + AllPlanCosts[p][currentLoc] + " ( at" + currentLoc + ")  <  ( at " + nextLocs[j] + " ) " + " " + AllPlanCosts[p][nextLocs[j]] + "\t For plan  " + p);
							//	printIndex(dimensions,res,currentLoc);
							//	printIndex(dimensions,res,nextLocs[j]);
							diff =  newdiff;
						}


					}
				}
			}
		}

		System.out.println("PCM found " + violations + " violations" +  " (" + diff + ")");


	}
	
	
	
	public static boolean inA(int we) {
		for (int f = 0; f < PicassoConstants.a.length; f++)
			if (we == PicassoConstants.a[f])
				return true;
		return false;
	}
	
	// functions for location weight calculations and the checking for validity
	

	public static void checkValidityofWeights() {
		double areaPlans =0;
		double relativeAreaPlans =0;
		areaSpace = 0.0;

		planRelativeArea = new double[gdp.getMaxPlanNumber()];

		for (int i=0; i< data.length; i++){
			areaSpace += locationWeight[i];
		}
		//	System.out.println(areaSpace);

		for (int i=0; i< gdp.getMaxPlanNumber(); i++){
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

	
	
	public static void getPlanCountArray() {
		planCount = new double[gdp.getMaxPlanNumber()];
		locationWeight = new float[data.length];
		// Set dim depending on whether we are dealing with full packet or slice
		// int dim;

		double StartPoint[] = { 0.0, 0.0, 0.0, 0.0, 0.0 };
		double EndPoint[] = { 1.0, 1.0, 1.0, 1.0, 1.0 };

		int resln = gdp.getMaxResolution();
		int dim = gdp.getDimension();

		int[] r = new int[dim];

		for (int i = 0; i < dim; i++)
			r[i] = resln;

		picsel = new float[5 * resln];
		// picsel = new float[dim*resln];

		//		if (resln == 10) {
		//			picsel[0] = 0.03258f;			picsel[1] = 0.16292f;			picsel[2] = 0.42359f;			picsel[3] = 0.94493f;			picsel[4] = 1.98762f;			picsel[5] = 4.07299f;
		//			picsel[6] = 8.24373f;			picsel[7] = 16.58521f;			picsel[8] = 33.26817f;			picsel[9] = 66.63408f;			picsel[10] = 0.03258f;			picsel[11] = 0.16292f;
		//			picsel[12] = 0.42359f;			picsel[13] = 0.94493f;			picsel[14] = 1.98762f;			picsel[15] = 4.07299f;			picsel[16] = 8.24373f;			picsel[17] = 16.58521f;
		//			picsel[18] = 33.26817f;			picsel[19] = 66.63408f;			picsel[20] = 0.03258f;			picsel[21] = 0.16292f;			picsel[22] = 0.42359f;			picsel[23] = 0.94493f;
		//			picsel[24] = 1.98762f;			picsel[25] = 4.07299f;			picsel[26] = 8.24373f;			picsel[27] = 16.58521f;			picsel[28] = 33.26817f;			picsel[29] = 66.63408f;
		//
		//		}
		//
		//		else if (resln == 30) {
		//			picsel[0] = 0.00273f;			picsel[1] = 0.00998f;			picsel[2] = 0.01963f;			picsel[3] = 0.03246f;			picsel[4] = 0.04953f;			picsel[5] = 0.07222f;
		//			picsel[6] = 0.10241f;			picsel[7] = 0.14256f;			picsel[8] = 0.19596f;			picsel[9] = 0.26698f;			picsel[10] = 0.36144f;			picsel[11] = 0.48707f;
		//			picsel[12] = 0.65416f;			picsel[13] = 0.87638f;			picsel[14] = 1.17194f;			picsel[15] = 1.56504f;			picsel[16] = 2.08785f;			picsel[17] = 2.78320f;
		//			picsel[18] = 3.70800f;			picsel[19] = 4.93800f;			picsel[20] = 6.57389f;			picsel[21] = 8.74963f;			picsel[22] = 11.64337f;			picsel[23] = 15.49203f;
		//			picsel[24] = 20.61076f;			picsel[25] = 27.41866f;			picsel[26] = 36.47317f;			picsel[27] = 48.51568f;			picsel[28] = 64.53220f;			picsel[29] = 85.83418f;
		//
		//			picsel[30] = 0.00273f;			picsel[31] = 0.00998f;			picsel[32] = 0.01963f;			picsel[33] = 0.03246f;			picsel[34] = 0.04953f;			picsel[35] = 0.07222f;
		//			picsel[36] = 0.10241f;			picsel[37] = 0.14256f;			picsel[38] = 0.19596f;			picsel[39] = 0.26698f;			picsel[40] = 0.36144f;			picsel[41] = 0.48707f;
		//			picsel[42] = 0.65416f;			picsel[43] = 0.87638f;			picsel[44] = 1.17194f;			picsel[45] = 1.56504f;			picsel[46] = 2.08785f;			picsel[47] = 2.78320f;
		//			picsel[48] = 3.70800f;			picsel[49] = 4.93800f;			picsel[50] = 6.57389f;			picsel[51] = 8.74963f;			picsel[52] = 11.64337f;			picsel[53] = 15.49203f;
		//			picsel[54] = 20.61076f;			picsel[55] = 27.41866f;			picsel[56] = 36.47317f;			picsel[57] = 48.51568f;			picsel[58] = 64.53220f;			picsel[59] = 85.83418f;
		//
		//			picsel[60] = 0.00273f;			picsel[61] = 0.00998f;			picsel[62] = 0.01963f;			picsel[63] = 0.03246f;			picsel[64] = 0.04953f;			picsel[65] = 0.07222f;
		//			picsel[66] = 0.10241f;			picsel[67] = 0.14256f;			picsel[68] = 0.19596f;			picsel[69] = 0.26698f;			picsel[70] = 0.36144f;			picsel[71] = 0.48707f;
		//			picsel[72] = 0.65416f;			picsel[73] = 0.87638f;			picsel[74] = 1.17194f;			picsel[75] = 1.56504f;			picsel[76] = 2.08785f;			picsel[77] = 2.78320f;
		//			picsel[78] = 3.70800f;			picsel[79] = 4.93800f;			picsel[80] = 6.57389f;			picsel[81] = 8.74963f;			picsel[82] = 11.64337f;			picsel[83] = 15.49203f;
		//			picsel[84] = 20.61076f;			picsel[85] = 27.41866f;			picsel[86] = 36.47317f;			picsel[87] = 48.51568f;			picsel[88] = 64.53220f;			picsel[89] = 85.83418f;
		//
		//		}


		if(resln == 10) {
			if(JSP_ExpoDistributionBase == 2) {
				// april - 2012
				//				picsel[0] = 0.005f;			picsel[1] = 0.01f;			picsel[2] = 0.02f;			picsel[3] = 0.05f;			picsel[4] = 0.1f;			picsel[5] = 0.15f;	
				//				picsel[6] = 0.30f;			picsel[7] = 0.50f;			picsel[8] = 0.70f;			picsel[9] = 0.90f;                                 
				//
				//				picsel[10] = 0.005f;		picsel[11] = 0.01f;			picsel[12] = 0.02f;			picsel[13] = 0.05f;			picsel[14] = 0.1f;			picsel[15] = 0.15f;	
				//				picsel[16] = 0.30f;			picsel[17] = 0.50f;			picsel[18] = 0.70f;			picsel[19] = 0.90f;                                 
				//
				//				picsel[20] = 0.005f;		picsel[21] = 0.01f;			picsel[22] = 0.02f;			picsel[23] = 0.05f;			picsel[24] = 0.1f;			picsel[25] = 0.15f;	
				//				picsel[26] = 0.30f;			picsel[27] = 0.50f;			picsel[28] = 0.70f;			picsel[29] = 0.90f;                                 
				//
				//				picsel[30] = 0.005f;		picsel[31] = 0.01f;			picsel[32] = 0.02f;			picsel[33] = 0.05f;			picsel[34] = 0.1f;			picsel[35] = 0.15f;	
				//				picsel[36] = 0.30f;			picsel[37] = 0.50f;			picsel[38] = 0.70f;			picsel[39] = 0.90f;                                 
				//
				//				picsel[40] = 0.005f;		picsel[41] = 0.01f;			picsel[42] = 0.02f;			picsel[43] = 0.05f;			picsel[44] = 0.1f;			picsel[45] = 0.15f;	
				//				picsel[46] = 0.30f;			picsel[47] = 0.50f;			picsel[48] = 0.70f;			picsel[49] = 0.90f;                                 
				//oct 2012

				//				picsel[0] = 0.0005f;		picsel[1] = 0.05f;			picsel[2] = 0.01f;			picsel[3] = 0.02f;			picsel[4] = 0.05f;			picsel[5] = 0.10f;	
				//				picsel[6] = 0.20f;			picsel[7] = 0.40f;			picsel[8] = 0.60f;			picsel[9] = 0.95f;                                 
				//
				//				picsel[10] = 0.0005f;		picsel[11] = 0.05f;			picsel[12] = 0.01f;			picsel[13] = 0.02f;			picsel[14] = 0.05f;			picsel[15] = 0.10f;	
				//				picsel[16] = 0.20f;			picsel[17] = 0.40f;			picsel[18] = 0.60f;			picsel[19] = 0.95f;
				//				
				//				picsel[20] = 0.0005f;		picsel[21] = 0.05f;			picsel[22] = 0.01f;			picsel[23] = 0.02f;			picsel[24] = 0.05f;			picsel[25] = 0.10f;	
				//				picsel[26] = 0.20f;			picsel[27] = 0.40f;			picsel[28] = 0.60f;			picsel[29] = 0.95f;
				//				
				//				picsel[30] = 0.0005f;		picsel[31] = 0.05f;			picsel[32] = 0.01f;			picsel[33] = 0.02f;			picsel[34] = 0.05f;			picsel[35] = 0.10f;	
				//				picsel[36] = 0.20f;			picsel[37] = 0.40f;			picsel[38] = 0.60f;			picsel[39] = 0.95f;
				//				
				//				picsel[40] = 0.0005f;		picsel[41] = 0.05f;			picsel[42] = 0.01f;			picsel[43] = 0.02f;			picsel[44] = 0.05f;			picsel[45] = 0.10f;	
				//				picsel[46] = 0.20f;			picsel[47] = 0.40f;			picsel[48] = 0.60f;			picsel[49] = 0.95f;


				picsel[0] = 0.00005f;		picsel[1] = 0.0005f;		picsel[2] = 0.005f;			picsel[3] = 0.02f;			picsel[4] = 0.05f;			picsel[5] = 0.10f;	
				picsel[6] = 0.15f;			picsel[7] = 0.25f;			picsel[8] = 0.50f;			picsel[9] = 0.99f;                                 

				picsel[10] = 0.00005f;		picsel[11] = 0.0005f;		picsel[12] = 0.005f;		picsel[13] = 0.02f;			picsel[14] = 0.05f;			picsel[15] = 0.10f;	
				picsel[16] = 0.15f;			picsel[17] = 0.25f;			picsel[18] = 0.50f;			picsel[19] = 0.99f;

				picsel[20] = 0.00005f;		picsel[21] = 0.0005f;		picsel[22] = 0.005f;		picsel[23] = 0.02f;			picsel[24] = 0.05f;			picsel[25] = 0.10f;	
				picsel[26] = 0.15f;			picsel[27] = 0.25f;			picsel[28] = 0.50f;			picsel[29] = 0.99f;

				picsel[30] = 0.00005f;		picsel[31] = 0.0005f;		picsel[32] = 0.005f;		picsel[33] = 0.02f;			picsel[34] = 0.05f;			picsel[35] = 0.10f;	
				picsel[36] = 0.15f;			picsel[37] = 0.25f;			picsel[38] = 0.50f;			picsel[39] = 0.99f;

				picsel[40] = 0.00005f;		picsel[41] = 0.0005f;		picsel[42] = 0.005f;		picsel[43] = 0.02f;			picsel[44] = 0.05f;			picsel[45] = 0.10f;	
				picsel[46] = 0.15f;			picsel[47] = 0.25f;			picsel[48] = 0.50f;			picsel[49] = 0.99f;


				//				picsel[0] = 0.16f;			picsel[1] = 0.42f;			picsel[2] = 0.94f;			picsel[3] = 1.98f;			picsel[4] = 4.07f;			picsel[5] = 8.24f;	
				//				picsel[6] = 16.58f;			picsel[7] = 33.26f;			picsel[8] = 66.63f;			picsel[9] = 99.50f;
				//
				//				picsel[10] = 0.16f;			picsel[11] = 0.42f;			picsel[12] = 0.94f;			picsel[13] = 1.98f;			picsel[14] = 4.07f;			picsel[15] = 8.24f;	
				//				picsel[16] = 16.58f;		picsel[17] = 33.26f;		picsel[18] = 66.63f;		picsel[19] = 99.50f;			
				//
				//				picsel[20] = 0.16f;			picsel[21] = 0.42f;			picsel[22] = 0.94f;			picsel[23] = 1.98f;			picsel[24] = 4.07f;			picsel[25] = 8.24f;	
				//				picsel[26] = 16.58f;		picsel[27] = 33.26f;		picsel[28] = 66.63f;		picsel[29] = 99.50f;
				//
				//				picsel[30] = 0.16f;			picsel[31] = 0.42f;			picsel[32] = 0.94f;			picsel[33] = 1.98f;			picsel[34] = 4.07f;			picsel[35] = 8.24f;	
				//				picsel[36] = 16.58f;		picsel[37] = 33.26f;		picsel[38] = 66.63f;		picsel[39] = 99.50f;
				//
				//				picsel[40] = 0.16f;			picsel[41] = 0.42f;			picsel[42] = 0.94f;			picsel[43] = 1.98f;			picsel[44] = 4.07f;			picsel[45] = 8.24f;	
				//				picsel[46] = 16.58f;		picsel[47] = 33.26f;		picsel[48] = 66.63f;		picsel[49] = 99.50f;
			}
			else if(JSP_ExpoDistributionBase == 5) {
				picsel[0] = 0.0000512f;		picsel[1] = 0.000256f;		picsel[2] = 0.00128f;		picsel[3] = 0.0064f;		picsel[4] = 0.032f;			picsel[5] = 0.16f;	
				picsel[6] = 0.8f;			picsel[7] = 4.0f;			picsel[8] = 20.0f;			picsel[9] = 99.50f;

				picsel[10] = 0.0000512f;	picsel[11] = 0.000256f;		picsel[12] = 0.00128f;		picsel[13] = 0.0064f;		picsel[14] = 0.032f;		picsel[15] = 0.16f;
				picsel[16] = 0.8f;			picsel[17] = 4.0f;			picsel[18] = 20.0f;			picsel[19] = 99.50f;

				picsel[20] = 0.0000512f;	picsel[21] = 0.000256f;		picsel[22] = 0.00128f;		picsel[23] = 0.0064f;		picsel[24] = 0.032f;		picsel[25] = 0.16f;
				picsel[26] = 0.8f;			picsel[27] = 4.0f;			picsel[28] = 20.0f;			picsel[29] = 99.50f;

				picsel[30] = 0.0000512f;	picsel[31] = 0.000256f;		picsel[32] = 0.00128f;		picsel[33] = 0.0064f;		picsel[34] = 0.032f;		picsel[35] = 0.16f;	
				picsel[36] = 0.8f;			picsel[37] = 4.0f;			picsel[38] = 20.0f;			picsel[39] = 99.50f;

				picsel[40] = 0.0000512f;	picsel[41] = 0.000256f;		picsel[42] = 0.00128f;		picsel[43] = 0.0064f;		picsel[44] = 0.032f;		picsel[45] = 0.16f;	
				picsel[46] = 0.8f;			picsel[47] = 4.0f;			picsel[48] = 20.0f;			picsel[49] = 99.50f;
			}
		}
		else if(resln == 20) {
			if(JSP_ExpoDistributionBase == 2) {
				// april - 2012
				picsel[0] = 0.005f;			picsel[1] = 0.02f;			picsel[2] = 0.04f;			picsel[3] = 0.06f;			picsel[4] = 0.08f;			picsel[5] = 0.10f;	
				picsel[6] = 0.12f;			picsel[7] = 0.14f;			picsel[8] = 0.16f;			picsel[9] = 0.18f;   		picsel[10] = 0.2f;			picsel[11] = 0.25f;
				picsel[12]= 0.30f;			picsel[13] = 0.40f;			picsel[14] = 0.50f;			picsel[15] = 0.60f;			picsel[16] = 0.70f;			picsel[17] = 0.80f;	
				picsel[18] = 0.90f;			picsel[19] = 0.9950f;	

				picsel[20] = 0.005f;		picsel[21] = 0.02f;			picsel[22] = 0.04f;			picsel[23] = 0.06f;			picsel[24] = 0.08f;			picsel[25] = 0.10f;	
				picsel[26] = 0.12f;			picsel[27] = 0.14f;			picsel[28] = 0.16f;			picsel[29] = 0.18f;   		picsel[30] = 0.2f;			picsel[31] = 0.25f;
				picsel[32]= 0.30f;			picsel[33] = 0.40f;			picsel[34] = 0.50f;			picsel[35] = 0.60f;			picsel[36] = 0.70f;			picsel[37] = 0.80f;	
				picsel[38] = 0.90f;			picsel[39] = 0.9950f;	

				picsel[40] = 0.005f;		picsel[41] = 0.02f;			picsel[42] = 0.04f;			picsel[43] = 0.06f;			picsel[44] = 0.08f;			picsel[45] = 0.10f;	
				picsel[46] = 0.12f;			picsel[47] = 0.14f;			picsel[48] = 0.16f;			picsel[49] = 0.18f;   		picsel[50] = 0.2f;			picsel[51] = 0.25f;
				picsel[52]= 0.30f;			picsel[53] = 0.40f;			picsel[54] = 0.50f;			picsel[55] = 0.60f;			picsel[56] = 0.70f;			picsel[57] = 0.80f;	
				picsel[58] = 0.90f;			picsel[59] = 0.9950f;	

				picsel[60] = 0.005f;		picsel[61] = 0.02f;			picsel[62] = 0.04f;			picsel[63] = 0.06f;			picsel[64] = 0.08f;			picsel[65] = 0.10f;	
				picsel[66] = 0.12f;			picsel[67] = 0.14f;			picsel[68] = 0.16f;			picsel[69] = 0.18f;   		picsel[70] = 0.2f;			picsel[71] = 0.25f;
				picsel[72]= 0.30f;			picsel[73] = 0.40f;			picsel[74] = 0.50f;			picsel[75] = 0.60f;			picsel[76] = 0.70f;			picsel[77] = 0.80f;	
				picsel[78] = 0.90f;			picsel[79] = 0.9950f;	

				picsel[80] = 0.005f;		picsel[81] = 0.02f;			picsel[82] = 0.04f;			picsel[83] = 0.06f;			picsel[84] = 0.08f;			picsel[85] = 0.10f;	
				picsel[86] = 0.12f;			picsel[87] = 0.14f;			picsel[88] = 0.16f;			picsel[89] = 0.18f;   		picsel[90] = 0.2f;			picsel[91] = 0.25f;
				picsel[92]= 0.30f;			picsel[93] = 0.40f;			picsel[94] = 0.50f;			picsel[95] = 0.60f;			picsel[96] = 0.70f;			picsel[97] = 0.80f;	
				picsel[98] = 0.90f;			picsel[99] = 0.9950f;	




			}
		}
		else if(resln == 30){
			picsel[0] = 0.005f;  			picsel[1] = 0.01f;			picsel[2] = 0.03f;			picsel[3] = 0.05f;			picsel[4] = 0.07f;  		picsel[5] = 0.10f;	
			picsel[6] = 0.14f;				picsel[7] = 0.19f;			picsel[8] = 0.26f;			picsel[9] = 0.36f;			picsel[10] = 0.48f;		    picsel[11] = 0.65f;
			picsel[12] = 0.87f;				picsel[13] = 1.17f;			picsel[14] = 1.56f;			picsel[15] = 2.08f;			picsel[16] = 2.78f;			picsel[17] = 3.70f;	
			picsel[18] = 4.93f;				picsel[19] = 6.57f;			picsel[20] = 8.74f;			picsel[21] = 11.64f;		picsel[22] = 15.49f;		picsel[23] = 20.61f;
			picsel[24] = 27.41f;			picsel[25] = 36.47f;		picsel[26] = 48.515f;		picsel[27] = 64.53f;		picsel[28] = 85.83f;		picsel[29] = 99.50f;

			picsel[30] = 0.005f;  			picsel[31] = 0.01f;			picsel[32] = 0.03f;			picsel[33] = 0.05f;			picsel[34] = 0.07f;  		picsel[35] = 0.10f;	
			picsel[36] = 0.14f;				picsel[37] = 0.19f;			picsel[38] = 0.26f;			picsel[39] = 0.36f;			picsel[40] = 0.48f;		    picsel[41] = 0.65f;
			picsel[42] = 0.87f;				picsel[43] = 1.17f;			picsel[44] = 1.56f;			picsel[45] = 2.08f;			picsel[46] = 2.78f;			picsel[47] = 3.70f;	
			picsel[48] = 4.93f;				picsel[49] = 6.57f;			picsel[50] = 8.74f;			picsel[51] = 11.64f;		picsel[52] = 15.49f;		picsel[53] = 20.61f;
			picsel[54] = 27.41f;			picsel[55] = 36.47f;		picsel[56] = 48.515f;		picsel[57] = 64.53f;		picsel[58] = 85.83f;		picsel[59] = 99.50f;

			picsel[60] = 0.005f;  			picsel[61] = 0.01f;			picsel[62] = 0.03f;			picsel[63] = 0.05f;			picsel[64] = 0.07f;  		picsel[65] = 0.10f;	
			picsel[66] = 0.14f;				picsel[67] = 0.19f;			picsel[68] = 0.26f;			picsel[69] = 0.36f;			picsel[70] = 0.48f;		    picsel[71] = 0.65f;
			picsel[72] = 0.87f;				picsel[73] = 1.17f;			picsel[74] = 1.56f;			picsel[75] = 2.08f;			picsel[76] = 2.78f;			picsel[77] = 3.70f;	
			picsel[78] = 4.93f;				picsel[79] = 6.57f;			picsel[80] = 8.74f;			picsel[81] = 11.64f;		picsel[82] = 15.49f;		picsel[83] = 20.61f;
			picsel[84] = 27.41f;			picsel[85] = 36.47f;		picsel[86] = 48.515f;		picsel[87] = 64.53f;		picsel[88] = 85.83f;		picsel[89] = 99.50f;
		}
		else if (resln == 100) {
			picsel[0] = 0.00137f;			picsel[1] = 0.00435f;			picsel[2] = 0.00757f;			picsel[3] = 0.01106f;			picsel[4] = 0.01483f;			picsel[5] = 0.01893f;
			picsel[6] = 0.02336f;			picsel[7] = 0.02816f;			picsel[8] = 0.03335f;			picsel[9] = 0.03898f;			picsel[10] = 0.04508f;			picsel[11] = 0.05168f;			
			picsel[12] = 0.05883f;			picsel[13] = 0.06657f;			picsel[14] = 0.07495f;			picsel[15] = 0.08404f;			picsel[16] = 0.09387f;			picsel[17] = 0.10452f;
			picsel[18] = 0.11606f;			picsel[19] = 0.12855f;			picsel[20] = 0.14208f;			picsel[21] = 0.15673f;			picsel[22] = 0.17260f;			picsel[23] = 0.18979f;
			picsel[24] = 0.20840f;			picsel[25] = 0.22856f;			picsel[26] = 0.25039f;			picsel[27] = 0.27403f;			picsel[28] = 0.29964f;			picsel[29] = 0.32737f;
			picsel[30] = 0.35740f;			picsel[31] = 0.38992f;			picsel[32] = 0.42514f;			picsel[33] = 0.46329f;			picsel[34] = 0.50461f;			picsel[35] = 0.54935f;
			picsel[36] = 0.59780f;			picsel[37] = 0.65028f;			picsel[38] = 0.70711f;			picsel[39] = 0.76867f;			picsel[40] = 0.83532f;			picsel[41] = 0.90752f;			
			picsel[42] = 0.98570f;			picsel[43] = 1.07037f;			picsel[44] = 1.16208f;			picsel[45] = 1.26139f;			picsel[46] = 1.36894f;			picsel[47] = 1.48543f;
			picsel[48] = 1.61158f;			picsel[49] = 1.74820f;			picsel[50] = 1.89616f;			picsel[51] = 2.05640f;			picsel[52] = 2.22994f;			picsel[53] = 2.41788f;
			picsel[54] = 2.62143f;			picsel[55] = 2.84187f;			picsel[56] = 3.08060f;			picsel[57] = 3.33915f;			picsel[58] = 3.61916f;			picsel[59] = 3.92241f;
			picsel[60] = 4.25083f;			picsel[61] = 4.60651f;			picsel[62] = 4.99171f;			picsel[63] = 5.40889f;			picsel[64] = 5.86068f;			picsel[65] = 6.34998f;
			picsel[66] = 6.87989f;			picsel[67] = 7.45378f;			picsel[68] = 8.07530f;			picsel[69] = 8.74841f;			picsel[70] = 9.47739f;			picsel[71] = 10.26688f;
			picsel[72] = 11.12189f;			picsel[73] = 12.04787f;			picsel[74] = 13.05070f;			picsel[75] = 14.13677f;			picsel[76] = 15.31298f;			picsel[77] = 16.58681f;
			picsel[78] = 17.96638f;			picsel[79] = 19.46045f;			picsel[80] = 21.07853f;			picsel[81] = 22.83091f;			picsel[82] = 24.72873f;			picsel[83] = 26.78408f;
			picsel[84] = 29.01001f;			picsel[85] = 31.42071f;			picsel[86] = 34.03148f;			picsel[87] = 36.85896f;			picsel[88] = 39.92111f;			picsel[89] = 43.23742f;
			picsel[90] = 46.82899f;			picsel[91] = 50.71866f;			picsel[92] = 54.93116f;			picsel[93] = 59.49331f;			picsel[94] = 64.43412f;			picsel[95] = 69.78501f;
			picsel[96] = 75.58002f;			picsel[97] = 81.85603f;			picsel[98] = 88.65294f;			picsel[99] = 96.01399f;

			picsel[100] = 0.00137f;			picsel[101] = 0.00435f;			picsel[102] = 0.00757f;			picsel[103] = 0.01106f;			picsel[104] = 0.01483f;			picsel[105] = 0.01893f;
			picsel[106] = 0.02336f;			picsel[107] = 0.02816f;			picsel[108] = 0.03335f;			picsel[109] = 0.03898f;			picsel[110] = 0.04508f;			picsel[111] = 0.05168f;
			picsel[112] = 0.05883f;			picsel[113] = 0.06657f;			picsel[114] = 0.07495f;			picsel[115] = 0.08404f;			picsel[116] = 0.09387f;			picsel[117] = 0.10452f;
			picsel[118] = 0.11606f;			picsel[119] = 0.12855f;			picsel[120] = 0.14208f;			picsel[121] = 0.15673f;			picsel[122] = 0.17260f;			picsel[123] = 0.18979f;
			picsel[124] = 0.20840f;			picsel[125] = 0.22856f;			picsel[126] = 0.25039f;			picsel[127] = 0.27403f;			picsel[128] = 0.29964f;			picsel[129] = 0.32737f;
			picsel[130] = 0.35740f;			picsel[131] = 0.38992f;			picsel[132] = 0.42514f;			picsel[133] = 0.46329f;			picsel[134] = 0.50461f;			picsel[135] = 0.54935f;
			picsel[136] = 0.59780f;			picsel[137] = 0.65028f;			picsel[138] = 0.70711f;			picsel[139] = 0.76867f;			picsel[140] = 0.83532f;			picsel[141] = 0.90752f;
			picsel[142] = 0.98570f;			picsel[143] = 1.07037f;			picsel[144] = 1.16208f;			picsel[145] = 1.26139f;			picsel[146] = 1.36894f;			picsel[147] = 1.48543f;
			picsel[148] = 1.61158f;			picsel[149] = 1.74820f;			picsel[150] = 1.89616f;			picsel[151] = 2.05640f;			picsel[152] = 2.22994f;			picsel[153] = 2.41788f;
			picsel[154] = 2.62143f;			picsel[155] = 2.84187f;			picsel[156] = 3.08060f;			picsel[157] = 3.33915f;			picsel[158] = 3.61916f;			picsel[159] = 3.92241f;
			picsel[160] = 4.25083f;			picsel[161] = 4.60651f;			picsel[162] = 4.99171f;			picsel[163] = 5.40889f;			picsel[164] = 5.86068f;			picsel[165] = 6.34998f;
			picsel[166] = 6.87989f;			picsel[167] = 7.45378f;			picsel[168] = 8.07530f;			picsel[169] = 8.74841f;			picsel[170] = 9.47739f;			picsel[171] = 10.26688f;
			picsel[172] = 11.12189f;		picsel[173] = 12.04787f;		picsel[174] = 13.05070f;		picsel[175] = 14.13677f;		picsel[176] = 15.31298f;		picsel[177] = 16.58681f;
			picsel[178] = 17.96638f;		picsel[179] = 19.46045f;		picsel[180] = 21.07853f;		picsel[181] = 22.83091f;		picsel[182] = 24.72873f;		picsel[183] = 26.78408f;
			picsel[184] = 29.01001f;		picsel[185] = 31.42071f;		picsel[186] = 34.03148f;		picsel[187] = 36.85896f;		picsel[188] = 39.92111f;		picsel[189] = 43.23742f;
			picsel[190] = 46.82899f;		picsel[191] = 50.71866f;		picsel[192] = 54.93116f;		picsel[193] = 59.49331f;		picsel[194] = 64.43412f;		picsel[195] = 69.78501f;
			picsel[196] = 75.58002f;		picsel[197] = 81.85603f;		picsel[198] = 88.65294f;		picsel[199] = 96.01399f;		

			picsel[200] = 0.00137f;			picsel[201] = 0.00435f;			picsel[202] = 0.00757f;			picsel[203] = 0.01106f;			picsel[204] = 0.01483f;			picsel[205] = 0.01893f;
			picsel[206] = 0.02336f;			picsel[207] = 0.02816f;			picsel[208] = 0.03335f;			picsel[209] = 0.03898f;			picsel[210] = 0.04508f;			picsel[211] = 0.05168f;
			picsel[212] = 0.05883f;			picsel[213] = 0.06657f;			picsel[214] = 0.07495f;			picsel[215] = 0.08404f;			picsel[216] = 0.09387f;			picsel[217] = 0.10452f;
			picsel[218] = 0.11606f;			picsel[219] = 0.12855f;			picsel[220] = 0.14208f;			picsel[221] = 0.15673f;			picsel[222] = 0.17260f;			picsel[223] = 0.18979f;
			picsel[224] = 0.20840f;			picsel[225] = 0.22856f;			picsel[226] = 0.25039f;			picsel[227] = 0.27403f;			picsel[228] = 0.29964f;			picsel[229] = 0.32737f;
			picsel[230] = 0.35740f;			picsel[231] = 0.38992f;			picsel[232] = 0.42514f;			picsel[233] = 0.46329f;			picsel[234] = 0.50461f;			picsel[235] = 0.54935f;
			picsel[236] = 0.59780f;			picsel[237] = 0.65028f;			picsel[238] = 0.70711f;			picsel[239] = 0.76867f;			picsel[240] = 0.83532f;			picsel[241] = 0.90752f;
			picsel[242] = 0.98570f;			picsel[243] = 1.07037f;			picsel[244] = 1.16208f;			picsel[245] = 1.26139f;			picsel[246] = 1.36894f;			picsel[247] = 1.48543f;
			picsel[248] = 1.61158f;			picsel[249] = 1.74820f;			picsel[250] = 1.89616f;			picsel[251] = 2.05640f;			picsel[252] = 2.22994f;			picsel[253] = 2.41788f;
			picsel[254] = 2.62143f;			picsel[255] = 2.84187f;			picsel[256] = 3.08060f;			picsel[257] = 3.33915f;			picsel[258] = 3.61916f;			picsel[259] = 3.92241f;
			picsel[260] = 4.25083f;			picsel[261] = 4.60651f;			picsel[262] = 4.99171f;			picsel[263] = 5.40889f;			picsel[264] = 5.86068f;			picsel[265] = 6.34998f;
			picsel[266] = 6.87989f;			picsel[267] = 7.45378f;			picsel[268] = 8.07530f;			picsel[269] = 8.74841f;			picsel[270] = 9.47739f;			picsel[271] = 10.26688f;
			picsel[272] = 11.12189f;		picsel[273] = 12.04787f;		picsel[274] = 13.05070f;		picsel[275] = 14.13677f;		picsel[276] = 15.31298f;		picsel[277] = 16.58681f;
			picsel[278] = 17.96638f;		picsel[279] = 19.46045f;		picsel[280] = 21.07853f;		picsel[281] = 22.83091f;		picsel[282] = 24.72873f;		picsel[283] = 26.78408f;
			picsel[284] = 29.01001f;		picsel[285] = 31.42071f;		picsel[286] = 34.03148f;		picsel[287] = 36.85896f;		picsel[288] = 39.92111f;		picsel[289] = 43.23742f;
			picsel[290] = 46.82899f;		picsel[291] = 50.71866f;		picsel[292] = 54.93116f;		picsel[293] = 59.49331f;		picsel[294] = 64.43412f;		picsel[295] = 69.78501f;
			picsel[296] = 75.58002f;		picsel[297] = 81.85603f;		picsel[298] = 88.65294f;		picsel[299] = 96.01399f;

		}

		/*
		 * if(gdp.getDimension()==1) dim = 1; else if(data.length > r[0] * r[1])
		 * //full packet { dim = dim; //set to actual number of dimensions }
		 * else //slice { if(getDimension()==1) dim = 1; else dim = 2; //if
		 * actual dimension >= 2 }
		 */

		float[] selvals;
		selvals = picsel;
		boolean scaleupflag = false;

		if (UNIFORM_DISTRIBUTION == 1) {
			for (int i = 0; i < data.length; i++) {
				planCount[data[i].getPlanNumber()]++;
				locationWeight[i] = 1;
			}
		}
		else if(JSP_ExpoDistributionBase == 2) {                 // april - 2012

			double locationWeightLocal[] = new double[resln];

			if(resln == 10) {
				//				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 2;              
				//				locationWeightLocal[3] = 4;         locationWeightLocal[4] = 5;				locationWeightLocal[5] = 7;             
				//				locationWeightLocal[6] = 20;        locationWeightLocal[7] = 20;			locationWeightLocal[8] = 20;             
				//				locationWeightLocal[9] = 20;

				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1;              
				locationWeightLocal[3] = 2;         locationWeightLocal[4] = 4;				locationWeightLocal[5] = 6;             
				locationWeightLocal[6] = 7;        locationWeightLocal[7] = 30;				locationWeightLocal[8] = 30;            
				locationWeightLocal[9] = 20;


			}
			else if(resln == 20){
				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1;
				locationWeightLocal[3] = 1;			locationWeightLocal[4] = 1;				locationWeightLocal[5] = 1;
				locationWeightLocal[6] = 1;			locationWeightLocal[7] = 1;				locationWeightLocal[8] = 1;
				locationWeightLocal[9] = 1;		

				locationWeightLocal[10] = 2;		locationWeightLocal[11] = 2;			locationWeightLocal[12] = 5;
				locationWeightLocal[13] = 5;		locationWeightLocal[14] = 5;			locationWeightLocal[15] = 5;
				locationWeightLocal[16] = 5;		locationWeightLocal[17] = 5;			locationWeightLocal[18] = 5;
				locationWeightLocal[19] = 2.5;						
			}
			else if(resln == 30){
				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1.6;
				locationWeightLocal[3] = 2;			locationWeightLocal[4] = 4;				locationWeightLocal[5] = 4;
				locationWeightLocal[6] = 4;			locationWeightLocal[7] = 12;			locationWeightLocal[8] = 20;
				locationWeightLocal[9] = 20;		

				locationWeightLocal[10] = 20;		locationWeightLocal[11] = 20;			locationWeightLocal[12] = 20;
				locationWeightLocal[13] = 20;		locationWeightLocal[14] = 20;			locationWeightLocal[15] = 20;
				locationWeightLocal[16] = 30;		locationWeightLocal[17] = 40;			locationWeightLocal[18] = 40;
				locationWeightLocal[19] = 40;

				locationWeightLocal[20] = 40;		locationWeightLocal[21] = 70;			locationWeightLocal[22] = 100;
				locationWeightLocal[23] = 150;		locationWeightLocal[24] = 200;			locationWeightLocal[25] = 200;
				locationWeightLocal[26] = 200;		locationWeightLocal[27] = 200;			locationWeightLocal[28] = 200;
				locationWeightLocal[29] = 100;	
			}



			for (int loc=0; loc < data.length; loc++)
			{
				double weight = 1.0;
				int tempLoc = loc;
				for(int d=0;d<dim;d++){
					weight *= locationWeightLocal[tempLoc % resln];
					tempLoc = tempLoc/10;
				}

				locationWeight[loc] = (float) weight;
				planCount[data[loc].getPlanNumber()] += weight;
			}	
		}


		else // Exponential
		{
			/*
			 * for(int i = 0; i < dim; i++) { if(getQueryPacket().getEndPoint(i)
			 * - getQueryPacket().getStartPoint(i) < 0.05) scaleupflag = true; }
			 */
			int idx[] = new int[dim]; // will be set to all 0's.

			if (PicassoConstants.a[0] == -1 || PicassoConstants.a[1] == -1	|| PicassoConstants.a[2] == -1 || PicassoConstants.a[3] == -1 || PicassoConstants.a[4] == -1)
				// this is necessary because after viewing a 1D diagram, one of the
				// PicassConstats.a elements is -1. This is to remove this.
			{
				PicassoConstants.a[0] = 0;
				PicassoConstants.a[1] = 1;
				PicassoConstants.a[2] = 2;
				PicassoConstants.a[3] = 3;
				PicassoConstants.a[4] = 4;
			}
			for (int i = 0; i < data.length; i++) {
				double fullval = 1.0;
				double curval = 0.0;
				int we;

				int[] ressum = new int[dim];
				for (int p = 1; p < dim; p++)
					ressum[p] += ressum[p - 1] + r[p - 1];
				// find the area represented by this point by multiplying its
				// length in each dimension
				if (dim == 1) {
					if (i != 0 && i != r[0] - 1) {
						curval = (selvals[i + 1] - selvals[i - 1]) / 2;
					} else if (i == 0) {
						curval = selvals[0] + (selvals[1] - selvals[0]) / 2;
					} else // if(k==getResolution()-1)
					{
						curval = EndPoint[0] * 100 - selvals[r[0] - 1] + (selvals[r[0] - 1] - selvals[r[0] - 2]) / 2;
					}
					fullval = curval;
				} else if (dim == 2 || dim == 3 || dim == 4 || dim == 5) {
					for (we = 0; we < dim; we++) {
						if (idx[we] != 0 && idx[we] != r[PicassoConstants.a[we]] - 1) // for in between first and last
						{
							//							 System.out.print("\n(");
							//							 System.out.print(selvals[ressum[PicassoConstants.a[we]]+ idx[we]+1]);
							//							 System.out.print("\t-\t");
							//							 System.out.print(selvals[ressum[PicassoConstants.a[we]]+ idx[we]-1]);
							//							 System.out.print(")/2");
							curval = (selvals[ressum[PicassoConstants.a[we]] + idx[we] + 1] - selvals[ressum[PicassoConstants.a[we]] + idx[we] - 1]) / 2;
						} else if (idx[we] == 0) // for first in row
						{
							//							 System.out.print("\n(");
							//							 System.out.print(selvals[ressum[PicassoConstants.a[we]]+ 1 ]);
							//							 System.out.print("\t-\t");
							//							 System.out.print(selvals[ressum[PicassoConstants.a[we]]]);
							//							 System.out.print(")/2");
							curval = /* getQueryPacket().getStartPoint(we)*100+ */(selvals[ressum[PicassoConstants.a[we]] + 1] - selvals[ressum[PicassoConstants.a[we]]]) / 2;
						} else // if(idx[we]==getResolution()-1) // for last in row
						{
							//							 System.out.print("\n(");
							//							 System.out.print(EndPoint[PicassoConstants.a[we]]*100);
							//							 System.out.print("\t-\t");
							//							 System.out.print(selvals[ressum[PicassoConstants.a[we]]+r[PicassoConstants.a[we]]-1]);
							//							 System.out.print(")");
							//							 System.out.print("\t+\t");
							//							 System.out.print("(");
							//							 System.out.print(selvals[ressum[PicassoConstants.a[we]]+r[PicassoConstants.a[we]]-1]);
							//							 System.out.print("\t-\t");
							//							 System.out.print(selvals[ressum[PicassoConstants.a[we]]+
							//							 r[PicassoConstants.a[we]]-2]);
							//							 System.out.print(")/2");
							curval = EndPoint[PicassoConstants.a[we]] * 100 - selvals[ressum[PicassoConstants.a[we]]+ r[PicassoConstants.a[we]] - 1] + (selvals[ressum[PicassoConstants.a[we]]
									+ r[PicassoConstants.a[we]] - 1] - selvals[ressum[PicassoConstants.a[we]] + r[PicassoConstants.a[we]] - 2]) / 2;
						}
						if (!scaleupflag)
							fullval *= curval;
						else
							fullval *= (curval * 10);
					}

					for (int p = 0; p < dim; p++) {
						idx[p]++;
						if (idx[p] == r[PicassoConstants.a[p]]) {
							idx[p] = 0;
						} else
							break;
					}
				} else {
					// start ma
					// modifying selvals as required

					int dim1 = PicassoConstants.a[0];
					int dim2 = PicassoConstants.a[1];
					int res1 = r[dim1];
					int res2 = r[dim2];

					int res[] = new int[dim];
					double startpt[] = new double[dim];
					double endpt[] = new double[dim];
					for (int k = 0; k < r.length; k++) {
						res[k] = r[k];
						startpt[k] = StartPoint[k];
						endpt[k] = EndPoint[k];
					}

					// swapping resolution and startpoint, endpoint locally for use in the area calculation
					int t = res[dim1];
					res[dim1] = res[dim2];
					res[dim2] = t;

					double x = startpt[dim1];
					startpt[dim1] = startpt[dim2];
					startpt[dim2] = x;

					x = endpt[dim1];
					endpt[dim1] = endpt[dim2];
					endpt[dim2] = x;

					float[] tvals = picsel;
					float temp1[] = new float[res1];
					float temp2[] = new float[res2];
					int index = 0;

					for (int k = 0; k < res1; k++) {
						temp1[k] = selvals[ressum[dim1] + k];
					}
					for (int k = 0; k < res2; k++) {
						temp2[k] = selvals[ressum[dim2] + k];
					}

					for (int k = 0; k < dim; k++) {
						if (inA(k)) {
							if (dim1 == k) {
								for (int j = 0; j < temp1.length; j++)
									tvals[index++] = temp1[j];
							} else if (dim2 == k) {
								for (int j = 0; j < temp2.length; j++)
									tvals[index++] = temp2[j];
							}
						} else {
							for (int j = 0; j < r[k]; j++)
								tvals[index++] = picsel[ressum[k] + j];
						}
					}
					selvals = tvals;

					for (we = 0; we < dim; we++) {
						if (idx[we] != 0 && idx[we] != res[we] - 1) {
							curval = (selvals[idx[we] + 1] - selvals[idx[we] - 1]) / 2;
						} else if (idx[we] == 0) {
							curval = startpt[we] * 100
									+ (selvals[1] - selvals[0]) / 2;
						} else // if(idx[we]==getResolution()-1)
						{
							curval = endpt[we]
									* 100
									- selvals[res[we] - 1]
											+ (selvals[res[we] - 1] - selvals[res[we] - 2])
											/ 2;
						}

						if (scaleupflag)
							fullval *= (curval * 10);
						else
							fullval *= curval;
					}

					for (int p = 0; p < dim; p++) {
						idx[p]++;
						if (idx[p] == res[p]) {
							idx[p] = 0;
						} else
							break;
					}
				}
				// System.out.println("Weight given for " + i + "(" + (idx[0]) + ", " + idx[1] + ")"+ " : " + twoDForm.format(1.0 * fullval));
				planCount[data[i].getPlanNumber()] += (fullval * 100);

				locationWeight[i] = (float) (fullval * 100);
			} // end of for loop (through all points)
		} // end of else (exponential) part

		for (int i = 0; i < data.length; i++) {
			if (planCount[data[i].getPlanNumber()] == 0)
				planCount[data[i].getPlanNumber()] = 1;
		}

		/*
		 * if(scaleupflag) { for(int i = 0; i < planCount.length; i++)
		 * planCount[i] /= 100Math.pow(10, getDimension()); }
		 */


		checkValidityofWeights();

	}
	
	
	// functions for location weight calculations and the checking for validity

	//////////////////////////////   LOCATION WEIGHT CALCULATIONS and PCM & POSP VALIDITY CHECKING CODE  //////////////////////////////////////////////////////////////
		
	
	

///////////////////// UTILITY CODE ////////////////////////////////
	
	public static void rewriteFilesGNUCompatiable(int index, boolean skip, boolean ret) throws IOException {
		if(skip)     return;
		
		//		int n = 22, length = 900;
		//		AllPlanCosts = new double[n][length];
		//
		//		//read all plans costs from pcst files
		//		for (int i = 0; i < n; i++){
		//			try {
		//				ObjectInputStream ip = new ObjectInputStream(new FileInputStream(new File(qname[index] +"/" + i + ".pcst")));
		//				double[] cost = (double[])ip.readObject();
		//				for(int j = 0; j < length; j++) {
		//					AllPlanCosts[i][j] = cost[j];
		//				}
		//			} 
		//			catch(Exception e){
		//				e.printStackTrace();
		//			}
		//		}

		//write costs in respective files in .txt format
		for (int i = 0; i < nPlans; i++){

			try{
				PrintWriter pw = new PrintWriter(new File(qname[index] +"/" + i + ".txt"));
				//				System.out.println("\tPoint = " + AllPlanCosts[0][0]);
				for(int j=0;j<data.length;j++){

					//int x = j % 100;

					//int y = j / 100;

					//pw.println( x + " " + y + " " + AllPlanCosts[i][j]);
					pw.println( AllPlanCosts[i][j]);
				}
				pw.close();
			}
			catch(Exception e){
				System.out.println("Error while saving gnu-plot compatiable file");
				e.printStackTrace();
			}
		}
		
		if(ret)  System.exit(0);
	}

	
		
	private static ADiagramPacket getGDP(File file) {
		ADiagramPacket gdp = null;
		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(fis);
			Object obj = ois.readObject();
			gdp = (ADiagramPacket) obj;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return gdp;
	}


	///////////////////// UTILITY CODE ////////////////////////////////

	

	
	
	
	

	
	
	////////////////////////// LITESEER REDUCTION CODE //////////////////////////////////////
	
	static boolean showLiteSeerDetails = false;
	static boolean verifySafetyOfLiteSeer = false;
	static int[] SEERplans = null;
	
	public static double Cost(int planNum, int ptIndex) {

		int pOptimal = data[ptIndex].getPlanNumber();
		double cOptimal = AllPlanCosts[pOptimal][ptIndex];
		double cPlan = AllPlanCosts[planNum][ptIndex];

		if (cPlan < cOptimal - 0.1 && LOGICAL_COSTS) {
			cPlan = cOptimal + 0.01;
		} // make costs logically ordered

		return cPlan;

		// return AllPlanCosts[planNum][ptIndex];
	}


	public static ADiagramPacket liteSeer_genericBenefit(double threshold, ADiagramPacket gdp) {

		long start = System.currentTimeMillis();
		ADiagramPacket ngdp = new ADiagramPacket(gdp);
		int n = gdp.getMaxPlanNumber();

		HashSet[] s = new HashSet[n];
		Integer[] x = new Integer[n];
		boolean[] notSwallowed = new boolean[n];
		DataValues[] data = gdp.getData();
		DataValues[] newData = new DataValues[data.length];
		boolean[][] swallow = new boolean[n][n];
		int res = gdp.getMaxResolution();
		sel = gdp.getPicassoSelectivity();
		int d = gdp.getDimension();

		int[] r = new int[d];
		for (int i = 0; i < d; i++)
			r[i] = res;

		double benefit[][] = new double[n][n];                   // to store the benefit of plan i replacing plan j

		int noPts = (int) Math.pow(2, d);
		int corners[] = new int[noPts];
		double cornerCosts[][] = new double[n][noPts];



		for (int i = 0; i < noPts; i++) {
			int[] in = new int[d];
			int xx = i;
			for (int j = 0; j < d; j++) {
				in[j] = xx & 1;
				xx >>= 1;
			in[j] *= (r[j] - 1);
			}
			int c = 0;
			for (int j = d - 1; j >= 0; j--) {
				c = c * r[j] + in[j];
			}
			//points.add(new Integer(c));
			corners[i] = c;                       // this has the locations of the corners of the space
		}


		//		int noPts = 4;
		//		int corners[] = new int[noPts];
		//		double cornerCosts[][] = new double[n][noPts];
		//		corners[0] = 62 ;
		//		corners[1] = 26162;
		//		corners[2] = 783062;
		//		corners[3] = 809162;



		for (int i = 0; i < n; i++) {
			s[i] = new HashSet();
			x[i] = new Integer(i);
			s[i].add(x[i]);

			for (int xx = 0; xx < noPts; xx++) {
				cornerCosts[i][xx] = Cost(i,corners[xx]);          // this has costs of the corners of the space
			}	
		}


		int[] area = new int[n];
		for(int i = 0;i < data.length;i++)
			area[data[i].getPlanNumber()]++;

		double[] stability = new double[n];
		for(int i = 0;i < n;i++){
			stability[i] = 0.0;

			//			System.out.println("Plan " + i + " has cornerCosts = " );
			for(int j = 0;j < noPts;j++){
				stability[i] += cornerCosts[i][j];
				//				System.out.print("\t" + cornerCosts[i][j] );
			}
			//			System.out.println("\t\t " + "Plan-" + i +" Sum = " + stability[i] );
		}




		double th = 1 + threshold / 100;
		for (int i = 0; i < n; i++) {
			boolean flag = false;
			for (int j = 0; j < n; j++) {

				//calculate benefit
				for(int pt = 0; pt < noPts; pt++){
					if(cornerCosts[i][pt] < cornerCosts[j][pt])
						benefit[i][j] += (cornerCosts[j][pt] - cornerCosts[i][pt]);
				}
				//calculate benefit

				// j -> swallower & i -> swallowee
				if (i != j) {
					boolean sw = true;
					for (int xx = 0; xx < noPts; xx++) {
						double ccc = cornerCosts[j][xx] - cornerCosts[i][xx];
						double cc = cornerCosts[j][xx] - th * cornerCosts[i][xx];
						if (cc > SANITY_CONSTANT) {
							sw = false;
						}
					}
					if (sw) {
						//						if(inEstimatedPOSP[j] == 1)
						{
							s[j].add(x[i]);

							flag = true;
							swallow[j][i] = true;
						}
					}
				}
			}
			if (!flag) {
				notSwallowed[i] = true;
			}
		}

		//		ArrayList soln = new ArrayList();
		//		HashSet temp = new HashSet();

		int point = 0,decisionsMade = 0, correctDecisionsMade = 0;  // to measure/verify LiteSeer Accuracy

		if(planSetDetailsSEER)
			System.out.print("\n\nIn liteseer\n\n");

		for(int i=0;i<n;i++){
			int count = 0;

			count = 0;
			if(showLiteSeerDetails) System.out.print("\nPlan-" + i + " (" + planRelativeArea[i] + ") \nSwallows : ");
			for(int j=0;j<n;j++){
				if(s[i].contains(x[j])){
					if(showLiteSeerDetails)	System.out.print(" " + j);
					count++;

					// to measure/verify LiteSeer Accuracy
					if(verifySafetyOfLiteSeer == true){
						for(point = 0; point < data.length; point++){
							if(AllPlanCosts[i][point] > (th) * AllPlanCosts[j][point])
								break;
						}
						decisionsMade++;
						if(point == data.length)	correctDecisionsMade++;
					}
					// to measure/verify LiteSeer Accuracy

				}
			}
			if(showLiteSeerDetails)	System.out.print("\nSwallows = " + count + " plans");


			count = 0;
			if(showLiteSeerDetails)	System.out.print("\nSwallowed by :");
			for(int j=0;j<n;j++){
				if(swallow[j][i] == true) {
					if(showLiteSeerDetails)	System.out.print(" " + j);
					count++;
				}
			}
			if(showLiteSeerDetails)	System.out.print("\nSwallowed by = " + count + " plans");
		}



		if(verifySafetyOfLiteSeer == true){
			System.out.println("Correct Decisions made by LiteSeer = " + ((100.0 * correctDecisionsMade)/decisionsMade));
			return ngdp;
		}


		SEERplans = new int[n];         // fill the replacement plan in this array
		double[] benefitIndex = new double[n];
		int[] soln = new int[n];
		int previousReplacer = -1;



		// filling according to benefit

		for(int i=0; i<n ; i++)			soln[i] = 0;    // it will be soln if it replaces more than 0


		for(int i=0; i<n; i++) {
			if(notSwallowed[i] == true) {
				SEERplans[i] = i;
				benefitIndex[i] = 1.0;
				soln[i] += 1;
				continue;
			}
			else {
				benefitIndex[i] = Double.MIN_VALUE;	
			}


			previousReplacer = -1;

			for(int j=0; j<n; j++) {
				if(swallow[j][i] == true || j==i) {
					double num = 0.0, denom = 0.0, curBenefit = 0.0;

					for(int k=0; k<noPts; k++) {
						num += cornerCosts[i][k];
						//denom += Math.min( Cost(i, PtIndex(selpt[k], r, d)), Cost(j, PtIndex(selpt[k], r, d)) );
						denom += cornerCosts[j][k] ;
					} 
					curBenefit = num/denom;

					//if(previousReplacer == -1 || (curBenefit > 1.001 || (benefit[j][i] > benefit[previousReplacer][i]))) {
					if(benefitIndex[i] < curBenefit) {
						benefitIndex[i] = curBenefit;
						SEERplans[i] = j;

						if(previousReplacer == -1) {
							previousReplacer = j;
							soln[j] += 1;	
						}
						else {
							soln[previousReplacer] -= 1;
							previousReplacer = j;
							soln[j] += 1;

						}
					}
				}
			}
		}




		//		System.out.print("\n");
		//		for(int i=0; i<data.length; i++) {
		//			if(soln.contains(new Integer(data[i].getPlanNumber())))
		//				System.out.print("(" + i % res + ", " + i/res + ")-" + data[i].getPlanNumber()+ "  ");
		//				
		//		}

		if(planSetDetailsSEER)
			System.out.print("\n The Solution is - \n");

		for(int i=0; i<n; i++){
			if(soln[i] >= 1) {
				//System.out.print("\t" + i + "\t" + plan[i] + "\t" + soln[i] + "\t" + stability[i] + "\t" + stability[plan[i]] + "\n");
				if(planSetDetailsSEER)
					System.out.print("\t" + i + "\t" + soln[i] + "\t" +planStrings[i] +"\n");
				countLSEERPlans++;
			}	
		}

		SEERplanSet = new int[countLSEERPlans];
		int count = 0;
		for(int i=0; i<n; i++){
			if(soln[i] >= 1) {
				SEERplanSet[count++] = i;
			}
		}

		if(planSetDetailsSEER)
			System.out.print("\n Total plans - " + countLSEERPlans + "\n");


		ngdp.setMaxPlanNumber(count);

		for (int i = 0; i < data.length; i++) {
			int pi = data[i].getPlanNumber();
			newData[i] = new DataValues();
			newData[i].setCard(data[i].getCard());
			newData[i].setPlanNumber(SEERplans[pi]);
		}
		ngdp.setDataPoints(newData);
		return ngdp;
	}

	////////////////////////// LITESEER REDUCTION CODE //////////////////////////////////////

	

	
}


