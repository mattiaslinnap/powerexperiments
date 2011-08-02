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
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class PowerExperiments extends Activity {
    
	public static final String TAG = "PowerExperiments";
	
	PowerManager powerManager;
	LocationManager locationManager;
	SensorManager sensorManager;
	WifiManager wifiManager;
	WakeLock partialWakeLock;
	Camera camera;
	MediaPlayer beepPlayer;
	CpuSpinner cpuSpinner;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        logDeviceInfo();        
        
        powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        partialWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    	partialWakeLock.setReferenceCounted(false);
    	camera = null;
    	beepPlayer = MediaPlayer.create(this, R.raw.beep2);        
        
        attachUiEvents();
    }
    
    void attachUiEvents() {
    	final Activity me = this;

    	((CheckBox)findViewById(R.id.check_partial_wakelock)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton view, boolean checked) {
				if (checked)
					partialWakeLock.acquire();
				else
					partialWakeLock.release();
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
    	
    	((Button)findViewById(R.id.button_kill)).setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				finish();
				System.exit(0);
			}
		});
    }    
    
    void addSensorEvents(CheckBox check, final int sensorType, final String failureToast, final SensorEventListener listener) {
    	final Activity me = this;
    	check.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton view, boolean checked) {
				if (checked) {
					List<Sensor> sensors = sensorManager.getSensorList(sensorType);
					if (sensors.size() == 0)
						Toast.makeText(me, failureToast, Toast.LENGTH_SHORT).show();
					else
						sensorManager.registerListener(listener, sensors.get(0), SensorManager.SENSOR_DELAY_FASTEST);
				} else {
					sensorManager.unregisterListener(listener);
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
		public void onSensorChanged(SensorEvent event) {}
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
	
	public void logDeviceInfo() {
		Log.i(TAG, "Build board: " + Build.BOARD);
        Log.i(TAG, "Build brand: " + Build.BRAND);
        Log.i(TAG, "Build device: " + Build.DEVICE);
        Log.i(TAG, "Build hardware: " + Build.HARDWARE);
        Log.i(TAG, "Build manufacturer: " + Build.MANUFACTURER);
        Log.i(TAG, "Build model: " + Build.MODEL);
        Log.i(TAG, "Build product: " + Build.PRODUCT);
        
        int cameras = Camera.getNumberOfCameras();
        Log.i(TAG, "Number of cameras: " + Camera.getNumberOfCameras());
        for (int i = 0; i < cameras; ++i) {
        	Camera cam = Camera.open(i);
        	Parameters params = cam.getParameters();
        	Log.i(TAG, "Camera " + i + " flash modes: " + params.getSupportedFlashModes());
        	cam.release();
        	cam = null;
        }
	}
}