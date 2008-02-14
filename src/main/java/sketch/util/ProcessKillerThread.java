/**
 *
 */
package streamit.misc;

/**
 * A thread that tracks a running process.  If the process exceeds a time
 * limit, it is killed off.
 *
 * @author Chris Jones
 */
public class ProcessKillerThread extends Thread {
	private final long fTimeout;
	private final Process proc;
	private volatile boolean aborted=false;
	private boolean killed = false;

	public ProcessKillerThread(Process p, int timeoutMinutes) {
		proc = p;
		fTimeout=((long)timeoutMinutes)*60*1000;
		setDaemon(true);
	}

	public void abort() {
		aborted=true;
		interrupt();
	}

	public boolean didKill () {
		return killed;
	}

	public void run() {
		try {
			sleep(fTimeout);
		}
		catch (InterruptedException e) {
		}
		if(aborted) return;
		System.out.println("Time limit exceeded!");
		killed = true;
		proc.destroy ();
	}
}
