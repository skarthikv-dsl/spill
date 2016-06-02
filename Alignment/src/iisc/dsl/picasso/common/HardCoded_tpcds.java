package iisc.dsl.picasso.common;

import java.util.HashMap;

public class HardCoded_tpcds {
	public static String queryTemp = "Q17";
	public final static boolean allIndex = false;  // Change if not AllIndex
	public static String serfCalPath = "C:\\Users\\dsladmin.Malhar\\Desktop\\SERFCalculation-tpcds\\";  // Change if not AllIndex
	public static String RQEP_expand;
	public static String allIndexString = "-AI";
	// calculateSERF , UnionPlansPCSTFiles = true ; generatePCSTFiles= false;
	public final static boolean calculateSERF = false; // Uses pcst files. Sets GlobalPCST to true.
	public final static boolean UnionPlansPCSTFiles = false; // pcst files are there for union of plans.
	public final static boolean generatePCSTFiles = true;
	// genDiagrams or doRQEP_Reduction is true. Both cannot be true at one instance.
	public final static boolean doRQEP_Reduction = false;
	public final static boolean genDiagrams = !doRQEP_Reduction;
	public final static boolean readReducedDiagram = false;
	
	public static String[] oldIndexes = {"PK__NATION__2D27B809","PK__REGION__2C3393D0","PK__SUPPLIER__300424B4","PK__PART__2F10007B","PK__PARTSUPP__31EC6D26","PK__CUSTOMER__32E0915F","PK__ORDERS__35BCFE0A","PK__LINEITEM__34C8D9D1"};
	public static String[] newIndexes_Expand2 = {"PK__NATION_Expanded___7C1A6C5A","PK__REGION_Expanded___7B264821","PK__SUPPLIER_Expande__7EF6D905","PK__PART_Expanded_2__7E02B4CC","PK__PARTSUPP_Expande__00DF2177","PK__CUSTOMER_Expande__01D345B0","PK__ORDERS_Expanded___04AFB25B","PK__LINEITEM_Expande__03BB8E22"};
	public static String[] newIndexes_Expand3 = {"PK__NATION_Expanded___14E61A24","PK__REGION_Expanded___13F1F5EB","PK__SUPPLIER_Expande__17C286CF","PK__PART_Expanded_3__16CE6296","PK__PARTSUPP_Expande__19AACF41","PK__CUSTOMER_Expande__1A9EF37A","PK__ORDERS_Expanded___1D7B6025","PK__LINEITEM_Expande__1C873BEC"};
	public static String[] newIndexes_Expand4 = {"PK__NATION_Expanded___2DB1C7EE","PK__REGION_Expanded___2CBDA3B5","PK__SUPPLIER_Expande__308E3499","PK__PART_Expanded_4__2F9A1060","PK__PARTSUPP_Expande__32767D0B","PK__CUSTOMER_Expande__336AA144","PK__ORDERS_Expanded___36470DEF","PK__LINEITEM_Expande__3552E9B6"};
	public static String[] newIndexes_Expand5 = {"PK__NATION_Expanded___4B422AD5","PK__REGION_Expanded___4A4E069C","PK__SUPPLIER_Expande__4E1E9780","PK__PART_Expanded_5__4D2A7347","PK__PARTSUPP_Expande__5006DFF2","PK__CUSTOMER_Expande__50FB042B","PK__ORDERS_Expanded___53D770D6","PK__LINEITEM_Expande__52E34C9D"};
	public static String[] newIndexes_Expand6 = {"PK__NATION_Expanded___640DD89F","PK__REGION_Expanded___6319B466","PK__SUPPLIER_Expande__66EA454A","PK__PART_Expanded_6__65F62111","PK__PARTSUPP_Expande__68D28DBC","PK__CUSTOMER_Expande__69C6B1F5","PK__ORDERS_Expanded___6D9742D9","PK__LINEITEM_Expande__6CA31EA0"};

	public static String[] oldTableNames = {"[NATION]","[REGION]","[SUPPLIER]","[PART]","[PARTSUPP]","[CUSTOMER]","[ORDERS]","[LINEITEM]"};
	
	
	//public static String[] QTDValues = {"QT2","QT5","QT8","QT9","QT10","QT16","QT5_3D","QT8_3D","QT9_3D"};
	public static String[] QTDValues = {"Q17"};  // DO NOT CHANGE THIS ORDER. allQueryTemplates and AllQueryTemplates are dependent on this order.
	public static String[] expandValues = {"_js","_js_0","_js_1"};
	public static String[][] allQueryTemplates = {
			{
				"select s_acctbal, s_name, n_name, p_partkey, p_mfgr, s_address, s_phone, s_comment from part, supplier, partsupp, nation, region where p_partkey = ps_partkey and s_suppkey = ps_suppkey and p_retailprice :varies and s_nationkey = n_nationkey and n_regionkey = r_regionkey and r_name = 'EUROPE' and ps_supplycost <= ( select min(ps_supplycost) from partsupp, supplier, nation, region where p_partkey = ps_partkey and s_suppkey = ps_suppkey and s_nationkey = n_nationkey and n_regionkey = r_regionkey and r_name = 'EUROPE' and ps_supplycost :varies ) order by s_acctbal desc, n_name, s_name, p_partkey",
				"select s_acctbal, s_name, n_name, p_partkey, p_mfgr, s_address, s_phone, s_comment from part_Expanded_2, supplier_Expanded_2, partsupp_Expanded_2, nation_Expanded_2, region_Expanded_2 where p_partkey = ps_partkey and s_suppkey = ps_suppkey and p_retailprice :varies and s_nationkey = n_nationkey and n_regionkey = r_regionkey and r_name = 'EUROPE' and ps_supplycost <= ( select min(ps_supplycost) from partsupp_Expanded_2, supplier_Expanded_2, nation_Expanded_2, region_Expanded_2 where p_partkey = ps_partkey and s_suppkey = ps_suppkey and s_nationkey = n_nationkey and n_regionkey = r_regionkey and r_name = 'EUROPE' and ps_supplycost :varies ) order by s_acctbal desc, n_name, s_name, p_partkey",
				"select s_acctbal, s_name, n_name, p_partkey, p_mfgr, s_address, s_phone, s_comment from part_Expanded_3, supplier_Expanded_3, partsupp_Expanded_3, nation_Expanded_3, region_Expanded_3 where p_partkey = ps_partkey and s_suppkey = ps_suppkey and p_retailprice :varies and s_nationkey = n_nationkey and n_regionkey = r_regionkey and r_name = 'EUROPE' and ps_supplycost <= ( select min(ps_supplycost) from partsupp_Expanded_3, supplier_Expanded_3, nation_Expanded_3, region_Expanded_3 where p_partkey = ps_partkey and s_suppkey = ps_suppkey and s_nationkey = n_nationkey and n_regionkey = r_regionkey and r_name = 'EUROPE' and ps_supplycost :varies ) order by s_acctbal desc, n_name, s_name, p_partkey"
				
			},
			};
	public final static HashMap<String, String[]> AllQueryTemplates = new HashMap();
	static
	{
		AllQueryTemplates.put("Q17", allQueryTemplates[0]);
		
	}
	
	
}
