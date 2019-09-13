/**
 * 
 */
package org.opencarto.algo.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.opencarto.datamodel.graph.Edge;
import org.opencarto.datamodel.graph.Graph;
import org.opencarto.datamodel.graph.Node;

/**
 * @author julien Gaffuri
 *
 */
public class NodeReduction {
	private final static Logger LOGGER = Logger.getLogger(NodeReduction.class.getName());


	//specify when a node can be reduced or not
	interface NodeReductionCriteria {
		public boolean isReducable(Node n);
	}

	//the default case: 2 edges that are not closed
	private static class DefaultNodeReductionCriteria implements NodeReductionCriteria {
		@Override
		public boolean isReducable(Node n) {
			ArrayList<Edge> es = n.getEdgesAsList();
			if(es.size() != 2) return false;
			Iterator<Edge> it = es.iterator();
			Edge e1 = it.next(), e2 = it.next();
			//happens in case of closed edge
			if(e1==e2) return false;
			return true;
		}
	}

	public static NodeReductionCriteria DEFAULT_NODE_REDUCTION_CRITERIA = new DefaultNodeReductionCriteria();




	//ensure node reduction. If it is reducable, merge the two edges and remove the node.
	//returns the deleted edge
	public static Edge ensure(Node n, NodeReductionCriteria nrc) {
		if(! nrc.isReducable(n)) return null;

		if(n.getEdges().size() != 2) {
			LOGGER.warn("Found reducable node with number of edges different to 2 around "+n.getC()+". Nb="+n.getEdges().size());
			return null;
		}

		//get edges to merge
		Iterator<Edge> it = n.getEdges().iterator();
		Edge e1 = it.next(), e2 = it.next();

		//ensure proper order between e1 -> n -> e2 before merging
		if(e1.getN2() == n && e2.getN1() == n) {}
		else if(e1.getN1() == n && e2.getN2() == n) {
			Edge e=e1; e1=e2; e2=e;
		} else if(e1.getN2() == n && e2.getN2() == n) {
			e2 = GraphUtils.revert(e2);
		} else if(e1.getN1() == n && e2.getN1() == n) {
			e1 = GraphUtils.revert(e1);
		} else {
			LOGGER.warn("Unhandled case in node reduction");
			return null;
		}

		//merge edges
		EdgeMerging.merge(n.getGraph(), e1, n, e2);
		return e2;
	}
	public static Edge ensure(Node n) {
		return ensure(n, DEFAULT_NODE_REDUCTION_CRITERIA);
	}

	//ensure reduction of several nodes
	//return the deleted edges
	public static Collection<Edge> ensure(Collection<Node> ns, NodeReductionCriteria nrc) {
		Collection<Edge> out = new ArrayList<>();
		for(Node n : ns) {
			Edge e = ensure(n, nrc);
			if(e != null) out.add(e);
		}
		return out;
	}
	public static Collection<Edge> ensure(Collection<Node> ns) {
		return ensure(ns, DEFAULT_NODE_REDUCTION_CRITERIA);
	}



	public static Collection<Edge> ensure(Graph g, NodeReductionCriteria nrc) {
		return ensure(new ArrayList<Node>( g.getNodes() ), nrc);
	}
	public static Collection<Edge> ensure(Graph g) {
		return ensure(g, DEFAULT_NODE_REDUCTION_CRITERIA);
	}

}
