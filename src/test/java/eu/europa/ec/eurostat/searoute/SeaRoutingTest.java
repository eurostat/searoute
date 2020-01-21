/**
 * 
 */
package eu.europa.ec.eurostat.searoute;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.MultiLineString;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.GeoJSONUtil;
import eu.europa.ec.eurostat.jgiscotools.util.GeoDistanceUtil;

/**
 * @author Julien Gaffuri
 *
 */
class SeaRoutingTest {
	private final static Logger LOGGER = LogManager.getLogger(SeaRoutingTest.class.getName());

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
		String rgj = GeoJSONUtil.toGeoJSON(routeGeom);

		LOGGER.info(route.getAttributes()); //{dToKM=30.434689972094713, dFromKM=4.134285055159354}
		LOGGER.info(routeGeom.getLength()); //239.80414982436778
		LOGGER.info(d); //25091.977608633395
		LOGGER.info(rgj); //{"type":"MultiLineString","coordinates":[[[5.2]]]}
	}

}
