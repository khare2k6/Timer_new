package com.ak.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class TimerExpiredReceiver extends BroadcastReceiver {


	private String TAG = TimerExpiredReceiver.class.getSimpleName();
	private PowerManager mPowerManager;
	private WakeLock mWakeLock;
	private final static int WAKELOCK_TIMEOUT = 2000;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG ,"onReceive of TimerExpiredReceiveer");
		mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		if(mWakeLock != null){
			Log.d(TAG,"wake lock acquired");
			mWakeLock.acquire(WAKELOCK_TIMEOUT);	
		}else{
			Log.d(TAG,"unable to acquire wakelock");
		}
		MyCountDownTimer2 timer = MyCountDownTimer2.getInstance();
		Handler timerhandler = timer.getHandler();
		Message msg = Message.obtain(timerhandler, MyCountDownTimer2.TIMER_EXPIRED);
		timerhandler.sendMessage(msg);

	}

}
