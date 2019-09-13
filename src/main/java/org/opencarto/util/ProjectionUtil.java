/**
 * 
 */
package org.opencarto.util;

import java.awt.Toolkit;
import java.util.Collection;

import javax.measure.Unit;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.util.CRSUtilities;
import org.locationtech.jts.geom.Geometry;
import org.opencarto.datamodel.Feature;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Basic conversion functions for mercator projection.
 * 
 * @author julien Gaffuri
 *
 */
public class ProjectionUtil {
	public final static Logger LOGGER = Logger.getLogger(ProjectionUtil.class.getName());

	//geographic: ETRS89 4937 (3D) 4258(2D)
	//# ETRS89
	//<4258> +proj=longlat +ellps=GRS80 +no_defs  <>
	//private static CoordinateReferenceSystem ETRS89_2D_CRS;
	//private static CoordinateReferenceSystem ETRS89_3D_CRS;

	//projected: ETRS89 ETRS-LAEA 3035
	//# ETRS89 / ETRS-LAEA
	//<3035> +proj=laea +lat_0=52 +lon_0=10 +x_0=4321000 +y_0=3210000 +ellps=GRS80 +units=m +no_defs  <>
	public static int WGS_84_CRS_EPSG = 4326;
	private static CoordinateReferenceSystem WGS_84_CRS;
	public static CoordinateReferenceSystem getWGS_84_CRS() {
		if(WGS_84_CRS == null) WGS_84_CRS = getCRS(WGS_84_CRS_EPSG);
		return WGS_84_CRS;
	}

	//3785->used in arcgis+"Popular Visualisation CRS / Mercator"
	//3857-> EPSG:3857 -- WGS84 Web Mercator (Auxiliary Sphere). Projection used in many popular web mapping applications (Google/Bing/OpenStreetMap/etc). Sometimes known as EPSG:900913.
	public static int WEB_MERCATOR_CRS_EPSG = 3857;
	private static CoordinateReferenceSystem WEB_MERCATOR_CRS;
	public static CoordinateReferenceSystem getWEB_MERCATOR_CRS() {
		if(WEB_MERCATOR_CRS == null) WEB_MERCATOR_CRS = getCRS(WEB_MERCATOR_CRS_EPSG);
		return WEB_MERCATOR_CRS;
	}

	public static int ETRS89_LAEA_SRS_EPSG = 3035;
	private static CoordinateReferenceSystem ETRS89_LAEA_CRS;
	public static CoordinateReferenceSystem getETRS89_LAEA_CRS() {
		if(ETRS89_LAEA_CRS == null) ETRS89_LAEA_CRS = getCRS(ETRS89_LAEA_SRS_EPSG);
		return ETRS89_LAEA_CRS;
	}

	/*public static int ETRS89_2D_SRS_EPSG = 4937;
	private static CoordinateReferenceSystem ETRS89_3D_CRS;
	public static CoordinateReferenceSystem getETRS89_3D_CRS() {
		if(ETRS89_3D_CRS == null) ETRS89_3D_CRS = getCRS(ETRS89_2D_SRS_EPSG);
		return ETRS89_3D_CRS;
	}*/

	public static int ETRS89_3D_SRS_EPSG = 4258;
	private static CoordinateReferenceSystem ETRS89_2D_CRS;
	public static CoordinateReferenceSystem getETRS89_2D_CRS() {
		if(ETRS89_2D_CRS == null) ETRS89_2D_CRS = getCRS(ETRS89_3D_SRS_EPSG);
		return ETRS89_2D_CRS;
	}



	public static CoordinateReferenceSystem getCRS(int EPSG){
		try {
			return CRS.decode("EPSG:" + EPSG);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Geometry project(Geometry geom, int sourceEPSG, int destEPSG) {
		return project(geom, getCRS(sourceEPSG), getCRS(destEPSG));
	}

	public static Geometry project(Geometry geom, CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS) {
		try {
			Geometry outGeom = JTS.transform(geom, CRS.findMathTransform(sourceCRS, targetCRS, true));
			return outGeom;
		} catch (Exception e) {
			System.err.println("Error while reprojecting.");
			e.printStackTrace();
		}
		return null;
	}



	public static Geometry toWebMercator(Geometry geom, CoordinateReferenceSystem sourceCRS) {
		return project(geom, sourceCRS, getWEB_MERCATOR_CRS());
	}

	public static void toWebMercator(Collection<Feature> fs, CoordinateReferenceSystem sourceCRS) {
		for(Feature f : fs)
			f.setGeom( toWebMercator(f.getGeom(), sourceCRS) );
	}



	public static Geometry toWGS84(Geometry geom, CoordinateReferenceSystem sourceCRS) {
		return project(geom, sourceCRS, getWGS_84_CRS());
	}

	public static void toWGS84(Collection<Feature> fs, CoordinateReferenceSystem sourceCRS) {
		for(Feature f : fs)
			f.setGeom( toWGS84(f.getGeom(), sourceCRS) );
	}



	public static Geometry toLAEA(Geometry geom, CoordinateReferenceSystem sourceCRS) {
		return project(geom, sourceCRS, getETRS89_LAEA_CRS());
	}

	public static void toLAEA(Collection<Feature> fs, CoordinateReferenceSystem sourceCRS) {
		for(Feature f : fs)
			f.setGeom( toLAEA(f.getGeom(), sourceCRS) );
	}











	public static final double EARTH_RADIUS_M = 6378137;
	public static final double degToRadFactor = Math.PI/180;
	public static final double ED = EARTH_RADIUS_M * degToRadFactor;
	public static final double PHI_MAX_RAD = Math.asin((Math.exp(2*Math.PI)-1)/(Math.exp(2*Math.PI)+1));
	public static final double PHI_MAX_DEG = PHI_MAX_RAD / degToRadFactor;


	// conversions between (XGeo, YGeo) and (lon, lat)

	/**
	 * @param lon The longitude.
	 * @return The X geo coordinate.
	 */
	public static double getXGeo(double lon) {
		return lon * ED;
	}

	/**
	 * @param xGeo The X geo coordinate.
	 * @return The longitude.
	 */
	public static double getLon(double xGeo) {
		return xGeo / ED;
	}

	/**
	 * @param lat The latitude.
	 * @return The Y geo coordinate.
	 */
	public static double getYGeo(double lat) {
		double s = Math.sin(lat * degToRadFactor);
		return EARTH_RADIUS_M * 0.5 * Math.log((1+s)/(1-s));
	}

	/**
	 * @param yGeo The Y geo coordinate.
	 * @return The latitude.
	 */
	public static double getLat(double yGeo) {
		return 90*( 4* Math.atan(Math.exp(yGeo/EARTH_RADIUS_M)) / Math.PI - 1 );
	}


	// conversions between (XPix, YPix) and (lon, lat)

	/**
	 * @param lon The longitude.
	 * @param zoomLevel The zoom level.
	 * @return The X pixel coordinate.
	 */
	public static double getXPixFromLon(double lon, int zoomLevel) {
		double x = (fit(lon, -180, 180) + 180) / 360; 
		int s = getTotalMapSizeInPixel(zoomLevel);
		return fit(s*x+0.5, 0, s-1);
	}

	/**
	 * @param XPix The X pixel coordinate.
	 * @param zoomLevel The zoom level.
	 * @return The longitude.
	 */
	public static double getLonFromXPix(double XPix, int zoomLevel) {
		double s = getTotalMapSizeInPixel(zoomLevel);
		return 360 * ((fit(XPix, 0, s-1) / s) - 0.5);
	}

	/**
	 * @param lat The latitude.
	 * @param zoomLevel The zoom level.
	 * @return The Y pixel coordinate.
	 */
	public static double getYPixFromLat(double lat, int zoomLevel) {
		double sin = Math.sin(  fit(lat, -PHI_MAX_DEG, PHI_MAX_DEG) * degToRadFactor);
		double y = 0.5 - Math.log((1 + sin) / (1 - sin)) / (4 * Math.PI);
		int s = getTotalMapSizeInPixel(zoomLevel);
		return fit(s*y+0.5, 0, s-1);
	}

	/**
	 * @param YPix The Y pixel coordinate.
	 * @param zoomLevel The zoom level.
	 * @return The latitude.
	 */
	public static double getLatFromYPix(double YPix, int zoomLevel) {
		double s = getTotalMapSizeInPixel(zoomLevel);
		double y = 0.5 - (fit(YPix, 0, s-1) / s);
		return 90 - 360 * Math.atan(Math.exp(-y*2*Math.PI)) / Math.PI;
	}


	// conversions between (XPix, YPix) and (XGeo, YGeo)

	/**
	 * @param xGeo The X geo coordinate.
	 * @param zoomLevel The zoom level.
	 * @return The X pixel coordinate.
	 */
	public static double getXPixFromXGeo(double xGeo, int zoomLevel) {
		return getXPixFromLon(getLon(xGeo), zoomLevel);
	}

	/**
	 * @param xPix The X pixel coordinate.
	 * @param zoomLevel The zoom level.
	 * @return The X geo coordinate.
	 */
	public static double getXGeoFromXPix(double xPix, int zoomLevel) {
		return getXGeo(getLonFromXPix(xPix, zoomLevel));
	}

	/**
	 * @param yGeo The Y geo coordinate.
	 * @param zoomLevel The zoom level.
	 * @return The Y pixel coordinate.
	 */
	public static double getYPixFromYGeo(double yGeo, int zoomLevel) {
		return getYPixFromLat(getLat(yGeo), zoomLevel);
	}

	/**
	 * @param yPix The Y pixel coordinate.
	 * @param zoomLevel The zoom level.
	 * @return The Y geo coordinate.
	 */
	public static double getYGeoFromYPix(double yPix, int zoomLevel) {
		return getYGeo(getLatFromYPix(yPix, zoomLevel));
	}



	/**
	 * Ensure x is within min and max.
	 * 
	 * @param x
	 * @param min
	 * @param max
	 * @return
	 */
	private static double fit(double x, double min, double max) { return Math.min(Math.max(x, min), max); }

	/**
	 * @param zoomLevel The zoom level.
	 * @return The total map size in pixel.
	 */
	private static int getTotalMapSizeInPixel(int zoomLevel) { return 256 << zoomLevel; }

	/**
	 * @param zoomLevel The zoom level.
	 * @return The pixel size in meters at the equator.
	 */
	public static double getPixelSizeEqu(int zoomLevel) {
		return 2*Math.PI*EARTH_RADIUS_M / getTotalMapSizeInPixel(zoomLevel);
	}

	/**
	 * @param lat The latitude.
	 * @return Deformation factor from the projection (depends only on the latitude). To retrieve the real distance from a projected one, multiply by this factor.
	 */
	public static double getDeformationFactor(double lat) {
		return Math.abs( Math.cos( fit(degToRadFactor*lat, -PHI_MAX_RAD, PHI_MAX_RAD) ) );
	}

	/**
	 * @param lat The latitude.
	 * @param zoomLevel The zoom level.
	 * @return The pixel size in meters at a given lat.
	 */
	public static double getPixelSize(double lat, int zoomLevel) {
		return getDeformationFactor(lat) * getPixelSizeEqu(zoomLevel);
	}

	/**
	 * The screen pixel size (in m)
	 */
	public final static double METERS_PER_PIXEL = 0.02540005/Toolkit.getDefaultToolkit().getScreenResolution();

	/**
	 * @param lat The latitude.
	 * @param zoomLevel The zoom level.
	 * @return The scale (the S of 1:S).
	 */
	public static double getScale(double lat, int zoomLevel) { return getPixelSize(lat, zoomLevel) / METERS_PER_PIXEL; }



	public enum CRSType { GEOG, CARTO, UNKNOWN }

	private static CRSType getCRSType(Unit<?> unit) {
		if(unit == null) return CRSType.UNKNOWN;
		switch (unit.toString()) {
		case "": return CRSType.UNKNOWN;
		case "°": return CRSType.GEOG;
		case "m": return CRSType.CARTO;
		default:
			LOGGER.warn("Unexpected unit of measure for projection: "+unit);
			return CRSType.UNKNOWN;
		}
	}
	public static CRSType getCRSType(CoordinateReferenceSystem crs) {
		return getCRSType(CRSUtilities.getUnit(crs.getCoordinateSystem()));
	}
	public static CRSType getCRSType(int epsg) {
		if(epsg==-1) return CRSType.UNKNOWN;
		return getCRSType(getCRS(epsg));
	}


	public static int getEPSGCode(CoordinateReferenceSystem crs) {
		try {
			for(ReferenceIdentifier ri : crs.getIdentifiers()) {
				if("EPSG".equals(ri.getCodeSpace()))
					return Integer.parseInt(ri.getCode());
			}
		} catch (NumberFormatException e) {}
		//LOGGER.warn("Could not find EPSG code for CRS: "+crs.toWKT());
		return -1;
	}

	/*
	public static void main(String[] args) {
		for(int epsg : new int[]{3035,4326,4258,3857}) {
			//System.out.println(getCRSType(epsg));
			CoordinateReferenceSystem crs = getCRS(epsg);
			System.out.println(epsg+" = "+getEPSGCode(crs));
		}
	}
	 */

}
