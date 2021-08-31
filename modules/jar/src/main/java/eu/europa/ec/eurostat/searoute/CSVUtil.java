/**
 * 
 */
package eu.europa.ec.eurostat.searoute;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

/**
 * @author julien Gaffuri
 *
 */
public class CSVUtil {

	/**
	 * @param filePath
	 * @return
	 */
	public static ArrayList<Map<String,String>> load(String filePath) {
		return load(filePath, CSVFormat.DEFAULT.withFirstRecordAsHeader());
	}

	/**
	 * @param filePath
	 * @param cf
	 * @return
	 */
	public static ArrayList<Map<String,String>> load(String filePath, CSVFormat cf) {
		ArrayList<Map<String,String>> data = new ArrayList<>();
		try {
			//parse file
			Reader in = new FileReader(filePath);
			Iterable<CSVRecord> raws = cf.parse(in);

			//read data
			for (CSVRecord raw : raws) data.add(raw.toMap());

			in.close();
		} catch (Exception e) { e.printStackTrace(); }
		return data;
	}

}
