/**
 * 
 */
package org.opencarto.algo.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opencarto.datamodel.graph.Edge;
import org.opencarto.datamodel.graph.Graph;
import org.opencarto.datamodel.graph.Node;

/**
 * @author julien Gaffuri
 *
 */
public class EdgeCollapse {
	//private final static Logger LOGGER = Logger.getLogger(EdgeCollapse.class.getName());


	//both nodes are collapsed to the center of the edge
	//return the center of the edge, as a trace for debugging purposes
	public static Coordinate collapseEdge(Edge e) {
		Graph g = e.getGraph();

		//get nodes
		Node n1 = e.getN1(), n2 = e.getN2();

		//break link edge/faces
		if(e.f1 != null) { e.f1.getEdges().remove(e); e.f1=null; }
		if(e.f2 != null) { e.f2.getEdges().remove(e); e.f2=null; }

		//remove edge
		g.remove(e);

		//move n1 to edge center
		NodeDisplacement.moveTo( n1, 0.5*(n1.getC().x+n2.getC().x), 0.5*(n1.getC().y+n2.getC().y) );

		//make n1 origin of all edges starting from node n2
		Set<Edge> es;
		es = new HashSet<Edge>(); es.addAll(n2.getOutEdges());
		for(Edge e_ : es) e_.setN1(n1);

		//make n1 destination of all edges going to n2
		es = new HashSet<Edge>(); es.addAll(n2.getInEdges());
		for(Edge e_ : es) e_.setN2(n1);

		//System.out.println(n2.getOutEdges().size() +"   "+ n2.getInEdges().size());

		//remove n2
		g.remove(n2);

		return new Coordinate(n1.getC().x, n1.getC().y);
	}

	//find one edge shorter than a threshold values
	public static Edge findTooShortEdge(Collection<Edge> es, double d) {
		for(Edge e : es)
			if(e.getGeometry().getLength() < d)
				return e;
		return null;
	}

	public static Edge findShortestEdge(Collection<Edge> es) {
		return findShortestEdge(es, Double.MAX_VALUE);
	}
	//find the shortest edge shorter than a threshold d (if any)
	public static Edge findShortestEdge(Collection<Edge> es, double d) {
		Edge eMin = null;
		double lMin = Double.MAX_VALUE;
		for(Edge e : es) {
			double l = e.getGeometry().getLength();
			if(e==null || l<lMin) {
				eMin=e; lMin=l;
			}
		}
		if(lMin<d) return eMin;
		return null;
	}

	//collapse too short edges
	//return the locations where edges have been collapsed, for debugging purposes
	public static Collection<LineString> collapseTooShortEdges(Graph g, double d, boolean startWithShortestEdge) {
		Collection<LineString> out = new ArrayList<LineString>();
		Edge e = startWithShortestEdge? findShortestEdge(g.getEdges(), d) : findTooShortEdge(g.getEdges(), d);
		while(e != null) {
			out.add(e.getGeometry());
			collapseEdge(e);
			e = startWithShortestEdge? findShortestEdge(g.getEdges(), d) : findTooShortEdge(g.getEdges(), d);
		}
		return out;
	}

	public static Collection<LineString> collapseTooShortEdges(Collection<LineString> lines, double d, boolean startWithShortestEdge, boolean planarGraph) {
		//create graph
		Graph g = planarGraph? GraphBuilder.buildFromLinearGeometriesPlanar(lines, false) : GraphBuilder.buildFromLinearGeometriesNonPlanar(lines);
		EdgeCollapse.collapseTooShortEdges(g, d, startWithShortestEdge);
		return GraphUtils.getEdgeGeometries(g);
	}

}
