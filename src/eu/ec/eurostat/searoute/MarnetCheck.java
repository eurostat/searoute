package eu.ec.eurostat.searoute;

import java.util.ArrayList;
import java.util.Collection;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opencarto.algo.base.Union;
import org.opencarto.datamodel.Feature;
import org.opencarto.io.GeoJSONUtil;
import org.opencarto.util.JTSGeomUtil;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.operation.linemerge.LineMerger;

public class MarnetCheck {

	public static void main(String[] args) {
		try {
			System.out.println("Start.");


			//load input
			ArrayList<Feature> fs = GeoJSONUtil.load("resources/marnet/marnet_densified.geojson");
			System.out.println(fs.size());

			//test linemerger
			LineMerger lm = new LineMerger();
			for(Feature f : fs) lm.add(f.getGeom());
			Collection<Geometry> lines = lm.getMergedLineStrings();
			System.out.println(lines.size());

			Geometry u = Union.getLineUnion(lines);
			System.out.println(u.getGeometryType());
			System.out.println(u.getCoordinates().length + " cs");
			Collection<LineString> lines_ = JTSGeomUtil.getLineStringGeometries(u);
			System.out.println(lines_.size());

			lm = new LineMerger();
			lm.add(lines_);
			lines = lm.getMergedLineStrings();
			System.out.println(lines.size());


			//TODO
			//DP filter
			//check number of connex components
			//remove duplicate network edges - always keep shorter
			//make noding - integrate
			//check 180/-180 compatibility

			ArrayList<Feature> fsOut = new ArrayList<Feature>();
			int i=0;
			for(Geometry ls : lines) {
				Feature f = new Feature();
				f.id = ""+(i++);
				f.setGeom(ls);
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
