package org.opencarto.algo.distances;

import org.locationtech.jts.geom.Geometry;
import org.opencarto.datamodel.Feature;
import org.opencarto.util.ProjectionUtil;

/**
 * Distance for simple features.
 * 
 * @author julien Gaffuri
 *
 */
public class FeatureDistance implements Distance<Feature> {
	private boolean projected = true;

	public FeatureDistance() {}
	public FeatureDistance(boolean projected){ this.projected = projected; }

	public double get(Feature f1, Feature f2) {
		Geometry g1 = f1.getGeom();
		Geometry g2 = f2.getGeom();
		if(!projected){
			g1 = ProjectionUtil.toWebMercator(g1, ProjectionUtil.getWGS_84_CRS());
			g2 = ProjectionUtil.toWebMercator(g2, ProjectionUtil.getWGS_84_CRS());
		}
		//System.out.println(g1.distance(g2));
		return g1.distance(g2);
	}

}
