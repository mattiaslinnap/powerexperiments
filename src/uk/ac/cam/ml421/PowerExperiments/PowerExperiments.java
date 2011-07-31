package uk.ac.cam.ml421.PowerExperiments;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
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
	Button buttonKill;
	
	PowerManager powerManager;
	LocationManager locationManager;
	WakeLock partialWakeLock;
	Camera camera;
	MediaPlayer beepPlayer;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
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
				if (camera != null) {
					Parameters params = camera.getParameters();
					params.setFlashMode(Parameters.FLASH_MODE_OFF);
					camera.setParameters(params);
					camera.release();
					camera = null;
				}				
				
				if (checked) {
					camera = Camera.open();
					Parameters params = camera.getParameters();
					params.setFlashMode(Parameters.FLASH_MODE_TORCH);
					camera.setParameters(params);
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
	
	public void beep() {
		beepPlayer.start();
	}
}