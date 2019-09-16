/**
 * 
 */
package org.opencarto.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opencarto.util.FileUtil;

/**
 * @author julien Gaffuri
 *
 */
public class CSVUtil {

	//load a csv file
	//NB: for tab separated files, use "([^\t]*)"
	public static ArrayList<HashMap<String,String>> load(String filePath) {
		return load(filePath, "\\s*(\"[^\"]*\"|[^,]*)\\s*");
	}

	public static ArrayList<HashMap<String,String>> load(String filePath, String patternString) {
		return load(filePath, null, patternString);
	}

	public static ArrayList<HashMap<String,String>> load(String filePath, String[] header, String patternString) {
		ArrayList<HashMap<String,String>> data = new ArrayList<HashMap<String,String>>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filePath));
			Pattern pattern = Pattern.compile(patternString);

			String line;
			Matcher m;
			ArrayList<String> keys;
			if(header==null){
				//read header
				line = br.readLine();
				m = pattern.matcher(line);
				keys = new ArrayList<String>();
				while(m.find()){
					keys.add(m.group(1));
					m.find();
				}
			} else
				keys = new ArrayList<String>(Arrays.asList(header));

			//read data
			while ((line = br.readLine()) != null) {
				m = pattern.matcher(line);
				LinkedHashMap<String,String> obj = new LinkedHashMap<String,String>();
				for(String key:keys){
					m.find();
					String value=m.group(1);
					//System.out.println("******"+value + "------"+("".equals(value)));
					if(!"".equals(value)) m.find();
					obj.put(key, value);
					//System.out.println(key+":"+value);
				}
				data.add(obj);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return data;		
	}


	//save a csv file
	public static void save(Collection<HashMap<String, String>> data, String outFile) { save(data, outFile, null); }
	public static void save(Collection<HashMap<String, String>> data, String outFile, List<String> keys) {
		try {
			if(data.size()==0){
				System.err.println("Cannot save CSV file: Empty dataset.");
				return;
			}

			//create output file
			File f = FileUtil.getFile(outFile, true, true);
			BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));

			//write header
			if(keys==null) keys = new ArrayList<String>(data.iterator().next().keySet());
			int i=0;
			for(String key : keys ){
				bw.write(key);
				if(i<keys.size()-1) bw.write(",");
				i++;
			}
			bw.write("\n");

			//write data
			for(Map<String, String> obj : data){
				i=0;
				for(String key : keys){
					String value = obj.get(key).toString();
					bw.write(value);
					if(i<keys.size()-1) bw.write(",");
					i++;
				}
				bw.write("\n");

				/*Collection<String> values = obj.values(); i=0;
				for(String value:values){
					bw.write(value);
					if(i<values.size()-1) bw.write(",");
					i++;
				}
				bw.write("\n");*/
			}
			bw.close();
		} catch (Exception e) {e.printStackTrace();}
	}

	public static HashSet<String> getUniqueValues(Collection<HashMap<String, String>> data, String key, boolean print) {
		HashSet<String> values = new HashSet<String>();
		for(HashMap<String, String> obj : data)
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
		ArrayList<HashMap<String, String>> data_ = new ArrayList<>();
		for(int i=0; i<data.size(); i++) {
			HashMap<String, String> m = new HashMap<>();
			m.put("id", i+"");
			m.put("val", data.get(i));
			data_.add(m);
		}
		ArrayList<String> keys = new ArrayList<String>(); keys.add("id"); keys.add("val");
		save(data_, outFile, keys);
	}

}
