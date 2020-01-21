/**
 * 
 */
package eu.europa.ec.eurostat.searoute;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.opengis.feature.simple.SimpleFeature;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.util.GeoDistanceUtil;

/**
 * @author julien Gaffuri
 *
 */
public class SeaRouting {
	//private final static Logger LOGGER = LogManager.getLogger(SeaRouting.class.getName());

	public static final int[] RESOLUTION_KM = new int[] { 100, 50, 20, 10, 5 };

	private Graph g;
	private EdgeWeighter defaultWeighter;
	private EdgeWeighter noSuezWeighter;
	private EdgeWeighter noPanamaWeighter;
	private EdgeWeighter noSuezNoPanamaWeighter;

	public SeaRouting() throws MalformedURLException { this(20); }
	public SeaRouting(int resKM) throws MalformedURLException { this("src/main/webapp/resources/marnet/marnet_plus_"+resKM+"KM.shp"); }
	public SeaRouting(String path) throws MalformedURLException { this(new File(path)); }
	public SeaRouting(File marnetFile) throws MalformedURLException { this(marnetFile.toURI().toURL()); }
	public SeaRouting(URL marnetFileURL) {
		try {
			Map<String, Serializable> map = new HashMap<>();
			map.put( "url", marnetFileURL );
			DataStore store = DataStoreFinder.getDataStore(map);
			FeatureCollection<?,?> fc =  store.getFeatureSource(store.getTypeNames()[0]).getFeatures();
			store.dispose();

			/*InputStream input = marnetFileURL.openStream();
			FeatureCollection fc = new FeatureJSON().readFeatureCollection(input);
			input.close();*/

			//build graph
			FeatureIterator<?> it = fc.features();
			FeatureGraphGenerator gGen = new FeatureGraphGenerator(new LineStringGraphGenerator());
			while(it.hasNext()) gGen.add(it.next());
			g = gGen.getGraph();
			it.close();
		} catch (Exception e) { e.printStackTrace(); }

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

		//define weighters
		defaultWeighter = buildEdgeWeighter(true, true);
		noSuezWeighter = buildEdgeWeighter(false, true);
		noPanamaWeighter = buildEdgeWeighter(true, false);
		noSuezNoPanamaWeighter = buildEdgeWeighter(false, false);
	}

	private EdgeWeighter buildEdgeWeighter(boolean allowSuez, boolean allowPanama) {
		return new DijkstraIterator.EdgeWeighter() {
			public double getWeight(Edge e) {
				//edge around the globe
				if( e.getObject()==null ) return 0;
				SimpleFeature f = (SimpleFeature) e.getObject();
				if(allowSuez && allowPanama)
					return GeoDistanceUtil.getLengthGeoKM((Geometry)f.getDefaultGeometry());
				String desc = (String)f.getAttribute("desc_");
				if(!allowSuez && "suez".equals(desc)) return Double.MAX_VALUE;
				if(!allowPanama && "panama".equals(desc)) return Double.MAX_VALUE;
				return GeoDistanceUtil.getLengthGeoKM((Geometry)f.getDefaultGeometry());
			}
		};
	}




	private Path getShortestPath(Graph g, Node sN, Node dN, EdgeWeighter weighter){
		DijkstraShortestPathFinder pf = new DijkstraShortestPathFinder(g, sN, weighter);
		pf.calculate();
		return pf.getPath(dN);
	}

	//lon,lat
	private Coordinate getPosition(Node n){
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

	//return the distance in km. Distance to closest node
	private double getDistanceToNetworkKM(Coordinate c) {
		return GeoDistanceUtil.getDistanceKM(c, getPosition(getNode(c)));
	}


	//return the route geometry from origin/destination coordinates
	public Feature getRoute(double oLon, double oLat, double dLon, double dLat) { return getRoute(oLon, oLat, dLon, dLat, true, true); }
	public Feature getRoute(double oLon, double oLat, double dLon, double dLat, boolean allowSuez, boolean allowPanama) {
		return getRoute(new Coordinate(oLon,oLat), new Coordinate(dLon,dLat), allowSuez, allowPanama);
	}
	public Feature getRoute(Coordinate oPos, Coordinate dPos, boolean allowSuez, boolean allowPanama) {
		return getRoute(oPos, getNode(oPos), dPos, getNode(dPos), allowSuez, allowPanama);
	}
	//get the route when the node are known
	public Feature getRoute(Coordinate oPos, Node oN, Coordinate dPos, Node dN, boolean allowSuez, boolean allowPanama) {
		GeometryFactory gf = new GeometryFactory();

		//get node positions
		Coordinate oNPos = getPosition(oN), dNPos = getPosition(dN);

		//test if route should be based on network
		//route do not need network if straight line between two points is smaller than the total distance to reach the network
		double dist = -1;
		dist = GeoDistanceUtil.getDistanceKM(oPos, dPos);
		double distN = -1;
		distN = GeoDistanceUtil.getDistanceKM(oPos, oNPos) + GeoDistanceUtil.getDistanceKM(dPos, dNPos);

		if(dist>=0 && distN>=0 && distN > dist){
			//return direct route
			Feature rf = new Feature();
			rf.setDefaultGeometry( gf.createMultiLineString(new LineString[]{ gf.createLineString(new Coordinate[]{oPos,dPos}) }) );
			return rf;
		}

		//Compute dijkstra from start node
		EdgeWeighter w = allowPanama & allowSuez ? defaultWeighter : allowPanama? noSuezWeighter : allowSuez ? noPanamaWeighter : noSuezNoPanamaWeighter;
		Path path = null;
		synchronized (g) {
			path = getShortestPath(g, oN, dN, w);
		}


		if(path == null) {
			Feature rf = new Feature();
			rf.setDefaultGeometry(null);
			return rf;
		}

		//build line geometry
		LineMerger lm = new LineMerger();
		for(Object o : path.getEdges()){
			Edge e = (Edge)o;
			SimpleFeature f = (SimpleFeature) e.getObject();
			if(f==null) continue;
			Geometry mls = (Geometry)f.getDefaultGeometry();
			lm.add(mls);
		}
		//add first and last parts
		lm.add(gf.createLineString(new Coordinate[]{oPos,oNPos}));
		lm.add(gf.createLineString(new Coordinate[]{dPos,dNPos}));

		Collection<?> lss = lm.getMergedLineStrings();
		Feature rf = new Feature();
		rf.setDefaultGeometry( gf.createMultiLineString( lss.toArray(new LineString[lss.size()]) ) );
		rf.setAttribute("dFromKM", GeoDistanceUtil.getDistanceKM(oPos, oNPos));
		rf.setAttribute("dToKM", GeoDistanceUtil.getDistanceKM(dPos, dNPos));
		return rf;
	}



	public Collection<Feature> filterPorts(Collection<Feature> ports, double minDistToNetworkKM) {
		Collection<Feature> pls = new HashSet<Feature>();
		for(Feature p : ports)
			if(getDistanceToNetworkKM(p.getDefaultGeometry().getCoordinate()) <= minDistToNetworkKM)
				pls.add(p);
		return pls;
	}


	public Collection<Feature> getRoutes(Collection<Feature> ports, String idProp) { return getRoutes(ports, idProp); }
	public Collection<Feature> getRoutes(Collection<Feature> ports, String idProp, boolean allowSuez, boolean allowPanama) {
		if(idProp == null) idProp = "ID";

		List<Feature> portsL = new ArrayList<Feature>(); portsL.addAll(ports);
		int nb=portsL.size(); nb=(nb*(nb-1))/2; int cnt=0;

		HashSet<Feature> srs = new HashSet<Feature>();
		for(int i=0; i<portsL.size(); i++) {
			Feature pi = portsL.get(i);
			for(int j=i+1; j<portsL.size(); j++) {
				Feature pj = portsL.get(j);
				System.out.println(pi.getAttribute(idProp) + " - " + pj.getAttribute(idProp) + " - " + (100*(cnt++)/nb) + "%");
				Feature sr = getRoute(pi.getDefaultGeometry().getCoordinate(), pj.getDefaultGeometry().getCoordinate(), allowSuez, allowPanama);
				Geometry geom = sr.getDefaultGeometry();
				sr.setAttribute("dkm", geom==null? -1 : GeoDistanceUtil.getLengthGeoKM(geom));
				sr.setAttribute("from", pi.getAttribute(idProp));
				sr.setAttribute("to", pj.getAttribute(idProp));
				srs.add(sr);
			}
		}
		return srs;
	}

	/*
	public static void main(String[] args) throws Exception {
		System.out.println("Start");

		//create the routing object
		SeaRouting sr = new SeaRouting();

		//get the route between Marseille (5.3E,43.3N) and Shanghai (121.8E,31.2N)
		Feature route = sr.getRoute(5.3, 43.3, 121.8, 31.2, false, true);

		//compute the distance in km
		MultiLineString routeGeom = (MultiLineString) route.getGeom();
		double d = GeoDistanceUtil.getLengthGeoKM(routeGeom);

		//export the route in geoJSON format
		String rgj = GeoJSONUtil.toGeoJSON(routeGeom);

		LOGGER.info(route);
		LOGGER.info(routeGeom);
		LOGGER.info(d);
		LOGGER.info(rgj);

		System.out.println("End");
	}
	 */

}
