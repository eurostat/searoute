/**
 * 
 */
package eu.europa.ec.eurostat.searoute;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
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
import eu.europa.ec.eurostat.jgiscotools.util.Util;

/**
 * @author julien Gaffuri
 *
 */
public class SeaRouting {
	private final static Logger LOGGER = LogManager.getLogger(SeaRouting.class.getName());

	/**
	 * The possible resolutions, in km.
	 */
	public static final int[] RESOLUTION_KM = new int[] { 100, 50, 20, 10, 5 };

	private Graph g;

	public SeaRouting() { this(20); }
	public SeaRouting(int resKM) {
		try {
			//load marnet
			File file = null;
			try {
				URL resource = getClass().getClassLoader().getResource("marnet/marnet_plus_"+resKM+"km.gpkg");
				file = new File(resource.toURI());
			} catch (Exception e) {
			} finally {
				if(file == null || !file.exists()) {
					//LOGGER.info("Could not find marnet as resource. Get it from the root.");
					file = new File("marnet/marnet_plus_"+resKM+"km.gpkg");
				}
			}

			HashMap<String, Object> params = new HashMap<>();
			params.put(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg");
			params.put(GeoPkgDataStoreFactory.DATABASE.key, file);
			DataStore store = DataStoreFinder.getDataStore(params);
			String[] names = store.getTypeNames();
			if(names.length >1 )
				LOGGER.warn("Several types found in GPKG. Only " + names[0] + " will be considered.");
			String name = names[0];
			LOGGER.debug(name);
			SimpleFeatureCollection fc = store.getFeatureSource(name).getFeatures();

			//build graph
			SimpleFeatureIterator it = fc.features();
			FeatureGraphGenerator gGen = new FeatureGraphGenerator(new LineStringGraphGenerator());
			while(it.hasNext())
				gGen.add(it.next());
			g = gGen.getGraph();
			it.close();
			store.dispose();
		} catch (Exception e) { e.printStackTrace(); }

		//link nodes around the globe
		for(Object o : g.getNodes()){
			Node n = (Node)o;
			Coordinate c = ((Point)n.getObject()).getCoordinate();
			if(c.x==180) {
				Node n_ = getNode(new Coordinate(-c.x,c.y));
				if(LOGGER.isTraceEnabled()) LOGGER.trace(c + " -> " + ((Point)n_.getObject()).getCoordinate());
				BasicEdge be = new BasicEdge(n, n_);
				n.add(be);
				n_.add(be);
				g.getEdges().add(be);
			}
		}
	}

	/**
	 * Build the edge weighters, with different possiblities regarding Suez and Panama channels.
	 * 
	 * @param allowSuez
	 * @param allowPanama
	 * @return
	 */
	private EdgeWeighter getEdgeWeighter(boolean allowSuez, boolean allowPanama, boolean allowMalacca,
			boolean allowGibraltar, boolean allowDover, boolean allowBering, boolean allowMagellan,
			boolean allowBabelmandeb, boolean allowKiel, boolean allowCorinth, boolean allowNorthwest, boolean allowNortheast) {
		return new DijkstraIterator.EdgeWeighter() {
			public double getWeight(Edge e) {
				//deal with edge around the globe
				if( e.getObject()==null ) return 0;
				SimpleFeature f = (SimpleFeature) e.getObject();

				//handle pass
				String pass = (String)f.getAttribute("pass");
				if(!allowSuez && "suez".equals(pass)) return Double.MAX_VALUE;
				if(!allowPanama && "panama".equals(pass)) return Double.MAX_VALUE;
				if(!allowMalacca && "malacca".equals(pass)) return Double.MAX_VALUE;
				if(!allowGibraltar && "gibraltar".equals(pass)) return Double.MAX_VALUE;
				if(!allowDover && "dover".equals(pass)) return Double.MAX_VALUE;
				if(!allowBering && "bering".equals(pass)) return Double.MAX_VALUE;
				if(!allowMagellan && "magellan".equals(pass)) return Double.MAX_VALUE;
				if(!allowBabelmandeb && "babelmandeb".equals(pass)) return Double.MAX_VALUE;
				if(!allowKiel && "kiel".equals(pass)) return Double.MAX_VALUE;
				if(!allowCorinth && "corinth".equals(pass)) return Double.MAX_VALUE;
				if(!allowNorthwest && "northwest".equals(pass)) return Double.MAX_VALUE;
				if(!allowNortheast && "northeast".equals(pass)) return Double.MAX_VALUE;

				return GeoDistanceUtil.getLengthGeoKM((Geometry)f.getDefaultGeometry());
			}
		};
	}




	/**
	 * Get shortest path between two netork nodes,
	 * using a predefined edge weighter.
	 * 
	 * @param g
	 * @param sN
	 * @param dN
	 * @param weighter
	 * @return
	 */
	private Path getShortestPath(Graph g, Node sN, Node dN, EdgeWeighter weighter){
		DijkstraShortestPathFinder pf = new DijkstraShortestPathFinder(g, sN, weighter);
		pf.calculate();
		return pf.getPath(dN);
	}

	/**
	 * Get the (lon,lat) position of a network node.
	 * 
	 * @param n
	 * @return
	 */
	private Coordinate getPosition(Node n){
		if(n==null) return null;
		Point pt = (Point)n.getObject();
		if(pt==null) return null;
		return pt.getCoordinate();
	}

	/**
	 * Get closest node from a position (lon,lat)
	 * 
	 * @param c
	 * @return
	 */
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


	/**
	 * Return the route geometry from origin/destination coordinates
	 * 
	 * @param oLon
	 * @param oLat
	 * @param dLon
	 * @param dLat
	 * @return
	 */
	public Feature getRoute(double oLon, double oLat, double dLon, double dLat) {
		return getRoute(oLon, oLat, dLon, dLat, true, true, true, true, true, true, true, true, false, false, false, false);
	}

	/**
	 * Return the route geometry from origin/destination coordinates
	 * 
	 * @param oLon
	 * @param oLat
	 * @param dLon
	 * @param dLat
	 * @param allowSuez
	 * @param allowPanama
	 * @param allowMalacca 
	 * @param allowGibraltar 
	 * @param allowDover 
	 * @param allowBering 
	 * @param allowMagellan 
	 * @param allowBabelmandeb 
	 * @param allowKiel 
	 * @param allowCorinth 
	 * @param allowNorthwest 
	 * @param allowNortheast 
	 * @return
	 */
	public Feature getRoute(double oLon, double oLat, double dLon, double dLat,
			boolean allowSuez, boolean allowPanama, boolean allowMalacca,
			boolean allowGibraltar, boolean allowDover, boolean allowBering, boolean allowMagellan,
			boolean allowBabelmandeb, boolean allowKiel, boolean allowCorinth, boolean allowNorthwest, boolean allowNortheast) {
		return getRoute(new Coordinate(oLon,oLat), new Coordinate(dLon,dLat),
				allowSuez, allowPanama, allowMalacca, allowGibraltar, allowDover, allowBering,
				allowMagellan, allowBabelmandeb, allowKiel, allowCorinth, allowNorthwest, allowNortheast);
	}
	public Feature getRoute(Coordinate oPos, Coordinate dPos,
			boolean allowSuez, boolean allowPanama, boolean allowMalacca,
			boolean allowGibraltar, boolean allowDover, boolean allowBering, boolean allowMagellan,
			boolean allowBabelmandeb, boolean allowKiel, boolean allowCorinth, boolean allowNorthwest, boolean allowNortheast) {
		return getRoute(oPos, getNode(oPos), dPos, getNode(dPos),
				allowSuez, allowPanama, allowMalacca, allowGibraltar, allowDover, allowBering,
				allowMagellan, allowBabelmandeb, allowKiel, allowCorinth, allowNorthwest, allowNortheast);
	}

	/**
	 * Return the route geometry from origin/destination graph nodes (when they are known)
	 * 
	 * @param oPos
	 * @param oN
	 * @param dPos
	 * @param dN
	 * @param allowSuez
	 * @param allowPanama
	 * @param allowMalacca 
	 * @param allowGibraltar 
	 * @param allowDover 
	 * @param allowBering 
	 * @param allowMagellan 
	 * @param allowBabelmandeb 
	 * @param allowKiel 
	 * @param allowCorinth 
	 * @param allowNorthwest 
	 * @param allowNortheast 
	 * @return
	 */
	public Feature getRoute(Coordinate oPos, Node oN, Coordinate dPos, Node dN,
			boolean allowSuez, boolean allowPanama, boolean allowMalacca,
			boolean allowGibraltar, boolean allowDover, boolean allowBering, boolean allowMagellan,
			boolean allowBabelmandeb, boolean allowKiel, boolean allowCorinth, boolean allowNorthwest, boolean allowNortheast) {
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
			rf.setGeometry( gf.createMultiLineString(new LineString[]{ gf.createLineString(new Coordinate[]{oPos,dPos}) }) );
			return rf;
		}

		//Get weighter
		EdgeWeighter w = getEdgeWeighter(allowSuez, allowPanama, allowMalacca, allowGibraltar, allowDover, allowBering,
				allowMagellan, allowBabelmandeb, allowKiel, allowCorinth, allowNorthwest, allowNortheast);

		//Compute dijkstra from start node
		Path path = null;
		synchronized (g) {
			path = getShortestPath(g, oN, dN, w);
		}


		if(path == null) {
			Feature rf = new Feature();
			rf.setGeometry(null);
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
		rf.setGeometry( gf.createMultiLineString( lss.toArray(new LineString[lss.size()]) ) );
		rf.setAttribute("distKM", "" + Util.round(GeoDistanceUtil.getLengthGeoKM(rf.getGeometry()), 2));
		rf.setAttribute("dFromKM", Util.round(GeoDistanceUtil.getDistanceKM(oPos, oNPos),2) );
		rf.setAttribute("dToKM", Util.round(GeoDistanceUtil.getDistanceKM(dPos, dNPos),2) );

		return rf;
	}


	/**
	 * Keep only ports that are close to the network nodes.
	 * 
	 * @param ports
	 * @param minDistToNetworkKM
	 * @return
	 */
	public Collection<Feature> filterPorts(Collection<Feature> ports, double minDistToNetworkKM) {
		Collection<Feature> pls = new HashSet<Feature>();
		for(Feature p : ports)
			if(getDistanceToNetworkKM(p.getGeometry().getCoordinate()) <= minDistToNetworkKM)
				pls.add(p);
		return pls;
	}

	/**
	 * Return the distance in km. Distance to closest node
	 * 
	 * @param c
	 * @return
	 */
	private double getDistanceToNetworkKM(Coordinate c) {
		return GeoDistanceUtil.getDistanceKM(c, getPosition(getNode(c)));
	}


	/**
	 * Compute the route matrix of a list of ports.
	 * 
	 * @param ports
	 * @param idProp
	 * @return
	 */
	public Collection<Feature> getRoutes(Collection<Feature> ports, String idProp) { return getRoutes(ports, idProp); }

	/**
	 * Compute the route matrix of a list of ports.
	 * 
	 * @param ports
	 * @param idProp
	 * @param allowSuez
	 * @param allowPanama
	 * @param allowMalacca 
	 * @param allowGibraltar 
	 * @param allowDover 
	 * @param allowBering 
	 * @param allowMagellan 
	 * @param allowBabelmandeb 
	 * @param allowKiel 
	 * @param allowCorinth 
	 * @param allowNorthwest 
	 * @param allowNortheast 
	 * @return
	 */
	public Collection<Feature> getRoutes(Collection<Feature> ports, String idProp,
			boolean allowSuez, boolean allowPanama, boolean allowMalacca,
			boolean allowGibraltar, boolean allowDover, boolean allowBering, boolean allowMagellan,
			boolean allowBabelmandeb, boolean allowKiel, boolean allowCorinth, boolean allowNorthwest, boolean allowNortheast) {
		if(idProp == null) idProp = "ID";

		List<Feature> portsL = new ArrayList<Feature>(); portsL.addAll(ports);
		int nb=portsL.size(); nb=(nb*(nb-1))/2; int cnt=0;

		HashSet<Feature> srs = new HashSet<Feature>();
		for(int i=0; i<portsL.size(); i++) {
			Feature pi = portsL.get(i);
			for(int j=i+1; j<portsL.size(); j++) {
				Feature pj = portsL.get(j);
				if(LOGGER.isDebugEnabled()) LOGGER.debug(pi.getAttribute(idProp) + " - " + pj.getAttribute(idProp) + " - " + (100*(cnt++)/nb) + "%");
				Feature sr = getRoute(pi.getGeometry().getCoordinate(), pj.getGeometry().getCoordinate(),
						allowSuez, allowPanama, allowMalacca, allowGibraltar, allowDover, allowBering,
						allowMagellan, allowBabelmandeb, allowKiel, allowCorinth, allowNorthwest, allowNortheast);
				Geometry geom = sr.getGeometry();
				sr.setAttribute("dkm", geom==null? -1 : GeoDistanceUtil.getLengthGeoKM(geom));
				sr.setAttribute("from", pi.getAttribute(idProp));
				sr.setAttribute("to", pj.getAttribute(idProp));
				srs.add(sr);
			}
		}
		return srs;
	}

}
