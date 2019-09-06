package org.opencarto.datamodel.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * A graph node.
 * It is located somewhere and is linked to incoming and outcoming edges.
 * 
 * @author julien gaffuri
 *
 */
public class Node extends GraphElement{
	//private final static Logger LOGGER = Logger.getLogger(Node.class.getName()));

	private static int ID = 0;

	Node(Graph graph, Coordinate c){
		super(graph, "N"+(ID++));
		this.c = c;
		graph.insertInSpatialIndex(this);
	}


	//the node position
	private Coordinate c;
	public Coordinate getC() { return c; }

	//geometry
	public Point getGeometry(){
		return new GeometryFactory().createPoint(c);
	}


	//the edges, incoming and outgoing
	private Set<Edge> inEdges = new HashSet<Edge>();
	public Set<Edge> getInEdges() { return inEdges; }
	private Set<Edge> outEdges = new HashSet<Edge>();
	public Set<Edge> getOutEdges() { return outEdges; }

	public Set<Edge> getEdges() {
		Set<Edge> out = new HashSet<Edge>();
		out.addAll(inEdges); out.addAll(outEdges);
		return out;
	}
	public ArrayList<Edge> getEdgesAsList() {
		ArrayList<Edge> out = new ArrayList<Edge>();
		out.addAll(getInEdges());
		out.addAll(getOutEdges());
		return out;
	}

	//get list of faces (computed on-the-fly)
	public HashSet<Face> getFaces(){
		HashSet<Face> faces = new HashSet<Face>();
		for(Edge e : getOutEdges()) faces.addAll(e.getFaces());
		return faces;
	}

}
