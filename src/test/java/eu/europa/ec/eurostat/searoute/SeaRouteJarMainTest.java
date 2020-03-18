package eu.europa.ec.eurostat.searoute;

import junit.framework.TestCase;

public class SeaRouteJarMainTest extends TestCase {

	public static void main(String[] args) throws Exception {
		junit.textui.TestRunner.run(SeaRoutingTest.class);
	}

	public void test1() {
		SeaRouteJarMain.main(new String[] {"-i", "src/test/resources/test_input.csv", "-res", "5", "-panama", "0", "-o", "target/testout/out.geojson"});
	}
}
