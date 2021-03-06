/*
 # Copyright (C) 2005, 2006 Indian Institute of Science
 # Bangalore 560012, INDIA
 #
 # This program is part of the PICASSO distribution invented at the
 # Database Systems Lab, Indian Institute of Science. The use of
 # the software is governed by the licensing agreement set up between 
 # the owner, Indian Institute of Science, and the licensee.
 #
 # This program is distributed WITHOUT ANY WARRANTY; without even the 
 # implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 #
 # The public URL of the PICASSO project is
 # http://dsl.serc.iisc.ernet.in/projects/PICASSO/index.html
 #
 # For any issues, contact 
 #       Prof. Jayant R. Haritsa
 #       SERC
 #       Indian Institute of Science
 #       Bangalore 560012, India.
 #       Telephone: (+91) 80 2293-2793
 #       Fax      : (+91) 80 2360-2648
 #       Email: haritsa@dsl.serc.iisc.ernet.in
 #       WWW: http://dsl.serc.iisc.ernet.in/~haritsa
 */
package iisc.dsl.picasso.server.db.mysql;

import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.DBSettings;
import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.Histogram;
import iisc.dsl.picasso.server.plan.Node;
import iisc.dsl.picasso.server.plan.Plan;
import iisc.dsl.picasso.server.network.ServerMessageUtil;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MysqlDatabase extends Database 
{
	public MysqlDatabase(DBSettings settings) throws PicassoException
	{
		super(settings);
	}
	
	public boolean connect(DBSettings settings) 
	{
		String connectString;
		if(isConnected())
				return true;
		this.settings = settings;
		try{
			connectString = "jdbc:mysql://" + settings.getServerName() + ":" +	settings.getServerPort() + "/" + settings.getDbName();
			//Register the JDBC driver for MySQL
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			//Get a connection to the database
			con = DriverManager.getConnection (connectString, settings.getUserName(), settings.getPassword());
			}
		catch (Exception e)	{ 
			ServerMessageUtil.SPrintToConsole("Database: " + e);
			return false;
			}
		if(con != null) {
			if( !(settings.getOptLevel().trim().equalsIgnoreCase("Default"))) {
				try	{
					Statement stmt = createStatement();
					String optLevelQuery ="alter session set OPTIMIZER_MODE = "+settings.getOptLevel();
					stmt.execute(optLevelQuery);
					}
				catch(SQLException se) {
					ServerMessageUtil.SPrintToConsole("Database : Error setting the Optimization Level of Oracle : "+se);
					}
			}
			else {
				try	{
					Statement stmt = createStatement();
					String optLevelQuery ="alter session set OPTIMIZER_MODE = "+settings.getOptLevel();
					stmt.execute(optLevelQuery);
					}
				catch(SQLException se) {
					ServerMessageUtil.SPrintToConsole("Database : Error setting the Optimization Level of Oracle : "+se);
					}
			}
			return true;
		}
		return false;
	}
	
	public Histogram getHistogram(String tabName, String schema, String attribName) throws PicassoException
	{
		return new MysqlHistogram(this, tabName, schema, attribName);
	}
	
//	MySql server doesn't have plantables 
	public void emptyPlanTable(){ }
	public void removeFromPlanTable(int qno){ }
	
	public boolean checkPlanTable()
	{
		return true;
	}
	protected void createPicassoColumns(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create view "+settings.getSchema()+".picasso_columns as SELECT COLUMN_NAME, TABLE_NAME, TABLE_SCHEMA AS owner" +
		" FROM  INFORMATION_SCHEMA.COLUMNS");
	}
	
	protected void createRangeResMap(Statement stmt) throws SQLException //-ma
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoRangeResMap ( QTID int NOT NULL, DIMNUM int NOT NULL, RESOLUTION int NOT NULL, "+
				"PRIMARY KEY(QTID,DIMNUM), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID))");
	}
	
	protected void createQTIDMap(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".QTIDMap ( QTID int, QTEMPLATE longtext, " +
				"QTNAME varchar(" + PicassoConstants.MEDIUM_COLUMN + ") UNIQUE NOT NULL, RESOLUTION integer, DIMENSION integer, EXECTYPE varchar(" + PicassoConstants.SMALL_COLUMN + "), DISTRIBUTION varchar(" + PicassoConstants.SMALL_COLUMN + "), " +
		"OPTLEVEL varchar(" + PicassoConstants.QTNAME_LENGTH + "), PLANDIFFLEVEL varchar(" + PicassoConstants.SMALL_COLUMN + "), GENTIMG bigint, GENDURATION bigint, PRIMARY KEY (QTID))");
	}
	
	protected void createPlanTree(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PlanTree ( QTID int NOT NULL, PLANNO int NOT NULL, ID int NOT NULL, PARENTID int NOT NULL, "+
				"NAME varchar(" + PicassoConstants.MEDIUM_COLUMN + "), COST float, CARD float, PRIMARY KEY(QTID,PLANNO,ID,PARENTID), " +
		"FOREIGN KEY(QTID) REFERENCES QTIDMap(QTID) ON DELETE CASCADE )");
	}
	
	protected void createPlanTreeArgs(Statement stmt) throws SQLException 
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PlanTreeArgs ( QTID int NOT NULL, PLANNO int NOT NULL, ID int NOT NULL, "+
				"ARGNAME varchar(" + PicassoConstants.SMALL_COLUMN + ") NOT NULL, ARGVALUE varchar(" + PicassoConstants.SMALL_COLUMN + ") NOT NULL, PRIMARY KEY(QTID,PLANNO,ID,ARGNAME,ARGVALUE), " +
		"FOREIGN KEY(QTID) REFERENCES QTIDMap(QTID) ON DELETE CASCADE )");
	}
	
	protected void createXMLPlan(Statement stmt) throws SQLException
	{
	}
	
	protected void createPlanStore(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PlanStore ( QTID int NOT NULL, QID int NOT NULL, PLANNO int, COST float, CARD float, " +
		"RUNCOST float, RUNCARD float, PRIMARY KEY(QTID,QID), FOREIGN KEY(QTID) REFERENCES QTIDMap(QTID) ON DELETE CASCADE )");
	}
	
	protected void createSelectivityMap(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".SelectivityMap ( QTID int NOT NULL, QID int NOT NULL, DIMENSION int NOT NULL, " +
		"SID int NOT NULL, PRIMARY KEY(QTID,QID,DIMENSION), FOREIGN KEY(QTID) REFERENCES QTIDMap(QTID) ON DELETE CASCADE )");
	}
	
	protected void createSelectivityLog(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".SelectivityLog ( QTID int NOT NULL, DIMENSION int NOT NULL, SID int NOT NULL, " +
				"PICSEL float, PLANSEL float, PREDSEL float, DATASEL float, CONST varchar(" + PicassoConstants.SMALL_COLUMN + "), " +
		"PRIMARY KEY(QTID,DIMENSION,SID), FOREIGN KEY(QTID) REFERENCES QTIDMap(QTID) ON DELETE CASCADE )");
	}
	
	protected void createApproxSpecs(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoApproxMap ( QTID int NOT NULL, " +
				"SAMPLESIZE float, SAMPLINGMODE int, AREAERROR float, IDENTITYERROR float,FPCMODE int, " +
				"PRIMARY KEY(QTID), FOREIGN KEY(QTID) REFERENCES "+settings.getSchema()+".PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}
	public Plan getPlan(String query) throws PicassoException
	{
		Plan plan = new Plan();
		ResultSet rset,rset1,rset2;
		Statement stmt;
		Node node;
		double card=1,cost=0;
		String info=null;			//information for sorting
		String type_query_id2=null;	//information for what kind of subquery corresponding to id=2		
		String typescan_id1=null;	//information for what kind of scan in first row,needed for id=1
		
					
		/*declaration for primary query tree formation*/
		int curNode=1,count=0;
		int tempParent1=0,tempChild2=0,tempParent2=0,tempChild3=0,tempParent3=0;
		
		/*declaration for dependent subquery tree formation in id=2*/
		int count_id2a=0;
		int nodecount=0;			//to count the no of nodes in dependent subquery
		
		/*declaration for id=3*/
		String name_id3 = null;		//information for the name of table corresponding to id=3
		String scan_id3 = null;		//information for the type of access corresponding to id=3
		int count_id3a = 0;
		
		/*declaration for id=4*/
		String name_id4 = null;		//information for the name of table corresponding to id=4
		String scan_id4 = null;		//information for the type of access corresponding to id=4
		int count_id4a = 0;
		
							
		node = new Node();			//setting up the root node whpse id=0,parent id=-1
		node.setId(0);
		node.setParentId(-1);
		node.setName("RETURN");
		plan.setNode(node,plan.getSize());
		
		try{
			stmt = con.createStatement ();
			rset=stmt.executeQuery("explain " + query);
			while(rset.next()){		
				/*
				 * Warning : Update from EXPLAIN SELECT in MySQL 5.0
				 * The following is the ordering of information accessed from MySQL explain tables
				 * 1 : id,ie,sequenial number of select within the query
				 * 2 : select_type,ie,type of select
				 * 3 : table,to which row of output refers
				 * 4 : type,ie,the join type
				 * 5 : possible_keys
				 * 6 : key
				 * 7 : key_len
				 * 8 : ref
				 * 9 : rows,ie,the cardinality
				 * 10: extra
				 */
			if(rset.getInt(1)==1)			//for primary queries, having id=1
			{
				node = new Node();			//setting up the leaf node containing name of table and having id=-1
				node.setId(-1);
				node.setParentId(curNode);
				tempParent1=curNode;
				node.setName(rset.getString(3));
				node.setCard((float)rset.getDouble(9));	//estimated no of rows for a particular table,ie,estimate cardinality
				if((rset.getDouble(9))!=0)	
						card=card*(rset.getDouble(9));
				if (count==0) 
						info = rset.getString(10);		//to check whether sorting has been performed or not
				plan.setNode(node,plan.getSize());
				if (count==0) 							//this info is needed if there is derived type of subquery corresponding to id=2
						typescan_id1 = rset.getString(4);		
				curNode++;
				count++;	
								
				
				node=new Node();			//setting up the node having information of type of table access
				node.setId(tempParent1);
				tempChild2=tempParent1;
				if (count==1)
				{
					node.setParentId(curNode+1);
					tempParent2=curNode+1;
				}
				else 
				{
					node.setParentId(curNode);
					tempParent2=curNode;
				}
				if((rset.getString(4)).equals("ALL"))
					node.setName("Table scan");
				else if ((rset.getString(4)).equals("index"))
					node.setName("Index scan");
				else if ((rset.getString(4)).equals("range"))
					node.setName("Index range scan");
				else if ((rset.getString(4)).equals("ref"))
					node.setName("Index by reference");
				else if ((rset.getString(4)).equals("eq_ref"))
					node.setName("Index by unique reference");
				else if ((rset.getString(4)).equals("const"))
					node.setName("Constant");
				else if ((rset.getString(4)).equals("system"))
					node.setName("Constant join");
				else if ((rset.getString(4)).equals("index_subquery"))
					node.setName("Index Subquery by IN ref");
				else if ((rset.getString(4)).equals("unique_subquery"))
					node.setName("Unique Subquery by IN ref");
				plan.setNode(node,plan.getSize());
				
				
				if(count>1){			//setting up the node having the type of join information,Nested Loop Join
					node=new Node();
					node.setId(tempParent2);
					node.setParentId(tempParent2+2);
					tempChild3=tempParent2;
					tempParent3=tempParent2+2;
					node.setName("NLJoin");
					plan.setNode(node,plan.getSize());
					curNode++;
					}
			}//case of  id = 1 ends
			
			if((rset.getInt(1)==2))			//for dependent/derived/sub queries corresonding to id=2
			{
			    type_query_id2 = rset.getString(2);//information of the type of sub query
			    
			    if ((type_query_id2.equals("DERIVED"))&& count_id2a==0)
			    {
			    	curNode = 1;
			    }
			    
				node = new Node();
				node.setId(-1);
				node.setParentId(curNode);
				tempParent1=curNode;
				node.setName(rset.getString(3));
				node.setCard((float)rset.getDouble(9));
				if((rset.getDouble(9))!=0)
						card=card*(rset.getDouble(9));
				plan.setNode(node,plan.getSize());
				curNode++;
				count_id2a++;	
				nodecount++;				//to count the total no of nodes created corresponding to id=2
				
				
				node=new Node();
				node.setId(tempParent1);
				tempChild2=tempParent1;
				if (count_id2a==1){
					node.setParentId(curNode+1);
					tempParent2=curNode+1;
				}
				else {
					node.setParentId(curNode);
					tempParent2=curNode;
				}
				if((rset.getString(4)).equals("ALL"))
					node.setName("Table scan");
				else if ((rset.getString(4)).equals("index"))
					node.setName("Index scan");
				else if ((rset.getString(4)).equals("range"))
					node.setName("Index range scan");
				else if ((rset.getString(4)).equals("ref"))
					node.setName("Index by reference");
				else if ((rset.getString(4)).equals("eq_ref"))
					node.setName("Index by unique reference");
				else if ((rset.getString(4)).equals("const"))
					node.setName("Constant");
				else if ((rset.getString(4)).equals("system"))
					node.setName("Constant join");
				else if ((rset.getString(4)).equals("index_subquery"))
					node.setName("Index subquery by IN ref");
				else if ((rset.getString(4)).equals("unique_subquery"))
					node.setName("Unique subquery by IN ref");
				plan.setNode(node,plan.getSize());
				nodecount++;				//to count the total no of nodes created corresponding to id=2
				
				
				if(count_id2a>1){
					node=new Node();
					node.setId(tempParent2);
					node.setParentId(tempParent2+2);
					tempChild3=tempParent2;
					tempParent3=tempParent2+2;
					node.setName("NLJoin");
					plan.setNode(node,plan.getSize());
					curNode++;
					nodecount++;			//to count the total no of nodes created corresponding to id=2
					}
				}//if id=2 and case "DEPENDENT SUBQUERY/DERIVED/SUBQUERY" ends
			
			if(rset.getInt(1)==3)			//for id=3 cases
			{ 
				name_id3 = (rset.getString(3));
				if((rset.getString(4)).equals("ALL"))
					scan_id3 = "Table Scan";
				else if ((rset.getString(4)).equals("index"))
					scan_id3 = "Index scan";
				else if ((rset.getString(4)).equals("range"))
					scan_id3 = "Index Range scan";
				else if ((rset.getString(4)).equals("ref"))
					scan_id3 = "Index by reference";
				else if ((rset.getString(4)).equals("eq_ref"))
					scan_id3 = "Index by unique reference";
				else if ((rset.getString(4)).equals("const"))
					scan_id3 = "const";
				else if ((rset.getString(4)).equals("unique_subquery"))
					scan_id3 = "Unique Subquery by IN ref";
				else if ((rset.getString(4)).equals("index_subquery"))
					scan_id3 = "Index Subquery by IN ref";
				
				count_id3a++;
			}
			
			if(rset.getInt(1)==4)			//for id=4 cases
			{ 
				name_id4 = (rset.getString(3));
				if((rset.getString(4)).equals("ALL"))
					scan_id4 = "Table Scan";
				else if ((rset.getString(4)).equals("index"))
					scan_id4 = "Index scan";
				else if ((rset.getString(4)).equals("range"))
					scan_id4 = "Index Range scan";
				else if ((rset.getString(4)).equals("ref"))
					scan_id4 = "Index by reference";
				else if ((rset.getString(4)).equals("eq_ref"))
					scan_id4 = "Index by unique reference";
				else if ((rset.getString(4)).equals("const"))
					scan_id4 = "const";
				else if ((rset.getString(4)).equals("unique_subquery"))
					scan_id4 = "Unique Subquery by IN ref";
				else if ((rset.getString(4)).equals("index_subquery"))
					scan_id4  = "Index Subquery by IN ref";
				
				count_id4a++;
			}
			}//while loop ends
			
			rset.close();
			stmt.close();
					
		}catch(SQLException e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Database : Error accessing plan : "+e);
			throw new PicassoException("Database : Error explaining query : "+e);
		}
		
		
		/*to determine the total query cost*/
		try{
		stmt = con.createStatement ();
		rset1=stmt.executeQuery("reset query cache ");
		rset1.close();
		stmt.close();
		}
		catch(SQLException e){
		e.printStackTrace();
		ServerMessageUtil.SPrintToConsole("Database : Error reseting cache : "+e);
		throw new PicassoException("Database : Error  reseting cache : "+e);
		}
		try{
			stmt = con.createStatement ();
			rset2=stmt.executeQuery("SHOW STATUS LIKE 'Last_query_cost'");
			if(rset2.next())
				cost = (rset2.getDouble(2));
			else
				cost = 0;
			rset2.close();
			stmt.close();
			}
		catch(SQLException e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Database : Error accessing cost : "+e);
			throw new PicassoException("Database : Error explaining cost "+e);
		}//cost case ends here
			
	
		
		/*when there is a single table having id=1*/
		if (count==1)
		{
			if (nodecount==0)		//there is no dependent subquery,ie,no id=2
					{						
					if (info.indexOf("filesort")==0)	//if there is no sort info
						{
							node = plan.getNode(plan.getSize()-1);
							node.setParentId(0);
						}
						
					else								//if there is a sort info
						{
							node = new Node();
							node.setId(tempParent2);
							node.setName("SORT");
							node.setParentId(0);
							plan.setNode(node,plan.getSize());
						}
					}
						
			else					//if there is a single table of dependent subquery in id=2
				{	
				if ((type_query_id2.equals("DEPENDENT SUBQUERY"))||(type_query_id2.equals("SUBQUERY")))
					{
					if (info.indexOf("filesort")==0)	//if there is no sort info
						{
							node = plan.getNode(plan.getSize()-nodecount-1);
							node.setParentId(tempParent2);
							
				 			node = new Node();
				 			node.setId(tempParent2);
				 			node.setParentId(0);
				 			node.setName("NLJoin");
				 			plan.setNode(node,plan.getSize());
				 		}
					else								//if there is a sort info
						{
							node = plan.getNode(plan.getSize()-nodecount-1);
							node.setParentId(tempParent2);
						
							node = new Node();
							node.setId(tempParent2);
							node.setParentId(tempParent2+1);
							node.setName("NLJoin");
							plan.setNode(node,plan.getSize());
							
							node = new Node();
							node.setId(tempParent2+1);
							node.setName("SORT");
							node.setParentId(0);
							plan.setNode(node,plan.getSize());				
						}
				}//case of "DEPENENT/SUBQUERY" ends here
				
				if (type_query_id2.equals("DERIVED"))		//if there is a derived sub query case
				{
					node = new Node();
					node.setId(tempParent3);
					node.setParentId(tempParent3+1);
					node.setName("<DERIVED>");
					plan.setNode(node,plan.getSize());
										
				if (info.indexOf("filesort")==0)	//if there is no sort info
					{
						node = new Node();
						node.setId(tempParent3+1);
						node.setParentId(0);
						if(typescan_id1.equals("ALL"))
							node.setName("Table Scan");
						else
							node.setName("System scan");
						plan.setNode(node,plan.getSize());
					}
				else								//if there is sort info
					{
						node = new Node();
						node.setId(tempParent3+1);
						node.setParentId(tempParent3+2);
						if(typescan_id1.equals("ALL"))
							node.setName("Table Scan");
						else
						node.setName("System scan");
						plan.setNode(node,plan.getSize());
						
						node = new Node();
						node.setId(tempParent3+2);
						node.setParentId(0);
						node.setName("SORT");
						plan.setNode(node,plan.getSize());
					}
					
				}//case of "DERIVED" ends here
				
			}//case of id=2 present ends here
			
		} //single table case ends here
		
		

		
		/*when there are multiple tables having id=1*/
		if (count!=1)
		{
			if (nodecount==0)			//indicates the id=2 is not present
				{
					if (info.indexOf("filesort")==0)	//if there is no sort info
						{
							node = plan.getNode(plan.getSize()-1);	
							node.setParentId(0);
						}
					else								//if there is sort info
						{
							node.setId(tempParent3);
							node.setParentId(0);
							node.setName("SORT");
							plan.setNode(node,plan.getSize());
						}
				}
				
			else						//indicates the id=2 is present
				{
				if ((type_query_id2.equals("DEPENDENT SUBQUERY"))||(type_query_id2.equals("SUBQUERY")))
					{
						node = plan.getNode(plan.getSize()-nodecount-1);
						if (nodecount==2)					//when there is a single row of id=2 information
								node.setParentId(tempParent2);
						else
								node.setParentId(tempParent3);
						
						
				  if (count_id3a==0 && count_id4a==0)		//there are no id=3 and id=4 information
					{	
					if (info.indexOf("filesort")==0)		//if there is no sort info
						{
							node = new Node();
							if (nodecount==2)				//when there is a single row of id=2 information
								node.setId(tempParent2);
							else
								node.setId(tempParent3);
							node.setParentId(0);
							node.setName("NLJoin");
							plan.setNode(node,plan.getSize());
						}									//case of no sort info ends here
					else									//if there is sort info
						{
							node = new Node();
							if (nodecount==2)				//when there is a single row of id=2 information
							{
								node.setId(tempParent2);
								node.setParentId(tempParent2+1);
							}
							else
							{
								node.setId(tempParent3);
								node.setParentId(tempParent3+1);
							}
							
							node.setName("NLJoin");
							plan.setNode(node,plan.getSize());
							
							node = new Node();
							if (nodecount==2)				//when there is a single row of id=2 information
								node.setId(tempParent2+1);
							else
								node.setId(tempParent3+1);
							node.setParentId(0);
							node.setName("SORT");
							plan.setNode(node,plan.getSize());
						}//case of sort info ends here
					
					}// case that no id=3 and no id=4 ends here
					
				if (count_id3a==1 && count_id4a==0)		//there is id=3, but no id=4 information
					{	
					if (info.indexOf("filesort")==0)	//if there is no sort info
						{
						if (nodecount==2)				//when there is a single row of id=2 information
							{
							node = new Node();
							node.setId(tempParent2);
							node.setParentId(tempParent2+1);
							node.setName("NLJoin");
							plan.setNode(node,plan.getSize());
							
							node = new Node();
							node.setId(tempParent2+1);
							node.setParentId(0);
							node.setName("NLJoin");
							plan.setNode(node,plan.getSize());
							
							node = new Node();
							node.setId(tempParent2+2);
							node.setParentId(tempParent2+1);
							node.setName(scan_id3);
							plan.setNode(node,plan.getSize());
							
							node = new Node();
							node.setId(-1);
							node.setParentId(tempParent2+2);
							node.setName(name_id3);
							plan.setNode(node,plan.getSize());
																		
							}
						else						
						{
							node = new Node();
							node.setId(tempParent3);
							node.setParentId(tempParent3+1);
							node.setName("NLJoin");
							plan.setNode(node,plan.getSize());
							
							node = new Node();
							node.setId(tempParent3+1);
							node.setParentId(0);
							node.setName("NLJoin");
							plan.setNode(node,plan.getSize());
							
							node = new Node();
							node.setId(tempParent3+2);
							node.setParentId(tempParent3+1);
							node.setName(scan_id3);
							plan.setNode(node,plan.getSize());
							
							node = new Node();
							node.setId(-1);
							node.setParentId(tempParent3+2);
							node.setName(name_id3);
							plan.setNode(node,plan.getSize());
						}
					}									//case of no sort info ends here
					else								//if there is sort info
						{
							if (nodecount==2)			//when there is a single row of id=2 information
								{
								node = new Node();
								node.setId(tempParent2);
								node.setParentId(tempParent2+1);
								node.setName("NLJoin");
								plan.setNode(node,plan.getSize());
								
								node = new Node();
								node.setId(tempParent2+1);
								node.setParentId(tempParent2+2);
								node.setName("NLJoin");
								plan.setNode(node,plan.getSize());
								
								node = new Node();
								node.setId(tempParent2+3);
								node.setParentId(tempParent2+1);
								node.setName(scan_id3);
								plan.setNode(node,plan.getSize());
								
								node = new Node();
								node.setId(-1);
								node.setParentId(tempParent2+3);
								node.setName(name_id3);
								plan.setNode(node,plan.getSize());
								
								node = new Node();
								node.setId(tempParent2+2);
								node.setParentId(0);
								node.setName("SORT");
								plan.setNode(node,plan.getSize());
																			
							}
							else
							{
								node = new Node();
								node.setId(tempParent3);
								node.setParentId(tempParent3+1);
								node.setName("NLJoin");
								plan.setNode(node,plan.getSize());
								
								node = new Node();
								node.setId(tempParent3+1);
								node.setParentId(tempParent3+2);
								node.setName("NLJoin");
								plan.setNode(node,plan.getSize());
								
								node = new Node();
								node.setId(tempParent3+3);
								node.setParentId(tempParent3+2);
								node.setName(scan_id3);
								plan.setNode(node,plan.getSize());
								
								node = new Node();
								node.setId(-1);
								node.setParentId(tempParent3+3);
								node.setName(name_id3);
								plan.setNode(node,plan.getSize());
								
								node = new Node();
								node.setId(tempParent3+2);
								node.setParentId(0);
								node.setName("SORT");
								plan.setNode(node,plan.getSize());
							}
						}//case there is sort info ends here
					
					}// case there is id=3 and no id=4 ends here
				
				if (count_id3a==1 && count_id4a==1)		//there is id=3 as well as id=4
				{	
				if (info.indexOf("filesort")==0)		//if there is no sort info
					{
					if (nodecount==2)					//when there is a single row of id=2 information
						{
						node = new Node();
						node.setId(tempParent2);
						node.setParentId(tempParent2+1);
						node.setName("NLJoin");
						plan.setNode(node,plan.getSize());
						
						node = new Node();
						node.setId(tempParent2+1);
						node.setParentId(tempParent2+2);
						node.setName("NLJoin");
						plan.setNode(node,plan.getSize());
						
						node = new Node();
						node.setId(tempParent2+2);
						node.setParentId(0);
						node.setName("NLJoin");
						plan.setNode(node,plan.getSize());
						
						node = new Node();
						node.setId(tempParent2+3);
						node.setParentId(tempParent2+1);
						node.setName(scan_id3);
						plan.setNode(node,plan.getSize());
						
						node = new Node();
						node.setId(-1);
						node.setParentId(tempParent2+3);
						node.setName(name_id3);
						plan.setNode(node,plan.getSize());
						
						
						node = new Node();
						node.setId(tempParent2+4);
						node.setParentId(tempParent2+2);
						node.setName(scan_id4);
						plan.setNode(node,plan.getSize());
						
						node = new Node();
						node.setId(-1);
						node.setParentId(tempParent2+4);
						node.setName(name_id4);
						plan.setNode(node,plan.getSize());
																	
						}
					else
					{
						node = new Node();
						node.setId(tempParent3);
						node.setParentId(tempParent3+1);
						node.setName("NLJoin");
						plan.setNode(node,plan.getSize());
						
						node = new Node();
						node.setId(tempParent3+1);
						node.setParentId(tempParent3+2);
						node.setName("NLJoin");
						plan.setNode(node,plan.getSize());
						
						node = new Node();
						node.setId(tempParent3+2);
						node.setParentId(tempParent3+3);
						node.setName("NLJoin");
						plan.setNode(node,plan.getSize());
						
						node = new Node();
						node.setId(tempParent3+4);
						node.setParentId(tempParent3+2);
						node.setName(scan_id3);
						plan.setNode(node,plan.getSize());
						
						node = new Node();
						node.setId(-1);
						node.setParentId(tempParent3+4);
						node.setName(name_id3);
						plan.setNode(node,plan.getSize());
						
						node = new Node();
						node.setId(tempParent3+5);
						node.setParentId(tempParent3+3);
						node.setName(scan_id4);
						plan.setNode(node,plan.getSize());
						
						node = new Node();
						node.setId(-1);
						node.setParentId(tempParent3+5);
						node.setName(name_id4);
						plan.setNode(node,plan.getSize());
					}
				}									//case of no sort info ends here
				else								//if there is sort info
					{
						if (nodecount==2)			//when there is a single row of id=2 information
							{
							node = new Node();
							node.setId(tempParent2);
							node.setParentId(tempParent2+1);
							node.setName("NLJoin");
							plan.setNode(node,plan.getSize());
							
							node = new Node();
							node.setId(tempParent2+1);
							node.setParentId(tempParent2+2);
							node.setName("NLJoin");
							plan.setNode(node,plan.getSize());
							
							node = new Node();
							node.setId(tempParent2+3);
							node.setParentId(tempParent2+1);
							node.setName(scan_id3);
							plan.setNode(node,plan.getSize());
							
							node = new Node();
							node.setId(-1);
							node.setParentId(tempParent2+3);
							node.setName(name_id3);
							plan.setNode(node,plan.getSize());
							
							node = new Node();
							node.setId(tempParent2+2);
							node.setParentId(0);
							node.setName("SORT");
							plan.setNode(node,plan.getSize());
																		
						}
						else
						{
							node = new Node();
							node.setId(tempParent3);
							node.setParentId(tempParent3+1);
							node.setName("NLJoin");
							plan.setNode(node,plan.getSize());
							
							node = new Node();
							node.setId(tempParent3+1);
							node.setParentId(tempParent3+2);
							node.setName("NLJoin");
							plan.setNode(node,plan.getSize());
							
							node = new Node();
							node.setId(tempParent3+3);
							node.setParentId(tempParent3+2);
							node.setName(scan_id3);
							plan.setNode(node,plan.getSize());
							
							node = new Node();
							node.setId(-1);
							node.setParentId(tempParent3+3);
							node.setName(name_id3);
							plan.setNode(node,plan.getSize());
							
							node = new Node();
							node.setId(tempParent3+2);
							node.setParentId(0);
							node.setName("SORT");
							plan.setNode(node,plan.getSize());
						}
					}//case there is sort info ends here
				
				  }// case there is id=3 and id=4 ends here
				
			    }//case of "DEPENENT SUBQUERY" ends here
				
			}//case of id=2 present ends here
			
		}//ends if count!=1
	
	/*setting the total cost and cardinality values to the root node*/
		node = plan.getNode(0);
		node.setCard((float)card);
		node.setCost((float)cost);
		
		
		return plan;
	}
	
	public Plan getPlan(String query,int startQueryNumber) throws PicassoException
	{
		return getPlan(query);
	}
	public void deletePicassoTables() throws PicassoException
	{
		try{
			Statement stmt = createStatement();
			stmt.executeUpdate("drop table "+getSchema()+".PicassoSelectivityLog");
			stmt.executeUpdate("drop table "+getSchema()+".PicassoSelectivityMap");
			stmt.executeUpdate("drop table "+getSchema()+".PicassoPlanTreeArgs");
			stmt.executeUpdate("drop table "+getSchema()+".PicassoPlanTree");
			stmt.executeUpdate("drop table "+getSchema()+".PicassoPlanStore");
			stmt.executeUpdate("drop table "+getSchema()+".PicassoRangeResMap"); //-ma
			stmt.executeUpdate("drop table "+getSchema()+".PicassoApproxMap");
			stmt.executeUpdate("drop table "+getSchema()+".PicassoQTIDMap");
			stmt.close();
		}catch(Exception e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Error Dropping Picasso Tables"+e);
			throw new PicassoException("Error Dropping Picasso Tables"+e);
		}
	}
}

