package uk.ac.cam.ml421.PowerExperiments;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class ScriptedGps extends Thread {
	
	public static final String TAG = PowerExperiments.TAG;
	
	public static final int NUM_LOOPS = 10;
	public static final int GPS_PER_LOOP = 3;
	
	PowerExperiments activity;
	WakeLock wakeLock;
	TimingLog timingLog;
	
	public ScriptedGps(PowerExperiments activity) {
		this.activity = activity;
	}
	
	public void run() {
		Looper.prepare();
		LocationManager lm = (LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);
		PowerManager pm = (PowerManager)activity.getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		wakeLock.setReferenceCounted(false);
		wakeLock.acquire();

		systemCleanup();
	
		activity.setInfo("Starting script");
		
		try {
			timingLog = new TimingLog();
			for (int loop = 0; loop < NUM_LOOPS; ++loop) {
				timingLog.logEvent("loop" + loop);
				
				Thread.sleep(5000);
				timingLog.logEvent("spinstart");
				spinSleep(5000);
				timingLog.logEvent("spinend");
				Thread.sleep(10000);
				
				for (int gps = 0; gps < GPS_PER_LOOP; ++gps) {
					timingLog.logEvent("gpsstart");
					listen(lm, 10000);
					timingLog.logEvent("gpsend");
					Thread.sleep(20000);
				}
				
				systemCleanup();
			}
			timingLog.close();			
		} catch (Exception e) {
			Log.e(TAG, "UH NO!", e);
			activity.setInfo("ERROR ERROR");
		}
		wakeLock.release();
		activity.beep();
	}
	
	void spinSleep(long delay) {
		long start = System.nanoTime();
		while (System.nanoTime() - start < delay * 1000000)
			;
	}
	
	void listen(LocationManager lm, long delay) throws InterruptedException {
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0.0f, gpsListener);
		Thread.sleep(delay);
		lm.removeUpdates(gpsListener);
	}
	
	void systemCleanup() {
		ProcessKiller.killUselessBackgroundServices(activity);
		System.gc();
		System.runFinalization();
	}
	
	LocationListener gpsListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			Log.e(PowerExperiments.TAG, "Test invalidated, should not get any locations!");
			try {
				activity.beep();
				Thread.sleep(500);
				activity.beep();
				Thread.sleep(500);
				activity.beep();
				timingLog.logEvent("invalid");
			} catch (InterruptedException e) {
			}
		}
		public void onProviderDisabled(String provider) {}
		public void onProviderEnabled(String provider) {}
		public void onStatusChanged(String provider, int status, Bundle extras) {}
	};
}
