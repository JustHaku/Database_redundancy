import java.sql.*;
import java.io.*;
import java.util.ArrayList;

/**
 *Reads a given database's structure and provides the sql commands needed to recreate it
 *
 * @author Nathaniel Vanderpuye
 */
public class SQLiteJDBC extends DbBasic {
	
	private DatabaseMetaData data;
	ArrayList<String> primaryKeys;
	ArrayList<String> foreignKeys;
	ArrayList<String> colInserts;
	
	/**
     *Extends the DbBasic class and uses a provided database name to connect to it
	 *Collects the meta data of the db using the established connection
	 *
     * @param dbName The name of the database to be used
     */
	public SQLiteJDBC(String dbName){
		super(dbName);
		
		try{
			data = con.getMetaData();
		}
		catch(Exception e){
			System.out.println(e);
		}
	}
	
	
	/**
     *Collects all the tables in the database and increments through their data 
	 *Uses other created methods to get relevant information needed to rebuild the table
	 *currently being incremented through
	 *
     */
	public void retrieveInfo(){
		
		ArrayList<String> allTables = new ArrayList<String>();
		ArrayList<ArrayList<Column>> allTableColumns = new ArrayList<ArrayList<Column>>();
		ArrayList<String> allPrimaryKeys = new ArrayList<String>();
		ArrayList<ArrayList<String>> allForeignKeys = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<String>> info = new ArrayList<ArrayList<String>>();
		
		try{
			ResultSet tables = data.getTables(null,null,null, new String[]{"TABLE"});

			while(tables.next()){
				
				String tableSchema = tables.getString("TABLE_SCHEM");
				String tableCatalog = tables.getString("TABLE_CAT");
				String tableName = tables.getString("TABLE_NAME");
				ResultSet primKeys;
				ResultSet forKeys;
				ResultSet cols = null;
				
				try{
					cols = data.getColumns(tableCatalog,tableSchema,tableName,null);
				}
				catch(Exception e){
					System.out.println(e);
				}
				
				String primaryKeys = getPrimKeys(tableCatalog,tableName,tableSchema);
				ArrayList<String> foreignKeys = getForKeys(tableCatalog,tableName,tableSchema);
				allTableColumns.add(getColumns(cols));
				allPrimaryKeys.add(primaryKeys);
				allForeignKeys.add(foreignKeys);
				allTables.add(tableName);
			}


			File replica = new File("Replica.txt");
			replica.createNewFile();
			FileWriter out = new FileWriter(replica);

			for(int i = 0; i < allTables.size(); i++){
				out.write(constructTables(allTables.get(i),allPrimaryKeys.get(i),allForeignKeys.get(i),allTableColumns.get(i)));
				info.add(getData(allTables.get(i), allTableColumns.get(i)));
			}
			
			for(int j = 0; j < info.size(); j++){
				for(int a = 0; a < info.get(j).size(); a++){
					out.write(info.get(j).get(a));
				}
			}

			out.flush();
			out.close();
		}
		catch(Exception e){
			System.out.println(e);
		}
	}
	
	/**
     *Collects the location of primary keys in a table using the tables given metadata 
	 *also formats the data to be used in database recreation
	 *
     *@param tableCat The Catalog of the table included in a tables metadata
	 *@param tableNam The Name of the table included in the tables metadata
	 *@param tableSchem The Schema of the table included in the tables metadata
	 *@return result An arraylist that contains the sql statement needed to declare a primary key
     */
	public String getPrimKeys(String tableCat,String tableNam,String tableSchem){
		ArrayList<String> primKeysTemp = new ArrayList<String>();
		String result = null;
		
		try{
			ResultSet pKeys = data.getPrimaryKeys(tableCat,tableSchem,tableNam);

			while(pKeys.next()){
				primKeysTemp.add(pKeys.getString("COLUMN_NAME"));
			}
			
			for(int i = 0; i < primKeysTemp.size();i++){
				if(i == 0){
					result = (primKeysTemp.get(i));
				}
				else{
					result = (result + ", " + primKeysTemp.get(i));
				}
			}

			result = ("PRIMARY KEY (" + result + ")");
		}
		catch (Exception e){
			System.out.println(e);
		}

		return result;
	}
	
	/**
     *Collects info regarding the foreign keys stored in a given table
	 *also formats the data to be used in database recreation
	 *
     *@param tableCat The Catalog of the table included in a tables metadata
	 *@param tableNam The Name of the table included in the tables metadata
	 *@param tableSchem The Schema of the table included in the tables metadata
	 *@return forKeys An arraylist that contains the sql statement needed to declare a foreign key
     */
	public ArrayList<String> getForKeys(String tableCat,String tableNam,String tableSchem){
		
		ArrayList<String> forKeys = new ArrayList<String>();
		
		try{
			ResultSet fKeys = data.getImportedKeys(tableCat,tableSchem,tableNam);

			while(fKeys.next()){
				forKeys.add(("FOREIGN KEY " + fKeys.getString("FKCOLUMN_NAME") + " REFERENCES " + fKeys.getString("PKTABLE_NAME") + "(" + fKeys.getString("PKCOLUMN_NAME") + ")"));
			}
		}
		catch (Exception e){
			System.out.println(e);
		}
		
		return forKeys;
	}
	
	/**
     *Uses the Column class to store information in an arraylist about a given tables columns
	 *
     *@param a The resultSet created from collecting the columns in a table using metadata
	 *@return allColumns An arraylist that contains the columns in the table
     */
	public ArrayList<Column> getColumns(ResultSet a){
		
		ArrayList<Column> allColumns = new ArrayList<Column>();		
			
		try{
			while(a.next()){
				allColumns.add(new Column(a.getString("COLUMN_NAME"),a.getString("DATA_TYPE"),a.getString("TYPE_NAME"),a.getString("COLUMN_SIZE"),a.getString("IS_NULLABLE")));
			}
		}
		catch(Exception e){
			System.out.println(e);
		}
			
		return allColumns;
	}
	
	/**
     *Collects info regarding the data stored in the columns of a given table
	 *also formats the data to be used in database recreation
	 *
     *@param tableName Used to reference the table the data is stored in
	 *@param columns Contains the columns from a given table
	 *@return tableData
     */
	public ArrayList<String> getData(String tableName,ArrayList<Column> columns){
		
		ArrayList <String> tableData = new ArrayList<String>();
		ResultSet info = null;
		
		try{
			String query = ("SELECT * FROM " + tableName);
			Statement sqlStmt = con.createStatement();
			info = sqlStmt.executeQuery(query);

			while(info.next()){

				String result = null;
				
				for(int i = 0; i < columns.size();i++){
					if(i == 0){
						if((columns.get(i)).getTypeNumber() == 12){
							result = ("\"" + info.getString(columns.get(i).getColumnName()) + "\"");
						}
						else{
							result = (info.getString(columns.get(i).getColumnName()));
						}
					}
					else{
						if((columns.get(i)).getTypeNumber() == 12){
							result = (result + ",\"" + info.getString(columns.get(i).getColumnName()) + "\"");
						}
						else{
							result = (result + "," + info.getString(columns.get(i).getColumnName()));
						}
					}
				}
				
				result = ("INSERT INTO " + tableName + " VALUES (" + result + ");\n");
				tableData.add(result);
			}
		}
		catch(Exception e){
			System.out.println(e);
		}
		
		return tableData;
	}
	
	/**
     *Constructs the sql statements needed to recreate the given database
	 *formats all the data
	 *
     *@param tableName Used to reference the table the data is stored in
	 *@param primaryKeys Used to reference the stored primary keys in the table
	 *@param foreignKeys Used to reference the stored foreign keys in the table
	 *@param columns Used to reference the stored columns in the table
	 
     */
	public String constructTables(String tableName,String primaryKeys,ArrayList<String> foreignKeys, ArrayList<Column> columns){
		
		String createTables = ("CREATE TABLE " + tableName + "(");
		
		for(int a = 0; a < columns.size(); a++){

			createTables = createTables + " " + columns.get(a).getColumnName() + " " + (columns.get(a)).getDataType();

			if((columns.get(a)).getIsNullable() == false){
				createTables = createTables + "NOT null";
			}

			createTables = createTables + ",";
		}
		
		createTables = createTables + primaryKeys;

		for(int b = 0; b < foreignKeys.size(); b++){
			if(b == 0){
				createTables = createTables + ",\n" + foreignKeys.get(b);
			}
		}
		
		createTables = createTables + ";\n";
		return createTables;
	}

    public static void main(String[] args) {
       
	   SQLiteJDBC uni = new SQLiteJDBC("University.db");
	   uni.retrieveInfo();
    }
}
