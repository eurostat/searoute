package org.opencarto.datamodel.graph;

import java.util.Collection;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

/**
 * A graph (directed) edge
 * It is defined by an origin node and a destination node.
 * It can be linked to a maximum of two faces.
 * Its geometry is a LineString.
 * 
 * @author julien gaffuri
 * 
 */
public class Edge extends GraphElement{
	private final static Logger LOGGER = Logger.getLogger(Edge.class.getName());

	private static int ID = 0;

	Edge(Graph graph, Node n1, Node n2) { this(graph, n1, n2, new Coordinate[]{n1.getC(), n2.getC()}); }
	Edge(Graph graph, Node n1, Node n2, Coordinate[] coords) {
		super(graph, "E"+(ID++));
		this.n1 = n1;
		this.n2 = n2;
		n1.getOutEdges().add(this);
		n2.getInEdges().add(this);

		if(coords == null) coords = new Coordinate[2];
		this.coords = coords;

		//ensures initial and final coordinates are the ones of the nodes
		coords[0] = getN1().getC();
		coords[coords.length-1] = getN2().getC();

		graph.insertInSpatialIndex(this);
	}

	//the origin node
	private Node n1;
	public Node getN1() { return n1; }
	public void setN1(Node n) {
		boolean b;

		if(n==n1) return;
		boolean samePosition = n1.getC().distance(n.getC()) == 0;

		if(!samePosition){
			b = getGraph().removeFromSpatialIndex(this);
			if(!b) LOGGER.error("Error when changing node 1 of edge "+getId()+". Could not remove it from spatial index.");
		}

		b = n1.getOutEdges().remove(this);
		if(!b) LOGGER.error("Error (1) when changing node 1 of edge "+getId());

		//set
		n1 = n;

		b = n1.getOutEdges().add(this);
		if(!b) LOGGER.error("Error (2) when changing node 1 of edge "+getId());

		coords[0] = n1.getC();

		if(!samePosition) {
			getGraph().insertInSpatialIndex(this);
			if(f1 != null) f1.updateGeometry();
			if(f2 != null) f2.updateGeometry();
		}
	}

	//the destination node
	private Node n2;
	public Node getN2() { return n2; }
	public void setN2(Node n) {
		boolean b;

		if(n==n2) return;
		boolean samePosition = n2.getC().distance(n.getC()) == 0;

		if(!samePosition){
			b = getGraph().removeFromSpatialIndex(this);
			if(!b) LOGGER.error("Error when changing node 2 of edge "+getId()+". Could not remove it from spatial index.");
		}

		b = n2.getInEdges().remove(this);
		if(!b) LOGGER.error("Error (1) when changing node 2 of edge "+getId());

		//set
		n2 = n;

		b = n2.getInEdges().add(this);
		if(!b) LOGGER.error("Error (2) when changing node 2 of edge "+getId());

		coords[coords.length-1] = n2.getC();

		if(!samePosition) {
			getGraph().insertInSpatialIndex(this);
			if(f1 != null) f1.updateGeometry();
			if(f2 != null) f2.updateGeometry();
		}
	}


	//the faces
	public Face f1=null, f2=null;



	//the edge geometry

	private Coordinate[] coords;
	public Coordinate[] getCoords() { return coords; }

	public void setGeom(Coordinate[] coords) {
		boolean b;
		b = getGraph().removeFromSpatialIndex(this);
		if(!b) LOGGER.error("Error when changing geometry of edge "+getId()+". Could not remove it from spatial index.");
		this.coords = coords;
		coords[0] = getN1().getC();
		coords[coords.length-1] = getN2().getC();
		getGraph().insertInSpatialIndex(this);
		if(f1 != null) f1.updateGeometry();
		if(f2 != null) f2.updateGeometry();
	}
	public LineString getGeometry(){
		return new GeometryFactory().createLineString(coords);
	}
	public Coordinate getC() { return getGeometry().getCentroid().getCoordinate(); }






	//get connected edges
	public HashSet<Edge> getEdges() {
		HashSet<Edge> out = new HashSet<Edge>();
		out.addAll(getN1().getInEdges());
		out.addAll(getN1().getOutEdges());
		out.addAll(getN2().getInEdges());
		out.addAll(getN2().getOutEdges());
		out.remove(this);
		return out;
	}

	//get faces as collection
	public Collection<Face> getFaces() {
		HashSet<Face> fs = new HashSet<Face>();
		if(f1 != null) fs.add(f1);
		if(f2 != null) fs.add(f2);
		return fs;
	}

	//cleaning
	public Edge clear() {
		this.n1 = null;
		this.n2 = null;
		this.coords = null;
		this.f1 = null;
		this.f2 = null;
		return this;
	}

}
