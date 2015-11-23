package com.rokus.motorcontroller;


public class Util {
	static final String LOG_TAG = "WII";

	/**
	 * Transforms a 0..127 -128 .. 0 range into a 128..0..-128 one
	 */
	static int fixJavaByte(byte init){
		int result = init;
		boolean negative = result < 0;
		if(negative) result = -result;
		result = - result + 128;
		if(negative) result = -result;
		return result;
	}
}

