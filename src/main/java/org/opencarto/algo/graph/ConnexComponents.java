package org.opencarto.algo.graph;

import java.util.Collection;
import java.util.HashSet;

import org.opencarto.datamodel.graph.Edge;
import org.opencarto.datamodel.graph.Graph;
import org.opencarto.datamodel.graph.Node;
import org.opencarto.util.FeatureUtil;

/**
 * @author julien Gaffuri
 *
 */
public class ConnexComponents {

	//used to specify which edges to use to build the connex components
	public interface EdgeFilter { boolean keep(Edge e); }

	public static Collection<Graph> get(Graph g) { return get(g, null, false); }

	/**
	 * @param g the input graph
	 * @param filter the filter to restrict the connex component construction to specific edges
	 * @param excludeGraphWithSingletonNode set to true to exclude components composed of a singleton node.
	 * @return
	 */
	public static Collection<Graph> get(Graph g, EdgeFilter filter, boolean excludeGraphWithSingletonNode) {
		Collection<Graph> ccs = new HashSet<Graph>();

		Collection<Node> ns = new HashSet<Node>(); ns.addAll(g.getNodes());

		Collection<Edge> es = new HashSet<Edge>();
		if(filter == null)
			es.addAll(g.getEdges());
		else
			for(Edge e : g.getEdges())
				if(filter.keep(e)) es.add(e);

		while(!ns.isEmpty()){
			Node seed = ns.iterator().next();
			Graph cc = get(g, seed, ns, es);
			if(excludeGraphWithSingletonNode && cc.getNodes().size()==1) continue;
			ccs.add(cc);
		}

		return ccs;
	}

	//extract the larger connex graph from seed node ns
	private static Graph get(Graph g_, Node seed, Collection<Node> ns, Collection<Edge> es) {
		ns.remove(seed);
		Graph g = new Graph();
		g.getNodes().add(seed);

		for(Edge e : seed.getOutEdges()){
			if(!es.contains(e)) continue;
			g.getEdges().add(e);
			es.remove(e);
			g = GraphUnion.get(g, get(g_, e.getN2(),ns,es));
		}

		for(Edge e : seed.getInEdges()){
			if(!es.contains(e)) continue;
			g.getEdges().add(e);
			es.remove(e);
			g = GraphUnion.get(g, get(g_, e.getN1(),ns,es));
		}

		return g;
	}

	//return the connex component with the maximum number of nodes
	public static Graph getMainNodeNb(Graph g) {
		Graph gM=null; int nb, maxNb=-1;
		for(Graph g_ : get(g)) {
			nb = g_.getNodes().size();
			if(nb<=maxNb) continue;
			maxNb=nb; gM=g_;
		}
		return gM;
	}

	//print number of nodes of largest graphs
	public static void printNodeNb(Collection<Graph> cc) { printNodeNb(cc,0); }
	public static void printNodeNb(Collection<Graph> ccs, int threshold) {
		int nb;
		for(Graph g : ccs) {
			nb = g.getNodes().size();
			if(nb < threshold) continue;
			System.out.println(nb);
		}
	}


	public static Collection keepOnlyLargestGraphConnexComponents(Collection lines, int minEdgeNumber) {
		Graph g = GraphBuilder.buildFromLinearFeaturesNonPlanar( FeatureUtil.geometriesToFeatures(lines) );
		Collection<Graph> ccs = ConnexComponents.get(g);
		Collection out = new HashSet();
		for(Graph cc : ccs) {
			if( cc.getEdges().size() < minEdgeNumber ) continue;
			for(Edge e : cc.getEdges())
				out.add(e.getGeometry());
		}
		return out;
	}

}
