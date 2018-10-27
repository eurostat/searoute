/**
 * 
 */
package eu.ec.eurostat.searoute;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.graph.build.feature.FeatureGraphGenerator;
import org.geotools.graph.build.line.LineStringGraphGenerator;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;
import org.geotools.graph.structure.basic.BasicEdge;
import org.geotools.graph.traverse.standard.DijkstraIterator;
import org.geotools.graph.traverse.standard.DijkstraIterator.EdgeWeighter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.linemerge.LineMerger;

/**
 * @author julien Gaffuri
 *
 */
public class SeaRouting {

	private Graph g;
	private EdgeWeighter weighter;

	public SeaRouting() { this("WebContent/resources/shp/marnet.shp"); }
	public SeaRouting(String shpPath) {
		try {
			//load features from shp file
			File file = new File(shpPath);
			if(!file.exists()) throw new IOException("File "+shpPath+" does not exist.");
			Map<String, Serializable> map = new HashMap<>();
			map.put( "url", file.toURI().toURL() );
			DataStore store = DataStoreFinder.getDataStore(map);
			FeatureCollection fc =  store.getFeatureSource(store.getTypeNames()[0]).getFeatures();

			//build graph
			FeatureIterator<?> it = fc.features();
			FeatureGraphGenerator gGen = new FeatureGraphGenerator(new LineStringGraphGenerator());
			while(it.hasNext()) gGen.add(it.next());
			g = gGen.getGraph();
			it.close();
			store.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}

		//link nodes around the globe
		for(Object o : g.getNodes()){
			Node n = (Node)o;
			Coordinate c = ((Point)n.getObject()).getCoordinate();
			if(c.x==180) {
				Node n_ = getNode(new Coordinate(-c.x,c.y));
				//System.out.println(c + " -> " + ((Point)n_.getObject()).getCoordinate());
				BasicEdge be = new BasicEdge(n, n_);
				n.getEdges().add(be);
				n_.getEdges().add(be);
				g.getEdges().add(be);
			}
		}

		//define weighter
		weighter = new DijkstraIterator.EdgeWeighter() {
			public double getWeight(Edge e) {
				//edge around the globe
				if( e.getObject()==null ) return 0;

				SimpleFeature f = (SimpleFeature) e.getObject();
				MultiLineString line = (MultiLineString) f.getDefaultGeometry();
				//return line.getLength();
				return Utils.getLengthGeo(line);
			}
		};

	}



	private Path getShortestPath(Graph g, Node sN, Node dN, EdgeWeighter weighter){
		DijkstraShortestPathFinder pf = new DijkstraShortestPathFinder(g, sN, weighter);
		pf.calculate();
		return pf.getPath(dN);
	}

	//lon,lat
	public Coordinate getPosition(Node n){
		if(n==null) return null;
		Point pt = (Point)n.getObject();
		if(pt==null) return null;
		return pt.getCoordinate();
	}

	//get closest node from a position (lon,lat)
	public Node getNode(Coordinate c){
		double dMin = Double.MAX_VALUE;
		Node nMin=null;
		for(Object o : g.getNodes()){
			Node n = (Node)o;
			double d = getPosition(n).distance(c); //TODO fix that !
			//double d=Utils.getDistance(getPosition(n), c);
			if(d==0) return n;
			if(d<dMin) {dMin=d; nMin=n;}
		}
		return nMin;
	}

	//return the route geometry from origin/destination coordinates
	public MultiLineString getRoute(double oLon, double oLat, double dLon, double dLat) {
		return getRoute(new Coordinate(oLon,oLat), new Coordinate(dLon,dLat));
	}
	public MultiLineString getRoute(Coordinate oPos, Coordinate dPos) {
		return getRoute(oPos, getNode(oPos), dPos, getNode(dPos));
	}
	//get the route when the node are known
	public MultiLineString getRoute(Coordinate oPos, Node oN, Coordinate dPos, Node dN) {
		//get node positions
		Coordinate oNPos = getPosition(oN), dNPos = getPosition(dN);

		//test if route should be based on network
		//route do not need network if straight line between two points is smaller than the total distance to reach the network
		double dist = -1;
		try { dist = Utils.getDistance(oPos,dPos); } catch (Exception e) {}
		double distN = -1;
		try { distN = Utils.getDistance(oPos,oNPos) + Utils.getDistance(dPos,dNPos); } catch (Exception e) {}

		if(dist>=0 && distN>=0 && distN > dist){
			//return direct
			GeometryFactory gf = new GeometryFactory();
			return gf.createMultiLineString(new LineString[]{ gf.createLineString(new Coordinate[]{oPos,dPos}) });
		}

		//Compute dijkstra from start node
		Path path = null;
		synchronized (g) {
			path = getShortestPath(g, oN,dN,weighter);
		}

		if(path == null) return null;

		//build line geometry
		LineMerger lm = new LineMerger();
		for(Object o : path.getEdges()){
			Edge e = (Edge)o;
			SimpleFeature f = (SimpleFeature) e.getObject();
			if(f==null) continue;
			MultiLineString mls = (MultiLineString) f.getDefaultGeometry();
			lm.add(mls);
		}
		//add first and last parts
		GeometryFactory gf = new GeometryFactory();
		lm.add(gf.createLineString(new Coordinate[]{oPos,oNPos}));
		lm.add(gf.createLineString(new Coordinate[]{dPos,dNPos}));

		Collection<?> lss = lm.getMergedLineStrings();
		return gf.createMultiLineString( lss.toArray(new LineString[lss.size()]) );
	}

/*
	public static void main(String[] args) {
		SeaRouting sr = new SeaRouting();
		//get from origin () to destination ()
		MultiLineString geom = sr.getRoute(5.3, 43.3, 121.8, 31.2);
		System.out.println(geom);
		double dist = Utils.getLengthGeo(geom);
		System.out.println(dist);
		String gj = Utils.toGeoJSON(geom);
		System.out.println(gj);

MULTILINESTRING ((121.8 31.2, 122.6775513 30.8345108, 121.3000031 27.7999897, 120 25.70000076, 117 22.99999046, 114.1000137 21.69998932, 112.0494766 16.96088028, 110.1000214 12.19997978, 107.3207092 7.609139919, 104.5999908 3, 103.6000137 1.100000024, 102 2.000010014, 100.6000137 3.199990034, 97 6.999989986, 94.00003052 6.699989796, 90.00006104 6.466430187, 89.99999237 6.466609955, 85.94792938 6.198349953, 81.89998627 5.899970055, 80.09999847 5.799990177, 73.70467377 7.606029987, 51 13.00000954, 45 11.99997044, 43.29999924 12.69999027, 42 14.99997997, 41.20000076 16.2999897, 38.90000916 20.74999046, 37 23.59999084, 34.5 26.99998093, 33.75001144 27.89998055, 32.59999847 29.70000076, 32.16669846 30.95000076, 32.16669083 30.99999046, 32.09999084 31.69998932, 27.62981033 33.32801056, 23.00000954 34.79999924, 15.19999027 36.40000153, 11 37.49998093, 9.649999619 38.90000153, 9.999990463 41.01998138, 6 42.79999924, 4.999989986 42.99998856, 5.3 43.3))
16445.686589005585
{"type":"MultiLineString","coordinates":[[[121.8,31.2],[122.6776,30.8345],[121.3,27.8],[120,25.7],[117,23],[114.1,21.7],[112.0495,16.9609],[110.1,12.2],[107.3207,7.6091],[104.6,3],[103.6,1.1],[102,2],[100.6,3.2],[97,7],[94,6.7],[90.0001,6.4664],[90,6.4666],[85.9479,6.1983],[81.9,5.9],[80.1,5.8],[73.7047,7.606],[51,13],[45,12],[43.3,12.7],[42,15],[41.2,16.3],[38.9,20.75],[37,23.6],[34.5,27],[33.75,27.9],[32.6,29.7],[32.1667,30.95],[32.1667,31],[32.1,31.7],[27.6298,33.328],[23,34.8],[15.2,36.4],[11,37.5],[9.65,38.9],[10,41.02],[6,42.8],[5,43],[5.3,43.3]]]}

	
	}
*/
}
