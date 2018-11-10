package eu.ec.eurostat.searoute;

public class MarnetCheck {

	public static void main(String[] args) {
		try {
			System.out.println("Start.");

			//make noding
			//remove duplicate network edges - always keep shorter
			//make network planar
			//check number of connex components
			//check 180/-180 compatibility


			/*
			//load features from shp file
			SHPData shpIn = SHPUtil.loadSHP("/home/julien/ctrweb/contraffic-ws/WebContent/resources/shp/marnet2.shp");

			System.out.println(shpIn.fs.size() + " links");

			//eliminate double links
			ArrayList<SimpleFeature> fsToRemove = new ArrayList<SimpleFeature>();
			for(int i=0; i<shpIn.fs.size(); i++){
				SimpleFeature fi = shpIn.fs.get(i);
				if(fsToRemove.contains(fi)) continue;
				MultiLineString li = (MultiLineString)fi.getDefaultGeometry();
				for(int j=i+1; j<shpIn.fs.size(); j++){
					SimpleFeature fj = shpIn.fs.get(j);
					MultiLineString lj = (MultiLineString)fj.getDefaultGeometry();
					if(li.getLength() != lj.getLength()) continue;
					Geometry inter = li.intersection(lj);
					if(inter.getLength() != lj.getLength()) continue;
					fsToRemove.add(fj);
				}
			}
			System.out.println(fsToRemove.size() + " overlapping links to remove.");
			shpIn.fs.removeAll(fsToRemove);
			System.out.println("   " + shpIn.fs.size() + " links remaining.");

			//TODO
			//integrate
			//DP filter
			//linemerger
			//cut lines depending on intersection

			System.out.println("Saving...");
			SHPUtil.saveSHP(shpIn.ft, shpIn.fs, "/home/julien/ctrweb/contraffic-ws/WebContent/resources/shp/", "marnet_fil.shp");
			//SHPUtil.saveSHP(lines, 4326, "/home/julien/ctrweb/contraffic-ws/WebContent/resources/shp/", "marnet_fil.shp");
			 */

			System.out.println("Done.");
		} catch (Exception e) { e.printStackTrace(); }
	}

}
