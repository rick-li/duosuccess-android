package com.duo.midi;
public class TimeCounter {
	private int startMillSec = 0;

	public void increaseOneSec() {
		startMillSec += 1000;
	}

	public int getStartMillSec() {
		//Log.d(TAG, "StartMillSec: " + startMillSec);
		return startMillSec;
	}
}