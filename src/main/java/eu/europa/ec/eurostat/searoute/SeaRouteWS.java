package eu.europa.ec.eurostat.searoute;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.graph.structure.Node;
import org.opencarto.datamodel.Feature;
import org.opencarto.util.GeoDistanceUtil;
import org.opencarto.util.Util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class SeaRouteWS extends HttpServlet {
	//   /usr/share/tomcat8/bin/catalina.sh start
	//   /usr/share/tomcat8/bin/catalina.sh stop
	//   http://localhost:8080/
	//logs: /var/lib/tomcat8/logs/
	//http://localhost:8080/searoutews/
	//http://localhost:8080/searoutews/seaws?ser=rou&opos=5.3,43.3&dpos=121.8,31.2
	//mvn tomcat7:deploy re un

	//move stuff to oc - improve structure - avoid overlaps
	//jar/war packaging


	private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final long serialVersionUID = 5326338791791803741L;

	private static final String ENC_CT = "; charset=utf-8";

	private HashMap<Integer,SeaRouting> srs = new HashMap<>();

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
		System.out.println("---" + this + " started - " + df.format(new Date()));
		//cache = new HashMap<String, Object[]>();
		try {
			for(int resKM : SeaRouting.RESOLUTION_KM)
				srs.put(resKM, new SeaRouting("webapps/searoute/resources/marnet/marnet_plus_"+resKM+"KM.shp"));
		} catch (MalformedURLException e) { e.printStackTrace(); }
	}


	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request,response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//writer for the response
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();

		try {
			//get service
			String ser = request.getParameter("ser");
			if(ser == null || "".equals(ser)) ser = "rou";

			switch (ser) {
			
			case "rouinfo":
				response.setContentType("text/html"+ENC_CT);
				//out.print("<html>");
				out.print("Service running.");

				/*out.print("Cache size: "+ cache.size() + " of "+CACHE_MAX_SIZE);

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
				out.print("</table>");*/

				//out.print("</html>");

				break;
			case "rou":

				//resolution
				int resKM = 20;
				try { resKM = Integer.parseInt(request.getParameter("res")); } catch (Exception e) {}
				SeaRouting sr = srs.get(resKM);
				if(sr==null) sr = srs.get(20);

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

				response.setContentType("text/json"+ENC_CT);
				if(oLon==null)
					out.print("{\"status\":\"empty\"}");
				if(oLon.length==0)
					out.print("{\"status\":\"empty\"}");
				else {
					if(oLon.length>1) out.print("[");
					returnRoute(out, oLon[0], oLat[0], dLon[0], dLat[0], distP, geomP, sr);
					for(int i=1; i<oLon.length; i++){
						out.print(",");
						returnRoute(out, oLon[i], oLat[i], dLon[i], dLat[i], distP, geomP, sr);
					}
					if(oLon.length>1) out.print("]");
				}
				break;
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

	private void returnRoute(PrintWriter out, double oLon, double oLat, double dLon, double dLat, boolean distP, boolean geomP, SeaRouting sr) {
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

			/*/try to find in cache
			st = getFromCache(oLocid, dLocid);
			if(st != null){
				out.print(st);
				return;
			}*/

			//get origin node/positions
			Coordinate oPos = new Coordinate(oLon,oLat);
			Node oN = sr.getNode(oPos);
			//get destination node/positions
			Coordinate dPos = new Coordinate(dLon,dLat);
			Node dN = sr.getNode(dPos);

			if(oN == null || dN == null){
				out.print( "{\"status\":\"error\",\"message\":\"Could not find start/end node\"}" );
				//setInCache(oLocid, dLocid, st);
				return;
			}


			//build the maritime route geometry
			Feature f = sr.getRoute(oPos, oN, dPos, dN);
			Geometry ls = f.getGeom();
			f = null;


			if(ls==null){
				out.print( "{\"status\":\"error\",\"message\":\"Shortest path not found\"}" );
				//setInCache(oLocid, dLocid, st);
				return;
			}

			String st;
			st = "{\"status\":\"ok\"";
			if(distP){
				double d = GeoDistanceUtil.getLengthGeoKM(ls);
				d = Util.round(d, 2);
				st += ",\"dist\":"+d;
			}
			if(geomP){
				//export as geojson
				st += ",\"geom\":";
				StringWriter writer = new StringWriter();
				new GeometryJSON().write(ls, writer);
				st += writer.toString();
				writer.close();
			}
			st += "}";

			out.print(st);
			//setInCache(oLocid, dLocid, st);

			//improve network / add inland transport / take into account impedance

		} catch (Exception e) {
			out.print("{\"status\":\"error\",\"message\":\"Unknown error\"}");
			//response.setContentType("text/"+ENC_CT);
			//e.printStackTrace();
			//e.printStackTrace(out);
			//out.print(e.getMessage());
			//setInCache(oLocid, dLocid, st);
			e.printStackTrace();
		}
	}

	public void destroy() {
		super.destroy();
		System.out.println("---" + this + " stopped - " + df.format(new Date()));
	}

}
