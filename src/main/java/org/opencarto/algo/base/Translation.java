/**
 * 
 */
package org.opencarto.algo.base;

import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * 
 * @author julien Gaffuri
 *
 */
public class Translation {
	private static Logger logger = Logger.getLogger(Translation.class.getName());


	public static Coordinate get(Coordinate coord, double dx, double dy){
		return new Coordinate(coord.x+dx, coord.y+dy);
	}

	public static Coordinate[] get(Coordinate[] coords, double dx, double dy){
		Coordinate[] coord_= new Coordinate[coords.length];
		for(int i=0; i<coords.length; i++) coord_[i] = get(coords[i], dx, dy);
		return coord_;
	}


	public static Point get(Point geom, double dx, double dy, GeometryFactory gf){
		return gf.createPoint( get(geom.getCoordinate(), dx, dy) );
	}


	public static LineString get(LineString geom, double dx, double dy, GeometryFactory gf){
		return gf.createLineString(get(geom.getCoordinates(), dx, dy));
	}

	public static LinearRing get(LinearRing geom, double dx, double dy, GeometryFactory gf){
		return gf.createLinearRing(get(geom.getCoordinates(), dx, dy));
	}


	public static Polygon get(Polygon geom, double dx, double dy, GeometryFactory gf){
		LinearRing lr = get((LinearRing)geom.getExteriorRing(), dx, dy, gf);
		LinearRing[] lr_ = new LinearRing[geom.getNumInteriorRing()];
		for(int j=0; j<geom.getNumInteriorRing(); j++) lr_[j] = get((LinearRing)geom.getInteriorRingN(j), dx, dy, gf);
		return gf.createPolygon(lr, lr_);
	}


	public static GeometryCollection get(GeometryCollection geomCol, double dx, double dy, GeometryFactory gf){
		Geometry[] gs = new Geometry[geomCol.getNumGeometries()];
		for(int i=0; i< geomCol.getNumGeometries(); i++) gs[i] = get(geomCol.getGeometryN(i), dx, dy, gf);
		return gf.createGeometryCollection(gs);
	}

	public static Geometry get(Geometry geom, double dx, double dy){
		return get(geom, dx, dy, geom.getFactory());
	}
	public static Geometry get(Geometry geom, double dx, double dy, GeometryFactory gf){
		if(geom instanceof Point) return get((Point)geom, dx, dy, gf);
		else if(geom instanceof Polygon) return get((Polygon)geom, dx, dy, gf);
		else if(geom instanceof LineString) return get((LineString)geom, dx, dy, gf);
		else if(geom instanceof LinearRing) return get((LinearRing)geom, dx, dy, gf);
		logger.warning("Translation of " + geom.getClass().getSimpleName() + " not supported yet.");
		return null;
	}
}
