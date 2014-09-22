package com.ak.ui;


import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore.Audio.Media;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ak.reikitimer.R;
import com.ak.remotecontroller.ITimerActionsListener;
import com.ak.service.ControllerService;

import com.inmobi.commons.InMobi;
import com.inmobi.monetization.IMBanner;
import com.inmobi.monetization.IMBannerListener;
import com.inmobi.monetization.IMErrorCode;
import com.ak.timer.TimerStateMachine.State;

public class MainActivity extends Activity implements ITimerActionsListener,OnItemSelectedListener,OnClickListener {
	private ControllerService mService;
	final static String TAG = MainActivity.class.getSimpleName();
	private static final int MEDIA_PICKER_ID = 0;
	private final float VOLUME_TOO_LOW = (float) 0.75;
	private static final long MINIMUM_TIME_PERID_FOR_TIMER = 3000;
	private final String APP_ID_INMOBI ="1cab93cea86a49618787b083bf5d91cb";
	private Intent mServiceIntent ;
	private Button mBtnStart,mBtnStop,mBtnPause,mBtnResume;
	private ImageButton mBtnDonate;
	private Spinner mSpinnerMinutes,mSpinnerSeconds;
	private TextView mTvElapsedTime,mTvMin,mTvSec,mTvRestartingLabel;
	private long mTime;
	int mMaxVolume,mCurrentVolume;
	private final String RESET_TIME = "00:00:00";
	private SharedPreferences mPreference ;
	private AlertDialog.Builder mAlertDialogBuilder;
	private AlertDialog mAlertDialog;
	private State mState;
	private AudioManager mAudioManager;
	private Toast mToast;
	private LinearLayout mBannerLayout;
	private IMBanner mBanner;
	RelativeLayout mParentLayout;
	BroadcastReceiver mReceiver ;
	public static final String INTENT_EXIT = "com.ak.exit";

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG,"onServiceDisconnected");
			mService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG,"onServiceConnected");
			mService = ((ControllerService.LocalBinder)service).getControllerService();
			mService.registerListener(MainActivity.this);
			if(mService.isTimerRunning()){
				updateButtonState(State.STARTED);
				setTimeOnSpinner(mService.getTime());
			}
		}
	};
	
	
	/**
	 * User may select to change the default tone 
	 * @param uri
	 */
	public void setMediaSource(Uri uri){
		mService.setMediaSource(uri);
	}
	
	/**
	 * Show the selected time on spinner.
	 * This is required when timer is already running and user
	 * is coming back to activity after onPause has happened 
	 * before
	 * @param milli
	 */
	private void setTimeOnSpinner(long milli){
		int sec = (int) (milli/1000);
		int min = sec / 60;
		sec = sec % 60;
		mSpinnerMinutes.setSelection(min);
		mSpinnerSeconds.setSelection(sec);
	}

	private void showToast(String message){
		if(mToast !=null)
			mToast.cancel();
		mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
		mToast.show();
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mBtnStart = (Button)findViewById(R.id.btn_start);
		mBtnStop = (Button)findViewById(R.id.btn_stop);
		mBtnPause = (Button)findViewById(R.id.btn_pause);
		mBtnResume = (Button)findViewById(R.id.btn_resume);
		mBtnDonate = (ImageButton) findViewById(R.id.btn_donate);
		mSpinnerMinutes = (Spinner)findViewById(R.id.spn_min);
		mSpinnerSeconds = (Spinner)findViewById(R.id.spn_sec);
		mTvElapsedTime = (TextView)findViewById(R.id.tv_elapsedTime);
		mTvRestartingLabel =(TextView)findViewById(R.id.tv_restartLabel);
		mTvMin = (TextView)findViewById(R.id.tv_labelMin);
		mTvSec = (TextView)findViewById(R.id.tv_labelSec);
		ArrayList<Integer>list = new ArrayList<Integer>();
		for(int i = 0 ;i<=60;i++)
			list.add(i);
		ArrayAdapter<Integer>adapter = new ArrayAdapter<Integer>(this, R.layout.spinner_item,list);
		mSpinnerMinutes.setAdapter(adapter);
		mSpinnerSeconds.setAdapter(adapter);
		//adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinnerMinutes.setOnItemSelectedListener(this);
		mSpinnerSeconds.setOnItemSelectedListener(this);
		mPreference = getSharedPreferences(ControllerService.SHARED_PREF,0);
		mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		mBannerLayout = (LinearLayout)findViewById(R.id.bannerLayout);
		mBanner = (IMBanner) findViewById(R.id.banner);
		mParentLayout = (RelativeLayout)findViewById(R.id.parentLayout);
		mReceiver = new BRReceiver();
		
		mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		mCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
	
		if(mCurrentVolume/(float)mMaxVolume < VOLUME_TOO_LOW)
			showToast("Better increase volume a little bit.");
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int action = event.getAction();
		int keyCode = event.getKeyCode();
		
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			if (action == KeyEvent.ACTION_UP) {
				if(mCurrentVolume < mMaxVolume){
					mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, ++mCurrentVolume, 0);
					showToast("Volume Incresed:"+mCurrentVolume);
				}
			}
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (action == KeyEvent.ACTION_DOWN) {
				if(mCurrentVolume > 0){
					mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, --mCurrentVolume, 0);
					showToast("Volume Decreased:"+mCurrentVolume);
				}
			}
			return true;
		default:
			return super.dispatchKeyEvent(event);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG,"onPause");
		unbindService(mServiceConnection);
		mBtnStart.setOnClickListener(null);
		mBtnResume.setOnClickListener(null);
		mBtnPause.setOnClickListener(null);
		mBtnStop.setOnClickListener(null);
		mBtnDonate.setOnClickListener(null);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG,"onDestroy..");
		unregisterReceiver(mReceiver);
		if(!isTimerRunning()){
			stopService(mServiceIntent);
		}
	}

	private void toggleDonateButtonVisibility(){
		if(isNetworkConnected()){
			mBtnDonate.setVisibility(View.VISIBLE);
		}else{
			mBtnDonate.setVisibility(View.INVISIBLE);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mServiceIntent = new Intent(MainActivity.this,ControllerService.class);
		Log.d(TAG,"onResume:"+mServiceIntent);
		startService(mServiceIntent);
		bindService(mServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
		mBtnStart.setOnClickListener(this);
		mBtnResume.setOnClickListener(this);
		mBtnPause.setOnClickListener(this);
		mBtnStop.setOnClickListener(this);
		mBtnDonate.setOnClickListener(this);
		//show donate button only if internet is connected
		toggleDonateButtonVisibility();
		//register receiver to listen for exit command from notification or widget
		IntentFilter intentFilter = new IntentFilter(INTENT_EXIT);
		registerReceiver(mReceiver, intentFilter);
		//register for getting connectivity events
		intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(mReceiver, intentFilter);
		mBanner.setIMBannerListener(new IMBannerListener() {
			@Override
			public void onShowBannerScreen(IMBanner arg0) {
				Log.d(TAG,"banner onShowBannerScreen");
			}
			@Override
			public void onLeaveApplication(IMBanner arg0) {
				Log.d(TAG,"banner onLeaveApplication");
			}
			@Override
			public void onDismissBannerScreen(IMBanner arg0) {
				Log.d(TAG,"banner onDismissBannerScreen");
			}
			@Override
			public void onBannerRequestFailed(IMBanner banner, IMErrorCode errorCode) {
				Log.d(TAG,"banner onBannerRequestFailed");
				/*FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
				layoutParams.setMargins(0, 0, 0, 80);
				
				mParentLayout.setLayoutParams(layoutParams);
				mBannerLayout.setVisibility(View.VISIBLE);	*/		
			}
			@Override
			public void onBannerRequestSucceeded(IMBanner arg0) {
				Log.d(TAG,"banner onBannerRequestSucceeded");
				FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
				layoutParams.setMargins(0, 0, 0, 80);
				mParentLayout.setLayoutParams(layoutParams);
				mBannerLayout.setVisibility(View.VISIBLE);
			}

			@Override
			public void onBannerInteraction(IMBanner arg0, Map<String, String> arg1) {
				Log.d(TAG,"banner onBannerInteraction");
			}
		});
		InMobi.initialize(this, APP_ID_INMOBI);
		mBanner.loadBanner();
		//InMobi.setLogLevel(LOG_LEVEL.NONE);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG,"onStop");
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	

	/**
	 * User can select any music file with the help of Default 
	 * music player on the device
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == MEDIA_PICKER_ID && resultCode == RESULT_OK){
			//updateMediaSource(data.getData().toString());
			updateMediaSource2(data.getData().toString());
			/*Toast.makeText(this, "Media updated", Toast.LENGTH_SHORT).show();
			Editor edit = mPreference.edit();
			edit.putString(ControllerService.MEDIA_URI_KEY, data.getData().toString());
			edit.commit();*/
			
		}
	}
	
	private void updateMediaSource(String data){
		Editor edit = mPreference.edit();
		if(data == null){
			edit.remove(ControllerService.MEDIA_URI_KEY);
			showToast("Default media selected");
		}else{
			if(isAudioFile(data)){
				edit.putString(ControllerService.MEDIA_URI_KEY, data);
				showToast("Media file updated");
			}
		}
		edit.commit();
		boolean isSetSuccess = mService.setMediaSourceChanged();
		if(!isSetSuccess){
			Toast.makeText(this, "Media format not supported", Toast.LENGTH_SHORT).show();
			
		}
	}
	
	private void updateMediaSource2(String data){
		Editor edit = mPreference.edit();
		if(TextUtils.isEmpty(data)){
			edit.remove(ControllerService.MEDIA_URI_KEY);
			showToast("Default media selected");
			edit.commit();
			boolean isSetSuccess = mService.setMediaSourceChanged();
		}else if(isAudioFile(data)){
			edit.putString(ControllerService.MEDIA_URI_KEY, data);
			showToast("Media file updated");
			edit.commit();
			boolean isSetSuccess = mService.setMediaSourceChanged();
		}else{
			showToast("Not supported format!");
			return;
		}
	}
	
	/**
	 * Checks if the file picked by user is 
	 * media file or not.
	 * @param data
	 * @return
	 */
	private boolean isAudioFile(String data){
		if(!TextUtils.isEmpty(data)){
			MediaPlayer player = MediaPlayer.create(this, Uri.parse(data));
			if(player != null){
				player.release();
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		
		/*
		 * Rest period is the halt between two consecutive 
		 * timer run
		 */
		case R.id.change_rest_period:
			showRestTimerDialog();
			break;
			
		/*
		 * To change default sound to be played after 
		 * time has finished. Start music player 
		 * to select media file
		 */
		case R.id.change_sound:
			Intent intent = new Intent();
		    intent.setType("audio/*");
		    intent.setAction(Intent.ACTION_GET_CONTENT);
		    intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
		    startActivityForResult(Intent.createChooser(intent,
		            "Complete action using"), MEDIA_PICKER_ID);
			break;
			
			/*
			 * User might want to reset to default bell sound instead
			 * of his selected music file
			 */
		case R.id.reset_media:
			updateMediaSource2(null);
			//updateMediaSource(null);
			/*Editor edit = mPreference.edit();
			edit.remove(ControllerService.MEDIA_URI_KEY);
			edit.commit();
			Toast.makeText(this, "Default Media selected", Toast.LENGTH_SHORT).show();
			mService.setMediaSourceChanged();*/
			//setMediaSource(getMediaUri());
			break;
			
		case R.id.exit:
//			mService.stopTimer();
//			mService.stopSelf();
//			finish();
			exit();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void exit(){
		Log.d(TAG,"exit called in activity");
		mService.stopTimer();
		mService.stopSelf();
		finish();
	}
	/**
	 * Show alert dialog to user to edit 
	 * rest timer value
	 */
	private void showRestTimerDialog() {
		mAlertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
		final EditText etRestTimer = new EditText(MainActivity.this);
		etRestTimer.setInputType(InputType.TYPE_CLASS_NUMBER);
		etRestTimer.setText(mPreference.getInt(ControllerService.REST_PERIOD_KEY, 
				ControllerService.DEFUALT_REST_TIMER_PERIOD)+"");
		mAlertDialogBuilder.setView(etRestTimer);
		
		mAlertDialogBuilder.setCancelable(true)
		.setTitle("Edit Rest Timer Period in sec")
		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				Editor edit = mPreference.edit();
				if(TextUtils.isEmpty(etRestTimer.getText().toString()))
					return;
				int restPeriod = Integer.parseInt(etRestTimer.getText().toString());
				Log.d(TAG,"rest period = "+restPeriod);
				edit.putInt(ControllerService.REST_PERIOD_KEY, restPeriod);
				edit.commit();
				mService.setRestTimer();
			}
		});
		mAlertDialog = mAlertDialogBuilder.create();
		mAlertDialog.show();
	}

	/**
	 * Every sec call back received from Timer
	 */
	@Override
	public void onTimeTick(long milliSecUntilFinished) {
		String time = convertTime(milliSecUntilFinished);
		mTvElapsedTime.setText(time);
	}

	/**
	 * Convert long time to String format
	 * @param millis
	 * @return
	 */
	private String convertTime(long millis){
		return String.format(Locale.US,"%02d:%02d:%02d", 
				TimeUnit.MILLISECONDS.toHours(millis),
				TimeUnit.MILLISECONDS.toMinutes(millis) -  
				TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
				TimeUnit.MILLISECONDS.toSeconds(millis) - 
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
	}
	@Override
	public void onTimerFinish() {
		Log.d(TAG,"onTimerFinished()");
	}

	/**
	 * Callback called when state timer 
	 * state changes 
	 */
	@Override
	public void onStateChange(State newState) {
		updateButtonState(newState);
	}

	/**
	 * Incase timer is not running , 
	 * better stop the service
	 * @return
	 */
	boolean isTimerRunning() {
		if (mState != null) {
			Log.d(TAG, "isTimerRunning:" + mState.toString());
			switch (mState) {
			case STOPPED:
			case INITED:
				return false;
			default:
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Every state requires to show different buttons 
	 * on the screen
	 * @param state
	 */
	private void updateButtonState(State state){
		mTvRestartingLabel.setVisibility(View.INVISIBLE);
		mState = state;
		switch(state){

		case INITED:
		case STOPPED:
			mSpinnerMinutes.setEnabled(true);
			mSpinnerSeconds.setEnabled(true);
			mBtnStart.setVisibility(View.VISIBLE);
			mBtnStop.setVisibility(View.VISIBLE);
			mBtnPause.setVisibility(View.INVISIBLE);
			mBtnResume.setVisibility(View.INVISIBLE);
			mTvElapsedTime.setText(RESET_TIME);
			mTvSec.setEnabled(true);
			mTvMin.setEnabled(true);
			break;
		case PLAYING_MEDIA:
			mBtnStart.setVisibility(View.VISIBLE);
			mBtnStop.setVisibility(View.VISIBLE);
			mBtnPause.setVisibility(View.INVISIBLE);
			mBtnResume.setVisibility(View.INVISIBLE);
			mTvElapsedTime.setText(RESET_TIME);
			break;

		case STARTED:
			mBtnStart.setVisibility(View.INVISIBLE);
			mBtnStop.setVisibility(View.VISIBLE);
			mBtnPause.setVisibility(View.VISIBLE);
			mBtnResume.setVisibility(View.INVISIBLE);
			mSpinnerMinutes.setEnabled(false);
			mSpinnerSeconds.setEnabled(false);
			mTvSec.setEnabled(false);
			mTvMin.setEnabled(false);
			//setMediaSource(getMediaUri());
			break;

		case PAUSED:
			mBtnStart.setVisibility(View.INVISIBLE);
			mBtnStop.setVisibility(View.VISIBLE);
			mBtnPause.setVisibility(View.INVISIBLE);
			mBtnResume.setVisibility(View.VISIBLE);
			break;

		case REST_PERIOD_STATE:
			mTvRestartingLabel.setVisibility(View.VISIBLE);
			//setMediaSource(DEGAULT_REST_TIMER_SOUND);
			break;
		}
	}
	
	/**
	 * When spinner item is selected, this callback is called
	 */
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		Log.d(TAG,"onItemSelected");

		int min = 0,sec=0;
		min = (Integer) mSpinnerMinutes.getSelectedItem();
		sec = (Integer) mSpinnerSeconds.getSelectedItem();
		mTime = (min * 60 * 1000) + (sec*1000);
	}

	/**
	 * Validates selected time on the spinner.
	 * Should be at least greater than 
	 * MINIMUM_TIME_PERID_FOR_TIMER 
	 * @param milli
	 * @return
	 */
	private boolean isValidTime(long milli){
		if(milli < MINIMUM_TIME_PERID_FOR_TIMER){
			Toast.makeText(this, "Too small duration", Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}
	@Override
	public void onClick(View v) {
		switch(v.getId()){
		
		case R.id.btn_start:
			if(isValidTime(mTime))
				mService.startTimer(mTime);
			break;
			
		case R.id.btn_stop:
			mService.stopTimer();
			break;
			
		case R.id.btn_pause:
			mService.pauseTimer();
			break;
			
		case R.id.btn_resume:
			mService.resumeTimer();
			break;
			
		case R.id.btn_donate:
			String url = getResources().getString(R.string.donate_url);
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(url));
			startActivity(i);
			break;
		}
	}

	private boolean isNetworkConnected(){
		ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork != null &&
				activeNetwork.isConnectedOrConnecting();
		return isConnected;
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}

	/*
	 * Broadcast receiver listening for exit commands from notification or 
	 * widget
	 */
	class BRReceiver extends BroadcastReceiver{

		private final String TAG = BRReceiver.class.getSimpleName();
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "onReceive in activity:"+intent.getAction());
			if(intent.getAction().equals(INTENT_EXIT)){
				exit();
			}else if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)){
				toggleDonateButtonVisibility();
			}
		}
	}
}
