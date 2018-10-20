package eu.ec.eurostat.searoute;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.geom.GeometryJSON;
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
import org.opencarto.util.Util;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.linemerge.LineMerger;

public class SeaRouteWS extends HttpServlet {

	//https://askubuntu.com/questions/135824/what-is-the-tomcat-installation-directory
	//   /usr/share/tomcat8/bin/catalina.sh start
	//   /usr/share/tomcat8/bin/catalina.sh stop
	//   http://localhost:8080/
	
	
	private static final long serialVersionUID = 5326338791791803741L;

	private static final String shpPath = "webapps/contrafficws/resources/shp/marnet.shp";
	private static final String ENC_CT = "; charset=utf-8";

	private Graph g;
	private EdgeWeighter weighter;


	/*/cache
	private HashMap<String, Object[]> cache;
	private static final int CACHE_MAX_SIZE = 60000; //TODO choose better the cache size, depending on RAM available
	private String getFromCache(String oLocid, String dLocid){
		if(oLocid == null || dLocid == null || "".equals(oLocid) || "".equals(dLocid)) return null;
		Object[] o = cache.get( oLocid + "_" + dLocid );
		if(o == null) return null;
		o[1] = ((int)o[1]) + 1;
		return (String) o[0];
	}
	private void setInCache(String oLocid, String dLocid, String st){
		if(cache.size() >= CACHE_MAX_SIZE) {
			//remove the ones too less used, below the median

			//get the limit value
			int[] a = new int[cache.size()]; int i=0;
			for(Object[] o : cache.values()) a[i++] = (int)o[1];
			Arrays.sort(a);
			int limit = a[ (int)(cache.size()*0.5) ];

			//remove all values below the limit
			Set<String> toRemove = new HashSet<String>();
			for(Entry<String,Object[]> e : cache.entrySet())
				if((int)e.getValue()[1] < limit) toRemove.add(e.getKey());
			for(String toRemove_ : toRemove) cache.remove(toRemove_);
		}
		if(oLocid == null || dLocid == null || st == null || "".equals(oLocid) || "".equals(dLocid) || "".equals(st)) return;
		cache.put(oLocid + "_" + dLocid, new Object[]{st, 1});
	}*/



	public void init() throws ServletException {
		super.init();
		System.out.println("---" + this + " started - " + Utils.df.format(new Date()));

		//cache = new HashMap<String, Object[]>();

		try {
			//load features from shp file
			File f = new File(shpPath);
			FileDataStore dso = FileDataStoreFinder.getDataStore( f );
			SimpleFeatureCollection fc = dso.getFeatureSource().getFeatures();

			//build graph
			FeatureGraphGenerator gGen = new FeatureGraphGenerator(new LineStringGraphGenerator());
			FeatureIterator<?> it = fc.features();
			while(it.hasNext()) gGen.add(it.next());
			g = gGen.getGraph();
			it.close();
			dso.dispose();

			//link nodes around the globe
			for(Object o : g.getNodes()){
				Node n = (Node)o;
				Coordinate c = ((Point)n.getObject()).getCoordinate();
				if(c.x==180) {
					Node n_ = getNode(g, new Coordinate(-c.x,c.y));
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

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		}
	}


	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request,response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//writer for the response
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();

		try {

			//service
			String ser = request.getParameter("ser");

			//service
			if(ser == null || "".equals(ser)){
				response.setContentType("text/json"+ENC_CT);
				out.println("{\"res\":\"error\",\"message\":\"No service specified\"}");
				return;
			}

			/*if("rouinfo".equals(ser)){
				response.setContentType("text/html"+ENC_CT);
				out.print("<html>");

				out.print("Cache size: "+ cache.size() + " of "+CACHE_MAX_SIZE);

				//show cache content
				ArrayList<String> list = new ArrayList<String>();
				list.addAll(cache.keySet());
				Collections.sort(list, new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) { return (int)cache.get(o2)[1] - (int)cache.get(o1)[1]; }
				});

				out.print("<table>");
				for(String id : list){
					Object[] o = cache.get(id);
					out.print("<tr>");
					out.print("<td>" + o[1] + "</td>");
					out.print("<td>" + id + "</td>");
					String st = ((String)o[0]);
					out.print("<td>" + st.substring(0, Math.min(150, st.length())) + " ...</td>");
					out.print("</tr>");
				}
				out.print("</table>");

				out.print("</html>");
				return;
			}*/

			//geometry asked
			boolean geomP = !("0".equals( request.getParameter("g") ));

			//distance asked
			boolean distP = !("0".equals( request.getParameter("d") ));

			//lat lon
			double[] oLon=null,oLat=null,dLon=null,dLat=null;

			//load directly position (lon,lat)
			String oPos = request.getParameter("opos");
			if(oPos!=null && !"".equals(oPos)){
				String[] s = oPos.split("\\s*,\\s*");
				int nb = s.length/2;
				oLon = new double[nb]; oLat = new double[nb];
				for(int i=0;i<nb;i++){
					oLon[i] = Double.parseDouble(s[2*i]);
					oLat[i] = Double.parseDouble(s[2*i+1]);
				}
			}
			String dPos = request.getParameter("dpos");
			if(dPos!=null && !"".equals(dPos)){
				String[] s = dPos.split("\\s*,\\s*");
				int nb = s.length/2;
				dLon = new double[nb]; dLat = new double[nb];
				for(int i=0;i<nb;i++){
					dLon[i] = Double.parseDouble(s[2*i]);
					dLat[i] = Double.parseDouble(s[2*i+1]);
				}
			}



			switch (ser) {

			case "rou":
				if(oLon != null && oLat!= null ){
					response.setContentType("text/json"+ENC_CT);
					returnRoute(out, oLon, oLat, dLon, dLat, distP, geomP);
					break;
				}
			default :
				response.setContentType("text/json"+ENC_CT);
				out.println("{\"res\":\"error\",\"message\":\"Unknown service: "+ser+"\"}");
			}

		} catch (Exception e) {
			response.setContentType("text/"+ENC_CT);
			e.printStackTrace();
			e.printStackTrace(out);
		} finally {
			//close writer
			if (out!=null) try {out.close();} catch (Exception e) {}
		}

	}


	private void returnRoute(PrintWriter out, double[] oLon, double[] oLat, double[] dLon, double[] dLat, boolean distP, boolean geomP) {
		if(oLon==null){
			out.print("{\"status\":\"empty\"}");
			return;
		}
		if(oLon.length==0){
			out.print("{\"status\":\"empty\"}");
			return;
		}
		if(oLon.length==1){
			returnRoute(out, oLon[0], oLat[0], dLon[0], dLat[0], distP, geomP);
			return;
		}

		out.print("[");
		returnRoute(out, oLon[0], oLat[0], dLon[0], dLat[0], distP, geomP);
		for(int i=1; i<oLon.length; i++){
			out.print(",");
			returnRoute(out, oLon[i], oLat[i], dLon[i], dLat[i], distP, geomP);
		}
		out.print("]");
	}

	private void returnRoute(PrintWriter out, double oLon, double oLat, double dLon, double dLat, boolean distP, boolean geomP) {
		try {
			if(oLon==Double.NaN || oLat==Double.NaN){
				out.print("{\"status\":\"error\",\"message\":\"Unknown origin location\"");
				out.print("}");
				return;
			}
			if(dLon==Double.NaN || dLat==Double.NaN){
				out.print("{\"status\":\"error\",\"message\":\"Unknown destination location\"");
				out.print("}");
				return;
			}
			if(oLon==dLon && oLat==dLat){
				out.print("{\"status\":\"empty\"");
				out.print("}");
				return;
			}


			String st;

			/*/try to find in cache
			st = getFromCache(oLocid, dLocid);
			if(st != null){
				out.print(st);
				return;
			}*/

			//get origin node/positions
			Coordinate oPos = new Coordinate(oLon,oLat);
			Node oN = getNode(g,oPos);
			Coordinate oNPos = getPosition(oN);
			//get destination node/positions
			Coordinate dPos = new Coordinate(dLon,dLat);
			Node dN = getNode(g,dPos);
			Coordinate dNPos = getPosition(dN);

			if(oN == null || dN == null){
				st = "{\"status\":\"error\",\"message\":\"Could not find start/end node\"";
				st += "}";
				out.print(st);
				//setInCache(oLocid, dLocid, st);
				return;
			}

			//the maritime route geometry
			MultiLineString ls=null;

			//test if route should be based on network
			//route do not need network if straight line between two points is smaller than teh total distance to reach the network
			double dist = -1;
			try { dist = Utils.getDistance(oPos,dPos); } catch (Exception e) {}
			double distN = -1;
			try { distN = Utils.getDistance(oPos,oNPos) + Utils.getDistance(dPos,dNPos); } catch (Exception e) {}
			if(dist>=0 && distN>=0 && distN > dist){
				//return direct
				GeometryFactory gf = new GeometryFactory();
				ls = gf.createMultiLineString(new LineString[]{ gf.createLineString(new Coordinate[]{oPos,dPos}) });
			} else {
				//Compute dijkstra from start node
				Path path = null;
				synchronized (g) {
					path = getShortestPath(g, oN,dN,weighter);
				}

				if(path == null) ls=null;
				else {
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
					ls = gf.createMultiLineString( lss.toArray(new LineString[lss.size()]) );
				}

			}

			if(ls==null){
				st = "{\"status\":\"error\",\"message\":\"Shortest path not found\"";
				st += "}";
				out.print(st);
				//setInCache(oLocid, dLocid, st);
				return;
			}

			st = "{\"status\":\"ok\"";
			if(distP){
				double d = Utils.getLengthGeo(ls);
				d = Util.round(d, 2);
				st += ",\"dist\":"+d;
			}
			if(geomP){
				//export as geojson
				StringWriter writer = new StringWriter();
				//new GeometryJSON().writeLine(ls, writer);
				new GeometryJSON().writeMultiLine(ls, writer);
				String geojson = writer.toString();
				st += ",\"geom\":"+geojson;
			}
			st += "}";
			out.print(st);
			//setInCache(oLocid, dLocid, st);

			//improve network / add inland transport / take into account impedance

		} catch (Exception e) {
			String st = "{\"status\":\"error\",\"message\":\"Unknown error\"";
			st += "}";
			out.print(st);
			//setInCache(oLocid, dLocid, st);
			e.printStackTrace();
		}
	}


	public void destroy() {
		super.destroy();
		System.out.println("---" + this + " stopped - " + Utils.df.format(new Date()));
	}

	private Path getShortestPath(Graph g, Node sN, Node dN, EdgeWeighter weighter){
		DijkstraShortestPathFinder pf = new DijkstraShortestPathFinder(g, sN, weighter);
		pf.calculate();
		return pf.getPath(dN);
	}

	//lon,lat
	static Coordinate getPosition(Node n){
		if(n==null) return null;
		Point pt = (Point)n.getObject();
		if(pt==null) return null;
		return pt.getCoordinate();
	}

	//get closest node from a position (lon,lat)
	static Node getNode(Graph g, Coordinate c){
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

}
