package eu.europa.ec.eurostat.searoute.w;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import org.opencarto.datamodel.Feature;
import org.opencarto.io.GeoJSONUtil;
import org.opencarto.util.GeoDistanceUtil;

import eu.europa.ec.eurostat.searoute.SeaRouting;

public class Mains {



	/*
	public static void main(String[] args) throws MalformedURLException {
		System.out.println("Start");

		ArrayList<HashMap<String, String>> missingPorts = CSVUtil.load("/home/juju/Bureau/gisco_port/missingPorts.csv");
		HashMap<String,String> cnts = new HashMap<>();
		cnts.put("PL", "Poland");
		cnts.put("NL", "Netherland");
		cnts.put("NO", "Norway");
		cnts.put("DE", "Germany");
		cnts.put("BE", "Belgium");
		cnts.put("TR", "Turkey");
		cnts.put("LT", "Lithuania");
		cnts.put("GR", "Greece");
		cnts.put("FR", "France");
		cnts.put("IT", "Italy");
		cnts.put("FI", "Finland");
		cnts.put("IE", "Ireland");
		cnts.put("DK", "Danemark");
		cnts.put("ES", "Spain");
		cnts.put("GB", "United Kingdom");
		for(HashMap<String, String> p : missingPorts) {
			String cc = p.get("ID").substring(0, 2);
			//System.out.println(cc + " " + cnts.get(cc));
			String qu = p.get("NAME") + ", " + cnts.get(cc);
			System.out.println(qu);

			LocationResult res = GWebServices.getLocation(qu);
			System.out.println(res.status);

			p.put("status", res.status);
			p.put("lon", "");
			p.put("lat", "");

			if(res.pos != null) p.put("poss", res.pos.length+"");
			if(res.pos != null && res.pos.length >= 2) {
				p.put("lon", res.pos[0]+"");
				p.put("lat", res.pos[1]+"");
			}
		}

		CSVUtil.save(missingPorts, "/home/juju/Bureau/gisco_port/missingPortsGeo.csv");

		System.out.println("End");
	}*/

	/*
	public static void main(String[] args) throws MalformedURLException {
		System.out.println("Start");

		//load port data
		ArrayList<Feature> ports = GeoJSONUtil.load("/home/juju/geodata/gisco/port_pt_2013_WGS84.geojson");
		ports.addAll( GeoJSONUtil.load("/home/juju/geodata/gisco/port_pt_2010_WGS84.geojson") );
		//index by id
		HashMap<String,Feature> iPorts = new HashMap<String,Feature>();
		for(Feature p : ports)
			iPorts.put(p.getProperties().get("PORT_ID").toString(), p);
		ports.clear(); ports=null;

		//load target route data
		ArrayList<HashMap<String, String>> mrs = CSVUtil.load("/home/juju/Bureau/gisco_port/Port-port_routes_distances.csv");
		//System.out.println(mrs.iterator().next());
		//{PORT=DE01DEBRV, LIB_EN=Bremerhaven, RELATION=GB01GBDVR, LIB_EN_1=Dover, KM="672,62", ORIGINE=P2P}


		//get the missing ports
		HashMap<String,HashMap<String, String>> portIds = new HashMap<>();
		for(HashMap<String, String> mr : mrs) {
			HashMap<String, String> p = new HashMap<>();
			p.put("ID", mr.get("PORT"));
			p.put("NAME", mr.get("LIB_EN"));
			portIds.put(p.get("ID"), p);
			p = new HashMap<>();
			p.put("ID", mr.get("RELATION"));
			p.put("NAME", mr.get("LIB_EN_1"));
			portIds.put(p.get("ID"), p);
		}
		System.out.println("Total unique ports needed: " + portIds.size());
		ArrayList<HashMap<String, String>> missingPorts = new ArrayList<>();
		for(HashMap<String, String> pc : portIds.values()) {
			Feature p = iPorts.get(pc.get("ID").toString().substring(4,9));
			if(p != null) continue;
			//System.out.println(pc);
			missingPorts.add(pc);
		}
		portIds.clear(); portIds=null;
		System.out.println("Nb missing ports = " + missingPorts.size());
		CSVUtil.save(missingPorts, "/home/juju/Bureau/gisco_port/missingPorts.csv");


		//run
		SeaRouting sr = new SeaRouting(100); //5: 10days - 20: 3days
		Collection<Feature> out = new ArrayList<>();
		int i=0;
		for(HashMap<String, String> mr : mrs) {
			String pc1 = mr.get("PORT");
			String pc2 = mr.get("RELATION");
			System.out.println(pc1 + " to " + pc2 + " - " + (i++) + "/" + mrs.size());

			//get ports
			Feature p1 = iPorts.get(pc1.substring(4,9));
			Feature p2 = iPorts.get(pc2.substring(4,9));
			if(p1 == null) continue;
			if(p2 == null) continue;

			//compute routing
			Feature r = sr.getRoute(p1.getGeom().getCoordinate(), p2.getGeom().getCoordinate());
			r.getProperties().putAll(mr);
			out .add(r);
			if(i>1000) break;
		}
		System.out.println("Final: " + out.size());

		//save
		//GeoJSONUtil.save(out, "/home/juju/Bureau/gisco_port/routes.geojson", DefaultGeographicCRS.WGS84);
		SHPUtil.saveSHP(out, "/home/juju/Bureau/gisco_port/routes.shp", DefaultGeographicCRS.WGS84);


		System.out.println("End");
	}
	 */



	/*
	public static void main(String[] args) throws MalformedURLException {
		System.out.println("Start");

		//load ports
		Collection<Feature> ports = GeoJSONUtil.load("/home/juju/geodata/gisco/port_pt_2013_WGS84.geojson");
		System.out.println(ports.size());

		SeaRouting sr = new SeaRouting(5);
		ports = getRandom(ports, 1000);
		//ports = sr.filterPorts(ports, 34);
		System.out.println(ports.size());

		Collection<Feature> rs = sr.getRoutes(ports, "PORT_ID");

		SHPUtil.saveSHP(rs, "/home/juju/Bureau/gisco_port/test.shp", DefaultGeographicCRS.WGS84);

		System.out.println("End");
	}
	 */




	public static void main(String[] args) throws MalformedURLException {
		System.out.println("Start");
		SeaRouting sr = new SeaRouting(100);

		System.out.println(new Date().toInstant());
		//get from origin () to destination ()
		Feature f = sr.getRoute(5.3, 43.3, 121.8, 31.2);
		System.out.println(new Date().toInstant());

		System.out.println(f.getProperties().get("dFromKM"));
		System.out.println(f.getProperties().get("dToKM"));
		System.out.println(f.getGeom());
		double dist = GeoDistanceUtil.getLengthGeoKM(f.getGeom());
		System.out.println(dist);
		String gj = GeoJSONUtil.toGeoJSON(f.getGeom());
		System.out.println(gj);
		System.out.println("End");
	}



	private static Collection getRandom(Collection col, int nb) {
		ArrayList<?> list = new ArrayList();
		list.addAll(col);
		Collections.shuffle(list);
		HashSet set = new HashSet<>();
		int i=0;
		for(Object o : list) {
			set.add(o);
			i++;
			if(i==nb) break;
		}
		return set;
	}

}
