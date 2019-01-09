package eu.europa.ec.eurostat.searoute.w;

import java.util.Collection;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opencarto.algo.meshsimplification.MeshSimplification;
import org.opencarto.io.SHPUtil;

import eu.europa.ec.eurostat.searoute.MarnetBuilding;
import eu.europa.ec.eurostat.searoute.SeaRouting;

public class MarnetBuildingMain {

	//TODO
	//5/12 14:42
	//add port connections from GISCO DB ?
	//for thin triangles (with short height), remove the longest segment
	//publish - document
	//ensure no land intersection ?

	public static void main(String[] args) {

		try {
			System.out.println("Start");
			for(int i=0; i<SeaRouting.RESOLUTION_KM.length; i++) {
				int resKM = SeaRouting.RESOLUTION_KM[i];
				double resDeg = MarnetBuilding.resDegs[i];
				System.out.println("*** res= "+resKM+"km - "+resDeg);
				Collection lines = MarnetBuilding.make(resDeg, "src/main/webapp/resources/marnet/marnet_densified.geojson", "/home/juju/geodata/gisco/mar_ais_gisco.geojson", "/home/juju/geodata/gisco/ef.geojson");
				System.out.println("save...");
				SHPUtil.saveSHP(MeshSimplification.linesToFeatures(lines), "src/main/webapp/resources/marnet/marnet_plus_"+resKM+"KM.shp", DefaultGeographicCRS.WGS84);
			}
			System.out.println("Done");
		} catch (Exception e) { e.printStackTrace(); }
	}

}
