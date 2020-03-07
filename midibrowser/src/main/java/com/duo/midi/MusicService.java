package com.duo.midi;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.duosuccess.midi.R;

import static android.support.v4.app.NotificationCompat.PRIORITY_LOW;

public class MusicService extends Service {
	public MusicService() {
	}

	private static final String TAG = "MusicService";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private MediaPlayer mp;

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		AVOSLogger.info("MusicService onDestory");
		stopMusic();
		stopForeground(true);
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	Timer timer = new Timer();
	WifiLock wifiLock = null;
	private static final int NOTIFY_PLAYER_ID = 1339;

	public Notification getNotification() {
		String channel;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			channel = createChannel();
		else {
			channel = "";
		}
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, channel)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("多成中醫");
		Notification notification = mBuilder
				.setPriority(PRIORITY_LOW)
				.setCategory(Notification.CATEGORY_SERVICE)
				.build();


		return notification;
	}

	@NonNull
	@TargetApi(26)
	private synchronized String createChannel() {
		NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

		String name = "多成中醫";
		int importance = NotificationManager.IMPORTANCE_LOW;

		NotificationChannel mChannel = new NotificationChannel("com.duosuccess", name, importance);

		mChannel.enableLights(true);
		mChannel.setName(name);
		mChannel.setLightColor(Color.BLUE);
		if (mNotificationManager != null) {
			mNotificationManager.createNotificationChannel(mChannel);
		} else {
			stopSelf();
		}
		return "com.duosuccess";
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// Notification myNotify = new Notification.Builder(this)
		// .setSmallIcon(R.drawable.ic_launcher).setContentTitle("多成中醫")
		// .setContentText("正在播放").build();
		Notification notification = getNotification();
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//			startMyOwnForeground();
//		else
//			startForeground(NOTIFY_PLAYER_ID, notification);
		this.startForeground(NOTIFY_PLAYER_ID, notification);
		Log.d(TAG, "onHandleIntent");
		if (intent == null) {
			Log.e(TAG, "intent is null, exiting.");
			return Service.START_STICKY_COMPATIBILITY;
		}
		String midiName = intent.getStringExtra("midiFile");
		FileInputStream fis = null;
		if(!midiName.endsWith(".mp3")){
			try {
				fis = this.openFileInput(midiName);
			} catch (Exception e1) {
				e1.printStackTrace();
				stopSelf();
				Toast.makeText(this, "音乐文件未找到。", Toast.LENGTH_LONG).show();
				AVOSLogger.error("File not found." + e1.getMessage());
			}
		}

		AVOSLogger.info("Start MP");
		mp = new MediaPlayer();
		mp.reset();
		try {
			mp.setOnErrorListener(new OnErrorListener() {

				@Override
				public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
					AVOSLogger.error("MP Error " + arg1 + " " + arg2);
					return true;
				}
			});
			if(midiName.endsWith(".mp3")){
				mp.setDataSource(midiName);
			}else{
				mp.setDataSource(fis.getFD());
			}

			mp.setOnPreparedListener(new OnPreparedListener() {

				@Override
				public void onPrepared(MediaPlayer lmp) {
					mp.setLooping(true);
					mp.setWakeMode(MusicService.this.getApplicationContext(),
							PowerManager.PARTIAL_WAKE_LOCK);

					mp.start();
					final long startAccr = System.currentTimeMillis();
					timer.scheduleAtFixedRate(new TimerTask() {

						@Override
						public void run() {
							final long nowAccr = System.currentTimeMillis();
							// Log.d(TAG, "(nowAccr - startAccr) % 60000"
							// + (nowAccr - startAccr) % 60000);
							if ((nowAccr - startAccr) % 60000 == 0) {
								Log.d(TAG, "Updating notification ");
								Notification notification = createNotification();
								NotificationManager notifManager = (NotificationManager) App
										.getInstance().getSystemService(
												Context.NOTIFICATION_SERVICE);
								notifManager.notify(NOTIFY_PLAYER_ID,
										notification);
							}
							// Log.v(TAG, "Current time is " + nowAccr);
							if ((nowAccr - startAccr) >= MusicFragment.musicDuration) { // stop
								stopMusic();
							}
						}

					}, 0, 10000);
				}
			});
			mp.prepareAsync();

		} catch (IOException e) {
			AVOSLogger.error("Music play Error." + e.getMessage());
			Log.e(TAG, "Error playing music", e);
			stopSelf();
			Toast.makeText(this, "无法播放", 15 * 1000).show();
		}
		return START_STICKY;
	}

	private Notification createNotification() {
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this);
		mBuilder.setSmallIcon(R.drawable.ic_launcher);
		mBuilder.setContentTitle("多成中醫");
		// mBuilder.setTicker(getResources().getString(
		// R.string.building_music_library));
		mBuilder.setContentText("正在播放");
		mBuilder.setOngoing(true);
		Notification notification = mBuilder.getNotification();
		notification.flags = Notification.FLAG_FOREGROUND_SERVICE
				| Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		return notification;
	}

	private void stopMusic() {
		if (mp != null && mp.isPlaying()) {
			AVOSLogger.info("Stop Music normally.");
			Log.i(TAG, "Stop the player inside the music service.");
			mp.stop();
			timer.cancel();
			// wifiLock.release();
		}
		stopSelf();
	}

}
