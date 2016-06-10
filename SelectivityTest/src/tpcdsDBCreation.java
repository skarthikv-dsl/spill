import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;


public class tpcdsDBCreation {

	public static void main(String args[])
	{

		Connection c = null;
		Statement stmt = null;
		//String[] tables = {"income_band","ship_mode","warehoue","reason","website","store","promotion","household_demographics","web_page","catalog_page","time_dim","date_dim","item","customer","customer_address","web_returns","catalog_returns","store_returns","inventory","web_sales","catalog_sales","store_sales"};
		String[] tables = {"store_returns","inventory","web_sales","catalog_sales","store_sales"};
		//String[] tables = {"region","nation"};
		ResultSet rs = null;
		for(int i=0;i< tables.length;i++){
			
			try {
				Class.forName("org.postgresql.Driver");
				c = DriverManager
						.getConnection("jdbc:postgresql://localhost:5432/job",
								"sa", "database");
				System.out.println("Opened database successfully");
				stmt = c.createStatement();
//				String query = "create table income_band (ib_income_band_sk         integer               not null,    ib_lower_bound            integer,    ib_upper_bound   integer ,    primary key(ib_income_band_sk));";
//				String query1 = " COPY income_band from '/home/dsladmin/Downloads/income_band2.dat' WITH DELIMITER AS '|'  NULL AS ''; ";
				String col_query = "select column_name from information_schema.columns where table_name = '"+tables[i]+"';";
				rs = stmt.executeQuery(col_query);
				while(rs.next()){
					//Retrieve by column name
					String cols = rs.getString("column_name");
					System.out.println(tables[i]+": "+cols);
					String index_query = "create index "+ " on "+tables[i]+ "("+cols+")";
					
				}
				
				rs.close();
				stmt.close();
				c.close();
				//stmt.execute(query1);
				//read the selectivity returned
			}
			catch ( Exception e ) {
				System.err.println( e.getClass().getName()+": "+ e.getMessage() );
				System.exit(0);
			}
		}

	}
}
    
