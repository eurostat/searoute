/**
 * 
 */
package org.opencarto.algo.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opencarto.algo.distances.HausdorffDistance;
import org.opencarto.datamodel.graph.Edge;
import org.opencarto.datamodel.graph.Face;
import org.opencarto.datamodel.graph.Graph;
import org.opencarto.datamodel.graph.Node;

/**
 * 
 * Various algorithms on graphs.
 * 
 * @author julien Gaffuri
 *
 */
public class GraphUtils {


	//return edges in common between two faces (if any)
	public static Set<Edge> getEdgesInCommon(Face f1, Face f2) {
		Set<Edge> out = new HashSet<Edge>();
		for(Edge e : f2.getEdges()) if(e.f1==f1 || e.f2==f1) out.add(e);
		return out;
	}

	//return the length of the boundary between two faces
	public static double getLength(Face f1, Face f2) {
		double length = 0;
		for(Edge e : getEdgesInCommon(f1, f2))
			length += e.getGeometry().getLength();
		return length;
	}



	//if the edge is closed, return the are. Return -1 else.
	public static double getArea(Edge e) {
		if(!TopologyAnalysis.isClosed(e)) return -1;
		return new GeometryFactory().createPolygon(e.getCoords()).getArea();
	}


	//remove edges with similar geometries (based on haussdorff distance)
	//the edges are supposed not to be linked to any face.
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
					return getLongest(e,e_);
			for(Edge e_ : e.getN2().getOutEdges())
				if(e!=e_ && e_.getN2() == e.getN1() && new HausdorffDistance(e.getGeometry(),e_.getGeometry()).getDistance()<haussdorffDistance)
					return getLongest(e,e_);
		}
		return null;
	}

	public static Edge getLongest(Edge e1, Edge e2) {
		double d1 = e1.getGeometry().getLength();
		double d2 = e2.getGeometry().getLength();
		if(d1<d2) return e2; else return e1;
	}


	/**
	 * Check if two edges are connected. If so, return the connection node.
	 * 
	 * @param e1
	 * @param e2
	 * @return
	 */
	public static Node areConnected(Edge e1, Edge e2) {
		if(e1.getN1() == e2.getN1()) return e1.getN1();
		if(e1.getN1() == e2.getN2()) return e1.getN1();
		if(e1.getN2() == e2.getN1()) return e1.getN2();
		if(e1.getN2() == e2.getN2()) return e1.getN2();
		return null;
	}


	//revert the edge orientation. Return the edge itself (not a copy)
	public static Edge revert(Edge e) {

		//revert coordinate list
		Coordinate[] cs = e.getCoords();
		Coordinate[] cs_ = new Coordinate[cs.length];
		for(int i=0; i<cs.length; i++)
			cs_[i] = cs[cs.length-1-i];

		//Revert nodes
		Node n=e.getN1(); e.setN1(e.getN2()); e.setN2(n);

		//update geometry
		e.setGeom(cs_);

		return e;
	}


	//return edge geometries
	public static Collection<LineString> getEdgeGeometries(Graph g) { return getEdgeGeometries(g.getEdges()); }
	public static Collection<LineString> getEdgeGeometries(Collection<Edge> es) {
		Collection<LineString> out = new HashSet<>();
		for(Edge e : es) out.add(e.getGeometry());
		return out;
	}

}
