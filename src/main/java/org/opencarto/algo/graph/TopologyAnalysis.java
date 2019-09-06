/**
 * 
 */
package org.opencarto.algo.graph;

import org.opencarto.datamodel.graph.Edge;
import org.opencarto.datamodel.graph.Face;
import org.opencarto.datamodel.graph.Node;

/**
 * @author julien Gaffuri
 *
 */
public class TopologyAnalysis {


	//nodes

	public static boolean isSingle(Node n){ return n.getInEdges().size() + n.getOutEdges().size() == 0; }
	public static boolean isDangle(Node n){ return n.getInEdges().size() + n.getOutEdges().size() == 1; }
	public static boolean isCoastal(Node n) {
		for(Edge e : n.getEdges()) if(isCoastal(e)) return true;
		return false;
	}
	public static String getTopologicalType(Node n) {
		if(isSingle(n)) return "single";
		if(isDangle(n)) return "dangle";
		if(isCoastal(n)) return "coastal";
		return "normal";
	}



	//edges

	public static boolean isIsthmus(Edge e){ return e.f1==null && e.f2==null; }
	public static boolean isCoastal(Edge e){ return e.f1==null || e.f2==null; }
	public static String getCoastalType(Edge e) {
		if(isIsthmus(e)) return "isthmus";
		if(isCoastal(e)) return "coastal";
		return "non_coastal";
	}

	public static boolean isClosed(Edge e){ return e.getN1()==e.getN2(); }
	public static boolean isIsolated(Edge e){ return !isClosed(e) && e.getN1().getEdges().size()==1 && e.getN2().getEdges().size()==1; }
	public static boolean isDangle(Edge e){ return !isClosed(e) && e.getN1().getEdges().size()==1 ^ e.getN2().getEdges().size()==1; }
	public static String getTopologicalType(Edge e) {
		if(isDangle(e)) return "dangle";
		if(isIsolated(e)) return "isolated";
		if(isClosed(e)) return "closed";
		return "normal";
	}



	//faces

	public static boolean isCoastal(Face f) {
		for(Edge e : f.getEdges()) if(isCoastal(e)) return true;
		return false;
	}
	public static boolean isEnclave(Face f) {
		if(isCoastal(f)) return false;
		return f.getTouchingFaces().size()==1;
	}
	public static boolean isIsland(Face f) { return f.getTouchingFaces().size()==0; }

	public static String getTopologicalType(Face f) {
		if(isEnclave(f)) return "enclave";
		if(isIsland(f)) return "island";
		if(isCoastal(f)) return "coastal";
		return "normal";
	}

}
