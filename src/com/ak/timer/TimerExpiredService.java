package com.ak.timer;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class TimerExpiredService extends IntentService {
	final static String WORKER_THREAD_NAME = "TimerExpiryThread";
	final static String TAG = TimerExpiredService.class.getSimpleName();

	public TimerExpiredService() {
		super(WORKER_THREAD_NAME);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		MyCountDownTimer2 timer = MyCountDownTimer2.getInstance();
		Log.d(TAG,"onHandleIntent..calling timerStop");
		Handler timerHandler = timer.getHandler();
		Message msg = Message.obtain(timerHandler, MyCountDownTimer2.TIMER_EXPIRED);
		timerHandler.sendMessage(msg);
	}

}
