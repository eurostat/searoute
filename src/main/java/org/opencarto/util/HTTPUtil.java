package org.opencarto.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;

public class HTTPUtil {

	public static void downloadFromURL(String url, String file) {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
			new File(file).delete();
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
			String line=null;
			while ((line = in.readLine()) != null) out.println(line);
			in.close();
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
