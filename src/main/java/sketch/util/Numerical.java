package sketch.util;

public class Numerical {

	/**
	 * Returns the bit for the given value at pos
	 * @param pos ranges from 0 to 31
	 * @return
	 */
	public static int getBit(int value, int pos) {
		int mask = 1 << pos;
		
		return ((value & mask) == 0) ? 0 : 1;
	}
}
