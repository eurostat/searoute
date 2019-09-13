/**
 * 
 */
package org.opencarto.algo.graph;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opencarto.datamodel.graph.Edge;

/**
 * @author julien Gaffuri
 *
 */
public class EdgeValidity {
	private final static Logger LOGGER = Logger.getLogger(EdgeValidity.class.getName());

	//check edge validity, that is:
	// - it does not self intersects (it is "simple")
	// - it does not intersects another edge
	public static boolean get(Edge e, boolean checkIsSimple, boolean checkEdgeToEdgeIntersection) {
		LineString g = e.getGeometry();

		if(g==null) return false;
		if(g.isEmpty()) return false;
		//if(!g.isValid()) return false; //unnecessary, since it is also tested in isSimple() method
		if(checkIsSimple && !g.isSimple()) return false;

		if(checkEdgeToEdgeIntersection){
			//check face does not overlap other edges
			Envelope env = g.getEnvelopeInternal();
			for(Edge e_ : (Collection<Edge>)e.getGraph().getEdgesAt(env)){
				if(e==e_) continue;
				LineString g2 = e_.getGeometry();

				if(g2==null || g2.isEmpty()) {
					LOGGER.warn("Null/empty geometry found for edge "+e_.getId());
					continue;
				}
				if(!g2.getEnvelopeInternal().intersects(env)) continue;

				try {
					//improve speed by using right geometrical predicate. crosses? overlap?
					//if(!g2.intersects(g)) continue;
					//if(g2.touches(g)) continue;
					//if(!g2.overlaps(g)) continue;

					//analyse intersection
					Geometry inter = g.intersection(g2);
					if(inter.isEmpty()) continue;
					if(inter.getLength()>0)
						return false;
					for(Coordinate c : inter.getCoordinates()){
						if( c.distance(e.getN1().getC())==0 || c.distance(e.getN2().getC())==0 ) continue;
						return false;
					}

					return false;
				} catch (Exception e1){ return false; }
			}
		}

		return true;
	}

}
