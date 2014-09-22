package com.ak.buzzer;

import android.net.Uri;

public interface IBuzzer {
	public void playSound();
	public void stopSound();
	public void onPlaybackComplete();
	public boolean isPlaybackRunning();
	public boolean setMediaSource(Uri resId);
	public void registerListener(IPlaybackListener listener);
	public void unregisterListener(IPlaybackListener listener);
}
