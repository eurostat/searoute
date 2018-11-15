package eu.ec.eurostat.searoute;

import java.util.ArrayList;
import java.util.Collection;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opencarto.algo.base.Union;
import org.opencarto.datamodel.Feature;
import org.opencarto.io.GeoJSONUtil;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.operation.linemerge.LineMerger;

public class MarnetCheck {

	public static void main(String[] args) {
		try {
			System.out.println("Start.");


			//load input
			ArrayList<Feature> fs = GeoJSONUtil.load("resources/marnet/marnet_working.geojson");

			//test linemerger
			LineMerger lm = new LineMerger();
			for(Feature f : fs) lm.add(f.getGeom());
			Collection<Geometry> lines = lm.getMergedLineStrings();

			Geometry u = Union.getLineUnion(lines);
			System.out.println(u.getGeometryType());
			System.out.println(u.getCoordinates().length);
			
			ArrayList<Feature> fsOut = new ArrayList<Feature>();
			int i=0;
			for(Geometry ls : lines) {
				Feature f = new Feature();
				f.id = ""+(i++);
				f.setGeom(ls);
				fsOut.add(f);
			}

			System.out.println(fs.size());
			System.out.println(fsOut.size());

			//make noding
			//remove duplicate network edges - always keep shorter
			//make network planar
			//check number of connex components
			//check 180/-180 compatibility


			//integrate
			//DP filter
			//linemerger
			//cut lines depending on intersection

			//save output
			//TODO test that !
			GeoJSONUtil.save(fsOut, "resources/marnet/marnet_working_out.geojson", DefaultGeographicCRS.WGS84);

			System.out.println("Done.");
		} catch (Exception e) { e.printStackTrace(); }
	}

}
