package org.opencarto.algo.line;

import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;

public class SplitLine {
	private static Logger logger = Logger.getLogger(SplitLine.class.getName());

	public static LineString[] get(LineString line, Coordinate splitC, GeometryFactory gf) {

		Coordinate[] cs = line.getCoordinates();
		int nb = cs.length;

		//check if the split coordinate is one of the line vertices
		int i;
		for(i=0; i<nb; i++) {
			Coordinate c = cs[i];
			if( c.x != splitC.x || c.y != splitC.y ) continue;

			if(i==0 || i==nb-1) return new LineString[] { line };

			//copy coords
			Coordinate[] cs0 = new Coordinate[i+1];
			for(int j=0; j<=i; j++) cs0[j] = cs[j];
			Coordinate[] cs1 = new Coordinate[nb-i];
			for(int j=0; j<nb-i; j++) cs1[j] = cs[i+j];

			return new LineString[] { gf.createLineString(cs0), gf.createLineString(cs1) };
		}

		//get the segment's index
		Coordinate c1, c2;
		c1 = cs[0];
		for(i=1; i<nb; i++) {
			c2 = cs[i];

			//check if split point is on segment
			if( new LineSegment(c1, c2).distance(splitC) > 0.001 ) {
				c1 = c2;
				continue;
			}

			//copy coords
			Coordinate[] cs0 = new Coordinate[i+1];
			for(int j=0; j<i; j++) cs0[j] = cs[j];
			cs0[i] = splitC;

			Coordinate[] cs1 = new Coordinate[nb-i+1];
			cs1[0] = splitC;
			for(int j=1; j<nb-i+1; j++) cs1[j] = cs[i+j-1];

			return new LineString[] { gf.createLineString(cs0), gf.createLineString(cs1) };
		}

		logger.warning("Impossible to split line: split coordinate do not touch the line.");
		return null;
	}
}
