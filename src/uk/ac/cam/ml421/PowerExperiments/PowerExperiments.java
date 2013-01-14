package uk.ac.cam.ml421.PowerExperiments;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.Parameters;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;


public class PowerExperiments extends Activity {
    
	public static final String TAG = "PowerExperiments";
	
	PowerManager powerManager;
	LocationManager locationManager;
	SensorManager sensorManager;
	Vibrator vibrator;
	WifiManager wifiManager;
	WakeLock partialWakeLock;
	WifiLock wifiLock;
	Camera camera;
	MediaPlayer beepPlayer;
	CpuSpinner cpuSpinner;
	ScriptedGps scriptedGps;
	ScriptedGpsWaitingForFixes scriptedGpsWaitingForFixes;
	TextView textInfo;
	Handler handler = new Handler();
	int accelEventCounter;
	long latestAccelTimestamp;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        //logDeviceInfo();        
        
        powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        partialWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    	partialWakeLock.setReferenceCounted(false);
    	wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, TAG);
    	wifiLock.setReferenceCounted(false);
    	camera = null;
    	beepPlayer = MediaPlayer.create(this, R.raw.beep2);
    	textInfo = (TextView)findViewById(R.id.text_info);
        
        attachUiEvents();
    }
    
    void attachUiEvents() {
    	final PowerExperiments me = this;

    	((CheckBox)findViewById(R.id.check_partial_wakelock)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton view, boolean checked) {
				if (checked)
					partialWakeLock.acquire();
				else
					partialWakeLock.release();
				beep();
			}
		});
    	
    	((Button)findViewById(R.id.button_delete_agps)).setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				if (!locationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, "delete_aiding_data", null)) {
					Toast.makeText(me, "Delete failed!", Toast.LENGTH_SHORT).show();
				}
			}
    	});
    	
    	((CheckBox)findViewById(R.id.check_wifilock)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton view, boolean checked) {
				if (checked)
					wifiLock.acquire();
				else
					wifiLock.release();
				beep();
			}
		});

    	((CheckBox)findViewById(R.id.check_gps)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton view, boolean checked) {
				if (checked) {
					long delay = Long.parseLong(((EditText)findViewById(R.id.edit_gps_delay)).getText().toString());
					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, delay, 0, gpsListener);					
				} else {
					locationManager.removeUpdates(gpsListener);
				}
			}
		});
    	
    	((CheckBox)findViewById(R.id.check_camera)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton view, boolean checked) {
				if (checked) {
					if (camera == null) {
						camera = Camera.open();
						Parameters params = camera.getParameters();
						params.setFocusMode(Parameters.FOCUS_MODE_INFINITY);
						params.setAntibanding(Parameters.ANTIBANDING_OFF);
						camera.setParameters(params);
						
						if (Build.MANUFACTURER.equalsIgnoreCase("samsung"))
							camera.startPreview();
					} else {
						Toast.makeText(me, "Camera already open", Toast.LENGTH_SHORT).show();
					}
				} else {
					if (camera != null) {
						camera.release();
						camera = null;
					} else {
						Toast.makeText(me, "Camera already closed", Toast.LENGTH_SHORT).show();
					}
				}
			}
		});
    	
    	((CheckBox)findViewById(R.id.check_flash)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton view, boolean checked) {
				if (camera == null) {
					Toast.makeText(me, "No open camera", Toast.LENGTH_SHORT).show();
				} else {				
					if (checked) {
						Parameters params = camera.getParameters();
						params.setFlashMode(Parameters.FLASH_MODE_TORCH);
						camera.setParameters(params);
					} else {
						Parameters params = camera.getParameters();
						params.setFlashMode(Parameters.FLASH_MODE_OFF);
						camera.setParameters(params);
					}
				}
			}
		});
    	
    	((CheckBox)findViewById(R.id.check_bright)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton view, boolean checked) {
				WindowManager.LayoutParams params = getWindow().getAttributes();
				params.screenBrightness = checked ? 1.0f : 0.1f;
				getWindow().setAttributes(params);
			}
		});
    	
    	addSensorEvents((CheckBox)findViewById(R.id.check_accel), Sensor.TYPE_ACCELEROMETER, "No accelerometer", accelListener);
    	addSensorEvents((CheckBox)findViewById(R.id.check_gyro), Sensor.TYPE_GYROSCOPE, "No gyroscope", gyroListener);
    	
    	((CheckBox)findViewById(R.id.check_vibrator)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
    		public void onCheckedChanged(CompoundButton view, boolean checked) {
    			if (checked)
    				vibrator.vibrate(60000);
    			else
    				vibrator.cancel();
    		}
    	});
    	
    	((CheckBox)findViewById(R.id.check_cpu)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton view, boolean checked) {
				if (cpuSpinner != null) {
					cpuSpinner.quit = true;
					cpuSpinner = null;
				}
				
				if (checked) {
					cpuSpinner = new CpuSpinner();
					cpuSpinner.start();
				}
			}
		});
    	
    	((Button)findViewById(R.id.button_wifi)).setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				if (!wifiManager.startScan())
					Toast.makeText(me, "Failed to start scan", Toast.LENGTH_SHORT).show();
			}
    	});
    	
    	((Button)findViewById(R.id.button_fixscript)).setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				scriptedGpsWaitingForFixes = new ScriptedGpsWaitingForFixes(me);
				Log.i(TAG, "GPS Script with fixes starting...");
				scriptedGpsWaitingForFixes.startScript();
			}
    	});
    	
    	((Button)findViewById(R.id.button_script)).setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				scriptedGps = new ScriptedGps(me);
				Log.i(TAG, "GPS Script starting...");
				//scriptedGps.run();  // run in UI thread.
				scriptedGps.start();
			}
    	});
    	
    	((Button)findViewById(R.id.button_kill)).setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				finish();
				System.exit(0);
			}
		});
    	
    	((Button)findViewById(R.id.button_killprocess)).setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				ProcessKiller.killUselessBackgroundServices(me);
			}
		});
    	
    	((Button)findViewById(R.id.button_beep)).setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				beep();
			}
		});
    }    
    
    void addSensorEvents(CheckBox check, final int sensorType, final String failureToast, final SensorEventListener listener) {
    	final Activity me = this;
    	check.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton view, boolean checked) {
				Log.d(TAG, "checked " + checked);
				if (checked) {
					List<Sensor> sensors = sensorManager.getSensorList(sensorType);
					if (sensors.size() == 0) {
						Toast.makeText(me, failureToast, Toast.LENGTH_SHORT).show();
					} else {
						if (sensorType == Sensor.TYPE_ACCELEROMETER) {
							accelEventCounter = 0;
							latestAccelTimestamp = 0;
						}
						sensorManager.registerListener(listener, sensorManager.getDefaultSensor(sensorType), SensorManager.SENSOR_DELAY_FASTEST);
					}
				} else {
					sensorManager.unregisterListener(listener);
					if (sensorType == Sensor.TYPE_ACCELEROMETER)
						Toast.makeText(me, "Accel events: " + accelEventCounter, Toast.LENGTH_LONG).show();
				}
			}
		});
    }
    
    LocationListener gpsListener = new LocationListener() {
		public void onStatusChanged(String provider, int status, Bundle extras) {}
		public void onProviderEnabled(String provider) {}
		public void onProviderDisabled(String provider) {}		
		public void onLocationChanged(Location location) {
			if (((CheckBox)findViewById(R.id.check_gps_beep)).isChecked()) {
				beep();
			}
		}
	};
	
	SensorEventListener accelListener = new SensorEventListener() {
		public void onSensorChanged(SensorEvent event) {
			++accelEventCounter;
			long millisSinceLast = (event.timestamp - latestAccelTimestamp) / 1000000;
			if (millisSinceLast > 5000) {
				latestAccelTimestamp = event.timestamp;
				beep();
			}
		}
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}
	};
	
	SensorEventListener gyroListener = new SensorEventListener() {
		public void onSensorChanged(SensorEvent event) {}
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}
	};
	
	class CpuSpinner extends Thread {
		volatile boolean quit = false;
		
		public void run() {			
			while (!quit);
		}
	}
	
	public void beep() {
		beepPlayer.start();
	}
	
	public void setInfo(final String message) {
		handler.post(new Runnable() {
			public void run() {
				textInfo.setText(message);
			}
		});
	}
	
	public void logDeviceInfo() {
		Log.i(TAG, "Build board: " + Build.BOARD);
        Log.i(TAG, "Build brand: " + Build.BRAND);
        Log.i(TAG, "Build device: " + Build.DEVICE);
        Log.i(TAG, "Build hardware: " + Build.HARDWARE);
        Log.i(TAG, "Build manufacturer: " + Build.MANUFACTURER);
        Log.i(TAG, "Build model: " + Build.MODEL);
        Log.i(TAG, "Build product: " + Build.PRODUCT);
                
        // Not on Android 2.2
//        int cameras = Camera.getNumberOfCameras();
//        Log.i(TAG, "Number of cameras: " + Camera.getNumberOfCameras());
//        for (int i = 0; i < cameras; ++i) {
//        	Camera cam = Camera.open(i);
//        	Parameters params = cam.getParameters();
//        	Log.i(TAG, "Camera " + i + " flash modes: " + params.getSupportedFlashModes() + ", currently " + params.getFlashMode());
//        	Log.i(TAG, "Camera " + i + " focus modes: " + params.getSupportedFocusModes() + ", currently " + params.getFocusMode());
//        	Log.i(TAG, "Camera " + i + " white balance modes: " + params.getSupportedWhiteBalance() + ", currently " + params.getWhiteBalance());
//        	Log.i(TAG, "Camera " + i + " antibanding modes: " + params.getSupportedAntibanding() + ", currently " + params.getAntibanding());
//        	cam.release();
//        	cam = null;
//        }
	}
}