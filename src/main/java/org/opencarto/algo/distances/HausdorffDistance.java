/**
 * 
 */
package org.opencarto.algo.distances;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.operation.distance.DistanceOp;

/**
 * @author julien Gaffuri
 *
 */
public class HausdorffDistance {

	private LineString line1, line2;
	private Coordinate c1 = null, c2 = null;
	private double distance = -1;

	public HausdorffDistance(LineString line1, LineString line2) {
		this.line1 = line1;
		this.line2 = line2;
	}

	public double getDistance() {
		if(this.distance == -1) update();
		return this.distance;
	}

	public Coordinate[] getCoordinates() {
		if(this.c1 == null || this.c2 == null) update();
		return new Coordinate[] {this.c1, this.c2};
	}

	public void update() {
		//compute two parts
		Object[] hd1 = compute_(this.line1, this.line2);
		Object[] hd2 = compute_(this.line2, this.line1);

		double d1 = ((Double)hd1[0]).doubleValue();
		double d2 = ((Double)hd2[0]).doubleValue();
		if( d1>d2 ) {
			this.distance = d1;
			this.c1 = (Coordinate) hd1[1];
			this.c2 = (Coordinate) hd1[2];
		} else {
			this.distance = d2;
			this.c1 = (Coordinate) hd2[2];
			this.c2 = (Coordinate) hd2[1];
		}
	}

	//returns an array with:
	// 0: the distance
	// 1: the point of lineA
	// 2: the point of lineB
	private static Object[] compute_(LineString lineA, LineString lineB) {
		//go through lineA
		Coordinate[] csA = lineA.getCoordinates();

		GeometryFactory gf = new GeometryFactory();
		double dist, distMax = -1;
		DistanceOp distOp;
		Coordinate cA, cAMax = null, cBMax = null;

		for(int i=0; i<csA.length; i++) {
			cA = csA[i];

			//find the shortest distance to lineB
			distOp = new DistanceOp(gf.createPoint(cA), lineB);
			dist = distOp.distance();

			if(dist > distMax) {
				distMax = dist;
				cAMax = cA;
				cBMax = distOp.nearestPoints()[1]; 
			}
		}
		return new Object[] {new Double(distMax), cAMax, cBMax};
	}

}
