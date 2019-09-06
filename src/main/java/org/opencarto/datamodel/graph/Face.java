/**
 * 
 */
package org.opencarto.datamodel.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.polygonize.Polygonizer;

/**
 * A graph face.
 * It is defined by a set of graph edges.
 * Its geometry is a Polygon, possibly with holes.
 * 
 * @author julien Gaffuri
 *
 */
public class Face extends GraphElement{
	private final static Logger LOGGER = Logger.getLogger(Face.class.getName());

	private static int ID = 0;

	Face(Graph graph, Set<Edge> edges){
		super(graph, "F"+(ID++));
		this.edges = edges;
		updateGeometry();
	}

	//the edges
	private Set<Edge> edges;
	public Set<Edge> getEdges() { return edges; }




	//the geometry, derived from edges geometries with polygoniser
	private Polygon geom = null;
	public Polygon getGeom() { return geom; }

	public void updateGeometry() {
		//remove current geometry from spatial index
		if(geom != null && !geom.isEmpty()) {
			boolean b = getGraph().removeFromSpatialIndex(this);
			if(!b) LOGGER.warn("Could not remove face "+this.getId()+" from spatial index when updating its geometry. NbPoints="+geom.getCoordinates().length);
		}

		geom = null;

		if(getEdges().size() == 0) return;

		//build new geometry with polygoniser
		Polygonizer pg = new Polygonizer();
		for(Edge e : edges) pg.add(e.getGeometry());
		Collection<?> polys = pg.getPolygons();
		pg = null;

		//among all these polygons, holes can be included.
		//the right one is the one whose enveloppe has the largest area
		double maxArea = -1;
		for(Object poly_ : polys){
			Polygon poly = (Polygon)poly_;
			double area = poly.getEnvelopeInternal().getArea();
			if(area < maxArea) continue;
			else if(area > maxArea) {
				maxArea = area;
				geom = poly;
			} else if(area == maxArea && poly.getArea() > geom.getArea()){
				geom = poly;
				//LOGGER.warn("Ambiguity to compute polygonal geometry of "+getId()+" with polygonisation of edges: 2 candidates geometries where found.");
			}
		}

		if(geom == null || geom.isEmpty())
			;//LOGGER.warn("Could not build geometry with polygonisation for face "+getId());
		else
			//update index
			getGraph().insertInSpatialIndex(this);
	}




	//get nodes
	public Set<Node> getNodes() {
		HashSet<Node> ns = new HashSet<Node>();
		for(Edge e:getEdges()){
			ns.add(e.getN1());
			ns.add(e.getN2());
		}
		return ns;
	}

	//get longest edge
	public Edge getLongestEdge() {
		Edge eMax = null; double lMax = -1;
		for(Edge e : getEdges()) {
			double l = e.getGeometry().getLength();
			if(l>lMax) { eMax = e;  lMax = l; }
		}
		return eMax;
	}

	//get touching faces
	public Set<Face> getTouchingFaces() {
		HashSet<Face> out = new HashSet<Face>();
		for(Edge e:getEdges()) out.addAll(e.getFaces());
		out.remove(this);
		return out;
	}



	//cleaning
	public Face clear() {
		if(getEdges() != null) getEdges().clear();
		geom = null;
		return this;
	}

}
