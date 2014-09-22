package com.ak.timer;

public interface ICountDownListener {

	public void onTimeTick(long milliSecUntilFinished);
	public void onTimerFinish();
}
