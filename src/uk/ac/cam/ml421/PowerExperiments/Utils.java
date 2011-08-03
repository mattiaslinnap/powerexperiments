package uk.ac.cam.ml421.PowerExperiments;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
	
	public static String humanFriendlyTimestamp() {
		SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss.SSS");
		return timestampFormat.format(new Date());
    }
}
