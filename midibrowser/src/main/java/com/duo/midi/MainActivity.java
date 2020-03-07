package com.duo.midi;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.duosuccess.midi.R;
import com.viewpagerindicator.TabPageIndicator;

public class MainActivity extends FragmentActivity {
	private static final String TAG = "Main";
	private boolean isMusicPageSelected = true;
	MusicFragment musicFragment = null;
	SonarFragment sonarFragment = null;
	DecentralizedFragment decentralizedFragment = null;
	TabPageIndicator indicator = null;
	public static MainActivity instance = null;
	WifiLock wifiLock;
	public static final String BATTER_SAVE_ALERT_KEY = "batterySaveKey";

	@SuppressLint("WifiManagerLeak")
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		instance = this;

		this.setContentView(R.layout.main);
		FragmentStatePagerAdapter adapter = new DuosuccessAdapter(
				getSupportFragmentManager());

		this.musicFragment = new MusicFragment();
		this.musicFragment.setRetainInstance(true);
		this.sonarFragment = new SonarFragment();
		this.sonarFragment.setRetainInstance(true);
		this.decentralizedFragment = new DecentralizedFragment();
		this.decentralizedFragment.setRetainInstance(true);

		ViewPager pager = (ViewPager) findViewById(R.id.pager);
		pager.setAdapter(adapter);

		this.indicator = (TabPageIndicator) findViewById(R.id.indicator);

		indicator.setViewPager(pager);
		indicator.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageSelected(int pageNo) {
				if (pageNo == 1 ) {
//					sonarFragment.startLocationService();
				}

				isMusicPageSelected = (pageNo == 0);
			}
		});

		wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
				.createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
		wifiLock.acquire();

		// check version.
		try {
			new UpgradeChecker(this).execute();
		} catch (Exception e) {
		}
		batterySaveAlert();
	}

	private void batterySaveAlert() {
		final SharedPreferences prefs = MainActivity.this
				.getPreferences(MODE_PRIVATE);
		if (!prefs.getBoolean(BATTER_SAVE_ALERT_KEY, true)) {
			return;
		}
		AlertDialog.Builder batterySaveAlertBuilder = new AlertDialog.Builder(
				this);
		View alertView = View.inflate(this, R.layout.alert, null);
		final TextView alertContentView = (TextView) alertView
				.findViewById(R.id.alertTextContent);
		final CheckBox alertCheckbox = (CheckBox) alertView
				.findViewById(R.id.alertCheckbox);
		alertContentView
				.setText("使用華為手機得用戶，為了避免省電程序阻止播放，請做如下設置：\n 打開手機管家->省電管理->受保護的後臺應用->在多成音樂瀏覽器后打鉤, 同時設置普通省電模式。\n 使用其他省電程序的請將 多成音樂瀏覽器加入類似的白名單。");
		batterySaveAlertBuilder.setCancelable(false).setView(alertView)
				.setTitle("溫馨提示").setPositiveButton("好", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int arg1) {

						if (alertCheckbox.isChecked()) {
							prefs.edit()
									.putBoolean(BATTER_SAVE_ALERT_KEY, false)
									.commit();
						}
						dialog.dismiss();
					}
				});
		batterySaveAlertBuilder.create().show();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		wifiLock.release();
	}

	private String getAppVersion() {
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			return pInfo.versionName;

		} catch (NameNotFoundException e1) {
			e1.printStackTrace();
		}
		return "0";
	}

	class DuosuccessAdapter extends FragmentStatePagerAdapter {
		public DuosuccessAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {

			if (position == 0) {
				return musicFragment;
			} else if (position == 1) {
				return sonarFragment;
			} else {
				return decentralizedFragment;
			}
		}

		@Override
		public CharSequence getPageTitle(int position) {

			if (position == 0) {
				return "多成音樂";
			} else if (position == 1){
				return "真太陽時";
			} else {
				return "去中心化";
			}

		}

		@Override
		public int getCount() {
			return 3;
		}
	}

	@Override
	public void onBackPressed() {
		Log.i(TAG, "Main back pressed.");

//		if (isMusicPageSelected) {
			boolean handled = this.musicFragment.onBackPressed();
			handled = this.decentralizedFragment.onBackPressed();
			if (!handled) {
				super.onBackPressed();
			}
//		} else {
//			super.onBackPressed();
//		}
	}

	public MusicFragment getMusicFragment() {
		return musicFragment;
	}

	public void setMusicFragment(MusicFragment musicFragment) {
		this.musicFragment = musicFragment;
	}

	public TabPageIndicator getIndicator() {
		return indicator;
	}

	public void setIndicator(TabPageIndicator indicator) {
		this.indicator = indicator;
	}

}
