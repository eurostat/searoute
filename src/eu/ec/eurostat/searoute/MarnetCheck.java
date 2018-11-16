package eu.ec.eurostat.searoute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opencarto.algo.base.Union;
import org.opencarto.datamodel.Feature;
import org.opencarto.io.GeoJSONUtil;
import org.opencarto.util.JTSGeomUtil;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

public class MarnetCheck {

	private static Collection lineMerge(Collection lines) {
		LineMerger lm = new LineMerger();
		lm.add(lines);
		return lm.getMergedLineStrings();
	}

	private static Collection planifyLines(Collection lines) {
		Geometry u = Union.getLineUnion(lines);
		return JTSGeomUtil.getLineStringGeometries(u);
	}

	private static Collection filterGeom(Collection lines, double d) {
		Collection out = new HashSet<Geometry>();
		for(Object line : lines) out.add( DouglasPeuckerSimplifier.simplify((Geometry)line, d) );
		return out;
	}

	public static void main(String[] args) {
		try {
			System.out.println("Start.");

			double res = 0.1;

			//load input lines
			ArrayList<Feature> fs = GeoJSONUtil.load("resources/marnet/marnet_densified.geojson");
			Collection lines = new HashSet<Geometry>();
			for(Feature f : fs) lines.add(f.getGeom());
			fs.clear(); fs = null;
			System.out.println(lines.size());

			lines = planifyLines(lines);
			System.out.println(lines.size());

			lines = lineMerge(lines);
			System.out.println(lines.size());

			lines = filterGeom(lines, res);
			System.out.println(lines.size());

			lines = planifyLines(lines);
			System.out.println(lines.size());

			lines = lineMerge(lines);
			System.out.println(lines.size());


			//TODO
			//make noding - resolutionise
			//check number of connex components
			//remove duplicate network edges - always keep shorter
			//check 180/-180 compatibility

			ArrayList<Feature> fsOut = new ArrayList<Feature>();
			int i=0;
			for(Object ls : lines) {
				Feature f = new Feature();
				f.id = ""+(i++);
				f.setGeom((Geometry)ls);
				fsOut.add(f);
			}

			//create graph
			/*Graph g = GraphBuilder.buildForNetworkFromLinearFeatures(fsOut);
			Collection<Graph> ccs = GraphConnexComponents.get(g);
			for(Graph cc : ccs) {
				System.out.println(" "+cc.getNodes().size());
			}*/


			System.out.println(fsOut.size());

			//save output
			GeoJSONUtil.save(fsOut, "resources/marnet/marnet_working_out.geojson", DefaultGeographicCRS.WGS84);

			System.out.println("Done.");
		} catch (Exception e) { e.printStackTrace(); }
	}

}
