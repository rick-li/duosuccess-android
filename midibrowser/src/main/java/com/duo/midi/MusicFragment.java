package com.duo.midi;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.duo.midi.alarm.WakefulIntentService;
import com.duo.midi.music.MusicRepeatListener;
import com.duosuccess.midi.R;
import com.google.common.collect.ImmutableMap;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;

public class MusicFragment extends Fragment implements Handler.Callback {
	private static final String TAG = "midi-browser";
	private static final String KEY_CONTENT = "MusicFragment:Content";

	private static final int CLICK_ON_WEBVIEW = 1;

	private static final String homeUrl = "http://69.195.73.224";
	// private static final String homeUrl =
	// "http://rick-li.github.io/android-midi/index.html";
	// private static final String homeUrl = "http://www.baidu.com";

	private final String tmpMidiFile = "duo-music.mid";
	public static WebView webView;
	private Timer musicTimer;
	private Timer waitTimer;
	private volatile boolean needRepeat = false;
	private volatile boolean fullscreenLocked = false;
	private String mContent = "music";

	private RelativeLayout quitFullScreenBar;
	private ImageView quitFullScreenBtn;

	public static int waitInterval = 1 * 60 * 60 * 1000;
	public static int musicDuration = 1 * 60 * 60 * 1000;
	public static int waitAddition = 10 * 60 * 1000;

	// public static int waitInterval = 10 * 1000;
	// public static int waitAddition = 6 * 1000;
	// public static int musicDuration = 10 * 1000;

	enum STATE {
		stop {
			@Override
			public String toString() {
				return "停止";
			}
		},
		wait {
			@Override
			public String toString() {
				return "等待";
			}
		},
		play {
			@Override
			public String toString() {
				return "播放";
			}
		}
	}

	private ActionBar actionBar;
	private ActionBar footer;
	private Handler handler;
	private ProgressDialog pd;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if ((savedInstanceState != null)
				&& savedInstanceState.containsKey(KEY_CONTENT)) {
			mContent = savedInstanceState.getString(KEY_CONTENT);
		}
		handler = new Handler(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.music, null);

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {

		actionBar = (ActionBar) this.getView().findViewById(R.id.actionbar);

		footer = (ActionBar) this.getView().findViewById(R.id.bottombar);
		quitFullScreenBar = (RelativeLayout) getView().findViewById(
				R.id.ly_btns);
		quitFullScreenBtn = (ImageView) getView().findViewById(
				R.id.iv_quit_fullscreen);
		footer.setTitle("停止");
		quitFullScreenBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				quitFullScreen();
			}

		});
		actionBar.setHomeAction(new Action() {

			@Override
			public int getDrawable() {
				return R.drawable.ic_title_home_default;
			}

			@Override
			public void performAction(View view) {
				webView.loadUrl(homeUrl);
			}

		});

		if (Build.VERSION.SDK_INT < 14) {
			actionBar.addAction(new Action() {

				@Override
				public int getDrawable() {
					return R.drawable.full_screen;
				}

				@Override
				public void performAction(View view) {
					goFullScreen();
				}
			});
		}
		actionBar.addAction(new Action() {

			@Override
			public int getDrawable() {
				return R.drawable.ic_menu_rotate_disable;
			}

			@Override
			public void performAction(View view) {
				ImageButton labelView = (ImageButton) view
						.findViewById(R.id.actionbar_item);
				needRepeat = !needRepeat;
				if (needRepeat) {
					try {
						// ParseAnalytics.trackEvent("User set repeat");
					} catch (Exception e) {
					}
					labelView.setImageResource(R.drawable.ic_menu_rotate);
					Toast.makeText(MusicFragment.this.getActivity(),
							"设定为每隔一小时播放", 1000).show();
				} else {
					labelView
							.setImageResource(R.drawable.ic_menu_rotate_disable);
					Toast.makeText(MusicFragment.this.getActivity(), "设定为单次播放",
							1000).show();
				}
			}

		});
		// refresh button
		actionBar.addAction(new Action() {

			@Override
			public int getDrawable() {
				return R.drawable.ic_menu_refresh;
			}

			@Override
			public void performAction(View view) {
				actionBar.setProgressBarVisibility(View.VISIBLE);
				handler.post(new Runnable() {

					@Override
					public void run() {
						webView.reload();

					}
				});
			}
		});

		// close button
		actionBar.addAction(new Action() {

			@Override
			public int getDrawable() {
				return R.drawable.ic_menu_close_clear_cancel;
			}

			@Override
			public void performAction(View view) {
				stopMedia();
				clearCache();
				MusicFragment.this.getActivity().finish();
				return;
			}

		});

		webView = (WebView) this.getView().findViewById(R.id.webView);
		WebSettings settings = webView.getSettings();
		settings.setJavaScriptEnabled(true);
		// WebView.enablePlatformNotifications();
		webView.requestFocusFromTouch();
		// settings.setPluginsEnabled(true);
		settings.setJavaScriptCanOpenWindowsAutomatically(true);
		settings.setDefaultZoom(ZoomDensity.CLOSE);
		settings.setBuiltInZoomControls(true);
		settings.setUseWideViewPort(true);
		settings.setSupportZoom(true);
		settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
		this.setObjectParam(settings, "setLoadWithOverviewMode", false);
		if (Build.VERSION.SDK_INT < 14) {
			this.setObjectParam(settings, "setDisplayZoomControls", false);
		}

		webView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Log.e(TAG, "Webview Touched.");
				if (MotionEvent.ACTION_UP == event.getAction()) {
					Log.e(TAG, "UP.");
					handler.sendEmptyMessage(CLICK_ON_WEBVIEW);
				}
				boolean consumed = webView.onTouchEvent(event);
				return consumed;
			}
		});

		// auto clear cache.
		clearCache();
		webView.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				Log.i(TAG, "Page started " + url);
				if (url.contains("/shop/") && url.startsWith("http:")) {
					url = url.replace("http:", "https:");
					url = url.replace("69.195.73.224", "www.duosuccess.com");
					Log.i(TAG, "new url is " + url);

				}
				fullscreenLocked = false;
				try {
					actionBar.setProgressBarVisibility(View.VISIBLE);
					// pd =
					// ProgressDialog.show(MusicFragment.this.getActivity(),
					// "", "页面加载，请稍侯");
					new Timer().schedule(new TimerTask() {

						@Override
						public void run() {
							if (pd != null) {
								pd.dismiss();
							}
						}

					}, 30 * 1000);
					stopMedia();
				} catch (Exception e) {
					Log.e(TAG, "Error stop music", e);
					// ParseAnalytics.trackEvent("Error stop music");
				}
				super.onPageStarted(view, url, favicon);
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Log.i(TAG, "Should overrid " + url);
				// handler.sendEmptyMessage(CLICK_ON_URL);
				stopMedia();
				if (url.contains("/shop/") && url.startsWith("http:")) {
					url = url.replace("http:", "https:");
					url = url.replace("69.195.73.224", "www.duosuccess.com");

					Log.i(TAG, "new url is " + url);
					view.loadUrl(url);
					return true;
				}

				return false;
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				// Avoid music to auto stop;
				// webView.loadUrl("javascript:document.getElementsByTagName('head')[0].appendChild('<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=1\">');");
				webView.loadUrl("javascript:window.stopmusic = function(){}");
				webView.loadUrl("javascript:midiExtractor.extract(document.querySelector('embed').src, window.location.href);");
				actionBar.setProgressBarVisibility(View.INVISIBLE);
				// pd.dismiss();
			}

			@Override
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				super.onReceivedError(view, errorCode, description, failingUrl);
				if (pd != null) {
					pd.dismiss();
				}
			}
		});

		class MidiExtractor {
			@SuppressWarnings("unused")
			public void extract(String midiFile, String pageUrl) {
				Log.i(TAG, "pageUrl is " + pageUrl);
				String midiUrl = midiFile;
				if (!midiFile.startsWith("http")) {
					midiUrl = pageUrl.substring(0, pageUrl.lastIndexOf("/"))
							+ "/" + midiFile;
				}
				Log.i(TAG, "midi is " + midiUrl);
				final Map<String, String> logDetail = ImmutableMap
						.<String, String> builder().put("midi", midiUrl)
						.build();
				// ParseAnalytics.trackEvent("Prepare download midi.",
				// logDetail);

				Toast.makeText(getActivity(), "准备下载音乐", 3000).show();
				clearCache();
				try {
					handler.post(new Runnable() {

						@Override
						public void run() {
							fullscreenLocked = true;
							quitFullScreen();
						}

					});
					setFooterText("正在下载音乐");
					URLConnection cn = new URL(midiUrl).openConnection();
					InputStream stream = cn.getInputStream();
					byte[] buffer = new byte[512];
					Activity activity = getActivity();
					FileOutputStream fos = activity.openFileOutput(tmpMidiFile,
							Activity.MODE_PRIVATE);
					int n = -1;
					while ((n = stream.read(buffer)) != -1) {
						fos.write(buffer, 0, n);
					}
					stream.close();
					fos.close();
					setFooterText("下载完成");
					// pd.dismiss();

				} catch (Exception e) {

					AVOSLogger.error("Unable to download music.");
					Log.e(TAG, "unable to play midi. ", e);
					Toast.makeText(getActivity(), "无法下载音乐文件", 5000).show();
				}

				try {

					playMusic();
				} catch (Exception e) {
					AVOSLogger.error("Unable to play music " + e.getMessage());
				}
			}
		}

		webView.addJavascriptInterface(new MidiExtractor(), "midiExtractor");
		webView.loadUrl(homeUrl);
		super.onViewCreated(view, savedInstanceState);
	}

	private void startWaitCountdown() {
		if (waitTimer != null) {
			Log.i(TAG, "Wait timer is already started.");
			return;
		}
		waitTimer = new Timer();
		final TimeCounter timeCounter = new TimeCounter();
		waitTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				timeCounter.increaseOneSec();

				setFooterText(STATE.wait
						+ " "
						+ new SimpleDateFormat("mm:ss").format(new Date(
								timeCounter.getStartMillSec())));

				if (timeCounter.getStartMillSec() == (waitInterval + waitAddition)) {
					stopWaitTimer();
					// Wait till alarm to be activated

				}
			}

		}, 0, 1000);
	}

	private void playMusic() throws Exception {

		Intent i = new Intent(this.getActivity(), MusicService.class);
		i.putExtra("midiFile", this.tmpMidiFile);
		this.getActivity().startService(i);
		final Date startDate = new Date();

		final SimpleDateFormat startSdf = new SimpleDateFormat("MM/dd HH:mm:ss");

		Log.i(TAG, "music start " + new Date());
		// stop after 1 hour
		if (musicTimer != null) {
			musicTimer.cancel();
		}
		musicTimer = new Timer();
		final long startTimeAccr = System.currentTimeMillis();
		final TimeCounter timeCounter = new TimeCounter();
		musicTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				timeCounter.increaseOneSec();

				setFooterText("开始时间 "
						+ startSdf.format(startDate)
						+ " 已播放 "
						+ new SimpleDateFormat("mm:ss").format(new Date(
								timeCounter.getStartMillSec())));
				long nowAccr = System.currentTimeMillis();
				if ((nowAccr - startTimeAccr) >= musicDuration) {
					stopMusicService();
					if (needRepeat) {
						if (alarmStarted) {
							WakefulIntentService.cancelAlarms(getActivity());
						}

						WakefulIntentService.scheduleAlarms(
								new MusicRepeatListener(), getActivity(), true);
						alarmStarted = true;
						Log.i(TAG, "Start schedule alarms.");

						startWaitCountdown();

					} else {
						webView.post(new Runnable() {

							@Override
							public void run() {
								webView.loadUrl(homeUrl);
							}

						});

					}
					Log.i(TAG, "music stop " + new Date().toString());

					musicTimer.cancel();
					musicTimer = null;
				}

			}
		}, 0, 1000);
	}

	private volatile boolean alarmStarted = false;

	private void setFooterText(final String text) {
		if (handler == null) {
			Log.e(TAG, "handler is null in setFooterText.");
			return;
		}
		handler.post(new Runnable() {

			@Override
			public void run() {
				footer.setTitle(text);
			}
		});
	}

	private void clearCache() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				webView.clearCache(true);
			}
		});
		String[] fileAry = this.getActivity().fileList();
		List<String> fileList = Arrays.asList(fileAry);
		if (fileList.contains(tmpMidiFile)) {
			if (this.getActivity().deleteFile(tmpMidiFile)) {
				Log.i(TAG, "Successfully cleared cache.");
				// ParseAnalytics.trackEvent("Cache clear successful.");
				Toast.makeText(this.getActivity(), "已经清除缓存音乐", 2000).show();
			}
		}

	}

	private void stopMusicService() {
		Log.i(TAG, "Stopping service");
		try {
			Intent i = new Intent(this.getActivity(), MusicService.class);
			this.getActivity().stopService(i);
		} catch (Exception e) {

		}
	}

	private void stopMedia() {
		stopMusicService();

		setFooterText(STATE.stop.toString());
		if (musicTimer != null) {
			musicTimer.cancel();
		}
		stopWaitTimer();
		clearCache();
	}

	public boolean onBackPressed() {
		Log.i(TAG, "music fragment back pressed");
		boolean handled = false;
		if (webView != null && webView.canGoBack()) {
			webView.goBack();
			handled = true;
		}
		stopMedia();
		return handled;
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "cancel alarms.");
		WakefulIntentService.cancelAlarms(this.getActivity());
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_CONTENT, mContent);
	}

	public WebView getWebView() {
		return webView;
	}

	public void setWebView(WebView webView) {
		this.webView = webView;
	}

	public boolean isNeedRepeat() {
		return needRepeat;
	}

	public void stopWaitTimer() {
		if (waitTimer != null) {
			Log.d(TAG, "Stopping WaitTimer.");
			waitTimer.cancel();
			waitTimer = null;
		}

	}

	public void setWaitTimer(Timer waitTimer) {
		this.waitTimer = waitTimer;
	}

	public Timer getWaitTimer() {
		return this.waitTimer;
	}

	boolean isInFullScreen = false;

	private void quitFullScreen() {
		actionBar.setVisibility(View.VISIBLE);
		footer.setVisibility(View.VISIBLE);
		quitFullScreenBar.setVisibility(View.GONE);

		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		lp.setMargins(0, 45, 0, 45);
		webView.setLayoutParams(lp);
		isInFullScreen = false;
	}

	private void goFullScreen() {
		if (fullscreenLocked) {
			return;
		}
		actionBar.setVisibility(View.GONE);
		footer.setVisibility(View.GONE);
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		lp.setMargins(0, 0, 0, 0);
		webView.setLayoutParams(lp);
		quitFullScreenBar.setVisibility(View.VISIBLE);
		isInFullScreen = true;

	}

	@Override
	public boolean handleMessage(Message msg) {

		if (msg.what == CLICK_ON_WEBVIEW) {

			goFullScreen();
			return true;
		}
		return false;
	}

	private void setObjectParam(Object paramObject, String paramString,
			boolean paramBoolean) {
		try {
			Class localClass = paramObject.getClass();
			Class[] arrayOfClass = new Class[1];
			arrayOfClass[0] = Boolean.TYPE;
			Method localMethod = localClass
					.getMethod(paramString, arrayOfClass);
			Object[] arrayOfObject = new Object[1];
			arrayOfObject[0] = Boolean.valueOf(paramBoolean);
			localMethod.invoke(paramObject, arrayOfObject);
			return;
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

}
