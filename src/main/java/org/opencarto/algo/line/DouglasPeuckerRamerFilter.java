/**
 * 
 */
package org.opencarto.algo.line;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

/**
 * @author julien Gaffuri
 *
 */
public class DouglasPeuckerRamerFilter {
	static Logger logger = Logger.getLogger(DouglasPeuckerRamerFilter.class.getName());

	public static <T extends Geometry> T get(T g, double d){
		if (d < 0.0) {
			logger.warning("Distance tolerance must be positive: " + d);
			return (T)g.clone();
		}

		if (d == 0.0) return (T)g.clone();

		Geometry g_;
		try {
			g_ = DouglasPeuckerSimplifier.simplify(g, d);
			if( g_ == null || g_.isEmpty() || !g_.isValid() || g_.getGeometryType() != g.getGeometryType() )
				g_ = TopologyPreservingSimplifier.simplify(g, d);
			else
				return (T)g_;
		} catch (Exception e) {
			return (T)g.clone();
		}

		if (g_ == null) {
			logger.warning("Null geometry");
			return (T)g.clone();
		} else if (g_.getGeometryType() != g.getGeometryType()) {
			logger.warning("Different types of geometry");
			//System.out.println(g.getGeometryType() + "   " + g_.getGeometryType());
			return (T)g.clone();
		} else if (!g_.isValid()) {
			logger.info("Non valid geometry");
			return (T)g.clone();
		} else if (g_.isEmpty() ) {
			logger.warning("Empty geometry");
			return (T)g.clone();
		} else return (T)g_;
	}

	public static ArrayList<Coordinate> getCoordinatesToRemove(Geometry geom, double dp) {
		Coordinate[] csToKeep = DouglasPeuckerSimplifier.simplify(geom, dp).getCoordinates();
		//Coordinate[] csToKeep = get(geom, dp).getCoordinates();
		ArrayList<Coordinate> cs = new ArrayList<Coordinate>();
		for(Coordinate c : geom.getCoordinates())
			if(!isIn(csToKeep, c)) cs.add(c);
		return cs;
	}

	private static boolean isIn(Coordinate[] cs, Coordinate c){
		for(Coordinate c_:cs)
			if(c_.x==c.x && c_.y==c.y) return true;
		return false;
	}

	//apply DPR filter to a collection of geometries
	public static <T extends Geometry> Collection<T> get(Collection<T> gs, double d){
		Collection<T> out = new ArrayList<T>();
		for(T g : gs) out.add((T)get(g,d));
		return out;
	}

}
