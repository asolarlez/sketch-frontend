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
	public ProcessStatus () {}
}
