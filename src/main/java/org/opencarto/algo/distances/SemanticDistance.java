/**
 * 
 */
package org.opencarto.algo.distances;

import java.util.Set;

import org.opencarto.datamodel.Feature;
import org.opencarto.util.FeatureUtil;

/**
 * Compare the attribute existance and their values (if they exist) of the two features. Count the number of differences.
 * 0 result means identical semantics.
 * 
 * @author julien Gaffuri
 *
 */
public class SemanticDistance implements Distance<Feature> {

	boolean checkOnlyExistingAttributeValues;
	public SemanticDistance(boolean checkOnlyExistingAttributeValues) {
		this.checkOnlyExistingAttributeValues = checkOnlyExistingAttributeValues;
	}

	/* (non-Javadoc)
	 * @see org.opencarto.algo.distances.Distance#get(java.lang.Object, java.lang.Object)
	 */
	@Override
	public double get(Feature f1, Feature f2) {

		//get set of attribute keys
		Set<String> keys = FeatureUtil.getAttributesSet(f1,f2);

		if(keys.size() == 0) return 0;

		int nbCommon=0, nbTot=0;
		for(String key : keys) {
			Object v1 = f1.get(key), v2 = f2.get(key);
			if(!checkOnlyExistingAttributeValues && (v1==null || v2==null)) { nbTot++; continue; }
			if(v1==null ^ v2==null) continue;
			nbTot++;
			if((v1==null&&v2==null) || v1.equals(v2)) nbCommon++;
		}
		return nbTot - nbCommon;
	}

	/*
	public static void main(String[] args) {
		Feature f1 = new Feature();
		f1.set("a", "val");
		f1.set("b", "jhgjh");
		Feature f2 = new Feature();
		//f2.set("a", "val");
		//f2.set("b", "jhgjh");
		System.out.println(new SemanticDistance(false).get(f1, f2));
	}
	 */
}
