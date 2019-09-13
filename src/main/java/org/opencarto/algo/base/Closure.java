/**
 * 
 */
package org.opencarto.algo.base;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;


/**
 * 
 * @author julien Gaffuri
 *
 */
public class Closure {

	public static Geometry get(Geometry geom, double d, int qSegs, int endCapStyle) {
		Geometry out = BufferOp.bufferOp(geom, d, qSegs, endCapStyle);
		out = BufferOp.bufferOp(out, -d, qSegs, endCapStyle);
		return out;
	}
	public static Geometry get(Geometry geometry, double d, int qSegs) { return get(geometry, d, qSegs, BufferParameters.CAP_ROUND); }
	public static Geometry get(Geometry geom, double d) { return get(geom, d, 10); }
	public static Geometry get(Geometry geom, double distMM, double scale, int qSegs, int endCapStyle) { return get(geom, distMM * scale * 0.001, qSegs, endCapStyle); }
	public static Geometry get(Geometry geometry, double distMM, double scale, int qSegs) { return get(geometry, distMM, scale, qSegs, BufferParameters.CAP_ROUND); }
	public static Geometry get(Geometry geometry, double distMM, double scale) { return get(geometry, distMM, scale, 10); }

	public static Geometry getGain(Geometry geom, double d, int qSegs){ return get(geom, d, qSegs).symDifference(geom); }

}
