/**
 *
 */
package streamit.frontend.spin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class for managing the options that SPIN understands.
 *
 * This class knows how to convert abstract SPIN options into the
 * preprocessor directives that SPIN models understand.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class Configuration {
	public enum StateCompressionPolicy {
		NONE,
		LOSSLESS_COLLAPSE, LOSSLESS_MIN_AUTOMATON, //LOSSLESS_COLLAPSE_AND_MA,
		LOSSY_BITSTATE_HASHING, LOSSY_HASH_COMPACT
	}

	protected StateCompressionPolicy compressionPolicy = StateCompressionPolicy.NONE;
	protected Map<String, String> compileParams = new HashMap<String, String> ();
	protected Map<String, String> runParams = new HashMap<String, String> ();

	public Configuration () {
		// sets default options
		setCompileParam ("-w");
		setCompileParam ("-D_POSIX_SOURCE");
		listUnreachedStates (false);
	}

	public List<String> getCompilerFlags () {
		List<String> flags = makeFlags (compileParams);
		if (compressionPolicy != StateCompressionPolicy.NONE)
			flags.add (getCompressionFlag ());
		return flags;
	}

	protected String getCompressionFlag () {
		switch (compressionPolicy) {
		case NONE:
			return "";
		case LOSSLESS_COLLAPSE:
			return "-DCOLLAPSE";
		case LOSSLESS_MIN_AUTOMATON:
			return "-DMA=500";
		case LOSSY_BITSTATE_HASHING:
			return "-DBISTATE";
		case LOSSY_HASH_COMPACT:
			return "-DHC";
		default:
			assert false : "Fatal error";  return null;
		}
	}

	public List<String> getRuntimeFlags () {
		return makeFlags (runParams);
	}

	/* === COMPILE-TIME PARAMS === */

	public void checkArrayBounds (boolean yes) {
		toggle (compileParams, !yes, "-DNOBOUNDCHECK");
	}

	public void checkChannelAssertions (boolean yes) {
		toggle (compileParams, !yes, "-DXUSAFE");
	}

	/** If disabled, only safety properties will be verified. */
	public void detectCycles (boolean yes) {
		toggle (compileParams, !yes, "-DSAFETY");
	}

	public void enforceFairness (boolean yes) {
		toggle (compileParams, !yes, "-DNOFAIR");
	}

	public void memoryLimitMB (int nMB) {
		setCompileParam ("-DMEMLIM", ""+ nMB);
	}

	public void stateCompressionPolicy (StateCompressionPolicy p) {
		// special case, since otherwise on a change we would have to ensure
		// that all other compression flags were eliminated from the options
		compressionPolicy = p;
	}

	/**
	 * For exhaustive verifications, the lg of the number of slots in
	 * the state table.
	 *
	 * For bitstate verifications, the lg of the number of bits in the hash
	 * array.
	 */
	public void stateHashTableSize (int lgN) {
		setRunParam ("-w", ""+ lgN);
	}

	public void validateNeverClaims (boolean yes) {
		toggle (compileParams, !yes, "-DNOCLAIM");
	}

	public void vectorSizeBytes (int bytes) {
		setCompileParam ("-DVECTORSZ", ""+ bytes);
	}

	/* === RUN-TIME PARAMS === */

	public void listUnreachedStates (boolean yes) {
		toggle (runParams, !yes, "-n");
	}

	public void searchDepth (int nSteps) {
		setRunParam ("-m", ""+ nSteps);
	}

	/* === HELPERS === */

	protected void toggle (Map<String, String> opts, boolean on, String opt) {
		if (on)  opts.put (opt, null);
		else	 opts.remove (opt);
	}

	protected void setCompileParam (String key) {  setCompileParam (key, null); }
	protected void setCompileParam (String key, String value) {
		compileParams.put (key, value);
	}
	protected void unsetCompileParam (String key) {
		compileParams.remove (key);
	}

	protected void setRunParam (String key) {  setCompileParam (key, null); }
	protected void setRunParam (String key, String value) {
		runParams.put (key, value);
	}
	protected void unsetRunParam (String key) {
		runParams.remove (key);
	}

	protected List<String> makeFlags (Map<String, String> opts) {
		List<String> flags = new ArrayList<String> ();
		for (String key : opts.keySet ()) {
			String value = opts.get (key);
			flags.add (key + (null == value ? "" : "="+ value));
		}
		return flags;
	}
}
