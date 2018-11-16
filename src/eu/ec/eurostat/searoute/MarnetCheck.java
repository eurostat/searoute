package eu.ec.eurostat.searoute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opencarto.algo.base.Union;
import org.opencarto.datamodel.Feature;
import org.opencarto.io.GeoJSONUtil;
import org.opencarto.util.JTSGeomUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

public class MarnetCheck {

	private static Collection lineMerge(Collection lines) {
		LineMerger lm = new LineMerger();
		lm.add(lines);
		return lm.getMergedLineStrings();
	}

	private static Collection planifyLines(Collection lines) {
		Geometry u = Union.getLineUnion(lines);
		return JTSGeomUtil.getLineStringGeometries(u);
	}

	private static Collection filterGeom(Collection lines, double d) {
		Collection out = new HashSet<Geometry>();
		for(Object line : lines) out.add( DouglasPeuckerSimplifier.simplify((Geometry)line, d) );
		return out;
	}

	private static Collection resPlanifyLines(Collection lines, double res) {
		lines = resApplyLines(lines, res);
		lines = planifyLines(lines);
		int sI=1,sF=0;
		while(sF<sI) {
			sI=lines.size();
			lines = resApplyLines(lines, res);
			lines = planifyLines(lines);
			sF=lines.size();
		}
		return lines;
	}


	public static void main(String[] args) {
		try {
			System.out.println("Start.");

			double res = 0.2;

			//load input lines
			ArrayList<Feature> fs = GeoJSONUtil.load("resources/marnet/marnet_densified.geojson");
			Collection lines = new HashSet<Geometry>();
			for(Feature f : fs) lines.add(f.getGeom());
			fs.clear(); fs = null;
			System.out.println(lines.size());

			lines = resPlanifyLines(lines, res);
			System.out.println(lines.size() + " resPlanifyLines");

			lines = lineMerge(lines);
			System.out.println(lines.size() + " lineMerge");

			lines = filterGeom(lines, res);
			System.out.println(lines.size() + " filterGeom");

			lines = resPlanifyLines(lines, res);
			System.out.println(lines.size() + " resPlanifyLines");

			lines = lineMerge(lines);
			System.out.println(lines.size() + " lineMerge");

			//create graph
			/*Graph g = GraphBuilder.buildForNetworkFromLinearFeatures(fsOut);
			Collection<Graph> ccs = GraphConnexComponents.get(g);
			for(Graph cc : ccs) {
				System.out.println(" "+cc.getNodes().size());
			}*/

			//TODO
			//check number of connex components
			//remove duplicate network edges - always keep shorter
			//check 180/-180 compatibility

			ArrayList<Feature> fsOut = new ArrayList<Feature>();
			int i=0;
			for(Object ls : lines) {
				Feature f = new Feature();
				f.id = ""+(i++);
				f.setGeom((Geometry)ls);
				fsOut.add(f);
			}

			//save output
			GeoJSONUtil.save(fsOut, "resources/marnet/marnet_working_out.geojson", DefaultGeographicCRS.WGS84);

			System.out.println("Done.");
		} catch (Exception e) { e.printStackTrace(); }
	}



	public static Collection resApplyLines(Collection lines, double res) {
		resApply_(lines, res);
		return resRemoveDuplicateCoordsLinear(lines);
	}

	public static Collection resRemoveDuplicateCoordsLinear(Collection lines) {
		Collection out = new HashSet<Geometry>();
		for(Object lineO : lines) {
			Geometry line = (Geometry) lineO;
			if(line.getLength() == 0) continue;
			Collection<Geometry> line_ = new HashSet<>(); line_.add(line);
			Geometry u = Union.getLineUnion(line_);
			if(u.isEmpty()) continue;
			if(u instanceof Point) continue;
			out.addAll(JTSGeomUtil.getLineStringGeometries(u));
		}
		return out;
	}




	public static void resApply_(Collection gs, double res) {
		for(Object g : gs) resApply((Geometry)g, res);
	}

	public static void resApply(Geometry g, double res) {
		resApply(g.getCoordinates(), res);
	}


	public static void resApply(Coordinate c, double res){
		c.x = Math.round(c.x/res)*res;
		c.y = Math.round(c.y/res)*res;
	}
	public static void resApply(Coordinate[] cs, double res){
		for(Coordinate c : cs) resApply(c, res);
	}


}
