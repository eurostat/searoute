package eu.ec.eurostat.searoute;

import java.util.Collection;
import java.util.HashSet;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opencarto.algo.base.Union;
import org.opencarto.datamodel.Feature;
import org.opencarto.datamodel.graph.Edge;
import org.opencarto.datamodel.graph.Graph;
import org.opencarto.datamodel.graph.GraphBuilder;
import org.opencarto.datamodel.graph.Node;
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
			System.out.println(" resPlanifyLines loop");
			sI=lines.size();
			lines = resApplyLines(lines, res);
			lines = planifyLines(lines);
			sF=lines.size();
		}
		return lines;
	}

	private static Collection featuresToLines(Collection fs) {
		Collection lines = new HashSet<Geometry>();
		for(Object f : fs) lines.add(((Feature)f).getGeom());
		return lines;
	}

	private static HashSet<Feature> linesToFeatures(Collection lines) {
		HashSet<Feature> fs = new HashSet<Feature>();
		int i=0;
		for(Object ls : lines) {
			Feature f = new Feature();
			f.id = ""+(i++);
			f.setGeom((Geometry)ls);
			fs.add(f);
		}
		return fs;
	}

	public static void main(String[] args) {
		try {
			System.out.println("Start.");

			double res = 0.05;

			//load input lines
			Collection lines = new HashSet<Geometry>();
			lines.addAll( featuresToLines( GeoJSONUtil.load("resources/marnet/marnet_densified.geojson") ));
			//lines.addAll( GeoJSONUtil.load("/home/juju/geodata/mar_ais_gisco/mar_ais_gisco.geojson") );
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


			HashSet<Feature> fs = linesToFeatures(lines);

			//create graph
			Graph g = GraphBuilder.buildForNetworkFromLinearFeaturesNonPlanar(fs);
			System.out.println("graph: " + g.getNodes().size()+" "+g.getEdges().size()+" "+g.getFaces().size());

			Edge e = findTooShortEdge(g, res*4);
			while(e != null) {
				System.out.println(e);
				removeEdge(g, e);
				e = findTooShortEdge(g, res*4);
			}

			System.out.println("graph: " + g.getNodes().size()+" "+g.getEdges().size()+" "+g.getFaces().size());

			//remove duplicate edges (necessary ?)
			//collapse too short edges
			//deal with too small/narrow faces
			//noding



			/*/check number of connex components
			Collection<Graph> ccs = GraphConnexComponents.get(g);
			for(Graph cc : ccs) {
				System.out.println(" "+cc.getNodes().size());
			}*/

			//TODO
			//check 180/-180 compatibility

			//save output
			GeoJSONUtil.save(fs, "resources/marnet/marnet_working_out.geojson", DefaultGeographicCRS.WGS84);

			System.out.println("Done.");
		} catch (Exception e) { e.printStackTrace(); }
	}





	public static Edge findTooShortEdge(Graph g, double d) {
		for(Edge e : g.getEdges())
			if(e.getGeometry().getLength() < d) return e;
		return null;
	}

	public static void removeEdge(Graph g, Edge e) {
		Node n = e.getN1(), n_ = e.getN2();

		//break link edge/faces
		if(e.f1 != null) { e.f1.getEdges().remove(e); e.f1=null; }
		if(e.f2 != null) { e.f2.getEdges().remove(e); e.f2=null; }

		//delete edge from graph
		g.remove(e);

		//move node n to edge center
		n.moveTo( 0.5*(n.getC().x+n_.getC().x), 0.5*(n.getC().y+n_.getC().y) );

		//make node n origin of all edges starting from node n_
		for(Edge e_:n_.getOutEdges()) e_.setN1(n);
		//make node n destination of all edges going to node n_
		for(Edge e_:n_.getInEdges()) e_.setN2(n);

		//delete node n_ from graph
		g.remove(n_);
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
