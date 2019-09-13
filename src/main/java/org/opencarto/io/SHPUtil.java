package org.opencarto.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opencarto.datamodel.Feature;
import org.opencarto.util.FeatureUtil;
import org.opencarto.util.FileUtil;
import org.opencarto.util.JTSGeomUtil;
import org.opencarto.util.ProjectionUtil;
import org.opencarto.util.ProjectionUtil.CRSType;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author julien Gaffuri
 *
 */
public class SHPUtil {
	private final static Logger LOGGER = Logger.getLogger(SHPUtil.class);

	//get basic info on shp file

	public static SimpleFeatureType getSchema(String shpFilePath){
		try {
			File file = new File(shpFilePath);
			if(!file.exists()) throw new IOException("File "+shpFilePath+" does not exist.");
			return FileDataStoreFinder.getDataStore(file).getSchema();
		} catch (Exception e) { e.printStackTrace(); }
		return null;
	}
	public static String[] getAttributeNames(String shpFilePath){
		return SimpleFeatureUtil.getAttributeNames(getSchema(shpFilePath));
	}
	public static CoordinateReferenceSystem getCRS(String shpFilePath){
		return getSchema(shpFilePath).getCoordinateReferenceSystem();
	}
	public static CRSType getCRSType(String shpFilePath) {
		return ProjectionUtil.getCRSType(getCRS(shpFilePath));
	}
	public static Envelope getBounds(String shpFilePath) {
		return getSimpleFeatures(shpFilePath).getBounds();
	}


	//load

	public static SimpleFeatureCollection getSimpleFeatures(String shpFilePath){ return getSimpleFeatures(shpFilePath, null); }
	public static SimpleFeatureCollection getSimpleFeatures(String shpFilePath, Filter f){
		try {
			File file = new File(shpFilePath);
			if(!file.exists()) throw new IOException("File "+shpFilePath+" does not exist.");
			FileDataStore store = FileDataStoreFinder.getDataStore(file);
			SimpleFeatureCollection a = store.getFeatureSource().getFeatures(f);
			//DefaultFeatureCollection sfs = DataUtilities.collection(a);
			store.dispose();
			return a;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static class SHPData{
		public SimpleFeatureType ft;
		public ArrayList<Feature> fs;
		public ReferencedEnvelope env;
		public SHPData(SimpleFeatureType ft, ArrayList<Feature> fs, ReferencedEnvelope env){
			this.ft=ft; this.fs=fs; this.env=env;
		}
	}

	public static SHPData loadSHP(String shpFilePath) { return loadSHP(shpFilePath, null); }
	public static SHPData loadSHP(String shpFilePath, Filter f) {
		SimpleFeatureCollection sfs = getSimpleFeatures(shpFilePath, f);
		SHPData sd = new SHPData(sfs.getSchema(), SimpleFeatureUtil.get(sfs), sfs.getBounds());
		return sd;
	}




	//save

	//public static void saveSHP(Collection<Feature> fs, String outFile) { saveSHP(fs, outFile, null); }
	public static void saveSHP(Collection<? extends Feature> fs, String outFile, CoordinateReferenceSystem crs) { saveSHP(fs,outFile,crs,null); }
	public static void saveSHP(Collection<? extends Feature> fs, String outFile, CoordinateReferenceSystem crs, List<String> atts) { saveSHP(SimpleFeatureUtil.get(fs, crs, atts), outFile); }
	public static void saveSHP(SimpleFeatureCollection sfs, String outFile) {
		try {
			if(sfs.size() == 0){
				//file.createNewFile();
				LOGGER.warn("Could not save file "+outFile+" - collection of features is empty");
				return;
			}

			//create output file
			File file = FileUtil.getFile(outFile, true, true);

			//create feature store
			HashMap<String, Serializable> params = new HashMap<String, Serializable>();
			params.put("url", file.toURI().toURL());
			params.put("create spatial index", Boolean.TRUE);
			ShapefileDataStore ds = (ShapefileDataStore) new ShapefileDataStoreFactory().createNewDataStore(params);

			ds.createSchema(sfs.getSchema());
			SimpleFeatureStore fst = (SimpleFeatureStore)ds.getFeatureSource(ds.getTypeNames()[0]);

			//creation transaction
			Transaction tr = new DefaultTransaction("create");
			fst.setTransaction(tr);
			try {
				fst.addFeatures(sfs);
				tr.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tr.rollback();
			} finally {
				tr.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	public static void saveGeomsSHP(Geometry geom, String outFile) {
		Collection<Geometry> geoms = new ArrayList<>();
		geoms.add(geom);
		saveGeomsSHP(geoms, outFile, null);
	}
	public static <T extends Geometry> void saveGeomsSHP(Collection<T> geoms, String outFile) {
		saveGeomsSHP(geoms, outFile, null);
	}
	public static <T extends Geometry> void saveGeomsSHP(Collection<T> geoms, String outFile, CoordinateReferenceSystem crs) {
		saveSHP(SimpleFeatureUtil.getFeaturesFromGeometries(geoms), outFile, crs);
	}

	public static void saveCoordsSHP(Collection<Coordinate> cs, String outFile) {
		saveCoordsSHP(cs, outFile, null);
	}
	public static void saveCoordsSHP(Collection<Coordinate> cs, String outFile, CoordinateReferenceSystem crs) {
		Collection<Point> pts = JTSGeomUtil.getPointsFromCoordinates(cs);
		saveSHP(SimpleFeatureUtil.getFeaturesFromGeometries(pts), outFile, crs);
	}




	//add feature to a shapefile
	private static void add(SimpleFeature f, String inFile) {
		try {
			Map<String,URL> map = new HashMap<String,URL>();
			map.put("url", new File(inFile).toURI().toURL());
			DataStore ds = DataStoreFinder.getDataStore(map);
			String typeName = ds.getTypeNames()[0];
			SimpleFeatureType ft = ds.getFeatureSource(typeName).getFeatures().getSchema();

			Transaction tr = new DefaultTransaction("create");
			String tn = ds.getTypeNames()[0];
			SimpleFeatureSource fs_ = ds.getFeatureSource(tn);

			if (fs_ instanceof SimpleFeatureStore) {
				SimpleFeatureStore fst = (SimpleFeatureStore) fs_;

				DefaultFeatureCollection objs = new DefaultFeatureCollection(null, ft);
				objs.add(f);

				fst.setTransaction(tr);
				try {
					fst.addFeatures(objs);
					tr.commit();
				} catch (Exception problem) {
					problem.printStackTrace();
					tr.rollback();
				} finally {
					tr.close();
				}
			} else {
				System.out.println(tn + " does not support read/write access");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}







	//remove empty or null geometries from collection
	public static void removeNullOrEmpty(Collection<SimpleFeature> fs, String geomAtt) {
		ArrayList<SimpleFeature> toRemove = new ArrayList<SimpleFeature>();
		for(SimpleFeature f:fs){
			Geometry g = (Geometry)f.getAttribute(geomAtt);
			if(g==null || g.isEmpty())
				toRemove.add(f);
		}
		fs.removeAll(toRemove);
	}

	//clean geometries of a shapefile
	public static void cleanGeometries(String inFile, String geomAtt, String outFile){
		System.out.println("Load data from "+inFile);
		SHPData data = loadSHP(inFile);

		System.out.print("clean all geometries...");
		for(Feature f : data.fs)
			f.setGeom( JTSGeomUtil.toMulti(JTSGeomUtil.clean( f.getGeom() )));
		System.out.println(" Done.");

		System.out.println("Save data to "+outFile);
		saveSHP(SimpleFeatureUtil.get(data.fs, getCRS(inFile)), outFile);
	}



	public static void shpToCSV(String inSHP, String outCSV) throws Exception{
		LOGGER.debug("Load "+inSHP);
		ArrayList<Feature> fs = SHPUtil.loadSHP(inSHP).fs;

		LOGGER.debug("Prepare file");
		File file = new File(outCSV);
		if(file.exists()) file.delete();
		file.createNewFile();
		BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));

		LOGGER.debug("Write header");
		ArrayList<String> keys = new ArrayList<String>(fs.get(0).getProperties().keySet());
		int i=0;
		for(String key : keys ){
			bw.write(key.replaceAll(",", ";"));
			if(i<keys.size()-1) bw.write(","); i++;
		}
		bw.write(",geomWKT\n");

		LOGGER.debug("Write data");
		for(Feature f : fs) {
			i=0;
			for(String key : keys){
				Object o = f.get(key);
				bw.write(o==null?"":o.toString().replaceAll(",", ";"));
				if(i<keys.size()-1) bw.write(","); i++;
			}
			bw.write(",");
			bw.write(f.getGeom().toText());
			bw.write("\n");
		}

		bw.close();
	}




	//clip and filter SHP
	public static void extractFilterClip(String in, String out) { extractFilterClip(in, out, null); }
	public static void extractFilterClip(String in, String out, Envelope env) { extractFilterClip(in, out, env, null); }
	public static void extractFilterClip(String in, String out, Envelope env, Filter f) {
		SHPData fsd = SHPUtil.loadSHP(in, f);
		if(env != null) fsd.fs = FeatureUtil.clip(fsd.fs, env);
		SHPUtil.saveSHP(fsd.fs, out, fsd.ft.getCoordinateReferenceSystem());
	}


	//clip all SHP files of a folder
	public static void clip(String inPath, String outPath, Envelope clipEnv) {
		for(File f : FileUtil.getFiles(inPath)) {
			if( !".shp".equals( FileUtil.getFileExtension(f).toLowerCase() ) ) continue;
			extractFilterClip(f.getAbsolutePath(), outPath + f.getName(), clipEnv);
		}
	}

	//delete shapefiles from disk
	public static void delete(String... shpFiles) throws MalformedURLException {
		for(String shpFile : shpFiles) {
			ShpFiles sf = new ShpFiles(new File(shpFile));
			sf.delete();
		}
	}

	//NB: all input files are assumed to have the same geometrical types and the same CRS
	public static void mergeGeoms(String outSHP, boolean delete, String... inSHPs) throws MalformedURLException {
		Collection<Geometry> geoms = new ArrayList<Geometry>();
		CoordinateReferenceSystem crs = null;
		for(String inSHP : inSHPs) {
			if(!new File(inSHP).exists()) continue;
			SHPData dt = SHPUtil.loadSHP(inSHP);
			if(crs == null) crs = dt.ft.getCoordinateReferenceSystem();
			ArrayList<Feature> fs = dt.fs;
			for(Feature f : fs) {
				geoms.add(f.getGeom());
			}
			if(delete) {
				new ShpFiles(new File(inSHP)).delete();
			}
		}
		SHPUtil.saveGeomsSHP(geoms, outSHP, crs);
	}

	//convert shape file to keep only non-multi geometries
	public static void saveAsSimpleGeometry(String inFile, String outFile, boolean showMessages) {
		SHPData data = SHPUtil.loadSHP(inFile);
		if(showMessages) System.out.println(data.fs.size()+" loaded from "+inFile);
		ArrayList<Feature> out = FeatureUtil.getFeaturesWithSimpleGeometrie(data.fs);
		if(showMessages) System.out.println("Result nb: "+out.size());
		SHPUtil.saveSHP(out, outFile, data.ft.getCoordinateReferenceSystem());
	}

}
