/**
 * 
 */
package eu.ec.eurostat.searoute;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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

		Options options = new Options();
		options.addOption(Option.builder("i").longOpt("inputFile").desc("Input file (CSV format).")
				.hasArg().argName("file").build());
		options.addOption(Option.builder("o").longOpt("outputFile").desc("Optional. Output file (CSV format). Default: 'out.csv'.")
				.hasArg().argName("file").build());
		options.addOption(Option.builder("olonCol").desc("Optional. The name of the column in the input file where the origin longitude is specified. Default: 'olon'.")
				.hasArg().argName("file").build());
		options.addOption(Option.builder("olatCol").desc("Optional. The name of the column in the input file where the origin latitude is specified. Default: 'olat'.")
				.hasArg().argName("file").build());
		options.addOption(Option.builder("dlonCol").desc("Optional. The name of the column in the input file where the destination longitude is specified. Default: 'dlon'.")
				.hasArg().argName("file").build());
		options.addOption(Option.builder("dlatCol").desc("Optional. The name of the column in the input file where the destination latitude is specified. Default: 'dlat'.")
				.hasArg().argName("file").build());
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

		//input file
		String inFile = cmd.getOptionValue("i");
		if(inFile==null) {
			System.err.println("An input file should be specified with -i option. Use -h option to show the help message.");
			return;
		} else if(!new File(inFile).exists()) {
			System.err.println("Input file does not exist: "+inFile);
			return;
		}

		//output file
		String outFile = cmd.getOptionValue("o");
		if(outFile == null) outFile = Paths.get("").toAbsolutePath().toString()+"/out.csv";

		//column names
		String olonCol = cmd.getOptionValue("olonCol");   if(olonCol == null) olonCol = "olon";
		String olatCol = cmd.getOptionValue("olatCol");   if(olatCol == null) olatCol = "olat";
		String dlonCol = cmd.getOptionValue("dlonCol");   if(dlonCol == null) dlonCol = "dlon";
		String dlatCol = cmd.getOptionValue("dlatCol");   if(dlatCol == null) dlatCol = "dlat";




		//load data
		ArrayList<HashMap<String, String>> data = CSVUtil.load(inFile);

		//check input data
		if(data.size() == 0) {
			System.out.println("Empty file "+inFile);
			return;
		}

		Set<String> keys = data.get(0).keySet();
		if(!keys.contains(olonCol) || !keys.contains(olatCol) || !keys.contains(dlonCol) || !keys.contains(dlatCol)) {
			System.out.println("Input file should contain "+olonCol+","+olatCol+","+dlonCol+","+dlatCol+" columns " + inFile);
			return;
		}

		//compute routes
		SeaRouting sr = new SeaRouting();
		for( HashMap<String, String> o : data ) {
			o.put("route", "na");
			o.put("dist", "na");

			double oLon = get(o, olonCol);
			double oLat = get(o, olatCol);
			double dLon = get(o, dlonCol);
			double dLat = get(o, dlatCol);

			if(Double.isNaN(oLon) || Double.isNaN(oLat) || Double.isNaN(dLon) || Double.isNaN(dLat))
				continue;

			//compute route
			MultiLineString g = sr.getRoute(oLon, oLat, dLon, dLat);
			o.put("route", Utils.toGeoJSON(g));

			//compute distance
			double d = Utils.getLengthGeo(g);
			o.put("dist", ""+Utils.round(d, 2));
		}

		//export
		Collection<Map<String, Object>> dataOut = new ArrayList<Map<String, Object>>();
		for(HashMap<String, String> d : data) {
			Map<String, Object> d_ = new HashMap<String,Object>();
			d_.putAll(d);
			dataOut.add(d_);
		}
		CSVUtil.save(dataOut, outFile);
	}


	private static double get(HashMap<String, String> o, String k) {
		String d = o.get(k);
		if(d==null) return Double.NaN;
		return Double.parseDouble(d);
	}

}
