package uk.ac.cam.ml421.PowerExperiments;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.View;
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
	Button buttonKill;
	
	PowerManager powerManager;
	LocationManager locationManager;
	WakeLock partialWakeLock;
	
	MediaPlayer beepPlayer;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        partialWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    	partialWakeLock.setReferenceCounted(false);
    	beepPlayer = MediaPlayer.create(this, R.raw.beep2);        
        
        attachUiEvents();
    }
    
    void attachUiEvents() {
    	checkPartialWakeLock = (CheckBox)findViewById(R.id.check_partial_wakelock);
    	checkGps = (CheckBox)findViewById(R.id.check_gps);
    	editGpsDelay = (EditText)findViewById(R.id.edit_gps_delay);
    	checkGpsBeep = (CheckBox)findViewById(R.id.check_gps_beep);
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