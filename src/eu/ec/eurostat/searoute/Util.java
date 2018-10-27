/**
 * 
 */
package eu.ec.eurostat.searoute;

import java.io.IOException;
import java.io.StringWriter;

import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.referencing.GeodeticCalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

/**
 * @author julien Gaffuri
 *
 */
public class Util {



	//in km
	public static double getDistance(Coordinate c1, Coordinate c2) {
		return getDistance(c1.x, c1.y ,c2.x, c2.y);
	}
	public static double getDistance(double slon, double slat, double dlon, double dlat){
		GeodeticCalculator gc = new GeodeticCalculator();
		gc.setStartingGeographicPoint(slon, slat);
		gc.setDestinationGeographicPoint(dlon, dlat);
		return gc.getOrthodromicDistance() * 0.001;
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
		} catch (IOException e) { e.printStackTrace(); }
		return writer.toString();
	}

	//round a double
	public static double round(double x, int decimalNB) {
		double pow = Math.pow(10, decimalNB);
		return ( (int)(x * pow + 0.5) ) / pow;
	}

}
