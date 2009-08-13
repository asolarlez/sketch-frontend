package sketch.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class InteractiveTimedProcess {
	protected Process	proc;
	protected int		timeoutMins;
	protected long		startMs;
	ProcessKillerThread killer = null;
	ProcessStatus status = new ProcessStatus ();
	
	
	public ProcessStatus status(){
		return status;
	}
	
	public InteractiveTimedProcess (int _timeoutMins, String... cmdLine)
	throws IOException {
		this (System.getProperty ("user.dir"), _timeoutMins, cmdLine);
	}

	public InteractiveTimedProcess (String workDir, int _timeoutMins,
			String... cmdLine) throws IOException {
		this (workDir, _timeoutMins, Arrays.asList (cmdLine));
	}

	public InteractiveTimedProcess (String workDir, int _timeoutMins,
			List<String> cmdLine) throws IOException {
		for (String s : cmdLine)
			assert s != null : "Null elt of command: '"+ cmdLine +"'";

		ProcessBuilder pb = new ProcessBuilder (cmdLine);
		pb.directory (new File (workDir));
		startMs = System.currentTimeMillis ();
		proc = pb.start ();
		timeoutMins = _timeoutMins;
	}

	public InteractiveTimedProcess (Process _proc) {
		this (_proc, 0);
	}

	public InteractiveTimedProcess (Process _proc, int _timeoutMins) {
		timeoutMins = 0;
		proc = _proc;
	}

	public void run () throws IOException, InterruptedException {		
		System.gc();
		try {
			if (timeoutMins > 0){
				killer = new ProcessKillerThread (proc, timeoutMins);
				killer.start();
			}
			status.out = ""; // Misc.readStream (proc.getInputStream (), logAllOutput);
			status.err = ""; //Misc.readStream (proc.getErrorStream (), true);			
		} finally {
			if (null != killer) {
				killer.abort ();
				status.killed = killer.didKill ();
			}
		}
	}
	
	public boolean stillActive(){
		try{
			proc.exitValue();
		}catch(IllegalThreadStateException its){
			return true;
		}
		status.exitCode = proc.exitValue();
		try {
			status.err = Misc.readStream (proc.getErrorStream (), true);
		}catch(IOException ioe){
			
		}
		return false;
	}
	

	public void cleanup(){
		try {
		status.exitCode = proc.waitFor ();
		status.out = Misc.readStream (proc.getInputStream (), true);
		status.err = Misc.readStream (proc.getErrorStream (), true);		
		}catch(InterruptedException ie){
			
		}catch(IOException ioe){
			
		}
		status.execTimeMs = System.currentTimeMillis () - startMs;
		if (null != killer) {
			killer.abort ();
			status.killed = killer.didKill ();
		}
	}
	

	public void waitForAnswer(String answer, boolean logAllOutput){
		
		try{
			status.out += Misc.readStreamUntil(proc.getInputStream(), answer, logAllOutput);
		}catch(Exception e){
			
		}	
		
	}
	

	public OutputStream getOutputStream() {

		return proc.getOutputStream();
	}

	public InputStream getErrorStream() {
		return proc.getErrorStream();
	}

	public InputStream getInputStream() {
		return proc.getInputStream();
	}

}
