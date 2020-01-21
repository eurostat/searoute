/**
 * 
 */
package eu.europa.ec.eurostat.searoute;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoJSONUtil;
import eu.europa.ec.eurostat.jgiscotools.util.GeoDistanceUtil;
import eu.europa.ec.eurostat.jgiscotools.util.Util;

/**
 * Main method for executable program.
 * Apply to csv file.
 * 
 * @author julien Gaffuri
 *
 */
public class SeaRouteJarMain {

	public static void main(String[] args) throws Exception {

		Options options = new Options();
		options.addOption(Option.builder("i").longOpt("inputFile").desc("Input file (CSV format).")
				.hasArg().argName("file").build());
		options.addOption(Option.builder("o").longOpt("outputFile").desc("Optional. Output file (GeoJSON format). Default: 'out.json'.")
				.hasArg().argName("file").build());
		options.addOption(Option.builder("res").longOpt("resolution").desc("Optional. The resolution of the output geometries, in km. Default: '20'.")
				.hasArg().argName("5, 10, 20, 50 or 100").build());
		options.addOption(Option.builder("olonCol").desc("Optional. The name of the column in the input file where the origin longitude is specified. Default: 'olon'.")
				.hasArg().argName("Column name").build());
		options.addOption(Option.builder("olatCol").desc("Optional. The name of the column in the input file where the origin latitude is specified. Default: 'olat'.")
				.hasArg().argName("Column name").build());
		options.addOption(Option.builder("dlonCol").desc("Optional. The name of the column in the input file where the destination longitude is specified. Default: 'dlon'.")
				.hasArg().argName("Column name").build());
		options.addOption(Option.builder("dlatCol").desc("Optional. The name of the column in the input file where the destination latitude is specified. Default: 'dlat'.")
				.hasArg().argName("Column name").build());
		options.addOption(Option.builder("suez").desc("Optional. Set to '1' to allow trips using Suez channel. Default: '1'.")
				.hasArg().argName("0 or 1").build());
		options.addOption(Option.builder("panama").desc("Optional. Set to '1' to allow trips using Panama channel. Default: '1'.")
				.hasArg().argName("0 or 1").build());
		options.addOption(Option.builder("h").desc("Show this help message").build());

		CommandLine cmd = null;
		try { cmd = new DefaultParser().parse( options, args); } catch (ParseException e) {
			System.err.println( "Parsing failed.  Reason: " + e.getMessage() );
			return;
		}

		//help statement
		if(cmd.hasOption("h")) {
			new HelpFormatter().printHelp("java -jar searoute.jar", options);
			return;
		}

		System.out.println("Read parameters...");

		//input file
		String inFile = cmd.getOptionValue("i");
		if(inFile==null) {
			System.err.println("An input file should be specified with -i option. Use -h option to show the help message.");
			return;
		} else if(!new File(inFile).exists()) {
			System.err.println("Input file does not exist: " + inFile);
			return;
		}

		//output file
		String outFile = cmd.getOptionValue("o");
		if(outFile == null) outFile = Paths.get("").toAbsolutePath().toString() + "/out.json";

		//resolution
		String res = cmd.getOptionValue("res"); if(res == null) res = "20";

		//column names
		String olonCol = cmd.getOptionValue("olonCol");   if(olonCol == null) olonCol = "olon";
		String olatCol = cmd.getOptionValue("olatCol");   if(olatCol == null) olatCol = "olat";
		String dlonCol = cmd.getOptionValue("dlonCol");   if(dlonCol == null) dlonCol = "dlon";
		String dlatCol = cmd.getOptionValue("dlatCol");   if(dlatCol == null) dlatCol = "dlat";

		//channels
		String suez = cmd.getOptionValue("suez");   if(suez == null) suez = "1";
		String panama = cmd.getOptionValue("panama");   if(panama == null) panama = "1";

		//load data
		ArrayList<HashMap<String, String>> data = CSVUtil.load(inFile);

		//check input data
		if(data.size() == 0) {
			System.out.println("Empty file " + inFile);
			return;
		}

		Set<String> keys = data.get(0).keySet();
		if(!keys.contains(olonCol) || !keys.contains(olatCol) || !keys.contains(dlonCol) || !keys.contains(dlatCol)) {
			System.out.println("Input file should contain "+olonCol+","+olatCol+","+dlonCol+","+dlatCol+" columns " + inFile);
			return;
		}

		System.out.println("Build maritime network (resolution: "+res+"km)...");

		//String marnetSHP = "/marnet.shp";
		URL marnetURL = new SeaRouteJarMain().getClass().getResource("/resources/marnet/marnet_plus_"+res+"KM.shp").toURI().toURL();
		SeaRouting sr = new SeaRouting(marnetURL);

		System.out.println("Compute maritime routes (nb: "+data.size()+")...");

		ArrayList<Feature> fs = new ArrayList<Feature>();
		for( HashMap<String, String> o : data ) {
			double oLon = get(o, olonCol);
			double oLat = get(o, olatCol);
			double dLon = get(o, dlonCol);
			double dLat = get(o, dlatCol);

			if(Double.isNaN(oLon) || Double.isNaN(oLat) || Double.isNaN(dLon) || Double.isNaN(dLat))
				continue;

			//compute route
			Feature f = sr.getRoute(oLon, oLat, dLon, dLat, "1".equals(suez), "1".equals(panama));

			//set data
			f.getAttributes().putAll(o);

			//compute distance
			double d = GeoDistanceUtil.getLengthGeoKM(f.getDefaultGeometry());
			f.setAttribute("distKM", ""+Util.round(d, 2));

			fs.add(f);
		}

		System.out.println("Save...");
		GeoJSONUtil.save(fs , outFile, null);

		/*
		//compute routes
		for( HashMap<String, String> o : data ) {
			o.put("geom", "na");
			o.put("distKM", "na");

			double oLon = get(o, olonCol);
			double oLat = get(o, olatCol);
			double dLon = get(o, dlonCol);
			double dLat = get(o, dlatCol);

			if(Double.isNaN(oLon) || Double.isNaN(oLat) || Double.isNaN(dLon) || Double.isNaN(dLat))
				continue;

			//compute route
			Feature f = sr.getRoute(oLon, oLat, dLon, dLat, "1".equals(suez), "1".equals(panama));
			for(Entry<?,?> e : f.getProperties().entrySet()) o.put(e.getKey().toString(), e.getValue().toString());
			String geomStr = GeoJSONUtil.toGeoJSON(f.getGeom());
			geomStr.replace("\"", "\"\"");
			o.put("geom", "\""+geomStr+"\"");

			//compute distance
			double d = GeoDistanceUtil.getLengthGeoKM(f.getGeom());
			o.put("distKM", ""+Util.round(d, 2));
		}

		//export
		CSVUtil.save(data, outFile);
		 */		
	}


	private static double get(HashMap<String, String> o, String k) {
		String d = o.get(k);
		if(d==null) return Double.NaN;
		return Double.parseDouble(d);
	}

}
