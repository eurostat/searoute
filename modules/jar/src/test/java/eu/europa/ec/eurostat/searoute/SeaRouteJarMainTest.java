package eu.europa.ec.eurostat.searoute;

import junit.framework.TestCase;

public class SeaRouteJarMainTest extends TestCase {

	public static void main(String[] args) throws Exception {
		junit.textui.TestRunner.run(SeaRouteJarMainTest.class);
	}

	public void test5() { test(5); }
	public void test10() { test(10); }
	public void test20() { test(20); }
	public void test50() { test(50); }
	public void test100() { test(100); }

	private void test(int resKM) {
		SeaRouteJarMain.main(new String[] {"-i", "src/test/resources/test_input.csv", "-res", ""+resKM, "-o", "target/testout/out_11_"+resKM+".geojson"});
		SeaRouteJarMain.main(new String[] {"-i", "src/test/resources/test_input.csv", "-res", ""+resKM, "-suez", "0", "-panama", "1", "-o", "target/testout/out_01_"+resKM+".geojson"});
		SeaRouteJarMain.main(new String[] {"-i", "src/test/resources/test_input.csv", "-res", ""+resKM, "-suez", "1", "-panama", "0", "-o", "target/testout/out_10_"+resKM+".geojson"});
		SeaRouteJarMain.main(new String[] {"-i", "src/test/resources/test_input.csv", "-res", ""+resKM, "-suez", "0", "-panama", "0", "-o", "target/testout/out_00_"+resKM+".geojson"});
	}

}
