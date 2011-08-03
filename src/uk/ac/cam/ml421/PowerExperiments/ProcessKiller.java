package uk.ac.cam.ml421.PowerExperiments;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.Process;
import android.util.Log;

public class ProcessKiller {

	public static final String TAG = PowerExperiments.TAG;
	
	public static final String[] WHITELIST = new String[] {
		"system",
		"inputmethod",
		"android.launcher",
		"android.nfc",
		"android.phone",
		"process.acore",
		"android.settings",
		"android.vending",
		"process.media",
		"process.gapps",
		};
	
	public static void killUselessBackgroundServices(Context context) {
		ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		
		
		Set<String> whitelist = new HashSet<String>(Arrays.asList(WHITELIST));
		for (RunningAppProcessInfo pi : am.getRunningAppProcesses()) {
			if (pi.pid != Process.myPid()) {
				boolean kill = true;
				// If any whitelist substrings are in the name, do not kill.
				for (String white : whitelist) {
					if (pi.processName.contains(white)) {
						kill = false;
						break;
					}
				}
				
				if (kill) {
					for (String s : pi.pkgList) {
						Log.e(TAG, "Killing " + pi.processName);
						am.restartPackage(s);
					}
				}
			}
		}
	}
	
}
