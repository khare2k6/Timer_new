package com.ak.timer;

import android.os.CountDownTimer;

/*
 * Not using now
 */
public class MyCountDownTimer extends CountDownTimer{

	private ICountDownListener listener;
	public MyCountDownTimer(long millisInFuture, long countDownInterval,ICountDownListener callback) {
		super(millisInFuture, countDownInterval);
		this.listener = callback;
	}
	@Override
	public void onFinish() {
		listener.onTimerFinish();
	}
	@Override
	public void onTick(long millisUntilFinished) {
		listener.onTimeTick(millisUntilFinished);
	}

}
