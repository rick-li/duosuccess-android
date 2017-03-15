package com.duo.midi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.duosuccess.midi.R;

public class UpgradeChecker extends AsyncTask<String, Void, Boolean> {
	private static final String TAG = "UpdateChecker";
	public static final String UPGRADE_SAVE_ALERT_KEY = "upgradeAlertKey";

	private static final String upgradeUrl = "http://rick-li.github.io/midi-browser/";
	private final Activity ctx;
	int versionCode = 0;
	String versionNumber = "";

	public UpgradeChecker(Activity ctx) {
		this.ctx = ctx;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		try {
			PackageInfo pInfo = ctx.getPackageManager().getPackageInfo(
					ctx.getPackageName(), 0);
			Log.i(TAG, "Current version code " + pInfo.versionCode);
			final SharedPreferences prefs = ctx
					.getPreferences(Context.MODE_PRIVATE);
			if (pInfo.versionCode < this.versionCode) {
				// if (true) {
				Log.i(TAG, "Creating dialog.");

				if (!prefs.getBoolean(UPGRADE_SAVE_ALERT_KEY, true)) {
					return;
				}
				View alertView = View.inflate(ctx, R.layout.alert, null);
				final TextView alertContentView = (TextView) alertView
						.findViewById(R.id.alertTextContent);
				final CheckBox alertCheckbox = (CheckBox) alertView
						.findViewById(R.id.alertCheckbox);
				alertContentView.setText("檢測到新版本" + this.versionNumber
						+ "，是否要升級?");
				AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
				builder.setView(alertView).setPositiveButton("好",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								Intent i = new Intent(Intent.ACTION_VIEW);
								i.setData(Uri.parse(upgradeUrl));
								ctx.startActivity(i);
							}

						});

				builder.setNegativeButton("以後再說",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								if (alertCheckbox.isChecked()) {
									prefs.edit()
											.putBoolean(UPGRADE_SAVE_ALERT_KEY,
													false).commit();
								}
							}

						});
				builder.create().show();

			}
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Unable to get version of app.", e);
		}
	}

	public void checkForUpdate(final Activity ctx) {
		String checkVerUrl = "http://rick-li.github.io/midi-browser/android-version.xml";
		boolean redirect = false;
		try {
			HttpURLConnection cn = (HttpURLConnection) new URL(checkVerUrl)
					.openConnection();
			int status = cn.getResponseCode();
			if (status != HttpURLConnection.HTTP_OK) {
				if (status == HttpURLConnection.HTTP_MOVED_TEMP
						|| status == HttpURLConnection.HTTP_MOVED_PERM
						|| status == HttpURLConnection.HTTP_SEE_OTHER)
					redirect = true;
			}
			if (redirect) {
				String newUrl = cn.getHeaderField("Location");
				cn = (HttpURLConnection) new URL(newUrl).openConnection();
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(
					cn.getInputStream()));

			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser xpp = factory.newPullParser();

			xpp.setInput(in);
			int eventType = xpp.getEventType();

			while (eventType != XmlPullParser.END_DOCUMENT) {

				if (eventType == XmlPullParser.START_TAG) {
					if ("versionCode".equalsIgnoreCase(xpp.getName())) {
						xpp.next();
						versionCode = Integer.parseInt(xpp.getText());
					} else if ("versionNumber".equalsIgnoreCase(xpp.getName())) {
						xpp.next();
						versionNumber = xpp.getText();
					}
				}
				eventType = xpp.next();
			}

			Log.i(TAG, "New versionCode: " + versionCode);
			Log.i(TAG, "versionNumber: " + versionNumber);
			in.close();
		} catch (Exception e) {
			Log.e(TAG, "Unable to check update.", e);
			ctx.runOnUiThread(new Runnable() {

				@Override
				public void run() {

					Toast.makeText(ctx, "無法檢測更新", 3000).show();
				}

			});
		}
	}

	@Override
	protected Boolean doInBackground(String... params) {
		checkForUpdate(this.ctx);
		return null;
	}

}
