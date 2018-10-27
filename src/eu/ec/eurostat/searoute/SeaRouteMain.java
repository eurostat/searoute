/**
 * 
 */
package eu.ec.eurostat.searoute;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.vividsolutions.jts.geom.MultiLineString;

/**
 * Main method for executable program.
 * Apply to csv file.
 * 
 * @author julien Gaffuri
 *
 */
public class SeaRouteMain {

	public static void main(String[] args) {
		String filePath = args.length==0?"input.csv":args[0];
		String outFilePath = "out.csv";

		//check file existence
		File f = new File(filePath);
		if(!f.exists()) {
			System.out.println("Could not find input file "+filePath);
			return;
		}

		//load data
		ArrayList<HashMap<String, String>> data = CSVUtil.load(filePath);

		//check input data
		if(data.size() == 0) {
			System.out.println("Empty file "+filePath);
			return;
		}
		Set<String> keys = data.get(0).keySet();
		if(!keys.contains("olon") || !keys.contains("olat") || !keys.contains("dlon") || !keys.contains("dlat")) {
			System.out.println("Input file should contain olon,olat,dlon,dlat columns "+filePath);
			return;
		}

		//compute routes
		SeaRouting sr = new SeaRouting();
		for( HashMap<String, String> o : data ) {
			o.put("route", "na");
			o.put("dist", "na");

			double oLon = get(o, "olon");
			double oLat = get(o, "olat");
			double dLon = get(o, "dlon");
			double dLat = get(o, "dlat");

			if(Double.isNaN(oLon) || Double.isNaN(oLat) || Double.isNaN(dLon) || Double.isNaN(dLat))
				continue;

			//compute route
			MultiLineString g = sr.getRoute(oLon, oLat, dLon, dLat);
			o.put("route", Util.toGeoJSON(g));

			//compute distance
			double d = Util.getLengthGeo(g);
			o.put("dist", ""+Util.round(d, 2));
		}

		//export
		Collection<Map<String, Object>> dataOut = new ArrayList<Map<String, Object>>();
		for(HashMap<String, String> d : data) {
			Map<String, Object> d_ = new HashMap<String,Object>();
			d_.putAll(d);
			dataOut.add(d_);
		}
		CSVUtil.save(dataOut, outFilePath);
	}


	private static double get(HashMap<String, String> o, String k) {
		String d = o.get(k);
		if(d==null) return Double.NaN;
		return Double.parseDouble(d);
	}

}
