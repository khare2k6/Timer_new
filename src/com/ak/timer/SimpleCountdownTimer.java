package com.ak.timer;

import android.os.CountDownTimer;

/*
 * Not using now
 */
public class SimpleCountdownTimer extends CountDownTimer{

	private ICountDownListener listener;
	public SimpleCountdownTimer(long millisInFuture, long countDownInterval,ICountDownListener callback) {
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
