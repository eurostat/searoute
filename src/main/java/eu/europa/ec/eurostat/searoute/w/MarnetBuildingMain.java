package eu.europa.ec.eurostat.searoute.w;

import java.util.Collection;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opencarto.algo.meshsimplification.MeshSimplification;
import org.opencarto.io.SHPUtil;

import eu.europa.ec.eurostat.searoute.MarnetBuilding;
import eu.europa.ec.eurostat.searoute.SeaRouting;

public class MarnetBuildingMain {

	//TODO
	//add port connections from GISCO DB
	//for thin triangles (with short height), remove the longest segment
	//publish - document
	//ensure no land intersection ?

	public static void main(String[] args) {
		double[] resDegs = new double[] { 0.5, 0.25, 0.1, 0.05, 0.025 };

		try {
			System.out.println("Start");
			for(int i=0; i<SeaRouting.RESOLUTION_KM.length; i++) {
				int resKM = SeaRouting.RESOLUTION_KM[i];
				double resDeg = resDegs[i];
				System.out.println("*** res= "+resKM+"km - "+resDeg);
				Collection lines = MarnetBuilding.make(resDeg, "resources/marnet/marnet_densified.geojson", "/home/juju/geodata/gisco/mar_ais_gisco.geojson", "/home/juju/geodata/gisco/ef.geojson");
				System.out.println("save...");
				SHPUtil.saveSHP(MeshSimplification.linesToFeatures(lines), "resources/marnet/marnet_plus_"+resKM+"KM.shp", DefaultGeographicCRS.WGS84);
			}
			System.out.println("Done");
		} catch (Exception e) { e.printStackTrace(); }
	}

}
