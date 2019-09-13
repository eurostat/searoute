package org.opencarto.algo.line;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opencarto.algo.distances.HausdorffDistance;

public class Cusmoo2 {
	private static Logger logger = Logger.getLogger(Cusmoo2.class.getName());

	public static LineString get(LineString line, double symbolWidthMM, double toleranceDistanceMM, double scale){
		double hdToleranceDistance = toleranceDistanceMM * scale * 0.001;

		if(logger.isLoggable(Level.FINE)) logger.fine("Cusmoo computation for " + line);
		LineString lsOut = Cusmoo.get(line, symbolWidthMM, scale);

		//compute hausdorf disance
		HausdorffDistance hd = new HausdorffDistance(line, lsOut);
		if(logger.isLoggable(Level.FINE)) logger.fine("Hdistance: " + hd.getDistance());

		if( hd.getDistance() < hdToleranceDistance )
			return lsOut;

		// out line too far from initial line
		//split initial line
		if(logger.isLoggable(Level.FINE)) logger.fine("Hdistance too big. Split to: " + hd.getCoordinates()[0]);
		LineString[] splitL = SplitLine.get(line, hd.getCoordinates()[0], line.getFactory());

		if(splitL == null || splitL.length == 0 || splitL.length == 1) {
			if(logger.isLoggable(Level.FINE)) logger.fine("Split failed");
			return line;
		}

		//apply cusmoo to parts
		LineString line1 = Cusmoo2.get(splitL[0], symbolWidthMM, toleranceDistanceMM, scale);
		LineString line2 = Cusmoo2.get(splitL[1], symbolWidthMM, toleranceDistanceMM, scale);
		return Cusmoo2.union(line1, line2);
	}

	private static LineString union(LineString line1, LineString line2) {
		Coordinate[] cs1 = line1.getCoordinates();
		Coordinate[] cs2 = line2.getCoordinates();
		Coordinate[] cs = new Coordinate[cs1.length + cs2.length -1];

		for(int i=0; i<cs1.length; i++) cs[i] = cs1[i];
		for(int i=cs1.length; i<cs1.length + cs2.length -1; i++) cs[i] = cs2[i-cs1.length+1];

		return line1.getFactory().createLineString(cs);
	}

}
