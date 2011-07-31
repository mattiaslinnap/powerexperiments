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
import android.widget.CompoundButton.OnCheckedChangeListener;

public class PowerExperiments extends Activity {
    
	public static final String TAG = "PowerExperiments";
	
	CheckBox checkPartialWakeLock;
	CheckBox checkGps;
	EditText editGpsDelay;
	CheckBox checkGpsBeep;
	CheckBox checkFlash;
	CheckBox checkBright;
	CheckBox checkGyro;
	Button buttonKill;
	
	PowerManager powerManager;
	LocationManager locationManager;
	SensorManager sensorManager;
	WakeLock partialWakeLock;
	Camera camera;
	MediaPlayer beepPlayer;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        logDeviceInfo();        
        
        powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        partialWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    	partialWakeLock.setReferenceCounted(false);
    	camera = null;
    	beepPlayer = MediaPlayer.create(this, R.raw.beep2);        
        
        attachUiEvents();
    }
    
    void attachUiEvents() {
    	checkPartialWakeLock = (CheckBox)findViewById(R.id.check_partial_wakelock);
    	checkGps = (CheckBox)findViewById(R.id.check_gps);
    	editGpsDelay = (EditText)findViewById(R.id.edit_gps_delay);
    	checkGpsBeep = (CheckBox)findViewById(R.id.check_gps_beep);
    	checkFlash = (CheckBox)findViewById(R.id.check_flash);
    	checkBright = (CheckBox)findViewById(R.id.check_bright);
    	checkGyro = (CheckBox)findViewById(R.id.check_gyro);
    	buttonKill = (Button)findViewById(R.id.button_kill);
    	
    	checkPartialWakeLock.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton view, boolean checked) {
				if (checked)
					partialWakeLock.acquire();
				else
					partialWakeLock.release();
				beep();
			}
		});

    	checkGps.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton view, boolean checked) {
				if (checked) {
					long delay = Long.parseLong(editGpsDelay.getText().toString());
					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, delay, 0, gpsListener);					
				} else {
					locationManager.removeUpdates(gpsListener);
				}
			}
		});
    	
    	checkFlash.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton view, boolean checked) {
				if (checked) {
					camera = Camera.open();
					Parameters params = camera.getParameters();
					params.setFlashMode(Parameters.FLASH_MODE_TORCH);
					camera.setParameters(params);
				} else {
					Parameters params = camera.getParameters();
					params.setFlashMode(Parameters.FLASH_MODE_OFF);
					camera.setParameters(params);
					camera.release();
					camera = null;
				}
			}
		});
    	
    	checkBright.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton view, boolean checked) {
				WindowManager.LayoutParams params = getWindow().getAttributes();
				params.screenBrightness = checked ? 1.0f : 0.1f;
				getWindow().setAttributes(params);
			}
		});
    	
    	checkGyro.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton view, boolean checked) {
				if (checked) {
					List<Sensor> gyros = sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE);
					Log.i(TAG, "There are " + gyros.size() + " gyros.");
					sensorManager.registerListener(sensorListener, gyros.get(0), SensorManager.SENSOR_DELAY_FASTEST);
				} else {
					sensorManager.unregisterListener(sensorListener);
				}
			}
		});
    	
    	buttonKill.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				finish();
				System.exit(0);
			}
		});
    }    
    
    LocationListener gpsListener = new LocationListener() {
		public void onStatusChanged(String provider, int status, Bundle extras) {}
		public void onProviderEnabled(String provider) {}
		public void onProviderDisabled(String provider) {}		
		public void onLocationChanged(Location location) {
			if (checkGpsBeep.isChecked()) {
				beep();
			}
		}
	};
	
	SensorEventListener sensorListener = new SensorEventListener() {
		public void onSensorChanged(SensorEvent event) {}
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}
	};
	
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