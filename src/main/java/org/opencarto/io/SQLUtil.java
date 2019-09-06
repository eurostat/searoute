/**
 * 
 */
package org.opencarto.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author julien Gaffuri
 *
 */
public class SQLUtil {

	//execute a query returning a resultset
	public static ResultSet getResult(Connection c, String qu){
		try {
			return c.createStatement().executeQuery(qu);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}


	//convert a result set into a pojo list
	public static ArrayList<HashMap<String, String>> toList(ResultSet res) {
		ArrayList<HashMap<String,String>> data = new ArrayList<HashMap<String,String>>();
		try {
			ResultSetMetaData rsmd = res.getMetaData();
			while(res.next()){
				HashMap<String,String> obj = new HashMap<String,String>();
				for (int i=1; i<rsmd.getColumnCount()+1; i++) {
					String key = rsmd.getColumnName(i);
					obj.put(key, res.getString(key));
				}
				data.add(obj);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return data;
	}

	public static int getCount(Connection c, String tableName){
		return getCount(c, tableName, null);
	}
	public static int getCount(Connection c, String tableName, String where){
		String qu = "SELECT COUNT(*) FROM "+tableName;
		if(where != null) qu += " WHERE "+where;
		return getSQLCount(c, qu);
	}
	public static int getSQLCount(Connection c, String qu){
		//System.out.println(qu);
		try {
			ResultSet res = getResult(c, qu);
			res.next();
			int out = res.getInt(1);
			res.close();
			return out;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}


	//save the result of a query as a CSV file
	public static void saveAsCSV(Connection c, String qu, String path, String file){
		try {
			ResultSet res = getResult(c, qu);
			saveAsCSV( res, path, file );
			res.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	/*public static void saveAsCSV(ResultSet res, String path, String file){
		try {
			new File(path).mkdirs();
			File f=new File(path+file);
			if(f.exists()) f.delete();

			CSVWriter cw = new CSVWriter(new FileWriter(path+file));
			cw.writeAll(res, true);
			cw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/

	public static void saveAsCSV(ResultSet res, String path, String file){
		try {
			new File(path).mkdirs();
			File f=new File(path+file);
			if(f.exists()) f.delete();

			BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));

			//write header
			ResultSetMetaData rsmd = res.getMetaData();
			for (int i=1; i<rsmd.getColumnCount()+1; i++) {
				bw.write( rsmd.getColumnName(i) );
				if(i<rsmd.getColumnCount()) bw.write(",");
			}
			bw.write("\n");

			//write data
			while(res.next()){
				for (int i=1; i<rsmd.getColumnCount()+1; i++) {
					String key = rsmd.getColumnName(i);
					String value = res.getString(key);
					if(value!=null) bw.write(res.getString(key));
					if(i<rsmd.getColumnCount()) bw.write(",");
				}
				bw.write("\n");
			}
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	//save unique values of a table column into a CSV file
	public static void getUniqueColumn(Connection c, String table, String col, String path, String file){
		getUniqueColumn(c, table, col, null, null, null, path, file);
	}
	public static void getUniqueColumn(Connection c, String table, String col, String dateCol, String date1, String date2, String path, String file){
		String qu = "SELECT DISTINCT "+col+" FROM "+table;
		if(dateCol!=null) qu += " WHERE "+dateCol+">=to_date('"+date1+"','YYYY-MM-DD') AND "+dateCol+"<=to_date('"+date2+"','YYYY-MM-DD')";
		//System.out.println(qu);
		saveAsCSV(c, qu, path, file);
	}

}
