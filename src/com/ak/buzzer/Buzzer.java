package com.ak.buzzer;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.util.Log;

public class Buzzer implements IBuzzer,OnCompletionListener{

	private List<IPlaybackListener> mListener;
	private MediaPlayer mPlayer;
	private AudioManager mAudioManager;
	private Context mContext;
	private OnAudioFocusChangeListener mAfChangeListener;
	private String TAG = Buzzer.class.getSimpleName();

	public Buzzer(Context context){
		mListener = new ArrayList<IPlaybackListener>();
		mContext = context;
	}
	
	@Override
	public void playSound() {
		if (mPlayer != null) {
			int result = mAudioManager.requestAudioFocus(mAfChangeListener,
					AudioManager.STREAM_MUSIC|AudioManager.STREAM_RING|AudioManager.STREAM_VOICE_CALL,
					AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
			if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
				mPlayer.start();
			} else {
				Log.d(TAG, "result = " + result + " didnt got audio focus!");
				onPlaybackComplete();
			}
		}
	}

	@Override
	public void onPlaybackComplete() {
		mAudioManager.abandonAudioFocus(mAfChangeListener);
		for(IPlaybackListener listener:mListener)
			listener.onPlaybackFinish();
	}

	private void setupAudioFocusListener(){
		mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		mAfChangeListener = new OnAudioFocusChangeListener() {
			
			@Override
			public void onAudioFocusChange(int focusChange) {
				
				switch(focusChange){
				case AudioManager.AUDIOFOCUS_LOSS:
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
					Log.d(TAG ,"result = "+focusChange+" AUDIOFOCUS_LOSS permanent!");
					if(isPlaybackRunning()){
						stopSound();
						onPlaybackComplete();
					}
					break;
				}
			}
		};
	}
	
	@Override
	public boolean isPlaybackRunning() {
		return mPlayer.isPlaying();
	}

	@Override
	public boolean setMediaSource(Uri resId) {
		if(mPlayer != null)
			mPlayer.release();
		boolean isSetSourceSuccess = false;
		Log.d(TAG,"setMediaSource:"+resId);
		mPlayer = MediaPlayer.create(mContext, resId);
		Log.d(TAG,"setMediaSource mPlayer:"+mPlayer);
		if(mPlayer == null){
			Log.d(TAG,"Media player creation failed");
			return isSetSourceSuccess;
		}
		isSetSourceSuccess = true;
		mPlayer.setOnCompletionListener(this);
		setupAudioFocusListener();
		return isSetSourceSuccess;
	}
	
	@Override
	public void registerListener(IPlaybackListener listener) {
		mListener.add(listener);
	}

	@Override
	public void unregisterListener(IPlaybackListener listener) {
		mListener.remove(listener);
	}

	@Override
	public void stopSound() {
		if(mPlayer == null) return;
		mPlayer.pause();
		mPlayer.seekTo(0);
		mAudioManager.abandonAudioFocus(mAfChangeListener);
	}

	@Override
	public void onCompletion(MediaPlayer arg0) {
		onPlaybackComplete();
	}
}
