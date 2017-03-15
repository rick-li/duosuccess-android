/***
  Copyright (c) 2009-11 CommonsWare, LLC
  
  Licensed under the Apache License, Version 2.0 (the "License"); you may
  not use this file except in compliance with the License. You may obtain
  a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.duo.midi.alarm;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.PowerManager;

abstract public class WakefulIntentService extends IntentService {
	abstract protected void doWakefulWork(Context context, Intent intent);

	static final String NAME = "com.commonsware.cwac.wakeful.WakefulIntentService";
	static final String LAST_ALARM = "lastAlarm";
	private static volatile PowerManager.WakeLock lockStatic = null;
	private static volatile WifiManager.WifiLock wifiLockStatic = null;
	private static Context context;

	synchronized private static PowerManager.WakeLock getLock(Context context) {
		if (lockStatic == null) {
			PowerManager mgr = (PowerManager) context
					.getSystemService(Context.POWER_SERVICE);

			lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, NAME);

			lockStatic.setReferenceCounted(true);
		}

		return (lockStatic);
	}

	synchronized private static WifiManager.WifiLock getWifiLock(Context context) {
		if (wifiLockStatic == null) {
			WifiManager wifiMgr = (WifiManager) context
					.getSystemService(Context.WIFI_SERVICE);
			wifiLockStatic = wifiMgr.createWifiLock("duosuccess wifi lock.");
			wifiLockStatic.setReferenceCounted(true);
		}
		return wifiLockStatic;
	}

	public static void sendWakefulWork(Context ctxt, Intent i) {
		getLock(ctxt.getApplicationContext()).acquire();
		getWifiLock(ctxt.getApplicationContext()).acquire();
		ctxt.startService(i);
	}

	public static void sendWakefulWork(Context ctxt, Class<?> clsService) {
		sendWakefulWork(ctxt, new Intent(ctxt, clsService));
	}

	public static void scheduleAlarms(AlarmListener listener, Context ctxt) {
		scheduleAlarms(listener, ctxt, true);
	}

	public static void scheduleAlarms(AlarmListener listener, Context ctxt,
			boolean force) {
		context = ctxt;

		if (force) {
			AlarmManager mgr = (AlarmManager) ctxt
					.getSystemService(Context.ALARM_SERVICE);
			Intent i = new Intent(ctxt, AlarmReceiver.class);
			PendingIntent pi = PendingIntent.getBroadcast(ctxt, 0, i, 0);

			listener.scheduleAlarms(mgr, pi, ctxt);
		}
	}

	public static void cancelAlarms(Context ctxt) {
		AlarmManager mgr = (AlarmManager) ctxt
				.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(ctxt, AlarmReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(ctxt, 0, i, 0);

		mgr.cancel(pi);
	}

	public WakefulIntentService(String name) {
		super(name);
		setIntentRedelivery(true);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		PowerManager.WakeLock lock = getLock(this.getApplicationContext());
		WifiManager.WifiLock wifiLock = getWifiLock(this
				.getApplicationContext());

		if (!lock.isHeld() || (flags & START_FLAG_REDELIVERY) != 0) {
			lock.acquire();
		}

		if (!wifiLock.isHeld() || (flags & START_FLAG_REDELIVERY) != 0) {
			wifiLock.acquire();
		}
		super.onStartCommand(intent, flags, startId);

		return (START_REDELIVER_INTENT);
	}

	@Override
	final protected void onHandleIntent(Intent intent) {
		try {
			doWakefulWork(context, intent);
		} finally {
			PowerManager.WakeLock lock = getLock(this.getApplicationContext());
			WifiManager.WifiLock wifiLock = getWifiLock(this
					.getApplicationContext());
			if (lock.isHeld()) {
				lock.release();
			}
			if (wifiLock.isHeld()) {
				wifiLock.release();
			}
		}
	}

	public interface AlarmListener {
		void scheduleAlarms(AlarmManager mgr, PendingIntent pi, Context ctxt);

		void sendWakefulWork(Context ctxt);

		long getMaxAge();
	}
}