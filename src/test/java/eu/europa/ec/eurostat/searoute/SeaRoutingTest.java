/**
 * 
 */
package eu.europa.ec.eurostat.searoute;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.MultiLineString;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.util.GeoDistanceUtil;
import junit.framework.TestCase;

/**
 * @author Julien Gaffuri
 *
 */
class SeaRoutingTest extends TestCase {
	//private final static Logger LOGGER = LogManager.getLogger(SeaRoutingTest.class.getName());

	@Test
	void testShangaiMarseille() throws Exception {

		//create the routing object
		SeaRouting sr = new SeaRouting();

		//get the route between Marseille (5.3E,43.3N) and Shanghai (121.8E,31.2N)
		Feature route = sr.getRoute(5.3, 43.3, 121.8, 31.2, false, true);

		//compute the distance in km
		MultiLineString routeGeom = (MultiLineString) route.getDefaultGeometry();
		double d = GeoDistanceUtil.getLengthGeoKM(routeGeom);

		//export the route in geoJSON format
		//String rgj = GeoJSONUtil.toGeoJSON(routeGeom);

		assertFalse(route.getAttribute("dToKM") == null);
		assertFalse(route.getAttribute("dFromKM") == null);
		assertFalse(routeGeom == null);
		assertTrue(routeGeom.getLength() < 250);
		assertTrue(routeGeom.getLength() > 200);
		assertTrue(d < 28000);
		assertTrue(d > 23000);

		//LOGGER.info(route.getAttributes()); //{dToKM=30.434689972094713, dFromKM=4.134285055159354}
		//LOGGER.info(routeGeom.getLength()); //239.80414982436778
		//LOGGER.info(d); //25091.977608633395
		//LOGGER.info(rgj); //{"type":"MultiLineString","coordinates":[[[5.2]]]}
	}

}
