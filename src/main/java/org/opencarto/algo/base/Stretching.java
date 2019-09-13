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
 * Geometry stretching functions
 * 
 * @author julien Gaffuri
 *
 */
public class Stretching {
	private static Logger logger = Logger.getLogger(Stretching.class.getName());

	public static Point get(Point geom, Coordinate c, double angle, double k, GeometryFactory gf) {
		double a = angle + Math.PI*0.5;
		Coordinate c2 = Rotation.get(geom.getCoordinate(), c, -1.0*a);
		double xc = c.x;
		double x;
		x = c2.x;
		Coordinate c3 = new Coordinate(xc+k*(x-xc), c2.y);
		return Rotation.get( gf.createPoint( c3 ), c, a, gf);
	}

	public static Polygon get(Polygon geom, Coordinate c, double angle, double k, GeometryFactory gf) {
		LinearRing lr = get((LinearRing)geom.getExteriorRing(), c, angle, k, gf);

		LinearRing[] holes = new LinearRing[geom.getNumInteriorRing()];
		for(int j=0; j<geom.getNumInteriorRing(); j++) holes[j] = get((LinearRing)geom.getInteriorRingN(j), c, angle, k, gf);

		return gf.createPolygon(lr, holes);
	}

	public static LinearRing get(LinearRing lr, Coordinate c, double angle, double k, GeometryFactory gf) {
		return gf.createLinearRing(get(lr.getCoordinates(), c, angle, k));
	}

	public static LineString get(LineString ls, Coordinate c, double angle, double k, GeometryFactory gf) {
		return gf.createLineString(get(ls.getCoordinates(), c, angle, k));
	}

	public static Coordinate[] get(Coordinate[] coord, Coordinate c, double angle, double coef) {
		double a = angle + Math.PI*0.5;
		Coordinate[] coord2 = Rotation.get(coord, c, -1.0*a);
		Coordinate[] coord_ = new Coordinate[coord2.length];
		double xc = c.x;
		Coordinate ci;
		double x;
		for(int i=0; i<coord2.length; i++) {
			ci = coord2[i];
			x = ci.x;
			coord_[i] = new Coordinate(xc+coef*(x-xc), ci.y);
		}
		return Rotation.get( coord_, c, a);
	}

	public static GeometryCollection get(GeometryCollection geomCol, Coordinate c, double angle, double k, GeometryFactory gf) {
		Geometry[] geoms = new Geometry[geomCol.getNumGeometries()];
		for(int i=0; i< geomCol.getNumGeometries(); i++) geoms[i] = get(geomCol.getGeometryN(i), c, angle, k, gf);
		return gf.createGeometryCollection(geoms);
	}

	public static Geometry get(Geometry geom, Coordinate c, double angle, double k, GeometryFactory gf) {
		if(geom instanceof Point) return get((Point)geom, c, angle, k, gf);
		else if(geom instanceof Polygon) return get((Polygon)geom, c, angle, k, gf);
		else if(geom instanceof LineString) return get((LineString)geom, c, angle, k, gf);
		else if(geom instanceof LinearRing) return get((LinearRing)geom, c, angle, k, gf);
		logger.warning("Stretching of " + geom.getClass().getSimpleName() + " not supported yet.");
		return null;
	}

}
