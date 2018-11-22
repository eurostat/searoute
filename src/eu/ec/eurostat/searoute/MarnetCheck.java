package eu.ec.eurostat.searoute;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opencarto.algo.base.Union;
import org.opencarto.algo.distances.HausdorffDistance;
import org.opencarto.algo.graph.GraphConnexComponents;
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

	//TODO
	//for thin triangles (with short height), remove the longest segment
	//add port connections from GISCO DB
	//ensure no land intersection

	public static void main(String[] args) {
		try {
			System.out.println("Start");
			for(double res : new double[] { 0.5, 0.2, 0.1, 0.05, 0.025 }) {
				System.out.println("*** res="+res);
				Collection lines = make(res, "resources/marnet/marnet_densified.geojson", "/home/juju/geodata/gisco/mar_ais_gisco.geojson", "/home/juju/geodata/gisco/ef.geojson");
				System.out.println("save...");
				GeoJSONUtil.save(linesToFeatures(lines), "resources/marnet/marnet_plus_"+res+".geojson", DefaultGeographicCRS.WGS84);
			}
			System.out.println("Done");
		} catch (Exception e) { e.printStackTrace(); }
	}

	private static Collection make(double res, String... datasources) {
		//load input lines
		Collection lines = new HashSet<Geometry>();

		//data sources preparation
		for(String ds : datasources) {
			System.out.println(" preparing "+ds);
			Collection ds_ = featuresToLines( GeoJSONUtil.load(ds));
			ds_ = prepare(ds_, res);
			lines.addAll(ds_);
		}

		System.out.println("Total before integration: " + lines.size());

		lines = planifyLines(lines);						System.out.println(lines.size() + " planifyLines");
		lines = lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = filterGeom(lines, res);						System.out.println(lines.size() + " filterGeom");
		lines = removeSimilarDuplicateEdges(lines, res);	System.out.println(lines.size() + " removeSimilarDuplicateEdges");
		lines = dtsePlanifyLines(lines, res);				System.out.println(lines.size() + " dtsePlanifyLines");
		lines = lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = planifyLines(lines);						System.out.println(lines.size() + " planifyLines");
		lines = lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = dtsePlanifyLines(lines, res);				System.out.println(lines.size() + " dtsePlanifyLines");
		lines = lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = planifyLines(lines);						System.out.println(lines.size() + " planifyLines");
		lines = lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = dtsePlanifyLines(lines, res);				System.out.println(lines.size() + " dtsePlanifyLines");
		lines = lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = resPlanifyLines(lines, res*0.01);			System.out.println(lines.size() + " resPlanifyLines");
		lines = lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = resPlanifyLines(lines, res*0.01);			System.out.println(lines.size() + " resPlanifyLines");

		//run with -Xss4m
		lines = keepOnlyLargestGraphConnexComponents(lines, 50);	System.out.println(lines.size() + " keepOnlyLargestGraphConnexComponents");

		return lines;
	}

	private static Collection prepare(Collection lines, double res) {
		lines = planifyLines(lines);						System.out.println(lines.size() + " planifyLines");
		lines = lineMerge(lines);							System.out.println(lines.size() + " lineMerge");
		lines = filterGeom(lines, res);						System.out.println(lines.size() + " filterGeom");
		lines = removeSimilarDuplicateEdges(lines, res);	System.out.println(lines.size() + " removeSimilarDuplicateEdges");
		lines = dtsePlanifyLines(lines, res);				System.out.println(lines.size() + " dtsePlanifyLines");
		lines = resPlanifyLines(lines, res*0.01);			System.out.println(lines.size() + " resPlanifyLines");
		return lines;
	}







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










	private static Collection keepOnlyLargestGraphConnexComponents(Collection lines, int minEdgeNumber) {
		Graph g = GraphBuilder.buildForNetworkFromLinearFeaturesNonPlanar( linesToFeatures(lines) );
		Collection<Graph> ccs = GraphConnexComponents.get(g);
		Collection out = new HashSet();
		for(Graph cc : ccs) {
			if( cc.getEdges().size() < minEdgeNumber ) continue;
			for(Edge e : cc.getEdges())
				out.add(e.getGeometry());
		}
		return out;
	}

	public static Collection removeSimilarDuplicateEdges(Collection lines, double haussdorffDistance) {
		Graph g = GraphBuilder.buildForNetworkFromLinearFeaturesNonPlanar( linesToFeatures(lines) );
		removeSimilarDuplicateEdges(g, haussdorffDistance);
		Collection out = new HashSet();
		for(Edge e : g.getEdges()) out.add(e.getGeometry());
		return out;
	}

	public static void removeSimilarDuplicateEdges(Graph g, double haussdorffDistance) {
		Edge e = findSimilarDuplicateEdgeToRemove(g, haussdorffDistance);
		while(e != null) {
			g.remove(e);
			e = findSimilarDuplicateEdgeToRemove(g, haussdorffDistance);
		}
	}

	public static Edge findSimilarDuplicateEdgeToRemove(Graph g, double haussdorffDistance) {
		for(Edge e : g.getEdges()) {
			for(Edge e_ : e.getN1().getOutEdges())
				if(e!=e_ && e_.getN2() == e.getN2() && new HausdorffDistance(e.getGeometry(),e_.getGeometry()).getDistance()<haussdorffDistance)
					return getLonger(e,e_);
			for(Edge e_ : e.getN2().getOutEdges())
				if(e!=e_ && e_.getN2() == e.getN1() && new HausdorffDistance(e.getGeometry(),e_.getGeometry()).getDistance()<haussdorffDistance)
					return getLonger(e,e_);
		}
		return null;
	}

	public static Edge getLonger(Edge e1, Edge e2) {
		double d1 = e1.getGeometry().getLength();
		double d2 = e2.getGeometry().getLength();
		if(d1<d2) return e2; else return e1;
	}



	private static Collection dtsePlanifyLines(Collection lines, double res) {
		lines = deleteTooShortEdge(lines, res);
		lines = planifyLines(lines);
		int sI=1,sF=0;
		while(sF<sI) {
			System.out.println(" dtsePlanifyLines loop   " + lines.size());
			sI=lines.size();
			lines = deleteTooShortEdge(lines, res);
			lines = planifyLines(lines);
			sF=lines.size();
		}
		return lines;
	}

	public static Collection deleteTooShortEdge(Collection lines, double d) {
		//create graph
		Graph g = GraphBuilder.buildForNetworkFromLinearFeaturesNonPlanar( linesToFeatures(lines) );
		deleteTooShortEdge(g, d);
		Collection out = new HashSet();
		for(Edge e : g.getEdges()) out.add(e.getGeometry());
		return out;
	}

	public static void deleteTooShortEdge(Graph g, double d) {
		Edge e = findTooShortEdge(g, d);
		while(e != null) {
			removeEdge(g, e);
			e = findTooShortEdge(g, d);
		}
	}


	//TODO shortest?
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
		Set<Edge> es;
		es = new HashSet<Edge>(); es.addAll(n_.getOutEdges());
		for(Edge e_ : es) e_.setN1(n);
		//make node n destination of all edges going to node n_
		es = new HashSet<Edge>(); es.addAll(n_.getInEdges());
		for(Edge e_ : es) e_.setN2(n);

		//delete node n_ from graph
		g.remove(n_);
	}









	private static Collection resPlanifyLines(Collection lines, double res) {
		lines = resApplyLines(lines, res);
		lines = planifyLines(lines);
		int sI=1,sF=0;
		while(sF<sI) {
			System.out.println(" resPlanifyLines loop" + lines.size());
			sI=lines.size();
			lines = resApplyLines(lines, res);
			lines = planifyLines(lines);
			sF=lines.size();
		}
		return lines;
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
		c.x = ((int)Math.round(c.x/res)) * res;
		c.y = ((int)Math.round(c.y/res)) * res;
	}
	public static void resApply(Coordinate[] cs, double res){
		for(Coordinate c : cs) resApply(c, res);
	}


}
