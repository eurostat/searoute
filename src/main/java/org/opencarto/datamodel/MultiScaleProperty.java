/**
 * 
 */
package org.opencarto.datamodel;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * Structure used to represent properties whose value depends on the scale.
 * 
 * @author Julien Gaffuri
 *
 */
public class MultiScaleProperty<T> {
	private final static String K = "K";

	private Map<String, T> values = new HashMap<String, T>();

	//the default value
	public T get(){ return values.get(K); }
	public MultiScaleProperty<T> set(T value){ values.put(K,value); return this; }

	//the values by scale
	public T get(int z){ return values.get(K+z); }
	public T get(String z) {if(z==null || z=="") return get(); else return get(Integer.parseInt(z));}
	public MultiScaleProperty<T> set(T value, int z){ values.put(K+z,value); return this; }
	public MultiScaleProperty<T> set(T st, int zMin, int zMax){
		for(int z=zMin; z<=zMax; z++) set(st,z);
		return this;
	}

	public MultiScaleProperty(){}
	public MultiScaleProperty(T def){ set(def); }
}
