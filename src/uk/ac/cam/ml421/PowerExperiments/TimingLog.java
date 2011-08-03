package uk.ac.cam.ml421.PowerExperiments;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public class TimingLog {
	
	PrintWriter file;
	
	public TimingLog() throws FileNotFoundException {
		new File("/sdcard/power").mkdirs();		
		file = new PrintWriter("/sdcard/power/timinglog-" + Utils.humanFriendlyTimestamp());
		logEvent("open");
	}
	
	public synchronized void logEvent(String event) {
		file.printf("%d %s\n", System.nanoTime(), event);
	}
	
	public synchronized void close() {
		logEvent("close");
		file.close();
	}
}
