/**
 * 
 */
package org.opencarto.datamodel.graph;

/**
 * Generic class for graph elements: Node, Edge, Face
 * 
 * @author julien gaffuri
 *
 */
public abstract class GraphElement {

	//the graph it belongs to
	private Graph graph;
	public Graph getGraph() { return graph; }

	//the id
	private String id;
	public String getId(){ return id; }

	public GraphElement(Graph graph, String id){
		this.graph = graph;
		this.id = id;
	}

	//an object linked to the element
	public Object obj;
	//a value attached to the element
	public double value;

	@Override
	public String toString() {
		return getClass().getSimpleName()+"-"+getId();
	}

}
