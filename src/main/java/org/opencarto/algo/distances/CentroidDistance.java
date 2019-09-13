/**
 * 
 */
package org.opencarto.algo.distances;

import org.locationtech.jts.geom.Geometry;
import org.opencarto.datamodel.Feature;

/**
 * @author julien Gaffuri
 *
 */
public class CentroidDistance implements Distance<Feature> {

	public double get(Feature f1, Feature f2) {
		Geometry g1 = f1.getGeom();
		Geometry g2 = f2.getGeom();
		return g1.getCentroid().distance(g2.getCentroid());
	}

}
