/**
 *
 */
package streamit.frontend.solvers;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public abstract class Statistics {
	public static float MiB (long bytes) {
		return ((float) bytes) / (1024.0f * 1024.0f);
	}

	public static float percent (long part, long whole) {
		return 100.0f * ((float) part) / (float) whole;
	}

	public static float sec (long ms) {
		return ((float) ms) / 1000.0f;
	}

	// I hate Java.
	public static long max (List<Long> L) {
		try {  return Collections.max (L);  }
		catch (NoSuchElementException e) {  return 0;  }
	}

	// I hate Java.
	public static long sum (List<Long> L) {
		long s = 0;
		for (long i : L) s += i;
		return s;
	}
}
