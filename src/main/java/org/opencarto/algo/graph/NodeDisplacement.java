/**
 * 
 */
package org.opencarto.algo.graph;

import org.locationtech.jts.geom.Coordinate;
import org.opencarto.datamodel.graph.Face;
import org.opencarto.datamodel.graph.Node;

/**
 * @author julien Gaffuri
 *
 */
public class NodeDisplacement {

	public static void moveTo(Node n, double x, double y) {
		if(n.getC().distance(new Coordinate(x,y))==0) return;

		//move position, updating the spatial index
		n.getGraph().removeFromSpatialIndex(n);
		n.getC().x = x;
		n.getC().y = y;
		n.getGraph().insertInSpatialIndex(n);

		//update faces geometries
		for(Face f : n.getFaces()) f.updateGeometry();

		//update edges coords
		//for(Edge e:getOutEdges()) e.coords[0]=getC();
		//for(Edge e:getInEdges()) e.coords[e.coords.length-1]=getC();
	}

}
