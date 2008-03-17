/**
 *
 */
package streamit.misc;

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
	public String out, err;
	public boolean killed;
	public long execTimeMs;		// TODO: find a less unholy place to put this
	public ProcessStatus () {}
}
