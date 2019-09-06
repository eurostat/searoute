/**
 * 
 */
package org.opencarto.algo.line;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.opencarto.algo.base.Rotation;
import org.opencarto.algo.base.Stretching;

/**
 * 
 * @author julien Gaffuri
 *
 */
public class Cusmoo {
	private static Logger logger = Logger.getLogger(Cusmoo.class.getName());

	public static LineString get(LineString line, double symbolWidthMM, double scale){
		return get(line, symbolWidthMM, scale, true);
	}

	public static LineString get(LineString line, double symbolWidthMM, double scale, boolean post){
		if(line.isClosed()) {
			logger.warning("Closed line not supported.");
			return line;
		}
		return line.getFactory().createLineString( get(line.getCoordinates(), symbolWidthMM, scale, post) );
	}

	public static Coordinate[] get(Coordinate[] coords, double symbolWidthMM, double scale){
		return get(coords, symbolWidthMM, scale, true);
	}

	public static Coordinate[] get(Coordinate[] coords, double symbolWidthMM, double scale, boolean post){

		if(coords.length<=2) return coords;

		//resolution in m = 0.1mm on the map
		double res = 0.1 *scale*0.001;

		double[] s = new double[coords.length];
		s[0] = 0.0;
		double s_ = 0.0;
		for(int k=1; k<coords.length; k++){
			s_ += coords[k-1].distance(coords[k]);
			s[k] = s_;
		}
		double length = s[coords.length-1];

		//segment orientations within (-Pi, Pi)
		double[] orientation = new double[coords.length-1];
		for(int k=0; k<coords.length-1; k++) orientation[k] = Math.atan2( coords[k+1].y - coords[k].y , coords[k+1].x - coords[k].x );

		//direction changes for each vertice within (-Pi, Pi)
		double[] alpha = new double[coords.length-1];
		alpha[0] = orientation[0];
		double deviation;
		for(int k=1; k<coords.length-1; k++) {
			deviation = orientation[k]-orientation[k-1];

			if (deviation >  Math.PI) deviation -= 2*Math.PI;
			if (deviation < -Math.PI) deviation += 2*Math.PI;

			alpha[k] = deviation;
		}

		//final coordinates
		int m = (int)(length/res);
		Coordinate[] out = new Coordinate[m];

		double sigma = symbolWidthMM * scale * 0.001;
		out[0] = coords[0];
		Coordinate c1, c2 = coords[0];
		double a = 0.0;
		for(int i=1; i<m; i++){

			//get the smoothed direction change at the point i
			double smoothedAlpha = smoothedAlpha(s, alpha, i-1, sigma, res);
			a += smoothedAlpha;
			if(logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, "angle="+a*180/Math.PI);

			//the new coordinate
			c1 = new Coordinate( c2.x + res*Math.cos(a), c2.y + res*Math.sin(a));
			out[i] = c1;
			c2 = c1;
		}

		//return out;

		//filtering
		LineString ls = new GeometryFactory().createLineString(out);
		out = DouglasPeuckerSimplifier.simplify(ls, res).getCoordinates();
		ls = null;

		if(!post) return out;

		//(light) rotation
		double a1 = Math.atan2( coords[coords.length-1].y - coords[0].y , coords[coords.length-1].x - coords[0].x );
		double a2 = Math.atan2( out[out.length-1].y - out[0].y , out[out.length-1].x - out[0].x );
		a = a1-a2;
		if (a >  Math.PI) a-=2*Math.PI;
		if (a < -Math.PI) a+=2*Math.PI;
		out = Rotation.get(out, coords[0], a);

		//stretching
		double k = coords[coords.length-1].distance(coords[0]) / out[out.length-1].distance(out[0]);
		return Stretching.get(out, coords[0], a1-Math.PI*0.5, k);
	}

	private final static double sqpi = Math.sqrt(Math.PI);

	private static double smoothedAlpha(double[] s, double[] alpha, int i, double sig, double res){
		if (i==0) return alpha[0];

		double out = 0.0;
		double x, sigma3 = 3*sig;
		for(int k=1; k<alpha.length; k++) {
			x = res*(i-0.5)-s[k];
			if( Math.abs(x) > sigma3) continue;
			out += alpha[k] * Math.exp( - Math.pow(x/sig, 2) );
			//sAlpha += alpha[k] * ( erf((i*resolution-xCurv[k])/sigma) - erf(((i-1)*resolution-xCurv[k])/sigma) );
		}
		return res*out/(sig*sqpi);
		//return 0.5*sAlpha;
	}

	/*
	// The Gauss error function: ( 2/sqrt(Pi) * intergral of exp(-t2) )
	private static double erf(double x) {
		double x_ = 1.0 / (1.0 + 0.47047 * Math.abs(x));
		x_ = x_ * (0.3480242 + x_ * (-0.0958798 + x_ * (0.7478556)));
		double erf = 1.0 - x_ * Math.exp(-x*x);
		if (x >= 0) return  erf;
		return -erf;
	}*/

}
