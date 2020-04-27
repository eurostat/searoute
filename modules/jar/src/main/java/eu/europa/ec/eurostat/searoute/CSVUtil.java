/**
 * 
 */
package eu.europa.ec.eurostat.searoute;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;

/**
 * @author julien Gaffuri
 *
 */
public class CSVUtil {


	/*public static void main(String[] args) {
		ArrayList<Map<String, String>> a = load("src/test/resources/csv/test.csv");
		System.out.println(a);
		save(a, "target/out.csv");
	}*/


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



	//save a csv file
	public static void save(Collection<Map<String, String>> data, String outFile) {
		ArrayList<String> header = new ArrayList<>( data.iterator().next().keySet() );
		CSVFormat cf = CSVFormat.DEFAULT.withHeader(header.toArray(new String[header.size()]));
		save(data, outFile, cf);
	}

	public static void save(Collection<Map<String, String>> data, String outFile, List<String> header) {
		CSVFormat cf = CSVFormat.DEFAULT.withHeader(header.toArray(new String[header.size()]));
		save(data, outFile, cf);
	}

	public static void save(Collection<Map<String, String>> data, String outFile, CSVFormat cf) {
		try {
			FileWriter out = new FileWriter(outFile);
			String[] header = cf.getHeader(); int nb = header.length;
			CSVPrinter printer = new CSVPrinter(out, cf);
			for(Map<String, String> raw : data) {
				String[] values = new String[nb];
				for(int i=0; i<nb; i++) values[i]=raw.get(header[i]);
				printer.printRecord(values);
			}
			printer.close();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static HashSet<String> getUniqueValues(Collection<Map<String, String>> data, String key, boolean print) {
		HashSet<String> values = new HashSet<String>();
		for(Map<String, String> obj : data)
			values.add(obj.get(key));
		if(print){
			System.out.println(key + " " + values.size()+" values");
			System.out.println(values);
		}
		return values;
	}

	public static ArrayList<HashMap<String, String>> getSubset(ArrayList<HashMap<String, String>> data, String key, String value) {
		ArrayList<HashMap<String, String>> dataOut = new ArrayList<HashMap<String, String>>();
		for(HashMap<String, String> obj : data)
			if(value.equals(obj.get(key))) dataOut.add(obj);
		return dataOut;
	}

	public static void save(List<String> data, String outFile) {
		ArrayList<Map<String, String>> data_ = new ArrayList<>();
		for(int i=0; i<data.size(); i++) {
			HashMap<String, String> m = new HashMap<>();
			m.put("id", i+"");
			m.put("val", data.get(i));
			data_.add(m);
		}
		ArrayList<String> keys = new ArrayList<String>(); keys.add("id"); keys.add("val");
		save(data_, outFile, keys);
	}




	/**
	 * Transform CSV data into a feature collection, with point geometry.
	 * 
	 * @param csvData
	 * @param xCol
	 * @param yCol
	 * @return
	 */
	public static Collection<Feature> CSVToFeatures(Collection<Map<String, String>> csvData, String xCol, String yCol) {
		Collection<Feature> out = new ArrayList<Feature>();
		GeometryFactory gf = new GeometryFactory();
		for (Map<String, String> h : csvData) {
			Feature f = new Feature();
			Coordinate c = new Coordinate(0,0);
			for(Entry<String,String> e : h.entrySet()) {
				if(xCol.equals(e.getKey())) c.x = Double.parseDouble(e.getValue());
				if(yCol.equals(e.getKey())) c.y = Double.parseDouble(e.getValue());
				f.setAttribute(e.getKey(), e.getValue());
			}
			f.setGeometry(gf.createPoint(c));
			out.add(f);
		}
		return out;
	}





	public static void setValue(Collection<Map<String, String>> data, String col, String value) {
		for(Map<String, String> r : data)
			r.put(col, value);
	}

	public static void addColumn(Collection<Map<String, String>> data, String col, String defaultValue) {
		for(Map<String, String> h : data) {
			if(h.get(col) == null || "".equals(h.get(col))) {
				h.put(col, defaultValue);
			}
		}
	}

	public static void addColumns(Collection<Map<String, String>> data, String[] cols, String defaultValue) {
		for(String col : cols)
			addColumn(data, col, defaultValue);
	}

	public static void removeColumn(Collection<Map<String, String>> data, String... cols) {
		for(String col : cols)
			for(Map<String, String> h : data) {
				if(h.get(col) != null)
					h.remove(col);
			}
	}

	public static void renameColumn(Collection<Map<String, String>> data, String oldName, String newName) {
		for(Map<String, String> h : data) {
			if(h.get(oldName) != null) {
				h.put(newName, h.get(oldName));
				h.remove(oldName);
			}
		}
	}

	public static void replaceValue(Collection<Map<String, String>> data, String col, String iniVal, String finVal) {
		for(Map<String, String> h : data) {
			String v = h.get(col);
			if(iniVal == null && v == null || iniVal != null && iniVal.equals(v))
				h.put(col, finVal);
		}
	}

	public static void replaceValue(Collection<Map<String, String>> data, String iniVal, String finVal) {
		for(Map<String, String> h : data)
			for(Entry<String,String> e : h.entrySet()) {
				String v = e.getValue();
				if(iniVal == null && v == null || iniVal != null && iniVal.equals(v))
					e.setValue(finVal);
			}
	}

	public static ArrayList<String> getValues(Collection<Map<String, String>> data, String col) {
		ArrayList<String> out = new ArrayList<>();
		for(Map<String, String> h : data)
			out.add(h.get(col));
		return out;
	}



	public static Collection<Map<String, String>> aggregateById(Collection<Map<String, String>> data, String idCol, String...  sumCols) {
		HashMap<String, Map<String, String>> ind = new HashMap<String, Map<String, String>>();
		for(Map<String, String> h : data) {
			String id  = h.get(idCol);
			Map<String, String> h_ = ind.get(id);
			if(h_ == null) {
				ind.put(id, h);
			} else {
				//increment number of beds
				for(String sumCol : sumCols) {					
					String nbs = h.get(sumCol);
					if(nbs == null || nbs.isEmpty()) continue;
					double nb = Double.parseDouble(nbs);
					String nbs_ = h_.get(sumCol);
					if(nbs_ == null || nbs_.isEmpty()) { h_.put(sumCol, ""+nb); continue; }
					double nb_ = Double.parseDouble(nbs_);
					h_.put(sumCol, ""+(nb+nb_));
				}
			}
		}
		return ind.values();
	}


	public static void join(List<Map<String, String>> data1, String key1, List<Map<String, String>> data2, String key2, boolean printWarnings) {
		//index data2 by key
		HashMap<String,Map<String,String>> ind2 = new HashMap<>();
		for(Map<String, String> elt : data2) ind2.put(elt.get(key2), elt);

		//join
		for(Map<String, String> elt : data1) {
			String k1 = elt.get(key1);
			Map<String, String> elt2 = ind2.get(k1);
			if(elt2 == null) {
				if(printWarnings) System.out.println("No element to join for key: " + k1);
				continue;
			}
			elt.putAll(elt2);
		}
	}

}
