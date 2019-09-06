/**
 * 
 */
package org.opencarto.util;

/**
 * Functions for thread management.
 * 
 * @author julien Gaffuri
 *
 */
public class ThreadUtil {

	/**
	 * Test if an interruption has to be thrown.
	 * 
	 * @throws InterruptedException
	 */
	public static void testStop() throws InterruptedException {
		if (Thread.interrupted()) {
			throw new InterruptedException();
		}
	}

}
