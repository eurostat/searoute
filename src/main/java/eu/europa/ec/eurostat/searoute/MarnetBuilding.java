/**
 * 
 */
package eu.europa.ec.eurostat.searoute;

import java.util.Collection;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.LineString;

import eu.europa.ec.eurostat.jgiscotools.algo.line.DouglasPeuckerRamerFilter;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.graph.algo.ConnexComponents;
import eu.europa.ec.eurostat.jgiscotools.graph.algo.GraphSimplify;

/**
 * Some functions to build maritime networks at different resolutions.
 * 
 * @author julien Gaffuri
 *
 */
public class MarnetBuilding {
	private final static Logger LOGGER = LogManager.getLogger(MarnetBuilding.class.getName());
	public static double[] resDegs = new double[] { 0.5, 0.25, 0.1, 0.05, 0.025 };

	/**
	 * Build a maritime network from a list of linear features representing maritime lines.
	 * 
	 * @param res The target resolution.
	 * @param fs Feature collections.
	 * @return
	 */
	public static Collection<LineString> makeFromLinearFeatures(double res, Collection<Feature>... fs) {
		//load input lines
		Collection<LineString> lines = new HashSet<LineString>();

		//data sources preparation
		for(Collection<Feature> fs_ : fs) {
			LOGGER.debug(" preparing " + fs_.size());
			Collection ls = FeatureUtil.featuresToGeometries(fs_);
			ls = prepare(ls, res);
			lines.addAll(ls);
		}
		return make(res, lines);
	}

	/**
	 * Build a maritime network from maritime lines.
	 * 
	 * @param res The target resolution.
	 * @param lines
	 * @return
	 */
	public static Collection<LineString> make(double res, Collection<LineString> lines) {
		LOGGER.debug("Total before integration: " + lines.size());

		lines = GraphSimplify.planifyLines(lines);						LOGGER.debug(lines.size() + " planifyLines");
		lines = GraphSimplify.lineMerge(lines);							LOGGER.debug(lines.size() + " lineMerge");
		lines = DouglasPeuckerRamerFilter.get(lines, res);						LOGGER.debug(lines.size() + " filterGeom");
		lines = GraphSimplify.removeSimilarDuplicateEdges(lines, res);	LOGGER.debug(lines.size() + " removeSimilarDuplicateEdges");
		//lines = MeshSimplification.dtsePlanifyLines(lines, res);				LOGGER.debug(lines.size() + " dtsePlanifyLines");
		lines = GraphSimplify.lineMerge(lines);							LOGGER.debug(lines.size() + " lineMerge");
		lines = GraphSimplify.planifyLines(lines);						LOGGER.debug(lines.size() + " planifyLines");
		lines = GraphSimplify.lineMerge(lines);							LOGGER.debug(lines.size() + " lineMerge");
		//lines = MeshSimplification.dtsePlanifyLines(lines, res);				LOGGER.debug(lines.size() + " dtsePlanifyLines");
		lines = GraphSimplify.lineMerge(lines);							LOGGER.debug(lines.size() + " lineMerge");
		lines = GraphSimplify.planifyLines(lines);						LOGGER.debug(lines.size() + " planifyLines");
		lines = GraphSimplify.lineMerge(lines);							LOGGER.debug(lines.size() + " lineMerge");
		//lines = MeshSimplification.dtsePlanifyLines(lines, res);				LOGGER.debug(lines.size() + " dtsePlanifyLines");
		lines = GraphSimplify.lineMerge(lines);							LOGGER.debug(lines.size() + " lineMerge");
		lines = GraphSimplify.resPlanifyLines(lines, res*0.01, false);			LOGGER.debug(lines.size() + " resPlanifyLines");
		lines = GraphSimplify.lineMerge(lines);							LOGGER.debug(lines.size() + " lineMerge");
		lines = GraphSimplify.resPlanifyLines(lines, res*0.01, false);			LOGGER.debug(lines.size() + " resPlanifyLines");

		//run with -Xss4m
		lines = ConnexComponents.keepOnlyLargestGraphConnexComponents(lines, 50);	LOGGER.debug(lines.size() + " keepOnlyLargestGraphConnexComponents");

		return lines;
	}

	private static Collection<LineString> prepare(Collection<LineString> lines, double res) {
		lines = GraphSimplify.planifyLines(lines);						LOGGER.debug(lines.size() + " planifyLines");
		lines = GraphSimplify.lineMerge(lines);							LOGGER.debug(lines.size() + " lineMerge");
		lines = DouglasPeuckerRamerFilter.get(lines, res);						LOGGER.debug(lines.size() + " filterGeom");
		lines = GraphSimplify.removeSimilarDuplicateEdges(lines, res);	LOGGER.debug(lines.size() + " removeSimilarDuplicateEdges");
		lines = GraphSimplify.collapseTooShortEdgesAndPlanifyLines(lines, res, true, true);				LOGGER.debug(lines.size() + " dtsePlanifyLines");
		lines = GraphSimplify.resPlanifyLines(lines, res*0.01, false);			LOGGER.debug(lines.size() + " resPlanifyLines");
		return lines;
	}

}
