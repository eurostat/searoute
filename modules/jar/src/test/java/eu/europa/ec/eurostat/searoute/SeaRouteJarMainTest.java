package eu.europa.ec.eurostat.searoute;

import junit.framework.TestCase;
import eu.europa.ec.eurostat.searoute.SeaRouteJarMain;

public class SeaRouteJarMainTest extends TestCase {

	/*public static void main(String[] args) throws Exception {
		junit.textui.TestRunner.run(SeaRouteJarMainTest.class);
	}*/

	public void test5() { test(5, "geojson"); }
	public void test10() { test(10, "geojson"); }
	public void test20() { test(20, "geojson"); }
	public void test50() { test(50, "geojson"); }
	public void test100() { test(100, "geojson"); }

	//public void testGPKG() { test(20, "gpkg"); }
	//public void testSHP() { test(20, "shp"); }

	private void test(int resKM, String format) {
		SeaRouteJarMain.main(new String[] {"-i", "src/test/resources/test_input.csv", "-res", ""+resKM, "-o", "target/testout/out_11_"+resKM+"."+format});
		SeaRouteJarMain.main(new String[] {"-i", "src/test/resources/test_input.csv", "-res", ""+resKM, "-suez", "0", "-panama", "1", "-o", "target/testout/out_01_"+resKM+"."+format});
		SeaRouteJarMain.main(new String[] {"-i", "src/test/resources/test_input.csv", "-res", ""+resKM, "-suez", "1", "-panama", "0", "-o", "target/testout/out_10_"+resKM+"."+format});
		SeaRouteJarMain.main(new String[] {"-i", "src/test/resources/test_input.csv", "-res", ""+resKM, "-suez", "0", "-panama", "0", "-o", "target/testout/out_00_"+resKM+"."+format});
		SeaRouteJarMain.main(new String[] {"-i", "src/test/resources/test_input.csv", "-res", ""+resKM, "-corinth", "1", "-panama", "0", "-kiel", "1", "-northwest", "1", "-malacca", "0", "-o", "target/testout/out_XX_"+resKM+"."+format});
	}

}
