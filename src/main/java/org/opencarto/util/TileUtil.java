/**
 * 
 */
package org.opencarto.util;

/**
 * 
 * Conversions between tile coordinates and geographic coordinates.
 * 
 * @author julien Gaffuri
 *
 */
public class TileUtil {

	//NB: the tile reference point is the top left corner.
	//Numbering from 0 to (2^zoom)-1, from left to right, from top to down

	public static double getLon(int xTile, int zoom) {
		return xTile / Math.pow(2.0, zoom) * 360.0 - 180;
	}

	public static double getLat(int yTile, int zoom) {
		double n = Math.PI - (2.0 * Math.PI * yTile) / Math.pow(2.0, zoom);
		return Math.toDegrees(Math.atan(Math.sinh(n)));
	}



	public static int getXTile(double lon, int zoom) {
		return (int)Math.floor( (lon + 180) / 360 * (1<<zoom) );
	}

	public static int getYTile(double lat, int zoom) {
		return (int)Math.floor( (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<zoom) ) ;
	}


	public static int[] getTileXY(int pixelX, int pixelY) {
		return new int[] { pixelX / 256, pixelY / 256 };
	}

	public static int[] getTileXYToPixelXY(int tileX, int tileY) {
		return new int[] { tileX * 256, tileY * 256 };
	}
}
