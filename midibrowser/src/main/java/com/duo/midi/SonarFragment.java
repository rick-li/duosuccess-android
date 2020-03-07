package com.duo.midi;

import java.util.Timer;
import java.util.TimerTask;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.duosuccess.midi.R;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;



public class SonarFragment extends Fragment {
	private static final String SONAR_URL = "https://rick-li.github.io/midi-browser/sonar.html";
	private static final String TAG = "duosuccess-sonar";
	public static final String PREFS_NAME = "LastAvailableLocation";

	public static final String LATITUDE = "latitude";
	public static final String LONGITUDE = "longitude";
	public static final String ADDR = "addr";
	private WebView sonarWebView;
	private ProgressDialog pd;
//	private LocationClient mLocationClient;
	private Timer locationTimer;
	private final Handler handler = new Handler();
	private volatile boolean initialzied = false;

//	public BDLocationListener myListener = new MyLocationListener();

	private static final int REQUEST_FINE_LOCATION = 1;
	private String geolocationOrigin;
	private GeolocationPermissions.Callback geolocationCallback;

	class GeoWebChromeClient extends WebChromeClient {
		@Override
		public void onGeolocationPermissionsShowPrompt(String origin,
													   GeolocationPermissions.Callback callback) {
			// Geolocation permissions coming from this app's Manifest will only be valid for devices with
			// API_VERSION < 23. On API 23 and above, we must check for permissions, and possibly
			// ask for them.
			String perm = Manifest.permission.ACCESS_FINE_LOCATION;
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
					ContextCompat.checkSelfPermission(SonarFragment.this.getContext(), perm) == PackageManager.PERMISSION_GRANTED) {
				// we're on SDK < 23 OR user has already granted permission
				callback.invoke(origin, true, false);
			} else {
				if (!ActivityCompat.shouldShowRequestPermissionRationale(SonarFragment.this.getActivity(), perm)) {
					// ask the user for permission
					ActivityCompat.requestPermissions(SonarFragment.this.getActivity(), new String[] {perm}, REQUEST_FINE_LOCATION);

					// we will use these when user responds
					geolocationOrigin = origin;
					geolocationCallback = callback;
				}
			}
		}
	}

	public SonarFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// ParseAnalytics.trackEvent("User open sonar.");
		View view = inflater.inflate(R.layout.sonar, null);
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {

		sonarWebView = (WebView) view.findViewById(R.id.sonarWebView);
		ActionBar actionBar = (ActionBar) view
				.findViewById(R.id.sonarActionbar);
		actionBar.addAction(new Action() {
			@Override
			public int getDrawable() {
				return R.drawable.ic_menu_refresh;
			}

			@Override
			public void performAction(View view) {
				sonarWebView.reload();
				initialzied = true;
//				startLocationService();
			}
		});
		init();

	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
			case REQUEST_FINE_LOCATION:
				boolean allow = false;
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					// user has allowed this permission
					allow = true;
				}
				if (geolocationCallback != null) {
					// call back to web chrome client
					geolocationCallback.invoke(geolocationOrigin, allow, false);
				}
				break;
		}
	}


	private void init() {

		WebSettings settings = sonarWebView.getSettings();
		settings.setJavaScriptEnabled(true);
		// WebView.enablePlatformNotifications();
		settings.setBuiltInZoomControls(true);
		settings.setJavaScriptCanOpenWindowsAutomatically(true);
		sonarWebView.setWebChromeClient(new GeoWebChromeClient());
		sonarWebView.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
			}
		});
		sonarWebView.loadUrl(SONAR_URL);
		initialzied = true;
		Log.d(TAG, "Sonar initialized.");
	}

	Timer locationBootstrapTimer;
	private volatile Looper mMyLooper;

//	public void startLocationService() {
//		Log.i(TAG, "Starting location service.");
//		if (locationBootstrapTimer != null) {
//			// in progress.
//			return;
//		}
//		locationBootstrapTimer = new Timer();
//		locationBootstrapTimer.schedule(new TimerTask() {
//			private void killMe() {
//				mMyLooper.quit();
//			}
//
//			@Override
//			public void run() {
//				Log.i(TAG, "Poll state, initialized " + isInitialzied());
//				if (!isInitialzied()) {
//					return;// wait till initialized.
//				}
//				getActivity().runOnUiThread(new Runnable() {
//
//					@Override
//					public void run() {
//						pd = ProgressDialog.show(
//								SonarFragment.this.getActivity(), "",
//								"正在獲取位置, 請稍候...");
//						if (mLocationClient != null
//								&& mLocationClient.isStarted()) {
//							mLocationClient.stop();
//						}
//						mLocationClient = new LocationClient(
//								getApplicationContext()); // 声明LocationClient类
//						mLocationClient.registerLocationListener(myListener);
//						setLocationOptions();
//						mLocationClient.start();
//						mLocationClient.requestLocation();
//						Log.i(TAG, "Start to retrieve location.");
//
//						locationTimer = new Timer();
//						locationTimer.schedule(new TimerTask() {
//
//							@Override
//							public void run() {
//								locationTimer.cancel();
//								Log.i(TAG,
//										"Unable to get location within 60 seconds, try last known location.");
//								handleLocation(
//										mLocationClient.getLastKnownLocation(),
//										true);
//							}
//
//						}, 30 * 1000);
//						if (locationBootstrapTimer != null) {
//							locationBootstrapTimer.cancel();
//							locationBootstrapTimer = null;
//						}
//
//					}
//
//				});
//				//
//
//			}
//		}, 0, 1000);
//
//	}

//	class MyLocationListener implements BDLocationListener {
//		@Override
//		public void onReceiveLocation(BDLocation location) {
//			handleLocation(location, false);
//		}
//
//		@Override
//		public void onReceivePoi(BDLocation location) {
//			handleLocation(location, false);
//		}
//	}
//
//	private boolean handleLocation(BDLocation location, boolean lastTry) {
//		SharedPreferences settings = this.getActivity().getSharedPreferences(
//				PREFS_NAME, 0);
//
//		if (location == null) {
//			Log.i(TAG, "Failed to get Baidu location.");
//			return false;
//		}
//		int locType = location.getLocType();
//
//		if (locType == 62 || locType == 63
//				|| (locType >= 162 && locType <= 167)) {
//			Log.i(TAG, "Failed to get Baidu location. locType is " + locType);
//			if (lastTry) {
//				if (pd != null) {
//					pd.dismiss();
//				}
//				if ("".equals(settings.getString(LATITUDE, ""))
//						|| "".equals(settings.getString(LONGITUDE, ""))) {
//
//					handler.post(new Runnable() {
//
//						@Override
//						public void run() {
//							Toast.makeText(SonarFragment.this.getActivity(),
//									"無法獲得當前位置", Toast.LENGTH_LONG).show();
//						}
//
//					});
//				} else {
//					Log.i(TAG, "Load from last available location:  lat-> "
//							+ settings.getString(LATITUDE, "") + " longi-> "
//							+ settings.getString(LONGITUDE, "") + " addr: "
//							+ settings.getString(ADDR, ""));
//					// Log.d(TAG,
//					// "javascript:window.geoPos = {'coords':{'latitude':"
//					// + locs[0]
//					// + ",'longitude':"
//					// + locs[1]
//					// + "}};showClock();");
//					mLocationClient.stop();
//					sonarWebView
//							.loadUrl("javascript:initPosition({'coords':{'latitude':"
//									+ settings.getString(LATITUDE, "")
//									+ ",'longitude':"
//									+ settings.getString(LONGITUDE, "")
//									+ ",'addr':'"
//									+ settings.getString(ADDR, "") + "'}});");
//					return true;
//				}
//			}
//			return false;
//		}
//		if (pd != null) {
//			pd.dismiss();
//		}
//		Log.i(TAG,
//				"Location received not null " + location.getAddrStr()
//						+ " lat: " + location.getLatitude() + " long: "
//						+ location.getLongitude());
//		mLocationClient.stop();
//
//		// save last available location
//		SharedPreferences.Editor editor = settings.edit();
//		editor.putString(LATITUDE, String.valueOf(location.getLatitude()));
//		editor.putString(LONGITUDE, String.valueOf(location.getLongitude()));
//		editor.putString(ADDR, String.valueOf(location.getAddrStr()));
//		editor.commit();
//		sonarWebView.loadUrl("javascript:initPosition({'coords':{'latitude':"
//				+ location.getLatitude() + ",'longitude':"
//				+ location.getLongitude() + ",'addr':'" + location.getAddrStr()
//				+ "'}});");
//		return true;
//	}

	public Context getApplicationContext() {
		return this.getActivity().getApplicationContext();
	}

//	private void setLocationOptions() {
//		LocationClientOption option = new LocationClientOption();
//		option.setOpenGps(true);
//		option.setAddrType("all");
//		option.setCoorType("bd09ll");
//		option.setScanSpan(10000);
//		option.setPriority(LocationClientOption.NetWorkFirst);
//		this.mLocationClient.setLocOption(option);
//	}

	@Override
	public void onDestroy() {
		this.initialzied = false;
//		if (mLocationClient != null && mLocationClient.isStarted()) {
//			mLocationClient.stop();
//			mLocationClient = null;
//		}
		super.onDestroy();
	}

	public boolean isInitialzied() {
		return initialzied;
	}

}
