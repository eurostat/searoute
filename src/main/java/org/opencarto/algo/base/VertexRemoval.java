package org.opencarto.algo.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opencarto.util.JTSGeomUtil;

/**
 * @author julien gaffuri
 *
 */
public class VertexRemoval {

	public static MultiPolygon[] remove(MultiPolygon g1, MultiPolygon g2, ArrayList<Coordinate> cs) {
		//first, try to remove all
		MultiPolygon g1_=g1, g2_=g2;
		for(Coordinate c:cs){
			g1_ = (MultiPolygon)remove(g1_,c);
			g2_ = (MultiPolygon)remove(g2_,c);
		}
		if(g1_.isValid() && g2_.isValid()) return new MultiPolygon[]{g1_,g2_};

		//try to remove one by one
		g1_=g1; g2_=g2;
		ArrayList<Coordinate> cs_ = new ArrayList<Coordinate>();
		cs_.addAll(cs);
		Object[] o = tryToRemove(g1_,g2_,cs_);
		while(o!=null){
			cs_.remove((Coordinate)o[0]);
			g1_ = (MultiPolygon)o[1];
			g2_ = (MultiPolygon)o[2];
			o = tryToRemove(g1_,g2_,cs_);
		}
		return new MultiPolygon[]{g1_,g2_};
	}

	private static Object[] tryToRemove(Geometry g1, Geometry g2, ArrayList<Coordinate> cs) {
		Collections.shuffle(cs);
		for(Coordinate c:cs){
			Geometry g1_ = remove(g1,c);
			if(!g1_.isValid()) continue;
			Geometry g2_ = remove(g2,c);
			if(!g2_.isValid()) continue;
			return new Object[]{c,g1_,g2_};
		}
		return null;
	}

	/*private static Object[] tryToRemove(Geometry g1, Geometry g2, ArrayList<Coordinate> cs, int nb) {
		Collections.shuffle(cs);
		for(Coordinate c:cs){
			Geometry g1_ = remove(g1,c);
			if(!g1_.isValid()) continue;
			Geometry g2_ = remove(g2,c);
			if(!g2_.isValid()) continue;
			return new Object[]{c,g1_,g2_};
		}
		return null;
	}*/

	public static Geometry remove(Geometry g, ArrayList<Coordinate> cs) {
		ArrayList<Coordinate> cs_=new ArrayList<Coordinate>();
		cs_.addAll(cs);
		Object[] o=tryToRemove(g,cs_);
		while(o!=null){
			cs_.remove((Coordinate)o[0]);
			g=(Geometry)o[1];
			o=tryToRemove(g,cs_);
		}
		return g;
	}

	private static Object[] tryToRemove(Geometry g, ArrayList<Coordinate> cs) {
		Collections.shuffle(cs);
		for(Coordinate c:cs){
			Geometry g2 = remove(g,c);
			if(!g2.isValid()) continue;
			return new Object[]{c,g2};
		}
		return null;
	}

	public static Geometry remove(Geometry g, Coordinate c) {
		if(g instanceof Point)
			return remove((Point)g, c);
		if(g instanceof LineString)
			return remove((LineString)g, c);
		if(g instanceof LinearRing)
			return remove((LinearRing)g, c);
		if(g instanceof Polygon)
			return remove((Polygon)g, c);
		else if(g instanceof MultiPolygon)
			return remove((MultiPolygon)g, c);
		else if(g instanceof GeometryCollection)
			return remove((GeometryCollection)g, c);
		else
			System.err.println("Method not supported for geometry type "+g.getClass().getSimpleName());
		return null;
	}

	private static Point remove(Point p, Coordinate c) {
		if(isIn(p.getCoordinates(), c))
			return null;
		return p;
	}

	private static Coordinate[] remove(Coordinate[] cs, Coordinate c) {
		if(!isIn(cs, c)) return cs;
		ArrayList<Coordinate> cs_=new ArrayList<Coordinate>();
		for(Coordinate c_:cs)
			if(c_.x!=c.x || c_.y!=c.y) cs_.add(c_);
		return cs_.toArray(new Coordinate[cs_.size()]);
	}

	private static LineString remove(LineString ls, Coordinate c) {
		if(!isIn(ls.getCoordinates(), c)) return ls;
		Coordinate[] cs = remove(ls.getCoordinates(), c);
		if(cs.length<=1) return null;
		return new GeometryFactory().createLineString(remove(ls.getCoordinates(),c));
	}

	private static LinearRing remove(LinearRing lr, Coordinate c) {
		if(!isIn(lr.getCoordinates(), c)) return lr;
		Coordinate[] cs = remove(lr.getCoordinates(), c);
		if(cs.length==lr.getCoordinates().length-2){
			//first and last coordinate have been removed: Close cs!
			Coordinate[] cs_=new Coordinate[cs.length+1];
			for(int i=0;i<cs.length;i++) cs_[i]=cs[i];
			cs_[cs.length]=cs[0];
			cs=cs_;
		}
		if(cs.length<=3) return null;
		return new GeometryFactory().createLinearRing(cs);
	}

	private static Polygon remove(Polygon p, Coordinate c) {
		if(!isIn(p.getCoordinates(), c)) return p;
		LinearRing shell = remove((LinearRing)p.getExteriorRing(), c);
		if(shell==null) return null;
		ArrayList<LinearRing> holes = new ArrayList<LinearRing>();
		for(int i=0; i<p.getNumInteriorRing(); i++){
			LinearRing hole = remove((LinearRing)p.getInteriorRingN(i), c);
			if(hole==null || hole.isEmpty()) continue;
			holes.add(hole);
		}
		return p.getFactory().createPolygon((LinearRing)shell, holes.toArray(new LinearRing[holes.size()]));
	}

	//return poly or line
	private static MultiPolygon remove(MultiPolygon mp, Coordinate c) {
		if(!isIn(mp.getCoordinates(), c)) return mp;
		Collection<Geometry> geoms = JTSGeomUtil.getGeometries(mp);
		ArrayList<Polygon> polysOut = new ArrayList<Polygon>();
		for(Geometry g:geoms){
			Polygon p = (Polygon)g;
			if(!isIn(g.getCoordinates(), c)) {
				polysOut.add(p);
				continue;
			}
			Geometry gOut = remove(p,c);
			if(gOut == null) continue;
			if(gOut.isEmpty()) continue;
			if(!(gOut instanceof Polygon)) continue;
			polysOut.add((Polygon)gOut);
		}
		if(polysOut.size()==0) return mp.getFactory().createMultiPolygon(new Polygon[]{});
		return mp.getFactory().createMultiPolygon( polysOut.toArray(new Polygon[polysOut.size()]) );
	}

	private static Geometry remove(GeometryCollection gcl, Coordinate c) {
		if(!isIn(gcl.getCoordinates(), c)) return gcl;
		Collection<Geometry> geoms = JTSGeomUtil.getGeometries(gcl);
		ArrayList<Geometry> geomsOut = new ArrayList<Geometry>();
		for(Geometry g:geoms){
			if(!isIn(g.getCoordinates(), c)) {
				geomsOut.add(g);
				continue;
			}
			Geometry gOut = remove(g,c);
			if(gOut!=null && !gOut.isEmpty()) geomsOut.add(gOut);
		}
		if(geomsOut.size()==0) return gcl.getFactory().createGeometryCollection(new Geometry[]{});
		if(geomsOut.size()==1) return geomsOut.get(0);
		return gcl.getFactory().createGeometryCollection( geomsOut.toArray(new Geometry[geomsOut.size()]) );
	}

	private static boolean isIn(Coordinate[] cs, Coordinate c){
		for(Coordinate c_:cs)
			if(c_.x==c.x && c_.y==c.y) return true;
		return false;
	}

}
