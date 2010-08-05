/**
 *
 */
package sketch.util;

/**
 * Simple 'struct' for returned status from processes:
 *  - exit code
 *  - output text
 *  - error text
 *
 * @author Chris Jones
 */
public class ProcessStatus {
	public int exitCode;
	public Throwable exception;
	public String out, err;
	public boolean killedByTimeout;
	public long execTimeMs;		// TODO: find a less unholy place to put this
	public ProcessStatus () {}
}
