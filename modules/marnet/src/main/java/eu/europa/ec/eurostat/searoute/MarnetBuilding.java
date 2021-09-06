/**
 * 
 */
package eu.europa.ec.eurostat.searoute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.index.strtree.STRtree;

import eu.europa.ec.eurostat.jgiscotools.algo.base.DouglasPeuckerRamerFilter;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.graph.algo.ConnexComponents;
import eu.europa.ec.eurostat.jgiscotools.graph.algo.GraphSimplify;
import eu.europa.ec.eurostat.jgiscotools.graph.algo.GraphUtils;
import eu.europa.ec.eurostat.jgiscotools.graph.base.GraphBuilder;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Graph;
import eu.europa.ec.eurostat.jgiscotools.io.geo.CRSUtil;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

/**
 * Some functions to build maritime networks at different resolutions.
 * 
 * @author julien Gaffuri
 *
 */
public class MarnetBuilding {
	private final static Logger LOGGER = LogManager.getLogger(MarnetBuilding.class.getName());

	
	/**
	 * Run that with option: -Xss4m
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		LOGGER.info("Start");

		//Configurator.setLevel(LogManager.getLogger(MarnetBuilding.class).getName(), Level.DEBUG);

		//load input data
		ArrayList<Feature> fs = GeoData.getFeatures("src/main/resources/marnet_densified.gpkg");
		ArrayList<Feature> passs = GeoData.getFeatures("src/main/resources/pass.gpkg");
		LOGGER.info(fs.size() + "   " + passs.size());

		//define resolutions
		double[] ress = {0.5, 0.25, 0.1, 0.05, 0.025, 0.01, 0.005};
		String[] ress_ = {"100km", "50km", "20km", "10km", "5km", "2km", "1km"};

		for(int i=0; i<ress.length; i++) {
			LOGGER.info("Build maritime network for resolution " + ress_[i]);
			Collection<LineString> out = makeFromLinearFeatures(ress[i], fs);

			LOGGER.info("   " + out.size());
			HashSet<Feature> outFs = FeatureUtil.geometriesToFeatures(out);
			LOGGER.info("   " + outFs.size());


			//set default attribute value for passes
			for(Feature f : outFs)
				f.setAttribute("pass", null);

			//spatial index of marnet
			STRtree ind = FeatureUtil.getSTRtree(outFs);
			for(Feature pass : passs) {
				LOGGER.info("Pass " + pass.getAttribute("id") + " " + pass.getAttribute("name"));
				List edges = ind.query(pass.getGeometry().getEnvelopeInternal());
				LOGGER.debug("   " + edges.size());
				for(Object e : edges) {
					Feature f = (Feature)e;
					if(! f.getGeometry().intersects(pass.getGeometry())) continue;
					f.setAttribute("pass", pass.getAttribute("id"));
				}
			}


			LOGGER.info("   Save...");
			GeoData.save(outFs, "target/out/marnet_plus_" + ress_[i] + ".gpkg", CRSUtil.getWGS_84_CRS());
		}

		LOGGER.info("End");
	}




	/**
	 * Build a maritime network from a list of linear features representing maritime lines
	 * for specified resolutions
	 * 
	 * @param resDeg The target resolution (in geographical coordinates).
	 * @param fs Feature collection.
	 * @return
	 */
	public static Collection<LineString> makeFromLinearFeatures(double resDeg, Collection<Feature> fs) {
		Collection lines = FeatureUtil.featuresToGeometries(fs);
		Collection<LineString> out = make(resDeg, lines);
		return out;
	}


	/**
	 * Build a maritime network from maritime lines.
	 * 
	 * @param res The target resolution.
	 * @param lines
	 * @return
	 */
	public static Collection<LineString> make(double res, Collection<LineString> lines) {

		lines = GraphBuilder.planifyLines(lines);						LOGGER.debug(lines.size() + " planifyLines");
		lines = GraphBuilder.lineMerge(lines);							LOGGER.debug(lines.size() + " lineMerge");
		lines = DouglasPeuckerRamerFilter.get(lines, res);						LOGGER.debug(lines.size() + " filterGeom");
		lines = removeSimilarDuplicateEdges(lines, res);	LOGGER.debug(lines.size() + " removeSimilarDuplicateEdges");

		lines = GraphSimplify.resPlanifyLines(lines, res*0.01, false);			LOGGER.debug(lines.size() + " resPlanifyLines");
		lines = GraphBuilder.lineMerge(lines);							LOGGER.debug(lines.size() + " lineMerge");
		lines = GraphSimplify.resPlanifyLines(lines, res*0.01, false);			LOGGER.debug(lines.size() + " resPlanifyLines");
		lines = GraphBuilder.lineMerge(lines);							LOGGER.debug(lines.size() + " lineMerge");

		lines = GraphSimplify.collapseTooShortEdgesAndPlanifyLines(lines, res, true, true);				LOGGER.debug(lines.size() + " collapseTooShortEdgesAndPlanifyLines");
		lines = GraphBuilder.planifyLines(lines);						LOGGER.debug(lines.size() + " planifyLines");
		lines = GraphBuilder.lineMerge(lines);							LOGGER.debug(lines.size() + " lineMerge");

		lines = removeSimilarDuplicateEdges(lines, res);	LOGGER.debug(lines.size() + " removeSimilarDuplicateEdges");

		//run with -Xss4m
		lines = ConnexComponents.keepOnlyLargestGraphConnexComponents(lines, 50);	LOGGER.debug(lines.size() + " keepOnlyLargestGraphConnexComponents");

		return lines;
	}




	/**
	 * @param lines
	 * @param haussdorffDistance
	 * @return
	 */
	public static Collection<LineString> removeSimilarDuplicateEdges(Collection<LineString> lines, double haussdorffDistance) {
		Graph g = GraphBuilder.buildFromLinearGeometriesNonPlanar(lines);
		GraphUtils.removeSimilarDuplicateEdges(g, haussdorffDistance);
		return GraphUtils.getEdgeGeometries(g);
	}

}
