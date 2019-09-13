/**
 * 
 */
package org.opencarto.algo.graph;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.opencarto.datamodel.graph.Edge;
import org.opencarto.datamodel.graph.Graph;
import org.opencarto.datamodel.graph.Node;

/**
 * @author julien Gaffuri
 *
 */
public class EdgeMerging {
	private final static Logger LOGGER = Logger.getLogger(EdgeMerging.class.getName());

	//merge two edges into a new single one. e1 should be the first edge, finishing at node n. e2 should start at node n
	//edge e1 is transformed to include also e2 - n and e2 are removed from the graph.
	public static void merge(Graph g, Edge e1, Node n, Edge e2) {

		if(TopologyAnalysis.isClosed(e1) || TopologyAnalysis.isClosed(e2)){
			LOGGER.error("Cannot merge edges if one of them is closed.");
			return;
		}

		if(e1.getN2() != n || e2.getN1() != n) {
			LOGGER.error("Cannot merge edges: Elements not well ordered");
			return;
		}

		LOGGER.debug("merge edges at node "+n.getId() +" "+ n.getC());

		//build new edge geometry
		int nb1 = e1.getCoords().length, nb2 = e2.getCoords().length;
		Coordinate[] coords = new Coordinate[nb1+nb2-1];
		for(int i=0; i<nb1; i++) coords[i] = e1.getCoords()[i];
		for(int i=nb1; i<nb1+nb2-1; i++) coords[i] = e2.getCoords()[i-nb1+1];

		//store final node
		Node n2 = e2.getN2();

		//disconnect and remove e2
		if(e2.f1!=null) { e2.f1.getEdges().remove(e2); e2.f1=null; }
		if(e2.f2!=null) { e2.f2.getEdges().remove(e2); e2.f2=null; }
		g.remove(e2);

		//update e1 with new geometry and new final node
		e1.setGeom(coords);
		e1.setN2(n2);

		//remove middle node
		g.remove(n);
	}

}
