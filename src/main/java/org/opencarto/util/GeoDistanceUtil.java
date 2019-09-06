/**
 * 
 */
package org.opencarto.util;

import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

/**
 * Some functions to compute distances from geographical coordinates.
 * 
 * @author julien Gaffuri
 *
 */
public class GeoDistanceUtil {


	public static double getDistanceKM(double slon, double slat, double dlon, double dlat){
		GeodeticCalculator gc = new GeodeticCalculator();
		gc.setStartingGeographicPoint(slon, slat);
		gc.setDestinationGeographicPoint(dlon, dlat);
		return gc.getOrthodromicDistance() * 0.001;
	}

	//in km
	public static double getDistanceKM(Coordinate c1, Coordinate c2) {
		return getDistanceKM(c1.x, c1.y ,c2.x, c2.y);
	}

	//in km
	public static double getLengthGeoKM(Geometry g) {
		if(g instanceof LineString) return getLengthGeoKM((LineString)g);
		if(g instanceof MultiLineString) return getLengthGeoKM((MultiLineString)g);
		System.err.println("getLengthGeo not implemented for geometry type "+g.getGeometryType());
		return -1;
	}
	//in km
	public static double getLengthGeoKM(MultiLineString mls){
		double dist = 0;
		for(int i=0; i<mls.getNumGeometries(); i++)
			dist += getLengthGeoKM( (LineString) mls.getGeometryN(i) );
		return dist;
	}
	//in km
	public static double getLengthGeoKM(LineString ls){
		Coordinate[] cs = ls.getCoordinates();
		Coordinate c1=cs[0],c2;
		double dist=0;
		for(int i=1;i<cs.length;i++){
			c2=cs[i];
			dist+=getDistanceKM(c1.x, c1.y ,c2.x, c2.y);
			c1=c2;
		}
		return dist;
	}


}
