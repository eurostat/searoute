/**
 * 
 */
package org.opencarto.algo.graph;

import java.util.ArrayList;
import java.util.Collection;

import org.opencarto.algo.distances.Distance;
import org.opencarto.datamodel.graph.Edge;
import org.opencarto.datamodel.graph.Graph;
import org.opencarto.datamodel.graph.Node;

/**
 * @author Julien Gaffuri
 *
 */
public class MinimumSpanningTree {

	public Graph perform(Collection<?> objs, Distance<Object> d) {
		//initialise graphs list
		ArrayList<Graph> graphs = new ArrayList<Graph>();
		for(Object obj:objs){
			Graph g = new Graph();
			Node n = g.buildNode(null);
			n.obj = obj;
			graphs.add(g);
		}

		//build MST
		while(graphs.size()>1){
			//find closest graphs
			Object[] cgs = getClosest(graphs, d);
			//aggregate them
			Graph gAg = aggregate((Graph)cgs[0], (Graph)cgs[1], (Node)cgs[2], (Node)cgs[3], (Double)cgs[4]);
			//
			graphs.add(gAg);
			graphs.remove(cgs[0]);
			graphs.remove(cgs[1]);
			//System.out.println((double)cgs[4]);
		}
		return graphs.get(0);
	}

	private static Graph aggregate(Graph g1, Graph g2, Node n1, Node n2, double edgeValue) {
		Graph gAg = GraphUnion.get(g1, g2);
		Edge e = gAg.buildEdge(n1, n2);
		e.value = edgeValue;
		return gAg;
	}

	private Object[] getClosest(ArrayList<Graph> graphs, Distance<Object> d) {
		Object[] closest = new Object[]{null, null, null, null, Double.MAX_VALUE};
		for(int i=0; i<graphs.size(); i++){
			Graph gi = graphs.get(i);
			for(int j=i+1; j<graphs.size(); j++){
				Object[] dist = distance(gi, graphs.get(j), d);
				if((Double)dist[4]<(Double)closest[4]) closest=dist;
			}
		}
		return closest;
	}

	private Object[] distance(Graph g1, Graph g2, Distance<Object> d) {
		double distMin = Double.MAX_VALUE;
		Node n1Min=null, n2Min=null;
		for(Node n1:g1.getNodes()){
			for(Node n2:g2.getNodes()){
				double dist = d.get(n1.obj,n2.obj);
				if(dist<distMin){
					distMin=dist;
					n1Min=n1; n2Min=n2;
				}
			}
		}
		return new Object[]{g1,g2,n1Min,n2Min,distMin};
	}


	/*
	public static void main(String[] args) {
		System.out.println("Start MST...");

		//load shp
		String path="E:/gaffuju/Desktop/data/import/producers_organisations/";
		SHPData data = SHPUtils.loadSHP(path+"fa_producers_organisations.shp");
		//String path="E:/gaffuju/Desktop/data/import/emodnet_partners/out/";
		//SHPData data = SHPUtils.loadSHP(path+"emodnet_partners_proj.shp");

		System.out.println(" " + data.fs.size());

		Graph graphMST = perform(data.fs, new FeatureEuclidianDistance("the_geom"));
		for(Node n:graphMST.getNodesIterator())
			n.c=((Geometry)((SimpleFeature)n.obj).getAttribute("the_geom")).getCoordinate();

		GraphToSHP.exportEdgesAsSHP(graphMST, path, "MSTedges.shp", 3785);
		GraphToSHP.exportNodesAsSHP(graphMST, path, "MSTnodes.shp", 3785);

		System.out.println("Done");
	}
	 */
}
