/**
 * 
 */
package org.opencarto.datamodel;

/**
 * @author julien Gaffuri
 *
 */
public class ZoomExtend {
	public int min=0,max=21;

	public ZoomExtend(int min, int max) {
		this.min = min;
		this.max = max;
	}
	
	public int size(){
		return max-min+1;
	}
}
