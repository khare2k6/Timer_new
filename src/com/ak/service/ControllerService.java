package com.ak.service;


import java.io.File;
import java.net.URI;

import org.w3c.dom.Text;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.ak.reikitimer.R;
import com.ak.remotecontroller.IController;
import com.ak.remotecontroller.ITimerActionsListener;
import com.ak.remotecontroller.RemoteController;
import com.ak.timer.TimerStateMachine.State;
import com.ak.ui.MainActivity;


public class ControllerService extends Service implements ITimerActionsListener{
	private IController mController;
	private final String TAG = ControllerService.class.getSimpleName();
	private final int ID = 1;
	private BroadcastReceiver mExitReceiver;
	Notification foregroundNotification;
	private Uri DEGAULT_BELL_SOUND = Uri.parse("android.resource://com.ak.reikitimer/" + R.raw.bell);
	private Uri DEGAULT_REST_TIMER_SOUND = Uri.parse("android.resource://com.ak.reikitimer/" + R.raw.startsound);
	private boolean mPlayRestMedia = false;
	private SharedPreferences mPreference;
	public static final String SHARED_PREF = "timerAppPref";
	public static final String MEDIA_URI_KEY = "getMediaUri";
	public static final String REST_PERIOD_KEY = "restTime";
	public static final int DEFUALT_REST_TIMER_PERIOD = 3;
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG,"onCreate");
		mController = new RemoteController(this);	
		foregroundNotification = getForegroundNotification();//new Notification.Builder(ControllerService.this)
//		.setContentTitle("Timer Running")
//		.setContentText("Repeatative timer is running")
//		.setSmallIcon(R.drawable.ic_clock2)
//		.setContentIntent(PendingIntent.getActivity(ControllerService.this, ID, 
//				new Intent(ControllerService.this,MainActivity.class), Intent.FLAG_ACTIVITY_NEW_TASK))
//		.build();
		startForeground(ID,foregroundNotification);
		mController.registerListener(this);
		mPreference = getSharedPreferences(SHARED_PREF, 0);
		//Initial setting media URI
		mController.setMediaSource(getMediaUri());
		mController.setRestTimer(mPreference.getInt(REST_PERIOD_KEY ,DEFUALT_REST_TIMER_PERIOD)*1000);
		mExitReceiver = new ExitReceiver();
		IntentFilter intentFilter = new IntentFilter(MainActivity.INTENT_EXIT);
		registerReceiver(mExitReceiver, intentFilter);
	}


	private Notification getForegroundNotification(){
		Intent intent = new Intent(MainActivity.INTENT_EXIT);
		PendingIntent pIntent = PendingIntent.getBroadcast(ControllerService.this, 0, intent, 0);
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(ControllerService.this)
		.setContentTitle("Timer Running")
		.setContentText("Repeatative timer is running")
		.setSmallIcon(R.drawable.ic_clock2)
		.setContentIntent(PendingIntent.getActivity(ControllerService.this, ID, 
				new Intent(ControllerService.this,MainActivity.class), Intent.FLAG_ACTIVITY_NEW_TASK))
		.addAction(android.R.drawable.button_onoff_indicator_off, "Exit", pIntent);
		return notificationBuilder.build();
	}
	@Override
	public IBinder onBind(Intent arg0) {
		Log.d(TAG,"onBind");
		return new LocalBinder();
	}

	public void setMediaSource(Uri resId){
		mController.setMediaSource(resId);
	}
	
	public boolean isTimerRunning(){
		return mController.isTimerRunning();
	}
	
	public boolean isRestTimerRunning(){
		return mController.isRestTimerRunning();
	}
	public long getTime(){
		return mController.getTime();
	}
	
	public void setRestTimer(){
		mController.setRestTimer(mPreference.getInt(REST_PERIOD_KEY ,DEFUALT_REST_TIMER_PERIOD)*1000);
		
	}
	public void startTimer(long milli){
		mController.startTimer(milli);
	}
	public void stopTimer(){
		mController.stopTimer();
	}
	
	public void pauseTimer(){
		mController.pauseTimer();
	}
	
	public void resumeTimer(){
		mController.resumeTimer();
	}
	
	public void registerListener(ITimerActionsListener listener){
		mController.registerListener(listener);
	}
	public boolean setMediaSourceChanged(){
		return mController.setMediaSource(getMediaUri());
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG,"onDestroy");
		stopForeground(true);
		unregisterReceiver(mExitReceiver);
	}

	public class LocalBinder extends Binder{
		public ControllerService getControllerService(){
			return ControllerService.this;
		}
	}

	@Override
	public void onTimeTick(long milliSecUntilFinished) {
		//do nothing
	}


	@Override
	public void onTimerFinish() {		
		//do nothing
	}
	/**
	 * Get URI of the media to be played as alarm
	 * on timer expiry
	 * @return
	 */
	private Uri getMediaUri(){
		if(mPlayRestMedia){
			return DEGAULT_REST_TIMER_SOUND;
		}
		String uri = mPreference.getString(MEDIA_URI_KEY, null);
		if(!TextUtils.isEmpty(uri) && isFileExisting(uri)){
			return Uri.parse(uri);
		}
		return DEGAULT_BELL_SOUND;
	}

	private boolean isFileExisting(String uri){
		String actualPath = URI.create(uri).getPath();
		File file = new File(actualPath);
		Log.d(TAG,"getExtenalStorage:"+Environment.getExternalStorageDirectory());
		Log.d(TAG,"isFileExisting:"+file.exists()+" file path:"+actualPath);
		return file.exists();
	}
	@Override
	public void onStateChange(State newState) {
		Log.d(TAG,"newState = "+newState.toString());
		switch(newState){
		case REST_PERIOD_STATE:
			Log.d(TAG,"setting to rest media");
			mPlayRestMedia = true;
			mController.setMediaSource(getMediaUri());
			break;
		case STARTED:
			if(mPlayRestMedia){
				mPlayRestMedia = false;
				Log.d(TAG,"resetting to bell sound");
				mController.setMediaSource(getMediaUri());
			}
			break;
		default:
			break;
		}
	}
	
	/*
	 * Broadcast receiver listening for exit commands from notification or 
	 * widget
	 */
	class ExitReceiver extends BroadcastReceiver{

		private final String TAG = ExitReceiver.class.getSimpleName();
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "onReceive in service:"+intent.getAction());
			if(intent.getAction().equals(MainActivity.INTENT_EXIT)){
				stopTimer();
				stopSelf();
			}
		}
	}
}
