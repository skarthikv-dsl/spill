import java.io.BufferedReader;
import java.io.File;
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
    //Note that rules for predicates is (predicate1<space>=<space>predicate2)
   	static String[] predicates = {"(catalog_sales.cs_sold_date_sk = date_dim.d_date_sk)","(customer.c_customer_sk = catalog_sales.cs_bill_customer_sk)","(customer_address.ca_address_sk = customer.c_current_addr_sk)"};
   	static String[] predicatesRev = {"(date_dim.d_date_sk = catalog_sales.cs_sold_date_sk)","(catalog_sales.cs_bill_customer_sk = customer.c_customer_sk)","(customer.c_current_addr_sk = customer_address.ca_address_sk)"};
//   	static String[] predicates = {"(orders.o_custkey = customer.c_custkey)","(lineitem.l_orderkey = orders.o_orderkey)"};
//   	static String[] predicatesRev = {"(customer.c_custkey = orders.o_custkey)","(orders.o_orderkey = lineitem.l_orderkey)"};   	
   		static String path = "/home/dsladmin/Srinivas/data/DSQT153DR10_E/";
//   	static String path = "/home/dsladmin/Srinivas/data/HQT102DR100_U_Page0/";
//   	static String[] relations = {"s","l","o","c","n1","n2"};
//   	static String[] relations = {"c","o","l","n"};
   		static String[] relations = {"cs","c","ca","d"};
   	static int numplans = 15;
   	public static HashMap<String, Integer> relationMap =  new HashMap<String, Integer>();
   	public static boolean FROM_CLAUSE = false;
   	
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
            String relStr1 = null,relStr2 = null;
            if(root.getPredicate().indexOf('.')>=2)
            	relStr1 = root.getPredicate().substring(root.getPredicate().indexOf('.')-2, root.getPredicate().indexOf('.'));
            String substr1 = root.getPredicate().substring(root.getPredicate().indexOf('.')+1, root.getPredicate().indexOf('_',root.getPredicate().indexOf('.')));
            String substreq = root.getPredicate().substring(root.getPredicate().indexOf('=')); //this is to choose the second occurence after '='
            String substr2 = substreq.substring(substreq.indexOf('.')+1, substreq.indexOf('_',substreq.indexOf('.')));
            if(substreq.indexOf('.')>=2)
            	relStr2 = substreq.substring(substreq.indexOf('.')-2, substreq.indexOf('.'));

            for(int rel =0;rel<relations.length;rel++){
            	if(relations[rel].equals(relStr1))
                    hashStrings.add(relStr1);
            	if(relations[rel].equals(relStr2))
                    hashStrings.add(relStr2);
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
        	//System.out.println("The spill_node is Id is "+spill_id);
        	assert(spill_id == -1 ): "cannot have this number";
        	
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
    
    public static void main(String[] args) throws NumberFormatException, IOException{
    	int[] elements = new int[]{5,7,2,1,4,6,8};
    	  // the number of plans
    	//String[] predicates = {"(lineitem.l_suppkey = supplier.s_suppkey)","(lineitem.l_orderkey = orders.o_orderkey)"};
    	//String[] predicatesRev = {"(supplier.s_suppkey = lineitem.l_suppkey)","(orders.o_orderkey = lineitem.l_orderkey)"};
    	
    	
    	//The following Five constants need to be hardcoded
       	//String[] predicates = {"(orders.o_custkey = customer.c_custkey)","(lineitem.l_orderkey = orders.o_orderkey)"};
    	//String[] predicatesRev = {"(customer.c_custkey = orders.o_custkey)","(orders.o_orderkey = lineitem.l_orderkey)"};
    	//String[] relations = {"c","o","l","n"};
    	//String path = "/home/dsladmin/Srinivas/data/HQT102DR100/";
	Properties prop = new Properties();
prop.load(new FileInputStream(new File("/home/dsladmin/Srinivas/data/settings/settings.conf")));
String BaseLocation = prop.getProperty("BaseLocation");
String QTName = prop.getProperty("QTName");
path = BaseLocation+QTName+"/";
numplans=new File(path+"planStructure").listFiles().length;
System.out.println("NUMPLANS ="+numplans+"\n");
    
	
        for(int i=0;i<numplans;i++){
        	//reading the plan
        	
            HashMap<Integer,Integer> parentMap = new HashMap<Integer,Integer>();
            HashMap<Integer,String> valueMap = new HashMap<Integer,String>();
            HashMap<Integer,ArrayList<Integer>> childrenMap = new HashMap<Integer,ArrayList<Integer>>();
            ArrayList<Integer> visited = new ArrayList<Integer>();
        	//File file = new File("/home/srinivas/Srinivas/data/HQT83D-OC-PL-SL_EXP2/planStructure/"+numPlans+".txt");
            //File file = new File("/home/dsladmin/Srinivas/data/writingPlans/"+"test.txt");
            if(i==33)
        		System.out.println("For testing");
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
        	BinaryTree tree;
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
