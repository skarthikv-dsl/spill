
/*
 # 
 # 
 # PROGRAM INFORMATION
 # 
 # 
 # Copyright (C) 2006 Indian Institute of Science, Bangalore, India.
 # All rights reserved.
 # 
 # This program is part of the Picasso Database Query Optimizer Visualizer
 # software distribution invented at the Database Systems Lab, Indian
 # Institute of Science (PI: Prof. Jayant R. Haritsa). The software is
 # free and its use is governed by the licensing agreement set up between
 # the copyright owner, Indian Institute of Science, and the licensee.
 # The software is distributed without any warranty; without even the
 # implied warranty of merchantability or fitness for a particular purpose.
 # The software includes external code modules, whose use is governed by
 # their own licensing conditions, which can be found in the Licenses file
 # of the Docs directory of the distribution.
 # 
 # 
 # The official project web-site is
 #     http://dsl.serc.iisc.ernet.in/projects/PICASSO/picasso.html
 # and the email contact address is 
 #     picasso@dsl.serc.iisc.ernet.in
 # 
 #
*/

package iisc.dsl.picasso.server.plan;

import java.util.Vector;
import java.util.ListIterator;

import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.common.HardCoded;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.TreeNode;

import java.io.Serializable;
import java.sql.Statement;
import java.sql.SQLException;

public class Node implements Serializable {
	private static final long serialVersionUID = 223L;
	private int 	id, parentId, type;
	private String 	name;
	private double 	cost, card;
	private String predicate;
	private Vector argType, argValue;
	
	public Node()
	{
		argType = new Vector();
		argValue = new Vector();
	}
	public int getId()
	{
		return id;
	}
	public void setId(int id)
	{
		this.id = id;
	}
	
	public int getParentId()
	{
		return parentId;
	}
	public void setParentId(int id)
	{
		this.parentId = id;
	}
	
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		if(name != null)
			this.name = name.trim();
		else
			this.name = "";
	}
	public String getPredicate()
	{
		return predicate;
	}
	public void setPredicate(String pred)
	{
		if(pred != null)
			this.predicate = pred.trim();
		else
			this.predicate = "";
	}
	public double getCost()
	{
		return cost;
	}
	public void setCost(double cost)
	{
		this.cost = cost;
	}
	
	public double getCard()
	{
		return card;
	}
	public void setCard(double card)
	{
		this.card = card;
	}

	public void populateTreeNode(TreeNode node)
	{
		node.setNodeValues(name, type, cost, cost, card, argType, argValue);
	}
	
	public long computeHash(String planDiffLevel)
	{
		long hash = 0,i=1;
		if(name != null)
			hash +=	name.hashCode();
		if(planDiffLevel.equals(PicassoConstants.SUBOPERATORLEVEL)){
			ListIterator it = argValue.listIterator();
			
			while(it.hasNext())
				hash += i++ * ((String)it.next()).hashCode();
		}
		hash = id*(parentId+1)*hash;
		return hash;
	}
	/*
	 * Function has been modified - by Nila for RQEP
	 */
	public long computeHash(String planDiffLevel, int nodeVal)
	{
		long hash = 0,i=1;
		if(name != null)
		{
			//if(HardCoded.RQEP_expand != null  && name.endsWith(HardCoded.RQEP_expand))
			if(name.contains("_Expanded_"))
			{
				String temp = name.substring(0,name.indexOf("_Expanded_"));
				//System.out.println(name+" :: "+temp);
				hash +=	temp.hashCode();
			}
			else
				hash +=	name.hashCode();
		}
		if(planDiffLevel.equals(PicassoConstants.SUBOPERATORLEVEL)){
			ListIterator it = argValue.listIterator();
			while(it.hasNext()){
			    String nodename = (String)it.next();
			    if(nodename.contains("_Expanded_"))
				{
			    	String nodename_bck = new String(nodename);
			    	int r;
			    	/**
			    	 * Replace the index Name first in the loop.
			    	 * After the loop (once index names are replaced), replace tablename by excluding the word '_Expanded_#'.
			    	 * Order should be important. Otherwise will get wrong replacements. 
			    	 */
					for(r=0;r<HardCoded.oldIndexes.length;r++)
					{
						//if(HardCoded.RQEP_expand.equals("_Expanded_2"))
							nodename = nodename.replace(HardCoded.newIndexes_Expand2[r],HardCoded.oldIndexes[r]);
						//else if(HardCoded.RQEP_expand.equals("_Expanded_3"))
							nodename = nodename.replace(HardCoded.newIndexes_Expand3[r],HardCoded.oldIndexes[r]);
						//else if(HardCoded.RQEP_expand.equals("_Expanded_4"))
							nodename = nodename.replace(HardCoded.newIndexes_Expand4[r],HardCoded.oldIndexes[r]);
						//else if(HardCoded.RQEP_expand.equals("_Expanded_5"))
							nodename = nodename.replace(HardCoded.newIndexes_Expand5[r],HardCoded.oldIndexes[r]);
						//else if(HardCoded.RQEP_expand.equals("_Expanded_6"))
							nodename = nodename.replace(HardCoded.newIndexes_Expand6[r],HardCoded.oldIndexes[r]);
						
						
						//replace(newTableNames[r],HardCoded.oldTableNames[r]);
					}
					// In case of Join Attribute, we will have two '_Expanded_#'
					while(nodename.contains("_Expanded_"))
					{
						nodename = nodename.substring(0,nodename.indexOf("_Expanded_")) + nodename.substring(nodename.indexOf("_Expanded_")+11);
					}
					//System.out.println(nodename_bck+" :: "+nodename);
					
				}
			    else if(nodename.contains("_js_"))  // RQEP - tpcds
				{
			    	System.out.print(nodename+" :: ");
					while(nodename.contains("_js_"))
					{
						nodename = nodename.substring(0,nodename.indexOf("_js_")) + nodename.substring(nodename.indexOf("_js_")+5);
					}
					System.out.println(nodename);
					
				}
			    if(nodename.contains("_js_"))  // RQEP - tpcds - should not happen
			    	System.out.println("$$$$$$$$$$ WRONG THINGS  : "+nodename);
				hash += i++ * nodename.hashCode();
			}
		}
		hash = nodeVal*hash;
		return hash;
	}
	
	public void show()
	{
		String tmp = ""+id +"\t"+parentId+"\t'"+name+"'\t"+cost+"\t"+card+"\t'";
		ListIterator itt = argType.listIterator();
		ListIterator itv = argValue.listIterator();
		while(itt.hasNext() && itv.hasNext())
			tmp+= (String)itt.next()+"="+(String)itv.next()+",";
		ServerMessageUtil.SPrintToConsole(tmp);
	}
	
	private String escapeQuotes(String str)
	{
		return str.replaceAll("'","''");
	}
	
	void storeNode(Statement stmt,int qtid,int planno, String schema) throws SQLException
	{
            	stmt.executeUpdate("INSERT INTO "+schema+".PicassoPlanTree values("+qtid+","+planno+","+id+","+parentId+",'"+name+"',"+cost+
				","+card+")");
		ListIterator itt = argType.listIterator();
		ListIterator itv = argValue.listIterator();
		while(itt.hasNext() && itv.hasNext()){
			stmt.executeUpdate("INSERT INTO "+schema+".PicassoPlanTreeArgs values("+qtid+","+planno+","+id+",'"+
			(String)itt.next()+"','"+escapeQuotes((String)itv.next())+"')");
		}
	}
	public boolean isArgTypePresent(String arg)
	{
		return argType.contains(arg);
	}
	public void addArgType(String arg)
	{
		argType.add(arg);
	}
	public void addArgValue(String arg) 
	{
		argValue.add(arg);
	}
	public Vector getArgType()
	{
		return argType;
	}
	public Vector getArgValue()
	{
		return argValue;
	}
}
