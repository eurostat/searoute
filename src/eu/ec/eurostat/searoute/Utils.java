package eu.ec.eurostat.searoute;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.referencing.GeodeticCalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class Utils {
	public static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");




	//in km
	public static double getDistance(double slon, double slat, double dlon, double dlat){
		GeodeticCalculator gc = new GeodeticCalculator();
		gc.setStartingGeographicPoint(slon, slat);
		gc.setDestinationGeographicPoint(dlon, dlat);
		return gc.getOrthodromicDistance() * 0.001;
	}

	public static double getDistance(Coordinate c1, Coordinate c2) {
		return getDistance(c1.x, c1.y ,c2.x, c2.y);
	}

	//in km
	public static double getLengthGeo(MultiLineString mls){
		double dist = 0;
		for(int i=0; i<mls.getNumGeometries(); i++)
			dist += getLengthGeo( (LineString) mls.getGeometryN(i) );
		return dist;
	}
	//in km
	public static double getLengthGeo(LineString ls){
		Coordinate[] cs = ls.getCoordinates();
		Coordinate c1=cs[0],c2;
		double dist=0;
		for(int i=1;i<cs.length;i++){
			c2=cs[i];
			dist+=getDistance(c1.x, c1.y ,c2.x, c2.y);
			c1=c2;
		}
		return dist;
	}

	public static String toGeoJSON(Geometry geom){
		StringWriter writer = new StringWriter();
		try {
			new GeometryJSON().write(geom, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return writer.toString();
	}


	/*public static String toUTF8(String st){
		try {
			//return Charset.forName("UTF-8").encode(st);
			return new String(st.getBytes("ISO-8859-1"), "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
			return st;
		}
	}*/
/*
	public static void main (String[] args){
		System.out.println(toUTF8("TITANÂ World"));
		System.out.println(toUTF8("été après"));
	}*/

	public static String toTitleCase(String s) {
	    String[] arr = s.split(" ");
	    StringBuffer sb = new StringBuffer();
	    for (int i = 0; i < arr.length; i++) {
	        sb.append(Character.toUpperCase(arr[i].charAt(0)))
	            .append(arr[i].substring(1)).append(" ");
	    }
	    return sb.toString().trim();
	}










	public static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s");
	public static DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd");
	public static DateFormat dateFormat3 = new SimpleDateFormat("yyyyMMdd");

	//returns the position of a contraffic location
	public static Coordinate getPosition(Connection c, Object locId){
		return getPosition(c, new Object[]{locId}).get(locId);
	}
	public static HashMap<String,Coordinate> getPosition(Connection c, Object[] locIds){
		if(locIds.length == 0) return new HashMap<String,Coordinate>();
		try {
			//SELECT LOCATION_HCODE AS ID,LONR,LATR FROM CONTRAFFIC.GIS_LOCATIONS WHERE LOCATION_HCODE IN (404,142);
			String query = "SELECT LOCATION_HCODE AS ID,LONR,LATR FROM CONTRAFFIC.GIS_LOCATIONS WHERE (1,LOCATION_HCODE) IN ((1,?)";
			for(int i=1; i<locIds.length; i++) query = query+",(1,?)";
			query = query+")";

			PreparedStatement pstmt = c.prepareStatement(query);
			for(int i=0; i<locIds.length; i++) {
				int locId = 404;
				try { locId = Integer.parseInt(locIds[i].toString()); } catch (NumberFormatException e) { e.printStackTrace(); }
				pstmt.setInt(i+1, locId);
			}

			ResultSet res = pstmt.executeQuery( );
			HashMap<String,Coordinate> out = new HashMap<String,Coordinate>();

			while(res.next()){
				String id = res.getString("ID");
				try {
					out.put(id, new Coordinate(res.getDouble("LONR"),res.getDouble("LATR")));
				} catch (Exception e) {
					out.put(id, null);
				}
			}
			res.close();
			pstmt.close();

			return out;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}


	/*public static void main(String[] args) {
		Connection conn = CTConnection.createConnection();
		//System.out.println(getPosition(conn, "404"));
		System.out.println(getPosition(conn, new String[]{"142","404"}));

		if (conn!=null) try {conn.close();}catch (Exception ignore) {}
	}*/

}
