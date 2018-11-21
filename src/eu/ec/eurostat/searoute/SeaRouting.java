/**
 * 
 */
package eu.ec.eurostat.searoute;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
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
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opencarto.datamodel.Feature;
import org.opencarto.io.GeoJSONUtil;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.linemerge.LineMerger;

/**
 * @author julien Gaffuri
 *
 */
public class SeaRouting {

	private Graph g;
	private EdgeWeighter weighter;

	public SeaRouting() throws MalformedURLException { this("WebContent/resources/marnet/marnet_plus.geojson"); }
	//public SeaRouting() throws MalformedURLException { this("WebContent/resources/shp/marnet.shp"); }
	public SeaRouting(String shpPath) throws MalformedURLException { this(new File(shpPath)); }
	public SeaRouting(File marnetFile) throws MalformedURLException { this(marnetFile.toURI().toURL()); }
	public SeaRouting(URL marnetFileURL) {
		try {
			/*Map<String, Serializable> map = new HashMap<>();
			map.put( "url", marnetFileURL );
			DataStore store = DataStoreFinder.getDataStore(map);
			FeatureCollection fc =  store.getFeatureSource(store.getTypeNames()[0]).getFeatures();
			store.dispose();*/

			InputStream input = marnetFileURL.openStream();
			FeatureCollection fc = new FeatureJSON().readFeatureCollection(input);
			input.close();

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

		//define weighter
		weighter = new DijkstraIterator.EdgeWeighter() {
			public double getWeight(Edge e) {
				//edge around the globe
				if( e.getObject()==null ) return 0;
				SimpleFeature f = (SimpleFeature) e.getObject();
				return Utils.getLengthGeo((Geometry)f.getDefaultGeometry());
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
		return Utils.getDistance(c, getPosition(getNode(c)));
	}


	//return the route geometry from origin/destination coordinates
	public Feature getRoute(double oLon, double oLat, double dLon, double dLat) {
		return getRoute(new Coordinate(oLon,oLat), new Coordinate(dLon,dLat));
	}
	public Feature getRoute(Coordinate oPos, Coordinate dPos) {
		return getRoute(oPos, getNode(oPos), dPos, getNode(dPos));
	}
	//get the route when the node are known
	public Feature getRoute(Coordinate oPos, Node oN, Coordinate dPos, Node dN) {
		GeometryFactory gf = new GeometryFactory();

		//get node positions
		Coordinate oNPos = getPosition(oN), dNPos = getPosition(dN);

		//test if route should be based on network
		//route do not need network if straight line between two points is smaller than the total distance to reach the network
		double dist = -1;
		dist = Utils.getDistance(oPos, dPos);
		double distN = -1;
		distN = Utils.getDistance(oPos, oNPos) + Utils.getDistance(dPos, dNPos);

		if(dist>=0 && distN>=0 && distN > dist){
			//return direct route
			Feature rf = new Feature();
			rf.setGeom( gf.createMultiLineString(new LineString[]{ gf.createLineString(new Coordinate[]{oPos,dPos}) }) );
			return rf;
		}

		//Compute dijkstra from start node
		Path path = null;
		synchronized (g) {
			path = getShortestPath(g, oN,dN,weighter);
		}


		if(path == null) {
			Feature rf = new Feature();
			rf.setGeom(null);
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
		rf.setGeom( gf.createMultiLineString( lss.toArray(new LineString[lss.size()]) ) );
		rf.getProperties().put("dFromKM", Utils.getDistance(oPos, oNPos));
		rf.getProperties().put("dToKM", Utils.getDistance(dPos, dNPos));
		return rf;
	}



	public Collection<Feature> filterPorts(Collection<Feature> ports, double minDistToNetworkKM) {
		Collection<Feature> pls = new HashSet<Feature>();
		for(Feature p : ports)
			if(getDistanceToNetworkKM(p.getGeom().getCoordinate()) <= minDistToNetworkKM)
				pls.add(p);
		return pls;
	}


	public Collection<Feature> getRoutes(Collection<Feature> ports, String idProp) {
		if(idProp == null) idProp = "ID";

		List<Feature> portsL = new ArrayList<Feature>(); portsL.addAll(ports);
		int nb=portsL.size(); nb=(nb*(nb+1))/2; int cnt=0;

		HashSet<Feature> srs = new HashSet<Feature>();
		for(int i=0; i<portsL.size(); i++) {
			Feature pi = portsL.get(i);
			for(int j=i+1; j<portsL.size(); j++) {
				System.out.println(100*(cnt++)/nb);
				Feature pj = portsL.get(j);
				Feature sr = getRoute(pi.getGeom().getCoordinate(), pj.getGeom().getCoordinate());
				Geometry geom = sr.getGeom();
				sr.getProperties().put("dkm", geom==null? -1 : Utils.getLengthGeo(geom));
				sr.getProperties().put("from", pi.getProperties().get(idProp));
				sr.getProperties().put("to", pj.getProperties().get(idProp));
				srs.add(sr);
			}
		}
		return srs;
	}





	public static void main(String[] args) throws MalformedURLException {
		System.out.println("Start");

		//load ports
		Collection<Feature> ports = GeoJSONUtil.load("/home/juju/geodata/gisco/port_pt_2013_WGS84_ext.geojson");
		System.out.println(ports.size());

		SeaRouting sr = new SeaRouting();
		ports = sr.filterPorts(ports, 0.1);
		System.out.println(ports.size());

		//TODO check route include beginning and end
		
		Collection<Feature> rs = sr.getRoutes(ports, "PORT_ID");

		GeoJSONUtil.save(rs, "/home/juju/Bureau/test.geojson", DefaultGeographicCRS.WGS84);

		System.out.println("End");
	}


	/*
	public static void main(String[] args) throws MalformedURLException {
		System.out.println("Start");
		SeaRouting sr = new SeaRouting();

		System.out.println(new Date().toInstant());
		//get from origin () to destination ()
		Feature f = sr.getRoute(5.3, 43.3, 121.8, 31.2);
		System.out.println(new Date().toInstant());

		System.out.println(f.getProperties().get("dFromKM"));
		System.out.println(f.getProperties().get("dToKM"));
		System.out.println(f.getGeom());
		double dist = Utils.getLengthGeo(f.getGeom());
		System.out.println(dist);
		String gj = Utils.toGeoJSON(f.getGeom());
		System.out.println(gj);
		System.out.println("End");
	}
	 */
}
