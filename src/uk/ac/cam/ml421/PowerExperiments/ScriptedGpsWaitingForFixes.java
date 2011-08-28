package uk.ac.cam.ml421.PowerExperiments;

import java.io.FileNotFoundException;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class ScriptedGpsWaitingForFixes {
	
	public static final String TAG = PowerExperiments.TAG;
	
	public static final boolean PULSE_CAMERA = false;
	public static final int NUM_LOOPS = 30;
	public static final int FIXES_PER_LOOP = 10;
	
	PowerExperiments activity;
	LocationManager lm;
	WakeLock wakeLock;
	TimingLog timingLog;
	
	volatile int loopCount;
	volatile int fixCount;
	
	public ScriptedGpsWaitingForFixes(PowerExperiments activity) {
		this.activity = activity;
	}
	
	public void startScript() {
		lm = (LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);
		PowerManager pm = (PowerManager)activity.getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		wakeLock.setReferenceCounted(false);
		wakeLock.acquire();

		activity.setInfo("Starting script");
		try {
			timingLog = new TimingLog();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "UH NO!", e);
			activity.setInfo("ERROR ERROR");
		}
		loopCount = 0;
		startLoop();
	}
	
	public void endScript() {
		timingLog.close();
		wakeLock.release();
		activity.setInfo("Script finished");
		try {
			activity.beep();
			Thread.sleep(300);
			activity.beep();
			Thread.sleep(300);
			activity.beep();
			Thread.sleep(300);
			activity.beep();
			Thread.sleep(300);
			activity.beep();
			Thread.sleep(300);
			activity.beep();
			Thread.sleep(300);
			activity.beep();
			Thread.sleep(300);
			activity.beep();
		} catch (InterruptedException e) {
			Log.e(TAG, "UH NO!", e);
			activity.setInfo("ERROR ERROR");
		}
	}

	public void startLoop() {
		timingLog.logEvent("loop" + loopCount);
		try {
			Thread.sleep(5000);
			insertPulse();
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			Log.e(TAG, "UH NO!", e);
			activity.setInfo("ERROR ERROR");
		}
		
		fixCount = 0;
		timingLog.logEvent("gpsstart");
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0.0f, gpsListener);
	}
	
	public void endLoop() {
		lm.removeUpdates(gpsListener);
		timingLog.logEvent("gpsend");
		try {
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			Log.e(TAG, "UH NO!", e);
			activity.setInfo("ERROR ERROR");
		}
		activity.beep();
		systemCleanup();
		++loopCount;
		if (loopCount >= NUM_LOOPS)
			endScript();
		else
			startLoop();
	}
	
	LocationListener gpsListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			if (fixCount < FIXES_PER_LOOP) {
				timingLog.logEvent("gpsfix");
				++fixCount;
			} else if (fixCount == FIXES_PER_LOOP) {
				endLoop();
			} else {
				// Extra fixes after removing listener, ignore.
			}
		}
		public void onProviderDisabled(String provider) {}
		public void onProviderEnabled(String provider) {}
		public void onStatusChanged(String provider, int status, Bundle extras) {}
	};

	void insertPulse() throws InterruptedException {
		if (PULSE_CAMERA) {
			Camera camera = Camera.open();
			Thread.sleep(1000);
			
			Parameters params = camera.getParameters();
			params.setFlashMode(Parameters.FLASH_MODE_TORCH);			
			camera.setParameters(params);
			timingLog.logEvent("spinstart");			
			Thread.sleep(5000);
			timingLog.logEvent("spinend");			
			params = camera.getParameters();
			params.setFlashMode(Parameters.FLASH_MODE_OFF);			
			camera.setParameters(params);
			
			Thread.sleep(1000);
			camera.release();
			camera = null;
		} else {
			timingLog.logEvent("spinstart");
			spinSleep(5000);
			timingLog.logEvent("spinend");
		}
	}
	
	void spinSleep(long delay) {
		long start = System.nanoTime();
		while (System.nanoTime() - start < delay * 1000000)
			;
	}
	
	void systemCleanup() {
		ProcessKiller.killUselessBackgroundServices(activity);
		System.gc();
		System.runFinalization();
	}
	
	
}

