//package com.ak.timer;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import android.content.Context;
//import android.net.Uri;
//import android.os.CountDownTimer;
//import android.util.Log;
//
//import com.ak.buzzer.Buzzer;
//import com.ak.buzzer.IBuzzer;
//import com.ak.buzzer.IPlaybackListener;
//import com.ak.remotecontroller.ITimerActionsListener;
//
//
//public class CDTimer implements ITimer,IPlaybackListener{
//	private IState mCurrentState;
//	private long mTime,mLeftTime,mRestPeriodTime ;
//	private final String TAG = CDTimer.class.getSimpleName();
//	private CountDownTimer mTimer,mRestPeriodTimer;
//	private boolean mIsRestPeriodTimerRunning = false;
//	private List<ITimerActionsListener> mListener;
//	private IBuzzer mBuzzer;
//	private State mEnumState;
//	private Context mContext;
//	private interface IState{
//		public void onEntry();
//		public void startTimer(Long time);
//		public void stopTimer();
//		public void pauseTimer();
//		public void resumeTimer();
//		public void onTimerFinish();
//		public void onMediaPlaybackFinish();
//	}
//
//	public enum State{
//		INITED,
//		STARTED,
//		STOPPED,
//		PAUSED,
//		PLAYING_MEDIA,
//		REST_PERIOD_STATE;
//	}
//	//Constructor
//	public CDTimer(Context context){
//		mContext = context;
//		mListener = new ArrayList<ITimerActionsListener>();
//		mCurrentState = new InitedState();
//		mBuzzer = new Buzzer(mContext);
//		mBuzzer.registerListener(this);
//	}
//
//
//	@Override
//	public void startTimer(long time) {
//		mCurrentState.startTimer(time);
//	}
//
//	@Override
//	public void stopTimer() {
//		mCurrentState.stopTimer();
//	}
//
//	@Override
//	public void pauseTimer() {
//		mCurrentState.pauseTimer();
//	}
//
//	@Override
//	public void resumeTimer() {
//		mCurrentState.resumeTimer();
//	}
//
//	private void changeState(IState newState){
//		Log.d(TAG,"changed state from:"+mCurrentState.toString()+" to->:"+newState.toString());
//		mCurrentState = newState;
//		Log.d(TAG,"to:"+mCurrentState.toString());
//		mCurrentState.onEntry();
//		for(ITimerActionsListener listener : mListener)
//			listener.onStateChange(mEnumState);
//	}
//
//	/**
//	 * Component which wants the onTick and 
//	 * timer finish callback
//	 * @param listener
//	 */
//	public void registerListener(ITimerActionsListener listener){
//		mListener.add(listener);
//	}
//
//	//TODO Long really needed? long will do
//	//TODO let this check in be UI.
//	private boolean isValidTime(Long time){
//		if(time != null && time != 0)
//			return true;
//		Log.d(TAG,"Time is not valid!:"+time);
//		return false;
//	}
//
//	private boolean setTime(Long time){
//		if(isValidTime(time)){
//			mTime = mLeftTime =time;
//			return true;
//		}
//		return false;
//	}
//
//
//	private class BaseState implements IState{
//
//		@Override
//		public void onEntry() {
//		}
//
//		@Override
//		public void startTimer(Long time) {
//			Log.d(TAG,"startTimer called in "+mCurrentState.toString()+" .No point!");
//		}
//
//		@Override
//		public void stopTimer() {
//			Log.d(TAG,"stopTimer called in "+mCurrentState.toString()+" .No point!");
//		}
//
//		@Override
//		public void pauseTimer() {
//			Log.d(TAG,"pauseTimer called in "+mCurrentState.toString()+" .No point!");		
//		}
//
//		@Override
//		public void resumeTimer() {
//			Log.d(TAG,"Resume Timer called in "+mCurrentState.toString()+" .No point!");
//		}
//
//		@Override
//		public void onTimerFinish() {
//			Log.d(TAG,"onTimerFinish called in "+mCurrentState.toString()+" .No point!");
//		}
//		
//		@Override
//		public void onMediaPlaybackFinish() {
//			Log.d(TAG,"onMediaPlaybackFinish called in "+mCurrentState.toString()+" .No point!");
//		}
//		
//		@Override
//		public String toString(){
//			return mCurrentState.getClass().getSimpleName();
//		}
//	}
//
//	/*
//	 * Inited State, mTime = valid time here.
//	 * Can come here from NONE state or after 
//	 * time has been explicitly stopped
//	 */
//	private class InitedState extends BaseState{
//
//		@Override
//		public void onEntry() {
//			mEnumState = State.INITED;
//		}
//
//		@Override
//		public void startTimer(Long time) {
//			Log.d(TAG,"startTimer called in "+mCurrentState.toString()+" with time:"+time/1000+" sec");
//			if(setTime(time)){
//				changeState(new StartedState());
//				return;
//			}
//			Log.d(TAG,"startTime failed!");
//		}
//	}
//	
//	private class RestPeriodState extends BaseState{
//
//		@Override
//		public void onEntry() {
//			mEnumState = State.REST_PERIOD_STATE;
//			mRestPeriodTimer = new MyCountDownTimer(mRestPeriodTime,1000,CDTimer.this);
//			mIsRestPeriodTimerRunning = true;
//			mRestPeriodTimer.start();
//		}
//
//		@Override
//		public void stopTimer() {
//			changeState(new StoppedState());
//		}
//
//		@Override
//		public void onTimerFinish() {
//			mRestPeriodTimer.cancel();
//			mLeftTime = mTime;
//			changeState(new PlayMediaState());
//		}
//
//		@Override
//		public void onMediaPlaybackFinish() {
//			changeState(new StartedState());
//		}
//		
//	}
//
//	/*
//	 * Started state:
//	 * -Timer is started
//	 * -Handling of Pausing the timer,stopping the timer
//	 * -Handling of Timer restart after timer has finished itself
//	 */
//	private class StartedState extends BaseState{
//
//		@Override
//		public void onEntry() {
//			Log.d(TAG,"starting CountDownTimer for "+mLeftTime/1000+" sec");
//			mEnumState = State.STARTED;
//			mTimer = new MyCountDownTimer(mLeftTime, 1000, CDTimer.this);
//			mTimer.start();
//		}
//
//		@Override
//		public void stopTimer() {
//			Log.d(TAG,"Stopping timer");
//			changeState(new StoppedState());
//		}
//
//		@Override
//		public void pauseTimer() {
//			Log.d(TAG,"Timer paused, left time:"+mLeftTime);
//			changeState(new PausedState());
//		}
//
//		@Override
//		public void onTimerFinish(){
//			mTimer.cancel();
//			mLeftTime = mTime;
//			changeState(new PlayMediaState());
//		}
//
//	}
//
//	/*
//	 * Stopped State
//	 * -Only timer can again be started here
//	 */
//	private class StoppedState extends BaseState{
//
//		@Override
//		public void onEntry() {
//			mEnumState = State.STOPPED;
//			mTimer.cancel();
//			mLeftTime = mTime;
//			changeState(new InitedState());
//		}
//	}
//
//	/*
//	 * Paused State
//	 * -Time can only be resumed here
//	 * -Timer can be stopped here
//	 */
//	private class PausedState extends BaseState{
//
//		@Override
//		public void onEntry() {
//			mEnumState = State.PAUSED;
//			mTimer.cancel();
//		}
//
//		@Override
//		public void stopTimer() {
//			changeState(new StoppedState());
//		}
//
//		@Override
//		public void resumeTimer() {
//			changeState(new StartedState());
//		}
//	}
//	
//	/* After timer complete, 
//	 * run playback state
//	 */
//	private class PlayMediaState extends BaseState{
//
//		@Override
//		public void onEntry() {
//			mEnumState = State.PLAYING_MEDIA;
//			mBuzzer.playSound();
//		}
//
//		@Override
//		public void stopTimer() {
//			mBuzzer.stopSound();
//			//changeState(new StartedState());
//			changeState(new StoppedState());
//		}
//
//		@Override
//		public void onMediaPlaybackFinish() {
//			if(mIsRestPeriodTimerRunning || mRestPeriodTime <= 0){
//				mIsRestPeriodTimerRunning = false;
//				changeState(new StartedState());
//				return;
//			}
//			changeState(new RestPeriodState());
//		}
//	}
//
//	@Override
//	public boolean isTimerRunning() {
//		Log.d(TAG,"isTimerRunning:"+(mCurrentState instanceof StartedState));
//		return mCurrentState instanceof StartedState;
//	}
//
//
//	@Override
//	public void onTimeTick(long timeLeft) {
//		mLeftTime = timeLeft;
//		//Log.d(TAG,"time left:"+timeLeft/1000);
//		for(ICountDownListener listener:mListener)
//			listener.onTimeTick(timeLeft);
//	}
//
//
//	@Override
//	public void onTimerFinish() {
//		mCurrentState.onTimerFinish();
//		//Log.d(TAG,"Timer finished");
//		for(ICountDownListener listener:mListener)
//			listener.onTimerFinish();	
//	}
//
//	@Override
//	public long getTime() {
//		return mTime;
//	}
//
//	@Override
//	public void onPlaybackFinish() {
//		Log.d(TAG,"onPlaybackFinish");
//		mCurrentState.onMediaPlaybackFinish();
//	}
//
//
//	@Override
//	public boolean setMediaSource(Uri resId) {
//		return mBuzzer.setMediaSource(resId);
//	}
//
//
//	@Override
//	public boolean isRestTimerRunning() {
//		return mIsRestPeriodTimerRunning;
//	}
//
//
//	@Override
//	public void setRestTimer(long millis) {
//		mRestPeriodTime = millis;
//	}
//}
