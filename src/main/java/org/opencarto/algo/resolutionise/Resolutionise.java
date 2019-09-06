/**
 * 
 */
package org.opencarto.algo.resolutionise;

import java.util.Collection;
import java.util.HashSet;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.opencarto.algo.base.Copy;
import org.opencarto.algo.base.Union;
import org.opencarto.util.JTSGeomUtil;

/**
 * @author julien Gaffuri
 *
 */
public class Resolutionise {
	/*public Puntal puntal = null;
	public Lineal lineal = null;
	public Polygonal polygonal = null;*/

	public static  Geometry getSimple(Geometry g, double resolution) {
		GeometryFactory gf = g.getFactory();

		if(g instanceof Point) {
			Geometry out = Copy.perform(g);
			apply(out.getCoordinates(), resolution);
			return out;
		}
		else if(g instanceof MultiPoint){
			Geometry out = Copy.perform(g);
			apply(out.getCoordinates(), resolution);
			return out.union();
		} else if(g instanceof LineString) {
			Geometry out = Copy.perform(g);
			apply(out.getCoordinates(), resolution);
			out = out.union();
			LineMerger merger = new LineMerger();
			merger.add(out);
			return gf.buildGeometry( merger.getMergedLineStrings() );
		} else if(g instanceof MultiLineString) {
			Geometry out = Copy.perform(g);
			apply(out.getCoordinates(), resolution);
			out = out.union();
			LineMerger merger = new LineMerger();
			merger.add(out);
			out = gf.buildGeometry( merger.getMergedLineStrings() );
			apply(out.getCoordinate(), resolution);
			out = out.union();
			merger = new LineMerger();
			merger.add(out);
			out = gf.buildGeometry( merger.getMergedLineStrings() );
			return out;
		} else if(g instanceof Polygon) {
			Geometry shellRes = getSimple(((Polygon)g).getExteriorRing(), resolution);
			Polygon p = gf.createPolygon(shellRes.getCoordinates());
			//TODO remove holes one by one
			//p = p.buffer(0);
			return p;
			//return out.buffer(0);
		} else if(g instanceof MultiPolygon) {
			MultiPolygon mp = (MultiPolygon)g;
			HashSet<Geometry> polys = new HashSet<Geometry>();
			for(int i=0; i<mp.getNumGeometries(); i++){
				Polygon p = (Polygon) mp.getGeometryN(i);
				Geometry pRes = getSimple(p, resolution);
				if(pRes.getArea()==0) continue;
				polys.addAll(JTSGeomUtil.getGeometries(pRes));
			}
			mp = gf.createMultiPolygon(polys.toArray(new Polygon[polys.size()]));
			return mp;
		}
		System.out.println("Resolutionise non implemented yet for geometry type: "+g.getGeometryType());
		return null;
	}



	/*
	public Resolutionise(Geometry g, double resolution){
		GeometryFactory gf = g.getFactory();

		if(g instanceof Point){
			//simply create point with rounded coordinates
			puntal = gf.createPoint(get(g.getCoordinate(), resolution));
		} else if(g instanceof MultiPoint) {
			//remove duplicates from rounded coordinates
			puntal = gf.createMultiPoint( removeDuplicates( get(g.getCoordinates(), resolution) ));
		} else if(g instanceof LineString) {
			//round coordinates and remove consecutive duplicates
			Coordinate[] cs = removeConsecutiveDuplicates(get(g.getCoordinates(), resolution));
			if(cs.length == 1)
				//line shrinked to point
				puntal = gf.createPoint(cs[0]);
			else {
				//generate resolutionised line
				Geometry line = gf.createLineString(cs);
				line = line.union(line);
				LineMerger merger = new LineMerger();
				merger.add(line);
				lineal = (Lineal) gf.buildGeometry( merger.getMergedLineStrings() );
			}
		} else if(g instanceof MultiLineString) {
			MultiLineString g_ = (MultiLineString)g;
			int nb = g_.getNumGeometries();

			//compute resolusionise of each component
			Resolutionise[] res = new Resolutionise[g_.getNumGeometries()];
			for(int i=0; i<nb; i++)
				res[i] = new Resolutionise(g_.getGeometryN(i), resolution);

			//store puntals
			for(int i=0; i<nb; i++) {
				if(res[i].puntal != null) puntal = puntal==null? res[i].puntal : (Puntal)((Geometry) puntal).union((Geometry) res[i].puntal);
			}

			//use linemerger for lineals
			LineMerger merger = new LineMerger();
			for(int i=0; i<nb; i++)
				if(res[i].lineal != null) merger.add((Geometry) res[i].lineal);
			Geometry out = gf.buildGeometry( merger.getMergedLineStrings() );
			if(!out.isEmpty()) {
				lineal = (Lineal)out;
				//union
				lineal = (Lineal) ((Geometry) lineal).union();

				/*if(lineal instanceof MultiLineString){
					//since lineal components could overlap, do another resolutionise
					g_ = (MultiLineString)lineal;
					nb = g_.getNumGeometries();
					res = new Resolutionise[g_.getNumGeometries()];
					for(int i=0; i<nb; i++)
						res[i] = new Resolutionise(g_.getGeometryN(i), resolution);

					//use linemerger again
					merger = new LineMerger();
					for(int i=0; i<nb; i++)
						merger.add((Geometry) res[i].lineal);
					lineal = (Lineal) gf.buildGeometry( merger.getMergedLineStrings() );
					//union
					lineal = (Lineal) ((Geometry) lineal).union();
				}/

				//complement puntal with lineal to ensure puntal does not intersect lineal
				if(puntal != null) puntal = (Puntal) ((Geometry)puntal).difference((Geometry)lineal);
			}
		} else if(g instanceof Polygon) {
			LineString er = ((Polygon) g).getExteriorRing();
			Resolutionise resEr = new Resolutionise(er, resolution);
			if(resEr.puntal!=null)
				//polygon shrinked to point
				puntal = resEr.puntal;
			else if(resEr.lineal instanceof LineString && ((LineString)resEr.lineal).isClosed()) {
				//some polygon part left...
				Polygon g_ = (Polygon)g;
				LinearRing shell = gf.createLinearRing(((Geometry)resEr.lineal).getCoordinates());
				LinearRing[] holes = new LinearRing[g_.getNumInteriorRing()];
				for(int i=0; i<g_.getNumInteriorRing(); i++){
					Resolutionise holeRes = new Resolutionise(g_.getInteriorRingN(i), resolution);
					Coordinate[] cs = holeRes.lineal!=null ? ((Geometry)holeRes.lineal).getCoordinates() : new Coordinate[0];
					holes[i] = gf.createLinearRing(cs);
				}
				polygonal = (Polygonal) gf.createPolygon(shell, holes).buffer(0);
			} else {
				//TODO polygon to line case
				//polygon shrinked to line
				lineal = resEr.lineal;
			}
		} else if(g instanceof MultiPolygon) {
			System.out.println("Resolutionise non implemented yet for MultiPolygon");
		} else {
			System.out.println("Resolutionise non implemented yet for geometry type: "+g.getGeometryType());
		}
	}*/

	/*/return result as a geometry collection
	public Geometry getGeometryCollection() {
		Geometry geom = null;
		if(polygonal !=null ) geom = geom==null? (Geometry)polygonal : geom.union((Geometry)polygonal);
		if(lineal !=null ) geom = geom==null? (Geometry)lineal : geom.union((Geometry)lineal);
		if(puntal != null) geom = geom==null? (Geometry) puntal : geom.union((Geometry)puntal);
		return geom;
	}*/


	//case of linear geometries

	public static Collection<LineString> applyLinear(LineString line, double resolution) {
		apply(line, resolution);
		return resRemoveDuplicateCoordsLinear(line);
	}

	public static Collection<LineString> resRemoveDuplicateCoordsLinear(LineString line) {
		if(line.getLength() == 0) return new HashSet<>();
		Collection<LineString> line_ = new HashSet<>(); line_.add(line);
		Geometry u = Union.getLineUnion(line_);
		return JTSGeomUtil.getLineStrings(u);
	}

	public static Collection<LineString> applyLinear(Collection<LineString> lines, double resolution) {
		Collection<LineString> out = new HashSet<>();
		for(LineString line : lines)
			out.addAll(applyLinear(line, resolution));
		return out;
	}







	//base functions

	/*public static Coordinate get(Coordinate c, double resolution){
		return new Coordinate(
				Math.round(c.x/resolution)*resolution,
				Math.round(c.y/resolution)*resolution
				);
	}
	public static Coordinate[] get(Coordinate[] cs, double resolution){
		Coordinate[] cs_ = new Coordinate[cs.length];
		for(int i=0; i<cs.length; i++) cs_[i] = get(cs[i], resolution);
		return cs_;
	}*/

	public static void apply(Collection<Geometry> gs, double resolution) {
		for(Geometry g : gs) apply(g, resolution);
	}

	public static void apply(Geometry g, double resolution) {
		apply(g.getCoordinates(), resolution);
	}

	public static void apply(Coordinate c, double resolution) {
		c.x = ((int)Math.round(c.x/resolution)) * resolution;
		c.y = ((int)Math.round(c.y/resolution)) * resolution;
	}

	public static void apply(Coordinate[] cs, double resolution) {
		for(Coordinate c : cs) apply(c, resolution);
	}



	/*
	private static boolean samePosition(Coordinate c1, Coordinate c2) { return c1.x==c2.x && c1.y==c2.y; }

	public static Coordinate[] removeDuplicates(Coordinate[] cs){
		ArrayList<Coordinate> csSorted = new ArrayList<Coordinate>(Arrays.asList(cs));
		Collections.sort(csSorted, new Comparator<Coordinate>() {
			public int compare(Coordinate c1, Coordinate c2) { return c1.x>c2.x?1:c1.y>c2.y?1:0; }
		});
		HashSet<Coordinate> cs_ = new HashSet<Coordinate>();
		Coordinate cPrev = null;
		for(Coordinate c : csSorted){
			if(cPrev==null || !samePosition(c,cPrev)) cs_.add(c);
			cPrev=c;
		}
		return cs_.toArray(new Coordinate[cs_.size()]);
	}

	public static Coordinate[] removeConsecutiveDuplicates(Coordinate[] cs){
		ArrayList<Coordinate> cs_ = new ArrayList<Coordinate>();
		Coordinate cPrev = null;
		for(Coordinate c : cs){
			if(cPrev==null || !samePosition(c,cPrev)) cs_.add(c);
			cPrev=c;
		}
		return cs_.toArray(new Coordinate[cs_.size()]);
	}*/

	/*	public static Collection<Geometry> resApplyLines(Collection<Geometry> lines, double res) {
		Resolutionise.apply(lines, res);
		return resRemoveDuplicateCoordsLinear(lines);
	}

	public static Collection<Geometry> resRemoveDuplicateCoordsLinear(Collection<Geometry> lines) {
		Collection<Geometry> out = new HashSet<>();
		for(Geometry line : lines) {
			if(line.getLength() == 0) continue;
			Collection<Geometry> line_ = new HashSet<>(); line_.add(line);
			Geometry u = Union.getLineUnion(line_);
			if(u.isEmpty()) continue;
			if(u instanceof Point) continue;
			out.addAll(JTSGeomUtil.getLineStringGeometries(u));
		}
		return out;
	}

	public static void main(String[] args) {
		//tests
		//TODO extract as true tests

		GeometryFactory gf = new GeometryFactory();

		//points
		Point pt;
		pt = gf.createPoint(new Coordinate(107.4, 502.78));
		System.out.println(pt);
		System.out.println(new Resolutionise(pt,1).puntal);
		System.out.println(new Resolutionise(pt,10).puntal);
		System.out.println(new Resolutionise(pt,100).puntal);

		pt = gf.createPoint(new Coordinate(87.5, 502.78));
		System.out.println(pt);
		System.out.println(new Resolutionise(pt,1).puntal);
		System.out.println(new Resolutionise(pt,10).puntal);
		System.out.println(new Resolutionise(pt,100).puntal);*/

	//multipoint
	/*MultiPoint pt;
		pt = gf.createMultiPoint(new Coordinate[] {new Coordinate(107.4, 502.78), new Coordinate(117.4, 500), new Coordinate(487.4, 1402.78)});
		System.out.println(pt);
		System.out.println(new Resolutionise(pt,1).puntal);
		System.out.println(new Resolutionise(pt,10).puntal);
		System.out.println(new Resolutionise(pt,100).puntal);
		System.out.println(new Resolutionise(pt,1000).puntal);*/

	/*/linestring
		LineString ls;
		ls = gf.createLineString(getCoordsArray(107.4, 502.78, 117.4, 500, 487.4, 1402.78));
		System.out.println(ls);
		System.out.println(new Resolutionise(ls,1).lineal);
		System.out.println(new Resolutionise(ls,10).lineal);
		System.out.println(new Resolutionise(ls,100).lineal);
		System.out.println(new Resolutionise(ls,1000).puntal);
		System.out.println("-------");
		ls = gf.createLineString(getCoordsArray(107.4, 502.78, 117.4, 504, 120.4, 490, 107.4, 503));
		System.out.println(ls);
		System.out.println(new Resolutionise(ls,1).lineal);
		System.out.println(new Resolutionise(ls,10).lineal);
		System.out.println(new Resolutionise(ls,100).puntal);
		System.out.println(new Resolutionise(ls,1000).lineal);
		System.out.println("-------");
		ls = gf.createLineString(getCoordsArray(0, 0, 1000,509, 1000, 500, 0, 1));
		System.out.println(ls);
		System.out.println(new Resolutionise(ls,10).lineal);
		System.out.println(new Resolutionise(ls,100).lineal);
		System.out.println("-------");
		ls = gf.createLineString(getCoordsArray(0, 1, 1000,1, 1000, 0, 1, 0, 1, -100, 0, -100, 0, 0));
		System.out.println(ls);
		System.out.println(new Resolutionise(ls,1).lineal);
		System.out.println(new Resolutionise(ls,10).lineal);
		System.out.println(new Resolutionise(ls,100).lineal);

		System.out.println("-------");
		ls = gf.createLineString(getCoordsArray(-10,0, 0,0, 0,-10, 1,-10, 1,0, 10,0, 10,1, -10,1, -10, 0));
		System.out.println(ls);
		System.out.println(new Resolutionise(ls,10).lineal);
		System.out.println(new Resolutionise(ls,100).puntal);
	}*/

	/*
	public static Coordinate[] getCoordsArray(double ... data){
		Coordinate[] cs = new Coordinate[data.length/2];
		for(int i=0; i<data.length/2; i++) cs[i] = new Coordinate(data[i*2],data[i*2+1]);
		return cs;
	}
	 */

	/*
	public static  Geometry get(Geometry g, double resolution) {
		Geometry out = Copy.perform(g);

		apply(out.getCoordinates(), resolution);

		if(out instanceof Point) ;
		else if(out instanceof MultiPoint) ;
		else if(out instanceof LineString) ;
		else if(out instanceof MultiLineString) ;
		else if(out instanceof Polygon) out = out.buffer(0);
		else if(out instanceof MultiPolygon) out = out.buffer(0);

		return out;
	}*/




	//round geometry coordinates
	/*public static void round(Geometry geom, int decimalNb) {
		CoordinateSequence cs = null;
		if (geom instanceof Point) cs = ((Point)geom).getCoordinateSequence();
		else if (geom instanceof LineString) cs = ((LineString)geom).getCoordinateSequence();
		else if (geom instanceof Polygon) {
			Polygon poly = (Polygon)geom;
			round( poly.getExteriorRing(), decimalNb);
			for(int i=0; i<poly.getNumInteriorRing() ; i++)
				round( poly.getInteriorRingN(i), decimalNb);				
			return;
		}
		else if (geom instanceof GeometryCollection) {
			GeometryCollection gc = (GeometryCollection)geom;
			for(int i=0; i<gc.getNumGeometries(); i++)
				round(gc.getGeometryN(i), decimalNb);
			return;
		}
		else {
			System.err.println("JTS geometry type not treated: " + geom.getClass().getSimpleName());
			return;
		}

		//round the coordinates
		for(int i=0; i<cs.size(); i++) {
			Coordinate c = cs.getCoordinate(i);
			cs.setOrdinate(i, 0, Util.round(c.x, decimalNb));
			cs.setOrdinate(i, 1, Util.round(c.y, decimalNb));
			if(!Double.isNaN(c.z)) cs.setOrdinate(i, 2, Util.round(c.z, decimalNb));
		}
		geom.geometryChanged();
	}*/

}
