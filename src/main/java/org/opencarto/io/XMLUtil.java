package org.opencarto.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

/**
 * @author julien Gaffuri
 *
 */
public class XMLUtil {

	public static Document parseXMLfromURL(String urlString){
		try{
			InputStream in = new URL(urlString).openConnection().getInputStream();
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		}
		catch(Exception e){
			e.printStackTrace();
		}       
		return null;
	}


	public static String getFileNameSpace(File file) {
		String ns = null;
		try {
			FileInputStream ips = new FileInputStream(file);
			Document XMLDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( ips );
			ns = XMLDoc.getDocumentElement().getAttribute("xmlns");
			try { ips.close(); } catch (IOException e) { e.printStackTrace(); }
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ns;
	}


}
