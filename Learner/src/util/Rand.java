package util;

import java.util.Random;

public class Rand {
	
	/**
	 * Returns a pseudorandom long between 0 (inclusive) and the given value (exclusive)
	 * @param rng the source of pseudorandomness
	 * @param n the upper bound (exclusive)
	 * @return a random value in the range
	 */
	public static long nextLong(Random rng, long n) {
		if (n < 0) {
			throw new IllegalArgumentException("n must be positive");
		}
		long bits, val;
		do {
			bits = (rng.nextLong() << 1) >>> 1;
			val = bits % n;
		} while (bits - val + (n - 1) < 0L);
		return val;
	}
}
