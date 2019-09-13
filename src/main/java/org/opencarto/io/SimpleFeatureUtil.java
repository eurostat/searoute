/**
 * 
 */
package org.opencarto.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.opencarto.datamodel.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Conversion functions from GT SimpleFeatures from/to OC features
 * 
 * @author julien Gaffuri
 *
 */
public class SimpleFeatureUtil {
	//private final static Logger LOGGER = Logger.getLogger(SimpleFeatureUtil.class);

	//SimpleFeature to feature
	public static Feature get(SimpleFeature sf, String[] attNames){
		Feature f = new Feature();
		//geom
		//f.setGeom(JTSGeomUtil.clean( (Geometry)sf.getProperty("the_geom").getValue() ));
		Property pg = sf.getProperty("the_geom");
		if(pg==null) pg = sf.getProperty("geometry");
		f.setGeom( (Geometry)pg.getValue() );
		//attributes
		for(String attName : attNames) f.set(attName, sf.getProperty(attName).getValue());
		return f;
	}
	public static Feature get(SimpleFeature sf){ return get(sf, getAttributeNames(sf.getFeatureType())); }

	public static ArrayList<Feature> get(SimpleFeatureCollection sfs) {
		SimpleFeatureIterator it = sfs.features();
		ArrayList<Feature> fs = new ArrayList<Feature>();
		String[] attNames = getAttributeNames(sfs.getSchema());
		while( it.hasNext()  )
			fs.add(get(it.next(), attNames));
		it.close();
		return fs;
	}


	//feature to SimpleFeature
	public static SimpleFeature get(Feature f){ return get(f, getFeatureType(f)); }
	public static SimpleFeature get(Feature f, CoordinateReferenceSystem crs){ return get(f, getFeatureType(f,crs)); }
	public static SimpleFeature get(Feature f, SimpleFeatureType ft){
		String[] attNames = getAttributeNames(ft);
		Object[] atts = new Object[attNames.length+1];
		atts[0] = f.getGeom();
		for(int i=0; i<attNames.length; i++) atts[i+1] = f.get(attNames[i]);
		return new SimpleFeatureBuilder(ft).buildFeature(f.id, atts);
	}
	public static SimpleFeatureCollection get(Collection<? extends Feature> fs, CoordinateReferenceSystem crs) { return get(fs, crs, null); }
	public static SimpleFeatureCollection get(Collection<? extends Feature> fs, CoordinateReferenceSystem crs, List<String> atts) {
		if(fs.size()==0) return new DefaultFeatureCollection(null, null);
		SimpleFeatureType ft = getFeatureType(fs.iterator().next(), crs, atts);
		DefaultFeatureCollection sfc = new DefaultFeatureCollection(null, ft);
		SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(ft);
		String[] attNames = getAttributeNames(ft);
		for(Feature f:fs) {
			Object[] data = new Object[attNames.length+1];
			data[0] = f.getGeom();
			for(int i=0; i<attNames.length; i++) data[i+1] = f.get(attNames[i]);
			sfc.add( sfb.buildFeature(f.id, data) );
		}
		return sfc;
	}


	/*public static ArrayList<SimpleFeature> toCollection(SimpleFeatureCollection sfs) {
		ArrayList<SimpleFeature> fs = new ArrayList<SimpleFeature>();
		FeatureIterator<SimpleFeature> it = sfs.features();
		try { while(it.hasNext()) fs.add(it.next()); }
		finally { it.close(); }
		return fs;
	}
	public static SimpleFeatureCollection toCollection(Collection<SimpleFeature> sfs) {
		DefaultFeatureCollection sfc = new DefaultFeatureCollection(null, sfs.iterator().next().getFeatureType());
		for(SimpleFeature sf:sfs) sfc.add(sf);
		return sfc;
	}*/



	private static SimpleFeatureType getFeatureType(Feature f) {
		return getFeatureType(f, null);
	}
	public static SimpleFeatureType getFeatureType(Feature f, CoordinateReferenceSystem crs) {
		List<String> atts = new ArrayList<String>();
		atts.addAll(f.getProperties().keySet());
		return getFeatureType(f, crs, atts);
	}
	public static SimpleFeatureType getFeatureType(Feature f, CoordinateReferenceSystem crs, List<String> atts) {
		if(atts == null) {
			atts = new ArrayList<String>();
			atts.addAll(f.getProperties().keySet());
		}
		return getFeatureType( f.getGeom().getGeometryType(), crs, atts );
	}

	public static SimpleFeatureType getFeatureType(String geomType) {
		return getFeatureType(geomType, null);
	}
	public static SimpleFeatureType getFeatureType(String geomType, CoordinateReferenceSystem crs) {
		return getFeatureType(geomType, crs, new String[]{});
	}
	public static SimpleFeatureType getFeatureType(String geomType, CoordinateReferenceSystem crs, List<String> data) {
		return getFeatureType(geomType, crs, data.toArray(new String[data.size()]));
	}
	public static SimpleFeatureType getFeatureType(String geomType, CoordinateReferenceSystem crs, String[] data) {
		try {
			SimpleFeatureType schema = getFeatureType(geomType, -1, data);
			return DataUtilities.createSubType(schema, null, crs);
		} catch (SchemaException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static SimpleFeatureType getFeatureType(String geomType, int epsgCode, String[] data) {
		String datast = "";
		for(String data_ : data) datast += ","+data_;
		return getFeatureType(geomType, epsgCode, datast==""? "" : datast.substring(1, datast.length()));
	}
	public static SimpleFeatureType getFeatureType(String geomType, int epsgCode, String data) {
		try {
			String st = "";
			st = "the_geom:"+geomType;
			if(epsgCode>0) st += ":srid="+epsgCode;
			if(data!=null) st += ","+data;
			return DataUtilities.createType("ep", st);
		} catch (SchemaException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String[] getAttributeNames(SimpleFeatureType ft){
		ArrayList<String> atts = new ArrayList<String>();
		for(int i=0; i<ft.getAttributeCount(); i++){
			String att = ft.getDescriptor(i).getLocalName();
			if("the_geom".equals(att)) continue;
			if("GEOM".equals(att)) continue;
			atts.add(att);
		}
		return atts.toArray(new String[atts.size()]);
	}

	public static <T extends Geometry> Collection<Feature> getFeaturesFromGeometries(Collection<T> geoms) {
		ArrayList<Feature> fs = new ArrayList<Feature>();
		for(Geometry geom : geoms){
			Feature f = new Feature();
			f.setGeom(geom);
			fs.add(f);
		}
		return fs;
	}




	/*public static void main(String[] args) {

		SimpleFeatureType sch = SHPUtil.loadSHP("/home/juju/Bureau/nuts_gene_data/test/test.shp").ft;

		for(int i=0; i<sch.getAttributeCount(); i++){
			System.out.println(sch.getDescriptor(i).getLocalName());
			System.out.println(sch.getDescriptor(i).getType());
		}

		Feature f = new Feature(); f = new Feature(); f = new Feature();
		f.props.put("type", "lalala");
		f.props.put("truc", "pspsps");
		f.setGeom(new GeometryFactory().createPoint(new Coordinate(15,48)));
		System.out.println(f.id);
		System.out.println(getFeatureType(f));
	}*/

}
