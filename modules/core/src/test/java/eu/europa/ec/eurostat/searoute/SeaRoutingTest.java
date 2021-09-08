/**
 * 
 */
package eu.europa.ec.eurostat.searoute;

import org.locationtech.jts.geom.MultiLineString;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.util.GeoDistanceUtil;
import junit.framework.TestCase;

/**
 * @author Julien Gaffuri
 *
 */
public class SeaRoutingTest extends TestCase {


	public void testShangaiMarseille() {
		//Marseille (5.3E,43.3N) and Shanghai (121.8E,31.2N)
		test(5.3, 43.3, 121.8, 31.2, 20, 250, 23000, 28000,
				false, true, true, true, true, true, true, true, false, false, false, false);
	}

	public void test1() {
		//Bontang_Safaga
		test(117.470056, 0.101287, 32.380686, 29.635942, 0, 99999999, 0, 99999999,
				false, false, true, true, true, true, true, true, false, false, false, false);
	}

	public void test2() {
		//Arzew_Colon
		test(-0.259368, 35.810036, -79.904864, 9.34731, 0, 99999999, 0, 99999999,
				false, false, true, true, true, true, true, true, false, false, false, false);
	}

	public void test3() {
		//Arzew_Safaga
		test(-0.259368, 35.810036, 32.380686, 29.635942, 0, 99999999, 0, 99999999,
				false, false, true, true, true, true, true, true, false, false, false, false);
	}


	/*
	public void testXXX() {
		test(
				117.470056, 0.101287, 32.380686, 29.635942,
				0, 99999999, 0, 99999999,
				//allowSuez   5.3, 43.3, 121.8, 31.2,
				true,
				//allowPanama   -79.4434247, 25.0316704, -81.2500792, -1.9156171,
				false,
				//allowMalacca  5.3, 43.3, 121.8, 31.2,
				false,
				//allowGibraltar   -0.259368, 35.810036, -79.904864, 9.34731,
				true,
				//allowDover    20.3264682, 59.050991, -0.5654702, 44.8425714,
				true,
				//allowBering   5.3, 43.3, 121.8, 31.2,
				false,
				//allowMagellan   -79.4434247, 25.0316704, -81.2500792, -1.9156171,
				true,
				//allowBabelmandeb  5.3, 43.3, 121.8, 31.2,
				false,
				//allowKiel    20.3264682, 59.050991, -0.5654702, 44.8425714,
				true,
				//allowCorinth   18.988493, 40.2282907, 22.9288012, 40.1072797,
				true,
				//allowNorthwest  5.3, 43.3, 121.8, 31.2,
				false,
				//allowNortheast   117.470056, 0.101287, 32.380686, 29.635942,
				true
				);
	}*/



	private void test(double oLon, double oLat, double dLon, double dLat,
			double rglMin, double rglMax, double dMin, double dMax,
			boolean allowSuez, boolean allowPanama, boolean allowMalacca,
			boolean allowGibraltar, boolean allowDover, boolean allowBering, boolean allowMagellan,
			boolean allowBabelmandeb, boolean allowKiel, boolean allowCorinth, boolean allowNorthwest, boolean allowNortheast) {

		for(int resKM : new int[] {5, 10, 20, 50, 100}) {		
			//create the routing object
			SeaRouting sr = new SeaRouting(resKM);

			//get the route
			Feature route = sr.getRoute(oLon, oLat, dLon, dLat,
					allowSuez, allowPanama, allowMalacca, allowGibraltar, allowDover, allowBering,
					allowMagellan, allowBabelmandeb, allowKiel, allowCorinth, allowNorthwest, allowNortheast);

			//compute the distance in km
			MultiLineString routeGeom = (MultiLineString) route.getGeometry();
			//if(routeGeom == null) return;
			double d = GeoDistanceUtil.getLengthGeoKM(routeGeom);

			//export the route in geoJSON format
			//String rgj = SeaRouting.toGeoJSON(routeGeom);
			//assertTrue(rgj.length() > 10);

			assertFalse(route.getAttribute("distKM") == null);
			assertFalse(route.getAttribute("dToKM") == null);
			assertFalse(route.getAttribute("dFromKM") == null);
			assertFalse(routeGeom == null);
			assertTrue(routeGeom.getLength() < rglMax);
			assertTrue(routeGeom.getLength() > rglMin);
			assertTrue(d < dMax);
			assertTrue(d > dMin);

			//LOGGER.info(route.getAttributes()); //{dToKM=30.434689972094713, dFromKM=4.134285055159354}
			//LOGGER.info(routeGeom.getLength()); //239.80414982436778
			//System.out.println(d);
			//System.out.println(routeGeom);
		}
	}

}
