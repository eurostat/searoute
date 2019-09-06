package org.opencarto.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class FileUtil {

	//get all files in a folder (recursivelly)
	public static ArrayList<File> getFiles(String folderPath) {
		return getFiles(new File(folderPath));
	}
	public static ArrayList<File> getFiles(File folder) {
		ArrayList<File> files = new ArrayList<File>();
		for (File file : folder.listFiles())
			if (file.isDirectory())
				files.addAll(getFiles(file));
			else
				files.add(file);
		return files;
	}

	public static String getFileExtension(File file) {
		String name = file.getName();
		int lastIndexOf = name.lastIndexOf(".");
		if (lastIndexOf == -1) return "";
		return name.substring(lastIndexOf);
	}

	//count file line number
	public static int fileLineCount(String inputFilePath){
		int i=0;
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(inputFilePath))));
			while (br.readLine() != null)
				i++;
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return i;
	}

	public static void createFolders(String filePath){
		File parent = new File(filePath).getParentFile();
		if (!parent.exists() && !parent.mkdirs())
			throw new IllegalStateException("Couldn't create dir: " + parent);
	}

	public static File getFile(String filePath, boolean createFolders, boolean eraseOnExist){
		if(createFolders) createFolders(filePath);
		File file = new File(filePath);
		if(eraseOnExist && file.exists()) file.delete();
		return file;
	}

}
