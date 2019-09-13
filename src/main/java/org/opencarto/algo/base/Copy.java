/**
 * 
 */
package org.opencarto.algo.base;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * @author julien Gaffuri
 *
 */
public class Copy {

	public static Geometry perform(Geometry g) {
		if(g==null) return null;
		else if (g instanceof Point)
			return g.getFactory().createPoint(perform(g.getCoordinate()));
		else if (g instanceof LinearRing)
			return g.getFactory().createLinearRing(perform(g.getCoordinates()));
		else if (g instanceof LineString)
			return g.getFactory().createLineString(perform(g.getCoordinates()));
		else if (g instanceof Polygon){
			Polygon p = (Polygon)g;
			LinearRing shell = (LinearRing) perform(p.getExteriorRing());
			LinearRing[] holes = new LinearRing[p.getNumInteriorRing()];
			for(int i=0; i<p.getNumInteriorRing(); i++) holes[i] = (LinearRing) perform(p.getInteriorRingN(i));
			return g.getFactory().createPolygon(shell, holes);
		}
		else if (g instanceof MultiPolygon){
			MultiPolygon mp = (MultiPolygon)g;
			Polygon[] gs = new Polygon[mp.getNumGeometries()];
			for(int i=0; i<mp.getNumGeometries(); i++) gs[i] = (Polygon) perform(mp.getGeometryN(i));
			return g.getFactory().createMultiPolygon(gs);
		}
		GeometryCollection gc = (GeometryCollection)g;
		Geometry[] gs = new Geometry[gc.getNumGeometries()];
		for(int i=0; i<gc.getNumGeometries(); i++) gs[i] = perform(gc.getGeometryN(i));
		return g.getFactory().createGeometryCollection(gs);
	}

	public static Coordinate perform(Coordinate c) { return new Coordinate(c.x, c.y); }

	public static Coordinate[] perform(Coordinate[] cs) {
		Coordinate[] cs_ = new Coordinate[cs.length];
		for(int i=0; i<cs.length; i++) cs_[i]=perform(cs[i]);
		return cs_;
	}

}
