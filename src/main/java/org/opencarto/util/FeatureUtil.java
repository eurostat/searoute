/**
 * 
 */
package org.opencarto.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.opencarto.datamodel.Feature;

/**
 * @author julien Gaffuri
 *
 */
public class FeatureUtil {
	private final static Logger LOGGER = Logger.getLogger(FeatureUtil.class.getName());

	//spatial indexing
	public static <T extends Feature> STRtree getSTRtree(Collection<T> fs) {
		STRtree index = new STRtree();
		for(Feature f : fs) index.insert(f.getGeom().getEnvelopeInternal(), f);
		return index;
	}
	public static <T extends Feature> Quadtree getQuadtree(Collection<T> fs) {
		Quadtree index = new Quadtree();
		for(Feature f : fs) index.insert(f.getGeom().getEnvelopeInternal(), f);
		return index;
	}

	public static <T extends Feature> STRtree getSTRtreeCoordinates(Collection<T> fs) {
		STRtree index = new STRtree();
		for(Feature f : fs) {
			for(Coordinate c : f.getGeom().getCoordinates())
				//TODO ensure no coordinate at same location ?
				index.insert(new Envelope(c), c);
		}
		return index;
	}


	//get envelope of features
	public static <T extends Feature> Envelope getEnvelope(Collection<T> features) { return getEnvelope(features, 1); }
	public static <T extends Feature> Envelope getEnvelope(Collection<T> features, double enlargementFactor) {
		if(features.size() == 0) {
			LOGGER.warn("No features in partition - cannot compute envelope");
			return null;
		}
		Envelope env = features.iterator().next().getGeom().getEnvelopeInternal();
		for(Feature f : features) env.expandToInclude(f.getGeom().getEnvelopeInternal());
		env.expandBy((enlargementFactor-1)*env.getWidth(), (enlargementFactor-1)*env.getHeight());
		return env;
	}

	public static <T extends Feature> Coordinate getMedianPosition(Collection<T> fs) {
		Coordinate c = new Coordinate();
		{
			ArrayList<Double> s = new ArrayList<Double>(); double[] s_;
			for(Feature f : fs) for(Coordinate c_ : f.getGeom().getCoordinates()) s.add(c_.x);
			s_ = ArrayUtils.toPrimitive(s.toArray(new Double[s.size()]));
			c.x = StatUtils.percentile(s_ ,50);
			s_ = null;
			s.clear();
		}{
			ArrayList<Double> s = new ArrayList<Double>(); double[] s_;
			for(Feature f : fs) for(Coordinate c_ : f.getGeom().getCoordinates()) s.add(c_.y);
			s_ = ArrayUtils.toPrimitive(s.toArray(new Double[s.size()]));
			c.y = StatUtils.percentile(s_ ,50);
			s_ = null;
			s_ = null;
			s.clear();
		}
		return c;
	}

	//check if an attribute is an identifier (that is it is unique)
	public static <T extends Feature> HashMap<String,Integer> checkIdentfier(Collection<T> fs, String idAtt) {
		//build id count index
		HashMap<String,Integer> index = new HashMap<String,Integer>();
		for(Feature f : fs) {
			Object id_ = f.get(idAtt);
			if(id_ == null) {
				LOGGER.warn("Could not find attribute " + idAtt + " for feature " + f.id);
				continue;
			}
			String id = id_.toString();
			Integer count = index.get(id);
			if(count == null) index.put(id, 1); else index.put(id, count+1);
		}
		//keep only the elements with more that one count
		HashMap<String,Integer> out = new HashMap<String,Integer>();
		for(Entry<String,Integer> e : index.entrySet())
			if(e.getValue() > 1) out.put(e.getKey(), e.getValue());
		return out;
	}

	//check if an attribute is an identifier (that is it is unique)
	public static <T extends Feature> int getVerticesNumber(Collection<T> fs) {
		int nb=0;
		for(Feature f : fs) {
			if(f.getGeom() == null) {
				LOGGER.warn("Could not count the number of vertices of feature "+f.id+": Null geometry.");
				continue;
			}
			nb += f.getGeom().getNumPoints();
		}
		return nb;
	}

	//considering multi/polygonal features, get the patches that are smallest than an area threshold
	public static <T extends Feature> ArrayList<Map<String, Object>> getInfoSmallPolygons(Collection<T> fs, double areaThreshold) {
		ArrayList<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		for(Feature f : fs) {
			Collection<Geometry> polys = JTSGeomUtil.getGeometries( JTSGeomUtil.getPolygonal(f.getGeom()) );
			for(Geometry poly : polys) {
				double area = poly.getArea();
				if( area > areaThreshold ) continue;
				Map<String, Object> m = new HashMap<String, Object>();
				m.put("id", f.id);
				m.put("area", area);
				m.put("position", poly.getCentroid().getCoordinate());
				out.add(m);
			}
		}
		return out;
	}

	public static <T extends Feature> Collection<Geometry> getGeometries(Collection<T> fs) {
		Collection<Geometry> gs = new ArrayList<Geometry>();
		for(Feature f : fs) gs.add(f.getGeom());
		return gs ;
	}
	public static <T extends Feature> Collection<MultiLineString> getGeometriesMLS(ArrayList<T> fs) {
		Collection<MultiLineString> gs = new ArrayList<MultiLineString>();
		for(Feature f : fs) gs.add((MultiLineString) f.getGeom());
		return gs ;
	}
	public static <T extends Feature> Collection<LineString> getGeometriesLS(ArrayList<T> fs) {
		Collection<LineString> gs = new ArrayList<LineString>();
		for(Feature f : fs) gs.add((LineString) f.getGeom());
		return gs ;
	}




	public static void dissolveById(Collection<Feature> fs) {
		//index features by id
		HashMap<String,List<Feature>> ind = new HashMap<String,List<Feature>>();
		for(Feature f : fs) {
			List<Feature> col = ind.get(f.id);
			if(col == null) {
				col = new ArrayList<Feature>();
				ind.put(f.id, col);
			}
			col.add(f);
		}

		//merge features having same id
		for(List<Feature> col : ind.values()) {
			if(col.size() == 1) continue;
			Collection<MultiPolygon> polys = new ArrayList<MultiPolygon>();
			for(Feature f : col) polys.add((MultiPolygon) f.getGeom());
			MultiPolygon mp = (MultiPolygon) JTSGeomUtil.toMulti(CascadedPolygonUnion.union(polys));
			for(int i=1; i<col.size(); i++) fs.remove(col.get(i));
			col.get(0).setGeom(mp);
		}
	}

	public static Collection<Feature> dissolve(Collection<Feature> fs, String propName) {
		//index features by property
		HashMap<String,List<Feature>> ind = new HashMap<String,List<Feature>>();
		for(Feature f : fs) {
			String prop = (String) f.get(propName);
			List<Feature> col = ind.get(prop);
			if(col == null) {
				col = new ArrayList<Feature>();
				ind.put(prop, col);
			}
			col.add(f);
		}

		//merge features having same property
		Collection<Feature> out = new ArrayList<Feature>();
		for(Entry<String,List<Feature>> e : ind.entrySet()) {
			Feature f = new Feature();
			f.set(propName, e.getKey());
			Collection<MultiPolygon> polys = new ArrayList<MultiPolygon>();
			for(Feature f_ : e.getValue()) polys.add((MultiPolygon) f_.getGeom());
			MultiPolygon mp = (MultiPolygon) JTSGeomUtil.toMulti(CascadedPolygonUnion.union(polys));
			f.setGeom(mp);
			out.add(f);
		}
		return out;
	}


	public static Collection<Feature> toFeatures(Collection<Map<String, Object>> ps) {
		Collection<Feature> out = new ArrayList<Feature>();
		for(Map<String, Object> p : ps) {
			Feature f = new Feature();
			f.getProperties().putAll(p);
			out.add(f);
		}
		return out;
	}
	public static Collection<Feature> toFeatures(ArrayList<HashMap<String, String>> ps) {
		Collection<Feature> out = new ArrayList<Feature>();
		for(Map<String, String> p : ps) {
			Feature f = new Feature();
			f.getProperties().putAll(p);
			out.add(f);
		}
		return out;
	}


	//get all property values
	public static Set<String> getPropValues(Collection<Feature> fs, String propKey) {
		Set<String> out = new HashSet<String>();
		for(Feature f : fs) out.add(f.get(propKey).toString());
		return out;
	}
	//get all property values
	public static List<String> getPropValuesAsList(Collection<Feature> fs, String propKey) {
		List<String> out = new ArrayList<String>();
		out.addAll(getPropValues(fs, propKey));
		return out;
	}

	public static HashMap<String, Feature> index(Collection<Feature> fs, String indexKey) {
		HashMap<String, Feature> out = new HashMap<String, Feature>();
		for(Feature f : fs) out.put(f.get(indexKey).toString(), f);
		return out;
	}

	//get set of attribute keys of several features
	public static Set<String> getAttributesSet(Feature... fs) {
		Set<String> keys = new HashSet<>();
		for(Feature f : fs)
			keys.addAll(f.getProperties().keySet());
		return keys;
	}


	//keep only features with non empty geometry
	public static Collection<Feature> filterFeaturesWithNonEmptyGeometries(Collection<Feature> fs) {
		HashSet<Feature> out = new HashSet<Feature>();
		for(Feature f : fs) if(!f.getGeom().isEmpty()) out.add(f);
		return out;
	}

	//warning: the new features are not true copies.
	public static <T extends Feature> ArrayList<Feature> getFeaturesWithSimpleGeometrie(Collection<T> in) {
		ArrayList<Feature> out = new ArrayList<Feature>();
		for(Feature f : in)
			out.addAll(getFeaturesWithSimpleGeometrie(f));
		return out;
	}

	//warning: the new features are not true copies.
	public static <T extends Feature> ArrayList<Feature> getFeaturesWithSimpleGeometrie(T f) {
		ArrayList<Feature> out = new ArrayList<Feature>();
		if(f.getGeom() == null || f.getGeom().isEmpty()) return out;
		for(Geometry g : JTSGeomUtil.getGeometries(f.getGeom())) {
			Feature f2 = new Feature();
			f2.getProperties().putAll(f.getProperties());
			f2.setGeom(g);
			out.add(f2);
		}
		return out;
	}


	//ensure that a the features do not have geometrycollection with more than one element.
	//If they have one, change the geom into a simple one.
	public static void ensureGeometryNotAGeometryCollection(Collection<Feature> fs) {
		for(Feature f : fs)
			ensureGeometryNotAGeometryCollection(f);
	}
	public static void ensureGeometryNotAGeometryCollection(Feature f) {
		if(!(f.getGeom() instanceof GeometryCollection)) return;
		GeometryCollection gc = (GeometryCollection) f.getGeom();
		if(gc.getNumGeometries() != 1)
			LOGGER.warn("Input geometries should not be a geometrycollection (" + gc.getClass().getSimpleName() + "). nb=" + gc.getNumGeometries() + " props=" + f.getProperties());
		f.setGeom( JTSGeomUtil.toSimple(gc) );
	}




	/*public static Collection<Feature> clip(Collection<Feature> fs, Envelope env, String geomType) {
		HashSet<Feature> out = new HashSet<Feature>();
		Collection<Feature> clipped = clip(fs, env);
		for(Feature f : clipped) {
			if(geomType.equals(f.getGeom().getGeometryType())) {
				out.add(f);
				continue;
			}
			if()
		}
		return out;
	}*/


	public static ArrayList<Feature> clip(Collection<Feature> fs, Envelope env) {
		ArrayList<Feature> out = new ArrayList<Feature>();
		Polygon envG = JTS.toGeometry(env);
		for(Feature f : fs) {
			if(!env.intersects(f.getGeom().getEnvelopeInternal())) continue;
			if(env.contains(f.getGeom().getEnvelopeInternal())) {
				out.add(f);
				continue;
			}
			Geometry inter = null;
			try {
				inter = f.getGeom().intersection(envG);
			} catch (Exception e) {
				e.printStackTrace();
				inter = f.getGeom();
			}
			if(inter == null || inter.isEmpty()) continue;
			f.setGeom(inter);
			out.add(f);
		}
		return out;
	}




	public static <T extends Feature> Collection<Geometry> featuresToGeometries(Collection<T> fs) {
		Collection<Geometry> gs = new HashSet<>();
		for(T f : fs) gs.add(f.getGeom());
		return gs;
	}

	public static <T extends Geometry> HashSet<Feature> geometriesToFeatures(Collection<T> geoms) {
		HashSet<Feature> fs = new HashSet<Feature>();
		int i=0;
		for(T g : geoms) {
			Feature f = new Feature();
			f.id = ""+(i++);
			f.setGeom(g);
			fs.add(f);
		}
		return fs;
	}

}
