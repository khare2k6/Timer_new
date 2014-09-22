package com.ak.remotecontroller;

import android.content.Context;
import android.net.Uri;

import com.ak.timer.TimerStateMachine;
import com.ak.timer.TimerStateMachine.State;
import com.ak.timer.ITimer;
//import com.ak.timer.TimerStateMachine;

public class RemoteController implements IController{

	private ITimer mTimer;
	private Context mContext;
	public RemoteController(Context context){
		mContext = context;
		mTimer = new TimerStateMachine(mContext);
	}

	@Override
	public void startTimer(long time) {
		mTimer.startTimer(time);
	}

	@Override
	public void stopTimer() {
		mTimer.stopTimer();
	}

	@Override
	public void pauseTimer() {
		mTimer.pauseTimer();
	
	}

	@Override
	public void resumeTimer() {
		mTimer.resumeTimer();	
	}

	@Override
	public boolean setMediaSource(Uri resId) {
		return mTimer.setMediaSource(resId);
	}

	@Override
	public void registerListener(ITimerActionsListener listener) {
		mTimer.registerListener(listener);
	}

	@Override
	public boolean isTimerRunning() {
		return mTimer.isTimerRunning();
	}

	@Override
	public long getTime() {
		return mTimer.getTime();
	}

	@Override
	public boolean isRestTimerRunning() {
		return mTimer.isRestTimerRunning();
	}

	@Override
	public void setRestTimer(long millis) {
		 mTimer.setRestTimer(millis);	
	}
	


}
