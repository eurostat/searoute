package org.opencarto.algo.line;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

public class LineDensification {

	public static LineString get(LineString line, double targetResolution){

		//out coords
		int nb = (int) (line.getLength()/targetResolution);
		Coordinate[] out = new Coordinate[nb+1];

		double d=0.0, a=0.0, dTot;
		int densIndex=0;
		Coordinate[] cs = line.getCoordinates();
		for(int i=0; i<cs.length-1; i++) {
			Coordinate c0=cs[i], c1=cs[i+1];
			dTot = c0.distance(c1);
			if (d<=dTot) a = Math.atan2( c1.y-c0.y, c1.x-c0.x );
			while(d <= dTot){
				out[densIndex]=new Coordinate(c0.x+d*Math.cos(a), c0.y+d*Math.sin(a));
				densIndex++;
				d+=targetResolution;
			}
			d-=dTot;
		}
		out[nb] = cs[cs.length-1];
		return line.getFactory().createLineString(out);
	}
}
