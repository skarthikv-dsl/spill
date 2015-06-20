import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
//import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.Properties;
import java.io.*;



class Vertex {

	private int id;
	private int parent_id;
	private String name;
	private String predicate;
	private int pipeline_id;
	public boolean blocking;


	public Vertex (int val,int parent_val, String nm, String pred){
		this.id = val;
		this.parent_id =  parent_val;
		this.name = nm;
		this.predicate = pred;
		this.pipeline_id = -1;
	}

	public int getId(){
		return id;
	}
	public void setId(int num){
		id = num;
		return;
	}
	public int getParentId(){
		return parent_id;
	}
	public void setParentId(int num){
		parent_id = num;
		return;
	}

	public int getPipelineId(){
		return pipeline_id;
	}
	public void setPipelineId(int num){
		pipeline_id = num;
		return;
	}

	public String getName(){
		return name;
	}
	public void setName(String nm){
		name = nm;
		return;
	}
	public String getPredicate(){
		return predicate;
	}
	public void setPredicate(String pred){
		predicate = pred;
		return;
	}

}



public class BinaryTree {

	Vertex root;
	BinaryTree left_child;
	BinaryTree right_child;
	BinaryTree parent;
	static HashMap<Integer,Integer> predicateMap = new HashMap<Integer,Integer>(); 

	public static Set<Pipeline> PIPELINES = new HashSet<Pipeline>();
	
	static String predicates [];
	static String predicatesRev [];
	static String path;
	static String relations [];
	
	//Note that rules for predicates is (predicate1<space>=<space>predicate2)
	//static String[] predicates = {"(store_sales.ss_hdemo_sk = household_demographics.hd_demo_sk)","(time_dim.t_time_sk = store_sales.ss_sold_time_sk)","(store_sales.ss_store_sk = store.s_store_sk)"};
	//static String[] predicatesRev = {"(household_demographics.hd_demo_sk = store_sales.ss_hdemo_sk)","(store_sales.ss_sold_time_sk = time_dim.t_time_sk)","(store.s_store_sk = store_sales.ss_store_sk)"};
	//   	static String[] predicates = {"(orders.o_custkey = customer.c_custkey)","(lineitem.l_orderkey = orders.o_orderkey)"};
	//   	static String[] predicatesRev = {"(customer.c_custkey = orders.o_custkey)","(orders.o_orderkey = lineitem.l_orderkey)"};   	
	//static String path = "/home/dsladmin/Srinivas/data/DSQT963DR30_E/";
	//   	static String path = "/home/dsladmin/Srinivas/data/HQT102DR100_U_Page0/";
	//   	static String[] relations = {"s","l","o","c","n1","n2"};
	//   	static String[] relations = {"c","o","l","n"};
	//static String[] relations = {"ss","hd","t","s"};
	static String[] rareRelationsNames ={"n1","n2","cd1","cd2"};
	static int numplans;
	public static HashMap<String, Integer> relationMap =  new HashMap<String, Integer>();
	public static boolean FROM_CLAUSE;

	public BinaryTree(){

	}
	public BinaryTree(Vertex root, BinaryTree left_child, BinaryTree right_child){
		this.root = root;
		this.left_child = left_child;
		this.right_child = right_child;
		this.parent = null;
		if(left_child!=null)
			left_child.parent = this;
		if(right_child!=null)
			right_child.parent = this;
	}

	/*
	   public BinaryTree insertElement(int element){
	   if (root==null)
	   return new BinaryTree(new Vertex(element, true), null, null);
	   else {
	   if (root.isLeaf()){
	   root.setLeaf(false);
	   if (element < root.getValue())
	   return new BinaryTree(root, new BinaryTree(new Vertex(element, true), null, null), null);
	   else
	   return new BinaryTree(root, null, new BinaryTree(new Vertex(element, true), null, null));
	   } else {
	   if (element < root.getValue())
	   if (left_child!=null)
	   return new BinaryTree(root, left_child.insertElement(element), right_child);
	   else
	   return new BinaryTree(root, new BinaryTree(new Vertex(element, true), null, null), right_child);

	   }
	   }
	   }*/

	public Vertex getRootVertex(){
		return root;
	}
	public BinaryTree getLeftChild(){
		return left_child;
	}

	public BinaryTree getParent(){
		return parent;
	}

	public BinaryTree getRightChild(){
		return right_child;
	}

	public void setLeftChild(BinaryTree tree){
		this.left_child = tree;
		if(tree!=null)
			tree.parent = this;
	}

	public void setRightChild(BinaryTree tree){
		this.right_child = tree;
		if(tree!=null)
			tree.parent = this;
	}

	/*public BinaryTree buildBinaryTree(int[] elements){
	  if (elements.length==0)
	  return null;
	  else{
	  BinaryTree tree = new BinaryTree(new Vertex(elements[0],1,"",""), left_child, right_child);
	//for (int i=1;i<elements.length;i++){

	tree.setLeftChild(new BinaryTree(new Vertex(elements[1],1,"",""), null, null));
	tree.setRightChild(new BinaryTree(new Vertex(elements[2],1,"",""), null, null));
	//}
	tree.getLeftChild().setLeftChild(new BinaryTree(new Vertex(elements[3],1,"",""), null, null));
	tree.getRightChild().setRightChild(new BinaryTree(new Vertex(elements[4],1,"",""), null, null));
	return tree;
	}
	}*/
	public void traversePreOrder(){
		if (root!=null)
			System.out.print(root.getId() + " ");
		if (left_child!=null)
			left_child.traversePreOrder();
		if (right_child!=null)
			right_child.traversePreOrder();
	}

	//this method is similar to traverseOrder which visits every node in the subtree
	public void getRelationNames(HashSet<String> hashStrings){
		if (root!=null && root.getPredicate()!=null){
			System.out.println(root.getPredicate() + " ");
			String relStr1 = null,relStr2 = null,relStr3 = null,relStr4 = null;
			if(root.getPredicate().indexOf('.')>=2)
				relStr1 = root.getPredicate().substring(root.getPredicate().indexOf('.')-2, root.getPredicate().indexOf('.'));
			if(root.getPredicate().indexOf('.')>=3)
				relStr3 = root.getPredicate().substring(root.getPredicate().indexOf('.')-3, root.getPredicate().indexOf('.'));
			String substr1 = root.getPredicate().substring(root.getPredicate().indexOf('.')+1, root.getPredicate().indexOf('_',root.getPredicate().indexOf('.')));
			String substreq = root.getPredicate().substring(root.getPredicate().indexOf('=')); //this is to choose the second occurence after '='
			String substr2 = substreq.substring(substreq.indexOf('.')+1, substreq.indexOf('_',substreq.indexOf('.')));
			if(substreq.indexOf('.')>=2)
				relStr2 = substreq.substring(substreq.indexOf('.')-2, substreq.indexOf('.'));
			if(substreq.indexOf('.')>=3)
				relStr4 = substreq.substring(substreq.indexOf('.')-3, substreq.indexOf('.'));
			for(int rel =0;rel<relations.length;rel++){
				if(relations[rel].equals(relStr1) && relStr1.matches("[0-9]$"))
					hashStrings.add(relStr1);
				if(relations[rel].equals(relStr2) && relStr2.matches("[0-9]$"))
					hashStrings.add(relStr2);
				if(relations[rel].equals(relStr3) && relStr3.matches("[0-9]$"))
					hashStrings.add(relStr3);
				if(relations[rel].equals(relStr4) && relStr4.matches("[0-9]$"))
					hashStrings.add(relStr4);
				if(relations[rel].equals(substr1))
					hashStrings.add(substr1);
				if(relations[rel].equals(substr2))
					hashStrings.add(substr2);
			}
		}
		if (left_child!=null)
			left_child.getRelationNames(hashStrings);
		if (right_child!=null)
			right_child.getRelationNames(hashStrings);
	}

	public BinaryTree getVertexById(int id){
		if (this.root!=null && this.root.getId()==id){
			//System.out.print(root.getId() + " ");
			return this;
		}
		else {
			BinaryTree  foundVertex =null;
			if(left_child!=null)
				foundVertex = left_child.getVertexById(id);
			if(foundVertex == null && right_child!=null) {
				foundVertex = right_child.getVertexById(id);
			}
			return foundVertex;
		}
	}
	public void BuildPredicateOrder(String[] predicates, String[] predicatesRev){
		if (root!=null){
			for(int i=0;i<predicates.length;i++){
				if(root.getPredicate()!=null && (root.getPredicate().contains(predicates[i]) || root.getPredicate().contains(predicatesRev[i])))
					predicateMap.put(root.getId(), i);
			}
		}
		if (left_child!=null)
			left_child.BuildPredicateOrder(predicates,predicatesRev);
		if (right_child!=null)
			right_child.BuildPredicateOrder(predicates,predicatesRev);
	}

	public void buildBinaryTree(int id,HashMap<Integer,Integer> parentMap, HashMap<Integer,String> valueMap, HashMap<Integer,ArrayList<Integer>> childrenMap, ArrayList<Integer> visited){
		BinaryTree ltree;

		//if(visited.co)
		if(childrenMap.get(id)!=null){
			for(int i =0;i < childrenMap.get(id).size();i++){
				int child_id = childrenMap.get(id).get(i);
				String[] tempStr= valueMap.get(child_id).split(",");
				if(tempStr[1].contains("null"))
					ltree = new BinaryTree(new Vertex(child_id,parentMap.get(child_id),tempStr[0].trim(),null), null, null);
				else
					ltree = new BinaryTree(new Vertex(child_id,parentMap.get(child_id),tempStr[0].trim(),tempStr[1].trim()), null, null);
				if(i==0){
					this.setLeftChild(ltree);
					ltree.buildBinaryTree(child_id, parentMap, valueMap, childrenMap, visited);
				}
				else{
					this.setRightChild(ltree);
					ltree.buildBinaryTree(child_id, parentMap, valueMap, childrenMap, visited);
				}


			}
		}

		return;

	}
	public void buildPipeline(Pipeline pipe, int node_id, int level,HashMap<Integer,ArrayList<Integer>> childrenMap){
		pipe.setLevel(level);
		PIPELINES.add(pipe);
		int childStart = 0;
		pipe.getNodeIds().isEmpty();
		Vertex node = getVertexById(node_id).getRootVertex();
		// break the pipeline at blocking operator
		if((node.getName().contains("Sort") || node.getName().contains("Aggregate")) && !pipe.getNodeIds().isEmpty()) {
			// new pipeline from this operator onwards
			buildPipeline(new Pipeline(), node_id, level+1,childrenMap);
			return;
		} else if(node.getName()!=null && node.getName().equals("Hash") && !pipe.getNodeIds().isEmpty()) {
			// new pipeline for build input
			//buildPipeline(new Pipeline(), childrenMap.get(node_id).get(0), 0,childrenMap);
			buildPipeline(new Pipeline(), node_id, level+1,childrenMap);
			//pipe.getNodeIds().add(node_id);
			// build input should not be part of this pipeline
			childStart = 1;
		}else if(childrenMap.get(node_id) == null || childrenMap.get(node_id).isEmpty()) {
			// not including this one as it represents base relation name, not operator
			pipe.getNodeIds().add(node_id);getVertexById(node_id).getRootVertex().setPipelineId(level);
			return;
		} else {
			pipe.getNodeIds().add(node_id);getVertexById(node_id).getRootVertex().setPipelineId(level);
		}

		for(int i=childStart; i<childrenMap.get(node_id).size(); i++) {
			buildPipeline(pipe, childrenMap.get(node_id).get(i), level, childrenMap);
		}

		// pipeline is complete now
		//		if(pipeline.getOperators().get(0).equals(tail)) {
		//			System.out.println(pipeline);
		//			count++;
		//			// add this to global set if not present already
		//		}

	}
	//TODO: Need to test this functionality
	public boolean isParent(BinaryTree t1, BinaryTree t2){
		//to check whether t1 is a parent of t2
		BinaryTree temp = t2;
		while(temp!=null){
			if(temp==t1)
				return true;
			temp =temp.getParent();
		}
		return false;
	}
	public int[] getSpillNode(int dimension, int planno) throws NumberFormatException, IOException{

		//predicateMap static variable needs to be empty
		predicateMap.clear();

		//Initial functions which are called to get the spill node number
		//       	String[] predicates = {"(lineitem.l_suppkey = supplier.s_suppkey)","(lineitem.l_orderkey = orders.o_orderkey)"};
		//    	String[] predicatesRev = {"(supplier.s_suppkey = lineitem.l_suppkey)","(orders.o_orderkey = lineitem.l_orderkey)"};

		//numPlans = 66;
		//for(int i=0;i<numPlans;i++){
		//reading the plan
		int returnValue [] = new int[2];
		HashMap<Integer,Integer> parentMap = new HashMap<Integer,Integer>();
		HashMap<Integer,String> valueMap = new HashMap<Integer,String>();
		HashMap<Integer,ArrayList<Integer>> childrenMap = new HashMap<Integer,ArrayList<Integer>>();
		ArrayList<Integer> visited = new ArrayList<Integer>();
	    Properties prop = new Properties();
        prop.load(new FileInputStream(new File("./src/Constants.properties")));
        path = prop.getProperty("apktPath");
        File file = new File(path+"planStructure/"+planno+".txt");
		
        String qtName = prop.getProperty("qtName");
        initialize(qtName);
        
    	int from_clause_int_val = Integer.parseInt(prop.getProperty("FROM_CLAUSE"));	
		if(from_clause_int_val == 1)
			FROM_CLAUSE = true;
		else
			FROM_CLAUSE = false;
		
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr); 
		String s;
		while(((s=br.readLine())!=null)){
			String temp[] = s.split(",");
			valueMap.put(Integer.parseInt(temp[0]),temp[2].trim()+","+temp[3].trim());
			parentMap.put(Integer.parseInt(temp[0]),Integer.parseInt(temp[1]));
			if(childrenMap.containsKey(Integer.parseInt(temp[1])))
				childrenMap.get(Integer.parseInt(temp[1])).add(Integer.parseInt(temp[0]));
			else{
				ArrayList<Integer> al = new ArrayList<Integer>();
				al.add(Integer.parseInt(temp[0]));
				childrenMap.put(Integer.parseInt(temp[1]), al);
			}

		}
		//BinaryTree tree;
		String[] tempStr= valueMap.get(0).split(",");
		//getRootVertex().setParentId(-1); already set in spill Node
		getRootVertex().setName(tempStr[0]);
		if(!tempStr[1].contains("null")){
			getRootVertex().setPredicate(tempStr[1]);
		}

		buildBinaryTree(0, parentMap, valueMap, childrenMap,visited);
		traversePreOrder();
		//System.out.println("Fourth verxtex is "+getVertexById(15).getRootVertex().getPredicate());

		BuildPredicateOrder(predicates,predicatesRev);
		assert(predicateMap.size() == predicates.length):"predicateMap length not matching";
			//System.out.println("The predicate map is "+predicateMap);
			//Predicate Map is a mapping from node_id to dimension
			int spill_id = -1;
			Iterator it = predicateMap.keySet().iterator();
			//get the node_id corresponding to the dimension in the plan tree
			while(it.hasNext()){
				Integer key = (Integer)it.next();
				if(predicateMap.get(key).intValue() == dimension){
					spill_id = key.intValue();
					break;
				}
			}
			System.out.println("The spill_node is Id is "+spill_id);
			assert(spill_id != -1 ): "cannot have this number";

				// determine the spill_node for postgres
				int spill_node = -1; 
				int temp = 0;
				BinaryTree t_spill = getVertexById(spill_id);
				//check what all relations
				HashSet<String> hashStrings = new HashSet<String>();
				t_spill.getRelationNames(hashStrings);
				for(int i=0;i<relations.length;i++){
					if(!FROM_CLAUSE)
						relationMap.put(relations[i], new Integer(i+1));
					else
						relationMap.put(relations[i], new Integer(i+2));
				}         	
				//System.out.println("Hash Strings are "+hashStrings);
				//System.out.println("The relationMap is "+relationMap);
				//build the spill_node number for this predicate
				for(String str : hashStrings){
					temp = temp * 10	+ relationMap.get(str).intValue()	;
				}
				//System.out.println("The spillnode id is "+temp);
				returnValue[0] = spill_id; //node id of the tree
				returnValue[1] = temp;
				return returnValue;

	}

	public void initialize(String qtName) {
		
		if(qtName.contains("DSQT74DR")){
			predicates = new String[]{"(customer_demographics.cd_demo_sk = store_sales.ss_cdemo_sk)","(store_sales.ss_sold_date_sk = date_dim.d_date_sk)","(item.i_item_sk = store_sales.ss_item_sk)","(store_sales.ss_promo_sk = promotion.p_promo_sk)"};
		    predicatesRev = new String[]{"(store_sales.ss_cdemo_sk = customer_demographics.cd_demo_sk)","(date_dim.d_date_sk = store_sales.ss_sold_date_sk)","(store_sales.ss_item_sk = item.i_item_sk)","(promotion.p_promo_sk = store_sales.ss_promo_sk)"};
		    relations = new String[]{"ss","cd","d","i","p"};
		}
		
		else if(qtName.contains("DSQT153DR")){
			predicates = new String[]{"(catalog_sales.cs_sold_date_sk = date_dim.d_date_sk)","(customer.c_customer_sk = catalog_sales.cs_bill_customer_sk)","(customer_address.ca_address_sk = customer.c_current_addr_sk)"};
		    predicatesRev = new String[]{"(date_dim.d_date_sk = catalog_sales.cs_sold_date_sk)","(catalog_sales.cs_bill_customer_sk = customer.c_customer_sk)","(customer.c_current_addr_sk = customer_address.ca_address_sk)"};
		    relations = new String[]{"cs","c","ca","d"};
		} 
		
		else if(qtName.contains("DSQT195DR")){
			predicates = new String[]{"(store_sales.ss_sold_date_sk = date_dim.d_date_sk)","(item.i_item_sk = store_sales.ss_item_sk)","(customer.c_customer_sk = store_sales.ss_customer_sk)","(store_sales.ss_store_sk = store.s_store_sk)","(customer_address.ca_address_sk = customer.c_current_addr_sk)"};
		    predicatesRev = new String[]{"(date_dim.d_date_sk = store_sales.ss_sold_date_sk)","(store_sales.ss_item_sk = item.i_item_sk)","(store_sales.ss_customer_sk = customer.c_customer_sk)","(store.s_store_sk = store_sales.ss_store_sk)","(customer.c_current_addr_sk = customer_address.ca_address_sk)"};
		    relations = new String[]{"ss","d","i","c","ca","s"};
		}
		
		else if(qtName.contains("DSQT264DR")){
			predicates = new String[]{"(customer_demographics.cd_demo_sk = catalog_sales.cs_bill_cdemo_sk)","(catalog_sales.cs_sold_date_sk = date_dim.d_date_sk)","(item.i_item_sk = catalog_sales.cs_item_sk)","(catalog_sales.cs_promo_sk = promotion.p_promo_sk)"};
		    predicatesRev = new String[]{"(catalog_sales.cs_bill_cdemo_sk = customer_demographics.cd_demo_sk)","(date_dim.d_date_sk = catalog_sales.cs_sold_date_sk)","(catalog_sales.cs_item_sk = item.i_item_sk)","(promotion.p_promo_sk = catalog_sales.cs_promo_sk)"};
		    relations = new String[]{"cs","cd","d","i","p"};
		} 
		
		else if(qtName.contains("DSQT262DR")){
			predicates = new String[]{"(customer_demographics.cd_demo_sk = catalog_sales.cs_bill_cdemo_sk)","(catalog_sales.cs_sold_date_sk = date_dim.d_date_sk)"};
		    predicatesRev = new String[]{"(catalog_sales.cs_bill_cdemo_sk = customer_demographics.cd_demo_sk)","(date_dim.d_date_sk = catalog_sales.cs_sold_date_sk)"};
		    relations = new String[]{"cs","cd","d","i","p"};
		}
		
		else if(qtName.contains("DSQT914DR")){
			predicates = new String[]{"(catalog_returns.cr_returned_date_sk = date_dim.d_date_sk)","(customer_address.ca_address_sk = customer.c_current_addr_sk)","(customer_demographics.cd_demo_sk = customer.c_current_cdemo_sk)","(household_demographics.hd_demo_sk = customer.c_current_hdemo_sk)"};
		    predicatesRev = new String[]{"(date_dim.d_date_sk = catalog_returns.cr_returned_date_sk)","(customer.c_current_addr_sk = customer_address.ca_address_sk)","(customer.c_current_cdemo_sk = customer_demographics.cd_demo_sk)","(customer.c_current_hdemo_sk = household_demographics.hd_demo_sk)"};
		    relations = new String[]{"cc","cr","d","c","ca","cd","hd"};
		} 
		
		else if(qtName.contains("DSQT916DR")){
			predicates = new String[]{"(catalog_returns.cr_call_center_sk = call_center.cc_call_center_sk)","(catalog_returns.cr_returned_date_sk = date_dim.d_date_sk)","(customer.c_customer_sk = catalog_returns.cr_returning_customer_sk)","(customer_address.ca_address_sk = customer.c_current_addr_sk)","(customer_demographics.cd_demo_sk = customer.c_current_cdemo_sk)","(household_demographics.hd_demo_sk = customer.c_current_hdemo_sk)"};
		    predicatesRev = new String[]{"(call_center.cc_call_center_sk = catalog_returns.cr_call_center_sk)","(date_dim.d_date_sk = catalog_returns.cr_returned_date_sk)","(catalog_returns.cr_returning_customer_sk = customer.c_customer_sk)","(customer.c_current_addr_sk = customer_address.ca_address_sk)","(customer.c_current_cdemo_sk = customer_demographics.cd_demo_sk)","(customer.c_current_hdemo_sk = household_demographics.hd_demo_sk)"};
		    relations = new String[]{"cc","cr","d","c","ca","cd","hd"};
		} 
		
		else if(qtName.contains("DSQT186DR")){
			predicates = new String[]{"(date_dim.d_date_sk = catalog_sales.cs_sold_date_sk)","(item.i_item_sk = catalog_sales.cs_item_sk)","(cd1.cd_demo_sk = catalog_sales.cs_bill_cdemo_sk)","(catalog_sales.cs_bill_customer_sk = customer.c_customer_sk)","(cd2.cd_demo_sk = customer.c_current_cdemo_sk)","(customer.c_current_addr_sk = customer_address.ca_address_sk)"};
		    predicatesRev = new String[]{"(catalog_sales.cs_sold_date_sk = date_dim.d_date_sk)","(catalog_sales.cs_item_sk = item.i_item_sk)","(catalog_sales.cs_bill_cdemo_sk = cd1.cd_demo_sk)","(customer.c_customer_sk = catalog_sales.cs_bill_customer_sk)","(customer.c_current_cdemo_sk = cd2.cd_demo_sk)","(customer_address.ca_address_sk = customer.c_current_addr_sk)"};
		    relations = new String[]{"cs","d","i","cd1","c","cd2","ca"};
		} 

		else if(qtName.contains("DSQT912DR")){
			predicates = new String[]{"(catalog_returns.cr_returned_date_sk = date_dim.d_date_sk)","(customer_address.ca_address_sk = customer.c_current_addr_sk)"};
		    predicatesRev = new String[]{"(date_dim.d_date_sk = catalog_returns.cr_returned_date_sk)","(customer.c_current_addr_sk = customer_address.ca_address_sk)"};
		    relations = new String[]{"cc","cr","d","c","ca","cd","hd"};
		} 
		
		else if(qtName.contains("DSQT963DR")){
			predicates = new String[]{"(store_sales.ss_hdemo_sk = household_demographics.hd_demo_sk)","(time_dim.t_time_sk = store_sales.ss_sold_time_sk)","(store_sales.ss_store_sk = store.s_store_sk)"};
		    predicatesRev = new String[]{"(household_demographics.hd_demo_sk = store_sales.ss_hdemo_sk)","(store_sales.ss_sold_time_sk = time_dim.t_time_sk)","(store.s_store_sk = store_sales.ss_store_sk)"};
		    relations = new String[]{"ss","hd","t","s"};
		} 
		
		else if(qtName.contains("DSQT962DR")){
			predicates = new String[]{"(store_sales.ss_hdemo_sk = household_demographics.hd_demo_sk)","(time_dim.t_time_sk = store_sales.ss_sold_time_sk)"};
		    predicatesRev = new String[]{"(household_demographics.hd_demo_sk = store_sales.ss_hdemo_sk)","(store_sales.ss_sold_time_sk = time_dim.t_time_sk)"};
		    relations = new String[]{"ss","hd","t","s"};
		} 
		
		else if(qtName.contains("HQT53DR")){
			predicates = new String[]{"(orders.o_custkey = customer.c_custkey)","(lineitem.l_orderkey = orders.o_orderkey)","(supplier.s_suppkey = lineitem.l_suppkey)"};
		    predicatesRev = new String[]{"(customer.c_custkey = orders.o_custkey)","(orders.o_orderkey = lineitem.l_orderkey)","(lineitem.l_suppkey = supplier.s_suppkey)"};
		    relations = new String[]{"c","o","l","s","n","r"};
		}
		
		
		else if(qtName.contains("HQT73DR")){
			predicates = new String[]{"(supplier.s_suppkey = lineitem.l_suppkey)","(lineitem.l_orderkey = orders.o_orderkey)","(orders.o_custkey = customer.c_custkey)"};
		    predicatesRev = new String[]{"(lineitem.l_suppkey = supplier.s_suppkey)","(orders.o_orderkey = lineitem.l_orderkey)","(customer.c_custkey = orders.o_custkey)"};
		    relations = new String[]{"s","l","o","c","n1","n2"};
		}
		
		else if(qtName.contains("HQT75DR")){
			predicates = new String[]{"(supplier.s_suppkey = lineitem.l_suppkey)","(lineitem.l_orderkey = orders.o_orderkey)","(orders.o_custkey = customer.c_custkey)","(supplier.s_nationkey = n1.n_nationkey)","(customer.c_nationkey = n2.n_nationkey)"};
		    predicatesRev = new String[]{"(lineitem.l_suppkey = supplier.s_suppkey)","(orders.o_orderkey = lineitem.l_orderkey)","(customer.c_custkey = orders.o_custkey)","(n1.n_nationkey = supplier.s_nationkey)","(n2.n_nationkey = customer.c_nationkey)"};
		    relations = new String[]{"s","l","o","c","n1","n2"};
		}
		
		else if(qtName.contains("HQT84DR")){
			predicates = new String[]{"(lineitem.l_partkey = part.p_partkey)","(supplier.s_suppkey = lineitem.l_suppkey)","(lineitem.l_orderkey = orders.o_orderkey)","(orders.o_custkey = customer.c_custkey)"};
		    predicatesRev = new String[]{"(part.p_partkey = lineitem.l_partkey)","(lineitem.l_suppkey = supplier.s_suppkey)","(orders.o_orderkey = lineitem.l_orderkey)","(customer.c_custkey = orders.o_custkey)"};
		    relations = new String[]{"p","s","l","o","c","n1","n2","r"};
		}
		
		else{
			assert(false) : "Initialize (Binary Tree)" +" :missing template";
		}
		
	}
	public static void main(String[] args) throws NumberFormatException, IOException{

		BinaryTree tree = new BinaryTree();
		Properties prop = new Properties();
		
		File f_iisc = new File("/home/dsladmin/Srinivas/data/settings/settings.conf");
		File f_ibm = new File("/home/ijcai/spillBound/data/settings/settings.conf");
		if(f_iisc.exists()){
			//Load the properties file.
			prop.load(new FileInputStream(f_iisc));
		}
		else if(f_ibm.exists()){
			prop.load(new FileInputStream(f_ibm));
		}
		else{
			System.out.println("Properties file not found");
			System.exit(0);
		}
		String BaseLocation = prop.getProperty("BaseLocation");
		String QTName = prop.getProperty("QTName");
		path = BaseLocation+QTName+"/";
		numplans=new File(path+"planStructure").listFiles().length;
		System.out.println("NUMPLANS ="+numplans+"\n");

		tree.initialize(QTName);
		for(int i=0;i<numplans;i++){
			//reading the plan

			HashMap<Integer,Integer> parentMap = new HashMap<Integer,Integer>();
			HashMap<Integer,String> valueMap = new HashMap<Integer,String>();
			HashMap<Integer,ArrayList<Integer>> childrenMap = new HashMap<Integer,ArrayList<Integer>>();
			ArrayList<Integer> visited = new ArrayList<Integer>();
			//File file = new File("/home/srinivas/Srinivas/data/HQT83D-OC-PL-SL_EXP2/planStructure/"+numPlans+".txt");
			//File file = new File("/home/dsladmin/Srinivas/data/writingPlans/"+"test.txt");
	
			File file = new File(path+"planStructure/"+i+".txt");
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr); 
			String s;
			while(((s=br.readLine())!=null)){
				String temp[] = s.split(",");
				valueMap.put(Integer.parseInt(temp[0]),temp[2].trim()+","+temp[3].trim());
				parentMap.put(Integer.parseInt(temp[0]),Integer.parseInt(temp[1]));
				if(childrenMap.containsKey(Integer.parseInt(temp[1])))
					childrenMap.get(Integer.parseInt(temp[1])).add(Integer.parseInt(temp[0]));
				else{
					ArrayList<Integer> al = new ArrayList<Integer>();
					al.add(Integer.parseInt(temp[0]));
					childrenMap.put(Integer.parseInt(temp[1]), al);
				}

			}
			
			String[] tempStr= valueMap.get(0).split(",");
			if(tempStr[1].contains("null"))
				tree = new BinaryTree(new Vertex(0,-1,tempStr[0],null),null,null);
			else
				tree = new BinaryTree(new Vertex(0,-1,tempStr[0],tempStr[1]),null,null);
			tree.buildBinaryTree(0, parentMap, valueMap, childrenMap,visited);
			tree.traversePreOrder();
			//System.out.println("Fourth verxtex is "+tree.getVertexById(15).getRootVertex().getPredicate());

			tree.BuildPredicateOrder(predicates,predicatesRev);
			assert(predicateMap.size() == predicates.length):"predicateMap length not matching";
				System.out.println("The predicate map is "+predicateMap);
				tree.buildPipeline(new Pipeline(),0,0,childrenMap);
				System.out.println("The number of pipelines are "+PIPELINES.size());
				for(Pipeline p : PIPELINES){
					p.print();
				}

				// sort the keys of the map
				Integer [] nodeids = new Integer[predicateMap.keySet().size()];
				Iterator it = predicateMap.keySet().iterator();
				int n = 0;
				//populate the nodeids 
				while(it.hasNext()){
					nodeids[n++] =  (Integer)it.next();
				}
				assert(n==predicateMap.keySet().size()) : "nodeIds data structure not populated properly";
					for(int j=0;j<nodeids.length;j++){
						for(int k=j+1; k<nodeids.length;k++){
							Vertex v_j = tree.getVertexById(nodeids[j]).getRootVertex();
							Vertex v_k = tree.getVertexById(nodeids[k]).getRootVertex();
							if((v_j.getPipelineId() < v_k.getPipelineId()) || tree.isParent(tree.getVertexById(nodeids[j]), tree.getVertexById(nodeids[k])) ){
								Integer temp = nodeids[j];
								nodeids[j] = nodeids[k];
								nodeids[k] = temp;
							}

						}
					}

					File filer = new File(path+"predicateOrder/"+i+".txt");
					FileWriter writer = new FileWriter(filer, false);  
					PrintWriter pw = new PrintWriter(writer);
					for(int j = 0; j<nodeids.length;j++)
						pw.println(predicateMap.get(nodeids[j]));
					pw.close();
					writer.close();
					//Clear all the static variables so that it is fresh for the next iteration
					predicateMap.clear();
		} //end for
	}
	}

	class Pipeline {
		private ArrayList<Integer> nodeIds;
		private int level;

		public Pipeline(){
			nodeIds = new ArrayList<Integer>();
		}

		public ArrayList<Integer> getNodeIds(){
			return nodeIds;
		}
		public void setNodeIds(ArrayList<Integer> al){
			nodeIds = al;
		}

		public int getLevel(){
			return level;
		}
		public void setLevel(int l){
			level = l;
		}
		public void print(){
			System.out.println("The pipeline level is "+level);
			for(int i=0;i<nodeIds.size();i++){
				System.out.print(nodeIds.get(i)+"\t");
			}
			System.out.println();
		}
	}
