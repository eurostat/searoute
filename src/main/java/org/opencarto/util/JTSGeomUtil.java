package org.opencarto.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.linemerge.LineMerger;

public class JTSGeomUtil {
	public final static Logger LOGGER = Logger.getLogger(JTSGeomUtil.class.getName());



	//easy and quick creation of geometries, mainly for testing purposes
	public static Coordinate[] createCoordinates(double... cs) {
		Coordinate[] cs_ = new Coordinate[cs.length/2];
		for(int i=0; i<cs_.length; i++) cs_[i] = new Coordinate(cs[2*i],cs[2*i+1]);
		return cs_;
	}
	public static LineString createLineString(double... cs) { return new GeometryFactory().createLineString(createCoordinates(cs)); }
	public static Polygon createPolygon(double... cs) { return new GeometryFactory().createPolygon(createCoordinates(cs)); }


	//clean geometry
	public static Geometry clean(Geometry geom) {
		if(geom instanceof MultiPolygon || geom instanceof Polygon)
			return geom.buffer(0);
		if(geom instanceof MultiLineString || geom instanceof LineString){
			LineMerger lm = new LineMerger();
			lm.add(geom);
			@SuppressWarnings("unchecked")
			ArrayList<LineString> ml = (ArrayList<LineString>) lm.getMergedLineStrings();
			if(ml.size()==1) return (Geometry)ml.iterator().next();
			return geom.getFactory().createMultiLineString( (LineString[])ml.toArray(new LineString[ml.size()]) );
		}
		if(geom instanceof MultiPoint)
			//TODO not tested
			return geom.union(geom);
		if(geom instanceof GeometryCollection)
			//TODO not tested
			return geom.union(geom);
		return geom;
	}


	//intersection test for geometry collections
	public static boolean intersects(Geometry geom1, Geometry geom2){
		if(!(geom1 instanceof GeometryCollection) && !(geom2 instanceof GeometryCollection))
			return geom1.intersects(geom2);

		Collection<Geometry> geoms1 = getGeometries(geom1);
		Collection<Geometry> geoms2 = getGeometries(geom2);

		for(Geometry g1 : geoms1)
			for(Geometry g2 : geoms2)
				if(g1.intersects(g2))
					return true;
		return false;
	}


	//build geometry from envelope
	public static Polygon getGeometry(Envelope env) {
		Coordinate[] cs = new Coordinate[]{new Coordinate(env.getMinX(),env.getMinY()), new Coordinate(env.getMaxX(),env.getMinY()), new Coordinate(env.getMaxX(),env.getMaxY()), new Coordinate(env.getMinX(),env.getMaxY()), new Coordinate(env.getMinX(),env.getMinY())};
		return new GeometryFactory().createPolygon(cs);
	}
	public static LineString getBoundary(Envelope env) { return getGeometry(env).getExteriorRing(); }

	public static Collection<Point> getPointsFromCoordinates(Collection<Coordinate> cs) {
		Collection<Point> out = new ArrayList<Point>();
		GeometryFactory gf = new GeometryFactory();
		for(Coordinate c : cs) out.add(gf.createPoint(c));
		return out;
	}

	//retrieve some geometries close to a position, without index
	public static <T extends Geometry> Collection<Geometry> getGeometriesCloseTo(Coordinate c, Collection<T> geoms, double squareDistance) {
		Collection<Geometry> out = new HashSet<Geometry>();
		Envelope env = new Envelope(c.x-squareDistance, c.x+squareDistance, c.y-squareDistance, c.y+squareDistance);
		for(Geometry geom : geoms)
			if(geom.getEnvelopeInternal().intersects(env)) out.add(geom);
		return out;
	}


	//get polygon rings
	public static Collection<LineString> getRings(Polygon p){
		Collection<LineString> lrs = new HashSet<LineString>();
		lrs.add(p.getExteriorRing());
		for(int i=0; i<p.getNumInteriorRing(); i++)
			lrs.add(p.getInteriorRingN(i));
		return lrs;
	}

	//test if env contains entirely env2, excluding the boundary. That is the boundaries do not intersect
	public static boolean containsSFS(Envelope env, Envelope env2) {
		if(env2.getMaxX() >= env.getMaxX()) return false;
		if(env2.getMaxY() >= env.getMaxY()) return false;
		if(env2.getMinX() <= env.getMinX()) return false;
		if(env2.getMinY() <= env.getMinY()) return false;
		return true;
	}









	//handle geom collections / multi geoemtry types



	//return list of geometries that are not GeometryCollection
	public static Collection<Geometry> getGeometries(Geometry geom){
		Collection<Geometry> out = new HashSet<Geometry>();
		int nb = geom.getNumGeometries();
		if(nb == 0)
			return out;
		if(nb == 1)
			out.add(geom.getGeometryN(0));
		else
			for(int i=0; i<nb; i++)
				out.addAll( getGeometries(geom.getGeometryN(i)) );
		return out;
	}
	//return list of geometries that are not GeometryCollection
	public static <T extends Geometry> Collection<Geometry> getGeometries(Collection<T> geoms){
		Collection<Geometry> out = new HashSet<Geometry>();
		for(T geom : geoms) out.addAll(getGeometries(geom));
		return out;
	}

	//convert singleton GeometryCollection into non-GeometryCollection
	public static Geometry toSimple(GeometryCollection gc) {
		int nb = gc.getNumGeometries();
		if(nb==0) return gc.getFactory().createPoint();
		if(nb>1) LOGGER.warn("Cannot convert GeometryCollection into non-GeometryCollection. Several component: "+nb+". Around "+gc.getCoordinate());
		return (Geometry) gc.getGeometryN(0);
	}
	public static Point toSimple(MultiPoint mp) {
		int nb = mp.getNumGeometries();
		if(nb==0) return mp.getFactory().createPoint();
		if(nb>1) LOGGER.warn("Cannot convert MultiPoint into Point. Several component: "+nb+". Around "+mp.getCoordinate());
		return (Point) mp.getGeometryN(0);
	}
	public static LineString toSimple(MultiLineString mls) {
		int nb = mls.getNumGeometries();
		if(nb==0) return mls.getFactory().createLineString(new Coordinate[]{});
		if(nb>1) LOGGER.warn("Cannot convert MultiLineString into LineString. Several component: "+nb+". Around "+mls.getCoordinate());
		return (LineString) mls.getGeometryN(0);
	}
	public static Polygon toSimple(MultiPolygon mp) {
		int nb = mp.getNumGeometries();
		if(nb==0) return mp.getFactory().createPolygon(new Coordinate[]{});
		if(nb>1) LOGGER.warn("Cannot convert MultiPolygon into Polygon. Several component: "+nb+". Around "+mp.getCoordinate());
		return (Polygon) mp.getGeometryN(0);
	}

	//get collection form of a geometry
	public static MultiPoint toMulti(Point geom) {
		return geom.getFactory().createMultiPoint(new Point[]{geom});
	}
	public static MultiLineString toMulti(LineString geom) {
		return geom.getFactory().createMultiLineString(new LineString[]{geom});
	}
	public static MultiPolygon toMulti(Polygon geom) {
		return geom.getFactory().createMultiPolygon(new Polygon[]{geom});
	}
	public static GeometryCollection toMulti(Geometry geom){
		if(geom == null)
			return null;
		if(geom.isEmpty())
			return geom.getFactory().createGeometryCollection(new Geometry[]{});
		if(geom instanceof Point)
			return toMulti((Point)geom);
		if(geom instanceof LineString)
			return toMulti((LineString)geom);
		if(geom instanceof Polygon)
			return toMulti((Polygon)geom);
		if(geom instanceof GeometryCollection)
			return (GeometryCollection)geom;
		LOGGER.error("Geom type not handeled: " + geom.getClass().getSimpleName());
		return null;
	}

	//extract only some geometrical primitives
	public static Collection<Polygon> getPolygons(Geometry g) { return getPolygons(g, -1); }
	public static Collection<Polygon> getPolygons(Geometry g, double areaDeletionThreshold) {
		Collection<Polygon> out = new ArrayList<Polygon>();
		for(Geometry g_ : getGeometries(g))
			if(!g_.isEmpty() && g_ instanceof Polygon){
				if(areaDeletionThreshold>0 && g_.getArea()<=areaDeletionThreshold) continue;
				out.add((Polygon)g_);
			}
		return out ;
	}
	public static <T extends Geometry> Collection<Polygon> getPolygons(Collection<T> gs, double areaDeletionThreshold) {
		Collection<Polygon> out = new ArrayList<Polygon>();
		for(T g : gs)
			out.addAll(getPolygons(g, areaDeletionThreshold));
		return out ;
	}

	public static Collection<LineString> getLineStrings(Geometry g) { return getLineStrings(g, -1); }
	public static Collection<LineString> getLineStrings(Geometry g, double lengthDeletionThreshold) {
		Collection<LineString> out = new ArrayList<LineString>();
		for(Geometry g_ : getGeometries(g)) {
			if(!g_.isEmpty() && g_ instanceof LineString) {
				if(lengthDeletionThreshold>0 && g_.getLength()<=lengthDeletionThreshold) continue;
				out.add((LineString)g_);
			}
		}
		return out ;
	}

	public static <T extends Geometry> Collection<LineString> getLineStrings(Collection<T> gs) { return getLineStrings(gs, -1); }
	public static <T extends Geometry> Collection<LineString> getLineStrings(Collection<T> gs, double lengthDeletionThreshold) {
		Collection<LineString> out = new ArrayList<LineString>();
		for(T g : gs)
			out.addAll(getLineStrings(g, lengthDeletionThreshold));
		return out ;
	}

	public static Collection<Point> getPoints(Geometry g) {
		Collection<Point> out = new ArrayList<Point>();
		for(Geometry g_ : getGeometries(g))
			if(!g_.isEmpty() && g_ instanceof Point)
				out.add((Point)g_);
		return out ;
	}
	public static <T extends Geometry> Collection<Point> getPoints(Collection<T> gs) {
		Collection<Point> out = new ArrayList<Point>();
		for(T g : gs)
			out.addAll(getPoints(g));
		return out ;
	}


	//keep only puntual part of a geometry
	public static MultiPoint getPuntual(Geometry g) {
		Collection<Point> pts = getPoints(g);
		if(pts.size()==0) return g.getFactory().createMultiPoint(new Point[]{});
		return g.getFactory().createMultiPoint(pts.toArray(new Point[pts.size()]));
	}

	//keep only linear part of a geometry
	public static MultiLineString getLinear(Geometry g) {
		Collection<LineString> lss = getLineStrings(g);
		if(lss.size()==0) return g.getFactory().createMultiLineString(new LineString[]{});
		return g.getFactory().createMultiLineString(lss.toArray(new LineString[lss.size()]));
	}

	//keep only polygonal part of a geometry
	public static MultiPolygon getPolygonal(Geometry g) {
		Collection<Polygon> mps = getPolygons(g);
		if(mps.size()==0) return g.getFactory().createMultiPolygon(new Polygon[]{});
		return g.getFactory().createMultiPolygon(mps.toArray(new Polygon[mps.size()]));
	}

}
