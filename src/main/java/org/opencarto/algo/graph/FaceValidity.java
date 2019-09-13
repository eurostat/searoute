/**
 * 
 */
package org.opencarto.algo.graph;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;
import org.opencarto.datamodel.graph.Face;

/**
 * @author julien Gaffuri
 *
 */
public class FaceValidity {
	private final static Logger LOGGER = Logger.getLogger(FaceValidity.class.getName());

	//check the face is ok, that is: its geometry is "simple" (no self adjency and internal ring are inside) and it does not overlap other faces
	public static boolean get(Face f, boolean checkIsSimple, boolean checkFaceToFaceOverlap) {
		Polygon g = f.getGeom();

		if(g == null || g.isEmpty()) return false;

		//if(!g.isValid()) return false; //unnecessary, since it is also tested in isSimple() method
		if(checkIsSimple && !g.isSimple()) return false;

		if(checkFaceToFaceOverlap){
			//check face does not overlap other faces
			Envelope env = g.getEnvelopeInternal();
			for(Face f2 : f.getGraph().getFacesAt(env)){
				if(f==f2) continue;
				Polygon g2 = f2.getGeom();

				if(g2==null || g2.isEmpty()) {
					LOGGER.warn("Null/empty geometry found for face "+f2.getId());
					continue;
				}
				if(!g2.getEnvelopeInternal().intersects(env)) continue;

				try {
					//if(!g2.intersects(g)) continue;
					//if(g2.touches(g)) continue;
					if(!g2.overlaps(g)) continue;
					return false;
				} catch (Exception e){ return false; }
			}
		}

		return true;
	}


}
