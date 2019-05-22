/**
 * 
 */
package eu.europa.ec.eurostat.searoute;

import java.util.Collection;
import java.util.HashSet;

import org.locationtech.jts.geom.Geometry;
import org.opencarto.algo.graph.ConnexComponents;
import org.opencarto.algo.graph.GraphSimplify;
import org.opencarto.io.GeoJSONUtil;
import org.opencarto.util.FeatureUtil;

/**
 * Some functions to build maritime networks at different resolutions.
 * 
 * @author julien Gaffuri
 *
 */
public class MarnetBuilding {
	public static double[] resDegs = new double[] { 0.5, 0.25, 0.1, 0.05, 0.025 };

	public static Collection make(double res, String... datasources) {
		//load input lines
		Collection lines = new HashSet<Geometry>();

		//data sources preparation
		for(String ds : datasources) {
			System.out.println(" preparing "+ds);
			Collection ds_ = FeatureUtil.featuresToGeometries( GeoJSONUtil.load(ds));
			ds_ = prepare(ds_, res);
			lines.addAll(ds_);
		}

		System.out.println("Total before integration: " + lines.size());

		lines = GraphSimplify.planifyLines(lines);						System.out.println(lines.size() + " planifyLines");
		lines = GraphSimplify.lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = GraphSimplify.DPsimplify(lines, res);						System.out.println(lines.size() + " filterGeom");
		lines = GraphSimplify.removeSimilarDuplicateEdges(lines, res);	System.out.println(lines.size() + " removeSimilarDuplicateEdges");
		//lines = MeshSimplification.dtsePlanifyLines(lines, res);				System.out.println(lines.size() + " dtsePlanifyLines");
		lines = GraphSimplify.lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = GraphSimplify.planifyLines(lines);						System.out.println(lines.size() + " planifyLines");
		lines = GraphSimplify.lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		//lines = MeshSimplification.dtsePlanifyLines(lines, res);				System.out.println(lines.size() + " dtsePlanifyLines");
		lines = GraphSimplify.lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = GraphSimplify.planifyLines(lines);						System.out.println(lines.size() + " planifyLines");
		lines = GraphSimplify.lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		//lines = MeshSimplification.dtsePlanifyLines(lines, res);				System.out.println(lines.size() + " dtsePlanifyLines");
		lines = GraphSimplify.lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = GraphSimplify.resPlanifyLines(lines, res*0.01);			System.out.println(lines.size() + " resPlanifyLines");
		lines = GraphSimplify.lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = GraphSimplify.resPlanifyLines(lines, res*0.01);			System.out.println(lines.size() + " resPlanifyLines");

		//run with -Xss4m
		lines = ConnexComponents.keepOnlyLargestGraphConnexComponents(lines, 50);	System.out.println(lines.size() + " keepOnlyLargestGraphConnexComponents");

		return lines;
	}

	private static Collection prepare(Collection lines, double res) {
		lines = GraphSimplify.planifyLines(lines);						System.out.println(lines.size() + " planifyLines");
		lines = GraphSimplify.lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = GraphSimplify.DPsimplify(lines, res);						System.out.println(lines.size() + " filterGeom");
		lines = GraphSimplify.removeSimilarDuplicateEdges(lines, res);	System.out.println(lines.size() + " removeSimilarDuplicateEdges");
		lines = GraphSimplify.collapseTooShortEdgesAndPlanifyLines(lines, res);				System.out.println(lines.size() + " dtsePlanifyLines");
		lines = GraphSimplify.resPlanifyLines(lines, res*0.01);			System.out.println(lines.size() + " resPlanifyLines");
		return lines;
	}

}
