/**
 * 
 */
package eu.europa.ec.eurostat.searoute;

import java.util.Collection;
import java.util.HashSet;

import org.opencarto.algo.meshsimplification.MeshSimplification;
import org.opencarto.io.GeoJSONUtil;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Some functions to build maritime networks at different resolutions.
 * 
 * @author julien Gaffuri
 *
 */
public class MarnetBuilding {


	public static Collection make(double res, String... datasources) {
		//load input lines
		Collection lines = new HashSet<Geometry>();

		//data sources preparation
		for(String ds : datasources) {
			System.out.println(" preparing "+ds);
			Collection ds_ = MeshSimplification.featuresToLines( GeoJSONUtil.load(ds));
			ds_ = prepare(ds_, res);
			lines.addAll(ds_);
		}

		System.out.println("Total before integration: " + lines.size());

		lines = MeshSimplification.planifyLines(lines);						System.out.println(lines.size() + " planifyLines");
		lines = MeshSimplification.lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = MeshSimplification.filterGeom(lines, res);						System.out.println(lines.size() + " filterGeom");
		lines = MeshSimplification.removeSimilarDuplicateEdges(lines, res);	System.out.println(lines.size() + " removeSimilarDuplicateEdges");
		lines = MeshSimplification.dtsePlanifyLines(lines, res);				System.out.println(lines.size() + " dtsePlanifyLines");
		lines = MeshSimplification.lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = MeshSimplification.planifyLines(lines);						System.out.println(lines.size() + " planifyLines");
		lines = MeshSimplification.lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = MeshSimplification.dtsePlanifyLines(lines, res);				System.out.println(lines.size() + " dtsePlanifyLines");
		lines = MeshSimplification.lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = MeshSimplification.planifyLines(lines);						System.out.println(lines.size() + " planifyLines");
		lines = MeshSimplification.lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = MeshSimplification.dtsePlanifyLines(lines, res);				System.out.println(lines.size() + " dtsePlanifyLines");
		lines = MeshSimplification.lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = MeshSimplification.resPlanifyLines(lines, res*0.01);			System.out.println(lines.size() + " resPlanifyLines");
		lines = MeshSimplification.lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = MeshSimplification.resPlanifyLines(lines, res*0.01);			System.out.println(lines.size() + " resPlanifyLines");

		//run with -Xss4m
		lines = MeshSimplification.keepOnlyLargestGraphConnexComponents(lines, 50);	System.out.println(lines.size() + " keepOnlyLargestGraphConnexComponents");

		return lines;
	}

	private static Collection prepare(Collection lines, double res) {
		lines = MeshSimplification.planifyLines(lines);						System.out.println(lines.size() + " planifyLines");
		lines = MeshSimplification.lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = MeshSimplification.filterGeom(lines, res);						System.out.println(lines.size() + " filterGeom");
		lines = MeshSimplification.removeSimilarDuplicateEdges(lines, res);	System.out.println(lines.size() + " removeSimilarDuplicateEdges");
		lines = MeshSimplification.dtsePlanifyLines(lines, res);				System.out.println(lines.size() + " dtsePlanifyLines");
		lines = MeshSimplification.resPlanifyLines(lines, res*0.01);			System.out.println(lines.size() + " resPlanifyLines");
		return lines;
	}

}
