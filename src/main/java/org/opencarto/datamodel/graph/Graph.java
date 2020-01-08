package org.opencarto.datamodel.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.quadtree.Quadtree;

/**
 * Valued and oriented graph.
 * A spatial index is defined for each of the graph element types: nodes, edges and faces.
 * 
 * @author julien Gaffuri
 *
 */
public class Graph {
	private final static Logger LOGGER = LogManager.getLogger(Graph.class.getName());

	//nodes
	private Set<Node> nodes = new HashSet<Node>();
	public Set<Node> getNodes() { return nodes; }
	//edges
	private Set<Edge> edges = new HashSet<Edge>();
	public Set<Edge> getEdges() { return edges; }
	//faces
	private Set<Face> faces = new HashSet<Face>();
	public Set<Face> getFaces() { return faces; }



	//build a node
	public Node buildNode(Coordinate c){
		Node n = new Node(this,c);
		nodes.add(n);
		return n;
	}

	//build an edge
	public Edge buildEdge(Node n1, Node n2){ return buildEdge(n1, n2, null); }
	public Edge buildEdge(Node n1, Node n2, Coordinate[] coords){
		Edge e = new Edge(this, n1, n2, coords);
		edges.add(e);
		return e;
	}

	//build a face
	public Face buildFace(Set<Edge> edges) {
		Face f = new Face(this, edges);
		for(Edge e : edges){
			if(e.f1==null) e.f1=f;
			else if(e.f2==null) e.f2=f;
			else LOGGER.error("Error when building face "+f.getId()+". Edge "+e.getId()+" is already linked to two faces: "+e.f1.getId()+" and "+e.f2.getId());
		}
		faces.add(f);
		return f;
	}




	//Remove a node from the graph. The node is supposed not to be linked to any edge.
	public void remove(Node n) {
		boolean b;

		//remove
		b = nodes.remove(n);
		if(!b) LOGGER.error("Error when removing node "+n.getId()+". Not in graph nodes list. Position="+n.getC());

		//remove from spatial index
		b = removeFromSpatialIndex(n);
		if(!b) LOGGER.error("Error when removing node "+n.getId()+". Not in spatial index. Position="+n.getC());

		//check if some edges are still linked to node
		if(n.getEdges().size()>0) {
			String st=""; for(Edge e : n.getEdges()) st+=" "+e.getId();
			LOGGER.error("Error when removing node "+n.getId()+". Edges are still linked to it (nb="+n.getEdges().size()+")"+st+". Position="+n.getC());
		}

		//check if some faces are still linked to node
		if(n.getFaces().size()>0) {
			String st=""; for(Face f : n.getFaces()) st+=" "+f.getId();
			LOGGER.error("Error when removing node "+n.getId()+". Faces are still linked to it (nb="+n.getFaces().size()+")"+st+". Position="+n.getC());
		}
	}

	//Remove an edge from a graph. The edge is supposed not to be linked to any face.
	public void remove(Edge e) {
		boolean b;

		//remove
		b = edges.remove(e);
		if(!b) LOGGER.error("Error when removing edge "+e.getId()+". Not in graph edges list. Position="+e.getC());

		//remove from spatial index
		b = removeFromSpatialIndex(e);
		if(!b) LOGGER.error("Error when removing edge "+e.getId()+". Not in spatial index. Position="+e.getC());

		//break link with nodes
		b = e.getN1().getOutEdges().remove(e);
		if(!b) LOGGER.error("Error when removing edge "+e.getId()+". Not in N1 out edges. Position="+e.getN1().getC());
		b = e.getN2().getInEdges().remove(e);
		if(!b) LOGGER.error("Error when removing edge "+e.getId()+". Not in N2 in edges. Position="+e.getN2().getC());

		//check if faces are still linked to edge
		if(e.f1 != null) LOGGER.error("Error when removing edge "+e.getId()+". It is still linked to face "+e.f1+". Position="+e.getC());
		if(e.f2 != null) LOGGER.error("Error when removing edge "+e.getId()+". It is still linked to face "+e.f2+". Position="+e.getC());

		//clear
		e.clear();
	}
	public void removeAll(Collection<Edge> es) { for(Edge e : es) remove(e); }


	//Remove a face
	public void remove(Face f) {
		boolean b;

		//remove
		b = getFaces().remove(f);
		if(!b) LOGGER.error("Could not remove face "+f.getId()+" from graph");

		//remove from spatial index
		b = removeFromSpatialIndex(f);
		if(!b) LOGGER.error("Error when removing face "+f.getId()+". Not in spatial index. Position="+f.getGeom().getCentroid());

		//break link with edges
		for(Edge e : f.getEdges()){
			if(e.f1==f) e.f1 = null;
			else if(e.f2==f) e.f2 = null;
			else LOGGER.error("Could not remove link between face "+f.getId()+" and edge "+e.getId()+". Edge was not linked to the face.");
		}

		//clear
		f.clear();
	}





	//support for spatial queries

	//nodes
	private Quadtree spIndNode = new Quadtree();
	public void insertInSpatialIndex(Node n){ spIndNode.insert(new Envelope(n.getC()), n); }
	public boolean removeFromSpatialIndex(Node n){ return spIndNode.remove(new Envelope(n.getC()), n); }
	@SuppressWarnings("unchecked")
	public Collection<Node> getNodesAt(Envelope env) { return (Collection<Node>)spIndNode.query(env); }
	public Node getNodeAt(Coordinate c) {
		for(Node n : getNodesAt(new Envelope(c))) if(c.distance(n.getC()) == 0) return n;
		return null;
	}
	public Node getCreateNodeAt(Coordinate c) {
		Node n = getNodeAt(c);
		if(n!=null) return n;
		return this.buildNode(c);
	}

	//edges
	private Quadtree spIndEdge = new Quadtree();
	public void insertInSpatialIndex(Edge e){ spIndEdge.insert(e.getGeometry().getEnvelopeInternal(), e); }
	public boolean removeFromSpatialIndex(Edge e){ return spIndEdge.remove(e.getGeometry().getEnvelopeInternal(), e); }
	@SuppressWarnings("unchecked")
	public Collection<Edge> getEdgesAt(Envelope env) { return (Collection<Edge>)spIndEdge.query(env); }

	//faces
	private Quadtree spIndFace = new Quadtree();
	public void insertInSpatialIndex(Face f){ spIndFace.insert(f.getGeom().getEnvelopeInternal(), f); }
	public boolean removeFromSpatialIndex(Face f){ return spIndFace.remove(f.getGeom().getEnvelopeInternal(), f); }
	@SuppressWarnings("unchecked")
	public Collection<Face> getFacesAt(Envelope env) { return (Collection<Face>)spIndFace.query(env); }





	//retrieve graph elements by id
	public Node getNode(String id){ return (Node)getElt(id, getNodes()); }
	public Edge getEdge(String id){ return (Edge)getElt(id, getEdges()); }
	public Face getFace(String id){ return (Face)getElt(id, getFaces()); }
	private GraphElement getElt(String id, Set<? extends GraphElement> elts){
		for(GraphElement ge:elts) if(ge.getId().equals(id)) return ge;
		return null;
	}

	//find edges linking two nodes
	public Set<Edge> getEdge(Node n1, Node n2) {
		Set<Edge> out = new HashSet<>();
		Envelope env = new Envelope(n1.getC(), n2.getC()); env.expandBy(0.1, 0.1);
		for(Edge e : getEdgesAt(env))
			if(e.getN1()==n1 && e.getN2()==n2)
				out.add(e);
		return out;
	}


	public void clear() {
		for(Node n : getNodes()) { n.getInEdges().clear(); n.getOutEdges().clear(); }
		getNodes().clear();
		for(Edge e : getEdges()) e.clear();
		getEdges().clear();
		for(Face f : getFaces()) f.clear();
		getFaces().clear();
	}

}
