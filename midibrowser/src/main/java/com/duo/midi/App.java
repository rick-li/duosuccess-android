package com.duo.midi;

import android.app.Application;
import android.util.Log;

//@ReportsCrashes(formKey = "", formUri = "https://racke1983cn.cloudant.com/acra-myapp/_design/acra-storage/_update/report", reportType = org.acra.sender.HttpSender.Type.JSON, httpMethod = org.acra.sender.HttpSender.Method.PUT, formUriBasicAuthLogin = "acknowerefinguedichiment", formUriBasicAuthPassword = "5sdXJDxoluab4HMVx11Y6QvN"

//)
public class App extends Application {
	public static String DEVICE_ID = "";
	private static App instance;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i("APP", "App oncreate");

		// ACRA.init(this);

		// TestinAgent.init(this, "afe8dfc7f4a4db78c70f9d8c85719eaa");
//		AVOSCloud.setLogLevel(AVOSCloud.LOG_LEVEL_VERBOSE);
//		AVOSCloud.initialize(this,
//				"5xtvwgqeg6aupmol6mb4aix1nw999yco56vspc48segcamv3",
//				"5gpbjwemyojw6s5on672xuse6glvt51jnnaha4jn2jsdwqf5");



//		AVOSLogger.info("App start");
		instance = this;
	}

	public static App getInstance() {
		return instance;
	}

}
