package eu.ec.eurostat.searoute;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.opencarto.datamodel.Feature;
import org.opencarto.io.GeoJSONUtil;

public class MarnetCheck {

	public static void main(String[] args) {
		try {
			System.out.println("Start.");


			//load input
			ArrayList<Feature> fs = GeoJSONUtil.load("resources/marnet/marnet_working.geojson");
			System.out.println(fs.size());



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
			//GeoJSONUtil.save(fs, "resources/marnet/marnet_working_out.geojson");

			System.out.println("Done.");
		} catch (Exception e) { e.printStackTrace(); }
	}

}
