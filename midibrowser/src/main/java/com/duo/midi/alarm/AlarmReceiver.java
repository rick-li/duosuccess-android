/***
  Copyright (c) 2011 CommonsWare, LLC
  
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.duo.midi.alarm.WakefulIntentService.AlarmListener;
import com.duo.midi.music.MusicRepeatListener;

public class AlarmReceiver extends BroadcastReceiver {
	private static final String TAG = "AlarmReceiver";

	@Override
	public void onReceive(Context ctxt, Intent intent) {
		Log.i(TAG, "AlarmReceiver onReceive.");
		AlarmListener listener = getListener(ctxt);

		if (listener != null) {
			listener.sendWakefulWork(ctxt);
		}
	}

	static MusicRepeatListener musicRepeatListener = new MusicRepeatListener();

	private WakefulIntentService.AlarmListener getListener(Context ctxt) {
		return musicRepeatListener;
	}
}